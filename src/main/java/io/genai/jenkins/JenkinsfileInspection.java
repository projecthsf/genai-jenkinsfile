package io.genai.jenkins;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import io.genai.jenkins.psi.JfBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Structural inspections for declarative pipelines, walking the real PSI:
 * <ul>
 *   <li><b>pipeline</b> without a <code>stages</code> section</li>
 *   <li><b>stage</b> without <code>steps</code> (or nested stages/parallel/matrix)</li>
 * </ul>
 * Each comes with a quick-fix that inserts the missing block.
 */
public final class JenkinsfileInspection extends LocalInspectionTool {

    @Override
    public ProblemDescriptor @Nullable [] checkFile(@NotNull PsiFile file,
                                                    @NotNull InspectionManager manager,
                                                    boolean isOnTheFly) {
        if (!(file instanceof JenkinsfileFile)) return null;

        List<ProblemDescriptor> problems = new ArrayList<>();
        for (JfBlock block : PsiTreeUtil.findChildrenOfType(file, JfBlock.class)) {
            String name = JfPsi.name(block);
            if ("pipeline".equals(name) && !JfPsi.hasChild(block, "stages")) {
                add(problems, manager, isOnTheFly, block,
                        JenkinsfileBundle.message("inspection.pipeline.noStages"), "stages");
            } else if ("stage".equals(name) && !JfPsi.hasChild(block, "steps", "stages", "parallel", "matrix")) {
                add(problems, manager, isOnTheFly, block,
                        JenkinsfileBundle.message("inspection.stage.noSteps"), "steps");
            }
        }
        return problems.toArray(ProblemDescriptor.EMPTY_ARRAY);
    }

    private static void add(List<ProblemDescriptor> out, InspectionManager manager, boolean onTheFly,
                            JfBlock block, String message, String missingChild) {
        PsiElement anchor = JfPsi.nameElement(block);
        out.add(manager.createProblemDescriptor(
                anchor, message, new InsertBlockFix(JfPsi.openBraceOffset(block), missingChild),
                ProblemHighlightType.WARNING, onTheFly));
    }

    /** Quick-fix: insert an empty {@code name { }} block right after a block's opening brace. */
    private static final class InsertBlockFix implements LocalQuickFix {
        private final int openBraceOffset;
        private final String childName;

        InsertBlockFix(int openBraceOffset, String childName) {
            this.openBraceOffset = openBraceOffset;
            this.childName = childName;
        }

        @Override public @NotNull String getFamilyName() { return JenkinsfileBundle.message("quickfix.addBlock", childName); }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiFile file = descriptor.getPsiElement().getContainingFile();
            PsiDocumentManager pdm = PsiDocumentManager.getInstance(project);
            Document doc = pdm.getDocument(file);
            if (doc == null || openBraceOffset < 0 || openBraceOffset >= doc.getTextLength()) return;
            JenkinsfileEdits.insertFirstChild(doc, openBraceOffset, List.of(childName + " {", "}"));
            pdm.commitDocument(doc);
        }
    }
}
