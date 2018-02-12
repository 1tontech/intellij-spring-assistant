package in.oneton.idea.spring.assistant.plugin.initializr;

import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.LanguageLevelModuleExtension;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.pom.java.LanguageLevel;
import in.oneton.idea.spring.assistant.plugin.initializr.misc.Icons;
import in.oneton.idea.spring.assistant.plugin.initializr.step.ProjectDetailsStep;
import in.oneton.idea.spring.assistant.plugin.initializr.step.ServerSelectionStep;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;

import static com.intellij.openapi.application.ApplicationManager.getApplication;
import static com.intellij.openapi.application.ModalityState.current;
import static com.intellij.openapi.module.StdModuleTypes.JAVA;
import static com.intellij.openapi.projectRoots.JavaSdk.getInstance;
import static com.intellij.openapi.projectRoots.JavaSdkVersion.fromLanguageLevel;
import static com.intellij.openapi.roots.ProjectRootManager.getInstance;
import static com.intellij.openapi.ui.Messages.showErrorDialog;
import static com.intellij.pom.java.LanguageLevel.parse;

public class InitializrModuleBuilder extends ModuleBuilder {

  public static final String CONTENT_TYPE = "application/vnd.initializr.v2.1+json";

  private ProjectCreationRequest request;

  @Override
  public void setupRootModel(ModifiableRootModel modifiableRootModel) {
    Sdk moduleOrProjectSdk = getModuleJdk() != null ?
        getModuleJdk() :
        getInstance(modifiableRootModel.getProject()).getProjectSdk();
    if (moduleOrProjectSdk != null) {
      modifiableRootModel.setSdk(moduleOrProjectSdk);
    }

    LanguageLevelModuleExtension languageLevelModuleExtension =
        modifiableRootModel.getModuleExtension(LanguageLevelModuleExtension.class);
    if (languageLevelModuleExtension != null && moduleOrProjectSdk != null) {
      if (safeGetProjectCreationRequest().isJavaVersionSet()) {
        LanguageLevel lastSelectedLanguageLevel =
            parse(safeGetProjectCreationRequest().getJavaVersion().getId());
        if (lastSelectedLanguageLevel != null) {
          JavaSdkVersion lastSelectedJavaSdkVersion = fromLanguageLevel(lastSelectedLanguageLevel);
          JavaSdkVersion moduleOrProjectLevelJavaSdkVersion =
              getInstance().getVersion(moduleOrProjectSdk);
          if (moduleOrProjectLevelJavaSdkVersion != null && moduleOrProjectLevelJavaSdkVersion
              .isAtLeast(lastSelectedJavaSdkVersion)) {
            languageLevelModuleExtension.setLanguageLevel(lastSelectedLanguageLevel);
          }
        }
      }
    }

    doAddContentEntry(modifiableRootModel);
  }

  @Nullable
  public ModuleWizardStep modifySettingsStep(@NotNull SettingsStep settingsStep) {
    JTextField moduleNameField = settingsStep.getModuleNameField();
    if (moduleNameField != null) {
      moduleNameField.setText(request.getArtifactId());
    }

    return super.modifySettingsStep(settingsStep);
  }

  public ModuleWizardStep[] createWizardSteps(@NotNull WizardContext wizardContext,
      @NotNull ModulesProvider modulesProvider) {
    return new ModuleWizardStep[] {new ProjectDetailsStep(this, wizardContext),
        new DependencySelectionStep(this)};
  }

  @Nullable
  public ModuleWizardStep getCustomOptionsStep(WizardContext context, Disposable parentDisposable) {
    return new ServerSelectionStep(this);
  }

  @NotNull
  @Override
  public Module createModule(@NotNull ModifiableModuleModel moduleModel)
      throws InvalidDataException, IOException, ModuleWithNameAlreadyExists, JDOMException,
      ConfigurationException {
    Module module = super.createModule(moduleModel);
    getApplication().invokeLater(() -> {
      ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
        try {
          InitializerDownloader downloader = new InitializerDownloader(this);
          downloader.execute(ProgressManager.getInstance().getProgressIndicator());
        } catch (IOException var2) {
          getApplication()
              .invokeLater(() -> showErrorDialog("Error: " + var2.getMessage(), "Creation Failed"));
        }
      }, "Downloading Required Files...", true, null);
      ModuleBuilderPostProcessor[] postProcessors =
          ModuleBuilderPostProcessor.EXTENSION_POINT_NAME.getExtensions();
      for (ModuleBuilderPostProcessor postProcessor : postProcessors) {
        if (!postProcessor.postProcess(module)) {
          return;
        }
      }
    }, current());
    return module;
  }

  @Override
  public Icon getNodeIcon() {
    return Icons.SpringBoot;
  }

  @Nullable
  @Override
  public String getBuilderId() {
    return "Spring Boot/Cloud Dataflow Initializr";
  }

  @Override
  public String getDescription() {
    return "Bootstrap spring applications using <b>Spring Boot</b> & <b>Spring Cloud Dataflow</b> starters";
  }

  @Override
  public String getPresentableName() {
    return "Spring Assistant";
  }

  public String getParentGroup() {
    return "Build Tools";
  }

  @Override
  public ModuleType getModuleType() {
    return JAVA;
  }

  public ProjectCreationRequest safeGetProjectCreationRequest() {
    if (request == null) {
      request = new ProjectCreationRequest();
    }
    return request;
  }
}
