package io.genai.jenkins;

import com.intellij.lang.Language;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import io.genai.jenkins.psi.JfStringLiteral;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Injects an embedded language into a string argument based on the step it belongs to:
 * <ul>
 *   <li>{@code sh} / {@code bat} / {@code pwsh} / {@code powershell} → Shell Script;</li>
 *   <li>{@code yaml} (the {@code kubernetes} agent's pod template) → YAML.</li>
 * </ul>
 * So the embedded script/manifest gets real highlighting (and completion/validation from
 * the host IDE). No-ops when the target language isn't bundled, or when disabled in settings.
 */
public final class JenkinsfileLanguageInjector implements MultiHostInjector {

    /** Leading step name → language ID to inject. */
    private static final Map<String, String> LANGUAGE_BY_STEP = Map.of(
            "sh", "Shell Script",
            "bat", "Shell Script",
            "pwsh", "Shell Script",
            "powershell", "Shell Script",
            "yaml", "yaml");

    @Override
    public @NotNull List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
        return List.of(JfStringLiteral.class);
    }

    @Override
    public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement context) {
        if (!JenkinsfileSettings.injectEmbeddedLanguages()) return;
        if (!(context instanceof JfStringLiteral host) || !host.isValidHost()) return;

        String langId = LANGUAGE_BY_STEP.get(leadingStep(host));
        if (langId == null) return;
        Language language = Language.findLanguageByID(langId);
        if (language == null) return;

        TextRange inner = ElementManipulators.getManipulator(host).getRangeInElement(host);
        if (inner.isEmpty()) return;
        registrar.startInjecting(language).addPlace(null, null, host, inner).doneInjecting();
    }

    /** The first identifier on the line the string starts on (the step being called). */
    private static String leadingStep(JfStringLiteral host) {
        CharSequence text = host.getContainingFile().getViewProvider().getContents();
        int start = host.getTextRange().getStartOffset();
        int i = start;
        while (i > 0 && text.charAt(i - 1) != '\n') i--;                 // line start
        while (i < start && Character.isWhitespace(text.charAt(i))) i++;  // skip indent
        int j = i;
        while (j < text.length() && (Character.isJavaIdentifierPart(text.charAt(j)) || text.charAt(j) == '$')) j++;
        return text.subSequence(i, j).toString();
    }
}
