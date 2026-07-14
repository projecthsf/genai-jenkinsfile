// This is a generated file. Not intended for manual editing.
package io.genai.jenkins.psi;

import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;

public class JfVisitor extends PsiElementVisitor {

  public void visitArgs(@NotNull JfArgs o) {
    visitPsiElement(o);
  }

  public void visitBlock(@NotNull JfBlock o) {
    visitPsiElement(o);
  }

  public void visitStringLiteral(@NotNull JfStringLiteral o) {
    visitPsiLanguageInjectionHost(o);
  }

  public void visitPsiLanguageInjectionHost(@NotNull PsiLanguageInjectionHost o) {
    visitElement(o);
  }

  public void visitPsiElement(@NotNull PsiElement o) {
    visitElement(o);
  }

}
