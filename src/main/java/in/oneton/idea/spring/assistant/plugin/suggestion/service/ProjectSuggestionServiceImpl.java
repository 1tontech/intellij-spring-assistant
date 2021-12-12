package in.oneton.idea.spring.assistant.plugin.suggestion.service;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.apache.commons.lang.time.StopWatch;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Future;

import static com.intellij.openapi.application.ApplicationManager.getApplication;

public class ProjectSuggestionServiceImpl implements ProjectSuggestionService {
  private static final Logger log = Logger.getInstance(ProjectSuggestionServiceImpl.class);
  private final Project project;

  private Future<?> currentExecution;
  private volatile boolean indexingInProgress;

  public ProjectSuggestionServiceImpl(@NotNull Project project) {
    this.project = project;
  }

  @Override
  public void reindex() {
    reindex(ModuleManager.getInstance(project).getModules());
  }

  @Override
  public void reindex(Module[] modules) {
    if (indexingInProgress) {
      if (currentExecution != null) {
        currentExecution.cancel(false);
      }
    }
    currentExecution = getApplication().executeOnPooledThread(() -> {
      VirtualFileManager.getInstance().asyncRefresh(() ->
          DumbService.getInstance(project).runReadActionInSmartMode(() -> {
            debug(() -> log.debug("-> Indexing requested for a subset of modules of project " + project.getName()));
            indexingInProgress = true;
            StopWatch timer = new StopWatch();
            timer.start();
            try {
              for (Module module : modules) {
                debug(() -> log.debug("--> Indexing requested for module " + module.getName()));
                StopWatch moduleTimer = new StopWatch();
                moduleTimer.start();
                try {
                  module.getService(SuggestionService.class).reindex();
                } finally {
                  moduleTimer.stop();
                  debug(() -> log.debug("<-- Indexing took " + moduleTimer + " for module " + module.getName()));
                }
              }
            } finally {
              indexingInProgress = false;
              timer.stop();
              debug(() -> log.debug("<- Indexing took " + timer + " for project " + project.getName()));
            }
          }));
    });
  }

  private void debug(Runnable doWhenDebug) {
    if (log.isDebugEnabled()) {
      doWhenDebug.run();
    }
  }

}
