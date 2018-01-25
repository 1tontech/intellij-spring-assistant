package in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClassType;
import gnu.trove.THashMap;
import in.oneton.idea.spring.assistant.plugin.ClassUtil;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata.json.SpringConfigurationMetadataGroup;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata.json.SpringConfigurationMetadataProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import static in.oneton.idea.spring.assistant.plugin.ClassUtil.findType;
import static in.oneton.idea.spring.assistant.plugin.Util.newListWithMembers;
import static in.oneton.idea.spring.assistant.plugin.Util.newSingleElementSortedSet;
import static java.util.Collections.unmodifiableList;

/**
 * Represents a node in the hierarchy of suggestions
 * Useful for navigating all suggestions & also acts as source of truth.
 * Note that some of the intermediate nodes might be there to just support the hierarchy
 * <p>
 * Also used for dot delimited search search. Each element corresponds to only a single section of the complete suggestion hierarchy
 * i.e if the we are building suggestions for
 * <ul>
 * <li>alpha.childNode11.charlie</li>
 * <li>alpha.childNode12.echo</li>
 * <li>alpha.echo</li>
 * <li>childNode11.charlie</li>
 * <li>childNode11.childNode12</li>
 * </ul>
 * <p>
 * The search for above properties would look like
 * <ul>
 * <li>
 * alpha
 * <ul>
 * <li>
 * childNode11
 * <ul>
 * <li>charlie</li>
 * </ul>
 * </li>
 * <li>
 * childNode12
 * <ul>
 * <li>echo</li>
 * </ul>
 * </li>
 * </ul>
 * </li>
 * <li>echo</li>
 * <li>
 * childNode11
 * <ul>
 * <li>charlie</li>
 * <li>childNode12</li>
 * </ul>
 * </li>
 * </ul>
 * <p>
 * We can expect a total of 5 tries in the complete search tree, each child trie being hosted by the parent trie element (+ a special toplevel trie for the whole tree)
 * <ul>
 * <li>(alpha + echo + childNode11) - for top level elements</li>
 * <li>alpha > (childNode11 + childNode12) - for children of <em>alpha</em></li>
 * <li>alpha > childNode11 > (charlie) - for children of <em>alpha > childNode11</em></li>
 * <li>alpha > childNode12 > (echo) - for children of <em>alpha > childNode12</em></li>
 * <li>childNode11 > (charlie + childNode12) - for children of <em>childNode11</em></li>
 * </ul>
 * <p>
 * <b>NOTE:</b> elements within the trie are indicated by enclosing them in curved brackets
 * <p>
 * <p>
 * This hierarchical trie is useful for searches like `a.ch.c` to correspond to `alpha.childNode11.charlie`
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(of = "originalName")
@EqualsAndHashCode(of = "name", callSuper = false)
public class MetadataNonPropertySuggestionNode extends MetadataSuggestionNode {

  private static final Logger log = Logger.getInstance(MetadataNonPropertySuggestionNode.class);

  /**
   * Sanitised name used for lookup. `-`, `_` are removed, upper cased characters are converted to lower case
   */
  private String name;
  /**
   * Section of the group/PROPERTY name. Sole purpose of this is to split all properties into their individual part
   */
  private String originalName;

  // TODO: Make sure that this will be part of search only if type & sourceType are part of the class path
  /**
   * Can be null for intermediate nodes that dont have a group entry in `spring-configuration-metadata.json`
   */
  @Nullable
  private SpringConfigurationMetadataGroup group;

  /**
   * Parent reference, for bidirectional navigation. Can be null for roots
   */
  @Nullable
  private MetadataNonPropertySuggestionNode parent;
  /**
   * Set of sources these suggestions belong to
   */
  private Set<String> belongsTo;
  /**
   * Child name -> child node. Aids in quick lookup. NOTE: All keys are sanitized
   */
  @Nullable
  private Map<String, MetadataSuggestionNode> childLookup;

  /**
   * Child trie for the nodes at next level, aids in prefix based searching. NOTE: All keys are sanitized
   */
  @Nullable
  private Trie<String, MetadataSuggestionNode> childrenTrie;

  /**
   * @param originalName name that is not sanitised
   * @param parent       parent MetadataNonPropertySuggestionNode node
   * @param belongsTo    file/jar containing this property
   * @return newly constructed group node
   */
  public static MetadataNonPropertySuggestionNode newInstance(String originalName,
      @Nullable MetadataNonPropertySuggestionNode parent, String belongsTo) {
    MetadataNonPropertySuggestionNodeBuilder builder =
        MetadataNonPropertySuggestionNode.builder().name(SuggestionNode.sanitise(originalName))
            .originalName(originalName).parent(parent);
    HashSet<String> belongsToSet = new HashSet<>();
    belongsToSet.add(belongsTo);
    builder.belongsTo(belongsToSet);
    return builder.build();
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
      boolean lastSegment = pathSegmentStartIndex == (pathSegments.length - 1);
      String pathSegment = pathSegments[pathSegmentStartIndex];
      if (hasChildren()) {
        assert childLookup != null;
        if (childLookup.containsKey(pathSegment)) {
          MetadataSuggestionNode child = childLookup.get(pathSegment);
          if (lastSegment) {
            deepestMatch = child;
          } else {
            deepestMatch = child
                .findDeepestMetadataNode(pathSegments, pathSegmentStartIndex + 1, matchAllSegments);
          }
          if (matchAllSegments && deepestMatch == null) {
            deepestMatch = this;
          }
        }
      } else if (lastSegment && name.equals(pathSegment)) {
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
      boolean lastSegment = pathSegmentStartIndex == (pathSegments.length - 1);
      String pathSegment = pathSegments[pathSegmentStartIndex];
      if (hasChildren()) {
        assert childLookup != null;
        if (childLookup.containsKey(pathSegment)) {
          MetadataSuggestionNode child = childLookup.get(pathSegment);
          if (lastSegment) {
            deepestMatch = child;
          } else {
            deepestMatch = child.findDeepestMatch(pathSegments, pathSegmentStartIndex + 1);
          }
        }
      } else if (lastSegment && name.equals(pathSegment)) {
        deepestMatch = this;
      }
    }

    return deepestMatch;
  }

  @Nullable
  @Override
  public List<SuggestionNode> findDeepestMatch(List<SuggestionNode> matchesRootTillCurrentNode,
      String[] pathSegments, int pathSegmentStartIndex) {
    List<SuggestionNode> deepestMatch = null;
    boolean haveMoreSegments = pathSegmentStartIndex < pathSegments.length;
    if (haveMoreSegments) {
      String currentPathSegment = pathSegments[pathSegmentStartIndex];
      boolean lastSegment = pathSegmentStartIndex == (pathSegments.length - 1);
      if (hasChildren()) {
        assert childLookup != null;
        if (childLookup.containsKey(currentPathSegment)) {
          MetadataSuggestionNode child = childLookup.get(currentPathSegment);
          matchesRootTillCurrentNode.add(child);
          if (lastSegment) {
            deepestMatch = matchesRootTillCurrentNode;
          } else {
            deepestMatch = child.findDeepestMatch(matchesRootTillCurrentNode, pathSegments,
                pathSegmentStartIndex + 1);
          }
        }
      } else if (lastSegment && name.equals(currentPathSegment)) {
        deepestMatch = matchesRootTillCurrentNode;
      }
    } else {
      deepestMatch = matchesRootTillCurrentNode;
    }

    return deepestMatch;
  }

  public void addChildren(Module module, SpringConfigurationMetadataGroup group,
      String[] pathSegments, int startIndex, String belongsTo) {
    MetadataNonPropertySuggestionNode groupNode =
        addChildren(pathSegments, startIndex, pathSegments.length - 1, belongsTo);
    groupNode.setGroup(module, group);
  }

  public void addChildren(Module module, SpringConfigurationMetadataProperty property,
      String[] pathSegments, int startIndex, String belongsTo) {
    MetadataNonPropertySuggestionNode parentNode;
    if (pathSegments.length == 1) {
      parentNode = this;
      addRefCascadeTillRoot(belongsTo);
    } else {
      parentNode = addChildren(pathSegments, startIndex, pathSegments.length - 2, belongsTo);
    }

    parentNode.addProperty(module, property, pathSegments[pathSegments.length - 1], belongsTo);
  }

  private void addProperty(Module module, SpringConfigurationMetadataProperty property,
      String pathSegment, String belongsTo) {
    addRefCascadeTillRoot(belongsTo);
    if (!hasChildren()) {
      childLookup = new THashMap<>();
      childrenTrie = new PatriciaTrie<>();
    }

    assert childLookup != null;
    assert childrenTrie != null;
    MetadataSuggestionNode childNode =
        MetadataPropertySuggestionNode.newInstance(module, pathSegment, property, this, belongsTo);

    childLookup.put(pathSegment, childNode);
    childrenTrie.put(pathSegment, childNode);
  }

  private MetadataNonPropertySuggestionNode addChildren(String[] pathSegments, int startIndex,
      int endIndexIncl, String belongsTo) {
    addRefCascadeTillRoot(belongsTo);
    if (!hasChildren()) {
      childLookup = new THashMap<>();
      childrenTrie = new PatriciaTrie<>();
    }

    assert childLookup != null;
    assert childrenTrie != null;

    String pathSegment = pathSegments[startIndex];
    MetadataNonPropertySuggestionNode childNode =
        MetadataNonPropertySuggestionNode.class.cast(childLookup.get(pathSegment));
    if (childNode == null) {
      childNode = MetadataNonPropertySuggestionNode.newInstance(pathSegment, this, belongsTo);
      childNode.setParent(this);

      childLookup.put(pathSegment, childNode);
      childrenTrie.put(pathSegment, childNode);
    }

    // If this is the last segment, lets set group
    if (startIndex == endIndexIncl) {
      return childNode;
    } else {
      return childNode.addChildren(pathSegments, startIndex + 1, endIndexIncl, belongsTo);
    }
  }

  @Override
  @Nullable
  protected SortedSet<Suggestion> findSuggestionsForKey(String ancestralKeysDotDelimited,
      List<SuggestionNode> matchesRootTillParentNode, String[] querySegmentPrefixes,
      int querySegmentPrefixStartIndex, boolean navigateDeepIfNoMatches) {
    boolean lookingForConcreteNode = querySegmentPrefixStartIndex >= querySegmentPrefixes.length;
    if (lookingForConcreteNode) {
      if (isGroup()) {
        // If we have only one child, lets send the child value directly instead of this node. This way user does not need trigger suggestion for level, esp. when we know there will is only be one child
        if (hasOnlyOneChild()) {
          assert childrenTrie != null;
          return addThisToMatchesAndSearchInNextLevel(ancestralKeysDotDelimited,
              matchesRootTillParentNode, querySegmentPrefixes, querySegmentPrefixStartIndex, false,
              childrenTrie.values());
        } else { // either there are no children/multiple children are present. Lets return suggestions
          assert group != null;
          return newSingleElementSortedSet(
              group.newSuggestion(ancestralKeysDotDelimited, matchesRootTillParentNode, this));
        }
      } else { // intermediate node, lets get all next level groups & properties
        assert childrenTrie != null;
        return addThisToMatchesAndSearchInNextLevel(ancestralKeysDotDelimited,
            matchesRootTillParentNode, querySegmentPrefixes, querySegmentPrefixStartIndex, false,
            childrenTrie.values());
      }
    } else {
      if (hasChildren()) {
        assert childrenTrie != null;
        String querySegmentPrefix = querySegmentPrefixes[querySegmentPrefixStartIndex];
        SortedMap<String, MetadataSuggestionNode> sortedPrefixToMetadataNode =
            childrenTrie.prefixMap(querySegmentPrefix);
        Collection<MetadataSuggestionNode> matchedChildren = sortedPrefixToMetadataNode.values();
        if (matchedChildren.size() != 0) {
          boolean lastQuerySegment =
              querySegmentPrefixStartIndex == (querySegmentPrefixes.length - 1);
          return addThisToMatchesAndSearchInNextLevel(ancestralKeysDotDelimited,
              matchesRootTillParentNode, querySegmentPrefixes, querySegmentPrefixStartIndex + 1,
              !lastQuerySegment, matchedChildren);
        } else if (navigateDeepIfNoMatches) {
          return addThisToMatchesAndSearchInNextLevel(ancestralKeysDotDelimited,
              matchesRootTillParentNode, querySegmentPrefixes, querySegmentPrefixStartIndex, true,
              childrenTrie.values());
        }
      }
      return null;
    }
  }

  @Override
  protected boolean hasOnlyOneChild() {
    return isLeaf() || (childrenTrie != null && childrenTrie.size() == 1);
    //     && childrenTrie.values().stream()
    //        .allMatch(MetadataSuggestionNode::hasOnlyOneChild)
  }

  private SortedSet<Suggestion> addThisToMatchesAndSearchInNextLevel(
      String ancestralKeysDotDelimited, List<SuggestionNode> matchesRootTillParentNode,
      String[] querySegmentPrefixes, int querySegmentPrefixStartIndex,
      boolean navigateDeepIfNoMatches, Collection<MetadataSuggestionNode> childNodes) {
    SortedSet<Suggestion> suggestions = null;
    for (MetadataSuggestionNode child : childNodes) {
      List<SuggestionNode> pathRootTillCurrentNode =
          unmodifiableList(newListWithMembers(matchesRootTillParentNode, this));
      Set<Suggestion> matchedSuggestions = child
          .findSuggestionsForKey(ancestralKeysDotDelimited, pathRootTillCurrentNode,
              querySegmentPrefixes, querySegmentPrefixStartIndex, navigateDeepIfNoMatches);
      if (matchedSuggestions != null) {
        if (suggestions == null) {
          suggestions = new TreeSet<>();
        }
        suggestions.addAll(matchedSuggestions);
      }
    }
    return suggestions;
  }

  /**
   * @param containerPath Represents path to the metadata file container
   * @return true if no children left & this item does not belong to any other source
   */
  @Override
  public boolean removeRefCascadeDown(String containerPath) {
    belongsTo.remove(containerPath);
    // If the current node & all its children belong to a single file, lets remove the whole tree
    if (belongsTo.size() == 0) {
      return true;
    }

    if (hasChildren()) {
      assert childLookup != null;
      assert childrenTrie != null;
      Iterator<MetadataSuggestionNode> iterator = childrenTrie.values().iterator();
      while (iterator.hasNext()) {
        MetadataSuggestionNode child = iterator.next();
        boolean canRemoveReference = child.removeRefCascadeDown(containerPath);
        if (canRemoveReference) {
          iterator.remove();
          childLookup.remove(child.getName());
          childrenTrie.remove(child.getName());
        }
      }
      if (!hasChildren()) {
        childLookup = null;
        childrenTrie = null;
      }
    }
    return false;
  }

  @Override
  protected boolean isRoot() {
    return parent == null;
  }

  @Override
  public boolean isGroup() {
    return true;
  }

  @Override
  public boolean isProperty() {
    return false;
  }

  @NotNull
  @Override
  public String getDocumentationForKey(String nodeNavigationPathDotDelimited) {
    if (isGroup()) {
      assert group != null;
      return group.getDocumentation(nodeNavigationPathDotDelimited);
    }
    throw new RuntimeException(
        "Documentation not supported for this element. Call supportsDocumentation() first");
  }

  @Nullable
  @Override
  public SortedSet<Suggestion> findSuggestionsForValue(String prefix) {
    return null;
  }

  @Nullable
  @Override
  public String getDocumentationForValue(String nodeNavigationPathDotDelimited, String value) {
    return null;
  }

  @Override
  public boolean isLeaf() {
    return !hasChildren();
  }

  private boolean hasChildren() {
    return childrenTrie != null && childrenTrie.size() != 0;
  }

  @NotNull
  @Override
  public SuggestionNodeType getType() {
    if (isGroup()) {
      assert group != null;
      return group.getNodeType();
    } else {
      return SuggestionNodeType.UNDEFINED;
    }
  }

  public void setGroup(Module module, SpringConfigurationMetadataGroup group) {
    updateGroupType(module, group);
    this.group = group;
  }

  @Override
  public void refreshClassProxy(Module module) {
    updateGroupType(module, group);
    if (hasChildren()) {
      assert childLookup != null;
      childLookup.values().forEach(child -> child.refreshClassProxy(module));
    }
  }

  private void updateGroupType(Module module, SpringConfigurationMetadataGroup group) {
    if (group != null && group.getType() != null) {
      PsiClassType groupClassType = ClassUtil.safeFindClassType(module, group.getType());
      if (groupClassType != null) {
        group.setNodeType(findType(groupClassType));
      } else {
        group.setNodeType(SuggestionNodeType.UNKNOWN_CLASS);
      }
    }
  }

}
