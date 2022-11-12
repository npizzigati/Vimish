package org.omegat.plugins.vimish;

import org.omegat.gui.preferences.BasePreferencesController;

import java.awt.Component;
import java.awt.Dimension;
import javax.swing.JComboBox;

import java.util.Map;

class VimishOptionsController extends BasePreferencesController {
  private VimishOptionsPanel panel;
  private Configuration configuration;
  private KeyMappings allKeyMappings;
  private KeyChords allKeyChords;
  VimishTableModel keyMappingsTableModel;
  VimishTableModel keyChordsTableModel;

  private final String VIEW_NAME = "Vimish";
  private final int MAX_ROW_COUNT = 4;

  VimishOptionsController() {
    configuration = Configuration.getConfiguration();
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
      initFromPrefs();
    }
    return panel;
  };

  private void initGui() {
    panel = new VimishOptionsPanel();
    // Key Mappings
    // Start mode selector combo box at normal mode
    panel.keyMappingsModeSelector.setSelectedIndex(0);
    panel.keyMappingsModeSelector.addActionListener(event -> {
      @SuppressWarnings("unchecked")
      JComboBox<String> comboBox = (JComboBox<String>) event.getSource();
      String mode = (String) comboBox.getSelectedItem();
      Map<String, String> selectedKeyMappings = null;
      switch (mode) {
        case "Normal":
          selectedKeyMappings = allKeyMappings.normalModeKeyMappings;
          break;
        case "Visual":
          selectedKeyMappings = allKeyMappings.visualModeKeyMappings;
          break;
        case "Insert":
          selectedKeyMappings = allKeyMappings.insertModeKeyMappings;
          break;
      }
      keyMappingsTableModel.refreshWith(selectedKeyMappings);
    });
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
    // Start mode selector combo box at normal mode
    panel.keyChordsModeSelector.setSelectedIndex(0);
    panel.keyChordsModeSelector.addActionListener(event -> {
      @SuppressWarnings("unchecked")
      JComboBox<String> comboBox = (JComboBox<String>) event.getSource();
      String mode = (String) comboBox.getSelectedItem();
      Map<String, String> selectedKeyChords = null;
      switch (mode) {
        case "Normal":
          selectedKeyChords = allKeyChords.normalModeKeyChords;
          break;
        case "Visual":
          selectedKeyChords = allKeyChords.visualModeKeyChords;
          break;
        case "Insert":
          selectedKeyChords = allKeyChords.insertModeKeyChords;
          break;
      }
      keyChordsTableModel.refreshWith(selectedKeyChords);
    });

    Dimension keyChordsTableSize = panel.keyChordsTable.getPreferredSize();
    panel.keyChordsTable.setPreferredScrollableViewportSize(
        new Dimension(keyChordsTableSize.width, panel.keyChordsTable.getRowHeight() * MAX_ROW_COUNT));

    panel.keyChordsAddButton.addActionListener(e -> {
      keyChordsTableModel.addRow();
      panel.keyChordsTable.changeSelection(panel.keyChordsTable.getRowCount() - 1, 0, false, false);
      panel.keyChordsTable.changeSelection(panel.keyChordsTable.getRowCount() - 1,
                                             panel.keyChordsTable.getColumnCount() - 1, false, true);
    });

    panel.keyChordsRemoveButton.addActionListener(e -> {
      keyChordsTableModel.removeRow(panel.keyChordsTable.getSelectedRow());
    });
  }

  @Override
  protected void initFromPrefs() {
    configuration.refresh();
    showData(false);
  }

  private void showData(boolean useDefaults) {
    boolean moveCursorBack;
    if (useDefaults) {
      moveCursorBack = configuration.DEFAULT_MOVE_CURSOR_BACK;
      allKeyMappings = configuration.DEFAULT_KEY_MAPPINGS;
      allKeyChords = configuration.DEFAULT_KEY_CHORDS;
    } else {
      moveCursorBack = configuration.getConfigMoveCursorBack();
      allKeyMappings = configuration.getKeyMappings();
      allKeyChords = configuration.getKeyChords();
    }
    panel.moveCursorBackCheckBox.setSelected(moveCursorBack);
    keyMappingsTableModel =
      new VimishTableModel(allKeyMappings.normalModeKeyMappings);
    panel.keyMappingsTable.setModel(keyMappingsTableModel);
    panel.keyMappingsModeSelector.setSelectedIndex(0);
    keyMappingsTableModel.addTableModelListener(event -> {
        Map<String, String> keyMappings =
          keyMappingsTableModel.getKeyTable();
        String mode =
          (String) panel.keyMappingsModeSelector.getSelectedItem();
        switch (mode) {
          case "Normal":
            allKeyMappings.normalModeKeyMappings = keyMappings;
            break;
          case "Visual":
            allKeyMappings.visualModeKeyMappings = keyMappings;
            break;
          case "Insert":
            allKeyMappings.insertModeKeyMappings = keyMappings;
            break;
        }
      });
    panel.keyMappingsTable.getColumnModel().getColumn(0).setPreferredWidth(150);
    panel.keyMappingsTable.getColumnModel().getColumn(1).setPreferredWidth(150);

    keyChordsTableModel =
      new VimishTableModel(allKeyChords.normalModeKeyChords);
    panel.keyChordsTable.setModel(keyChordsTableModel);
    panel.keyChordsModeSelector.setSelectedIndex(0);
    keyChordsTableModel.addTableModelListener(event -> {
        Map<String, String> keyChordsHash =
          keyChordsTableModel.getKeyTable();
        String mode =
          (String) panel.keyChordsModeSelector.getSelectedItem();
        switch (mode) {
          case "Normal":
            allKeyChords.normalModeKeyChords = keyChordsHash;
            break;
          case "Visual":
            allKeyChords.visualModeKeyChords = keyChordsHash;
            break;
          case "Insert":
            allKeyChords.insertModeKeyChords = keyChordsHash;
            break;
        }
      });
    panel.keyChordsTable.getColumnModel().getColumn(0).setPreferredWidth(150);
    panel.keyChordsTable.getColumnModel().getColumn(1).setPreferredWidth(150);
  }

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

    newData.moveCursorBack = panel.moveCursorBackCheckBox.isSelected();
    newData.keyMappings = allKeyMappings;
    newData.keyChords = allKeyChords;
    configuration.writeToFile(newData);

    // Flag key equivalency (chord, mapping, abbreviation) changes
    configuration.flagKeyTablesRefreshNeeded();
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
   * Restore preferences controlled by this view to their default state.
   */
  @Override
  public void restoreDefaults() {
    showData(true);
  };
}
