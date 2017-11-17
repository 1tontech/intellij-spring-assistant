package in.oneton.idea.spring.boot.config.autosuggest.plugin;

import com.intellij.psi.PsiElement;
import lombok.experimental.UtilityClass;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLSequenceItem;
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl;

import java.util.Optional;

@UtilityClass
public class Util {
  public static boolean isValue(final PsiElement psiElement) {
    return !(org.apache.commons.lang.StringUtils.isBlank(psiElement.getText())
        || psiElement instanceof YAMLKeyValue) && Optional.ofNullable(psiElement.getParent())
        .map(PsiElement::getParent).filter(el -> el instanceof YAMLKeyValue)
        .map(YAMLKeyValue.class::cast).filter(el -> el.getValue() == psiElement.getParent())
        .isPresent();
  }

  public static boolean isArrayStringElement(final PsiElement psiElement) {
    return psiElement.getParent() instanceof YAMLPlainTextImpl && psiElement.getParent()
        .getParent() instanceof YAMLSequenceItem;
  }

  public static Optional<String> getKeyNameIfKey(final PsiElement psiElement) {
    return getAsYamlKeyValue(psiElement).map(YAMLKeyValue::getKeyText);
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
}
