package org.omegat.plugins.vimish;

import org.omegat.util.Log;

import java.awt.Component;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.JCheckBox;

@SuppressWarnings("serial")
class VimishOptionsPanel extends JPanel {
  JCheckBox moveCursorBackCheckBox;
  JTable keyMappingsTable;

  // Headers for key mappings table
  String[] columns = { "Key Sequence", "Mapped To" };
  String[][] data = { { "ab", "bc" },
                      { "cd", "fg" } };

  VimishOptionsPanel() {
    initComponents();
  }

  private void initComponents() {
    moveCursorBackCheckBox = new JCheckBox("Move cursor back one position when exiting insert mode"); 
    add(moveCursorBackCheckBox);

    keyMappingsTable = new JTable(data, columns);
    add(new JScrollPane(keyMappingsTable));
  } 
}
