package in.oneton.idea.spring.assistant.plugin.model;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import in.oneton.idea.spring.assistant.plugin.model.json.SpringConfigurationMetadataGroup;
import in.oneton.idea.spring.assistant.plugin.model.json.SpringConfigurationMetadataProperty;
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
import java.util.Set;
import java.util.SortedMap;

import static in.oneton.idea.spring.assistant.plugin.model.SuggestionNode.sanitize;
import static java.lang.System.arraycopy;

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
public class MetadataGroupSuggestionNode extends MetadataSuggestionNode {

  private static final Logger log = Logger.getInstance(MetadataGroupSuggestionNode.class);

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
  private MetadataGroupSuggestionNode parent;
  /**
   * Set of sources these suggestions belong to
   */
  private Set<String> belongsTo;
  @Nullable
  private Set<MetadataSuggestionNode> children;
  /**
   * Child trie for the nodes at next level, aids in searching
   */
  @Nullable
  private Trie<String, MetadataSuggestionNode> sanitisedChildTrie;
  private SuggestionNodeType type;

  public static MetadataGroupSuggestionNode newInstance(String name,
      @Nullable MetadataGroupSuggestionNode parent, String belongsTo) {
    MetadataGroupSuggestionNodeBuilder builder =
        MetadataGroupSuggestionNode.builder().name(sanitize(name)).originalName(name)
            .parent(parent);
    HashSet<String> belongsToSet = new HashSet<>();
    belongsToSet.add(belongsTo);
    builder.belongsTo(belongsToSet);
    return builder.build();
  }

  @Override
  public MetadataSuggestionNode findDeepestMatch(String[] pathSegments, int pathSegmentStartIndex,
      boolean matchAllSegments) {
    MetadataSuggestionNode deepestMatch = null;
    if (!matchAllSegments) {
      deepestMatch = this;
    }
    boolean haveMoreSegments = pathSegmentStartIndex < pathSegments.length;
    if (haveMoreSegments) {
      boolean lastSegment = pathSegmentStartIndex == (pathSegments.length - 1);
      String sanitizedPathSegment = sanitize(pathSegments[pathSegmentStartIndex]);
      if (hasChildren()) {
        assert children != null;
        for (MetadataSuggestionNode child : children) {
          if (child.getName().equals(sanitizedPathSegment)) {
            if (lastSegment) {
              deepestMatch = child;
            } else {
              deepestMatch =
                  child.findDeepestMatch(pathSegments, pathSegmentStartIndex + 1, matchAllSegments);
            }
            if (deepestMatch != null) {
              break;
            }
          }
        }
      } else if (lastSegment && name.equals(sanitizedPathSegment)) {
        deepestMatch = this;
      }
    }

    return deepestMatch;
  }

  public void addChildren(SpringConfigurationMetadataGroup group, String[] pathSegments,
      int startIndex, String belongsTo) {
    addRefCascadeTillRoot(belongsTo);
    if (children == null) {
      children = new HashSet<>();
      sanitisedChildTrie = new PatriciaTrie<>();
    }

    String pathSegment = pathSegments[startIndex];
    String sanitizedPathSegment = sanitize(pathSegment);
    MetadataGroupSuggestionNode childNode =
        MetadataGroupSuggestionNode.newInstance(pathSegment, this, belongsTo);

    children.add(childNode);
    childNode.setParent(this);

    assert sanitisedChildTrie != null;
    sanitisedChildTrie.put(sanitizedPathSegment, childNode);

    // If this is the last segment, lets set group
    boolean lastSegment = startIndex == pathSegments.length - 1;
    if (lastSegment) {
      childNode.setGroup(group);
    } else {
      childNode.addChildren(group, pathSegments, startIndex + 1, belongsTo);
    }
  }

  public void addChildren(Module module, SpringConfigurationMetadataProperty property,
      String[] pathSegments, int startIndex, String belongsTo) {
    addRefCascadeTillRoot(belongsTo);
    if (children == null) {
      children = new HashSet<>();
      sanitisedChildTrie = new PatriciaTrie<>();
    }

    String pathSegment = pathSegments[startIndex];
    String sanitizedPathSegment = sanitize(pathSegment);

    // If this is the last segment, lets set path
    boolean lastSegment = startIndex == pathSegments.length - 1;

    MetadataSuggestionNode childNode;
    if (lastSegment) {
      childNode = MetadataPropertySuggestionNode
          .newInstance(module, pathSegment, property, this, belongsTo);
    } else {
      childNode = MetadataGroupSuggestionNode.newInstance(pathSegment, this, belongsTo);
    }
    children.add(childNode);
    sanitisedChildTrie.put(sanitizedPathSegment, childNode);

    if (!lastSegment) {
      MetadataGroupSuggestionNode.class.cast(childNode)
          .addChildren(module, property, pathSegments, startIndex + 1, belongsTo);
    }
  }

