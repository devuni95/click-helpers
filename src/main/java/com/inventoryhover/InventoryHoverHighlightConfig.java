package com.inventoryhover;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(InventoryHoverHighlightConfig.GROUP)
public interface InventoryHoverHighlightConfig extends Config
{
	String GROUP = "inventoryhoverhighlight";

	enum HighlightStyle
	{
		OUTLINE,
		FILL,
		BOTH
	}

	enum GridMode
	{
		OFF,
		IN_PANEL,
		ALWAYS
	}

	enum RingAnimation
	{
		GROW,
		SHRINK,
		PULSE,
		CROSS
	}

	enum CooldownStyle
	{
		FILL,
		DRAIN,
		DIM
	}

	/** How the eat and drink delay outline shows the time left. */
	enum DelayOutlineStyle
	{
		SOLID,
		FADE,
		TRACE
	}

	/** How the spotlight's fades change. */
	enum SpotlightShape
	{
		SMOOTH,
		SOFT,
		SHARP,
		BANDED
	}

	/** The spotlight's shape: how distance from the mouse is measured. */
	enum SpotlightType
	{
		CIRCLE,
		ELLIPSE,
		SQUARE,
		DIAMOND,
		STAR,
		HEXAGON,
		CROSS,
		FLOWER,
		VERTICAL_BAND,
		HORIZONTAL_BAND,
		RINGS,
		SPIRAL,
		RADAR
	}

	/** How the spotlight's shape moves. */
	enum SpotlightMotion
	{
		NONE,
		SPIN,
		WOBBLE
	}

	/** An animation on top of the spotlight. */
	enum SpotlightEffect
	{
		NONE,
		PULSE,
		RIPPLES,
		SPARKLES,
		EMBERS,
		ORBIT,
		SNOW
	}

	/** Where in the spotlight the effect shows. */
	enum EffectArea
	{
		OVERLAY,
		GLOW,
		GRADIENT,
		WHOLE
	}

	enum TickCue
	{
		OFF,
		PULSE,
		DRAIN,
		COLOR
	}

	enum PracticeStats
	{
		OFF,
		LAST_ROUND,
		SESSION,
		BOTH
	}

	enum ChartUnit
	{
		SECONDS,
		TICKS
	}

	enum MissDotShape
	{
		CIRCLE,
		RING,
		SQUARE,
		DIAMOND,
		TRIANGLE,
		CROSS,
		PLUS,
		HEART
	}


	// Shown in the side panel's Settings tab, see SettingsSections
	String areasSection = "areas";

	// Shown in the side panel's Settings tab, see SettingsSections
	String clickSection = "click";

	// Shown in the side panel's Settings tab, see SettingsSections
	String missSection = "misses";

	// Shown in the side panel's Settings tab, see SettingsSections
	String deadSection = "dead";

	// Shown in the side panel's Settings tab, see SettingsSections
	String prayerSection = "prayerspells";

	// Shown in the side panel's Settings tab, see SettingsSections
	String tickSection = "tick";

	// Shown in the side panel's Settings tab, see SettingsSections
	String chartSection = "stats";

	// Shown in the side panel's Settings tab, see SettingsSections
	String itemsSection = "items";

	// Shown in the side panel's Settings tab, see SettingsSections
	String delaySection = "delays";


	// Shown in the side panel's Settings tab, see SettingsSections
	String practiceSection = "practice";

	@ConfigSection(
		name = "Import/export settings",
		description = "Export all settings to the clipboard as JSON, import shared JSON settings, and the side panel",
		position = 12,
		closedByDefault = true
	)
	String shareSection = "share";

	// Shown in the side panel's Settings tab, see SettingsSections
	String invHoverSection = "invHover";

	// Shown in the side panel's Settings tab, see SettingsSections
	String invSpotlightSection = "invSpotlight";

	// Shown in the side panel's Settings tab, see SettingsSections
	String invMissDotsSection = "invMissDots";

	// Shown in the side panel's Settings tab, see SettingsSections
	String prayerHoverSection = "prayerHover";

	// Shown in the side panel's Settings tab, see SettingsSections
	String prayerSpotlightSection = "prayerSpotlight";

	// Shown in the side panel's Settings tab, see SettingsSections
	String prayerMissDotsSection = "prayerMissDots";

	// Shown in the side panel's Settings tab, see SettingsSections
	String spellHoverSection = "spellHover";

	// Shown in the side panel's Settings tab, see SettingsSections
	String spellSpotlightSection = "spellSpotlight";

	// Shown in the side panel's Settings tab, see SettingsSections
	String spellMissDotsSection = "spellMissDots";





	// Hover highlight








	@ConfigItem(
		keyName = "roundedAreas",
		hidden = true,
		name = "Round grid and dead areas to highlight radius",
		description = "Round the clickable grid and the dead area shading with the hover highlight's corner radius."
			+ "<br>Note: the game's clickable areas are square, so slot corners can look dead even though"
			+ " clicks there still work",
		position = 7,
		section = areasSection
	)
	default boolean roundedAreas()
	{
		return true;
	}





	// Areas

