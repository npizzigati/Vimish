package org.omegat.plugins.vimish;

import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.border.TitledBorder;
import javax.swing.border.EmptyBorder;

@SuppressWarnings("serial")
class VimishOptionsPanel extends JPanel {
  BorderLayout borderLayout;
  JCheckBox moveCursorBackCheckBox;
  JCheckBox useSystemClipboardCheckBox;
  // key mappings
  JTable keyMappingsTable;
  JComboBox<String> keyMappingsModeSelector;
  JButton keyMappingsAddButton;
  JButton keyMappingsRemoveButton;

  // Key chords
  JTable keyChordsTable;
  JComboBox<String> keyChordsModeSelector;
  JButton keyChordsAddButton;
  JButton keyChordsRemoveButton;

  // Abbreviations
  JTable abbreviationTable;

  VimishOptionsPanel() {
    BorderLayout borderLayout = new BorderLayout();
    borderLayout.setVgap(10);
    setLayout(borderLayout);
    setBorder(new EmptyBorder(10, 5, 20, 5));
    initComponents();
  }

  private void initComponents() {
    JPanel generalOptionsPanel = new JPanel();

    TitledBorder generalOptionsTitle = BorderFactory.createTitledBorder("General Options");

    useSystemClipboardCheckBox = new JCheckBox("Use system clipboard (link unnamed register to system clipboard)");
    moveCursorBackCheckBox = new JCheckBox("Move cursor back one position when exiting insert mode (Vim default)");
    generalOptionsPanel.add(useSystemClipboardCheckBox);
    generalOptionsPanel.add(moveCursorBackCheckBox);
    generalOptionsPanel.setLayout(new BoxLayout(generalOptionsPanel, BoxLayout.Y_AXIS));
    generalOptionsPanel.setBorder(generalOptionsTitle);
    add(generalOptionsPanel, BorderLayout.NORTH);

    // Center panel to hold the tables
    GridLayout allTablesLayout = new GridLayout(0, 1);
    JPanel allTablesPanel = new JPanel(allTablesLayout);
    allTablesLayout.setVgap(10);
    add(allTablesPanel);

    // Key mappings table, selector and buttons
    JPanel keyMappingsPanel = new JPanel(new BorderLayout());
    JPanel keyMappingsButtonBox = new JPanel();
    JPanel keyMappingsModeSelectorBox = new JPanel(new FlowLayout(FlowLayout.LEFT));

    keyMappingsTable = new JTable();
    TitledBorder keyMappingsTitle = BorderFactory.createTitledBorder("Key Mappings");
    keyMappingsPanel.setBorder(keyMappingsTitle);

    String[] keyMappingsComboBoxOptions = { "Normal", "Visual", "Insert" };
    keyMappingsModeSelector = new JComboBox<>(keyMappingsComboBoxOptions);

    keyMappingsModeSelectorBox.add(new JLabel("Mode:"));
    keyMappingsModeSelectorBox.add(keyMappingsModeSelector);

    keyMappingsPanel.add(keyMappingsModeSelectorBox, BorderLayout.NORTH);
    keyMappingsPanel.add(new JScrollPane(keyMappingsTable), BorderLayout.CENTER);

    keyMappingsAddButton = new JButton("Add");
    keyMappingsAddButton.setPreferredSize(new Dimension(100, 25));
    keyMappingsRemoveButton = new JButton("Remove");
    keyMappingsRemoveButton.setPreferredSize(new Dimension(100, 25));
    keyMappingsButtonBox.setPreferredSize(new Dimension(110, 50));
    keyMappingsButtonBox.add(keyMappingsAddButton);
    keyMappingsButtonBox.add(keyMappingsRemoveButton);
    keyMappingsPanel.add(keyMappingsButtonBox, BorderLayout.EAST);

    allTablesPanel.add(keyMappingsPanel);

    // Key chords table, selector and buttons
    JPanel keyChordsPanel = new JPanel(new BorderLayout());
    JPanel keyChordsButtonBox = new JPanel();
    JPanel keyChordsModeSelectorBox = new JPanel(new FlowLayout(FlowLayout.LEFT));

    keyChordsTable = new JTable();
    TitledBorder keyChordsTitle = BorderFactory.createTitledBorder("Key Chords");
    keyChordsPanel.setBorder(keyChordsTitle);

    String[] KeyChordsComboBoxOptions = { "Normal", "Visual", "Insert" };
    keyChordsModeSelector = new JComboBox<>(KeyChordsComboBoxOptions);

    keyChordsModeSelectorBox.add(new JLabel("Mode:"));
    keyChordsModeSelectorBox.add(keyChordsModeSelector);

    keyChordsPanel.add(keyChordsModeSelectorBox, BorderLayout.NORTH);
    keyChordsPanel.add(new JScrollPane(keyChordsTable), BorderLayout.CENTER);

    keyChordsAddButton = new JButton("Add");
    keyChordsAddButton.setPreferredSize(new Dimension(100, 25));
    keyChordsRemoveButton = new JButton("Remove");
    keyChordsRemoveButton.setPreferredSize(new Dimension(100, 25));
    keyChordsButtonBox.setPreferredSize(new Dimension(110, 50));
    keyChordsButtonBox.add(keyChordsAddButton);
    keyChordsButtonBox.add(keyChordsRemoveButton);
    keyChordsPanel.add(keyChordsButtonBox, BorderLayout.EAST);

    allTablesPanel.add(keyChordsPanel);
  }
}
