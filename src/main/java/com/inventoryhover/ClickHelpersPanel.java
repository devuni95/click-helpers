package com.inventoryhover;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;

/**
 * Side panel with the settings Export and Import buttons, the switch practice
 * start button and a chart of practice progress across sessions.
 */
class ClickHelpersPanel extends PluginPanel
{
	private static final Color SWITCH_LINE = new Color(90, 170, 255);
	private static final Color ACCURACY_LINE = new Color(80, 200, 120);

	private final InventoryHoverHighlightPlugin plugin;
	private final ConfigManager configManager;
	private final ColorPickerManager colorPickerManager;

	private final JLabel shareStatus = new JLabel(" ");
	private final JComboBox<String> loadoutBox = new JComboBox<>();
	private final JLabel loadoutStatus = new JLabel(" ");
	private final JLabel communityStatus = new JLabel(" ");
	/** Config key with no config item for the Anonymous box, so it never shows in exports. */
	private static final String ANONYMOUS_KEY = "communityAnonymous";
	private final CommunityBrowser browser;
	private SettingsView settingsView;
	private final JTextField loadoutName = new JTextField();
	private final JButton sectionsButton = new JButton();
	private final JTextField publishName = new JTextField();
	/** Sections to publish, or null for all of them. */
	private Set<String> chosenSections;
	private final JButton practiceButton = new JButton();
	private final JLabel sessionLabel = new JLabel();
	private final ProgressChart chart = new ProgressChart();
	private final JPanel historyList = new JPanel();

	ClickHelpersPanel(InventoryHoverHighlightPlugin plugin, InventoryHoverHighlightConfig config,
		ConfigManager configManager, ColorPickerManager colorPickerManager)
	{
		this.plugin = plugin;
		this.configManager = configManager;
		this.colorPickerManager = colorPickerManager;
		this.browser = new CommunityBrowser(plugin, () -> plugin.communityProblem(false), this::refreshLoadouts);
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(8, 8, 8, 8));

		JPanel settingsTab = tabPage();
		JPanel practiceTab = tabPage();

		settingsTab.add(heading("Export / import"));
		JPanel shareButtons = new JPanel(new GridLayout(1, 2, 6, 0));
		JButton export = new JButton("Export");
		export.setToolTipText("Copy all Click Helpers settings to the clipboard as JSON");
		export.addActionListener(e -> shareStatus.setText(wrap(plugin.exportSettings())));
		JButton importButton = new JButton("Import");
		importButton.setToolTipText("Apply Click Helpers settings JSON from the clipboard");
		importButton.addActionListener(e -> shareStatus.setText(wrap(plugin.importSettings())));
		shareButtons.add(export);
		shareButtons.add(importButton);
		settingsTab.add(left(shareButtons));
		shareStatus.setFont(FontManager.getRunescapeSmallFont());
		settingsTab.add(left(shareStatus));

