package in.oneton.idea.spring.boot.config.autosuggest.plugin.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLMapping;

/**
 * Finds {@link YAMLMapping}s with duplicated keys.
 */
public class DuplicateKeyAnnotator implements Annotator {

  @Override
  public void annotate(@NotNull final PsiElement element,
      @NotNull final AnnotationHolder annotationHolder) {
    if (element instanceof YAMLMapping) {
      // TODO: Fix
      //      final YAMLMapping mapping = (YAMLMapping) element;
      //      final Collection<YAMLKeyValue> keyValues = mapping.getKeyValues();
      //      final Set<String> existingKeys = new HashSet<>(keyValues.size());
      //      for (final YAMLKeyValue keyValue : keyValues) {
      //        if (keyValue.getKey() != null && !existingKeys.add(keyValue.getKeyText().trim())) {
      //          annotationHolder.createErrorAnnotation(keyValue.getKey(),
      //              "Duplicated PROPERTY '" + keyValue.getKeyText() + "'")
      //              .registerFix(new DeletePropertyIntentionAction());
      //        }
      //      }
    }
  }
}
