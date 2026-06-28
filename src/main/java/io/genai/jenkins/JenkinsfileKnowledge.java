package io.genai.jenkins;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/**
 * The bundled global catalog, parsed once from the stub resource
 * {@code pipeline.jenkinsfile}. This is the universal baseline; a project extends or
 * overrides it with its own {@code .jenkins/*.jenkinsfile} stubs — see
 * {@link JenkinsfileCatalog}. The stub is the single source of truth for completion,
 * documentation and parameter info.
 */
public final class JenkinsfileKnowledge {

    public static final String RESOURCE = "pipeline.jenkinsfile";

    private static final String TEXT = readResource();
    private static final Map<String, StubEntry> BASE =
            Collections.unmodifiableMap(JenkinsfileStubParser.parse(TEXT));

    private JenkinsfileKnowledge() {}

    /** Parsed bundled entries (name → entry). Read-only. */
    public static Map<String, StubEntry> base() { return BASE; }

    /** Raw text of the bundled stub, used to materialise the External Libraries copy. */
    public static String baseText() { return TEXT; }

    private static String readResource() {
        try (InputStream in = JenkinsfileKnowledge.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                System.err.println("Jenkinsfile: " + RESOURCE + " not found on classpath");
                return "";
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("Jenkinsfile: failed to read " + RESOURCE + ": " + e);
            return "";
        }
    }
}