		settingsTab.add(heading("Loadouts"));
		settingsTab.add(left(fieldLabel("Saved loadouts")));
		loadoutBox.setToolTipText("Your saved loadouts; the one you last saved or loaded is picked");
		settingsTab.add(left(loadoutBox));
		JPanel loadoutButtons = new JPanel(new GridLayout(1, 2, 6, 0));
		JButton load = new JButton("Load");
		load.setToolTipText("Apply the picked loadout");
		JCheckBox clearOthers = new JCheckBox("Clear other settings");
		clearOthers.setToolTipText("When loading, put every setting the loadout does not set back to its default");
		load.addActionListener(e ->
			loadoutStatus.setText(wrap(plugin.loadouts().loadLoadout((String) loadoutBox.getSelectedItem(), null,
				clearOthers.isSelected()))));
		JButton delete = new JButton("Delete");
		delete.setToolTipText("Delete the picked loadout");
		delete.addActionListener(e ->
		{
			String picked = (String) loadoutBox.getSelectedItem();
			if (picked == null)
			{
				loadoutStatus.setText(wrap("Pick a loadout to delete."));
				return;
			}
			int answer = JOptionPane.showConfirmDialog(this, "Delete the loadout " + picked + "?\nThis cannot be undone.",
				"Delete loadout", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (answer == JOptionPane.YES_OPTION)
			{
				loadoutStatus.setText(wrap(plugin.loadouts().deleteLoadout(picked)));
				refreshLoadouts(null);
			}
		});
		loadoutButtons.add(load);
		loadoutButtons.add(delete);
		settingsTab.add(left(loadoutButtons));
		settingsTab.add(left(clearOthers));

		settingsTab.add(left(fieldLabel("Save current settings as")));
		loadoutName.setToolTipText("Fills in with the picked loadout, so saving updates it; type a new name for a new loadout");
		settingsTab.add(left(loadoutName));
		JButton saveLoadout = new JButton("Save");
		saveLoadout.setToolTipText("Save your current settings under this name, replacing a loadout with the same name");
		saveLoadout.addActionListener(e ->
		{
			loadoutStatus.setText(wrap(plugin.loadouts().saveLoadout(loadoutName.getText())));
			refreshLoadouts(loadoutName.getText().trim());
		});
		settingsTab.add(left(saveLoadout));
		loadoutStatus.setFont(FontManager.getRunescapeSmallFont());
		settingsTab.add(left(loadoutStatus));
		loadoutBox.addActionListener(e ->
		{
			Object picked = loadoutBox.getSelectedItem();
			if (picked != null)
			{
				loadoutName.setText((String) picked);
				publishName.setText((String) picked);
			}
		});
		refreshLoadouts(plugin.loadouts().currentLoadout());

		buildCommunitySection(settingsTab);

		settingsTab.add(heading("Feeling lucky"));
		JButton randomize = new JButton("Randomize all settings");
		randomize.setToolTipText("Random values for every setting except the click chart, hotkeys and text lists; turns off tab sync");
		randomize.addActionListener(e ->
		{
			int answer = JOptionPane.showConfirmDialog(this,
				"Randomize all Click Helpers settings?\nSave a loadout first to keep your current ones.",
				"Randomize settings", JOptionPane.OK_CANCEL_OPTION);
			if (answer == JOptionPane.OK_OPTION)
			{
				loadoutStatus.setText(wrap(plugin.loadouts().randomizeAll()));
			}
		});
		settingsTab.add(left(randomize));

		practiceTab.add(heading("Switch practice"));
		practiceButton.addActionListener(e -> plugin.togglePractice());
		practiceTab.add(left(practiceButton));
		sessionLabel.setFont(FontManager.getRunescapeSmallFont());
		practiceTab.add(left(sessionLabel));
		practiceTab.add(left(chart));

		JPanel legend = new JPanel(new GridLayout(1, 2));
		legend.add(legendLabel("Switch time", SWITCH_LINE));
		legend.add(legendLabel("Accuracy", ACCURACY_LINE));
		practiceTab.add(left(legend));

		historyList.setLayout(new BoxLayout(historyList, BoxLayout.Y_AXIS));
		practiceTab.add(left(historyList));

		JButton clear = new JButton("Clear history");
		clear.addActionListener(e -> plugin.clearPracticeHistory());
		practiceTab.add(left(clear));

		practiceTab.add(heading("Practice settings"));
		JPanel settings = new JPanel(new GridLayout(0, 2, 4, 4));
		spinner(settings, "Slots per switch", "practiceTargets", config.practiceTargets(), 1, 28, 1);
		spinner(settings, "Pause (ms)", "practiceRoundDelay", config.practiceRoundDelay(), 0, 5000, 100);
		check(settings, "Random extra pause", "practiceRandomDelay", config.practiceRandomDelay());
		label(settings, "Time limit (s, 0 off)");
		JSpinner timeLimit = new JSpinner(new SpinnerNumberModel(config.practiceTimeLimitSeconds(), 0.0, 30.0, 0.1));
		timeLimit.addChangeListener(e -> set("practiceTimeLimitSeconds", timeLimit.getValue()));
		settings.add(timeLimit);
		check(settings, "Timer bar", "practiceTimerBar", config.practiceTimerBar());
		spinner(settings, "Rounds (0 endless)", "practiceRounds", config.practiceRounds(), 0, 500, 5);
		check(settings, "Click in order", "practiceOrdered", config.practiceOrdered());
		check(settings, "Number targets", "practiceNumbers", config.practiceNumbers());
		combo(settings, "Stats", "practiceStatsMode", config.practiceStatsMode());
		color(settings, "Target color", "practiceTargetColor", config.practiceTargetColor());
		color(settings, "Hit color", "practiceHitColor", config.practiceHitColor());
		practiceTab.add(left(settings));

		// Settings tools and practice live on separate tabs so they never mix
		JTabbedPane tabs = new JTabbedPane();
		settingsView = new SettingsView(configManager, config, colorPickerManager);
		tabs.addTab("Settings", top(settingsView));
		tabs.addTab("Loadouts", top(settingsTab));
		tabs.addTab("Practice", top(practiceTab));
		add(tabs, BorderLayout.NORTH);
		update(false, null, new ArrayList<>());
	}

