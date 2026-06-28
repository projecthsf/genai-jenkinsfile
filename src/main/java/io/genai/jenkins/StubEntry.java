package io.genai.jenkins;

import java.util.List;
import java.util.Map;

/**
 * One construct parsed from a stub file: a {@code def name(params)} plus its
 * {@code /** … *}{@code /} doc (description, {@code @kind}, {@code @param} notes).
 * The single source of truth for completion, documentation and parameter info.
 */
public final class StubEntry {

    public final String name;
    public final String kind;                 // step / directive / section / post / when / agent
    public final String doc;                  // description (may contain HTML), or ""
    public final List<String> params;         // parameter names, in order
    public final Map<String, String> paramTypes;  // name → type (may be empty)
    public final Map<String, String> paramDocs;   // name → description (may be empty)
    public final List<String> allowedIn;      // names of blocks this may appear in; empty = unconstrained
    public final boolean unique;               // may appear at most once in its parent block

    public StubEntry(String name, String kind, String doc, List<String> params,
                     Map<String, String> paramTypes, Map<String, String> paramDocs, List<String> allowedIn) {
        this(name, kind, doc, params, paramTypes, paramDocs, allowedIn, false);
    }

    public StubEntry(String name, String kind, String doc, List<String> params,
                     Map<String, String> paramTypes, Map<String, String> paramDocs,
                     List<String> allowedIn, boolean unique) {
        this.name = name;
        this.kind = kind == null || kind.isEmpty() ? "step" : kind;
        this.doc = doc == null ? "" : doc;
        this.params = params;
        this.paramTypes = paramTypes;
        this.paramDocs = paramDocs;
        this.allowedIn = allowedIn == null ? List.of() : allowedIn;
        this.unique = unique;
    }

    public boolean isBlock() {
        return "section".equals(kind) || "directive".equals(kind) || "post".equals(kind);
    }
}
