// This is a generated file. Not intended for manual editing.
package io.genai.jenkins.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static io.genai.jenkins.psi.JenkinsfileTypes.*;
import io.genai.jenkins.psi.JfStringLiteralMixin;
import io.genai.jenkins.psi.*;

public class JfStringLiteralImpl extends JfStringLiteralMixin implements JfStringLiteral {

  public JfStringLiteralImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull JfVisitor visitor) {
    visitor.visitStringLiteral(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof JfVisitor) accept((JfVisitor)visitor);
    else super.accept(visitor);
  }

}