	/** Shows a setting's new value in the Settings tab; safe to call from any thread. */
	void settingChanged(String key)
	{
		if (settingsView != null)
		{
			settingsView.settingChanged(key);
		}
	}

	/** Shows the practice state; call on the Swing thread. */
	void update(boolean practicing, SwitchPractice.Summary current, List<SwitchPractice.Summary> history)
	{
		practiceButton.setText(practicing ? "Stop practice" : "Start practice");
		if (current == null)
		{
			sessionLabel.setText(practicing ? "Click the lit slots in your inventory" : "Not practicing");
		}
		else
		{
			sessionLabel.setText(wrap(String.format("%d rounds: %.0f ms average, best %d ms, %.1f ticks,"
					+ " %.0f%% in 1 tick, %.0f%% accuracy, %d timeouts",
				current.rounds, current.switchMillis, current.bestSwitchMillis, current.ticks, current.oneTickRate,
				current.accuracy, current.timeouts)));
		}

		List<SwitchPractice.Summary> points = new ArrayList<>(history);
		if (current != null)
		{
			points.add(current);
		}
		chart.setPoints(points);

		historyList.removeAll();
		SimpleDateFormat format = new SimpleDateFormat("MMM d HH:mm");
		for (int i = history.size() - 1; i >= Math.max(0, history.size() - 8); i--)
		{
			SwitchPractice.Summary s = history.get(i);
			JLabel row = new JLabel(String.format("%s  %d rnd  %.0f ms  %.0f%% 1t  %.0f%%",
				format.format(new Date(s.time)), s.rounds, s.switchMillis, s.oneTickRate, s.accuracy));
			row.setFont(FontManager.getRunescapeSmallFont());
			row.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			historyList.add(row);
		}
		revalidate();
		repaint();
	}

	/** Refills the loadout dropdown, selecting {@code select} when given. */
	void refreshLoadouts(String select)
	{
		loadoutBox.removeAllItems();
		for (String name : plugin.loadouts().loadoutNames())
		{
			loadoutBox.addItem(name);
		}
		if (select != null && !select.isEmpty())
		{
			loadoutBox.setSelectedItem(select);
		}
		Object picked = loadoutBox.getSelectedItem();
		loadoutName.setText(picked == null ? "" : (String) picked);
		publishName.setText(picked == null ? "" : (String) picked);
	}

	private void set(String key, Object value)
	{
		configManager.setConfiguration(InventoryHoverHighlightConfig.GROUP, key, value);
	}

