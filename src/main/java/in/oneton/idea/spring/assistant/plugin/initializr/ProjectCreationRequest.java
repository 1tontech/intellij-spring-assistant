package in.oneton.idea.spring.assistant.plugin.initializr;

import gnu.trove.THashSet;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.DependencyComposite.DependencyGroup.Dependency;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.IdAndName;
import lombok.Data;

import java.util.Set;

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
  private IdAndName language;
  private IdAndName javaVersion;
  private IdAndName packaging;
  private IdAndName bootVersion;
  private Set<Dependency> dependencies = new THashSet<>();

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
