package org.omegat.plugins.vimish;

import org.omegat.gui.preferences.IPreferencesController;
import org.omegat.util.Log;

import java.awt.Component;
import java.awt.Dimension;
import javax.swing.JComboBox;

import java.util.Map;

class VimishOptionsController implements IPreferencesController {
  private VimishOptionsPanel panel;
  private Configuration configuration = Configuration.getConfiguration();
  private KeyMappings keyMappings;
  VimishTableModel keyMappingsTableModel;
  VimishTableModel chordsTableModel;

  private final String VIEW_NAME = "Vimish";
  private final int MAX_ROW_COUNT = 4;

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
      initPanel();
    }
    return panel;
  };

  private void initPanel() {

    panel = new VimishOptionsPanel();

    boolean moveCursorBack = configuration.getConfigMoveCursorBack();
    panel.moveCursorBackCheckBox.setSelected(moveCursorBack);

    // Key Mappings
    // Start mode selector combo box at normal mode
    panel.keyMappingsModeSelector.setSelectedIndex(0);

    panel.keyMappingsModeSelector.addActionListener(event -> {
      @SuppressWarnings("unchecked")
      JComboBox<String> comboBox = (JComboBox<String>) event.getSource();
      String mode = (String) comboBox.getSelectedItem();
      Map<String, String> keyMappingsHash = null;
      switch (mode) {
        case "Normal":
          keyMappingsHash = keyMappings.normalModeKeyMappings;
          break;
        case "Visual":
          keyMappingsHash = keyMappings.visualModeKeyMappings;
          break;
        case "Insert":
          keyMappingsHash = keyMappings.insertModeKeyMappings;
          break;
      }
      keyMappingsTableModel.refreshWith(keyMappingsHash);
    });

    keyMappings = configuration.getKeyMappings();

    keyMappingsTableModel =
      new VimishTableModel(keyMappings.normalModeKeyMappings);
    panel.keyMappingsTable.setModel(keyMappingsTableModel);

    keyMappingsTableModel.addTableModelListener(event -> {
        Map<String, String> keyMappingsHash =
          keyMappingsTableModel.getKeyMappingsHash();
        String mode =
          (String) panel.keyMappingsModeSelector.getSelectedItem();
        switch (mode) {
          case "Normal":
            keyMappings.normalModeKeyMappings = keyMappingsHash;
            break;
          case "Visual":
            keyMappings.visualModeKeyMappings = keyMappingsHash;
            break;
          case "Insert":
            keyMappings.insertModeKeyMappings = keyMappingsHash;
            break;
        }
      Log.log("current visual: " + keyMappings.visualModeKeyMappings.toString());
      });

    panel.keyMappingsTable.getColumnModel().getColumn(0).setPreferredWidth(150);
    panel.keyMappingsTable.getColumnModel().getColumn(1).setPreferredWidth(150);

    Dimension keyMappingsTableSize = panel.keyMappingsTable.getPreferredSize();
    panel.keyMappingsTable.setPreferredScrollableViewportSize(
        new Dimension(keyMappingsTableSize.width, panel.keyMappingsTable.getRowHeight() * MAX_ROW_COUNT));

    panel.keyMappingsAddButton.addActionListener(e -> {
      keyMappingsTableModel.addRow();
      panel.keyMappingsTable.changeSelection(panel.keyMappingsTable.getRowCount() - 1, 0, false, false);
      panel.keyMappingsTable.changeSelection(panel.keyMappingsTable.getRowCount() - 1,
                                             panel.keyMappingsTable.getColumnCount() - 1, false, true);
    });

    panel.keyMappingsRemoveButton.addActionListener(e -> {
      keyMappingsTableModel.removeRow(panel.keyMappingsTable.getSelectedRow());
    });

    // Key chords
    Dimension keyChordsTableSize = panel.keyChordsTable.getPreferredSize();
    panel.keyChordsTable.setPreferredScrollableViewportSize(
        new Dimension(keyChordsTableSize.width, panel.keyChordsTable.getRowHeight() * MAX_ROW_COUNT));
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
    ConfigurationData newData = new ConfigurationData();
    newData.keyMappings = keyMappings;
    newData.moveCursorBack = panel.moveCursorBackCheckBox.isSelected();
    configuration.writeToFile(newData);

    // Flag key equivalency (chord, mapping, abbreviation) changes
    configuration.flagKeyEquivalenciesRefreshNeeded();
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
