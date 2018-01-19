package in.oneton.idea.spring.assistant.plugin.model;

import org.jetbrains.annotations.Contract;

import javax.annotation.Nullable;
import java.util.Set;

public interface SuggestionNode {

  static String sanitize(String name) {
    return name.replaceAll("_", "").replace("-", "").toLowerCase();
  }

  @Contract("_, _, true -> null; _, _, false -> !null")
  SuggestionNode findDeepestMatch(String[] pathSegments, int startWith, boolean matchAllSegments);

  @Nullable
  Set<Suggestion> findSuggestions(String[] querySegments, int suggestionDepth, int startWith,
      boolean forceSearchAcrossTree);

  @Nullable
  Set<Suggestion> findChildSuggestions(String[] querySegments, int suggestionDepth, int startWith,
      boolean forceSearchAcrossTree);

  String getFullPath();

  boolean isGroup();

  boolean isLeaf();

  int getDepth();

  Set<Suggestion> getSuggestionValues();

  String getSuggestionReplacement(String existingIndentation, String indentPerLevel, int maxDepth);

  String getNewOverallIndent(String existingIndentation, String indentPerLevel, int maxDepth);

}
