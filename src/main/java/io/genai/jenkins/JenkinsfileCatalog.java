package io.genai.jenkins;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-project catalog: the bundled global stub ({@link JenkinsfileKnowledge}) merged
 * with the project's own stub files at {@code <project>/.jenkins/*.jenkinsfile}. A
 * team teaches the plugin its steps just by committing a stub file — same rich format
 * as the global one. Project entries override bundled ones.
 *
 * <p>The merged index is cached and rebuilt only when the project stubs change.
 */
@Service(Service.Level.PROJECT)
public final class JenkinsfileCatalog {

    private static final String STUB_DIR = ".jenkins";

    private final Project project;
    private volatile Snapshot snapshot;

    public JenkinsfileCatalog(Project project) {
        this.project = project;
    }

    public static JenkinsfileCatalog getInstance(Project project) {
        return project.getService(JenkinsfileCatalog.class);
    }

    /** Completions (name → kind), bundled base plus any project entries, in stable order. */
    public Map<String, String> completions() {
        Map<String, String> out = new LinkedHashMap<>();
        current().index.forEach((name, e) -> out.put(name, e.kind));
        return out;
    }

    /** Full entry for a name (rich doc / params / types), or null. */
    public @Nullable StubEntry entry(String name) {
        return name == null ? null : current().index.get(name);
    }

    /** Description for a name, or null if unknown / undocumented. */
    public @Nullable String doc(String name) {
        StubEntry e = entry(name);
        return (e == null || e.doc.isEmpty()) ? null : e.doc;
    }

    /** Parameter names for a step, or null. */
    public @Nullable List<String> stepParams(String name) {
        StubEntry e = entry(name);
        return (e == null || e.params.isEmpty()) ? null : e.params;
    }

    private Snapshot current() {
        List<Path> files = projectStubFiles();
        long stamp = stampOf(files);

        Snapshot s = snapshot;
        if (s != null && s.stamp == stamp) return s;

        Map<String, StubEntry> index = new LinkedHashMap<>(JenkinsfileKnowledge.base());
        for (Path f : files) {
            try {
                index.putAll(JenkinsfileStubParser.parse(Files.readString(f, StandardCharsets.UTF_8)));
            } catch (Exception e) {
                System.err.println("Jenkinsfile: failed to read project stub " + f + ": " + e);
            }
        }
        s = new Snapshot(index, stamp);
        snapshot = s;
        return s;
    }

    private List<Path> projectStubFiles() {
        List<Path> out = new ArrayList<>();
        String base = project.getBasePath();
        if (base == null) return out;
        Path dir = Paths.get(base, STUB_DIR);
        if (!Files.isDirectory(dir)) return out;
        try (var stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".jenkinsfile"))
                  .sorted()
                  .forEach(out::add);
        } catch (Exception e) {
            // ignore — no project stubs
        }
        return out;
    }

    private static long stampOf(List<Path> files) {
        long h = 1;
        for (Path f : files) {
            try {
                h = h * 31 + f.toString().hashCode();
                h = h * 31 + Files.getLastModifiedTime(f).toMillis();
                h = h * 31 + Files.size(f);
            } catch (Exception ignored) {
            }
        }
        return h;
    }

    private static final class Snapshot {
        final Map<String, StubEntry> index;
        final long stamp;

        Snapshot(Map<String, StubEntry> index, long stamp) {
            this.index = index;
            this.stamp = stamp;
        }
    }
}
