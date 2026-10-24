package com.inventoryhover;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Window for browsing community loadouts: a table sortable by any column,
 * search over names, authors and sections, a section filter, a filter for
 * your own loadouts, download counts, a small x to delete your own, and a
 * preview of every setting a loadout changes.
 */
class CommunityBrowser extends JFrame
{
	private static final String[] COLUMNS = {"Name", "Author", "Published", "Sections", "Downloads", ""};
	private static final int DOWNLOADS_COLUMN = 4;
	/** The column with a small x on your own loadouts, to delete them. */
	private static final int REMOVE_COLUMN = 5;

	private final InventoryHoverHighlightPlugin plugin;
	/** Why the community features cannot be used right now, or null. */
	private final Supplier<String> problem;
	/** Called with the name of a loadout added from the browser, so the side panel lists it. */
	private final Consumer<String> loadoutAdded;

	private final Model model = new Model();
	private final JTable table = new JTable(model);
	private final TableRowSorter<Model> sorter = new TableRowSorter<>(model);
	private final JTextField search = new JTextField();
	private final JButton sectionFilter = new JButton("Any sections");
	private final JCheckBox exactSections = new JCheckBox("Only these");
	/** Sections a loadout must set, or null for any; and every section seen in the list. */
	private java.util.Set<String> wantedSections;
	private final List<String> knownSections = new ArrayList<>();
	private final JCheckBox mineOnly = new JCheckBox("Mine only");
	private final JLabel status = new JLabel(" ");
	private final JLabel title = new JLabel("Pick a loadout");
	private final JLabel byline = new JLabel(" ");
	private final JTextArea preview = new JTextArea();
	private final JButton load = new JButton("Load");
	private final JButton delete = new JButton("Delete");
	private final JButton copy = new JButton("Copy JSON");

	CommunityBrowser(InventoryHoverHighlightPlugin plugin, Supplier<String> problem, Consumer<String> loadoutAdded)
	{
		super("Click Helpers - Community loadouts");
		this.plugin = plugin;
		this.problem = problem;
		this.loadoutAdded = loadoutAdded;
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		setMinimumSize(new Dimension(640, 400));
		setSize(980, 560);

		JPanel root = new JPanel(new BorderLayout(0, 8));
		root.setBorder(new EmptyBorder(10, 10, 10, 10));
		root.setBackground(ColorScheme.DARK_GRAY_COLOR);
		root.add(toolbar(), BorderLayout.NORTH);

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tablePane(), detailsPane());
		split.setResizeWeight(0.62);
		split.setBorder(null);
		root.add(split, BorderLayout.CENTER);

