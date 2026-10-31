package com.inventoryhover;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.Prayer;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.Text;
import net.runelite.client.util.WildcardMatcher;

// One shared instance: the practice status overlay reads its practice lines
@Singleton
class InventoryHoverHighlightOverlay extends Overlay
{
	// Placeholder item the game puts in empty slots of some item containers
	private static final int EMPTY_SLOT_ITEM = 6512;

	// Fraction of a tick the pulse cue stays visible
	private static final double PULSE_LENGTH = 0.25;

	private static final int MARKER_SIZE = 6;

	// How long a clicked practice target keeps glowing
	private static final long PRACTICE_HIT_NANOS = 400_000_000L;

	// Game popups inside tracked panels that must stay readable: spell and prayer
	// descriptions and filter menus
	private static final int[] POPUPS = {
		InterfaceID.MagicSpellbook.TOOLTIP,
		InterfaceID.MagicSpellbook.FILTERMENU_CONTAINER,
		InterfaceID.Prayerbook.TOOLTIP,
		InterfaceID.Prayerbook.FILTERMENU_CONTAINER,
	};

	private enum PanelKind
	{
		INVENTORY,
		BANK,
		PRAYERS,
		SPELLS
	}

	/** One panel (inventory, prayers, spells) and the clickable areas inside it. */
	private static class Panel
	{
		final PanelKind kind;
		/** Prefix of the tab settings this panel uses. */
		final String tab;
		final Rectangle area;
		/** Whether the tick cue applies; only the prayer tab, where flicking happens. */
		boolean tickCue;
		/** Areas the hover highlight can fill. */
		final List<Rectangle> targets = new ArrayList<>();
		/** Every slot, including skipped ones, so dead area shading skips them. */
		final List<Rectangle> slots = new ArrayList<>();
		/** Slots with no item, where a click does nothing. */
		final List<Rectangle> empties = new ArrayList<>();
		/** Slot widgets holding an item, with their bounds. */
		final List<ItemSlot> items = new ArrayList<>();
		/** Other clickable areas, like filter buttons, that are neither dead nor hover targets. */
		final List<Rectangle> others = new ArrayList<>();
		/** Widget of each target, for drawing its icon again. */
		final Map<Rectangle, Widget> widgets = new HashMap<>();

		Panel(PanelKind kind, Rectangle area)
		{
			this.kind = kind;
			this.area = area;
			this.tab = kind == PanelKind.PRAYERS ? TabSettings.PRAYERS
				: kind == PanelKind.SPELLS ? TabSettings.SPELLS
				: TabSettings.INVENTORY;
		}
	}

	private static class ItemSlot
	{
		final Widget widget;
		final Rectangle bounds;

		ItemSlot(Widget widget, Rectangle bounds)
		{
			this.widget = widget;
			this.bounds = bounds;
		}
	}

	/** An area marked at a moment, like a hover fading out or a miss outline. */
	private static class Mark
	{
		final Rectangle bounds;
		final boolean tickCue;
		final long time;
		/** Panel the mark belongs to, or null when it shows in any panel. */
		final PanelKind kind;

		Mark(Rectangle bounds, boolean tickCue, long time)
		{
			this(bounds, tickCue, time, null);
		}

		Mark(Rectangle bounds, boolean tickCue, long time, PanelKind kind)
		{
			this.bounds = bounds;
			this.tickCue = tickCue;
			this.time = time;
			this.kind = kind;
		}
	}

	/** Recently hovered areas that are fading out. */
	private final List<Mark> trails = new ArrayList<>();

	/** Slots nearest a recent dead click. */
	private final List<Mark> missOutlines = new ArrayList<>();

	/** Clicks that did nothing, as {x, y, nanoTime}. */
	private final List<long[]> deadClicks = new ArrayList<>();

	/** Click time of the last click that hit nothing, for click result colors. */
	private long lastMissNanos;

	/**
	 * A dead click kept as a small dot, stored relative to the panel it landed
	 * in. Dots only show while their own panel is open, so switching between the
	 * inventory, prayer and spellbook tabs keeps each tab's misses apart.
	 */
	private static class MissDot
	{
		final PanelKind kind;
		final int dx;
		final int dy;
		final long time;

		MissDot(PanelKind kind, int dx, int dy, long time)
		{
			this.kind = kind;
			this.dx = dx;
			this.dy = dy;
			this.time = time;
		}
	}

	private final List<MissDot> missDots = new ArrayList<>();

	/** Panels of the frame being drawn. */
	private List<Panel> framePanels = new ArrayList<>();

	/** Fill passes, one per panel, drawn right after the game draws that panel's interface. */
	private final List<Overlay> fillOverlays = List.of(
		new FillOverlay(InterfaceID.INVENTORY, PanelKind.INVENTORY),
		new FillOverlay(InterfaceID.BANKSIDE, PanelKind.BANK),
		new FillOverlay(InterfaceID.PRAYERBOOK, PanelKind.PRAYERS),
		new FillOverlay(InterfaceID.MAGIC_SPELLBOOK, PanelKind.SPELLS));

	/** Practice targets clicked recently, glowing in the hit color. */
	private final List<Mark> practiceHits = new ArrayList<>();

	private Rectangle lastTarget;
	private boolean lastTickCue;
	private PanelKind lastKind;

	/** Area under the mouse that is waiting out the hover delay. */
	private Rectangle hoverCandidate;
	private long hoverSince;

	private final Random random = new Random();
	private Color discoColor = Color.WHITE;
	private long discoSince;

	private final Client client;
	private final InventoryHoverHighlightPlugin plugin;
	private final InventoryHoverHighlightConfig config;
	private final ItemManager itemManager;
	private final SpriteManager spriteManager;
	private final TabSettings tabs;

