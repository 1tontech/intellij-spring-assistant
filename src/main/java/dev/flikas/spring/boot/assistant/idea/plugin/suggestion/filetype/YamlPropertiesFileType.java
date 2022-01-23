package dev.flikas.spring.boot.assistant.idea.plugin.suggestion.filetype;

import com.intellij.openapi.fileTypes.LanguageFileType;
import in.oneton.idea.spring.assistant.plugin.misc.Icons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLLanguage;

import javax.swing.*;

public class YamlPropertiesFileType extends LanguageFileType {
  public static final YamlPropertiesFileType INSTANCE = new YamlPropertiesFileType();

  private YamlPropertiesFileType() {
    super(YAMLLanguage.INSTANCE, true);
  }

  @Override
  public @NonNls @NotNull String getName() {
    return "spring-boot-properties-yaml";
  }

  @Override
  public @NotNull String getDescription() {
    return "Spring yaml properties file";
  }

  @Override
  public @NotNull String getDefaultExtension() {
    return "yaml";
  }

  @Override
  public @Nullable Icon getIcon() {
    return Icons.SpringBoot;
  }
}
