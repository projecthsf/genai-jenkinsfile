package io.genai.jenkins

import com.intellij.codeInsight.hints.declarative.InlayHintsCollector
import com.intellij.codeInsight.hints.declarative.InlayHintsProvider
import com.intellij.codeInsight.hints.declarative.InlayTreeSink
import com.intellij.codeInsight.hints.declarative.InlineInlayPosition
import com.intellij.codeInsight.hints.declarative.SharedBypassCollector
import com.intellij.lang.ASTNode
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.TokenSet
import io.genai.jenkins.psi.JenkinsfileTypes

/**
 * Shows a block's label after its closing brace — e.g. `}  stage 'Build'` — for
 * blocks that span multiple lines, so you can tell what a distant `}` closes.
 */
class JenkinsfileInlayProvider : InlayHintsProvider {

    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector =
        Collector(editor)

    private class Collector(private val editor: Editor) : SharedBypassCollector {

        private val nameTokens = TokenSet.create(JenkinsfileTypes.KEYWORD, JenkinsfileTypes.IDENTIFIER)

        override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
            val node = element.node ?: return
            if (node.elementType !== JenkinsfileTypes.BLOCK) return

            val open = node.findChildByType(JenkinsfileTypes.LBRACE) ?: return
            val close = node.findChildByType(JenkinsfileTypes.RBRACE) ?: return

            val doc = editor.document
            if (doc.getLineNumber(open.startOffset) == doc.getLineNumber(close.startOffset)) return  // single line

            val label = labelOf(node) ?: return
            sink.addPresentation(
                InlineInlayPosition(close.textRange.endOffset, relatedToPrevious = true),
                hasBackground = false,
            ) {
                text("  $label")
            }
        }

        private fun labelOf(node: ASTNode): String? {
            val name = node.findChildByType(nameTokens)?.text ?: return null
            val arg = node.findChildByType(JenkinsfileTypes.ARGS)
                ?.findChildByType(JenkinsfileTypes.STRING_LITERAL)
                ?.text
                ?.trim('\'', '"', ' ')
            return if (!arg.isNullOrEmpty()) "$name $arg" else name
        }
    }
}
