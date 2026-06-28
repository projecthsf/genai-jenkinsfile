package io.genai.jenkins;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import io.genai.jenkins.psi.JenkinsfileTypes;
import io.genai.jenkins.psi.JfBlock;
import org.jetbrains.annotations.NotNull;

/**
 * Two context checks, both data-driven from the stub catalog:
 * <ul>
 *   <li><b>placement</b> — a directive used in a block that doesn't allow it
 *       ({@code @allowedIn}), e.g. {@code agent} only in {@code pipeline}/{@code stage};</li>
 *   <li><b>cardinality</b> — a directive that may appear only once ({@code @unique})
 *       used twice in the same block, e.g. two {@code agent} or two {@code always}.</li>
 * </ul>
 */
public final class JenkinsfileContextInspection extends LocalInspectionTool {

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        JenkinsfileCatalog catalog = JenkinsfileCatalog.getInstance(holder.getFile().getProject());

        return new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (element instanceof JfBlock block) {
                    checkDuplicates(block);   // flag duplicate @unique directives among this block's children
                    // Block-form directive placement: agent { }, options { }, steps { } …
                    StubEntry e = catalog.entry(JfPsi.name(block));
                    if (e == null || e.allowedIn.isEmpty()) return;
                    JfBlock parent = PsiTreeUtil.getParentOfType(block, JfBlock.class);
                    check(e, parent == null ? null : JfPsi.name(parent), JfPsi.nameElement(block));
                } else if (isBareDirective(element)) {
                    // Bare directive: agent any, agent none.
                    JfBlock parent = PsiTreeUtil.getParentOfType(element, JfBlock.class);
                    if (parent == null || JfPsi.nameElement(parent) == element) return;
                    StubEntry e = catalog.entry(element.getText());
                    if (e != null && !e.allowedIn.isEmpty()) check(e, JfPsi.name(parent), element);
                }
            }

            private boolean isBareDirective(PsiElement element) {
                ASTNode node = element.getNode();
                if (node == null || element.getFirstChild() != null) return false;
                if (node.getElementType() != JenkinsfileTypes.KEYWORD
                        && node.getElementType() != JenkinsfileTypes.IDENTIFIER) return false;
                return element.getParent() instanceof JfBlock;   // a statement in a block, not inside args
            }

            private void check(StubEntry e, String parentName, PsiElement anchor) {
                if (parentName != null && e.allowedIn.contains(parentName)) return;
                holder.registerProblem(anchor,
                        JenkinsfileBundle.message("inspection.context.notAllowed", e.name, String.join(", ", e.allowedIn)));
            }

            /** Flag the 2nd+ occurrence of a {@code @unique} directive among a block's direct children. */
            private void checkDuplicates(JfBlock container) {
                PsiElement self = JfPsi.nameElement(container);
                java.util.Set<String> seen = new java.util.HashSet<>();
                for (ASTNode c = container.getNode().getFirstChildNode(); c != null; c = c.getTreeNext()) {
                    PsiElement psi = c.getPsi();
                    String name;
                    PsiElement anchor;
                    if (psi instanceof JfBlock cb) {
                        name = JfPsi.name(cb);
                        anchor = JfPsi.nameElement(cb);
                    } else if (psi != self && isBareDirective(psi)) {
                        name = psi.getText();
                        anchor = psi;
                    } else {
                        continue;
                    }
                    StubEntry e = name == null ? null : catalog.entry(name);
                    if (e == null || !e.unique) continue;
                    if (!seen.add(name)) {
                        holder.registerProblem(anchor, JenkinsfileBundle.message("inspection.context.duplicate", name));
                    }
                }
            }
        };
    }
}
