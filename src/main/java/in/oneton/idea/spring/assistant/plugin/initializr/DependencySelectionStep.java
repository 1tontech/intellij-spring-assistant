package in.oneton.idea.spring.assistant.plugin.initializr;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import in.oneton.idea.spring.assistant.plugin.initializr.step.DependencySelection;

import javax.swing.*;
import java.awt.*;

public class DependencySelectionStep extends ModuleWizardStep {

  private final ProjectCreationRequest request;
  private JPanel rootPanel;

  DependencySelectionStep(InitializrModuleBuilder moduleBuilder) {
    request = moduleBuilder.safeGetProjectCreationRequest();
    rootPanel = new JPanel();
    rootPanel.setLayout(new BorderLayout());
  }

  @Override
  public void _init() {
    rootPanel.removeAll();
    DependencySelection dependencySelection = new DependencySelection();
    dependencySelection.init(request);
    rootPanel.add(dependencySelection.getRootPanel());
    rootPanel.revalidate();
  }

  @Override
  public JComponent getComponent() {
    return rootPanel;
  }

  @Override
  public void updateDataModel() {

  }

}
