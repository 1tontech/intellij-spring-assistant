package in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz;

import com.intellij.psi.PsiClass;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType;
import org.jetbrains.annotations.NotNull;

import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType.ENUM;

public class EnumClassSuggestionNode extends ClassSuggestionNode {

  public EnumClassSuggestionNode(PsiClass target) {
    super(target);
  }

  @NotNull
  @Override
  public SuggestionNodeType getType() {
    return ENUM;
  }
}
