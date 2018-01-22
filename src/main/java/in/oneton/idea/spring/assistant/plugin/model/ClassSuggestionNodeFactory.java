package in.oneton.idea.spring.assistant.plugin.model;

import com.intellij.psi.PsiClass;
import in.oneton.idea.spring.assistant.plugin.completion.ArrayClassSuggestionNode;
import in.oneton.idea.spring.assistant.plugin.completion.BooleanClassSuggestionNode;
import in.oneton.idea.spring.assistant.plugin.completion.ByteClassSuggestionNode;
import in.oneton.idea.spring.assistant.plugin.completion.CharacterClassSuggestionNode;
import in.oneton.idea.spring.assistant.plugin.completion.CollectionClassSuggestionNode;
import in.oneton.idea.spring.assistant.plugin.completion.DoubleClassSuggestionNode;
import in.oneton.idea.spring.assistant.plugin.completion.EnumClassSuggestionNode;
import in.oneton.idea.spring.assistant.plugin.completion.FloatClassSuggestionNode;
import in.oneton.idea.spring.assistant.plugin.completion.GenericClassSuggestionNode;
import in.oneton.idea.spring.assistant.plugin.completion.IntegerClassSuggestionNode;
import in.oneton.idea.spring.assistant.plugin.completion.LongClassSuggestionNode;
import in.oneton.idea.spring.assistant.plugin.completion.MapClassSuggestionNode;
import in.oneton.idea.spring.assistant.plugin.completion.ShortClassSuggestionNode;
import in.oneton.idea.spring.assistant.plugin.completion.StringClassSuggestionNode;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import static in.oneton.idea.spring.assistant.plugin.PsiUtil.findType;

@UtilityClass
public final class ClassSuggestionNodeFactory {
  public static ClassSuggestionNode newInstance(@NotNull PsiClass psiClass) {
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
