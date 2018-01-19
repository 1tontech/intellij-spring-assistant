package in.oneton.idea.spring.assistant.plugin.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.properties.psi.Property;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import in.oneton.idea.spring.assistant.plugin.service.SuggestionIndexService;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.intellij.psi.util.PsiTreeUtil.getParentOfType;
import static in.oneton.idea.spring.assistant.plugin.Util.truncateIdeaDummyIdentifier;
import static java.util.Collections.singletonList;

class PropertiesCompletionProvider extends CompletionProvider<CompletionParameters> {
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

    Property property = getParentOfType(element, Property.class);

    List<LookupElementBuilder> suggestions;
    // For top level element, since there is no parent keyValue would be null
    String queryString = truncateIdeaDummyIdentifier(element);

    if (property == null) {
      if (module == null) {
        suggestions = service
            .computeSuggestions(project, element.getClass().getClassLoader(), null, queryString);
      } else {
        suggestions = service
            .computeSuggestions(project, module, element.getClass().getClassLoader(), null,
                queryString);
      }
    } else {
      assert property.getKey() != null;
      List<String> containerElements =
          singletonList(truncateIdeaDummyIdentifier(property.getKey()));

      if (module == null) {
        suggestions = service
            .computeSuggestions(project, element.getClass().getClassLoader(), containerElements,
                queryString);
      } else {
        suggestions = service
            .computeSuggestions(project, module, element.getClass().getClassLoader(),
                containerElements, queryString);
      }
    }

    if (suggestions != null) {
      suggestions.forEach(resultSet::addElement);
    }
  }

}
