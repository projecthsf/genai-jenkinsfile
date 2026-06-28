package io.genai.jenkins;

import com.intellij.formatting.Block;
import com.intellij.formatting.FormattingContext;
import com.intellij.formatting.FormattingModel;
import com.intellij.formatting.FormattingModelBuilder;
import com.intellij.formatting.FormattingModelProvider;
import com.intellij.formatting.Indent;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Indentation by brace/paren/bracket depth — the one rule that lays out both the
 * declarative skeleton ({@code pipeline → stages → stage → steps}) and the Groovy
 * inside {@code script { }} (including {@code if/else if} chains, {@code try/catch}
 * and closures) correctly.
 *
 * <p>Each token that starts a line is indented to its nesting depth; a line that
 * opens with a closing bracket is dedented one level so {@code }} lines up with the
 * {@code {} that opened it. Tokens that don't start a line, and the inner lines of
 * multi-line strings/comments, keep their existing spacing.
 */
public final class JenkinsfileFormattingModelBuilder implements FormattingModelBuilder {

    @Override
    public @NotNull FormattingModel createModel(@NotNull FormattingContext context) {
        PsiFile file = context.getContainingFile();
        ASTNode root = file.getNode();
        int unit = indentSize(context.getCodeStyleSettings(), file);

        List<Block> children = layout(root, file.getText(), unit);
        Block rootBlock = new JenkinsfileFormatBlock(root, Indent.getNoneIndent(), children);
        return FormattingModelProvider.createFormattingModelForPsiFile(file, rootBlock, context.getCodeStyleSettings());
    }

    private static int indentSize(CodeStyleSettings settings, PsiFile file) {
        int size = settings.getIndentOptionsByFile(file).INDENT_SIZE;
        return size > 0 ? size : 4;
    }

    /** Build one leaf block per token, each with its depth-based indent. */
    private static List<Block> layout(ASTNode root, CharSequence text, int unit) {
        List<ASTNode> leaves = new ArrayList<>();
        collectLeaves(root, leaves);

        List<Block> blocks = new ArrayList<>(leaves.size());
        int depth = 0;
        int prevEnd = 0;
        for (ASTNode leaf : leaves) {
            int start = leaf.getStartOffset();
            boolean lineLeading = start == 0 || containsNewline(text, prevEnd, start);
            IElementType t = leaf.getElementType();

            Indent indent;
            if (lineLeading) {
                int level = JenkinsfileFormatBlock.isCloser(t) ? depth - 1 : depth;
                if (level < 0) level = 0;
                indent = Indent.getSpaceIndent(level * unit);
            } else {
                indent = Indent.getNoneIndent();
            }
            blocks.add(new JenkinsfileFormatBlock(leaf, indent, null));

            if (JenkinsfileFormatBlock.isOpener(t)) depth++;
            else if (JenkinsfileFormatBlock.isCloser(t) && depth > 0) depth--;
            prevEnd = leaf.getStartOffset() + leaf.getTextLength();
        }
        return blocks;
    }

    private static void collectLeaves(ASTNode node, List<ASTNode> out) {
        for (ASTNode child = node.getFirstChildNode(); child != null; child = child.getTreeNext()) {
            if (child.getFirstChildNode() != null) {
                collectLeaves(child, out);
            } else if (child.getElementType() != TokenType.WHITE_SPACE && child.getTextLength() > 0) {
                out.add(child);
            }
        }
    }

    private static boolean containsNewline(CharSequence text, int from, int to) {
        for (int i = from; i < to && i < text.length(); i++) {
            if (text.charAt(i) == '\n') return true;
        }
        return false;
    }
}
