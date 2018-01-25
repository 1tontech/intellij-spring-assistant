package in.oneton.idea.spring.assistant.plugin.service;

import com.google.gson.Gson;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import gnu.trove.THashMap;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata.MetadataContainerInfo;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata.MetadataNonPropertySuggestionNode;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata.MetadataPropertySuggestionNode;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata.MetadataSuggestionNode;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata.json.SpringConfigurationMetadata;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata.json.SpringConfigurationMetadataGroup;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata.json.SpringConfigurationMetadataHint;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata.json.SpringConfigurationMetadataProperty;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.Future;

import static com.intellij.openapi.application.ApplicationManager.getApplication;
import static in.oneton.idea.spring.assistant.plugin.Util.modifiableList;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.Suggestion.PERIOD_DELIMITER;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNode.sanitise;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata.MetadataContainerInfo.getContainerFile;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class SuggestionServiceImpl implements SuggestionService {

  private static final Logger log = Logger.getInstance(SuggestionServiceImpl.class);

  private final Map<String, Map<String, MetadataContainerInfo>>
      moduleNameToSeenContainerPathToContainerInfo;
  /**
   * Within the trie, all keys are stored in sanitised format to enable us find keys without worrying about hiphens, underscores, e.t.c in the keys themselves
   */
  private final Map<String, Trie<String, MetadataSuggestionNode>> moduleNameToRootSearchIndex;
  private Future<?> currentExecution;
  private volatile boolean indexingInProgress;

  SuggestionServiceImpl() {

    moduleNameToSeenContainerPathToContainerInfo = new THashMap<>();
    moduleNameToRootSearchIndex = new THashMap<>();
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

  private static String firstPathSegment(String element) {
    return element.trim().split(PERIOD_DELIMITER, -1)[0];
  }

  @Override
  public void init(Project project) {
    reIndex(project);
  }

  @Override
  public void reIndex(Project project) {
    if (indexingInProgress) {
      currentExecution.cancel(false);
    }
    //noinspection CodeBlock2Expr
    currentExecution = getApplication().executeOnPooledThread(() -> {
      getApplication().runReadAction(() -> {
        indexingInProgress = true;
        StopWatch timer = new StopWatch();
        timer.start();
        try {
          debug(() -> log.debug("-> Indexing requested for project " + project.getName()));
          // OrderEnumerator.orderEntries(project) is returning everything from all modules including root level module(which is called project in gradle terms)
          // So, we should not be doing anything with this

          Module[] modules = ModuleManager.getInstance(project).getModules();
          for (Module module : modules) {
            reindexModule(emptyList(), emptyList(), module);
          }
        } finally {
          indexingInProgress = false;
          timer.stop();
          debug(() -> log
              .debug("<- Indexing took " + timer.toString() + " for project " + project.getName()));
        }
      });
    });
  }

  @Override
  public void reindex(Project project, Module[] modules) {
    if (indexingInProgress) {
      if (currentExecution != null) {
        currentExecution.cancel(false);
      }
    }
    //noinspection CodeBlock2Expr
    currentExecution = getApplication().executeOnPooledThread(() -> {
      getApplication().runReadAction(() -> {
        debug(() -> log.debug(
            "-> Indexing requested for a subset of modules of project " + project.getName()));
        indexingInProgress = true;
        StopWatch timer = new StopWatch();
        timer.start();
        try {
          for (Module module : modules) {
            debug(() -> log.debug("--> Indexing requested for module " + module.getName()));
            StopWatch moduleTimer = new StopWatch();
            moduleTimer.start();
            try {
              reindexModule(emptyList(), emptyList(), module);
            } finally {
              moduleTimer.stop();
              debug(() -> log.debug(
                  "<-- Indexing took " + moduleTimer.toString() + " for module " + module
                      .getName()));
            }
          }
        } finally {
          indexingInProgress = false;
          timer.stop();
          debug(() -> log
              .debug("<- Indexing took " + timer.toString() + " for project " + project.getName()));
        }
      });
    });
  }

  @Override
  public void reindex(Project project, Module module) {
    reindex(project, new Module[] {module});
  }

  @Nullable
  @Override
  public List<SuggestionNode> findMatchedNodesRootTillEnd(Project project, Module module,
      List<String> containerElements) {
    if (moduleNameToRootSearchIndex.containsKey(module.getName())) {
      String[] pathSegments =
          containerElements.stream().flatMap(element -> stream(toSanitizedPathSegments(element)))
              .toArray(String[]::new);
      MetadataSuggestionNode searchStartNode =
          moduleNameToRootSearchIndex.get(module.getName()).get(pathSegments[0]);
      if (searchStartNode != null) {
        List<SuggestionNode> matches = modifiableList(searchStartNode);
        if (pathSegments.length > 1) {
          return searchStartNode.findDeepestMatch(matches, pathSegments, 1);
        }
        return matches;
      }
    }
    return null;
  }

  @Override
  public boolean canProvideSuggestions(Project project, Module module) {
    Trie<String, MetadataSuggestionNode> rootSearchIndex =
        moduleNameToRootSearchIndex.get(module.getName());
    return rootSearchIndex != null && rootSearchIndex.size() != 0;
  }

  @Override
  public List<LookupElementBuilder> computeSuggestions(Project project, Module module,
      PsiElement element, @Nullable List<String> ancestralKeys,
      String queryWithDotDelimitedPrefixes) {
    return computeSuggestions(moduleNameToRootSearchIndex.get(module.getName()), element,
        ancestralKeys, queryWithDotDelimitedPrefixes);
  }

  private List<MetadataContainerInfo> computeNewContainersToProcess(OrderEnumerator orderEnumerator,
      Map<String, MetadataContainerInfo> seenContainerPathToContainerInfo) {
    List<MetadataContainerInfo> containersToProcess = new ArrayList<>();
    for (VirtualFile metadataFileContainer : orderEnumerator.recursively().classes().getRoots()) {
      MetadataContainerInfo metadataContainerInfo =
          MetadataContainerInfo.newInstance(metadataFileContainer);
      boolean seenBefore =
          seenContainerPathToContainerInfo.containsKey(metadataContainerInfo.getContainerPath());

      boolean updatedSinceLastSeen = false;
      if (seenBefore) {
        MetadataContainerInfo seenMetadataContainerInfo =
            seenContainerPathToContainerInfo.get(metadataContainerInfo.getContainerPath());
        updatedSinceLastSeen = metadataContainerInfo.isModified(seenMetadataContainerInfo);
        if (updatedSinceLastSeen) {
          debug(() -> log.debug(
              "Container seems to have been updated. Previous version: " + seenMetadataContainerInfo
                  + "; Newer version: " + metadataContainerInfo));
        }
      }

      boolean looksFresh = !seenBefore || updatedSinceLastSeen;
      boolean processMetadata = looksFresh && metadataContainerInfo.containsMetadataFile();
      if (processMetadata) {
        containersToProcess.add(metadataContainerInfo);
      }

      if (looksFresh) {
        seenContainerPathToContainerInfo
            .put(metadataContainerInfo.getContainerPath(), metadataContainerInfo);
      }
    }

    if (containersToProcess.size() == 0) {
      debug(() -> log.debug("No (new)metadata files to index"));
    }
    return containersToProcess;
  }

  private List<LookupElementBuilder> computeSuggestions(
      Trie<String, MetadataSuggestionNode> rootSearchIndex, PsiElement element,
      @Nullable List<String> ancestralKeys, String queryWithDotDelimitedPrefixes) {
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
          SuggestionNode searchStartNode = null;
          if (ancestralKeySegments.length > 1) {
            String[] sanitisedAncestralPathSegments =
                stream(ancestralKeySegments).map(SuggestionNode::sanitise).toArray(String[]::new);
            searchStartNode = rootNode.findDeepestMatch(sanitisedAncestralPathSegments, 1);
          }
          if (searchStartNode != null) {
            // if node is a leaf, this means, the user is looking for values for the given key, lets find the suggestions for values
            if (!searchStartNode.isLeaf()) {
              suggestions = searchStartNode.findSuggestionsForValue(element.getText());
            } else {
              String ancestralKeysDotDelimited = stream(ancestralKeySegments).collect(joining("."));
              suggestions = searchStartNode
                  .findSuggestionsForKey(ancestralKeysDotDelimited, emptyList(),
                      querySegmentPrefixes, 0);
            }
          }
        }
      } else {
        String rootQuerySegmentPrefix = querySegmentPrefixes[0];
        if (isEmpty(rootQuerySegmentPrefix)) {
          Collection<MetadataSuggestionNode> nodesToSearchWithin = rootSearchIndex.values();
          suggestions = computeSuggestions(nodesToSearchWithin, querySegmentPrefixes, 0);
        } else {
          SortedMap<String, MetadataSuggestionNode> topLevelQueryResults =
              rootSearchIndex.prefixMap(rootQuerySegmentPrefix);
          Collection<MetadataSuggestionNode> childNodes = topLevelQueryResults.values();
          suggestions = computeSuggestions(childNodes, querySegmentPrefixes, 1);
        }
      }

      if (suggestions != null) {
        return toLookupElementBuilders(suggestions);
      }
      return null;
    } finally {
      timer.stop();
      debug(() -> log.debug("Search took " + timer.toString()));
    }
  }

  @Nullable
  private Set<Suggestion> computeSuggestions(Collection<MetadataSuggestionNode> nodesToSearchWithin,
      String[] querySegmentPrefixes, int querySegmentPrefixStartIndex) {
    Set<Suggestion> suggestions = null;
    for (MetadataSuggestionNode suggestionNode : nodesToSearchWithin) {
      Set<Suggestion> matchedSuggestions = suggestionNode
          .findSuggestionsForKey(null, modifiableList(suggestionNode), querySegmentPrefixes,
              querySegmentPrefixStartIndex);
      if (matchedSuggestions != null) {
        if (suggestions == null) {
          suggestions = new HashSet<>();
        }
        suggestions.addAll(matchedSuggestions);
      }
    }
    return suggestions;
  }

  @Nullable
  private List<LookupElementBuilder> toLookupElementBuilders(
      @Nullable Set<Suggestion> suggestions) {
    if (suggestions != null) {
      return suggestions.stream().map(Suggestion::newLookupElement).collect(toList());
    }
    return null;
  }

  /**
   * Finds the containers that are not reachable from current classpath
   *
   * @param orderEnumerator                  classpath roots to work with
   * @param seenContainerPathToContainerInfo seen container paths
   * @return list of container paths that are no longer valid
   */
  private List<MetadataContainerInfo> computeContainersToRemove(OrderEnumerator orderEnumerator,
      Map<String, MetadataContainerInfo> seenContainerPathToContainerInfo) {
    Set<String> newContainerPaths = stream(orderEnumerator.recursively().classes().getRoots())
        .map(metadataFileContainer -> getContainerFile(metadataFileContainer).getUrl())
        .collect(toSet());
    Set<String> knownContainerPathSet = new HashSet<>(seenContainerPathToContainerInfo.keySet());
    knownContainerPathSet.removeAll(newContainerPaths);
    return knownContainerPathSet.stream().map(seenContainerPathToContainerInfo::get)
        .collect(toList());
  }

  private void processContainers(Module module, List<MetadataContainerInfo> containersToProcess,
      List<MetadataContainerInfo> containersToRemove,
      Map<String, MetadataContainerInfo> seenContainerPathToContainerInfo,
      Trie<String, MetadataSuggestionNode> rootSearchIndex) {
    // Lets remove references to files that are no longer present in classpath
    containersToRemove.forEach(
        container -> removeReferences(seenContainerPathToContainerInfo, rootSearchIndex,
            container));

    for (MetadataContainerInfo metadataContainerInfo : containersToProcess) {
      // lets remove existing references from search index, as these files are modified, so that we can rebuild index
      if (seenContainerPathToContainerInfo.containsKey(metadataContainerInfo.getContainerPath())) {
        removeReferences(seenContainerPathToContainerInfo, rootSearchIndex, metadataContainerInfo);
      }

      String metadataFilePath = metadataContainerInfo.getPath();
      try (InputStream inputStream = metadataContainerInfo.getMetadataFile().getInputStream()) {
        SpringConfigurationMetadata springConfigurationMetadata = new Gson()
            .fromJson(new BufferedReader(new InputStreamReader(inputStream)),
                SpringConfigurationMetadata.class);
        buildMetadataHierarchy(module, rootSearchIndex, metadataContainerInfo,
            springConfigurationMetadata);

        seenContainerPathToContainerInfo
            .put(metadataContainerInfo.getContainerPath(), metadataContainerInfo);
      } catch (IOException e) {
        log.error("Exception encountered while processing metadata file: " + metadataFilePath, e);
        removeReferences(seenContainerPathToContainerInfo, rootSearchIndex, metadataContainerInfo);
      }
    }
  }

  private void reindexModule(List<MetadataContainerInfo> newProjectSourcesToProcess,
      List<MetadataContainerInfo> projectContainersToRemove, Module module) {
    Map<String, MetadataContainerInfo> moduleSeenContainerPathToSeenContainerInfo =
        moduleNameToSeenContainerPathToContainerInfo
            .computeIfAbsent(module.getName(), k -> new THashMap<>());

    Trie<String, MetadataSuggestionNode> moduleRootSearchIndex =
        moduleNameToRootSearchIndex.get(module.getName());
    if (moduleRootSearchIndex == null) {
      moduleRootSearchIndex = new PatriciaTrie<>();
      moduleNameToRootSearchIndex.put(module.getName(), moduleRootSearchIndex);
    }

    OrderEnumerator moduleOrderEnumerator = OrderEnumerator.orderEntries(module);

    List<MetadataContainerInfo> newModuleContainersToProcess =
        computeNewContainersToProcess(moduleOrderEnumerator,
            moduleSeenContainerPathToSeenContainerInfo);
    newModuleContainersToProcess.addAll(newProjectSourcesToProcess);

    List<MetadataContainerInfo> moduleContainersToRemove =
        computeContainersToRemove(moduleOrderEnumerator,
        moduleSeenContainerPathToSeenContainerInfo);
    moduleContainersToRemove.addAll(projectContainersToRemove);

    processContainers(module, newModuleContainersToProcess, moduleContainersToRemove,
        moduleSeenContainerPathToSeenContainerInfo, moduleRootSearchIndex);
  }

  private void buildMetadataHierarchy(Module module,
      Trie<String, MetadataSuggestionNode> rootSearchIndex,
      MetadataContainerInfo metadataContainerInfo,
      SpringConfigurationMetadata springConfigurationMetadata) {
    debug(() -> log.debug("Adding container to index " + metadataContainerInfo));
    String containerPath = metadataContainerInfo.getContainerPath();
    addGroupsToIndex(module, rootSearchIndex, springConfigurationMetadata, containerPath);
    addPropertiesToIndex(module, rootSearchIndex, springConfigurationMetadata, containerPath);
    addHintsToIndex(rootSearchIndex, springConfigurationMetadata, containerPath);
    debug(() -> log.debug("Done adding container to index"));
  }

  private void addHintsToIndex(Trie<String, MetadataSuggestionNode> rootSearchIndex,
      SpringConfigurationMetadata springConfigurationMetadata, String containerPath) {
    List<SpringConfigurationMetadataHint> hints = springConfigurationMetadata.getHints();
    if (hints != null) {
      hints.sort(comparing(SpringConfigurationMetadataHint::getName));
      for (SpringConfigurationMetadataHint hint : hints) {
        String[] pathSegments = toSanitizedPathSegments(hint.getExpectedPropertyName());
        MetadataSuggestionNode closestMetadata =
            findDeepestMetadataMatch(rootSearchIndex, pathSegments, true);
        if (closestMetadata != null) {
          if (!closestMetadata.isProperty()) {
            log.error(
                "Unexpected hint " + hint.getName() + " is assigned to  group " + closestMetadata
                    .getPathFromRoot()
                    + " found. Hints can be only assigned to property. Ignoring the hint completely.Existing group belongs to ("
                    + closestMetadata.getBelongsTo().stream().collect(joining(","))
                    + "), New hint belongs " + containerPath);
          } else {
            MetadataPropertySuggestionNode propertySuggestionNode =
                MetadataPropertySuggestionNode.class.cast(closestMetadata);
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

  private void addPropertiesToIndex(Module module,
      Trie<String, MetadataSuggestionNode> rootSearchIndex,
      SpringConfigurationMetadata springConfigurationMetadata, String containerPath) {
    List<SpringConfigurationMetadataProperty> properties = springConfigurationMetadata.getProperties();
    properties.sort(comparing(SpringConfigurationMetadataProperty::getName));
    for (SpringConfigurationMetadataProperty property : properties) {
      String[] pathSegments = toSanitizedPathSegments(property.getName());
      MetadataSuggestionNode closestMetadata =
          findDeepestMetadataMatch(rootSearchIndex, pathSegments, false);

      int startIndex;
      if (closestMetadata == null) { // path does not have a corresponding root element
        String unsanitisedRootSegment = firstPathSegment(property.getName());
        boolean onlyRootSegmentExists = pathSegments.length == 1;
        if (onlyRootSegmentExists) {
          closestMetadata = MetadataPropertySuggestionNode
              .newInstance(module, unsanitisedRootSegment, property, null, containerPath);
        } else {
          closestMetadata = MetadataNonPropertySuggestionNode
              .newInstance(unsanitisedRootSegment, null, containerPath);
        }
        rootSearchIndex.put(pathSegments[0], closestMetadata);

        // since we already handled the root level item, let addChildren start from index 1 of pathSegments
        startIndex = 1;
      } else {
        startIndex = closestMetadata.numOfHopesToRoot() + 1;
      }

      if (closestMetadata.isProperty()) {
        log.error("Detected a duplicate metadata property for suggestion path " + closestMetadata.getPathFromRoot() + ". Ignoring property. Existing property belongs to ("
            + closestMetadata.getBelongsTo().stream().collect(joining(",")) + "), New property belongs to " + containerPath);
      } else {
        // Can happen when `a.b.c` is already added to the metadata tree from an earlier metadata source & now we are trying to add a group for `a.b`
        // In this e.g, startIndex would be 2. So, there is no point in adding children. We only need to update the tree appropriately
        if (startIndex >= pathSegments.length) {
          if (closestMetadata.isGroup()) {
            log.error(
                "Detected conflict between a new metadata property & existing group for suggestion path "
                    + closestMetadata.getPathFromRoot() + ". Ignoring property. Existing group belongs to (" + closestMetadata
                    .getBelongsTo().stream().collect(joining(",")) + "), New property belongs to " + containerPath);
          }
        } else {
          MetadataNonPropertySuggestionNode.class.cast(closestMetadata)
              .addChildren(module, property, pathSegments, startIndex, containerPath);
        }
      }
    }
  }

  private void addGroupsToIndex(Module module, Trie<String, MetadataSuggestionNode> rootSearchIndex,
      SpringConfigurationMetadata springConfigurationMetadata, String containerPath) {
    List<SpringConfigurationMetadataGroup> groups = springConfigurationMetadata.getGroups();
    if (groups != null) {
      groups.sort(comparing(SpringConfigurationMetadataGroup::getName));
      for (SpringConfigurationMetadataGroup group : groups) {
        String[] pathSegments = toSanitizedPathSegments(group.getName());
        MetadataSuggestionNode closestMetadata = MetadataSuggestionNode.class
            .cast(findDeepestMetadataMatch(rootSearchIndex, pathSegments, false));

        int startIndex;
        if (closestMetadata == null) { // path does not have a corresponding root element
          // lets build just the root element. Rest of the path segments will be taken care of by the addChildren method
          String unsanitisedRootSegment = firstPathSegment(group.getName());
          MetadataNonPropertySuggestionNode newGroupSuggestionNode =
              MetadataNonPropertySuggestionNode
                  .newInstance(unsanitisedRootSegment, null, containerPath);
          boolean noMoreSegmentsLeft = pathSegments.length == 1;
          if (noMoreSegmentsLeft) {
            newGroupSuggestionNode.setGroup(module, group);
          }
          rootSearchIndex.put(pathSegments[0], newGroupSuggestionNode);

          closestMetadata = newGroupSuggestionNode;
          // since we already handled the root level item, let addChildren start from index 1 of pathSegments
          startIndex = 1;
        } else {
          startIndex = closestMetadata.numOfHopesToRoot() + 1;
        }

        // Can happen when `a.b.c` is already added to the metadata tree from an earlier metadata source & now we are trying to add a group for `a.b`
        // In this e.g, startIndex would be 2. So, there is no point in adding children. We only need to update the tree appropriately
        if (closestMetadata.isProperty()) {
          log.error(
              "Detected conflict between an existing metadata property & new group for suggestion path "
                  + closestMetadata.getPathFromRoot()
                  + ". Ignoring new group. Existing Property belongs to (" + closestMetadata
                  .getBelongsTo().stream().collect(joining(",")) + "), New Group belongs to "
                  + containerPath);
        } else {
          if (startIndex >= pathSegments.length) {
            if (!closestMetadata.isGroup()) {
              // Node is an intermediate node that has neither group nor property assigned to it, lets assign this group to it
              MetadataNonPropertySuggestionNode groupSuggestionNode =
                  MetadataNonPropertySuggestionNode.class.cast(closestMetadata);
              groupSuggestionNode.addRefCascadeTillRoot(containerPath);
              groupSuggestionNode.setGroup(module, group);
            }
          } else {
            MetadataNonPropertySuggestionNode.class.cast(closestMetadata)
                .addChildren(module, group, pathSegments, startIndex, containerPath);
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

  private void removeReferences(Map<String, MetadataContainerInfo> containerPathToContainerInfo,
      Trie<String, MetadataSuggestionNode> rootSearchIndex,
      MetadataContainerInfo metadataContainerInfo) {
    debug(() -> log.debug("Removing references to " + metadataContainerInfo));
    String containerPath = metadataContainerInfo.getContainerPath();
    containerPathToContainerInfo.remove(containerPath);

    Iterator<String> searchIndexIterator = rootSearchIndex.keySet().iterator();
    while (searchIndexIterator.hasNext()) {
      SuggestionNode root = rootSearchIndex.get(searchIndexIterator.next());
      if (root != null) {
        boolean removeTree = MetadataSuggestionNode.class.cast(root)
            .removeRefCascadeDown(metadataContainerInfo.getContainerPath());
        if (removeTree) {
          searchIndexIterator.remove();
        }
      }
    }
  }

  /**
   * Debug logging can be enabled by adding fully classified class name/package name with # prefix
   * For eg., to enable debug logging, go `Help > Debug log settings` & type `#in.oneton.idea.spring.assistant.plugin.service.SuggestionServiceImpl`
   *
   * @param doWhenDebug code to execute when debug is enabled
   */
  private void debug(Runnable doWhenDebug) {
    if (log.isDebugEnabled()) {
      doWhenDebug.run();
    }
  }
}
