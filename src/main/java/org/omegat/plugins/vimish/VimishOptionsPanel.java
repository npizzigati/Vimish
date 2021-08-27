package org.omegat.plugins.vimish;

import org.omegat.util.Log;

import java.awt.Component;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.JScrollPane;
import javax.swing.JCheckBox;
import javax.swing.BorderFactory;
import javax.swing.border.TitledBorder;

@SuppressWarnings("serial")
class VimishOptionsPanel extends JPanel {
  JCheckBox moveCursorBackCheckBox;
  JTable keyMappingsTable;

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
    JPanel keyMappingsPanel = new JPanel();
    JPanel abbreviationsPanel = new JPanel();
    JPanel keyChordsPanel = new JPanel();

    TitledBorder generalOptionsTitle = BorderFactory.createTitledBorder("General Options");
    TitledBorder keyMappingsTitle = BorderFactory.createTitledBorder("Key Mappings");
    TitledBorder abbreviationsTitle = BorderFactory.createTitledBorder("Abbreviations");
    TitledBorder keyChordsTitle = BorderFactory.createTitledBorder("Key Chords");

    moveCursorBackCheckBox = new JCheckBox("Move cursor back one position when exiting insert mode (Vim default)");
    generalOptionsPanel.add(moveCursorBackCheckBox);
    generalOptionsPanel.setBorder(generalOptionsTitle);
    add(generalOptionsPanel, BorderLayout.NORTH);

    keyMappingsTable = new JTable();
    keyMappingsPanel.add(new JScrollPane(keyMappingsTable));
    keyMappingsPanel.setBorder(keyMappingsTitle);
    add(keyMappingsPanel, BorderLayout.CENTER);
  }
}
