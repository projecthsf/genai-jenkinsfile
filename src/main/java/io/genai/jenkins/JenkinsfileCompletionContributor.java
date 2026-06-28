package io.genai.jenkins;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Document;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Context-aware completion:
 * <ul>
 *   <li>inside a known step's {@code (...)} → that step's parameter names;</li>
 *   <li>otherwise → declarative directives/sections and steps (with block auto-insert).</li>
 * </ul>
 */
public final class JenkinsfileCompletionContributor extends CompletionContributor {

    public JenkinsfileCompletionContributor() {
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement().withLanguage(JenkinsfileLanguage.INSTANCE),
                new CompletionProvider<>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters,
                                                  @NotNull ProcessingContext context,
                                                  @NotNull CompletionResultSet result) {
                        CharSequence text = parameters.getEditor().getDocument().getCharsSequence();
                        int offset = parameters.getOffset();
                        JenkinsfileCatalog catalog = JenkinsfileCatalog.getInstance(parameters.getPosition().getProject());

                        // Inside (...) of a call: offer the step's parameter names (if known),
                        // never the global step list.
                        int open = JfCalls.enclosingParen(text, offset);
                        if (open >= 0) {
                            List<String> params = catalog.stepParams(JfCalls.nameBefore(text, open));
                            if (params != null) {
                                for (String p : params) {
                                    result.addElement(paramLookup(p));
                                }
                            }
                            return;
                        }

                        // Statement position: offer only what's valid inside the enclosing block.
                        String block = enclosingBlockName(parameters.getPosition());
                        for (Map.Entry<String, String> e : catalog.completions().entrySet()) {
                            if (relevant(catalog, catalog.entry(e.getKey()), block)) {
                                result.addElement(lookup(e.getKey(), e.getValue()));
                            }
                        }
                    }
                });
    }

    /** Blocks whose direct content is steps (so step completions belong there). */
    private static final java.util.Set<String> STEP_CONTAINERS = java.util.Set.of(
            "steps", "script", "always", "success", "failure", "unstable", "changed",
            "fixed", "regression", "aborted", "unsuccessful", "cleanup");
    /** Blocks that hold agent types/params. */
    private static final java.util.Set<String> AGENT_CONTAINERS = java.util.Set.of(
            "agent", "docker", "dockerfile", "kubernetes", "node");
    /** Structural kinds — declarative skeleton, not calls. */
    private static final java.util.Set<String> STRUCTURAL = java.util.Set.of("section", "directive", "post", "when");

    /** Name of the JfBlock enclosing the caret, or null at the top level / unknown. */
    private static String enclosingBlockName(com.intellij.psi.PsiElement position) {
        io.genai.jenkins.psi.JfBlock b =
                com.intellij.psi.util.PsiTreeUtil.getParentOfType(position, io.genai.jenkins.psi.JfBlock.class);
        return b != null ? JfPsi.name(b) : null;
    }

    /** Whether construct {@code e} is valid directly inside the block named {@code block}. */
    static boolean relevant(JenkinsfileCatalog catalog, StubEntry e, String block) {
        if (e == null) return true;
        if (block == null) return true;                       // top level / scripted — don't over-filter
        StubEntry container = catalog.entry(block);
        if (container == null) return true;                   // unknown block (e.g. shared-library) — be lenient
        if (!e.allowedIn.isEmpty()) return e.allowedIn.contains(block);
        if (STRUCTURAL.contains(e.kind)) return true;         // structural without placement rule (pipeline, script)
        if ("agent".equals(e.kind)) return AGENT_CONTAINERS.contains(block);
        // a call (step / "steps" / unknown kind) — only inside a step-bearing block
        return STEP_CONTAINERS.contains(block) || isCallKind(container.kind);
    }

    private static boolean isCallKind(String kind) {
        return !STRUCTURAL.contains(kind) && !"agent".equals(kind);
    }

    /** A named-argument completion: inserts {@code name: } (caret after), except for the
     *  {@code body} closure which isn't a named arg. */
    private static LookupElement paramLookup(String param) {
        LookupElementBuilder el = LookupElementBuilder.create(param).withTypeText("parameter", true);
        if (param.equals("body")) return el;
        return el.withTailText(": ", true).withInsertHandler((ctx, item) -> {
            int at = ctx.getTailOffset();
            // don't duplicate a ':' the user (or a re-completion) already has
            CharSequence doc = ctx.getDocument().getCharsSequence();
            if (at < doc.length() && doc.charAt(at) == ':') return;
            ctx.getDocument().insertString(at, ": ");
            ctx.getEditor().getCaretModel().moveToOffset(at + 2);
        });
    }

    private static LookupElement lookup(String name, String category) {
        boolean blockKind = "section".equals(category) || "directive".equals(category) || "post".equals(category);
        boolean autoBrace = blockKind && !name.equals("agent");

        LookupElementBuilder el = LookupElementBuilder.create(name)
                .withTypeText(category, true)
                .withBoldness("step".equals(category));
        if (autoBrace) {
            el = el.withTailText(name.equals("stage") ? "('') { … }" : "  { … }", true)
                   .withInsertHandler(blockInsertHandler(name.equals("stage")));
        }
        return el;
    }

    private static InsertHandler<LookupElement> blockInsertHandler(boolean stage) {
        return (InsertionContext ctx, LookupElement item) -> {
            Document doc = ctx.getDocument();
            int tail = ctx.getTailOffset();

            int line = doc.getLineNumber(tail);
            int lineStart = doc.getLineStartOffset(line);
            String prefix = doc.getText(new com.intellij.openapi.util.TextRange(lineStart, tail));
            String indent = leadingWhitespace(prefix);
            String inner = indent + "    ";

            String text = (stage ? "('')" : "") + " {\n" + inner + "\n" + indent + "}";
            doc.insertString(tail, text);
            ctx.commitDocument();

            int caret = stage
                    ? tail + 2
                    : tail + (" {\n" + inner).length();
            ctx.getEditor().getCaretModel().moveToOffset(caret);
        };
    }

    private static String leadingWhitespace(String s) {
        int i = 0;
        while (i < s.length() && (s.charAt(i) == ' ' || s.charAt(i) == '\t')) i++;
        return s.substring(0, i);
    }
}
