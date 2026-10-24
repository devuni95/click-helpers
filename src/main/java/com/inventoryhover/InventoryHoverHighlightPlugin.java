package com.inventoryhover;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.inject.Provides;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemComposition;
import net.runelite.api.ParamID;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Keybind;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.Text;
import net.runelite.client.util.WildcardMatcher;

@PluginDescriptor(
	name = "Click Helpers",
	description = "Shows clickable areas, click feedback and tick timing for inventory, prayers and spells",
	tags = {"inventory", "hover", "highlight", "click"}
)
public class InventoryHoverHighlightPlugin extends Plugin
{
	// Cache enum mapping the autocast varbit value to the spell's object
	private static final int AUTOCAST_SPELLS_ENUM = 1986;

	static final int TICK_NANOS = 600_000_000;

	static final int PRAYER_COUNT = 30;

	// Sprite the game draws behind an active prayer
	private static final int ACTIVE_PRAYER_GLOW_SPRITE = 4892;

	// Item opacity the game reads as fully transparent
	private static final int TRANSPARENT = 255;

	// Ticks after eating or drinking before the next one of the same kind counts
	static final int CONSUME_DELAY_TICKS = 3;

	// Practice sessions kept for the progress chart
	private static final int PRACTICE_HISTORY_SIZE = 50;
	private static final String PRACTICE_HISTORY_KEY = "practiceHistory";

	private static final Map<String, Prayer> PRAYERS_BY_NAME = new HashMap<>();

	static
	{
		for (Prayer prayer : Prayer.values())
		{
			PRAYERS_BY_NAME.put(prayer.name(), prayer);
		}
	}

	private static final int INVENTORY_GROUP = InterfaceID.Inventory.ITEMS >>> 16;
	private static final int BANKSIDE_GROUP = InterfaceID.Bankside.ITEMS >>> 16;
	private static final int PRAYER_GROUP = InterfaceID.Prayerbook.CONTAINER >>> 16;
	private static final int SPELLBOOK_GROUP = InterfaceID.MagicSpellbook.SPELLLAYER >>> 16;

	private static final int[] ITEM_CONTAINERS = {InterfaceID.Inventory.ITEMS, InterfaceID.Bankside.ITEMS};

	// Ticks after a click in which a refusal message is blamed on it
	private static final int REJECT_WINDOW_TICKS = 2;

	// Lowercase fragments of game messages that refuse an action
	private static final String[] REJECT_PHRASES = {
		"enough space", "inventory space", "free space", "enough free", "enough room",
		"can't", "cannot", "you need", "you don't have", "you do not have", "do not have enough",
		"nothing interesting happens", "unable to"
	};

	/** Eat and drink delays that items share. */
	enum Consumable
	{
		FOOD,
		COMBO_FOOD,
		POTION
	}

	static class Click
	{
		final Rectangle bounds;
		final long time;
		int count;
		/** Whether the game will ignore the click, like eating during the eat delay. */
		boolean blocked;

		Click(Rectangle bounds, long time, int count)
		{
			this.bounds = bounds;
			this.time = time;
			this.count = count;
		}
	}

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private InventoryHoverHighlightConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	/**
	 * Left clicks as {x, y, nanoTime}, queued from the input thread. The overlay
	 * decides on the client thread whether each one landed on a dead spot.
	 */
	final Queue<long[]> mouseClicks = new ConcurrentLinkedQueue<>();

	/** When the last left click happened, for effects that play on click; 0 before any click. */
	volatile long lastClickNanos;

	/** Where the last left click was, for click bursts that stay where you clicked. */
	volatile int lastClickX;
	volatile int lastClickY;

	/** When the game last refused a click, for click result colors. */
	volatile long lastRejectedNanos;

	private final MouseAdapter mouseListener = new MouseAdapter()
	{
		@Override
		public MouseEvent mousePressed(MouseEvent e)
		{
			net.runelite.api.Point mouse = client.getMouseCanvasPosition();
			if (SwingUtilities.isLeftMouseButton(e) && !client.isMenuOpen() && mouse != null)
			{
				long now = System.nanoTime();
				mouseClicks.add(new long[]{mouse.getX(), mouse.getY(), now});
				lastClickX = mouse.getX();
				lastClickY = mouse.getY();
				lastClickNanos = now;
			}
			return e;
		}
	};

