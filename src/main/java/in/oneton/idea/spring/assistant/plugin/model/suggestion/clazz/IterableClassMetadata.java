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
import java.util.TreeSet;

import static com.intellij.util.containers.ContainerUtil.isEmpty;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType.ITERABLE;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz.ClassSuggestionNodeFactory.newMetadataProxy;
import static in.oneton.idea.spring.assistant.plugin.util.GenericUtil.newListWithMembers;
import static in.oneton.idea.spring.assistant.plugin.util.PsiCustomUtil.getComponentType;
import static java.util.stream.Collectors.toCollection;

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
    PsiType componentType = getComponentType(type);
    if (componentType != null) {
      delegate = newMetadataProxy(module, componentType);
    }
  }

  @Nullable
  @Override
  protected SuggestionDocumentationHelper doFindDirectChild(Module module, String pathSegment) {
    return doWithDelegateOrReturnNull(delegate -> delegate.findDirectChild(module, pathSegment));
  }

  @Override
  protected Collection<? extends SuggestionDocumentationHelper> doFindDirectChildrenForQueryPrefix(
      Module module, String querySegmentPrefix) {
    return doWithDelegateOrReturnNull(
        delegate -> delegate.findDirectChildrenForQueryPrefix(module, querySegmentPrefix));
  }

  @Nullable
  @Override
  protected List<SuggestionNode> doFindDeepestSuggestionNode(Module module,
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
        IterableKeySuggestionNode wrappedNode =
            new IterableKeySuggestionNode((SuggestionNode) directChildKeyMatch);
        matchesRootTillParentNode.add(wrappedNode);
        boolean lastPathSegment = pathSegmentStartIndex == pathSegments.length - 1;
        if (lastPathSegment) {
          return matchesRootTillParentNode;
        } else {
          return wrappedNode
              .findDeepestSuggestionNode(module, matchesRootTillParentNode, pathSegments,
                  pathSegmentStartIndex + 1);
        }
      }
      return null;
    }, null);
  }

  @Nullable
  @Override
  protected SortedSet<Suggestion> doFindKeySuggestionsForQueryPrefix(Module module,
      FileType fileType, List<SuggestionNode> matchesRootTillMe, int numOfAncestors,
      String[] querySegmentPrefixes, int querySegmentPrefixStartIndex) {
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
  public boolean doCheckIsLeaf(Module module) {
    return doWithDelegateAndReturn(delegate -> delegate.isLeaf(module), true);
  }

  @NotNull
  @Override
  public SuggestionNodeType getSuggestionNodeType() {
    return ITERABLE;
  }

  @Nullable
  @Override
  public PsiType getPsiType(Module module) {
    return type;
  }

  private <T> T doWithDelegateOrReturnNull(
      MetadataProxyInvokerWithReturnValue<T> targetInvokerWithReturnValue) {
    return doWithDelegateAndReturn(targetInvokerWithReturnValue, null);
  }

  private <T> T doWithDelegateAndReturn(
      MetadataProxyInvokerWithReturnValue<T> targetInvokerWithReturnValue, T defaultReturnValue) {
    if (delegate != null) {
      return targetInvokerWithReturnValue.invoke(delegate);
    }
    return defaultReturnValue;
  }

}