	@Inject
	InventoryHoverHighlightOverlay(Client client, InventoryHoverHighlightPlugin plugin,
		InventoryHoverHighlightConfig config, ItemManager itemManager, SpriteManager spriteManager, TabSettings tabs)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.itemManager = itemManager;
		this.spriteManager = spriteManager;
		this.tabs = tabs;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		// Lower priorities draw first, so other plugins' overlays on the same panels, like rune counts on spells, stay on top
		setPriority(PRIORITY_LOW - 1);
	}

	/** The fill pass overlays, added and removed with this overlay. */
	List<Overlay> fillOverlays()
	{
		return fillOverlays;
	}

	/**
	 * Draws the slot backgrounds and hover fills of one panel right after the
	 * game draws its interface. Item overlays from other plugins, such as item
	 * charges, draw after the same interface at a higher priority, so their text
	 * stays on top of the fills.
	 */
	private class FillOverlay extends Overlay
	{
		private final PanelKind kind;

		FillOverlay(int interfaceId, PanelKind kind)
		{
			this.kind = kind;
			setPosition(OverlayPosition.DYNAMIC);
			setLayer(OverlayLayer.MANUAL);
			// Item overlays use PRIORITY_LOW, and lower priorities draw first
			setPriority(PRIORITY_LOW - 1);
			drawAfterInterface(interfaceId);
		}

		@Override
		public Dimension render(Graphics2D graphics)
		{
			renderFills(graphics, kind);
			return null;
		}
	}

	/**
	 * Draws the fills of one panel from the hover state of the last frame. The
	 * main pass runs after every interface, so it is always one frame ahead.
	 */
	private void renderFills(Graphics2D graphics, PanelKind kind)
	{
		if (plugin.pausedForEmptyInventory())
		{
			return;
		}
		Panel panel = panelOf(framePanels, kind);
		if (panel == null)
		{
			return;
		}

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		long now = System.nanoTime();
		double phase = tickPhase(now);
		List<Panel> panels = List.of(panel);

		// The game's tooltips and filter menus are part of the same interface, so keep the fills off them
		Area popups = collectPopups();
		Shape originalClip = graphics.getClip();
		if (!popups.isEmpty())
		{
			Area clip = new Area(originalClip != null ? originalClip
				: new Rectangle(0, 0, client.getCanvasWidth(), client.getCanvasHeight()));
			clip.subtract(popups);
			graphics.setClip(clip);
		}

		drawSlotBackgrounds(graphics, panels);
		drawPrayerBookCue(graphics, panels, lastTarget, phase, true);
		drawTrails(graphics, panels, phase, now, true);
		if (lastTarget != null && lastKind == kind)
		{
			drawHighlight(graphics, lastTarget, phase, lastTickCue, false, panel, true);
		}
		graphics.setClip(originalClip);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (plugin.pausedForEmptyInventory())
		{
			// Drop clicks made meanwhile, so nothing replays when an item comes back
			plugin.mouseClicks.clear();
			return null;
		}
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		long now = System.nanoTime();

		List<Panel> panels = collectPanels();
		framePanels = panels;

		Point mouse = client.getMouseCanvasPosition();
		boolean hoverEnabled = mouse != null && mouse.getX() >= 0 && mouse.getY() >= 0;

		Panel panel = null;
		Rectangle candidate = null;
		if (hoverEnabled)
		{
			panel = panelAt(panels, mouse);
			if (panel != null && !(tabOf(panel.kind).hideWhenMenuOpen && client.isMenuOpen()))
			{
				candidate = smallestAt(panel.targets, mouse);
			}
		}

		// Never draw over the game's own popups
		Area popups = collectPopups();
		Shape originalClip = graphics.getClip();
		if (!popups.isEmpty())
		{
			Area clip = new Area(originalClip != null ? originalClip
				: new Rectangle(0, 0, client.getCanvasWidth(), client.getCanvasHeight()));
			clip.subtract(popups);
			graphics.setClip(clip);
			if (mouse != null && popups.contains(mouse.getX(), mouse.getY()))
			{
				panel = null;
				candidate = null;
			}
		}

		Rectangle target = applyHoverDelay(candidate, panel == null ? 0 : tabOf(panel.kind).hoverDelay, now);
		updateDisco(target, panel, now);

		drawDeadAreas(graphics, panels, panel);
		drawSpotlights(graphics, panels, panel, mouse);
		drawHiddenItems(graphics, panels);
		drawCooldowns(graphics, panels, now);
		drawGrid(graphics, panels, panel, target);
		drawActivePrayers(graphics);
		drawSelectedSpell(graphics);
		drawAutocast(graphics, target);
		drawFlashes(graphics, now);

		double phase = tickPhase(now);
		drawPrayerBookCue(graphics, panels, target, phase, false);
		updateTrails(target, panel, now);
		drawTrails(graphics, panels, phase, now, false);

		if (target != null)
		{
			drawHighlight(graphics, target, phase, panel.tickCue, false, panel, false);
		}

		drawQueuedClicks(graphics);
		updatePractice(panels, now);
		processClicks(panels, popups);
		drawPractice(graphics, panels, now);
		drawMissDots(graphics, panels, now);
		drawMissOutlines(graphics, panels, now);
		drawRings(graphics, deadClicks, config.misclickColor(), now);
		drawRings(graphics, plugin.rejectedClicks, config.rejectedClickColor(), now);
		graphics.setClip(originalClip);
		return null;
	}

	/** Picks a new disco color on a new hovered area, or when the disco interval runs out. */
	private void updateDisco(Rectangle target, Panel panel, long now)
	{
		TabSettings.Tab tab = tabOf(panel == null ? null : panel.kind);
		if (!tab.discoMode || target == null)
		{
			return;
		}

		boolean newArea = !target.equals(lastTarget);
		boolean expired = tab.discoInterval > 0 && now - discoSince >= tab.discoInterval * 1_000_000L;
		if (newArea || expired)
		{
			discoColor = Color.getHSBColor(random.nextFloat(), 0.85f, 1f);
			discoSince = now;
		}
	}

	/** The hovered area once the mouse has rested on it for the hover delay, or null. */
	private Rectangle applyHoverDelay(Rectangle candidate, int delayMillis, long now)
	{
		if (!Objects.equals(candidate, hoverCandidate))
		{
			hoverCandidate = candidate;
			hoverSince = now;
		}
		if (candidate == null || now - hoverSince < delayMillis * 1_000_000L)
		{
			return null;
		}
		return candidate;
	}

	/** Visible bounds of the game popups, from their drawn leaf widgets. */
	private Area collectPopups()
	{
		Area area = new Area();
		for (int id : POPUPS)
		{
			Widget widget = client.getWidget(id);
			if (widget != null && !widget.isHidden())
			{
				addLeaves(widget, area);
			}
		}
		return area;
	}

	/**
	 * Adds the bounds of visible widgets with no visible children. Popup roots are
	 * often layers the size of the whole panel, so only the drawn parts count.
	 */
	private static boolean addLeaves(Widget widget, Area area)
	{
		if (widget == null || widget.isSelfHidden())
		{
			return false;
		}

		boolean childAdded = false;
		for (Widget[] children : new Widget[][]{
			widget.getStaticChildren(), widget.getDynamicChildren(), widget.getNestedChildren()})
		{
			if (children != null)
			{
				for (Widget child : children)
				{
					childAdded |= addLeaves(child, area);
				}
			}
		}
		if (childAdded)
		{
			return true;
		}

		// An empty layer draws nothing
		Rectangle bounds = widget.getBounds();
		if (widget.getType() == WidgetType.LAYER || bounds == null || bounds.width <= 0 || bounds.height <= 0)
		{
			return false;
		}
		area.add(new Area(bounds));
		return true;
	}

	// Clickable area detection

	private List<Panel> collectPanels()
	{
		List<Panel> panels = new ArrayList<>();
		addSlotPanel(panels, PanelKind.INVENTORY, InterfaceID.Inventory.ITEMS, -1);
		if (config.bankInventory())
		{
			// The bank inventory's item layer hugs its slots, so its frame is what holds the gaps
			addSlotPanel(panels, PanelKind.BANK, InterfaceID.Bankside.ITEMS, InterfaceID.Bankside.ITEMS_CONTAINER);
		}
		boolean outer = config.deadOuterAreas();
		if (config.prayers())
		{
			addClickablePanel(panels, PanelKind.PRAYERS, InterfaceID.Prayerbook.CONTAINER,
				outer ? InterfaceID.Prayerbook.UNIVERSE : -1);
		}
		if (config.spells())
		{
			addClickablePanel(panels, PanelKind.SPELLS, InterfaceID.MagicSpellbook.SPELLLAYER,
				outer ? InterfaceID.MagicSpellbook.UNIVERSE : -1);
		}
		return panels;
	}

	/**
	 * Widens a panel to an outer widget, like the whole tab. The outer widget's
	 * other clickable parts, like filter buttons, are kept apart so they are
	 * neither shaded nor counted as misses.
	 */
	private static Rectangle outerArea(Rectangle area, Widget outer)
	{
		if (outer == null || outer.isHidden() || outer.getBounds() == null)
		{
			return area;
		}
		return area.union(outer.getBounds());
	}

	private static void addOthers(Panel panel, Widget outer)
	{
		if (outer == null || outer.isHidden())
		{
			return;
		}

		Map<Rectangle, Widget> found = new LinkedHashMap<>();
		collectClickable(outer, found);
		for (Rectangle r : found.keySet())
		{
			if (!panel.slots.contains(r) && !panel.targets.contains(r))
			{
				panel.others.add(r);
			}
		}
	}

	/**
	 * Adds an item container panel. Each dynamic child of the container is one
	 * slot, and its bounds are the exact area the client accepts clicks in.
	 */
	private void addSlotPanel(List<Panel> panels, PanelKind kind, int containerId, int outerId)
	{
		Widget container = client.getWidget(containerId);
		if (container == null || container.isHidden() || container.getBounds() == null)
		{
			return;
		}

		Widget[] slots = container.getDynamicChildren();
		if (slots == null)
		{
			return;
		}

		Point origin = container.getCanvasLocation();
		if (origin == null)
		{
			return;
		}

		Widget outer = outerId == -1 ? null : client.getWidget(outerId);
		Panel panel = new Panel(kind, outerArea(container.getBounds(), outer));
		for (Widget slot : slots)
		{
			if (slot == null || slot.getWidth() <= 0 || slot.getHeight() <= 0)
			{
				continue;
			}

			// Hidden (empty) slots are not rendered, so getBounds() is stale for
			// them. Build every slot from the container position plus the slot's
			// layout offset, which the game keeps current even while hidden.
			Rectangle bounds = new Rectangle(
				origin.getX() + slot.getRelativeX(),
				origin.getY() + slot.getRelativeY(),
				slot.getWidth(),
				slot.getHeight());

			panel.slots.add(bounds);
			boolean empty = slot.isSelfHidden() || isEmpty(slot);
			if (empty)
			{
				panel.empties.add(bounds);
			}
			else
			{
				panel.items.add(new ItemSlot(slot, bounds));
				panel.widgets.put(bounds, slot);
			}
			// The game hides the widget of an empty slot, so the hidden flag
			// only matters when empty slots are excluded
			if (!config.onlyItems() || !empty)
			{
				panel.targets.add(bounds);
			}
		}
		addOthers(panel, outer);
		panels.add(panel);
	}

	/**
	 * Adds a panel of every visible clickable widget under the root. Bounds are
	 * read live each frame, so this follows the prayer/spellbook layout when it
	 * is filtered or resized.
	 */
	private void addClickablePanel(List<Panel> panels, PanelKind kind, int rootId, int outerId)
	{
		Widget root = client.getWidget(rootId);
		if (root == null || root.isHidden() || root.getBounds() == null)
		{
			return;
		}

		Map<Rectangle, Widget> found = new LinkedHashMap<>();
		collectClickable(root, found);

		Widget outer = outerId == -1 ? null : client.getWidget(outerId);
		Panel panel = new Panel(kind, outerArea(root.getBounds(), outer));
		panel.tickCue = kind == PanelKind.PRAYERS;
		// A clickable widget can contain a smaller clickable child; keep only the innermost
		for (Map.Entry<Rectangle, Widget> entry : found.entrySet())
		{
			Rectangle r = entry.getKey();
			boolean containsOther = false;
			for (Rectangle other : found.keySet())
			{
				if (other != r && r.contains(other))
				{
					containsOther = true;
					break;
				}
			}
			if (!containsOther)
			{
				panel.targets.add(r);
				panel.widgets.put(r, entry.getValue());
			}
		}
		panel.slots.addAll(panel.targets);
		addOthers(panel, outer);
		panels.add(panel);
	}

	private static void collectClickable(Widget widget, Map<Rectangle, Widget> found)
	{
		if (widget == null || widget.isSelfHidden())
		{
			return;
		}

		Rectangle bounds = widget.getBounds();
		if (bounds != null && bounds.width > 0 && bounds.height > 0 && isClickable(widget))
		{
			found.putIfAbsent(bounds, widget);
		}

		collectClickable(widget.getStaticChildren(), found);
		collectClickable(widget.getDynamicChildren(), found);
		collectClickable(widget.getNestedChildren(), found);
	}

	private static void collectClickable(Widget[] widgets, Map<Rectangle, Widget> found)
	{
		if (widgets != null)
		{
			for (Widget child : widgets)
			{
				collectClickable(child, found);
			}
		}
	}

	private static Panel panelAt(List<Panel> panels, Point mouse)
	{
		for (Panel panel : panels)
		{
			if (panel.area.contains(mouse.getX(), mouse.getY()))
			{
				return panel;
			}
		}
		return null;
	}

	private static Rectangle smallestAt(List<Rectangle> rects, Point mouse)
	{
		Rectangle best = null;
		for (Rectangle r : rects)
		{
			if (r.contains(mouse.getX(), mouse.getY()) && (best == null || area(r) < area(best)))
			{
				best = r;
			}
		}
		return best;
	}

	/** The target with an item nearest a point, the one a miss most likely meant, or null. */
	private static Rectangle nearestItem(Panel panel, Point at)
	{
		Rectangle best = null;
		long bestDistance = Long.MAX_VALUE;
		for (Rectangle r : panel.targets)
		{
			if (panel.empties.contains(r))
			{
				continue;
			}
			long dx = Math.max(0, Math.max(r.x - at.getX(), at.getX() - (r.x + r.width)));
			long dy = Math.max(0, Math.max(r.y - at.getY(), at.getY() - (r.y + r.height)));
			long distance = dx * dx + dy * dy;
			if (distance < bestDistance)
			{
				best = r;
				bestDistance = distance;
			}
		}
		return best;
	}

	private static boolean isClickable(Widget widget)
	{
		String verb = widget.getTargetVerb();
		if (verb != null && !verb.isEmpty())
		{
			return true;
		}

		String[] actions = widget.getActions();
		if (actions != null)
		{
			for (String action : actions)
			{
				if (action != null && !action.isEmpty())
				{
					return true;
				}
			}
		}
		return false;
	}

	private static long area(Rectangle r)
	{
		return (long) r.width * r.height;
	}

	private static boolean isEmpty(Widget slot)
	{
		int id = slot.getItemId();
		return id == -1 || id == EMPTY_SLOT_ITEM || slot.getItemQuantity() <= 0;
	}

	// Drawing

	/**
	 * Progress through the current click window, 0 when it opens and 1 when it
	 * closes. A click reaches the server about one round trip after the client
	 * sees a tick, so the window boundary is one round trip (the latency
	 * setting) before each tick the client sees, not the tick itself.
	 */
	private double tickPhase(long now)
	{
		long elapsed = now - plugin.lastTickNanos + config.tickLatency() * 1_000_000L;
		double phase = (double) elapsed / InventoryHoverHighlightPlugin.TICK_NANOS;
		// Wrap past the boundary, since the next tick arrives after the window closes
		return phase - Math.floor(phase);
	}

	/**
	 * Draws the hover highlight. With {@code cueOnly}, draws only the tick cue,
	 * for prayers that are not hovered. With {@code fills}, draws only the fills,
	 * which the fill pass draws before other plugins' item overlays; otherwise
	 * draws everything but the fills.
	 */
	private void drawHighlight(Graphics2D graphics, Rectangle bounds, double phase, boolean useTickCue, boolean cueOnly,
		Panel panel, boolean fills)
	{
		// Every cue mode restarts at the cue position: 0 at the cue point, 1 just before the next
		double cycle = phase - config.pulsePosition() / 100.0;
		cycle -= Math.floor(cycle);

		TabSettings.Tab tab = tabOf(panel == null ? null : panel.kind);
		InventoryHoverHighlightConfig.HighlightStyle style = tab.style;
		InventoryHoverHighlightConfig.TickCue cue = useTickCue
			? config.tickCue()
			: InventoryHoverHighlightConfig.TickCue.OFF;

		if (fills)
		{
			drawHighlightFills(graphics, bounds, cycle, cue, cueOnly, panel, tab);
			return;
		}

		// Drain and Color are drawn on the outline, so they force it on even in fill-only style
		boolean outlineCue = cue == InventoryHoverHighlightConfig.TickCue.DRAIN
			|| cue == InventoryHoverHighlightConfig.TickCue.COLOR;
		if ((cueOnly || style == InventoryHoverHighlightConfig.HighlightStyle.FILL) && !outlineCue)
		{
			return;
		}
		// A miss outline replaces the normal outline until it fades
		if (!outlineCue && isMissOutlined(bounds))
		{
			return;
		}

		Color outline = hoverColor(tab.outlineColor, tab);
		if (cue == InventoryHoverHighlightConfig.TickCue.COLOR)
		{
			// Hue from green (1/3) at the start of the tick to red (0) at the end
			Color hue = Color.getHSBColor((float) ((1 - cycle) / 3), 1f, 1f);
			outline = withAlpha(hue, outline.getAlpha());
		}

		int width = tab.outlineWidth;
		graphics.setStroke(new BasicStroke(width));
		Shape shape = outlineShape(bounds, width, tab.cornerRadius);

		if (cue == InventoryHoverHighlightConfig.TickCue.DRAIN)
		{
			// Faint full outline, with a bright part that shrinks as the tick runs out
			graphics.setColor(withAlpha(outline, outline.getAlpha() / 3));
			graphics.draw(shape);
			graphics.setColor(outline);
			drawPartial(graphics, shape, 1 - cycle);
		}
		else
		{
			graphics.setColor(outline);
			graphics.draw(shape);
		}
	}

	/** The hover fill and pulse cue of a highlight. */
	private void drawHighlightFills(Graphics2D graphics, Rectangle bounds, double cycle,
		InventoryHoverHighlightConfig.TickCue cue, boolean cueOnly, Panel panel, TabSettings.Tab tab)
	{
		// Keep the icon's own colors by leaving its pixels out of the fill
		Panel iconPanel = !cueOnly && tab.keepIconColors ? panel : null;
		if (!cueOnly && tab.style != InventoryHoverHighlightConfig.HighlightStyle.OUTLINE)
		{
			fillAroundIcon(graphics, iconPanel, bounds, fillShape(bounds, tab.cornerRadius),
				hoverColor(tab.fillColor, tab));
		}

		if (cue == InventoryHoverHighlightConfig.TickCue.PULSE)
		{
			if (cycle < PULSE_LENGTH)
			{
				Color pulse = config.pulseColor();
				double strength = 1 - cycle / PULSE_LENGTH;
				fillAroundIcon(graphics, iconPanel, bounds, fillShape(bounds, tab.cornerRadius),
					withAlpha(pulse, (int) (pulse.getAlpha() * strength)));
			}
		}
	}

	/** A hover color, swapped for the disco color in disco mode with the same alpha. */
	private Color hoverColor(Color color, TabSettings.Tab tab)
	{
		return tab.discoMode ? withAlpha(discoColor, color.getAlpha()) : color;
	}

	/** Tick cue on the listed prayers that are not hovered. */
	private void drawPrayerBookCue(Graphics2D graphics, List<Panel> panels, Rectangle target, double phase,
		boolean fills)
	{
		if (!config.tickCueAllPrayers() || config.tickCue() == InventoryHoverHighlightConfig.TickCue.OFF
			|| plugin.cuePrayers.isEmpty() || panels.stream().noneMatch(p -> p.kind == PanelKind.PRAYERS))
		{
			return;
		}

		Panel prayers = panelOf(panels, PanelKind.PRAYERS);
		for (int i = 0; i < InventoryHoverHighlightPlugin.PRAYER_COUNT; i++)
		{
			Widget widget = client.getWidget(InterfaceID.Prayerbook.PRAYER1 + i);
			if (widget == null || widget.isHidden() || widget.getBounds() == null || widget.getName() == null)
			{
				continue;
			}

			Rectangle bounds = widget.getBounds();
			String name = Text.removeTags(widget.getName());
			if (!bounds.equals(target) && plugin.cuePrayers.stream().anyMatch(p -> WildcardMatcher.matches(p, name)))
			{
				drawHighlight(graphics, bounds, phase, true, true, prayers, fills);
			}
		}
	}

	/** Starts a fade-out for the previously hovered area when the hover moves. */
	private void updateTrails(Rectangle target, Panel panel, long now)
	{
		if (lastTarget != null && !lastTarget.equals(target))
		{
			Rectangle left = lastTarget;
			trails.removeIf(t -> t.bounds.equals(left));
			if (tabOf(lastKind).hoverFade > 0)
			{
				trails.add(new Mark(left, lastTickCue, now, lastKind));
			}
		}
		if (target != null)
		{
			// The live hover replaces any fade on the same area
			trails.removeIf(t -> t.bounds.equals(target));
		}
		lastTarget = target;
		lastTickCue = panel != null && panel.tickCue;
		lastKind = panel == null ? null : panel.kind;
	}

	/** Draws recently hovered areas, including their tick cue, fading out. */
	private void drawTrails(Graphics2D graphics, List<Panel> panels, double phase, long now, boolean fills)
	{
		Composite original = graphics.getComposite();
		for (Iterator<Mark> it = trails.iterator(); it.hasNext(); )
		{
			Mark trail = it.next();
			long duration = tabOf(trail.kind).hoverFade * 1_000_000L;
			long age = now - trail.time;
			if (age >= duration)
			{
				it.remove();
				continue;
			}

			float strength = 1f - (float) age / duration;
			graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, strength));
			Panel panel = trail.kind == null ? null : panelOf(panels, trail.kind);
			if (fills && panel == null)
			{
				continue;
			}
			drawHighlight(graphics, trail.bounds, phase, trail.tickCue, false, panel, fills);
		}
		graphics.setComposite(original);
	}

	/**
	 * Sorts the mouse clicks queued by the plugin. A click on an empty slot or a
	 * gap with no interaction counts as a miss and feeds the miss markers.
	 */
	private void processClicks(List<Panel> panels, Area popups)
	{
		for (long[] click; (click = plugin.mouseClicks.poll()) != null; )
		{
			Point at = new Point((int) click[0], (int) click[1]);
			if (popups.contains(at.getX(), at.getY()))
			{
				continue;
			}

			Panel panel = panelAt(panels, at);
			if (panel == null)
			{
				continue;
			}

			SwitchPractice.Result practice = practiceClick(panel, at, click[2]);
			if (practice == SwitchPractice.Result.HIT)
			{
				continue;
			}
			if (practice == SwitchPractice.Result.IGNORED)
			{
				boolean onEmpty = smallestAt(panel.empties, at) != null;
				if (!onEmpty && (smallestAt(panel.targets, at) != null || smallestAt(panel.others, at) != null))
				{
					continue;
				}
				plugin.stats.add(ClickStats.Kind.MISSED);
			}

			lastMissNanos = click[2];
			if (config.deadClickMarker())
			{
				deadClicks.add(click);
			}
			if (tabs.get(panel.tab).missDots)
			{
				missDots.add(new MissDot(panel.kind, at.getX() - panel.area.x, at.getY() - panel.area.y, click[2]));
			}
			if (config.missOutline())
			{
				Rectangle nearest = nearestItem(panel, at);
				if (nearest != null)
				{
					missOutlines.removeIf(m -> m.bounds.equals(nearest));
					missOutlines.add(new Mark(nearest, false, click[2], panel.kind));
				}
			}
		}
	}

	/** Small dots that stay at dead clicks, fading over the miss dot duration. */
	private void drawMissDots(Graphics2D graphics, List<Panel> panels, long now)
	{
		for (Iterator<MissDot> it = missDots.iterator(); it.hasNext(); )
		{
			MissDot dot = it.next();
			TabSettings.Tab tab = tabOf(dot.kind);
			long age = now - dot.time;
			if (!tab.missDots || age >= tab.missDotDuration * 1_000_000_000L)
			{
				it.remove();
				continue;
			}

			// Keep the dot while its panel is closed, but only draw it in its own panel
			Panel panel = panelOf(panels, dot.kind);
			if (panel == null)
			{
				continue;
			}
			long duration = tab.missDotDuration * 1_000_000_000L;

			double strength = 1 - (double) age / duration;
			Color color = tab.missDotColor;
			graphics.setColor(withAlpha(color, (int) (color.getAlpha() * strength)));
			drawDot(graphics, tab.missDotShape, panel.area.x + dot.dx, panel.area.y + dot.dy, tab.missDotSize,
				tab.missDotHollow);
		}
	}

	private static void drawDot(Graphics2D graphics, InventoryHoverHighlightConfig.MissDotShape shape,
		double x, double y, int size, boolean hollow)
	{
		double h = size / 2.0;
		graphics.setStroke(new BasicStroke(1.5f));
		Path2D path = new Path2D.Double();
		Shape dot;
		switch (shape)
		{
			case RING:
				graphics.draw(new Ellipse2D.Double(x - h, y - h, size, size));
				return;
			case CROSS:
				graphics.draw(new Line2D.Double(x - h, y - h, x + h, y + h));
				graphics.draw(new Line2D.Double(x - h, y + h, x + h, y - h));
				return;
			case PLUS:
				graphics.draw(new Line2D.Double(x - h, y, x + h, y));
				graphics.draw(new Line2D.Double(x, y - h, x, y + h));
				return;
			case SQUARE:
				dot = new Rectangle2D.Double(x - h, y - h, size, size);
				break;
			case DIAMOND:
				path.moveTo(x, y - h);
				path.lineTo(x + h, y);
				path.lineTo(x, y + h);
				path.lineTo(x - h, y);
				path.closePath();
				dot = path;
				break;
			case TRIANGLE:
				path.moveTo(x, y - h);
				path.lineTo(x + h, y + h);
				path.lineTo(x - h, y + h);
				path.closePath();
				dot = path;
				break;
			case HEART:
				// Two lobes meeting at a dip on top, curving down to a point at the bottom
				path.moveTo(x, y + h);
				path.curveTo(x - h * 1.2, y + h * 0.2, x - h * 0.6, y - h * 1.0, x, y - h * 0.4);
				path.curveTo(x + h * 0.6, y - h * 1.0, x + h * 1.2, y + h * 0.2, x, y + h);
				path.closePath();
				dot = path;
				break;
			default:
				dot = new Ellipse2D.Double(x - h, y - h, size, size);
				break;
		}

		if (hollow)
		{
			graphics.draw(dot);
		}
		else
		{
			graphics.fill(dot);
		}
	}

	/** Tab settings of a panel kind; the general settings for none. */
	private TabSettings.Tab tabOf(PanelKind kind)
	{
		if (kind == null)
		{
			return tabs.get(TabSettings.INVENTORY);
		}
		switch (kind)
		{
			case PRAYERS:
				return tabs.get(TabSettings.PRAYERS);
			case SPELLS:
				return tabs.get(TabSettings.SPELLS);
			default:
				return tabs.get(TabSettings.INVENTORY);
		}
	}

	private static Panel panelOf(List<Panel> panels, PanelKind kind)
	{
		for (Panel panel : panels)
		{
			if (panel.kind == kind)
			{
				return panel;
			}
		}
		return null;
	}

	/** Outline on the slot nearest a dead click, fading over the miss outline duration. */
	private void drawMissOutlines(Graphics2D graphics, List<Panel> panels, long now)
	{
		if (!config.missOutline())
		{
			missOutlines.clear();
			return;
		}

		long duration = config.missOutlineDuration() * 1_000_000L;
		Color color = config.missOutlineColor();
		for (Iterator<Mark> it = missOutlines.iterator(); it.hasNext(); )
		{
			Mark mark = it.next();
			long age = now - mark.time;
			if (age >= duration)
			{
				it.remove();
				continue;
			}
			// Only draw the outline while the panel it belongs to is open
			if (mark.kind != null && panelOf(panels, mark.kind) == null)
			{
				continue;
			}

			double strength = 1 - (double) age / duration;
			graphics.setColor(withAlpha(color, (int) (color.getAlpha() * strength)));
			int width = config.missOutlineWidth() > 0 ? config.missOutlineWidth() : tabOf(mark.kind).outlineWidth + 1;
			graphics.setStroke(new BasicStroke(width));
			graphics.draw(outlineShape(mark.bounds, width));
		}
	}

	private boolean isMissOutlined(Rectangle bounds)
	{
		for (Mark mark : missOutlines)
		{
			if (mark.bounds.equals(bounds))
			{
				return true;
			}
		}
		return false;
	}

	/** Fading click markers for clicks given as {x, y, nanoTime}; expired ones are removed. */
	private void drawRings(Graphics2D graphics, List<long[]> clicks, Color color, long now)
	{
		long duration = config.ringDuration() * 1_000_000L;
		InventoryHoverHighlightConfig.RingAnimation animation = config.ringAnimation();
		graphics.setStroke(new BasicStroke(2));
		for (Iterator<long[]> it = clicks.iterator(); it.hasNext(); )
		{
			long[] click = it.next();
			long age = now - click[2];
			if (age >= duration)
			{
				it.remove();
				continue;
			}

			double progress = (double) age / duration;
			graphics.setColor(withAlpha(color, (int) (color.getAlpha() * (1 - progress))));

			double radius;
			switch (animation)
			{
				case SHRINK:
					radius = 10 - 6 * progress;
					break;
				case PULSE:
					radius = 6 + 2 * Math.sin(progress * Math.PI * 4);
					break;
				case CROSS:
					radius = 4 + 2 * progress;
					graphics.draw(new Line2D.Double(click[0] - radius, click[1] - radius, click[0] + radius, click[1] + radius));
					graphics.draw(new Line2D.Double(click[0] - radius, click[1] + radius, click[0] + radius, click[1] - radius));
					continue;
				default:
					radius = 5 + 4 * progress;
					break;
			}
			graphics.draw(new Ellipse2D.Double(click[0] - radius, click[1] - radius, radius * 2, radius * 2));
		}
	}

	private void drawFlashes(Graphics2D graphics, long now)
	{
		if (!config.clickFlash())
		{
			plugin.flashes.clear();
			return;
		}

		long duration = config.clickFlashDuration() * 1_000_000L;
		Color color = config.clickFlashColor();
		for (Iterator<InventoryHoverHighlightPlugin.Click> it = plugin.flashes.iterator(); it.hasNext(); )
		{
			InventoryHoverHighlightPlugin.Click click = it.next();
			long age = now - click.time;
			if (age >= duration)
			{
				it.remove();
				continue;
			}

			double strength = 1 - (double) age / duration;
			graphics.setColor(withAlpha(color, (int) (color.getAlpha() * strength)));
			graphics.fill(fillShape(click.bounds));
		}
	}

	private void drawQueuedClicks(Graphics2D graphics)
	{
		if (!config.clickMarker())
		{
			return;
		}

		for (InventoryHoverHighlightPlugin.Click click : plugin.queuedClicks.values())
		{
			Rectangle b = click.bounds;
			// Yellow for a click the game will ignore, like eating during the eat delay
			graphics.setColor(click.blocked ? config.rejectedClickColor()
				: click.count > 1 ? config.doubleClickColor() : config.clickMarkerColor());
			graphics.fill(new Ellipse2D.Float(
				b.x + b.width - MARKER_SIZE - 2,
				b.y + 2,
				MARKER_SIZE,
				MARKER_SIZE));
		}
	}

	/** Shades the parts of a panel where a click does nothing. */
	private void drawDeadAreas(Graphics2D graphics, List<Panel> panels, Panel hovered)
	{
		InventoryHoverHighlightConfig.GridMode mode = config.deadAreas();
		if (mode == InventoryHoverHighlightConfig.GridMode.OFF)
		{
			return;
		}

		for (Panel panel : panels)
		{
			if (mode == InventoryHoverHighlightConfig.GridMode.IN_PANEL && panel != hovered)
			{
				continue;
			}

			Color color = deadAreaColor(panel.kind);
			if (color == null)
			{
				continue;
			}

			graphics.setColor(color);
			graphics.fill(deadArea(panel));
		}
	}

	/** The parts of a panel where a click does nothing. */
	private Area deadArea(Panel panel)
	{
		Area dead = new Area(panel.area);
		for (Rectangle slot : panel.slots)
		{
			dead.subtract(new Area(config.roundedAreas() ? fillShape(slot) : slot));
		}
		for (Rectangle other : panel.others)
		{
			dead.subtract(new Area(other));
		}
		return dead;
	}

	/** Dead area color of a panel, or null when its shading is off. */
	private Color deadAreaColor(PanelKind kind)
	{
		switch (kind)
		{
			case INVENTORY:
				return config.deadInventory() ? config.deadInventoryColor() : null;
			case BANK:
				return config.deadBank() ? config.deadBankColor() : null;
			case PRAYERS:
				return config.deadPrayers() ? config.deadPrayersColor() : null;
			default:
				return config.deadSpells() ? config.deadSpellsColor() : null;
		}
	}

	/** Glow at the mouse that dims the hovered panel, or every panel, outward. */
	private void drawSpotlights(Graphics2D graphics, List<Panel> panels, Panel hovered, Point mouse)
	{
		boolean mouseOk = mouse != null && mouse.getX() >= 0 && mouse.getY() >= 0;
		Paint original = graphics.getPaint();
		for (Panel panel : panels)
		{
			TabSettings.Tab tab = tabs.get(panel.tab);
			if (!tab.spotlight)
			{
				bursts.remove(panel.kind.name());
				continue;
			}

			boolean shown = mouseOk && (panel == hovered || tab.spotlightAlways);
			List<Burst> running = updateBursts(tab, panel, hovered, mouse, mouseOk);
			Shape area = tab.spotlightDeadOnly ? deadArea(panel) : panel.area;
			double strength = effectStrength(tab);
			Spot spot = spotlight(tab, strength);
			if (shown)
			{
				graphics.setPaint(spotPaint(tab, spot, mouse));
				if (replacesFill(tab, strength))
				{
					Area rest = new Area(area);
					rest.subtract(bandShape(effectBand(tab, mouse, area), mouse));
					graphics.fill(rest);
				}
				else
				{
					graphics.fill(area);
				}
			}

			if (!tab.spotlightEffectDetach)
			{
				if (shown && strength > 0)
				{
					Color result = tab.spotlightEffectOnClick && tab.spotlightClickResult ? clickResultColor(tab) : null;
					drawSpotlightEffect(graphics, tab, mouse, mouse, area, spot, strength, effectSeconds(tab), result);
				}
				continue;
			}

			// Detached effects each keep running where they started, even with the mouse elsewhere
			long now = System.nanoTime();
			double length = Math.max(100, tab.spotlightEffectLength) / 1000.0;
			double speed = Math.max(10, tab.spotlightEffectSpeed) / 100.0;
			for (Burst burst : running)
			{
				double age = (now - burst.start) / 1e9;
				double burstStrength = 1 - Math.pow(Math.min(1, age / length), 3);
				Color result = tab.spotlightEffectOnClick && tab.spotlightClickResult ? burstResultColor(tab, burst) : null;
				drawSpotlightEffect(graphics, tab, mouseOk ? mouse : burst.at, burst.at, area, spot, burstStrength,
					age * speed, result);
			}
		}
		drawClicksAnywhere(graphics, panels);
		graphics.setPaint(original);
	}

	/** The spotlight's paint: a radial gradient for a circle, else the shaped spotlight paint. */
	private static Paint spotPaint(TabSettings.Tab tab, Spot spot, Point mouse)
	{
		if (!SpotlightPaint.needsPaint(tab.spotlightType, tab.spotlightMotion))
		{
			return gradient(new Point2D.Float(mouse.getX(), mouse.getY()), spot.radius, spot.stops, spot.colors);
		}
		return new SpotlightPaint(tab.spotlightType, tab.spotlightMotion, shapeSeconds(tab), mouse.getX(), mouse.getY(),
			spot.radius, spot.stops, spot.colors);
	}

	/** A running clock for the spotlight's motion and patterns, at the effect speed. */
	private static double shapeSeconds(TabSettings.Tab tab)
	{
		return System.nanoTime() / 1e9 * Math.max(10, tab.spotlightEffectSpeed) / 100.0;
	}

	/** Click bursts outside every panel, from the inventory's effect settings. */
	private final List<Burst> worldBursts = new ArrayList<>();
	private long worldClickSeen;
	private long lastWorldBurst;

	/** Whether clicks anywhere play the inventory's effect. */
	private static boolean clicksAnywhere(TabSettings.Tab tab)
	{
		return tab.spotlight && tab.spotlightClickAnywhere && tab.spotlightEffectOnClick && tab.spotlightEffect != null
			&& tab.spotlightEffect != InventoryHoverHighlightConfig.SpotlightEffect.NONE;
	}

	/** Whether a click in this panel plays the click anywhere effect in place of the panel's own. */
	private boolean clickGoesAnywhere(Panel panel)
	{
		TabSettings.Tab tab = tabs.get(TabSettings.INVENTORY);
		return tabOf(panel.kind) == tab && tab.spotlightClickAnywherePanels && clicksAnywhere(tab);
	}

	/**
	 * Whether a click in this panel may start a burst under the click anywhere
	 * delay. With Click effects anywhere on, inventory clicks share that delay
	 * with clicks outside, so fast clicking cannot stack up bursts; other panels
	 * and the inventory without it are never held back. Counts the burst as played.
	 */
	private boolean restedForAnywhere(Panel panel, long click)
	{
		TabSettings.Tab tab = tabs.get(TabSettings.INVENTORY);
		if (tabOf(panel.kind) != tab || !clicksAnywhere(tab))
		{
			return true;
		}
		long delay = Math.max(0, tab.spotlightClickAnywhereDelay) * 1_000_000L;
		if (lastWorldBurst != 0 && click - lastWorldBurst < delay)
		{
			return false;
		}
		lastWorldBurst = click;
		return true;
	}

	private void drawClicksAnywhere(Graphics2D graphics, List<Panel> panels)
	{
		TabSettings.Tab tab = tabs.get(TabSettings.INVENTORY);
		if (!clicksAnywhere(tab))
		{
			worldBursts.clear();
			return;
		}

		long click = plugin.lastClickNanos;
		if (click != 0 && click != worldClickSeen)
		{
			worldClickSeen = click;
			int x = plugin.lastClickX;
			int y = plugin.lastClickY;
			long delay = Math.max(0, tab.spotlightClickAnywhereDelay) * 1_000_000L;
			// Measured from the last burst that played, even if it has already ended
			boolean rested = lastWorldBurst == 0 || click - lastWorldBurst >= delay;
			if (rested && panels.stream().noneMatch(p -> p.area.contains(x, y) && !clickGoesAnywhere(p)))
			{
				lastWorldBurst = click;
				worldBursts.forEach(b -> b.settled = b.settled != null ? b.settled : burstResultColor(tab, b));
				worldBursts.add(new Burst(new Point(x, y), click));
			}
		}

		long now = System.nanoTime();
		long lengthNanos = Math.max(100, tab.spotlightEffectLength) * 1_000_000L;
		worldBursts.removeIf(b -> now - b.start >= lengthNanos);
		if (worldBursts.isEmpty())
		{
			return;
		}

		// Kept off the slots of panels set to Only dead areas, as their own effects are
		Area screen = new Area(new Rectangle(0, 0, client.getCanvasWidth(), client.getCanvasHeight()));
		for (Panel panel : panels)
		{
			if (tabOf(panel.kind).spotlightDeadOnly)
			{
				Area slots = new Area(panel.area);
				slots.subtract(deadArea(panel));
				screen.subtract(slots);
			}
		}
		double length = lengthNanos / 1e9;
		double speed = Math.max(10, tab.spotlightEffectSpeed) / 100.0;
		for (Burst burst : worldBursts)
		{
			double age = (now - burst.start) / 1e9;
			double strength = 1 - Math.pow(Math.min(1, age / length), 3);
			Spot spot = spotlight(tab, strength);
			Color result = tab.spotlightClickResult ? burstResultColor(tab, burst) : null;
			// Centered on the click, so gradient colors and areas are measured from there
			drawSpotlightEffect(graphics, tab, burst.at, burst.at, screen, spot, strength, age * speed, result);
		}
	}

	/** A detached effect: where and when it started, and its click's color once it is known for sure. */
	private static class Burst
	{
		final Point at;
		final long start;
		Color settled;

		Burst(Point at, long start)
		{
			this.at = at;
			this.start = start;
		}
	}

	/** Each tab's running detached effects, oldest first. */
	private final Map<String, List<Burst>> bursts = new HashMap<>();
	private final Set<String> hoveredTabs = new HashSet<>();
	private final Map<String, Long> burstClicks = new HashMap<>();

	/**
	 * Starts and ends a tab's detached effects: a new one at each click inside
	 * the panel with Effect on click, else each time the mouse enters the panel.
	 * Each lasts the effect length; returns the running ones.
	 */
	private List<Burst> updateBursts(TabSettings.Tab tab, Panel panel, Panel hovered, Point mouse, boolean mouseOk)
	{
		// By panel, since the bank's inventory shares the inventory's tab settings
		String key = panel.kind.name();
		List<Burst> list = bursts.computeIfAbsent(key, k -> new ArrayList<>());
		if (!tab.spotlightEffectDetach || tab.spotlightEffect == null
			|| tab.spotlightEffect == InventoryHoverHighlightConfig.SpotlightEffect.NONE)
		{
			list.clear();
			hoveredTabs.remove(key);
			return list;
		}

		long now = System.nanoTime();
		if (tab.spotlightEffectOnClick)
		{
			long click = plugin.lastClickNanos;
			Long seen = burstClicks.get(key);
			if (click != 0 && (seen == null || seen != click))
			{
				burstClicks.put(key, click);
				if (panel.area.contains(plugin.lastClickX, plugin.lastClickY) && !clickGoesAnywhere(panel)
					&& restedForAnywhere(panel, click))
				{
					// The earlier bursts' clicks are settled now that a newer click came
					list.forEach(b -> b.settled = b.settled != null ? b.settled : burstResultColor(tab, b));
					list.add(new Burst(new Point(plugin.lastClickX, plugin.lastClickY), click));
				}
			}
		}
		else
		{
			boolean inside = mouseOk && panel == hovered;
			if (inside && !hoveredTabs.contains(key))
			{
				list.add(new Burst(mouse, now));
			}
			if (inside)
			{
				hoveredTabs.add(key);
			}
			else
			{
				hoveredTabs.remove(key);
			}
		}

		long length = Math.max(100, tab.spotlightEffectLength) * 1_000_000L;
		list.removeIf(b -> now - b.start >= length);
		return list;
	}

	/** A burst's click color: settled once a newer click came, else from the latest click's result. */
	private Color burstResultColor(TabSettings.Tab tab, Burst burst)
	{
		if (burst.settled != null)
		{
			return burst.settled;
		}
		if (burst.start == plugin.lastClickNanos)
		{
			return clickResultColor(tab);
		}
		return lastMissNanos == burst.start ? tab.spotlightClickMissColor : tab.spotlightClickSuccessColor;
	}



	private static final int PARTICLES = 14;
	/** Most particles one effect draws in a frame, however far its area reaches. */
	private static final int MAX_PARTICLES = 120;

	/** The animated part of a spotlight effect, inside the spotlight's area. */
	private void drawSpotlightEffect(Graphics2D graphics, TabSettings.Tab tab, Point mouse, Point origin, Shape area,
		Spot spot, double strength, double seconds, Color result)
	{
		InventoryHoverHighlightConfig.SpotlightEffect effect = tab.spotlightEffect;
		if (effect == InventoryHoverHighlightConfig.SpotlightEffect.PULSE)
		{
			return;
		}

		Shape originalClip = graphics.getClip();
		graphics.clip(area);
		float[] bandRadii = effectBand(tab, mouse, area);
		float inner = bandRadii[0];
		float outer = bandRadii[1];
		boolean detached = tab.spotlightEffectDetach;
		if (!detached)
		{
			graphics.clip(bandShape(bandRadii, mouse));
		}

		// Replacing a fill takes more particles, in the gradient's colors, so the area still reads
		boolean replacing = replacesFill(tab, strength);
		// Effects are laid out over the band's outer radius, and kept to the band by the clip. Detached ones
		// reach as far as set, with their particles spread apart as set
		float radius = outer;
		if (detached)
		{
			radius *= Math.max(25, tab.spotlightEffectReach) / 100f;
		}
		// Keep about the same density when the band reaches past the spotlight, such as out to the falloff color
		float reach = Math.min(6f, radius / Math.max(1f, tab.spotlightRadius));
		float amount = Math.max(10, Math.min(400, tab.spotlightEffectAmount)) / 100f;
		int particles = Math.max(1, Math.min(MAX_PARTICLES,
			(int) ((replacing ? PARTICLES * 3 : PARTICLES) * Math.max(1f, reach * reach) * amount)));
		if (detached)
		{
			// Twice the spread means twice the gap between particles, so a quarter as many over the same area
			float spread = Math.max(25, Math.min(1000, tab.spotlightEffectSpread)) / 100f;
			particles = Math.max(1, Math.min(MAX_PARTICLES, Math.round(particles / (spread * spread))));
		}
		float cx = mouse.getX();
		float cy = mouse.getY();
		// The effect plays out around this point: the mouse, or where a detached effect started
		float clickX = origin.getX();
		float clickY = origin.getY();
		float scale = Math.max(25, tab.spotlightEffectSize) / 100f;
		// A set color wins: the click color during click bursts, then the effect color
		Color override = result != null ? result : tab.spotlightEffectOverride;
		// The glow color is made easier to see than the glow itself; every color fades out with a click burst
		Color glow = tab.spotlightGlow;
		Color color = override != null
			? withAlpha(override, (int) (override.getAlpha() * strength))
			: withAlpha(glow, (int) (Math.max(glow.getAlpha(), 150) * strength));
		// Without a set color, every particle takes what the spotlight draws under it
		boolean fromGradient = override == null;

		switch (effect)
		{
			case RIPPLES:
				graphics.setStroke(new BasicStroke(2f * scale));
				for (int i = 0; i < 3; i++)
				{
					double phase = seconds / 1.6 + i / 3.0;
					double progress = phase - Math.floor(phase);
					float[] at = new float[]{clickX, clickY};
					float r = (float) (inner + progress * (outer - inner));
					double from = Math.hypot(at[0] - cx, at[1] - cy) + r;
					Color ring = fromGradient ? particleColor(spot, detached ? from : r, strength) : color;
					graphics.setColor(withAlpha(ring, (int) (ring.getAlpha() * (1 - progress))));
					graphics.draw(new Ellipse2D.Float(at[0] - r, at[1] - r, r * 2, r * 2));
				}
				break;
			case SPARKLES:
				for (int i = 0; i < particles; i++)
				{
					double life = 0.9 + noise(i, 0) * 0.6;
					double phase = (seconds + noise(i, 1) * life) / life;
					long cycle = (long) Math.floor(phase);
					double t = phase - cycle;
					double angle = noise(i, cycle * 2 + 3) * Math.PI * 2;
					double distance = inner + Math.sqrt(noise(i, cycle * 2 + 4)) * (outer - inner) * 0.95;
					float[] at = new float[]{clickX, clickY};
					float x = (float) (at[0] + Math.cos(angle) * distance);
					float y = (float) (at[1] + Math.sin(angle) * distance);
					double twinkle = Math.sin(t * Math.PI);
					float size = (float) (1.5 + twinkle * 2.5) * scale;
					Color sparkle = fromGradient ? particleColorAt(spot, x - cx, y - cy, strength) : color;
					graphics.setColor(withAlpha(sparkle, (int) (sparkle.getAlpha() * twinkle)));
					// A twinkle star that turns a little as it shines
					graphics.fill(star(x, y, size * 1.8f, (float) (noise(i, cycle * 2 + 5) * 0.6 + t * 0.4)));
				}
				break;
			case EMBERS:
			case SNOW:
				boolean rising = effect == InventoryHoverHighlightConfig.SpotlightEffect.EMBERS;
				for (int i = 0; i < particles; i++)
				{
					double life = 1.4 + noise(i, 0) * 1.2;
					double phase = (seconds + noise(i, 1) * life) / life;
					long cycle = (long) Math.floor(phase);
					double t = phase - cycle;
					double startX = (noise(i, cycle * 2 + 3) * 2 - 1) * radius * 0.7;
					double sway = Math.sin(seconds * 2 + i) * radius * 0.06;
					double travel = (t * 2 - 1) * radius * 0.8;
					float[] at = new float[]{clickX, clickY};
					float x = (float) (at[0] + startX + sway);
					float y = (float) (rising ? at[1] - travel : at[1] + travel);
					float size = (float) (rising ? 3 - t * 2 : 2 + noise(i, 2) * 2) * scale;
					double fade = Math.sin(t * Math.PI);
					Color own = fromGradient ? particleColorAt(spot, x - cx, y - cy, strength) : color;
					Color shade = rising ? mix(own, withAlpha(new Color(255, 120, 40), own.getAlpha()), (float) t) : own;
					Color particle = withAlpha(shade, (int) (shade.getAlpha() * fade));
					if (rising)
					{
						drawEmber(graphics, x, y, size, particle);
					}
					else
					{
						// Each flake spins slowly, some one way and some the other
						double spin = seconds * (noise(i, 6) - 0.5) * 1.5 + noise(i, 7) * Math.PI;
						drawSnowflake(graphics, x, y, size * 2.2f, (float) spin, particle, scale);
					}
				}
				break;
			case ORBIT:
				for (int i = 0; i < 6; i++)
				{
					double angle = seconds * 1.8 + i * Math.PI / 3;
					float orbit = inner + (outer - inner) * 0.55f;
					for (int tail = 0; tail < 5; tail++)
					{
						double a = angle - tail * 0.12;
						float x = (float) (clickX + Math.cos(a) * orbit);
						float y = (float) (clickY + Math.sin(a) * orbit);
						float size = (4f - tail * 0.6f) * scale;
						Color dot = fromGradient ? particleColorAt(spot, x - cx, y - cy, strength) : color;
						graphics.setColor(withAlpha(dot, dot.getAlpha() * (5 - tail) / 5));
						graphics.fill(new Ellipse2D.Float(x - size / 2, y - size / 2, size, size));
					}
				}
				break;
			default:
				break;
		}
		graphics.setClip(originalClip);
	}

	/** A four-pointed twinkle star with curved-in sides, turned by an angle. */
	private static Shape star(float x, float y, float outer, float angle)
	{
		float inner = outer * 0.28f;
		Path2D path = new Path2D.Float();
		for (int point = 0; point < 8; point++)
		{
			double a = angle + point * Math.PI / 4;
			float r = point % 2 == 0 ? outer : inner;
			float px = (float) (x + Math.cos(a) * r);
			float py = (float) (y + Math.sin(a) * r);
			if (point == 0)
			{
				path.moveTo(px, py);
			}
			else
			{
				path.lineTo(px, py);
			}
		}
		path.closePath();
		return path;
	}

	/** A six-armed snowflake with a pair of small branches on each arm. */
	private static void drawSnowflake(Graphics2D graphics, float x, float y, float radius, float angle, Color color,
		float scale)
	{
		graphics.setColor(color);
		graphics.setStroke(new BasicStroke(Math.max(0.8f, scale * 0.9f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		for (int arm = 0; arm < 6; arm++)
		{
			double a = angle + arm * Math.PI / 3;
			float ex = (float) (x + Math.cos(a) * radius);
			float ey = (float) (y + Math.sin(a) * radius);
			graphics.draw(new Line2D.Float(x, y, ex, ey));
			// Branches two thirds out, angled back toward the center
			float bx = (float) (x + Math.cos(a) * radius * 0.6);
			float by = (float) (y + Math.sin(a) * radius * 0.6);
			float branch = radius * 0.35f;
			for (int side = -1; side <= 1; side += 2)
			{
				double b = a + side * Math.PI / 4;
				graphics.draw(new Line2D.Float(bx, by, (float) (bx + Math.cos(b) * branch), (float) (by + Math.sin(b) * branch)));
			}
		}
	}

	/** A rising ember: a soft halo, a bright core stretched upward, and a short fading trail below. */
	private static void drawEmber(Graphics2D graphics, float x, float y, float size, Color color)
	{
		size = Math.max(size, 1f);
		graphics.setColor(withAlpha(color, color.getAlpha() / 4));
		graphics.fill(new Ellipse2D.Float(x - size * 1.5f, y - size * 1.5f, size * 3, size * 3));
		graphics.setColor(withAlpha(color, color.getAlpha() / 3));
		graphics.setStroke(new BasicStroke(size * 0.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.draw(new Line2D.Float(x, y + size * 0.6f, x, y + size * 3f));
		graphics.setColor(color);
		graphics.fill(new Ellipse2D.Float(x - size / 2, y - size * 0.8f, size, size * 1.6f));
		// A hot white-ish center
		graphics.setColor(withAlpha(mix(color, Color.WHITE, 0.6f), color.getAlpha()));
		graphics.fill(new Ellipse2D.Float(x - size / 4, y - size * 0.35f, size / 2, size * 0.7f));
	}

	/**
	 * The tab's color for how the last click went: refused when the game
	 * refused a click since it, missed when it hit nothing, else worked.
	 */
	private Color clickResultColor(TabSettings.Tab tab)
	{
		long click = plugin.lastClickNanos;
		if (plugin.lastRejectedNanos >= click)
		{
			return tab.spotlightClickRefusedColor;
		}
		if (lastMissNanos == click)
		{
			return tab.spotlightClickMissColor;
		}
		return tab.spotlightClickSuccessColor;
	}

	/** The gradient's color at a particle's offset from the mouse, measured the way the gradient type does. */
	private static Color particleColorAt(Spot spot, double dx, double dy, double strength)
	{
		double distance = SpotlightPaint.distance(spot.type, spot.motion, spot.seconds, dx, dy);
		Color at = particleColor(spot, distance, strength);
		double keep = SpotlightPaint.pattern(spot.type, spot.seconds, dx, dy, spot.radius);
		return keep < 1 ? withAlpha(at, (int) (at.getAlpha() * keep)) : at;
	}

	/** The gradient's color where a particle is, a bit stronger since particles cover less, faded with a click burst. */
	private static Color particleColor(Spot spot, double distance, double strength)
	{
		Color at = spot.colorAt(distance);
		// Stronger than the fill, since particles cover less, and never so faint they vanish
		return withAlpha(at, (int) (Math.min(255, Math.max(at.getAlpha() * 1.6, 120)) * strength));
	}

	/** A steady pseudo-random number from 0 to 1 for a particle and a draw, so particles need no stored state. */
	private static double noise(int particle, long salt)
	{
		long h = particle * 0x9E3779B97F4A7C15L + salt * 0xC2B2AE3D27D4EB4FL;
		h ^= h >>> 31;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 29;
		return (h >>> 11) / (double) (1L << 53);
	}

	/**
	 * Glow at the mouse that turns clear at the glow size, then darkens to the
	 * dim color at the radius. With falloff, the dim holds until the falloff
	 * start, then the same gradient runs in reverse out to the falloff end,
	 * ending in the falloff color.
	 */
	/** A spotlight's gradient: its outer radius and its shaped stops and colors. */
	private static class Spot
	{
		final float radius;
		final float[] stops;
		final Color[] colors;
		/** The gradient type, for measuring a particle's distance the same way the paint does. */
		InventoryHoverHighlightConfig.SpotlightType type;
		InventoryHoverHighlightConfig.SpotlightMotion motion;
		double seconds;

		Spot(float radius, float[] stops, Color[] colors)
		{
			this.radius = radius;
			this.stops = stops;
			this.colors = colors;
		}

		/** The gradient's color at a distance from the mouse. */
		Color colorAt(double distance)
		{
			return SpotlightPaint.colorAt((float) (distance / radius), stops, colors);
		}
	}

	/** The band the effect lives in, as {inner, outer} distances from the mouse. */
	private static float[] effectBand(TabSettings.Tab tab, Point mouse, Shape area)
	{
		float spotRadius = tab.spotlightRadius;
		float glowRadius = spotRadius * tab.spotlightGlowSize / 100f;
		InventoryHoverHighlightConfig.EffectArea where = tab.spotlightEffectArea;
		if (where == null || where == InventoryHoverHighlightConfig.EffectArea.OVERLAY)
		{
			// On top of the spotlight, within its radius, as the effects first worked
			return new float[]{0, spotRadius};
		}
		float inner = where == InventoryHoverHighlightConfig.EffectArea.GRADIENT ? glowRadius : 0;
		// With falloff, the gradient runs on past the radius to the falloff end
		float gradientEnd = tab.spotlightFalloff
			? Math.max(Math.max(spotRadius, tab.spotlightFalloffDistance) + 1, tab.spotlightFalloffEnd)
			: spotRadius;
		if (tab.spotlightFalloff && tab.spotlightFalloffColorEnd > 0 && tab.spotlightFadeEndColor == null)
		{
			// The falloff color ends, so the band does too
			float end = Math.max(Math.max(spotRadius, tab.spotlightFalloffDistance) + 1, tab.spotlightFalloffEnd);
			float clear = Math.max(spotRadius, tab.spotlightFalloffDistance)
				+ (end - Math.max(spotRadius, tab.spotlightFalloffDistance)) * (1 - tab.spotlightGlowSize / 100f);
			gradientEnd = Math.max(gradientEnd, falloffColorEnd(tab, end, clear));
		}
		else if (tab.spotlightFalloff && (tab.spotlightFalloffColor.getAlpha() > 0 || tab.spotlightFadeEndColor != null))
		{
			// The falloff color is kept past the falloff end, so the band reaches the panel's farthest corner
			Rectangle2D bounds = area.getBounds2D();
			double farX = Math.max(Math.abs(bounds.getMinX() - mouse.getX()), Math.abs(bounds.getMaxX() - mouse.getX()));
			double farY = Math.max(Math.abs(bounds.getMinY() - mouse.getY()), Math.abs(bounds.getMaxY() - mouse.getY()));
			gradientEnd = Math.max(gradientEnd, (float) Math.hypot(farX, farY) + 1);
		}
		float outer = where == InventoryHoverHighlightConfig.EffectArea.GLOW && glowRadius > 0 ? glowRadius : gradientEnd;
		return new float[]{inner, outer};
	}

	/** The effect band as a ring around the mouse. */
	private static Area bandShape(float[] band, Point mouse)
	{
		float inner = band[0];
		float outer = band[1];
		Area ring = new Area(new Ellipse2D.Float(mouse.getX() - outer, mouse.getY() - outer, outer * 2, outer * 2));
		if (inner > 0)
		{
			ring.subtract(new Area(new Ellipse2D.Float(mouse.getX() - inner, mouse.getY() - inner, inner * 2, inner * 2)));
		}
		return ring;
	}

	/** Whether the effect takes the place of the fill in its band right now. */
	private static boolean replacesFill(TabSettings.Tab tab, double strength)
	{
		return tab.spotlightEffectReplace && strength > 0 && tab.spotlightEffect != null
			&& tab.spotlightEffectArea != InventoryHoverHighlightConfig.EffectArea.OVERLAY
			&& tab.spotlightEffect != InventoryHoverHighlightConfig.SpotlightEffect.NONE
			&& tab.spotlightEffect != InventoryHoverHighlightConfig.SpotlightEffect.PULSE;
	}

	private Spot spotlight(TabSettings.Tab tab, double effectStrength)
	{
		Spot spot = shapedSpot(tab, effectStrength);
		spot.type = tab.spotlightType;
		spot.motion = tab.spotlightMotion;
		spot.seconds = shapeSeconds(tab);
		return spot;
	}

	private Spot shapedSpot(TabSettings.Tab tab, double effectStrength)
	{
		float radius = tab.spotlightRadius;
		if (tab.spotlightEffect == InventoryHoverHighlightConfig.SpotlightEffect.PULSE && effectStrength > 0)
		{
			// Breathe about 15% in and out, about once a second and a half
			radius *= 1 + 0.15f * (float) effectStrength * (float) Math.sin(effectSeconds(tab) * Math.PI * 2 / 1.5);
		}
		float glowSize = tab.spotlightGlowSize / 100f;
		Color dim = tab.spotlightDim;
		Color glow = glowSize > 0 ? tab.spotlightGlow : withAlpha(dim, 0);
		if (!tab.spotlightFalloff)
		{
			return shaped(radius, new float[]{0f, glowSize, 1f}, new Color[]{glow, withAlpha(dim, 0), dim},
				tab.spotlightShape);
		}

		// Past the falloff start the same gradient runs in reverse, scaled to the falloff length:
		// the dim fades to clear over the share it took to build up, then the falloff color fades
		// in over the glow's share, mirroring the glow at the mouse
		Color falloff = tab.spotlightFalloffColor;
		float start = Math.max(radius, tab.spotlightFalloffDistance);
		float end = Math.max(start + 1, tab.spotlightFalloffEnd);
		float clear = start + (end - start) * (1 - glowSize);
		float colorEnd = falloffColorEnd(tab, end, clear);
		if (colorEnd > end)
		{
			// The falloff color fades back out over the same length it took to fade in, mirroring it
			float fadeOut = Math.max(colorEnd - (end - clear), end);
			return shaped(colorEnd,
				new float[]{0f, glowSize * radius / colorEnd, radius / colorEnd, start / colorEnd, clear / colorEnd,
					clear / colorEnd, end / colorEnd, fadeOut / colorEnd, 1f},
				new Color[]{glow, withAlpha(dim, 0), dim, dim, withAlpha(dim, 0), withAlpha(falloff, 0), falloff, falloff,
					tab.spotlightFadeEndColor != null ? tab.spotlightFadeEndColor : withAlpha(falloff, 0)},
				tab.spotlightShape);
		}
		return shaped(end,
			new float[]{0f, glowSize * radius / end, radius / end, start / end, clear / end, clear / end, 1f},
			new Color[]{glow, withAlpha(dim, 0), dim, dim, withAlpha(dim, 0), withAlpha(falloff, 0), falloff},
			tab.spotlightShape);
	}

	/** Where the falloff color has faded back out, or 0 when it is kept to the panel's edges. */
	private static float falloffColorEnd(TabSettings.Tab tab, float end, float clear)
	{
		if (tab.spotlightFalloffColorEnd <= 0)
		{
			return 0;
		}
		// At least long enough for the mirrored fade
		return Math.max(tab.spotlightFalloffColorEnd, end + (end - clear) + 1);
	}

	/**
	 * The effect's clock in seconds, scaled by the effect speed: time since the
	 * last click for click bursts, else a running clock.
	 */
	private double effectSeconds(TabSettings.Tab tab)
	{
		return realEffectSeconds(tab) * Math.max(10, tab.spotlightEffectSpeed) / 100.0;
	}

	/** The effect's clock in real seconds, so a click burst lasts the same at any speed. */
	private double realEffectSeconds(TabSettings.Tab tab)
	{
		long now = System.nanoTime();
		return tab.spotlightEffectOnClick ? (now - plugin.lastClickNanos) / 1e9 : now / 1e9;
	}

	/** 1 while the effect plays, fading to 0 at the end of a click burst. */
	private double effectStrength(TabSettings.Tab tab)
	{
		if (tab.spotlightEffect == null || tab.spotlightEffect == InventoryHoverHighlightConfig.SpotlightEffect.NONE)
		{
			return 0;
		}
		if (!tab.spotlightEffectOnClick)
		{
			return 1;
		}
		if (plugin.lastClickNanos == 0)
		{
			return 0;
		}
		double age = realEffectSeconds(tab);
		double length = Math.max(100, tab.spotlightEffectLength) / 1000.0;
		return age >= length ? 0 : 1 - Math.pow(age / length, 3);
	}

	/** Steps in each fade when it is reshaped. */
	private static final int SHAPE_STEPS = 12;
	private static final int BANDS = 4;

	/**
	 * A gradient with every fade between two stops reshaped: eased for Soft,
	 * squeezed into a crisp edge for Sharp, or cut into steps for Banded.
	 * Smooth keeps the even fades.
	 */
	private static Spot shaped(float radius, float[] stops, Color[] colors,
		InventoryHoverHighlightConfig.SpotlightShape shape)
	{
		if (shape == null || shape == InventoryHoverHighlightConfig.SpotlightShape.SMOOTH)
		{
			return new Spot(radius, stops, colors);
		}

		List<Float> outStops = new ArrayList<>();
		List<Color> outColors = new ArrayList<>();
		outStops.add(stops[0]);
		outColors.add(colors[0]);
		for (int i = 1; i < stops.length; i++)
		{
			float from = stops[i - 1];
			float to = stops[i];
			Color a = colors[i - 1];
			Color b = colors[i];
			if (to - from < 0.002f || a.equals(b))
			{
				outStops.add(to);
				outColors.add(b);
				continue;
			}

			if (shape == InventoryHoverHighlightConfig.SpotlightShape.BANDED)
			{
				for (int band = 1; band <= BANDS; band++)
				{
					// Each band holds one shade, then jumps to the next at its edge
					float edge = from + (to - from) * band / BANDS;
					Color shade = mix(a, b, (band - 0.5f) / BANDS);
					outStops.add(edge - (to - from) / BANDS + 0.0005f);
					outColors.add(shade);
					outStops.add(edge);
					outColors.add(shade);
				}
				outStops.add(to);
				outColors.add(b);
				continue;
			}

			for (int step = 1; step <= SHAPE_STEPS; step++)
			{
				float t = (float) step / SHAPE_STEPS;
				float eased = shape == InventoryHoverHighlightConfig.SpotlightShape.SOFT
					? t * t * (3 - 2 * t)
					// Sharp: most of the change happens in a narrow band at the middle
					: 1f / (1f + (float) Math.exp(-14 * (t - 0.5f)));
				outStops.add(from + (to - from) * t);
				outColors.add(mix(a, b, eased));
			}
		}

		float[] newStops = new float[outStops.size()];
		for (int i = 0; i < newStops.length; i++)
		{
			newStops[i] = Math.min(1f, Math.max(0f, outStops.get(i)));
		}
		return new Spot(radius, newStops, outColors.toArray(new Color[0]));
	}

	/** A color part way from one to another, alpha included. */
	private static Color mix(Color a, Color b, float t)
	{
		return SpotlightPaint.mix(a, b, t);
	}

	/** A radial gradient whose stops are nudged apart, since they must keep increasing. */
	private static RadialGradientPaint gradient(Point2D center, float radius, float[] stops, Color[] colors)
	{
		float[] fixed = stops.clone();
		for (int i = 1; i < fixed.length; i++)
		{
			fixed[i] = Math.max(fixed[i], fixed[i - 1] + 0.001f);
		}
		// Squeeze back under 1 from the end if nudging pushed past it
		fixed[fixed.length - 1] = 1f;
		for (int i = fixed.length - 2; i >= 0; i--)
		{
			fixed[i] = Math.min(fixed[i], fixed[i + 1] - 0.001f);
		}
		fixed[0] = 0f;
		return new RadialGradientPaint(center, radius, fixed, colors);
	}

	/** Colors the background of every slot with the tab's slot background, around the icons. */
	private void drawSlotBackgrounds(Graphics2D graphics, List<Panel> panels)
	{
		for (Panel panel : panels)
		{
			Color color = tabs.get(panel.tab).slotBackground;
			if (color == null)
			{
				continue;
			}

			for (Rectangle slot : panel.slots)
			{
				fillAroundIcon(graphics, panel, slot, fillShape(slot), color);
			}
		}
	}

	/**
	 * Fills a shape over an area, leaving out the pixels of the icon there so the
	 * game's own icon keeps its colors. The game has already drawn the icon, so
	 * filling over it and drawing it again would blend its edges and shadow twice.
	 * With no panel, fills the whole shape.
	 */
	private void fillAroundIcon(Graphics2D graphics, Panel panel, Rectangle bounds, Shape shape, Color color)
	{
		if (panel == null || bounds.width <= 0 || bounds.height <= 0)
		{
			graphics.setColor(color);
			graphics.fill(shape);
			return;
		}

		BufferedImage mask = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = mask.createGraphics();
		g.setRenderingHints(graphics.getRenderingHints());
		g.translate(-bounds.x, -bounds.y);
		g.setColor(color);
		g.fill(shape);
		// Cut the icon out of the fill by its alpha
		g.setComposite(AlphaComposite.DstOut);
		drawIcon(g, panel, bounds);
		g.dispose();
		graphics.drawImage(mask, bounds.x, bounds.y, null);
	}

	/** Draws the icon of the item, prayer or spell in an area again, over anything drawn on it. */
	private void drawIcon(Graphics2D graphics, Panel panel, Rectangle bounds)
	{
		Widget widget = panel.widgets.get(bounds);
		if (widget == null)
		{
			return;
		}

		if (panel.kind == PanelKind.INVENTORY || panel.kind == PanelKind.BANK)
		{
			int id = widget.getItemId();
			int quantity = widget.getItemQuantity();
			if (plugin.isFaded(widget) || id <= 0 || id == EMPTY_SLOT_ITEM || quantity <= 0)
			{
				return;
			}
			boolean stackable = quantity > 1 || itemManager.getItemComposition(id).isStackable();
			BufferedImage image = itemManager.getImage(id, quantity, stackable);
			if (image != null)
			{
				graphics.drawImage(image, bounds.x, bounds.y, null);
			}
			return;
		}
		drawSprites(graphics, widget);
	}

	/**
	 * Draws a sprite the way the game draws it in a widget: its cached image
	 * already covers the sprite's whole area, padding included, so drawing it at
	 * its natural size puts the pixels where the game has them. Widget bounds are
	 * the wrong size to draw to, since they scale with the interface, which would
	 * stretch the sprite instead of keeping it the same size as the game's.
	 */
	private void drawSprite(Graphics2D graphics, int spriteId, Rectangle bounds)
	{
		BufferedImage image = spriteManager.getSprite(spriteId, 0);
		if (image == null)
		{
			return;
		}
		graphics.drawImage(image, bounds.x, bounds.y, null);
	}

	/** Draws the sprites of a widget and its visible children at their places. */
	private void drawSprites(Graphics2D graphics, Widget widget)
	{
		if (widget == null || widget.isSelfHidden())
		{
			return;
		}

		Rectangle bounds = widget.getBounds();
		if (widget.getSpriteId() >= 0 && bounds != null && bounds.width > 0 && bounds.height > 0)
		{
			drawSprite(graphics, widget.getSpriteId(), bounds);
		}

		for (Widget[] children : new Widget[][]{
			widget.getStaticChildren(), widget.getDynamicChildren(), widget.getNestedChildren()})
		{
			if (children != null)
			{
				for (Widget child : children)
				{
					drawSprites(graphics, child);
				}
			}
		}
	}

	// Switch practice

	/** Starts practice rounds on the inventory's empty slots. */
	private void updatePractice(List<Panel> panels, long now)
	{
		SwitchPractice practice = plugin.practice;
		Panel inventory = panelOf(panels, PanelKind.INVENTORY);
		if (!practice.isActive() || inventory == null)
		{
			return;
		}

		List<Integer> empty = new ArrayList<>();
		for (int i = 0; i < inventory.slots.size(); i++)
		{
			if (inventory.empties.contains(inventory.slots.get(i)))
			{
				empty.add(i);
			}
		}
		practice.roundDelay = config.practiceRoundDelay() * 1_000_000L;
		practice.extraDelay = config.practiceRandomDelay() ? practice.roundDelay : 0;
		practice.timeLimit = (long) (config.practiceTimeLimitSeconds() * 1_000_000_000L);
		practice.ordered = config.practiceOrdered();
		practice.roundsPerSession = config.practiceRounds();

		int rounds = practice.roundCount();
		practice.update(empty, config.practiceTargets(), now, client.getTickCount());
		if (practice.roundCount() != rounds)
		{
			plugin.practiceRoundDone();
		}
		if (practice.isComplete())
		{
			plugin.practiceSessionDone();
		}
	}

	/** Scores a click for switch practice; IGNORED when it is not a practice click. */
	private SwitchPractice.Result practiceClick(Panel panel, Point at, long time)
	{
		SwitchPractice practice = plugin.practice;
		if (panel.kind != PanelKind.INVENTORY || !practice.isActive())
		{
			return SwitchPractice.Result.IGNORED;
		}

		int slot = -1;
		for (int i = 0; i < panel.slots.size(); i++)
		{
			if (panel.slots.get(i).contains(at.getX(), at.getY()))
			{
				slot = i;
				break;
			}
		}

		int rounds = practice.roundCount();
		SwitchPractice.Result result = practice.click(slot, time, client.getTickCount());
		if (result == SwitchPractice.Result.HIT)
		{
			practiceHits.add(new Mark(panel.slots.get(slot), false, time, PanelKind.INVENTORY));
		}
		if (practice.roundCount() != rounds)
		{
			plugin.practiceRoundDone();
			if (practice.isComplete())
			{
				plugin.practiceSessionDone();
			}
		}
		return result;
	}

	/** Practice targets, glowing hits and the status line above the inventory. */
	private void drawPractice(Graphics2D graphics, List<Panel> panels, long now)
	{
		SwitchPractice practice = plugin.practice;
		Panel inventory = panelOf(panels, PanelKind.INVENTORY);
		if (!practice.isActive())
		{
			practiceHits.clear();
			return;
		}
		if (inventory == null)
		{
			return;
		}

		int width = tabs.get(TabSettings.INVENTORY).outlineWidth;
		boolean numbers = config.practiceNumbers() || config.practiceOrdered();
		int next = practice.nextTarget();
		int number = 0;
		graphics.setFont(FontManager.getRunescapeBoldFont());
		for (Map.Entry<Integer, Boolean> target : practice.targets().entrySet())
		{
			number++;
			if (target.getKey() >= inventory.slots.size())
			{
				continue;
			}
			Rectangle bounds = inventory.slots.get(target.getKey());
			Color color = target.getValue() ? config.practiceHitColor() : config.practiceTargetColor();
			graphics.setColor(color);
			graphics.fill(fillShape(bounds));
			// In order mode the next target gets a thicker outline
			int stroke = config.practiceOrdered() && target.getKey() == next ? width + 1 : width;
			graphics.setStroke(new BasicStroke(stroke));
			graphics.setColor(withAlpha(color, 255));
			graphics.draw(outlineShape(bounds, stroke));

			if (numbers)
			{
				String label = String.valueOf(number);
				int textWidth = graphics.getFontMetrics().stringWidth(label);
				int x = (int) bounds.getCenterX() - textWidth / 2;
				int y = (int) bounds.getCenterY() + graphics.getFontMetrics().getAscent() / 2 - 1;
				graphics.setColor(Color.BLACK);
				graphics.drawString(label, x + 1, y + 1);
				graphics.setColor(Color.WHITE);
				graphics.drawString(label, x, y);
			}
		}

		if (config.practiceTimerBar() && practice.timeLimit > 0 && !practice.targets().isEmpty())
		{
			double left = practice.timeLeft(now);
			Rectangle area = inventory.area;
			graphics.setColor(new Color(0, 0, 0, 120));
			graphics.fillRect(area.x, area.y, area.width, 4);
			// Green when the round starts, red as time runs out
			graphics.setColor(Color.getHSBColor((float) (left / 3), 0.9f, 1f));
			graphics.fillRect(area.x, area.y, (int) Math.round(area.width * left), 4);
		}

		Color hit = config.practiceHitColor();
		for (Iterator<Mark> it = practiceHits.iterator(); it.hasNext(); )
		{
			Mark mark = it.next();
			long age = now - mark.time;
			if (age >= PRACTICE_HIT_NANOS)
			{
				it.remove();
				continue;
			}
			double strength = 1 - (double) age / PRACTICE_HIT_NANOS;
			graphics.setColor(withAlpha(hit, (int) (hit.getAlpha() * strength)));
			graphics.fill(fillShape(mark.bounds));
		}

		InventoryHoverHighlightConfig.PracticeStats mode = config.practiceStatsMode();
		practiceLines = mode == InventoryHoverHighlightConfig.PracticeStats.OFF
			? new ArrayList<>() : practiceStatus(inventory, practice, mode);
	}

	/** The practice status for the movable practice overlay; empty when there is nothing to show. */
	private List<String> practiceLines = new ArrayList<>();

	List<String> practiceLines()
	{
		return plugin.practice.isActive() ? practiceLines : new ArrayList<>();
	}

	/** The practice status and stats, one line each. */
	private static List<String> practiceStatus(Panel inventory, SwitchPractice practice,
		InventoryHoverHighlightConfig.PracticeStats mode)
	{
		List<String> lines = new ArrayList<>();
		boolean showLast = mode != InventoryHoverHighlightConfig.PracticeStats.SESSION;
		boolean showSession = mode != InventoryHoverHighlightConfig.PracticeStats.LAST_ROUND;
		SwitchPractice.Summary session = practice.summary();

		if (inventory.empties.isEmpty())
		{
			lines.add("Practice: empty some inventory slots");
		}
		else if (practice.lastRound == null)
		{
			lines.add("Practice: click the lit slots");
		}
		else
		{
			if (showSession && session != null)
			{
				String rounds = practice.roundsPerSession > 0
					? session.rounds + "/" + practice.roundsPerSession
					: String.valueOf(session.rounds);
				lines.add(String.format("Rounds %s, 1-tick %.0f%%, accuracy %.0f%%, timeouts %d",
					rounds, session.oneTickRate, session.accuracy, session.timeouts));
				lines.add(String.format("Avg %.0f ms (best %d), %.1f ticks, react %.0f ms",
					session.switchMillis, session.bestSwitchMillis, session.ticks, session.reactMillis));
			}
			if (showLast)
			{
				SwitchPractice.Round last = practice.lastRound;
				lines.add(last.timedOut
					? String.format("Last: timed out, %d hit, %d miss%s", last.hits, last.misses,
						last.misses == 1 ? "" : "es")
					: String.format("Last: %d ms, %d tick%s, %d miss%s", last.switchMillis, last.ticks,
						last.ticks == 1 ? "" : "s", last.misses, last.misses == 1 ? "" : "es"));
			}
		}

		return lines;
	}

	/** Backfill and icon outline for items the plugin made transparent. */
	private void drawHiddenItems(Graphics2D graphics, List<Panel> panels)
	{
		if (!config.hideItems() || (!config.hiddenItemShowOutline() && !config.hiddenItemShowFill()))
		{
			return;
		}

		Color fill = config.hiddenItemFill();
		Color outline = config.hiddenItemOutline();
		for (Panel panel : panels)
		{
			for (ItemSlot slot : panel.items)
			{
				if (!plugin.isFaded(slot.widget))
				{
					continue;
				}

				if (config.hiddenItemShowFill())
				{
					graphics.setColor(fill);
					graphics.fill(fillShape(slot.bounds));
				}
				if (config.hiddenItemShowOutline() && outline.getAlpha() > 0)
				{
					BufferedImage image = itemManager.getItemOutline(
						slot.widget.getItemId(), slot.widget.getItemQuantity(), outline);
					if (image != null)
					{
						graphics.drawImage(image, slot.bounds.x, slot.bounds.y, null);
					}
				}
			}
		}
	}

	/** Shades food and potions while eating or drinking another one would be ignored. */
	private void drawCooldowns(Graphics2D graphics, List<Panel> panels, long now)
	{
		boolean shade = config.cooldowns();
		boolean outline = config.cooldownOutline();
		if (!shade && !outline)
		{
			return;
		}

		Color color = config.cooldownColor();
		Color outlineColor = config.cooldownOutlineColor();
		int outlineWidth = config.cooldownOutlineWidth();
		for (Panel panel : panels)
		{
			if (panel.kind != PanelKind.INVENTORY)
			{
				continue;
			}

			for (ItemSlot slot : panel.items)
			{
				InventoryHoverHighlightPlugin.Consumable kind = plugin.consumableFor(slot.widget.getItemId());
				int ticks = kind == null ? 0 : plugin.consumeRemaining(kind);
				if (ticks <= 0)
				{
					continue;
				}

				long total = (long) plugin.consumeLength(kind) * InventoryHoverHighlightPlugin.TICK_NANOS;
				long remaining = ticks * (long) InventoryHoverHighlightPlugin.TICK_NANOS - (now - plugin.lastTickNanos);
				double fraction = Math.max(0, Math.min(1, (double) remaining / total));
				Rectangle b = slot.bounds;
				if (outline)
				{
					drawDelayOutline(graphics, b, fraction, outlineColor, outlineWidth);
				}
				if (!shade)
				{
					continue;
				}
				switch (config.cooldownStyle())
				{
					case DRAIN:
					{
						int height = (int) Math.round(b.height * fraction);
						graphics.setColor(color);
						graphics.fill(new Rectangle(b.x, b.y + b.height - height, b.width, height));
						break;
					}
					case DIM:
						graphics.setColor(withAlpha(color, (int) (color.getAlpha() * fraction)));
						graphics.fill(fillShape(b));
						break;
					default:
						graphics.setColor(color);
						graphics.fill(fillShape(b));
						break;
				}
			}
		}
	}

	/** Outlines every area the hover highlight can fill. */
	private void drawGrid(Graphics2D graphics, List<Panel> panels, Panel hovered, Rectangle target)
	{
		InventoryHoverHighlightConfig.GridMode mode = config.grid();
		if (mode == InventoryHoverHighlightConfig.GridMode.OFF)
		{
			return;
		}

		graphics.setColor(config.gridColor());
		graphics.setStroke(new BasicStroke(1));
		for (Panel panel : panels)
		{
			if (mode == InventoryHoverHighlightConfig.GridMode.IN_PANEL && panel != hovered)
			{
				continue;
			}
			for (Rectangle r : config.gridShowEmpty() ? panel.slots : panel.targets)
			{
				// The hover highlight draws over the hovered area
				if (!r.equals(target))
				{
					graphics.draw(config.roundedAreas() ? outlineShape(r, 1)
						: new Rectangle(r.x, r.y, r.width - 1, r.height - 1));
				}
			}
		}
	}

	/** Highlights active prayers using the server-confirmed state. */
	private void drawActivePrayers(Graphics2D graphics)
	{
		if (!config.activePrayers())
		{
			return;
		}

		Widget container = client.getWidget(InterfaceID.Prayerbook.CONTAINER);
		if (container == null || container.isHidden())
		{
			return;
		}

		int width = tabs.get(TabSettings.PRAYERS).outlineWidth;
		for (int i = 0; i < InventoryHoverHighlightPlugin.PRAYER_COUNT; i++)
		{
			Widget widget = client.getWidget(InterfaceID.Prayerbook.PRAYER1 + i);
			if (widget == null || widget.isHidden() || widget.getBounds() == null)
			{
				continue;
			}

			Prayer prayer = plugin.prayerFor(widget);
			if (prayer == null)
			{
				continue;
			}

			boolean confirmed = plugin.isPrayerActive(prayer);
			boolean predicted = config.pendingPrayers() ? plugin.predictedActive(prayer) : confirmed;
			if (!confirmed && !predicted)
			{
				continue;
			}

			Rectangle bounds = widget.getBounds();
			Shape outline = outlineShape(bounds, width);
			if (confirmed == predicted)
			{
				graphics.setColor(config.activePrayerFill());
				graphics.fill(fillShape(bounds));
				graphics.setStroke(new BasicStroke(width));
				graphics.setColor(config.activePrayerColor());
				graphics.draw(outline);
				continue;
			}

			// Pending: the click is sent but the server has not confirmed it yet.
			// Turning on: dashed outline only. Turning off: half fill, dashed outline.
			if (confirmed)
			{
				Color fill = config.activePrayerFill();
				graphics.setColor(withAlpha(fill, fill.getAlpha() / 2));
				graphics.fill(fillShape(bounds));
			}
			graphics.setStroke(new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
				10f, new float[]{3f, 3f}, 0f));
			graphics.setColor(config.activePrayerColor());
			graphics.draw(outline);
		}
	}

	/** Gold highlight on a spell selected for manual casting, like an active prayer. */
	private void drawSelectedSpell(Graphics2D graphics)
	{
		if (!config.selectedSpell() || !client.isWidgetSelected())
		{
			return;
		}

		Widget spell = client.getSelectedWidget();
		if (spell == null || spell.isHidden() || spell.getBounds() == null
			|| spell.getId() >>> 16 != InterfaceID.MagicSpellbook.SPELLLAYER >>> 16)
		{
			return;
		}

		Rectangle bounds = spell.getBounds();
		int width = tabs.get(TabSettings.SPELLS).outlineWidth;
		graphics.setColor(config.activePrayerFill());
		graphics.fill(fillShape(bounds));
		graphics.setStroke(new BasicStroke(width));
		graphics.setColor(config.activePrayerColor());
		graphics.draw(outlineShape(bounds, width));
	}

	private void drawAutocast(Graphics2D graphics, Rectangle hovered)
	{
		if (!config.autocastMarker() || !config.spells() || plugin.autocastComponent == -1)
		{
			return;
		}

		Widget spell = client.getWidget(plugin.autocastComponent);
		if (spell == null || spell.isHidden())
		{
			return;
		}

		Rectangle bounds = spell.getBounds();
		// Hovering the spell shows the hover highlight instead
		if (bounds == null || bounds.equals(hovered))
		{
			return;
		}

		int width = tabs.get(TabSettings.SPELLS).outlineWidth;
		graphics.setColor(config.autocastColor());
		graphics.setStroke(new BasicStroke(width));
		graphics.draw(outlineShape(bounds, width));
	}

	private Shape fillShape(Rectangle b)
	{
		return fillShape(b, shapeTab(b).cornerRadius);
	}

	/** Settings of the tab whose panel holds an area, for its corner radius; the inventory's when none. */
	private TabSettings.Tab shapeTab(Rectangle b)
	{
		for (Panel panel : framePanels)
		{
			if (panel.area.contains(b.getCenterX(), b.getCenterY()))
			{
				return tabs.get(panel.tab);
			}
		}
		return tabs.get(TabSettings.INVENTORY);
	}

	private static Shape fillShape(Rectangle b, int radius)
	{
		int arc = radius * 2;
		return new RoundRectangle2D.Float(b.x, b.y, b.width, b.height, arc, arc);
	}

	private Shape outlineShape(Rectangle b, int width)
	{
		return outlineShape(b, width, shapeTab(b).cornerRadius);
	}

	private static Shape outlineShape(Rectangle b, int width, int radius)
	{
		// Inset by half the stroke so the outline stays inside the clickable area
		int arc = radius * 2;
		float inset = width / 2f;
		return new RoundRectangle2D.Float(b.x + inset, b.y + inset, b.width - width, b.height - width, arc, arc);
	}

	/** Draws the first {@code fraction} of the shape's outline, measured by length. */
	/** The eat and drink delay outline on a slot, with {@code fraction} of the delay left. */
	private void drawDelayOutline(Graphics2D graphics, Rectangle bounds, double fraction, Color color, int width)
	{
		Stroke originalStroke = graphics.getStroke();
		graphics.setStroke(new BasicStroke(width));
		Shape shape = outlineShape(bounds, width);
		switch (config.cooldownOutlineStyle())
		{
			case FADE:
				graphics.setColor(withAlpha(color, (int) (color.getAlpha() * fraction)));
				graphics.draw(shape);
				break;
			case TRACE:
				graphics.setColor(color);
				drawPartial(graphics, shape, fraction);
				break;
			default:
				graphics.setColor(color);
				graphics.draw(shape);
				break;
		}
		graphics.setStroke(originalStroke);
	}

	private static void drawPartial(Graphics2D graphics, Shape shape, double fraction)
	{
		if (fraction <= 0)
		{
			return;
		}
		if (fraction >= 1)
		{
			graphics.draw(shape);
			return;
		}

		List<Line2D> segments = new ArrayList<>();
		double total = 0;
		float[] c = new float[6];
		float startX = 0, startY = 0, x = 0, y = 0;
		for (PathIterator it = shape.getPathIterator(null, 0.5); !it.isDone(); it.next())
		{
			switch (it.currentSegment(c))
			{
				case PathIterator.SEG_MOVETO:
					startX = x = c[0];
					startY = y = c[1];
					break;
				case PathIterator.SEG_LINETO:
					segments.add(new Line2D.Float(x, y, c[0], c[1]));
					x = c[0];
					y = c[1];
					break;
				case PathIterator.SEG_CLOSE:
					segments.add(new Line2D.Float(x, y, startX, startY));
					x = startX;
					y = startY;
					break;
			}
		}
		for (Line2D s : segments)
		{
			total += s.getP1().distance(s.getP2());
		}

		double remaining = total * fraction;
		Path2D path = new Path2D.Float();
		boolean started = false;
		for (Line2D s : segments)
		{
			double length = s.getP1().distance(s.getP2());
			if (length == 0)
			{
				continue;
			}
			if (!started)
			{
				path.moveTo(s.getX1(), s.getY1());
				started = true;
			}
			if (remaining >= length)
			{
				path.lineTo(s.getX2(), s.getY2());
				remaining -= length;
			}
			else
			{
				double t = remaining / length;
				path.lineTo(s.getX1() + (s.getX2() - s.getX1()) * t, s.getY1() + (s.getY2() - s.getY1()) * t);
				break;
			}
		}
		graphics.draw(path);
	}

	private static Color withAlpha(Color c, int alpha)
	{
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
	}
}
