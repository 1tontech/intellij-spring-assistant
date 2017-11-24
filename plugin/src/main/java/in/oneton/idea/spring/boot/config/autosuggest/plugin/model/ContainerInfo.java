package in.oneton.idea.spring.boot.config.autosuggest.plugin.model;

import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import lombok.Builder;
import lombok.Getter;

import javax.annotation.Nullable;

import static com.intellij.openapi.fileTypes.FileTypes.ARCHIVE;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

@Getter
@Builder
public class ContainerInfo {
  /**
   * Can point to archive/directory containing the metadata file
   */
  private String containerPath;
  @Nullable
  private String path;
  private boolean archive;
  /**
   * If containerPath points to archive, then represents the timestamp of the archive
   * else, represents the timestamp of the metadata file within
   */
  private long timestamp;

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

  public static ContainerInfo newInstance(VirtualFile fileContainer) {
    VirtualFile containerFile = getContainerFile(fileContainer);
    ContainerInfoBuilder builder = ContainerInfo.builder().containerPath(containerFile.getUrl())
        .archive(fileContainer.getFileType() == ARCHIVE);
    VirtualFile metadataFile = findMetadataFile(fileContainer);
    if (metadataFile != null) {
      builder.path(metadataFile.getUrl()).timestamp(metadataFile.getModificationStamp());
    } else {
      builder.timestamp(containerFile.getTimeStamp());
    }
    return builder.build();
  }

  public boolean isUpdatedAfter(ContainerInfo other) {
    return this.timestamp > other.timestamp;
  }

  public boolean containsMetadataFile() {
    return path != null;
  }

  public VirtualFile getMetadataFile() {
    assert path != null;
    return VirtualFileManager.getInstance().findFileByUrl(path);
  }
}
