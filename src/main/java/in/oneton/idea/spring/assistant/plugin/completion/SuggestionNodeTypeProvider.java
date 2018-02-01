package in.oneton.idea.spring.assistant.plugin.completion;

import com.intellij.openapi.module.Module;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType;
import org.jetbrains.annotations.NotNull;

public interface SuggestionNodeTypeProvider {
  /**
   * @param module module to which this node belongs
   * @return type of node
   */
  @NotNull
  SuggestionNodeType getSuggestionNodeType(Module module);
}
