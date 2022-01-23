package dev.flikas.spring.boot.assistant.idea.plugin.suggestion.reference;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.impl.light.LightMethodBuilder;
import com.intellij.psi.util.PsiTypesUtil;
import in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNode;
import in.oneton.idea.spring.assistant.plugin.suggestion.clazz.GenericClassMemberWrapper;
import in.oneton.idea.spring.assistant.plugin.suggestion.clazz.MetadataProxy;
import in.oneton.idea.spring.assistant.plugin.suggestion.completion.SuggestionDocumentationHelper;
import in.oneton.idea.spring.assistant.plugin.suggestion.metadata.MetadataNonPropertySuggestionNode;
import in.oneton.idea.spring.assistant.plugin.suggestion.metadata.MetadataPropertySuggestionNode;
import in.oneton.idea.spring.assistant.plugin.suggestion.metadata.json.SpringConfigurationMetadataGroup;
import in.oneton.idea.spring.assistant.plugin.suggestion.service.SuggestionService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.openapi.util.text.StringUtil.isNotEmpty;
import static in.oneton.idea.spring.assistant.plugin.misc.GenericUtil.truncateIdeaDummyIdentifier;
import static in.oneton.idea.spring.assistant.plugin.misc.PsiCustomUtil.findModule;
import static in.oneton.idea.spring.assistant.plugin.suggestion.Suggestion.PERIOD_DELIMITER;
import static in.oneton.idea.spring.assistant.plugin.suggestion.SuggestionNode.sanitise;
import static java.util.Objects.requireNonNull;

public class YamlKeyReference extends PsiReferenceBase<PsiElement> {
  private static final Logger log = Logger.getInstance(YamlKeyReference.class);
  private final YAMLKeyValue yamlKeyValue;

  public YamlKeyReference(@NotNull YAMLKeyValue yamlKeyValue) {
    super(yamlKeyValue);
    this.yamlKeyValue = yamlKeyValue;
  }

  @Override
  public @Nullable PsiElement resolve() {
    Module module = findModule(yamlKeyValue);
    if (module == null) {
      return null;
    }
    SuggestionService service = module.getService(SuggestionService.class);
    if (!service.canProvideSuggestions()) {
      return null;
    }

    List<String> ancestralKeys = new ArrayList<>();
    PsiElement context = yamlKeyValue;
    do {
      if (context instanceof YAMLKeyValue) {
        ancestralKeys.add(0, truncateIdeaDummyIdentifier(((YAMLKeyValue) context).getKeyText()));
      }
      context = requireNonNull(context).getParent();
    } while (context != null);
    List<SuggestionNode> matchedNodesFromRootTillLeaf = service.findMatchedNodesRootTillEnd(ancestralKeys);
    if (matchedNodesFromRootTillLeaf == null) {
      return null;
    }

    SuggestionNode node = matchedNodesFromRootTillLeaf.get(matchedNodesFromRootTillLeaf.size() - 1);
    if (node instanceof MetadataNonPropertySuggestionNode) {
      SpringConfigurationMetadataGroup group = ((MetadataNonPropertySuggestionNode) node).getGroup();
      if (group == null) return null;
      MetadataProxy delegate = group.getDelegate(module);
      if (delegate == null) return null;
      PsiClass psiClass = PsiTypesUtil.getPsiClass(delegate.getPsiType(module));
      if (psiClass == null) return null;
      if (isNotEmpty(group.getSourceType()) && isNotEmpty(group.getSourceMethod())) {
        @Nullable PsiClass sourceClass = JavaPsiFacade
            .getInstance(module.getProject())
            .findClass(group.getSourceType(), module.getModuleRuntimeScope(false));
        if (sourceClass != null) {
          PsiMethod method = sourceClass.findMethodBySignature(new LightMethodBuilder(
              psiClass.getManager(),
              group.getSourceMethod().replace("()", "")
          ), false);
          if (method != null) {
            return method;
          }
        }
      }
      return psiClass;
    } else if (node instanceof MetadataPropertySuggestionNode) {
      MetadataNonPropertySuggestionNode parent = ((MetadataPropertySuggestionNode) node).getParent();
      if (parent == null) return null;
      SpringConfigurationMetadataGroup group = parent.getGroup();
      if (group == null) return null;
      MetadataProxy delegate = group.getDelegate(module);
      if (delegate == null) return null;
      String[] splits = ((MetadataPropertySuggestionNode) node).getName().trim().split(PERIOD_DELIMITER, -1);
      SuggestionDocumentationHelper child = delegate.findDirectChild(module, sanitise(splits[splits.length - 1]));
      if (child instanceof GenericClassMemberWrapper) {
        return ((GenericClassMemberWrapper) child).getMember();
      }
    }
    return null;
  }
}
