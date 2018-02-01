package in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiType;
import in.oneton.idea.spring.assistant.plugin.completion.SuggestionDocumentationHelper;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;

import static com.intellij.codeInsight.documentation.DocumentationManager.createHyperlink;
import static com.intellij.openapi.util.text.StringUtil.isEmpty;
import static in.oneton.idea.spring.assistant.plugin.util.GenericUtil.dotDelimitedOriginalNames;
import static in.oneton.idea.spring.assistant.plugin.util.PsiCustomUtil.toClassFqn;
import static in.oneton.idea.spring.assistant.plugin.util.PsiCustomUtil.toClassNonQualifiedName;

/**
 * Is a placeholder for classes that are known leafs with no additional navigation support
 * For e.g, all numbers, character & string classes from jdk
 */
public class DummyClassMetadata extends ClassMetadata {

  private SuggestionNodeType nodeType;

  DummyClassMetadata(@NotNull SuggestionNodeType nodeType) {
    this.nodeType = nodeType;
  }

  @Override
  protected void init(Module module) {
  }

  @Nullable
  @Override
  protected SuggestionDocumentationHelper doFindDirectChild(Module module, String pathSegment) {
    if (!isEmpty(pathSegment)) {
      switch (nodeType) {
        case BYTE:
          //noinspection EmptyCatchBlock
          try {
            //noinspection ResultOfMethodCallIgnored
            Byte.parseByte(pathSegment);
            return new DummyKeySuggestionDocumentationHelper(pathSegment);
          } catch (NumberFormatException e) {
          }
          break;
        case SHORT:
          //noinspection EmptyCatchBlock
          try {
            //noinspection ResultOfMethodCallIgnored
            Short.parseShort(pathSegment);
            return new DummyKeySuggestionDocumentationHelper(pathSegment);
          } catch (NumberFormatException e) {
          }
          break;
        case INT:
          //noinspection EmptyCatchBlock
          try {
            //noinspection ResultOfMethodCallIgnored
            Integer.parseInt(pathSegment);
            return new DummyKeySuggestionDocumentationHelper(pathSegment);
          } catch (NumberFormatException e) {
          }
          break;
        case LONG:
          //noinspection EmptyCatchBlock
          try {
            //noinspection ResultOfMethodCallIgnored
            Long.parseLong(pathSegment);
            return new DummyKeySuggestionDocumentationHelper(pathSegment);
          } catch (NumberFormatException e) {
          }
          break;
        case FLOAT:
          //noinspection EmptyCatchBlock
          try {
            //noinspection ResultOfMethodCallIgnored
            Float.parseFloat(pathSegment);
            return new DummyKeySuggestionDocumentationHelper(pathSegment);
          } catch (NumberFormatException e) {
          }
          break;
        case DOUBLE:
          //noinspection EmptyCatchBlock
          try {
            //noinspection ResultOfMethodCallIgnored
            Double.parseDouble(pathSegment);
            return new DummyKeySuggestionDocumentationHelper(pathSegment);
          } catch (NumberFormatException e) {
          }
          break;
        case CHAR:
          if (pathSegment.length() == 1) {
            return new DummyKeySuggestionDocumentationHelper(pathSegment);
          }
          break;
        case STRING:
          return new DummyKeySuggestionDocumentationHelper(pathSegment);
      }
    }
    return null;
  }

  @Override
  protected Collection<? extends SuggestionDocumentationHelper> doFindDirectChildrenForQueryPrefix(
      Module module, String querySegmentPrefix) {
    return null;
  }

  @Nullable
  @Override
  protected List<SuggestionNode> doFindDeepestSuggestionNode(Module module,
      List<SuggestionNode> matchesRootTillParentNode, String[] pathSegments,
      int pathSegmentStartIndex) {
    return null;
  }

  @Nullable
  @Override
  protected SortedSet<Suggestion> doFindKeySuggestionsForQueryPrefix(Module module,
      @Nullable String ancestralKeysDotDelimited, List<SuggestionNode> matchesRootTillParentNode,
      String[] querySegmentPrefixes, int querySegmentPrefixStartIndex) {
    return null;
  }

  @Override
  protected SortedSet<Suggestion> doFindValueSuggestionsForPrefix(Module module,
      List<SuggestionNode> matchesRootTillMe, String prefix) {
    return null;
  }

  @Nullable
  @Override
  protected String doGetDocumentationForValue(Module module, String nodeNavigationPathDotDelimited,
      String value) {
    return null;
  }

  @Override
  public boolean isLeaf(Module module) {
    return true;
  }

  //  @Override
  //  public void refreshMetadata(Module module) {
  //  }

  @NotNull
  @Override
  public SuggestionNodeType getSuggestionNodeType() {
    return nodeType;
  }

  @Nullable
  @Override
  public PsiType getPsiType() {
    switch (nodeType) {
      case BYTE:
        return PsiType.BYTE;
      case SHORT:
        return PsiType.SHORT;
      case INT:
        return PsiType.INT;
      case LONG:
        return PsiType.INT;
      case FLOAT:
        return PsiType.INT;
      case DOUBLE:
        return PsiType.INT;
      case CHAR:
        return PsiType.INT;
      case STRING:
        return PsiType.INT;
      case UNKNOWN_CLASS:
      default:
        return null;
    }
  }

  private Suggestion newSuggestion(Module module, String ancestralKeysDotDelimited,
      List<SuggestionNode> matchesRootTillMe) {
    Suggestion.SuggestionBuilder builder =
        Suggestion.builder().ancestralKeysDotDelimited(ancestralKeysDotDelimited)
            .pathOrValue(dotDelimitedOriginalNames(module, matchesRootTillMe))
            .matchesTopFirst(matchesRootTillMe).icon(nodeType.getIcon());

    PsiType psiType = getPsiType();
    if (psiType != null) {
      builder.shortType(toClassNonQualifiedName(psiType));
    }
    return builder.build();
  }


  private class DummyKeySuggestionDocumentationHelper implements SuggestionDocumentationHelper {

    private final String value;

    DummyKeySuggestionDocumentationHelper(String value) {
      this.value = value;
    }

    @Nullable
    @Override
    public String getOriginalName(Module module) {
      return value;
    }

    @NotNull
    @Override
    public Suggestion buildSuggestion(Module module, String ancestralKeysDotDelimited,
        List<SuggestionNode> matchesRootTillMe) {
      return newSuggestion(module, ancestralKeysDotDelimited, matchesRootTillMe);
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

      PsiType psiType = getPsiType();
      if (psiType != null) {
        String classFqn = toClassFqn(psiType);
        StringBuilder linkBuilder = new StringBuilder();
        createHyperlink(linkBuilder, classFqn, classFqn, false);
        builder.append(" (").append(linkBuilder.toString()).append(")");
      }

      return builder.toString();
    }

  }

}
