package io.genai.jenkins.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

/**
 * Tells the platform the editable "value" range of a string literal (inside the
 * quotes) and how to write changes back — needed for language injection.
 */
public final class JfStringLiteralManipulator extends AbstractElementManipulator<JfStringLiteral> {

    @Override
    public @NotNull TextRange getRangeInElement(@NotNull JfStringLiteral element) {
        String t = element.getText();
        if (t.length() >= 6 && (t.startsWith("'''") || t.startsWith("\"\"\""))) {
            int end = (t.endsWith("'''") || t.endsWith("\"\"\"")) ? t.length() - 3 : t.length();
            return new TextRange(3, Math.max(3, end));
        }
        if (t.length() >= 2 && (t.startsWith("'") || t.startsWith("\""))) {
            int end = (t.endsWith("'") || t.endsWith("\"")) ? t.length() - 1 : t.length();
            return new TextRange(1, Math.max(1, end));
        }
        return new TextRange(0, t.length());
    }

    @Override
    public JfStringLiteral handleContentChange(@NotNull JfStringLiteral element, @NotNull TextRange range, String newContent)
            throws IncorrectOperationException {
        String oldText = element.getText();
        String newText = oldText.substring(0, range.getStartOffset()) + newContent + oldText.substring(range.getEndOffset());
        ASTNode leaf = element.getNode().getFirstChildNode();
        if (leaf instanceof LeafElement) {
            ((LeafElement) leaf).replaceWithText(newText);
        }
        return element;
    }
}
