package in.oneton.idea.spring.assistant.plugin.suggestion.service;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiElement;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.suggestion.completion.FileType;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

public interface SuggestionService {
  static SuggestionService getInstance(@NotNull Module module) {
    return module.getService(SuggestionService.class);
  }

  @Nullable
  List<SuggestionNode> findMatchedNodesRootTillEnd(List<String> containerElements);

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  boolean canProvideSuggestions();

  /**
   * @param fileType                      type of file requesting suggestion
   * @param element                       element on which search is triggered. Useful for cases like identifying chioces that were already selected incase of an enum, e.t.c
   * @param ancestralKeys                 hierarchy of element from where the suggestion is requested. i.e if in yml user is trying to get suggestions for `s.a` under `spring:\n\trabbitmq.listener:` element, then this value would ['spring', 'rabbitmq.listener']
   * @param queryWithDotDelimitedPrefixes query string user is trying to search for. In the above example, the value for this would be `s.a`
   * @param siblingsToExclude             siblings to exclude from search
   * @return results matching query string (without the containerElementsLeafToRoot). In the above example the values would be `simple.acknowledge-mode` & `simple.auto-startup`
   */
  @Nullable
  List<LookupElement> findSuggestionsForQueryPrefix(FileType fileType, PsiElement element,
      @Nullable List<String> ancestralKeys,
      String queryWithDotDelimitedPrefixes, @Nullable Set<String> siblingsToExclude);

  void reindex();
}
