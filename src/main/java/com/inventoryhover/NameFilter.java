package com.inventoryhover;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Checks names of published loadouts: length, characters and blocked words.
 * The plugin ships no word list; {@link CommunityLoadouts} downloads a public
 * one and hands it over with {@link #setBlockedWords}.
 */
final class NameFilter
{
	static final int MIN_LENGTH = 3;
	static final int MAX_LENGTH = 32;

	/** Characters a name may use. The database rules use the same pattern. */
	private static final Pattern ALLOWED = Pattern.compile("^[A-Za-z0-9 '_.!?()-]+$");

	/**
	 * Blocked words this long or longer also count inside a longer word, like
	 * a word glued to "loadout". Shorter ones count only as whole words, since
	 * they hide in harmless words.
	 */
	private static final int INSIDE_WORD_LENGTH = 6;

	/** Harmless words that look like blocked ones, such as place names; never blocked. */
	private static final Set<String> HARMLESS = Set.of("therapist", "therapists", "niger");

	/**
	 * Word endings taken off before the whole-word check, so plurals and similar
	 * forms match too, with the fewest letters each must leave. Endings that
	 * also end harmless words, like the "y" of "spicy", need a longer stem.
	 */
	private static final String[] ENDINGS = {"ing", "ers", "ies", "es", "er", "ed", "y", "s"};
	private static final int[] ENDING_MIN_STEM = {4, 4, 5, 5, 4, 5, 5, 4};

	private static volatile Set<String> wholeWords = Collections.emptySet();
	private static volatile List<String> insideWords = Collections.emptyList();
	private static volatile List<String> phrases = Collections.emptyList();

	private NameFilter()
	{
	}

	/** Whether a word list is loaded, so word checks can run. */
	static boolean hasBlockedWords()
	{
		return !wholeWords.isEmpty() || !phrases.isEmpty();
	}

	/** Loads the blocked words, one word or phrase per line. */
	static void setBlockedWords(List<String> lines)
	{
		Set<String> whole = new HashSet<>();
		List<String> inside = new ArrayList<>();
		List<String> multi = new ArrayList<>();
		for (String line : lines)
		{
			List<String> words = words(line);
			if (words.isEmpty())
			{
				continue;
			}
			if (words.size() > 1)
			{
				multi.add(String.join(" ", words));
				continue;
			}
			String word = words.get(0);
			whole.add(word);
			if (word.length() >= INSIDE_WORD_LENGTH)
			{
				inside.add(word);
			}
		}
		wholeWords = whole;
		insideWords = inside;
		phrases = multi;
	}

	/**
	 * Whether a name holds a blocked word: as a whole word, inside a longer
	 * word for long blocked words, as a phrase, or spaced out letter by letter.
	 * Numbers and symbols that stand in for letters and repeated letters are
	 * evened out first.
	 */
	static boolean isBlocked(String name)
	{
		List<String> words = words(name);
		String joined = " " + String.join(" ", words) + " ";
		String letters = String.join("", words);
		if (!HARMLESS.contains(letters) && wholeWords.contains(letters))
		{
			return true;
		}
		for (String word : words)
		{
			if (HARMLESS.contains(word))
			{
				continue;
			}
			if (wholeWords.contains(word) || wholeWords.contains(stem(word)))
			{
				return true;
			}
			for (String inside : insideWords)
			{
				if (word.contains(inside))
				{
					return true;
				}
			}
		}
		for (String phrase : phrases)
		{
			if (joined.contains(" " + phrase + " ") || letters.equals(phrase.replace(" ", "")))
			{
				return true;
			}
		}
		return false;
	}

	/** The lowercase words of a text, with stand-in letters swapped back and repeated letters merged. */
	private static List<String> words(String text)
	{
		List<String> words = new ArrayList<>();
		for (String word : unleet(text.toLowerCase()).split("[^a-z]+"))
		{
			if (!word.isEmpty())
			{
				words.add(collapse(word));
			}
		}
		return words;
	}

	/** A word without its first matching ending that leaves enough letters. */
	private static String stem(String word)
	{
		for (int i = 0; i < ENDINGS.length; i++)
		{
			if (word.endsWith(ENDINGS[i]) && word.length() - ENDINGS[i].length() >= ENDING_MIN_STEM[i])
			{
				return word.substring(0, word.length() - ENDINGS[i].length());
			}
		}
		return word;
	}

	/** Merges runs of the same letter, so "baaad" reads like "bad". */
	private static String collapse(String text)
	{
		StringBuilder out = new StringBuilder(text.length());
		for (char c : text.toCharArray())
		{
			if (out.length() == 0 || out.charAt(out.length() - 1) != c)
			{
				out.append(c);
			}
		}
		return out.toString();
	}

	/** Why a name cannot be published, or null when it can. Word checks need the word list loaded. */
	static String problem(String name)
	{
		String trimmed = name == null ? "" : name.trim();
		if (trimmed.length() < MIN_LENGTH || trimmed.length() > MAX_LENGTH)
		{
			return "Names are " + MIN_LENGTH + " to " + MAX_LENGTH + " characters long.";
		}
		if (!ALLOWED.matcher(trimmed).matches())
		{
			return "Names can use letters, numbers, spaces and ' _ . ! ? ( ) - only.";
		}
		if (isBlocked(trimmed))
		{
			return "That name is not allowed.";
		}
		return null;
	}

	/** Why a player cannot publish under this name, or null when they can. Only default loadouts say "default". */
	static String publishProblem(String name)
	{
		String problem = problem(name);
		if (problem == null && name.toLowerCase().contains("default"))
		{
			return "Names cannot use the word default, which marks the default loadouts.";
		}
		return problem;
	}

	/** Swaps numbers and symbols that stand in for letters, so "n4me" reads as "name". */
	private static String unleet(String text)
	{
		StringBuilder out = new StringBuilder(text.length());
		for (char c : text.toCharArray())
		{
			switch (c)
			{
				case '0': out.append('o'); break;
				case '1': case '!': out.append('i'); break;
				case '3': out.append('e'); break;
				case '4': case '@': out.append('a'); break;
				case '5': case '$': out.append('s'); break;
				case '7': out.append('t'); break;
				case '8': out.append('b'); break;
				default: out.append(c);
			}
		}
		return out.toString();
	}

	/** A fixed author name for an install id, like "Swift Goblin 42", so players never type one. */
	static String authorName(String uid)
	{
		String[] adjectives = {"Swift", "Brave", "Quiet", "Lucky", "Clever", "Steady", "Bold", "Calm",
			"Sharp", "Nimble", "Mighty", "Sly", "Keen", "Gentle", "Rapid", "Patient"};
		String[] creatures = {"Goblin", "Imp", "Cow", "Chicken", "Rat", "Spider", "Troll", "Gnome",
			"Dwarf", "Ogre", "Wolf", "Bat", "Frog", "Penguin", "Monk", "Wizard"};
		int hash = uid == null ? 0 : uid.hashCode() & 0x7fffffff;
		return adjectives[hash % adjectives.length] + " " + creatures[(hash / 16) % creatures.length]
			+ " " + (hash / 256) % 100;
	}
}
