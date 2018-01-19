package in.oneton.idea.spring.assistant.plugin.model;

import com.intellij.openapi.diagnostic.Logger;
import in.oneton.idea.spring.assistant.plugin.model.json.SpringConfigurationMetadataDeprecationLevel;
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
@EqualsAndHashCode(of = "name")
public class MetadataNode implements SuggestionNode {

  private static final Logger log = Logger.getInstance(MetadataNode.class);

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
   * Can be null for properties & intermediate nodes that dont have a group entry in `spring-configuration-metadata.json`
   */
  @Nullable
  private SpringConfigurationMetadataGroup group;

  // TODO: Make sure that this will be part of search only if type & sourceType are part of the class path
  @Nullable
  private SpringConfigurationMetadataProperty property;

  /**
   * Parent reference, for bidirectional navigation. Can be null for roots
   */
  @Nullable
  private MetadataNode parent;
  @Nullable
  private Set<MetadataNode> children;
  /**
   * Child trie for the nodes at next level, aids in searching
   */
  @Nullable
  private Trie<String, MetadataNode> sanitisedChildTrie;
  /**
   * Set of sources these suggestions belong to
   */
  private Set<String> belongsTo;

  public static MetadataNode newInstance(String name, @Nullable MetadataNode parent,
      String belongsTo) {
    MetadataNodeBuilder builder =
        MetadataNode.builder().name(sanitize(name)).originalName(name).parent(parent);
    HashSet<String> belongsToSet = new HashSet<>();
    belongsToSet.add(belongsTo);
    builder.belongsTo(belongsToSet);
    return builder.build();
  }

  @Override
  public MetadataNode findDeepestMatch(String[] pathSegments, int startWith,
      boolean matchAllSegments) {
    MetadataNode deepestMatch = null;
    if (!matchAllSegments) {
      deepestMatch = this;
    }
    boolean haveMoreSegments = startWith < pathSegments.length;
    if (haveMoreSegments) {
      boolean lastSegment = startWith == (pathSegments.length - 1);
      String sanitizedPathSegment = sanitize(pathSegments[startWith]);
      if (hasChildren()) {
        assert children != null;
        for (MetadataNode child : children) {
          if (child.name.equals(sanitizedPathSegment)) {
            if (lastSegment) {
              deepestMatch = child;
            } else {
              deepestMatch = child.findDeepestMatch(pathSegments, startWith + 1, matchAllSegments);
            }
            if (deepestMatch != null) {
              break;
            }
          }
        }
        // After going over all children, if deepest match could not be found, I am the deepest node
        if (!matchAllSegments && deepestMatch == null) {
          deepestMatch = this;
        }
      } else if (lastSegment && name.equals(sanitizedPathSegment)) {
        deepestMatch = this;
      }
    }

    return deepestMatch;
  }

  public void addChildren(SpringConfigurationMetadataGroup group, String[] pathSegments,
      String belongsTo) {
    int startIndex = computeStartIndexAndAddSourcePath();
    addSourcePathTillRoot(belongsTo);

    // Case where alpha.childNode11.charlie is already added via source 1 & source2 tries to add a group for alpha.childNode11
    if (startIndex >= pathSegments.length) {
      if (this.group == null) {
        this.group = group;
      }
    } else {
      if (children == null) {
        children = new HashSet<>();
        sanitisedChildTrie = new PatriciaTrie<>();
      }

      String pathSegment = pathSegments[startIndex];
      String sanitizedPathSegment = sanitize(pathSegment);
      MetadataNode childNode = MetadataNode.newInstance(pathSegment, this, belongsTo);

      // If this is the last segment, lets set group
      boolean noMoreSegmentsLeft = startIndex == pathSegments.length - 1;
      if (noMoreSegmentsLeft) {
        childNode.setGroup(group);
      }
      children.add(childNode);
      childNode.setParent(this);
      sanitisedChildTrie.put(sanitizedPathSegment, childNode);
      childNode.addChildren(group, pathSegments, belongsTo);
    }
  }

