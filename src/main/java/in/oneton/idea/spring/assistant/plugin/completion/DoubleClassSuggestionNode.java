package in.oneton.idea.spring.assistant.plugin.completion;

import com.intellij.psi.PsiClass;
import in.oneton.idea.spring.assistant.plugin.model.ClassSuggestionNode;
import in.oneton.idea.spring.assistant.plugin.model.SuggestionNodeType;

import static in.oneton.idea.spring.assistant.plugin.model.SuggestionNodeType.DOUBLE;

public class DoubleClassSuggestionNode extends ClassSuggestionNode {

  public DoubleClassSuggestionNode(PsiClass target) {
    super(target);
  }

  @Override
  public SuggestionNodeType getType() {
    return DOUBLE;
  }
}
