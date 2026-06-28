package io.genai.jenkins;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a Jenkins GDSL descriptor ({@code <JENKINS_URL>/pipeline-syntax/gdsl}) into
 * pipeline steps, and renders them as a project stub. Used by the
 * "Generate Jenkins Pipeline Stub" action so a user can produce
 * {@code <project>/.jenkins/pipeline.jenkinsfile} from a file saved in the browser —
 * no Python script needed. Ported from {@code tools/gen-catalog.py}.
 */
public final class JenkinsGdslParser {

    private static final Pattern NAME = Pattern.compile("name\\s*:\\s*'([^']*)'");
    private static final Pattern PARAMS = Pattern.compile("params\\s*:\\s*\\[(.*?)]", Pattern.DOTALL);
    private static final Pattern PARAM_KEY = Pattern.compile("([A-Za-z_]\\w*)\\s*:");
    private static final Pattern DOC = Pattern.compile("doc\\s*:\\s*'((?:[^'\\\\]|\\\\.)*)'");

    private JenkinsGdslParser() {}

    public static final class Step {
        public final List<String> params;
        public final String doc;
        Step(List<String> params, String doc) {
            this.params = params;
            this.doc = doc;
        }
    }

    /** name → step, one entry per {@code method(name: '…', params: […], doc: '…')}. */
    public static Map<String, Step> parse(String gdsl) {
        Map<String, Step> steps = new TreeMap<>();
        if (gdsl == null) return steps;
        String[] chunks = gdsl.split("method\\(");
        for (int i = 1; i < chunks.length; i++) {
            String chunk = chunks[i];
            Matcher nm = NAME.matcher(chunk);
            if (!nm.find()) continue;
            String name = nm.group(1);

            List<String> params = new ArrayList<>();
            Matcher pm = PARAMS.matcher(chunk);
            if (pm.find()) {
                Matcher km = PARAM_KEY.matcher(pm.group(1));
                while (km.find()) params.add(km.group(1));
            }

            String doc = "";
            Matcher dm = DOC.matcher(chunk);
            if (dm.find()) {
                doc = dm.group(1).replace("\\'", "'").replace("\\n", " ").trim();
            }
            steps.putIfAbsent(name, new Step(params, doc));
        }
        return steps;
    }

    /** Render parsed steps as a project stub file (steps only; directives come from the global stub). */
    public static String toStub(Map<String, Step> steps) {
        StringBuilder sb = new StringBuilder();
        sb.append("// Jenkins Pipeline — project steps generated from your Jenkins GDSL.\n")
          .append("// Edit freely (add @param descriptions or types); the plugin reads this file.\n\n");
        for (Map.Entry<String, Step> e : steps.entrySet()) {
            Step s = e.getValue();
            sb.append("/**\n");
            if (!s.doc.isEmpty()) sb.append(" * ").append(s.doc).append('\n');
            sb.append(" * @kind step\n");
            for (String p : s.params) sb.append(" * @param ").append(p).append('\n');
            sb.append(" */\n");
            sb.append("def ").append(e.getKey()).append('(')
              .append(String.join(", ", s.params)).append(") {}\n\n");
        }
        return sb.toString();
    }
}
