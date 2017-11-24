package in.oneton.idea.spring.boot.config.autosuggest.plugin.service;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import in.oneton.idea.spring.boot.config.autosuggest.plugin.model.MetadataNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

public interface SuggestionIndexService {
  static SuggestionIndexService getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, SuggestionIndexService.class);
  }

  void init(Project project) throws IOException;

  void reIndex(Project project);

  void reindex(Project project, Module module);

  @Nullable
  MetadataNode findDeepestExactMatch(Project project, List<String> containerElementsLeafToRoot);

  @Nullable
  MetadataNode findDeepestExactMatch(Project project, Module module,
      List<String> containerElements);

  boolean canProvideSuggestions(Project project);

  boolean canProvideSuggestions(Project project, Module module);

  /**
   * @param project       project to which these suggestions should be shown
   * @param ancestralKeys hierarchy of element from where the suggestion is requested. i.e if in yml user is trying to get suggestions for `s.a` under `spring:\n\trabbitmq.listener:` element, then this value would ['spring', 'rabbitmq.listener']
   * @param queryString   query string user is trying to search for. In the above example, the value for this would be `s.a`
   * @return results matching query string (without the containerElementsLeafToRoot). In the above example the values would be `simple.acknowledge-mode` & `simple.auto-startup`
   */
  @Nullable
  List<LookupElementBuilder> computeSuggestions(Project project, ClassLoader classLoader,
      @Nullable List<String> ancestralKeys, String queryString);

  /**
   * @param project       project to which these suggestions should be shown
   * @param module        module to which these suggestions should be shown
   * @param ancestralKeys hierarchy of element from where the suggestion is requested. i.e if in yml user is trying to get suggestions for `s.a` under `spring:\n\trabbitmq.listener:` element, then this value would ['spring', 'rabbitmq.listener']
   * @param queryString   query string user is trying to search for. In the above example, the value for this would be `s.a`
   * @return results matching query string (without the containerElementsLeafToRoot). In the above example the values would be `simple.acknowledge-mode` & `simple.auto-startup`
   */
  @Nullable
  List<LookupElementBuilder> computeSuggestions(Project project, Module module,
      ClassLoader classLoader, @Nullable List<String> ancestralKeys, String queryString);

}
