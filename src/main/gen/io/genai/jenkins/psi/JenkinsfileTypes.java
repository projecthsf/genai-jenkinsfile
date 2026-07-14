// This is a generated file. Not intended for manual editing.
package io.genai.jenkins.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import io.genai.jenkins.psi.impl.*;

public interface JenkinsfileTypes {

  IElementType ARGS = new JenkinsfileElementType("ARGS");
  IElementType BLOCK = new JenkinsfileElementType("BLOCK");
  IElementType STRING_LITERAL = new JenkinsfileElementType("STRING_LITERAL");

  IElementType BLOCK_COMMENT = new JenkinsfileTokenType("BLOCK_COMMENT");
  IElementType IDENTIFIER = new JenkinsfileTokenType("IDENTIFIER");
  IElementType KEYWORD = new JenkinsfileTokenType("keyword");
  IElementType LBRACE = new JenkinsfileTokenType("{");
  IElementType LBRACK = new JenkinsfileTokenType("[");
  IElementType LINE_COMMENT = new JenkinsfileTokenType("LINE_COMMENT");
  IElementType LPAREN = new JenkinsfileTokenType("(");
  IElementType NUMBER = new JenkinsfileTokenType("NUMBER");
  IElementType OPERATOR = new JenkinsfileTokenType("OPERATOR");
  IElementType RBRACE = new JenkinsfileTokenType("}");
  IElementType RBRACK = new JenkinsfileTokenType("]");
  IElementType RPAREN = new JenkinsfileTokenType(")");
  IElementType STRING = new JenkinsfileTokenType("string");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == ARGS) {
        return new JfArgsImpl(node);
      }
      else if (type == BLOCK) {
        return new JfBlockImpl(node);
      }
      else if (type == STRING_LITERAL) {
        return new JfStringLiteralImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
