package org.omegat.plugins.vimish;

// import org.omegat.util.Log;

import java.util.HashMap;

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

  void storeSmallDeletion(String content) {
    store("unnamed", content);
    store("-", content);
  }
  
  void storeBigDeletion(String content) {
    store("unnamed", content);
    shiftContents();
    store("1", content);
  }

  void storeYankedText(String content) {
    store("unnamed", content);
    store("0", content);
  }

  private void store(String key, String content) {
    registerData.put(key, content);
  }

  String retrieve(String key) {
    return registerData.getOrDefault(key, "");
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
    

  
