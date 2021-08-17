/**
 * Vimish
 * Vim-style editing for OmegaT
 * 
 * @author Nick Pizzigati
 */

package org.omegat.plugins.vimish;

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
  static boolean isFirstLoad = true;
  /**
   * Plugin loader
   */
  public static void loadPlugins() {
    Core.registerMarkerClass(VimishVisualMarker.class);
    CoreEvents.registerApplicationEventListener(new IApplicationEventListener() {
      @Override
      public void onApplicationStartup() {
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
          Dispatcher dispatcher = new Dispatcher();
          dispatcher.installKeyEventDispatcher();
          isFirstLoad = false;
        }
      }
    });
  }
} 
