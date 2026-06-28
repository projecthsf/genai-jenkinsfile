package io.genai.jenkins;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * Right-click on a {@code *.gdsl} file (e.g. one saved from
 * {@code <JENKINS_URL>/pipeline-syntax/gdsl}) → "Generate Jenkins Pipeline Stub" —
 * writes {@code <project>/.jenkins/pipeline.jenkinsfile} straight from that file, no
 * file chooser. Shows only on {@code .gdsl} files.
 */
public final class GenerateProjectStubFromFileAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        VirtualFile f = e.getData(CommonDataKeys.VIRTUAL_FILE);
        boolean isGdsl = f != null && !f.isDirectory() && "gdsl".equalsIgnoreCase(f.getExtension());
        e.getPresentation().setEnabledAndVisible(isGdsl && e.getProject() != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VirtualFile f = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (project == null || f == null) return;
        GenerateProjectStubAction.generate(project, f, this);
    }
}
