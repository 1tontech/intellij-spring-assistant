package in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClassType;
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

import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType.ITERABLE;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz.ClassSuggestionNodeFactory.newMetadataProxy;
import static in.oneton.idea.spring.assistant.plugin.util.PsiCustomUtil.getFirstTypeParameter;

public class IterableClassMetadata extends ClassMetadata {

  @NotNull
  private final PsiClassType type;

  @Nullable
  private MetadataProxy delegate;

  IterableClassMetadata(@NotNull PsiClassType type) {
    this.type = type;
  }

  @Override
  protected void init(Module module) {
    // Since raw Iterable/Collection can also be specified
    PsiType componentType = getFirstTypeParameter(type);
    if (componentType != null) {
      delegate = newMetadataProxy(module, componentType);
    }
  }

  @Nullable
  @Override
  protected SuggestionDocumentationHelper doFindDirectChild(Module module, String pathSegment) {
    throw new IllegalAccessError(
        "Should never be called, as there can be never the case of Map<Iteable<K, V1>, V2>");
  }

  @Override
  protected Collection<? extends SuggestionDocumentationHelper> doFindDirectChildrenForQueryPrefix(
      Module module, String querySegmentPrefix) {
    throw new IllegalAccessError(
        "Should never be called, as there can be never the case of Map<Iteable<K, V1>, V2>");
  }

  @Nullable
  @Override
  protected List<SuggestionNode> doFindDeepestSuggestionNode(Module module,
      List<SuggestionNode> matchesRootTillParentNode, String[] pathSegments,
      int pathSegmentStartIndex) {
    return doWithDelegateAndReturn(delegate -> delegate
        .findDeepestSuggestionNode(module, matchesRootTillParentNode, pathSegments,
            pathSegmentStartIndex), null);
  }

  @Nullable
  @Override
  protected SortedSet<Suggestion> doFindKeySuggestionsForQueryPrefix(Module module,
      FileType fileType, List<SuggestionNode> matchesRootTillParentNode, int numOfAncestors,
      String[] querySegmentPrefixes, int querySegmentPrefixStartIndex) {
    return doWithDelegateAndReturn(delegate -> delegate
        .findKeySuggestionsForQueryPrefix(module, fileType, matchesRootTillParentNode,
            numOfAncestors, querySegmentPrefixes, querySegmentPrefixStartIndex), null);
  }

  @Override
  protected SortedSet<Suggestion> doFindValueSuggestionsForPrefix(Module module, FileType fileType,
      List<SuggestionNode> matchesRootTillMe, String prefix) {
    return doWithDelegateAndReturn(delegate -> delegate
        .findValueSuggestionsForPrefix(module, fileType, matchesRootTillMe, prefix), null);
  }

  @Nullable
  @Override
  protected String doGetDocumentationForValue(Module module, String nodeNavigationPathDotDelimited,
      String value) {
    return doWithDelegateAndReturn(delegate -> delegate
        .getDocumentationForValue(module, nodeNavigationPathDotDelimited, value), null);
  }

  @Override
  public boolean isLeaf(Module module) {
    return doWithDelegateAndReturn(delegate -> delegate.isLeaf(module), true);
  }

  @NotNull
  @Override
  public SuggestionNodeType getSuggestionNodeType() {
    return ITERABLE;
  }

  @Nullable
  @Override
  public PsiType getPsiType() {
    return type;
  }

  private <T> T doWithDelegateAndReturn(
      MetadataProxyInvokerWithReturnValue<T> targetInvokerWithReturnValue, T defaultReturnValue) {
    if (delegate != null) {
      return targetInvokerWithReturnValue.invoke(delegate);
    }
    return defaultReturnValue;
  }

}
