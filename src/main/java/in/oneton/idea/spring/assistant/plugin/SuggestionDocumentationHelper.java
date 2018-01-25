package in.oneton.idea.spring.assistant.plugin;

import in.oneton.idea.spring.assistant.plugin.model.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface SuggestionDocumentationHelper {
  @NotNull
  String getOriginalName();

  @NotNull
  Suggestion buildSuggestion(String ancestralKeysDotDelimited,
      List<SuggestionNode> matchesRootTillParentNode, SuggestionNode currentNode);

  @NotNull
  String getDocumentationForKey(String nodeNavigationPathDotDelimited);
}
