package org.omegat.plugins.vimish;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;
import java.util.Hashtable;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter.HighlightPainter;
import org.omegat.core.data.SourceTextEntry;
import org.omegat.gui.editor.mark.IMarker;
import org.omegat.gui.editor.mark.Mark;

public class VimishVisualMarker implements IMarker {
  static private Hashtable<String, Integer> markPoints =
      new Hashtable<String, Integer>();
  static private MarkOrientation markOrientation;

  static void setMarkStart(int markStart) {
    markPoints.put("start", markStart);
  }

  static void setMarkOrientation(MarkOrientation orientation) {
    markOrientation = orientation;
  }

  static MarkOrientation getMarkOrientation() {
    return markOrientation;
  }

  static void setMarkEnd(int markEnd) {
    markPoints.put("end", markEnd);
  }

  static Integer getMarkStart() {
    if (markPoints.isEmpty())
      return null;

    return markPoints.get("start");
  }

  static Integer getMarkEnd() {
    if (markPoints.isEmpty()) {
      return null;
    }
    
    return markPoints.get("end");
  }

  static void resetMarks() {
    markPoints = new Hashtable<String, Integer>();
    markOrientation = MarkOrientation.NONE;
  }

  @Override
  public List<Mark> getMarksForEntry(SourceTextEntry sourceTextEntry,
                                     java.lang.String sourceText,
                                     java.lang.String translationText,
                                     boolean isActive) throws Exception {
    Color color = new Color(184, 207, 229);
    final HighlightPainter PAINTER = new DefaultHighlighter.DefaultHighlightPainter(color);

    List<Mark> marks = new LinkedList<Mark>();
    if (isActive && translationText != null && !markPoints.isEmpty()) {
      Mark mark = new Mark(Mark.ENTRY_PART.TRANSLATION, getMarkStart(), getMarkEnd());
      mark.painter = PAINTER;
      marks.add(mark);
    }
    return marks;
  }
}
