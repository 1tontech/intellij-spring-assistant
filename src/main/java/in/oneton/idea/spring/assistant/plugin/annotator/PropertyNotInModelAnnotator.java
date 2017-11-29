package in.oneton.idea.spring.assistant.plugin.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;

/**
 * Finds {@link YAMLKeyValue}s with {@link YAMLMapping}s as values, and finds the corresponding schema model. Marks any child properties within the mapping which do not exist in the model as an error.
 */
public class PropertyNotInModelAnnotator implements Annotator {

  @Override
  public void annotate(@NotNull final PsiElement element,
      @NotNull final AnnotationHolder annotationHolder) {
    // TODO: Fix this
    //    final ModelProvider modelProvider = ModelProvider.INSTANCE;
    //    final ResourceTypeKey resourceKey = KubernetesYamlPsiUtil.findResourceKey(element);
    //    if (resourceKey != null && element instanceof YAMLKeyValue) {
    //      final YAMLKeyValue keyValue = (YAMLKeyValue) element;
    //      final Model model = KubernetesYamlPsiUtil.modelForKey(modelProvider, resourceKey, keyValue);
    //      if (keyValue.getValue() instanceof YAMLMapping && model != null) {
    //        final YAMLMapping mapping = (YAMLMapping) keyValue.getValue();
    //        final Set<String> expectedProperties = model.getProperties().keySet();
    //        //noinspection ConstantConditions
    //        mapping.getKeyValues().stream()
    //            .filter(k -> !expectedProperties.contains(k.getKeyText().trim())).forEach(
    //            k -> annotationHolder.createWarningAnnotation(k.getKey(),
    //                "SpringConfigurationMetadataProperty '" + k.getKeyText()
    //                    + "' is not expected here.").registerFix(new DeletePropertyIntentionAction()));
    //      }
    //    }
  }
}
