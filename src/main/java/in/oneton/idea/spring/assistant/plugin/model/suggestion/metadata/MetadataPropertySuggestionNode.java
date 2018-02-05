package in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata;

import com.intellij.openapi.module.Module;
import in.oneton.idea.spring.assistant.plugin.completion.FileType;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata.json.SpringConfigurationMetadataProperty;
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
import java.util.SortedSet;

import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNode.sanitise;
import static in.oneton.idea.spring.assistant.plugin.util.GenericUtil.newSingleElementSortedSet;

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
  private MetadataNonPropertySuggestionNode parent;
  /**
   * Set of sources these suggestions belong to
   */
  private Set<String> belongsTo;
  // TODO: Make sure that this will be part of search only if type & sourceType are part of the class path
  private SpringConfigurationMetadataProperty property;

  /**
   * @param originalName name that is not sanitised
   * @param property     property to associate
   * @param parent       parent MetadataNonPropertySuggestionNode node
   * @param belongsTo    file/jar containing this property
   * @return newly constructed property node
   */
  public static MetadataPropertySuggestionNode newInstance(String originalName,
      @NotNull SpringConfigurationMetadataProperty property,
      @Nullable MetadataNonPropertySuggestionNode parent, String belongsTo) {
    MetadataPropertySuggestionNode.MetadataPropertySuggestionNodeBuilder builder =
        MetadataPropertySuggestionNode.builder().name(sanitise(originalName))
            .originalName(originalName).property(property).parent(parent);
    HashSet<String> belongsToSet = new HashSet<>();
    belongsToSet.add(belongsTo);
    builder.belongsTo(belongsToSet);
    return builder.build();
  }

  /**
   * A property node can represent either leaf/an object depending on `type` & `hint`s associated with `SpringConfigurationMetadataProperty`
   *
   * @param module module
   * @return true if leaf, false otherwise
   */
  @Override
  public boolean isLeaf(Module module) {
    return property.isLeaf(module);
  }

  @Override
  public boolean isMetadataNonProperty() {
    return false;
  }

  /**
   * Type information can come from `hint` & `type` attribute of `SpringConfigurationMetadataProperty`
   *
   * @param module module
   * @return node type
   */
  @NotNull
  @Override
  public SuggestionNodeType getSuggestionNodeType(Module module) {
    return property.getSuggestionNodeType(module);
  }

  @NotNull
  @Override
  public String getOriginalName() {
    return originalName;
  }

  @Override
  public MetadataSuggestionNode findDeepestMetadataNode(String[] pathSegments,
      int pathSegmentStartIndex, boolean matchAllSegments) {
    MetadataSuggestionNode deepestMatch = null;
    if (!matchAllSegments) {
      deepestMatch = this;
    }
    boolean haveMoreSegments = pathSegmentStartIndex < pathSegments.length;
    if (haveMoreSegments) {
      String pathSegment = pathSegments[pathSegmentStartIndex];
      boolean lastSegment = pathSegmentStartIndex == (pathSegments.length - 1);
      // since this is the last node in the metadata subsection of the tree, lets just return if this is not the last segment & last node
      if (name.equals(pathSegment) && lastSegment) {
        deepestMatch = this;
      }
    }

    return deepestMatch;
  }

  @Nullable
  @Override
  public List<SuggestionNode> findDeepestSuggestionNode(Module module,
      List<SuggestionNode> matchesRootTillParentNode, String[] pathSegments,
      int pathSegmentStartIndex) {
    List<SuggestionNode> deepestMatch = null;
    boolean haveMoreSegments = pathSegmentStartIndex < pathSegments.length;
    if (haveMoreSegments) {
      if (!property.isLeaf(module)) {
        deepestMatch = property
            .findChildDeepestKeyMatch(module, matchesRootTillParentNode, pathSegments,
                pathSegmentStartIndex);
      }
    }

    return deepestMatch;
  }

  @Nullable
  @Override
  public SortedSet<Suggestion> findKeySuggestionsForQueryPrefix(Module module, FileType fileType,
      List<SuggestionNode> matchesRootTillMe, int numOfAncestors, String[] querySegmentPrefixes,
      int querySegmentPrefixStartIndex, @Nullable Set<String> siblingsToExclude) {
    if (!property.isDeprecatedError()) {
      boolean lookingForConcreteNode = querySegmentPrefixStartIndex >= querySegmentPrefixes.length;
      if (lookingForConcreteNode) {
        return newSingleElementSortedSet(
            property.buildKeySuggestion(module, fileType, matchesRootTillMe, numOfAncestors));
      } else {
        if (!property.isLeaf(module)) {
          return property.findChildKeySuggestionsForQueryPrefix(module, fileType, matchesRootTillMe,
              numOfAncestors, querySegmentPrefixes, querySegmentPrefixStartIndex,
              siblingsToExclude);
        }
      }
    }
    return null;
  }

  @Nullable
  @Override
  public String getDocumentationForKey(Module module, String nodeNavigationPathDotDelimited) {
    return property.getDocumentationForKey(nodeNavigationPathDotDelimited);
  }

  @Nullable
  @Override
  public SortedSet<Suggestion> findValueSuggestionsForPrefix(Module module, FileType fileType,
      List<SuggestionNode> matchesRootTillMe, String prefix,
      @Nullable Set<String> siblingsToExclude) {
    return property
        .findSuggestionsForValues(module, fileType, matchesRootTillMe, prefix, siblingsToExclude);
  }

  @Nullable
  @Override
  public String getDocumentationForValue(Module module, String nodeNavigationPathDotDelimited,
      String value) {
    return property.getDocumentationForValue(module, nodeNavigationPathDotDelimited, value);
  }

  @Override
  protected boolean isRoot() {
    return parent == null;
  }

  @Override
  public boolean isGroup() {
    return false;
  }

  @Override
  public boolean isProperty() {
    return true;
  }

  @Override
  protected boolean hasOnlyOneChild(Module module) {
    // since we have to delegate any further lookups to the delegate (which has additional cost associated with parsing & building childrenTrie dynamically)
    // lets always lie to caller that we have more than one child so that the search terminates at this node on the initial lookup
    return false;
  }

  @Override
  public String toTree() {
    return originalName + (isRoot() ? "(root + property)" : "(property)");
  }

  @Override
  public boolean removeRefCascadeDown(String containerPath) {
    belongsTo.remove(containerPath);
    // If the current node & all its children belong to a single file, lets remove the whole tree
    return belongsTo.size() == 0;
  }

  @Override
  public void refreshClassProxy(Module module) {
    property.refreshDelegate(module);
  }

}
