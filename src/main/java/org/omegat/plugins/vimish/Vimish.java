/**
 * Vimish
 * Vim-style editing for OmegaT
 * 
 * @author Nick Pizzigati
 */

package org.omegat.plugins.vimish;

import javax.swing.JOptionPane;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;

import org.omegat.gui.editor.EditorTextArea3;

import org.omegat.core.CoreEvents;
import org.omegat.core.events.*;
import org.omegat.core.data.SourceTextEntry;

// TODO: options: - integrate system and vim clipboards
//                - position of cursor on entering insert mode
//                  (whether to adjust position by one)
//                - key or chord for escape
public class Vimish {
  static boolean isFirstLoad = true;
  /**
   * Plugin loader
   */
  public static void loadPlugins() {
    CoreEvents.registerApplicationEventListener(new IApplicationEventListener() {
      @Override
      public void onApplicationStartup() {
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

        // Consume keys entered outside main editing area
        if (isOutsideMainEditingArea(event)) {
          return true;
        }

        KeySequence keySequence = KeySequence.getKeySequence();
        String keyString = determineKeyString(event);

        keySequence.applyKey(keyString);

        if (Mode.INSERT.isActive()) {
          if ((int)event.getKeyChar() == 27) {
            Mode.NORMAL.activate();
            keySequence.resetSequence();
            VimishCaret.processCaret();
            return true;
          } else {
            return false;
          }
        } else {
          // Normal mode
          if (event.getKeyChar() == 'i') {
            Mode.INSERT.activate();
            VimishCaret.processCaret();
          } 
          return true;
        }
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
