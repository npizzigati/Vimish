package org.omegat.plugins.vimish;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import javax.swing.table.AbstractTableModel;

@SuppressWarnings("serial")
public class VimishTableModel extends AbstractTableModel {
  private List<String[]> keyValuePairs;

  public VimishTableModel(Map<String, String> keyTable) {
    this.keyValuePairs = getKeyValuePairs(keyTable);
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

  public void refreshWith(Map<String, String> keyTable) {
    this.keyValuePairs = getKeyValuePairs(keyTable);
    fireTableDataChanged();
  }

  public Map<String, String> getKeyTable() {
    Map<String, String> keyTable = new LinkedHashMap<String, String>();
    keyValuePairs.forEach(array -> {
      keyTable.put(array[0], array[1]);
    });
    return keyTable;
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

  private List<String[]> getKeyValuePairs(Map<String, String> keyMappings) {
    List<String[]> keyValuePairs = new LinkedList<String[]>();

    if (keyMappings != null) {
      keyMappings.forEach((k, v) -> {
        keyValuePairs.add(new String[] { k, v });
      });
    }
    return keyValuePairs;
  }
}
