package io.genai.jenkins;

import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.roots.SyntheticLibrary;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Collection;
import java.util.List;

/** The "Jenkins Pipeline" node under External Libraries, holding the generated step stub. */
public final class JenkinsfilePipelineLibrary extends SyntheticLibrary implements ItemPresentation {

    private final VirtualFile root;

    JenkinsfilePipelineLibrary(VirtualFile root) {
        this.root = root;
    }

    @Override
    public Collection<VirtualFile> getSourceRoots() {
        return List.of(root);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof JenkinsfilePipelineLibrary other && root.equals(other.root);
    }

    @Override
    public int hashCode() {
        return root.hashCode();
    }

    @Override
    public @Nullable String getPresentableText() {
        return "GenAI Jenkinsfile";
    }

    @Override
    public @Nullable Icon getIcon(boolean unused) {
        return JenkinsfileFileType.ICON;
    }

    @Override
    public @Nullable String getLocationString() {
        return null;
    }
}
