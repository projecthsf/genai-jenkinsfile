package io.genai.jenkins;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * When you type {@code (} or {@code ,} in a step call, shows the parameter-info hint;
 * typing {@code (} also opens completion so a known step's parameter names appear
 * immediately (no need to press ⌃Space).
 */
public final class JenkinsfileTypedHandler extends TypedHandlerDelegate {

    @Override
    public @NotNull Result checkAutoPopup(char charTyped, @NotNull Project project,
                                          @NotNull Editor editor, @NotNull PsiFile file) {
        if (!(file instanceof JenkinsfileFile)) return Result.CONTINUE;
        if (charTyped == '(' || charTyped == ',') {
            AutoPopupController controller = AutoPopupController.getInstance(project);
            controller.autoPopupParameterInfo(editor, null);
            if (charTyped == '(') controller.scheduleAutoPopup(editor);
            return Result.STOP;
        }
        return Result.CONTINUE;
    }
}
