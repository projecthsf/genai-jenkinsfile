package io.genai.jenkins;

import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import io.genai.jenkins.lexer.JenkinsfileFlexLexer;
import io.genai.jenkins.psi.JenkinsfileTypes;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

/** Maps generated-lexer tokens to editor colours. */
public final class JenkinsfileSyntaxHighlighter extends SyntaxHighlighterBase {

    public static final TextAttributesKey KEYWORD =
            createTextAttributesKey("JENKINSFILE_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
    /** A known declarative directive/section/condition (when, steps, allOf, …) — coloured semantically. */
    public static final TextAttributesKey DIRECTIVE =
            createTextAttributesKey("JENKINSFILE_DIRECTIVE", DefaultLanguageHighlighterColors.KEYWORD);
    /** A known pipeline step (echo, sh, archiveArtifacts, …) — coloured semantically. */
    public static final TextAttributesKey STEP =
            createTextAttributesKey("JENKINSFILE_STEP", DefaultLanguageHighlighterColors.FUNCTION_CALL);
    /** A Groovy annotation, e.g. {@code @Library}, {@code @NonCPS}. */
    public static final TextAttributesKey ANNOTATION =
            createTextAttributesKey("JENKINSFILE_ANNOTATION", DefaultLanguageHighlighterColors.METADATA);
    /** A named-argument key, e.g. the {@code name} in {@code string(name: 'X')}. */
    public static final TextAttributesKey NAMED_ARG =
            createTextAttributesKey("JENKINSFILE_NAMED_ARGUMENT", DefaultLanguageHighlighterColors.INSTANCE_FIELD);
    /** An environment variable being defined, e.g. {@code STAGE_VAR} in {@code environment { STAGE_VAR = '…' }}. */
    public static final TextAttributesKey ENV_VAR =
            createTextAttributesKey("JENKINSFILE_ENV_VAR", DefaultLanguageHighlighterColors.CONSTANT);
    public static final TextAttributesKey STRING =
            createTextAttributesKey("JENKINSFILE_STRING", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey NUMBER =
            createTextAttributesKey("JENKINSFILE_NUMBER", DefaultLanguageHighlighterColors.NUMBER);
    public static final TextAttributesKey LINE_COMMENT =
            createTextAttributesKey("JENKINSFILE_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey BLOCK_COMMENT =
            createTextAttributesKey("JENKINSFILE_BLOCK_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT);
    public static final TextAttributesKey BRACES =
            createTextAttributesKey("JENKINSFILE_BRACES", DefaultLanguageHighlighterColors.BRACES);
    public static final TextAttributesKey PARENS =
            createTextAttributesKey("JENKINSFILE_PARENS", DefaultLanguageHighlighterColors.PARENTHESES);
    public static final TextAttributesKey BRACKETS =
            createTextAttributesKey("JENKINSFILE_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS);
    public static final TextAttributesKey OPERATOR =
            createTextAttributesKey("JENKINSFILE_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN);
    public static final TextAttributesKey IDENTIFIER =
            createTextAttributesKey("JENKINSFILE_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER);
    public static final TextAttributesKey BAD_CHAR =
            createTextAttributesKey("JENKINSFILE_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER);

    @Override
    public @NotNull Lexer getHighlightingLexer() {
        return new FlexAdapter(new JenkinsfileFlexLexer(null));
    }

    @Override
    public TextAttributesKey @NotNull [] getTokenHighlights(IElementType t) {
        if (t == JenkinsfileTypes.KEYWORD) return pack(KEYWORD);
        if (t == JenkinsfileTypes.STRING) return pack(STRING);
        if (t == JenkinsfileTypes.NUMBER) return pack(NUMBER);
        if (t == JenkinsfileTypes.LINE_COMMENT) return pack(LINE_COMMENT);
        if (t == JenkinsfileTypes.BLOCK_COMMENT) return pack(BLOCK_COMMENT);
        if (t == JenkinsfileTypes.LBRACE || t == JenkinsfileTypes.RBRACE) return pack(BRACES);
        if (t == JenkinsfileTypes.LPAREN || t == JenkinsfileTypes.RPAREN) return pack(PARENS);
        if (t == JenkinsfileTypes.LBRACK || t == JenkinsfileTypes.RBRACK) return pack(BRACKETS);
        if (t == JenkinsfileTypes.IDENTIFIER) return pack(IDENTIFIER);
        if (t == JenkinsfileTypes.OPERATOR) return pack(OPERATOR);
        if (t == TokenType.BAD_CHARACTER) return pack(BAD_CHAR);
        return TextAttributesKey.EMPTY_ARRAY;
    }
}
