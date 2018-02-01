package in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz;

import com.intellij.openapi.module.Module;
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

// TODO make this guy just a forwarder, not a suggestion node


/**
 * Since the class documentation is tied to the actual class being available in the classpath of the target project the plugin is operating on,
 * it is possible that the target class is no longer available in the classpath due to change of dependencies. So, lets always access target via the proxy so that we dont have to worry about whether the target class exists in classpath or not
 */
public interface MetadataProxy {

  @Nullable
  SuggestionDocumentationHelper findDirectChild(Module module, String pathSegment);

  @Nullable
  Collection<? extends SuggestionDocumentationHelper> findDirectChildrenForQueryPrefix(
      Module module, String querySegmentPrefix);

  @Nullable
  List<SuggestionNode> findDeepestSuggestionNode(Module module,
      List<SuggestionNode> matchesRootTillParentNode, String[] pathSegments,
      int pathSegmentStartIndex);

  @Nullable
  SortedSet<Suggestion> findKeySuggestionsForQueryPrefix(Module module,
      @Nullable String ancestralKeysDotDelimited, List<SuggestionNode> matchesRootTillMe,
      String[] querySegmentPrefixes, int querySegmentPrefixStartIndex);

  @Nullable
  SortedSet<Suggestion> findValueSuggestionsForPrefix(Module module,
      List<SuggestionNode> matchesRootTillMe, String prefix);

  @Nullable
  String getDocumentationForValue(Module module, String nodeNavigationPathDotDelimited,
      String value);

  boolean isLeaf(Module module);

  @NotNull
  SuggestionNodeType getSuggestionNodeType(Module module);

  @Nullable
  PsiType getPsiType(Module module);

  boolean targetRepresentsArray();

  boolean targetClassRepresentsIterable(Module module);

  // TODO: Since Object graphs can have infinite loops, need to find a mechanism (adding state like refreshAttempt that is a monotonically increasing number) to mark a each object in the graph in the current run, so that we dont enter infinite loop. Fix this later
  //  /**
  //   * Will be called after every build completion event, to refresh metadata so that it is upto date wrt underlying classpath
  //   *
  //   * @param module module
  //   */
  //  void refreshMetadata(Module module);

}
