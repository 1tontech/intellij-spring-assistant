package in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiType;
import gnu.trove.THashMap;
import in.oneton.idea.spring.assistant.plugin.completion.FileType;
import in.oneton.idea.spring.assistant.plugin.completion.SuggestionDocumentationHelper;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType;
import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import static com.intellij.codeInsight.documentation.DocumentationManager.createHyperlink;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNode.sanitise;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType.ENUM;
import static in.oneton.idea.spring.assistant.plugin.util.GenericUtil.dotDelimitedOriginalNames;
import static in.oneton.idea.spring.assistant.plugin.util.PsiCustomUtil.computeDocumentation;
import static in.oneton.idea.spring.assistant.plugin.util.PsiCustomUtil.getReferredPsiType;
import static in.oneton.idea.spring.assistant.plugin.util.PsiCustomUtil.isValidType;
import static in.oneton.idea.spring.assistant.plugin.util.PsiCustomUtil.toClassFqn;
import static in.oneton.idea.spring.assistant.plugin.util.PsiCustomUtil.toClassNonQualifiedName;
import static in.oneton.idea.spring.assistant.plugin.util.PsiCustomUtil.toValidPsiClass;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

public class EnumClassMetadata extends ClassMetadata {

  @NotNull
  private final PsiClassType type;

  @Nullable
  private Map<String, PsiField> childLookup;
  @Nullable
  private Trie<String, PsiField> childrenTrie;

  EnumClassMetadata(@NotNull PsiClassType type) {
    this.type = type;
    PsiClass enumClass = requireNonNull(toValidPsiClass(type));
    assert enumClass.isEnum();
  }

  @Override
  protected void init(Module module) {
    init(type);
  }

  @Nullable
  @Override
  protected SuggestionDocumentationHelper doFindDirectChild(Module module, String pathSegment) {
    if (childLookup != null && childLookup.containsKey(pathSegment)) {
      return new EnumKeySuggestionDocumentationHelper(childLookup.get(pathSegment));
    }
    return null;
  }

  @Override
  protected Collection<? extends SuggestionDocumentationHelper> doFindDirectChildrenForQueryPrefix(
      Module module, String querySegmentPrefix) {
    if (childrenTrie != null) {
      SortedMap<String, PsiField> prefixMap = childrenTrie.prefixMap(querySegmentPrefix);
      if (prefixMap != null && prefixMap.size() != 0) {
        return prefixMap.values().stream().map(EnumKeySuggestionDocumentationHelper::new)
            .collect(toList());
      }
    }
    return null;
  }

  @Nullable
  @Override
  protected List<SuggestionNode> doFindDeepestSuggestionNode(Module module,
      List<SuggestionNode> matchesRootTillParentNode, String[] pathSegments,
      int pathSegmentStartIndex) {
    throw new IllegalAccessError(
        "Should not be called. To use as a map key call findDirectChild(..) instead");
  }

  @Nullable
  @Override
  protected SortedSet<Suggestion> doFindKeySuggestionsForQueryPrefix(Module module,
      FileType fileType, List<SuggestionNode> matchesRootTillParentNode, int numOfAncestors,
      String[] querySegmentPrefixes, int querySegmentPrefixStartIndex) {
    throw new IllegalAccessError(
        "Should not be called. To use as a map key call findDirectChild(..) instead");
  }

  @Override
  protected SortedSet<Suggestion> doFindValueSuggestionsForPrefix(Module module, FileType fileType,
      List<SuggestionNode> matchesRootTillMe, String prefix) {
    if (childrenTrie != null) {
      SortedMap<String, PsiField> prefixMap = childrenTrie.prefixMap(prefix);
      if (prefixMap != null && prefixMap.size() != 0) {
        return prefixMap.values().stream().map(
            psiField -> newSuggestion(fileType, matchesRootTillMe, matchesRootTillMe.size(), true,
                psiField)).collect(toCollection(TreeSet::new));
      }
    }
    return null;
  }

