package io.genai.jenkins;

import com.intellij.ide.util.PropertiesComponent;

/** Plugin settings, stored application-wide. */
public final class JenkinsfileSettings {

    private static final String INJECT_LANGUAGES = "io.genai.jenkins.injectLanguages";

    private JenkinsfileSettings() {}

    /** Whether to inject embedded languages (Shell into sh/bat/pwsh, YAML into kubernetes yaml). Default on. */
    public static boolean injectEmbeddedLanguages() {
        return PropertiesComponent.getInstance().getBoolean(INJECT_LANGUAGES, true);
    }

    public static void setInjectEmbeddedLanguages(boolean value) {
        PropertiesComponent.getInstance().setValue(INJECT_LANGUAGES, value, true);
    }
}