	private static JLabel label(JPanel settings, String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeSmallFont());
		settings.add(label);
		return label;
	}

	private void spinner(JPanel settings, String text, String key, int value, int min, int max, int step)
	{
		label(settings, text);
		JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, min, max, step));
		spinner.addChangeListener(e -> set(key, spinner.getValue()));
		settings.add(spinner);
	}

	private void check(JPanel settings, String text, String key, boolean value)
	{
		label(settings, text);
		JCheckBox box = new JCheckBox();
		box.setSelected(value);
		box.addActionListener(e -> set(key, box.isSelected()));
		settings.add(box);
	}

	private <E extends Enum<E>> void combo(JPanel settings, String text, String key, E value)
	{
		label(settings, text);
		JComboBox<E> box = new JComboBox<>(value.getDeclaringClass().getEnumConstants());
		box.setSelectedItem(value);
		box.addActionListener(e -> set(key, box.getSelectedItem()));
		settings.add(box);
	}

	private void color(JPanel settings, String text, String key, Color value)
	{
		label(settings, text);
		JButton swatch = new JButton();
		swatch.setBackground(value);
		swatch.setToolTipText("Click to pick a color");
		swatch.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				RuneliteColorPicker picker = colorPickerManager.create(swatch, swatch.getBackground(), text, false);
				picker.setOnColorChange(c ->
				{
					swatch.setBackground(c);
					set(key, c);
				});
				picker.setVisible(true);
			}
		});
		settings.add(swatch);
	}

	/** Publishing the picked loadout and browsing loadouts shared by other players. */
	private void buildCommunitySection(JPanel page)
	{
		page.add(heading("Community"));
		communityStatus.setFont(FontManager.getRunescapeSmallFont());
		JLabel about = new JLabel(wrap("Share your loadouts with other players under a made-up author name,"
			+ " up to " + CommunityLoadouts.SLOTS + ". Publishing a name again replaces it."));
		about.setFont(FontManager.getRunescapeSmallFont());
		page.add(left(about));

		page.add(left(fieldLabel("Publish as")));
		publishName.setToolTipText("Name to publish the picked loadout under; fills in with the loadout's name");
		page.add(left(publishName));
		JCheckBox anonymous = new JCheckBox("Anonymous");
		anonymous.setToolTipText("Show a made-up author name; untick to show your game name as the author");
		anonymous.setSelected(!"false".equals(configManager.getConfiguration(InventoryHoverHighlightConfig.GROUP, ANONYMOUS_KEY)));
		anonymous.addActionListener(e ->
		{
			if (!anonymous.isSelected() && JOptionPane.showConfirmDialog(this,
				"Your game name will be sent to Google Firebase, a 3rd party server not controlled or verified by"
					+ " the RuneLite developers, and shown to everyone as the author of what you publish.\n\n"
					+ "Show your game name?", "Community loadouts", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION)
			{
				anonymous.setSelected(true);
				return;
			}
			configManager.setConfiguration(InventoryHoverHighlightConfig.GROUP, ANONYMOUS_KEY, anonymous.isSelected());
		});
		page.add(left(anonymous));
		page.add(left(fieldLabel("Sections to publish")));
		sectionsButton.setToolTipText("Pick which settings sections to publish");
		sectionsButton.addActionListener(e -> chooseSections());
		updateSectionsButton();
		page.add(left(sectionsButton));

		JPanel buttons = new JPanel(new GridLayout(1, 2, 6, 0));
		JButton publish = new JButton("Publish");
		publish.setToolTipText("Publish the picked loadout, as it was saved, under the name above");
		publish.addActionListener(e ->
		{
			if (showProblem(true))
			{
				return;
			}
			String loadout = (String) loadoutBox.getSelectedItem();
			if (loadout == null)
			{
				communityStatus.setText(wrap("Save a loadout first, then publish it."));
				return;
			}
			String name = publishName.getText().trim().isEmpty() ? loadout : publishName.getText().trim();
			publish.setEnabled(false);
			communityStatus.setText(wrap("Publishing " + name + "..."));
			String settings = plugin.loadouts().onlySections(plugin.loadouts().loadoutJson(loadout), chosenSections);
			Consumer<String> send = author -> plugin.community().publish(name, settings, author, result ->
			{
				publish.setEnabled(true);
				communityStatus.setText(wrap(result.message));
				if (result.clearName)
				{
					publishName.setText("");
				}
				browser.refreshIfOpen();
			});
			if (anonymous.isSelected())
			{
				send.accept(null);
				return;
			}
			plugin.playerName(author ->
			{
				if (author == null)
				{
					publish.setEnabled(true);
					communityStatus.setText(wrap("Log in to the game to publish under your game name."));
					return;
				}
				send.accept(author);
			});
		});
		JButton browse = new JButton("Browse");
		browse.setToolTipText("Search, sort and preview published loadouts in a window");
		browse.addActionListener(e ->
		{
			if (!showProblem(false))
			{
				browser.open();
			}
		});
		buttons.add(publish);
		buttons.add(browse);
		page.add(left(buttons));
		page.add(left(communityStatus));
	}

	/** Shows why the community features cannot be used, if they cannot; returns whether it showed one. */
	private boolean showProblem(boolean publishing)
	{
		String problem = plugin.communityProblem(publishing);
		if (problem != null)
		{
			communityStatus.setText(wrap(problem));
			// The status line can sit below the visible part of the panel, so say it where it is seen
			JOptionPane.showMessageDialog(this, problem, "Community loadouts", JOptionPane.INFORMATION_MESSAGE);
		}
		return problem != null;
	}

	private static JPanel tabPage()
	{
		JPanel page = new JPanel();
		page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
		page.setBorder(new EmptyBorder(4, 0, 0, 0));
		return page;
	}

	/** Keeps a tab page's rows at the top instead of stretching them over the tab. */
	private static JPanel top(JPanel page)
	{
		JPanel holder = new JPanel(new BorderLayout());
		holder.add(page, BorderLayout.NORTH);
		return holder;
	}

	private static String wrap(String text)
	{
		return "<html><body style='width: 170px'>" + text + "</body></html>";
	}

	private static JComponent heading(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setForeground(Color.WHITE);
		label.setBorder(new EmptyBorder(8, 0, 4, 0));
		return left(label);
	}

	/** Asks which settings sections to publish. */
	private void chooseSections()
	{
		List<String> names = plugin.loadouts().sectionNames();
		Set<String> picked = SectionPicker.pick(this, "Sections to publish",
			"Publish only these sections. Loading the published loadout changes only these sections.",
			names, chosenSections);
		if (picked != null)
		{
			chosenSections = picked.size() == names.size() ? null : picked;
			updateSectionsButton();
		}
	}

	private void updateSectionsButton()
	{
		sectionsButton.setText(chosenSections == null ? "All sections"
			: chosenSections.size() + (chosenSections.size() == 1 ? " section" : " sections"));
	}

	/** A small label above a field. */
	private static JLabel fieldLabel(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setBorder(new EmptyBorder(6, 0, 2, 0));
		return label;
	}

	private static JLabel legendLabel(String text, Color color)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(color);
		return label;
	}

	private static JComponent left(JComponent component)
	{
		component.setAlignmentX(LEFT_ALIGNMENT);
		return component;
	}

	/** Line chart of switch time and accuracy per practice session, oldest on the left. */
	private static class ProgressChart extends JComponent
	{
		private static final int HEIGHT = 120;
		private List<SwitchPractice.Summary> points = new ArrayList<>();

		ProgressChart()
		{
			setPreferredSize(new Dimension(PANEL_WIDTH - 16, HEIGHT));
			setMaximumSize(new Dimension(Integer.MAX_VALUE, HEIGHT));
			setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
		}

		void setPoints(List<SwitchPractice.Summary> points)
		{
			this.points = points;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			Graphics2D graphics = (Graphics2D) g.create();
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int w = getWidth();
			int h = getHeight();
			graphics.setColor(ColorScheme.DARKER_GRAY_COLOR);
			graphics.fillRect(0, 0, w, h);

			graphics.setFont(FontManager.getRunescapeSmallFont());
			FontMetrics metrics = graphics.getFontMetrics();
			if (points.isEmpty())
			{
				String empty = "Finish a practice round to see progress";
				graphics.setColor(ColorScheme.LIGHT_GRAY_COLOR);
				graphics.drawString(empty, Math.max(2, (w - metrics.stringWidth(empty)) / 2), h / 2);
				graphics.dispose();
				return;
			}

			int pad = 6;
			int top = pad + metrics.getHeight();
			int bottom = h - pad;
			double maxMillis = 1;
			for (SwitchPractice.Summary s : points)
			{
				maxMillis = Math.max(maxMillis, s.switchMillis);
			}

			graphics.setColor(SWITCH_LINE);
			graphics.drawString(String.format("%.0f ms", maxMillis), pad, pad + metrics.getAscent());
			graphics.setColor(ACCURACY_LINE);
			String full = "100%";
			graphics.drawString(full, w - pad - metrics.stringWidth(full), pad + metrics.getAscent());

			graphics.setColor(new Color(255, 255, 255, 40));
			graphics.drawLine(pad, bottom, w - pad, bottom);

			drawLine(graphics, w, top, bottom, pad, SWITCH_LINE, maxMillis, true);
			drawLine(graphics, w, top, bottom, pad, ACCURACY_LINE, 100, false);
			graphics.dispose();
		}

		private void drawLine(Graphics2D graphics, int w, int top, int bottom, int pad, Color color, double max,
			boolean switchTime)
		{
			Path2D path = new Path2D.Double();
			int n = points.size();
			for (int i = 0; i < n; i++)
			{
				SwitchPractice.Summary s = points.get(i);
				double value = switchTime ? s.switchMillis : s.accuracy;
				double x = n == 1 ? w / 2.0 : pad + (w - pad * 2) * (double) i / (n - 1);
				double y = bottom - (bottom - top) * Math.min(1, value / max);
				if (i == 0)
				{
					path.moveTo(x, y);
				}
				else
				{
					path.lineTo(x, y);
				}
				graphics.setColor(color);
				graphics.fillOval((int) x - 2, (int) y - 2, 4, 4);
			}
			graphics.setColor(color);
			graphics.setStroke(new BasicStroke(1.5f));
			graphics.draw(path);
		}
	}
}
