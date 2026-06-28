package io.genai.jenkins;

import com.intellij.lang.Language;
import com.intellij.psi.PsiElement;
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider;
import io.genai.jenkins.psi.JenkinsfileTypes;
import io.genai.jenkins.psi.JfBlock;
import org.jetbrains.annotations.NotNull;

/**
 * Editor breadcrumbs: shows the block path to the caret, e.g.
 * {@code pipeline ▸ stages ▸ stage 'Build' ▸ steps}. Each {@link JfBlock} is one crumb,
 * labelled by its name plus its first string argument (so stages are distinguishable).
 */
public final class JenkinsfileBreadcrumbsProvider implements BreadcrumbsProvider {

    private static final Language[] LANGUAGES = {JenkinsfileLanguage.INSTANCE};

    @Override
    public Language[] getLanguages() {
        return LANGUAGES;
    }

    @Override
    public boolean acceptElement(@NotNull PsiElement element) {
        return element instanceof JfBlock;
    }

    @Override
    public @NotNull String getElementInfo(@NotNull PsiElement element) {
        JfBlock block = (JfBlock) element;
        String name = JfPsi.name(block);
        if (name == null) name = "block";
        String arg = JfPsi.firstStringText(block.getNode().findChildByType(JenkinsfileTypes.ARGS));
        return (arg != null && !arg.isEmpty()) ? name + " '" + arg + "'" : name;
    }
}
