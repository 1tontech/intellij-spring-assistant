package in.oneton.idea.spring.boot.config.autosuggest.plugin.model;

import in.oneton.idea.spring.boot.config.autosuggest.plugin.model.json.SpringConfigurationMetadataGroup;
import in.oneton.idea.spring.boot.config.autosuggest.plugin.model.json.SpringConfigurationMetadataProperty;
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

import static in.oneton.idea.spring.boot.config.autosuggest.plugin.model.json.SpringConfigurationMetadataDeprecationLevel.error;

/**
 * Represents a node in the hierarchy of suggestions
 * Useful for navigating all suggestions & also acts as source of truth.
 * Note that some of the intermediate nodes might be there to just support the hierarchy
 * <p>
 * Also used for dot delimited search search. Each element corresponds to only a single section of the complete suggestion hierarchy
 * i.e if the we are building suggestions for
 * alpha.childNode11.charlie
 * alpha.childNode12.echo
 * alpha.echo
 * childNode11.charlie
 * childNode11.childNode12
 * <p>
 * The search for above properties would look like
 * <p>
 * alpha
 * childNode11
 * charlie
 * childNode12
 * echo
 * childNode11
 * charlie
 * childNode12
 * <p>
 * We can expect a total of 4 tries in the complete search tree, each child trie being hosted by the parent trie element. This is represented below by `>`
 * <p>
 * (alpha + childNode11) - 1st trie
 * alpha > (childNode11 + childNode12 + echo) - 2nd trie
 * alpha > childNode11 > charlie - 3rd trie
 * childNode11 > (charlie + childNode12) - 4th trie
 * <p>
 * This hierarchical trie is useful for searches like `a.b` to correspond to `alpha.childNode11.charlie`
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(of = "originalName")
@EqualsAndHashCode(of = "name")
public class MetadataNode {
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
  private Set<SuggestionSourcePathAndTimestamp> belongsTo;

  public static MetadataNode newInstance(String name, @Nullable MetadataNode parent,
      SuggestionSourcePathAndTimestamp sourcePathAndTimestamp) {
    MetadataNodeBuilder builder =
        MetadataNode.builder().name(sanitize(name)).originalName(name).parent(parent);
    HashSet<SuggestionSourcePathAndTimestamp> belongsTo = new HashSet<>();
    belongsTo.add(sourcePathAndTimestamp);
    builder.belongsTo(belongsTo);
    return builder.build();
  }

  public static String sanitize(String name) {
    return name.replaceAll("_", "").replace("-", "").toLowerCase();
  }

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
      SuggestionSourcePathAndTimestamp sourcePathAndTimestamp) {
    int startIndex = computeStartIndexAndAddSourcePath();
    addSourcePathTillRoot(sourcePathAndTimestamp);

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
      MetadataNode childNode = MetadataNode.newInstance(pathSegment, this, sourcePathAndTimestamp);

      // If this is the last segment, lets set group
      boolean noMoreSegmentsLeft = startIndex == pathSegments.length - 1;
      if (noMoreSegmentsLeft) {
        childNode.setGroup(group);
      }
      children.add(childNode);
      childNode.setParent(this);
      sanitisedChildTrie.put(sanitizedPathSegment, childNode);
      childNode.addChildren(group, pathSegments, sourcePathAndTimestamp);
    }
  }

  public void addChildren(SpringConfigurationMetadataProperty property, String[] pathSegments,
      SuggestionSourcePathAndTimestamp sourcePathAndTimestamp) {
    int startIndex = computeStartIndexAndAddSourcePath();
    addSourcePathTillRoot(sourcePathAndTimestamp);

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
      MetadataNode childNode = MetadataNode.newInstance(pathSegment, this, sourcePathAndTimestamp);

      // If this is the last segment, lets set path
      boolean noMoreSegmentsLeft = startIndex == pathSegments.length - 1;
      if (noMoreSegmentsLeft) {
        childNode.setProperty(property);
      }
      children.add(childNode);
      childNode.setParent(this);
      sanitisedChildTrie.put(sanitizedPathSegment, childNode);
      childNode.addChildren(property, pathSegments, sourcePathAndTimestamp);
    }
  }

  @Nullable
  public Set<Suggestion> findSuggestions(String[] querySegments, int startWith,
      ClassLoader classLoader) {
    if (isGroup() && startWith >= querySegments.length) {
      assert group != null;
      return newSingleElementSet(
          group.newSuggestion(this, computeSuggestion(startWith), classLoader));
    } else if (isLeaf() && validSuggestion() && startWith >= querySegments.length) {
      assert property != null;
      return newSingleElementSet(
          property.newSuggestion(this, computeSuggestion(startWith), classLoader));
    } else {
      return findChildSuggestions(querySegments, startWith, classLoader);
    }
  }

  @Nullable
  public Set<Suggestion> findChildSuggestions(String[] querySegments, int startWith,
      ClassLoader classLoader) {
    Set<Suggestion> suggestions = null;
    if (startWith >= querySegments.length) {
      if (children == null || sanitisedChildTrie == null) {
        return null;
      } else {
        Set<Suggestion> firstSetOfSuggestions = null;
        for (MetadataNode child : children) {
          Set<Suggestion> childSuggestions =
              child.findSuggestions(querySegments, startWith + 1, classLoader);
          if (childSuggestions != null) {
            // If more than one child is willing to offer suggestions, lets add this intermediate node as a suggestion, instead of child suggestions
            if (firstSetOfSuggestions != null) {
              firstSetOfSuggestions = newSingleElementSet(
                  Suggestion.builder().suggestion(computeSuggestion(startWith)).ref(this).build());
              break;
            }
            firstSetOfSuggestions = childSuggestions;
          }
        }
        suggestions = firstSetOfSuggestions;
      }
    } else {
      if (sanitisedChildTrie != null) {
        String sanitisedQuerySegment = sanitize(querySegments[startWith]);
        SortedMap<String, MetadataNode> sortedPrefixToMetadataNode =
            sanitisedChildTrie.prefixMap(sanitisedQuerySegment);
        Collection<MetadataNode> childNodes = sortedPrefixToMetadataNode.values();
        if (childNodes.size() != 0) {
          for (MetadataNode child : childNodes) {
            Set<Suggestion> childSuggestions =
                child.findSuggestions(querySegments, startWith + 1, classLoader);
            if (childSuggestions != null) {
              if (suggestions == null) {
                suggestions = new HashSet<>();
              }
              suggestions.addAll(childSuggestions);
            }
          }
        }
      }
    }
    return suggestions;
  }

  /**
   * @param sourcePathAndTimestamp
   * @return true if no children left & this item does not belong to any other source
   */
  public boolean removeRef(SuggestionSourcePathAndTimestamp sourcePathAndTimestamp) {
    belongsTo.remove(sourcePathAndTimestamp);
    // If the current node & all its children belong to a single file, lets remove the whole tree
    if (belongsTo.size() == 0) {
      return true;
    }

    if (hasChildren()) {
      assert children != null;
      Iterator<MetadataNode> iterator = children.iterator();
      while (iterator.hasNext()) {
        MetadataNode child = iterator.next();
        boolean canRemoveReference = child.removeRef(sourcePathAndTimestamp);
        if (canRemoveReference) {
          iterator.remove();
        }
      }
      if (children.size() == 0) {
        children = null;
      }
    }
    return false;
  }

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

  public boolean isRoot() {
    return parent == null;
  }

  public boolean isGroup() {
    return group != null;
  }

  private boolean hasChildren() {
    return children != null && children.size() != 0;
  }

  public boolean isLeaf() {
    return property != null;
  }

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
  public Set<Suggestion> getSuggestionValues(ClassLoader classLoader) {
    assert property != null;
    return property.getValueSuggestions(this, classLoader);
  }

  @NotNull
  private HashSet<Suggestion> newSingleElementSet(Suggestion suggestion) {
    HashSet<Suggestion> suggestions = new HashSet<>();
    suggestions.add(suggestion);
    return suggestions;
  }

  private boolean validSuggestion() {
    assert property != null;
    return !property.isDeprecated() || property.getDeprecation() == null
        || property.getDeprecation().getLevel() != error;
  }

  private void addSourcePathTillRoot(SuggestionSourcePathAndTimestamp sourcePathAndTimestamp) {
    MetadataNode node = this;
    do {
      if (node.belongsTo.contains(sourcePathAndTimestamp)) {
        break;
      }
      node.belongsTo.add(sourcePathAndTimestamp);
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

}
