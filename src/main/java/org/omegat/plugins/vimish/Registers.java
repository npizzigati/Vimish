package org.omegat.plugins.vimish;

// import org.omegat.util.Log;

import java.util.HashMap;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Clipboard;
import java.io.IOException;
import java.awt.datatransfer.UnsupportedFlavorException;
import org.omegat.util.Log;

class Registers {
  private static Registers instance;

  private Clipboard systemClipboard;
  private HashMap<String, String> registerData =
      new HashMap<String, String>();

  private Registers() {
    systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
  }

  static Registers getRegisters() {
    if (instance == null) {
      instance = new Registers();
    }
    return instance;
  }

  void storeSmallDeletion(String registerKey, String content) {
    store("\"", content);
    if (Util.isEmpty(registerKey)) {
      store("-", content);
    } else {
      store(registerKey, content);
    }
  }

  void storeBigDeletion(String registerKey, String content) {
    store("\"", content);
    if (Util.isEmpty(registerKey)) {
      shiftContents();
      store("1", content);
    } else {
      store(registerKey, content);
    }
  }

  void storeYank(String registerKey, String content) {
    store("\"", content);
    if (Util.isEmpty(registerKey)) {
      store("0", content);
    } else {
      store(registerKey, content);
    }
  }

  private void store(String registerKey, String content) {
    if (registerKey.equals("*") | registerKey.equals("+")) {
      writeToSystemClipboard(content);
      return;
    }
    if (Util.isLowerCase(registerKey)) {
      registerData.put(registerKey, content);
    } else {
      String lowerCaseRegKey = registerKey.toLowerCase();
      String currentContent = retrieve(lowerCaseRegKey);
      registerData.put(lowerCaseRegKey, currentContent + content);
    }
  }

  String retrieve(String registerKey) {
    if (registerKey.equals("*") || registerKey.equals("+")) {
      return readSystemClipboard();
    }
    return registerData.getOrDefault(registerKey.toLowerCase(), "");
  }

  void shiftContents() {
    for (Integer i = 9; i > 1; i--) {
      String currentRegisterKey = i.toString();
      String previousRegisterKey = (Integer.valueOf(i - 1)).toString();

      String previousRegisterContent = retrieve(previousRegisterKey);
      store(currentRegisterKey, previousRegisterContent);
    }
  }

  void writeToSystemClipboard(String content) {
    systemClipboard.setContents(new StringSelection(content), null);
  }

  String readSystemClipboard() {
    String contents = "";
    try {
      contents = (String) systemClipboard.getData(DataFlavor.stringFlavor);
    } catch (IOException | UnsupportedFlavorException e) {
      Log.log("Unable to access system clipboard: " + e);
    }
    return contents;
  }
}
