package io.genai.jenkins

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import io.genai.jenkins.lexer.JenkinsfileFlexLexer
import io.genai.jenkins.parser.JenkinsfileParser
import io.genai.jenkins.psi.JenkinsfileTypes

class JenkinsfileParserDefinition : ParserDefinition {

    override fun createLexer(project: Project?): Lexer = FlexAdapter(JenkinsfileFlexLexer(null))
    override fun createParser(project: Project?): PsiParser = JenkinsfileParser()
    override fun getFileNodeType(): IFileElementType = FILE
    override fun getCommentTokens(): TokenSet = COMMENTS
    override fun getStringLiteralElements(): TokenSet = STRINGS
    override fun getWhitespaceTokens(): TokenSet = WHITESPACE
    override fun createElement(node: ASTNode): PsiElement = JenkinsfileTypes.Factory.createElement(node)
    override fun createFile(viewProvider: FileViewProvider): PsiFile = JenkinsfileFile(viewProvider)

    companion object {
        val FILE = IFileElementType(JenkinsfileLanguage.INSTANCE)
        private val COMMENTS = TokenSet.create(JenkinsfileTypes.LINE_COMMENT, JenkinsfileTypes.BLOCK_COMMENT)
        private val STRINGS = TokenSet.create(JenkinsfileTypes.STRING)
        private val WHITESPACE = TokenSet.create(TokenType.WHITE_SPACE)
    }
}
