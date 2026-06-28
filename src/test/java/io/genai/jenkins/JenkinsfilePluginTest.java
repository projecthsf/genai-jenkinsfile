package io.genai.jenkins;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Light tests: the grammar parses a real pipeline cleanly, and completion fires. */
public class JenkinsfilePluginTest extends BasePlatformTestCase {

    private static final String PIPELINE =
            "pipeline {\n" +
            "  agent any\n" +
            "  stages {\n" +
            "    stage('Build') {\n" +
            "      steps {\n" +
            "        echo 'hi'\n" +
            "        sh 'make'\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "  post {\n" +
            "    always { echo 'done' }\n" +
            "  }\n" +
            "}\n";

    public void testParsesWithoutErrors() {
        PsiFile file = myFixture.configureByText("Jenkinsfile", PIPELINE);
        Collection<PsiErrorElement> errors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement.class);
        assertEmpty(errors);
    }

    /** Named args, operators and punctuation must parse without errors. */
    public void testNamedArgsAndOperatorsParse() {
        PsiFile file = myFixture.configureByText("Jenkinsfile",
                "pipeline {\n" +
                "  agent any\n" +
                "  stages {\n" +
                "    stage('x') {\n" +
                "      steps {\n" +
                "        node(label: 'text')\n" +                       // bodyless call
                "        archiveArtifacts artifacts: 'build/*.zip'\n" +   // call without parens
                "        node(label: 'linux', retries: 2) { sh 'make' }\n" +
                "        timeout(time: 5, unit: 'MINUTES') { echo 'go' }\n" +
                "        script { def v = 1 + 2 }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        assertEmpty(PsiTreeUtil.findChildrenOfType(file, PsiErrorElement.class));
    }

    /** Named-argument keys (name:, defaultValue:) get the named-argument colour. */
    public void testNamedArgumentColored() {
        myFixture.configureByText("Jenkinsfile",
                "pipeline {\n  parameters {\n    string(name: 'ENV', defaultValue: 'dev')\n  }\n}\n");
        boolean colored = myFixture.doHighlighting().stream()
                .anyMatch(h -> h.forcedTextAttributesKey == JenkinsfileSyntaxHighlighter.NAMED_ARG);
        assertTrue("named-argument keys should be coloured", colored);
    }

    /** Command-style named args (environment name: …, value: …) and env-var defs are coloured. */
    public void testCommandNamedArgsAndEnvVarColored() {
        myFixture.configureByText("Jenkinsfile",
                "pipeline {\n  stages { stage('x') {\n" +
                "    when { environment name: 'RUN_TESTS', value: 'true' }\n" +
                "    environment { STAGE_VAR = 'local-init' }\n" +
                "    steps { echo 'hi' }\n" +
                "  } }\n}\n");
        var keys = myFixture.doHighlighting().stream()
                .map(h -> h.forcedTextAttributesKey).filter(java.util.Objects::nonNull).toList();
        assertTrue("command-style named arg should be coloured", keys.contains(JenkinsfileSyntaxHighlighter.NAMED_ARG));
        assertTrue("env var definition should be coloured", keys.contains(JenkinsfileSyntaxHighlighter.ENV_VAR));
    }

    /** A known step called inside arguments (string(...) in withCredentials([...])) is coloured. */
    public void testNestedCallColored() {
        myFixture.configureByText("Jenkinsfile",
                "pipeline {\n  stages { stage('x') { steps {\n" +
                "    withCredentials([string(credentialsId: 'c', variable: 'V')]) { echo 'x' }\n" +
                "  } } }\n}\n");
        boolean stepColored = myFixture.doHighlighting().stream()
                .anyMatch(h -> h.forcedTextAttributesKey == JenkinsfileSyntaxHighlighter.STEP);
        assertTrue("nested string(...) call should be coloured", stepColored);
    }

    /** Primitive data types (int, boolean) are keyword tokens. */
    public void testPrimitiveTypeIsKeyword() {
        com.intellij.lexer.Lexer lex = new JenkinsfileSyntaxHighlighter().getHighlightingLexer();
        lex.start("int");
        assertEquals(io.genai.jenkins.psi.JenkinsfileTypes.KEYWORD, lex.getTokenType());
    }

    /** Folding offers regions for multi-line blocks and multi-line strings, with sensible placeholders. */
    public void testFolding() {
        myFixture.configureByText("Jenkinsfile",
                "pipeline {\n  stages {\n    stage('Build') {\n      steps {\n" +
                "        sh \"\"\"\n          echo hi\n        \"\"\"\n" +
                "      }\n    }\n  }\n}\n");
        JenkinsfileFoldingBuilder builder = new JenkinsfileFoldingBuilder();
        com.intellij.lang.folding.FoldingDescriptor[] regions =
                builder.buildFoldRegions(myFixture.getFile(), myFixture.getEditor().getDocument(), false);
        // pipeline, stages, stage, steps, and the sh triple-string = 5 multi-line regions
        assertTrue("expected several fold regions, got " + regions.length, regions.length >= 5);
        boolean blockPlaceholder = false, stringPlaceholder = false;
        for (com.intellij.lang.folding.FoldingDescriptor d : regions) {
            String ph = builder.getPlaceholderText(d.getElement());
            if ("{…}".equals(ph)) blockPlaceholder = true;
            if ("\"\"\"…\"\"\"".equals(ph)) stringPlaceholder = true;
        }
        assertTrue("block folds to {…}", blockPlaceholder);
        assertTrue("triple string folds to \"\"\"…\"\"\"", stringPlaceholder);
    }

    /** Breadcrumbs label each block by name + first string arg, so stages are distinguishable. */
    public void testBreadcrumbs() {
        myFixture.configureByText("Jenkinsfile",
                "pipeline {\n  stages {\n    stage('Build') {\n      steps { echo 'hi' }\n    }\n  }\n}\n");
        JenkinsfileBreadcrumbsProvider provider = new JenkinsfileBreadcrumbsProvider();
        java.util.Collection<io.genai.jenkins.psi.JfBlock> blocks =
                com.intellij.psi.util.PsiTreeUtil.findChildrenOfType(myFixture.getFile(), io.genai.jenkins.psi.JfBlock.class);
        boolean stage = false, pipeline = false;
        for (io.genai.jenkins.psi.JfBlock b : blocks) {
            assertTrue(provider.acceptElement(b));
            String info = provider.getElementInfo(b);
            if (info.equals("pipeline")) pipeline = true;
            if (info.equals("stage 'Build'")) stage = true;
        }
        assertTrue("pipeline crumb", pipeline);
        assertTrue("stage crumb shows its name", stage);
    }

    /** A library annotation at the top of a Jenkinsfile parses cleanly. */
    public void testLibraryAnnotationParses() {
        PsiFile file = myFixture.configureByText("Jenkinsfile",
                "@Library('jenkins-shared-library') _\n" +
                "pipeline {\n  agent any\n  stages { stage('x') { steps { echo 'hi' } } }\n}\n");
        assertEmpty(PsiTreeUtil.findChildrenOfType(file, PsiErrorElement.class));
    }

    /** A multi-line sh """…""" with an interior double quote (echo "x") must be ONE string token
     *  (the previous `~`-based lexer rule split it, which then broke Shell injection). */
    public void testTripleQuoteWithInteriorDoubleQuote() {
        PsiFile f = myFixture.configureByText("Jenkinsfile",
                "pipeline {\n  stages { stage('x') { steps {\n" +
                "    sh \"\"\"\n      echo \"hi\"\n      docker build -t ${REG}:img .\n      rm x\n    \"\"\"\n" +
                "  } } }\n}\n");
        assertEmpty(PsiTreeUtil.findChildrenOfType(f, PsiErrorElement.class));
        boolean oneToken = PsiTreeUtil.findChildrenOfType(f, io.genai.jenkins.psi.JfStringLiteral.class).stream()
                .anyMatch(s -> s.getText().startsWith("\"\"\"")
                        && s.getText().contains("echo \"hi\"") && s.getText().endsWith("\"\"\""));
        assertTrue("triple-quoted string with interior quotes must be a single token", oneToken);
    }

    /** '$' interpolation (e.g. ${REGISTRY}, $BUILD_NUMBER) must not be a bad character. */
    public void testDollarVariablesParse() {
        PsiFile file = myFixture.configureByText("Jenkinsfile",
                "pipeline {\n" +
                "  agent any\n" +
                "  stages {\n" +
                "    stage('x') {\n" +
                "      steps {\n" +
                "        echo $BUILD_NUMBER\n" +
                "        sh \"docker push ${REGISTRY}:img-$BUILD_NUMBER\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        assertEmpty(PsiTreeUtil.findChildrenOfType(file, PsiErrorElement.class));
    }

    /** Groovy boolean/bitwise operators inside script blocks (&&, ||) must not be bad characters. */
    public void testGroovyBooleanOperatorsParse() {
        PsiFile file = myFixture.configureByText("Jenkinsfile",
                "pipeline {\n" +
                "  stages {\n" +
                "    stage('x') {\n" +
                "      steps {\n" +
                "        script {\n" +
                "          if (env.BRANCH_NAME == 'main' && params.RUN_TESTS == true) {\n" +
                "            echo 'run'\n" +
                "          } else if (env.BRANCH_NAME != 'main' || !params.RUN_TESTS) {\n" +
                "            echo 'skip'\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        assertEmpty(PsiTreeUtil.findChildrenOfType(file, PsiErrorElement.class));
    }

    public void testFileTypeAssociation() {
        PsiFile file = myFixture.configureByText("Jenkinsfile", PIPELINE);
        assertInstanceOf(file, JenkinsfileFile.class);
    }

    /** Names like Jenkinsfile.prod and *.jenkinsfile are recognised; lookalikes are not. */
    public void testFileNameMatching() {
        assertInstanceOf(myFixture.configureByText("Jenkinsfile.prod", PIPELINE), JenkinsfileFile.class);
        assertInstanceOf(myFixture.configureByText("ci.jenkinsfile", PIPELINE), JenkinsfileFile.class);
        // must NOT claim a name that merely ends with "Jenkinsfile"
        assertFalse(myFixture.configureByText("MyJenkinsfile.prod", PIPELINE) instanceof JenkinsfileFile);
    }

    /** Reformat lays out the declarative skeleton with 4-space nesting. */
    public void testReformatsDeclarativeSkeleton() {
        myFixture.configureByText("Jenkinsfile",
                "pipeline {\n" +
                "stages {\n" +
                "stage('Build') {\n" +
                "steps {\n" +
                "echo 'hi'\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "}\n");
        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(getProject(), (Runnable) () ->
                com.intellij.psi.codeStyle.CodeStyleManager.getInstance(getProject()).reformat(myFixture.getFile()));
        assertEquals(
                "pipeline {\n" +
                "    stages {\n" +
                "        stage('Build') {\n" +
                "            steps {\n" +
                "                echo 'hi'\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}\n",
                myFixture.getFile().getText());
    }

    /** Reformat indents Groovy inside a script block by brace depth — including else-if chains. */
    public void testReformatsGroovyInScript() {
        myFixture.configureByText("Jenkinsfile",
                "pipeline {\n" +
                "stages {\n" +
                "stage('x') {\n" +
                "steps {\n" +
                "script {\n" +
                "if (a && b) {\n" +
                "echo 'x'\n" +
                "} else if (c || d) {\n" +
                "echo 'y'\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "}\n");
        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(getProject(), (Runnable) () ->
                com.intellij.psi.codeStyle.CodeStyleManager.getInstance(getProject()).reformat(myFixture.getFile()));
        assertEquals(
                "pipeline {\n" +
                "    stages {\n" +
                "        stage('x') {\n" +
                "            steps {\n" +
                "                script {\n" +
                "                    if (a && b) {\n" +
                "                        echo 'x'\n" +
                "                    } else if (c || d) {\n" +
                "                        echo 'y'\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}\n",
                myFixture.getFile().getText());
    }

    /** Inside a known step's parens, completion offers that step's parameter names. */
    public void testCompletionInsideStepOffersParams() {
        myFixture.configureByText("Jenkinsfile",
                "pipeline {\n  stages {\n    stage('x') {\n      steps {\n        sh(<caret>)\n      }\n    }\n  }\n}\n");
        myFixture.complete(CompletionType.BASIC);
        List<String> lookups = myFixture.getLookupElementStrings();
        assertNotNull(lookups);
        assertContainsElements(lookups, "script");
    }

    /** Ctrl-Click on a usage of a def-variable navigates to its declaration. */
    public void testGotoDefVariable() {
        myFixture.configureByText("Jenkinsfile",
                "pipeline {\n  stages {\n    stage('x') {\n      steps {\n        script {\n" +
                "          def foo = 1\n" +
                "          echo fo<caret>o\n" +
                "        }\n      }\n    }\n  }\n}\n");
        PsiElement leaf = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        PsiElement[] targets = new JenkinsfileGotoDeclarationHandler()
                .getGotoDeclarationTargets(leaf, myFixture.getCaretOffset(), myFixture.getEditor());
        assertNotNull(targets);
        assertEquals(1, targets.length);
        assertEquals("foo", targets[0].getText());
        assertEquals("def", PsiTreeUtil.prevVisibleLeaf(targets[0]).getText());   // it's the declaration
    }

    /** Ctrl-Click on env.KEY navigates to its environment{} declaration. */
    public void testGotoEnvKey() {
        myFixture.configureByText("Jenkinsfile",
                "pipeline {\n  environment {\n    REGISTRY = 'docker.io'\n  }\n" +
                "  stages {\n    stage('x') {\n      steps {\n        echo env.REGIS<caret>TRY\n" +
                "      }\n    }\n  }\n}\n");
        PsiElement leaf = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        PsiElement[] targets = new JenkinsfileGotoDeclarationHandler()
                .getGotoDeclarationTargets(leaf, myFixture.getCaretOffset(), myFixture.getEditor());
        assertNotNull(targets);
        assertEquals(1, targets.length);
        assertEquals("REGISTRY", targets[0].getText());
        assertEquals("=", PsiTreeUtil.nextVisibleLeaf(targets[0]).getText());      // it's the assignment
    }

    /** Ctrl-Click on ${env.KEY} inside a string navigates to the environment declaration. */
    public void testGotoEnvKeyInStringQualified() {
        assertGotoTarget(
                "pipeline {\n  environment {\n    REGISTRY = 'docker.io'\n  }\n" +
                "  stages { stage('x') { steps {\n        sh \"push ${env.REGIS<caret>TRY}/img\"\n} } }\n}\n",
                "REGISTRY");
    }

    /** Ctrl-Click on a bare $VAR inside a string also resolves to the environment declaration. */
    public void testGotoEnvKeyInStringBare() {
        assertGotoTarget(
                "pipeline {\n  environment {\n    REGISTRY = 'docker.io'\n  }\n" +
                "  stages { stage('x') { steps {\n        sh \"push $REGIS<caret>TRY/img\"\n} } }\n}\n",
                "REGISTRY");
    }

    /** Ctrl-Click on ${foo} inside a string resolves to a def declaration. */
    public void testGotoDefInStringInterpolation() {
        assertGotoTarget(
                "pipeline {\n  stages { stage('x') { steps {\n        script {\n          def foo = 1\n" +
                "          sh \"run ${fo<caret>o}\"\n        }\n} } }\n}\n",
                "foo");
    }

    /** Plain (non-interpolated) string text is not a navigation target. */
    public void testNoGotoForPlainStringText() {
        myFixture.configureByText("Jenkinsfile",
                "pipeline {\n  environment {\n    REGISTRY = 'doc<caret>ker.io'\n  }\n}\n");
        PsiElement leaf = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertNull(new JenkinsfileGotoDeclarationHandler()
                .getGotoDeclarationTargets(leaf, myFixture.getCaretOffset(), myFixture.getEditor()));
    }

    private void assertGotoTarget(String source, String expectedTargetText) {
        myFixture.configureByText("Jenkinsfile", source);
        PsiElement leaf = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        PsiElement[] targets = new JenkinsfileGotoDeclarationHandler()
                .getGotoDeclarationTargets(leaf, myFixture.getCaretOffset(), myFixture.getEditor());
        assertNotNull("expected a navigation target", targets);
        assertEquals(1, targets.length);
        assertEquals(expectedTargetText, targets[0].getText());
    }

    /** A directive keyword (stages) now resolves into the stub library too. */
    public void testGotoDirectiveResolvesIntoStub() {
        myFixture.configureByText("Jenkinsfile", "pipeline {\n  sta<caret>ges {\n  }\n}\n");
        PsiElement leaf = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        PsiElement[] targets = new JenkinsfileGotoDeclarationHandler()
                .getGotoDeclarationTargets(leaf, myFixture.getCaretOffset(), myFixture.getEditor());
        assertNotNull(targets);
        assertEquals(1, targets.length);
        assertEquals("stages", targets[0].getText());
        assertEquals(JenkinsfileKnowledge.RESOURCE, targets[0].getContainingFile().getName());
    }

    /** A known directive token is a documentation target, so hover shows the doc popup. */
    public void testHoverDocResolvesKnownWord() {
        myFixture.configureByText("Jenkinsfile", "pipeline {\n  st<caret>ages {\n  }\n}\n");
        JenkinsfileDocumentationProvider provider = new JenkinsfileDocumentationProvider();
        PsiElement ctx = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        PsiElement target = provider.getCustomDocumentationElement(
                myFixture.getEditor(), myFixture.getFile(), ctx, myFixture.getCaretOffset());
        assertNotNull("known word should be a doc target", target);
        assertNotNull(provider.generateDoc(target, ctx));
    }

    /** Quick-doc renders a signature, the description and a parameters section (with per-param docs). */
    public void testDocRendersSignatureAndParams() {
        StubEntry e = new StubEntry("sh", "step", "Run a shell script",
                List.of("script", "returnStdout"),
                Map.of("script", "String"),
                Map.of("script", "the command to run"),
                List.of());
        String html = JenkinsfileDocumentationProvider.render(e);
        assertTrue(html.contains("sh("));
        assertTrue(html.contains("script"));
        assertTrue(html.contains("returnStdout"));
        assertTrue(html.contains("Run a shell script"));
        assertTrue(html.contains("Parameters"));
        assertTrue(html.contains("the command to run"));   // per-param doc rendered
        assertTrue(html.contains("String"));               // param type rendered
    }

    /** A block directive's signature shows the braces form, and lists where it's allowed. */
    public void testDocBlockSignature() {
        StubEntry e = new StubEntry("steps", "section", "Run steps", List.of(), Map.of(), Map.of(), List.of("stage"));
        String html = JenkinsfileDocumentationProvider.render(e);
        assertTrue(html.contains("steps { … }"));
        assertTrue(html.contains("Allowed in"));
        assertTrue(html.contains("stage"));
    }

    /** Hovering @Library shows a static shared-library explanation. */
    public void testLibraryAnnotationDoc() {
        myFixture.configureByText("Jenkinsfile", "@Library('x') _\npipeline { }\n");
        JenkinsfileDocumentationProvider provider = new JenkinsfileDocumentationProvider();
        PsiElement libToken = myFixture.getFile().findElementAt(myFixture.getFile().getText().indexOf("Library"));
        String html = provider.generateDoc(libToken, libToken);
        assertNotNull(html);
        assertTrue(html.contains("@Library"));
        assertTrue(html.contains("shared library"));
    }

    /** Inside `post` only post-conditions are offered — not directives or steps. */
    public void testCompletionInPostOffersOnlyConditions() {
        myFixture.configureByText("Jenkinsfile", "pipeline {\n  post {\n    <caret>\n  }\n}\n");
        myFixture.complete(CompletionType.BASIC);
        List<String> lookups = myFixture.getLookupElementStrings();
        assertNotNull(lookups);
        assertContainsElements(lookups, "always", "success", "failure", "cleanup");
        assertFalse("agent not valid in post", lookups.contains("agent"));
        assertFalse("sh not valid directly in post", lookups.contains("sh"));
    }

    /** Context-aware completion: each construct is offered only where it's valid. */
    public void testCompletionRelevance() {
        JenkinsfileCatalog c = JenkinsfileCatalog.getInstance(getProject());
        // stages holds only stage
        assertTrue(JenkinsfileCompletionContributor.relevant(c, c.entry("stage"), "stages"));
        assertFalse(JenkinsfileCompletionContributor.relevant(c, c.entry("steps"), "stages"));
        // post holds only conditions
        assertTrue(JenkinsfileCompletionContributor.relevant(c, c.entry("always"), "post"));
        assertFalse(JenkinsfileCompletionContributor.relevant(c, c.entry("agent"), "post"));
        assertFalse(JenkinsfileCompletionContributor.relevant(c, c.entry("sh"), "post"));
        // steps holds steps
        assertTrue(JenkinsfileCompletionContributor.relevant(c, c.entry("sh"), "steps"));
        // pipeline holds top-level directives, not raw steps
        assertTrue(JenkinsfileCompletionContributor.relevant(c, c.entry("agent"), "pipeline"));
        assertFalse(JenkinsfileCompletionContributor.relevant(c, c.entry("sh"), "pipeline"));
        // unknown block (e.g. a shared-library block step) → lenient
        assertTrue(JenkinsfileCompletionContributor.relevant(c, c.entry("sh"), "myCustomBlock"));
    }

    /** Pressing Enter after an opening brace indents the new line one level deeper. */
    public void testEnterIndentsToBraceDepth() {
        myFixture.configureByText("Jenkinsfile", "pipeline {\n    stages {<caret>\n    }\n}\n");
        myFixture.type('\n');
        // new line sits two levels deep (inside pipeline → stages) = 8 spaces
        assertEquals("pipeline {\n    stages {\n        \n    }\n}\n", myFixture.getFile().getText());
    }

    /** Pressing Enter after a closing brace keeps the new line at the reduced depth. */
    public void testEnterAfterCloseStaysAtOuterDepth() {
        myFixture.configureByText("Jenkinsfile", "pipeline {\n    stages {\n    }<caret>\n}\n");
        myFixture.type('\n');
        // after stages' closing brace we're back inside pipeline = 4 spaces
        assertEquals("pipeline {\n    stages {\n    }\n    \n}\n", myFixture.getFile().getText());
    }

    /** When-conditions are now in the catalog (so the annotator colours allOf/branch/expression). */
    public void testCatalogHasWhenConditions() {
        JenkinsfileCatalog catalog = JenkinsfileCatalog.getInstance(getProject());
        assertEquals("when", catalog.completions().get("allOf"));
        assertEquals("when", catalog.completions().get("anyOf"));
        assertEquals("when", catalog.completions().get("expression"));
    }

    /** Semantic highlighting: steps get the step colour, everything else the directive colour. */
    public void testAnnotatorColorKeys() {
        StubEntry step = new StubEntry("sh", "step", "", List.of(), Map.of(), Map.of(), List.of());
        StubEntry directive = new StubEntry("when", "directive", "", List.of(), Map.of(), Map.of(), List.of());
        StubEntry cond = new StubEntry("allOf", "when", "", List.of(), Map.of(), Map.of(), List.of());
        StubEntry agentParam = new StubEntry("image", "agent", "", List.of(), Map.of(), Map.of(), List.of());
        // all invocations share the step/"call" colour — steps, agent/option config, and
        // call-style when-conditions (branch 'main')
        assertEquals(JenkinsfileSyntaxHighlighter.STEP, JenkinsfileAnnotator.keyFor(step, false));
        assertEquals(JenkinsfileSyntaxHighlighter.STEP, JenkinsfileAnnotator.keyFor(agentParam, false));
        assertEquals(JenkinsfileSyntaxHighlighter.STEP, JenkinsfileAnnotator.keyFor(cond, false));   // branch 'main'
        // structural keywords stay directive-coloured even bare (agent any)
        assertEquals(JenkinsfileSyntaxHighlighter.DIRECTIVE, JenkinsfileAnnotator.keyFor(directive, false));
        // block-openers are directives: a `{ }` opener (allOf { }, unstable { }) even if also a step
        assertEquals(JenkinsfileSyntaxHighlighter.DIRECTIVE, JenkinsfileAnnotator.keyFor(cond, true));
        assertEquals(JenkinsfileSyntaxHighlighter.DIRECTIVE, JenkinsfileAnnotator.keyFor(step, true));
    }

    /** The context inspection flags a directive placed in a block that doesn't allow it. */
    public void testContextInspectionFlagsMisplacedAgent() {
        myFixture.enableInspections(new JenkinsfileContextInspection());
        myFixture.configureByText("Jenkinsfile",
                "pipeline {\n  options {\n    agent any\n  }\n}\n");
        boolean flagged = myFixture.doHighlighting().stream().anyMatch(h ->
                h.getDescription() != null && h.getDescription().contains("agent")
                        && h.getDescription().contains("only allowed"));
        assertTrue("agent inside options should be flagged", flagged);
    }

    /** A directive that may appear only once is flagged when duplicated in the same block. */
    public void testContextFlagsDuplicateDirective() {
        myFixture.enableInspections(new JenkinsfileContextInspection());
        myFixture.configureByText("Jenkinsfile",
                "pipeline {\n  agent any\n  agent none\n  stages { stage('x') { steps { echo 'hi' } } }\n}\n");
        long dup = myFixture.doHighlighting().stream()
                .filter(h -> h.getDescription() != null && h.getDescription().contains("only appear once"))
                .count();
        assertEquals("the second agent should be flagged once", 1, dup);
    }

    /** A single occurrence of a unique directive is not flagged. */
    public void testContextAllowsSingleUniqueDirective() {
        myFixture.enableInspections(new JenkinsfileContextInspection());
        myFixture.configureByText("Jenkinsfile",
                "pipeline {\n  agent any\n  options { timeout(time: 1, unit: 'HOURS') }\n" +
                "  stages { stage('x') { steps { echo 'hi' } } }\n}\n");
        boolean flagged = myFixture.doHighlighting().stream()
                .anyMatch(h -> h.getDescription() != null && h.getDescription().contains("only appear once"));
        assertFalse("single agent/options should be fine", flagged);
    }

    /** A when-condition placed outside a when block is flagged. */
    public void testContextFlagsConditionOutsideWhen() {
        myFixture.enableInspections(new JenkinsfileContextInspection());
        myFixture.configureByText("Jenkinsfile",
                "pipeline {\n  stages { stage('x') {\n    options { branch 'main' }\n  } }\n}\n");
        boolean flagged = myFixture.doHighlighting().stream().anyMatch(h ->
                h.getDescription() != null && h.getDescription().contains("branch")
                        && h.getDescription().contains("only allowed"));
        assertTrue("branch outside when should be flagged", flagged);
    }

    /** environment is dual-purpose (directive + when-condition): valid inside when, not flagged. */
    public void testContextAllowsEnvironmentInWhen() {
        myFixture.enableInspections(new JenkinsfileContextInspection());
        myFixture.configureByText("Jenkinsfile",
                "pipeline {\n  stages { stage('x') {\n" +
                "    when { environment name: 'RUN_TESTS', value: 'true' }\n" +
                "    steps { echo 'hi' }\n  } }\n}\n");
        boolean flagged = myFixture.doHighlighting().stream().anyMatch(h ->
                h.getDescription() != null && h.getDescription().contains("environment")
                        && h.getDescription().contains("only allowed"));
        assertFalse("environment as a when-condition should be allowed", flagged);
    }

    /** The context inspection allows a directive in a valid block. */
    public void testContextInspectionAllowsAgentInStage() {
        myFixture.enableInspections(new JenkinsfileContextInspection());
        myFixture.configureByText("Jenkinsfile",
                "pipeline {\n  agent any\n  stages {\n    stage('x') {\n      agent any\n" +
                "      steps { echo 'hi' }\n    }\n  }\n}\n");
        boolean flagged = myFixture.doHighlighting().stream().anyMatch(h ->
                h.getDescription() != null && h.getDescription().contains("agent")
                        && h.getDescription().contains("only allowed"));
        assertFalse("agent in pipeline/stage should be allowed", flagged);
    }

    /** GDSL → stub conversion parses steps and round-trips through the stub parser. */
    public void testGdslToStubRoundTrip() {
        String gdsl =
                "method(name: 'sh', type: 'Object', params: [script:'java.lang.String'], doc: 'Shell Script')\n" +
                "method(name: 'archiveArtifacts', type: 'Object', params: [artifacts:'java.lang.String', fingerprint:'boolean'], doc: 'Archive the artifacts')\n";
        Map<String, JenkinsGdslParser.Step> steps = JenkinsGdslParser.parse(gdsl);
        assertEquals(2, steps.size());
        assertEquals(List.of("artifacts", "fingerprint"), steps.get("archiveArtifacts").params);

        String stub = JenkinsGdslParser.toStub(steps);
        Map<String, StubEntry> parsed = JenkinsfileStubParser.parse(stub);
        assertTrue(parsed.containsKey("sh"));
        assertEquals("step", parsed.get("sh").kind);
        assertEquals("Shell Script", parsed.get("sh").doc);
        assertEquals(List.of("artifacts", "fingerprint"), parsed.get("archiveArtifacts").params);
    }

    /** Embedded-language injection defaults on and is toggleable. */
    public void testEmbeddedLanguageInjectionSetting() {
        assertTrue("default should be on", JenkinsfileSettings.injectEmbeddedLanguages());
        JenkinsfileSettings.setInjectEmbeddedLanguages(false);
        assertFalse(JenkinsfileSettings.injectEmbeddedLanguages());
        JenkinsfileSettings.setInjectEmbeddedLanguages(true);
        assertTrue(JenkinsfileSettings.injectEmbeddedLanguages());
    }

    /** Standard option directives and agent/docker config keys are catalogued, so config blocks
     *  colour uniformly (no half-highlighted gaps). */
    public void testCatalogHasConfigKeys() {
        JenkinsfileCatalog catalog = JenkinsfileCatalog.getInstance(getProject());
        // option directives → step-coloured like timeout
        assertEquals("step", catalog.completions().get("retry"));
        assertEquals("step", catalog.completions().get("disableConcurrentBuilds"));
        // docker/agent params → agent-coloured like label
        assertEquals("agent", catalog.completions().get("image"));
        assertEquals("agent", catalog.completions().get("registryUrl"));
        // agent types
        assertEquals("agent", catalog.completions().get("dockerfile"));
        assertEquals("agent", catalog.completions().get("kubernetes"));
    }

    /** The bundled stub catalog is parsed into steps and directives with kinds and docs. */
    public void testStubCatalogParsed() {
        JenkinsfileCatalog catalog = JenkinsfileCatalog.getInstance(getProject());
        assertEquals("step", catalog.completions().get("sh"));
        assertEquals("step", catalog.completions().get("node"));
        assertEquals("section", catalog.completions().get("pipeline"));
        assertNotNull(catalog.doc("pipeline"));
        assertNotNull(catalog.stepParams("sh"));   // sh has 'script'
    }

    /** Ctrl-Click on a global step resolves into the bundled stub's def declaration. */
    public void testGotoStepResolvesIntoStub() {
        myFixture.configureByText("Jenkinsfile",
                "pipeline {\n  stages { stage('x') { steps {\n        s<caret>h 'make'\n} } }\n}\n");
        PsiElement leaf = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        PsiElement[] targets = new JenkinsfileGotoDeclarationHandler()
                .getGotoDeclarationTargets(leaf, myFixture.getCaretOffset(), myFixture.getEditor());
        assertNotNull("step should resolve into the stub library", targets);
        assertEquals(1, targets.length);
        assertEquals("sh", targets[0].getText());
        assertEquals(JenkinsfileKnowledge.RESOURCE, targets[0].getContainingFile().getName());
    }

    /** A project stub file (.jenkins/*.jenkinsfile) adds its own steps on top of the bundled base. */
    public void testProjectStubExtendsCatalog() throws Exception {
        String base = getProject().getBasePath();
        assertNotNull(base);
        java.nio.file.Path dir = java.nio.file.Paths.get(base, ".jenkins");
        java.nio.file.Files.createDirectories(dir);
        java.nio.file.Files.writeString(dir.resolve("vc.jenkinsfile"),
                "/**\n * Deploy a VC service.\n * @kind step\n" +
                " * @param service the service name\n * @param env target environment\n */\n" +
                "def vcDeploy(service, env) {}\n");

        JenkinsfileCatalog catalog = JenkinsfileCatalog.getInstance(getProject());
        assertTrue("project step should be present", catalog.completions().containsKey("vcDeploy"));
        assertEquals(List.of("service", "env"), catalog.stepParams("vcDeploy"));
        assertNotNull(catalog.doc("vcDeploy"));
        StubEntry e = catalog.entry("vcDeploy");
        assertNotNull(e);
        assertEquals("the service name", e.paramDocs.get("service"));   // per-param doc parsed
        // bundled base still available
        assertTrue(catalog.completions().containsKey("stage"));
    }
}
