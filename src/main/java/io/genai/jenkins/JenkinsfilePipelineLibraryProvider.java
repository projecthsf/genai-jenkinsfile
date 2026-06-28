package io.genai.jenkins;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider;
import com.intellij.openapi.roots.SyntheticLibrary;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Surfaces the bundled global stub ({@link JenkinsfileKnowledge#RESOURCE}) as a
 * read-only "Jenkins Pipeline" library under <b>External Libraries</b>, and provides
 * it as a Go-to-Declaration target. The stub is materialised once to the IDE system
 * directory (it's identical for every project) and refreshed only if its content
 * changes between plugin versions.
 */
public final class JenkinsfilePipelineLibraryProvider extends AdditionalLibraryRootsProvider {

    private record Cached(int contentHash, VirtualFile dir, VirtualFile file) {}

    private static final AtomicReference<Cached> CACHE = new AtomicReference<>();

    @Override
    public @NotNull Collection<SyntheticLibrary> getAdditionalProjectLibraries(@NotNull Project project) {
        Cached c = ensure();
        return (c != null && c.dir != null && c.dir.isValid())
                ? List.of(new JenkinsfilePipelineLibrary(c.dir))
                : List.of();
    }

    /** The bundled stub PsiFile for {@code project}, or null — a Go-to-Declaration target. */
    public static @Nullable PsiFile stubPsiFile(@NotNull Project project) {
        Cached c = ensure();
        if (c == null || c.file == null || !c.file.isValid()) return null;
        return PsiManager.getInstance(project).findFile(c.file);
    }

    private static @Nullable Cached ensure() {
        String content = JenkinsfileKnowledge.baseText();
        int hash = content.hashCode();

        Cached current = CACHE.get();
        if (current != null && current.contentHash == hash && current.file != null && current.file.isValid()) {
            return current;
        }

        try {
            Path dir = Paths.get(PathManager.getSystemPath(), "jenkinsfile-stubs", "global");
            Path file = dir.resolve(JenkinsfileKnowledge.RESOURCE);
            Files.createDirectories(dir);
            Files.writeString(file, content, StandardCharsets.UTF_8);

            VirtualFile vfile = VfsUtil.findFileByIoFile(file.toFile(), true);
            VirtualFile vdir = vfile != null ? vfile.getParent() : null;
            Cached fresh = new Cached(hash, vdir, vfile);
            CACHE.set(fresh);
            return fresh;
        } catch (Exception e) {
            System.err.println("Jenkinsfile: failed to materialise pipeline stub: " + e);
            return null;
        }
    }
}
