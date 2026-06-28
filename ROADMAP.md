# Jenkinsfile Support — roadmap & architecture

## Architecture (the "proper 1.0" stack)

- **Language**: Kotlin + Java, IntelliJ Platform Gradle Plugin 2.x.
- **Grammar**: `src/main/grammar/Jenkinsfile.flex` (JFlex lexer) +
  `src/main/grammar/Jenkinsfile.bnf` (Grammar-Kit). Generated at build time into
  `build/generated/grammar/` → lexer, parser, and PSI (`JfBlock`, `JfArgs`,
  `JenkinsfileTypes`, visitor, impls). **The grammar is the single source of truth.**
- **Runtime**: `JenkinsfileParserDefinition` (Kotlin) wires the generated lexer +
  parser + PSI. Everything else is built on that PSI.
- **Cross-IDE**: depends only on `com.intellij.modules.platform` + no `until-build`,
  so it loads in every JetBrains IDE (IDEA, GoLand, PyCharm, WebStorm, …).

### ⚠️ Build requirement — JDK 17 (portable)
The Kotlin 1.9 compiler crashes on JDK 26, so the **Gradle daemon runs on JDK 17**.
This is configured portably (no machine-specific path):
- `gradle/gradle-daemon-jvm.properties` → `toolchainVersion=17` (daemon toolchain)
- `settings.gradle.kts` → foojay resolver (auto-downloads a JDK 17 if none installed)
- `gradle.properties` → `kotlin.compiler.execution.strategy=in-process`,
  `kotlin.stdlib.default.dependency=false`

The launcher JVM can be anything (e.g. 26); Gradle picks a JDK 17 for the daemon.

### CI
`.github/workflows/ci.yml` runs `./gradlew test buildPlugin` on every push/PR
(JDK 17 via setup-java; downloads IntelliJ IDEA Community since there's no local IDE).

## Done (9/9 features + foundation)

- [x] Syntax highlighting (generated lexer + `KEYWORD` token)
- [x] Basic completion (directives/sections/steps, block auto-insert)
- [x] Documentation (F1) for directives/steps
- [x] Structure view — nested outline, walks `JfBlock` PSI
- [x] Inspections + quick-fixes (pipeline-needs-stages, stage-needs-steps) — on PSI
- [x] Intention actions (Add stage / Add post / Wrap in script) — on PSI
- [x] Parameter info for step args
- [x] Formatting (`FormattingModelBuilder`, indent by `BLOCK` nesting)
- [x] Inlay hints (Kotlin declarative API — block label at closing brace)
- [x] Brace matching, line/block commenting, color settings page
- [x] Test suite (`BasePlatformTestCase`, `./gradlew test`)
- [x] **Stub-as-source catalog** — completion/docs/parameter-info/navigation all read
      a stub file (`pipeline.jenkinsfile`: a `/** @kind @param */` doc block above each
      `def`). `JenkinsfileKnowledge` parses the bundled global stub; `JenkinsfileCatalog`
      merges per-project `.jenkins/*.jenkinsfile` stubs on top. Single rich source; no
      hard-coded maps, no TSV at runtime.
- [x] **External Libraries node** — the global stub is surfaced as a read-only
      "Jenkins Pipeline" library; Go-to-Declaration on a step/directive jumps into it.
- [x] **CI** — GitHub Actions (`./gradlew test buildPlugin`), portable JDK-17 daemon.

## Remaining polish

- [x] **Stub generator** — `tools/gen-catalog.py` parses a Jenkins
      `…/pipeline-syntax/gdsl` (URL or file) and emits **stub** format, merging the
      declarative directives from `tools/catalog-seed.tsv` (generator input). Run it
      against a live Jenkins to get its exact installed-step set. (See `tools/README.md`.)
- [ ] Optionally run the generator against the team's Jenkins and commit the result as
      `<repo>/.jenkins/pipeline.jenkinsfile`.
- [ ] **Plugin Verifier in CI** — `verifyPlugin` is blocked: the `pluginVerification`
      DSL in IntelliJ Platform Gradle Plugin 2.1.0 is incompatible with Gradle 9.3's
      stricter config rules ("provider of configurations"). Bump the plugin (or pin
      Gradle) then re-enable `pluginVerification { ides { recommended() } }`.
- [ ] **Parameter info on PSI** — currently a small text scan finds the enclosing `(`.
      A true PSI version needs the grammar to model method calls (`call ::= ID args`),
      since plain `sh(...)` (no following `{`) isn't an `args` node today.
- [ ] **Kotlin-ize** the remaining Java feature files (cosmetic; behavior is done).
- [ ] Richer grammar (real Groovy expressions) if deeper resolve/rename is wanted.

## Build & run

```bash
./gradlew runIde        # sandbox IDE
./gradlew test          # test suite
./gradlew buildPlugin    # -> build/distributions/jenkinsfile-support-1.0.0.zip
```
