package in.oneton.idea.spring.assistant.plugin.completion;

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
import in.oneton.idea.spring.assistant.plugin.ClassUtil;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.service.SuggestionService;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.intellij.psi.util.PsiTreeUtil.getParentOfType;
import static in.oneton.idea.spring.assistant.plugin.ClassUtil.getKeyNameOfObject;
import static in.oneton.idea.spring.assistant.plugin.Util.truncateIdeaDummyIdentifier;
import static java.util.stream.Collectors.joining;

public class YamlDocumentationProvider extends AbstractDocumentationProvider {
  @Override
  public String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
    if (element instanceof DocumentationProxyElement) {
      DocumentationProxyElement proxyElement = DocumentationProxyElement.class.cast(element);
      SuggestionNode target = proxyElement.target;

      // Intermediate nodes will not have
      if (target != null && target.supportsDocumentation()) {
        if (proxyElement.requestedForTargetValue) {
          return target.getDocumentationForValue(proxyElement.nodeNavigationPathDotDelimited,
              proxyElement.value);
        } else {
          return target.getDocumentationForKey(proxyElement.nodeNavigationPathDotDelimited);
        }
      }
    }
    return super.generateDoc(element, originalElement);
  }

  /*
   * This will called if the user tries to lookup documentation for the choices being shown (ctrl+q within suggestion dialog)
   */
  @Override
  public PsiElement getDocumentationElementForLookupItem(PsiManager psiManager, Object object,
      @Nullable PsiElement element) {
    if (object instanceof Suggestion) {
      //noinspection unchecked
      Suggestion suggestion = Suggestion.class.cast(object);
      String text = null;
      if (element != null) {
        text = element.getText();
      }
      return new DocumentationProxyElement(psiManager, JavaLanguage.INSTANCE,
          suggestion.getFullPath(),
          // TODO: Fix this
          suggestion.getMatchesTopFirst().get(suggestion.getMatchesTopFirst().size() - 1),
          suggestion.isForValue(), text);
    }
    return super.getDocumentationElementForLookupItem(psiManager, object, element);
  }

  @Nullable
  @Override
  public PsiElement getCustomDocumentationElement(@NotNull Editor editor, @NotNull PsiFile file,
      @Nullable PsiElement element) {
    if (element != null) {
      List<SuggestionNode> matchedNodesFromRootTillLeaf;
      boolean requestedForTargetValue;
      String value;

      SuggestionService suggestionService =
          ServiceManager.getService(element.getProject(), SuggestionService.class);

      Project project = element.getProject();
      Module module = ModuleUtil.findModuleForPsiElement(element);

      List<String> containerElements = new ArrayList<>();
      Optional<String> keyNameIfKey = getKeyNameOfObject(element);
      keyNameIfKey.ifPresent(containerElements::add);
      YAMLKeyValue keyValue = getParentOfType(element, YAMLKeyValue.class);

      while (keyValue != null) {
        assert keyValue.getKey() != null;
        containerElements.add(0, truncateIdeaDummyIdentifier(keyValue.getKey()));
        keyValue = getParentOfType(keyValue, YAMLKeyValue.class);
      }

      requestedForTargetValue = false;
      value = element.getText();

      if (containerElements.size() > 0) {
        matchedNodesFromRootTillLeaf =
            suggestionService.findMatchedNodesRootTillEnd(project, module, containerElements);
        if (matchedNodesFromRootTillLeaf != null) {
          SuggestionNode target =
              matchedNodesFromRootTillLeaf.get(matchedNodesFromRootTillLeaf.size() - 1);
          if (target.isLeaf()) {
            requestedForTargetValue = ClassUtil.isYamlValue(element);
          }
          String targetNavigationPathDotDelimited =
              matchedNodesFromRootTillLeaf.stream().map(SuggestionNode::getNameForDocumentation)
                  .collect(joining("."));
          return new DocumentationProxyElement(file.getManager(), file.getLanguage(),
              targetNavigationPathDotDelimited, target, requestedForTargetValue, value);
        }
      }
    }
    return super.getCustomDocumentationElement(editor, file, element);
  }

  @ToString(of = "nodeNavigationPathDotDelimited")
  private static class DocumentationProxyElement extends LightElement {
    private final SuggestionNode target;
    private final boolean requestedForTargetValue;
    @Nullable
    private final String value;
    private final String nodeNavigationPathDotDelimited;

    DocumentationProxyElement(@NotNull final PsiManager manager, @NotNull final Language language,
        String nodeNavigationPathDotDelimited, @NotNull final SuggestionNode target,
        boolean requestedForTargetValue, @Nullable String value) {
      super(manager, language);
      this.nodeNavigationPathDotDelimited = nodeNavigationPathDotDelimited;
      this.target = target;
      this.requestedForTargetValue = requestedForTargetValue;
      this.value = value;
    }

    @Override
    public String getText() {
      return nodeNavigationPathDotDelimited;
    }
  }

}
