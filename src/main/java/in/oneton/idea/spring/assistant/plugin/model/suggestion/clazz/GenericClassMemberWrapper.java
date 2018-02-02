package in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import in.oneton.idea.spring.assistant.plugin.completion.FileType;
import in.oneton.idea.spring.assistant.plugin.completion.SuggestionDocumentationHelper;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.SortedSet;

import static com.intellij.codeInsight.documentation.DocumentationManager.createHyperlink;
import static in.oneton.idea.spring.assistant.plugin.insert.handler.YamlValueInsertHandler.unescapeValue;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType.UNKNOWN_CLASS;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz.ClassSuggestionNodeFactory.newMetadataProxy;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata.json.SpringConfigurationMetadataDeprecationLevel.warning;
import static in.oneton.idea.spring.assistant.plugin.util.GenericUtil.methodForDocumentationNavigation;
import static in.oneton.idea.spring.assistant.plugin.util.GenericUtil.removeGenerics;
import static in.oneton.idea.spring.assistant.plugin.util.PsiCustomUtil.computeDocumentation;
import static in.oneton.idea.spring.assistant.plugin.util.PsiCustomUtil.getContainingClass;
import static in.oneton.idea.spring.assistant.plugin.util.PsiCustomUtil.getReferredPsiType;
import static in.oneton.idea.spring.assistant.plugin.util.PsiCustomUtil.toClassFqn;
import static in.oneton.idea.spring.assistant.plugin.util.PsiCustomUtil.toClassNonQualifiedName;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;

// TODO: Should the psiMember also be accessed via the cache as this also might become invalid as soon as the class becomes invalid?


/**
 * Documentation is never requested for a class, instead its requested for properties within the class.
 * This acts as a proxy for documentation & basic details about the property/getter method that contains actual documentation
 */
public class GenericClassMemberWrapper implements SuggestionNode, SuggestionDocumentationHelper {

  private final PsiMember member;

  @NotNull
  private final String originalName;
  @Nullable
  private final String documentation;
  @Nullable
  private final String longType;
  @Nullable
  private final String shortType;
  private final boolean deprecated;

  private MetadataProxy proxy;

  public GenericClassMemberWrapper(@NotNull PsiMember member) {
    this.member = member;
    this.originalName = requireNonNull(member.getName());
    this.documentation = computeDocumentation(member);
    this.longType = toClassFqn(getReferredPsiType(member));
    this.shortType = toClassNonQualifiedName(getReferredPsiType(member));
    this.deprecated = computeDeprecationStatus();
  }

  public MetadataProxy getMemberReferredClassMetadataProxy(Module module) {
    if (proxy != null) {
      proxy = newMetadataProxy(module, getReferredPsiType(member));
    }
    return proxy;
  }

  private <T> T doWithMemberReferredClassProxy(Module module, ProxyInvoker<T> invoker,
      T defaultValue) {
    MetadataProxy delegate = getMemberReferredClassMetadataProxy(module);
    if (delegate != null) {
      return invoker.invoke(delegate);
    }
    return defaultValue;
  }

  @Nullable
  @Override
  public List<SuggestionNode> findDeepestSuggestionNode(Module module,
      List<SuggestionNode> matchesRootTillParentNode, String[] pathSegments,
      int pathSegmentStartIndex) {
    return doWithMemberReferredClassProxy(module, proxy -> proxy
        .findDeepestSuggestionNode(module, matchesRootTillParentNode, pathSegments,
            pathSegmentStartIndex), null);
  }

  @Nullable
  @Override
  public SortedSet<Suggestion> findKeySuggestionsForQueryPrefix(Module module, FileType fileType,
      List<SuggestionNode> matchesRootTillMe, int numOfAncestors, String[] querySegmentPrefixes,
      int querySegmentPrefixStartIndex) {
    return doWithMemberReferredClassProxy(module, proxy -> proxy
        .findKeySuggestionsForQueryPrefix(module, fileType, matchesRootTillMe, numOfAncestors,
            querySegmentPrefixes, querySegmentPrefixStartIndex), null);
  }

  @NotNull
  public String getOriginalName() {
    return originalName;
  }

  @Nullable
  @Override
  public String getNameForDocumentation(Module module) {
    return doWithMemberReferredClassProxy(module, proxy -> {
      if (proxy.targetRepresentsArray() || proxy.targetClassRepresentsIterable(module)) {
        return originalName + "[]";
      } else {
        return originalName;
      }
    }, originalName);
  }

