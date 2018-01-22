package in.oneton.idea.spring.assistant.plugin.model;

public abstract class AbstractSuggestionNode implements SuggestionNode {
  /**
   * Sanitised name used for lookup. `-`, `_` are removed, upper cased characters are converted to lower case
   */
  private String name;
  /**
   * Section of the group/PROPERTY name. Sole purpose of this is to split all properties into their individual part
   */
  private String originalName;
}
