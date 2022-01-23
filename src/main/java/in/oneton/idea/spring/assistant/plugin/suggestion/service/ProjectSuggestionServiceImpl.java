package in.oneton.idea.spring.assistant.plugin.suggestion.service;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class ProjectSuggestionServiceImpl implements ProjectSuggestionService {
  private static final Logger log = Logger.getInstance(ProjectSuggestionServiceImpl.class);
  private final Project project;


  public ProjectSuggestionServiceImpl(@NotNull Project project) {
    this.project = project;
  }

  @Override
  public void reindex() {
    debug(() -> log.debug("-> Indexing requested for all modules of project " + project.getName()));
    reindex(ModuleManager.getInstance(project).getModules());
  }

  @Override
  public void reindex(Module[] modules) {
    debug(() -> log.debug("-> Indexing requested for a subset of modules of project " + project.getName()));
    for (Module module : modules) {
      module.getService(SuggestionService.class).reindex();
    }
  }

  private void debug(Runnable doWhenDebug) {
    if (log.isDebugEnabled()) {
      doWhenDebug.run();
    }
  }

}
