package com.inventoryhover;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigDescriptor;
import net.runelite.client.config.ConfigItemDescriptor;
import net.runelite.client.config.ConfigManager;

/**
 * Copies the plugin's settings to the clipboard as readable JSON, grouped by
 * settings section, and applies settings pasted back from the clipboard. Uses
 * only the clipboard and the config manager, never files.
 */
@Singleton
class SettingsSharing
{
	private static final String EXPORT_MARKER = "click-helpers";
	private static final int EXPORT_VERSION = 1;
	private static final int MAX_IMPORT_CHARS = 64 * 1024;

	/** Config key holding the loadouts as a JSON object of name to exported settings. */
	private static final String LOADOUTS_KEY = "loadouts";

	/** Config key holding the name of the loadout last saved or loaded. */
	private static final String CURRENT_KEY = "currentLoadout";

	/** Settings that start something and must never be copied or pasted. */
	private static final Set<String> SKIPPED_KEYS = Set.of("copySettings", "pasteSettings", "practiceMode",
		// A shared loadout must never turn on network features for whoever loads it
		"communityLoadouts",
		// Personal choices that a loadout should not change for whoever loads it
		"syncTabSettings", "sidePanel");

	private static final List<String> TAB_PREFIXES = TabSettings.PREFIXES;

	private final InventoryHoverHighlightConfig config;
	private final ConfigManager configManager;

	/** Whether settings are being imported, which pauses tab sync. */
	private boolean importing;

	/** Keys of the tab settings, found on first use. */
	private Set<String> tabKeys;
	private final Gson gson;

	@Inject
	SettingsSharing(InventoryHoverHighlightConfig config, ConfigManager configManager, Gson gson)
	{
		this.config = config;
		this.configManager = configManager;
		this.gson = gson;
	}

