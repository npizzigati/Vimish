package org.omegat.plugins.vimish;

import java.util.HashMap;

class Registers {
  private static Registers instance;

  private HashMap<String, String> registers =
      new HashMap<String, String>();

  private Registers() {}

  static Registers getRegisters() {
    if (instance == null) {
      instance = new Registers();
    }
    return instance;
  }
  
  void store(String content) {
    shiftContents();
    registers.put("0", content);
  }

  String retrieve(String key) {
    return registers.get(key);
  }

  void shiftContents() {
    for (Integer i = 0; i < 9; i++) {
      String currentRegisterKey = i.toString();
      String nextRegisterKey = (Integer.valueOf(i + 1)).toString(); 

      String currentRegisterContent =
        registers.getOrDefault(currentRegisterKey, "");
      registers.put(nextRegisterKey, currentRegisterContent);
    }
  }
}
    

  
