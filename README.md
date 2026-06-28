# Jenkinsfile Support — JetBrains IDE plugin

Free, full editor support for **Jenkinsfile** (Jenkins declarative & scripted
pipelines) in any JetBrains IDE. Depends only on the platform module, so it runs
everywhere (IntelliJ IDEA, GoLand, PyCharm, WebStorm, PhpStorm, CLion, Rider, …).

## Features

- **Syntax highlighting** — keywords, strings, numbers, comments, brackets.
- **Completion** — directives/sections (`pipeline`, `agent`, `stages`, `stage`,
  `steps`, `when`, `post`, …) and common steps (`sh`, `git`, `withCredentials`, …),
  with `{ }` block auto-insert.
- **Documentation** (F1) for directives and steps.
- **Structure view** — nested pipeline → stages → stage → steps outline.
- **Inspections + quick-fixes** — e.g. *pipeline needs `stages`*, *stage needs `steps`*.
- **Intentions** (⌥⏎) — Add new stage, Add `post` block, Wrap line in `script { }`.
- **Parameter info** — named args for step calls (`sh(script, returnStdout, …)`).
- **Formatting** (⌘⌥L) — indent by block nesting.
- **Inlay hints** — block label at each multi-line closing brace (`}  stage 'Build'`).
- Brace matching, line/block commenting, and a Color Scheme page.

Recognises files named `Jenkinsfile` and `*.jenkinsfile`.

## Architecture

A real custom language built on **Kotlin + Grammar-Kit/JFlex**: the grammar
(`src/main/grammar/*.bnf`, `*.flex`) generates the lexer, parser and PSI at build
time, and all features run on that PSI. Cross-IDE (`com.intellij.modules.platform`
only). See `ROADMAP.md` for details and remaining items.

## Build & run

```bash
./gradlew runIde        # launch a sandbox IDE with the plugin
./gradlew test          # run the test suite
./gradlew buildPlugin    # -> build/distributions/jenkinsfile-support-1.0.0.zip
```

Install the zip via **Settings ▸ Plugins ▸ ⚙ ▸ Install Plugin from Disk…**.

> **Build note:** pinned to **JDK 17** (`gradle.properties` → `org.gradle.java.home`)
> because the Kotlin 1.9 compiler can't run on JDK 26. Update that path for your
> machine if needed.
