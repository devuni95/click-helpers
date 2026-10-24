package com.inventoryhover;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import net.runelite.client.config.ConfigManager;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Loadouts that players publish for others, kept in a Firebase Realtime
 * Database. Each install signs in anonymously, which gives it an id but asks
 * the player for nothing, and may keep {@link #SLOTS} loadouts. The database
 * rules in firebase/database.rules.json enforce the slots, the name format and
 * bans. Names are checked against a public word list, downloaded on first use,
 * so the plugin ships no word list of its own.
 * Nothing is sent until the player turns the feature on and presses a button.
 */
@Singleton
class CommunityLoadouts
{
	/** Database URL from the Firebase console, like https://name-default-rtdb.firebaseio.com. Empty turns the feature off. */
	static final String DATABASE_URL = "https://click-helpers-loadouts-2b22f-default-rtdb.firebaseio.com";

	/** Web API key from the Firebase project settings. It is public by design. */
	static final String API_KEY = "AIzaSyDe8WwgmJ9zxI5cHXYB_0wudG3JjJ0uFsk";

	static final int SLOTS = 10;

	/**
	 * Owner id of the default loadouts, kept under /users like any other, so older
	 * versions list them too. Only admins write there: Firebase ids are 28
	 * characters, so no install can have this one.
	 */
	static final String DEFAULT_UID = "default";
	/**
	 * Author shown on the default loadouts, whose names also end in "(Default)" so
	 * older versions show them as defaults. Players cannot use the word in either.
	 */
	static final String DEFAULT_AUTHOR = "Default";

	private static final int MAX_LISTED = 200;
	/** The English list of the LDNOOBW project, pinned to one commit so it cannot change under us. */
	private static final String WORD_LIST_URL = "https://cdn.jsdelivr.net/gh/LDNOOBW/"
		+ "List-of-Dirty-Naughty-Obscene-and-Otherwise-Bad-Words@5faf2ba42d7b1c0977169ec3611df25a3c08eb13/en";
	private static final String SIGN_UP_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signUp";
	private static final String REFRESH_URL = "https://securetoken.googleapis.com/v1/token";
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	/** What a game name may look like; the database rules check the same. */
	private static final String PLAYER_NAME = "^[A-Za-z0-9 _-]{1,12}$";

	/** Config keys with no config item, so they never show in the panel or in exports. */
	private static final String UID_KEY = "communityUid";
	private static final String REFRESH_KEY = "communityRefreshToken";

	/** One published loadout. */
	static class Entry
	{
		final String uid;
		final int slot;
		final String name;
		final long created;
		final String settings;
		/** The game name the publisher chose to show, or null for the made-up author name. */
		final String playerName;
		/** Names of the settings sections the loadout sets, like "Click feedback". */
		final List<String> sections;
		/** How many other installs loaded it. */
		int installs;
		/** Whether this install is counted in {@link #installs}. */
		boolean installedByMe;

		Entry(String uid, int slot, String name, long created, String settings, String playerName, List<String> sections)
		{
			this.playerName = playerName;
			this.sections = sections;
			this.uid = uid;
			this.slot = slot;
			this.name = name;
			this.created = created;
			this.settings = settings;
		}

		String author()
		{
			if (isDefault())
			{
				return DEFAULT_AUTHOR;
			}
			return playerName != null ? playerName : NameFilter.authorName(uid);
		}

		/** Whether it is one of the default loadouts that ship with the plugin's database. */
		boolean isDefault()
		{
			return DEFAULT_UID.equals(uid);
		}

		@Override
		public String toString()
		{
			return name + " - " + author();
		}
	}

	/** The outcome of publishing. */
	static class Result
	{
		final String message;
		/** Whether the name was refused for its words, so the panel clears it. */
		final boolean clearName;

		Result(String message, boolean clearName)
		{
			this.message = message;
			this.clearName = clearName;
		}
	}

	private final OkHttpClient http;
	private final Gson gson;
	private final ConfigManager configManager;
	private final SettingsSharing sharing;
	private final ScheduledExecutorService executor;

	private String idToken;
	private long idTokenExpiresAt;

	@Inject
	CommunityLoadouts(OkHttpClient http, Gson gson, ConfigManager configManager, SettingsSharing sharing,
		ScheduledExecutorService executor)
	{
		this.http = http.newBuilder().callTimeout(15, TimeUnit.SECONDS).build();
		this.gson = gson;
		this.configManager = configManager;
		this.sharing = sharing;
		this.executor = executor;
	}

	static boolean isSetUp()
	{
		return !DATABASE_URL.isEmpty() && !API_KEY.isEmpty();
	}

	/** This install's id, or null before its first publish. */
	String uid()
	{
		return configManager.getConfiguration(InventoryHoverHighlightConfig.GROUP, UID_KEY);
	}

	/** Fetches the newest published loadouts; calls back on the Swing thread. */
	void list(Consumer<List<Entry>> done, Consumer<String> failed)
	{
		executor.execute(() ->
		{
			try
			{
				// Without the list, names are only checked for length and characters
				loadWordList();
				JsonElement users = getJson(DATABASE_URL + "/users.json");
				JsonElement installs = optionalJson(DATABASE_URL + "/installs.json");
				String me = uid();
				List<Entry> entries = new ArrayList<>();
				if (users != null && users.isJsonObject())
				{
					for (Map.Entry<String, JsonElement> user : users.getAsJsonObject().entrySet())
					{
						entries.addAll(entriesOf(user.getKey(), user.getValue(), false));
					}
				}
				countInstalls(entries, installs, me);
				List<Entry> defaults = new ArrayList<>();
				entries.removeIf(e -> e.isDefault() && defaults.add(e));
				defaults.sort(Comparator.comparingInt(e -> e.slot));
				entries.sort(Comparator.comparingLong((Entry e) -> e.created).reversed());
				List<Entry> newest = new ArrayList<>(entries.subList(0, Math.min(MAX_LISTED, entries.size())));
				// Defaults come first and whatever the cap, since they are few and meant to be found
				newest.addAll(0, defaults);
				SwingUtilities.invokeLater(() -> done.accept(newest));
			}
			catch (IOException | RuntimeException e)
			{
				SwingUtilities.invokeLater(() -> failed.accept("Could not load community loadouts. Try again later."));
			}
		});
	}

	/**
	 * Publishes exported settings under a name, replacing this install's
	 * loadout with the same name; calls back on the Swing thread. With a
	 * {@code playerName}, it shows as the author instead of the made-up name.
	 */
	void publish(String rawName, String settings, String rawPlayerName, Consumer<Result> done)
	{
		String name = rawName == null ? "" : rawName.trim();
		String problem = NameFilter.publishProblem(name);
		if (problem != null)
		{
			done.accept(new Result(problem, false));
			return;
		}
		// Game names use non-breaking spaces between words
		String playerName = rawPlayerName == null ? null : rawPlayerName.replace('\u00a0', ' ').trim();
		if (playerName != null && (!playerName.matches(PLAYER_NAME) || playerName.equalsIgnoreCase(DEFAULT_AUTHOR)))
		{
			done.accept(new Result("Could not read your game name. Tick Anonymous to publish under a made-up name.", false));
			return;
		}

		if (settings == null)
		{
			done.accept(new Result("Pick a saved loadout to publish.", false));
			return;
		}
		executor.execute(() ->
		{
			Result result;
			try
			{
				result = publishNow(name, settings, playerName);
			}
			catch (IOException | RuntimeException e)
			{
				result = new Result("Could not publish right now. Try again later.", false);
			}
			Result finished = result;
			SwingUtilities.invokeLater(() -> done.accept(finished));
		});
	}

	private Result publishNow(String name, String settings, String playerName) throws IOException
	{
		if (!loadWordList())
		{
			return new Result("Could not check the name right now. Try again later.", false);
		}
		if (NameFilter.isBlocked(name))
		{
			return new Result("That name is not allowed, so it was cleared. Type another name to publish under.", true);
		}

		String token = token();
		String uid = uid();
		// Hidden loadouts still take a slot, so hiding one never frees room for another
		List<Entry> mine = entriesOf(uid, getJson(DATABASE_URL + "/users/" + uid + ".json"), true);
		int slot = -1;
		for (Entry entry : mine)
		{
			if (entry.name.equalsIgnoreCase(name))
			{
				slot = entry.slot;
			}
		}
		// Replacing a loadout by name keeps its installs; a new one in a free slot starts at 0
		boolean fresh = slot == -1;
		for (int i = 0; slot == -1 && i < SLOTS; i++)
		{
			int free = i;
			if (mine.stream().noneMatch(e -> e.slot == free))
			{
				slot = free;
			}
		}
		if (slot == -1)
		{
			return new Result("You have " + SLOTS + " published loadouts. Delete one first.", false);
		}

		JsonObject body = new JsonObject();
		body.addProperty("name", name);
		JsonObject now = new JsonObject();
		now.addProperty(".sv", "timestamp");
		body.add("created", now);
		body.addProperty("settings", settings);
		if (playerName != null)
		{
			body.addProperty("author", playerName);
		}
		Request request = new Request.Builder()
			.url(authed(DATABASE_URL + "/users/" + uid + "/" + slot + ".json", token))
			.put(RequestBody.create(JSON, gson.toJson(body)))
			.build();
		try (Response response = http.newCall(request).execute())
		{
			if (response.code() == 401 || response.code() == 403)
			{
				return new Result("Publishing is closed right now, or this install may not publish.", false);
			}
			if (!response.isSuccessful())
			{
				return new Result("Could not publish right now. Try again later.", false);
			}
		}
		if (fresh)
		{
			clearInstalls(uid, slot, token);
		}
		String author = playerName != null ? playerName : NameFilter.authorName(uid);
		return new Result("Published " + name + " as " + author + sectionsNote(settings) + ".", false);
	}

	/** A note on which sections a loadout holds, when it holds only some of them. */
	private String sectionsNote(String settings)
	{
		List<String> sections = sectionsOf(settings);
		int all = sharing.sectionNames().size();
		if (sections.isEmpty() || sections.size() >= all)
		{
			return "";
		}
		String list = sections.size() <= 4 ? ": " + String.join(", ", sections) : "";
		return ", with only " + sections.size() + " of " + all + " sections" + list;
	}

	/** Deletes one of this install's loadouts; calls back on the Swing thread. */
	void delete(Entry entry, Consumer<String> done)
	{
		if (entry == null || !entry.uid.equals(uid()))
		{
			done.accept("You can only delete your own loadouts.");
			return;
		}

		executor.execute(() ->
		{
			String message;
			try
			{
				Request request = new Request.Builder()
					.url(authed(DATABASE_URL + "/users/" + entry.uid + "/" + entry.slot + ".json", token()))
					.delete()
					.build();
				boolean deleted;
				try (Response response = http.newCall(request).execute())
				{
					deleted = response.isSuccessful();
				}
				message = deleted ? "Deleted " + entry.name + "." : "Could not delete it right now.";
				if (deleted)
				{
					clearInstalls(entry.uid, entry.slot, token());
				}
			}
			catch (IOException | RuntimeException e)
			{
				message = "Could not delete it right now.";
			}
			String finished = message;
			SwingUtilities.invokeLater(() -> done.accept(finished));
		});
	}

	/** Applies a published loadout; returns a message. */
	String load(Entry entry)
	{
		return load(entry, null);
	}

	/** Applies some sections of a published loadout, or all of it when {@code sections} is null; returns a message. */
	String load(Entry entry, java.util.Set<String> sections)
	{
		if (entry == null)
		{
			return "Pick a loadout to load.";
		}
		try
		{
			return "Loaded " + entry.name + " (" + sharing.importShared(entry.settings, sections) + " settings).";
		}
		catch (RuntimeException e)
		{
			return "That loadout could not be read.";
		}
	}

	/**
	 * Counts this install as one that loaded the loadout, once per install, and
	 * never for your own loadouts. Calls back on the Swing thread when the count
	 * went up; failures are ignored, since the count is only a rough signal.
	 */
	void recordInstall(Entry entry, Runnable counted)
	{
		if (entry == null || entry.installedByMe || entry.uid.equals(uid()))
		{
			return;
		}
		executor.execute(() ->
		{
			try
			{
				String token = token();
				String me = uid();
				if (me == null || me.equals(entry.uid))
				{
					return;
				}
				Request request = new Request.Builder()
					.url(authed(DATABASE_URL + "/installs/" + entry.uid + "/" + entry.slot + "/" + me + ".json", token))
					.put(RequestBody.create(JSON, "true"))
					.build();
				try (Response response = http.newCall(request).execute())
				{
					if (!response.isSuccessful())
					{
						return;
					}
				}
				SwingUtilities.invokeLater(() ->
				{
					if (!entry.installedByMe)
					{
						entry.installedByMe = true;
						entry.installs++;
						counted.run();
					}
				});
			}
			catch (IOException | RuntimeException e)
			{
				// Not counted
			}
		});
	}

	/** Removes the install count of one of this install's slots, so a new loadout there starts at 0. */
	private void clearInstalls(String uid, int slot, String token)
	{
		Request request = new Request.Builder()
			.url(authed(DATABASE_URL + "/installs/" + uid + "/" + slot + ".json", token))
			.delete()
			.build();
		try (Response ignored = http.newCall(request).execute())
		{
			// Best effort: a stale count only means a new loadout in this slot starts above 0
		}
		catch (IOException e)
		{
			// Same
		}
	}

	/**
	 * Data the list can do without, such as download counts, or
	 * null when it cannot be read, so the published loadouts still show.
	 */
	private JsonElement optionalJson(String url)
	{
		try
		{
			return getJson(url);
		}
		catch (IOException | RuntimeException e)
		{
			return null;
		}
	}

	/** Fills in each loadout's download count, and whether this install ({@code me}) is one of them. */
	private static void countInstalls(List<Entry> entries, JsonElement installs, String me)
	{
		for (Entry entry : entries)
		{
			JsonObject installers = installersOf(installs, entry);
			entry.installs = installers == null ? 0 : installers.size();
			entry.installedByMe = installers != null && me != null && installers.has(me);
		}
	}

	/** The installer ids of one loadout in the downloaded installs tree, or null for none. */
	private static JsonObject installersOf(JsonElement installs, Entry entry)
	{
		if (installs == null || !installs.isJsonObject())
		{
			return null;
		}
		JsonElement slots = installs.getAsJsonObject().get(entry.uid);
		JsonElement slot = null;
		// Like users, slots come back as an array or as an object keyed by slot
		if (slots != null && slots.isJsonArray() && entry.slot < slots.getAsJsonArray().size())
		{
			slot = slots.getAsJsonArray().get(entry.slot);
		}
		else if (slots != null && slots.isJsonObject())
		{
			slot = slots.getAsJsonObject().get(String.valueOf(entry.slot));
		}
		return slot != null && slot.isJsonObject() ? slot.getAsJsonObject() : null;
	}

	/** Downloads the word list once; returns whether it is loaded. */
	private synchronized boolean loadWordList()
	{
		if (NameFilter.hasBlockedWords())
		{
			return true;
		}
		try (Response response = http.newCall(new Request.Builder().url(WORD_LIST_URL).build()).execute())
		{
			ResponseBody body = response.body();
			if (!response.isSuccessful() || body == null)
			{
				return false;
			}
			NameFilter.setBlockedWords(Arrays.asList(body.string().split("\\R")));
			return NameFilter.hasBlockedWords();
		}
		catch (IOException e)
		{
			return false;
		}
	}

	/** A current sign-in token, signing in anonymously the first time. */
	private synchronized String token() throws IOException
	{
		if (idToken != null && System.currentTimeMillis() < idTokenExpiresAt)
		{
			return idToken;
		}

		String refresh = configManager.getConfiguration(InventoryHoverHighlightConfig.GROUP, REFRESH_KEY);
		if (refresh != null)
		{
			JsonObject answer = refreshSignIn(refresh);
			if (answer == null)
			{
				// The account is gone, so start over with a new one and new slots
				refresh = null;
			}
			else
			{
				idToken = answer.get("id_token").getAsString();
				refresh = answer.get("refresh_token").getAsString();
				configManager.setConfiguration(InventoryHoverHighlightConfig.GROUP, UID_KEY, answer.get("user_id").getAsString());
				idTokenExpiresAt = expiry(answer.get("expires_in"));
			}
		}
		if (refresh == null)
		{
			JsonObject answer = post(new Request.Builder()
				.url(SIGN_UP_URL + "?key=" + API_KEY)
				.post(RequestBody.create(JSON, "{\"returnSecureToken\":true}"))
				.build());
			idToken = answer.get("idToken").getAsString();
			refresh = answer.get("refreshToken").getAsString();
			configManager.setConfiguration(InventoryHoverHighlightConfig.GROUP, UID_KEY, answer.get("localId").getAsString());
			idTokenExpiresAt = expiry(answer.get("expiresIn"));
		}
		configManager.setConfiguration(InventoryHoverHighlightConfig.GROUP, REFRESH_KEY, refresh);
		return idToken;
	}

	/**
	 * Swaps a saved refresh token for a new sign-in. Null when Firebase refuses
	 * the token, which happens when the account was deleted or disabled.
	 */
	private JsonObject refreshSignIn(String refresh) throws IOException
	{
		Request request = new Request.Builder()
			.url(REFRESH_URL + "?key=" + API_KEY)
			.post(new FormBody.Builder()
				.add("grant_type", "refresh_token")
				.add("refresh_token", refresh)
				.build())
			.build();
		try (Response response = http.newCall(request).execute())
		{
			ResponseBody body = response.body();
			if (response.code() == 400)
			{
				return null;
			}
			if (!response.isSuccessful() || body == null)
			{
				throw new IOException("Sign-in failed: " + response.code());
			}
			return gson.fromJson(body.string(), JsonObject.class);
		}
	}

	/** When a token runs out, a minute early to be safe. */
	private static long expiry(JsonElement seconds)
	{
		long life = seconds == null ? 3600 : Long.parseLong(seconds.getAsString());
		return System.currentTimeMillis() + (life - 60) * 1000;
	}

	private JsonObject post(Request request) throws IOException
	{
		try (Response response = http.newCall(request).execute())
		{
			ResponseBody body = response.body();
			if (!response.isSuccessful() || body == null)
			{
				throw new IOException("Sign-in failed: " + response.code());
			}
			return gson.fromJson(body.string(), JsonObject.class);
		}
	}

	private JsonElement getJson(String url) throws IOException
	{
		try (Response response = http.newCall(new Request.Builder().url(url).build()).execute())
		{
			ResponseBody body = response.body();
			if (!response.isSuccessful() || body == null)
			{
				throw new IOException("Request failed: " + response.code());
			}
			return gson.fromJson(body.string(), JsonElement.class);
		}
	}

	/** The section names in exported settings, or none when they cannot be read. */
	private List<String> sectionsOf(String settings)
	{
		List<String> sections = new ArrayList<>();
		try
		{
			JsonObject root = gson.fromJson(settings, JsonObject.class);
			if (root != null && root.has("settings") && root.get("settings").isJsonObject())
			{
				for (Map.Entry<String, JsonElement> section : root.getAsJsonObject("settings").entrySet())
				{
					if (section.getValue().isJsonObject())
					{
						sections.add(section.getKey());
					}
				}
			}
		}
		catch (RuntimeException e)
		{
			// Listed without sections
		}
		return sections;
	}

	/**
	 * The game name a loadout shows as its author, or null for the made-up name.
	 * The name cannot be checked against the game, so blocked words fall back too.
	 */
	private static String playerNameOf(JsonObject value)
	{
		JsonElement author = value.get("author");
		if (author == null || !author.isJsonPrimitive())
		{
			return null;
		}
		String name = author.getAsString();
		return name.matches(PLAYER_NAME) && !name.equalsIgnoreCase(DEFAULT_AUTHOR) && !NameFilter.isBlocked(name)
			? name : null;
	}

	private static HttpUrl authed(String url, String token)
	{
		return HttpUrl.parse(url).newBuilder().addQueryParameter("auth", token).build();
	}

	/**
	 * The loadouts under one install id. Unless asked for, skips hidden ones,
	 * which admins mark from the Firebase console, and any that fail the local
	 * name checks.
	 */
	private List<Entry> entriesOf(String uid, JsonElement slots, boolean all)
	{
		List<Entry> entries = new ArrayList<>();
		if (uid == null || slots == null)
		{
			return entries;
		}

		// Firebase returns keys 0 to 9 as an array, with nulls for empty slots, unless
		// few slots are used, when it returns an object keyed by slot
		Map<String, JsonElement> bySlot = new LinkedHashMap<>();
		if (slots.isJsonArray())
		{
			for (int i = 0; i < slots.getAsJsonArray().size(); i++)
			{
				bySlot.put(String.valueOf(i), slots.getAsJsonArray().get(i));
			}
		}
		else if (slots.isJsonObject())
		{
			for (Map.Entry<String, JsonElement> slot : slots.getAsJsonObject().entrySet())
			{
				bySlot.put(slot.getKey(), slot.getValue());
			}
		}

		for (Map.Entry<String, JsonElement> slot : bySlot.entrySet())
		{
			if (slot.getValue() == null || !slot.getValue().isJsonObject())
			{
				continue;
			}
			try
			{
				JsonObject value = slot.getValue().getAsJsonObject();
				String name = value.get("name").getAsString();
				boolean hidden = value.has("hidden") && value.get("hidden").getAsBoolean();
				if (!all && (hidden || NameFilter.problem(name) != null))
				{
					continue;
				}
				String settings = value.get("settings").getAsString();
				entries.add(new Entry(uid, Integer.parseInt(slot.getKey()), name,
					value.get("created").getAsLong(), settings, playerNameOf(value), sectionsOf(settings)));
			}
			catch (RuntimeException e)
			{
				// Skip anything malformed
			}
		}
		return entries;
	}
}
