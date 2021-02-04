package in.oneton.idea.spring.assistant.plugin.initializr.metadata;

import com.google.gson.annotations.SerializedName;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.DependencyComposite.DependencyGroup.Dependency;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.io.spring.initializr.util.Version;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.io.spring.initializr.util.VersionRange;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

@Data
@ToString
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

        @NotNull
        public Optional<DependencyGroup> findGroupForDependency(Dependency dependency) {
            return groups.stream()
                    .filter(group -> group.getDependencies().stream().anyMatch(dep -> dep.equals(dependency)))
                    .findFirst();
        }

        @Data
        @FieldNameConstants
        @ToString(onlyExplicitlyIncluded = true)
        @EqualsAndHashCode(onlyExplicitlyIncluded = true)
        public static class DependencyGroup {

            @ToString.Include
            @EqualsAndHashCode.Include
            private String name;

            @SerializedName("values")
            private List<Dependency> dependencies;


            @Data
            @EqualsAndHashCode(onlyExplicitlyIncluded = true)
            public static class Dependency {

                @EqualsAndHashCode.Include
                private String id;
                private String name;
                private String description;

                @Nullable
                private VersionRange versionRange;

                @Nullable
                @SerializedName("_links")
                private DependencyLinksContainer linksContainer;

                @SuppressWarnings("BooleanMethodIsAlwaysInverted")
                public boolean isVersionCompatible(Version bootVersion) {
                    return versionRange == null || versionRange.match(bootVersion);
                }


                @Data
                public static class DependencyLinksContainer {

                    @Nullable
                    @SerializedName("reference")
                    private List<DependencyLink> references;

                    @Nullable
                    @SerializedName("guide")
                    private List<DependencyLink> guides;


                    @Data
                    @Builder
                    @NoArgsConstructor
                    @AllArgsConstructor
                    public static class DependencyLink {

                        private String href;

                        @Nullable
                        private String title;
                        private boolean templated;

                        @NotNull
                        public String getHrefAfterReplacement(final String bootVersion) {
                            if (templated) { //TODO: > Analisar replace
                                return href.replace("\\{bootVersion}", bootVersion);
                            }
                            return href;
                        }
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
        @ToString(onlyExplicitlyIncluded = true)
        @EqualsAndHashCode(onlyExplicitlyIncluded = true)
        public static class ProjectType implements IdContainer {

            @EqualsAndHashCode.Include
            private String id;

            @ToString.Include
            private String name;

            private String description;
            private String action;
        }
    }


    @Data
    public static class IdAndNameComposite {

        @SerializedName("default")
        private String defaultValue;

        private List<IdAndName> values;
    }


    @Data
    @ToString(onlyExplicitlyIncluded = true)
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public static class IdAndName implements IdContainer {

        @EqualsAndHashCode.Include
        private String id;

        @ToString.Include
        private String name;

        public Version parseIdAsVersion() {
            return Version.parse(id);
        }
    }


    @Data
    public static class DefaultValueHolder {

        @SerializedName("default")
        private String defaultValue;
    }

}
