package io.genai.jenkins;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a stub file (our own Jenkinsfile syntax) into {@link StubEntry} records.
 * A stub entry is a {@code def name(params)} preceded by an optional doc comment:
 *
 * <pre>
 * /**
 *  * Run a shell script.
 *  * &#64;kind step
 *  * &#64;param script the command to run
 *  *&#47;
 * def sh(String script) {}
 * </pre>
 *
 * The doc text before the first {@code @tag} is the description (HTML allowed);
 * {@code @kind} sets the construct kind; {@code @param name desc} documents a param.
 */
public final class JenkinsfileStubParser {

    private static final Pattern DEF = Pattern.compile("\\bdef\\s+(\\w+)\\s*\\(([^)]*)\\)");

    private JenkinsfileStubParser() {}

    /** Parse all entries, keyed by name in file order (later entries overwrite earlier). */
    public static Map<String, StubEntry> parse(CharSequence text) {
        Map<String, StubEntry> out = new LinkedHashMap<>();
        if (text == null) return out;
        String s = text.toString();

        Matcher m = DEF.matcher(s);
        while (m.find()) {
            String name = m.group(1);
            String paramsRaw = m.group(2);

            List<String> params = new ArrayList<>();
            Map<String, String> types = new LinkedHashMap<>();
            for (String seg : paramsRaw.split(",")) {
                String p = seg.trim();
                if (p.isEmpty()) continue;
                int eq = p.indexOf('=');
                if (eq >= 0) p = p.substring(0, eq).trim();      // drop default value
                String[] words = p.split("\\s+");
                String pname = words[words.length - 1];
                params.add(pname);
                if (words.length >= 2) types.put(pname, words[words.length - 2]);
            }

            Comment c = commentBefore(s, m.start());
            out.put(name, new StubEntry(name, c.kind, c.description, params, types, c.paramDocs, c.allowedIn, c.unique));
        }
        return out;
    }

    /** Read and parse the {@code /** … *}{@code /} block immediately preceding {@code defStart}. */
    private static Comment commentBefore(String s, int defStart) {
        Comment c = new Comment();
        int close = s.lastIndexOf("*/", defStart);
        if (close < 0) return c;
        // only attach if nothing but whitespace sits between the comment and the def
        if (!s.substring(close + 2, defStart).isBlank()) return c;
        int open = s.lastIndexOf("/**", close);
        if (open < 0) return c;

        String body = s.substring(open + 3, close);
        StringBuilder desc = new StringBuilder();
        for (String line : body.split("\n")) {
            String t = line.strip();
            if (t.startsWith("*")) t = t.substring(1).strip();
            if (t.startsWith("@kind")) {
                c.kind = t.substring("@kind".length()).trim();
            } else if (t.startsWith("@unique")) {
                c.unique = true;
            } else if (t.startsWith("@allowedIn")) {
                for (String b : t.substring("@allowedIn".length()).trim().split("[,\\s]+")) {
                    if (!b.isEmpty()) c.allowedIn.add(b);
                }
            } else if (t.startsWith("@param")) {
                String rest = t.substring("@param".length()).trim();
                int sp = rest.indexOf(' ');
                if (sp < 0) {
                    if (!rest.isEmpty()) c.paramDocs.put(rest, "");
                } else {
                    c.paramDocs.put(rest.substring(0, sp), rest.substring(sp + 1).trim());
                }
            } else if (!t.startsWith("@") && !t.isEmpty()) {
                if (desc.length() > 0) desc.append(' ');
                desc.append(t);
            }
        }
        c.description = desc.toString();
        return c;
    }

    private static final class Comment {
        String kind = "";
        String description = "";
        boolean unique = false;
        final Map<String, String> paramDocs = new LinkedHashMap<>();
        final java.util.List<String> allowedIn = new java.util.ArrayList<>();
    }
}
