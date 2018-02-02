package in.oneton.idea.spring.assistant.plugin.model.suggestion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.codeInsight.lookup.LookupElementRenderer;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.module.Module;
import in.oneton.idea.spring.assistant.plugin.completion.FileType;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz.ClassMetadata;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata.json.SpringConfigurationMetadataDeprecationLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.List;

import static com.intellij.openapi.util.text.StringUtil.shortenTextWithEllipsis;
import static com.intellij.ui.JBColor.RED;
import static com.intellij.ui.JBColor.YELLOW;
import static in.oneton.idea.spring.assistant.plugin.util.GenericUtil.dotDelimitedOriginalNames;
import static in.oneton.idea.spring.assistant.plugin.util.GenericUtil.getFirstSentenceWithoutDot;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.jetbrains.yaml.YAMLHighlighter.SCALAR_TEXT;

@Getter
@EqualsAndHashCode(of = "suggestionToDisplay")
@ToString
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
          if (!lookupString.equals(suggestion.value)) {
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
   * <li>Storing the whole value (support dynamic nodes aswell, as a single PsiClass as leaf might be referred via multiple paths)</li>
   * <li>Storing reference to leaf & navigate up till the root (efficient)</li>
   * </ol>
   * The second solution does not address suggestions that are derived from {@link ClassMetadata} as these nodes are not tied to a single branch of the suggestion tree
   */
  @NotNull
  private List<? extends SuggestionNode> matchesTopFirst;
  /**
   * Defines the number of ancestors from root, below which the current suggestion should be shown
   */
  private int numOfAncestors;
  /**
   * Whether or not the suggestion corresponds to value within key -> value pair
   */
  private boolean forValue;
  /**
   * Will only be set when the suggestion is for value
   * If value represents key in key: value, this can contain just one level of node name/can contains multiple levels dot delimited if suggestion matches multiple levels
   * If the value represents value in key: value, this value will only be one level
   */
  @Nullable
  private String value;
  /**
   * Whether the current value represents the default value
   */
  @Setter
  private boolean representingDefaultValue;
  /**
   * Type of file that requested this suggestion
   */
  @NotNull
  private FileType fileType;
  @Nullable
  private Icon icon;

  private String suggestionToDisplay;
  private String pathDotDelimitedRootToLeaf;

  @Builder
  public Suggestion(@Nullable String description, @Nullable String shortType,
      @Nullable String defaultValue,
      @Nullable SpringConfigurationMetadataDeprecationLevel deprecationLevel,
      @NotNull List<? extends SuggestionNode> matchesTopFirst, int numOfAncestors, boolean forValue,
      @Nullable String value, boolean representingDefaultValue, @NotNull FileType fileType,
      @Nullable Icon icon) {
    if (!forValue) {
      assert numOfAncestors < matchesTopFirst.size();
    } else {
      assert numOfAncestors == matchesTopFirst.size();
    }

    this.description = description;
    this.shortType = shortType;
    this.defaultValue = defaultValue;
    this.deprecationLevel = deprecationLevel;
    this.matchesTopFirst = matchesTopFirst;
    this.numOfAncestors = numOfAncestors;
    this.forValue = forValue;
    this.value = value;
    this.representingDefaultValue = representingDefaultValue;
    this.fileType = fileType;
    this.icon = icon;
    this.pathDotDelimitedRootToLeaf =
        matchesTopFirst.stream().map(SuggestionNode::getOriginalName).collect(joining("."));
    this.suggestionToDisplay =
        forValue ? value : dotDelimitedOriginalNames(matchesTopFirst, numOfAncestors);
  }

  public LookupElementBuilder newLookupElement() {
    LookupElementBuilder builder = LookupElementBuilder.create(this, suggestionToDisplay);
    if (forValue) {
      if (description != null) {
        builder = builder.withTypeText(description, true);
      }
      if (representingDefaultValue) {
        builder = builder.bold();
      }
      builder = builder.withInsertHandler(fileType.newValueInsertHandler());
    } else {
      builder = builder.withRenderer(CUSTOM_SUGGESTION_RENDERER)
          .withInsertHandler(fileType.newKeyInsertHandler());
    }
    return builder;
  }

  public String getFullPath(Module module) {
    return dotDelimitedOriginalNames(matchesTopFirst);
  }

  public SuggestionNodeType getSuggestionNodeType(Module module) {
    return getLastSuggestionNode().getSuggestionNodeType(module);
  }

  public SuggestionNode getLastSuggestionNode() {
    return matchesTopFirst.get(matchesTopFirst.size() - 1);
  }

  @Override
  public int compareTo(@NotNull Suggestion other) {
    if (forValue) {
      assert value != null;
      assert other.value != null;
      return value.compareTo(other.value);
    } else {
      return pathDotDelimitedRootToLeaf.compareTo(other.pathDotDelimitedRootToLeaf);
    }
  }

  public List<? extends SuggestionNode> getMatchesForReplacement() {
    return matchesTopFirst.stream().skip(numOfAncestors).collect(toList());
  }

  public String getLeafOriginalNameOrValue() {
    return forValue ? value : getLastSuggestionNode().getOriginalName();
  }
}
