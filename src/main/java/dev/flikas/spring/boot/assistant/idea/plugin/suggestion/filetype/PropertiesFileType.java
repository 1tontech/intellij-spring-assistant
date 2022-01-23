package dev.flikas.spring.boot.assistant.idea.plugin.suggestion.filetype;

import com.intellij.lang.properties.PropertiesLanguage;
import com.intellij.openapi.fileTypes.LanguageFileType;
import in.oneton.idea.spring.assistant.plugin.misc.Icons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class PropertiesFileType extends LanguageFileType {
  public static final PropertiesFileType INSTANCE = new PropertiesFileType();

  private PropertiesFileType() {
    super(PropertiesLanguage.INSTANCE, true);
  }

  @Override
  public @NonNls @NotNull String getName() {
    return "spring-boot-properties";
  }

  @Override
  public @NotNull String getDescription() {
    return "Spring properties file";
  }

  @Override
  public @NotNull String getDefaultExtension() {
    return "properties";
  }

  @Override
  public @Nullable Icon getIcon() {
    return Icons.SpringBoot;
  }
}
