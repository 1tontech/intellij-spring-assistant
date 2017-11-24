package in.oneton.idea.spring.boot.config.autosuggest.plugin;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLSequenceItem;
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl;

import java.text.BreakIterator;
import java.util.Locale;
import java.util.Optional;

import static com.intellij.openapi.util.text.StringUtil.containsChar;
import static com.intellij.openapi.util.text.StringUtil.endsWithChar;
import static com.intellij.openapi.util.text.StringUtil.isNotEmpty;
import static com.intellij.openapi.util.text.StringUtil.replace;
import static java.text.BreakIterator.getSentenceInstance;

@UtilityClass
public class Util {
  public static final String PERIOD_DELIMITER = "\\.";

  public static boolean isValue(final PsiElement psiElement) {
    return !(org.apache.commons.lang.StringUtils.isBlank(psiElement.getText())
        || psiElement instanceof YAMLKeyValue) && Optional.ofNullable(psiElement.getParent())
        .map(PsiElement::getParent).filter(el -> el instanceof YAMLKeyValue)
        .map(YAMLKeyValue.class::cast).filter(el -> el.getValue() == psiElement.getParent())
        .isPresent();
  }

  public static String typeForDocumentationNavigation(String type) {
    return type.replaceAll("\\$", ".");
  }

  public static String methodForDocumentationNavigation(String typeAndMethod) {
    return typeForDocumentationNavigation(typeAndMethod).replaceAll("\\(.+\\)", "");
  }

  public static boolean isArrayStringElement(final PsiElement psiElement) {
    return psiElement.getParent() instanceof YAMLPlainTextImpl && psiElement.getParent()
        .getParent() instanceof YAMLSequenceItem;
  }

  public static Optional<String> getKeyNameIfKey(final PsiElement psiElement) {
    return getAsYamlKeyValue(psiElement).map(YAMLKeyValue::getKeyText);
  }

  @NotNull
  public static String getCodeStyleIntent(InsertionContext insertionContext) {
    final CodeStyleSettings currentSettings =
        CodeStyleSettingsManager.getSettings(insertionContext.getProject());
    final CommonCodeStyleSettings.IndentOptions indentOptions =
        currentSettings.getIndentOptions(insertionContext.getFile().getFileType());
    return indentOptions.USE_TAB_CHARACTER ?
        "\t" :
        StringUtil.repeatSymbol(' ', indentOptions.INDENT_SIZE);
  }

  private static Optional<YAMLKeyValue> getAsYamlKeyValue(final PsiElement psiElement) {
    return Optional.ofNullable(psiElement).map(PsiElement::getParent)
        .filter(el -> el instanceof YAMLKeyValue).map(YAMLKeyValue.class::cast)
        .filter(value -> value.getKey() == psiElement);
  }

  public static Optional<String> getKeyNameOfObject(final PsiElement psiElement) {
    return Optional.of(psiElement).filter(el -> el instanceof YAMLKeyValue)
        .map(YAMLKeyValue.class::cast).map(YAMLKeyValue::getName);
  }

  @NotNull
  public static String getFirstSentenceWithoutDot(String fullSentence) {
    if (containsChar(fullSentence, '.')) {
      BreakIterator breakIterator = getSentenceInstance(Locale.US);
      breakIterator.setText(fullSentence);
      fullSentence = fullSentence.substring(breakIterator.first(), breakIterator.next()).trim();
    }

    if (isNotEmpty(fullSentence)) {
      String withoutDot = endsWithChar(fullSentence, '.') ?
          fullSentence.substring(0, fullSentence.length() - 1) :
          fullSentence;
      return replace(withoutDot, "\n", "");
    } else {
      return "";
    }
  }

}