	@ConfigItem(
		keyName = "prayers",
		hidden = true,
		name = "Prayers",
		description = "Also highlight the hovered prayer in the prayer tab",
		position = 0,
		section = areasSection
	)
	default boolean prayers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "spells",
		hidden = true,
		name = "Spells",
		description = "Also highlight the hovered spell in the spellbook",
		position = 1,
		section = areasSection
	)
	default boolean spells()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bankInventory",
		hidden = true,
		name = "Bank inventory",
		description = "Also highlight the inventory shown next to the bank",
		position = 2,
		section = areasSection
	)
	default boolean bankInventory()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hideWhenEmpty",
		hidden = true,
		name = "Off with empty inventory",
		description = "Draw nothing while your inventory is empty, as if the plugin were off. Everything comes back as"
			+ " soon as an item is in your inventory. Switch practice still shows",
		position = -1,
		section = areasSection
	)
	default boolean hideWhenEmpty()
	{
		return false;
	}

	@ConfigItem(
		keyName = "onlyItems",
		hidden = true,
		name = "Only slots with items",
		description = "Skip empty inventory slots",
		position = 3,
		section = areasSection
	)
	default boolean onlyItems()
	{
		return true;
	}

	@ConfigItem(
		keyName = "grid",
		hidden = true,
		name = "Clickable grid",
		description = "Outline every clickable area the hover highlight can fill."
			+ "<br>In panel: only the panel under the mouse"
			+ "<br>Always: every visible panel",
		position = 4,
		section = areasSection
	)
	default GridMode grid()
	{
		return GridMode.OFF;
	}

	@ConfigItem(
		keyName = "gridShowEmpty",
		hidden = true,
		name = "Grid on empty slots",
		description = "Also outline inventory slots with no item, so every clickable slot shows",
		position = 5,
		section = areasSection
	)
	default boolean gridShowEmpty()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "gridColor",
		hidden = true,
		name = "Grid color",
		description = "Color of the clickable grid outlines",
		position = 6,
		section = areasSection
	)
	default Color gridColor()
	{
		return new Color(255, 255, 255, 40);
	}

	// Click feedback

	@ConfigItem(
		keyName = "clickFlash",
		hidden = true,
		name = "Click flash",
		description = "Briefly flash the slot you clicked",
		position = 0,
		section = clickSection
	)
	default boolean clickFlash()
	{
		return true;
	}

	@ConfigItem(
		keyName = "clickMarker",
		hidden = true,
		name = "Queued click marker",
		description = "Mark a clicked slot with a dot until the next game tick. The dot is yellow"
			+ " (rejected click color) when the click lands during the eat or drink delay",
		position = 1,
		section = clickSection
	)
	default boolean clickMarker()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "clickFlashColor",
		hidden = true,
		name = "Flash color",
		description = "Color of the click flash",
		position = 2,
		section = clickSection
	)
	default Color clickFlashColor()
	{
		return new Color(255, 255, 255, 110);
	}

	@Units(Units.MILLISECONDS)
	@Range(min = 50, max = 1000)
	@ConfigItem(
		keyName = "clickFlashDuration",
		hidden = true,
		name = "Flash duration",
		description = "How long the click flash takes to fade",
		position = 3,
		section = clickSection
	)
	default int clickFlashDuration()
	{
		return 200;
	}

	@Alpha
	@ConfigItem(
		keyName = "clickMarkerColor",
		hidden = true,
		name = "Marker color",
		description = "Color of the queued click marker",
		position = 4,
		section = clickSection
	)
	default Color clickMarkerColor()
	{
		return new Color(0, 255, 0, 220);
	}

	@Alpha
	@ConfigItem(
		keyName = "doubleClickColor",
		hidden = true,
		name = "Double click color",
		description = "Marker color when the same slot is clicked twice in one tick",
		position = 5,
		section = clickSection
	)
	default Color doubleClickColor()
	{
		return new Color(255, 60, 60, 220);
	}

	// Missed clicks

	@ConfigItem(
		keyName = "deadClickMarker",
		hidden = true,
		name = "Dead click marker",
		description = "Show a red ring where you click an empty slot or a gap with no interaction",
		position = 0,
		section = missSection
	)
	default boolean deadClickMarker()
	{
		return true;
	}

	@ConfigItem(
		keyName = "rejectedClickMarker",
		hidden = true,
		name = "Rejected click marker",
		description = "Show a yellow ring where a click reached the game but was refused,"
			+ " like equipping an item with no inventory space for the swap",
		position = 1,
		section = missSection
	)
	default boolean rejectedClickMarker()
	{
		return true;
	}


	@ConfigItem(
		keyName = "missOutline",
		hidden = true,
		name = "Miss outline",
		description = "Outline the slot nearest a dead click, the one you most likely meant to click."
			+ " It replaces the normal outline on that slot until it fades",
		position = 3,
		section = missSection
	)
	default boolean missOutline()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "misclickColor",
		hidden = true,
		name = "Dead click color",
		description = "Color of the dead click marker",
		position = 4,
		section = missSection
	)
	default Color misclickColor()
	{
		return new Color(255, 60, 60, 180);
	}

	@Alpha
	@ConfigItem(
		keyName = "rejectedClickColor",
		hidden = true,
		name = "Rejected click color",
		description = "Color of the rejected click marker",
		position = 5,
		section = missSection
	)
	default Color rejectedClickColor()
	{
		return new Color(255, 220, 0, 200);
	}

	@ConfigItem(
		keyName = "ringAnimation",
		hidden = true,
		name = "Ring animation",
		description = "How dead and rejected click markers animate."
			+ "<br>Grow: ring grows while it fades"
			+ "<br>Shrink: ring shrinks onto the click"
			+ "<br>Pulse: ring beats while it fades"
			+ "<br>Cross: an X instead of a ring",
		position = 6,
		section = missSection
	)
	default RingAnimation ringAnimation()
	{
		return RingAnimation.GROW;
	}

	@Units(Units.MILLISECONDS)
	@Range(min = 100, max = 3000)
	@ConfigItem(
		keyName = "ringDuration",
		hidden = true,
		name = "Ring duration",
		description = "How long dead and rejected click markers take to fade",
		position = 7,
		section = missSection
	)
	default int ringDuration()
	{
		return 500;
	}



	@Alpha
	@ConfigItem(
		keyName = "missOutlineColor",
		hidden = true,
		name = "Miss outline color",
		description = "Color of the miss outline",
		position = 10,
		section = missSection
	)
	default Color missOutlineColor()
	{
		return new Color(255, 40, 40, 130);
	}

	@Units(Units.MILLISECONDS)
	@Range(min = 100, max = 10000)
	@ConfigItem(
		keyName = "missOutlineDuration",
		hidden = true,
		name = "Miss outline duration",
		description = "How long the miss outline stays before it is gone",
		position = 11,
		section = missSection
	)
	default int missOutlineDuration()
	{
		return 1500;
	}

	@Range(
		min = 0,
		max = 10
	)
	@ConfigItem(
		keyName = "missOutlineWidth",
		hidden = true,
		name = "Miss outline width",
		description = "Line width of the miss outline in pixels; 0 makes it one thicker than the tab's hover outline (default 2)",
		position = 12,
		section = missSection
	)
	default int missOutlineWidth()
	{
		return 2;
	}



	// Dead areas

	@ConfigItem(
		keyName = "deadAreas",
		hidden = true,
		name = "Dead areas",
		description = "Shade the gaps where a click does nothing."
			+ "<br>In panel: only the panel under the mouse"
			+ "<br>Always: every visible panel",
		position = 0,
		section = deadSection
	)
	default GridMode deadAreas()
	{
		return GridMode.ALWAYS;
	}

	@ConfigItem(
		keyName = "deadInventory",
		hidden = true,
		name = "Inventory",
		description = "Shade dead areas in the inventory",
		position = 1,
		section = deadSection
	)
	default boolean deadInventory()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "deadInventoryColor",
		hidden = true,
		name = "Inventory color",
		description = "Color of the dead areas in the inventory",
		position = 2,
		section = deadSection
	)
	default Color deadInventoryColor()
	{
		return new Color(30, 30, 32, 90);
	}

	@ConfigItem(
		keyName = "deadBank",
		hidden = true,
		name = "Bank inventory",
		description = "Shade dead areas in the inventory shown next to the bank",
		position = 3,
		section = deadSection
	)
	default boolean deadBank()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "deadBankColor",
		hidden = true,
		name = "Bank inventory color",
		description = "Color of the dead areas in the bank inventory",
		position = 4,
		section = deadSection
	)
	default Color deadBankColor()
	{
		return new Color(30, 30, 32, 90);
	}

	@ConfigItem(
		keyName = "deadPrayers",
		hidden = true,
		name = "Prayers",
		description = "Shade dead areas in the prayer tab",
		position = 5,
		section = deadSection
	)
	default boolean deadPrayers()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "deadPrayersColor",
		hidden = true,
		name = "Prayers color",
		description = "Color of the dead areas in the prayer tab",
		position = 6,
		section = deadSection
	)
	default Color deadPrayersColor()
	{
		return new Color(30, 30, 32, 90);
	}

	@ConfigItem(
		keyName = "deadSpells",
		hidden = true,
		name = "Spells",
		description = "Shade dead areas in the spellbook",
		position = 7,
		section = deadSection
	)
	default boolean deadSpells()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "deadSpellsColor",
		hidden = true,
		name = "Spells color",
		description = "Color of the dead areas in the spellbook",
		position = 8,
		section = deadSection
	)
	default Color deadSpellsColor()
	{
		return new Color(30, 30, 32, 90);
	}

	@ConfigItem(
		keyName = "deadOuterAreas",
		hidden = true,
		name = "Cover whole prayer and spell tab",
		description = "Also shade the rest of the prayer and spellbook tab, like the empty space left by a filter."
			+ " Clicks there count as misses. Filter and back buttons stay unshaded",
		position = 9,
		section = deadSection
	)
	default boolean deadOuterAreas()
	{
		return false;
	}

	// Prayers and spells

	@ConfigItem(
		keyName = "activePrayers",
		hidden = true,
		name = "Active prayer highlight",
		description = "Replace the game's active prayer glow with a highlight that follows the"
			+ " server-confirmed prayer state, so it does not flicker when clicking fast",
		position = 0,
		section = prayerSection
	)
	default boolean activePrayers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "pendingPrayers",
		hidden = true,
		name = "Pending prayer state",
		description = "Show a dashed outline from your click until the server confirms the prayer change",
		position = 1,
		section = prayerSection
	)
	default boolean pendingPrayers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "selectedSpell",
		hidden = true,
		name = "Selected spell highlight",
		description = "Give a spell selected for manual casting the same gold highlight as an active prayer",
		position = 2,
		section = prayerSection
	)
	default boolean selectedSpell()
	{
		return true;
	}

	@ConfigItem(
		keyName = "autocastMarker",
		hidden = true,
		name = "Autocast marker",
		description = "Outline the spell set to autocast; hovering it shows the hover highlight instead",
		position = 3,
		section = prayerSection
	)
	default boolean autocastMarker()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "activePrayerColor",
		hidden = true,
		name = "Active outline",
		description = "Outline color of an active prayer or selected spell",
		position = 4,
		section = prayerSection
	)
	default Color activePrayerColor()
	{
		return new Color(255, 235, 120, 230);
	}

	@Alpha
	@ConfigItem(
		keyName = "activePrayerFill",
		hidden = true,
		name = "Active fill",
		description = "Fill color of an active prayer or selected spell",
		position = 5,
		section = prayerSection
	)
	default Color activePrayerFill()
	{
		return new Color(250, 188, 95, 75);
	}

	@Alpha
	@ConfigItem(
		keyName = "autocastColor",
		hidden = true,
		name = "Autocast color",
		description = "Color of the autocast marker",
		position = 6,
		section = prayerSection
	)
	default Color autocastColor()
	{
		return new Color(0, 200, 255, 180);
	}

	// Tick cue

	@ConfigItem(
		keyName = "tickCue",
		hidden = true,
		name = "Mid-tick cue",
		description = "Show progress through the click window on the hovered prayer."
			+ "<br>Pulse: brightens at the cue position"
			+ "<br>Drain: outline refills at the cue position, then shrinks"
			+ "<br>Color: outline turns green at the cue position, then goes to red",
		position = 0,
		section = tickSection
	)
	default TickCue tickCue()
	{
		return TickCue.PULSE;
	}

	@ConfigItem(
		keyName = "tickCueAllPrayers",
		hidden = true,
		name = "Cue on listed prayers",
		description = "Also show the mid-tick cue on the prayers in the list below, even when not hovered",
		position = 1,
		section = tickSection
	)
	default boolean tickCueAllPrayers()
	{
		return false;
	}

	@ConfigItem(
		keyName = "cuePrayers",
		hidden = true,
		name = "Cue prayers",
		description = "Comma separated prayer names that get the mid-tick cue; * is a wildcard, so * means every prayer",
		position = 2,
		section = tickSection
	)
	default String cuePrayers()
	{
		return "Protect from Magic, Protect from Missiles, Protect from Melee";
	}

	@Alpha
	@ConfigItem(
		keyName = "pulseColor",
		hidden = true,
		name = "Pulse color",
		description = "Color of the mid-tick cue pulse",
		position = 3,
		section = tickSection
	)
	default Color pulseColor()
	{
		return new Color(186, 140, 230, 90);
	}

	@Units(Units.MILLISECONDS)
	@Range(max = 300)
	@ConfigItem(
		keyName = "tickLatency",
		hidden = true,
		name = "Mid-tick cue latency",
		description = "Your round-trip ping to the world. A click must reach the server before"
			+ " its tick, so the click window closes this long before the client sees the next tick",
		position = 4,
		section = tickSection
	)
	default int tickLatency()
	{
		return 20;
	}

	@Units(Units.PERCENT)
	@Range(max = 90)
	@ConfigItem(
		keyName = "pulsePosition",
		hidden = true,
		name = "Cue position",
		description = "Where in the click window every cue mode kicks off. Slightly before the middle"
			+ " leaves room for reaction time while early clicks stay in the same tick",
		position = 5,
		section = tickSection
	)
	default int pulsePosition()
	{
		return 40;
	}

	// Click chart

	@ConfigItem(
		keyName = "clickChart",
		hidden = true,
		name = "Click chart",
		description = "Floating chart of your recent clicks: clean click rate, good, missed, refused and"
			+ " double clicks. Alt-drag it to move it",
		position = 0,
		section = chartSection
	)
	default boolean clickChart()
	{
		return false;
	}

	@ConfigItem(
		keyName = "clickChartHotkey",
		hidden = true,
		name = "Click chart hotkey",
		description = "Hotkey that shows or hides the click chart",
		position = 1,
		section = chartSection
	)
	default Keybind clickChartHotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "clickChartUnit",
		hidden = true,
		name = "Time unit",
		description = "Measure the chart in seconds or in game ticks (0.6 seconds each)",
		position = 2,
		section = chartSection
	)
	default ChartUnit clickChartUnit()
	{
		return ChartUnit.SECONDS;
	}

	@Range(min = 1, max = 300)
	@ConfigItem(
		keyName = "clickChartInterval",
		hidden = true,
		name = "Bar interval",
		description = "Seconds or ticks each bar covers",
		position = 3,
		section = chartSection
	)
	default int clickChartInterval()
	{
		return 5;
	}

	@Range(min = 4, max = 120)
	@ConfigItem(
		keyName = "clickChartBars",
		hidden = true,
		name = "Bars",
		description = "How many bars to show; the chart covers bars times the interval",
		position = 4,
		section = chartSection
	)
	default int clickChartBars()
	{
		return 24;
	}

	@Units(Units.PIXELS)
	@Range(min = 90, max = 600)
	@ConfigItem(
		keyName = "clickChartWidth",
		hidden = true,
		name = "Width",
		description = "Width of the chart in pixels; text shortens to fit narrow charts",
		position = 5,
		section = chartSection
	)
	default int clickChartWidth()
	{
		return 190;
	}

	@Units(Units.PIXELS)
	@Range(min = 15, max = 300)
	@ConfigItem(
		keyName = "clickChartHeight",
		hidden = true,
		name = "Bar height",
		description = "Height of the bar area in pixels",
		position = 6,
		section = chartSection
	)
	default int clickChartHeight()
	{
		return 50;
	}

	@ConfigItem(
		keyName = "clickChartLegend",
		hidden = true,
		name = "Legend",
		description = "Show the colored legend with the counts for the window",
		position = 7,
		section = chartSection
	)
	default boolean clickChartLegend()
	{
		return true;
	}

	@ConfigItem(
		keyName = "clickChartTimeAxis",
		hidden = true,
		name = "Time labels",
		description = "Show the time labels under the bars",
		position = 8,
		section = chartSection
	)
	default boolean clickChartTimeAxis()
	{
		return true;
	}

	@ConfigItem(
		keyName = "clickChartAverage",
		hidden = true,
		name = "Moving average",
		description = "Draw a line of the average clicks per bar over the last few bars",
		position = 9,
		section = chartSection
	)
	default boolean clickChartAverage()
	{
		return false;
	}

	@Range(min = 2, max = 60)
	@ConfigItem(
		keyName = "clickChartAverageBars",
		hidden = true,
		name = "Average over",
		description = "How many bars the moving average covers",
		position = 10,
		section = chartSection
	)
	default int clickChartAverageBars()
	{
		return 5;
	}







	// Hide items

	@ConfigItem(
		keyName = "hideItems",
		hidden = true,
		name = "Hide non-fight items",
		description = "Hide the icon of items with no fight action, such as items with only Use, Open"
			+ " or Bury, and noted items. They keep a faint outline and still work when clicked",
		position = 0,
		section = itemsSection
	)
	default boolean hideItems()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideItemsHotkey",
		hidden = true,
		name = "Hide toggle hotkey",
		description = "Hotkey that turns the item hiding on and off; when unset, hiding stays on",
		position = 1,
		section = itemsSection
	)
	default Keybind hideItemsHotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "fightActions",
		hidden = true,
		name = "Fight actions",
		description = "Comma separated inventory actions that keep an item visible",
		position = 2,
		section = itemsSection
	)
	default String fightActions()
	{
		return "Eat, Drink, Wield, Wear, Equip, Break, Guzzle, Invoke, Activate, Teleport, Rub";
	}

	@ConfigItem(
		keyName = "alwaysShowItems",
		hidden = true,
		name = "Always show",
		description = "Comma separated item names that are never hidden; * is a wildcard",
		position = 3,
		section = itemsSection
	)
	default String alwaysShowItems()
	{
		return "";
	}

	@ConfigItem(
		keyName = "hiddenItemShowOutline",
		hidden = true,
		name = "Show hidden item outline",
		description = "Draw a faint outline of a hidden item's icon; turn off with the backfill to leave the slot looking empty",
		position = 4,
		section = itemsSection
	)
	default boolean hiddenItemShowOutline()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hiddenItemShowFill",
		hidden = true,
		name = "Show hidden item backfill",
		description = "Shade the slot of a hidden item; turn off with the outline to leave the slot looking empty",
		position = 5,
		section = itemsSection
	)
	default boolean hiddenItemShowFill()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "hiddenItemOutline",
		hidden = true,
		name = "Hidden item outline",
		description = "Outline drawn in place of a hidden item's icon",
		position = 6,
		section = itemsSection
	)
	default Color hiddenItemOutline()
	{
		return new Color(255, 255, 255, 70);
	}

	@Alpha
	@ConfigItem(
		keyName = "hiddenItemFill",
		hidden = true,
		name = "Hidden item backfill",
		description = "Fill drawn behind a hidden item so you can tell the slot is taken",
		position = 7,
		section = itemsSection
	)
	default Color hiddenItemFill()
	{
		return new Color(255, 255, 255, 12);
	}

	// Eat and drink delay

	@ConfigItem(
		keyName = "cooldowns",
		hidden = true,
		name = "Eat and drink delay",
		description = "Mark food or potions while eating or drinking another one would be ignored."
			+ " Every item with the same action shares the delay; karambwans have their own",
		position = 0,
		section = delaySection
	)
	default boolean cooldowns()
	{
		return false;
	}

	@ConfigItem(
		keyName = "cooldownStyle",
		hidden = true,
		name = "Delay style",
		description = "How the delay is drawn."
			+ "<br>Fill: shade the whole slot"
			+ "<br>Drain: shade that empties as the delay runs out"
			+ "<br>Dim: shade that fades as the delay runs out",
		position = 1,
		section = delaySection
	)
	default CooldownStyle cooldownStyle()
	{
		return CooldownStyle.DRAIN;
	}

	@Alpha
	@ConfigItem(
		keyName = "cooldownColor",
		hidden = true,
		name = "Delay color",
		description = "Color of the eat and drink delay",
		position = 2,
		section = delaySection
	)
	default Color cooldownColor()
	{
		return new Color(0, 0, 0, 130);
	}

	@ConfigItem(
		keyName = "cooldownOutline",
		hidden = true,
		name = "Delay outline",
		description = "Outline items that cannot be eaten or drunk yet. Works with or without the delay shading above",
		position = 3,
		section = delaySection
	)
	default boolean cooldownOutline()
	{
		return false;
	}

	@ConfigItem(
		keyName = "cooldownOutlineStyle",
		hidden = true,
		name = "Delay outline style",
		description = "Solid: a steady outline until the delay ends. Fade: the outline fades out as the delay runs out."
			+ " Trace: the outline shrinks around the slot as the delay runs out",
		position = 4,
		section = delaySection
	)
	default DelayOutlineStyle cooldownOutlineStyle()
	{
		return DelayOutlineStyle.TRACE;
	}

	@Alpha
	@ConfigItem(
		keyName = "cooldownOutlineColor",
		hidden = true,
		name = "Delay outline color",
		description = "Color of the delay outline",
		position = 5,
		section = delaySection
	)
	default Color cooldownOutlineColor()
	{
		return new Color(255, 140, 0, 220);
	}

	@Range(
		min = 1,
		max = 10
	)
	@ConfigItem(
		keyName = "cooldownOutlineWidth",
		hidden = true,
		name = "Delay outline width",
		description = "Line width of the delay outline in pixels",
		position = 6,
		section = delaySection
	)
	default int cooldownOutlineWidth()
	{
		return 2;
	}

	// Spotlight











	// Switch practice

	@ConfigItem(
		keyName = "practiceMode",
		name = "Switch practice",
		description = "Light up random empty inventory slots to click as fast as you can, like a gear switch."
			+ " Clicks off the targets count as misses. Clicking an item still uses it, so practice with free slots."
			+ " Progress shows in the side panel",
		position = 0,
		section = practiceSection,
		hidden = true
	)
	default boolean practiceMode()
	{
		return false;
	}

	@ConfigItem(
		keyName = "practiceHotkey",
		hidden = true,
		name = "Practice hotkey",
		description = "Hotkey that starts or stops switch practice",
		position = 1,
		section = practiceSection
	)
	default Keybind practiceHotkey()
	{
		return Keybind.NOT_SET;
	}

	@Range(min = 1, max = 28)
	@ConfigItem(
		keyName = "practiceTargets",
		name = "Slots per switch",
		description = "How many empty slots light up each round, like the number of items in a switch",
		position = 2,
		section = practiceSection,
		hidden = true
	)
	default int practiceTargets()
	{
		return 4;
	}

	@Units(Units.MILLISECONDS)
	@Range(max = 5000)
	@ConfigItem(
		keyName = "practiceRoundDelay",
		name = "Time between rounds",
		description = "Pause after a round is done before the next targets light up",
		position = 3,
		section = practiceSection,
		hidden = true
	)
	default int practiceRoundDelay()
	{
		return 1200;
	}

	@ConfigItem(
		keyName = "practiceRandomDelay",
		name = "Random extra pause",
		description = "Add up to one more round delay at random, so you react instead of anticipating",
		position = 4,
		section = practiceSection,
		hidden = true
	)
	default boolean practiceRandomDelay()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "practiceTargetColor",
		name = "Target color",
		description = "Color of the slots to click",
		position = 5,
		section = practiceSection,
		hidden = true
	)
	default Color practiceTargetColor()
	{
		return new Color(255, 150, 30, 150);
	}

	@Alpha
	@ConfigItem(
		keyName = "practiceHitColor",
		name = "Hit color",
		description = "Color a target turns once it is clicked",
		position = 6,
		section = practiceSection,
		hidden = true
	)
	default Color practiceHitColor()
	{
		return new Color(60, 220, 100, 140);
	}

	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "practiceTimeLimitSeconds",
		name = "Round time limit",
		description = "Time to click every target before the round fails; unclicked targets count as misses."
			+ " 0 for no limit",
		position = 7,
		section = practiceSection,
		hidden = true
	)
	default double practiceTimeLimitSeconds()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "practiceTimerBar",
		name = "Timer bar",
		description = "Show a bar across the top of the inventory that empties as the round time limit runs out",
		position = 8,
		section = practiceSection,
		hidden = true
	)
	default boolean practiceTimerBar()
	{
		return true;
	}

	@Range(max = 500)
	@ConfigItem(
		keyName = "practiceRounds",
		name = "Rounds per session",
		description = "Stop practice after this many rounds and save the session; 0 to keep going until stopped",
		position = 9,
		section = practiceSection,
		hidden = true
	)
	default int practiceRounds()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "practiceOrdered",
		name = "Click in order",
		description = "Targets must be clicked in their numbered order; a click on any other target is a miss",
		position = 10,
		section = practiceSection,
		hidden = true
	)
	default boolean practiceOrdered()
	{
		return false;
	}

	@ConfigItem(
		keyName = "practiceNumbers",
		name = "Number targets",
		description = "Show each target's number; always shown when clicking in order",
		position = 11,
		section = practiceSection,
		hidden = true
	)
	default boolean practiceNumbers()
	{
		return false;
	}

	@ConfigItem(
		keyName = "practiceStatsMode",
		name = "Stats above inventory",
		description = "Practice stats shown above the inventory."
			+ "<br>Last round: switch time, ticks and misses"
			+ "<br>Session: rounds, average and best time, 1-tick rate, accuracy and timeouts",
		position = 12,
		section = practiceSection,
		hidden = true
	)
	default PracticeStats practiceStatsMode()
	{
		return PracticeStats.BOTH;
	}

	// Import/export settings

	@ConfigItem(
		keyName = "copySettings",
		hidden = true,
		name = "Export settings",
		description = "Tick to export all Click Helpers settings to the clipboard as JSON, to share them."
			+ " The box unticks itself",
		position = 0,
		section = shareSection
	)
	default boolean copySettings()
	{
		return false;
	}

	@ConfigItem(
		keyName = "pasteSettings",
		hidden = true,
		name = "Import settings",
		description = "Tick to import Click Helpers settings JSON from the clipboard. The box unticks itself",
		position = 1,
		section = shareSection
	)
	default boolean pasteSettings()
	{
		return false;
	}

	@ConfigItem(
		keyName = "sidePanel",
		name = "Side panel",
		description = "Add the Click Helpers side panel. It holds all the other settings, loadouts and switch practice,"
			+ " so turn it back on here to change them",
		position = 2,
		section = shareSection
	)
	default boolean sidePanel()
	{
		return true;
	}

	@ConfigItem(
		keyName = "communityLoadouts",
		name = "Community loadouts",
		description = "Browse and publish loadouts in the side panel's Community tab. When on, the panel"
			+ " connects to Google Firebase to list and publish loadouts, and downloads a public word"
			+ " list from jsdelivr.net to check names. Nothing is sent until you press a button. Loading"
			+ " someone's loadout sends an anonymous id to count the download. Your game name is sent only"
			+ " if you untick Anonymous before publishing",
		warning = "This plugin submits your IP address, and may submit other data, to a 3rd party server"
			+ " not controlled or verified by the RuneLite developers.",
		position = 3,
		section = shareSection
	)
	default boolean communityLoadouts()
	{
		return false;
	}

	// Tab settings

	@ConfigItem(
		keyName = "syncTabSettings",
		hidden = true,
		name = "Sync tab settings",
		description = "Keep the inventory, prayer and spellbook settings the same: changing one tab's setting"
			+ " changes it in the other tabs too. Turning it on copies the inventory settings to the others",
		position = 0
	)
	default boolean syncTabSettings()
	{
		return false;
	}

	// Inventory: hover

	@ConfigItem(
		keyName = "invStyle",
		hidden = true,
		name = "Style",
		description = "How the hovered area is drawn",
		position = 0,
		section = invHoverSection
	)
	default HighlightStyle invStyle()
	{
		return HighlightStyle.BOTH;
	}

	@Alpha
	@ConfigItem(
		keyName = "invOutlineColor",
		hidden = true,
		name = "Outline color",
		description = "Color of the hover outline",
		position = 1,
		section = invHoverSection
	)
	default Color invOutlineColor()
	{
		return new Color(255, 255, 255, 120);
	}

	@Alpha
	@ConfigItem(
		keyName = "invFillColor",
		hidden = true,
		name = "Fill color",
		description = "Color of the hover fill",
		position = 2,
		section = invHoverSection
	)
	default Color invFillColor()
	{
		return new Color(98, 189, 125, 60);
	}

	@Range(min = 1, max = 4)
	@ConfigItem(
		keyName = "invOutlineWidth",
		hidden = true,
		name = "Outline width",
		description = "Thickness of outlines in pixels",
		position = 3,
		section = invHoverSection
	)
	default int invOutlineWidth()
	{
		return 1;
	}

	@Range(max = 10)
	@ConfigItem(
		keyName = "invCornerRadius",
		hidden = true,
		name = "Corner radius",
		description = "Rounds the corners of highlights and shading; 0 for square",
		position = 4,
		section = invHoverSection
	)
	default int invCornerRadius()
	{
		return 4;
	}

	@Units(Units.MILLISECONDS)
	@Range(max = 5000)
	@ConfigItem(
		keyName = "invHoverFade",
		hidden = true,
		name = "Hover fade-out",
		description = "How long a highlight, including its tick cue, fades after the mouse leaves; 0 to disable",
		position = 5,
		section = invHoverSection
	)
	default int invHoverFade()
	{
		return 400;
	}

	@Units(Units.MILLISECONDS)
	@Range(max = 1000)
	@ConfigItem(
		keyName = "invHoverDelay",
		hidden = true,
		name = "Hover delay",
		description = "How long the mouse must rest on an area before it is highlighted; 0 for instant",
		position = 6,
		section = invHoverSection
	)
	default int invHoverDelay()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "invHideWhenMenuOpen",
		hidden = true,
		name = "Hide when right-click menu open",
		description = "Do not highlight while a right-click menu is open",
		position = 7,
		section = invHoverSection
	)
	default boolean invHideWhenMenuOpen()
	{
		return false;
	}

	@ConfigItem(
		keyName = "invDiscoMode",
		hidden = true,
		name = "Disco mode",
		description = "Pick a random hover color every time the mouse moves to a new area",
		position = 8,
		section = invHoverSection
	)
	default boolean invDiscoMode()
	{
		return false;
	}

	@Units(Units.MILLISECONDS)
	@Range(max = 5000)
	@ConfigItem(
		keyName = "invDiscoInterval",
		hidden = true,
		name = "Disco interval",
		description = "In disco mode, also pick a new color this often while the mouse rests; 0 to only change on a new area",
		position = 9,
		section = invHoverSection
	)
	default int invDiscoInterval()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "invKeepIconColors",
		hidden = true,
		name = "Keep icon colors on hover",
		description = "Leave the icon out of the hover fill, so the fill only colors the slot background",
		position = 10,
		section = invHoverSection
	)
	default boolean invKeepIconColors()
	{
		return false;
	}

	@ConfigItem(
		keyName = "invSlotBackground",
		hidden = true,
		name = "Slot background",
		description = "Color the background of every slot; icons stay on top",
		position = 11,
		section = invHoverSection
	)
	default boolean invSlotBackground()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "invSlotBackgroundColor",
		hidden = true,
		name = "Slot background color",
		description = "Background color of the slots",
		position = 12,
		section = invHoverSection
	)
	default Color invSlotBackgroundColor()
	{
		return new Color(60, 60, 70, 90);
	}

	// Inventory: spotlight

	@ConfigItem(
		keyName = "invSpotlight",
		hidden = true,
		name = "Spotlight",
		description = "Light glow at the mouse that dims the panel outward, to show what is in reach",
		position = 0,
		section = invSpotlightSection
	)
	default boolean invSpotlight()
	{
		return false;
	}

	@Range(min = 20, max = 2000)
	@ConfigItem(
		keyName = "invSpotlightRadius",
		hidden = true,
		name = "Radius",
		description = "Distance in pixels from the mouse where the dimming is full",
		position = 1,
		section = invSpotlightSection
	)
	default int invSpotlightRadius()
	{
		return 90;
	}

	@Alpha
	@ConfigItem(
		keyName = "invSpotlightDim",
		hidden = true,
		name = "Dim color",
		description = "Color at the edge of the spotlight and beyond",
		position = 2,
		section = invSpotlightSection
	)
	default Color invSpotlightDim()
	{
		return new Color(0, 0, 0, 110);
	}

	@Alpha
	@ConfigItem(
		keyName = "invSpotlightGlow",
		hidden = true,
		name = "Glow color",
		description = "Color of the glow at the mouse",
		position = 3,
		section = invSpotlightSection
	)
	default Color invSpotlightGlow()
	{
		return new Color(255, 255, 255, 45);
	}

	@Units(Units.PERCENT)
	@Range(max = 100)
	@ConfigItem(
		keyName = "invSpotlightGlowSize",
		hidden = true,
		name = "Glow size",
		description = "How far the glow reaches, as a share of the radius; 0 for no glow, so only the dimming shows",
		position = 4,
		section = invSpotlightSection
	)
	default int invSpotlightGlowSize()
	{
		return 30;
	}

	@ConfigItem(
		keyName = "invSpotlightShape",
		hidden = true,
		name = "Gradient",
		description = "How the spotlight fades: Smooth evenly, Soft eased in and out, Sharp with a crisp edge,"
			+ " or Banded in steps",
		position = 5,
		section = invSpotlightSection
	)
	default SpotlightShape invSpotlightShape()
	{
		return SpotlightShape.SMOOTH;
	}

	@ConfigItem(
		keyName = "invSpotlightType",
		hidden = true,
		name = "Gradient type",
		description = "The spotlight's shape, or a pattern in it. Shapes: circle, ellipse, square, diamond, star,"
			+ " hexagon, cross, flower, and vertical or horizontal bands. Patterns on a circle: rings, a turning spiral,"
			+ " or a radar sweep. All of them carry on through the falloff",
		position = 6,
		section = invSpotlightSection
	)
	default SpotlightType invSpotlightType()
	{
		return SpotlightType.CIRCLE;
	}

	@ConfigItem(
		keyName = "invSpotlightMotion",
		hidden = true,
		name = "Gradient motion",
		description = "Move the spotlight's shape: Spin turns it slowly, Wobble ripples its edge. Uses the effect speed",
		position = 7,
		section = invSpotlightSection
	)
	default SpotlightMotion invSpotlightMotion()
	{
		return SpotlightMotion.NONE;
	}

	@ConfigItem(
		keyName = "invSpotlightEffect",
		hidden = true,
		name = "Effect",
		description = "An animation on the spotlight: Pulse breathes in and out, Ripples sends rings outward,"
			+ " and Sparkles, Embers, Orbit and Snow add particles",
		position = 8,
		section = invSpotlightSection
	)
	default SpotlightEffect invSpotlightEffect()
	{
		return SpotlightEffect.NONE;
	}

	@ConfigItem(
		keyName = "invSpotlightEffectColor",
		hidden = true,
		name = "Effect on",
		description = "Where the effect shows. Overlay draws it on top within the spotlight radius and never changes"
			+ " the spotlight itself. Glow, Gradient and Whole keep it to the glow near the mouse, the gradient past"
			+ " the glow's edge, or everything, and can replace that part with Replace with effect",
		position = 9,
		section = invSpotlightSection
	)
	default EffectArea invSpotlightEffectColor()
	{
		return EffectArea.OVERLAY;
	}

	@ConfigItem(
		keyName = "invSpotlightEffectReplace",
		hidden = true,
		name = "Replace with effect",
		description = "Draw the Effect on area only as the effect, in the gradient's own colors, instead of a smooth"
			+ " fill; with Gradient and Sparkles, the gradient is made of sparkles. Has no effect with Overlay",
		position = 10,
		section = invSpotlightSection
	)
	default boolean invSpotlightEffectReplace()
	{
		return false;
	}

	@ConfigItem(
		keyName = "invSpotlightEffectCustom",
		hidden = true,
		name = "Custom effect color",
		description = "Use the effect color below. When off, each particle and ring takes the color the spotlight"
			+ " draws under it, so it shades from the glow into the gradient and falloff as it moves",
		position = 11,
		section = invSpotlightSection
	)
	default boolean invSpotlightEffectCustom()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "invSpotlightEffectOverride",
		hidden = true,
		name = "Effect color",
		description = "Color of the effect, used when Custom effect color is on",
		position = 12,
		section = invSpotlightSection
	)
	default Color invSpotlightEffectOverride()
	{
		return new Color(255, 255, 255, 200);
	}

	@ConfigItem(
		keyName = "invSpotlightEffectOnClick",
		hidden = true,
		name = "Effect on click",
		description = "Play the effect as a short burst when you click, instead of all the time",
		position = 13,
		section = invSpotlightSection
	)
	default boolean invSpotlightEffectOnClick()
	{
		return false;
	}

	@ConfigItem(
		keyName = "invSpotlightClickResult",
		hidden = true,
		name = "Click result colors",
		description = "With Effect on click, color each burst by how the click went, using the three colors below",
		position = 14,
		section = invSpotlightSection
	)
	default boolean invSpotlightClickResult()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "invSpotlightClickSuccessColor",
		hidden = true,
		name = "Good click color",
		description = "Burst color for a good click, one that did something, with Click result colors on",
		position = 15,
		section = invSpotlightSection
	)
	default Color invSpotlightClickSuccessColor()
	{
		return new Color(0, 255, 0, 200);
	}

	@Alpha
	@ConfigItem(
		keyName = "invSpotlightClickMissColor",
		hidden = true,
		name = "Bad click color",
		description = "Burst color for a bad click, one that hit nothing, with Click result colors on",
		position = 16,
		section = invSpotlightSection
	)
	default Color invSpotlightClickMissColor()
	{
		return new Color(255, 60, 60, 200);
	}

	@Alpha
	@ConfigItem(
		keyName = "invSpotlightClickRefusedColor",
		hidden = true,
		name = "Refused click color",
		description = "Burst color when the game refused the click, with Click result colors on",
		position = 17,
		section = invSpotlightSection
	)
	default Color invSpotlightClickRefusedColor()
	{
		return new Color(255, 220, 0, 200);
	}

	@Range(
		min = 100,
		max = 10000
	)
	@Units(Units.MILLISECONDS)
	@ConfigItem(
		keyName = "invSpotlightEffectLength",
		hidden = true,
		name = "Effect length",
		description = "How long each click burst lasts with Effect on click, and each detached effect with Detach"
			+ " from mouse",
		position = 18,
		section = invSpotlightSection
	)
	default int invSpotlightEffectLength()
	{
		return 1200;
	}

	@Range(
		min = 10,
		max = 500
	)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "invSpotlightEffectSpeed",
		hidden = true,
		name = "Effect speed",
		description = "How fast the effect moves: 100% is normal, lower is slower, higher is faster",
		position = 19,
		section = invSpotlightSection
	)
	default int invSpotlightEffectSpeed()
	{
		return 100;
	}

	@Range(
		min = 25,
		max = 400
	)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "invSpotlightEffectSize",
		hidden = true,
		name = "Effect size",
		description = "How big the effect's particles, rings and trails are: 100% is normal",
		position = 20,
		section = invSpotlightSection
	)
	default int invSpotlightEffectSize()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "invSpotlightEffectDetach",
		hidden = true,
		name = "Detach from mouse",
		description = "Let each effect run on its own where it started: where you clicked with Effect on click, else"
			+ " where the mouse entered the panel. Each runs for the effect length wherever the mouse goes, and new"
			+ " ones start alongside older ones",
		position = 21,
		section = invSpotlightSection
	)
	default boolean invSpotlightEffectDetach()
	{
		return false;
	}

	@Range(
		min = 25,
		max = 1000
	)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "invSpotlightEffectReach",
		hidden = true,
		name = "Detached reach",
		description = "How far a detached effect reaches from where it started: 100% is as far as it reaches on"
			+ " the mouse. Raise it for clicks anywhere to fill more of the screen",
		position = 22,
		section = invSpotlightSection
	)
	default int invSpotlightEffectReach()
	{
		return 100;
	}

	@Range(
		min = 25,
		max = 400
	)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "invSpotlightEffectSpread",
		hidden = true,
		name = "Detached spread",
		description = "How far apart a detached effect's particles, such as snowflakes or sparkles, are: 100% is"
			+ " normal, higher spreads them out with fewer over the same area",
		position = 23,
		section = invSpotlightSection
	)
	default int invSpotlightEffectSpread()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "invSpotlightClickAnywhere",
		hidden = true,
		name = "Click effects anywhere",
		description = "With Effect on click, clicks outside the inventory, prayer and spellbook panels, such as on the"
			+ " game world, also play this effect where you clicked",
		position = 24,
		section = invSpotlightSection
	)
	default boolean invSpotlightClickAnywhere()
	{
		return false;
	}

	@Range(
		min = 0,
		max = 10000
	)
	@Units(Units.MILLISECONDS)
	@ConfigItem(
		keyName = "invSpotlightClickAnywhereDelay",
		hidden = true,
		name = "Click anywhere delay",
		description = "Shortest time between click effects outside the panels; clicks sooner than this play nothing."
			+ " 0 plays one for every click",
		position = 25,
		section = invSpotlightSection
	)
	default int invSpotlightClickAnywhereDelay()
	{
		return 600;
	}

	@ConfigItem(
		keyName = "invSpotlightAlways",
		hidden = true,
		name = "When not hovered",
		description = "Also spotlight this panel while the mouse is outside it",
		position = 26,
		section = invSpotlightSection
	)
	default boolean invSpotlightAlways()
	{
		return false;
	}

	@ConfigItem(
		keyName = "invSpotlightDeadOnly",
		hidden = true,
		name = "Only dead areas",
		description = "Only dim and light the dead areas, so slots keep their colors",
		position = 27,
		section = invSpotlightSection
	)
	default boolean invSpotlightDeadOnly()
	{
		return false;
	}

	@ConfigItem(
		keyName = "invSpotlightFalloff",
		hidden = true,
		name = "Falloff",
		description = "Past the falloff start, run the spotlight gradient in reverse: the dim fades out, then the falloff color fades in by the falloff end",
		position = 28,
		section = invSpotlightSection
	)
	default boolean invSpotlightFalloff()
	{
		return false;
	}


	@Range(min = 20, max = 4000)
	@ConfigItem(
		keyName = "invSpotlightFalloffDistance",
		hidden = true,
		name = "Falloff start",
		description = "Distance in pixels from the mouse where the dimming starts fading back out",
		position = 29,
		section = invSpotlightSection
	)
	default int invSpotlightFalloffDistance()
	{
		return 250;
	}

	@Range(min = 20, max = 4000)
	@ConfigItem(
		keyName = "invSpotlightFalloffEnd",
		hidden = true,
		name = "Falloff end",
		description = "Distance in pixels from the mouse where the dimming is gone; kept past the falloff start",
		position = 30,
		section = invSpotlightSection
	)
	default int invSpotlightFalloffEnd()
	{
		return 400;
	}

	@Alpha
	@ConfigItem(
		keyName = "invSpotlightFalloffColor",
		hidden = true,
		name = "Falloff color",
		description = "Color the falloff turns into by the falloff end, mirroring the glow at the mouse; kept beyond it."
			+ " Leave its alpha at 0 for the dimming to just fade out",
		position = 31,
		section = invSpotlightSection
	)
	default Color invSpotlightFalloffColor()
	{
		return new Color(0, 0, 0, 0);
	}

	@Range(
		min = 0,
		max = 3000
	)
	@ConfigItem(
		keyName = "invSpotlightFalloffColorEnd",
		hidden = true,
		name = "Falloff color end",
		description = "Distance in pixels from the mouse where the falloff color has faded back out, over the same"
			+ " length it took to fade in. 0 keeps the falloff color out to the panel's edges",
		position = 32,
		section = invSpotlightSection
	)
	default int invSpotlightFalloffColorEnd()
	{
		return 0;
	}

	@Alpha
	@ConfigItem(
		keyName = "invSpotlightFadeEndColor",
		hidden = true,
		name = "Fade end color",
		description = "Color the falloff color blends into at the falloff color end, kept beyond it. Fully see-through"
			+ " (the default) fades it out to nothing. Needs a falloff color end",
		position = 33,
		section = invSpotlightSection
	)
	default Color invSpotlightFadeEndColor()
	{
		return new Color(0, 0, 0, 0);
	}

	// Inventory: miss dots

	@ConfigItem(
		keyName = "invMissDots",
		hidden = true,
		name = "Miss dots",
		description = "Leave a small dot at every dead click, so you can see where your misses land",
		position = 0,
		section = invMissDotsSection
	)
	default boolean invMissDots()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "invMissDotColor",
		hidden = true,
		name = "Color",
		description = "Color of the miss dots",
		position = 1,
		section = invMissDotsSection
	)
	default Color invMissDotColor()
	{
		return new Color(255, 40, 40, 200);
	}

	@ConfigItem(
		keyName = "invMissDotShape",
		hidden = true,
		name = "Shape",
		description = "Shape of the miss dots",
		position = 2,
		section = invMissDotsSection
	)
	default MissDotShape invMissDotShape()
	{
		return MissDotShape.CIRCLE;
	}

	@ConfigItem(
		keyName = "invMissDotHollow",
		hidden = true,
		name = "Hollow",
		description = "Draw the miss dots as outlines instead of filled shapes",
		position = 2,
		section = invMissDotsSection
	)
	default boolean invMissDotHollow()
	{
		return false;
	}

	@Range(min = 2, max = 16)
	@ConfigItem(
		keyName = "invMissDotSize",
		hidden = true,
		name = "Size",
		description = "Size of the miss dots in pixels",
		position = 3,
		section = invMissDotsSection
	)
	default int invMissDotSize()
	{
		return 4;
	}

	@Units(Units.SECONDS)
	@Range(min = 1, max = 600)
	@ConfigItem(
		keyName = "invMissDotDuration",
		hidden = true,
		name = "Duration",
		description = "How long each miss dot stays; it fades over this time",
		position = 4,
		section = invMissDotsSection
	)
	default int invMissDotDuration()
	{
		return 15;
	}

	// Prayer: hover

	@ConfigItem(
		keyName = "prayerStyle",
		hidden = true,
		name = "Style",
		description = "How the hovered area is drawn",
		position = 0,
		section = prayerHoverSection
	)
	default HighlightStyle prayerStyle()
	{
		return HighlightStyle.BOTH;
	}

	@Alpha
	@ConfigItem(
		keyName = "prayerOutlineColor",
		hidden = true,
		name = "Outline color",
		description = "Color of the hover outline",
		position = 1,
		section = prayerHoverSection
	)
	default Color prayerOutlineColor()
	{
		return new Color(255, 255, 255, 120);
	}

	@Alpha
	@ConfigItem(
		keyName = "prayerFillColor",
		hidden = true,
		name = "Fill color",
		description = "Color of the hover fill",
		position = 2,
		section = prayerHoverSection
	)
	default Color prayerFillColor()
	{
		return new Color(98, 189, 125, 60);
	}

	@Range(min = 1, max = 4)
	@ConfigItem(
		keyName = "prayerOutlineWidth",
		hidden = true,
		name = "Outline width",
		description = "Thickness of outlines in pixels",
		position = 3,
		section = prayerHoverSection
	)
	default int prayerOutlineWidth()
	{
		return 1;
	}

	@Range(max = 10)
	@ConfigItem(
		keyName = "prayerCornerRadius",
		hidden = true,
		name = "Corner radius",
		description = "Rounds the corners of highlights and shading; 0 for square",
		position = 4,
		section = prayerHoverSection
	)
	default int prayerCornerRadius()
	{
		return 4;
	}

	@Units(Units.MILLISECONDS)
	@Range(max = 5000)
	@ConfigItem(
		keyName = "prayerHoverFade",
		hidden = true,
		name = "Hover fade-out",
		description = "How long a highlight, including its tick cue, fades after the mouse leaves; 0 to disable",
		position = 5,
		section = prayerHoverSection
	)
	default int prayerHoverFade()
	{
		return 400;
	}

	@Units(Units.MILLISECONDS)
	@Range(max = 1000)
	@ConfigItem(
		keyName = "prayerHoverDelay",
		hidden = true,
		name = "Hover delay",
		description = "How long the mouse must rest on an area before it is highlighted; 0 for instant",
		position = 6,
		section = prayerHoverSection
	)
	default int prayerHoverDelay()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "prayerHideWhenMenuOpen",
		hidden = true,
		name = "Hide when right-click menu open",
		description = "Do not highlight while a right-click menu is open",
		position = 7,
		section = prayerHoverSection
	)
	default boolean prayerHideWhenMenuOpen()
	{
		return false;
	}

	@ConfigItem(
		keyName = "prayerDiscoMode",
		hidden = true,
		name = "Disco mode",
		description = "Pick a random hover color every time the mouse moves to a new area",
		position = 8,
		section = prayerHoverSection
	)
	default boolean prayerDiscoMode()
	{
		return false;
	}

	@Units(Units.MILLISECONDS)
	@Range(max = 5000)
	@ConfigItem(
		keyName = "prayerDiscoInterval",
		hidden = true,
		name = "Disco interval",
		description = "In disco mode, also pick a new color this often while the mouse rests; 0 to only change on a new area",
		position = 9,
		section = prayerHoverSection
	)
	default int prayerDiscoInterval()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "prayerKeepIconColors",
		hidden = true,
		name = "Keep icon colors on hover",
		description = "Leave the icon out of the hover fill, so the fill only colors the slot background",
		position = 10,
		section = prayerHoverSection
	)
	default boolean prayerKeepIconColors()
	{
		return false;
	}

	@ConfigItem(
		keyName = "prayerSlotBackground",
		hidden = true,
		name = "Slot background",
		description = "Color the background of every slot; icons stay on top",
		position = 11,
		section = prayerHoverSection
	)
	default boolean prayerSlotBackground()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "prayerSlotBackgroundColor",
		hidden = true,
		name = "Slot background color",
		description = "Background color of the slots",
		position = 12,
		section = prayerHoverSection
	)
	default Color prayerSlotBackgroundColor()
	{
		return new Color(60, 60, 70, 90);
	}

	// Prayer: spotlight

	@ConfigItem(
		keyName = "prayerSpotlight",
		hidden = true,
		name = "Spotlight",
		description = "Light glow at the mouse that dims the panel outward, to show what is in reach",
		position = 0,
		section = prayerSpotlightSection
	)
	default boolean prayerSpotlight()
	{
		return false;
	}

	@Range(min = 20, max = 2000)
	@ConfigItem(
		keyName = "prayerSpotlightRadius",
		hidden = true,
		name = "Radius",
		description = "Distance in pixels from the mouse where the dimming is full",
		position = 1,
		section = prayerSpotlightSection
	)
	default int prayerSpotlightRadius()
	{
		return 90;
	}

	@Alpha
	@ConfigItem(
		keyName = "prayerSpotlightDim",
		hidden = true,
		name = "Dim color",
		description = "Color at the edge of the spotlight and beyond",
		position = 2,
		section = prayerSpotlightSection
	)
	default Color prayerSpotlightDim()
	{
		return new Color(0, 0, 0, 110);
	}

	@Alpha
	@ConfigItem(
		keyName = "prayerSpotlightGlow",
		hidden = true,
		name = "Glow color",
		description = "Color of the glow at the mouse",
		position = 3,
		section = prayerSpotlightSection
	)
	default Color prayerSpotlightGlow()
	{
		return new Color(255, 255, 255, 45);
	}

	@Units(Units.PERCENT)
	@Range(max = 100)
	@ConfigItem(
		keyName = "prayerSpotlightGlowSize",
		hidden = true,
		name = "Glow size",
		description = "How far the glow reaches, as a share of the radius; 0 for no glow, so only the dimming shows",
		position = 4,
		section = prayerSpotlightSection
	)
	default int prayerSpotlightGlowSize()
	{
		return 30;
	}

	@ConfigItem(
		keyName = "prayerSpotlightShape",
		hidden = true,
		name = "Gradient",
		description = "How the spotlight fades: Smooth evenly, Soft eased in and out, Sharp with a crisp edge,"
			+ " or Banded in steps",
		position = 5,
		section = prayerSpotlightSection
	)
	default SpotlightShape prayerSpotlightShape()
	{
		return SpotlightShape.SMOOTH;
	}

	@ConfigItem(
		keyName = "prayerSpotlightType",
		hidden = true,
		name = "Gradient type",
		description = "The spotlight's shape, or a pattern in it. Shapes: circle, ellipse, square, diamond, star,"
			+ " hexagon, cross, flower, and vertical or horizontal bands. Patterns on a circle: rings, a turning spiral,"
			+ " or a radar sweep. All of them carry on through the falloff",
		position = 6,
		section = prayerSpotlightSection
	)
	default SpotlightType prayerSpotlightType()
	{
		return SpotlightType.CIRCLE;
	}

	@ConfigItem(
		keyName = "prayerSpotlightMotion",
		hidden = true,
		name = "Gradient motion",
		description = "Move the spotlight's shape: Spin turns it slowly, Wobble ripples its edge. Uses the effect speed",
		position = 7,
		section = prayerSpotlightSection
	)
	default SpotlightMotion prayerSpotlightMotion()
	{
		return SpotlightMotion.NONE;
	}

	@ConfigItem(
		keyName = "prayerSpotlightEffect",
		hidden = true,
		name = "Effect",
		description = "An animation on the spotlight: Pulse breathes in and out, Ripples sends rings outward,"
			+ " and Sparkles, Embers, Orbit and Snow add particles",
		position = 8,
		section = prayerSpotlightSection
	)
	default SpotlightEffect prayerSpotlightEffect()
	{
		return SpotlightEffect.NONE;
	}

	@ConfigItem(
		keyName = "prayerSpotlightEffectColor",
		hidden = true,
		name = "Effect on",
		description = "Where the effect shows. Overlay draws it on top within the spotlight radius and never changes"
			+ " the spotlight itself. Glow, Gradient and Whole keep it to the glow near the mouse, the gradient past"
			+ " the glow's edge, or everything, and can replace that part with Replace with effect",
		position = 9,
		section = prayerSpotlightSection
	)
	default EffectArea prayerSpotlightEffectColor()
	{
		return EffectArea.OVERLAY;
	}

	@ConfigItem(
		keyName = "prayerSpotlightEffectReplace",
		hidden = true,
		name = "Replace with effect",
		description = "Draw the Effect on area only as the effect, in the gradient's own colors, instead of a smooth"
			+ " fill; with Gradient and Sparkles, the gradient is made of sparkles. Has no effect with Overlay",
		position = 10,
		section = prayerSpotlightSection
	)
	default boolean prayerSpotlightEffectReplace()
	{
		return false;
	}

	@ConfigItem(
		keyName = "prayerSpotlightEffectCustom",
		hidden = true,
		name = "Custom effect color",
		description = "Use the effect color below. When off, each particle and ring takes the color the spotlight"
			+ " draws under it, so it shades from the glow into the gradient and falloff as it moves",
		position = 11,
		section = prayerSpotlightSection
	)
	default boolean prayerSpotlightEffectCustom()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "prayerSpotlightEffectOverride",
		hidden = true,
		name = "Effect color",
		description = "Color of the effect, used when Custom effect color is on",
		position = 12,
		section = prayerSpotlightSection
	)
	default Color prayerSpotlightEffectOverride()
	{
		return new Color(255, 255, 255, 200);
	}

	@ConfigItem(
		keyName = "prayerSpotlightEffectOnClick",
		hidden = true,
		name = "Effect on click",
		description = "Play the effect as a short burst when you click, instead of all the time",
		position = 13,
		section = prayerSpotlightSection
	)
	default boolean prayerSpotlightEffectOnClick()
	{
		return false;
	}

	@ConfigItem(
		keyName = "prayerSpotlightClickResult",
		hidden = true,
		name = "Click result colors",
		description = "With Effect on click, color each burst by how the click went, using the three colors below",
		position = 14,
		section = prayerSpotlightSection
	)
	default boolean prayerSpotlightClickResult()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "prayerSpotlightClickSuccessColor",
		hidden = true,
		name = "Good click color",
		description = "Burst color for a good click, one that did something, with Click result colors on",
		position = 15,
		section = prayerSpotlightSection
	)
	default Color prayerSpotlightClickSuccessColor()
	{
		return new Color(0, 255, 0, 200);
	}

	@Alpha
	@ConfigItem(
		keyName = "prayerSpotlightClickMissColor",
		hidden = true,
		name = "Bad click color",
		description = "Burst color for a bad click, one that hit nothing, with Click result colors on",
		position = 16,
		section = prayerSpotlightSection
	)
	default Color prayerSpotlightClickMissColor()
	{
		return new Color(255, 60, 60, 200);
	}

	@Alpha
	@ConfigItem(
		keyName = "prayerSpotlightClickRefusedColor",
		hidden = true,
		name = "Refused click color",
		description = "Burst color when the game refused the click, with Click result colors on",
		position = 17,
		section = prayerSpotlightSection
	)
	default Color prayerSpotlightClickRefusedColor()
	{
		return new Color(255, 220, 0, 200);
	}

	@Range(
		min = 100,
		max = 10000
	)
	@Units(Units.MILLISECONDS)
	@ConfigItem(
		keyName = "prayerSpotlightEffectLength",
		hidden = true,
		name = "Effect length",
		description = "How long each click burst lasts with Effect on click, and each detached effect with Detach"
			+ " from mouse",
		position = 18,
		section = prayerSpotlightSection
	)
	default int prayerSpotlightEffectLength()
	{
		return 1200;
	}

	@Range(
		min = 10,
		max = 500
	)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "prayerSpotlightEffectSpeed",
		hidden = true,
		name = "Effect speed",
		description = "How fast the effect moves: 100% is normal, lower is slower, higher is faster",
		position = 19,
		section = prayerSpotlightSection
	)
	default int prayerSpotlightEffectSpeed()
	{
		return 100;
	}

	@Range(
		min = 25,
		max = 400
	)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "prayerSpotlightEffectSize",
		hidden = true,
		name = "Effect size",
		description = "How big the effect's particles, rings and trails are: 100% is normal",
		position = 20,
		section = prayerSpotlightSection
	)
	default int prayerSpotlightEffectSize()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "prayerSpotlightEffectDetach",
		hidden = true,
		name = "Detach from mouse",
		description = "Let each effect run on its own where it started: where you clicked with Effect on click, else"
			+ " where the mouse entered the panel. Each runs for the effect length wherever the mouse goes, and new"
			+ " ones start alongside older ones",
		position = 21,
		section = prayerSpotlightSection
	)
	default boolean prayerSpotlightEffectDetach()
	{
		return false;
	}

	@Range(
		min = 25,
		max = 1000
	)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "prayerSpotlightEffectReach",
		hidden = true,
		name = "Detached reach",
		description = "How far a detached effect reaches from where it started: 100% is as far as it reaches on"
			+ " the mouse. Raise it for clicks anywhere to fill more of the screen",
		position = 22,
		section = prayerSpotlightSection
	)
	default int prayerSpotlightEffectReach()
	{
		return 100;
	}

	@Range(
		min = 25,
		max = 400
	)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "prayerSpotlightEffectSpread",
		hidden = true,
		name = "Detached spread",
		description = "How far apart a detached effect's particles, such as snowflakes or sparkles, are: 100% is"
			+ " normal, higher spreads them out with fewer over the same area",
		position = 23,
		section = prayerSpotlightSection
	)
	default int prayerSpotlightEffectSpread()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "prayerSpotlightAlways",
		hidden = true,
		name = "When not hovered",
		description = "Also spotlight this panel while the mouse is outside it",
		position = 24,
		section = prayerSpotlightSection
	)
	default boolean prayerSpotlightAlways()
	{
		return false;
	}

	@ConfigItem(
		keyName = "prayerSpotlightDeadOnly",
		hidden = true,
		name = "Only dead areas",
		description = "Only dim and light the dead areas, so slots keep their colors",
		position = 25,
		section = prayerSpotlightSection
	)
	default boolean prayerSpotlightDeadOnly()
	{
		return false;
	}

	@ConfigItem(
		keyName = "prayerSpotlightFalloff",
		hidden = true,
		name = "Falloff",
		description = "Past the falloff start, run the spotlight gradient in reverse: the dim fades out, then the falloff color fades in by the falloff end",
		position = 26,
		section = prayerSpotlightSection
	)
	default boolean prayerSpotlightFalloff()
	{
		return false;
	}


	@Range(min = 20, max = 4000)
	@ConfigItem(
		keyName = "prayerSpotlightFalloffDistance",
		hidden = true,
		name = "Falloff start",
		description = "Distance in pixels from the mouse where the dimming starts fading back out",
		position = 27,
		section = prayerSpotlightSection
	)
	default int prayerSpotlightFalloffDistance()
	{
		return 250;
	}

	@Range(min = 20, max = 4000)
	@ConfigItem(
		keyName = "prayerSpotlightFalloffEnd",
		hidden = true,
		name = "Falloff end",
		description = "Distance in pixels from the mouse where the dimming is gone; kept past the falloff start",
		position = 28,
		section = prayerSpotlightSection
	)
	default int prayerSpotlightFalloffEnd()
	{
		return 400;
	}

	@Alpha
	@ConfigItem(
		keyName = "prayerSpotlightFalloffColor",
		hidden = true,
		name = "Falloff color",
		description = "Color the falloff turns into by the falloff end, mirroring the glow at the mouse; kept beyond it."
			+ " Leave its alpha at 0 for the dimming to just fade out",
		position = 29,
		section = prayerSpotlightSection
	)
	default Color prayerSpotlightFalloffColor()
	{
		return new Color(0, 0, 0, 0);
	}

	@Range(
		min = 0,
		max = 3000
	)
	@ConfigItem(
		keyName = "prayerSpotlightFalloffColorEnd",
		hidden = true,
		name = "Falloff color end",
		description = "Distance in pixels from the mouse where the falloff color has faded back out, over the same"
			+ " length it took to fade in. 0 keeps the falloff color out to the panel's edges",
		position = 30,
		section = prayerSpotlightSection
	)
	default int prayerSpotlightFalloffColorEnd()
	{
		return 0;
	}

	@Alpha
	@ConfigItem(
		keyName = "prayerSpotlightFadeEndColor",
		hidden = true,
		name = "Fade end color",
		description = "Color the falloff color blends into at the falloff color end, kept beyond it. Fully see-through"
			+ " (the default) fades it out to nothing. Needs a falloff color end",
		position = 31,
		section = prayerSpotlightSection
	)
	default Color prayerSpotlightFadeEndColor()
	{
		return new Color(0, 0, 0, 0);
	}

	// Prayer: miss dots

	@ConfigItem(
		keyName = "prayerMissDots",
		hidden = true,
		name = "Miss dots",
		description = "Leave a small dot at every dead click, so you can see where your misses land",
		position = 0,
		section = prayerMissDotsSection
	)
	default boolean prayerMissDots()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "prayerMissDotColor",
		hidden = true,
		name = "Color",
		description = "Color of the miss dots",
		position = 1,
		section = prayerMissDotsSection
	)
	default Color prayerMissDotColor()
	{
		return new Color(255, 40, 40, 200);
	}

	@ConfigItem(
		keyName = "prayerMissDotShape",
		hidden = true,
		name = "Shape",
		description = "Shape of the miss dots",
		position = 2,
		section = prayerMissDotsSection
	)
	default MissDotShape prayerMissDotShape()
	{
		return MissDotShape.CIRCLE;
	}

	@ConfigItem(
		keyName = "prayerMissDotHollow",
		hidden = true,
		name = "Hollow",
		description = "Draw the miss dots as outlines instead of filled shapes",
		position = 2,
		section = prayerMissDotsSection
	)
	default boolean prayerMissDotHollow()
	{
		return false;
	}

	@Range(min = 2, max = 16)
	@ConfigItem(
		keyName = "prayerMissDotSize",
		hidden = true,
		name = "Size",
		description = "Size of the miss dots in pixels",
		position = 3,
		section = prayerMissDotsSection
	)
	default int prayerMissDotSize()
	{
		return 4;
	}

	@Units(Units.SECONDS)
	@Range(min = 1, max = 600)
	@ConfigItem(
		keyName = "prayerMissDotDuration",
		hidden = true,
		name = "Duration",
		description = "How long each miss dot stays; it fades over this time",
		position = 4,
		section = prayerMissDotsSection
	)
	default int prayerMissDotDuration()
	{
		return 15;
	}

	// Spellbook: hover

	@ConfigItem(
		keyName = "spellStyle",
		hidden = true,
		name = "Style",
		description = "How the hovered area is drawn",
		position = 0,
		section = spellHoverSection
	)
	default HighlightStyle spellStyle()
	{
		return HighlightStyle.BOTH;
	}

	@Alpha
	@ConfigItem(
		keyName = "spellOutlineColor",
		hidden = true,
		name = "Outline color",
		description = "Color of the hover outline",
		position = 1,
		section = spellHoverSection
	)
	default Color spellOutlineColor()
	{
		return new Color(255, 255, 255, 120);
	}

	@Alpha
	@ConfigItem(
		keyName = "spellFillColor",
		hidden = true,
		name = "Fill color",
		description = "Color of the hover fill",
		position = 2,
		section = spellHoverSection
	)
	default Color spellFillColor()
	{
		return new Color(98, 189, 125, 60);
	}

	@Range(min = 1, max = 4)
	@ConfigItem(
		keyName = "spellOutlineWidth",
		hidden = true,
		name = "Outline width",
		description = "Thickness of outlines in pixels",
		position = 3,
		section = spellHoverSection
	)
	default int spellOutlineWidth()
	{
		return 1;
	}

	@Range(max = 10)
	@ConfigItem(
		keyName = "spellCornerRadius",
		hidden = true,
		name = "Corner radius",
		description = "Rounds the corners of highlights and shading; 0 for square",
		position = 4,
		section = spellHoverSection
	)
	default int spellCornerRadius()
	{
		return 4;
	}

	@Units(Units.MILLISECONDS)
	@Range(max = 5000)
	@ConfigItem(
		keyName = "spellHoverFade",
		hidden = true,
		name = "Hover fade-out",
		description = "How long a highlight, including its tick cue, fades after the mouse leaves; 0 to disable",
		position = 5,
		section = spellHoverSection
	)
	default int spellHoverFade()
	{
		return 400;
	}

	@Units(Units.MILLISECONDS)
	@Range(max = 1000)
	@ConfigItem(
		keyName = "spellHoverDelay",
		hidden = true,
		name = "Hover delay",
		description = "How long the mouse must rest on an area before it is highlighted; 0 for instant",
		position = 6,
		section = spellHoverSection
	)
	default int spellHoverDelay()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "spellHideWhenMenuOpen",
		hidden = true,
		name = "Hide when right-click menu open",
		description = "Do not highlight while a right-click menu is open",
		position = 7,
		section = spellHoverSection
	)
	default boolean spellHideWhenMenuOpen()
	{
		return false;
	}

	@ConfigItem(
		keyName = "spellDiscoMode",
		hidden = true,
		name = "Disco mode",
		description = "Pick a random hover color every time the mouse moves to a new area",
		position = 8,
		section = spellHoverSection
	)
	default boolean spellDiscoMode()
	{
		return false;
	}

	@Units(Units.MILLISECONDS)
	@Range(max = 5000)
	@ConfigItem(
		keyName = "spellDiscoInterval",
		hidden = true,
		name = "Disco interval",
		description = "In disco mode, also pick a new color this often while the mouse rests; 0 to only change on a new area",
		position = 9,
		section = spellHoverSection
	)
	default int spellDiscoInterval()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "spellKeepIconColors",
		hidden = true,
		name = "Keep icon colors on hover",
		description = "Leave the icon out of the hover fill, so the fill only colors the slot background",
		position = 10,
		section = spellHoverSection
	)
	default boolean spellKeepIconColors()
	{
		return false;
	}

	@ConfigItem(
		keyName = "spellSlotBackground",
		hidden = true,
		name = "Slot background",
		description = "Color the background of every slot; icons stay on top",
		position = 11,
		section = spellHoverSection
	)
	default boolean spellSlotBackground()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "spellSlotBackgroundColor",
		hidden = true,
		name = "Slot background color",
		description = "Background color of the slots",
		position = 12,
		section = spellHoverSection
	)
	default Color spellSlotBackgroundColor()
	{
		return new Color(60, 60, 70, 90);
	}

	// Spellbook: spotlight

	@ConfigItem(
		keyName = "spellSpotlight",
		hidden = true,
		name = "Spotlight",
		description = "Light glow at the mouse that dims the panel outward, to show what is in reach",
		position = 0,
		section = spellSpotlightSection
	)
	default boolean spellSpotlight()
	{
		return false;
	}

	@Range(min = 20, max = 2000)
	@ConfigItem(
		keyName = "spellSpotlightRadius",
		hidden = true,
		name = "Radius",
		description = "Distance in pixels from the mouse where the dimming is full",
		position = 1,
		section = spellSpotlightSection
	)
	default int spellSpotlightRadius()
	{
		return 90;
	}

	@Alpha
	@ConfigItem(
		keyName = "spellSpotlightDim",
		hidden = true,
		name = "Dim color",
		description = "Color at the edge of the spotlight and beyond",
		position = 2,
		section = spellSpotlightSection
	)
	default Color spellSpotlightDim()
	{
		return new Color(0, 0, 0, 110);
	}

	@Alpha
	@ConfigItem(
		keyName = "spellSpotlightGlow",
		hidden = true,
		name = "Glow color",
		description = "Color of the glow at the mouse",
		position = 3,
		section = spellSpotlightSection
	)
	default Color spellSpotlightGlow()
	{
		return new Color(255, 255, 255, 45);
	}

	@Units(Units.PERCENT)
	@Range(max = 100)
	@ConfigItem(
		keyName = "spellSpotlightGlowSize",
		hidden = true,
		name = "Glow size",
		description = "How far the glow reaches, as a share of the radius; 0 for no glow, so only the dimming shows",
		position = 4,
		section = spellSpotlightSection
	)
	default int spellSpotlightGlowSize()
	{
		return 30;
	}

	@ConfigItem(
		keyName = "spellSpotlightShape",
		hidden = true,
		name = "Gradient",
		description = "How the spotlight fades: Smooth evenly, Soft eased in and out, Sharp with a crisp edge,"
			+ " or Banded in steps",
		position = 5,
		section = spellSpotlightSection
	)
	default SpotlightShape spellSpotlightShape()
	{
		return SpotlightShape.SMOOTH;
	}

	@ConfigItem(
		keyName = "spellSpotlightType",
		hidden = true,
		name = "Gradient type",
		description = "The spotlight's shape, or a pattern in it. Shapes: circle, ellipse, square, diamond, star,"
			+ " hexagon, cross, flower, and vertical or horizontal bands. Patterns on a circle: rings, a turning spiral,"
			+ " or a radar sweep. All of them carry on through the falloff",
		position = 6,
		section = spellSpotlightSection
	)
	default SpotlightType spellSpotlightType()
	{
		return SpotlightType.CIRCLE;
	}

	@ConfigItem(
		keyName = "spellSpotlightMotion",
		hidden = true,
		name = "Gradient motion",
		description = "Move the spotlight's shape: Spin turns it slowly, Wobble ripples its edge. Uses the effect speed",
		position = 7,
		section = spellSpotlightSection
	)
	default SpotlightMotion spellSpotlightMotion()
	{
		return SpotlightMotion.NONE;
	}

	@ConfigItem(
		keyName = "spellSpotlightEffect",
		hidden = true,
		name = "Effect",
		description = "An animation on the spotlight: Pulse breathes in and out, Ripples sends rings outward,"
			+ " and Sparkles, Embers, Orbit and Snow add particles",
		position = 8,
		section = spellSpotlightSection
	)
	default SpotlightEffect spellSpotlightEffect()
	{
		return SpotlightEffect.NONE;
	}

	@ConfigItem(
		keyName = "spellSpotlightEffectColor",
		hidden = true,
		name = "Effect on",
		description = "Where the effect shows. Overlay draws it on top within the spotlight radius and never changes"
			+ " the spotlight itself. Glow, Gradient and Whole keep it to the glow near the mouse, the gradient past"
			+ " the glow's edge, or everything, and can replace that part with Replace with effect",
		position = 9,
		section = spellSpotlightSection
	)
	default EffectArea spellSpotlightEffectColor()
	{
		return EffectArea.OVERLAY;
	}

	@ConfigItem(
		keyName = "spellSpotlightEffectReplace",
		hidden = true,
		name = "Replace with effect",
		description = "Draw the Effect on area only as the effect, in the gradient's own colors, instead of a smooth"
			+ " fill; with Gradient and Sparkles, the gradient is made of sparkles. Has no effect with Overlay",
		position = 10,
		section = spellSpotlightSection
	)
	default boolean spellSpotlightEffectReplace()
	{
		return false;
	}

	@ConfigItem(
		keyName = "spellSpotlightEffectCustom",
		hidden = true,
		name = "Custom effect color",
		description = "Use the effect color below. When off, each particle and ring takes the color the spotlight"
			+ " draws under it, so it shades from the glow into the gradient and falloff as it moves",
		position = 11,
		section = spellSpotlightSection
	)
	default boolean spellSpotlightEffectCustom()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "spellSpotlightEffectOverride",
		hidden = true,
		name = "Effect color",
		description = "Color of the effect, used when Custom effect color is on",
		position = 12,
		section = spellSpotlightSection
	)
	default Color spellSpotlightEffectOverride()
	{
		return new Color(255, 255, 255, 200);
	}

	@ConfigItem(
		keyName = "spellSpotlightEffectOnClick",
		hidden = true,
		name = "Effect on click",
		description = "Play the effect as a short burst when you click, instead of all the time",
		position = 13,
		section = spellSpotlightSection
	)
	default boolean spellSpotlightEffectOnClick()
	{
		return false;
	}

	@ConfigItem(
		keyName = "spellSpotlightClickResult",
		hidden = true,
		name = "Click result colors",
		description = "With Effect on click, color each burst by how the click went, using the three colors below",
		position = 14,
		section = spellSpotlightSection
	)
	default boolean spellSpotlightClickResult()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "spellSpotlightClickSuccessColor",
		hidden = true,
		name = "Good click color",
		description = "Burst color for a good click, one that did something, with Click result colors on",
		position = 15,
		section = spellSpotlightSection
	)
	default Color spellSpotlightClickSuccessColor()
	{
		return new Color(0, 255, 0, 200);
	}

	@Alpha
	@ConfigItem(
		keyName = "spellSpotlightClickMissColor",
		hidden = true,
		name = "Bad click color",
		description = "Burst color for a bad click, one that hit nothing, with Click result colors on",
		position = 16,
		section = spellSpotlightSection
	)
	default Color spellSpotlightClickMissColor()
	{
		return new Color(255, 60, 60, 200);
	}

	@Alpha
	@ConfigItem(
		keyName = "spellSpotlightClickRefusedColor",
		hidden = true,
		name = "Refused click color",
		description = "Burst color when the game refused the click, with Click result colors on",
		position = 17,
		section = spellSpotlightSection
	)
	default Color spellSpotlightClickRefusedColor()
	{
		return new Color(255, 220, 0, 200);
	}

	@Range(
		min = 100,
		max = 10000
	)
	@Units(Units.MILLISECONDS)
	@ConfigItem(
		keyName = "spellSpotlightEffectLength",
		hidden = true,
		name = "Effect length",
		description = "How long each click burst lasts with Effect on click, and each detached effect with Detach"
			+ " from mouse",
		position = 18,
		section = spellSpotlightSection
	)
	default int spellSpotlightEffectLength()
	{
		return 1200;
	}

	@Range(
		min = 10,
		max = 500
	)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "spellSpotlightEffectSpeed",
		hidden = true,
		name = "Effect speed",
		description = "How fast the effect moves: 100% is normal, lower is slower, higher is faster",
		position = 19,
		section = spellSpotlightSection
	)
	default int spellSpotlightEffectSpeed()
	{
		return 100;
	}

	@Range(
		min = 25,
		max = 400
	)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "spellSpotlightEffectSize",
		hidden = true,
		name = "Effect size",
		description = "How big the effect's particles, rings and trails are: 100% is normal",
		position = 20,
		section = spellSpotlightSection
	)
	default int spellSpotlightEffectSize()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "spellSpotlightEffectDetach",
		hidden = true,
		name = "Detach from mouse",
		description = "Let each effect run on its own where it started: where you clicked with Effect on click, else"
			+ " where the mouse entered the panel. Each runs for the effect length wherever the mouse goes, and new"
			+ " ones start alongside older ones",
		position = 21,
		section = spellSpotlightSection
	)
	default boolean spellSpotlightEffectDetach()
	{
		return false;
	}

	@Range(
		min = 25,
		max = 1000
	)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "spellSpotlightEffectReach",
		hidden = true,
		name = "Detached reach",
		description = "How far a detached effect reaches from where it started: 100% is as far as it reaches on"
			+ " the mouse. Raise it for clicks anywhere to fill more of the screen",
		position = 22,
		section = spellSpotlightSection
	)
	default int spellSpotlightEffectReach()
	{
		return 100;
	}

	@Range(
		min = 25,
		max = 400
	)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "spellSpotlightEffectSpread",
		hidden = true,
		name = "Detached spread",
		description = "How far apart a detached effect's particles, such as snowflakes or sparkles, are: 100% is"
			+ " normal, higher spreads them out with fewer over the same area",
		position = 23,
		section = spellSpotlightSection
	)
	default int spellSpotlightEffectSpread()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "spellSpotlightAlways",
		hidden = true,
		name = "When not hovered",
		description = "Also spotlight this panel while the mouse is outside it",
		position = 24,
		section = spellSpotlightSection
	)
	default boolean spellSpotlightAlways()
	{
		return false;
	}

	@ConfigItem(
		keyName = "spellSpotlightDeadOnly",
		hidden = true,
		name = "Only dead areas",
		description = "Only dim and light the dead areas, so slots keep their colors",
		position = 25,
		section = spellSpotlightSection
	)
	default boolean spellSpotlightDeadOnly()
	{
		return false;
	}

	@ConfigItem(
		keyName = "spellSpotlightFalloff",
		hidden = true,
		name = "Falloff",
		description = "Past the falloff start, run the spotlight gradient in reverse: the dim fades out, then the falloff color fades in by the falloff end",
		position = 26,
		section = spellSpotlightSection
	)
	default boolean spellSpotlightFalloff()
	{
		return false;
	}


	@Range(min = 20, max = 4000)
	@ConfigItem(
		keyName = "spellSpotlightFalloffDistance",
		hidden = true,
		name = "Falloff start",
		description = "Distance in pixels from the mouse where the dimming starts fading back out",
		position = 27,
		section = spellSpotlightSection
	)
	default int spellSpotlightFalloffDistance()
	{
		return 250;
	}

	@Range(min = 20, max = 4000)
	@ConfigItem(
		keyName = "spellSpotlightFalloffEnd",
		hidden = true,
		name = "Falloff end",
		description = "Distance in pixels from the mouse where the dimming is gone; kept past the falloff start",
		position = 28,
		section = spellSpotlightSection
	)
	default int spellSpotlightFalloffEnd()
	{
		return 400;
	}

	@Alpha
	@ConfigItem(
		keyName = "spellSpotlightFalloffColor",
		hidden = true,
		name = "Falloff color",
		description = "Color the falloff turns into by the falloff end, mirroring the glow at the mouse; kept beyond it."
			+ " Leave its alpha at 0 for the dimming to just fade out",
		position = 29,
		section = spellSpotlightSection
	)
	default Color spellSpotlightFalloffColor()
	{
		return new Color(0, 0, 0, 0);
	}

	@Range(
		min = 0,
		max = 3000
	)
	@ConfigItem(
		keyName = "spellSpotlightFalloffColorEnd",
		hidden = true,
		name = "Falloff color end",
		description = "Distance in pixels from the mouse where the falloff color has faded back out, over the same"
			+ " length it took to fade in. 0 keeps the falloff color out to the panel's edges",
		position = 30,
		section = spellSpotlightSection
	)
	default int spellSpotlightFalloffColorEnd()
	{
		return 0;
	}

	@Alpha
	@ConfigItem(
		keyName = "spellSpotlightFadeEndColor",
		hidden = true,
		name = "Fade end color",
		description = "Color the falloff color blends into at the falloff color end, kept beyond it. Fully see-through"
			+ " (the default) fades it out to nothing. Needs a falloff color end",
		position = 31,
		section = spellSpotlightSection
	)
	default Color spellSpotlightFadeEndColor()
	{
		return new Color(0, 0, 0, 0);
	}

	// Spellbook: miss dots

	@ConfigItem(
		keyName = "spellMissDots",
		hidden = true,
		name = "Miss dots",
		description = "Leave a small dot at every dead click, so you can see where your misses land",
		position = 0,
		section = spellMissDotsSection
	)
	default boolean spellMissDots()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "spellMissDotColor",
		hidden = true,
		name = "Color",
		description = "Color of the miss dots",
		position = 1,
		section = spellMissDotsSection
	)
	default Color spellMissDotColor()
	{
		return new Color(255, 40, 40, 200);
	}

	@ConfigItem(
		keyName = "spellMissDotShape",
		hidden = true,
		name = "Shape",
		description = "Shape of the miss dots",
		position = 2,
		section = spellMissDotsSection
	)
	default MissDotShape spellMissDotShape()
	{
		return MissDotShape.CIRCLE;
	}

	@ConfigItem(
		keyName = "spellMissDotHollow",
		hidden = true,
		name = "Hollow",
		description = "Draw the miss dots as outlines instead of filled shapes",
		position = 2,
		section = spellMissDotsSection
	)
	default boolean spellMissDotHollow()
	{
		return false;
	}

	@Range(min = 2, max = 16)
	@ConfigItem(
		keyName = "spellMissDotSize",
		hidden = true,
		name = "Size",
		description = "Size of the miss dots in pixels",
		position = 3,
		section = spellMissDotsSection
	)
	default int spellMissDotSize()
	{
		return 4;
	}

	@Units(Units.SECONDS)
	@Range(min = 1, max = 600)
	@ConfigItem(
		keyName = "spellMissDotDuration",
		hidden = true,
		name = "Duration",
		description = "How long each miss dot stays; it fades over this time",
		position = 4,
		section = spellMissDotsSection
	)
	default int spellMissDotDuration()
	{
		return 15;
	}
}
