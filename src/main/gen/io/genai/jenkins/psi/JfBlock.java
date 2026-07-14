// This is a generated file. Not intended for manual editing.
package io.genai.jenkins.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface JfBlock extends PsiElement {

  @NotNull
  List<JfArgs> getArgsList();

  @NotNull
  List<JfBlock> getBlockList();

  @NotNull
  List<JfStringLiteral> getStringLiteralList();

}
