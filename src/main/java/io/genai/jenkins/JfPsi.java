package io.genai.jenkins;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import io.genai.jenkins.psi.JenkinsfileTypes;
import io.genai.jenkins.psi.JfBlock;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Helpers for navigating the generated {@link JfBlock} PSI tree. */
public final class JfPsi {

    private static final TokenSet NAME = TokenSet.create(JenkinsfileTypes.KEYWORD, JenkinsfileTypes.IDENTIFIER);

    private JfPsi() {}

    /** First string-literal value (quotes stripped) directly under {@code scope}, or null. */
    public static @Nullable String firstStringText(@Nullable ASTNode scope) {
        if (scope == null) return null;
        ASTNode sl = scope.findChildByType(JenkinsfileTypes.STRING_LITERAL);
        if (sl == null) return null;
        return sl.getText().replaceAll("^['\"]+|['\"]+$", "");
    }

    /** The block's leading keyword/identifier (e.g. {@code stage}), or null. */
    public static @Nullable String name(JfBlock block) {
        ASTNode n = block.getNode().findChildByType(NAME);
        return n != null ? n.getText() : null;
    }

    /** The PSI element to anchor a problem/intention on (the name token, or the block). */
    public static PsiElement nameElement(JfBlock block) {
        ASTNode n = block.getNode().findChildByType(NAME);
        return n != null ? n.getPsi() : block;
    }

    public static List<JfBlock> childBlocks(PsiElement element) {
        return PsiTreeUtil.getChildrenOfTypeAsList(element, JfBlock.class);
    }

    public static boolean hasChild(JfBlock block, String... names) {
        for (JfBlock c : childBlocks(block)) {
            String n = name(c);
            for (String x : names) if (x.equals(n)) return true;
        }
        return false;
    }

    public static @Nullable JfBlock child(JfBlock block, String name) {
        for (JfBlock c : childBlocks(block)) if (name.equals(name(c))) return c;
        return null;
    }

    public static int openBraceOffset(JfBlock block) {
        ASTNode n = block.getNode().findChildByType(JenkinsfileTypes.LBRACE);
        return n != null ? n.getStartOffset() : -1;
    }

    public static int closeBraceEndOffset(JfBlock block) {
        ASTNode n = block.getNode().findChildByType(JenkinsfileTypes.RBRACE);
        return n != null ? n.getTextRange().getEndOffset() : -1;
    }

    /** Innermost block containing {@code at} (self included), or null. */
    public static @Nullable JfBlock innermost(@Nullable PsiElement at) {
        return PsiTreeUtil.getParentOfType(at, JfBlock.class, false);
    }

    /** Nearest enclosing block (self included) whose name is one of {@code names}. */
    public static @Nullable JfBlock enclosing(@Nullable PsiElement at, String... names) {
        JfBlock b = PsiTreeUtil.getParentOfType(at, JfBlock.class, false);
        while (b != null) {
            String n = name(b);
            for (String x : names) if (x.equals(n)) return b;
            b = PsiTreeUtil.getParentOfType(b, JfBlock.class);
        }
        return null;
    }
}