  public void addChildren(SpringConfigurationMetadataProperty property, String[] pathSegments,
      String belongsTo) {
    int startIndex = computeStartIndexAndAddSourcePath();
    addSourcePathTillRoot(belongsTo);

    // Case where alpha.childNode11.charlie is already added via source 1 & source2 tries to add a group for alpha.childNode11
    if (startIndex >= pathSegments.length) {
      if (this.property == null) {
        this.property = property;
      }
    } else {
      if (children == null) {
        children = new HashSet<>();
        sanitisedChildTrie = new PatriciaTrie<>();
      }

      String pathSegment = pathSegments[startIndex];
      String sanitizedPathSegment = sanitize(pathSegment);
      MetadataNode childNode = MetadataNode.newInstance(pathSegment, this, belongsTo);

      // If this is the last segment, lets set path
      boolean noMoreSegmentsLeft = startIndex == pathSegments.length - 1;
      if (noMoreSegmentsLeft) {
        childNode.setProperty(property);
      }
      children.add(childNode);
      childNode.setParent(this);
      sanitisedChildTrie.put(sanitizedPathSegment, childNode);
      childNode.addChildren(property, pathSegments, belongsTo);
    }
  }

  @Override
  @Nullable
  public Set<Suggestion> findSuggestions(String[] querySegments, int suggestionDepth, int startWith,
      boolean forceSearchAcrossTree) {
    if (isLeaf()) {
      if (propertyCanBeShownAsSuggestion() && (startWith >= querySegments.length || name.startsWith(sanitize(querySegments[startWith])))) {
        assert property != null;
        return newSingleElementSet(
            property.newSuggestion(this, computeSuggestion(suggestionDepth)));
      }
      return null;
    } else {
      if (isGroup() && startWith >= querySegments.length) {
        // If we have only one leaf, lets send the leaf value directly instead of this node
        if (hasOnlyOneLeaf()) {
          return findChildSuggestions(querySegments, suggestionDepth, startWith,
              forceSearchAcrossTree);
        } else {
          assert group != null;
          return newSingleElementSet(group.newSuggestion(this, computeSuggestion(suggestionDepth)));
        }
      } else {
        return findChildSuggestions(querySegments, suggestionDepth, startWith, forceSearchAcrossTree);
      }
    }
  }

  private boolean hasOnlyOneLeaf() {
    return isLeaf() || (children != null && children.size() == 1 && children.stream()
        .allMatch(MetadataNode::hasOnlyOneLeaf));
  }

