package in.oneton.idea.spring.assistant.plugin.model.suggestion;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.SortedSet;

public interface SuggestionNode {

  static String sanitise(String name) {
    return name.trim().replaceAll("_", "").replace("-", "").toLowerCase();
  }

  /**
   * All {@code pathSegments} starting from {@code pathSegmentStartIndex} will be attempted to be matched. If a result is found, it will be returned. Else null
   *
   * @param pathSegments          path segments to match against
   * @param pathSegmentStartIndex index within {@code pathSegments} to start match from
   * @return leaf suggestion node that matches path segments starting with {@code pathSegmentStartIndex} or null otherwise
   */
  SuggestionNode findDeepestMatch(String[] pathSegments, int pathSegmentStartIndex);

  // TODO: May be we need to remove previous method as we are creating just one list per call

  /**
   * If {@code matchAllSegments} is true, all {@code pathSegments} starting from {@code pathSegmentStartIndex} will be attempted to be matched. If a result is found, it will be returned. Else null
   * Else, method should attempt to match as deep as it can match & return bottom most match
   * <p>
   * <b>NOTE:</b> Though this method does every thing the other `findDeepestMatch` does, the reason for the existence the other method is for performance reasons
   *
   * @param matchesRootTillParentNode matches till parent node
   * @param pathSegments              path segments to match against
   * @param pathSegmentStartIndex     index within {@code pathSegments} to start match from
   * @return leaf suggestion node that matches path segments starting with {@code pathSegmentStartIndex} or null otherwise
   */
  @Nullable
  List<SuggestionNode> findDeepestMatch(List<SuggestionNode> matchesRootTillParentNode,
      String[] pathSegments, int pathSegmentStartIndex);

  /**
   * @param ancestralKeysDotDelimited    all ancestral keys dot delimited, required for showing full path in documentation
   * @param matchesRootTillParentNode    path from root till parent node
   * @param querySegmentPrefixes         the search text parts split based on period delimiter
   * @param querySegmentPrefixStartIndex current index in the `querySegmentPrefixes` to start search from
   * @return Suggestions matching the given querySegmentPrefixes criteria from within the children
   */
  @Nullable
  SortedSet<Suggestion> findSuggestionsForKey(@Nullable String ancestralKeysDotDelimited,
      List<SuggestionNode> matchesRootTillParentNode, String[] querySegmentPrefixes,
      int querySegmentPrefixStartIndex);

  /**
   * @return false if an intermediate node (neither group, nor property, nor class). true otherwise
   */
  boolean supportsDocumentation();

  String getOriginalName();

  @Nullable
  String getNameForDocumentation();

  /**
   * @param nodeNavigationPathDotDelimited node path
   * @return Documentation for key under cursor. Includes available choices for values (if applicable). Null if documentation is not available
   */
  @Nullable
  String getDocumentationForKey(String nodeNavigationPathDotDelimited);

  /**
   * Find all applicable suggestions for the given search text
   *
   * @param prefix prefix to find matches for
   * @return suggestions that contain the given search text
   */
  @Nullable
  SortedSet<Suggestion> findSuggestionsForValue(String prefix);

  /**
   * @param nodeNavigationPathDotDelimited node path
   * @param value                          value selected/typed by user for the current node used as key
   * @return Documentation for selected value with current node as key
   */
  @Nullable
  String getDocumentationForValue(String nodeNavigationPathDotDelimited, String value);

  /**
   * @return whether the node is a leaf or not
   */
  boolean isLeaf();

  /**
   * @return type of node
   */
  @NotNull
  SuggestionNodeType getType();

}
