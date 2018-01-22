package in.oneton.idea.spring.assistant.plugin.model;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import in.oneton.idea.spring.assistant.plugin.PsiUtil;
import in.oneton.idea.spring.assistant.plugin.model.json.SpringConfigurationMetadataDeprecationLevel;
import in.oneton.idea.spring.assistant.plugin.model.json.SpringConfigurationMetadataProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static in.oneton.idea.spring.assistant.plugin.model.SuggestionNode.sanitize;
import static in.oneton.idea.spring.assistant.plugin.model.SuggestionNodeType.ARRAY;
import static in.oneton.idea.spring.assistant.plugin.model.SuggestionNodeType.MAP;
import static in.oneton.idea.spring.assistant.plugin.model.SuggestionNodeType.UNDEFINED;
import static in.oneton.idea.spring.assistant.plugin.model.SuggestionNodeType.UNKNOWN_CLASS;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Represents leaf node in the tree that holds the reference to dynamic suggestion node
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(of = "originalName")
@EqualsAndHashCode(of = "name", callSuper = false)
public class MetadataPropertySuggestionNode extends MetadataSuggestionNode {

  /**
   * Sanitised name used for lookup. `-`, `_` are removed, upper cased characters are converted to lower case
   */
  private String name;
  /**
   * Section of the group/PROPERTY name. Sole purpose of this is to split all properties into their individual part
   */
  private String originalName;
  /**
   * Parent reference, for bidirectional navigation. Can be null for roots
   */
  @Nullable
  private MetadataGroupSuggestionNode parent;
  /**
   * Set of sources these suggestions belong to
   */
  private Set<String> belongsTo;
  // TODO: Make sure that this will be part of search only if type & sourceType are part of the class path
  private SpringConfigurationMetadataProperty property;

  private String typeAsStr;
  /**
   * Responsible for all suggestion queries that needs to be matched against a class
   */
  @Nullable
  private ClassSuggestionNode delegate;

  public static MetadataPropertySuggestionNode newInstance(Module module, String name,
      @NotNull SpringConfigurationMetadataProperty property,
      @Nullable MetadataGroupSuggestionNode parent, String belongsTo) {
    MetadataPropertySuggestionNode.MetadataPropertySuggestionNodeBuilder builder =
        MetadataPropertySuggestionNode.builder().name(sanitize(name)).originalName(name)
            .property(property).parent(parent);
    HashSet<String> belongsToSet = new HashSet<>();
    belongsToSet.add(belongsTo);
    builder.belongsTo(belongsToSet);

    if (property.getType() != null) {
      PsiClass propertyClazz = PsiUtil.findClass(module, property.getType());
      if (propertyClazz != null) {
        builder.delegate(ClassSuggestionNodeFactory.newInstance(propertyClazz));
      }
      builder.typeAsStr(property.getType());
    }
    return builder.build();
  }

  @Nullable
  @Override
  public Set<Suggestion> findSuggestionsForKey(List<MetadataSuggestionNode> pathRootTillParentNode,
      int suggestionDepthFromEndOfPath, String[] querySegmentPrefixes,
      int querySegmentPrefixStartIndex, boolean navigateDeepIfNoMatches) {
    if (isLeaf()) {
      if (propertyCanBeShownAsSuggestion() && (
          querySegmentPrefixStartIndex >= querySegmentPrefixes.length || name
              .startsWith(sanitize(querySegmentPrefixes[querySegmentPrefixStartIndex])))) {
        assert property != null;
        pathRootTillParentNode.add(this);
        return newSingleElementSet(
            property.newSuggestion(pathRootTillParentNode, computeSuggestion(suggestionDepth)));
      }
      return null;
    } else {
      if (isGroup() && querySegmentPrefixStartIndex >= querySegmentPrefixes.length) {
        // If we have only one leaf, lets send the leaf value directly instead of this node
        if (hasOnlyOneLeaf()) {
          pathRootTillParentNode.add(this);
          return findChildSuggestionsForKey(pathRootTillParentNode, querySegmentPrefixes,
              suggestionDepth, querySegmentPrefixStartIndex, navigateDeepIfNoMatches);
        } else {
          assert group != null;
          pathRootTillParentNode.add(this);
          return newSingleElementSet(
              group.newSuggestion(pathRootTillParentNode, computeSuggestion(suggestionDepth)));
        }
      } else {
        return findChildSuggestionsForKey(pathRootTillParentNode, querySegmentPrefixes,
            suggestionDepth, querySegmentPrefixStartIndex, navigateDeepIfNoMatches);
      }
    }
  }

  /**
   * A property node can represent either leaf/an object depending on `type` & `hint`s associated with `SpringConfigurationMetadataProperty`
   *
   * @return true if leaf, false otherwise
   */
  @Override
  public boolean isLeaf() {
    SuggestionNodeType type = getType();
    return type.representsLeaf() || (type.potentiallyLeaf() && (
        property.doesGenericHintRepresentsArray() || delegate == null || delegate.isLeaf()));
  }

  //  public void setProperty(@Nullable SpringConfigurationMetadataProperty property) {
  //    this.property = property;
  //    if (property.getType() != null) {
  //      PsiClass propertyClazz = PsiUtil.findClass(module, property.getType());
  //      if (propertyClazz != null) {
  //        builder.delegate(ClassSuggestionNodeFactory.newInstance(propertyClazz));
  //      }
  //      builder.typeAsStr(property.getType());
  //    }
  //    if (type != null) {
  //      this.type = PsiUtil.findType(type);
  //    } else if (property.getHint() != null) {
  //      this.type = property.getHint().;
  //    } else {
  //      this.type = UNKNOWN;
  //    }
  //  }

  @Nullable
  @Override
  public Set<Suggestion> findSuggestionsForValue() {
    assert property != null;
    return property.getValueSuggestions(this);
  }

  /**
   * Type information can come from `hint` & `type` attribute of `SpringConfigurationMetadataProperty`
   *
   * @return
   */
  @Override
  public SuggestionNodeType getType() {
    if (property.hasValueHint()) {
      return MAP;
    } else if (property.doesGenericHintRepresentsArray()) {
      return ARRAY;
    } else {
      return delegate != null ?
          delegate.getType() :
          (!isEmpty(typeAsStr) ? UNKNOWN_CLASS : UNDEFINED);
    }
  }

  private boolean propertyCanBeShownAsSuggestion() {
    assert property != null;
    return !property.isDeprecated() || property.getDeprecation() == null
        || property.getDeprecation().getLevel()
        != SpringConfigurationMetadataDeprecationLevel.error;
  }


}
