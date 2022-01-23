package in.oneton.idea.spring.assistant.plugin.suggestion.component;

import com.intellij.openapi.compiler.CompilationStatusListener;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.messages.MessageBusConnection;
import in.oneton.idea.spring.assistant.plugin.suggestion.service.ProjectSuggestionService;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.compiler.CompilerTopics.COMPILATION_STATUS;

public class BootstrapImpl implements StartupActivity.Background {

  private static final Logger log = Logger.getInstance(BootstrapImpl.class);

  @Override
  public void runActivity(@NotNull Project project) {
    ProgressManager.getInstance().run(
        new Task.Backgroundable(project, "Index spring configuration metadata") {
          @Override
          public void run(@NotNull ProgressIndicator indicator) {
            DumbService.getInstance(project).runReadActionInSmartMode(() -> {
              debug(() -> log.debug("Project " + project.getName() + " is opened, indexing will start"));
              try {
                project.getService(ProjectSuggestionService.class).reindex();
              } finally {
                debug(() -> log.debug("Indexing complete for project " + project.getName()));
              }
            });
          }
        }
    );

    try {
      debug(() -> log.debug("Subscribing to compilation events for project " + project.getName()));
      MessageBusConnection connection = project.getMessageBus().connect();
      // TODO Use com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener instead?
      connection.subscribe(COMPILATION_STATUS, new CompilationStatusListener() {
        @Override
        public void compilationFinished(boolean aborted, int errors, int warnings,
            @NotNull CompileContext compileContext) {
          debug(() -> log.debug("Received compilation status event for project " + project.getName()));
          if (errors == 0) {
            VirtualFileManager.getInstance().asyncRefresh(() -> {
              project.getService(ProjectSuggestionService.class)
                     .reindex(compileContext.getCompileScope().getAffectedModules());
              debug(() -> log.debug("Compilation status processed for project " + project.getName()));
            });
          } else {
            debug(() -> log.debug("Skipping reindexing completely as there are " + errors + " errors"));
          }
        }
      });
      debug(() -> log.debug("Subscribe to compilation events for project " + project.getName()));
    } catch (Throwable e) {
      log.error("Failed to subscribe to compilation events for project " + project.getName(), e);
    }
  }

  /**
   * Debug logging can be enabled by adding fully classified class name/package name with # prefix
   * For eg., to enable debug logging, go `Help > Debug log settings` & type `#in.oneton.idea.spring.assistant.plugin.suggestion.service.SuggestionServiceImpl`
   *
   * @param doWhenDebug code to execute when debug is enabled
   */
  private void debug(Runnable doWhenDebug) {
    if (log.isDebugEnabled()) {
      doWhenDebug.run();
    }
  }

}
