package in.oneton.idea.spring.boot.config.autosuggest.plugin.service;

import com.google.gson.Gson;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VirtualFile;
import in.oneton.idea.spring.boot.config.autosuggest.plugin.model.MetadataNode;
import in.oneton.idea.spring.boot.config.autosuggest.plugin.model.Suggestion;
import in.oneton.idea.spring.boot.config.autosuggest.plugin.model.SuggestionSourcePathAndTimestamp;
import in.oneton.idea.spring.boot.config.autosuggest.plugin.model.json.SpringConfigurationMetadata;
import in.oneton.idea.spring.boot.config.autosuggest.plugin.model.json.SpringConfigurationMetadataGroup;
import in.oneton.idea.spring.boot.config.autosuggest.plugin.model.json.SpringConfigurationMetadataHint;
import in.oneton.idea.spring.boot.config.autosuggest.plugin.model.json.SpringConfigurationMetadataProperty;
import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import static com.intellij.openapi.application.ApplicationManager.getApplication;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

public class SuggestionIndexServiceImpl implements SuggestionIndexService {

  private static final Logger log = Logger.getInstance(SuggestionIndexServiceImpl.class);

  private static final String PERIOD_DELIMITER = "\\.";
  protected final Map<String, Long> projectProcessedLibraryNameToTimestamp;
  protected final Map<String, MetadataNode> projectSanitisedRoots;
  protected final Trie<String, MetadataNode> projectSanitisedRootSearchIndex;
  protected final Set<SuggestionSourcePathAndTimestamp> projectProcessedMetadataFiles;

  protected final Map<String, Map<String, Long>> moduleNameToProcessedLibraryNameToTimestamp;
  protected final Map<String, Map<String, MetadataNode>> moduleNameToSanitisedRoots;
  protected final Map<String, Trie<String, MetadataNode>> moduleNameToSanitisedRootSearchIndex;
  protected final Map<String, Set<SuggestionSourcePathAndTimestamp>>
      moduleNameToProcessedMetadataFiles;
  // TODO: Implement this
  private final Trie<String, SuggestionSourcePathAndTimestamp> deprecatedIndex =
      new PatriciaTrie<>();
  private volatile boolean indexingInProgress;

  SuggestionIndexServiceImpl() {
    projectProcessedLibraryNameToTimestamp = new HashMap<>();
    projectSanitisedRoots = new HashMap<>();
    projectSanitisedRootSearchIndex = new PatriciaTrie<>();
    projectProcessedMetadataFiles = new HashSet<>();

    moduleNameToProcessedLibraryNameToTimestamp = new HashMap<>();
    moduleNameToSanitisedRoots = new HashMap<>();
    moduleNameToSanitisedRootSearchIndex = new HashMap<>();
    moduleNameToProcessedMetadataFiles = new HashMap<>();
  }

  @Override
  public void init(Project project) {
    if (!indexingInProgress) {
      //noinspection CodeBlock2Expr
      getApplication().executeOnPooledThread(() -> {
        getApplication().runReadAction(() -> {
          reIndex(project);
        });
      });
    }
  }

  @Override
  public void reIndex(Project project) {
    if (!indexingInProgress) {
      indexingInProgress = true;
      log.info("Indexing requested for project " + project.getName());
      OrderEnumerator projectOrderEnumerator = OrderEnumerator.orderEntries(project);
      List<VirtualFile> newProjectSourcesToProcess =
          computeMetadataFilesToProcess(projectOrderEnumerator,
              projectProcessedLibraryNameToTimestamp, projectProcessedMetadataFiles);

      processSources(newProjectSourcesToProcess, projectSanitisedRoots,
          projectSanitisedRootSearchIndex, projectProcessedMetadataFiles);

      for (Module module : ModuleManager.getInstance(project).getModules()) {
        reindexModule(newProjectSourcesToProcess, module);
      }
      indexingInProgress = false;
      // TODO: If either library or resource from classpath is removed, We need to remove references from search index & other parts
    }
  }

