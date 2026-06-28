package io.genai.jenkins;

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.lang.documentation.DocumentationMarkup;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Quick documentation (F1 / hover) for a known directive, step or annotation, rendered
 * in the platform's standard layout: a signature box, the description, and a sections
 * table (kind, parameters, where it's allowed). Construct data comes from the stub
 * catalog; annotation docs are static.
 */
public final class JenkinsfileDocumentationProvider extends AbstractDocumentationProvider {

    /** Static docs for the common Groovy annotations used in Jenkinsfiles. */
    private static final Map<String, String> ANNOTATION_DOCS = Map.of(
            "Library",
            "Loads a Jenkins <b>shared library</b> using <code>name@version</code> (the version is a Git branch, tag "
            + "or commit). The library is configured in Jenkins under <i>Manage Jenkins ▸ System ▸ Global Pipeline "
            + "Libraries</i> and resolved from its Git repo at build time — it makes the library's global steps "
            + "(<code>vars/</code>) and classes (<code>src/</code>) available to the pipeline. The trailing "
            + "<code>_</code> applies the annotation when you aren't importing a specific class.",
            "NonCPS",
            "Marks a method to run as plain Groovy, <b>outside</b> Jenkins' CPS transformation. Use it for "
            + "non-serializable work (e.g. iterating non-serializable objects); such methods must not call pipeline steps.",
            "Field",
            "Groovy <code>@Field</code> — promotes a script-level variable to a field so it is visible across the "
            + "whole script (including methods).");

    @Override
    public @Nullable String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        PsiElement target = originalElement != null ? originalElement : element;
        if (target == null) return null;
        String word = wordOf(target);
        if (word == null) return null;

        // Annotation (@Library, @NonCPS, …): static explanation.
        if (isAnnotationName(target)) {
            String doc = ANNOTATION_DOCS.getOrDefault(word, "A Groovy annotation.");
            return renderAnnotation(word, doc);
        }

        StubEntry e = JenkinsfileCatalog.getInstance(target.getProject()).entry(word);
        return e != null ? render(e) : null;
    }

    /** Builds the quick-doc HTML for a catalog entry. Package-private for unit testing. */
    static String render(StubEntry e) {
        StringBuilder sb = new StringBuilder();

        sb.append(DocumentationMarkup.DEFINITION_START).append(signature(e)).append(DocumentationMarkup.DEFINITION_END);

        if (!e.doc.isEmpty()) {
            sb.append(DocumentationMarkup.CONTENT_START).append(e.doc).append(DocumentationMarkup.CONTENT_END);
        }

        sb.append(DocumentationMarkup.SECTIONS_START);
        section(sb, "Kind", esc(e.kind));
        if (!e.params.isEmpty()) section(sb, "Parameters", paramsHtml(e));
        if (!e.allowedIn.isEmpty()) section(sb, "Allowed in", allowedHtml(e));
        sb.append(DocumentationMarkup.SECTIONS_END);

        return sb.toString();
    }

    private static String renderAnnotation(String name, String doc) {
        return DocumentationMarkup.DEFINITION_START + '@' + esc(name) + DocumentationMarkup.DEFINITION_END
                + DocumentationMarkup.CONTENT_START + doc + DocumentationMarkup.CONTENT_END
                + DocumentationMarkup.SECTIONS_START
                + DocumentationMarkup.SECTION_HEADER_START + "Kind:" + DocumentationMarkup.SECTION_SEPARATOR
                + "annotation" + DocumentationMarkup.SECTION_END
                + DocumentationMarkup.SECTIONS_END;
    }

    private static String signature(StubEntry e) {
        String n = esc(e.name);
        if (e.isBlock()) return n + " { … }";
        StringBuilder s = new StringBuilder(n).append('(');
        if (e.params.size() > 1) s.append('\n');
        for (int i = 0; i < e.params.size(); i++) {
            String p = e.params.get(i);
            if (e.params.size() > 1) s.append("    ");
            String type = e.paramTypes.get(p);
            if (type != null) s.append(esc(type)).append(' ');
            s.append(esc(p));
            if (i < e.params.size() - 1) s.append(',');
            if (e.params.size() > 1) s.append('\n');
        }
        return s.append(')').toString();
    }

    private static String paramsHtml(StubEntry e) {
        boolean anyDocs = e.paramDocs.values().stream().anyMatch(d -> d != null && !d.isEmpty());
        if (!anyDocs) return String.join(", ", e.params);
        StringBuilder s = new StringBuilder();
        for (String p : e.params) {
            String d = e.paramDocs.get(p);
            s.append("<code>").append(esc(p)).append("</code>");
            if (d != null && !d.isEmpty()) s.append(" — ").append(esc(d));
            s.append("<br/>");
        }
        return s.toString();
    }

    private static String allowedHtml(StubEntry e) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < e.allowedIn.size(); i++) {
            if (i > 0) s.append(", ");
            s.append("<code>").append(esc(e.allowedIn.get(i))).append("</code>");
        }
        return s.toString();
    }

    private static void section(StringBuilder sb, String header, String value) {
        sb.append(DocumentationMarkup.SECTION_HEADER_START).append(header).append(':')
          .append(DocumentationMarkup.SECTION_SEPARATOR).append(value)
          .append(DocumentationMarkup.SECTION_END);
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** True if {@code element} is the name of an annotation (the token right after '@'). */
    private static boolean isAnnotationName(PsiElement element) {
        PsiElement prev = PsiTreeUtil.prevLeaf(element);
        return prev != null && "@".equals(prev.getText());
    }

    /**
     * Marks a known directive/step/annotation token as a documentation target, so the doc
     * popup appears on mouse hover (and Ctrl-hover), not only via F1.
     */
    @Override
    public @Nullable PsiElement getCustomDocumentationElement(@NotNull Editor editor, @NotNull PsiFile file,
                                                              @Nullable PsiElement contextElement, int targetOffset) {
        if (contextElement == null) return null;
        // Hovering the '@' itself → document the annotation name that follows it.
        if ("@".equals(contextElement.getText())) {
            PsiElement next = PsiTreeUtil.nextLeaf(contextElement);
            return next != null && !next.getText().isBlank() ? next : null;
        }
        String word = wordOf(contextElement);
        if (word == null) return null;
        if (isAnnotationName(contextElement)) return contextElement;
        return JenkinsfileCatalog.getInstance(contextElement.getProject()).entry(word) != null ? contextElement : null;
    }

    @Override
    public @Nullable String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
        PsiElement target = originalElement != null ? originalElement : element;
        if (target == null) return null;
        if (isAnnotationName(target)) return "@" + wordOf(target);
        String word = wordOf(target);
        return JenkinsfileCatalog.getInstance(target.getProject()).entry(word) != null ? word : null;
    }

    private static String wordOf(@Nullable PsiElement element) {
        if (element == null) return null;
        String text = element.getText();
        return (text == null || text.isBlank()) ? null : text.trim();
    }
}
