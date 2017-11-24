package in.oneton.idea.spring.boot.config.autosuggest.plugin.completion;

import com.intellij.lang.Language;
import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.light.LightElement;
import in.oneton.idea.spring.boot.config.autosuggest.plugin.Util;
import in.oneton.idea.spring.boot.config.autosuggest.plugin.model.MetadataNode;
import in.oneton.idea.spring.boot.config.autosuggest.plugin.model.Suggestion;
import in.oneton.idea.spring.boot.config.autosuggest.plugin.service.SuggestionIndexService;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.intellij.codeInsight.completion.CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED;
import static com.intellij.psi.util.PsiTreeUtil.getParentOfType;
import static in.oneton.idea.spring.boot.config.autosuggest.plugin.Util.getKeyNameOfObject;

public class YamlDocumentationProvider extends AbstractDocumentationProvider {
  @Override
  public String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
    if (element instanceof DocumentationProxyElement) {
      DocumentationProxyElement proxyElement = DocumentationProxyElement.class.cast(element);
      MetadataNode target = proxyElement.target;
      boolean requestedForTargetValue = proxyElement.requestedForTargetValue;
      String value = proxyElement.value;

      // Only group & leaf are expected to have documentation
      if (target != null && (target.isGroup() || target.isLeaf())) {
        if (requestedForTargetValue) {
          assert target.getProperty() != null;
          return target.getProperty()
              .getDocumentationForValue(target, value, element.getClass().getClassLoader());
        } else if (target.isGroup()) {
          assert target.getGroup() != null;
          return target.getGroup().getDocumentation(target);
        } else {
          assert target.getProperty() != null;
          return target.getProperty().getDocumentationForKey(target);
        }
      }
    }
    return super.generateDoc(element, originalElement);
  }

  @Override
  public PsiElement getDocumentationElementForLookupItem(PsiManager psiManager, Object object,
      @Nullable PsiElement element) {
    if (object instanceof Suggestion) {
      Suggestion suggestion = Suggestion.class.cast(object);
      MetadataNode target = suggestion.getRef();
      boolean requestedForTargetValue = suggestion.isReferringToValue();
      String text = null;
      if (element != null) {
        text = element.getText();
      }
      return new DocumentationProxyElement(psiManager, JavaLanguage.INSTANCE, target,
          requestedForTargetValue, text);
    }
    return super.getDocumentationElementForLookupItem(psiManager, object, element);
  }

  @Nullable
  @Override
  public PsiElement getCustomDocumentationElement(@NotNull Editor editor, @NotNull PsiFile file,
      @Nullable PsiElement contextElement) {
    if (contextElement != null) {
      MetadataNode target;
      boolean requestedForTargetValue;
      String value;

      SuggestionIndexService service =
          ServiceManager.getService(contextElement.getProject(), SuggestionIndexService.class);

      Project project = contextElement.getProject();
      Module module = ModuleUtil.findModuleForPsiElement(contextElement);

      List<String> containerElements = new ArrayList<>();
      Optional<String> keyNameIfKey = getKeyNameOfObject(contextElement);
      keyNameIfKey.ifPresent(containerElements::add);
      YAMLKeyValue keyValue = getParentOfType(contextElement, YAMLKeyValue.class);

      while (keyValue != null) {
        containerElements.add(0, keyValue.getKeyText().replace(DUMMY_IDENTIFIER_TRIMMED, ""));
        keyValue = getParentOfType(keyValue, YAMLKeyValue.class);
      }

      requestedForTargetValue = false;
      value = contextElement.getText();

      if (containerElements.size() > 0) {
        if (module == null) {
          target = service.findDeepestExactMatch(project, containerElements);
        } else {
          target = service.findDeepestExactMatch(project, module, containerElements);
        }
        if (target != null && target.isLeaf()) {
          requestedForTargetValue = Util.isValue(contextElement);
        }

        if (target != null) {
          return new DocumentationProxyElement(file.getManager(), file.getLanguage(), target,
              requestedForTargetValue, value);
        }
      }
    }
    return super.getCustomDocumentationElement(editor, file, contextElement);
  }

  @ToString(of = "target")
  private static class DocumentationProxyElement extends LightElement {
    private final MetadataNode target;
    @Nullable
    private final String value;
    /**
     * Documentation can be requested for the key (or) value. Value would be `false` if the documentation is requested for target. `true` if the request if for value
     */
    private boolean requestedForTargetValue;

    DocumentationProxyElement(@NotNull final PsiManager manager, @NotNull final Language language,
        @NotNull final MetadataNode target, boolean requestedForTargetValue,
        @Nullable String value) {
      super(manager, language);
      this.target = target;
      this.requestedForTargetValue = requestedForTargetValue;
      this.value = value;
    }

    @Override
    public String getText() {
      return target.getFullPath();
    }
  }

}
