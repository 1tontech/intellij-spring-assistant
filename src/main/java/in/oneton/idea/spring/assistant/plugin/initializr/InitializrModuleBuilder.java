package in.oneton.idea.spring.assistant.plugin.initializr;

import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.LanguageLevelModuleExtension;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.pom.java.LanguageLevel;
import in.oneton.idea.spring.assistant.plugin.initializr.misc.Icons;
import in.oneton.idea.spring.assistant.plugin.initializr.step.ProjectDetailsStep;
import in.oneton.idea.spring.assistant.plugin.initializr.step.ServerSelectionStep;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class InitializrModuleBuilder extends ModuleBuilder {

  public static final String CONTENT_TYPE = "application/vnd.initializr.v2.1+json";

  private ProjectCreationRequest request;

  @Override
  public void setupRootModel(ModifiableRootModel modifiableRootModel) {
    Sdk sdk = this.getModuleJdk() != null ?
        this.getModuleJdk() :
        ProjectRootManager.getInstance(modifiableRootModel.getProject()).getProjectSdk();
    if (sdk != null) {
      modifiableRootModel.setSdk(sdk);
    }

    LanguageLevelModuleExtension moduleExt =
        modifiableRootModel.getModuleExtension(LanguageLevelModuleExtension.class);
    if (moduleExt != null && sdk != null) {
      if (safeGetProjectCreationRequest().isJavaVersionSet()) {
        LanguageLevel parse =
            LanguageLevel.parse(safeGetProjectCreationRequest().getJavaVersion().getId());
        if (parse != null) {
          JavaSdkVersion javaSdkVersion = JavaSdkVersion.fromLanguageLevel(parse);
          JavaSdkVersion sdkJavaVersion = JavaSdk.getInstance().getVersion(sdk);
          if (sdkJavaVersion != null && sdkJavaVersion.isAtLeast(javaSdkVersion)) {
            moduleExt.setLanguageLevel(parse);
          }
        }
      }
    }

    this.doAddContentEntry(modifiableRootModel);
  }

  @Nullable
  public ModuleWizardStep modifySettingsStep(@NotNull SettingsStep settingsStep) {
    JTextField moduleNameField = settingsStep.getModuleNameField();
    if (moduleNameField != null) {
      moduleNameField.setText(this.request.getArtifactId());
    }

    return super.modifySettingsStep(settingsStep);
  }

  public ModuleWizardStep[] createWizardSteps(@NotNull WizardContext wizardContext,
      @NotNull ModulesProvider modulesProvider) {
    return new ModuleWizardStep[] {new ProjectDetailsStep(this, wizardContext)};
    //      , new InitializrDependencySelectionStep(this)
  }

  @Nullable
  public ModuleWizardStep getCustomOptionsStep(WizardContext context, Disposable parentDisposable) {
    return new ServerSelectionStep(this);
  }

  @Override
  public Icon getNodeIcon() {
    return Icons.SpringBoot;
  }

  @Nullable
  @Override
  public String getBuilderId() {
    return "Spring Boot/Cloud Initializr";
  }

  @Override
  public String getDescription() {
    return "Bootstrap spring applications using <b>Spring Boot</b> & <b>Spring Cloud</b> starters";
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
    return StdModuleTypes.JAVA;
  }

  public ProjectCreationRequest safeGetProjectCreationRequest() {
    if (request == null) {
      request = new ProjectCreationRequest();
    }
    return request;
  }
}
