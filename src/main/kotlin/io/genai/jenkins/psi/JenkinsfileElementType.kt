package io.genai.jenkins.psi

import com.intellij.psi.tree.IElementType
import io.genai.jenkins.JenkinsfileLanguage

/** Composite (rule) element type produced by the generated parser. */
class JenkinsfileElementType(debugName: String) :
    IElementType(debugName, JenkinsfileLanguage.INSTANCE)
