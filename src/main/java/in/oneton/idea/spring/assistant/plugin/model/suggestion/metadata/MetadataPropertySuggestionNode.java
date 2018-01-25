package in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClassType;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz.ClassSuggestionNodeProxy;
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

import static in.oneton.idea.spring.assistant.plugin.ClassUtil.safeFindClassType;
import static in.oneton.idea.spring.assistant.plugin.ClassUtil.toClass;
import static in.oneton.idea.spring.assistant.plugin.Util.newListWithMembers;
import static in.oneton.idea.spring.assistant.plugin.Util.newSingleElementSortedSet;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNode.sanitise;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType.ARRAY;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType.MAP;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType.UNDEFINED;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType.UNKNOWN_CLASS;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata.json.SpringConfigurationMetadataDeprecationLevel.error;
import static java.util.Collections.unmodifiableList;
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
  private MetadataNonPropertySuggestionNode parent;
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
  private ClassSuggestionNodeProxy delegate;

  /**
   * @param module       IDEA module to which this property belongs
   * @param originalName name that is not sanitised
   * @param property     property to associate
   * @param parent       parent MetadataNonPropertySuggestionNode node
   * @param belongsTo    file/jar containing this property
   * @return newly constructed property node
   */
  public static MetadataPropertySuggestionNode newInstance(Module module, String originalName,
      @NotNull SpringConfigurationMetadataProperty property,
      @Nullable MetadataNonPropertySuggestionNode parent, String belongsTo) {
    MetadataPropertySuggestionNode.MetadataPropertySuggestionNodeBuilder builder =
        MetadataPropertySuggestionNode.builder().name(sanitise(originalName))
            .originalName(originalName).property(property).parent(parent);
    HashSet<String> belongsToSet = new HashSet<>();
    belongsToSet.add(belongsTo);
    builder.belongsTo(belongsToSet);

    if (property.getType() != null) {
      PsiClassType propertyClassType = safeFindClassType(module, property.getType());
      if (propertyClassType != null) {
        builder.delegate(new ClassSuggestionNodeProxy(toClass(propertyClassType)));
      }
      builder.typeAsStr(property.getType());
    }
    return builder.build();
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

  /**
   * Type information can come from `hint` & `type` attribute of `SpringConfigurationMetadataProperty`
   *
   * @return node type
   */
  @NotNull
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

  @Override
  public SuggestionNode findDeepestMatch(String[] pathSegments, int pathSegmentStartIndex) {
    SuggestionNode deepestMatch = null;
    boolean haveMoreSegments = pathSegmentStartIndex < pathSegments.length;
    if (haveMoreSegments) {
      String pathSegment = pathSegments[pathSegmentStartIndex];
      if (name.equals(pathSegment)) {
        boolean lastSegment = pathSegmentStartIndex == (pathSegments.length - 1);
        if (lastSegment) {
          deepestMatch = this;
        } else {
          if (delegate != null && delegate.hasChildren()) {
            deepestMatch = delegate.findDeepestMatch(pathSegments, pathSegmentStartIndex);
          }
        }
      }
    }

    return deepestMatch;
  }

  @Nullable
  @Override
  public List<SuggestionNode> findDeepestMatch(List<SuggestionNode> matchesRootTillParentNode,
      String[] pathSegments, int pathSegmentStartIndex) {
    List<SuggestionNode> deepestMatch = null;
    boolean haveMoreSegments = pathSegmentStartIndex < pathSegments.length;
    if (haveMoreSegments) {
      String pathSegment = pathSegments[pathSegmentStartIndex];
      if (name.equals(pathSegment)) {
        matchesRootTillParentNode.add(this);
        boolean lastSegment = pathSegmentStartIndex == (pathSegments.length - 1);
        if (lastSegment) {
          deepestMatch = matchesRootTillParentNode;
        } else {
          if (delegate != null && delegate.hasChildren()) {
            deepestMatch = delegate.findDeepestMatch(matchesRootTillParentNode, pathSegments,
                pathSegmentStartIndex + 1);
          }
        }
      }
    }

    return deepestMatch;
  }

  @Nullable
  @Override
  public SortedSet<Suggestion> findSuggestionsForKey(@Nullable String ancestralKeysDotDelimited,
      List<SuggestionNode> matchesRootTillParentNode, String[] querySegmentPrefixes,
      int querySegmentPrefixStartIndex) {
    if (isNotDeprecatedError()) {
      boolean lookingForConcreteNode = querySegmentPrefixStartIndex >= querySegmentPrefixes.length;
      if (lookingForConcreteNode) {
        return newSingleElementSortedSet(
            property.buildSuggestion(ancestralKeysDotDelimited, matchesRootTillParentNode, this));
      } else {
        String querySegmentPrefix = querySegmentPrefixes[querySegmentPrefixStartIndex];
        if (name.startsWith(querySegmentPrefix)) {
          boolean lastQuerySegment =
              querySegmentPrefixStartIndex == (querySegmentPrefixes.length - 1);
          if (lastQuerySegment) {
            return newSingleElementSortedSet(property
                .buildSuggestion(ancestralKeysDotDelimited, matchesRootTillParentNode, this));
          } else {
            if (delegate != null) {
              return delegate.findSuggestionsForKey(ancestralKeysDotDelimited,
                  unmodifiableList(newListWithMembers(matchesRootTillParentNode, this)),
                  querySegmentPrefixes, querySegmentPrefixStartIndex + 1);
            }
          }
        }
      }
    }
    return null;
  }

  @Nullable
  @Override
  public String getDocumentationForKey(String nodeNavigationPathDotDelimited) {
    return property.getDocumentationForKey(nodeNavigationPathDotDelimited);
  }

  @Nullable
  @Override
  protected SortedSet<Suggestion> findSuggestionsForKey(@Nullable String ancestralKeysDotDelimited,
      List<SuggestionNode> matchesRootTillParentNode, String[] querySegmentPrefixes,
      int querySegmentPrefixStartIndex, boolean navigateDeepIfNoMatches) {
    return findSuggestionsForKey(ancestralKeysDotDelimited, matchesRootTillParentNode,
        querySegmentPrefixes, querySegmentPrefixStartIndex);
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
  protected boolean hasOnlyOneChild() {
    // since we have to delegate any further lookups to the delegate (which has additional cost associated with parsing & building childrenTrie dynamically)
    // lets always lie to caller that we have more than one child so that the search terminates at this node on the initial lookup
    return false;
  }

  @Override
  public boolean removeRefCascadeDown(String containerPath) {
    belongsTo.remove(containerPath);
    // If the current node & all its children belong to a single file, lets remove the whole tree
    return belongsTo.size() == 0;
  }

  @Override
  public void refreshClassProxy(Module module) {
    if (property.getType() != null) {
      // Lets update the delegate information only if anything has changed from last time we saw this
      PsiClassType propertyClassType = safeFindClassType(module, property.getType());
      // In the previous refresh, class could not be found. Now class is available in the classpath
      if (propertyClassType != null && delegate == null) {
        delegate = new ClassSuggestionNodeProxy(toClass(propertyClassType));
      }
      // In the previous refresh, class was available in classpath. Now it is nolonger available
      if (propertyClassType == null && delegate != null) {
        delegate = null;
      }
    }
  }

  /**
   * @return false if the property is not deprecated & level is error, true otherwise
   */
  private boolean isNotDeprecatedError() {
    return !property.isDeprecated() || property.getDeprecation() == null
        || property.getDeprecation().getLevel() != error;
  }

}
