package in.oneton.idea.spring.assistant.plugin.suggestion.completion;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.patterns.PlatformPatterns;
import dev.flikas.spring.boot.assistant.idea.plugin.suggestion.filetype.YamlPropertiesFileType;
import org.jetbrains.yaml.YAMLLanguage;

import static com.intellij.patterns.PlatformPatterns.virtualFile;

public class YamlCompletionContributor extends CompletionContributor {

  public YamlCompletionContributor() {
    extend(
        CompletionType.BASIC,
        PlatformPatterns.psiElement().withLanguage(YAMLLanguage.INSTANCE)
                        .inVirtualFile(virtualFile().ofType(YamlPropertiesFileType.INSTANCE)),
        new YamlCompletionProvider()
    );
  }

}
