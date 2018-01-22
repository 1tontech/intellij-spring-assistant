package in.oneton.idea.spring.assistant.plugin.model;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Set;

public interface SuggestionNode {

  static String sanitize(String name) {
    return name.trim().replaceAll("_", "").replace("-", "").toLowerCase();
  }

  /**
   * @return Documentation for the current node as key. Usually includes available choices for values (if applicable)
   */
  @NotNull
  String getDocumentationForKey();

  /**
   * Find all applicable suggestions for the given search test
   *
   * @param prefix prefix to find matches for
   * @return suggestions that contain the given search text
   */
  @Nullable
  Set<Suggestion> findSuggestionsForValue(String prefix);

  /**
   * @param value value selected/typed by user for the current node used as key
   * @return Documentation for selected value with current node as key
   */
  @NotNull
  String getDocumentationForValue(String value);

  /**
   * @return whether the node is a leaf or not
   */
  boolean isLeaf();

  /**
   * @return type of node
   */
  SuggestionNodeType getType();

}
