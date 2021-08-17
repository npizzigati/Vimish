package org.omegat.plugins.vimish;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;

import org.omegat.gui.editor.EditorController;
import org.omegat.gui.editor.EditorTextArea3;
import org.omegat.core.Core;
import org.omegat.util.Log;

class Dispatch {
  private EditorController editor = (EditorController) Core.getEditor();
  private KeySequence keySequence;
  private KeyChords keyChords;

  Dispatch() {
    Actions actions = new Actions(editor);
    keySequence = new KeySequence(actions);
    keyChords = new KeyChords(this);
  }

  void installKeyEventDispatcher() {
    KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    manager.addKeyEventDispatcher(new KeyEventDispatcher() {

      @Override
      public boolean dispatchKeyEvent(KeyEvent event) {

        // Temporary logging
        if (event.getID() == KeyEvent.KEY_TYPED) {
          Log.log("Key typed event char: " + event.getKeyChar()
                  + "(int)char: " + (int)event.getKeyChar() + " code: " + event.getKeyCode());
        } else if (event.getID() == KeyEvent.KEY_PRESSED) {
          Log.log("Key pressed event: " + event.getKeyCode());
        }

        // Don't consume keys entered outside main editing area
        if (isOutsideMainEditingArea(event)) {
          return false;
        }

        // Don't consume action-key keyPressed events (e.g. arrow
        // keys, page up, page down, home, end, etc.), except for
        // INSERT key (to prevent problems with caret)
        if (event.isActionKey() && event.getID() == KeyEvent.KEY_PRESSED
            && event.getKeyCode() != KeyEvent.VK_INSERT) {
          return false;
        }

        // Consume other non-keyTyped events, to prevent
        // duplication of events in the form of keyPressed and
        // keyReleased events
        if (!(event.getID() == KeyEvent.KEY_TYPED)) {
          return true;
        }

        String keyString = determineKeyString(event);

        keyChords.process(keyString);

        // consume key event
        return true;
      }
    });
  }

  public void sendToKeyMapper(String keyChordResult) {
    keySequence.apply(keyChordResult);
  }

  private String determineKeyString(KeyEvent event) {
    String keyString = null;
    char keyChar = event.getKeyChar();

    switch((int)keyChar) {
    case KeyEvent.VK_ESCAPE:
      keyString = "<ESC>";
      break;
    case KeyEvent.VK_BACK_SPACE:
      keyString = "<BACKSPACE>";
      break;
    case KeyEvent.VK_ENTER:
      keyString = "<ENTER>";
      break;
    case KeyEvent.VK_TAB:
      keyString = "<TAB>";
      break;
    case KeyEvent.VK_DELETE:
      keyString = "<DEL>";
      break;
    default:
      keyString = String.valueOf(keyChar);
      break;
    }

    return keyString;
  } 

  private boolean isOutsideMainEditingArea(KeyEvent event) {
    return !(event.getComponent().getClass() == EditorTextArea3.class);
  }
}
