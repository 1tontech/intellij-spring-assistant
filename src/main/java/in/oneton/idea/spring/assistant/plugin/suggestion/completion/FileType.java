package in.oneton.idea.spring.assistant.plugin.suggestion.completion;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.lookup.LookupElement;
import in.oneton.idea.spring.assistant.plugin.suggestion.handler.YamlKeyInsertHandler;
import in.oneton.idea.spring.assistant.plugin.suggestion.handler.YamlValueInsertHandler;

// TODO: Add properties support
public enum FileType {
  yaml, properties;

  public InsertHandler<LookupElement> newKeyInsertHandler() {
    switch (this) {
      case yaml:
        return new YamlKeyInsertHandler();
      default:
        return null;
    }
  }

  public InsertHandler<LookupElement> newValueInsertHandler() {
    switch (this) {
      case yaml:
        return new YamlValueInsertHandler();
      default:
        return null;
    }
  }

}
