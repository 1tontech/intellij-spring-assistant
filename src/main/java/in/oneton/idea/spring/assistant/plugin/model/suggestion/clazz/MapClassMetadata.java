package in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiType;
import in.oneton.idea.spring.assistant.plugin.completion.FileType;
import in.oneton.idea.spring.assistant.plugin.completion.SuggestionDocumentationHelper;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static com.intellij.util.containers.ContainerUtil.isEmpty;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType.MAP;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType.UNKNOWN_CLASS;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz.ClassSuggestionNodeFactory.newMetadataProxy;
import static in.oneton.idea.spring.assistant.plugin.util.GenericUtil.newListWithMembers;
import static in.oneton.idea.spring.assistant.plugin.util.PsiCustomUtil.getTypeParameters;
import static in.oneton.idea.spring.assistant.plugin.util.PsiCustomUtil.isValidType;
import static java.util.stream.Collectors.toCollection;

public class MapClassMetadata extends ClassMetadata {

  @NotNull
  private final PsiClassType type;

  @Nullable
  private PsiType keyType;
  @Nullable
  private PsiType valueType;
  @Nullable
  private MetadataProxy keyMetadataProxy;
  @Nullable
  private MetadataProxy valueMetadataProxy;

  MapClassMetadata(@NotNull PsiClassType type) {
    this.type = type;
  }

  @Override
  protected void init(Module module) {
    init(module, type);
  }

  @Nullable
  @Override
  protected SuggestionDocumentationHelper doFindDirectChild(Module module, String pathSegment) {
    throw new IllegalAccessError(
        "Should never be called, as there can be never the case of Map<Map<K, V1>, V2>");
  }

  @Override
  protected Collection<? extends SuggestionDocumentationHelper> doFindDirectChildrenForQueryPrefix(
      Module module, String querySegmentPrefix) {
    throw new IllegalAccessError(
        "Should never be called, as there can be never the case of Map<Map<K, V1>, V2>");
  }

  @Nullable
  @Override
  protected List<SuggestionNode> doFindDeepestSuggestionNode(Module module,
      List<SuggestionNode> matchesRootTillParentNode, String[] pathSegments,
      int pathSegmentStartIndex) {
    if (keyMetadataProxy != null) {
      String pathSegment = pathSegments[pathSegmentStartIndex];
      SuggestionDocumentationHelper directChildKeyMatch =
          keyMetadataProxy.findDirectChild(module, pathSegment);
      if (directChildKeyMatch != null) {
        matchesRootTillParentNode.add(new MapKeySuggestionNode(directChildKeyMatch));
        boolean lastPathSegment = pathSegmentStartIndex == pathSegments.length - 1;
        if (lastPathSegment) {
          return matchesRootTillParentNode;
        } else {
          if (valueMetadataProxy != null) {
            return valueMetadataProxy
                .findDeepestSuggestionNode(module, matchesRootTillParentNode, pathSegments,
                    pathSegmentStartIndex + 1);
          }
        }
      }
    }
    return null;
  }

  @Nullable
  @Override
  protected SortedSet<Suggestion> doFindKeySuggestionsForQueryPrefix(Module module,
      FileType fileType, List<SuggestionNode> matchesRootTillMe, int numOfAncestors,
      String[] querySegmentPrefixes, int querySegmentPrefixStartIndex) {
    boolean lastPathSegment = querySegmentPrefixStartIndex == querySegmentPrefixes.length - 1;
    if (lastPathSegment && keyMetadataProxy != null) {
      String querySegmentPrefix = querySegmentPrefixes[querySegmentPrefixStartIndex];
      Collection<? extends SuggestionDocumentationHelper> directChildKeyMatches =
          keyMetadataProxy.findDirectChildrenForQueryPrefix(module, querySegmentPrefix);
      if (!isEmpty(directChildKeyMatches)) {
        return directChildKeyMatches.stream().map(helper -> helper.buildSuggestion(module, fileType,
            newListWithMembers(matchesRootTillMe, new MapKeySuggestionNode(helper)),
            numOfAncestors)).collect(toCollection(TreeSet::new));

      }
    }
    return null;
  }

  public SortedSet<Suggestion> findChildKeySuggestionForQueryPrefix(Module module,
      FileType fileType, List<SuggestionNode> matchesRootTillMe, int numOfAncestors,
      String[] querySegmentPrefixes, int querySegmentPrefixStartIndex) {
    if (valueMetadataProxy != null) {
      return valueMetadataProxy
          .findKeySuggestionsForQueryPrefix(module, fileType, matchesRootTillMe, numOfAncestors,
              querySegmentPrefixes, querySegmentPrefixStartIndex);
    }
    return null;
  }

