package in.oneton.idea.spring.assistant.plugin;

import com.intellij.codeInsight.daemon.impl.analysis.JavaGenericsUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PropertyUtil;
import gnu.trove.THashMap;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static com.intellij.psi.PsiModifier.PUBLIC;
import static com.intellij.psi.PsiModifier.STATIC;
import static com.intellij.psi.search.GlobalSearchScope.moduleScope;
import static com.intellij.psi.util.PropertyUtil.isSimplePropertyGetter;
import static com.intellij.psi.util.PropertyUtil.isSimplePropertySetter;
import static com.intellij.psi.util.PsiModificationTracker.JAVA_STRUCTURE_MODIFICATION_COUNT;


@UtilityClass
public class PsiUtil {

  @Nullable
  public PsiClass findClass(@NotNull Project project, @NotNull Module module,
      @NotNull String name) {
    return JavaPsiFacade.getInstance(project).findClass(name, moduleScope(module));
  }

  @NotNull
  public static Optional<PsiField> findSettablePsiField(@NotNull PsiClass clazz,
      @Nullable String propertyName) {
    PsiMethod propertySetter = PropertyUtil.findPropertySetter(clazz, propertyName, false, true);
    return null == propertySetter ?
        Optional.empty() :
        Optional.ofNullable(PropertyUtil.findPropertyFieldByMember(propertySetter));
  }

  @Contract("null->false")
  public static boolean isPrimitiveOrBoxed(@Nullable PsiType psiType) {
    return psiType instanceof PsiPrimitiveType || PsiPrimitiveType.getUnboxedType(psiType) != null;
  }

  @Contract("_, null->false")
  private static boolean representsCollection(@NotNull PsiClass psiClass, @Nullable PsiType type) {
    return type != null && getCollectionItemType(psiClass, type) != null;
  }

  @Nullable
  private static PsiType getCollectionItemType(@NotNull PsiClass psiClass, @NotNull PsiType type) {
    return JavaGenericsUtil.getCollectionItemType(type, psiClass.getResolveScope());
  }

  @NotNull
  public static Map<String, PsiMember> findWritableProperties(@Nullable PsiClass psiClass) {
    if (psiClass != null) {
      return CachedValuesManager.getCachedValue(psiClass, () -> CachedValueProvider.Result
          .create(prepareWritableProperties(psiClass), JAVA_STRUCTURE_MODIFICATION_COUNT));
    }
    return Collections.emptyMap();
  }

  @NotNull
  private static Map<String, PsiMember> prepareWritableProperties(@NotNull PsiClass psiClass) {
    final Map<String, PsiMember> acceptableMembers = new THashMap<>();
    for (PsiMethod method : psiClass.getAllMethods()) {
      if (method.hasModifierProperty(STATIC) || !method.hasModifierProperty(PUBLIC)) {
        continue;
      }
      if (isSimplePropertyGetter(method)) {
        PsiMember acceptableMember = method;
        final String propertyName = PropertyUtil.getPropertyName(method);
        assert propertyName != null;

        PsiMethod setter = findInstancePropertySetter(psiClass, propertyName);
        if (setter != null) {
          final PsiType setterArgType = setter.getParameterList().getParameters()[0].getType();
          final PsiField field = psiClass.findFieldByName(propertyName, true);
          if (field != null && !field.hasModifierProperty(STATIC)) {
            final PsiType fieldType = getWritablePropertyType(psiClass, field);
            if (fieldType == null || setterArgType.isConvertibleFrom(fieldType)) {
              acceptableMember = field;
            }
          }
        } else {
          final PsiType returnType = method.getReturnType();
          if (returnType != null && representsCollection(psiClass, returnType)) {
            final PsiField field = psiClass.findFieldByName(propertyName, true);
            if (field != null && !field.hasModifierProperty(STATIC)) {
              final PsiType fieldType = getWritablePropertyType(psiClass, field);
              if (fieldType == null || returnType.isAssignableFrom(fieldType)) {
                acceptableMember = field;
              }
            }
          } else {
            acceptableMember = null;
          }
        }
        if (acceptableMember != null)
          acceptableMembers.put(propertyName, acceptableMember);
      }
    }
    return acceptableMembers;
  }

