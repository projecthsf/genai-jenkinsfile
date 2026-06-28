package io.genai.jenkins;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

/**
 * "Wrap line in script { }" — available with the caret on a non-blank line
 * inside a {@code steps} block, for dropping into scripted (Groovy) pipeline.
 */
public final class WrapInScriptIntention implements IntentionAction {

    @Override public @NotNull String getText() { return JenkinsfileBundle.message("intention.wrapScript.text"); }
    @Override public @NotNull String getFamilyName() { return JenkinsfileBundle.message("intention.familyName"); }
    @Override public boolean startInWriteAction() { return true; }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        if (!(file instanceof JenkinsfileFile) || editor == null) return false;
        int offset = editor.getCaretModel().getOffset();
        if (JfPsi.enclosing(file.findElementAt(offset), "steps") == null) return false;
        String line = currentLine(editor.getDocument(), offset);
        return !line.isBlank() && !line.strip().startsWith("script");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        Document doc = editor.getDocument();
        int offset = editor.getCaretModel().getOffset();
        int line = doc.getLineNumber(offset);
        int start = doc.getLineStartOffset(line);
        int end = doc.getLineEndOffset(line);
        String text = doc.getText(new TextRange(start, end));

        String indent = JenkinsfileEdits.indentOfLine(doc, offset);
        String body = text.strip();
        String replacement = indent + "script {\n" + indent + "    " + body + "\n" + indent + "}";
        doc.replaceString(start, end, replacement);
    }

    private static String currentLine(Document doc, int offset) {
        int line = doc.getLineNumber(offset);
        return doc.getText(new TextRange(doc.getLineStartOffset(line), doc.getLineEndOffset(line)));
    }
}
