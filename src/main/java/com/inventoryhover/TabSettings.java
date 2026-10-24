package com.inventoryhover;

import java.awt.Color;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

/**
 * Hover, slot background, spotlight and miss dot settings of one tab, read by
 * the tab's key prefix. Resolved values are cached until a setting changes.
 */
@Singleton
class TabSettings
{
	static final String INVENTORY = "inv";
	static final String PRAYERS = "prayer";
	static final String SPELLS = "spell";

	static final List<String> PREFIXES = List.of(INVENTORY, PRAYERS, SPELLS);

	/** Config sections that hold tab settings. */
	static final Set<String> SECTIONS = Set.of(InventoryHoverHighlightConfig.invHoverSection, InventoryHoverHighlightConfig.invSpotlightSection, InventoryHoverHighlightConfig.invMissDotsSection, InventoryHoverHighlightConfig.prayerHoverSection, InventoryHoverHighlightConfig.prayerSpotlightSection, InventoryHoverHighlightConfig.prayerMissDotsSection, InventoryHoverHighlightConfig.spellHoverSection, InventoryHoverHighlightConfig.spellSpotlightSection, InventoryHoverHighlightConfig.spellMissDotsSection);

	static class Tab
	{
		InventoryHoverHighlightConfig.HighlightStyle style;
		Color outlineColor;
		Color fillColor;
		int outlineWidth;
		int cornerRadius;
		int hoverFade;
		int hoverDelay;
		boolean hideWhenMenuOpen;
		boolean discoMode;
		int discoInterval;
		boolean keepIconColors;
		/** Slot background color, or null when off. */
		Color slotBackground;
		boolean spotlight;
		int spotlightRadius;
		Color spotlightDim;
		Color spotlightGlow;
		int spotlightGlowSize;
		InventoryHoverHighlightConfig.SpotlightShape spotlightShape;
		InventoryHoverHighlightConfig.SpotlightType spotlightType;
		InventoryHoverHighlightConfig.SpotlightMotion spotlightMotion;
		InventoryHoverHighlightConfig.SpotlightEffect spotlightEffect;
		/** Where the effect shows; stored under the key "SpotlightEffectColor". */
		InventoryHoverHighlightConfig.EffectArea spotlightEffectArea;
		boolean spotlightEffectReplace;
		boolean spotlightEffectOnClick;
		/** The custom effect color; null when Custom effect color is off or the color is fully see-through. */
		Color spotlightEffectOverride;
		boolean spotlightClickResult;
		Color spotlightClickSuccessColor;
		Color spotlightClickMissColor;
		Color spotlightClickRefusedColor;
		int spotlightEffectSpeed;
		int spotlightEffectLength;
		int spotlightEffectSize;
		/** How many particles effects draw, in percent of normal. */
		int spotlightEffectAmount;
		boolean spotlightEffectDetach;
		/** How far detached effects reach, in percent of the band they would fill on the mouse. */
		int spotlightEffectReach;
		/** How far apart detached effects' particles are, in percent of the normal gap. */
		int spotlightEffectSpread;
		/** Inventory only: click bursts outside every panel too. */
		boolean spotlightClickAnywhere;
		/** Inventory only: clicks in the inventory play the click anywhere effect too, in place of their own. */
		boolean spotlightClickAnywherePanels;
		int spotlightClickAnywhereDelay;
		boolean spotlightAlways;
		boolean spotlightDeadOnly;
		boolean spotlightFalloff;
		int spotlightFalloffDistance;
		int spotlightFalloffEnd;
		Color spotlightFalloffColor;
		int spotlightFalloffColorEnd;
		/** Null when fully see-through, meaning fade out to nothing. */
		Color spotlightFadeEndColor;
		boolean missDots;
		Color missDotColor;
		InventoryHoverHighlightConfig.MissDotShape missDotShape;
		boolean missDotHollow;
		int missDotSize;
		int missDotDuration;
	}

	private final ConfigManager configManager;
	private final Map<String, Tab> cache = new ConcurrentHashMap<>();

	@Inject
	TabSettings(ConfigManager configManager)
	{
		this.configManager = configManager;
	}

	Tab get(String prefix)
	{
		return cache.computeIfAbsent(prefix, this::resolve);
	}

	void invalidate()
	{
		cache.clear();
	}

