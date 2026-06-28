package io.genai.jenkins.psi

import com.intellij.psi.tree.IElementType
import io.genai.jenkins.JenkinsfileLanguage

/**
 * Lexer token element type. [toString] returns a human-friendly name so
 * parser-error messages read like "'{' expected, got '}'" instead of leaking the
 * internal class/debug name.
 */
class JenkinsfileTokenType(debugName: String) :
    IElementType(debugName, JenkinsfileLanguage.INSTANCE) {

    override fun toString(): String = when (val name = super.toString()) {
        "{", "}", "(", ")", "[", "]" -> "'$name'"
        "IDENTIFIER" -> "identifier"
        "NUMBER" -> "number"
        "OPERATOR" -> "operator"
        "LINE_COMMENT", "BLOCK_COMMENT" -> "comment"
        else -> name   // keyword, string
    }
}