  @Nullable
  @Override
  public SortedSet<Suggestion> findValueSuggestionsForPrefix(Module module, FileType fileType,
      List<SuggestionNode> matchesRootTillMe, String prefix) {
    return doWithMemberReferredClassProxy(module,
        proxy -> proxy.findValueSuggestionsForPrefix(module, fileType, matchesRootTillMe, prefix),
        null);
  }

  @Override
  public boolean isLeaf(Module module) {
    return doWithMemberReferredClassProxy(module, proxy -> proxy.isLeaf(module), true);
  }

  @Override
  public boolean isMetadataNonProperty() {
    return false;
  }

  private boolean computeDeprecationStatus() {
    if (member instanceof PsiField) {
      return stream(PsiField.class.cast(member).getType().getAnnotations()).anyMatch(annotation -> {
        String fqn = annotation.getQualifiedName();
        return fqn != null && fqn.equals("java.lang.Deprecated");
      });
    } else if (member instanceof PsiMethod) {
      return stream(requireNonNull(PsiMethod.class.cast(member).getReturnType()).getAnnotations())
          .anyMatch(annotation -> {
            String fqn = annotation.getQualifiedName();
            return fqn != null && fqn.equals("java.lang.Deprecated");
          });
    }
    throw new RuntimeException("Method supports targets of type PsiField & PsiMethod only");
  }

  @NotNull
  @Override
  public Suggestion buildSuggestion(Module module, FileType fileType,
      List<SuggestionNode> matchesRootTillMe, int numOfAncestors) {
    Suggestion.SuggestionBuilder builder =
        Suggestion.builder().icon(proxy.getSuggestionNodeType(module).getIcon())
            .description(documentation).shortType(shortType).numOfAncestors(numOfAncestors)
            .matchesTopFirst(matchesRootTillMe);
    if (deprecated) {
      builder.deprecationLevel(warning);
    }
    return builder.fileType(fileType).build();
  }

  @Override
  public boolean supportsDocumentation() {
    return true;
  }

  // TODO: Method is incomplete. Fix this
  @NotNull
  @Override
  public String getDocumentationForKey(Module module, String nodeNavigationPathDotDelimited) {
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

    String classFqn = toClassFqn(getReferredPsiType(member));

    if (classFqn != null) {
      StringBuilder linkBuilder = new StringBuilder();
      createHyperlink(linkBuilder, classFqn, classFqn, false);
      builder.append(" (").append(linkBuilder.toString()).append(")");
    }

    if (documentation != null) {
      builder.append("<p>").append(documentation).append("</p>");
    }

    PsiClass sourceTypePsiClass = getContainingClass(member);
    if (sourceTypePsiClass != null) {
      String sourceType = sourceTypePsiClass.toString();
      String sourceTypeInJavadocFormat = removeGenerics(sourceType);
      sourceTypeInJavadocFormat += ("." + sourceType);

      StringBuilder buffer = new StringBuilder();
      createHyperlink(buffer, methodForDocumentationNavigation(sourceTypeInJavadocFormat),
          sourceTypeInJavadocFormat, false);
      sourceTypeInJavadocFormat = buffer.toString();
      builder.append("<p>Declared at ").append(sourceTypeInJavadocFormat).append("</p>");
    }

    return builder.toString();
  }

  @Override
  public String getDocumentationForValue(Module module, String nodeNavigationPathDotDelimited,
      String value) {
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

    if (longType != null) {
      StringBuilder linkBuilder = new StringBuilder();
      createHyperlink(linkBuilder, longType, longType, false);
      builder.append(" (").append(linkBuilder.toString()).append(")");
    }

    builder.append("<p>").append(unescapeValue(value)).append("</p>");

    if (documentation != null) {
      builder.append("<p>").append(documentation).append("</p>");
    }

    //    String sourceType = getContainingClass(member).toString();
    //    String sourceTypeInJavadocFormat = removeGenerics(sourceType);
    //    sourceTypeInJavadocFormat += ("." + sourceType);
    //
    //    StringBuilder buffer = new StringBuilder();
    //    DocumentationManager
    //        .createHyperlink(buffer, methodForDocumentationNavigation(sourceTypeInJavadocFormat),
    //            sourceTypeInJavadocFormat, false);
    //    sourceTypeInJavadocFormat = buffer.toString();
    //    builder.append("<p>Declared at ").append(sourceTypeInJavadocFormat).append("</p>");

    return builder.toString();
  }

  @NotNull
  @Override
  public SuggestionNodeType getSuggestionNodeType(Module module) {
    return doWithMemberReferredClassProxy(module, proxy -> proxy.getSuggestionNodeType(module),
        UNKNOWN_CLASS);
  }

  private interface ProxyInvoker<T> {
    T invoke(MetadataProxy proxy);
  }

}
