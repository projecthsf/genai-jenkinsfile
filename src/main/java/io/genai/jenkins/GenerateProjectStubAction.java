package io.genai.jenkins;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Tools-menu action: generate {@code <project>/.jenkins/pipeline.jenkinsfile} from a
 * Jenkins GDSL file the user saved from the browser. Lets teammates produce a
 * project stub without the Python script — open {@code <JENKINS_URL>/pipeline-syntax/gdsl}
 * while logged in, save it, and point this action at it.
 */
public final class GenerateProjectStubAction extends AnAction {

    private static final String TITLE = "Generate Jenkins Pipeline Stub";
    private static final String REL_PATH = "/.jenkins/pipeline.jenkinsfile";

    private static final String README = String.join("\n",
            "# .jenkins — Jenkinsfile plugin catalog",
            "",
            "The **GenAI Jenkinsfile** plugin gives `Jenkinsfile` editing in JetBrains IDEs:",
            "completion, hover docs, parameter info, navigation and inspections.",
            "",
            "It ships a catalog of the **standard declarative syntax** (pipeline, agent, stages,",
            "steps, when, post, options, …) — but it can't know **your** Jenkins' extras:",
            "",
            "- steps from your **shared libraries** (`@Library`), and",
            "- steps from the **plugins installed on your Jenkins**.",
            "",
            "Those vary per Jenkins instance, so this `pipeline.jenkinsfile` stub teaches the plugin",
            "exactly the steps your Jenkins offers. **Commit it** so the whole team gets the same help.",
            "",
            "## How to (re)generate it",
            "",
            "1. In your browser, log in to your Jenkins.",
            "2. Open **`<JENKINS_URL>/pipeline-syntax/gdsl`** (or: *Pipeline Syntax* page → the GDSL link).",
            "   This returns a Groovy descriptor (`.gdsl`) listing every installed step.",
            "3. **Save** that page to a file in the repo, e.g. `.jenkins/jenkins.gdsl`.",
            "4. In the IDE, either:",
            "   - **right-click the `.gdsl` file → \"Generate Jenkins Pipeline Stub\"**, or",
            "   - **Tools ▸ Generate Jenkins Pipeline Stub…** and pick the saved file.",
            "5. This writes/refreshes `.jenkins/pipeline.jenkinsfile`. Commit the result.",
            "",
            "(CLI alternative: `python3 tools/gen-catalog.py <gdsl-url-or-file> > .jenkins/pipeline.jenkinsfile`.)",
            "",
            "## Editing the stub",
            "",
            "`pipeline.jenkinsfile` is plain text — one `def name(params) {}` per construct with a",
            "`/** … */` doc block. You can hand-enrich any entry; the plugin re-reads it on save:",
            "",
            "```groovy",
            "/**",
            " * Run a shell script and optionally capture its output.",
            " * @kind step",
            " * @param script the command to run",
            " * @param returnStdout capture stdout instead of printing it",
            " */",
            "def sh(String script, boolean returnStdout) {}",
            "```",
            "",
            "Tags: `@kind` (step/directive/section/post/when/agent), `@param`, `@allowedIn` (where it's",
            "valid), `@unique` (at most once per block). Entries here override the bundled catalog.",
            "");

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project p = e.getProject();
        e.getPresentation().setEnabled(p != null && p.getBasePath() != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || project.getBasePath() == null) return;

        FileChooserDescriptor desc = new FileChooserDescriptor(true, false, false, false, false, false)
                .withTitle("Select Jenkins GDSL File")
                .withDescription("Open <JENKINS_URL>/pipeline-syntax/gdsl in your browser (logged in), save it, then select it here.");
        VirtualFile gdslFile = FileChooser.chooseFile(desc, project, null);
        if (gdslFile != null) generate(project, gdslFile, GenerateProjectStubAction.class);
    }

    /** Read {@code gdslFile}, parse it and write {@code .jenkins/pipeline.jenkinsfile}. Shared
     *  by the Tools-menu action and the right-click-on-a-.gdsl-file action. */
    static void generate(@NotNull Project project, @NotNull VirtualFile gdslFile, @NotNull Object requestor) {
        if (project.getBasePath() == null) return;

        String gdsl;
        try {
            gdsl = new String(gdslFile.contentsToByteArray(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            Messages.showErrorDialog(project, "Could not read the file: " + ex.getMessage(), TITLE);
            return;
        }

        Map<String, JenkinsGdslParser.Step> steps = JenkinsGdslParser.parse(gdsl);
        if (steps.isEmpty()) {
            Messages.showWarningDialog(project,
                    "No steps found. Make sure this is the GDSL page (…/pipeline-syntax/gdsl), not the HTML page.", TITLE);
            return;
        }
        String stub = JenkinsGdslParser.toStub(steps);

        String dirPath = project.getBasePath() + "/.jenkins";
        VirtualFile existing = VfsUtil.findFileByIoFile(new java.io.File(dirPath + "/pipeline.jenkinsfile"), false);
        if (existing != null && Messages.showYesNoDialog(project,
                ".jenkins/pipeline.jenkinsfile already exists. Overwrite it?", TITLE, Messages.getQuestionIcon()) != Messages.YES) {
            return;
        }

        try {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                try {
                    VirtualFile dir = VfsUtil.createDirectoryIfMissing(dirPath);
                    if (dir == null) throw new IOException("could not create .jenkins directory");
                    VirtualFile f = dir.findChild("pipeline.jenkinsfile");
                    if (f == null) f = dir.createChildData(requestor, "pipeline.jenkinsfile");
                    VfsUtil.saveText(f, stub);
                    // Drop a README so teammates understand why this file exists and how to refresh it.
                    if (dir.findChild("README.md") == null) {
                        VfsUtil.saveText(dir.createChildData(requestor, "README.md"), README);
                    }
                    FileEditorManager.getInstance(project).openFile(f, true);
                } catch (IOException io) {
                    throw new RuntimeException(io);
                }
            });
        } catch (Exception ex) {
            Messages.showErrorDialog(project, "Failed to write the stub: " + ex.getMessage(), TITLE);
            return;
        }

        Messages.showInfoMessage(project,
                "Generated " + steps.size() + " steps into ." + REL_PATH + ".\n"
                        + "Commit it so your team shares the same completion/docs.", TITLE);
    }
}
