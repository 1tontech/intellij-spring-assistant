package in.oneton.idea.spring.assistant.plugin.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import in.oneton.idea.spring.assistant.plugin.service.SuggestionIndexService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.codeInsight.completion.CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED;
import static com.intellij.psi.util.PsiTreeUtil.getParentOfType;

class YamlCompletionProvider extends CompletionProvider<CompletionParameters> {
  @Override
  protected void addCompletions(@NotNull final CompletionParameters completionParameters,
      final ProcessingContext processingContext, @NotNull final CompletionResultSet resultSet) {

    PsiElement element = completionParameters.getPosition();
    if (element instanceof PsiComment) {
      return;
    }

    Project project = element.getProject();
    Module module = ModuleUtil.findModuleForPsiElement(element);

    SuggestionIndexService service =
        ServiceManager.getService(project, SuggestionIndexService.class);

    if ((module == null || !service.canProvideSuggestions(project, module)) && !service
        .canProvideSuggestions(project)) {
      return;
    }

    YAMLKeyValue keyValue = getParentOfType(element, YAMLKeyValue.class);

    List<LookupElementBuilder> suggestions;
    // For top level element, since there is no parent keyValue would be null
    if (keyValue == null) {
      if (module == null) {
        suggestions = service.computeSuggestions(project, element.getClass().getClassLoader(), null,
            element.getText().replace(DUMMY_IDENTIFIER_TRIMMED, ""));
      } else {
        suggestions = service
            .computeSuggestions(project, module, element.getClass().getClassLoader(), null,
                element.getText().replace(DUMMY_IDENTIFIER_TRIMMED, ""));
      }
    } else {
      List<String> containerElements = new ArrayList<>();
      do {
        containerElements.add(0, keyValue.getKeyText().replace(DUMMY_IDENTIFIER_TRIMMED, ""));
        keyValue = getParentOfType(keyValue, YAMLKeyValue.class);
      } while (keyValue != null);

      if (module == null) {
        suggestions = service
            .computeSuggestions(project, element.getClass().getClassLoader(), containerElements,
                element.getText().replace(DUMMY_IDENTIFIER_TRIMMED, ""));
      } else {
        suggestions = service
            .computeSuggestions(project, module, element.getClass().getClassLoader(),
                containerElements, element.getText().replace(DUMMY_IDENTIFIER_TRIMMED, ""));
      }
    }

    if (suggestions != null) {
      suggestions.forEach(resultSet::addElement);
    }
  }
}
