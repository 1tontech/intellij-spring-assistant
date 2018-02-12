package in.oneton.idea.spring.assistant.plugin.suggestion.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;

/**
 * Ensures the values of {@link YAMLKeyValue}s conform to the data type of the corresponding schema element.
 */
public class DataTypeCheckerAnnotator implements Annotator {

  // TODO: implement checking for the scalar types
  //    private static final Pattern NULL_REGEX = Pattern.compile("null|Null|NULL|~");
  //    private static final Pattern BOOL_REGEX = Pattern.compile("true|True|TRUE|false|False|FALSE");
  //    private static final Pattern INT_REGEX = Pattern.compile("[-+]?[0-9]+|0o[0-7]+|0x[0-9a-fA-F]+");
  //    private static final Pattern FLOAT_REGEX = Pattern.compile("[-+]?(\\.[0-9]+|[0-9]+(\\.[0-9]*)?)([eE][-+]?[0-9]+)?|[-+]?(\\.inf|\\.Inf|\\.INF)|\\.nan|\\.NaN|\\.NAN");

  @Override
  public void annotate(@NotNull final PsiElement element,
      @NotNull final AnnotationHolder annotationHolder) {
    // TODO: Complete this
    //    final ModelProvider modelProvider = ModelProvider.INSTANCE;
    //    final ResourceTypeKey resourceKey = KubernetesYamlPsiUtil.findResourceKey(element);
    //    if (resourceKey != null && element instanceof YAMLKeyValue) {
    //      final YAMLKeyValue keyValue = (YAMLKeyValue) element;
    //      final Property property =
    //          KubernetesYamlPsiUtil.propertyForKey(modelProvider, resourceKey, keyValue);
    //      final YAMLValue value = keyValue.getValue();
    //      if (property != null && property.getSuggestionNodeType() != null && value != null) {
    //        switch (property.getSuggestionNodeType()) {
    //          case ARRAY:
    //            if (!(value instanceof YAMLSequence)) {
    //              annotationHolder.createErrorAnnotation(value,
    //                  "The content of " + keyValue.getKeyText() + " should be an ARRAY.");
    //            }
    //            break;
    //          case OBJECT:
    //            if (!(value instanceof YAMLMapping)) {
    //              annotationHolder.createErrorAnnotation(value,
    //                  "The content of " + keyValue.getKeyText() + " should be an OBJECT.");
    //            }
    //            break;
    //        }
    //      }
    //    }
  }

}
