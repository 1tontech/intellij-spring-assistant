package in.oneton.idea.spring.assistant.plugin.model.suggestion;

import com.intellij.openapi.module.Module;
import in.oneton.idea.spring.assistant.plugin.completion.DocumentationProvider;
import in.oneton.idea.spring.assistant.plugin.completion.SuggestionNodeTypeProvider;

import javax.annotation.Nullable;
import java.util.List;
import java.util.SortedSet;

public interface SuggestionNode extends DocumentationProvider, SuggestionNodeTypeProvider {

  static String sanitise(String name) {
    return name.trim().replaceAll("_", "").replace("-", "").toLowerCase();
  }

  /**
   * If {@code matchAllSegments} is true, all {@code pathSegments} starting from {@code pathSegmentStartIndex} will be attempted to be matched. If a result is found, it will be returned. Else null
   * Else, method should attempt to match as deep as it can match & return bottom most match
   * <p>
   * <b>NOTE:</b> Though this method does every thing the other `findDeepestSuggestionNode` does, the reason for the existence the other method is for performance reasons
   *
   * @param module                    module
   * @param matchesRootTillParentNode matches till parent node
   * @param pathSegments              path segments to match against
   * @param pathSegmentStartIndex     index within {@code pathSegments} to start match from
   * @return leaf suggestion node that matches path segments starting with {@code pathSegmentStartIndex} or null otherwise
   */
  @Nullable
  List<SuggestionNode> findDeepestSuggestionNode(Module module,
      List<SuggestionNode> matchesRootTillParentNode, String[] pathSegments,
      int pathSegmentStartIndex);

  /**
   * @param module                       module
   * @param ancestralKeysDotDelimited    all ancestral keys dot delimited, required for showing full path in documentation
   * @param matchesRootTillMe            path from root till current node
   * @param querySegmentPrefixes         the search text parts split based on period delimiter
   * @param querySegmentPrefixStartIndex current index in the `querySegmentPrefixes` to start search from
   * @return Suggestions matching the given querySegmentPrefixes criteria from within the children
   */
  @Nullable
  SortedSet<Suggestion> findKeySuggestionsForQueryPrefix(Module module,
      @Nullable String ancestralKeysDotDelimited, List<SuggestionNode> matchesRootTillMe,
      String[] querySegmentPrefixes, int querySegmentPrefixStartIndex);

  /**
   * @param module module
   * @return original name without any sanitising, null if the suggestion node does not have any corresponding name
   */
  @Nullable
  String getOriginalName(Module module);

  @Nullable
  String getNameForDocumentation(Module module);

  /**
   * Find all applicable suggestions for the given search text
   *
   * @param module            module
   * @param matchesRootTillMe path from root till current node
   * @param prefix            prefix to find matches for
   * @return suggestions that contain the given search text
   */
  @Nullable
  SortedSet<Suggestion> findValueSuggestionsForPrefix(Module module,
      List<SuggestionNode> matchesRootTillMe, String prefix);

  /**
   * @param module module
   * @return whether the node is a leaf or not
   */
  boolean isLeaf(Module module);

}
