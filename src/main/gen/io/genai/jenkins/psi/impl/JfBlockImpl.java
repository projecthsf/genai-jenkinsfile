// This is a generated file. Not intended for manual editing.
package io.genai.jenkins.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static io.genai.jenkins.psi.JenkinsfileTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import io.genai.jenkins.psi.*;

public class JfBlockImpl extends ASTWrapperPsiElement implements JfBlock {

  public JfBlockImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull JfVisitor visitor) {
    visitor.visitBlock(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof JfVisitor) accept((JfVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<JfArgs> getArgsList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, JfArgs.class);
  }

  @Override
  @NotNull
  public List<JfBlock> getBlockList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, JfBlock.class);
  }

  @Override
  @NotNull
  public List<JfStringLiteral> getStringLiteralList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, JfStringLiteral.class);
  }

}
