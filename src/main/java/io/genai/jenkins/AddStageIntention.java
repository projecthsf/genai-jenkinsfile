package io.genai.jenkins;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import io.genai.jenkins.psi.JfBlock;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** "Add new stage" — available with the caret inside a {@code stages} block. */
public final class AddStageIntention implements IntentionAction {

    @Override public @NotNull String getText() { return JenkinsfileBundle.message("intention.addStage.text"); }
    @Override public @NotNull String getFamilyName() { return JenkinsfileBundle.message("intention.familyName"); }
    @Override public boolean startInWriteAction() { return true; }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return file instanceof JenkinsfileFile && editor != null && stagesAt(editor, file) != null;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        JfBlock stages = stagesAt(editor, file);
        if (stages == null) return;
        Document doc = editor.getDocument();
        int open = JfPsi.openBraceOffset(stages);
        JenkinsfileEdits.insertFirstChild(doc, open, List.of(
                "stage('') {",
                "    steps {",
                "    }",
                "}"));
        editor.getCaretModel().moveToOffset(open + 1 + "\n    stage('".length());
    }

    private static JfBlock stagesAt(Editor editor, PsiFile file) {
        return JfPsi.enclosing(file.findElementAt(editor.getCaretModel().getOffset()), "stages");
    }
}
