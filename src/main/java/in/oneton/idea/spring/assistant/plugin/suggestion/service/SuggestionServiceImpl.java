package in.oneton.idea.spring.assistant.plugin.suggestion.service;

import com.google.gson.GsonBuilder;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import in.oneton.idea.spring.assistant.plugin.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.suggestion.completion.FileType;
import in.oneton.idea.spring.assistant.plugin.suggestion.metadata.MetadataContainerInfo;
import in.oneton.idea.spring.assistant.plugin.suggestion.metadata.MetadataNonPropertySuggestionNode;
import in.oneton.idea.spring.assistant.plugin.suggestion.metadata.MetadataPropertySuggestionNode;
import in.oneton.idea.spring.assistant.plugin.suggestion.metadata.MetadataSuggestionNode;
import in.oneton.idea.spring.assistant.plugin.suggestion.metadata.json.GsonPostProcessEnablingTypeFactory;
import in.oneton.idea.spring.assistant.plugin.suggestion.metadata.json.SpringConfigurationMetadata;
import in.oneton.idea.spring.assistant.plugin.suggestion.metadata.json.SpringConfigurationMetadataGroup;
import in.oneton.idea.spring.assistant.plugin.suggestion.metadata.json.SpringConfigurationMetadataHint;
import in.oneton.idea.spring.assistant.plugin.suggestion.metadata.json.SpringConfigurationMetadataProperty;
import in.oneton.idea.spring.assistant.plugin.suggestion.metadata.json.SpringConfigurationMetadataValueProviderType;
import in.oneton.idea.spring.assistant.plugin.suggestion.metadata.json.SpringConfigurationMetadataValueProviderTypeDeserializer;
import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang.time.StopWatch;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import static in.oneton.idea.spring.assistant.plugin.misc.GenericUtil.modifiableList;
import static in.oneton.idea.spring.assistant.plugin.misc.GenericUtil.truncateIdeaDummyIdentifier;
import static in.oneton.idea.spring.assistant.plugin.suggestion.Suggestion.PERIOD_DELIMITER;
import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNode.sanitise;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * A Module level service which holds the index of Spring Boot configuration metadata,
 * provides facilities of creating, updating and querying the index.
 */
public class SuggestionServiceImpl implements SuggestionService {
  private static final Logger log = Logger.getInstance(SuggestionServiceImpl.class);

  private final Module module;

  private final Map<String, MetadataContainerInfo> seenContainerPathToContainerInfo;
  /**
   * Within the trie, all keys are stored in sanitised format to enable us find keys without worrying about hyphens, underscores, e.t.c in the keys themselves
   */
  private final Trie<String, MetadataSuggestionNode> rootSearchIndex;


  SuggestionServiceImpl(Module module) {
    this.module = module;
    seenContainerPathToContainerInfo = new THashMap<>();
    rootSearchIndex = new PatriciaTrie<>();
  }

  private static String firstPathSegment(String element) {
    return element.trim().split(PERIOD_DELIMITER, -1)[0];
  }

  private static String[] toSanitizedPathSegments(String element) {
    String[] splits = element.trim().split(PERIOD_DELIMITER, -1);
    for (int i = 0; i < splits.length; i++) {
      splits[i] = sanitise(splits[i]);
    }
    return splits;
  }

  private static String[] toRawPathSegments(String element) {
    String[] splits = element.trim().split(PERIOD_DELIMITER, -1);
    for (int i = 0; i < splits.length; i++) {
      splits[i] = splits[i].trim();
    }
    return splits;
  }


  @Nullable
  @Override
  public List<SuggestionNode> findMatchedNodesRootTillEnd(List<String> containerElements) {
    String[] pathSegments = containerElements
        .stream()
        .flatMap(element -> stream(toSanitizedPathSegments(element)))
        .toArray(String[]::new);
    MetadataSuggestionNode searchStartNode = rootSearchIndex.get(pathSegments[0]);
    if (searchStartNode != null) {
      List<SuggestionNode> matches = modifiableList(searchStartNode);
      if (pathSegments.length > 1) {
        return searchStartNode.findDeepestSuggestionNode(module, matches, pathSegments, 1);
      }
      return matches;
    }
    return null;
  }

