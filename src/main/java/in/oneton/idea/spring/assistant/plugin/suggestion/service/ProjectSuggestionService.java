package in.oneton.idea.spring.assistant.plugin.suggestion.service;

import com.intellij.openapi.module.Module;

public interface ProjectSuggestionService {
  void reindex();

  void reindex(Module[] modules);

}
