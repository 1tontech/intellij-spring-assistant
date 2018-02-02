package in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
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

import static com.intellij.openapi.util.Key.create;
import static com.intellij.psi.util.CachedValueProvider.Result.create;
import static com.intellij.psi.util.CachedValuesManager.getCachedValue;
import static com.intellij.psi.util.PsiModificationTracker.JAVA_STRUCTURE_MODIFICATION_COUNT;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType.UNKNOWN_CLASS;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz.ClassSuggestionNodeFactory.newClassMetadata;
import static in.oneton.idea.spring.assistant.plugin.util.PsiCustomUtil.toValidPsiClass;
import static java.util.Objects.requireNonNull;

public class ClassMetadataProxy implements MetadataProxy {

  @NotNull
  private final PsiClass targetClass;

  @NotNull
  private final PsiClassType type;

  ClassMetadataProxy(@NotNull PsiClassType type) {
    this.type = type;
    targetClass = requireNonNull(toValidPsiClass(type));
  }

  @Nullable
  @Override
  public SuggestionDocumentationHelper findDirectChild(Module module, String pathSegment) {
    return doWithTargetAndReturn(target -> target.findDirectChild(module, pathSegment), null);
  }

  @Nullable
  @Override
  public Collection<? extends SuggestionDocumentationHelper> findDirectChildrenForQueryPrefix(
      Module module, String querySegmentPrefix) {
    return doWithTargetAndReturn(
        target -> target.findDirectChildrenForQueryPrefix(module, querySegmentPrefix), null);
  }

  @Nullable
  @Override
  public List<SuggestionNode> findDeepestSuggestionNode(Module module,
      List<SuggestionNode> matchesRootTillParentNode, String[] pathSegments,
      int pathSegmentStartIndex) {
    return doWithTargetAndReturn(target -> target
        .findDeepestSuggestionNode(module, matchesRootTillParentNode, pathSegments,
            pathSegmentStartIndex), null);
  }

  @Nullable
  @Override
  public SortedSet<Suggestion> findKeySuggestionsForQueryPrefix(Module module, FileType fileType,
      List<SuggestionNode> matchesRootTillMe, int numOfAncestors, String[] querySegmentPrefixes,
      int querySegmentPrefixStartIndex) {
    return doWithTargetAndReturn(target -> target
        .findKeySuggestionsForQueryPrefix(module, fileType, matchesRootTillMe, numOfAncestors,
            querySegmentPrefixes, querySegmentPrefixStartIndex), null);
  }

  @Nullable
  @Override
  public SortedSet<Suggestion> findValueSuggestionsForPrefix(Module module, FileType fileType,
      List<SuggestionNode> matchesRootTillMe, String prefix) {
    return doWithTargetAndReturn(
        target -> target.findValueSuggestionsForPrefix(module, fileType, matchesRootTillMe, prefix),
        null);
  }

  @Nullable
  @Override
  public String getDocumentationForValue(Module module, String nodeNavigationPathDotDelimited,
      String value) {
    return doWithTargetAndReturn(
        target -> target.getDocumentationForValue(module, nodeNavigationPathDotDelimited, value),
        null);
  }

  @Override
  public boolean isLeaf(Module module) {
    return doWithTargetAndReturn(target -> target.isLeaf(module), true);
  }

  @NotNull
  @Override
  public SuggestionNodeType getSuggestionNodeType(Module module) {
    return doWithTargetAndReturn(ClassMetadata::getSuggestionNodeType, UNKNOWN_CLASS);
  }

  @Nullable
  @Override
  public PsiType getPsiType(Module module) {
    return doWithTargetAndReturn(ClassMetadata::getPsiType, null);
  }

  @Override
  public boolean targetRepresentsArray() {
    return false;
  }

  @Override
  public boolean targetClassRepresentsIterable(Module module) {
    return doWithTargetAndReturn(IterableClassMetadata.class::isInstance, false);
  }

  //  @Override
  //  public void refreshMetadata(Module module) {
  //    doWithTarget(target -> target.refreshMetadata(module));
  //  }

  <T> T doWithTargetAndReturn(TargetInvokerWithReturnValue<T> targetInvokerWithReturnValue,
      T defaultReturnValue) {
    ClassMetadata target = getTarget();
    if (target != null) {
      return targetInvokerWithReturnValue.invoke(target);
    }
    return defaultReturnValue;
  }

  //  private void doWithTarget(TargetInvoker targetInvoker) {
  //    ClassMetadata target = getTarget();
  //    if (target != null) {
  //      targetInvoker.invoke(target);
  //    }
  //  }

  private ClassMetadata getTarget() {
    // TODO: Check if ValueHintPsiElement from spring boot can be used in anyway
    // TODO: Verify if we will have just one Map for each type of Map/we can expect multiple
    return getCachedValue(targetClass, create("spring_assistant_plugin_class_metadata"),
        () -> create(newClassMetadata(type), JAVA_STRUCTURE_MODIFICATION_COUNT));
  }


  protected interface TargetInvokerWithReturnValue<T> {
    T invoke(ClassMetadata classMetadata);
  }


  private interface TargetInvoker {
    void invoke(ClassMetadata classMetadata);
  }

}
