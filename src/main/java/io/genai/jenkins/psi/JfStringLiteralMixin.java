package io.genai.jenkins.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.LiteralTextEscaper;
import com.intellij.psi.PsiLanguageInjectionHost;
import org.jetbrains.annotations.NotNull;

/** Base impl for string-literal PSI: makes it a language-injection host. */
public abstract class JfStringLiteralMixin extends ASTWrapperPsiElement implements PsiLanguageInjectionHost {

    public JfStringLiteralMixin(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public boolean isValidHost() {
        return true;
    }

    @Override
    public PsiLanguageInjectionHost updateText(@NotNull String text) {
        return (PsiLanguageInjectionHost) ElementManipulators.handleContentChange(this, text);
    }

    /**
     * Identity escaper that reports {@code isOneLine() == false}. The built-in
     * {@link LiteralTextEscaper#createSimple} escaper claims one-line, which corrupts
     * offset mapping for triple-quoted (multi-line) strings — producing "Empty element
     * parsed" errors in the injected fragment.
     */
    @Override
    public @NotNull LiteralTextEscaper<JfStringLiteralMixin> createLiteralTextEscaper() {
        return new LiteralTextEscaper<>(this) {
            @Override
            public boolean decode(@NotNull TextRange rangeInsideHost, @NotNull StringBuilder outChars) {
                outChars.append(myHost.getText(), rangeInsideHost.getStartOffset(), rangeInsideHost.getEndOffset());
                return true;
            }

            @Override
            public int getOffsetInHost(int offsetInDecoded, @NotNull TextRange rangeInsideHost) {
                int offset = rangeInsideHost.getStartOffset() + offsetInDecoded;
                return Math.min(offset, rangeInsideHost.getEndOffset());
            }

            @Override
            public boolean isOneLine() {
                return false;
            }
        };
    }
}
