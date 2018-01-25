package in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata.json;

import com.google.gson.annotations.Expose;
import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.openapi.diagnostic.Logger;
import in.oneton.idea.spring.assistant.plugin.SuggestionDocumentationHelper;
import in.oneton.idea.spring.assistant.plugin.model.ValueType;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata.MetadataNonPropertySuggestionNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static in.oneton.idea.spring.assistant.plugin.Util.dotDelimitedOriginalNames;
import static in.oneton.idea.spring.assistant.plugin.Util.methodForDocumentationNavigation;
import static in.oneton.idea.spring.assistant.plugin.Util.newListWithMembers;
import static in.oneton.idea.spring.assistant.plugin.Util.removeGenerics;
import static in.oneton.idea.spring.assistant.plugin.Util.shortenedType;
import static in.oneton.idea.spring.assistant.plugin.Util.typeForDocumentationNavigation;
import static in.oneton.idea.spring.assistant.plugin.insert.handler.YamlValueInsertHandler.unescapeValue;
import static in.oneton.idea.spring.assistant.plugin.model.ValueType.ARRAY;
import static in.oneton.idea.spring.assistant.plugin.model.ValueType.BOOLEAN;
import static in.oneton.idea.spring.assistant.plugin.model.ValueType.ENUM;
import static in.oneton.idea.spring.assistant.plugin.model.ValueType.parse;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata.json.SpringConfigurationMetadataDeprecationLevel.error;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata.json.SpringConfigurationMetadataDeprecationLevel.warning;
import static java.util.Arrays.stream;
import static java.util.Comparator.comparing;
import static java.util.Objects.compare;
import static java.util.stream.Collectors.toSet;

/**
 * Refer to https://docs.spring.io/spring-boot/docs/2.0.0.M6/reference/htmlsingle/#configuration-metadata-property-attributes
 */
