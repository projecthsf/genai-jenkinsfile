package io.genai.jenkins;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;

/** PSI file root for a Jenkinsfile. */
public final class JenkinsfileFile extends PsiFileBase {

    public JenkinsfileFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, JenkinsfileLanguage.INSTANCE);
    }

    @Override
    public @NotNull FileType getFileType() {
        return JenkinsfileFileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "Jenkinsfile";
    }
}