  @Override
  protected SortedSet<Suggestion> doFindValueSuggestionsForPrefix(Module module, FileType fileType,
      List<SuggestionNode> matchesRootTillMe, String prefix) {
    if (valueMetadataProxy != null) {
      boolean valueIsLeaf = valueMetadataProxy.isLeaf(module);
      assert valueIsLeaf;
      return valueMetadataProxy
          .findValueSuggestionsForPrefix(module, fileType, matchesRootTillMe, prefix);
    }
    return null;
  }

  @Nullable
  @Override
  protected String doGetDocumentationForValue(Module module, String nodeNavigationPathDotDelimited,
      String value) {
    if (valueMetadataProxy != null) {
      return valueMetadataProxy
          .getDocumentationForValue(module, nodeNavigationPathDotDelimited, value);
    }
    return null;
  }

  @Override
  public boolean isLeaf(Module module) {
    return false;
  }

  //  @Override
  //  public void refreshMetadata(Module module) {
  //    PsiClass psiClass = toValidPsiClass(type);
  //    if (psiClass != null && keyMetadataProxy == null && valueMetadataProxy == null) {
  //      init(module, psiClass);
  //    }
  //  }

  @NotNull
  @Override
  public SuggestionNodeType getSuggestionNodeType() {
    return MAP;
  }

  @Nullable
  @Override
  public PsiType getPsiType() {
    return type;
  }

  private void init(Module module, PsiClassType type) {
    if (isValidType(type)) {
      Collection<PsiType> typeParameters = getTypeParameters(type);
      // If Map is raw, there are still two type parameters
      if (typeParameters != null && typeParameters.size() == 2) {
        Iterator<PsiType> iterator = typeParameters.iterator();
        keyType = iterator.next();
        if (keyType != null) {
          keyMetadataProxy = newMetadataProxy(module, keyType);
        }
        valueType = iterator.next();
        if (valueType != null) {
          valueMetadataProxy = newMetadataProxy(module, valueType);
        }
      }
    } else {
      keyType = null;
      keyMetadataProxy = null;
      valueType = null;
      valueMetadataProxy = null;
    }
  }

  @Nullable
  public PsiType getKeyType() {
    return keyType;
  }

  @Nullable
  public PsiType getValueType() {
    return valueType;
  }


  class MapKeySuggestionNode implements SuggestionNode {

    private final SuggestionDocumentationHelper helper;

    MapKeySuggestionNode(SuggestionDocumentationHelper helper) {
      this.helper = helper;
    }

    @Nullable
    @Override
    public List<SuggestionNode> findDeepestSuggestionNode(Module module,
        List<SuggestionNode> matchesRootTillParentNode, String[] pathSegments,
        int pathSegmentStartIndex) {
      throw new IllegalAccessError("Should never be called");
    }

    @Nullable
    @Override
    public SortedSet<Suggestion> findKeySuggestionsForQueryPrefix(Module module, FileType fileType,
        List<SuggestionNode> matchesRootTillMe, int numOfAncestors, String[] querySegmentPrefixes,
        int querySegmentPrefixStartIndex) {
      return findChildKeySuggestionForQueryPrefix(module, fileType, matchesRootTillMe,
          numOfAncestors, querySegmentPrefixes, querySegmentPrefixStartIndex);
    }

    @Override
    public String getDocumentationForKey(Module module, String nodeNavigationPathDotDelimited) {
      return helper.getDocumentationForKey(module, nodeNavigationPathDotDelimited);
    }

    @Nullable
    @Override
    public SortedSet<Suggestion> findValueSuggestionsForPrefix(Module module, FileType fileType,
        List<SuggestionNode> matchesRootTillMe, String prefix) {
      if (valueMetadataProxy != null) {
        return valueMetadataProxy
            .findValueSuggestionsForPrefix(module, fileType, matchesRootTillMe, prefix);
      }
      return null;
    }

    @Override
    public String getDocumentationForValue(Module module, String nodeNavigationPathDotDelimited,
        String value) {
      if (valueMetadataProxy != null) {
        return valueMetadataProxy
            .getDocumentationForValue(module, nodeNavigationPathDotDelimited, value);
      }
      return null;
    }

    @Override
    public boolean isLeaf(Module module) {
      return valueMetadataProxy == null || valueMetadataProxy.isLeaf(module);
    }

    @Override
    public boolean isMetadataNonProperty() {
      return false;
    }

    @Nullable
    @Override
    public String getOriginalName() {
      return helper.getOriginalName();
    }

    @Nullable
    @Override
    public String getNameForDocumentation(Module module) {
      return getOriginalName();
    }

    @Override
    public boolean supportsDocumentation() {
      return helper.supportsDocumentation();
    }

    @NotNull
    @Override
    public SuggestionNodeType getSuggestionNodeType(Module module) {
      if (valueMetadataProxy != null) {
        return valueMetadataProxy.getSuggestionNodeType(module);
      }
      return UNKNOWN_CLASS;
    }

  }

}
