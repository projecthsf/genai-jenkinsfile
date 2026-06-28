package io.genai.jenkins;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Map;

/** Settings ▸ Editor ▸ Color Scheme ▸ Jenkinsfile — lets users tune the colours. */
public final class JenkinsfileColorSettingsPage implements ColorSettingsPage {

    private static final AttributesDescriptor[] DESCRIPTORS = {
            new AttributesDescriptor(JenkinsfileBundle.message("color.keyword"), JenkinsfileSyntaxHighlighter.KEYWORD),
            new AttributesDescriptor(JenkinsfileBundle.message("color.directive"), JenkinsfileSyntaxHighlighter.DIRECTIVE),
            new AttributesDescriptor(JenkinsfileBundle.message("color.step"), JenkinsfileSyntaxHighlighter.STEP),
            new AttributesDescriptor(JenkinsfileBundle.message("color.annotation"), JenkinsfileSyntaxHighlighter.ANNOTATION),
            new AttributesDescriptor(JenkinsfileBundle.message("color.namedArg"), JenkinsfileSyntaxHighlighter.NAMED_ARG),
            new AttributesDescriptor(JenkinsfileBundle.message("color.envVar"), JenkinsfileSyntaxHighlighter.ENV_VAR),
            new AttributesDescriptor(JenkinsfileBundle.message("color.string"), JenkinsfileSyntaxHighlighter.STRING),
            new AttributesDescriptor(JenkinsfileBundle.message("color.number"), JenkinsfileSyntaxHighlighter.NUMBER),
            new AttributesDescriptor(JenkinsfileBundle.message("color.lineComment"), JenkinsfileSyntaxHighlighter.LINE_COMMENT),
            new AttributesDescriptor(JenkinsfileBundle.message("color.blockComment"), JenkinsfileSyntaxHighlighter.BLOCK_COMMENT),
            new AttributesDescriptor(JenkinsfileBundle.message("color.braces"), JenkinsfileSyntaxHighlighter.BRACES),
            new AttributesDescriptor(JenkinsfileBundle.message("color.parens"), JenkinsfileSyntaxHighlighter.PARENS),
            new AttributesDescriptor(JenkinsfileBundle.message("color.brackets"), JenkinsfileSyntaxHighlighter.BRACKETS),
            new AttributesDescriptor(JenkinsfileBundle.message("color.operator"), JenkinsfileSyntaxHighlighter.OPERATOR),
            new AttributesDescriptor(JenkinsfileBundle.message("color.identifier"), JenkinsfileSyntaxHighlighter.IDENTIFIER),
    };

    @Override public @Nullable Icon getIcon() { return JenkinsfileFileType.ICON; }
    @Override public @NotNull SyntaxHighlighter getHighlighter() { return new JenkinsfileSyntaxHighlighter(); }
    @Override public @NotNull AttributesDescriptor[] getAttributeDescriptors() { return DESCRIPTORS; }
    @Override public @NotNull ColorDescriptor[] getColorDescriptors() { return ColorDescriptor.EMPTY_ARRAY; }
    @Override public @NotNull String getDisplayName() { return JenkinsfileBundle.message("color.pageName"); }

    @Override
    public @Nullable Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return Map.of(
                "dir", JenkinsfileSyntaxHighlighter.DIRECTIVE,
                "step", JenkinsfileSyntaxHighlighter.STEP,
                "anno", JenkinsfileSyntaxHighlighter.ANNOTATION,
                "narg", JenkinsfileSyntaxHighlighter.NAMED_ARG,
                "env", JenkinsfileSyntaxHighlighter.ENV_VAR);
    }

    @Override
    public @NotNull String getDemoText() {
        return "<anno>@Library</anno>('shared') _\n" +
                "// Declarative pipeline\n" +
                "<dir>pipeline</dir> {\n" +
                "    <dir>agent</dir> any\n" +
                "    <dir>environment</dir> {\n" +
                "        <env>VERSION</env> = '1.0.0'\n" +
                "    }\n" +
                "    <dir>parameters</dir> {\n" +
                "        <step>string</step>(<narg>name</narg>: 'ENV', <narg>defaultValue</narg>: 'dev')\n" +
                "    }\n" +
                "    <dir>stages</dir> {\n" +
                "        <dir>stage</dir>('Build') {\n" +
                "            <dir>steps</dir> {\n" +
                "                <step>echo</step> \"Building ${VERSION}\"\n" +
                "                <step>sh</step> 'make build'\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "    <dir>post</dir> {\n" +
                "        <dir>always</dir> { <step>echo</step> 'Done' }\n" +
                "    }\n" +
                "}\n";
    }
}
