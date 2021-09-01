package org.omegat.plugins.vimish;

import org.omegat.util.Log;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import javax.swing.table.AbstractTableModel;

@SuppressWarnings("serial")
public class VimishTableModel extends AbstractTableModel {
  private List<String[]> keyValuePairs;

  public VimishTableModel(List<String[]> keyValuePairs) {
    this.keyValuePairs = keyValuePairs;
  }

  public Object getValueAt(int rowIndex, int columnIndex) {
    String[] row = keyValuePairs.get(rowIndex);
    return row[columnIndex];
  }

  public void setValueAt(Object value, int rowIndex, int columnIndex) {
    String[] row = keyValuePairs.get(rowIndex);
    row[columnIndex] = (String) value;
    fireTableDataChanged();
  }

  public Object getModel() {
    return this;
  }

  public void refreshWith(List<String[]> keyValuePairs) {
    this.keyValuePairs = keyValuePairs;
    fireTableDataChanged();
  }

  public Map<String, String> getKeyMappingsHash() {
    Map<String, String> keyMappingsHash = new LinkedHashMap<String, String>();
    keyValuePairs.forEach(array -> {
      keyMappingsHash.put(array[0], array[1]);
    });
    return keyMappingsHash;
  }

  public int getRowCount() {
      return keyValuePairs.size();
  }

  public int getColumnCount() {
      return 2;
  }

  /** Adds a new empty mapping.*/
  public int addRow() {
    int rows = keyValuePairs.size();
    keyValuePairs.add(new String[] {"", ""});
    fireTableRowsInserted(rows, rows);
    return rows;
  }

  public void removeRow(int row) {
    keyValuePairs.remove(row);
    fireTableRowsDeleted(row, row);
  }

  public String getColumnName(int column) {
    switch (column) {
      case 0:
        return "Keys";
      case 1:
        return "Mapped to";
    }
    return "";
  }

  public boolean isCellEditable(int rowIndex, int columnIndex) {
      return true;
  }
}
