package org.omegat.plugins.vimish;

import java.util.Collections;
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
}
