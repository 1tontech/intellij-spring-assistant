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
import in.oneton.idea.spring.assistant.plugin.model.ContainerInfo;
import in.oneton.idea.spring.assistant.plugin.model.MetadataGroupSuggestionNode;
import in.oneton.idea.spring.assistant.plugin.model.MetadataPropertySuggestionNode;
import in.oneton.idea.spring.assistant.plugin.model.MetadataSuggestionNode;
import in.oneton.idea.spring.assistant.plugin.model.Suggestion;
import in.oneton.idea.spring.assistant.plugin.model.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.model.json.SpringConfigurationMetadata;
import in.oneton.idea.spring.assistant.plugin.model.json.SpringConfigurationMetadataGroup;
import in.oneton.idea.spring.assistant.plugin.model.json.SpringConfigurationMetadataHint;
import in.oneton.idea.spring.assistant.plugin.model.json.SpringConfigurationMetadataProperty;
import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
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
import static in.oneton.idea.spring.assistant.plugin.Util.toPathSegments;
import static in.oneton.idea.spring.assistant.plugin.model.ContainerInfo.getContainerFile;
import static in.oneton.idea.spring.assistant.plugin.model.SuggestionNode.sanitize;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class SuggestionServiceImpl implements SuggestionService {

  private static final Logger log = Logger.getInstance(SuggestionServiceImpl.class);

  // TODO: Need to check if the project level items can be removed
  protected final Map<String, ContainerInfo> projectSeenContainerPathToContainerInfo;
  protected final Trie<String, MetadataSuggestionNode> projectSanitisedRootSearchIndex;
  protected final Map<String, Map<String, ContainerInfo>>
      moduleNameToSeenContainerPathToContainerInfo;
  private final Map<String, Trie<String, MetadataSuggestionNode>>
      moduleNameToSanitisedRootSearchIndex;
  private Future<?> currentExecution;
  private volatile boolean indexingInProgress;

  SuggestionServiceImpl() {
    projectSeenContainerPathToContainerInfo = new THashMap<>();
    projectSanitisedRootSearchIndex = new PatriciaTrie<>();

    moduleNameToSeenContainerPathToContainerInfo = new THashMap<>();
    moduleNameToSanitisedRootSearchIndex = new THashMap<>();
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
  public SuggestionNode findDeepestExactMatch(Project project, List<String> containerElements) {
    String[] pathSegments =
        containerElements.stream().flatMap(element -> stream(toPathSegments(element)))
            .toArray(String[]::new);
    MetadataSuggestionNode searchStartNode =
        projectSanitisedRootSearchIndex.get(sanitize(pathSegments[0]));
    if (searchStartNode != null) {
      if (pathSegments.length > 1) {
        return searchStartNode.findDeepestMatch(pathSegments, 1, true);
      }
      return searchStartNode;
    }
    return null;
  }

  @Nullable
  @Override
  public SuggestionNode findDeepestExactMatch(Project project, Module module,
      List<String> containerElements) {
    if (moduleNameToSanitisedRootSearchIndex.containsKey(module.getName())) {
      String[] pathSegments =
          containerElements.stream().flatMap(element -> stream(toPathSegments(element)))
              .toArray(String[]::new);
      MetadataSuggestionNode searchStartNode =
          moduleNameToSanitisedRootSearchIndex.get(module.getName()).get(sanitize(pathSegments[0]));
      if (searchStartNode != null) {
        if (pathSegments.length > 1) {
          return searchStartNode.findDeepestMatch(pathSegments, 1, true);
        }
        return searchStartNode;
      }
    }
    return null;
  }

  @Override
  public boolean canProvideSuggestions(Project project) {
    return moduleNameToSanitisedRootSearchIndex.values().stream().mapToInt(Map::size).sum() != 0;
  }

  @Override
  public boolean canProvideSuggestions(Project project, Module module) {
    Trie<String, MetadataSuggestionNode> sanitisedRootSearchIndex =
        moduleNameToSanitisedRootSearchIndex.get(module.getName());
    return sanitisedRootSearchIndex != null && sanitisedRootSearchIndex.size() != 0;
  }

  @Override
  public List<LookupElementBuilder> computeSuggestions(Project project, PsiElement element,
      @Nullable List<String> ancestralKeys, String queryWithDotDelimitedPrefixes) {
    return computeSuggestions(projectSanitisedRootSearchIndex, element, ancestralKeys,
        queryWithDotDelimitedPrefixes);
  }

  @Override
  public List<LookupElementBuilder> computeSuggestions(Project project, Module module,
      PsiElement element, @Nullable List<String> ancestralKeys,
      String queryWithDotDelimitedPrefixes) {
    return computeSuggestions(moduleNameToSanitisedRootSearchIndex.get(module.getName()), element,
        ancestralKeys, queryWithDotDelimitedPrefixes);
  }

  private List<ContainerInfo> computeNewContainersToProcess(OrderEnumerator orderEnumerator,
      Map<String, ContainerInfo> seenContainerPathToContainerInfo) {
    List<ContainerInfo> containersToProcess = new ArrayList<>();
    for (VirtualFile metadataFileContainer : orderEnumerator.recursively().classes().getRoots()) {
      ContainerInfo containerInfo = ContainerInfo.newInstance(metadataFileContainer);
      boolean seenBefore =
          seenContainerPathToContainerInfo.containsKey(containerInfo.getContainerPath());

      boolean updatedSinceLastSeen = false;
      if (seenBefore) {
        ContainerInfo seenContainerInfo =
            seenContainerPathToContainerInfo.get(containerInfo.getContainerPath());
        updatedSinceLastSeen = containerInfo.isModified(seenContainerInfo);
        if (updatedSinceLastSeen) {
          debug(() -> log.debug(
              "Container seems to have been updated. Previous version: " + seenContainerInfo
                  + "; Newer version: " + containerInfo));
        }
      }

      boolean looksFresh = !seenBefore || updatedSinceLastSeen;
      boolean processMetadata = looksFresh && containerInfo.containsMetadataFile();
      if (processMetadata) {
        containersToProcess.add(containerInfo);
      }

      if (looksFresh) {
        seenContainerPathToContainerInfo.put(containerInfo.getContainerPath(), containerInfo);
      }
    }

    if (containersToProcess.size() == 0) {
      debug(() -> log.debug("No (new)metadata files to index"));
    }
    return containersToProcess;
  }

  private List<LookupElementBuilder> computeSuggestions(
      Trie<String, MetadataSuggestionNode> sanitisedRootSearchIndex, PsiElement element,
      @Nullable List<String> ancestralKeys, String queryWithDotDelimitedPrefixes) {
    debug(() -> log.debug("Search requested for " + queryWithDotDelimitedPrefixes));
    StopWatch timer = new StopWatch();
    timer.start();
    try {
      String sanitizedQueryWithDotDelimitedPrefixes = sanitize(queryWithDotDelimitedPrefixes);
      String[] querySegmentPrefixes = toPathSegments(sanitizedQueryWithDotDelimitedPrefixes);
      Set<Suggestion> suggestions = null;
      if (ancestralKeys != null) {
        String[] ancestralKeySegments =
            ancestralKeys.stream().flatMap(key -> stream(toPathSegments(key)))
                .toArray(String[]::new);
        MetadataSuggestionNode searchStartNode =
            sanitisedRootSearchIndex.get(sanitize(ancestralKeySegments[0]));
        if (searchStartNode != null) {
          if (ancestralKeySegments.length > 1) {
            searchStartNode = searchStartNode.findDeepestMatch(ancestralKeySegments, 1, true);
          }
          if (searchStartNode != null) {
            // if node is a leaf, this means, the user is looking for values for the given key, lets find the suggestions for values
            if (!searchStartNode.isLeaf()) {
              suggestions = searchStartNode.findSuggestionsForValue(element.getText());
            } else {
              List<MetadataSuggestionNode> nodesFromRootTillStartNode =
                  searchStartNode.getNodesFromRootTillMe();
              suggestions = searchStartNode
                  .findChildSuggestionsForKey(nodesFromRootTillStartNode, 0, querySegmentPrefixes,
                      0, true);
              //              // since we don't have any matches at the root level, may be a subset of intermediary nodes might match the entered string
              //              if (suggestions == null) {
              //                suggestions = searchStartNode
              //                    .findChildSuggestionsForKey(nodesFromRootTillStartNode, querySegmentPrefixes, 0,
              //                        0, true);
              //              }
            }
          }
        }
      } else {
        String sanitisedRootQuerySegmentPrefix = sanitize(querySegmentPrefixes[0]);
        if (StringUtils.isEmpty(sanitisedRootQuerySegmentPrefix)) {
          Collection<MetadataSuggestionNode> nodesToSearchWithin =
              sanitisedRootSearchIndex.values();
          suggestions = getSuggestions(nodesToSearchWithin, 1, querySegmentPrefixes, 0, true);
        } else {
          SortedMap<String, MetadataSuggestionNode> topLevelQueryResults =
              sanitisedRootSearchIndex.prefixMap(sanitisedRootQuerySegmentPrefix);
          Collection<MetadataSuggestionNode> childNodes = topLevelQueryResults.values();
          suggestions = getSuggestions(childNodes, 1, querySegmentPrefixes, 1, false);
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
  private Set<Suggestion> getSuggestions(Collection<MetadataSuggestionNode> nodesToSearchWithin,
      @SuppressWarnings("SameParameterValue") int suggestionDepthFromEndOfPath,
      String[] querySegmentPrefixes, int querySegmentPrefixStartIndex,
      boolean navigateDeepIfNoMatches) {
    Set<Suggestion> suggestions = null;
    for (MetadataSuggestionNode suggestionNode : nodesToSearchWithin) {
      Set<Suggestion> matchedSuggestions = suggestionNode
          .findSuggestionsForKey(modifiableList(suggestionNode), suggestionDepthFromEndOfPath,
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
  private List<ContainerInfo> computeContainersToRemove(OrderEnumerator orderEnumerator,
      Map<String, ContainerInfo> seenContainerPathToContainerInfo) {
    Set<String> newContainerPaths =
        Arrays.stream(orderEnumerator.recursively().classes().getRoots())
            .map(metadataFileContainer -> getContainerFile(metadataFileContainer).getUrl())
            .collect(toSet());
    Set<String> knownContainerPathSet = new HashSet<>(seenContainerPathToContainerInfo.keySet());
    knownContainerPathSet.removeAll(newContainerPaths);
    return knownContainerPathSet.stream().map(seenContainerPathToContainerInfo::get)
        .collect(toList());
  }

  private void processContainers(Module module, List<ContainerInfo> containersToProcess,
      List<ContainerInfo> containersToRemove,
      Map<String, ContainerInfo> seenContainerPathToContainerInfo,
      Trie<String, MetadataSuggestionNode> sanitisedRootSearchIndex) {
    // Lets remove references to files that are no longer present in classpath
    containersToRemove.forEach(
        container -> removeReferences(seenContainerPathToContainerInfo, sanitisedRootSearchIndex,
            container));

    for (ContainerInfo containerInfo : containersToProcess) {
      // lets remove existing references from search index, as these files are modified, so that we can rebuild index
      if (seenContainerPathToContainerInfo.containsKey(containerInfo.getContainerPath())) {
        removeReferences(seenContainerPathToContainerInfo, sanitisedRootSearchIndex, containerInfo);
      }

      String metadataFilePath = containerInfo.getPath();
      try (InputStream inputStream = containerInfo.getMetadataFile().getInputStream()) {
        SpringConfigurationMetadata springConfigurationMetadata = new Gson()
            .fromJson(new BufferedReader(new InputStreamReader(inputStream)),
                SpringConfigurationMetadata.class);
        buildMetadataHierarchy(module, sanitisedRootSearchIndex, containerInfo,
            springConfigurationMetadata);

        seenContainerPathToContainerInfo.put(containerInfo.getContainerPath(), containerInfo);
      } catch (IOException e) {
        log.error("Exception encountered while processing metadata file: " + metadataFilePath, e);
        removeReferences(seenContainerPathToContainerInfo, sanitisedRootSearchIndex, containerInfo);
      }
    }
  }

  private void reindexModule(List<ContainerInfo> newProjectSourcesToProcess,
      List<ContainerInfo> projectContainersToRemove, Module module) {
    Map<String, ContainerInfo> moduleSeenContainerPathToSeenContainerInfo =
        moduleNameToSeenContainerPathToContainerInfo
            .computeIfAbsent(module.getName(), k -> new THashMap<>());

    Trie<String, MetadataSuggestionNode> moduleSanitisedRootSearchIndex =
        moduleNameToSanitisedRootSearchIndex.get(module.getName());
    if (moduleSanitisedRootSearchIndex == null) {
      moduleSanitisedRootSearchIndex = new PatriciaTrie<>();
      moduleNameToSanitisedRootSearchIndex.put(module.getName(), moduleSanitisedRootSearchIndex);
    }

    OrderEnumerator moduleOrderEnumerator = OrderEnumerator.orderEntries(module);

    List<ContainerInfo> newModuleContainersToProcess =
        computeNewContainersToProcess(moduleOrderEnumerator,
            moduleSeenContainerPathToSeenContainerInfo);
    newModuleContainersToProcess.addAll(newProjectSourcesToProcess);

    List<ContainerInfo> moduleContainersToRemove = computeContainersToRemove(moduleOrderEnumerator,
        moduleSeenContainerPathToSeenContainerInfo);
    moduleContainersToRemove.addAll(projectContainersToRemove);

    processContainers(module, newModuleContainersToProcess, moduleContainersToRemove,
        moduleSeenContainerPathToSeenContainerInfo, moduleSanitisedRootSearchIndex);
  }

  private void buildMetadataHierarchy(Module module,
      Trie<String, MetadataSuggestionNode> sanitisedRootSearchIndex, ContainerInfo containerInfo,
      SpringConfigurationMetadata springConfigurationMetadata) {
    debug(() -> log.debug("Adding container to index " + containerInfo));
    String containerPath = containerInfo.getContainerPath();
    // populate groups
    List<SpringConfigurationMetadataGroup> groups = springConfigurationMetadata.getGroups();
    if (groups != null) {
      groups.sort(comparing(SpringConfigurationMetadataGroup::getName));
      for (SpringConfigurationMetadataGroup group : groups) {
        String[] pathSegments = toPathSegments(group.getName());
        MetadataSuggestionNode closestMetadata = MetadataSuggestionNode.class
            .cast(findDeepestMatch(sanitisedRootSearchIndex, pathSegments, false));

        int startIndex;
        if (closestMetadata == null) { // path does not have a corresponding root element
          // lets build just the root element. Rest of the path segments will be taken care of by the addChildren method
          String rootSegment = pathSegments[0];
          MetadataGroupSuggestionNode newGroupSuggestionNode =
              MetadataGroupSuggestionNode.newInstance(rootSegment, null, containerPath);
          boolean noMoreSegmentsLeft = pathSegments.length == 1;
          if (noMoreSegmentsLeft) {
            newGroupSuggestionNode.setGroup(group);
          }
          String sanitizedRootSegment = sanitize(rootSegment);
          sanitisedRootSearchIndex.put(sanitizedRootSegment, newGroupSuggestionNode);

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
              MetadataGroupSuggestionNode groupSuggestionNode =
                  MetadataGroupSuggestionNode.class.cast(closestMetadata);
              groupSuggestionNode.addRefCascadeTillRoot(containerPath);
              groupSuggestionNode.setGroup(group);
            }
          } else {
            MetadataGroupSuggestionNode.class.cast(closestMetadata)
                .addChildren(group, pathSegments, startIndex, containerPath);
          }
        }
        //        if (startIndex >= pathSegments.length) {
        //          if (closestMetadata.isProperty()) {
        //            log.error("Detected conflict between a metadata property & group for suggestion path "
        //                + closestMetadata.getPathFromRoot() + ". Ignoring group. Property belongs to ("
        //                + closestMetadata.getBelongsTo().stream().collect(joining(","))
        //                + "), Group belongs to " + containerPath);
        //          } else if (!closestMetadata.isGroup()) {
        //            // Node is an intermediate node that has neither group nor property assigned to it, lets assign this group to it
        //            MetadataGroupSuggestionNode groupSuggestionNode =
        //                MetadataGroupSuggestionNode.class.cast(closestMetadata);
        //            groupSuggestionNode.addRefCascadeTillRoot(containerPath);
        //            groupSuggestionNode.setGroup(group);
        //          }
        //        } else {
        //          if (closestMetadata.isGroup()) {
        //            MetadataGroupSuggestionNode.class.cast(closestMetadata)
        //                .addChildren(group, pathSegments, startIndex, containerPath);
        //          } else {
        //            log.error("Detected conflict between a metadata property & group for suggestion path "
        //                + closestMetadata.getPathFromRoot() + ". Ignoring group. Property belongs to ("
        //                + closestMetadata.getBelongsTo().stream().collect(joining(","))
        //                + "), Group belongs to " + containerPath);
        //          }
        //        }
      }
    }

    // populate properties
    List<SpringConfigurationMetadataProperty> properties =
        springConfigurationMetadata.getProperties();
    properties.sort(comparing(SpringConfigurationMetadataProperty::getName));
    for (SpringConfigurationMetadataProperty property : properties) {
      String[] pathSegments = toPathSegments(property.getName());
      MetadataSuggestionNode closestMetadata =
          findDeepestMatch(sanitisedRootSearchIndex, pathSegments, false);

      int startIndex;
      if (closestMetadata == null) { // path does not have a corresponding root element
        String rootSegment = pathSegments[0];
        boolean onlyRootSegmentExists = pathSegments.length == 1;
        if (onlyRootSegmentExists) {
          closestMetadata = MetadataPropertySuggestionNode
              .newInstance(module, rootSegment, property, null, containerPath);
        } else {
          closestMetadata =
              MetadataGroupSuggestionNode.newInstance(rootSegment, null, containerPath);
        }
        String sanitizedRootSegment = sanitize(rootSegment);
        sanitisedRootSearchIndex.put(sanitizedRootSegment, closestMetadata);

        // since we already handled the root level item, let addChildren start from index 1 of pathSegments
        startIndex = 1;
      } else {
        startIndex = closestMetadata.numOfHopesToRoot() + 1;
      }

      if (closestMetadata.isProperty()) {
        log.error("Detected a duplicate metadata property for suggestion path " + closestMetadata
            .getPathFromRoot() + ". Ignoring property. Existing property belongs to ("
            + closestMetadata.getBelongsTo().stream().collect(joining(","))
            + "), New property belongs to " + containerPath);
      } else {
        // Can happen when `a.b.c` is already added to the metadata tree from an earlier metadata source & now we are trying to add a group for `a.b`
        // In this e.g, startIndex would be 2. So, there is no point in adding children. We only need to update the tree appropriately
        if (startIndex >= pathSegments.length) {
          if (closestMetadata.isGroup()) {
            log.error(
                "Detected conflict between a new metadata property & existing group for suggestion path "
                    + closestMetadata.getPathFromRoot()
                    + ". Ignoring property. Existing group belongs to (" + closestMetadata
                    .getBelongsTo().stream().collect(joining(",")) + "), New property belongs to "
                    + containerPath);
          }
        } else {
          MetadataGroupSuggestionNode.class.cast(closestMetadata)
              .addChildren(module, property, pathSegments, startIndex, containerPath);
        }
      }

      //      // Can happen when `a.b.c` is already added to the metadata tree from an earlier metadata source & now we are trying to add a group for `a.b`
      //      // In this e.g, startIndex would be 2. So, there is no point in adding children. We only need to update the tree appropriately
      //      if (startIndex >= pathSegments.length) {
      //        if (closestMetadata.isGroup()) {
      //          log.error("Detected conflict between a metadata property & group for suggestion path "
      //              + closestMetadata.getPathFromRoot() + ". Ignoring property. Group belongs to ("
      //              + closestMetadata.getBelongsTo().stream().collect(joining(","))
      //              + "), Property belongs to " + containerPath);
      //        } else if (closestMetadata.isProperty()) {
      //          log.error("Detected a duplicate metadata property for suggestion path "
      //              + closestMetadata.getPathFromRoot() + ". Ignoring property. Existing property belongs to ("
      //              + closestMetadata.getBelongsTo().stream().collect(joining(","))
      //              + "), New property belongs to " + containerPath);
      //        }
      //      } else {
      //        if (closestMetadata.isGroup()) {
      //          closestMetadata.addChildren(property, pathSegments, containerPath);
      //        } else {
      //
      //        }
      //      }
    }

    // update hints
    List<SpringConfigurationMetadataHint> hints = springConfigurationMetadata.getHints();
    if (hints != null) {
      hints.sort(comparing(SpringConfigurationMetadataHint::getName));
      for (SpringConfigurationMetadataHint hint : hints) {
        String[] pathSegments = toPathSegments(hint.getExpectedPropertyName());
        MetadataSuggestionNode closestMetadata =
            findDeepestMatch(sanitisedRootSearchIndex, pathSegments, true);
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

  @Nullable
  private MetadataSuggestionNode findDeepestMatch(
      Map<String, MetadataSuggestionNode> sanitisedRoots, String[] pathSegments,
      boolean matchAllSegments) {
    String firstSegment = pathSegments[0];
    MetadataSuggestionNode closestMatchedRoot = sanitisedRoots.get(sanitize(firstSegment));
    if (closestMatchedRoot != null) {
      closestMatchedRoot = closestMatchedRoot.findDeepestMatch(pathSegments, 1, matchAllSegments);
    }
    return closestMatchedRoot;
  }

  private void removeReferences(Map<String, ContainerInfo> containerPathToContainerInfo,
      Trie<String, MetadataSuggestionNode> sanitisedRootSearchIndex, ContainerInfo containerInfo) {
    debug(() -> log.debug("Removing references to " + containerInfo));
    String containerPath = containerInfo.getContainerPath();
    containerPathToContainerInfo.remove(containerPath);

    Iterator<String> searchIndexIterator = sanitisedRootSearchIndex.keySet().iterator();
    while (searchIndexIterator.hasNext()) {
      SuggestionNode root = sanitisedRootSearchIndex.get(searchIndexIterator.next());
      if (root != null) {
        boolean removeTree = MetadataSuggestionNode.class.cast(root)
            .removeRefCascadeDown(containerInfo.getContainerPath());
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
