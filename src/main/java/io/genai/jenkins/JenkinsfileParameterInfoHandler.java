package io.genai.jenkins;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.lang.parameterInfo.CreateParameterInfoContext;
import com.intellij.lang.parameterInfo.ParameterInfoContext;
import com.intellij.lang.parameterInfo.ParameterInfoHandler;
import com.intellij.lang.parameterInfo.ParameterInfoUIContext;
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Parameter info for step calls like {@code sh(script: '…', returnStdout: true)}.
 * Without a full call PSI, it locates the enclosing {@code (...)} and the step
 * name by scanning the document text, then shows that step's named arguments
 * (from {@link JenkinsfileKnowledge}), bolding the current one.
 */
public final class JenkinsfileParameterInfoHandler
        implements ParameterInfoHandler<PsiElement, List<String>> {

    @Override
    public @Nullable PsiElement findElementForParameterInfo(@NotNull CreateParameterInfoContext context) {
        CharSequence text = context.getEditor().getDocument().getCharsSequence();
        int open = JfCalls.enclosingParen(text, context.getOffset());
        if (open < 0) return null;
        List<String> params = JenkinsfileCatalog.getInstance(context.getProject())
                .stepParams(JfCalls.nameBefore(text, open));
        if (params == null || params.isEmpty()) return null;
        context.setItemsToShow(new Object[]{ params });
        PsiElement el = context.getFile().findElementAt(open);
        return el != null ? el : context.getFile();
    }

    @Override
    public void showParameterInfo(@NotNull PsiElement element, @NotNull CreateParameterInfoContext context) {
        context.showHint(element, element.getTextRange().getStartOffset(), this);
    }

    @Override
    public @Nullable PsiElement findElementForUpdatingParameterInfo(@NotNull UpdateParameterInfoContext context) {
        CharSequence text = context.getEditor().getDocument().getCharsSequence();
        int open = JfCalls.enclosingParen(text, context.getOffset());
        if (open < 0) return null;
        if (JenkinsfileCatalog.getInstance(context.getProject())
                .stepParams(JfCalls.nameBefore(text, open)) == null) return null;
        PsiElement el = context.getFile().findElementAt(open);   // same anchor as the initial lookup
        return el != null ? el : context.getFile();
    }

    @Override
    public void updateParameterInfo(@NotNull PsiElement element, @NotNull UpdateParameterInfoContext context) {
        CharSequence text = context.getEditor().getDocument().getCharsSequence();
        int open = JfCalls.enclosingParen(text, context.getOffset());
        context.setCurrentParameter(open < 0 ? -1 : JfCalls.paramIndex(text, open, context.getOffset()));
    }

    @Override
    public void updateUI(List<String> params, @NotNull ParameterInfoUIContext context) {
        StringBuilder sb = new StringBuilder();
        int cur = context.getCurrentParameterIndex();
        int hlStart = -1, hlEnd = -1;
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) sb.append(", ");
            int start = sb.length();
            sb.append(params.get(i));
            if (i == cur) { hlStart = start; hlEnd = sb.length(); }
        }
        context.setupUIComponentPresentation(sb.toString(), hlStart, hlEnd,
                false, false, false, context.getDefaultParameterColor());
    }

    @Override public boolean couldShowInLookup() { return false; }
    @Override public Object @Nullable [] getParametersForLookup(LookupElement item, ParameterInfoContext context) { return null; }

}