  @Override
  public boolean canProvideSuggestions() {
    return rootSearchIndex != null && rootSearchIndex.size() != 0;
  }

  @Override
  public List<LookupElement> findSuggestionsForQueryPrefix(FileType fileType, PsiElement element,
      @Nullable List<String> ancestralKeys, String queryWithDotDelimitedPrefixes,
      @Nullable Set<String> siblingsToExclude) {
    debug(() -> log.debug("Search requested for " + queryWithDotDelimitedPrefixes));
    StopWatch timer = new StopWatch();
    timer.start();
    try {
      String[] querySegmentPrefixes = toSanitizedPathSegments(queryWithDotDelimitedPrefixes);
      Set<Suggestion> suggestions = null;
      if (ancestralKeys != null) {
        String[] ancestralKeySegments =
            ancestralKeys.stream().flatMap(key -> stream(toRawPathSegments(key)))
                         .toArray(String[]::new);
        MetadataSuggestionNode rootNode = rootSearchIndex.get(sanitise(ancestralKeySegments[0]));
        if (rootNode != null) {
          List<SuggestionNode> matchesRootToDeepest;
          SuggestionNode startSearchFrom = null;
          if (ancestralKeySegments.length > 1) {
            String[] sanitisedAncestralPathSegments =
                stream(ancestralKeySegments).map(SuggestionNode::sanitise).toArray(String[]::new);
            matchesRootToDeepest = rootNode
                .findDeepestSuggestionNode(module, modifiableList(rootNode),
                    sanitisedAncestralPathSegments, 1);
            if (matchesRootToDeepest != null && matchesRootToDeepest.size() != 0) {
              startSearchFrom = matchesRootToDeepest.get(matchesRootToDeepest.size() - 1);
            }
          } else {
            startSearchFrom = rootNode;
            matchesRootToDeepest = singletonList(rootNode);
          }

          if (startSearchFrom != null) {
            // if search start node is a leaf, this means, the user is looking for values for the given key, lets find the suggestions for values
            if (startSearchFrom.isLeaf(module)) {
              suggestions = startSearchFrom.findValueSuggestionsForPrefix(module, fileType,
                  unmodifiableList(matchesRootToDeepest),
                  sanitise(truncateIdeaDummyIdentifier(element.getText())), siblingsToExclude);
            } else {
              suggestions = startSearchFrom.findKeySuggestionsForQueryPrefix(module, fileType,
                  unmodifiableList(matchesRootToDeepest), matchesRootToDeepest.size(),
                  querySegmentPrefixes, 0, siblingsToExclude);
            }
          }
        }
      } else {
        String rootQuerySegmentPrefix = querySegmentPrefixes[0];
        SortedMap<String, MetadataSuggestionNode> topLevelQueryResults =
            rootSearchIndex.prefixMap(rootQuerySegmentPrefix);

        Collection<MetadataSuggestionNode> childNodes;
        int querySegmentPrefixStartIndex;

        // If no results are found at the top level, let dive deeper and find matches
        if (topLevelQueryResults == null || topLevelQueryResults.size() == 0) {
          childNodes = rootSearchIndex.values();
          querySegmentPrefixStartIndex = 0;
        } else {
          childNodes = topLevelQueryResults.values();
          querySegmentPrefixStartIndex = 1;
        }

        Collection<MetadataSuggestionNode> nodesToSearchAgainst;
        if (siblingsToExclude != null) {
          Set<MetadataSuggestionNode> nodesToExclude = siblingsToExclude
              .stream()
              .flatMap(exclude -> rootSearchIndex.prefixMap(exclude).values().stream())
              .collect(toSet());
          nodesToSearchAgainst =
              childNodes.stream().filter(node -> !nodesToExclude.contains(node)).collect(toList());
        } else {
          nodesToSearchAgainst = childNodes;
        }

        suggestions = doFindSuggestionsForQueryPrefix(fileType, nodesToSearchAgainst,
            querySegmentPrefixes, querySegmentPrefixStartIndex);
      }

      if (suggestions != null) {
        return toLookupElements(suggestions);
      }
      return null;
    } finally {
      timer.stop();
      debug(() -> log.debug("Search took " + timer));
    }
  }

