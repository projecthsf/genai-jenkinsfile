package io.genai.jenkins;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import io.genai.jenkins.psi.JenkinsfileTypes;
import org.jetbrains.annotations.Nullable;

/**
 * Go to Declaration for Jenkinsfile symbols:
 * <ul>
 *   <li>{@code def NAME} / {@code def NAME(...)} in script blocks;</li>
 *   <li>{@code environment { KEY = … }} — {@code env.KEY}, {@code "${KEY}"} or a bare {@code KEY};</li>
 *   <li>any catalog construct — a step ({@code sh}) <em>or</em> a directive keyword
 *       ({@code pipeline}, {@code agent}, {@code stages}, …) — resolves into the
 *       generated stub library ({@link JenkinsfilePipelineLibraryProvider}).</li>
 * </ul>
 */
public final class JenkinsfileGotoDeclarationHandler implements GotoDeclarationHandler {

    @Override
    public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement source, int offset, Editor editor) {
        if (source == null) return null;
        PsiFile file = source.getContainingFile();
        if (!(file instanceof JenkinsfileFile)) return null;

        // Interpolated variable inside a string: "${VAR}", "${env.VAR}", "$VAR".
        if (isString(source)) {
            PsiElement t = resolveInString(file, source.getText(), offset - source.getTextRange().getStartOffset());
            return (t != null && t != source) ? new PsiElement[]{t} : null;
        }

        // A name token is an identifier (step / variable) or a directive keyword (pipeline, agent, …).
        if (!isNameToken(source)) return null;
        String name = source.getText();
        if (name == null || name.isEmpty()) return null;

        PsiElement prev = PsiTreeUtil.prevVisibleLeaf(source);
        boolean dotted = prev != null && ".".equals(prev.getText());

        PsiElement target;
        if (dotted) {
            PsiElement qualifier = PsiTreeUtil.prevVisibleLeaf(prev);
            String q = qualifier != null ? qualifier.getText() : "";
            target = ("env".equals(q) || "environment".equals(q)) ? findEnvKey(file, name) : null;
        } else {
            target = findDef(file, name);
            if (target == null) target = findEnvKey(file, name);
            if (target == null) target = findStub(file.getProject(), name);
        }

        return (target != null && target != source) ? new PsiElement[]{target} : null;
    }

    /** The {@code NAME} of a {@code def NAME …} declaration, or null. */
    private static @Nullable PsiElement findDef(PsiFile file, String name) {
        for (PsiElement leaf = firstLeaf(file); leaf != null; leaf = PsiTreeUtil.nextLeaf(leaf)) {
            if (!isIdentifier(leaf) || !name.equals(leaf.getText())) continue;
            PsiElement before = PsiTreeUtil.prevVisibleLeaf(leaf);
            if (before != null && "def".equals(before.getText())) return leaf;
        }
        return null;
    }

    /** The {@code def NAME} of a construct — in a project {@code .jenkins} stub if defined there,
     *  otherwise in the bundled global stub library. */
    private static @Nullable PsiElement findStub(com.intellij.openapi.project.Project project, String name) {
        if (!JenkinsfileCatalog.getInstance(project).completions().containsKey(name)) return null;
        for (PsiFile stub : projectStubFiles(project)) {
            PsiElement decl = findDeclByText(stub, name);
            if (decl != null) return decl;
        }
        PsiFile global = JenkinsfilePipelineLibraryProvider.stubPsiFile(project);
        return global != null ? findDeclByText(global, name) : null;
    }

    /** PsiFiles for the project's own {@code <project>/.jenkins/*.jenkinsfile} stubs. */
    private static java.util.List<PsiFile> projectStubFiles(com.intellij.openapi.project.Project project) {
        java.util.List<PsiFile> out = new java.util.ArrayList<>();
        String base = project.getBasePath();
        if (base == null) return out;
        java.nio.file.Path dir = java.nio.file.Paths.get(base, ".jenkins");
        com.intellij.openapi.vfs.VirtualFile vdir =
                com.intellij.openapi.vfs.LocalFileSystem.getInstance().findFileByNioFile(dir);
        if (vdir == null || !vdir.isDirectory()) return out;
        for (com.intellij.openapi.vfs.VirtualFile child : vdir.getChildren()) {
            if (!child.isDirectory() && child.getName().endsWith(".jenkinsfile")) {
                PsiFile pf = com.intellij.psi.PsiManager.getInstance(project).findFile(child);
                if (pf != null) out.add(pf);
            }
        }
        return out;
    }

    /** Like {@link #findDef} but matches by text regardless of token kind — directive names
     *  ({@code pipeline}, {@code stages}) are keyword tokens even after {@code def} in the stub. */
    private static @Nullable PsiElement findDeclByText(PsiFile file, String name) {
        for (PsiElement leaf = firstLeaf(file); leaf != null; leaf = PsiTreeUtil.nextLeaf(leaf)) {
            if (!name.equals(leaf.getText())) continue;
            PsiElement before = PsiTreeUtil.prevVisibleLeaf(leaf);
            if (before != null && "def".equals(before.getText())) return leaf;
        }
        return null;
    }

    /** The {@code KEY} of a {@code KEY = …} assignment inside an {@code environment} block, or null. */
    private static @Nullable PsiElement findEnvKey(PsiFile file, String name) {
        for (PsiElement leaf = firstLeaf(file); leaf != null; leaf = PsiTreeUtil.nextLeaf(leaf)) {
            if (!isIdentifier(leaf) || !name.equals(leaf.getText())) continue;
            PsiElement after = PsiTreeUtil.nextVisibleLeaf(leaf);
            if (after == null || !"=".equals(after.getText())) continue;
            if (JfPsi.enclosing(leaf, "environment") != null) return leaf;
        }
        return null;
    }

    /**
     * Resolve a variable interpolated in a string at {@code local} offset within
     * {@code text}: {@code ${VAR}}, {@code ${env.VAR}} or {@code $VAR}. Returns null
     * for plain (non-interpolated) string text.
     */
    private static @Nullable PsiElement resolveInString(PsiFile file, String text, int local) {
        if (local < 0 || local > text.length()) return null;

        int s = local, e = local;
        while (s > 0 && isIdentChar(text.charAt(s - 1))) s--;
        while (e < text.length() && isIdentChar(text.charAt(e))) e++;
        if (s == e) return null;
        String word = text.substring(s, e);

        // Optional qualifier directly before the word: "env." / "params."
        String qualifier = null;
        int i = s;
        if (i > 0 && text.charAt(i - 1) == '.') {
            int q = i - 2;
            while (q >= 0 && isIdentChar(text.charAt(q))) q--;
            qualifier = text.substring(q + 1, i - 1);
            i = q + 1;
        }

        // Must be an interpolation: a '$' (optionally '${') immediately before.
        int j = i;
        if (j > 0 && text.charAt(j - 1) == '{') j--;
        if (j == 0 || text.charAt(j - 1) != '$') return null;

        if ("env".equals(qualifier) || "environment".equals(qualifier)) return findEnvKey(file, word);
        if ("params".equals(qualifier) || "parameters".equals(qualifier)) return null;   // declared in strings
        if (qualifier != null) return null;                                              // unknown qualifier

        PsiElement env = findEnvKey(file, word);
        return env != null ? env : findDef(file, word);
    }

    private static PsiElement firstLeaf(PsiFile file) {
        return PsiTreeUtil.getDeepestFirst(file);
    }

    private static boolean isIdentifier(PsiElement e) {
        ASTNode node = e.getNode();
        return node != null && node.getElementType() == JenkinsfileTypes.IDENTIFIER;
    }

    /** Identifier (step/variable) or directive keyword (pipeline, agent, stages, …). */
    private static boolean isNameToken(PsiElement e) {
        ASTNode node = e.getNode();
        if (node == null) return false;
        return node.getElementType() == JenkinsfileTypes.IDENTIFIER
                || node.getElementType() == JenkinsfileTypes.KEYWORD;
    }

    private static boolean isString(PsiElement e) {
        ASTNode node = e.getNode();
        return node != null && node.getElementType() == JenkinsfileTypes.STRING;
    }

    private static boolean isIdentChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
