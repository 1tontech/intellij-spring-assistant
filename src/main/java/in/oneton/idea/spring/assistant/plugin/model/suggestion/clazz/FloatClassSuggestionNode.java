package in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz;

import com.intellij.psi.PsiClass;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType;
import org.jetbrains.annotations.NotNull;

public class FloatClassSuggestionNode extends ClassSuggestionNode {

  public FloatClassSuggestionNode(PsiClass target) {
    super(target);
  }

  @NotNull
  @Override
  public SuggestionNodeType getType() {
    return SuggestionNodeType.FLOAT;
  }
}
