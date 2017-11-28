package in.oneton.idea.spring.boot.config.autosuggest.plugin.component;

import com.intellij.openapi.compiler.CompilationStatusListener;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import in.oneton.idea.spring.boot.config.autosuggest.plugin.service.SuggestionIndexService;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static com.intellij.openapi.compiler.CompilerTopics.COMPILATION_STATUS;

public class BootstrapImpl implements Bootstrap, ProjectComponent {

  private static final Logger log = Logger.getInstance(BootstrapImpl.class);

  private final Project project;
  private MessageBusConnection connection;

  public BootstrapImpl(Project project) {
    this.project = project;
  }

  @NotNull
  @Override
  public String getComponentName() {
    return "Compilation event subscriber";
  }

  @Override
  public void projectOpened() {
    // This will trigger indexing
    SuggestionIndexService service =
        ServiceManager.getService(project, SuggestionIndexService.class);

    try {
      service.init(project);
    } catch (IOException e) {
      log.error(e);
    }
    connection = project.getMessageBus().connect();
    connection.subscribe(COMPILATION_STATUS, new CompilationStatusListener() {
      @Override
      public void compilationFinished(boolean aborted, int errors, int warnings,
          CompileContext compileContext) {
        if (errors == 0) {
          CompileScope projectCompileScope = compileContext.getProjectCompileScope();
          CompileScope compileScope = compileContext.getCompileScope();
          if (projectCompileScope == compileScope) {
            service.reIndex(project);
          } else {
            service.reindex(project, compileContext.getCompileScope().getAffectedModules());
          }
        }
      }

      @Override
      public void automakeCompilationFinished(int errors, int warnings,
          CompileContext compileContext) {

      }

      @Override
      public void fileGenerated(String outputRoot, String relativePath) {

      }
    });
  }

  @Override
  public void projectClosed() {
    // TODO: Need to remove current project from index
    connection.disconnect();
  }

  @Override
  public void initComponent() {
  }

  @Override
  public void disposeComponent() {
  }
}
