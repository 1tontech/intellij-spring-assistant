package in.oneton.idea.spring.assistant.plugin;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.openapi.module.Module;
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
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.intellij.codeInsight.completion.CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED;
import static com.intellij.openapi.util.text.StringUtil.containsChar;
import static com.intellij.openapi.util.text.StringUtil.endsWithChar;
import static com.intellij.openapi.util.text.StringUtil.isNotEmpty;
import static com.intellij.openapi.util.text.StringUtil.replace;
import static java.text.BreakIterator.getSentenceInstance;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

@UtilityClass
public class Util {
  public static final String PERIOD_DELIMITER = "\\.";
  private static Pattern methodToFragmentConverter = Pattern.compile("(.+)\\.(.+)\\(.*\\)");

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
    return methodToFragmentConverter.matcher(typeForDocumentationNavigation(typeAndMethod))
        .replaceAll("$1#$2");
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

  public static String moduleNamesAsStrCommaDelimited(List<Module> newModules,
      boolean includeProjectName) {
    return moduleNamesAsStrCommaDelimited(newModules.stream(), includeProjectName);
  }

  public static String moduleNamesAsStrCommaDelimited(Module[] newModules,
      boolean includeProjectName) {
    return moduleNamesAsStrCommaDelimited(stream(newModules), includeProjectName);
  }

  private static String moduleNamesAsStrCommaDelimited(Stream<Module> moduleStream,
      boolean includeProjectName) {
    return moduleStream.map(module -> includeProjectName ?
        module.getProject().getName() + ":" + module.getName() :
        module.getName()).collect(joining(", "));
  }

  public static String truncateIdeaDummyIdentifier(@NotNull PsiElement element) {
    return truncateIdeaDummyIdentifier(element.getText());
  }

  public static String truncateIdeaDummyIdentifier(String text) {
    return text.replace(DUMMY_IDENTIFIER_TRIMMED, "");
  }

}
