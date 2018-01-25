package in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz;

import com.intellij.psi.PsiClass;
import in.oneton.idea.spring.assistant.plugin.SuggestionDocumentationHelper;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.SortedSet;

import static com.intellij.psi.util.CachedValueProvider.Result.create;
import static com.intellij.psi.util.CachedValuesManager.getCachedValue;
import static com.intellij.psi.util.PsiModificationTracker.JAVA_STRUCTURE_MODIFICATION_COUNT;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType.UNKNOWN_CLASS;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz.ClassSuggestionNodeFactory.newInstance;

/**
 * Since the class documentation is tied to the actual class being available in the classpath of the target project the plugin is operating on,
 * it is possible that the target class is no longer available in the classpath due to change of dependencies. So, lets always access target via the proxy so that we dont have to worry about whether the target class exists in classpath or not
 */
public class ClassSuggestionNodeProxy implements SuggestionNode {

  private final PsiClass targetClazz;
  private final SuggestionDocumentationHelper helper;

  public ClassSuggestionNodeProxy(PsiClass targetClazz, SuggestionDocumentationHelper helper) {
    this.targetClazz = targetClazz;
    this.helper = helper;
  }

  @Override
  public SuggestionNode findDeepestMatch(String[] pathSegments, int pathSegmentStartIndex) {
    return doWithTargetAndReturn(
        target -> target.findDeepestMatch(pathSegments, pathSegmentStartIndex), null);
  }

  @Nullable
  @Override
  public List<SuggestionNode> findDeepestMatch(List<SuggestionNode> matchesRootTillParentNode,
      String[] pathSegments, int pathSegmentStartIndex) {
    return doWithTargetAndReturn(target -> target
        .findDeepestMatch(matchesRootTillParentNode, pathSegments, pathSegmentStartIndex), null);
  }

  @Nullable
  @Override
  public SortedSet<Suggestion> findSuggestionsForKey(@Nullable String ancestralKeysDotDelimited,
      List<SuggestionNode> matchesRootTillParentNode, String[] querySegmentPrefixes,
      int querySegmentPrefixStartIndex) {
    return doWithTargetAndReturn(target -> target
        .findSuggestionsForKey(ancestralKeysDotDelimited, matchesRootTillParentNode,
            querySegmentPrefixes, querySegmentPrefixStartIndex), null);
  }

  @Override
  public boolean supportsDocumentation() {
    return doWithTargetAndReturn(ClassSuggestionNode::supportsDocumentation, false);
  }

  @Nullable
  @Override
  public String getOriginalName() {
    return helper != null ? helper.getOriginalName() : null;
  }

  //  @Nullable
  //  @Override
  //  public String getNameForDocumentation() {
  //    return doWithTargetAndReturn(ClassSuggestionNode::getNameForDocumentation, null);
  //  }

  //  @Nullable
  //  @Override
  //  public String getDocumentationForKey(String nodeNavigationPathDotDelimited) {
  //    return doWithTargetAndReturn(
  //        target -> target.getDocumentationForKey(nodeNavigationPathDotDelimited), null);
  //  }

  @Nullable
  @Override
  public SortedSet<Suggestion> findSuggestionsForValue(String prefix) {
    return doWithTargetAndReturn(target -> target.findSuggestionsForValue(prefix), null);
  }

  @Nullable
  @Override
  public String getDocumentationForValue(String nodeNavigationPathDotDelimited, String value) {
    return doWithTargetAndReturn(
        target -> target.getDocumentationForValue(nodeNavigationPathDotDelimited, value), null);
  }

  @Override
  public boolean isLeaf() {
    return doWithTargetAndReturn(ClassSuggestionNode::isLeaf, true);
  }

  @NotNull
  @Override
  public SuggestionNodeType getType() {
    return doWithTargetAndReturn(ClassSuggestionNode::getType, UNKNOWN_CLASS);
  }

  public boolean hasChildren() {
    return doWithTargetAndReturn(ClassSuggestionNode::hasChildren, false);
  }

  private ClassSuggestionNode getTarget() {
    if (targetClazz != null) {
      return getCachedValue(targetClazz,
          () -> create(newInstance(targetClazz), JAVA_STRUCTURE_MODIFICATION_COUNT));
    }
    return null;
  }

  private <T> T doWithTargetAndReturn(TargetInvoker<T> targetInvoker, T defaultReturnValue) {
    ClassSuggestionNode target = getTarget();
    if (target != null) {
      return targetInvoker.invoke(target);
    }
    return defaultReturnValue;
  }

  private interface TargetInvoker<T> {
    T invoke(ClassSuggestionNode classSuggestionNode);
  }
}
