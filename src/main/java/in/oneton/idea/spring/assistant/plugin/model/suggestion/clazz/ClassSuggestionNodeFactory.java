package in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz;

import com.intellij.psi.PsiClass;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import static in.oneton.idea.spring.assistant.plugin.ClassUtil.findType;

@UtilityClass
final class ClassSuggestionNodeFactory {
  static ClassSuggestionNode newInstance(@NotNull PsiClass psiClass) {
    SuggestionNodeType type = findType(psiClass);
    switch (type) {
      case BOOLEAN:
        return new BooleanClassSuggestionNode(psiClass);
      case BYTE:
        return new ByteClassSuggestionNode(psiClass);
      case SHORT:
        return new ShortClassSuggestionNode(psiClass);
      case INTEGER:
        return new IntegerClassSuggestionNode(psiClass);
      case LONG:
        return new LongClassSuggestionNode(psiClass);
      case FLOAT:
        return new FloatClassSuggestionNode(psiClass);
      case DOUBLE:
        return new DoubleClassSuggestionNode(psiClass);
      case CHARACTER:
        return new CharacterClassSuggestionNode(psiClass);
      case STRING:
        return new StringClassSuggestionNode(psiClass);
      case ENUM:
        return new EnumClassSuggestionNode(psiClass);
      case ARRAY:
        return new ArrayClassSuggestionNode(psiClass);
      case COLLECTION:
        return new CollectionClassSuggestionNode(psiClass);
      case MAP:
        return new MapClassSuggestionNode(psiClass);
      case KNOWN_CLASS:
        return new GenericClassSuggestionNode(psiClass, true);
      case UNKNOWN_CLASS:
        return new GenericClassSuggestionNode(psiClass, false);
      default:
        throw new IllegalStateException(
            "Class suggestion node for the specified class " + psiClass + " is undefined");
    }
  }
}
