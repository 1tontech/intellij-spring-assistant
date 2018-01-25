package in.oneton.idea.spring.assistant.plugin;

import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.javadoc.PsiDocComment;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz.ClassSuggestionNodeProxy;
import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

import static com.intellij.psi.util.PropertyUtil.getPropertyName;
import static in.oneton.idea.spring.assistant.plugin.ClassUtil.getContainerPsiClass;
import static in.oneton.idea.spring.assistant.plugin.ClassUtil.getPsiClassType;
import static in.oneton.idea.spring.assistant.plugin.Util.dotDelimitedOriginalNames;
import static in.oneton.idea.spring.assistant.plugin.Util.methodForDocumentationNavigation;
import static in.oneton.idea.spring.assistant.plugin.Util.newListWithMembers;
import static in.oneton.idea.spring.assistant.plugin.Util.removeGenerics;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;

/**
 * Documentation is never requested for a class, instead its requested for properties within the class.
 * This acts as a proxy for documentation & basic details about the property/getter method that contains actual documentation
 */
@Builder
@Getter
public class PsiMemberWrapper implements SuggestionDocumentationHelper {
  private PsiMember target;
  private PsiClass psiClass;
  private ClassSuggestionNodeProxy proxy;

  public ClassSuggestionNodeProxy getTargetNode() {
    if (proxy != null) {
      psiClass = getTargetPsiClass();
      proxy = new ClassSuggestionNodeProxy(psiClass, this);
    }
    return proxy;
  }

  @NotNull
  public String getOriginalName() {
    if (target instanceof PsiField) {
      //noinspection ConstantConditions
      return target.getName();
    } else if (target instanceof PsiMethod) {
      //noinspection ConstantConditions
      return getPropertyName((PsiMethod) target);
    }
    throw new RuntimeException("Method supports targets of type PsiField & PsiMethod only");
  }

  @Nullable
  public String getDocumentation() {
    PsiDocComment docComment = null;
    if (target instanceof PsiField) {
      docComment = PsiField.class.cast(target).getDocComment();
    } else if (target instanceof PsiMethod) {
      docComment = PsiMethod.class.cast(target).getDocComment();
    }
    if (docComment != null) {
      return docComment.getText();
    }
    throw new RuntimeException("Method supports targets of type PsiField & PsiMethod only");
  }

  @NotNull
  public String getShortType() {
    return removeGenerics(getTargetPsiClass().getText());
  }

  @NotNull
  public boolean isDeprecated() {
    if (target instanceof PsiField) {
      return stream(PsiField.class.cast(target).getType().getAnnotations()).anyMatch(annotation -> {
        String fqn = annotation.getQualifiedName();
        return fqn != null && fqn.equals("java.lang.Deprecated");
      });
    } else if (target instanceof PsiMethod) {
      return stream(requireNonNull(PsiMethod.class.cast(target).getReturnType()).getAnnotations())
          .anyMatch(annotation -> {
            String fqn = annotation.getQualifiedName();
            return fqn != null && fqn.equals("java.lang.Deprecated");
          });
    }
    throw new RuntimeException("Method supports targets of type PsiField & PsiMethod only");
  }

  private PsiClass getTargetPsiClass() {
    return requireNonNull(getPsiClassType(target)).resolve();
  }

  @NotNull
  @Override
  public Suggestion buildSuggestion(String ancestralKeysDotDelimited,
      List<SuggestionNode> matchesRootTillParentNode, SuggestionNode currentNode) {
    return Suggestion.builder()
        .value(dotDelimitedOriginalNames(matchesRootTillParentNode) + "." + getOriginalName())
        .icon(proxy.getType().getIcon()).description(getDocumentation()).shortType(getShortType())
        .ancestralKeysDotDelimited(ancestralKeysDotDelimited)
        .matchesTopFirst(newListWithMembers(matchesRootTillParentNode, proxy)).build();
  }

  // TODO: Method is incomplete. Fix this
  @NotNull
  @Override
  public String getDocumentationForKey(String nodeNavigationPathDotDelimited) {
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

    StringBuilder linkBuilder = new StringBuilder();
    DocumentationManager
        .createHyperlink(linkBuilder, psiClass.toString(), psiClass.toString(), false);
    builder.append(" (").append(linkBuilder.toString()).append(")");

    if (getDocumentation() != null) {
      builder.append("<p>").append(getDocumentation()).append("</p>");
    }

    String sourceType = getContainerPsiClass(target).toString();
    String sourceTypeInJavadocFormat = removeGenerics(sourceType);
    sourceTypeInJavadocFormat += ("." + sourceType);

    StringBuilder buffer = new StringBuilder();
    DocumentationManager
        .createHyperlink(buffer, methodForDocumentationNavigation(sourceTypeInJavadocFormat),
            sourceTypeInJavadocFormat, false);
    sourceTypeInJavadocFormat = buffer.toString();
    builder.append("<p>Declared at ").append(sourceTypeInJavadocFormat).append("</p>");

    return builder.toString();
  }
}
