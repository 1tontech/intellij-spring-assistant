package in.oneton.idea.spring.assistant.plugin.model;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.codeInsight.lookup.LookupElementRenderer;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.TextAttributes;
import in.oneton.idea.spring.assistant.plugin.Util;
import in.oneton.idea.spring.assistant.plugin.insert.handler.YamlKeyInsertHandler;
import in.oneton.idea.spring.assistant.plugin.insert.handler.YamlValueInsertHandler;
import in.oneton.idea.spring.assistant.plugin.model.json.SpringConfigurationMetadataDeprecationLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import javax.swing.*;

import static com.intellij.openapi.util.text.StringUtil.shortenTextWithEllipsis;
import static com.intellij.ui.JBColor.RED;
import static java.awt.Color.YELLOW;
import static org.jetbrains.yaml.YAMLHighlighter.SCALAR_TEXT;

@Getter
@Builder
@EqualsAndHashCode(of = "suggestion")
public class Suggestion {

  private static final LookupElementRenderer<LookupElement> CUSTOM_SUGGESTION_RENDERER =
      new LookupElementRenderer<LookupElement>() {
        public void renderElement(LookupElement element, LookupElementPresentation presentation) {
          Suggestion suggestion = (Suggestion) element.getObject();
          if (suggestion.icon != null) {
            presentation.setIcon(suggestion.icon);
          }

          presentation.setStrikeout(suggestion.deprecationLevel != null);
          if (suggestion.deprecationLevel != null) {
            if (suggestion.deprecationLevel == SpringConfigurationMetadataDeprecationLevel.error) {
              presentation.setItemTextForeground(RED);
            } else {
              presentation.setItemTextForeground(YELLOW);
            }
          }

          String lookupString = element.getLookupString();
          presentation.setItemText(lookupString);
          if (!lookupString.equals(suggestion.suggestion)) {
            presentation.setItemTextBold(true);
          }

          String shortDescription;
          if (suggestion.defaultValue != null) {
            shortDescription = shortenTextWithEllipsis(suggestion.defaultValue, 60, 0, true);
            TextAttributes attrs =
                EditorColorsManager.getInstance().getGlobalScheme().getAttributes(SCALAR_TEXT);
            presentation.setTailText("=" + shortDescription, attrs.getForegroundColor());
          }

          if (suggestion.description != null) {
            presentation.appendTailText(
                " (" + Util.getFirstSentenceWithoutDot(suggestion.description) + ")", true);
          }

          if (suggestion.shortType != null) {
            presentation.setTypeText(suggestion.shortType);
          }
        }
      };

  @Nullable
  private Icon icon;
  private String suggestion;
  @Nullable
  private String description;
  @Nullable
  private String shortType;
  @Nullable
  private String defaultValue;
  @Nullable
  private SpringConfigurationMetadataDeprecationLevel deprecationLevel;
  private MetadataNode ref;
  /**
   * Whether or not the suggestion corresponds to value (with key -> value pair)
   */
  private boolean referringToValue;
  /**
   * Whether the current value represents the default value
   */
  @Setter
  private boolean representingDefaultValue;

  public LookupElementBuilder newLookupElement(ClassLoader classLoader) {
    LookupElementBuilder builder = LookupElementBuilder.create(this, suggestion);
    if (referringToValue) {
      if (description != null) {
        builder = builder.withTypeText(description, true);
      }
      if (representingDefaultValue) {
        builder = builder.bold();
      }
      builder = builder.withInsertHandler(new YamlValueInsertHandler());
    } else {
      builder = builder.withRenderer(CUSTOM_SUGGESTION_RENDERER)
          .withInsertHandler(new YamlKeyInsertHandler(ref, classLoader));
    }
    return builder;
  }

  public int getMaxDepth() {
    return suggestion.split(Util.PERIOD_DELIMITER).length;
  }
}
