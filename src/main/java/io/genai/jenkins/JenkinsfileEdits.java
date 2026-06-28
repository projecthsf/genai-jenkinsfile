package io.genai.jenkins;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;

import java.util.List;

/** Small document-editing helpers shared by the intentions/quick-fixes (offset-based). */
public final class JenkinsfileEdits {

    private JenkinsfileEdits() {}

    public static String indentOfLine(Document doc, int offset) {
        int line = doc.getLineNumber(offset);
        int start = doc.getLineStartOffset(line);
        String s = doc.getText(new TextRange(start, doc.getLineEndOffset(line)));
        int i = 0;
        while (i < s.length() && (s.charAt(i) == ' ' || s.charAt(i) == '\t')) i++;
        return s.substring(0, i);
    }

    /** Insert {@code lines} (indented one level in) right after an opening brace. */
    public static void insertFirstChild(Document doc, int openBraceOffset, List<String> lines) {
        doc.insertString(openBraceOffset + 1, render(indentOfLine(doc, openBraceOffset), lines));
    }

    /**
     * Insert {@code lines} after {@code afterOffset} (e.g. a child's closing brace),
     * or right after {@code parentOpenBrace} if {@code afterOffset} is negative.
     * Indentation is taken from the parent's opening-brace line.
     */
    public static void insertAfter(Document doc, int parentOpenBrace, int afterOffset, List<String> lines) {
        int at = afterOffset >= 0 ? afterOffset : parentOpenBrace + 1;
        doc.insertString(at, render(indentOfLine(doc, parentOpenBrace), lines));
    }

    private static String render(String parentIndent, List<String> lines) {
        String inner = parentIndent + "    ";
        StringBuilder sb = new StringBuilder();
        for (String ln : lines) sb.append("\n").append(inner).append(ln);
        return sb.toString();
    }
}
