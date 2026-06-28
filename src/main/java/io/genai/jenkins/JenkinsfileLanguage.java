package io.genai.jenkins;

import com.intellij.lang.Language;

/** The Jenkinsfile (Jenkins declarative/scripted pipeline) language. */
public final class JenkinsfileLanguage extends Language {

    public static final JenkinsfileLanguage INSTANCE = new JenkinsfileLanguage();

    private JenkinsfileLanguage() {
        super("Jenkinsfile");
    }
}
