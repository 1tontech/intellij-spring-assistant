package in.oneton.idea.spring.assistant.plugin.initializr.misc;

import com.intellij.ui.CollectionComboBoxModel;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static org.apache.commons.lang3.StringUtils.isEmpty;

@UtilityClass
public class FormUtil {
  @NotNull
  public static <T extends InitializerMetadata.IdContainer> CollectionComboBoxModel<T> newCollectionComboBoxModel(
      @NotNull List<T> values, @Nullable String defaultValue) {
    T defaultIdAndName = null;
    if (!isEmpty(defaultValue)) {
      for (T idAndName : values) {
        if (idAndName.getId().equals(defaultValue)) {
          defaultIdAndName = idAndName;
          break;
        }
      }
    }
    return new CollectionComboBoxModel<>(values, defaultIdAndName);
  }
}
