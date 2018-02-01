package in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata.json;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiType;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz.MapClassMetadataProxy;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz.MetadataProxy;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz.MetadataProxyInvokerWithReturnValue;
import in.oneton.idea.spring.assistant.plugin.util.PsiCustomUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType.ENUM;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType.MAP;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType.UNDEFINED;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType.UNKNOWN_CLASS;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType.VALUES;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.clazz.ClassSuggestionNodeFactory.newMetadataProxy;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata.json.SpringConfigurationMetadataDeprecationLevel.error;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.metadata.json.SpringConfigurationMetadataDeprecationLevel.warning;
import static in.oneton.idea.spring.assistant.plugin.util.GenericUtil.dotDelimitedOriginalNames;
import static in.oneton.idea.spring.assistant.plugin.util.GenericUtil.methodForDocumentationNavigation;
import static in.oneton.idea.spring.assistant.plugin.util.GenericUtil.removeGenerics;
import static in.oneton.idea.spring.assistant.plugin.util.GenericUtil.shortenedType;
import static in.oneton.idea.spring.assistant.plugin.util.GenericUtil.typeForDocumentationNavigation;
import static in.oneton.idea.spring.assistant.plugin.util.PsiCustomUtil.safeGetValidType;
import static java.util.Comparator.comparing;
import static java.util.Objects.compare;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;

/**
 * Refer to https://docs.spring.io/spring-boot/docs/2.0.0.M6/reference/htmlsingle/#configuration-metadata-property-attributes
 */
