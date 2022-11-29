package org.omegat.plugins.vimish;

import javax.swing.text.DefaultCaret;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import java.awt.Rectangle;
import java.awt.Graphics;

import org.omegat.util.gui.Styles;
import org.omegat.util.Java8Compat;
import org.omegat.util.Log;

@SuppressWarnings("serial")

/**
 * This caret allows different caret shapes for Vimish modes.
 *
 * It will be used if Vimish can get a reference to the
 * the JTextComponent (EditorTextArea3) instance used as the main editing
 * area (through reflection).
 *
 * If it can't due to restrictions on reflection, the regular 
 * OmegaT caret will be used.
 *
 * Caret drawing logic has been taken from
 * OmegaT's OvertypeCaret class with minor modifications
 */
class VimishCaret extends DefaultCaret {
  private static VimishCaret instance;
  private static JTextComponent editingArea;

  private VimishCaret() {
  }

  static void setUpCaret() {
    editingArea = Util.getEditingArea();
    if (editingArea != null) {
      VimishCaret caret = getVimishCaret();
      caret.setBlinkRate(editingArea.getCaret().getBlinkRate());
      editingArea.setCaret(caret);
    } else {
      Log.log("Unable to set Vimish modal caret. Will use standard OmegaT caret");
    }
  }

  static VimishCaret getVimishCaret() {
    if (instance == null) {
      instance = new VimishCaret();
    }
    return instance;
  }

  static void refreshCaret() {
    getVimishCaret().repaint();
  }

  @Override
  public void paint(Graphics g) {
    int dot = getDot();
    Rectangle r = null;
    char dotChar;
    try {
      r = Java8Compat.modelToView(editingArea, dot);
      if (r == null) {
        return;
      }
      dotChar = editingArea.getText(dot, 1).charAt(0);
    } catch (BadLocationException e) {
      return;
    }
    if (x != r.x || y != r.y) {
      repaint();
      x = r.x;
      y = r.y;
      height = r.height;
    }

    editingArea.setCaretColor(Styles.EditorColor.COLOR_BACKGROUND.getColor());
    g.setColor(editingArea.getCaretColor());
    g.setXORMode(Styles.EditorColor.COLOR_FOREGROUND.getColor());
    if (Mode.NORMAL.isActive() || Mode.VISUAL.isActive() || Mode.SEARCH.isActive()) {
      width = g.getFontMetrics().charWidth(dotChar);
    } else {
      width = 1;
    }
    if (isVisible()) {
      g.fillRect(r.x, r.y, width, r.height);
    }
  }

  @Override
  protected synchronized void damage(Rectangle r) {
    if (r == null) {
      return;
    }
    if (Mode.NORMAL.isActive() || Mode.VISUAL.isActive() || Mode.SEARCH.isActive()) {
      x = r.x;
      y = r.y;
      height = r.height;
      if (width <= 0) {
        width = getComponent().getWidth();
      }
      repaint();
    } else {
      super.damage(r);
    }
  }
}
