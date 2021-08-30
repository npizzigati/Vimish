package org.omegat.plugins.vimish;

import org.omegat.util.Log;

import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.border.TitledBorder;

@SuppressWarnings("serial")
class VimishOptionsPanel extends JPanel {
  JCheckBox moveCursorBackCheckBox;
  JTable keyMappingsTable;
  JComboBox<String> modeSelector;
  JButton keyMappingsAddButton;
  JButton keyMappingsRemoveButton;

  // Headers for key mappings table
  String[] columns = { "Key Sequence", "Mapped To" };
  String[][] data = { { "ab", "bc" },
                      { "cd", "fg" } };

  VimishOptionsPanel() {
    setLayout(new BorderLayout());
    initComponents();
  }

  private void initComponents() {
    JPanel generalOptionsPanel = new JPanel();
    JPanel keyMappingsPanel = new JPanel(new BorderLayout());
    JPanel keyMappingsButtonBox = new JPanel();
    JPanel modeSelectorBox = new JPanel(new FlowLayout(FlowLayout.LEFT));
    JPanel abbreviationsPanel = new JPanel(new BorderLayout());
    JPanel keyChordsPanel = new JPanel(new BorderLayout());

    TitledBorder generalOptionsTitle = BorderFactory.createTitledBorder("General Options");
    TitledBorder keyMappingsTitle = BorderFactory.createTitledBorder("Key Mappings");
    TitledBorder abbreviationsTitle = BorderFactory.createTitledBorder("Abbreviations");
    TitledBorder keyChordsTitle = BorderFactory.createTitledBorder("Key Chords");

    moveCursorBackCheckBox = new JCheckBox("Move cursor back one position when exiting insert mode (Vim default)");
    generalOptionsPanel.add(moveCursorBackCheckBox);
    generalOptionsPanel.setBorder(generalOptionsTitle);
    add(generalOptionsPanel, BorderLayout.NORTH);

    // Key mappings table and buttons
    keyMappingsTable = new JTable();
    keyMappingsPanel.setBorder(keyMappingsTitle);
    // keyMappingsPanel.setLayout(new BorderLayout());

    String[] comboBoxOptions = { "Normal", "Visual", "Insert" };
    modeSelector = new JComboBox<>(comboBoxOptions);
    JLabel modeSelectorLabel = new JLabel("Mode:");

    modeSelectorBox.add(modeSelectorLabel);
    modeSelectorBox.add(modeSelector);

    keyMappingsPanel.add(modeSelectorBox, BorderLayout.NORTH);
    keyMappingsPanel.add(new JScrollPane(keyMappingsTable), BorderLayout.CENTER);

    keyMappingsAddButton = new JButton("Add");
    keyMappingsAddButton.setPreferredSize(new Dimension(100, 25));
    keyMappingsRemoveButton = new JButton("Remove");
    keyMappingsRemoveButton.setPreferredSize(new Dimension(100, 25));
    keyMappingsButtonBox.setPreferredSize(new Dimension(110, 50));
    keyMappingsButtonBox.add(keyMappingsAddButton);
    keyMappingsButtonBox.add(keyMappingsRemoveButton);
    keyMappingsPanel.add(keyMappingsButtonBox, BorderLayout.EAST);

    add(keyMappingsPanel, BorderLayout.CENTER);
  }
}
