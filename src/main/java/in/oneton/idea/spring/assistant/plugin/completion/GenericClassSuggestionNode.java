package in.oneton.idea.spring.assistant.plugin.completion;

import com.intellij.psi.PsiClass;
import in.oneton.idea.spring.assistant.plugin.model.ClassSuggestionNode;
import in.oneton.idea.spring.assistant.plugin.model.SuggestionNodeType;

import static in.oneton.idea.spring.assistant.plugin.model.SuggestionNodeType.KNOWN_CLASS;
import static in.oneton.idea.spring.assistant.plugin.model.SuggestionNodeType.UNKNOWN_CLASS;

public class GenericClassSuggestionNode extends ClassSuggestionNode {

  private final boolean knownAtIndexTime;

  public GenericClassSuggestionNode(PsiClass target, boolean knownAtIndexTime) {
    super(target);
    this.knownAtIndexTime = knownAtIndexTime;
  }

  @Override
  public SuggestionNodeType getType() {
    return knownAtIndexTime ? KNOWN_CLASS : UNKNOWN_CLASS;
  }
}
