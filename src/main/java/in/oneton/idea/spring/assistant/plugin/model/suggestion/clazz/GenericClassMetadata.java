package in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiType;
import in.oneton.idea.spring.assistant.plugin.completion.FileType;
import in.oneton.idea.spring.assistant.plugin.completion.SuggestionDocumentationHelper;
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

import static com.intellij.util.containers.ContainerUtil.isEmpty;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType.KNOWN_CLASS;
import static in.oneton.idea.spring.assistant.plugin.util.GenericUtil.newListWithMembers;
import static in.oneton.idea.spring.assistant.plugin.util.PsiCustomUtil.getSanitisedPropertyToPsiMemberWrapper;
import static in.oneton.idea.spring.assistant.plugin.util.PsiCustomUtil.isValidType;
import static in.oneton.idea.spring.assistant.plugin.util.PsiCustomUtil.toValidPsiClass;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toCollection;

public class GenericClassMetadata extends ClassMetadata {

  @NotNull
  private final PsiClassType type;

  @Nullable
  private Map<String, GenericClassMemberWrapper> childLookup;
  @Nullable
  private Trie<String, GenericClassMemberWrapper> childrenTrie;

  GenericClassMetadata(@NotNull PsiClassType type) {
    this.type = type;
  }

  @Override
  protected void init(Module module) {
    init(type);
  }

  private void init(@NotNull PsiClassType type) {
    if (isValidType(type)) {
      childLookup = getSanitisedPropertyToPsiMemberWrapper(toValidPsiClass(type));
      if (childLookup != null) {
        childrenTrie = new PatriciaTrie<>();
        childLookup.forEach((k, v) -> childrenTrie.put(k, v));
      }
    } else {
      childLookup = null;
      childrenTrie = null;
    }
  }

  @Nullable
  @Override
  protected SuggestionDocumentationHelper doFindDirectChild(Module module, String pathSegment) {
    // TODO: Does spring Environment support setting any type other than boolean, number, string & enum to be set as keys. If not we should throw an exception
    // For now lets allow
    if (childLookup != null && childLookup.containsKey(pathSegment)) {
      return childLookup.get(pathSegment);
    }
    return null;
  }

  @Override
  protected Collection<? extends SuggestionDocumentationHelper> doFindDirectChildrenForQueryPrefix(
      Module module, String querySegmentPrefix) {
    // TODO: Does spring Environment support setting any type other than boolean, number, string & enum to be set as keys. If not we should throw an exception
    // For now lets allow
    if (childrenTrie != null) {
      SortedMap<String, GenericClassMemberWrapper> prefixMap =
          childrenTrie.prefixMap(querySegmentPrefix);
      if (!isEmpty(prefixMap)) {
        return prefixMap.values();
      }
    }
    return null;
  }

  @Nullable
  @Override
  protected List<SuggestionNode> doFindDeepestSuggestionNode(Module module,
      List<SuggestionNode> matchesRootTillParentNode, String[] pathSegments,
      int pathSegmentStartIndex) {
    if (!isLeaf(module)) {
      String pathSegment = pathSegments[pathSegmentStartIndex];
      if (childLookup != null && childLookup.containsKey(pathSegment)) {
        GenericClassMemberWrapper memberWrapper = childLookup.get(pathSegment);
        matchesRootTillParentNode.add(memberWrapper);
        boolean lastPathSegment = pathSegmentStartIndex == pathSegments.length - 1;
        if (lastPathSegment) {
          return matchesRootTillParentNode;
        } else {
          return memberWrapper.getMemberReferredClassMetadataProxy(module)
              .findDeepestSuggestionNode(module, matchesRootTillParentNode, pathSegments,
                  pathSegmentStartIndex + 1);
        }
      }
    }
    return null;
  }

  @Nullable
  @Override
  protected SortedSet<Suggestion> doFindKeySuggestionsForQueryPrefix(Module module,
      FileType fileType, List<SuggestionNode> matchesRootTillCurrentNode, int numOfAncestors,
      String[] querySegmentPrefixes, int querySegmentPrefixStartIndex) {
    if (!isLeaf(module)) {
      if (childrenTrie != null) {
        String querySegmentPrefix = querySegmentPrefixes[querySegmentPrefixStartIndex];
        SortedMap<String, GenericClassMemberWrapper> sortedPrefixToMemberWrapper =
            childrenTrie.prefixMap(querySegmentPrefix);
        if (sortedPrefixToMemberWrapper.size() != 0) {
          Collection<GenericClassMemberWrapper> wrappers = sortedPrefixToMemberWrapper.values();
          boolean lastQuerySegment =
              querySegmentPrefixStartIndex == (querySegmentPrefixes.length - 1);
          if (lastQuerySegment) {
            return wrappers.stream().map(wrapper -> wrapper.buildSuggestionForKey(module, fileType,
                unmodifiableList(newListWithMembers(matchesRootTillCurrentNode, wrapper)),
                numOfAncestors)).collect(toCollection(TreeSet::new));
          } else {
            SortedSet<Suggestion> suggestions = null;
            for (GenericClassMemberWrapper wrapper : wrappers) {
              List<SuggestionNode> pathRootTillCurrentNode =
                  unmodifiableList(newListWithMembers(matchesRootTillCurrentNode, wrapper));
              Set<Suggestion> matchedSuggestions =
                  wrapper.getMemberReferredClassMetadataProxy(module)
                      .findKeySuggestionsForQueryPrefix(module, fileType, pathRootTillCurrentNode,
                          numOfAncestors, querySegmentPrefixes, querySegmentPrefixStartIndex);
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
      }
    }
    return null;
  }

  @Override
  protected SortedSet<Suggestion> doFindValueSuggestionsForPrefix(Module module, FileType fileType,
      List<SuggestionNode> matchesRootTillMe, String prefix) {
    // Can be called if the class is specified as a value within an array & the class does nto have any children
    return null;
  }

  @Nullable
  @Override
  protected String doGetDocumentationForValue(Module module, String nodeNavigationPathDotDelimited,
      String value) {
    throw new IllegalAccessError("Method should never be called for a generic class");
  }

  @Override
  public boolean doCheckIsLeaf(Module module) {
    return childLookup == null || childLookup.size() == 0;
  }

  @NotNull
  @Override
  public SuggestionNodeType getSuggestionNodeType() {
    return KNOWN_CLASS;
  }

  @NotNull
  @Override
  public PsiType getPsiType(Module module) {
    return type;
  }
}
