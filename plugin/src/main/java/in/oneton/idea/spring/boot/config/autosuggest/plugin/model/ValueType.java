package in.oneton.idea.spring.boot.config.autosuggest.plugin.model;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static in.oneton.idea.spring.boot.config.autosuggest.plugin.Util.getCodeStyleIntent;

public enum ValueType {
  BOOLEAN, INTEGER, NUMBER, CHARACTER, STRING, ENUM, ARRAY, OBJECT, UNKNOWN;

  public static final String CARET = "<caret>";

  private static final Pattern PACKAGE_REMOVAL_PATTERN =
      Pattern.compile("[a-zA-Z_][a-zA-Z_0-9]*\\.");
  private static final Pattern GENERIC_SECTION_REMOVAL_PATTERN = Pattern.compile("<[^>]+>");
  @SuppressWarnings("unused")
  private static final Pattern CLASSNAME_MATCH_PATTERN = Pattern.compile("([a-zA-Z_][a-zA-Z_0-9]*(?:\\.[a-zA-Z_][a-zA-Z_0-9]*)*)");

  public static ValueType parse(String type, ClassLoader classLoader) {
    if (type == null) {
      return UNKNOWN;
    }

    try {
      if (type.endsWith("[]")) {
        return ARRAY;
      }

      String genericStringRemoved = removeGenerics(type);
      Class<?> clazz = Class.forName(genericStringRemoved, false, classLoader);
      if (Boolean.class.isAssignableFrom(clazz)) {
        return BOOLEAN;
      } else if (Byte.class.isAssignableFrom(clazz) || Short.class.isAssignableFrom(clazz)
          || Integer.class.isAssignableFrom(clazz) || Long.class.isAssignableFrom(clazz)) {
        return INTEGER;
      } else if (Number.class.isAssignableFrom(clazz)) {
        return NUMBER;
      } else if (Character.class.isAssignableFrom(clazz)) {
        return CHARACTER;
      } else if (String.class.isAssignableFrom(clazz) || char[].class.isAssignableFrom(clazz)) {
        return STRING;
      } else if (clazz.isEnum()) {
        return ENUM;
      } else if (clazz.isArray() || Collection.class.isAssignableFrom(clazz)) {
        return ARRAY;
      } else if (Map.class.isAssignableFrom(clazz)) {
        return OBJECT;
      } else {
        return OBJECT;
      }
    } catch (ClassNotFoundException e) {
      return UNKNOWN;
    }
  }

  public static String removeGenerics(String type) {
    Matcher matcher = GENERIC_SECTION_REMOVAL_PATTERN.matcher(type);
    if (matcher.find()) {
      return matcher.replaceAll("");
    }
    return type;
  }

  public static String shortenedType(String type) {
    if (type == null) {
      return null;
    }
    Matcher matcher = PACKAGE_REMOVAL_PATTERN.matcher(type);
    if (matcher.find()) {
      return matcher.replaceAll("");
    }
    return type;
  }

  public Icon getIcon() {
    switch (this) {
      case BOOLEAN:
      case INTEGER:
      case NUMBER:
      case CHARACTER:
      case STRING:
        return AllIcons.Nodes.Property;
      case ENUM:
        return AllIcons.Nodes.Enum;
      case ARRAY:
        return AllIcons.Json.Array;
      case OBJECT:
      default:
        return AllIcons.Json.Object;
    }
  }

  @NotNull
  public String getPlaceholderSufix(InsertionContext insertionContext, String existingIndentation) {
    switch (this) {
      case BOOLEAN:
      case INTEGER:
      case NUMBER:
      case CHARACTER:
      case STRING:
      case ENUM:
      default:
        return ": " + CARET;
      case ARRAY:
        return getPlaceholderSufixForArray(insertionContext, existingIndentation);
      case OBJECT:
        return getPlaceholderSufixForObject(insertionContext, existingIndentation);
    }
  }

  @NotNull
  public String getPlaceholderSufixForArray(InsertionContext insertionContext,
      String existingIndentation) {
    final String additionalIndent = getCodeStyleIntent(insertionContext);
    return ":\n" + existingIndentation + additionalIndent + "- " + CARET;
  }

  @NotNull
  public String getPlaceholderSufixForObject(InsertionContext insertionContext,
      String existingIndentation) {
    final String additionalIndent = getCodeStyleIntent(insertionContext);
    return ":\n" + existingIndentation + additionalIndent + CARET;
  }

}