@EqualsAndHashCode(of = "name")
public class SpringConfigurationMetadataProperty
    implements Comparable<SpringConfigurationMetadataProperty>, GsonPostProcessable {

  /**
   * The full name of the PROPERTY. Names are in lower-case period-separated form (for example, server.servlet.path). This attribute is mandatory.
   */
  @Setter
  @Getter
  private String name;
  @Nullable
  @Setter
  @SerializedName("type")
  private String className;
  @Nullable
  @Setter
  private String description;
  /**
   * The class name of the source that contributed this PROPERTY. For example, if the PROPERTY were from a class annotated with @ConfigurationProperties, this attribute would contain the fully qualified name of that class. If the source type is unknown, it may be omitted.
   */
  @Nullable
  @Setter
  private String sourceType;
  /**
   * Specify whether the PROPERTY is deprecated. If the field is not deprecated or if that information is not known, it may be omitted. The next table offers more detail about the springConfigurationMetadataDeprecation attribute.
   */
  @Nullable
  @Setter
  private SpringConfigurationMetadataDeprecation deprecation;
  /**
   * The default value, which is used if the PROPERTY is not specified. If the type of the PROPERTY is an ARRAY, it can be an ARRAY of value(s). If the default value is unknown, it may be omitted.
   */
  @Nullable
  @Setter
  private Object defaultValue;

  /**
   * Represents either the only hint associated (or) key specific hint when the property represents a map
   */
  @Nullable
  @Setter
  @Expose(deserialize = false)
  private SpringConfigurationMetadataHint genericOrKeyHint;

  /**
   * If the property of type map, the property can have both keys & values. This hint represents value
   */
  @Nullable
  @Setter
  @Expose(deserialize = false)
  private SpringConfigurationMetadataHint valueHint;

  /**
   * Responsible for all suggestion queries that needs to be matched against a class
   */
  @Nullable
  private MetadataProxy delegate;

  @Nullable
  private SuggestionNodeType nodeType;
  private boolean delegateCreationAttempted;

  @Override
  public void doOnGsonDeserialization() {
    if (isMapWithPredefinedKeys() || isMapWithPredefinedValues()) {
      nodeType = MAP;
    } else if (genericOrKeyHint != null) {
      nodeType = VALUES;
    }
  }

  @Nullable
  public List<SuggestionNode> findChildDeepestKeyMatch(Module module,
      List<SuggestionNode> matchesRootTillParentNode, String[] pathSegments,
      int pathSegmentStartIndex) {
    if (!isLeaf(module)) {
      if (isMapWithPredefinedKeys()) { // map
        assert genericOrKeyHint != null;
        String pathSegment = pathSegments[pathSegmentStartIndex];
        SpringConfigurationMetadataHintValue valueHint =
            genericOrKeyHint.findHintValueWithName(pathSegment);
        if (valueHint != null) {
          matchesRootTillParentNode.add(new HintAwareSuggestionNode(valueHint, true));
          boolean lastPathSegment = pathSegmentStartIndex == pathSegments.length - 1;
          if (lastPathSegment) {
            return matchesRootTillParentNode;
          } else {
            if (!isMapWithPredefinedValues()) {
              return doWithDelegateOrReturnNull(module, delegate -> delegate
                  .findDeepestSuggestionNode(module, matchesRootTillParentNode, pathSegments,
                      pathSegmentStartIndex));
            }
          }
        }
      } else {
        return doWithDelegateOrReturnNull(module, delegate -> delegate
            .findDeepestSuggestionNode(module, matchesRootTillParentNode, pathSegments,
                pathSegmentStartIndex));
      }
    }
    return null;
  }

  @Nullable
  public SortedSet<Suggestion> findChildKeySuggestionsForQueryPrefix(Module module,
      @Nullable String ancestralKeysDotDelimited, List<SuggestionNode> matchesRootTillMe,
      String[] querySegmentPrefixes, int querySegmentPrefixStartIndex) {
    boolean lastPathSegment = querySegmentPrefixStartIndex == querySegmentPrefixes.length - 1;
    if (lastPathSegment && !isLeaf(module)) {
      if (isMapWithPredefinedKeys()) { // map
        assert genericOrKeyHint != null;
        String querySegment = querySegmentPrefixes[querySegmentPrefixStartIndex];
        return genericOrKeyHint.findHintValuesWithPrefix(querySegment).stream().map(hintValue -> {
          HintAwareSuggestionNode suggestionNode = new HintAwareSuggestionNode(hintValue, true);
          return hintValue
              .buildSuggestionForKey(module, ancestralKeysDotDelimited, matchesRootTillMe,
                  suggestionNode, getMapKeyType(module));
        }).collect(toCollection(TreeSet::new));
      } else {
        return doWithDelegateOrReturnNull(module, delegate -> delegate
            .findKeySuggestionsForQueryPrefix(module, ancestralKeysDotDelimited, matchesRootTillMe,
                querySegmentPrefixes, querySegmentPrefixStartIndex));
      }
    }
    return null;
  }

  @NotNull
  public Suggestion buildKeySuggestion(Module module, String ancestralKeysDotDelimited,
      List<SuggestionNode> matchesRootTillMe) {
    Suggestion.SuggestionBuilder builder =
        Suggestion.builder().icon(getSuggestionNodeType(module).getIcon())
            .pathOrValue(dotDelimitedOriginalNames(module, matchesRootTillMe))
            .description(description).shortType(shortenedType(className))
            .defaultValue(getDefaultValueAsStr())
            .ancestralKeysDotDelimited(ancestralKeysDotDelimited)
            .matchesTopFirst(matchesRootTillMe);
    if (deprecation != null) {
      builder.deprecationLevel(deprecation.getLevel() != null ? deprecation.getLevel() : warning);
    }
    return builder.build();
  }

  @NotNull
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
    if (className != null) {
      StringBuilder buffer = new StringBuilder();
      DocumentationManager
          .createHyperlink(buffer, typeForDocumentationNavigation(className), className, false);
      typeInJavadocFormat = buffer.toString();

      builder.append(" (").append(typeInJavadocFormat).append(")");
    }

    if (description != null) {
      builder.append("<p>").append(description).append("</p>");
    }

    if (defaultValue != null) {
      builder.append("<p><em>Default value: </em>").append(getDefaultValueAsStr()).append("</p>");
    }

    if (sourceType != null) {
      String sourceTypeInJavadocFormat = removeGenerics(sourceType);

      // lets show declaration point only if does not match the type
      if (typeInJavadocFormat == null || !sourceTypeInJavadocFormat.equals(typeInJavadocFormat)) {
        StringBuilder buffer = new StringBuilder();
        DocumentationManager.createHyperlink(buffer, methodForDocumentationNavigation(sourceTypeInJavadocFormat),
            sourceTypeInJavadocFormat, false);
        sourceTypeInJavadocFormat = buffer.toString();

        builder.append("<p>Declared at ").append(sourceTypeInJavadocFormat).append("</p>");
      }
    }

    if (deprecation != null) {
      builder.append("<p><b>").append(isDeprecatedError() ?
          "ERROR: DO NOT USE THIS PROPERTY AS IT IS COMPLETELY UNSUPPORTED" :
          "WARNING: PROPERTY IS DEPRECATED").append("</b></p>");

      if (deprecation.getReason() != null) {
        builder.append("@deprecated Reason: ").append(deprecation.getReason());
      }

      if (deprecation.getReplacement() != null) {
        builder.append("<p>Replaced by property <b>").append(deprecation.getReplacement()).append("</b></p>");
      }
    }

    return builder.toString();
  }

  public boolean isLeaf(Module module) {
    return !isMapWithPredefinedKeys() && !isMapWithPredefinedValues() && !isLeafWithKnownValues()
        && getSuggestionNodeType(module).representsLeaf();
  }

  @NotNull
  public SuggestionNodeType getSuggestionNodeType(Module module) {
    if (nodeType == null) {
      if (className != null) {
        refreshDelegate(module);

        if (delegate != null) {
          nodeType = delegate.getSuggestionNodeType(module);
        }

        if (nodeType == null) {
          nodeType = UNKNOWN_CLASS;
        }
      } else {
        nodeType = UNDEFINED;
      }
    }

    return nodeType;
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
          // TODO: uncomment below code. but fix infinite loops as objects can refer to each other. Refer to delegate.refreshMetadata on how to fix
          //        } else {
          //          delegate.refreshMetadata(module);
        }
      }
      // In the previous refresh, class was available in classpath. Now it is no longer available
      if (!validTypeExists && delegate != null) {
        delegate = null;
      }
    }
    delegateCreationAttempted = true;
  }

  @Override
  public int compareTo(@NotNull SpringConfigurationMetadataProperty o) {
    return compare(this, o, comparing(thiz -> thiz.name));
  }

  @NotNull
  public String getOriginalName() {
    return name;
  }

  /**
   * @return true if the property is deprecated & level is error, false otherwise
   */
  public boolean isDeprecatedError() {
    return deprecation != null && deprecation.getLevel() == error;
  }

  public SortedSet<Suggestion> findSuggestionsForValues(Module module,
      List<SuggestionNode> matchesRootTillContainerProperty, String prefix) {
    assert isLeaf(module);
    if (nodeType == VALUES) {
      Collection<SpringConfigurationMetadataHintValue> matches =
          requireNonNull(genericOrKeyHint).findHintValuesWithPrefix(prefix);
      if (matches != null && matches.size() != 0) {
        return matches.stream().map(match -> match
            .buildSuggestionForKey(module, null, matchesRootTillContainerProperty,
                new HintAwareSuggestionNode(match, false), getPsiType(module)))
            .collect(toCollection(TreeSet::new));
      }
    } else {
      return doWithDelegateOrReturnNull(module, delegate -> delegate
          .findValueSuggestionsForPrefix(module, matchesRootTillContainerProperty, prefix));
    }

    return null;
  }

  private PsiType getPsiType(Module module) {
    if (className != null) {
      return safeGetValidType(module, className);
    }
    return null;
  }

  private boolean isMapWithPredefinedValues() {
    return valueHint != null && valueHint.representsValueOfMap();
  }

  private boolean isMapWithPredefinedKeys() {
    return isLeafWithKnownValues() && requireNonNull(genericOrKeyHint).representsKeyOfMap();
  }

  private boolean isLeafWithKnownValues() {
    return genericOrKeyHint != null;
  }

  private <T> T doWithDelegateOrReturnNull(Module module,
      MetadataProxyInvokerWithReturnValue<T> invoker) {
    MetadataProxy delegate = getDelegate(module);
    if (delegate != null) {
      return invoker.invoke(delegate);
    }
    return null;
  }

  private <T> T doWithMapDelegateOrReturnNull(Module module,
      MetadataProxyInvokerWithReturnValue<T> invoker) {
    MetadataProxy delegate = getDelegate(module);
    if (delegate != null) {
      assert delegate instanceof MapClassMetadataProxy;
      return invoker.invoke(MapClassMetadataProxy.class.cast(delegate));
    }
    return null;
  }

  private String getDefaultValueAsStr() {
    if (defaultValue != null && !(defaultValue instanceof Array)
        && !(defaultValue instanceof Collection)) {
      if (className != null && defaultValue instanceof Double) {
        // if defaultValue is a number, its being parsed by gson as double & we will see an incorrect fraction when we take toString()
        switch (className) {
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

  @Nullable
  private MetadataProxy getDelegate(Module module) {
    if (!delegateCreationAttempted) {
      refreshDelegate(module);
    }
    return delegate;
  }

  @Nullable
  private PsiType getMapKeyType(Module module) {
    SuggestionNodeType nodeType = getSuggestionNodeType(module);
    if (nodeType == MAP) {
      return doWithDelegateOrReturnNull(module, delegate -> {
        assert delegate instanceof MapClassMetadataProxy;
        return MapClassMetadataProxy.class.cast(delegate).getMapKeyType();
      });
    }
    return null;
  }

  @Nullable
  private PsiType getMapValueType(Module module) {
    SuggestionNodeType nodeType = getSuggestionNodeType(module);
    if (nodeType == MAP) {
      return doWithDelegateOrReturnNull(module, delegate -> {
        assert delegate instanceof MapClassMetadataProxy;
        return MapClassMetadataProxy.class.cast(delegate).getMapValueType();
      });
    }
    return null;
  }


  class HintAwareSuggestionNode implements SuggestionNode {
    private final SpringConfigurationMetadataHintValue target;
    private final boolean representsMap;

    /**
     * @param target        hint value
     * @param representsMap whether this node represents map or enum
     */
    HintAwareSuggestionNode(SpringConfigurationMetadataHintValue target, boolean representsMap) {
      this.target = target;
      this.representsMap = representsMap;
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
    public SortedSet<Suggestion> findKeySuggestionsForQueryPrefix(Module module,
        @Nullable String ancestralKeysDotDelimited, List<SuggestionNode> matchesRootTillMe,
        String[] querySegmentPrefixes, int querySegmentPrefixStartIndex) {
      assert representsMap;
      return doWithMapDelegateOrReturnNull(module,
          delegate -> MapClassMetadataProxy.class.cast(delegate)
              .findChildKeySuggestionForQueryPrefix(module, ancestralKeysDotDelimited,
                  matchesRootTillMe, querySegmentPrefixes, querySegmentPrefixStartIndex));
    }

    @Override
    public boolean supportsDocumentation() {
      return true;
    }

    @Override
    public String getOriginalName(Module module) {
      if (representsMap) {
        return target.toString();
      } else {
        return SpringConfigurationMetadataProperty.this.getOriginalName();
      }
    }

    @Nullable
    @Override
    public String getNameForDocumentation(Module module) {
      return getOriginalName(module);
    }

    @Nullable
    @Override
    public String getDocumentationForKey(Module module, String nodeNavigationPathDotDelimited) {
      return target
          .getDocumentationForKey(module, nodeNavigationPathDotDelimited, getDelegate(module));
    }

    @Nullable
    @Override
    public SortedSet<Suggestion> findValueSuggestionsForPrefix(Module module,
        List<SuggestionNode> matchesRootTillMe, String prefix) {
      if (isMapWithPredefinedValues()) {
        assert valueHint != null;
        Collection<SpringConfigurationMetadataHintValue> matches =
            valueHint.findHintValuesWithPrefix(prefix);
        if (matches != null && matches.size() != 0) {
          return matches.stream().map(match -> match
              .buildSuggestionForValue(matchesRootTillMe, getDefaultValueAsStr(),
                  getMapValueType(module))).collect(toCollection(TreeSet::new));
        }
      } else {
        return doWithDelegateOrReturnNull(module,
            delegate -> delegate.findValueSuggestionsForPrefix(module, matchesRootTillMe, prefix));
      }
      return null;
    }

    @Nullable
    @Override
    public String getDocumentationForValue(Module module, String nodeNavigationPathDotDelimited,
        String value) {
      if (representsMap) {
        if (isMapWithPredefinedValues()) {
          assert valueHint != null;
          Collection<SpringConfigurationMetadataHintValue> matches =
              valueHint.findHintValuesWithPrefix(value);
          assert matches != null && matches.size() == 1;
          SpringConfigurationMetadataHintValue hint = matches.iterator().next();
          return hint
              .getDocumentationForValue(nodeNavigationPathDotDelimited, getMapValueType(module));
        } else {
          return doWithDelegateOrReturnNull(module, delegate -> delegate
              .getDocumentationForValue(module, nodeNavigationPathDotDelimited, value));
        }
      } else {
        return target
            .getDocumentationForValue(nodeNavigationPathDotDelimited, getMapValueType(module));
      }
    }

    @Override
    public boolean isLeaf(Module module) {
      if (isLeafWithKnownValues() || isMapWithPredefinedValues()) {
        return true;
      }
      // whether the node is a leaf or not depends on the value of the map that containing property points to
      return PsiCustomUtil.getSuggestionNodeType(getMapValueType(module)).representsLeaf();
    }

    @NotNull
    @Override
    public SuggestionNodeType getSuggestionNodeType(Module module) {
      if (isLeafWithKnownValues() || isMapWithPredefinedValues()) { // predefined values
        return ENUM;
      }
      return PsiCustomUtil.getSuggestionNodeType(getMapValueType(module));
    }

  }

}