  @Override
  @Nullable
  public Set<Suggestion> findSuggestionsForKey(List<MetadataSuggestionNode> pathRootTillParentNode,
      int suggestionDepthFromEndOfPath, String[] querySegmentPrefixes,
      int querySegmentPrefixStartIndex, boolean navigateDeepIfNoMatches) {
    if (isLeaf()) {
      if (querySegmentPrefixStartIndex >= querySegmentPrefixes.length || name
          .startsWith(sanitize(querySegmentPrefixes[querySegmentPrefixStartIndex]))) {
        assert property != null;
        pathRootTillParentNode.add(this);
        return newSingleElementSet(property.newSuggestion(pathRootTillParentNode,
            computeSuggestion(suggestionDepthFromEndOfPath)));
      }
      return null;
    } else {
      if (isGroup() && querySegmentPrefixStartIndex >= querySegmentPrefixes.length) {
        // If we have only one leaf, lets send the leaf value directly instead of this node
        if (hasOnlyOneLeaf()) {
          pathRootTillParentNode.add(this);
          return findChildSuggestionsForKey(pathRootTillParentNode, suggestionDepthFromEndOfPath,
              querySegmentPrefixes, querySegmentPrefixStartIndex, navigateDeepIfNoMatches);
        } else {
          assert group != null;
          pathRootTillParentNode.add(this);
          return newSingleElementSet(group.newSuggestion(pathRootTillParentNode,
              computeSuggestion(suggestionDepthFromEndOfPath)));
        }
      } else {
        return findChildSuggestionsForKey(pathRootTillParentNode, suggestionDepthFromEndOfPath,
            querySegmentPrefixes, querySegmentPrefixStartIndex, navigateDeepIfNoMatches);
      }
    }
  }

  @Override
  @Nullable
  public Set<Suggestion> findChildSuggestionsForKey(
      List<MetadataSuggestionNode> pathRootTillParentNode, int suggestionDepthFromEndOfPath,
      String[] querySegmentPrefixes, int querySegmentPrefixStartIndex,
      boolean navigateDeepIfNoMatches) {
    Set<Suggestion> suggestions = null;
    // If we are searching for `spring` & spring does not have a group/property associated, we would want to go deep till we find all next level descendants that are either groups & properties, so that user dont get bombarded with too many or too little values
    if (querySegmentPrefixStartIndex >= querySegmentPrefixes.length) {
      if (children == null || sanitisedChildTrie == null) {
        return null;
      } else {
        suggestions = findSuggestions(pathRootTillParentNode, suggestionDepthFromEndOfPath + 1,
            querySegmentPrefixes, querySegmentPrefixStartIndex + 1, false, children);
        if (suggestions == null && navigateDeepIfNoMatches) {
          // Since the search did not match any results, lets ask all the children if any of their have matches
          suggestions = findSuggestions(pathRootTillParentNode, suggestionDepthFromEndOfPath + 1,
              querySegmentPrefixes, querySegmentPrefixStartIndex + 1, true, children);
        }
      }
    } else {
      if (sanitisedChildTrie != null) {
        String sanitisedQuerySegment = sanitize(querySegmentPrefixes[querySegmentPrefixStartIndex]);
        SortedMap<String, MetadataSuggestionNode> sortedPrefixToMetadataNode =
            sanitisedChildTrie.prefixMap(sanitisedQuerySegment);
        Collection<MetadataSuggestionNode> matchedChildren = sortedPrefixToMetadataNode.values();
        if (matchedChildren.size() != 0) {
          suggestions = findSuggestions(pathRootTillParentNode, suggestionDepthFromEndOfPath + 1,
              querySegmentPrefixes, querySegmentPrefixStartIndex + 1, navigateDeepIfNoMatches,
              matchedChildren);
        }

        if (suggestions == null && navigateDeepIfNoMatches) {
          // Since we will be searching in the next level assuming the current level is a match
          // 1. Let modify the search segments so that we retain the search string position
          // 2. We pass along the match to the next level
          // To do this, lets copy the current node's text into `querySegmentPrefixes` just before current segment, so that suggestions would be aware of the parent context when showing suggestions
          String[] newQuerySegmentPrefixes = new String[querySegmentPrefixes.length + 1];
          newQuerySegmentPrefixes[querySegmentPrefixStartIndex + 1] =
              querySegmentPrefixes[querySegmentPrefixStartIndex];
          newQuerySegmentPrefixes[querySegmentPrefixStartIndex] = originalName;
          if (querySegmentPrefixes.length > 1) {
            arraycopy(querySegmentPrefixes, 0, newQuerySegmentPrefixes, 0,
                querySegmentPrefixStartIndex);
          }
          if (querySegmentPrefixStartIndex + 1 < querySegmentPrefixes.length) {
            arraycopy(querySegmentPrefixes, querySegmentPrefixStartIndex + 1,
                newQuerySegmentPrefixes, querySegmentPrefixStartIndex + 2,
                (querySegmentPrefixes.length - querySegmentPrefixStartIndex));
          }
          // Since the search did not match any results, lets ask all the children if any of their children have matches
          for (MetadataSuggestionNode child : sanitisedChildTrie.values()) {
            Set<Suggestion> matchedSuggestions = child
                .findSuggestionsForKey(pathRootTillParentNode, suggestionDepthFromEndOfPath + 1,
                    newQuerySegmentPrefixes, querySegmentPrefixStartIndex + 1, true);
            if (matchedSuggestions != null) {
              if (suggestions == null) {
                suggestions = new HashSet<>();
              }
              suggestions.addAll(matchedSuggestions);
            }
          }
        }
      }
    }
    return suggestions;
  }

