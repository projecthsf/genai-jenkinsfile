import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.grammarkit.tasks.GenerateParserTask

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij.platform") version "2.1.0"
    id("org.jetbrains.grammarkit") version "2022.3.2.2"
}

group = "io.genai.jenkins"
version = "1.0.0"

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
        instrumentationTools()
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
val generatedRoot = layout.buildDirectory.dir("generated/grammar")

val generateJenkinsParser by tasks.registering(GenerateParserTask::class) {
    sourceFile.set(file("src/main/grammar/Jenkinsfile.bnf"))
    targetRootOutputDir.set(generatedRoot)
    pathToParser.set("io/genai/jenkins/parser/JenkinsfileParser.java")
    pathToPsiRoot.set("io/genai/jenkins/psi")
    purgeOldFiles.set(true)
}

val generateJenkinsLexer by tasks.registering(GenerateLexerTask::class) {
    sourceFile.set(file("src/main/grammar/Jenkinsfile.flex"))
    targetOutputDir.set(generatedRoot.map { it.dir("io/genai/jenkins/lexer") })
    purgeOldFiles.set(true)
    dependsOn(generateJenkinsParser)   // .flex imports the generated JenkinsfileTypes
}

sourceSets["main"].java.srcDir(generatedRoot)

tasks.named("compileKotlin") { dependsOn(generateJenkinsParser, generateJenkinsLexer) }
tasks.named("compileJava") { dependsOn(generateJenkinsParser, generateJenkinsLexer) }

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
}
