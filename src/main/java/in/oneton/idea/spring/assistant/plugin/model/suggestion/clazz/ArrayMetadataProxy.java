package in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiType;
import in.oneton.idea.spring.assistant.plugin.completion.SuggestionDocumentationHelper;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;

import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType.ARRAY;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz.ClassSuggestionNodeFactory.newMetadataProxy;

public class ArrayMetadataProxy implements MetadataProxy {

  @NotNull
  private final PsiArrayType type;
  @Nullable
  private final MetadataProxy delegate;

  ArrayMetadataProxy(Module module, @NotNull PsiArrayType type) {
    this.type = type;
    delegate = newMetadataProxy(module, type.getComponentType());
  }

  @Nullable
  @Override
  public SuggestionDocumentationHelper findDirectChild(Module module, String pathSegment) {
    return doWithDelegateAndReturn(delegate -> delegate.findDirectChild(module, pathSegment), null);
  }

  @Nullable
  @Override
  public Collection<? extends SuggestionDocumentationHelper> findDirectChildrenForQueryPrefix(
      Module module, String querySegmentPrefix) {
    return doWithDelegateAndReturn(
        delegate -> delegate.findDirectChildrenForQueryPrefix(module, querySegmentPrefix), null);
  }

  @Nullable
  @Override
  public List<SuggestionNode> findDeepestSuggestionNode(Module module,
      List<SuggestionNode> matchesRootTillParentNode, String[] pathSegments,
      int pathSegmentStartIndex) {
    return doWithDelegateAndReturn(delegate -> delegate
        .findDeepestSuggestionNode(module, matchesRootTillParentNode, pathSegments,
            pathSegmentStartIndex), null);
  }

  @Nullable
  @Override
  public SortedSet<Suggestion> findKeySuggestionsForQueryPrefix(Module module,
      @Nullable String ancestralKeysDotDelimited, List<SuggestionNode> matchesRootTillMe,
      String[] querySegmentPrefixes, int querySegmentPrefixStartIndex) {
    return doWithDelegateAndReturn(delegate -> delegate
        .findKeySuggestionsForQueryPrefix(module, ancestralKeysDotDelimited, matchesRootTillMe,
            querySegmentPrefixes, querySegmentPrefixStartIndex), null);
  }

  @Nullable
  @Override
  public SortedSet<Suggestion> findValueSuggestionsForPrefix(Module module,
      List<SuggestionNode> matchesRootTillMe, String prefix) {
    return doWithDelegateAndReturn(
        delegate -> delegate.findValueSuggestionsForPrefix(module, matchesRootTillMe, prefix),
        null);
  }

  @Nullable
  @Override
  public String getDocumentationForValue(Module module, String nodeNavigationPathDotDelimited,
      String value) {
    return doWithDelegateAndReturn(delegate -> delegate
        .getDocumentationForValue(module, nodeNavigationPathDotDelimited, value), null);
  }

  @Override
  public boolean isLeaf(Module module) {
    return doWithDelegateAndReturn(delegate -> delegate.isLeaf(module), true);
  }

  //  @Override
  //  public void refreshMetadata(Module module) {
  //    doWithDelegate(delegate -> delegate.refreshMetadata(module));
  //  }

  @NotNull
  @Override
  public SuggestionNodeType getSuggestionNodeType(Module module) {
    return ARRAY;
  }

  @Nullable
  @Override
  public PsiType getPsiType(Module module) {
    return type;
  }

  @Override
  public boolean targetRepresentsArray() {
    return true;
  }

  @Override
  public boolean targetClassRepresentsIterable(Module module) {
    return false;
  }

  private <T> T doWithDelegateAndReturn(
      MetadataProxyInvokerWithReturnValue<T> targetInvokerWithReturnValue, T defaultReturnValue) {
    if (delegate != null) {
      return targetInvokerWithReturnValue.invoke(delegate);
    }
    return defaultReturnValue;
  }

  private void doWithDelegate(MetadataProxyInvoker targetInvoker) {
    if (delegate != null) {
      targetInvoker.invoke(delegate);
    }
  }

}