@Data
@EqualsAndHashCode(of = "name")
public class SpringConfigurationMetadataProperty
    implements Comparable<SpringConfigurationMetadataProperty>, SuggestionDocumentationHelper {

  private static final Logger log = Logger.getInstance(SpringConfigurationMetadataProperty.class);

  /**
   * The full name of the PROPERTY. Names are in lower-case period-separated form (for example, server.servlet.path). This attribute is mandatory.
   */
  private String name;
  @Nullable
  private String type;
  @Nullable
  private String description;
  /**
   * The class name of the source that contributed this PROPERTY. For example, if the PROPERTY were from a class annotated with @ConfigurationProperties, this attribute would contain the fully qualified name of that class. If the source type is unknown, it may be omitted.
   */
  @Nullable
  private String sourceType;
  /**
   * Specify whether the PROPERTY is deprecated. If the field is not deprecated or if that information is not known, it may be omitted. The next table offers more detail about the springConfigurationMetadataDeprecation attribute.
   */
  @Nullable
  private SpringConfigurationMetadataDeprecation deprecation;
  /**
   * The default value, which is used if the PROPERTY is not specified. If the type of the PROPERTY is an ARRAY, it can be an ARRAY of value(s). If the default value is unknown, it may be omitted.
   */
  @Nullable
  private Object defaultValue;

  /**
   * Represents either the only hint associated (or) key specific hint when the property represents a map
   */
  @Nullable
  @Expose(deserialize = false)
  private SpringConfigurationMetadataHint genericOrKeyHint;

  /**
   * If the property of type map, the property can have both keys & values. This hint represents value
   */
  @Nullable
  @Expose(deserialize = false)
  private SpringConfigurationMetadataHint valueHint;

  @Override
  public int compareTo(@NotNull SpringConfigurationMetadataProperty o) {
    return compare(this, o, comparing(thiz -> thiz.name));
  }

  @NotNull
  @Override
  public Suggestion buildSuggestion(String ancestralKeysDotDelimited,
      List<SuggestionNode> matchesRootTillParentNode, SuggestionNode currentNode) {
    Suggestion.SuggestionBuilder builder =
        Suggestion.builder().icon(currentNode.getType().getIcon())
            .value(dotDelimitedOriginalNames(matchesRootTillParentNode, currentNode))
            .description(this.description).shortType(shortenedType(this.type))
            .defaultValue(defaultValueAsStr(defaultValue))
            .ancestralKeysDotDelimited(ancestralKeysDotDelimited)
            .matchesTopFirst(newListWithMembers(matchesRootTillParentNode, currentNode));
    if (deprecation != null) {
      builder.deprecationLevel(deprecation.getLevel() != null ? deprecation.getLevel() : warning);
    }
    return builder.build();
  }

  public String getDocumentationForKey(String nodeNavigationPathDotDelimited) {
    // Format for the documentation is as follows
    /*
     * <p><b>a.b.c</b> ({@link com.acme.Generic}<{@link com.acme.Class1}, {@link com.acme.Class2}>)</p>
     * <p><em>Default Value</em> default value</p>
     * <p>Long description</p>
     * or of this type
     * <p><b>Type</b> {@link com.acme.Array}[]</p>
     * <p><b>Declared at</b>{@link com.acme.GenericRemovedClass#method}></p> <-- only for groups with method info
     * <b>WARNING:</b>
     * @deprecated Due to something something. Replaced by <b>c.d.e</b>
     */
    StringBuilder builder =
        new StringBuilder().append("<b>").append(nodeNavigationPathDotDelimited).append("</b>");

    String typeInJavadocFormat = null;
    if (type != null) {
      StringBuilder buffer = new StringBuilder();
      DocumentationManager
          .createHyperlink(buffer, typeForDocumentationNavigation(type), type, false);
      typeInJavadocFormat = buffer.toString();

      builder.append(" (").append(typeInJavadocFormat).append(")");
    }

    if (description != null) {
      builder.append("<p>").append(description).append("</p>");
    }

    if (defaultValue != null) {
      builder.append("<p><em>Default value: </em>").append(defaultValueAsStr(defaultValue))
          .append("</p>");
    }

    if (sourceType != null) {
      String sourceTypeInJavadocFormat = removeGenerics(sourceType);

      // lets show declaration point only if does not match the type
      if (typeInJavadocFormat == null || !sourceTypeInJavadocFormat.equals(typeInJavadocFormat)) {
        StringBuilder buffer = new StringBuilder();
        DocumentationManager
            .createHyperlink(buffer, methodForDocumentationNavigation(sourceTypeInJavadocFormat),
                sourceTypeInJavadocFormat, false);
        sourceTypeInJavadocFormat = buffer.toString();

        builder.append("<p>Declared at ").append(sourceTypeInJavadocFormat).append("</p>");
      }
    }

    if (isDeprecated()) {
      builder.append("<p><b>").append((deprecation == null || error != deprecation.getLevel()) ?
          "WARNING: PROPERTY IS DEPRECATED" :
          "ERROR: DO NOT USE THIS PROPERTY AS IT IS COMPLETELY UNSUPPORTED").append("</b></p>");

      if (deprecation != null && deprecation.getReason() != null) {
        builder.append("@deprecated Reason: ").append(deprecation.getReason());
      }

      if (deprecation != null && deprecation.getReplacement() != null) {
        builder.append("<p>Replaced by property <b>").append(deprecation.getReplacement())
            .append("</b></p>");
      }
    }

    return builder.toString();
  }

  @Nullable
  public Set<Suggestion> getValueSuggestions(MetadataNonPropertySuggestionNode propertyNode) {
    Set<Suggestion> suggestions = null;

    if (hint != null && hint.getValues() != null) {
      suggestions = stream(hint.getValues())
          .filter(v -> !(v.getValue() instanceof Array) || !(v.getValue() instanceof Collection))
          .map(choice -> Suggestion.builder().suggestion(choice.toString())
              .description(choice.getDescription()).ref(propertyNode).forValue(true).build())
          .collect(toSet());
    }

    ValueType valueType = parse(type);

    Set<Suggestion> additionalSuggestions = null;

    if (valueType == ARRAY) {
      if (defaultValue instanceof Array) {
        Object[] choices = Object[].class.cast(this.defaultValue);
        additionalSuggestions = stream(choices).map(
            choice -> Suggestion.builder().suggestion(choice.toString()).ref(propertyNode)
                .forValue(true).build()).collect(toSet());
      } else if (defaultValue instanceof Collection) {
        @SuppressWarnings("unchecked")
        Stream<Suggestion> choiceStream = Collection.class.cast(defaultValue).stream().map(
            choice -> Suggestion.builder().suggestion(choice.toString()).ref(propertyNode)
                .forValue(true).build());
        additionalSuggestions = choiceStream.collect(toSet());
      }
    } else if (valueType == BOOLEAN) {
      additionalSuggestions = new HashSet<>();
      additionalSuggestions
          .add(Suggestion.builder().suggestion("true").ref(propertyNode).forValue(true).build());
      additionalSuggestions
          .add(Suggestion.builder().suggestion("false").ref(propertyNode).forValue(true).build());
    } else if (valueType == ENUM) {
      Class<?> enumClazz = null;
      try {
        enumClazz = classLoader.loadClass(type);
      } catch (ClassNotFoundException e) {
        // This exception should not happen as we already know
        log.error("Enum " + type + " could not be loaded. This path should never be hit", e);
      }
      additionalSuggestions = stream(enumClazz.getEnumConstants()).map(
          v -> Suggestion.builder().suggestion(v.toString()).ref(propertyNode).forValue(true)
              .build()).collect(toSet());
    }

    if (additionalSuggestions != null) {
      if (suggestions != null) {
        suggestions.addAll(additionalSuggestions);
      } else {
        suggestions = additionalSuggestions;
      }
    }

    if (suggestions != null && defaultValue != null && valueType != ARRAY) {
      suggestions.stream().filter(v -> v.getSuggestion().equals(defaultValueAsStr(defaultValue)))
          .findFirst().ifPresent(v -> v.setRepresentingDefaultValue(true));
    }

    return suggestions;
  }

  public String getDocumentationForValue(String nodeNavigationPathDotDelimited, String value) {
    StringBuilder builder =
        new StringBuilder().append("<b>").append(nodeNavigationPathDotDelimited).append("</b>");

    if (type != null) {
      StringBuilder buffer = new StringBuilder();
      DocumentationManager
          .createHyperlink(buffer, typeForDocumentationNavigation(type), type, false);
      String typeInJavadocFormat = buffer.toString();

      builder.append(" (").append(typeInJavadocFormat).append(")");
    }

    String trimmedValue = unescapeValue(value);
    builder.append("<p>").append(trimmedValue).append("</p>");

    Set<Suggestion> choices = getValueSuggestions(this);
    if (choices != null) {
      choices.stream().filter(choice -> choice.getSuggestion().equals(trimmedValue)).findFirst()
          .ifPresent(suggestion -> {
            if (suggestion.getDescription() != null) {
              builder.append("<p>").append(suggestion.getDescription()).append("</p>");
            }
          });
    }

    return builder.toString();
  }

  public boolean isDeprecated() {
    return deprecation != null;
  }

  private String defaultValueAsStr(@Nullable Object defaultValue) {
    if (defaultValue != null && !(defaultValue instanceof Array)
        && !(defaultValue instanceof Collection)) {
      if (type != null && defaultValue instanceof Double) {
        // if defaultValue is a number, its being parsed by gson as double & we will see an incorrect fraction when we take toString()
        switch (type) {
          case "java.lang.Integer":
            return Integer.toString(((Double) defaultValue).intValue());
          case "java.lang.Byte":
            return Byte.toString(((Double) defaultValue).byteValue());
          case "java.lang.Short":
            return Short.toString(((Double) defaultValue).shortValue());
        }
      }
      return defaultValue.toString();
    }
    return null;
  }

  public boolean hasNonObjectDefaultValue() {
    return defaultValue != null && defaultValue instanceof String;
  }

  public boolean hasValueHint() {
    return valueHint != null;
  }

  public boolean doesGenericHintRepresentsArray() {
    return genericOrKeyHint != null && !hasValueHint() && genericOrKeyHint.getValues() != null
        && genericOrKeyHint.getValues().length > 0;
  }

}
