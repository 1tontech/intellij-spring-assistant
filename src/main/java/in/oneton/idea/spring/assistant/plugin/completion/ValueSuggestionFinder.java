package in.oneton.idea.spring.assistant.plugin.completion;

import in.oneton.idea.spring.assistant.plugin.model.Suggestion;
import in.oneton.idea.spring.assistant.plugin.model.SuggestionNode;

import java.util.Collection;

public interface ValueSuggestionFinder {
  Collection<Suggestion> findValue(SuggestionNode leaf);
}
