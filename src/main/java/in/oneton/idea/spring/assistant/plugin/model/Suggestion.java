package in.oneton.idea.spring.assistant.plugin.model;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.codeInsight.lookup.LookupElementRenderer;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.TextAttributes;
import in.oneton.idea.spring.assistant.plugin.Util;
import in.oneton.idea.spring.assistant.plugin.insert.handler.PropertyKeyInsertHandler;
import in.oneton.idea.spring.assistant.plugin.insert.handler.YamlKeyInsertHandler;
import in.oneton.idea.spring.assistant.plugin.insert.handler.YamlValueInsertHandler;
import in.oneton.idea.spring.assistant.plugin.model.json.SpringConfigurationMetadataDeprecationLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.List;

import static com.intellij.openapi.util.text.StringUtil.shortenTextWithEllipsis;
import static com.intellij.ui.JBColor.RED;
import static com.intellij.ui.JBColor.YELLOW;
import static org.jetbrains.yaml.YAMLHighlighter.SCALAR_TEXT;

@Getter
@Builder
@EqualsAndHashCode(of = "suggestion")
public class Suggestion {

  private static final LookupElementRenderer<LookupElement> CUSTOM_SUGGESTION_RENDERER = new LookupElementRenderer<LookupElement>() {
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
        presentation.appendTailText(" (" + Util.getFirstSentenceWithoutDot(suggestion.description) + ")", true);
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
  /**
   * There are two approaches to storing a reference to suggestion
   * <ol>
   * <li>Storing the whole path (support dynamic nodes aswell, as a single PsiClass as leaf might be referred via multiple paths)</li>
   * <li>Storing reference to leaf & navigate up till the root (efficient)</li>
   * </ol>
   * The second solution does not address suggestions that are derived from {@link ClassSuggestionNode} as these nodes are not tied to a single branch of the suggestion tree
   */
  private List<? extends SuggestionNode> nodesRootToLeaf;
  /**
   * Whether or not the suggestion corresponds to value (with key -> value pair)
   */
  private boolean forValue;
  /**
   * Whether the current value represents the default value
   */
  @Setter
  private boolean representingDefaultValue;
  private boolean yaml;

  public LookupElementBuilder newLookupElement() {
    LookupElementBuilder builder = LookupElementBuilder.create(this, suggestion);
    if (forValue) {
      if (description != null) {
        builder = builder.withTypeText(description, true);
      }
      if (representingDefaultValue) {
        builder = builder.bold();
      }
      builder = builder.withInsertHandler(
          yaml ? new YamlValueInsertHandler() : new PropertyValueInsertHandler());
    } else {
      builder = builder.withRenderer(CUSTOM_SUGGESTION_RENDERER).withInsertHandler(yaml ?
          new YamlKeyInsertHandler(nodesRootToLeaf) :
          new PropertyKeyInsertHandler(nodesRootToLeaf));
    }
    return builder;
  }

  public int getMaxDepth() {
    return suggestion.split(Util.PERIOD_DELIMITER).length;
  }

  public String getFullPath() {
    StringBuilder builder = new StringBuilder();
    builder.append(originalName);
    MetadataGroupSuggestionNode next = this.parent;
    while (next != null) {
      builder.insert(0, ".").insert(0, next.originalName);
      next = next.parent;
    }
    return builder.toString();
  }

  @Override
  public String getSuggestionReplacement(String existingIndentation, String indentPerLevel,
      int maxDepth) {
    int numOfHops = 0;
    MetadataGroupSuggestionNode currentNode = this;

    StringBuilder builder = new StringBuilder(
        existingIndentation + getIndent(indentPerLevel, (maxDepth - 1)) + currentNode.originalName);

    currentNode = currentNode.parent;
    numOfHops++;
    while (currentNode != null && numOfHops < maxDepth) {
      builder.insert(0, existingIndentation + getIndent(indentPerLevel, (maxDepth - numOfHops - 1))
          + currentNode.originalName + ":\n");
      currentNode = currentNode.parent;
      numOfHops++;
    }

    return builder.delete(0, existingIndentation.length()).toString();
  }

  @Override
  public String getNewOverallIndent(String existingIndentation, String indentPerLevel,
      int maxDepth) {
    return existingIndentation + getIndent(indentPerLevel, (maxDepth - 1));
  }

  private String getIndent(String indent, int numOfHops) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < numOfHops; i++) {
      builder.append(indent);
    }
    return builder.toString();
  }
}
