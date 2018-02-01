package in.oneton.idea.spring.assistant.plugin.model.suggestion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.codeInsight.lookup.LookupElementRenderer;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.module.Module;
import in.oneton.idea.spring.assistant.plugin.insert.handler.YamlKeyInsertHandler;
import in.oneton.idea.spring.assistant.plugin.insert.handler.YamlValueInsertHandler;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz.ClassMetadata;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata.json.SpringConfigurationMetadataDeprecationLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.List;

import static com.intellij.openapi.util.text.StringUtil.shortenTextWithEllipsis;
import static com.intellij.ui.JBColor.RED;
import static com.intellij.ui.JBColor.YELLOW;
import static in.oneton.idea.spring.assistant.plugin.util.GenericUtil.dotDelimitedOriginalNames;
import static in.oneton.idea.spring.assistant.plugin.util.GenericUtil.getFirstSentenceWithoutDot;
import static org.jetbrains.yaml.YAMLHighlighter.SCALAR_TEXT;

@Getter
@Builder
@EqualsAndHashCode(of = "pathOrValue")
public class Suggestion implements Comparable<Suggestion> {
  public static final String PERIOD_DELIMITER = "\\.";

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
          if (!lookupString.equals(suggestion.pathOrValue)) {
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
            presentation
                .appendTailText(" (" + getFirstSentenceWithoutDot(suggestion.description) + ")",
                    true);
          }

          if (suggestion.shortType != null) {
            presentation.setTypeText(suggestion.shortType);
          }
        }
      };

  @Nullable
  private Icon icon;
  /**
   * Suggestion shown to user.
   * If pathOrValue represents key in key: value, this can contain just one level of node name/can contains multiple levels dot delimited if suggestion matches multiple levels
   * If the pathOrValue represents value in key: value, this value will only be one level
   */
  private String pathOrValue;
  @Nullable
  private String description;
  @Nullable
  private String shortType;
  @Nullable
  private String defaultValue;
  @Nullable
  private SpringConfigurationMetadataDeprecationLevel deprecationLevel;
  @Nullable
  private String ancestralKeysDotDelimited;
  /**
   * There are two approaches to storing a reference to suggestion
   * <ol>
   * <li>Storing the whole pathOrValue (support dynamic nodes aswell, as a single PsiClass as leaf might be referred via multiple paths)</li>
   * <li>Storing reference to leaf & navigate up till the root (efficient)</li>
   * </ol>
   * The second solution does not address suggestions that are derived from {@link ClassMetadata} as these nodes are not tied to a single branch of the suggestion tree
   */
  private List<? extends SuggestionNode> matchesTopFirst;
  /**
   * Whether or not the suggestion corresponds to pathOrValue within key -> pathOrValue pair
   */
  private boolean forValue;
  /**
   * Whether the current value represents the default value
   */
  @Setter
  private boolean representingDefaultValue;
  /**
   * Is this suggestion triggered for yaml/property file
   */
  private boolean yaml;

  public LookupElementBuilder newLookupElement() {
    LookupElementBuilder builder = LookupElementBuilder.create(this, pathOrValue);
    if (forValue) {
      if (description != null) {
        builder = builder.withTypeText(description, true);
      }
      if (representingDefaultValue) {
        builder = builder.bold();
      }
      // TODO: Add properties support
      builder = builder.withInsertHandler(yaml ? new YamlValueInsertHandler() : null);
    } else {
      // TODO: Add properties support
      builder = builder.withRenderer(CUSTOM_SUGGESTION_RENDERER)
          .withInsertHandler(yaml ? new YamlKeyInsertHandler() : null);
    }
    return builder;
  }

  public String getFullPath(Module module) {
    return ancestralKeysDotDelimited + dotDelimitedOriginalNames(module, matchesTopFirst);
  }

  public String getSuggestionReplacement(Module module, String existingIndentation,
      String indentPerLevel) {
    StringBuilder builder = new StringBuilder();
    int i = 0;
    do {
      SuggestionNode currentNode = matchesTopFirst.get(i);
      builder.append(existingIndentation)
          .append(getIndent(indentPerLevel, matchesTopFirst.size() - i - 1))
          .append(currentNode.getOriginalName(module));
      i++;
    } while (i < matchesTopFirst.size());
    return builder.delete(0, existingIndentation.length()).toString();
  }

  public String getNewOverallIndent(String existingIndentation, String indentPerLevel) {
    return existingIndentation + getIndent(indentPerLevel, matchesTopFirst.size() - 1);
  }

  private String getIndent(String indent, int numOfHops) {
    if (numOfHops == 0) {
      return "";
    }
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < numOfHops; i++) {
      builder.append(indent);
    }
    return builder.toString();
  }

  public SuggestionNodeType getSuggestionNodeType(Module module) {
    return matchesTopFirst.get(matchesTopFirst.size() - 1).getSuggestionNodeType(module);
  }

  @Override
  public int compareTo(@NotNull Suggestion other) {
    return pathOrValue.compareTo(other.pathOrValue);
  }
}
