package in.oneton.idea.spring.assistant.plugin.completion;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import in.oneton.idea.spring.assistant.plugin.model.ClassSuggestionNode;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DynamicSuggestionNodeFactory {

  public ClassSuggestionNode newInstance(PsiMember target) {
    if (target instanceof PsiField) {
      PsiType targetType = ((PsiField) target).getType();
    } else if (target instanceof PsiMethod) {
      PsiType targetType = ((PsiField) target).getType();
    } else if (target instanceof PsiClass) {

    }
  }

}
