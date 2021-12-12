package in.oneton.idea.spring.assistant.plugin.suggestion.component;

import com.intellij.openapi.compiler.CompilationStatusListener;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.util.messages.MessageBusConnection;
import in.oneton.idea.spring.assistant.plugin.suggestion.service.ProjectSuggestionService;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.compiler.CompilerTopics.COMPILATION_STATUS;

public class BootstrapImpl implements StartupActivity {

  private static final Logger log = Logger.getInstance(BootstrapImpl.class);

  @Override
  public void runActivity(@NotNull Project project) {
    // This will trigger indexing
    ProjectSuggestionService service = project.getService(ProjectSuggestionService.class);

    try {
      debug(() -> log.debug("Project " + project.getName() + " is opened, indexing will start"));
      service.reindex();
    } finally {
      debug(() -> log.debug("Indexing complete for project " + project.getName()));
    }

    try {
      debug(() -> log.debug("Subscribing to compilation events for project " + project.getName()));
      MessageBusConnection connection = project.getMessageBus().connect();
      connection.subscribe(COMPILATION_STATUS, new CompilationStatusListener() {
        @Override
        public void compilationFinished(boolean aborted, int errors, int warnings,
            @NotNull CompileContext compileContext) {
          debug(() -> log.debug("Received compilation status event for project " + project.getName()));
          if (errors == 0) {
            service.reindex(compileContext.getCompileScope().getAffectedModules());
            debug(() -> log.debug("Compilation status processed for project " + project.getName()));
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