  @Nullable
  private Set<Suggestion> doFindSuggestionsForQueryPrefix(FileType fileType,
      Collection<MetadataSuggestionNode> nodesToSearchWithin, String[] querySegmentPrefixes,
      int querySegmentPrefixStartIndex) {
    Set<Suggestion> suggestions = null;
    for (MetadataSuggestionNode suggestionNode : nodesToSearchWithin) {
      Set<Suggestion> matchedSuggestions = suggestionNode
          .findKeySuggestionsForQueryPrefix(module, fileType, modifiableList(suggestionNode), 0,
              querySegmentPrefixes, querySegmentPrefixStartIndex);
      if (matchedSuggestions != null) {
        if (suggestions == null) {
          suggestions = new THashSet<>();
        }
        suggestions.addAll(matchedSuggestions);
      }
    }
    return suggestions;
  }

  @Nullable
  private List<LookupElement> toLookupElements(@Nullable Set<Suggestion> suggestions) {
    if (suggestions != null) {
      return suggestions.stream().map(Suggestion::newLookupElement).collect(toList());
    }
    return null;
  }

  /**
   * Debug logging can be enabled by adding fully classified class name/package name with # prefix
   * For eg., to enable debug logging, go `Help > Debug log settings` & type `#in.oneton.idea.spring.assistant.plugin.suggestion.service.SuggestionServiceImpl`
   *
   * @param doWhenDebug code to execute when debug is enabled
   */
  private void debug(Runnable doWhenDebug) {
    if (log.isDebugEnabled()) {
      doWhenDebug.run();
    }
  }

  @Override
  public void reindex() {
    OrderEnumerator moduleOrderEnumerator = OrderEnumerator.orderEntries(module);
    List<MetadataContainerInfo> newModuleContainersToProcess = computeNewContainersToProcess(moduleOrderEnumerator);
    List<MetadataContainerInfo> moduleContainersToRemove = computeContainersToRemove(moduleOrderEnumerator);
    processContainers(newModuleContainersToProcess, moduleContainersToRemove);
  }

  private List<MetadataContainerInfo> computeNewContainersToProcess(OrderEnumerator orderEnumerator) {
    List<MetadataContainerInfo> containersToProcess = new ArrayList<>();
    for (VirtualFile metadataFileContainer : orderEnumerator.recursively().classes().getRoots()) {
      Collection<MetadataContainerInfo> metadataContainerInfos =
          MetadataContainerInfo.newInstances(metadataFileContainer);
      for (MetadataContainerInfo metadataContainerInfo : metadataContainerInfos) {
        boolean seenBefore = seenContainerPathToContainerInfo
            .containsKey(metadataContainerInfo.getContainerArchiveOrFileRef());

        boolean updatedSinceLastSeen = false;
        if (seenBefore) {
          MetadataContainerInfo seenMetadataContainerInfo = seenContainerPathToContainerInfo
              .get(metadataContainerInfo.getContainerArchiveOrFileRef());
          updatedSinceLastSeen = metadataContainerInfo.isModified(seenMetadataContainerInfo);
          if (updatedSinceLastSeen) {
            debug(() -> log.debug("Container seems to have been updated. Previous version: "
                + seenMetadataContainerInfo + "; Newer version: " + metadataContainerInfo));
          }
        }

        boolean looksFresh = !seenBefore || updatedSinceLastSeen;
        boolean processMetadata = looksFresh && metadataContainerInfo.containsMetadataFile();
        if (processMetadata) {
          containersToProcess.add(metadataContainerInfo);
        }

        if (looksFresh) {
          seenContainerPathToContainerInfo
              .put(metadataContainerInfo.getContainerArchiveOrFileRef(), metadataContainerInfo);
        }
      }
    }

    if (containersToProcess.size() == 0) {
      debug(() -> log.debug("No (new)metadata files to index"));
    }
    return containersToProcess;
  }

