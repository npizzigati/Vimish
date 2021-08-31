package org.omegat.plugins.vimish;

import org.omegat.gui.preferences.IPreferencesController;
import org.omegat.util.Log;

import java.awt.Component;
import java.awt.Dimension;
import javax.swing.table.TableModel;
import javax.swing.JComboBox;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.List;

class VimishOptionsController implements IPreferencesController {
  private VimishOptionsPanel panel;
  private Configuration configuration = Configuration.getConfiguration();
  VimishTableModel tableModel;

  private final String VIEW_NAME = "Vimish";
  private final int MAX_ROW_COUNT = 4;
  private Map<String, String> keyMappingsHash;

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

    // Start combo box at normal mode
    panel.modeSelector.setSelectedIndex(0);
    panel.modeSelector.addActionListener(event -> {
      @SuppressWarnings("unchecked")
      JComboBox<String> comboBox = (JComboBox<String>) event.getSource();
      String mode = (String) comboBox.getSelectedItem();
      switch (mode) {
        case "Normal":
          keyMappingsHash = configuration.getKeyMappings().normalModeKeyMappings;
          break;
        case "Visual":
          keyMappingsHash = configuration.getKeyMappings().visualModeKeyMappings;
          break;
        case "Insert":
          keyMappingsHash = configuration.getKeyMappings().insertModeKeyMappings;
          break;
      }
      tableModel.refreshWith(getKeyValuePairs());
    });

    // Default init setting
    keyMappingsHash = configuration.getKeyMappings().normalModeKeyMappings;

    List<String[]> keyValuePairs = getKeyValuePairs();
    tableModel = new VimishTableModel(keyValuePairs);
    panel.keyMappingsTable.setModel(tableModel);

    panel.keyMappingsTable.getColumnModel().getColumn(0).setPreferredWidth(150);
    panel.keyMappingsTable.getColumnModel().getColumn(1).setPreferredWidth(150);

    Dimension tableSize = panel.keyMappingsTable.getPreferredSize();
    panel.keyMappingsTable.setPreferredScrollableViewportSize(
        new Dimension(tableSize.width, panel.keyMappingsTable.getRowHeight() * MAX_ROW_COUNT));

    panel.keyMappingsAddButton.addActionListener(e -> {
      tableModel.addRow();
      panel.keyMappingsTable.changeSelection(panel.keyMappingsTable.getRowCount() - 1, 0, false, false);
      panel.keyMappingsTable.changeSelection(panel.keyMappingsTable.getRowCount() - 1,
                                             panel.keyMappingsTable.getColumnCount() - 1, false, true);
    });

    panel.keyMappingsRemoveButton.addActionListener(e -> {
      tableModel.removeRow(panel.keyMappingsTable.getSelectedRow());
    });

  }

  private List<String[]> getKeyValuePairs() {
    List<String[]> keyValuePairs = new LinkedList<String[]>();

    if (keyMappingsHash != null) {
      keyMappingsHash.forEach((k, v) -> {
        keyValuePairs.add(new String[] { k, v });
      });
    }
    return keyValuePairs;
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

    Map<String, String> keyMappingsHash = tableModel.getKeyMappingsHash();
    ConfigurationData newData = new ConfigurationData();
    newData.moveCursorBack = panel.moveCursorBackCheckBox.isSelected();
    KeyMappings tmpMappings = new KeyMappings();
    tmpMappings.normalModeKeyMappings = keyMappingsHash;
    newData.keyMappings = tmpMappings;
    configuration.writeToFile(newData);

    // Flag key equivalency (chord, mapping, abbreviation) changes
    configuration.flagKeyEquivalenciesAsChanged();
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
