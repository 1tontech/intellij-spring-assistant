package in.oneton.idea.spring.assistant.plugin.initializr;

import com.intellij.ide.util.newProjectWizard.AddModuleWizard;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.plugins.gradle.service.project.wizard.GradleProjectImportBuilder;
import org.jetbrains.plugins.gradle.service.project.wizard.GradleProjectImportProvider;

import static com.intellij.openapi.components.ServiceManager.getService;
import static in.oneton.idea.spring.assistant.plugin.util.PsiCustomUtil.findFileUnderRootInModule;

public class GradleModuleBuilderPostProcessor implements ModuleBuilderPostProcessor {
  @Override
  public boolean postProcess(Module module) {
    // TODO: Find a way to use GradleModuleBuilder instead of GradleProjectImportBuilder when adding a child module to the parent
    Project project = module.getProject();
    VirtualFile gradleFile = findFileUnderRootInModule(module, "build.gradle");
    if (gradleFile == null) { // not a gradle project
      return true;
    } else {
      ProjectDataManager projectDataManager = getService(ProjectDataManager.class);
      GradleProjectImportBuilder importBuilder = new GradleProjectImportBuilder(projectDataManager);
      GradleProjectImportProvider importProvider = new GradleProjectImportProvider(importBuilder);
      AddModuleWizard addModuleWizard =
          new AddModuleWizard(project, gradleFile.getPath(), importProvider);
      if (addModuleWizard.getStepCount() > 0 && !addModuleWizard
          .showAndGet()) { // user has cancelled import project prompt
        return true;
      } else { // user chose to import via the gradle import prompt
        importBuilder.commit(project, null, null);
        return false;
      }
    }
  }
}
