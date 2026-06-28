package io.genai.jenkins;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import io.genai.jenkins.psi.JfBlock;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * "Add 'post' block" — available with the caret inside a {@code pipeline} or
 * {@code stage} that has no {@code post} yet. Inserts it after {@code stages}
 * (pipeline) or {@code steps} (stage) to respect declarative section order.
 * Not offered directly inside {@code stages}, where {@code post} isn't valid.
 */
public final class AddPostBlockIntention implements IntentionAction {

    @Override public @NotNull String getText() { return JenkinsfileBundle.message("intention.addPost.text"); }
    @Override public @NotNull String getFamilyName() { return JenkinsfileBundle.message("intention.familyName"); }
    @Override public boolean startInWriteAction() { return true; }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        if (!(file instanceof JenkinsfileFile) || editor == null) return false;
        JfBlock owner = owner(file, editor);
        return owner != null && !JfPsi.hasChild(owner, "post");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        JfBlock owner = owner(file, editor);
        if (owner == null || JfPsi.hasChild(owner, "post")) return;
        Document doc = editor.getDocument();
        JfBlock after = JfPsi.child(owner, "pipeline".equals(JfPsi.name(owner)) ? "stages" : "steps");
        int afterOffset = after != null ? JfPsi.closeBraceEndOffset(after) : -1;
        JenkinsfileEdits.insertAfter(doc, JfPsi.openBraceOffset(owner), afterOffset, List.of(
                "post {",
                "    always {",
                "    }",
                "}"));
    }

    private static JfBlock owner(PsiFile file, Editor editor) {
        PsiElement at = file.findElementAt(editor.getCaretModel().getOffset());
        JfBlock innermost = JfPsi.innermost(at);
        if (innermost != null && "stages".equals(JfPsi.name(innermost))) return null;  // post invalid here
        return JfPsi.enclosing(at, "stage", "pipeline");
    }
}
