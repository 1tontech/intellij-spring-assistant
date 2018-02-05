package in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiArrayType;
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
import java.util.TreeSet;

import static com.intellij.util.containers.ContainerUtil.isEmpty;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType.ARRAY;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz.ClassSuggestionNodeFactory.newMetadataProxy;
import static in.oneton.idea.spring.assistant.plugin.util.GenericUtil.newListWithMembers;
import static java.util.stream.Collectors.toCollection;

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
    return doWithDelegateAndReturn(delegate -> {
      String pathSegment = pathSegments[pathSegmentStartIndex];
      SuggestionDocumentationHelper directChildKeyMatch =
          delegate.findDirectChild(module, pathSegment);
      if (directChildKeyMatch != null) {
        // TODO: Need to identify a better mechanism than this dirty way. Probably use ClassSuggestionNode as return type for findDirectChildrenForQueryPrefix
        // since we are in an iterable(multiple values), keys would be requested, only if the object we are referring is not a leaf => GenericClassWrapper
        assert directChildKeyMatch instanceof SuggestionNode;
        matchesRootTillParentNode
            .add(new IterableKeySuggestionNode((SuggestionNode) directChildKeyMatch));
        boolean lastPathSegment = pathSegmentStartIndex == pathSegments.length - 1;
        if (lastPathSegment) {
          return matchesRootTillParentNode;
        } else {
          return delegate.findDeepestSuggestionNode(module, matchesRootTillParentNode, pathSegments,
              pathSegmentStartIndex);
        }
      }
      return null;
    }, null);
  }

  @Nullable
  @Override
  public SortedSet<Suggestion> findKeySuggestionsForQueryPrefix(Module module, FileType fileType,
      List<SuggestionNode> matchesRootTillMe, int numOfAncestors, String[] querySegmentPrefixes,
      int querySegmentPrefixStartIndex) {
    return doWithDelegateAndReturn(delegate -> {
      String querySegmentPrefix = querySegmentPrefixes[querySegmentPrefixStartIndex];
      Collection<? extends SuggestionDocumentationHelper> matches =
          delegate.findDirectChildrenForQueryPrefix(module, querySegmentPrefix);
      if (!isEmpty(matches)) {
        return matches.stream().map(helper -> {
          // TODO: Need to identify a better mechanism than this dirty way. Probably use ClassSuggestionNode as return type for findDirectChildrenForQueryPrefix
          // since we are in an iterable(multiple values), keys would be requested, only if the object we are referring is not a leaf => GenericClassWrapper
          assert helper instanceof SuggestionNode;
          List<SuggestionNode> rootTillMe = newListWithMembers(matchesRootTillMe,
              new IterableKeySuggestionNode((SuggestionNode) helper));
          return helper.buildSuggestionForKey(module, fileType, rootTillMe, numOfAncestors);
        }).collect(toCollection(TreeSet::new));
      }
      return null;
    }, null);
  }

  @Nullable
  @Override
  public SortedSet<Suggestion> findValueSuggestionsForPrefix(Module module, FileType fileType,
      List<SuggestionNode> matchesRootTillMe, String prefix) {
    return doWithDelegateAndReturn(delegate -> delegate
        .findValueSuggestionsForPrefix(module, fileType, matchesRootTillMe, prefix), null);
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
