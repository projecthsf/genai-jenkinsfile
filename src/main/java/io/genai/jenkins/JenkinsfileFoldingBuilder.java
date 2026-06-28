package io.genai.jenkins;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import io.genai.jenkins.psi.JenkinsfileTypes;
import io.genai.jenkins.psi.JfBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Code folding: collapse each block's {@code { … }}, multi-line strings
 * ({@code """…"""}, {@code '''…'''}) and multi-line block comments. The text before a
 * block's brace (its name and args) stays visible — e.g. {@code stage('Build') {…}}.
 */
public final class JenkinsfileFoldingBuilder extends FoldingBuilderEx implements DumbAware {

    @Override
    public FoldingDescriptor @NotNull [] buildFoldRegions(@NotNull PsiElement root, @NotNull Document document, boolean quick) {
        List<FoldingDescriptor> regions = new ArrayList<>();

        // Block bodies: from '{' to '}'.
        for (JfBlock block : PsiTreeUtil.findChildrenOfType(root, JfBlock.class)) {
            ASTNode lbrace = block.getNode().findChildByType(JenkinsfileTypes.LBRACE);
            ASTNode rbrace = block.getNode().findChildByType(JenkinsfileTypes.RBRACE);
            if (lbrace == null || rbrace == null) continue;
            addIfMultiline(regions, block.getNode(),
                    new TextRange(lbrace.getStartOffset(), rbrace.getTextRange().getEndOffset()), document);
        }

        // Multi-line strings and block comments.
        for (PsiElement leaf = PsiTreeUtil.getDeepestFirst(root); leaf != null; leaf = PsiTreeUtil.nextLeaf(leaf)) {
            ASTNode node = leaf.getNode();
            if (node == null) continue;
            IElementType t = node.getElementType();
            if (t == JenkinsfileTypes.STRING || t == JenkinsfileTypes.BLOCK_COMMENT) {
                addIfMultiline(regions, node, leaf.getTextRange(), document);
            }
        }
        return regions.toArray(FoldingDescriptor.EMPTY_ARRAY);
    }

    private static void addIfMultiline(List<FoldingDescriptor> out, ASTNode node, TextRange range, Document doc) {
        if (range.getLength() <= 0 || range.getEndOffset() > doc.getTextLength()) return;
        if (doc.getLineNumber(range.getStartOffset()) == doc.getLineNumber(range.getEndOffset() - 1)) return; // single line
        out.add(new FoldingDescriptor(node, range));
    }

    @Override
    public @Nullable String getPlaceholderText(@NotNull ASTNode node) {
        IElementType t = node.getElementType();
        if (t == JenkinsfileTypes.BLOCK_COMMENT) return "/*…*/";
        if (t == JenkinsfileTypes.STRING) {
            String text = node.getText();
            if (text.startsWith("\"\"\"")) return "\"\"\"…\"\"\"";
            if (text.startsWith("'''")) return "'''…'''";
            return text.startsWith("\"") ? "\"…\"" : "'…'";
        }
        return "{…}";   // a block body
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode node) {
        return false;
    }
}
