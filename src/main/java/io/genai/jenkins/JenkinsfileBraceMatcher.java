package io.genai.jenkins;

import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import io.genai.jenkins.psi.JenkinsfileTypes;
import org.jetbrains.annotations.Nullable;

public final class JenkinsfileBraceMatcher implements PairedBraceMatcher {

    private static final BracePair[] PAIRS = {
            new BracePair(JenkinsfileTypes.LBRACE, JenkinsfileTypes.RBRACE, true),
            new BracePair(JenkinsfileTypes.LPAREN, JenkinsfileTypes.RPAREN, false),
            new BracePair(JenkinsfileTypes.LBRACK, JenkinsfileTypes.RBRACK, false),
    };

    @Override public BracePair[] getPairs() { return PAIRS; }

    @Override
    public boolean isPairedBracesAllowedBeforeType(IElementType lbraceType, @Nullable IElementType contextType) {
        return true;
    }

    @Override
    public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
        return openingBraceOffset;
    }
}
