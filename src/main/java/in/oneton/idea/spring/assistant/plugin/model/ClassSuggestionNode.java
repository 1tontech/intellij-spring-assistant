package in.oneton.idea.spring.assistant.plugin.model;

import com.intellij.psi.PsiClass;
import org.apache.commons.collections4.Trie;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Set;

import static in.oneton.idea.spring.assistant.plugin.model.SuggestionNodeType.ARRAY;
import static in.oneton.idea.spring.assistant.plugin.model.SuggestionNodeType.COLLECTION;
import static in.oneton.idea.spring.assistant.plugin.model.SuggestionNodeType.KNOWN_CLASS;
import static in.oneton.idea.spring.assistant.plugin.model.SuggestionNodeType.MAP;

/**
 * Represents a suggestion node that derives suggestions from java classes
 * Every dynamic suggestion node loads PSI members lazily
 */
public abstract class ClassSuggestionNode extends AbstractSuggestionNode {

  protected final PsiClass target;

  @Nullable
  private Trie<String, ClassSuggestionNode> sanitisedChildTrie;

  protected ClassSuggestionNode(@Nullable PsiClass target) {
    this.target = target;
  }

  @Nullable
  public final SuggestionNode findDeepestExactMatch(String[] pathSegments, int startWith,
      boolean matchAllSegments) {
    if (getType() == ARRAY || getType() == COLLECTION || getType() == MAP
        || getType() == KNOWN_CLASS) {
      initIfNotAlready();
      return doFindDeepestExactMatch(pathSegments, startWith, matchAllSegments);
    }
    return null;
  }

  @Contract("_, _, true -> null; _, _, false -> !null")
  protected abstract SuggestionNode doFindDeepestExactMatch(String[] pathSegments, int startWith,
      boolean matchAllSegments);

  @Nullable
  public final Set<Suggestion> findSuggestionsForKey(String[] querySegmentPrefixes,
      int suggestionDepth, int startWith, boolean forceSearchAcrossTree) {
    initIfNotAlready();
    return doFindSuggestionsForKey(querySegmentPrefixes, suggestionDepth, startWith,
        forceSearchAcrossTree);
  }

  @Nullable
  protected abstract Set<Suggestion> doFindSuggestionsForKey(String[] querySegmentPrefixes,
      int suggestionDepth, int startWith, boolean forceSearchAcrossTree);

  //  @Nullable
  //  @Override
  //  public final Set<Suggestion> findChildSuggestionsForKey(String[] querySegmentPrefixes,
  //      int suggestionDepth, int startWith, boolean forceSearchAcrossTree) {
  //    initIfNotAlready();
  //    return doFindChildSuggestionsForKey(querySegmentPrefixes, suggestionDepth, startWith,
  //        forceSearchAcrossTree);
  //  }
  //
  //  @Nullable
  //  protected abstract Set<Suggestion> doFindChildSuggestionsForKey(String[] querySegmentPrefixes,
  //      int suggestionDepth, int startWith, boolean forceSearchAcrossTree);

  @Override
  public final String getDocumentationForKey() {
    initIfNotAlready();
    return doGetDocumentationForKey();
  }

  @NotNull
  protected abstract String doGetDocumentationForKey();

  @Nullable
  @Override
  public Set<Suggestion> findSuggestionsForValue(String searchText) {
    initIfNotAlready();
    return doFindSuggestionsForValue(searchText);
  }

  @Nullable
  protected abstract Set<Suggestion> doFindSuggestionsForValue(String searchText);

  @Override
  public String getDocumentationForValue(String value) {
    initIfNotAlready();
    return doGetDocumentationForValue(value);
  }

  @NotNull
  protected abstract String doGetDocumentationForValue(String value);

  @Override
  public boolean removeRef(String containerPath) {
    // Since this is a dynamic node, there are no references to remove
    return true;
  }

  @Override
  public final boolean isGroup() {
    // todo: implement
    return false;
  }

  @Override
  public final boolean isLeaf() {
    // todo: implement
    return false;
  }

  @Override
  public SuggestionNodeType getType() {
    return type;
  }

  private void initIfNotAlready() {
    // todo: Init target properties
    // 1. For search, such as, name, originalName child trie, e.t.c
    // 2. Properties that are require for suggestions & documentation, such as, short & long documentation, type information in short form, e.t.c
    //    Map<String, PsiMember> writableProperties = PsiUtil.findWritableProperties(clazz);
    //    writableProperties.forEach();
  }

}
