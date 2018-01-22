package in.oneton.idea.spring.assistant.plugin.model;

public enum SuggestionNodeType {
  BOOLEAN, BYTE, SHORT, INTEGER, LONG, FLOAT, DOUBLE, CHARACTER, STRING, ENUM, ARRAY, COLLECTION, MAP, KNOWN_CLASS, UNKNOWN_CLASS, UNDEFINED;

  public boolean isWholeNumber() {
    return this == BYTE || this == SHORT || this == INTEGER || this == LONG;
  }

  public boolean isDecimal() {
    return this == FLOAT || this == DOUBLE;
  }

  public boolean representsLeaf() {
    return this == BOOLEAN || isWholeNumber() || isDecimal() || this == CHARACTER || this == STRING
        || this == ENUM || this == UNKNOWN_CLASS || this == UNDEFINED;
  }

  public boolean potentiallyLeaf() {
    return representsLeaf() || this == ARRAY || this == COLLECTION;
  }
}
