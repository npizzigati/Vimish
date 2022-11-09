package org.omegat.plugins.vimish;

// import org.omegat.util.Log;

import java.util.HashMap;
import org.omegat.util.Log;

class Registers {
  private static Registers instance;

  private HashMap<String, String> registerData =
      new HashMap<String, String>();

  private Registers() {}

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
    if (Util.isLowerCase(registerKey)) {
      registerData.put(registerKey, content);
    } else {
      String lowerCaseRegKey = registerKey.toLowerCase();
      String currentContent = retrieve(lowerCaseRegKey);
      registerData.put(lowerCaseRegKey, currentContent + content);
    }
  }

  String retrieve(String registerKey) {
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
}