	private Tab resolve(String p)
	{
		Tab tab = new Tab();
		tab.style = read(p + "Style", InventoryHoverHighlightConfig.HighlightStyle.class, InventoryHoverHighlightConfig.HighlightStyle.BOTH);
		tab.outlineColor = read(p + "OutlineColor", Color.class, new Color(255, 255, 255, 120));
		tab.fillColor = read(p + "FillColor", Color.class, new Color(98, 189, 125, 60));
		tab.outlineWidth = read(p + "OutlineWidth", int.class, 1);
		tab.cornerRadius = read(p + "CornerRadius", int.class, 4);
		tab.hoverFade = read(p + "HoverFade", int.class, 400);
		tab.hoverDelay = read(p + "HoverDelay", int.class, 0);
		tab.hideWhenMenuOpen = read(p + "HideWhenMenuOpen", boolean.class, false);
		tab.discoMode = read(p + "DiscoMode", boolean.class, false);
		tab.discoInterval = read(p + "DiscoInterval", int.class, 0);
		tab.keepIconColors = read(p + "KeepIconColors", boolean.class, false);
		tab.slotBackground = read(p + "SlotBackground", boolean.class, false)
			? read(p + "SlotBackgroundColor", Color.class, new Color(60, 60, 70, 90))
			: null;
		tab.spotlight = read(p + "Spotlight", boolean.class, false);
		tab.spotlightRadius = read(p + "SpotlightRadius", int.class, 90);
		tab.spotlightDim = read(p + "SpotlightDim", Color.class, new Color(0, 0, 0, 110));
		tab.spotlightGlow = read(p + "SpotlightGlow", Color.class, new Color(255, 255, 255, 45));
		tab.spotlightGlowSize = read(p + "SpotlightGlowSize", int.class, 30);
		tab.spotlightShape = read(p + "SpotlightShape", InventoryHoverHighlightConfig.SpotlightShape.class,
			InventoryHoverHighlightConfig.SpotlightShape.SMOOTH);
		tab.spotlightType = read(p + "SpotlightType", InventoryHoverHighlightConfig.SpotlightType.class,
			InventoryHoverHighlightConfig.SpotlightType.CIRCLE);
		tab.spotlightMotion = read(p + "SpotlightMotion", InventoryHoverHighlightConfig.SpotlightMotion.class,
			InventoryHoverHighlightConfig.SpotlightMotion.NONE);
		tab.spotlightEffect = read(p + "SpotlightEffect", InventoryHoverHighlightConfig.SpotlightEffect.class,
			InventoryHoverHighlightConfig.SpotlightEffect.NONE);
		tab.spotlightEffectArea = read(p + "SpotlightEffectColor", InventoryHoverHighlightConfig.EffectArea.class,
			InventoryHoverHighlightConfig.EffectArea.OVERLAY);
		tab.spotlightEffectReplace = read(p + "SpotlightEffectReplace", boolean.class, false);
		tab.spotlightEffectOnClick = read(p + "SpotlightEffectOnClick", boolean.class, false);
		tab.spotlightEffectOverride = read(p + "SpotlightEffectCustom", boolean.class, false)
			? visible(read(p + "SpotlightEffectOverride", Color.class, new Color(255, 255, 255, 200)))
			: null;
		tab.spotlightClickResult = read(p + "SpotlightClickResult", boolean.class, false);
		tab.spotlightClickSuccessColor = read(p + "SpotlightClickSuccessColor", Color.class, new Color(0, 255, 0, 200));
		tab.spotlightClickMissColor = read(p + "SpotlightClickMissColor", Color.class, new Color(255, 60, 60, 200));
		tab.spotlightClickRefusedColor = read(p + "SpotlightClickRefusedColor", Color.class, new Color(255, 220, 0, 200));
		tab.spotlightEffectSpeed = read(p + "SpotlightEffectSpeed", int.class, 100);
		tab.spotlightEffectLength = read(p + "SpotlightEffectLength", int.class, 1200);
		tab.spotlightEffectSize = read(p + "SpotlightEffectSize", int.class, 100);
		tab.spotlightEffectAmount = read(p + "SpotlightEffectAmount", int.class, 100);
		tab.spotlightEffectDetach = read(p + "SpotlightEffectDetach", boolean.class, false);
		tab.spotlightEffectReach = read(p + "SpotlightEffectReach", int.class, 100);
		tab.spotlightEffectSpread = read(p + "SpotlightEffectSpread", int.class, 100);
		tab.spotlightClickAnywhere = read(p + "SpotlightClickAnywhere", boolean.class, false);
		tab.spotlightClickAnywherePanels = read(p + "SpotlightClickAnywherePanels", boolean.class, false);
		tab.spotlightClickAnywhereDelay = read(p + "SpotlightClickAnywhereDelay", int.class, 600);
		tab.spotlightAlways = read(p + "SpotlightAlways", boolean.class, false);
		tab.spotlightDeadOnly = read(p + "SpotlightDeadOnly", boolean.class, false);
		tab.spotlightFalloff = read(p + "SpotlightFalloff", boolean.class, false);
		tab.spotlightFalloffDistance = read(p + "SpotlightFalloffDistance", int.class, 250);
		tab.spotlightFalloffEnd = read(p + "SpotlightFalloffEnd", int.class, 400);
		tab.spotlightFalloffColor = read(p + "SpotlightFalloffColor", Color.class, new Color(0, 0, 0, 0));
		tab.spotlightFalloffColorEnd = read(p + "SpotlightFalloffColorEnd", int.class, 0);
		tab.spotlightFadeEndColor = visible(read(p + "SpotlightFadeEndColor", Color.class, new Color(0, 0, 0, 0)));
		tab.missDots = read(p + "MissDots", boolean.class, false);
		tab.missDotColor = read(p + "MissDotColor", Color.class, new Color(255, 40, 40, 200));
		tab.missDotShape = read(p + "MissDotShape", InventoryHoverHighlightConfig.MissDotShape.class, InventoryHoverHighlightConfig.MissDotShape.CIRCLE);
		tab.missDotHollow = read(p + "MissDotHollow", boolean.class, false);
		tab.missDotSize = read(p + "MissDotSize", int.class, 4);
		tab.missDotDuration = read(p + "MissDotDuration", int.class, 15);
		return tab;
	}

	/** A stored setting, or its default when it was never set. */
	@SuppressWarnings("unchecked")
	/** A color, or null when it is fully see-through. */
	private static Color visible(Color color)
	{
		return color == null || color.getAlpha() == 0 ? null : color;
	}

	private <T> T read(String key, Type type, T fallback)
	{
		Object value = configManager.getConfiguration(InventoryHoverHighlightConfig.GROUP, key, type);
		return value == null ? fallback : (T) value;
	}
}
