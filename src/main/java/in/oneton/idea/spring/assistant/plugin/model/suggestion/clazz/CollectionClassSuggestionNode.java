package in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz;

import com.intellij.psi.PsiClass;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType.COLLECTION;

public class CollectionClassSuggestionNode extends ClassSuggestionNode {

  public CollectionClassSuggestionNode(@Nullable PsiClass target) {
    super(target);
  }

  @NotNull
  @Override
  public SuggestionNodeType getType() {
    return COLLECTION;
  }
}
