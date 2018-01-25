package in.oneton.idea.spring.assistant.plugin.model.suggestion;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static in.oneton.idea.spring.assistant.plugin.Util.getCodeStyleIntent;

public enum SuggestionNodeType {
  BOOLEAN, BYTE, SHORT, INTEGER, LONG, FLOAT, DOUBLE, CHARACTER, STRING, ENUM, ARRAY, COLLECTION, MAP, KNOWN_CLASS, UNKNOWN_CLASS, UNDEFINED;

  public static final String CARET = "<caret>";

  public boolean isWholeNumber() {
    return this == BYTE || this == SHORT || this == INTEGER || this == LONG;
  }

  public boolean isDecimal() {
    return this == FLOAT || this == DOUBLE;
  }

  public boolean representsLeaf() {
    return representsPrimitiveOrString() || representsEnum() || this == UNKNOWN_CLASS
        || this == UNDEFINED;
  }

  private boolean representsEnum() {
    return this == ENUM;
  }

  private boolean representsPrimitiveOrString() {
    return this == BOOLEAN || isWholeNumber() || isDecimal() || this == CHARACTER || this == STRING;
  }

  public boolean potentiallyLeaf() {
    return representsLeaf() || representsArrayOrCollection();
  }

  public boolean representsArrayOrCollection() {
    return this == ARRAY || this == COLLECTION;
  }

  public Icon getIcon() {
    if (representsPrimitiveOrString()) {
      return AllIcons.Nodes.Property;
    } else if (representsEnum()) {
      return AllIcons.Nodes.Enum;
    } else if (representsArrayOrCollection()) {
      return AllIcons.Json.Array;
    } else {
      return AllIcons.Json.Object;
    }
  }

  @NotNull
  public String getPlaceholderSufixForKey(InsertionContext insertionContext,
      String existingIndentation) {
    if (representsLeaf()) {
      return ": " + CARET;
    } else if (representsArrayOrCollection()) {
      return ":\n" + existingIndentation + getCodeStyleIntent(insertionContext) + "- " + CARET;
    } else { // map or class
      return ":\n" + existingIndentation + getCodeStyleIntent(insertionContext) + CARET;
    }
  }

}
