package in.oneton.idea.spring.assistant.plugin.completion;

import in.oneton.idea.spring.assistant.plugin.model.Suggestion;

import java.util.Collection;

public interface KeySuggestionFinder {
  Collection<Suggestion> findValue(String[] querySegments, int suggestionDepth, int startWith,
      boolean forceSearchAcrossTree);
}
