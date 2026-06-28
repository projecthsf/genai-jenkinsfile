package io.genai.jenkins

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE = "messages.JenkinsfileBundle"

/**
 * Single entry point for all user-facing text. Add new strings to
 * `messages/JenkinsfileBundle.properties` (and locale variants like
 * `JenkinsfileBundle_fr.properties`) and look them up via [message].
 */
object JenkinsfileBundle : DynamicBundle(BUNDLE) {

    @JvmStatic
    @Nls
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String =
        getMessage(key, *params)
}
