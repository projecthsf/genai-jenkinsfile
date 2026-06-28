package io.genai.jenkins;

import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import io.genai.jenkins.psi.JenkinsfileTypes;
import org.jetbrains.annotations.NotNull;

/**
 * Auto-indents the line produced by Enter to the current brace/paren/bracket depth —
 * the same rule the formatter uses — so a new line lands at the right nesting level
 * (one deeper after {@code {}, back out before {@code }}). Lines inside a multi-line
 * string or block comment are left to the platform.
 */
public final class JenkinsfileEnterHandler extends EnterHandlerDelegateAdapter {

    @Override
    public Result postProcessEnter(@NotNull PsiFile file, @NotNull Editor editor, @NotNull DataContext dataContext) {
        if (!(file instanceof JenkinsfileFile)) return Result.Continue;

        Document doc = editor.getDocument();
        PsiDocumentManager.getInstance(file.getProject()).commitDocument(doc);

        int caret = editor.getCaretModel().getOffset();
        int line = doc.getLineNumber(caret);
        int lineStart = doc.getLineStartOffset(line);
        int lineEnd = doc.getLineEndOffset(line);
        CharSequence text = doc.getCharsSequence();

        // Inside a multi-line string/comment? Leave it alone.
        if (lineStart > 0) {
            PsiElement prev = file.findElementAt(lineStart - 1);
            if (prev != null) {
                IElementType t = prev.getNode().getElementType();
                if (t == JenkinsfileTypes.STRING || t == JenkinsfileTypes.BLOCK_COMMENT) return Result.Continue;
            }
        }

        int depth = braceDepthBefore(file.getNode(), lineStart);

        int firstNonWs = lineStart;
        while (firstNonWs < lineEnd && (text.charAt(firstNonWs) == ' ' || text.charAt(firstNonWs) == '\t')) firstNonWs++;
        if (firstNonWs < lineEnd) {
            char c = text.charAt(firstNonWs);
            if (c == '}' || c == ')' || c == ']') depth = Math.max(0, depth - 1);
        }

        int unit = CodeStyle.getIndentOptions(file).INDENT_SIZE;
        if (unit <= 0) unit = 4;
        String want = " ".repeat(depth * unit);
        String have = text.subSequence(lineStart, firstNonWs).toString();
        if (want.equals(have)) return Result.Continue;

        doc.replaceString(lineStart, firstNonWs, want);
        PsiDocumentManager.getInstance(file.getProject()).commitDocument(doc);
        editor.getCaretModel().moveToOffset(lineStart + want.length());
        return Result.Stop;
    }

    /** Net unmatched openers in the document before {@code offset}, counted from real tokens. */
    private static int braceDepthBefore(ASTNode root, int offset) {
        int[] depth = {0};
        walk(root, offset, depth);
        return Math.max(0, depth[0]);
    }

    private static void walk(ASTNode node, int offset, int[] depth) {
        for (ASTNode c = node.getFirstChildNode(); c != null; c = c.getTreeNext()) {
            if (c.getStartOffset() >= offset) return;          // tokens are ordered; past the line now
            if (c.getFirstChildNode() != null) {
                walk(c, offset, depth);
            } else {
                IElementType t = c.getElementType();
                if (JenkinsfileFormatBlock.isOpener(t)) depth[0]++;
                else if (JenkinsfileFormatBlock.isCloser(t) && depth[0] > 0) depth[0]--;
            }
        }
    }
}
