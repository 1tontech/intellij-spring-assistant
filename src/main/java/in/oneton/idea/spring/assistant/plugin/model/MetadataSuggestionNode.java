package in.oneton.idea.spring.assistant.plugin.model;

import org.jetbrains.annotations.Contract;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import static java.util.stream.Collectors.joining;

public abstract class MetadataSuggestionNode implements SuggestionNode {

  protected abstract String getName();

  protected abstract String getOriginalName();

  @Nullable
  protected abstract MetadataGroupSuggestionNode getParent();

  public abstract Set<String> getBelongsTo();

  /**
   * If {@code matchAllSegments} is true, all {@code pathSegments} starting from {@code pathSegmentStartIndex} will be attempted to be matched. If a result is found, it will be returned. Else null
   * Else, method should attempt to match as deep as it can match & return bottom most match
   *
   * @param pathSegments          path segments to match against
   * @param pathSegmentStartIndex index within {@code pathSegments} to start match from
   * @param matchAllSegments      should all {@code pathSegments} be matched or not
   * @return leaf suggestion node that matches path segments starting with {@code pathSegmentStartIndex} or null otherwise
   */
  @Contract("_, _, true -> null; _, _, false -> !null")
  public abstract MetadataSuggestionNode findDeepestMatch(String[] pathSegments,
      int pathSegmentStartIndex, boolean matchAllSegments);

  /**
   * @param pathRootTillParentNode       path from root till parent node
   * @param suggestionDepthFromEndOfPath represents number of items in `pathRootTillParentNode` from end that match current `querySegmentPrefixes`
   * @param querySegmentPrefixes         the search text parts split based on period delimiter
   * @param querySegmentPrefixStartIndex current index in the `querySegmentPrefixes` to start search from
   * @param navigateDeepIfNoMatches      whether search should proceed further down if the search cannot find results at this level
   * @return Suggestions matching the given querySegmentPrefixes criteria
   */
  @Nullable
  public abstract Set<Suggestion> findSuggestionsForKey(
      List<MetadataSuggestionNode> pathRootTillParentNode, int suggestionDepthFromEndOfPath,
      String[] querySegmentPrefixes, int querySegmentPrefixStartIndex,
      boolean navigateDeepIfNoMatches);

  /**
   * @param pathRootTillParentNode       path from root till parent node
   * @param suggestionDepthFromEndOfPath represents number of items in `pathRootTillParentNode` from end that match current `querySegmentPrefixes`
   * @param querySegmentPrefixes         the search text parts split based on period delimiter
   * @param querySegmentPrefixStartIndex current index in the `querySegmentPrefixes` to start search from
   * @param navigateDeepIfNoMatches      whether search should proceed further down if the search cannot find results at this level
   * @return Suggestions matching the given querySegmentPrefixes criteria from within the children
   */
  @Nullable
  public abstract Set<Suggestion> findChildSuggestionsForKey(
      List<MetadataSuggestionNode> pathRootTillParentNode, int suggestionDepthFromEndOfPath,
      String[] querySegmentPrefixes, int querySegmentPrefixStartIndex,
      boolean navigateDeepIfNoMatches);

  protected abstract boolean isRoot();

  /**
   * @return whether the node expects any children or not
   */
  public abstract boolean isGroup();

  /**
   * @return whether the node expects any children or not
   */
  public abstract boolean isProperty();

  protected abstract boolean hasOnlyOneLeaf();

  public int numOfHopesToRoot() {
    int hopCount = 0;
    MetadataSuggestionNode current = getParent();
    while (current != null) {
      hopCount++;
      current = current.getParent();
    }
    return hopCount;
  }

  public String getPathFromRoot() {
    Stack<String> leafTillRoot = new Stack<>();
    MetadataSuggestionNode current = this;
    do {
      leafTillRoot.push(current.getOriginalName());
      current = current.getParent();
    } while (current != null);
    return leafTillRoot.stream().collect(joining("."));
  }

  /**
   * @param containerPath Represents path to the metadata file container
   * @return true if no children left & this item does not belong to any other source
   */
  public abstract boolean removeRefCascadeDown(String containerPath);

  public List<MetadataSuggestionNode> getNodesFromRootTillMe() {
    List<MetadataSuggestionNode> rootTillMe = new ArrayList<>();
    MetadataSuggestionNode current = this;
    do {
      rootTillMe.add(0, current);
      current = current.getParent();
    } while (current != null);
    return rootTillMe;
  }

  public void addRefCascadeTillRoot(String containerPath) {
    MetadataSuggestionNode node = this;
    do {
      if (node.getBelongsTo().contains(containerPath)) {
        break;
      }
      node.getBelongsTo().add(containerPath);
      node = node.getParent();
    } while (node != null && !node.isRoot());
  }

}
