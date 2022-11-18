package org.omegat.plugins.vimish;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;

import org.omegat.gui.editor.EditorTextArea3;
import org.omegat.util.Log;

class Dispatcher {
  private PreRouter preRouter;

  Dispatcher() {
    preRouter = new PreRouter();
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

        // Don't consume events with ALT or CTRL modifiers
        int extModifierMask = event.getModifiersEx();
        if ((extModifierMask & InputEvent.ALT_DOWN_MASK) != 0 ||
            (extModifierMask & InputEvent.CTRL_DOWN_MASK) != 0) {
          return false;
        }

        // Don't consume action-key keyPressed events (e.g. page
        // up, page down, home, end, etc.), except for INSERT key
        // (to prevent problems with caret). Also exclude arrow
        // keys, since these will be processed below.
        if (event.isActionKey() && event.getID() == KeyEvent.KEY_PRESSED
            && event.getKeyCode() != KeyEvent.VK_INSERT
            && event.getKeyCode() != KeyEvent.VK_RIGHT
            && event.getKeyCode() != KeyEvent.VK_LEFT) {
          return false;
        }

        // Consume other non-keyTyped events, to prevent
        // duplication of events in the form of keyPressed and
        // keyReleased events. Exclude arrow keys, since they don't have
        // an associated keyTyped event.
        if (event.getID() != KeyEvent.KEY_TYPED
            && event.getKeyCode() != KeyEvent.VK_RIGHT
            && event.getKeyCode() != KeyEvent.VK_LEFT) {
          return true;
        }

        // Consume the keyReleased events that have passed thru
        // to this point (i.e. the arrow key keyReleased events)
        // We will let the arrow key keyPressed events pass thru
        // to be processed below.
        if (event.getID() == KeyEvent.KEY_RELEASED) {
          return true;
        }

        String keyString = determineKeyString(event);

        // keyChordController.process(keyString);
        preRouter.process(keyString);

        // consume key event
        return true;
      }
    });
  }

  private String determineKeyString(KeyEvent event) {
    String keyString = null;
    char keyChar = event.getKeyChar();
    int keyCode = event.getKeyCode();
    boolean shiftPressed = false;
    // Handle arrow keys first, which only have reliable key
    // codes (the result of getKeyChar() is meaningless for
    // action keys)
    if (keyCode == KeyEvent.VK_LEFT) {
      keyString = "\u2732LEFT\u2732";
    } else if (keyCode == KeyEvent.VK_RIGHT) {
      keyString = "\u2732RIGHT\u2732";
    } else {
      switch((int)keyChar) {
      case KeyEvent.VK_ESCAPE:
        keyString = "\u2732ESC\u2732";
        break;
      case KeyEvent.VK_BACK_SPACE:
        keyString = "\u2732BACKSPACE\u2732";
        break;
      case KeyEvent.VK_ENTER:
        keyString = "\u2732ENTER\u2732";
        break;
      case KeyEvent.VK_TAB:
        keyString = "\u2732TAB\u2732";
        break;
      case KeyEvent.VK_DELETE:
        keyString = "\u2732DEL\u2732";
        break;
      default:
        keyString = String.valueOf(keyChar);
        break;
      }
    }

    if ((event.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0) {
      shiftPressed = true;
    }

    if (shiftPressed && keyString.equals("\u2732TAB\u2732")) {
      keyString = "\u2732S-TAB\u2732";
    }

    return keyString;
  }

  private boolean isOutsideMainEditingArea(KeyEvent event) {
    return !(event.getComponent().getClass() == EditorTextArea3.class);
  }
}
