package in.oneton.idea.spring.assistant.plugin.completion;

import com.intellij.openapi.module.Module;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNode;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public interface SuggestionDocumentationHelper {

  SuggestionDocumentationHelper EXCEPTION_RAISING_INSTANCE = new SuggestionDocumentationHelper() {
    @NotNull
    @Override
    public String getOriginalName() {
      throw new IllegalAccessError("Should never be called");
    }

    @NotNull
    @Override
    public Suggestion buildSuggestion(Module module, FileType fileType,
        List<SuggestionNode> matchesRootTillMe, int numOfAncestors) {
      throw new IllegalAccessError("Should never be called");
    }

    @Override
    public boolean supportsDocumentation() {
      throw new IllegalAccessError("Should never be called");
    }

    @NotNull
    @Override
    public String getDocumentationForKey(Module module, String nodeNavigationPathDotDelimited) {
      throw new IllegalAccessError("Should never be called");
    }
  };

  @Nullable
  String getOriginalName();

  @NotNull
  Suggestion buildSuggestion(Module module, FileType fileType,
      List<SuggestionNode> matchesRootTillMe, int numOfAncestors);

  /**
   * @return false if an intermediate node (neither group, nor property, nor class). true otherwise
   */
  boolean supportsDocumentation();

  @NotNull
  String getDocumentationForKey(Module module, String nodeNavigationPathDotDelimited);
}
