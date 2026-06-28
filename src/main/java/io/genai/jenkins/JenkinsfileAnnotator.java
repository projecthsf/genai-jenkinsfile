package io.genai.jenkins;

import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.project.DumbAware;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import io.genai.jenkins.psi.JenkinsfileTypes;
import io.genai.jenkins.psi.JfBlock;
import org.jetbrains.annotations.NotNull;

/**
 * Semantic highlighting: colours a name by what it <em>is</em> in the catalog rather
 * than by a fixed word list. A known directive/section/condition gets the directive
 * colour; a known step gets the step (function-call) colour. Real Groovy keywords
 * (if/for/try/def/…) are still coloured by the lexer.
 *
 * <p>Only names used as a statement — a block name or the leading token of a line —
 * are coloured; identifiers inside argument lists or strings are left alone.
 */
public final class JenkinsfileAnnotator implements Annotator, DumbAware {

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        ASTNode node = element.getNode();
        if (node == null || element.getFirstChild() != null) return;          // leaves only

        // Groovy annotation: @Library, @NonCPS, … Colour the '@' and the name, each within its
        // OWN element range — an annotation must stay inside the element being visited, or it
        // corrupts the highlighting pass for the rest of the file.
        if (node.getElementType() == JenkinsfileTypes.OPERATOR && "@".equals(element.getText())) {
            PsiElement next = PsiTreeUtil.nextLeaf(element);
            if (next != null && isName(next)) colour(holder, element, JenkinsfileSyntaxHighlighter.ANNOTATION);
            return;
        }
        if (!isName(element)) return;
        PsiElement prev = PsiTreeUtil.prevLeaf(element);
        if (prev != null && "@".equals(prev.getText())) {
            colour(holder, element, JenkinsfileSyntaxHighlighter.ANNOTATION);
            return;
        }

        PsiElement before = PsiTreeUtil.prevVisibleLeaf(element);
        PsiElement after = PsiTreeUtil.nextVisibleLeaf(element);
        String afterText = after == null ? "" : after.getText();
        String beforeText = before == null ? "" : before.getText();

        // Named-argument key: `name :` opening an argument — `( name :`, `, name :`, `[ name :`,
        // or command-style `step name :` (e.g. environment name: 'X', value: 'Y').
        if (":".equals(afterText)
                && ("(".equals(beforeText) || ",".equals(beforeText) || "[".equals(beforeText) || isName(before))) {
            colour(holder, element, JenkinsfileSyntaxHighlighter.NAMED_ARG);
            return;
        }

        // Environment variable being defined: `NAME =` (single '=') inside an environment { } block.
        if ("=".equals(afterText) && !"=".equals(textOf(PsiTreeUtil.nextVisibleLeaf(after))) && insideEnvironment(element)) {
            colour(holder, element, JenkinsfileSyntaxHighlighter.ENV_VAR);
            return;
        }

        StubEntry e = JenkinsfileCatalog.getInstance(element.getProject()).entry(element.getText());
        if (element.getParent() instanceof JfBlock parent) {
            if (e == null) return;
            // A name that opens a block (unstable { }, stage('x') { }) is structural → directive colour,
            // even if the same name is also a step (e.g. the `unstable` step).
            boolean blockName = JfPsi.nameElement(parent) == element;
            colour(holder, element, keyFor(e, blockName));
            return;
        }
        // A known call nested in arguments/expressions, e.g. string(...) inside withCredentials([...]).
        if (e != null && "(".equals(afterText)) {
            colour(holder, element, keyFor(e, false));
        }
    }

    private static String textOf(PsiElement e) {
        return e == null ? "" : e.getText();
    }

    private static boolean insideEnvironment(PsiElement element) {
        JfBlock block = PsiTreeUtil.getParentOfType(element, JfBlock.class);
        return block != null && "environment".equals(JfPsi.name(block));
    }

    private static boolean isName(PsiElement element) {
        ASTNode n = element.getNode();
        return n != null && element.getFirstChild() == null
                && (n.getElementType() == JenkinsfileTypes.KEYWORD || n.getElementType() == JenkinsfileTypes.IDENTIFIER);
    }

    private static void colour(@NotNull AnnotationHolder holder, @NotNull PsiElement element, @NotNull TextAttributesKey key) {
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element)
                .textAttributes(key)
                .create();
    }

    /**
     * Colour rule:
     * <ul>
     *   <li>a block opener ({@code X { }}) → directive colour (it's structural);</li>
     *   <li>an invocation — a step or a config-property call ({@code image '…'},
     *       {@code timeout(…)}, {@code string(…)}) → the step/"function call" colour;</li>
     *   <li>everything else (sections, directives, when-conditions) → directive colour.</li>
     * </ul>
     * So all function-call-style names share one colour, regardless of which block they sit in.
     */
    static TextAttributesKey keyFor(StubEntry e, boolean blockName) {
        if (blockName) return JenkinsfileSyntaxHighlighter.DIRECTIVE;
        // Structural declarative keywords stay directive-coloured even written bare (e.g. agent any).
        if ("section".equals(e.kind) || "directive".equals(e.kind) || "post".equals(e.kind)) {
            return JenkinsfileSyntaxHighlighter.DIRECTIVE;
        }
        // Everything else used as a call — steps, agent/option config, param/trigger/tool calls,
        // and call-style when-conditions like `branch 'main'` — shares the call colour.
        return JenkinsfileSyntaxHighlighter.STEP;
    }
}
