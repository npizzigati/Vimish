package org.omegat.plugins.vimish;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import javax.swing.text.JTextComponent;


import org.omegat.core.Core;
import org.omegat.gui.editor.EditorController;
import org.omegat.util.Log;

class Util {
  static JTextComponent getEditingArea() {
    EditorController editor = (EditorController) Core.getEditor();
    JTextComponent area = null;
    try {
        java.lang.reflect.Field protectedField = EditorController.class.getDeclaredField("editor");
        protectedField.setAccessible(true);
        area = (JTextComponent) protectedField.get(editor);
    } catch(NoSuchFieldException nsfe) {
        Log.log(nsfe);
    } catch(IllegalAccessException iae) {
        Log.log(iae);
    }
    return area;
  }

  static String repeat(String word, int times) {
    return String.join("", Collections.nCopies(times, word));
  }

  static boolean isEmpty(String item) {
    return item == null || item.equals("");
  }

  static boolean isLowerCase(String text) {
    return text.toLowerCase().equals(text);
  }

  /**
   * Normalize all special keys in users hash to notation we use internally
   */
  static Map<String, String> normalizeTable(Map<String, String> userHash) {
    if (userHash == null) {
      return userHash;
    }
    Map<String, String> normalizedHash = new HashMap<String, String>();
    userHash.forEach((k, v) -> {
        String normalizedKey = normalizeString(k);
        String normalizedValue = normalizeString(v);
        normalizedHash.put(normalizedKey, normalizedValue);
      });

    return normalizedHash;
  }

  static String normalizeString(String str) {
    str = str.replaceAll("(?i)(<ESC>|<ESCAPE>)", "\u2732ESC\u2732");
    str = str.replaceAll("(?i)(<BS>|<BACKSPACE)", "\u2732BACKSPACE\u2732");
    str = str.replaceAll("(?i)(<CR>|<RETURN>|<ENTER>)", "\u2732ENTER\u2732");
    str = str.replaceAll("(?i)(<DEL>|<DELETE>)", "\u2732DEL\u2732");
    str = str.replaceAll("(?i)(<LEFT>)", "\u2732LEFT\u2732");
    str = str.replaceAll("(?i)(<RIGHT>)", "\u2732RIGHT\u2732");
    str = str.replaceAll("(?i)(<TAB>)", "\u2732TAB\u2732");
    str = str.replaceAll("(?i)(<S-TAB>)", "\u2732S-TAB\u2732");
    return str;
  }
}
