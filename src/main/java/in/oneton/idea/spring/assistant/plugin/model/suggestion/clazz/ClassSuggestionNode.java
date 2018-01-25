package in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz;

import com.intellij.psi.PsiClass;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.SortedSet;

import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType.ARRAY;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType.COLLECTION;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType.KNOWN_CLASS;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType.MAP;

/**
 * Represents a suggestion node that derives suggestions from java classes
 * Every dynamic suggestion node loads PSI members lazily
 */
public abstract class ClassSuggestionNode {

  protected final PsiClass target;
  private boolean initComplete;

  protected ClassSuggestionNode(@Nullable PsiClass target) {
    this.target = target;
  }

  @Nullable
  public final SuggestionNode findDeepestMatch(String[] pathSegments, int pathSegmentStartIndex) {
    if (getType() == ARRAY || getType() == COLLECTION || getType() == MAP
        || getType() == KNOWN_CLASS) {
      initIfNotAlready();
      return doFindDeepestMatch(pathSegments, pathSegmentStartIndex);
    }
    return null;
  }

  @Nullable
  protected abstract SuggestionNode doFindDeepestMatch(String[] pathSegments,
      int pathSegmentStartIndex);

  @Nullable
  public List<SuggestionNode> findDeepestMatch(List<SuggestionNode> matchesRootTillParentNode,
      String[] pathSegments, int pathSegmentStartIndex) {
    initIfNotAlready();
    return doFindDeepestMatch(matchesRootTillParentNode, pathSegments, pathSegmentStartIndex);
  }

  @Nullable
  protected abstract List<SuggestionNode> doFindDeepestMatch(
      List<SuggestionNode> matchesRootTillParentNode, String[] pathSegments,
      int pathSegmentStartIndex);

  @Nullable
  public SortedSet<Suggestion> findSuggestionsForKey(@Nullable String ancestralKeysDotDelimited,
      List<SuggestionNode> matchesRootTillParentNode, String[] querySegmentPrefixes,
      int querySegmentPrefixStartIndex) {
    initIfNotAlready();
    return doFindSuggestionsForKey(ancestralKeysDotDelimited, matchesRootTillParentNode,
        querySegmentPrefixes, querySegmentPrefixStartIndex);
  }

  @Nullable
  protected abstract SortedSet<Suggestion> doFindSuggestionsForKey(
      @Nullable String ancestralKeysDotDelimited, List<SuggestionNode> matchesRootTillParentNode,
      String[] querySegmentPrefixes, int querySegmentPrefixStartIndex);


  //  @Nullable
  //  public String getDocumentationForKey(String nodeNavigationPathDotDelimited) {
  //    initIfNotAlready();
  //    return doGetDocumentationForKey(nodeNavigationPathDotDelimited);
  //  }
  //
  //  @NotNull
  //  protected abstract String doGetDocumentationForKey(String nodeNavigationPathDotDelimited);

  @Nullable
  public SortedSet<Suggestion> findSuggestionsForValue(String searchText) {
    initIfNotAlready();
    return doFindSuggestionsForValue(searchText);
  }

  @Nullable
  protected abstract SortedSet<Suggestion> doFindSuggestionsForValue(String searchText);

  @Nullable
  public String getDocumentationForValue(String nodeNavigationPathDotDelimited, String value) {
    initIfNotAlready();
    return doGetDocumentationForValue(nodeNavigationPathDotDelimited, value);
  }

  @NotNull
  protected abstract String doGetDocumentationForValue(String nodeNavigationPathDotDelimited,
      String value);

  //// todo: Init target properties
  //    // 1. For search, such as, name, originalName child trie, e.t.c
  //    // 2. Properties that are require for suggestions & documentation, such as, short & long documentation, type information in short form, e.t.c
  //    //    Map<String, PsiMember> writableProperties = ClassUtil.findWritableProperties(clazz);
  //    //    writableProperties.forEach();
  private void initIfNotAlready() {
    if (!initComplete) {
      init();
      initComplete = true;
    }
  }

  protected abstract void init();

  public abstract boolean supportsDocumentation();

  public abstract boolean isLeaf();

  public abstract boolean hasChildren();

  /**
   * @return type of node
   */
  @NotNull
  public abstract SuggestionNodeType getType();

}
