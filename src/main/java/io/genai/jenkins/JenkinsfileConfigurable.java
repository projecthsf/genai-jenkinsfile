package io.genai.jenkins;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.psi.PsiManager;
import com.intellij.ui.components.JBCheckBox;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/** Settings ▸ Languages &amp; Frameworks ▸ Jenkinsfile. */
public final class JenkinsfileConfigurable implements Configurable {

    private JBCheckBox injectShell;

    @Override
    public @Nls String getDisplayName() {
        return JenkinsfileBundle.message("settings.displayName");
    }

    @Override
    public @Nullable JComponent createComponent() {
        injectShell = new JBCheckBox(JenkinsfileBundle.message("settings.injectLanguages"));
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(injectShell, BorderLayout.NORTH);
        return panel;
    }

    @Override
    public boolean isModified() {
        return injectShell.isSelected() != JenkinsfileSettings.injectEmbeddedLanguages();
    }

    @Override
    public void apply() {
        JenkinsfileSettings.setInjectEmbeddedLanguages(injectShell.isSelected());
        // Re-highlight open files so the injection change takes effect immediately.
        for (Project p : ProjectManager.getInstance().getOpenProjects()) {
            PsiManager.getInstance(p).dropPsiCaches();
            DaemonCodeAnalyzer.getInstance(p).restart();
        }
    }

    @Override
    public void reset() {
        injectShell.setSelected(JenkinsfileSettings.injectEmbeddedLanguages());
    }
}
