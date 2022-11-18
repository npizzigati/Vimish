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
import org.omegat.gui.preferences.PreferencesControllers;


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
        PreferencesControllers.addSupplier(VimishOptionsController::new);
        installEntryListener();
        Log.log("VIMISH PLUGIN LOADED");
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