  @Nullable
  @Override
  protected String doGetDocumentationForValue(Module module, String nodeNavigationPathDotDelimited,
      String value) {
    if (childLookup != null) {
      // Format for the documentation is as follows
      /*
       * <p><b>a.b.c</b> ({@link com.acme.Generic}<{@link com.acme.Class1}, {@link com.acme.Class2}>)</p>
       * <p>Long description</p>
       * or of this type
       * <p><b>Type</b> {@link com.acme.Array}[]</p>
       * <p><b>Declared at</b>{@link com.acme.GenericRemovedClass#method}></p> <-- only for groups with method info
       */
      StringBuilder builder =
          new StringBuilder().append("<b>").append(nodeNavigationPathDotDelimited).append("</b>");

      String classFqn = toClassFqn(type);
      if (classFqn != null) {
        StringBuilder linkBuilder = new StringBuilder();
        createHyperlink(linkBuilder, classFqn, classFqn, false);
        builder.append(" (").append(linkBuilder.toString()).append(")");
      }

      PsiField psiField = childLookup.get(value);
      builder.append("<p>").append(requireNonNull(psiField.getName())).append("</p>");

      String documentation = computeDocumentation(psiField);
      if (documentation != null) {
        builder.append("<p>").append(documentation).append("</p>");
      }
      return builder.toString();
    }
    return null;
  }

  @Override
  public boolean doCheckIsLeaf(Module module) {
    return true;
  }

  @NotNull
  @Override
  public SuggestionNodeType getSuggestionNodeType() {
    return ENUM;
  }

  @Nullable
  @Override
  public PsiType getPsiType(Module module) {
    return type;
  }

  private void init(@NotNull PsiClassType type) {
    if (isValidType(type)) {
      PsiField[] fields = requireNonNull(toValidPsiClass(type)).getFields();
      List<PsiField> acceptableFields = new ArrayList<>();
      for (PsiField field : fields) {
        if (field != null && field.getType().equals(type)) {
          acceptableFields.add(field);
        }
      }
      if (acceptableFields.size() != 0) {
        childLookup = new THashMap<>();
        childrenTrie = new PatriciaTrie<>();
        acceptableFields.forEach(field -> {
          childLookup.put(sanitise(requireNonNull(field.getName())), field);
          childrenTrie.put(sanitise(field.getName()), field);
        });
      }
    } else {
      childLookup = null;
      childrenTrie = null;
    }
  }

  private Suggestion newSuggestion(FileType fileType, List<SuggestionNode> matchesRootTillMe,
      int numOfAncestors, boolean forValue, @NotNull PsiField value) {
    Suggestion.SuggestionBuilder builder =
        Suggestion.builder().numOfAncestors(numOfAncestors).matchesTopFirst(matchesRootTillMe)
            .shortType(toClassNonQualifiedName(type)).icon(ENUM.getIcon()).fileType(fileType);
    if (forValue) {
      builder.suggestionToDisplay(requireNonNull(value.getName()));
    } else {
      builder.suggestionToDisplay(dotDelimitedOriginalNames(matchesRootTillMe, numOfAncestors));
    }
    builder.forValue(forValue);
    return builder.build();
  }


  class EnumKeySuggestionDocumentationHelper implements SuggestionDocumentationHelper {
    private final PsiField field;

    EnumKeySuggestionDocumentationHelper(PsiField field) {
      this.field = field;
    }

    @Nullable
    @Override
    public String getOriginalName() {
      return field.getName();
    }

    @NotNull
    @Override
    public Suggestion buildSuggestionForKey(Module module, FileType fileType,
        List<SuggestionNode> matchesRootTillMe, int numOfAncestors) {
      return newSuggestion(fileType, matchesRootTillMe, numOfAncestors, false, field);
    }

    @Override
    public boolean supportsDocumentation() {
      return true;
    }

    @NotNull
    @Override
    public String getDocumentationForKey(Module module, String nodeNavigationPathDotDelimited) {
      /*
       * <p><b>a.b.c</b> ({@link com.acme.Generic}<{@link com.acme.Class1}, {@link com.acme.Class2}>)</p>
       * <p>Long description</p>
       */
      StringBuilder builder =
          new StringBuilder().append("<b>").append(nodeNavigationPathDotDelimited).append("</b>");

      String classFqn = toClassFqn(getReferredPsiType(field));

      if (classFqn != null) {
        StringBuilder linkBuilder = new StringBuilder();
        createHyperlink(linkBuilder, classFqn, classFqn, false);
        builder.append(" (").append(linkBuilder.toString()).append(")");
      }

      String documentation = computeDocumentation(field);
      if (documentation != null) {
        builder.append("<p>").append(documentation).append("</p>");
      }
      return builder.toString();
    }

    @NotNull
    @Override
    public SuggestionNodeType getSuggestionNodeType(Module module) {
      return ENUM;
    }
  }

}
