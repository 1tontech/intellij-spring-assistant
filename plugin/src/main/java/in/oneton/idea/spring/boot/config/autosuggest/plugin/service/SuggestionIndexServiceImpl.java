package in.oneton.idea.spring.boot.config.autosuggest.plugin.service;

import com.google.gson.Gson;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.vfs.VirtualFile;
import in.oneton.idea.spring.boot.config.autosuggest.plugin.model.ContainerInfo;
import in.oneton.idea.spring.boot.config.autosuggest.plugin.model.MetadataNode;
import in.oneton.idea.spring.boot.config.autosuggest.plugin.model.Suggestion;
import in.oneton.idea.spring.boot.config.autosuggest.plugin.model.json.SpringConfigurationMetadata;
import in.oneton.idea.spring.boot.config.autosuggest.plugin.model.json.SpringConfigurationMetadataGroup;
import in.oneton.idea.spring.boot.config.autosuggest.plugin.model.json.SpringConfigurationMetadataHint;
import in.oneton.idea.spring.boot.config.autosuggest.plugin.model.json.SpringConfigurationMetadataProperty;
import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import static com.intellij.openapi.application.ApplicationManager.getApplication;
import static in.oneton.idea.spring.boot.config.autosuggest.plugin.Util.PERIOD_DELIMITER;
import static in.oneton.idea.spring.boot.config.autosuggest.plugin.model.ContainerInfo.getContainerFile;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class SuggestionIndexServiceImpl implements SuggestionIndexService {

  private static final Logger log = Logger.getInstance(SuggestionIndexServiceImpl.class);

  protected final Map<String, ContainerInfo> projectSeenContainerPathToContainerInfo;
  protected final Trie<String, MetadataNode> projectSanitisedRootSearchIndex;

  protected final Map<String, Map<String, ContainerInfo>>
      moduleNameToSeenContainerPathToContainerInfo;
  protected final Map<String, Trie<String, MetadataNode>> moduleNameToSanitisedRootSearchIndex;

  private volatile boolean indexingInProgress;

  SuggestionIndexServiceImpl() {
    projectSeenContainerPathToContainerInfo = new HashMap<>();
    projectSanitisedRootSearchIndex = new PatriciaTrie<>();

    moduleNameToSeenContainerPathToContainerInfo = new HashMap<>();
    moduleNameToSanitisedRootSearchIndex = new HashMap<>();
  }

  @Override
  public void init(Project project) {
    reIndex(project);
  }

  @Override
  public void reIndex(Project project) {
    if (!indexingInProgress) {
      getApplication().executeOnPooledThread(() -> {
        getApplication().runReadAction(() -> {
          indexingInProgress = true;
          try {
            log.info("Indexing requested for project " + project.getName());
            OrderEnumerator projectOrderEnumerator = OrderEnumerator.orderEntries(project);
            List<ContainerInfo> newProjectContainersToProcess =
                computeNewContainersToProcess(projectOrderEnumerator,
                    projectSeenContainerPathToContainerInfo);
            List<ContainerInfo> projectContainersToRemove =
                computeContainersToRemove(projectOrderEnumerator,
                    projectSeenContainerPathToContainerInfo);

            processContainers(newProjectContainersToProcess, projectContainersToRemove,
                projectSeenContainerPathToContainerInfo, projectSanitisedRootSearchIndex);

            Module[] modules = ModuleManager.getInstance(project).getModules();
            for (Module module : modules) {
              reindexModule(newProjectContainersToProcess, emptyList(), module);
            }
          } finally {
            indexingInProgress = false;
          }
        });
      });
    }
  }

  @Override
  public void reindex(Project project, Module module) {
    if (!indexingInProgress) {
      getApplication().executeOnPooledThread(() -> {
        getApplication().runReadAction(() -> {
          indexingInProgress = true;
          try {
            reindexModule(emptyList(), emptyList(), module);
          } finally {
            indexingInProgress = false;
          }
        });
      });
    }
  }

  @Nullable
  @Override
  public MetadataNode findDeepestExactMatch(Project project, List<String> containerElements) {
    String[] pathSegments =
        containerElements.stream().flatMap(element -> stream(toPathSegments(element)))
            .toArray(String[]::new);
    MetadataNode searchStartNode =
        projectSanitisedRootSearchIndex.get(MetadataNode.sanitize(pathSegments[0]));
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
  public MetadataNode findDeepestExactMatch(Project project, Module module,
      List<String> containerElements) {
    if (moduleNameToSanitisedRootSearchIndex.containsKey(module.getName())) {
      String[] pathSegments =
          containerElements.stream().flatMap(element -> stream(toPathSegments(element)))
              .toArray(String[]::new);
      MetadataNode searchStartNode = moduleNameToSanitisedRootSearchIndex.get(module.getName())
          .get(MetadataNode.sanitize(pathSegments[0]));
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
    return !indexingInProgress && projectSanitisedRootSearchIndex.size() != 0;
  }

  @Override
  public boolean canProvideSuggestions(Project project, Module module) {
    Trie<String, MetadataNode> sanitisedRootSearchIndex =
        moduleNameToSanitisedRootSearchIndex.get(module.getName());
    return !indexingInProgress && sanitisedRootSearchIndex != null
        && sanitisedRootSearchIndex.size() != 0;
  }

  @Override
  public List<LookupElementBuilder> computeSuggestions(Project project, ClassLoader classLoader,
      @Nullable List<String> ancestralKeys, String queryString) {
    return computeSuggestions(projectSanitisedRootSearchIndex, classLoader, ancestralKeys,
        queryString);
  }

  @Override
  public List<LookupElementBuilder> computeSuggestions(Project project, Module module,
      ClassLoader classLoader, @Nullable List<String> ancestralKeys, String queryString) {
    return computeSuggestions(moduleNameToSanitisedRootSearchIndex.get(module.getName()),
        classLoader, ancestralKeys, queryString);
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
        updatedSinceLastSeen = containerInfo.isUpdatedAfter(seenContainerInfo);
      }

      boolean processMetadata =
          (!seenBefore || updatedSinceLastSeen) && containerInfo.containsMetadataFile();
      if (processMetadata) {
        containersToProcess.add(containerInfo);
      }

      if (!seenBefore) {
        seenContainerPathToContainerInfo.put(containerInfo.getContainerPath(), containerInfo);
      }
    }

    if (containersToProcess.size() == 0) {
      log.info("No (new)metadata files to index");
    }
    return containersToProcess;
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
            .map(metadataFileContainer -> getContainerFile(metadataFileContainer).getPath())
            .collect(toSet());
    Set<String> knownContainerPathSet = seenContainerPathToContainerInfo.keySet();
    knownContainerPathSet.removeAll(newContainerPaths);
    return knownContainerPathSet.stream().map(seenContainerPathToContainerInfo::get)
        .collect(toList());
  }

  private void processContainers(List<ContainerInfo> containersToProcess,
      List<ContainerInfo> containersToRemove,
      Map<String, ContainerInfo> seenContainerPathToContainerInfo,
      Trie<String, MetadataNode> sanitisedRootSearchIndex) {
    // Lets remove references to files that are no longer present in classpath
    containersToRemove.forEach(this::removeReferences);

    for (ContainerInfo containerInfo : containersToProcess) {
      // lets remove existing references from search index, as these files are modified, so that we can rebuild index
      if (seenContainerPathToContainerInfo.containsKey(containerInfo.getContainerPath())) {
        removeReferencesFromIndex(containerInfo, sanitisedRootSearchIndex);
      }

      String metadataFilePath = containerInfo.getPath();
      try (InputStream inputStream = containerInfo.getMetadataFile().getInputStream()) {
        SpringConfigurationMetadata springConfigurationMetadata = new Gson()
            .fromJson(new BufferedReader(new InputStreamReader(inputStream)),
                SpringConfigurationMetadata.class);
        buildMetadataHierarchy(sanitisedRootSearchIndex, metadataFilePath,
            springConfigurationMetadata);
      } catch (IOException e) {
        log.error("Exception encountered while processing metadata file: " + metadataFilePath, e);
        removeReferences(containerInfo);
      }
    }
  }

  private void reindexModule(List<ContainerInfo> newProjectSourcesToProcess,
      List<ContainerInfo> projectContainersToRemove, Module module) {
    log.info("Indexing requested for module " + module.getName());
    Map<String, ContainerInfo> moduleSeenContainerPathToSeenContainerInfo =
        moduleNameToSeenContainerPathToContainerInfo
            .computeIfAbsent(module.getName(), k -> new HashMap<>());

    Trie<String, MetadataNode> moduleSanitisedRootSearchIndex =
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

    processContainers(newModuleContainersToProcess, moduleContainersToRemove,
        moduleSeenContainerPathToSeenContainerInfo, moduleSanitisedRootSearchIndex);
    log.info("Indexing for module is complete");
  }

  private List<LookupElementBuilder> computeSuggestions(
      Trie<String, MetadataNode> sanitisedRootSearchIndex, ClassLoader classLoader,
      @Nullable List<String> ancestralKeys, String queryString) {
    log.info("Search started");
    String sanitizedQueryString = MetadataNode.sanitize(queryString);
    String[] querySegments = toPathSegments(sanitizedQueryString);
    Set<Suggestion> suggestions = null;
    if (ancestralKeys != null) {
      String[] pathSegments =
          ancestralKeys.stream().flatMap(element -> stream(toPathSegments(element)))
              .toArray(String[]::new);
      MetadataNode searchStartNode =
          sanitisedRootSearchIndex.get(MetadataNode.sanitize(pathSegments[0]));
      if (searchStartNode != null) {
        if (pathSegments.length > 1) {
          searchStartNode = searchStartNode.findDeepestMatch(pathSegments, 1, true);
        }
        if (searchStartNode != null) {
          if (!searchStartNode.isLeaf()) {
            suggestions =
                searchStartNode.findChildSuggestions(querySegments, 0, classLoader, false);
            if (suggestions == null) {
              suggestions =
                  searchStartNode.findChildSuggestions(querySegments, 0, classLoader, true);
            }
          } else {
            suggestions = searchStartNode.getSuggestionValues(classLoader, false);
          }
        }
      }
    } else {
      String sanitisedQuerySegment = MetadataNode.sanitize(querySegments[0]);
      SortedMap<String, MetadataNode> topLevelQueryResults =
          sanitisedRootSearchIndex.prefixMap(sanitisedQuerySegment);
      Collection<MetadataNode> childNodes = topLevelQueryResults.values();
      suggestions = getSuggestions(classLoader, querySegments, childNodes, false);

      if (suggestions == null) {
        Collection<MetadataNode> nodesToSearchWithin = sanitisedRootSearchIndex.values();
        suggestions = getSuggestions(classLoader, querySegments, nodesToSearchWithin, true);
      }
    }

    log.info("Search done");

    if (suggestions != null) {
      return toLookupElementBuilders(suggestions, classLoader);
    }
    return null;
  }

  @Nullable
  private Set<Suggestion> getSuggestions(ClassLoader classLoader, String[] querySegments,
      Collection<MetadataNode> nodesToSearchWithin, boolean proceedTillLeaf) {
    Set<Suggestion> suggestions = null;
    for (MetadataNode metadataNode : nodesToSearchWithin) {
      Set<Suggestion> childSuggestions =
          metadataNode.findSuggestions(querySegments, 1, classLoader, proceedTillLeaf);
      if (childSuggestions != null) {
        if (suggestions == null) {
          suggestions = new HashSet<>();
        }
        suggestions.addAll(childSuggestions);
      }
    }
    return suggestions;
  }

  private void buildMetadataHierarchy(Trie<String, MetadataNode> sanitisedRootSearchIndex,
      String metadataFileOrLibraryPath, SpringConfigurationMetadata springConfigurationMetadata) {
    // populate groups
    List<SpringConfigurationMetadataGroup> groups = springConfigurationMetadata.getGroups();
    if (groups != null) {
      groups.sort(comparing(SpringConfigurationMetadataGroup::getName));
      for (SpringConfigurationMetadataGroup group : groups) {
        String[] pathSegments = toPathSegments(group.getName());
        MetadataNode closestMetadata =
            findDeepestMatch(sanitisedRootSearchIndex, pathSegments, false);
        if (closestMetadata == null) {
          String firstSegment = pathSegments[0];
          closestMetadata = MetadataNode.newInstance(firstSegment, null, metadataFileOrLibraryPath);
          boolean noMoreSegmentsLeft = pathSegments.length == 1;
          if (noMoreSegmentsLeft) {
            closestMetadata.setGroup(group);
          }
          String sanitizedFirstSegment = MetadataNode.sanitize(firstSegment);
          sanitisedRootSearchIndex.put(sanitizedFirstSegment, closestMetadata);
        }
        closestMetadata.addChildren(group, pathSegments, metadataFileOrLibraryPath);
      }
    }

    // populate properties
    List<SpringConfigurationMetadataProperty> properties =
        springConfigurationMetadata.getProperties();
    properties.sort(comparing(SpringConfigurationMetadataProperty::getName));
    for (SpringConfigurationMetadataProperty property : properties) {
      String[] pathSegments = toPathSegments(property.getName());
      MetadataNode closestMetadata =
          findDeepestMatch(sanitisedRootSearchIndex, pathSegments, false);
      if (closestMetadata == null) {
        String firstSegment = pathSegments[0];
        closestMetadata = MetadataNode.newInstance(firstSegment, null, metadataFileOrLibraryPath);
        boolean noMoreSegmentsLeft = pathSegments.length == 1;
        if (noMoreSegmentsLeft) {
          closestMetadata.setProperty(property);
        }
        String sanitizedFirstSegment = MetadataNode.sanitize(firstSegment);
        sanitisedRootSearchIndex.put(sanitizedFirstSegment, closestMetadata);
      }
      closestMetadata.addChildren(property, pathSegments, metadataFileOrLibraryPath);
    }

    // update hints
    List<SpringConfigurationMetadataHint> hints = springConfigurationMetadata.getHints();
    if (hints != null) {
      hints.sort(comparing(SpringConfigurationMetadataHint::getName));
      for (SpringConfigurationMetadataHint hint : hints) {
        String[] pathSegments = toPathSegments(hint.getName());
        MetadataNode closestMetadata =
            findDeepestMatch(sanitisedRootSearchIndex, pathSegments, true);
        if (closestMetadata != null && closestMetadata.getDepth() == pathSegments.length) {
          assert closestMetadata.getProperty() != null;
          closestMetadata.getProperty().setHint(hint);
        }
      }
    }
  }

  @Nullable
  private MetadataNode findDeepestMatch(Map<String, MetadataNode> sanitisedRoots,
      String[] pathSegments, boolean matchAllSegments) {
    String firstSegment = pathSegments[0];
    MetadataNode closestMatchedRoot = sanitisedRoots.get(MetadataNode.sanitize(firstSegment));
    if (closestMatchedRoot != null) {
      closestMatchedRoot = closestMatchedRoot.findDeepestMatch(pathSegments, 1, matchAllSegments);
    }
    return closestMatchedRoot;
  }

  private void removeReferences(ContainerInfo containerInfo) {
    String containerPath = containerInfo.getContainerPath();
    projectSeenContainerPathToContainerInfo.remove(containerPath);
    removeReferencesFromIndex(containerInfo, projectSanitisedRootSearchIndex);

    moduleNameToSeenContainerPathToContainerInfo.forEach(
        (moduleName, seenContainerPathToContainerInfo) -> seenContainerPathToContainerInfo
            .remove(containerPath));
    moduleNameToSanitisedRootSearchIndex.forEach(
        (moduleName, sanitisedRootSearchIndex) -> removeReferencesFromIndex(containerInfo,
            sanitisedRootSearchIndex));
  }

  private void removeReferencesFromIndex(ContainerInfo containerInfo,
      Trie<String, MetadataNode> sanitisedRootSearchIndex) {
    for (String key : sanitisedRootSearchIndex.keySet()) {
      MetadataNode root = sanitisedRootSearchIndex.get(key);
      boolean removeTree = root.removeRef(containerInfo.getContainerPath());
      if (removeTree) {
        sanitisedRootSearchIndex.remove(key);
      }
    }
  }

  @Nullable
  private List<LookupElementBuilder> toLookupElementBuilders(@Nullable Set<Suggestion> suggestions,
      ClassLoader classLoader) {
    if (suggestions != null) {
      return suggestions.stream().map(v -> v.newLookupElement(classLoader)).collect(toList());
    }
    return null;
  }

  private String[] toPathSegments(String element) {
    return element.split(PERIOD_DELIMITER, -1);
  }

}
