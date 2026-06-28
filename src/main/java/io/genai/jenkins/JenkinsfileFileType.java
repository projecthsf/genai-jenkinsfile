package io.genai.jenkins;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/** Recognises {@code Jenkinsfile} (and {@code *.jenkinsfile}) as this language. */
public final class JenkinsfileFileType extends LanguageFileType {

    public static final JenkinsfileFileType INSTANCE = new JenkinsfileFileType();
    public static final Icon ICON = IconLoader.getIcon("/icons/jenkinsfile.svg", JenkinsfileFileType.class);

    private JenkinsfileFileType() {
        super(JenkinsfileLanguage.INSTANCE);
    }

    @Override public @NotNull String getName() { return "Jenkinsfile"; }
    @Override public @NotNull String getDescription() { return JenkinsfileBundle.message("filetype.description"); }
    @Override public @NotNull String getDefaultExtension() { return "jenkinsfile"; }
    @Override public Icon getIcon() { return ICON; }
}
