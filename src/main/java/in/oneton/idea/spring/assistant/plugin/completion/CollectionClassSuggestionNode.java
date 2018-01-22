package in.oneton.idea.spring.assistant.plugin.completion;

import com.intellij.psi.PsiClass;
import in.oneton.idea.spring.assistant.plugin.model.ClassSuggestionNode;
import in.oneton.idea.spring.assistant.plugin.model.SuggestionNodeType;

import javax.annotation.Nullable;

import static in.oneton.idea.spring.assistant.plugin.model.SuggestionNodeType.COLLECTION;

public class CollectionClassSuggestionNode extends ClassSuggestionNode {

  public CollectionClassSuggestionNode(@Nullable PsiClass target) {
    super(target);
  }

  @Override
  public SuggestionNodeType getType() {
    return COLLECTION;
  }
}
