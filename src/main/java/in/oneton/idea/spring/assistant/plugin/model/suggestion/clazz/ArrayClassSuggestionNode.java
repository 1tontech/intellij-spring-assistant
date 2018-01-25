package in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz;

import com.intellij.psi.PsiClass;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType;
import org.jetbrains.annotations.NotNull;

import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType.ARRAY;

public class ArrayClassSuggestionNode extends ClassSuggestionNode {

  public ArrayClassSuggestionNode(PsiClass target) {
    super(target);
  }

  @Override
  public boolean isLeaf() {
    return false;
  }

  @NotNull
  @Override
  public SuggestionNodeType getType() {
    return ARRAY;
  }
}
