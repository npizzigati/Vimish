package org.omegat.plugins.vimish;

import org.omegat.gui.preferences.IPreferencesController;
import org.omegat.util.Log;
import org.omegat.util.StaticUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class VimishOptionsController implements IPreferencesController {
  private VimishOptionsPanel panel;
  private ObjectMapper jsonMapper = new ObjectMapper();
  private Configuration configuration = ConfigurationManager.getConfigurationManager().getConfiguration();

  private final String VIEW_NAME = "Vimish";
  private final String PREFERENCES_FILENAME = "VimishPrefs.json";

  private File preferencesFile = new File(StaticUtils.getConfigDir(), PREFERENCES_FILENAME);

  /**
   * An interface used by observers interested in knowing when a preference has
   * been altered that requires the application to be restarted or the project to
   * be reloaded.
   */
  interface FurtherActionListener {
    void setReloadRequired(boolean reloadRequired);

    void setRestartRequired(boolean restartRequired);
  }

  /**
   * Get the GUI (the "view") controlled by this controller. This should not be a
   * window (e.g. JDialog, JFrame) but rather a component embeddable in a window
   * (e.g. JPanel).
   */
  @Override
  public Component getGui() {
    if (panel == null) {
      initGui();
      initFromConfiguration();
    }
    return panel;
  };

  private void initGui() {
    Log.log("Initializing GUI");
    panel = new VimishOptionsPanel();
  }

  protected void initFromConfiguration() {
    boolean moveCursorBack = configuration.moveCursorBack;
    panel.moveCursorBackCheckBox.setSelected(moveCursorBack); 
  }

  @Override
  public void addFurtherActionListener(IPreferencesController.FurtherActionListener listener) {
  };

  @Override
  public void removeFurtherActionListener(IPreferencesController.FurtherActionListener listener) {
  };

  /**
   * Returns whether a preference has been altered such as to require the
   * application to be restarted.
   */
  @Override
  public boolean isRestartRequired() {
    return false;
  };

  /**
   * Returns whether a preference has been altered such as to require the project
   * to be reloaded.
   */
  @Override
  public boolean isReloadRequired() {
    return false;
  };

  /**
   * Implementors should override this to return the name of the view as shown in
   * the view tree.
   */
  @Override
  public String toString() {
    return VIEW_NAME; 
  };


  /**
   * Get the parent view in the view tree. Implementors should override this to
   * return the class of the desired parent; by default this is the Plugins view.
   */
  // @Override
  // public Class<? extends IPreferencesController> getParentViewClass() {
  //   return null;
  // }

  /**
   * Commit changes.
   */
  @Override
  public void persist() {
    File configurationFile = ConfigurationManager.getConfigurationManager().configurationFile;
    configuration.moveCursorBack = panel.moveCursorBackCheckBox.isSelected();
    try {
      jsonMapper.writeValue(configurationFile, configuration);
    } catch(JsonProcessingException jpe) {
      Log.log("Unable to create JSON: " + jpe);
    } catch(IOException ioe) {
      Log.log("Unable to write Vimish preferences file: " + ioe);
    }
  };

  /**
   * Validate the current preferences. Implementors should override to implement
   * validation logic as necessary.
   * <p>
   * When validation fails, implementors should <i>not</i> raise dialogs; instead
   * they should offer feedback within the view GUI.
   *
   * @return True if the settings are valid and OK to be persisted; false if not
   */
  @Override
  public boolean validate() {
    return true;
  }

  /**
   * Restore preferences controlled by this view to their current persisted state.
   */
  @Override
  public void undoChanges() {
  };

  /**
   * Restore preferences controlled by this view to their default state.
   */
  @Override
  public void restoreDefaults() {
  };
}
