package in.oneton.idea.spring.assistant.plugin.initializr.step;

import com.intellij.ui.CollectionComboBoxModel;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.IdAndName;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.IdAndNameComposite;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.ProjectTypeComposite;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.ProjectTypeComposite.ProjectType;

import javax.swing.*;

import static in.oneton.idea.spring.assistant.plugin.initializr.misc.FormUtil.newCollectionComboBoxModel;

public class ProjectDetailsForm {

  private JPanel rootPanel;
  private JTextField groupId;
  private JTextField artifactId;
  private JTextField projectName;
  private JTextField projectDescription;
  private JTextField packageName;
  private JComboBox<IdAndName> packagingType;
  private JComboBox<IdAndName> javaVersion;
  private JComboBox<ProjectType> projectType;
  private JComboBox<IdAndName> projectLanguage;
  private JTextField projectVersion;

  private JLabel groupIdLabel;
  private JLabel artifactIdLabel;
  private JLabel projectNameLabel;
  private JLabel projectDescriptionLabel;
  private JLabel packageNameLabel;
  private JLabel packagingTypeLabel;
  private JLabel javaVersionLabel;
  private JLabel projectTypeLabel;
  private JLabel projectLanguageLabel;
  private JLabel projectVersionLabel;

  public void init(InitializerMetadata metadata) {
    groupId.setText(metadata.getGroupIdHolder().getDefaultValue());
    artifactId.setText(metadata.getArtifactIdHolder().getDefaultValue());
    projectName.setText(metadata.getNameHolder().getDefaultValue());
    projectDescription.setText(metadata.getDescriptionHolder().getDefaultValue());
    packageName.setText(metadata.getPackageNameHolder().getDefaultValue());
    projectVersion.setText(metadata.getVersionHolder().getDefaultValue());

    IdAndNameComposite packagingTypeComposite = metadata.getPackagingTypeComposite();
    CollectionComboBoxModel<IdAndName> packagingTypeComboBoxModel =
        newCollectionComboBoxModel(packagingTypeComposite.getValues(),
            packagingTypeComposite.getDefaultValue());
    packagingType.setModel(packagingTypeComboBoxModel);

    IdAndNameComposite javaVersionComposite = metadata.getJavaVersionComposite();
    CollectionComboBoxModel<IdAndName> javaVersionComboBoxModel =
        newCollectionComboBoxModel(javaVersionComposite.getValues(),
            javaVersionComposite.getDefaultValue());
    javaVersion.setModel(javaVersionComboBoxModel);

    IdAndNameComposite languageComposite = metadata.getLanguageComposite();
    CollectionComboBoxModel<IdAndName> projectLanguageComboBoxModel =
        newCollectionComboBoxModel(languageComposite.getValues(),
            languageComposite.getDefaultValue());
    projectLanguage.setModel(projectLanguageComboBoxModel);

    ProjectTypeComposite projectTypeComposite = metadata.getProjectTypeComposite();
    CollectionComboBoxModel<ProjectType> projectTypeComboBoxModel =
        newCollectionComboBoxModel(projectTypeComposite.getTypes(),
            projectTypeComposite.getDefaultValue());
    projectType.setModel(projectTypeComboBoxModel);
  }

  public JPanel getRoot() {
    return rootPanel;
  }

}