  @Nullable
  public static PsiType getWritablePropertyType(@Nullable PsiClass containingClass,
      @Nullable PsiElement declaration) {
    if (declaration instanceof PsiField) {
      return getWrappedPropertyType((PsiField) declaration, JavaFxCommonNames.ourWritableMap);
    }
    if (declaration instanceof PsiMethod) {
      final PsiMethod method = (PsiMethod) declaration;
      if (method.getParameterList().getParametersCount() != 0) {
        return getSetterArgumentType(method);
      }
      final String propertyName = PropertyUtil.getPropertyName(method);
      final PsiClass psiClass =
          containingClass != null ? containingClass : method.getContainingClass();
      if (propertyName != null && containingClass != null) {
        final PsiMethod setter = findInstancePropertySetter(psiClass, propertyName);
        if (setter != null) {
          final PsiType setterArgumentType = getSetterArgumentType(setter);
          if (setterArgumentType != null)
            return setterArgumentType;
        }
      }
      return getGetterReturnType(method);
    }
    return null;
  }

  @Nullable
  public static PsiType getWrappedPropertyType(final PsiField field) {
    return CachedValuesManager.getCachedValue(field, () -> {
      final PsiType fieldType = field.getType();
      final PsiClassType.ClassResolveResult resolveResult =
          com.intellij.psi.util.PsiUtil.resolveGenericsClassInType(fieldType);
      final PsiClass fieldClass = resolveResult.getElement();
      if (fieldClass == null) {
        final PsiType propertyType = eraseFreeTypeParameters(fieldType, field);
        return CachedValueProvider.Result.create(propertyType, JAVA_STRUCTURE_MODIFICATION_COUNT);
      }
      return CachedValueProvider.Result.create(null, JAVA_STRUCTURE_MODIFICATION_COUNT);
    });
  }

  @Nullable
  private static PsiType getSetterArgumentType(@NotNull PsiMethod method) {
    return CachedValuesManager.getCachedValue(method, () -> {
      final PsiParameter[] parameters = method.getParameterList().getParameters();
      if (!method.hasModifierProperty(STATIC) && parameters.length == 1) {
        final PsiType argumentType = eraseFreeTypeParameters(parameters[0].getType(), method);
        return CachedValueProvider.Result.create(argumentType, JAVA_STRUCTURE_MODIFICATION_COUNT);
      }
      return CachedValueProvider.Result.create(null, JAVA_STRUCTURE_MODIFICATION_COUNT);
    });
  }

  @Nullable
  private static PsiType eraseFreeTypeParameters(@Nullable PsiType psiType,
      @NotNull PsiMember member) {
    final PsiClass containingClass = member.getContainingClass();
    return eraseFreeTypeParameters(psiType, containingClass);
  }

  @Nullable
  private static PsiType eraseFreeTypeParameters(@Nullable PsiType psiType,
      @Nullable PsiClass containingClass) {
    if (containingClass == null)
      return null;
    return JavaPsiFacade.getElementFactory(containingClass.getProject())
        .createRawSubstitutor(containingClass).substitute(psiType);
  }

  private static PsiType getGetterReturnType(@NotNull PsiMethod method) {
    return CachedValuesManager.getCachedValue(method, () -> {
      final PsiType returnType = eraseFreeTypeParameters(method.getReturnType(), method);
      return CachedValueProvider.Result.create(returnType, JAVA_STRUCTURE_MODIFICATION_COUNT);
    });
  }

  @Nullable
  public static PsiMethod findInstancePropertySetter(@NotNull PsiClass psiClass,
      @Nullable String propertyName) {
    if (StringUtil.isEmpty(propertyName))
      return null;
    final String suggestedSetterName = PropertyUtil.suggestSetterName(propertyName);
    final PsiMethod[] setters = psiClass.findMethodsByName(suggestedSetterName, true);
    for (PsiMethod setter : setters) {
      if (setter.hasModifierProperty(PUBLIC) && !setter.hasModifierProperty(STATIC)
          && isSimplePropertySetter(setter)) {
        return setter;
      }
    }
    return null;
  }

}
