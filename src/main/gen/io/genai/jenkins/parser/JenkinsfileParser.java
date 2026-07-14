// This is a generated file. Not intended for manual editing.
package io.genai.jenkins.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static io.genai.jenkins.psi.JenkinsfileTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class JenkinsfileParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType root_, PsiBuilder builder_) {
    parseLight(root_, builder_);
    return builder_.getTreeBuilt();
  }

  public void parseLight(IElementType root_, PsiBuilder builder_) {
    boolean result_;
    builder_ = adapt_builder_(root_, builder_, this, null);
    Marker marker_ = enter_section_(builder_, 0, _COLLAPSE_, null);
    result_ = parse_root_(root_, builder_);
    exit_section_(builder_, 0, marker_, root_, result_, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType root_, PsiBuilder builder_) {
    return parse_root_(root_, builder_, 0);
  }

  static boolean parse_root_(IElementType root_, PsiBuilder builder_, int level_) {
    return jenkinsfile(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // LPAREN (atom | args) * RPAREN
  public static boolean args(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "args")) return false;
    if (!nextTokenIs(builder_, LPAREN)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, LPAREN);
    result_ = result_ && args_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, RPAREN);
    exit_section_(builder_, marker_, ARGS, result_);
    return result_;
  }

  // (atom | args) *
  private static boolean args_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "args_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!args_1_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "args_1", pos_)) break;
    }
    return true;
  }

  // atom | args
  private static boolean args_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "args_1_0")) return false;
    boolean result_;
    result_ = atom(builder_, level_ + 1);
    if (!result_) result_ = args(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // string_literal | NUMBER | KEYWORD | IDENTIFIER | OPERATOR | LBRACK | RBRACK
  static boolean atom(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "atom")) return false;
    boolean result_;
    result_ = string_literal(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, NUMBER);
    if (!result_) result_ = consumeToken(builder_, KEYWORD);
    if (!result_) result_ = consumeToken(builder_, IDENTIFIER);
    if (!result_) result_ = consumeToken(builder_, OPERATOR);
    if (!result_) result_ = consumeToken(builder_, LBRACK);
    if (!result_) result_ = consumeToken(builder_, RBRACK);
    return result_;
  }

  /* ********************************************************** */
  // (KEYWORD | IDENTIFIER) args? LBRACE item * RBRACE
  public static boolean block(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "block")) return false;
    if (!nextTokenIs(builder_, "<block>", IDENTIFIER, KEYWORD)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, BLOCK, "<block>");
    result_ = block_0(builder_, level_ + 1);
    result_ = result_ && block_1(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, LBRACE);
    pinned_ = result_; // pin = 3
    result_ = result_ && report_error_(builder_, block_3(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, RBRACE) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // KEYWORD | IDENTIFIER
  private static boolean block_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "block_0")) return false;
    boolean result_;
    result_ = consumeToken(builder_, KEYWORD);
    if (!result_) result_ = consumeToken(builder_, IDENTIFIER);
    return result_;
  }

  // args?
  private static boolean block_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "block_1")) return false;
    args(builder_, level_ + 1);
    return true;
  }

  // item *
  private static boolean block_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "block_3")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!item(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "block_3", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // block | atom | args
  static boolean item(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "item")) return false;
    boolean result_;
    result_ = block(builder_, level_ + 1);
    if (!result_) result_ = atom(builder_, level_ + 1);
    if (!result_) result_ = args(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // item *
  static boolean jenkinsfile(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "jenkinsfile")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!item(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "jenkinsfile", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // STRING
  public static boolean string_literal(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "string_literal")) return false;
    if (!nextTokenIs(builder_, STRING)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, STRING);
    exit_section_(builder_, marker_, STRING_LITERAL, result_);
    return result_;
  }

}
