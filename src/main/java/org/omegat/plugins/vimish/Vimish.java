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

import org.omegat.gui.editor.EditorController;
import org.omegat.gui.editor.EditorTextArea3;
import org.omegat.core.Core;
import org.omegat.core.CoreEvents;
import org.omegat.core.events.*;
import org.omegat.core.data.SourceTextEntry;
import org.omegat.util.Log;

// TODO: options: - integrate system and vim clipboards
//                - position of cursor on entering insert mode
//                  (whether to adjust position by one)
//                - key or chord for escape
public class Vimish {
  static KeySequence keySequence;
  static KeyChords keyChords;
  static boolean isFirstLoad = true;
  /**
   * Plugin loader
   */
  public static void loadPlugins() {
    Core.registerMarkerClass(VimishVisualMarker.class);
    CoreEvents.registerApplicationEventListener(new IApplicationEventListener() {
      @Override
      public void onApplicationStartup() {
        EditorController editor = (EditorController) Core.getEditor();
        Actions actions = new Actions(editor);
        keySequence = new KeySequence(actions);
        keyChords = new KeyChords(keySequence);
        Log.log("VIMISH PLUGIN LOADED");
        installEntryListener();
      }

      @Override
      public void onApplicationShutdown() {
      }
    });
  }

  public static void unloadPlugins() {
  }

  private static void installEntryListener() {
    CoreEvents.registerEntryEventListener(new IEntryEventListener() {
      @Override
      public void onNewFile(String activeFileName) {
      };

      @Override
      public void onEntryActivated(SourceTextEntry newEntry) {
        // It seems to be more reliable to do this setup here
        // than in the onNewFile method above (on startup, the
        // onNewFile method appears not to trigger in some cases.)
        if (isFirstLoad) {
          VimishCaret.setUpCaret();
          installKeyEventDispatcher();
          isFirstLoad = false;
        }
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

        if (!(event.getID() == KeyEvent.KEY_TYPED)) {
          return true;
        }
        // Consume other non-keyTyped events, except backspace in insert mode
        // if (!(event.getID() == KeyEvent.KEY_TYPED)) {
        //   if (event.getID() == KeyEvent.KEY_PRESSED
        //       && event.getKeyCode() == KeyEvent.VK_BACK_SPACE
        //       && Mode.INSERT.isActive()) {
        //     return false;
        //   } else {
        //     return true;
        //   }
        // }

        // In insert mode, pass keypress through to editing area
        // mode unless escape pressed
        // TODO: Change this so that it also sends Ctrl- sequences
        // to KeySequence#apply() for processing
        // if (Mode.INSERT.isActive() &&
        //     (int)event.getKeyChar() != KeyEvent.VK_ESCAPE) {
        //   return false;
        // }

        String keyString = determineKeyString(event);

        if (keyChords.isActive()) {
          keyChords.process(keyString);
        } else {
          keySequence.apply(keyString);
        }
        
        // This next line should be applied for each case as
        // appropriate in keyChord section, or uncommented if
        // keyChord section is not implemented.

        // Consume keypress
        return true;

      }
    });
  }


  private static String determineKeyString(KeyEvent event) {
    String keyString = null;
    char keyChar = event.getKeyChar();

    // Temporary logging
    if (event.getID() == KeyEvent.KEY_TYPED) {
      Log.log("Key typed event char: " + event.getKeyChar()
              + "(int)char: " + (int)event.getKeyChar() + " code: " + event.getKeyCode());
    } else if (event.getID() == KeyEvent.KEY_PRESSED) {
      Log.log("Key pressed event: " + event.getKeyCode());
    }

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

  private static boolean isOutsideMainEditingArea(KeyEvent event) {
    return !(event.getComponent().getClass() == EditorTextArea3.class);
  }
} 
