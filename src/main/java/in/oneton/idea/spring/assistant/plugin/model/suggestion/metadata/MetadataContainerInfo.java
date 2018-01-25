package in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata;

import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import javax.annotation.Nullable;

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

  private static VirtualFile findMetadataFile(VirtualFile root) {
    if (!root.is(VFileProperty.SYMLINK)) {
      //noinspection UnsafeVfsRecursion
      for (VirtualFile child : asList(root.getChildren())) {
        if (child.getName().equals("spring-configuration-metadata.json")) {
          return child;
        }
        VirtualFile matchedFile = findMetadataFile(child);
        if (matchedFile != null) {
          return matchedFile;
        }
      }
    }
    return null;
  }

  public static MetadataContainerInfo newInstance(VirtualFile fileContainer) {
    VirtualFile containerFile = getContainerFile(fileContainer);
    boolean archive = fileContainer.getFileType() == ARCHIVE;
    ContainerInfoBuilder builder =
        MetadataContainerInfo.builder().containerPath(containerFile.getUrl()).archive(archive);
    VirtualFile metadataFile = findMetadataFile(fileContainer);
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
