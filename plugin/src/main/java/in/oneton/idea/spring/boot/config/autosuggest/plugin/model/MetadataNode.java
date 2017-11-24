package in.oneton.idea.spring.boot.config.autosuggest.plugin.model;

import com.intellij.openapi.diagnostic.Logger;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;

import static in.oneton.idea.spring.boot.config.autosuggest.plugin.model.json.SpringConfigurationMetadataDeprecationLevel.error;
import static java.lang.System.arraycopy;

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
      String metadataFileOrLibraryPath) {
    MetadataNodeBuilder builder =
        MetadataNode.builder().name(sanitize(name)).originalName(name).parent(parent);
    HashSet<String> belongsTo = new HashSet<>();
    belongsTo.add(metadataFileOrLibraryPath);
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
      String metadataFileOrLibraryPath) {
    int startIndex = computeStartIndexAndAddSourcePath();
    addSourcePathTillRoot(metadataFileOrLibraryPath);

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
      MetadataNode childNode =
          MetadataNode.newInstance(pathSegment, this, metadataFileOrLibraryPath);

      // If this is the last segment, lets set group
      boolean noMoreSegmentsLeft = startIndex == pathSegments.length - 1;
      if (noMoreSegmentsLeft) {
        childNode.setGroup(group);
      }
      children.add(childNode);
      childNode.setParent(this);
      sanitisedChildTrie.put(sanitizedPathSegment, childNode);
      childNode.addChildren(group, pathSegments, metadataFileOrLibraryPath);
    }
  }

  public void addChildren(SpringConfigurationMetadataProperty property, String[] pathSegments,
      String metadataFileOrLibraryPath) {
    int startIndex = computeStartIndexAndAddSourcePath();
    addSourcePathTillRoot(metadataFileOrLibraryPath);

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
      MetadataNode childNode =
          MetadataNode.newInstance(pathSegment, this, metadataFileOrLibraryPath);

      // If this is the last segment, lets set path
      boolean noMoreSegmentsLeft = startIndex == pathSegments.length - 1;
      if (noMoreSegmentsLeft) {
        childNode.setProperty(property);
      }
      children.add(childNode);
      childNode.setParent(this);
      sanitisedChildTrie.put(sanitizedPathSegment, childNode);
      childNode.addChildren(property, pathSegments, metadataFileOrLibraryPath);
    }
  }

  @Nullable
  public Set<Suggestion> findSuggestions(String[] querySegments, int startWith,
      ClassLoader classLoader, boolean forceSearchAcrossTree) {
    if (isGroup() && startWith >= querySegments.length) {
      log.info(
          "Group matched: " + this + ". Args matched against: " + Arrays.toString(querySegments)
              + " at element with index: " + startWith);
      assert group != null;
      return newSingleElementSet(
          group.newSuggestion(this, computeSuggestion(startWith), classLoader));
    } else if (isLeaf() && validSuggestion() && startWith >= querySegments.length) {
      log.info(
          "Property matched: " + this + ". Args matched against: " + Arrays.toString(querySegments)
              + " at element with index: " + startWith);
      assert property != null;
      return newSingleElementSet(
          property.newSuggestion(this, computeSuggestion(startWith), classLoader));
    } else {
      return findChildSuggestions(querySegments, startWith, classLoader, forceSearchAcrossTree);
    }
  }

  @Nullable
  public Set<Suggestion> findChildSuggestions(String[] querySegments, int startWith,
      ClassLoader classLoader, boolean forceSearchAcrossTree) {
    log.info("Searching children with arguments " + Arrays.toString(querySegments) + " at index "
        + startWith + " within " + this);
    Set<Suggestion> suggestions = null;
    // If we are searching for `spring` & spring does not have a group/property associated, we would want to go deep till we find all next level descendants that are either groups & properties, so that user dont get bombarded with too many or too little values
    if (startWith >= querySegments.length) {
      if (children == null || sanitisedChildTrie == null) {
        return null;
      } else {
        suggestions =
            findChildSuggestionsDeepWithin(querySegments, startWith + 1, classLoader, false);
        if (suggestions == null && forceSearchAcrossTree) {
          // Since the search did not match any results, lets ask all the children if any of their have matches
          Set<Suggestion> childSuggestions =
              findChildSuggestionsDeepWithin(querySegments, startWith, classLoader, true);
          if (childSuggestions != null) {
            return new HashSet<>(childSuggestions);
          }
        }
      }
    } else {
      if (sanitisedChildTrie != null) {
        String sanitisedQuerySegment = sanitize(querySegments[startWith]);
        SortedMap<String, MetadataNode> sortedPrefixToMetadataNode =
            sanitisedChildTrie.prefixMap(sanitisedQuerySegment);
        Collection<MetadataNode> childNodes = sortedPrefixToMetadataNode.values();
        if (childNodes.size() != 0) {
          suggestions = findChildSuggestionsDeepWithin(querySegments, startWith + 1, classLoader,
              forceSearchAcrossTree, childNodes);
        }

        if (suggestions == null && forceSearchAcrossTree) {
          // Since the search did not match any results, lets ask all the children if any of their children have matches
          for (MetadataNode metadataNode : sanitisedChildTrie.values()) {
            String[] newQuerySegments = new String[querySegments.length + 1];
            newQuerySegments[0] = metadataNode.originalName;
            arraycopy(querySegments, 0, newQuerySegments, 1, querySegments.length);
            Set<Suggestion> childSuggestions = metadataNode
                .findChildSuggestions(newQuerySegments, startWith + 1, classLoader, true);
            if (childSuggestions != null) {
              if (suggestions == null) {
                suggestions = new HashSet<>();
              }
              suggestions.addAll(childSuggestions);
            }
          }
        }
        return suggestions;
      }
    }
    return null;
  }

  private Set<Suggestion> findChildSuggestionsDeepWithin(String[] querySegments, int startWith,
      ClassLoader classLoader, boolean b) {
    Set<Suggestion> suggestions = null;
    assert children != null;
    for (MetadataNode child : children) {
      Set<Suggestion> childSuggestions =
          child.findSuggestions(querySegments, startWith, classLoader, b);
      if (childSuggestions != null) {
        if (suggestions == null) {
          suggestions = new HashSet<>();
        }
        suggestions.addAll(childSuggestions);
      }
    }
    return suggestions;
  }

  private Set<Suggestion> findChildSuggestionsDeepWithin(String[] querySegments, int startWith,
      ClassLoader classLoader, boolean forceSearchAcrossTree, Collection<MetadataNode> childNodes) {
    Set<Suggestion> suggestions = null;
    for (MetadataNode child : childNodes) {
      Set<Suggestion> childSuggestions =
          child.findSuggestions(querySegments, startWith, classLoader, forceSearchAcrossTree);
      if (childSuggestions != null) {
        if (suggestions == null) {
          suggestions = new HashSet<>();
        }
        suggestions.addAll(childSuggestions);
      }
    }
    return suggestions;
  }

  /**
   * @param metadataFileOrLibraryPath Represents either path to the library (or) metadata file
   * @return true if no children left & this item does not belong to any other source
   */
  public boolean removeRef(String metadataFileOrLibraryPath) {
    belongsTo.remove(metadataFileOrLibraryPath);
    // If the current node & all its children belong to a single file, lets remove the whole tree
    if (belongsTo.size() == 0) {
      return true;
    }

    if (hasChildren()) {
      assert children != null;
      Iterator<MetadataNode> iterator = children.iterator();
      while (iterator.hasNext()) {
        MetadataNode child = iterator.next();
        boolean canRemoveReference = child.removeRef(metadataFileOrLibraryPath);
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
  public Set<Suggestion> getSuggestionValues(ClassLoader classLoader,
      boolean forceSearchAcrossTree) {
    assert property != null;
    return property.getValueSuggestions(this, classLoader, forceSearchAcrossTree);
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

  private void addSourcePathTillRoot(String metadataFileOrLibraryPath) {
    MetadataNode node = this;
    do {
      if (node.belongsTo.contains(metadataFileOrLibraryPath)) {
        break;
      }
      node.belongsTo.add(metadataFileOrLibraryPath);
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

  public String getSuggestionReplacement(String existingIndentation, String indent, int maxDepth) {
    int numOfHops = 0;
    MetadataNode currentNode = this;

    StringBuilder builder = new StringBuilder(
        existingIndentation + getIndent(indent, (maxDepth - 1)) + currentNode.originalName);

    currentNode = currentNode.parent;
    numOfHops++;
    while (currentNode != null && numOfHops < maxDepth) {
      builder.insert(0, existingIndentation + getIndent(indent, (maxDepth - numOfHops - 1))
          + currentNode.originalName + ":\n");
      currentNode = currentNode.parent;
      numOfHops++;
    }

    return builder.delete(0, existingIndentation.length()).toString();
  }

  public String getNewOverallIndent(String existingIndentation, String indent, int maxDepth) {
    return existingIndentation + getIndent(indent, (maxDepth - 1));
  }

  public String getIndent(String indent, int numOfHops) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < numOfHops; i++) {
      builder.append(indent);
    }
    return builder.toString();
  }

}
