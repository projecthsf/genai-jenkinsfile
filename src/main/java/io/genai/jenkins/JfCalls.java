package io.genai.jenkins;

/**
 * Text helpers for step calls like {@code sh(script: '…')}. Shared by completion
 * and parameter-info: locate the enclosing {@code (}, the step name before it, and
 * the current argument index — without needing a method-call PSI node.
 */
public final class JfCalls {

    private JfCalls() {}

    /** Offset of the {@code (} that encloses {@code caret}, or -1. Stops at braces. */
    public static int enclosingParen(CharSequence t, int caret) {
        int depth = 0;
        for (int i = caret - 1; i >= 0; i--) {
            char c = t.charAt(i);
            if (c == ')') depth++;
            else if (c == '(') { if (depth == 0) return i; depth--; }
            else if (c == '{' || c == '}') return -1;
        }
        return -1;
    }

    /** The identifier immediately before {@code parenOffset} (the called step). */
    public static String nameBefore(CharSequence t, int parenOffset) {
        int j = parenOffset - 1;
        while (j >= 0 && Character.isWhitespace(t.charAt(j))) j--;
        int end = j + 1;
        while (j >= 0 && Character.isJavaIdentifierPart(t.charAt(j))) j--;
        return t.subSequence(j + 1, end).toString();
    }

    /** Zero-based index of the argument the caret is in, counting top-level commas. */
    public static int paramIndex(CharSequence t, int open, int caret) {
        int depth = 0, idx = 0;
        for (int i = open + 1; i < caret && i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '(' || c == '[' || c == '{') depth++;
            else if (c == ')' || c == ']' || c == '}') depth--;
            else if (c == ',' && depth == 0) idx++;
        }
        return idx;
    }
}
