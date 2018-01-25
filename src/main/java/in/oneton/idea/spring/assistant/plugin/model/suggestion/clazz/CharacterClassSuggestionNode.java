package in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz;

import com.intellij.psi.PsiClass;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType;
import org.jetbrains.annotations.NotNull;

import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType.CHARACTER;

public class CharacterClassSuggestionNode extends ClassSuggestionNode {

  public CharacterClassSuggestionNode(PsiClass target) {
    super(target);
  }

  @NotNull
  @Override
  public SuggestionNodeType getType() {
    return CHARACTER;
  }
}