  @Override
  @Nullable
  public Set<Suggestion> findChildSuggestions(String[] querySegments, int suggestionDepth,
      int startWith, boolean forceSearchAcrossTree) {
    Set<Suggestion> suggestions = null;
    // If we are searching for `spring` & spring does not have a group/property associated, we would want to go deep till we find all next level descendants that are either groups & properties, so that user dont get bombarded with too many or too little values
    if (startWith >= querySegments.length) {
      if (children == null || sanitisedChildTrie == null) {
        return null;
      } else {
        suggestions =
            findSuggestions(querySegments, suggestionDepth + 1, startWith + 1, false, children);
        if (suggestions == null && forceSearchAcrossTree) {
          // Since the search did not match any results, lets ask all the children if any of their have matches
          suggestions =
              findSuggestions(querySegments, suggestionDepth + 1, startWith + 1, true, children);
        }
      }
    } else {
      if (sanitisedChildTrie != null) {
        String sanitisedQuerySegment = sanitize(querySegments[startWith]);
        SortedMap<String, MetadataNode> sortedPrefixToMetadataNode = sanitisedChildTrie.prefixMap(sanitisedQuerySegment);
        Collection<MetadataNode> matchedChildren = sortedPrefixToMetadataNode.values();
        if (matchedChildren.size() != 0) {
          suggestions = findSuggestions(querySegments, suggestionDepth + 1, startWith + 1,
              forceSearchAcrossTree, matchedChildren);
        }

        if (suggestions == null && forceSearchAcrossTree) {
          // Since we will be searching in the next level assuming the current level is a match
          // 1. Let modify the search segments so that we retain the search string position
          // 2. We pass along the match to the next level
          // To do this, lets copy the current node's text into `querySegments` just before current segment, so that suggestions would be aware of the parent context when showing suggestions
          String[] newQuerySegments = new String[querySegments.length + 1];
          newQuerySegments[startWith + 1] = querySegments[startWith];
          newQuerySegments[startWith] = originalName;
          if (querySegments.length > 1) {
            arraycopy(querySegments, 0, newQuerySegments, 0, startWith);
          }
          if (startWith + 1 < querySegments.length) {
            arraycopy(querySegments, startWith + 1, newQuerySegments, startWith + 2,
                (querySegments.length - startWith));
          }
          // Since the search did not match any results, lets ask all the children if any of their children have matches
          for (MetadataNode child : sanitisedChildTrie.values()) {
            Set<Suggestion> matchedSuggestions =
                child.findSuggestions(newQuerySegments, suggestionDepth + 1, startWith + 1, true);
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

  private Set<Suggestion> findSuggestions(String[] querySegments, int suggestionDepth,
      int startWith, boolean forceSearchAcrossTree, Collection<MetadataNode> childNodes) {
    Set<Suggestion> suggestions = null;
    for (MetadataNode child : childNodes) {
      Set<Suggestion> matchedSuggestions =
          child.findSuggestions(querySegments, suggestionDepth, startWith, forceSearchAcrossTree);
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
  public boolean removeRef(String containerPath) {
    belongsTo.remove(containerPath);
    // If the current node & all its children belong to a single file, lets remove the whole tree
    if (belongsTo.size() == 0) {
      return true;
    }

    if (hasChildren()) {
      assert children != null;
      assert sanitisedChildTrie != null;
      Iterator<MetadataNode> iterator = children.iterator();
      while (iterator.hasNext()) {
        MetadataNode child = iterator.next();
        boolean canRemoveReference = child.removeRef(containerPath);
        if (canRemoveReference) {
          iterator.remove();
          sanitisedChildTrie.remove(child.name);
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
  public String getFullPath() {
    StringBuilder builder = new StringBuilder();
    builder.append(originalName);
    MetadataNode next = this.parent;
    while (next != null) {
      builder.insert(0, ".").insert(0, next.originalName);
      next = next.parent;
    }
    return builder.toString();
  }

  private boolean isRoot() {
    return parent == null;
  }

  @Override
  public boolean isGroup() {
    return group != null;
  }

  private boolean hasChildren() {
    return children != null && children.size() != 0;
  }

  @Override
  public boolean isLeaf() {
    return property != null;
  }

  @Override
  public int getDepth() {
    int depth = 0;
    MetadataNode current = this;
    do {
      depth++;
      current = current.parent;
    } while (current != null);
    return depth;
  }

  @Nullable
  @Override
  public Set<Suggestion> getSuggestionValues() {
    assert property != null;
    return property.getValueSuggestions(this);
  }

  @NotNull
  private HashSet<Suggestion> newSingleElementSet(Suggestion suggestion) {
    HashSet<Suggestion> suggestions = new HashSet<>();
    suggestions.add(suggestion);
    return suggestions;
  }

  private boolean propertyCanBeShownAsSuggestion() {
    assert property != null;
    return !property.isDeprecated() || property.getDeprecation() == null
        || property.getDeprecation().getLevel()
        != SpringConfigurationMetadataDeprecationLevel.error;
  }

  private void addSourcePathTillRoot(String containerPath) {
    MetadataNode node = this;
    do {
      if (node.belongsTo.contains(containerPath)) {
        break;
      }
      node.belongsTo.add(containerPath);
      node = node.parent;
    } while (node != null && !node.isRoot());
  }

  private int computeStartIndexAndAddSourcePath() {
    int startWithIndex = 0;
    MetadataNode node = this;
    do {
      startWithIndex++;
      node = node.parent;
    } while (node != null);
    return startWithIndex;
  }

  private String computeSuggestion(int numOfHops) {
    StringBuilder builder = new StringBuilder();

    boolean appendDot = false;
    MetadataNode currentNode = this;
    while (currentNode != null && numOfHops > 0) {
      builder.insert(0, currentNode.originalName + (appendDot ? "." : ""));
      appendDot = true;
      currentNode = currentNode.parent;
      numOfHops--;
    }

    return builder.toString();
  }

  @Override
  public String getSuggestionReplacement(String existingIndentation, String indentPerLevel,
      int maxDepth) {
    int numOfHops = 0;
    MetadataNode currentNode = this;

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
