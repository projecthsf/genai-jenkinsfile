package io.genai.jenkins;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import io.genai.jenkins.psi.JenkinsfileTypes;
import io.genai.jenkins.psi.JfBlock;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A node in the Structure view, backed by the real PSI: the root is the file and
 * each {@link JfBlock} ({@code name(args) { … }}) is a node whose children are the
 * blocks nested inside it — so you get the true pipeline → stages → stage → steps
 * outline, kept accurate as you edit.
 */
public final class JenkinsfileStructureViewElement implements StructureViewTreeElement, SortableTreeElement {

    private static final TokenSet NAME = TokenSet.create(JenkinsfileTypes.KEYWORD, JenkinsfileTypes.IDENTIFIER);

    private final PsiElement element;   // PsiFile (root) or JfBlock
    private final String label;

    public JenkinsfileStructureViewElement(@NotNull PsiFile file) {
        this.element = file;
        this.label = file.getName();
    }

    private JenkinsfileStructureViewElement(@NotNull JfBlock block) {
        this.element = block;
        this.label = labelOf(block);
    }

    @Override public Object getValue() { return element; }
    @Override public @NotNull String getAlphaSortKey() { return label; }

    @Override
    public @NotNull ItemPresentation getPresentation() {
        return new PresentationData(label, null,
                element instanceof PsiFile ? JenkinsfileFileType.ICON : AllIcons.Nodes.Method, null);
    }

    @Override
    public TreeElement @NotNull [] getChildren() {
        Collection<JfBlock> blocks = PsiTreeUtil.getChildrenOfTypeAsList(element, JfBlock.class);
        List<TreeElement> kids = new ArrayList<>(blocks.size());
        for (JfBlock b : blocks) kids.add(new JenkinsfileStructureViewElement(b));
        return kids.toArray(TreeElement.EMPTY_ARRAY);
    }

    /** Block label: its name plus the first string arg, e.g. {@code stage 'Build'}. */
    private static String labelOf(JfBlock block) {
        ASTNode node = block.getNode();
        ASTNode nameNode = node.findChildByType(NAME);
        String name = nameNode != null ? nameNode.getText() : "block";
        String s = JfPsi.firstStringText(node.findChildByType(JenkinsfileTypes.ARGS));
        return (s != null && !s.isEmpty()) ? name + " " + s : name;
    }
}
