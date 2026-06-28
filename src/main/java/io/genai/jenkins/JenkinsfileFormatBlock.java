package io.genai.jenkins;

import com.intellij.formatting.Block;
import com.intellij.formatting.ChildAttributes;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Spacing;
import com.intellij.lang.ASTNode;
import com.intellij.psi.formatter.common.AbstractBlock;
import com.intellij.psi.tree.IElementType;
import io.genai.jenkins.psi.JenkinsfileTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One formatting block. The whole layout is computed up front by
 * {@link JenkinsfileFormattingModelBuilder} from brace/paren depth, so each block
 * just carries its pre-computed {@link Indent}; leaves carry {@code null} children.
 * New-line (Enter) indentation is handled separately by {@link JenkinsfileEnterHandler}.
 */
public final class JenkinsfileFormatBlock extends AbstractBlock {

    private final Indent indent;
    private final List<Block> children;   // null = leaf

    JenkinsfileFormatBlock(@NotNull ASTNode node, @Nullable Indent indent, @Nullable List<Block> children) {
        super(node, null, null);
        this.indent = indent;
        this.children = children;
    }

    @Override
    public Indent getIndent() {
        return indent;
    }

    @Override
    protected List<Block> buildChildren() {
        return children != null ? children : List.of();
    }

    @Override
    public @Nullable Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
        return null;   // keep existing inline spacing and blank lines; we only set indentation
    }

    @Override
    public boolean isLeaf() {
        return children == null;
    }

    @Override
    public @NotNull ChildAttributes getChildAttributes(int newChildIndex) {
        return new ChildAttributes(Indent.getNoneIndent(), null);
    }

    static boolean isOpener(IElementType t) {
        return t == JenkinsfileTypes.LBRACE || t == JenkinsfileTypes.LPAREN || t == JenkinsfileTypes.LBRACK;
    }

    static boolean isCloser(IElementType t) {
        return t == JenkinsfileTypes.RBRACE || t == JenkinsfileTypes.RPAREN || t == JenkinsfileTypes.RBRACK;
    }
}