  /**
   * Finds the containers that are not reachable from current classpath
   *
   * @param orderEnumerator classpath roots to work with
   * @return list of container paths that are no longer valid
   */
  private List<MetadataContainerInfo> computeContainersToRemove(OrderEnumerator orderEnumerator) {
    Set<String> newContainerPaths = stream(orderEnumerator.recursively().classes().getRoots())
        .flatMap(MetadataContainerInfo::getContainerArchiveOrFileRefs).collect(toSet());
    Set<String> knownContainerPathSet = new THashSet<>(seenContainerPathToContainerInfo.keySet());
    knownContainerPathSet.removeAll(newContainerPaths);
    return knownContainerPathSet.stream().map(seenContainerPathToContainerInfo::get).collect(toList());
  }

  private void processContainers(List<MetadataContainerInfo> toProcess, List<MetadataContainerInfo> toRemove) {
    // Lets remove references to files that are no longer present in classpath
    toRemove.forEach(this::removeReferences);

    for (MetadataContainerInfo metadataContainerInfo : toProcess) {
      // lets remove existing references from search index, as these files are modified, so that we can rebuild index
      if (seenContainerPathToContainerInfo.containsKey(metadataContainerInfo.getContainerArchiveOrFileRef())) {
        removeReferences(metadataContainerInfo);
      }

      String metadataFilePath = metadataContainerInfo.getFileUrl();
      try (InputStream inputStream = metadataContainerInfo.getMetadataFile().getInputStream()) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        // register custom mapper adapters
        gsonBuilder.registerTypeAdapter(SpringConfigurationMetadataValueProviderType.class,
            new SpringConfigurationMetadataValueProviderTypeDeserializer());
        gsonBuilder.registerTypeAdapterFactory(new GsonPostProcessEnablingTypeFactory());
        SpringConfigurationMetadata springConfigurationMetadata = gsonBuilder
            .create()
            .fromJson(new BufferedReader(new InputStreamReader(inputStream)), SpringConfigurationMetadata.class);
        buildMetadataHierarchy(metadataContainerInfo, springConfigurationMetadata);
        seenContainerPathToContainerInfo.put(metadataContainerInfo.getContainerArchiveOrFileRef(),
            metadataContainerInfo);
      } catch (IOException e) {
        log.error("Exception encountered while processing metadata file: " + metadataFilePath, e);
        removeReferences(metadataContainerInfo);
      }
    }
  }

  private void removeReferences(MetadataContainerInfo metadataContainerInfo) {
    debug(() -> log.debug("Removing references to " + metadataContainerInfo));
    String containerPath = metadataContainerInfo.getContainerArchiveOrFileRef();
    seenContainerPathToContainerInfo.remove(containerPath);

    Iterator<String> searchIndexIterator = rootSearchIndex.keySet().iterator();
    while (searchIndexIterator.hasNext()) {
      MetadataSuggestionNode root = rootSearchIndex.get(searchIndexIterator.next());
      if (root != null) {
        boolean removeTree = root.removeRefCascadeDown(metadataContainerInfo.getContainerArchiveOrFileRef());
        if (removeTree) {
          searchIndexIterator.remove();
        }
      }
    }
  }

  private void buildMetadataHierarchy(MetadataContainerInfo metadataContainerInfo,
      SpringConfigurationMetadata springConfigurationMetadata) {
    debug(() -> log.debug("Adding container to index " + metadataContainerInfo));
    String containerPath = metadataContainerInfo.getContainerArchiveOrFileRef();
    addGroupsToIndex(springConfigurationMetadata, containerPath);
    addPropertiesToIndex(springConfigurationMetadata, containerPath);
    addHintsToIndex(springConfigurationMetadata, containerPath);
    debug(() -> log.debug("Done adding container to index"));
  }

  private void addGroupsToIndex(SpringConfigurationMetadata springConfigurationMetadata,
      String containerArchiveOrFileRef) {
    List<SpringConfigurationMetadataGroup> groups = springConfigurationMetadata.getGroups();
    if (groups != null) {
      groups.sort(comparing(SpringConfigurationMetadataGroup::getName));
      for (SpringConfigurationMetadataGroup group : groups) {
        String[] pathSegments = toSanitizedPathSegments(group.getName());
        String[] rawPathSegments = toRawPathSegments(group.getName());

        MetadataSuggestionNode closestMetadata = findDeepestMetadataMatch(rootSearchIndex, pathSegments, false);

        int startIndex;
        if (closestMetadata == null) { // path does not have a corresponding root element
          // lets build just the root element. Rest of the path segments will be taken care of by the addChildren method
          boolean onlyRootSegmentExists = pathSegments.length == 1;
          MetadataNonPropertySuggestionNode newGroupSuggestionNode =
              MetadataNonPropertySuggestionNode
                  .newInstance(rawPathSegments[0], null, containerArchiveOrFileRef);
          if (onlyRootSegmentExists) {
            newGroupSuggestionNode.setGroup(module, group);
          }
          rootSearchIndex.put(pathSegments[0], newGroupSuggestionNode);

          closestMetadata = newGroupSuggestionNode;
          // since we already handled the root level item, let addChildren start from index 1 of pathSegments
          startIndex = 1;
        } else {
          startIndex = closestMetadata.numOfHopesToRoot() + 1;
        }

        if (closestMetadata.isProperty()) {
          log.warn(
              "Detected conflict between an existing metadata property & new group for suggestion path "
                  + closestMetadata.getPathFromRoot(module)
                  + ". Ignoring new group. Existing Property belongs to (" + closestMetadata
                  .getBelongsTo().stream().collect(joining(",")) + "), New Group belongs to "
                  + containerArchiveOrFileRef);
        } else {
          // lets add container as a reference till root
          MetadataNonPropertySuggestionNode groupSuggestionNode =
              (MetadataNonPropertySuggestionNode) closestMetadata;
          groupSuggestionNode.addRefCascadeTillRoot(containerArchiveOrFileRef);

          boolean haveMoreSegmentsLeft = startIndex < rawPathSegments.length;
          if (haveMoreSegmentsLeft) {
            groupSuggestionNode
                .addChildren(module, group, rawPathSegments, startIndex, containerArchiveOrFileRef);
          } else {
            // Node is an intermediate node that has neither group nor property assigned to it, lets assign this group to it
            // Can happen when `a.b.c` is already added to the metadata tree from an earlier metadata source & now we are trying to add a group for `a.b`
            // In this e.g, startIndex would be 2. So, there is no point in adding children. We only need to update the tree appropriately
            groupSuggestionNode.setGroup(module, group);
          }
        }
      }
    }
  }

  private void addPropertiesToIndex(SpringConfigurationMetadata springConfigurationMetadata,
      String containerArchiveOrFileRef) {
    List<SpringConfigurationMetadataProperty> properties =
        springConfigurationMetadata.getProperties();
    properties.sort(comparing(SpringConfigurationMetadataProperty::getName));
    for (SpringConfigurationMetadataProperty property : properties) {
      String[] pathSegments = toSanitizedPathSegments(property.getName());
      String[] rawPathSegments = toRawPathSegments(property.getName());
      MetadataSuggestionNode closestMetadata =
          findDeepestMetadataMatch(rootSearchIndex, pathSegments, false);

      int startIndex;
      if (closestMetadata == null) { // path does not have a corresponding root element
        boolean onlyRootSegmentExists = pathSegments.length == 1;
        if (onlyRootSegmentExists) {
          closestMetadata = MetadataPropertySuggestionNode
              .newInstance(rawPathSegments[0], property, null, containerArchiveOrFileRef);
        } else {
          closestMetadata = MetadataNonPropertySuggestionNode
              .newInstance(rawPathSegments[0], null, containerArchiveOrFileRef);
        }
        rootSearchIndex.put(pathSegments[0], closestMetadata);

        // since we already handled the root level item, let addChildren start from index 1 of pathSegments
        startIndex = 1;
      } else {
        startIndex = closestMetadata.numOfHopesToRoot() + 1;
      }

      boolean haveMoreSegmentsLeft = startIndex < rawPathSegments.length;

      if (haveMoreSegmentsLeft) {
        if (!closestMetadata.isProperty()) {
          ((MetadataNonPropertySuggestionNode) closestMetadata)
              .addChildren(property, rawPathSegments, startIndex,
                  containerArchiveOrFileRef);
        } else {
          log.warn("Detected conflict between a new group & existing property for suggestion path "
              + closestMetadata.getPathFromRoot(module)
              + ". Ignoring property. Existing non property node belongs to (" + closestMetadata
              .getBelongsTo().stream().collect(joining(",")) + "), New property belongs to "
              + containerArchiveOrFileRef);
        }
      } else {
        if (!closestMetadata.isProperty()) {
          log.warn(
              "Detected conflict between a new metadata property & existing non property node for suggestion path "
                  + closestMetadata.getPathFromRoot(module)
                  + ". Ignoring property. Existing non property node belongs to (" + closestMetadata
                  .getBelongsTo().stream().collect(joining(",")) + "), New property belongs to "
                  + containerArchiveOrFileRef);
        } else {
          closestMetadata.addRefCascadeTillRoot(containerArchiveOrFileRef);
          log.debug("Detected a duplicate metadata property for suggestion path " + closestMetadata
              .getPathFromRoot(module) + ". Ignoring property. Existing property belongs to ("
              + closestMetadata.getBelongsTo().stream().collect(joining(","))
              + "), New property belongs to " + containerArchiveOrFileRef);
        }
      }
    }
  }

  private void addHintsToIndex(SpringConfigurationMetadata springConfigurationMetadata, String containerPath) {
    List<SpringConfigurationMetadataHint> hints = springConfigurationMetadata.getHints();
    if (hints != null) {
      hints.sort(comparing(SpringConfigurationMetadataHint::getName));
      for (SpringConfigurationMetadataHint hint : hints) {
        String[] pathSegments = toSanitizedPathSegments(hint.getExpectedPropertyName());
        MetadataSuggestionNode closestMetadata =
            findDeepestMetadataMatch(rootSearchIndex, pathSegments, true);
        if (closestMetadata != null) {
          if (!closestMetadata.isProperty()) {
            log.warn(
                "Unexpected hint " + hint.getName() + " is assigned to  group " + closestMetadata
                    .getPathFromRoot(module)
                    + " found. Hints can be only assigned to property. Ignoring the hint completely.Existing group belongs to ("
                    + closestMetadata.getBelongsTo().stream().collect(joining(","))
                    + "), New hint belongs " + containerPath);
          } else {
            MetadataPropertySuggestionNode propertySuggestionNode =
                (MetadataPropertySuggestionNode) closestMetadata;
            if (hint.representsValueOfMap()) {
              propertySuggestionNode.getProperty().setValueHint(hint);
            } else {
              propertySuggestionNode.getProperty().setGenericOrKeyHint(hint);
            }
          }
        }
      }
    }
  }

  private MetadataSuggestionNode findDeepestMetadataMatch(Map<String, MetadataSuggestionNode> roots,
      String[] pathSegments, boolean matchAllSegments) {
    MetadataSuggestionNode closestMatchedRoot = roots.get(pathSegments[0]);
    if (closestMatchedRoot != null) {
      closestMatchedRoot =
          closestMatchedRoot.findDeepestMetadataNode(pathSegments, 1, matchAllSegments);
    }
    return closestMatchedRoot;
  }

  @SuppressWarnings("unused")
  private String toTree() {
    StringBuilder builder = new StringBuilder();
    rootSearchIndex.forEach((k, v) -> {
      builder.append("Module: ").append(k).append("\n")
             .append(v.toTree().trim().replaceFirst("^", "  ").replaceAll("\n", "\n  "))
             .append("\n");
    });
    return builder.toString();
  }
}
