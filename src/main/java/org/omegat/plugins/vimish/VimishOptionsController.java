package org.omegat.plugins.vimish;

import org.omegat.gui.preferences.IPreferencesController;
import org.omegat.util.Log;

import java.awt.Component;
import java.awt.Dimension;
import javax.swing.table.TableModel;
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
    boolean moveCursorBack = configuration.getConfigMoveCursorBack();
    Map<String, String> keyMappingsHash = configuration.getConfigKeyMappingsHash();

    panel = new VimishOptionsPanel();
    panel.moveCursorBackCheckBox.setSelected(moveCursorBack);

    List<String[]> keyValuePairs = getKeyValuePairs(keyMappingsHash);
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

  }

  private List<String[]> getKeyValuePairs(Map<String, String> keyMappingsHash) {

    List<String[]> keyValuePairs = new LinkedList<String[]>();
    keyMappingsHash.forEach((k, v) -> {
      keyValuePairs.add(new String[] { k, v });
    });
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

    // Get table data
    // VimishTableModel tableModel = (VimishTableModel) panel.keyMappingsTable.getModel();
    Map<String, String> keyMappingsHash = tableModel.getKeyMappingsHash();
    ConfigurationData newData = new ConfigurationData();
    newData.moveCursorBack = panel.moveCursorBackCheckBox.isSelected();
    newData.keyMappings = keyMappingsHash;
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
