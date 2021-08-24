package org.omegat.plugins.vimish;

import org.omegat.util.Log;

import java.awt.Component;
import javax.swing.JPanel;
import javax.swing.JCheckBox;

@SuppressWarnings("serial")
class VimishOptionsPanel extends JPanel {
  JCheckBox moveCursorBackCheckBox;

  VimishOptionsPanel() {
    initComponents();
  }

  private void initComponents() {
    moveCursorBackCheckBox = new JCheckBox("Move cursor back one position when exiting insert mode"); 
    add(moveCursorBackCheckBox);
  } 
}