		status.setFont(FontManager.getRunescapeSmallFont());
		status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		root.add(status, BorderLayout.SOUTH);
		setContentPane(root);
		showSelected();
	}

	/** Opens the window and loads the list. */
	void open()
	{
		if (!isVisible())
		{
			setLocationRelativeTo(null);
		}
		setVisible(true);
		toFront();
		refresh();
	}

	/** Reloads the list if the window is open, such as after publishing. */
	void refreshIfOpen()
	{
		if (isVisible())
		{
			refresh();
		}
	}

	void refresh()
	{
		String why = problem.get();
		if (why != null)
		{
			status.setText(why);
			return;
		}
		status.setText("Loading...");
		plugin.community().list(entries ->
		{
			CommunityLoadouts.Entry selected = selected();
			model.setEntries(entries, plugin.community().uid(), plugin.loadouts().sectionNames().size());
			updateSections(entries);
			applyFilter();
			reselect(selected);
		}, status::setText);
	}

	private JPanel toolbar()
	{
		JPanel bar = new JPanel(new BorderLayout(8, 0));
		bar.setOpaque(false);

		search.setToolTipText("Search names, authors and sections");
		search.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				applyFilter();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				applyFilter();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				applyFilter();
			}
		});
		JPanel searchBox = new JPanel(new BorderLayout(6, 0));
		searchBox.setOpaque(false);
		searchBox.add(new JLabel("Search"), BorderLayout.WEST);
		searchBox.add(search, BorderLayout.CENTER);
		bar.add(searchBox, BorderLayout.CENTER);

		JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		filters.setOpaque(false);
		sectionFilter.setToolTipText("Only loadouts that set all the sections you pick");
		sectionFilter.addActionListener(e ->
		{
			java.util.Set<String> picked = SectionPicker.pick(this, "Filter by sections",
				"Show only loadouts that set every section you tick. Tick Only these to hide loadouts that also set other"
					+ " sections. Tick none to show every loadout.",
				knownSections, wantedSections == null ? java.util.Collections.emptySet() : wantedSections);
			if (picked != null)
			{
				wantedSections = picked.isEmpty() ? null : picked;
				sectionFilter.setText(wantedSections == null ? "Any sections"
					: wantedSections.size() + (wantedSections.size() == 1 ? " section" : " sections"));
				applyFilter();
			}
		});
		exactSections.setOpaque(false);
		exactSections.setToolTipText("Show only loadouts that set exactly the picked sections and nothing else");
		exactSections.addActionListener(e -> applyFilter());
		mineOnly.setOpaque(false);
		mineOnly.setToolTipText("Only loadouts you published");
		mineOnly.addActionListener(e -> applyFilter());
		JButton refresh = new JButton("Refresh");
		refresh.setToolTipText("Download the newest published loadouts");
		refresh.addActionListener(e -> refresh());
		filters.add(sectionFilter);
		filters.add(exactSections);
		filters.add(mineOnly);
		filters.add(refresh);
		bar.add(filters, BorderLayout.EAST);
		return bar;
	}

	private JScrollPane tablePane()
	{
		table.setRowSorter(sorter);
		sorter.setSortKeys(List.of(new RowSorter.SortKey(2, SortOrder.DESCENDING)));
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setFillsViewportHeight(true);
		table.setRowHeight(24);
		table.setShowGrid(false);
		table.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		table.getTableHeader().setReorderingAllowed(false);
		table.getColumnModel().getColumn(0).setPreferredWidth(200);
		table.getColumnModel().getColumn(1).setPreferredWidth(140);
		table.getColumnModel().getColumn(2).setPreferredWidth(100);
		table.getColumnModel().getColumn(3).setPreferredWidth(70);
		table.getColumnModel().getColumn(DOWNLOADS_COLUMN).setPreferredWidth(90);
		// Wide enough for the header and its sort arrow
		table.getColumnModel().getColumn(DOWNLOADS_COLUMN).setMinWidth(85);
		table.getColumnModel().getColumn(REMOVE_COLUMN).setMinWidth(24);
		table.getColumnModel().getColumn(REMOVE_COLUMN).setMaxWidth(24);
		sorter.setSortable(REMOVE_COLUMN, false);
		table.setDefaultRenderer(Object.class, new Renderer());
		table.setDefaultRenderer(Date.class, new Renderer());
		table.setDefaultRenderer(Integer.class, new Renderer());
		table.getSelectionModel().addListSelectionListener(e ->
		{
			if (!e.getValueIsAdjusting())
			{
				showSelected();
			}
		});
		table.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				int row = table.rowAtPoint(e.getPoint());
				if (row >= 0 && table.columnAtPoint(e.getPoint()) == REMOVE_COLUMN)
				{
					if (model.isMine(table.convertRowIndexToModel(row)))
					{
						table.setRowSelectionInterval(row, row);
						deleteSelected();
					}
					return;
				}
				if (e.getClickCount() == 2 && selected() != null)
				{
					loadSelected();
				}
			}
		});
		JScrollPane scroll = new JScrollPane(table);
		scroll.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
		return scroll;
	}

	private JPanel detailsPane()
	{
		JPanel details = new JPanel(new BorderLayout(0, 6));
		details.setBorder(new EmptyBorder(0, 10, 0, 0));
		details.setOpaque(false);

		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		byline.setFont(FontManager.getRunescapeSmallFont());
		byline.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		JPanel heading = new JPanel(new GridLayout(0, 1));
		heading.setOpaque(false);
		heading.add(title);
		heading.add(byline);
		details.add(heading, BorderLayout.NORTH);

		preview.setEditable(false);
		preview.setLineWrap(true);
		preview.setWrapStyleWord(true);
		preview.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		preview.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		preview.setBorder(new EmptyBorder(6, 6, 6, 6));
		JScrollPane scroll = new JScrollPane(preview);
		scroll.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
		details.add(scroll, BorderLayout.CENTER);

		load.setToolTipText("Add as a new loadout, or apply over your current settings (or double-click a row)");
		load.addActionListener(e -> loadSelected());
		delete.setToolTipText("Delete your published loadout");
		delete.addActionListener(e -> deleteSelected());
		copy.setToolTipText("Copy the loadout's settings JSON, to share or import elsewhere");
		copy.addActionListener(e ->
		{
			CommunityLoadouts.Entry entry = selected();
			if (entry != null)
			{
				java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
					.setContents(new java.awt.datatransfer.StringSelection(entry.settings), null);
				status.setText("Copied " + entry.name + " to the clipboard.");
			}
		});
		JPanel buttons = new JPanel(new GridLayout(1, 3, 6, 0));
		buttons.setOpaque(false);
		buttons.add(load);
		buttons.add(copy);
		buttons.add(delete);
		details.add(buttons, BorderLayout.SOUTH);
		return details;
	}

	private void applyFilter()
	{
		String text = search.getText().trim().toLowerCase();
		java.util.Set<String> wanted = wantedSections;
		boolean exact = exactSections.isSelected();
		boolean mine = mineOnly.isSelected();
		String uid = plugin.community().uid();
		sorter.setRowFilter(new RowFilter<Model, Integer>()
		{
			@Override
			public boolean include(Entry<? extends Model, ? extends Integer> row)
			{
				CommunityLoadouts.Entry entry = model.entries.get(row.getIdentifier());
				if (mine && !entry.uid.equals(uid))
				{
					return false;
				}
				if (wanted != null && (!entry.sections.containsAll(wanted)
					|| exact && entry.sections.size() != wanted.size()))
				{
					return false;
				}
				return text.isEmpty() || entry.name.toLowerCase().contains(text)
					|| entry.author().toLowerCase().contains(text)
					|| String.join(" ", entry.sections).toLowerCase().contains(text);
			}
		});
		long own = model.entries.stream().filter(e -> e.uid.equals(uid)).count();
		status.setText(table.getRowCount() + " of " + model.entries.size() + " loadouts shown. You have published "
			+ own + " of " + CommunityLoadouts.SLOTS + ".");
		showSelected();
	}

	private void updateSections(List<CommunityLoadouts.Entry> entries)
	{
		TreeSet<String> names = new TreeSet<>();
		entries.forEach(e -> names.addAll(e.sections));
		knownSections.clear();
		knownSections.addAll(names);
	}

	private CommunityLoadouts.Entry selected()
	{
		int row = table.getSelectedRow();
		return row < 0 ? null : model.entries.get(table.convertRowIndexToModel(row));
	}

	private void reselect(CommunityLoadouts.Entry previous)
	{
		if (previous == null)
		{
			return;
		}
		for (int i = 0; i < model.entries.size(); i++)
		{
			CommunityLoadouts.Entry entry = model.entries.get(i);
			if (entry.uid.equals(previous.uid) && entry.slot == previous.slot)
			{
				int view = table.convertRowIndexToView(i);
				if (view >= 0)
				{
					table.setRowSelectionInterval(view, view);
				}
				return;
			}
		}
	}

	private void showSelected()
	{
		CommunityLoadouts.Entry entry = selected();
		boolean mine = entry != null && entry.uid.equals(plugin.community().uid());
		load.setEnabled(entry != null);
		copy.setEnabled(entry != null);
		delete.setEnabled(mine);
		if (entry == null)
		{
			title.setText("Pick a loadout");
			byline.setText(" ");
			preview.setText("Select a loadout to see every setting it changes.");
			return;
		}
		title.setText(entry.name);
		byline.setText("by " + entry.author() + (mine ? " (you)" : "") + ", "
			+ new SimpleDateFormat("MMM d yyyy, HH:mm").format(new Date(entry.created))
			+ ", " + entry.installs + (entry.installs == 1 ? " download" : " downloads"));
		preview.setText(plugin.loadouts().describe(entry.settings));
		preview.setCaretPosition(0);
	}

	private void loadSelected()
	{
		CommunityLoadouts.Entry entry = selected();
		if (entry == null)
		{
			return;
		}
		java.util.Set<String> picked = entry.sections.size() <= 1 ? null : SectionPicker.pick(this,
			"Sections to load", "Load only these sections of " + entry.name + ". Your other settings stay as they are.",
			entry.sections, null);
		if (entry.sections.size() > 1 && (picked == null || picked.isEmpty()))
		{
			return;
		}
		// Every section ticked means the whole loadout
		java.util.Set<String> sections = picked == null || picked.size() == entry.sections.size() ? null : picked;
		String scope = sections == null ? "" : " (" + sections.size() + " of " + entry.sections.size() + " sections)";

		String[] options = {"Add as new loadout", "Replace current settings", "Cancel"};
		JCheckBox clearOthers = new JCheckBox("Clear other settings");
		clearOthers.setToolTipText("With Replace current settings, put every setting the loadout does not set back to its default");
		JPanel question = new JPanel(new BorderLayout(0, 8));
		question.setOpaque(false);
		question.add(new JLabel("<html>How do you want to load " + entry + scope + "?<br><br>Add as new loadout keeps your"
			+ " current settings and adds it to your loadouts list.<br>Replace current settings applies it now; tick"
			+ " Clear other settings to put everything it does not set back to its default.</html>"), BorderLayout.CENTER);
		question.add(clearOthers, BorderLayout.SOUTH);
		int answer = JOptionPane.showOptionDialog(this, question,
			"Load community loadout", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		if (answer == 0)
		{
			try
			{
				String name = plugin.loadouts().addLoadout(entry.name, plugin.loadouts().onlySections(entry.settings, sections));
				loadoutAdded.accept(name);
				status.setText("Added loadout " + name + scope + ". Load it from the Loadouts list when you want it.");
				countDownload(entry);
			}
			catch (RuntimeException e)
			{
				status.setText("That loadout could not be read.");
			}
		}
		else if (answer == 1)
		{
			String message = plugin.community().load(entry, sections, clearOthers.isSelected());
			status.setText(message);
			if (message.startsWith("Loaded"))
			{
				countDownload(entry);
			}
		}
	}

	/** Counts a download of someone else's loadout and redraws its row when the count goes up. */
	private void countDownload(CommunityLoadouts.Entry entry)
	{
		plugin.community().recordInstall(entry, () ->
		{
			int row = model.entries.indexOf(entry);
			if (row >= 0)
			{
				model.fireTableRowsUpdated(row, row);
			}
			if (selected() == entry)
			{
				showSelected();
			}
		});
	}

	private void deleteSelected()
	{
		CommunityLoadouts.Entry entry = selected();
		String why = problem.get();
		if (entry == null || why != null)
		{
			if (why != null)
			{
				status.setText(why);
			}
			return;
		}
		int answer = JOptionPane.showConfirmDialog(this, "Delete your published loadout " + entry.name + "?",
			"Delete community loadout", JOptionPane.OK_CANCEL_OPTION);
		if (answer == JOptionPane.OK_OPTION)
		{
			plugin.community().delete(entry, message ->
			{
				status.setText(message);
				refresh();
			});
		}
	}

	/** Rows of the table, one per loadout. */
	private static class Model extends AbstractTableModel
	{
		private List<CommunityLoadouts.Entry> entries = new ArrayList<>();
		private String uid;
		/** How many sections a full loadout has, so partial ones show as a share of it. */
		private int totalSections;

		void setEntries(List<CommunityLoadouts.Entry> entries, String uid, int totalSections)
		{
			this.entries = new ArrayList<>(entries);
			this.uid = uid;
			this.totalSections = totalSections;
			fireTableDataChanged();
		}

		boolean isMine(int row)
		{
			return entries.get(row).uid.equals(uid);
		}

		@Override
		public int getRowCount()
		{
			return entries.size();
		}

		@Override
		public int getColumnCount()
		{
			return COLUMNS.length;
		}

		@Override
		public String getColumnName(int column)
		{
			return COLUMNS[column];
		}

		@Override
		public Class<?> getColumnClass(int column)
		{
			return column == 2 ? Date.class : column == 3 || column == DOWNLOADS_COLUMN ? Integer.class : String.class;
		}

		@Override
		public Object getValueAt(int row, int column)
		{
			CommunityLoadouts.Entry entry = entries.get(row);
			switch (column)
			{
				case 0: return entry.name;
				case 1: return entry.author();
				case 2: return new Date(entry.created);
				case 3: return entry.sections.size();
				case DOWNLOADS_COLUMN: return entry.installs;
				default: return isMine(row) ? "\u00d7" : "";
			}
		}
	}

	/**
	 * Draws rows in the RuneLite colors, with your own loadouts' authors and
	 * delete marks in orange and the Default author in green.
	 */
	private class Renderer extends DefaultTableCellRenderer
	{
		private final SimpleDateFormat format = new SimpleDateFormat("MMM d yyyy");

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
			boolean hasFocus, int row, int column)
		{
			Object shown = value instanceof Date ? format.format((Date) value) : value;
			if (column == 3 && value instanceof Integer)
			{
				int count = (Integer) value;
				shown = model.totalSections > 0 && count >= model.totalSections ? "All"
					: count + (model.totalSections > 0 ? " of " + model.totalSections : "");
			}
			super.getTableCellRendererComponent(table, shown, isSelected, false, row, column);
			boolean remove = column == REMOVE_COLUMN;
			setBorder(new EmptyBorder(0, remove ? 0 : 6, 0, remove ? 0 : 6));
			setHorizontalAlignment(remove ? CENTER : column == DOWNLOADS_COLUMN ? RIGHT : LEFT);
			boolean mine = model.isMine(table.convertRowIndexToModel(row));
			setToolTipText(remove && mine ? "Delete your published loadout" : null);
			if (!isSelected)
			{
				setBackground(row % 2 == 0 ? ColorScheme.DARKER_GRAY_COLOR : ColorScheme.DARK_GRAY_COLOR);
				boolean isDefault = model.entries.get(table.convertRowIndexToModel(row)).isDefault();
				setForeground((column == 1 || remove) && mine ? ColorScheme.BRAND_ORANGE
					: column == 1 && isDefault ? ColorScheme.PROGRESS_COMPLETE_COLOR : Color.WHITE);
			}
			return this;
		}
	}
}