  @Override
  public void reindex(Project project, Module module) {
    if (!indexingInProgress) {
      indexingInProgress = true;
      reindexModule(new ArrayList<>(), module);
      indexingInProgress = false;
      // TODO: If either library or resource from classpath is removed, We need to remove references from search index & other parts
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
      @Nullable List<String> containerElements, String queryString) {
    return computeSuggestions(projectSanitisedRootSearchIndex, classLoader, containerElements,
        queryString);
  }

  @Override
  public List<LookupElementBuilder> computeSuggestions(Project project, Module module,
      ClassLoader classLoader, @Nullable List<String> containerElements, String queryString) {
    return computeSuggestions(moduleNameToSanitisedRootSearchIndex.get(module.getName()),
        classLoader, containerElements, queryString);
  }

  private void reindexModule(List<VirtualFile> newProjectSourcesToProcess, Module module) {
    log.info("Indexing requested for module " + module.getName());

    Map<String, Long> moduleProcessedLibraryNameToTimestamp =
        moduleNameToProcessedLibraryNameToTimestamp
            .computeIfAbsent(module.getName(), k -> new HashMap<>());

    Map<String, MetadataNode> moduleSanitisedRoots =
        moduleNameToSanitisedRoots.computeIfAbsent(module.getName(), k -> new HashMap<>());

    Trie<String, MetadataNode> moduleSanitisedRootSearchIndex =
        moduleNameToSanitisedRootSearchIndex.get(module.getName());
    if (moduleSanitisedRootSearchIndex == null) {
      moduleSanitisedRootSearchIndex = new PatriciaTrie<>();
      moduleNameToSanitisedRootSearchIndex.put(module.getName(), moduleSanitisedRootSearchIndex);
    }

    Set<SuggestionSourcePathAndTimestamp> moduleProcessedMetadataFiles =
        moduleNameToProcessedMetadataFiles.computeIfAbsent(module.getName(), k -> new HashSet<>());

    OrderEnumerator moduleOrderEnumerator = OrderEnumerator.orderEntries(module);
    List<VirtualFile> newModuleSourcesToProcess =
        computeMetadataFilesToProcess(moduleOrderEnumerator, moduleProcessedLibraryNameToTimestamp,
            moduleProcessedMetadataFiles);

    newModuleSourcesToProcess.addAll(newProjectSourcesToProcess);

    processSources(newModuleSourcesToProcess, moduleSanitisedRoots, moduleSanitisedRootSearchIndex,
        moduleProcessedMetadataFiles);
    log.info("Indexing for module is complete");
  }

  private List<VirtualFile> computeMetadataFilesToProcess(OrderEnumerator projectOrderEnumerator,
      Map<String, Long> processedLibraryToTimestamp,
      Set<SuggestionSourcePathAndTimestamp> processedMetadataFiles) {
    List<VirtualFile> metadataFilesToProcess = new ArrayList<>();
    stream(projectOrderEnumerator.recursively().classes().getRoots())
        .forEach(metadataFileContainer -> {
          // metadataFileContainer can be a library(jar) or class folder under the project/module
          boolean isLibrary = !metadataFileContainer.isWritable();
          boolean metadataFileContainerProcessedBefore =
              isLibrary && processedLibraryToTimestamp.containsKey(metadataFileContainer.getPath());
          boolean libraryUpdatedSinceLastIndex = metadataFileContainerProcessedBefore
              && processedLibraryToTimestamp.get(metadataFileContainer.getPath())
              != metadataFileContainer.getModificationStamp();

          boolean processMetadataFileContainer =
              !metadataFileContainerProcessedBefore || libraryUpdatedSinceLastIndex;
          if (processMetadataFileContainer) {
            VirtualFile matchedSource = findSuggestionSource(metadataFileContainer, "");
            if (matchedSource != null) {
              SuggestionSourcePathAndTimestamp sourcePathAndTimestamp =
                  SuggestionSourcePathAndTimestamp.builder().path(matchedSource.getPath())
                      .timestamp(matchedSource.getModificationStamp()).build();
              if (!processedMetadataFiles.contains(sourcePathAndTimestamp)) {
                // Lets remove entries from existing index, so that the file can be reprocessed
                if (!isLibrary) {
                  processedMetadataFiles.remove(sourcePathAndTimestamp);
                  removeReferencesFromIndex(sourcePathAndTimestamp);
                }
                metadataFilesToProcess.add(matchedSource);
              }
            }

            if (isLibrary) {
              processedLibraryToTimestamp.put(metadataFileContainer.getPath(),
                  metadataFileContainer.getModificationStamp());
            }
          }
        });

    if (metadataFilesToProcess.size() == 0) {
      log.info("No (new)metadata files to index");
    }
    return metadataFilesToProcess;
  }

  private void processSources(List<VirtualFile> metadataFiles,
      Map<String, MetadataNode> sanitisedRoots, Trie<String, MetadataNode> sanitisedRootSearchIndex,
      Set<SuggestionSourcePathAndTimestamp> processedMetadataFiles) {
    for (VirtualFile metadataFile : metadataFiles) {
      SuggestionSourcePathAndTimestamp sourcePathAndTimestamp =
          SuggestionSourcePathAndTimestamp.builder().path(metadataFile.getPath())
              .timestamp(metadataFile.getModificationStamp()).build();
      try (InputStream inputStream = metadataFile.getInputStream()) {
        SpringConfigurationMetadata springConfigurationMetadata = new Gson()
            .fromJson(new BufferedReader(new InputStreamReader(inputStream)),
                SpringConfigurationMetadata.class);
        buildMetadataHierarchy(sanitisedRoots, sanitisedRootSearchIndex, sourcePathAndTimestamp,
            springConfigurationMetadata);
        processedMetadataFiles.add(sourcePathAndTimestamp);
      } catch (IOException e) {
        // Ignoring as there is nothing much we can do w.r.t this. Lets move on
        processedMetadataFiles.remove(sourcePathAndTimestamp);
        removeReferencesFromIndex(sourcePathAndTimestamp);
      }
    }
  }

  @NotNull
  private String[] toPathSegments(String element) {
    return element.split(PERIOD_DELIMITER, -1);
  }

  private List<LookupElementBuilder> computeSuggestions(
      Trie<String, MetadataNode> sanitisedRootSearchIndex, ClassLoader classLoader,
      @Nullable List<String> containerElements, String queryString) {
    String sanitizedQueryString = MetadataNode.sanitize(queryString);
    String[] querySegments = toPathSegments(sanitizedQueryString);
    if (containerElements != null) {
      String[] pathSegments =
          containerElements.stream().flatMap(element -> stream(toPathSegments(element)))
              .toArray(String[]::new);
      MetadataNode searchStartNode =
          sanitisedRootSearchIndex.get(MetadataNode.sanitize(pathSegments[0]));
      if (searchStartNode != null) {
        if (pathSegments.length > 1) {
          searchStartNode = searchStartNode.findDeepestMatch(pathSegments, 1, true);
        }
        if (searchStartNode != null) {
          if (!searchStartNode.isLeaf()) {
            return toLookupElementBuilders(
                searchStartNode.findChildSuggestions(querySegments, 0, classLoader), classLoader);
          } else {
            return toLookupElementBuilders(searchStartNode.getSuggestionValues(classLoader),
                classLoader);
          }
        }
      }
      return null;
    } else {
      String sanitisedQuerySegment = MetadataNode.sanitize(querySegments[0]);
      SortedMap<String, MetadataNode> topLevelQueryResults =
          sanitisedRootSearchIndex.prefixMap(sanitisedQuerySegment);
      Collection<MetadataNode> childNodes = topLevelQueryResults.values();
      Set<Suggestion> suggestions = null;
      for (MetadataNode metadataNode : childNodes) {
        Set<Suggestion> childSuggestions =
            metadataNode.findSuggestions(querySegments, 1, classLoader);
        if (childSuggestions != null) {
          if (suggestions == null) {
            suggestions = new HashSet<>();
          }
          suggestions.addAll(childSuggestions);
        }
      }

      return toLookupElementBuilders(suggestions, classLoader);
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

  private void buildMetadataHierarchy(Map<String, MetadataNode> sanitisedRoots,
      Trie<String, MetadataNode> sanitisedRootSearchIndex,
      SuggestionSourcePathAndTimestamp sourcePathAndTimestamp,
      SpringConfigurationMetadata springConfigurationMetadata) {
    // populate groups
    List<SpringConfigurationMetadataGroup> groups = springConfigurationMetadata.getGroups();
    if (groups != null) {
      groups.sort(comparing(SpringConfigurationMetadataGroup::getName));
      for (SpringConfigurationMetadataGroup group : groups) {
        String[] pathSegments = toPathSegments(group.getName());
        MetadataNode closestMetadata = findDeepestMatch(sanitisedRoots, pathSegments, false);
        if (closestMetadata == null) {
          String firstSegment = pathSegments[0];
          closestMetadata = MetadataNode.newInstance(firstSegment, null, sourcePathAndTimestamp);
          boolean noMoreSegmentsLeft = pathSegments.length == 1;
          if (noMoreSegmentsLeft) {
            closestMetadata.setGroup(group);
          }
          String sanitizedFirstSegment = MetadataNode.sanitize(firstSegment);
          sanitisedRoots.put(sanitizedFirstSegment, closestMetadata);
          sanitisedRootSearchIndex.put(sanitizedFirstSegment, closestMetadata);
        }
        closestMetadata.addChildren(group, pathSegments, sourcePathAndTimestamp);
      }
    }

    // populate properties
    List<SpringConfigurationMetadataProperty> properties =
        springConfigurationMetadata.getProperties();
    properties.sort(comparing(SpringConfigurationMetadataProperty::getName));
    for (SpringConfigurationMetadataProperty property : properties) {
      String[] pathSegments = toPathSegments(property.getName());
      MetadataNode closestMetadata = findDeepestMatch(sanitisedRoots, pathSegments, false);
      if (closestMetadata == null) {
        String firstSegment = pathSegments[0];
        closestMetadata = MetadataNode.newInstance(firstSegment, null, sourcePathAndTimestamp);
        boolean noMoreSegmentsLeft = pathSegments.length == 1;
        if (noMoreSegmentsLeft) {
          closestMetadata.setProperty(property);
        }
        String sanitizedFirstSegment = MetadataNode.sanitize(firstSegment);
        sanitisedRoots.put(sanitizedFirstSegment, closestMetadata);
        sanitisedRootSearchIndex.put(sanitizedFirstSegment, closestMetadata);
      }
      closestMetadata.addChildren(property, pathSegments, sourcePathAndTimestamp);
    }

    // update hints
    List<SpringConfigurationMetadataHint> hints = springConfigurationMetadata.getHints();
    if (hints != null) {
      hints.sort(comparing(SpringConfigurationMetadataHint::getName));
      for (SpringConfigurationMetadataHint hint : hints) {
        String[] pathSegments = toPathSegments(hint.getName());
        MetadataNode closestMetadata = findDeepestMatch(sanitisedRoots, pathSegments, true);
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

  @Nullable
  private VirtualFile findSuggestionSource(VirtualFile root, String spacing) {
    if (!root.is(VFileProperty.SYMLINK)) {
      //      System.out.println(spacing + root.getName());
      //noinspection UnsafeVfsRecursion
      for (VirtualFile child : asList(root.getChildren())) {
        if (child.getName().equals("spring-configuration-metadata.json")) {
          return child;
        }
        VirtualFile matchedFile = findSuggestionSource(child, spacing + " ");
        if (matchedFile != null) {
          return matchedFile;
        }
      }
    }
    return null;
  }

  private void removeReferencesFromIndex(SuggestionSourcePathAndTimestamp sourcePathAndTimestamp) {
    projectSanitisedRoots.forEach((k, v) -> v.removeRef(sourcePathAndTimestamp));
    projectProcessedMetadataFiles.remove(sourcePathAndTimestamp);

    moduleNameToSanitisedRoots.forEach((name, sanitisedRoots) -> sanitisedRoots
        .forEach((k, v) -> v.removeRef(sourcePathAndTimestamp)));
    moduleNameToProcessedMetadataFiles
        .forEach((name, sanitisedRoots) -> sanitisedRoots.remove(sourcePathAndTimestamp));
  }

}
