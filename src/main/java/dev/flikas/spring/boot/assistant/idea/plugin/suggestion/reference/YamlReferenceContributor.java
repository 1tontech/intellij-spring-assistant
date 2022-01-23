package dev.flikas.spring.boot.assistant.idea.plugin.suggestion.reference;

import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;
import dev.flikas.spring.boot.assistant.idea.plugin.suggestion.filetype.YamlPropertiesFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.intellij.patterns.PsiJavaPatterns.virtualFile;

public class YamlReferenceContributor extends PsiReferenceContributor {

  @Override
  public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
    PsiElementPattern.Capture<YAMLKeyValue> pattern =
        psiElement(YAMLKeyValue.class)
            .withLanguage(YAMLLanguage.INSTANCE)
            .inVirtualFile(virtualFile().ofType(YamlPropertiesFileType.INSTANCE));
    registrar.registerReferenceProvider(pattern, new PsiReferenceProvider() {
      @Override
      public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
          @NotNull ProcessingContext context) {
        if (element instanceof YAMLKeyValue) {
          return new YamlKeyReference[]{new YamlKeyReference((YAMLKeyValue) element)};
        } else {
          return PsiReference.EMPTY_ARRAY;
        }
      }
    });
  }

}
