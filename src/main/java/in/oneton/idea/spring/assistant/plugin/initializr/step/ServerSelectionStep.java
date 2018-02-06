package in.oneton.idea.spring.assistant.plugin.initializr.step;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.TextFieldWithStoredHistory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.util.ui.FormBuilder;
import in.oneton.idea.spring.assistant.plugin.initializr.InitializrModuleBuilder;
import in.oneton.idea.spring.assistant.plugin.initializr.ProjectCreationRequest;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.net.URI;
import java.net.URISyntaxException;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public class ServerSelectionStep extends ModuleWizardStep {

  private static final int GAP_BETWEEN_COMPONENTS = 5;
  private static final String SPRING_IO_INITIALIZR_SERVER_URL = "https://start.spring.io";

  private final ProjectCreationRequest request;

  private JBRadioButton defaultRadioButton = new JBRadioButton("Default:", true);
  private JBRadioButton customRadioButton = new JBRadioButton("Custom:", false);
  private TextFieldWithStoredHistory customInitializrServerUrlTextField =
      new TextFieldWithStoredHistory("spring.assistant.initializr.custom.server.url.history");

  public ServerSelectionStep(InitializrModuleBuilder moduleBuilder) {
    request = moduleBuilder.safeGetProjectCreationRequest();
    init();
  }

  /**
   * Should handle both forward & backward navigation
   */
  private void init() {
    if (isEmpty(request.getServerUrl()) || SPRING_IO_INITIALIZR_SERVER_URL
        .equals(request.getServerUrl())) {
      defaultRadioButton.setSelected(true);
      customRadioButton.setSelected(false);

      // Incase if the user has selected spring.io in custom url while moving forward, lets simply remove that from history
      customInitializrServerUrlTextField.getHistory().remove(SPRING_IO_INITIALIZR_SERVER_URL);
    } else {
      defaultRadioButton.setSelected(false);
      customRadioButton.setSelected(true);
      customInitializrServerUrlTextField.setText(request.getServerUrl());
      customInitializrServerUrlTextField.requestFocus();
    }
  }

  @Override
  public JComponent getComponent() {
    FormBuilder formBuilder = new FormBuilder();
    formBuilder.addComponent(new JBLabel("Choose Spring Initializr sever url"));
    formBuilder.addVerticalGap(GAP_BETWEEN_COMPONENTS);
    JPanel defaultPanel = newDefaultChoicePanel();
    formBuilder.addComponent(defaultPanel);
    formBuilder.addVerticalGap(GAP_BETWEEN_COMPONENTS);
    JPanel customPanel = newCustomChoicePanel();
    formBuilder.addComponent(customPanel);
    ButtonGroup group = new ButtonGroup();
    group.add(defaultRadioButton);
    group.add(customRadioButton);
    JPanel panel = new JPanel(new VerticalLayout(GAP_BETWEEN_COMPONENTS));
    panel.add(formBuilder.getPanel());
    return panel;
  }

  @Override
  public boolean validate() throws ConfigurationException {
    if (customRadioButton.isSelected()) {
      String serverUrl = customInitializrServerUrlTextField.getText();
      if (isEmpty(serverUrl)) {
        customInitializrServerUrlTextField.requestFocus();
        throw new ConfigurationException("Custom Initializr Server url must be set");
      } else if (serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
        customInitializrServerUrlTextField.requestFocus();
        throw new ConfigurationException("Custom Initializr Server URL must be a valid url");
      }
      try {
        new URI(serverUrl);
        return true;
      } catch (URISyntaxException e) {
        customInitializrServerUrlTextField.requestFocus();
        throw new ConfigurationException("Custom Initializr Server URL must be a valid url");
      }
    }
    return true;
  }

  @Override
  public void updateDataModel() {
    if (defaultRadioButton.isSelected()) {
      request.setServerUrl(SPRING_IO_INITIALIZR_SERVER_URL);
    } else {
      request.setServerUrl(customInitializrServerUrlTextField.getText());
    }
  }

  @NotNull
  private JPanel newCustomChoicePanel() {
    JPanel customPanel = new JPanel(new HorizontalLayout(GAP_BETWEEN_COMPONENTS));
    customPanel.add(customRadioButton);
    customPanel.add(customInitializrServerUrlTextField);
    customInitializrServerUrlTextField.addActionListener(e -> {
      customRadioButton.setSelected(true);
      customInitializrServerUrlTextField.requestFocus();
    });
    return customPanel;
  }

  @NotNull
  private JPanel newDefaultChoicePanel() {
    JPanel defaultPanel = new JPanel(new HorizontalLayout(GAP_BETWEEN_COMPONENTS));
    defaultPanel.add(defaultRadioButton);
    HyperlinkLabel label = new HyperlinkLabel(SPRING_IO_INITIALIZR_SERVER_URL);
    label.setHyperlinkTarget(SPRING_IO_INITIALIZR_SERVER_URL);
    defaultPanel.add(label);
    return defaultPanel;
  }

}
