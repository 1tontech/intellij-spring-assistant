package in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata;

import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;

import static com.intellij.openapi.fileTypes.FileTypes.ARCHIVE;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

@Getter
@Builder
@ToString
public class MetadataContainerInfo {
  /**
   * Can point to archive/directory containing the metadata file
   */
  private String containerPath;
  @Nullable
  private String path;
  private boolean archive;
  /**
   * If containerPath points to archive, then represents the timestamp of the archive
   * else, represents the length of the generated metadata file
   */
  private long marker;

  public static VirtualFile getContainerFile(VirtualFile fileContainer) {
    if (fileContainer.getFileType() == ARCHIVE) {
      return requireNonNull(JarFileSystem.getInstance().getLocalVirtualFileFor(fileContainer));
    } else {
      return fileContainer;
    }
  }

  private static VirtualFile findMetadataFile(VirtualFile root, String metadataFileName) {
    if (!root.is(VFileProperty.SYMLINK)) {
      //noinspection UnsafeVfsRecursion
      for (VirtualFile child : asList(root.getChildren())) {
        // TODO: Add support additional MetadataContainerInfo for `additional-spring-configuration-metadata.json` when the root is a directory. This allows to support custom configuration within the project. This was we dont need to delegate the build to gradle. Intellij can handle it directly
        if (child.getName().equals(metadataFileName)) {
          return child;
        }
        VirtualFile matchedFile = findMetadataFile(child, metadataFileName);
        if (matchedFile != null) {
          return matchedFile;
        }
      }
    }
    return null;
  }

  public static Collection<MetadataContainerInfo> newInstances(VirtualFile fileContainer) {
    Collection<MetadataContainerInfo> containerInfos = new ArrayList<>();
    VirtualFile containerFile = getContainerFile(fileContainer);
    boolean archive = fileContainer.getFileType() == ARCHIVE;
    MetadataContainerInfo containerInfo =
        newInstance(fileContainer, containerFile, "spring-configuration-metadata.json", archive);
    containerInfos.add(containerInfo);
    if (!archive) {
      // Even after enabling annotation processor support in intellij, for the projects with `spring-boot-configuration-processor` in classpath, intellij is not merging `spring-configuration-metadata.json` & the generated `additional-spring-configuration-metadata.json`. So lets merge these two ourselves if root is not an archive
      MetadataContainerInfo additionalContainerInfo =
          newInstance(fileContainer, containerFile, "additional-spring-configuration-metadata.json",
              false);
      if (additionalContainerInfo != null) {
        containerInfos.add(additionalContainerInfo);
      }
    }
    return containerInfos;
  }

  private static MetadataContainerInfo newInstance(VirtualFile fileContainer,
      VirtualFile containerFile, String metadataFileName, boolean archive) {
    MetadataContainerInfoBuilder builder =
        MetadataContainerInfo.builder().containerPath(containerFile.getUrl()).archive(archive);
    VirtualFile metadataFile = findMetadataFile(fileContainer, metadataFileName);
    if (metadataFile != null) {
      // since build might auto generate the metadata file in the project, its better to rely on
      builder.path(metadataFile.getUrl())
          .marker(archive ? metadataFile.getModificationCount() : metadataFile.getLength());
    } else {
      builder.marker(containerFile.getModificationCount());
    }
    return builder.build();
  }

  public boolean isModified(MetadataContainerInfo other) {
    return this.marker != other.marker;
  }

  public boolean containsMetadataFile() {
    return path != null;
  }

  public VirtualFile getMetadataFile() {
    assert path != null;
    return VirtualFileManager.getInstance().findFileByUrl(path);
  }
}
