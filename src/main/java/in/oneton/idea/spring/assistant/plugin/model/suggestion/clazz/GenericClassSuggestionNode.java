package in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz;

import com.intellij.psi.PsiClass;
import in.oneton.idea.spring.assistant.plugin.PsiMemberWrapper;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType;
import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import static in.oneton.idea.spring.assistant.plugin.ClassUtil.getSanitisedPropertyToPsiMemberWrapper;
import static in.oneton.idea.spring.assistant.plugin.Util.newListWithMembers;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType.KNOWN_CLASS;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toCollection;

public class GenericClassSuggestionNode extends ClassSuggestionNode {

  private Map<String, PsiMemberWrapper> childLookup;
  private Trie<String, PsiMemberWrapper> childrenTrie;

  public GenericClassSuggestionNode(PsiClass target) {
    super(target);
  }

  @Override
  protected void init() {
    childLookup = getSanitisedPropertyToPsiMemberWrapper(target);
    if (childLookup != null) {
      childrenTrie = new PatriciaTrie<>();
      childLookup.forEach((k, v) -> childrenTrie.put(k, v));
    }
  }

  @Nullable
  @Override
  protected SuggestionNode doFindDeepestMatch(String[] pathSegments, int pathSegmentStartIndex) {
    String pathSegment = pathSegments[pathSegmentStartIndex];
    if (childLookup.containsKey(pathSegment)) {
      ClassSuggestionNodeProxy targetNode = childLookup.get(pathSegment).getTargetNode();
      boolean lastPathSegment = pathSegmentStartIndex == pathSegments.length - 1;
      if (lastPathSegment) {
        return targetNode;
      } else {
        return targetNode.findDeepestMatch(pathSegments, pathSegmentStartIndex + 1);
      }
    }
    return null;
  }

  @Nullable
  @Override
  protected List<SuggestionNode> doFindDeepestMatch(List<SuggestionNode> matchesRootTillParentNode,
      String[] pathSegments, int pathSegmentStartIndex) {
    String pathSegment = pathSegments[pathSegmentStartIndex];
    if (childLookup.containsKey(pathSegment)) {
      ClassSuggestionNodeProxy targetNode = childLookup.get(pathSegment).getTargetNode();
      matchesRootTillParentNode.add(targetNode);
      boolean lastPathSegment = pathSegmentStartIndex == pathSegments.length - 1;
      if (lastPathSegment) {
        return matchesRootTillParentNode;
      } else {
        return targetNode
            .findDeepestMatch(matchesRootTillParentNode, pathSegments, pathSegmentStartIndex + 1);
      }
    }
    return null;
  }

  @Nullable
  @Override
  protected SortedSet<Suggestion> doFindSuggestionsForKey(
      @Nullable String ancestralKeysDotDelimited, List<SuggestionNode> matchesRootTillParentNode,
      String[] querySegmentPrefixes, int querySegmentPrefixStartIndex) {
    String querySegmentPrefix = querySegmentPrefixes[querySegmentPrefixStartIndex];
    SortedMap<String, PsiMemberWrapper> sortedPrefixToMemberWrapper =
        childrenTrie.prefixMap(querySegmentPrefix);
    if (sortedPrefixToMemberWrapper.size() != 0) {
      Collection<PsiMemberWrapper> wrappers = sortedPrefixToMemberWrapper.values();
      boolean lastQuerySegment = querySegmentPrefixStartIndex == (querySegmentPrefixes.length - 1);
      if (lastQuerySegment) {
        return wrappers.stream().map(wrapper -> wrapper
            .buildSuggestion(ancestralKeysDotDelimited, matchesRootTillParentNode,
                wrapper.getTargetNode())).collect(toCollection(TreeSet::new));
      } else {
        SortedSet<Suggestion> suggestions = null;
        for (PsiMemberWrapper wrapper : wrappers) {
          List<SuggestionNode> pathRootTillCurrentNode = unmodifiableList(
              newListWithMembers(matchesRootTillParentNode, wrapper.getTargetNode()));
          Set<Suggestion> matchedSuggestions = wrapper.getTargetNode()
              .findSuggestionsForKey(ancestralKeysDotDelimited, pathRootTillCurrentNode,
                  querySegmentPrefixes, querySegmentPrefixStartIndex);
          if (matchedSuggestions != null) {
            if (suggestions == null) {
              suggestions = new TreeSet<>();
            }
            suggestions.addAll(matchedSuggestions);
          }
        }
        return suggestions;
      }
    }
    return null;
  }

  @Override
  public boolean supportsDocumentation() {
    return true;
  }

  @Override
  public boolean isLeaf() {
    return !hasChildren();
  }

  @Override
  public boolean hasChildren() {
    return childLookup != null && childLookup.size() != 0;
  }

  @NotNull
  @Override
  public SuggestionNodeType getType() {
    return KNOWN_CLASS;
  }
}
