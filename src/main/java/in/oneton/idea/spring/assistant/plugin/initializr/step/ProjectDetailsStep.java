package in.oneton.idea.spring.assistant.plugin.initializr.step;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.ui.components.JBLoadingPanel;
import in.oneton.idea.spring.assistant.plugin.initializr.InitializrModuleBuilder;
import in.oneton.idea.spring.assistant.plugin.initializr.ProjectCreationRequest;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.DependencyComposite.DependencyGroup.Dependency.DependencyLinksContainer.DependencyLink;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.io.spring.initializr.util.VersionRange;
import in.oneton.idea.spring.assistant.plugin.initializr.misc.DependencyOneOrMoreDeserializer;
import in.oneton.idea.spring.assistant.plugin.initializr.misc.VersionRangeDeserializer;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import static com.intellij.openapi.application.ApplicationManager.getApplication;
import static com.intellij.openapi.ui.Messages.showErrorDialog;
import static com.intellij.ui.ScrollPaneFactory.createScrollPane;
import static com.intellij.util.io.HttpRequests.createErrorMessage;
import static com.intellij.util.io.HttpRequests.request;
import static in.oneton.idea.spring.assistant.plugin.initializr.InitializrModuleBuilder.CONTENT_TYPE;
import static javax.swing.SwingUtilities.invokeLater;

public class ProjectDetailsStep extends ModuleWizardStep implements Disposable {

  private static final Type DEPENDENCY_LINKS_LIST_TYPE = new TypeToken<List<DependencyLink>>() {
  }.getType();

  private final JBLoadingPanel loadingPanel = new JBLoadingPanel(new BorderLayout(), this, 300);
  private final ProjectCreationRequest request;

  public ProjectDetailsStep(InitializrModuleBuilder moduleBuilder, WizardContext wizardContext) {
    request = moduleBuilder.safeGetProjectCreationRequest();
  }

  @Override
  public void _init() {
    if (request.getMetadata() == null) {
      loadingPanel.getContentPanel().removeAll();
      loadingPanel.startLoading();
      // Lets fetch metadata on a background thread, so as not to block UI thread
      getApplication().executeOnPooledThread(() -> {
        try {
          InitializerMetadata metadata =
              request(request.getServerUrl()).accept(CONTENT_TYPE).userAgent(userAgent())
                  .connect(httpRequest -> {
                    invokeLater(() -> loadingPanel.setLoadingText(
                        "Please wait.. Spring Initializr options are being loaded.."));
                    BufferedReader reader;
                    try {
                      reader = httpRequest.getReader();
                    } catch (IOException e) {
                      throw new IOException(createErrorMessage(e, httpRequest, false), e);
                    }

                    GsonBuilder builder = new GsonBuilder();
                    builder.registerTypeAdapter(VersionRange.class, new VersionRangeDeserializer());
                    builder.registerTypeAdapter(DEPENDENCY_LINKS_LIST_TYPE,
                        new DependencyOneOrMoreDeserializer());
                    return builder.create().fromJson(reader, InitializerMetadata.class);
                  });
          request.setMetadata(metadata);
          invokeLater(() -> {
            ProjectDetailsForm detailsForm = new ProjectDetailsForm();
            detailsForm.init(metadata);
            loadingPanel.add(createScrollPane(detailsForm.getRoot(), true), "North");
          });
        } catch (IOException e) {
          invokeLater(() -> showErrorDialog("Initialization failed for '" + request.getServerUrl()
              + "'\nPlease check URL, network and proxy settings.\n\nError message:\n" + e
              .getMessage(), "Spring Initializr Error"));
        } catch (JsonSyntaxException | JsonIOException e) {
          invokeLater(() -> showErrorDialog(
              "Error while parsing metadata from server '" + request.getServerUrl()
                  + "'\nMake sure you connected to the correct initializr server."
                  + "\n\nError message:\n" + e.getMessage(), "Spring Initializr Error"));
        } finally {
          invokeLater(() -> {
            loadingPanel.stopLoading();
            loadingPanel.revalidate();
          });
        }
      });
    }
  }

  private String userAgent() {
    return ApplicationNamesInfo.getInstance().getFullProductName() + "/" + ApplicationInfo
        .getInstance().getFullVersion();
  }

  @Override
  public JComponent getComponent() {
    return loadingPanel;
  }

  @Override
  public boolean validate() {
    // TODO: validate
    return true;
  }

  @Override
  public void updateDataModel() {
    // TODO: update request
  }

  @Override
  public void dispose() {

  }

}
