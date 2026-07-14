import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.grammarkit.tasks.GenerateParserTask
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    // 2.18.0 (not 2.1.0): the pluginVerification DSL used by the publish gate is
    // incompatible with Gradle 9.3 on 2.1.0. Matches the php-portable setup.
    id("org.jetbrains.intellij.platform") version "2.18.0"
    id("org.jetbrains.grammarkit") version "2022.3.2.2"
}

group = "io.genai.jenkins"
version = "1.0.1"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Dev builds use the locally-installed GoLand; CI (and any machine without it) uses a
// downloaded IDE, so the build is reproducible anywhere.
val useLocalIde = file("/Applications/GoLand.app/Contents").exists() &&
        !providers.environmentVariable("CI").isPresent

dependencies {
    intellijPlatform {
        if (useLocalIde) {
            local("/Applications/GoLand.app/Contents")
        } else {
            intellijIdeaCommunity("2024.1")
        }
        // instrumentationTools() removed in plugin 2.x — added automatically now.
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(17) }
}

kotlin {
    jvmToolchain(17)
}

// ---- Grammar-Kit / JFlex code generation -----------------------------------
// The GENERATED parser/lexer live in src/main/gen and ARE committed, so the project builds
// on CI (and any clean checkout) without the grammar sources. The grammar itself
// (src/main/grammar/*.bnf|*.flex) is kept private and out of the repo (see .gitignore), and
// is NOT wired into the normal build. When you change the grammar locally, regenerate with
//   ./gradlew generateGrammar
// and commit the updated src/main/gen. Generation is only offered when the grammar is present.
val genRoot = layout.projectDirectory.dir("src/main/gen")
sourceSets["main"].java.srcDir(genRoot)

val grammarPresent = file("src/main/grammar/Jenkinsfile.bnf").exists() &&
        file("src/main/grammar/Jenkinsfile.flex").exists()

if (grammarPresent) {
    val generateJenkinsParser by tasks.registering(GenerateParserTask::class) {
        sourceFile.set(file("src/main/grammar/Jenkinsfile.bnf"))
        targetRootOutputDir.set(genRoot)
        pathToParser.set("io/genai/jenkins/parser/JenkinsfileParser.java")
        pathToPsiRoot.set("io/genai/jenkins/psi")
        purgeOldFiles.set(true)
    }

    val generateJenkinsLexer by tasks.registering(GenerateLexerTask::class) {
        sourceFile.set(file("src/main/grammar/Jenkinsfile.flex"))
        targetOutputDir.set(genRoot.dir("io/genai/jenkins/lexer"))
        purgeOldFiles.set(true)
        dependsOn(generateJenkinsParser)   // .flex imports the generated JenkinsfileTypes
    }

    tasks.register("generateGrammar") {
        group = "build"
        description = "Regenerate the committed src/main/gen parser & lexer from the private grammar."
        dependsOn(generateJenkinsParser, generateJenkinsLexer)
    }
}

// The settings indexer is flaky against a local() IDE; skip it.
tasks.named("buildSearchableOptions") { enabled = false }

// Never ship grammar sources — only the generated parser/lexer classes are needed at runtime.
tasks.withType<Jar>().configureEach {
    exclude("**/*.bnf", "**/*.flex", "**/grammar/**")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "233"
            untilBuild = provider { null }
        }
    }

    // `./gradlew publishPlugin` reads the JetBrains Marketplace token from the PUBLISH_TOKEN
    // env var (set as a GitHub Actions secret). No signing configured, so uploads are unsigned.
    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }

    // `./gradlew verifyPlugin` runs the JetBrains Plugin Verifier (same tool Marketplace uses).
    // This is a publish gate in CI (see .github/workflows/publish.yml).
    pluginVerification {
        failureLevel.set(listOf(
            FailureLevel.COMPATIBILITY_PROBLEMS,
            FailureLevel.INTERNAL_API_USAGES,
            FailureLevel.MISSING_DEPENDENCIES,
            FailureLevel.INVALID_PLUGIN,
        ))
        ides {
            // Verify against the newest released unified IDEA. One download, enough to catch
            // forward-compat problems.
            latest {
                types.set(listOf(IntelliJPlatformType.IntellijIdea))
            }
        }
    }
}