  @Override
  protected boolean hasOnlyOneLeaf() {
    return isLeaf() || (children != null && children.size() == 1 && children.stream()
        .allMatch(MetadataSuggestionNode::hasOnlyOneLeaf));
  }

  private Set<Suggestion> findSuggestions(List<MetadataSuggestionNode> matchesRootTillParentNode,
      int suggestionDepthFromEndOfMatches, String[] querySegmentPrefixes,
      int querySegmentPrefixStartIndex, boolean navigateDeepIfNoMatches,
      Collection<MetadataSuggestionNode> childNodes) {
    Set<Suggestion> suggestions = null;
    for (MetadataSuggestionNode child : childNodes) {
      Set<Suggestion> matchedSuggestions = child
          .findSuggestionsForKey(matchesRootTillParentNode, suggestionDepthFromEndOfMatches,
              querySegmentPrefixes, querySegmentPrefixStartIndex, navigateDeepIfNoMatches);
      if (matchedSuggestions != null) {
        if (suggestions == null) {
          suggestions = new HashSet<>();
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
  public boolean removeRefCascadeDown(String containerPath) {
    belongsTo.remove(containerPath);
    // If the current node & all its children belong to a single file, lets remove the whole tree
    if (belongsTo.size() == 0) {
      return true;
    }

    if (hasChildren()) {
      assert children != null;
      assert sanitisedChildTrie != null;
      Iterator<MetadataSuggestionNode> iterator = children.iterator();
      while (iterator.hasNext()) {
        MetadataSuggestionNode child = iterator.next();
        boolean canRemoveReference = child.removeRefCascadeDown(containerPath);
        if (canRemoveReference) {
          iterator.remove();
          sanitisedChildTrie.remove(child.getName());
        }
      }
      if (children.size() == 0) {
        children = null;
        sanitisedChildTrie = null;
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

  @Override
  public boolean isLeaf() {
    return !hasChildren();
  }

  private boolean hasChildren() {
    return children != null && children.size() != 0;
  }

  public int getDepth() {
    int depth = 0;
    MetadataGroupSuggestionNode current = this;
    do {
      depth++;
      current = current.parent;
    } while (current != null);
    return depth;
  }

  @Override
  public SuggestionNodeType getType() {
    return type;
  }

  @NotNull
  private HashSet<Suggestion> newSingleElementSet(Suggestion suggestion) {
    HashSet<Suggestion> suggestions = new HashSet<>();
    suggestions.add(suggestion);
    return suggestions;
  }

  private int computeStartIndexAndAddSourcePath() {
    int startWithIndex = 0;
    MetadataGroupSuggestionNode node = this;
    do {
      startWithIndex++;
      node = node.parent;
    } while (node != null);
    return startWithIndex;
  }

  private String computeSuggestion(int numOfHops) {
    StringBuilder builder = new StringBuilder();

    boolean appendDot = false;
    MetadataGroupSuggestionNode currentNode = this;
    while (currentNode != null && numOfHops > 0) {
      builder.insert(0, currentNode.originalName + (appendDot ? "." : ""));
      appendDot = true;
      currentNode = currentNode.parent;
      numOfHops--;
    }

    return builder.toString();
  }
}
