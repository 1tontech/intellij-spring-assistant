package in.oneton.idea.spring.assistant.plugin.suggestion.metadata.json;

import com.google.gson.annotations.SerializedName;
import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTypesUtil;
import in.oneton.idea.spring.assistant.plugin.misc.GenericUtil;
import in.oneton.idea.spring.assistant.plugin.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType;
import in.oneton.idea.spring.assistant.plugin.suggestion.clazz.MetadataProxy;
import in.oneton.idea.spring.assistant.plugin.suggestion.completion.FileType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.intellij.lang.documentation.DocumentationMarkup.CONTENT_END;
import static com.intellij.lang.documentation.DocumentationMarkup.CONTENT_START;
import static com.intellij.lang.documentation.DocumentationMarkup.DEFINITION_END;
import static com.intellij.lang.documentation.DocumentationMarkup.DEFINITION_START;
import static com.intellij.lang.documentation.DocumentationMarkup.SECTIONS_END;
import static com.intellij.lang.documentation.DocumentationMarkup.SECTIONS_START;
import static com.intellij.lang.documentation.DocumentationMarkup.SECTION_END;
import static com.intellij.lang.documentation.DocumentationMarkup.SECTION_HEADER_START;
import static com.intellij.lang.documentation.DocumentationMarkup.SECTION_SEPARATOR;
import static in.oneton.idea.spring.assistant.plugin.misc.GenericUtil.methodForDocumentationNavigation;
import static in.oneton.idea.spring.assistant.plugin.misc.GenericUtil.removeGenerics;
import static in.oneton.idea.spring.assistant.plugin.misc.GenericUtil.shortenedType;
import static in.oneton.idea.spring.assistant.plugin.misc.GenericUtil.updateClassNameAsJavadocHtml;
import static in.oneton.idea.spring.assistant.plugin.misc.PsiCustomUtil.safeGetValidType;
import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNodeType.UNKNOWN_CLASS;
import static in.oneton.idea.spring.assistant.plugin.suggestion.clazz.ClassSuggestionNodeFactory.newMetadataProxy;

/**
 * Refer to https://docs.spring.io/spring-boot/docs/2.0.0/reference/htmlsingle/#configuration-metadata-group-attributes
 */
@Data
@EqualsAndHashCode(of = "name")
public class SpringConfigurationMetadataGroup {

  private String name;
  @Nullable
  @SerializedName("type")
  private String className;
  @Nullable
  private String description;
  @Nullable
  private String sourceType;
  @Nullable
  private String sourceMethod;
  @NotNull
  private SuggestionNodeType nodeType = SuggestionNodeType.UNDEFINED;
  /**
   * Responsible for all suggestion queries that needs to be matched against a class
   */
  @Nullable
  private MetadataProxy delegate;

  private boolean delegateCreationAttempted;

  public String getDocumentation(Module module, String nodeNavigationPathDotDelimited) {
    StringBuilder doc = new StringBuilder();

    //Unfortunately, even though there is a 'description' field for the group metadata, `spring boot configuration processor` will never fill it.
    //So, it is better to use group type's document instead.
    if (description == null && className != null && module != null) {
      MetadataProxy delegate = getDelegate(module);
      if (delegate != null) {
        @Nullable PsiClass groupType = PsiTypesUtil.getPsiClass(delegate.getPsiType(module));
        if (groupType != null) {
          doc.append(DocumentationManager.getProviderFromElement(groupType).generateDoc(groupType, null));
        }
      }
    }
    if (doc.length() == 0) {
      // Otherwise, format for the documentation is as follows
      /*
       * {@link com.acme.Generic}<{@link com.acme.Class1}, {@link com.acme.Class2}>
       * a.b.c
       * ---
       * Long description
       */
      doc.append(DEFINITION_START);
      if (className != null) {
        int l = updateClassNameAsJavadocHtml(doc, className);
        if (l > 20) {
          doc.append('\n');
        } else {
          doc.append(' ');
        }
      }
      doc.append(nodeNavigationPathDotDelimited)
         .append(DEFINITION_END);
      if (description != null) {
        doc.append(CONTENT_START).append(description).append(CONTENT_END);
      }
    }

    // Append "Declared at" section as follows:
    // Declared at: {@link com.acme.GenericRemovedClass#method}> <-- only for groups with method info
    if (sourceType != null) {
      String sourceTypeInJavadocFormat = removeGenerics(sourceType);
      if (sourceMethod != null) {
        sourceTypeInJavadocFormat += "." + sourceMethod;
      }

      // lets show declaration point only if does not match the type
      if (!sourceTypeInJavadocFormat.equals(removeGenerics(className))) {
        StringBuilder buffer = new StringBuilder();
        DocumentationManager.createHyperlink(
            buffer,
            methodForDocumentationNavigation(sourceTypeInJavadocFormat),
            sourceTypeInJavadocFormat,
            false
        );
        sourceTypeInJavadocFormat = buffer.toString();
        doc.append(SECTIONS_START)
           .append(SECTION_HEADER_START)
           .append("<span style='white-space:nowrap'>Declared at:</span>")
           .append(SECTION_SEPARATOR)
           .append(sourceTypeInJavadocFormat)
           .append(SECTION_END)
           .append(SECTIONS_END);
      }
    }

    return doc.toString();
  }

  public Suggestion newSuggestion(FileType fileType, List<SuggestionNode> matchesRootTillMe,
      int numOfAncestors) {
    return Suggestion.builder().suggestionToDisplay(
                         GenericUtil.dotDelimitedOriginalNames(matchesRootTillMe, numOfAncestors))
                     .description(description).shortType(shortenedType(className)).numOfAncestors(numOfAncestors)
                     .matchesTopFirst(matchesRootTillMe).icon(nodeType.getIcon()).fileType(fileType).build();
  }


  public void refreshDelegate(Module module) {
    if (className != null) {
      // Lets update the delegate information only if anything has changed from last time we saw this
      PsiType type = getPsiType(module);
      boolean validTypeExists = type != null;
      // In the previous refresh, class could not be found. Now class is available in the classpath
      if (validTypeExists) {
        if (delegate == null) {
          delegate = newMetadataProxy(module, type);
          // lets force the nodeType to recalculated
          nodeType = null;
        }
      }
      // In the previous refresh, class was available in classpath. Now it is no longer available
      if (!validTypeExists && delegate != null) {
        delegate = null;
        nodeType = UNKNOWN_CLASS;
      }
    }
    delegateCreationAttempted = true;
  }


  private PsiType getPsiType(Module module) {
    if (className != null) {
      return safeGetValidType(module, className);
    }
    return null;
  }

  @Nullable
  public MetadataProxy getDelegate(Module module) {
    if (!delegateCreationAttempted) {
      refreshDelegate(module);
    }
    return delegate;
  }
}
