package in.oneton.idea.spring.assistant.plugin.initializr.metadata;

import com.google.gson.annotations.SerializedName;
import gnu.trove.THashSet;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.io.spring.initializr.util.Version;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.io.spring.initializr.util.VersionRange;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

@Data
public class InitializerMetadata {

  @SerializedName("dependencies")
  private DependencyComposite dependencyComposite;

  @SerializedName("type")
  private ProjectTypeComposite projectTypeComposite;

  @SerializedName("packaging")
  private IdAndNameComposite packagingTypeComposite;

  @SerializedName("javaVersion")
  private IdAndNameComposite javaVersionComposite;

  @SerializedName("language")
  private IdAndNameComposite languageComposite;

  @SerializedName("bootVersion")
  private IdAndNameComposite bootVersionComposite;

  @SerializedName("groupId")
  private DefaultValueHolder groupIdHolder;

  @SerializedName("artifactId")
  private DefaultValueHolder artifactIdHolder;

  @SerializedName("version")
  private DefaultValueHolder versionHolder;

  @SerializedName("name")
  private DefaultValueHolder nameHolder;

  @SerializedName("description")
  private DefaultValueHolder descriptionHolder;

  @SerializedName("packageName")
  private DefaultValueHolder packageNameHolder;


  public interface IdContainer {
    String getId();
  }


  @Data
  public static class DependencyComposite {
    @SerializedName("values")
    private List<DependencyGroup> groups;


    @Data
    @EqualsAndHashCode(of = "name")
    public static class DependencyGroup {
      private String name;
      @SerializedName("values")
      private List<Dependency> dependencies;

      @Override
      public String toString() {
        return name;
      }

      public Set<Integer> getIncompatibleDependencyIndexes(Version bootVersion) {
        Set<Integer> incompatibleDependencyIndexes = new THashSet<>();
        for (int i = 0; i < dependencies.size(); i++) {
          Dependency dependency = dependencies.get(i);
          VersionRange versionRange = dependency.getVersionRange();
          if (versionRange != null && !versionRange.match(bootVersion)) {
            incompatibleDependencyIndexes.add(i);
          }
        }
        return incompatibleDependencyIndexes;
      }

      @Data
      @EqualsAndHashCode(of = "id")
      public static class Dependency {
        private String id;
        private String name;
        private String description;
        @Nullable
        private VersionRange versionRange;
        @Nullable
        @SerializedName("_links")
        private DependencyLinksContainer linksContainer;


        @Data
        public static class DependencyLinksContainer {
          @Nullable
          private DependencyLink reference;
          @Nullable
          private List<DependencyLink> guide;


          @Getter
          @Setter
          @Builder
          @NoArgsConstructor
          @AllArgsConstructor
          public static class DependencyLink {
            private String href;
            @Nullable
            private String title;
          }
        }
      }
    }
  }


  @Data
  public static class ProjectTypeComposite {
    @SerializedName("default")
    private String defaultValue;
    @SerializedName("values")
    private List<ProjectType> types;


    @Data
    @EqualsAndHashCode(of = "id")
    public static class ProjectType implements IdContainer {
      private String id;
      private String name;
      private String description;
      private String action;

      @Override
      public String toString() {
        return name;
      }
    }
  }


  @Data
  public static class IdAndNameComposite {
    @SerializedName("default")
    private String defaultValue;
    private List<IdAndName> values;
  }


  @Data
  @EqualsAndHashCode(of = "id")
  public static class IdAndName implements IdContainer {
    private String id;
    private String name;

    @Override
    public String toString() {
      return name;
    }

    public Version getVersion() {
      return Version.parse(id);
    }
  }


  @Data
  public static class DefaultValueHolder {
    @SerializedName("default")
    private String defaultValue;
  }

}
