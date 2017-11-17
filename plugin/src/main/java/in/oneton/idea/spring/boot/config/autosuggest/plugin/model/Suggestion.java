package in.oneton.idea.spring.boot.config.autosuggest.plugin.model;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import in.oneton.idea.spring.boot.config.autosuggest.plugin.insert.handler.YamlKeyInsertHandler;
import in.oneton.idea.spring.boot.config.autosuggest.plugin.insert.handler.YamlValueInsertHandler;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import javax.swing.*;

@Getter
@Builder
@EqualsAndHashCode(of = "suggestion")
public class Suggestion {
  @Nullable
  private Icon icon;
  private String suggestion;
  @Nullable
  private String description;
  private MetadataNode ref;
  private boolean deprecated;
  /**
   * Whether or not the suggestion corresponds to value (with key -> value pair)
   */
  private boolean referringToValue;
  /**
   * Whether teh current value represents the default value
   */
  @Setter
  private boolean defaultValue;

  public LookupElementBuilder newLookupElement(ClassLoader classLoader) {
    LookupElementBuilder builder = LookupElementBuilder.create(this, suggestion);
    if (icon != null) {
      builder = builder.withIcon(icon);
    }
    if (description != null) {
      builder = builder.withTypeText(description, true);
    }
    if (deprecated) {
      builder = builder.strikeout();
    }
    if (referringToValue) {
      if (defaultValue) {
        builder = builder.bold();
      }
      builder = builder.withInsertHandler(new YamlValueInsertHandler());
    } else {
      builder = builder.withInsertHandler(new YamlKeyInsertHandler(ref, classLoader));
    }
    return builder;
  }
}
