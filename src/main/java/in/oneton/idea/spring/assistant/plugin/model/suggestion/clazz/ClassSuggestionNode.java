package in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz;

import com.intellij.openapi.module.Module;
import in.oneton.idea.spring.assistant.plugin.completion.FileType;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ClassSuggestionNode extends SuggestionNode {
  @NotNull
  Suggestion buildSuggestionForKey(Module module, FileType fileType,
      List<SuggestionNode> matchesRootTillMe, int numOfAncestors);
}
