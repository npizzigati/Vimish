/**
 * Vimish
 * Vim-style editing for OmegaT
 * 
 * @author Nick Pizzigati
 */

package org.omegat.plugins.vimish;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;

import org.omegat.gui.editor.EditorTextArea3;
import org.omegat.core.Core;
import org.omegat.core.CoreEvents;
import org.omegat.core.events.*;
import org.omegat.core.data.SourceTextEntry;

// TODO: options: - integrate system and vim clipboards
//                - position of cursor on entering insert mode
//                  (whether to adjust position by one)
//                - key or chord for escape
public class Vimish {
  static Actions actions;
  static KeySequence keySequence;
  static boolean isFirstLoad = true;
  /**
   * Plugin loader
   */
  public static void loadPlugins() {
    Core.registerMarkerClass(VimishVisualMarker.class);
    CoreEvents.registerApplicationEventListener(new IApplicationEventListener() {
      @Override
      public void onApplicationStartup() {
        actions = new Actions();
        keySequence = new KeySequence(actions);
        installEntryListener();
      }

      @Override
      public void onApplicationShutdown() {
      }
    });
  }

  private static void installEntryListener() {
    CoreEvents.registerEntryEventListener(new IEntryEventListener() {
      @Override
      public void onNewFile(String activeFileName) {
        if (isFirstLoad) {
          VimishCaret.setUpCaret();
          installKeyEventDispatcher();
          isFirstLoad = false;
        }
      };

      @Override
      public void onEntryActivated(SourceTextEntry newEntry) {
      }
    });
  }

  private static void installKeyEventDispatcher() {
    KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    manager.addKeyEventDispatcher(new KeyEventDispatcher() {
      @Override
      public boolean dispatchKeyEvent(KeyEvent event) {
        // Don't consume keys entered outside main editing area
        if (isOutsideMainEditingArea(event)) {
          return false;
        }

        // Don't consume action-key keyPressed events
        if (event.isActionKey() && event.getID() == KeyEvent.KEY_PRESSED) {
          return false;
        }

        // Consume other non-keyTyped events, except backspace in insert mode
        if (!(event.getID() == KeyEvent.KEY_TYPED)) {
          if (event.getID() == KeyEvent.KEY_PRESSED
              && event.getKeyCode() == KeyEvent.VK_BACK_SPACE
              && Mode.INSERT.isActive()) {
            return false;
          } else {
            return true;
          }
        }

        // In insert mode, pass keypress through to editing area
        // mode unless escape pressed
        // TODO: Change this so that it also sends Ctrl- sequences
        // to KeySequence#apply() for processing
        if (Mode.INSERT.isActive() &&
            (int)event.getKeyChar() != KeyEvent.VK_ESCAPE) {
          return false;
        }

        // KeySequence keySequence = KeySequence.getKeySequence(actions);
        String keyString = determineKeyString(event);
        keySequence.apply(keyString);

        // Consume keypress
        return true;

      }
    });
  }

  static String determineKeyString(KeyEvent event) {
    String keyString = null;
    char keyChar = event.getKeyChar();
    // Do I also have to determine the key code for enter?
    if ((int)keyChar == KeyEvent.VK_ESCAPE) {
      keyString = "ESC";
    } else {
      keyString = String.valueOf(keyChar);
    }
    return keyString;
  } 

  private static boolean isOutsideMainEditingArea(KeyEvent event) {
    return !(event.getComponent().getClass() == EditorTextArea3.class);
  }
} 