	/** Whether the hotkey has item hiding switched on. */
	private volatile boolean itemsHidden = true;

	private final HotkeyListener hideItemsHotkey = new HotkeyListener(() -> config.hideItemsHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			itemsHidden = !itemsHidden;
		}
	};

	@Inject
	private InventoryHoverHighlightOverlay overlay;

	private final HotkeyListener clickChartHotkey = new HotkeyListener(() -> config.clickChartHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			configManager.setConfiguration(InventoryHoverHighlightConfig.GROUP, "clickChart", !config.clickChart());
		}
	};

	@Inject
	private ClickChartOverlay chartOverlay;

	@Inject
	private SettingsSharing settingsSharing;

	@Inject
	private CommunityLoadouts communityLoadouts;

	@Inject
	private PracticeStatusOverlay practiceStatusOverlay;

	@Inject
	private TabSettings tabSettings;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ColorPickerManager colorPickerManager;

	@Inject
	private Gson gson;

	private ClickHelpersPanel panel;
	private NavigationButton navButton;

	final SwitchPractice practice = new SwitchPractice();

	/** Whether the game's prayer glows were shown again for the empty inventory pause. */
	private boolean prayerGlowsRestored;

	/** Finished practice sessions, oldest first. Read on the Swing thread, so replaced, never changed. */
	private volatile List<SwitchPractice.Summary> practiceHistory = List.of();

	private final HotkeyListener practiceHotkey = new HotkeyListener(() -> config.practiceHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			togglePractice();
		}
	};

	final ClickStats stats = new ClickStats();

	/** Clicks to flash, oldest first. */
	final List<Click> flashes = new ArrayList<>();

	/** Clicks the game refused, as {x, y, nanoTime}, drawn as yellow rings. */
	final List<long[]> rejectedClicks = new ArrayList<>();

	/** Last tracked click as {x, y, tickCount}, or null. */
	private int[] lastClick;

	/** Clicks sent since the last game tick, keyed by widget. */
	final Map<Long, Click> queuedClicks = new LinkedHashMap<>();

	long lastTickNanos;

	/** Prayer toggles clicked but not yet confirmed by the server. */
	private final Map<Prayer, Pending> pendingPrayers = new EnumMap<>(Prayer.class);

	private static class Pending
	{
		final boolean expected;
		int ticks;

		Pending(boolean expected)
		{
			this.expected = expected;
		}
	}

	/** Component id of the spell set to autocast, or -1. */
	int autocastComponent = -1;

	/** Each running eat or drink delay as {start tick, end tick}. */
	private final Map<Consumable, int[]> consumeDelays = new EnumMap<>(Consumable.class);

	/** Consumable kind per item id, null when the item is neither food nor potion. */
	private final Map<Integer, Consumable> consumableCache = new HashMap<>();

	/** Whether each item id is hidden by the item hiding. */
	private final Map<Integer, Boolean> hiddenCache = new HashMap<>();

	/** Item slots made transparent, keyed by slot. */
	private final Map<Long, Widget> fadedSlots = new HashMap<>();

	private Set<String> fightActions = Set.of();
	private List<String> alwaysShow = List.of();

	/** Prayer name patterns that get the mid-tick cue when not hovered. */
	List<String> cuePrayers = List.of();


	@Provides
	InventoryHoverHighlightConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(InventoryHoverHighlightConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		overlay.fillOverlays().forEach(overlayManager::add);
		overlayManager.add(practiceStatusOverlay);
		mouseManager.registerMouseListener(mouseListener);
		keyManager.registerKeyListener(hideItemsHotkey);
		lastTickNanos = System.nanoTime();
		parseItemLists();
		overlayManager.add(chartOverlay);
		keyManager.registerKeyListener(clickChartHotkey);
		keyManager.registerKeyListener(practiceHotkey);
		clientThread.invoke(this::updateAutocast);

		migrateToTabs();
		practiceHistory = loadPracticeHistory();
		panel = new ClickHelpersPanel(this, config, configManager, colorPickerManager);
		navButton = NavigationButton.builder()
			.tooltip("Click Helpers")
			.icon(panelIcon())
			.priority(10)
			.panel(panel)
			.build();
		if (config.sidePanel())
		{
			clientToolbar.addNavigation(navButton);
		}
		if (config.practiceMode())
		{
			clientThread.invoke(() -> practice.start(System.nanoTime()));
		}
		refreshPanel();
	}

	@Override
	protected void shutDown()
	{
		clientThread.invoke(() ->
		{
			restorePrayerGlows();
			restoreItems();
		});
		overlayManager.remove(overlay);
		overlay.fillOverlays().forEach(overlayManager::remove);
		overlayManager.remove(practiceStatusOverlay);
		mouseManager.unregisterMouseListener(mouseListener);
		keyManager.unregisterKeyListener(hideItemsHotkey);
		overlayManager.remove(chartOverlay);
		keyManager.unregisterKeyListener(clickChartHotkey);
		keyManager.unregisterKeyListener(practiceHotkey);
		clientToolbar.removeNavigation(navButton);
		savePracticeSession();
		stats.reset();
		mouseClicks.clear();
		flashes.clear();
		queuedClicks.clear();
		rejectedClicks.clear();
		lastClick = null;
		pendingPrayers.clear();
		consumeDelays.clear();
		consumableCache.clear();
		hiddenCache.clear();
		tabSettings.invalidate();
		autocastComponent = -1;
	}

	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		updateItemHiding();

		if (!config.activePrayers())
		{
			return;
		}
		if (pausedForEmptyInventory())
		{
			// The plugin draws no prayer gold while paused, so give the game its own glow back
			if (!prayerGlowsRestored)
			{
				restorePrayerGlows();
				prayerGlowsRestored = true;
			}
			return;
		}
		prayerGlowsRestored = false;

		// The game re-shows the glow whenever its prayer script runs, so hide it every frame
		for (int i = 0; i < PRAYER_COUNT; i++)
		{
			Widget glow = findPrayerGlow(client.getWidget(InterfaceID.Prayerbook.PRAYER1 + i));
			if (glow != null && !glow.isSelfHidden())
			{
				glow.setHidden(true);
			}
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals(InventoryHoverHighlightConfig.GROUP))
		{
			return;
		}

		tabSettings.invalidate();
		if (panel != null)
		{
			panel.settingChanged(event.getKey());
		}
		switch (event.getKey())
		{
			case "activePrayers":
				if (!config.activePrayers())
				{
					clientThread.invoke(this::restorePrayerGlows);
				}
				break;
			case "fightActions":
			case "alwaysShowItems":
			case "cuePrayers":
				clientThread.invoke(() ->
				{
					parseItemLists();
					hiddenCache.clear();
				});
				break;
			case "sidePanel":
				if (config.sidePanel())
				{
					clientToolbar.addNavigation(navButton);
				}
				else
				{
					clientToolbar.removeNavigation(navButton);
				}
				break;
			// Action toggles: run once, then switch themselves back off
			case "copySettings":
				if (config.copySettings())
				{
					configManager.setConfiguration(InventoryHoverHighlightConfig.GROUP, "copySettings", false);
					notify(settingsSharing.copyToClipboard());
				}
				break;
			case "pasteSettings":
				if (config.pasteSettings())
				{
					configManager.setConfiguration(InventoryHoverHighlightConfig.GROUP, "pasteSettings", false);
					notify(settingsSharing.pasteFromClipboard());
				}
				break;
			case "syncTabSettings":
				if (config.syncTabSettings())
				{
					settingsSharing.syncAllTabs();
				}
				break;
			default:
				if (config.syncTabSettings())
				{
					settingsSharing.syncTabSetting(event.getKey(), event.getNewValue());
				}
				break;
			case "practiceMode":
				clientThread.invoke(() ->
				{
					if (config.practiceMode() && !practice.isActive())
					{
						practice.start(System.nanoTime());
					}
					else if (!config.practiceMode() && practice.isActive())
					{
						savePracticeSession();
					}
					refreshPanel();
				});
				break;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		lastTickNanos = System.nanoTime();
		stats.setTick(client.getTickCount());
		queuedClicks.clear();

		// Drop pending toggles once the server agrees, or after a couple of ticks
		pendingPrayers.entrySet().removeIf(e ->
			isPrayerActive(e.getKey()) == e.getValue().expected || ++e.getValue().ticks > 2);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			updateAutocast();
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
		{
			consumeDelays.clear();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarbitId() == VarbitID.AUTOCAST_SPELL)
		{
			updateAutocast();
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		Widget widget = event.getWidget();
		if (widget == null || !isTracked(widget))
		{
			return;
		}

		Rectangle bounds = widget.getBounds();
		if (bounds == null)
		{
			return;
		}

		long now = System.nanoTime();
		flashes.add(new Click(bounds, now, 1));
		stats.add(ClickStats.Kind.USEFUL);

		net.runelite.api.Point mouse = client.getMouseCanvasPosition();
		lastClick = mouse == null
			? new int[]{(int) bounds.getCenterX(), (int) bounds.getCenterY(), client.getTickCount()}
			: new int[]{mouse.getX(), mouse.getY(), client.getTickCount()};

		boolean blocked = false;
		int group = widget.getId() >>> 16;
		if (group == PRAYER_GROUP)
		{
			Prayer prayer = prayerFor(widget);
			if (prayer != null)
			{
				pendingPrayers.put(prayer, new Pending(!predictedActive(prayer)));
			}
		}
		else if (group == INVENTORY_GROUP)
		{
			blocked = startConsumeDelay(Text.removeTags(event.getMenuOption()), event.getItemId());
			if (blocked)
			{
				stats.add(ClickStats.Kind.REJECTED);
				lastRejectedNanos = System.nanoTime();
			}
		}

		long key = ((long) widget.getId() << 16) | (widget.getIndex() & 0xffff);
		Click queued = queuedClicks.get(key);
		if (queued != null)
		{
			queued.count++;
			stats.add(ClickStats.Kind.DOUBLE);
		}
		else
		{
			queued = new Click(bounds, now, 1);
			queuedClicks.put(key, queued);
		}
		queued.blocked |= blocked;
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.ENGINE)
		{
			return;
		}

		String message = Text.removeTags(event.getMessage()).toLowerCase().replace('’', '\'');
		if (lastClick == null)
		{
			return;
		}

		if (client.getTickCount() - lastClick[2] > REJECT_WINDOW_TICKS)
		{
			lastClick = null;
			return;
		}

		for (String phrase : REJECT_PHRASES)
		{
			if (message.contains(phrase))
			{
				stats.add(ClickStats.Kind.REJECTED);
				lastRejectedNanos = System.nanoTime();
				if (config.rejectedClickMarker())
				{
					rejectedClicks.add(new long[]{lastClick[0], lastClick[1], System.nanoTime()});
				}
				lastClick = null;
				return;
			}
		}
	}

	private boolean isTracked(Widget widget)
	{
		int group = widget.getId() >>> 16;
		return group == INVENTORY_GROUP
			|| (group == BANKSIDE_GROUP && config.bankInventory())
			|| (group == PRAYER_GROUP && config.prayers())
			|| (group == SPELLBOOK_GROUP && config.spells());
	}

	private void notify(String message)
	{
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(message)
			.build());
	}

	/**
	 * Copies the old general hover, spotlight and miss dot settings into every
	 * tab once, for tabs that did not use their own, so nothing changes after
	 * the general settings were removed.
	 */
	private void migrateToTabs()
	{
		String group = InventoryHoverHighlightConfig.GROUP;
		if (configManager.getConfiguration(group, "tabsMigrated") != null)
		{
			return;
		}

		Map<String, String[]> moved = Map.of(
		"OwnHover", new String[]{"style", "outlineColor", "fillColor", "outlineWidth", "cornerRadius", "hoverFade", "hoverDelay", "hideWhenMenuOpen", "discoMode", "discoInterval", "keepIconColors"},
		"OwnSpotlight", new String[]{"spotlight", "spotlightRadius", "spotlightDim", "spotlightGlow", "spotlightGlowSize", "spotlightAlways", "spotlightDeadOnly", "spotlightFalloff", "spotlightFalloffDistance"},
		"OwnMissDots", new String[]{"missDots", "missDotColor", "missDotShape", "missDotSize", "missDotDuration"});
		for (Map.Entry<String, String[]> entry : moved.entrySet())
		{
			for (String prefix : TabSettings.PREFIXES)
			{
				String own = configManager.getConfiguration(group, prefix + entry.getKey());
				configManager.unsetConfiguration(group, prefix + entry.getKey());
				if (Boolean.parseBoolean(own))
				{
					continue;
				}
				for (String key : entry.getValue())
				{
					String value = configManager.getConfiguration(group, key);
					if (value != null)
					{
						configManager.setConfiguration(group, prefix + Character.toUpperCase(key.charAt(0)) + key.substring(1), value);
					}
				}
			}
			for (String key : entry.getValue())
			{
				configManager.unsetConfiguration(group, key);
			}
		}
		configManager.setConfiguration(group, "tabsMigrated", true);
	}

	// Side panel

	/** Saves, loads and deletes settings loadouts for the side panel. */
	SettingsSharing loadouts()
	{
		return settingsSharing;
	}

	/** Publishes, lists and deletes community loadouts for the side panel. */
	CommunityLoadouts community()
	{
		return communityLoadouts;
	}

	/** Why the Community tab cannot be used right now, or null when it can. */
	String communityProblem(boolean publishing)
	{
		if (!CommunityLoadouts.isSetUp())
		{
			return "Community loadouts are not available in this version.";
		}
		if (!config.communityLoadouts())
		{
			return "Community loadouts is off. Turn it on in RuneLite's Click Helpers plugin settings (the gear icon), under Import/export settings.";
		}
		if (publishing && client.getGameState() != GameState.LOGGED_IN)
		{
			return "Log in to the game to publish a loadout.";
		}
		return null;
	}

	/** Reads the logged-in player's game name on the client thread; calls back on the Swing thread, with null when logged out. */
	void playerName(Consumer<String> done)
	{
		clientThread.invokeLater(() ->
		{
			Player player = client.getLocalPlayer();
			String name = player == null ? null : player.getName();
			SwingUtilities.invokeLater(() -> done.accept(name));
		});
	}

	/** Exports the settings to the clipboard; returns a message for the player. */
	String exportSettings()
	{
		String message = settingsSharing.copyToClipboard();
		notify(message);
		return message;
	}

	/** Imports settings from the clipboard; returns a message for the player. */
	String importSettings()
	{
		String message = settingsSharing.pasteFromClipboard();
		notify(message);
		return message;
	}

	/** Starts or stops switch practice. */
	void togglePractice()
	{
		configManager.setConfiguration(InventoryHoverHighlightConfig.GROUP, "practiceMode", !config.practiceMode());
	}

	void clearPracticeHistory()
	{
		practiceHistory = List.of();
		configManager.unsetConfiguration(InventoryHoverHighlightConfig.GROUP, PRACTICE_HISTORY_KEY);
		refreshPanel();
	}

	/** Called by the overlay when a practice round is done. */
	void practiceRoundDone()
	{
		refreshPanel();
	}

	/** Called by the overlay when the set number of rounds is done; ends and saves the session. */
	void practiceSessionDone()
	{
		SwitchPractice.Summary summary = practice.summary();
		if (summary != null)
		{
			notify(String.format("Switch practice done: %d rounds, %.0f ms average (best %d ms),"
					+ " %.0f%% in 1 tick, %.0f%% accuracy, %d timeouts.",
				summary.rounds, summary.switchMillis, summary.bestSwitchMillis, summary.oneTickRate,
				summary.accuracy, summary.timeouts));
		}
		savePracticeSession();
		configManager.setConfiguration(InventoryHoverHighlightConfig.GROUP, "practiceMode", false);
		refreshPanel();
	}

	/** Ends the practice session and adds it to the history when it has a finished round. */
	private void savePracticeSession()
	{
		if (!practice.isActive())
		{
			return;
		}

		SwitchPractice.Summary summary = practice.stop();
		if (summary == null)
		{
			return;
		}

		List<SwitchPractice.Summary> history = new ArrayList<>(practiceHistory);
		history.add(summary);
		while (history.size() > PRACTICE_HISTORY_SIZE)
		{
			history.remove(0);
		}
		practiceHistory = Collections.unmodifiableList(history);
		configManager.setConfiguration(InventoryHoverHighlightConfig.GROUP, PRACTICE_HISTORY_KEY, gson.toJson(history));
	}

	private List<SwitchPractice.Summary> loadPracticeHistory()
	{
		String json = configManager.getConfiguration(InventoryHoverHighlightConfig.GROUP, PRACTICE_HISTORY_KEY);
		if (json == null || json.isEmpty())
		{
			return List.of();
		}
		try
		{
			SwitchPractice.Summary[] sessions = gson.fromJson(json, SwitchPractice.Summary[].class);
			return sessions == null ? List.of() : List.of(sessions);
		}
		catch (JsonParseException e)
		{
			return List.of();
		}
	}

	private void refreshPanel()
	{
		ClickHelpersPanel target = panel;
		if (target == null)
		{
			return;
		}
		boolean practicing = practice.isActive();
		SwitchPractice.Summary current = practice.summary();
		List<SwitchPractice.Summary> history = practiceHistory;
		SwingUtilities.invokeLater(() -> target.update(practicing, current, history));
	}

	/** Side panel icon: a cursor dot inside a slot outline. */
	private static BufferedImage panelIcon()
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(new Color(98, 189, 125));
		g.setStroke(new BasicStroke(1.5f));
		g.drawRoundRect(1, 2, 13, 11, 4, 4);
		g.setColor(Color.WHITE);
		g.fillOval(6, 6, 4, 4);
		g.dispose();
		return image;
	}

	// Eat and drink delays

	/** Starts the eat or drink delay; returns true when the click falls in a running delay. */
	private boolean startConsumeDelay(String option, int itemId)
	{
		Consumable kind = null;
		if ("Eat".equalsIgnoreCase(option))
		{
			kind = isComboFood(itemId) ? Consumable.COMBO_FOOD : Consumable.FOOD;
		}
		else if ("Drink".equalsIgnoreCase(option))
		{
			kind = Consumable.POTION;
		}

		if (kind == null)
		{
			return false;
		}
		// A click during the delay is ignored by the game, so it does not restart the delay
		if (consumeRemaining(kind) > 0)
		{
			return true;
		}

		// Food, then a potion, then a karambwan can all go in one tick, but not in
		// another order: a potion blocks food for the rest of its tick, and a
		// karambwan blocks food for its whole delay and potions for its tick
		int tick = client.getTickCount();
		switch (kind)
		{
			case FOOD:
				extendDelay(Consumable.FOOD, tick, foodDelayTicks(client.getItemDefinition(itemId).getName()));
				break;
			case POTION:
				extendDelay(Consumable.POTION, tick, CONSUME_DELAY_TICKS);
				extendDelay(Consumable.FOOD, tick, 1);
				break;
			case COMBO_FOOD:
				extendDelay(Consumable.COMBO_FOOD, tick, CONSUME_DELAY_TICKS);
				extendDelay(Consumable.FOOD, tick, CONSUME_DELAY_TICKS);
				extendDelay(Consumable.POTION, tick, 1);
				break;
		}
		return false;
	}

	/** Makes a delay last at least {@code ticks} from {@code tick}. */
	private void extendDelay(Consumable kind, int tick, int ticks)
	{
		int[] delay = consumeDelays.get(kind);
		if (delay == null || delay[1] < tick + ticks)
		{
			consumeDelays.put(kind, new int[]{tick, tick + ticks});
		}
	}

	/**
	 * Eat delay of a food by name. Most food is 3 ticks; pies, pizzas, cakes and
	 * giant crab meat are faster.
	 */
	static int foodDelayTicks(String name)
	{
		String n = name.toLowerCase();
		boolean half = n.startsWith("half ") || n.startsWith("1/2 ");
		if (n.contains("giant crab meat"))
		{
			return 2;
		}
		if (n.endsWith(" pizza"))
		{
			return half ? 2 : 1;
		}
		if (n.endsWith(" pie"))
		{
			// Apple, meat and redberry pies take 2 ticks for the second half; members' pies 1
			boolean slowHalf = n.endsWith("apple pie") || n.endsWith("meat pie") || n.endsWith("redberry pie");
			return half && slowHalf ? 2 : 1;
		}
		if (n.equals("cake") || n.equals("2/3 cake") || n.equals("chocolate cake") || n.equals("2/3 chocolate cake"))
		{
			return 2;
		}
		return CONSUME_DELAY_TICKS;
	}

	/** Ticks left on a delay, counting the current tick as a whole one, or 0. */
	int consumeRemaining(Consumable kind)
	{
		int[] delay = consumeDelays.get(kind);
		return delay == null ? 0 : Math.max(0, delay[1] - client.getTickCount());
	}

	/** Full length in ticks of the running delay, for drawing how much is left. */
	int consumeLength(Consumable kind)
	{
		int[] delay = consumeDelays.get(kind);
		return delay == null ? CONSUME_DELAY_TICKS : Math.max(1, delay[1] - delay[0]);
	}

	/** The delay an item shares, from its inventory actions, or null. */
	Consumable consumableFor(int itemId)
	{
		return consumableCache.computeIfAbsent(itemId, id ->
		{
			ItemComposition item = client.getItemDefinition(id);
			for (String action : item.getInventoryActions())
			{
				if ("Eat".equalsIgnoreCase(action))
				{
					return isComboFood(id) ? Consumable.COMBO_FOOD : Consumable.FOOD;
				}
				if ("Drink".equalsIgnoreCase(action))
				{
					return Consumable.POTION;
				}
			}
			return null;
		});
	}

	private boolean isComboFood(int itemId)
	{
		return client.getItemDefinition(itemId).getName().toLowerCase().contains("karambwan");
	}

	// Item hiding

	private void parseItemLists()
	{
		fightActions = Text.fromCSV(config.fightActions()).stream()
			.map(String::toLowerCase)
			.collect(Collectors.toSet());
		alwaysShow = Text.fromCSV(config.alwaysShowItems());
		cuePrayers = Text.fromCSV(config.cuePrayers());
	}

	private boolean hidingActive()
	{
		return config.hideItems() && (Keybind.NOT_SET.equals(config.hideItemsHotkey()) || itemsHidden);
	}

	/** Whether an item has no fight action, so it can be hidden. */
	private boolean isNonFightItem(int itemId)
	{
		return hiddenCache.computeIfAbsent(itemId, id ->
		{
			ItemComposition item = client.getItemDefinition(id);
			String name = item.getName();
			for (String pattern : alwaysShow)
			{
				if (WildcardMatcher.matches(pattern, name))
				{
					return false;
				}
			}
			// A noted item can only be used, never eaten, drunk or worn
			if (item.getNote() != -1)
			{
				return true;
			}
			return Arrays.stream(item.getInventoryActions())
				.noneMatch(a -> a != null && fightActions.contains(a.toLowerCase()));
		});
	}

	/** Makes non-fight items transparent, or restores them when hiding is off. */
	private void updateItemHiding()
	{
		// Restore straight from the stored widgets, since a closed panel (like the
		// bank) is no longer reachable from its container
		if (!hidingActive())
		{
			if (!fadedSlots.isEmpty())
			{
				restoreItems();
			}
			return;
		}

		for (int containerId : ITEM_CONTAINERS)
		{
			Widget container = client.getWidget(containerId);
			Widget[] slots = container == null ? null : container.getDynamicChildren();
			if (slots == null)
			{
				continue;
			}

			for (Widget slot : slots)
			{
				if (slot == null)
				{
					continue;
				}

				long key = slotKey(slot);
				int itemId = slot.getItemId();
				boolean hide = !slot.isSelfHidden() && itemId > 0 && slot.getItemQuantity() > 0
					&& isNonFightItem(itemId);
				if (hide)
				{
					if (slot.getOpacity() != TRANSPARENT)
					{
						slot.setOpacity(TRANSPARENT);
					}
					fadedSlots.put(key, slot);
				}
				else if (fadedSlots.remove(key) != null)
				{
					slot.setOpacity(0);
				}
			}
		}
	}

	private void restoreItems()
	{
		for (Widget slot : fadedSlots.values())
		{
			slot.setOpacity(0);
		}
		fadedSlots.clear();
	}

	/** Whether the item in a slot is hidden by the item hiding. */
	boolean isFaded(Widget slot)
	{
		return fadedSlots.containsKey(slotKey(slot));
	}

	private static long slotKey(Widget slot)
	{
		return ((long) slot.getId() << 16) | (slot.getIndex() & 0xffff);
	}

	// Prayers

	/** Whether to draw nothing because the inventory is empty and that setting is on; never during switch practice. */
	boolean pausedForEmptyInventory()
	{
		if (!config.hideWhenEmpty() || practice.isActive())
		{
			return false;
		}
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory == null)
		{
			return true;
		}
		for (Item item : inventory.getItems())
		{
			if (item.getId() > 0 && item.getQuantity() > 0)
			{
				return false;
			}
		}
		return true;
	}

	/** Shows the game's glow again on the prayers that are active. */
	private void restorePrayerGlows()
	{
		for (int i = 0; i < PRAYER_COUNT; i++)
		{
			Widget widget = client.getWidget(InterfaceID.Prayerbook.PRAYER1 + i);
			Widget glow = findPrayerGlow(widget);
			if (glow != null)
			{
				Prayer prayer = prayerFor(widget);
				glow.setHidden(prayer == null || !isPrayerActive(prayer));
			}
		}
	}

	/** The child of a prayer widget that draws the game's active glow, or null. */
	private static Widget findPrayerGlow(Widget widget)
	{
		if (widget == null)
		{
			return null;
		}
		if (widget.getSpriteId() == ACTIVE_PRAYER_GLOW_SPRITE)
		{
			return widget;
		}

		for (Widget[] children : new Widget[][]{
			widget.getStaticChildren(), widget.getDynamicChildren(), widget.getNestedChildren()})
		{
			if (children == null)
			{
				continue;
			}
			for (Widget child : children)
			{
				Widget glow = findPrayerGlow(child);
				if (glow != null)
				{
					return glow;
				}
			}
		}
		return null;
	}

	/** The prayer a prayer tab widget belongs to, matched by its name, or null. */
	Prayer prayerFor(Widget widget)
	{
		if (widget == null || widget.getName() == null)
		{
			return null;
		}
		String key = Text.removeTags(widget.getName()).toUpperCase().replaceAll("[^A-Z0-9]+", "_");
		return PRAYERS_BY_NAME.get(key);
	}

	/** Prayer state including clicks the server has not confirmed yet. */
	boolean predictedActive(Prayer prayer)
	{
		Pending pending = pendingPrayers.get(prayer);
		return pending != null ? pending.expected : isPrayerActive(prayer);
	}

	/** Server-confirmed prayer state, unaffected by the client's predicted toggle. */
	boolean isPrayerActive(Prayer prayer)
	{
		return client.getServerVarbitValue(prayer.getVarbit()) == 1;
	}

	private void updateAutocast()
	{
		autocastComponent = -1;
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		int index = client.getVarbitValue(VarbitID.AUTOCAST_SPELL);
		if (index <= 0)
		{
			return;
		}

		EnumComposition spells = client.getEnum(AUTOCAST_SPELLS_ENUM);
		int spellObj = spells == null ? -1 : spells.getIntValue(index);
		if (spellObj <= 0)
		{
			return;
		}

		autocastComponent = client.getItemDefinition(spellObj).getIntValue(ParamID.SPELL_BUTTON);
	}
}
