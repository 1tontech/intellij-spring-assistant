package in.oneton.idea.spring.assistant.plugin.initializr;

import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.DependencyComposite.DependencyGroup.Dependency;
import lombok.Data;

import java.util.List;

@Data
public class ProjectCreationRequest {

  private String serverUrl;
  private InitializerMetadata metadata;

  private String groupId;
  private String artifactId;
  private String version;
  private String name;
  private String description;
  private String packageName;
  private InitializerMetadata.IdAndName language;
  private InitializerMetadata.IdAndName javaVersion;
  private InitializerMetadata.IdAndName packaging;
  private InitializerMetadata.IdAndName bootVersion;
  private List<Dependency> dependencies;

  public void setServerUrl(String serverUrl) {
    if (!serverUrl.equals(this.serverUrl)) {
      this.serverUrl = serverUrl;
      metadata = null;
    }
  }

  public boolean isJavaVersionSet() {
    return javaVersion != null;
  }

}
