package in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiType;
import in.oneton.idea.spring.assistant.plugin.completion.FileType;
import in.oneton.idea.spring.assistant.plugin.completion.SuggestionDocumentationHelper;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;

/**
 * Represents a suggestion node that derives suggestions from java classes
 * Every dynamic suggestion node loads PSI members lazily
 */
public abstract class ClassMetadata {

  private boolean initComplete;

  private void initIfNotAlready(Module module) {
    if (!initComplete) {
      init(module);
      initComplete = true;
    }
  }

  /**
   * Init properties such as. This will be called only once
   * 1. For search, such as, name, originalName child trie, e.t.c
   * 2. Properties that are require for suggestions & documentation, such as, short & long documentation, type information in short form, e.t.c
   *
   * @param module module
   */
  protected abstract void init(Module module);

  @Nullable
  public SuggestionDocumentationHelper findDirectChild(Module module, String name) {
    initIfNotAlready(module);
    return doFindDirectChild(module, name);
  }

  @Nullable
  protected abstract SuggestionDocumentationHelper doFindDirectChild(Module module,
      String pathSegment);

  public Collection<? extends SuggestionDocumentationHelper> findDirectChildrenForQueryPrefix(
      Module module, String querySegmentPrefix) {
    initIfNotAlready(module);
    return doFindDirectChildrenForQueryPrefix(module, querySegmentPrefix);
  }

  protected abstract Collection<? extends SuggestionDocumentationHelper> doFindDirectChildrenForQueryPrefix(
      Module module, String querySegmentPrefix);

  @Nullable
  public List<SuggestionNode> findDeepestSuggestionNode(Module module,
      List<SuggestionNode> matchesRootTillParentNode, String[] pathSegments,
      int pathSegmentStartIndex) {
    initIfNotAlready(module);
    return doFindDeepestSuggestionNode(module, matchesRootTillParentNode, pathSegments,
        pathSegmentStartIndex);
  }

  @Nullable
  protected abstract List<SuggestionNode> doFindDeepestSuggestionNode(Module module,
      List<SuggestionNode> matchesRootTillParentNode, String[] pathSegments,
      int pathSegmentStartIndex);

  @Nullable
  public SortedSet<Suggestion> findKeySuggestionsForQueryPrefix(Module module, FileType fileType,
      List<SuggestionNode> matchesRootTillParentNode, int numOfAncestors,
      String[] querySegmentPrefixes, int querySegmentPrefixStartIndex) {
    initIfNotAlready(module);
    return doFindKeySuggestionsForQueryPrefix(module, fileType, matchesRootTillParentNode,
        numOfAncestors, querySegmentPrefixes, querySegmentPrefixStartIndex);
  }

  @Nullable
  protected abstract SortedSet<Suggestion> doFindKeySuggestionsForQueryPrefix(Module module,
      FileType fileType, List<SuggestionNode> matchesRootTillParentNode, int numOfAncestors,
      String[] querySegmentPrefixes, int querySegmentPrefixStartIndex);

  @Nullable
  public SortedSet<Suggestion> findValueSuggestionsForPrefix(Module module, FileType fileType,
      List<SuggestionNode> matchesRootTillMe, String prefix) {
    initIfNotAlready(module);
    return doFindValueSuggestionsForPrefix(module, fileType, matchesRootTillMe, prefix);
  }

  protected abstract SortedSet<Suggestion> doFindValueSuggestionsForPrefix(Module module,
      FileType fileType, List<SuggestionNode> matchesRootTillMe, String prefix);

  @Nullable
  public String getDocumentationForValue(Module module, String nodeNavigationPathDotDelimited,
      String value) {
    initIfNotAlready(module);
    return doGetDocumentationForValue(module, nodeNavigationPathDotDelimited, value);
  }

  @Nullable
  protected abstract String doGetDocumentationForValue(Module module,
      String nodeNavigationPathDotDelimited, String value);

  public boolean isLeaf(Module module) {
    initIfNotAlready(module);
    return doCheckIsLeaf(module);
  }

  protected abstract boolean doCheckIsLeaf(Module module);

  /**
   * @return type of node
   */
  @NotNull
  public abstract SuggestionNodeType getSuggestionNodeType();

  @Nullable
  public abstract PsiType getPsiType(Module module);

}
