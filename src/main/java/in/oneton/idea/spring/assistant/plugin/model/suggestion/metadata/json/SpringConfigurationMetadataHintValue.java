package in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata.json;

import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiType;
import in.oneton.idea.spring.assistant.plugin.completion.FileType;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz.MetadataProxy;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;

import static com.intellij.codeInsight.documentation.DocumentationManager.createHyperlink;
import static in.oneton.idea.spring.assistant.plugin.insert.handler.YamlValueInsertHandler.unescapeValue;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType.ENUM;
import static in.oneton.idea.spring.assistant.plugin.util.GenericUtil.newListWithMembers;
import static in.oneton.idea.spring.assistant.plugin.util.GenericUtil.shortenedType;
import static in.oneton.idea.spring.assistant.plugin.util.PsiCustomUtil.toClassFqn;
import static in.oneton.idea.spring.assistant.plugin.util.PsiCustomUtil.toClassNonQualifiedName;
import static java.util.Objects.requireNonNull;

/**
 * Refer to https://docs.spring.io/spring-boot/docs/2.0.0.M6/reference/htmlsingle/#configuration-metadata-hints-attributes
 */
@Data
public class SpringConfigurationMetadataHintValue {
  /**
   * A valid value for the element to which the hint refers. If the type of the associated PROPERTY is an ARRAY, it can also be an ARRAY of value(s). This attribute is mandatory.
   */
  @SerializedName("value")
  private Object nameAsObjOrArray;
  @Nullable
  private String description;

  @Override
  public String toString() {
    if (nameAsObjOrArray instanceof Array) {
      StringBuilder builder = new StringBuilder("[");
      int length = Array.getLength(nameAsObjOrArray);
      for (int i = 0; i < length; i++) {
        Object arrayElement = Array.get(nameAsObjOrArray, i);
        builder.append(" ").append(arrayElement.toString());
        if (i == length - 1) {
          builder.append(" ");
        } else {
          builder.append(",");
        }
      }
      return builder.append("]").toString();
    } else if (nameAsObjOrArray instanceof Collection) {
      Collection nameAsCollection = Collection.class.cast(nameAsObjOrArray);
      StringBuilder builder = new StringBuilder("[");
      for (int i = 0; i < nameAsCollection.size(); i++) {
        Object arrayElement = Array.get(nameAsObjOrArray, i);
        builder.append(" ").append(arrayElement.toString());
        if (i == nameAsCollection.size() - 1) {
          builder.append(" ");
        } else {
          builder.append(",");
        }
      }
      return builder.append("]").toString();
    } else {
      return nameAsObjOrArray.toString();
    }
  }

  public boolean representsSingleValue() {
    return !nameAsObjOrArray.getClass().isArray() && !Collection.class.isInstance(nameAsObjOrArray);
  }

  @NotNull
  public Suggestion buildSuggestionForKey(Module module, FileType fileType,
      List<SuggestionNode> matchesRootTillParentNode, int numOfAncestors, SuggestionNode match,
      @Nullable PsiType keyType) {
    Suggestion.SuggestionBuilder builder =
        Suggestion.builder().description(description).numOfAncestors(numOfAncestors)
            .matchesTopFirst(newListWithMembers(matchesRootTillParentNode, match));

    if (keyType != null) {
      builder.shortType(toClassNonQualifiedName(keyType));
      builder.icon(ENUM.getIcon());
    }
    return builder.fileType(fileType).build();
  }

  // TODO: Fix this
  @NotNull
  public String getDocumentationForKey(Module module, String nodeNavigationPathDotDelimited,
      @Nullable MetadataProxy delegate) {
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

    if (delegate != null && delegate.getPsiType(module) != null) {
      String classFqn = toClassFqn(requireNonNull(delegate.getPsiType(module)));
      if (classFqn != null) {
        StringBuilder linkBuilder = new StringBuilder();
        createHyperlink(linkBuilder, classFqn, classFqn, false);
        builder.append(" (").append(linkBuilder.toString()).append(")");
      }
    }

    if (description != null) {
      builder.append("<p>").append(description).append("</p>");
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
  public Suggestion buildSuggestionForValue(FileType fileType,
      List<? extends SuggestionNode> matchesRootTillLeaf, @Nullable String defaultValue,
      @Nullable PsiType valueType) {
    Suggestion.SuggestionBuilder builder =
        Suggestion.builder().value(toString()).description(description).forValue(true)
            .matchesTopFirst(matchesRootTillLeaf).numOfAncestors(matchesRootTillLeaf.size());

    if (valueType != null) {
      builder.shortType(shortenedType(valueType.getCanonicalText()));
      builder.icon(ENUM.getIcon());
    }

    builder.representingDefaultValue(toString().equals(defaultValue));
    return builder.fileType(fileType).build();
  }

  // TODO: Fix this
  @NotNull
  public String getDocumentationForValue(@NotNull String nodeNavigationPathDotDelimited,
      @Nullable PsiType mapValueType) {
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

    if (mapValueType != null) {
      String className = mapValueType.getCanonicalText();
      StringBuilder linkBuilder = new StringBuilder();
      createHyperlink(linkBuilder, className, className, false);
      builder.append(" (").append(linkBuilder.toString()).append(")");
    }

    String trimmedValue = unescapeValue(toString());
    builder.append("<p>").append(trimmedValue).append("</p>");


    if (description != null) {
      builder.append("<p>").append(description).append("</p>");
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

}