	/** Copies the settings to the clipboard; returns a message for the player. */
	String copyToClipboard()
	{
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(exportJson()), null);
		return "Click Helpers settings exported to the clipboard.";
	}

	/** Applies settings from the clipboard; returns a message for the player. */
	String pasteFromClipboard()
	{
		String json;
		try
		{
			json = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
		}
		catch (Exception e)
		{
			return "Nothing to import: the clipboard has no text.";
		}

		if (json == null || json.length() > MAX_IMPORT_CHARS)
		{
			return "The clipboard does not hold Click Helpers settings.";
		}

		try
		{
			return "Imported " + importJson(json) + " Click Helpers settings.";
		}
		catch (RuntimeException e)
		{
			return "The clipboard does not hold Click Helpers settings.";
		}
	}

	/**
	 * Every setting of this plugin with its current value, grouped under its
	 * settings section name in the order the config panel shows them, so the
	 * export reads like the panel. Settings still at their default are included
	 * too, so loading the export sets every setting of its sections. With
	 * {@code onlySections}, only those sections are included.
	 */
	private JsonObject currentSettings(Set<String> onlySections)
	{
		ConfigDescriptor descriptor = configManager.getConfigDescriptor(config);
		Map<String, String> sectionNames = new HashMap<>();
		JsonObject settings = new JsonObject();
		for (SettingsSections.Section section : SettingsSections.ALL)
		{
			sectionNames.put(section.key, section.name);
			settings.add(section.name, new JsonObject());
		}

		descriptor.getItems().stream()
			.sorted(Comparator.comparingInt(ConfigItemDescriptor::position))
			.filter(item -> !SKIPPED_KEYS.contains(item.key()))
			.forEach(item ->
			{
				String section = sectionNames.getOrDefault(item.getItem().section(), "Other");
				if (onlySections != null && !onlySections.contains(section))
				{
					return;
				}
				String value = effectiveValue(item);
				if (value == null)
				{
					return;
				}
				if (!settings.has(section))
				{
					settings.add(section, new JsonObject());
				}
				settings.getAsJsonObject(section).addProperty(item.key(), value);
			});

		// Leave out sections with nothing in them
		for (String section : new ArrayList<>(settings.keySet()))
		{
			if (settings.getAsJsonObject(section).size() == 0)
			{
				settings.remove(section);
			}
		}
		return settings;
	}

	/** Names of the settings sections that can be saved, in config panel order. */
	List<String> sectionNames()
	{
		return new ArrayList<>(currentSettings(null).keySet());
	}

	/** Whether every setting's default has been stored, so each one can be read like a changed one. */
	private boolean defaultsStored;

	/**
	 * A setting's current value as stored text. Defaults are stored once with
	 * RuneLite's own call, which only fills in settings that were never changed.
	 */
	private String effectiveValue(ConfigItemDescriptor item)
	{
		if (!defaultsStored)
		{
			configManager.setDefaultConfiguration(config, false);
			defaultsStored = true;
		}
		String value = configManager.getConfiguration(InventoryHoverHighlightConfig.GROUP, item.key());
		if (value == null)
		{
			// Reset since the defaults were stored, so store them again
			configManager.setDefaultConfiguration(config, false);
		}
		return value != null ? value : configManager.getConfiguration(InventoryHoverHighlightConfig.GROUP, item.key());
	}


	/** Copies a changed tab setting to the same setting of the other tabs; other keys are ignored. */
	void syncTabSetting(String key, String value)
	{
		if (importing)
		{
			return;
		}
		String prefix = tabPrefixOf(key);
		if (prefix == null)
		{
			return;
		}

		String suffix = key.substring(prefix.length());
		for (String other : TAB_PREFIXES)
		{
			String target = other + suffix;
			// Stop when the value already matches, so the change events of the copies end here
			if (other.equals(prefix) || Objects.equals(value, configManager.getConfiguration(InventoryHoverHighlightConfig.GROUP, target)))
			{
				continue;
			}
			if (value == null)
			{
				configManager.unsetConfiguration(InventoryHoverHighlightConfig.GROUP, target);
			}
			else
			{
				configManager.setConfiguration(InventoryHoverHighlightConfig.GROUP, target, value);
			}
		}
	}

	/** Copies every inventory tab setting to the other tabs. */
	void syncAllTabs()
	{
		for (String key : tabKeys())
		{
			if (key.startsWith(TabSettings.INVENTORY))
			{
				syncTabSetting(key, configManager.getConfiguration(InventoryHoverHighlightConfig.GROUP, key));
			}
		}
	}

	/** The tab prefix of a tab setting key, or null for any other key. */
	private String tabPrefixOf(String key)
	{
		if (!tabKeys().contains(key))
		{
			return null;
		}
		for (String prefix : TAB_PREFIXES)
		{
			if (key.startsWith(prefix))
			{
				return prefix;
			}
		}
		return null;
	}

	/** Keys of every setting in a tab section. Matched by section, since general keys like "prayers" share a prefix. */
	private Set<String> tabKeys()
	{
		if (tabKeys == null)
		{
			Set<String> sections = TabSettings.SECTIONS;
			tabKeys = configManager.getConfigDescriptor(config).getItems().stream()
				.filter(item -> sections.contains(item.getItem().section()))
				.map(ConfigItemDescriptor::key)
				.collect(Collectors.toSet());
		}
		return tabKeys;
	}

	private Set<String> settingKeys()
	{
		return configManager.getConfigDescriptor(config).getItems().stream()
			.map(ConfigItemDescriptor::key)
			.filter(key -> !SKIPPED_KEYS.contains(key))
			.collect(Collectors.toSet());
	}

	/** Sections randomize leaves alone: the click chart and the import/export switches. */
	private static final Set<String> NOT_RANDOMIZED_SECTIONS = Set.of(InventoryHoverHighlightConfig.chartSection,
		InventoryHoverHighlightConfig.shareSection);

	/**
	 * Sets every on/off, number, color and choice setting to a random value,
	 * with each tab randomized on its own; returns a message. The click chart,
	 * import/export switches, hotkeys, text lists, the miss outline width and
	 * hidden settings stay.
	 */
	String randomizeAll()
	{
		Random random = new Random();
		// Sync would copy each tab's random values over the others, so turn it off first
		configManager.setConfiguration(InventoryHoverHighlightConfig.GROUP, "syncTabSettings", false);
		int changed = 0;
		for (ConfigItemDescriptor item : configManager.getConfigDescriptor(config).getItems())
		{
			// Practice settings are kept as they are; the side panel's practice tab sets them
			// Off with empty inventory would look like the plugin broke, so it is left as it is
			// A thick miss outline hides the slot it marks, so its width is left as it is too
			if (SKIPPED_KEYS.contains(item.key()) || item.key().equals("hideWhenEmpty")
				|| item.key().equals("missOutlineWidth")
				|| item.key().startsWith("practice")
				|| NOT_RANDOMIZED_SECTIONS.contains(item.getItem().section()))
			{
				continue;
			}

			Object value;
			Type type = item.getType();
			if (type == boolean.class || type == Boolean.class)
			{
				value = random.nextBoolean();
			}
			else if (type == int.class || type == Integer.class)
			{
				int min = item.getRange() == null ? 0 : item.getRange().min();
				int max = item.getRange() == null ? 100 : Math.min(item.getRange().max(), 100_000);
				value = min + random.nextInt(Math.max(1, max - min + 1));
			}
			else if (type == Color.class)
			{
				value = new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256),
					item.getAlpha() == null ? 255 : 40 + random.nextInt(216));
			}
			else if (type instanceof Class && ((Class<?>) type).isEnum())
			{
				Object[] options = ((Class<?>) type).getEnumConstants();
				value = options[random.nextInt(options.length)];
			}
			else
			{
				continue;
			}
			configManager.setConfiguration(InventoryHoverHighlightConfig.GROUP, item.key(), value);
			changed++;
		}
		return "Randomized " + changed + " settings and turned off tab sync. Load a loadout to go back.";
	}

	// Loadouts: named snapshots of every setting, kept in the plugin's own config, never in files

	/** Loadout names, in the order they were saved. */
	List<String> loadoutNames()
	{
		return new ArrayList<>(loadouts().keySet());
	}

	/** Saves every current setting under a name, replacing a loadout with that name; returns a message. */
	String saveLoadout(String name)
	{
		return saveLoadout(name, null);
	}

	/**
	 * Saves the current settings of some sections under a name, replacing a
	 * loadout with that name; returns a message. Loading it later only changes
	 * those sections. All sections when {@code sections} is null.
	 */
	String saveLoadout(String name, Set<String> sections)
	{
		name = name == null ? "" : name.trim();
		if (name.isEmpty())
		{
			return "Type a name for the loadout first.";
		}
		JsonObject all = loadouts();
		boolean replaced = all.has(name);
		if (sections != null && sections.isEmpty())
		{
			return "Pick at least one section to save.";
		}
		all.add(name, gson.fromJson(exportJson(sections), JsonObject.class));
		configManager.setConfiguration(InventoryHoverHighlightConfig.GROUP, LOADOUTS_KEY, gson.toJson(all));
		configManager.setConfiguration(InventoryHoverHighlightConfig.GROUP, CURRENT_KEY, name);
		String scope = sections == null ? "" : " with " + sections.size() + (sections.size() == 1 ? " section" : " sections");
		return (replaced ? "Updated loadout " : "Saved loadout ") + name + scope + ".";
	}

	/** Applies a saved loadout; returns a message. */
	String loadLoadout(String name)
	{
		return loadLoadout(name, null);
	}

	/** Applies some sections of a saved loadout, or all of them when {@code sections} is null; returns a message. */
	String loadLoadout(String name, Set<String> sections)
	{
		JsonObject all = loadouts();
		if (name == null || !all.has(name))
		{
			return "Pick a loadout to load.";
		}
		int applied = importJson(onlySections(gson.toJson(all.get(name)), sections));
		configManager.setConfiguration(InventoryHoverHighlightConfig.GROUP, CURRENT_KEY, name);
		return "Loaded loadout " + name + " (" + applied + " settings).";
	}

	/** Deletes a saved loadout; returns a message. */
	String deleteLoadout(String name)
	{
		JsonObject all = loadouts();
		if (name == null || all.remove(name) == null)
		{
			return "Pick a loadout to delete.";
		}
		configManager.setConfiguration(InventoryHoverHighlightConfig.GROUP, LOADOUTS_KEY, gson.toJson(all));
		if (name.equals(currentLoadout()))
		{
			configManager.unsetConfiguration(InventoryHoverHighlightConfig.GROUP, CURRENT_KEY);
		}
		return "Deleted loadout " + name + ".";
	}

	/** The loadout last saved or loaded, or null. */
	String currentLoadout()
	{
		String name = configManager.getConfiguration(InventoryHoverHighlightConfig.GROUP, CURRENT_KEY);
		return name != null && loadouts().has(name) ? name : null;
	}

	/** The saved settings of a loadout as compact JSON, or null when there is no such loadout. */
	String loadoutJson(String name)
	{
		JsonObject all = loadouts();
		return name != null && all.has(name) ? gson.toJson(all.get(name)) : null;
	}

	/**
	 * Saves exported settings, such as a community loadout, as a new loadout
	 * without applying them. Picks a free name based on the one given; returns
	 * the name used.
	 */
	String addLoadout(String name, String json)
	{
		JsonObject settings = json == null || json.length() > MAX_IMPORT_CHARS ? null : gson.fromJson(json, JsonObject.class);
		if (settings == null || !settings.has("plugin") || !EXPORT_MARKER.equals(settings.get("plugin").getAsString()))
		{
			throw new JsonParseException("Not Click Helpers settings");
		}
		JsonObject all = loadouts();
		String base = name == null || name.trim().isEmpty() ? "Community loadout" : name.trim();
		String free = base;
		for (int i = 2; all.has(free); i++)
		{
			free = base + " (" + i + ")";
		}
		all.add(free, settings);
		configManager.setConfiguration(InventoryHoverHighlightConfig.GROUP, LOADOUTS_KEY, gson.toJson(all));
		return free;
	}

	private JsonObject loadouts()
	{
		String json = configManager.getConfiguration(InventoryHoverHighlightConfig.GROUP, LOADOUTS_KEY);
		try
		{
			JsonObject all = json == null ? null : gson.fromJson(json, JsonObject.class);
			return all == null ? new JsonObject() : all;
		}
		catch (JsonParseException e)
		{
			return new JsonObject();
		}
	}

	/**
	 * A readable list of the settings in exported JSON, grouped by section, with
	 * each setting's name from the config panel. Settings this version does not
	 * know are counted, since importing skips them.
	 */
	String describe(String json)
	{
		JsonObject root;
		try
		{
			root = gson.fromJson(json, JsonObject.class);
		}
		catch (JsonParseException e)
		{
			return "These settings could not be read.";
		}
		if (root == null || !root.has("settings") || !root.get("settings").isJsonObject())
		{
			return "These settings could not be read.";
		}

		Map<String, ConfigItemDescriptor> items = new HashMap<>();
		for (ConfigItemDescriptor item : configManager.getConfigDescriptor(config).getItems())
		{
			items.put(item.key(), item);
		}

		StringBuilder out = new StringBuilder();
		int unknown = 0;
		for (Map.Entry<String, JsonElement> section : root.getAsJsonObject("settings").entrySet())
		{
			if (!section.getValue().isJsonObject())
			{
				continue;
			}
			StringBuilder lines = new StringBuilder();
			for (Map.Entry<String, JsonElement> setting : section.getValue().getAsJsonObject().entrySet())
			{
				ConfigItemDescriptor item = items.get(setting.getKey());
				if (item == null || SKIPPED_KEYS.contains(setting.getKey()) || !setting.getValue().isJsonPrimitive())
				{
					unknown++;
					continue;
				}
				lines.append("  ").append(item.name()).append(": ")
					.append(readable(item, setting.getValue().getAsString())).append('\n');
			}
			if (lines.length() > 0)
			{
				out.append(section.getKey()).append('\n').append(lines).append('\n');
			}
		}
		if (unknown > 0)
		{
			out.append(unknown).append(" settings this version does not have will be skipped.");
		}
		return out.length() == 0 ? "This loadout sets nothing." : out.toString().trim();
	}

	/** A stored setting value as the config panel would show it. */
	private static String readable(ConfigItemDescriptor item, String value)
	{
		Type type = item.getType();
		if (type == boolean.class || type == Boolean.class)
		{
			return Boolean.parseBoolean(value) ? "On" : "Off";
		}
		if (type == Color.class)
		{
			try
			{
				Color color = new Color((int) Long.parseLong(value), true);
				return String.format("#%02X%02X%02X, %d%% opaque", color.getRed(), color.getGreen(), color.getBlue(),
					Math.round(color.getAlpha() * 100 / 255f));
			}
			catch (NumberFormatException e)
			{
				return value;
			}
		}
		if (type instanceof Class && ((Class<?>) type).isEnum())
		{
			String words = value.replace('_', ' ').toLowerCase();
			return words.isEmpty() ? value : Character.toUpperCase(words.charAt(0)) + words.substring(1);
		}
		return value;
	}

	/**
	 * Exported settings cut down to some sections, or unchanged when
	 * {@code sections} is null. Settings outside any section are dropped too.
	 */
	String onlySections(String json, Set<String> sections)
	{
		if (json == null || sections == null)
		{
			return json;
		}
		JsonObject root = gson.fromJson(json, JsonObject.class);
		if (root == null || !root.has("settings") || !root.get("settings").isJsonObject())
		{
			return json;
		}
		JsonObject kept = new JsonObject();
		for (Map.Entry<String, JsonElement> section : root.getAsJsonObject("settings").entrySet())
		{
			if (section.getValue().isJsonObject() && sections.contains(section.getKey()))
			{
				kept.add(section.getKey(), section.getValue());
			}
		}
		root.add("settings", kept);
		return gson.toJson(root);
	}

	/** Applies settings from a published loadout; returns how many were applied. Unknown keys are skipped. */
	int importShared(String json, Set<String> sections)
	{
		return importShared(onlySections(json, sections));
	}

	int importShared(String json)
	{
		if (json == null || json.length() > MAX_IMPORT_CHARS)
		{
			throw new JsonParseException("Not Click Helpers settings");
		}
		return importJson(json);
	}

	private String exportJson()
	{
		return exportJson(null);
	}

	private String exportJson(Set<String> sections)
	{
		JsonObject root = new JsonObject();
		root.addProperty("plugin", EXPORT_MARKER);
		root.addProperty("version", EXPORT_VERSION);
		root.add("settings", currentSettings(sections));
		return gson.newBuilder().setPrettyPrinting().create().toJson(root);
	}

	/**
	 * Applies exported settings; returns how many were applied. Settings may be
	 * grouped by section or flat. Unknown keys are skipped.
	 */
	private int importJson(String json)
	{
		JsonObject root = gson.fromJson(json, JsonObject.class);
		if (root == null || !root.has("plugin") || !EXPORT_MARKER.equals(root.get("plugin").getAsString())
			|| !root.has("settings") || !root.get("settings").isJsonObject())
		{
			throw new JsonParseException("Not Click Helpers settings");
		}

		Map<String, String> values = new HashMap<>();
		for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("settings").entrySet())
		{
			if (entry.getValue().isJsonObject())
			{
				for (Map.Entry<String, JsonElement> inner : entry.getValue().getAsJsonObject().entrySet())
				{
					if (inner.getValue().isJsonPrimitive())
					{
						values.put(inner.getKey(), inner.getValue().getAsString());
					}
				}
			}
			else if (entry.getValue().isJsonPrimitive())
			{
				values.put(entry.getKey(), entry.getValue().getAsString());
			}
		}

		Set<String> known = settingKeys();
		int applied = 0;
		// Each setting's change event would otherwise copy it over the other tabs before their own values land
		importing = true;
		try
		{
			for (Map.Entry<String, String> entry : values.entrySet())
			{
				if (known.contains(entry.getKey()))
				{
					configManager.setConfiguration(InventoryHoverHighlightConfig.GROUP, entry.getKey(), entry.getValue());
					applied++;
				}
			}
		}
		finally
		{
			importing = false;
		}
		return applied;
	}
}
