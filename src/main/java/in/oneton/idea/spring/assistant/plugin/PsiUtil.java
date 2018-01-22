package in.oneton.idea.spring.assistant.plugin;

import com.intellij.codeInsight.daemon.impl.analysis.JavaGenericsUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PropertyUtil;
import gnu.trove.THashMap;
import in.oneton.idea.spring.assistant.plugin.model.SuggestionNodeType;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static com.intellij.psi.PsiModifier.PUBLIC;
import static com.intellij.psi.PsiModifier.STATIC;
import static com.intellij.psi.PsiPrimitiveType.getUnboxedType;
import static com.intellij.psi.search.GlobalSearchScope.moduleScope;
import static com.intellij.psi.util.CachedValueProvider.Result.create;
import static com.intellij.psi.util.CachedValuesManager.getCachedValue;
import static com.intellij.psi.util.InheritanceUtil.isInheritor;
import static com.intellij.psi.util.PropertyUtil.findPropertyFieldByMember;
import static com.intellij.psi.util.PropertyUtil.findPropertySetter;
import static com.intellij.psi.util.PropertyUtil.getPropertyName;
import static com.intellij.psi.util.PropertyUtil.isSimplePropertyGetter;
import static com.intellij.psi.util.PropertyUtil.isSimplePropertySetter;
import static com.intellij.psi.util.PsiModificationTracker.JAVA_STRUCTURE_MODIFICATION_COUNT;
import static in.oneton.idea.spring.assistant.plugin.model.SuggestionNodeType.ARRAY;
import static in.oneton.idea.spring.assistant.plugin.model.SuggestionNodeType.BOOLEAN;
import static in.oneton.idea.spring.assistant.plugin.model.SuggestionNodeType.BYTE;
import static in.oneton.idea.spring.assistant.plugin.model.SuggestionNodeType.CHARACTER;
import static in.oneton.idea.spring.assistant.plugin.model.SuggestionNodeType.COLLECTION;
import static in.oneton.idea.spring.assistant.plugin.model.SuggestionNodeType.DOUBLE;
import static in.oneton.idea.spring.assistant.plugin.model.SuggestionNodeType.ENUM;
import static in.oneton.idea.spring.assistant.plugin.model.SuggestionNodeType.FLOAT;
import static in.oneton.idea.spring.assistant.plugin.model.SuggestionNodeType.INTEGER;
import static in.oneton.idea.spring.assistant.plugin.model.SuggestionNodeType.KNOWN_CLASS;
import static in.oneton.idea.spring.assistant.plugin.model.SuggestionNodeType.LONG;
import static in.oneton.idea.spring.assistant.plugin.model.SuggestionNodeType.MAP;
import static in.oneton.idea.spring.assistant.plugin.model.SuggestionNodeType.SHORT;
import static in.oneton.idea.spring.assistant.plugin.model.SuggestionNodeType.STRING;
import static in.oneton.idea.spring.assistant.plugin.model.SuggestionNodeType.UNKNOWN_CLASS;


@UtilityClass
public class PsiUtil {

  @Nullable
  public PsiClass findClass(@NotNull Module module, @NotNull String fqn) {
    return JavaPsiFacade.getInstance(module.getProject()).findClass(fqn, moduleScope(module));
  }

  @NotNull
  public static Optional<PsiField> findSettablePsiField(@NotNull PsiClass clazz,
      @Nullable String propertyName) {
    PsiMethod propertySetter = findPropertySetter(clazz, propertyName, false, true);
    return null == propertySetter ?
        Optional.empty() : Optional.ofNullable(findPropertyFieldByMember(propertySetter));
  }

  @NotNull
  public static SuggestionNodeType findType(@Nullable PsiClass psiClass) {
    if (psiClass == null) {
      return UNKNOWN_CLASS;
    }
    if (isBoolean(psiClass)) {
      return BOOLEAN;
    } else if (isByte(psiClass)) {
      return BYTE;
    } else if (isShort(psiClass)) {
      return SHORT;
    } else if (isInteger(psiClass)) {
      return INTEGER;
    } else if (isLong(psiClass)) {
      return LONG;
    } else if (isFloat(psiClass)) {
      return FLOAT;
    } else if (isDouble(psiClass)) {
      return DOUBLE;
    } else if (isCharacter(psiClass)) {
      return CHARACTER;
    } else if (isString(psiClass)) {
      return STRING;
    } else if (isEnum(psiClass)) {
      return ENUM;
    } else if (isArray(psiClass)) {
      return ARRAY;
    } else if (isMap(psiClass)) {
      return MAP;
    } else if (isCollection(psiClass)) {
      return COLLECTION;
    } else {
      return KNOWN_CLASS;
    }
  }

  public static boolean isBoolean(@NotNull PsiClass psiClass) {
    return isClassSameOrDescendantOf(psiClass, "java.lang.Boolean");
  }

  public static boolean isByte(@NotNull PsiClass psiClass) {
    return isClassSameOrDescendantOf(psiClass, "java.lang.Byte");
  }

  public static boolean isShort(@NotNull PsiClass psiClass) {
    return isClassSameOrDescendantOf(psiClass, "java.lang.Short");
  }

  public static boolean isInteger(@NotNull PsiClass psiClass) {
    return isClassSameOrDescendantOf(psiClass, "java.lang.Integer");
  }

  public static boolean isLong(@NotNull PsiClass psiClass) {
    return isClassSameOrDescendantOf(psiClass, "java.lang.Long");
  }

  public static boolean isFloat(@NotNull PsiClass psiClass) {
    return isClassSameOrDescendantOf(psiClass, "java.lang.Float");
  }

  public static boolean isDouble(@NotNull PsiClass psiClass) {
    return isClassSameOrDescendantOf(psiClass, "java.lang.Double");
  }

  public static boolean isCharacter(@NotNull PsiClass psiClass) {
    return isClassSameOrDescendantOf(psiClass, "java.lang.Character");
  }

  public static boolean isString(@NotNull PsiClass psiClass) {
    return isClassSameOrDescendantOf(psiClass, "java.lang.String");
  }

  public static boolean isArray(@NotNull PsiClass psiClass) {
    return psiClass instanceof PsiArrayType;
  }

  public static boolean isEnum(@NotNull PsiClass psiClass) {
    return psiClass.isEnum();
  }

  public static boolean isMap(@NotNull PsiClass psiClass) {
    return isClassSameOrDescendantOf(psiClass, "java.lang.Map");
  }

  public static boolean isCollection(@NotNull PsiClass psiClass) {
    return isClassSameOrDescendantOf(psiClass, "java.lang.Collection");
  }

  private static boolean isClassSameOrDescendantOf(@NotNull PsiClass psiClass,
      String expectedClassFqn) {
    return psiClass.getQualifiedName() != null && isInheritor(psiClass, expectedClassFqn);
  }

  @Contract("null->false")
  public static boolean isPrimitiveOrBoxed(@Nullable PsiType psiType) {
    return psiType instanceof PsiPrimitiveType || getUnboxedType(psiType) != null;
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
      return getCachedValue(psiClass,
          () -> create(prepareWritableProperties(psiClass), JAVA_STRUCTURE_MODIFICATION_COUNT));
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
        final String propertyName = getPropertyName(method);
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
      final String propertyName = getPropertyName(method);
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
    return getCachedValue(field, () -> {
      final PsiType fieldType = field.getType();
      final PsiClassType.ClassResolveResult resolveResult =
          com.intellij.psi.util.PsiUtil.resolveGenericsClassInType(fieldType);
      final PsiClass fieldClass = resolveResult.getElement();
      if (fieldClass == null) {
        final PsiType propertyType = eraseFreeTypeParameters(fieldType, field);
        return create(propertyType, JAVA_STRUCTURE_MODIFICATION_COUNT);
      }
      return create(null, JAVA_STRUCTURE_MODIFICATION_COUNT);
    });
  }

  @Nullable
  private static PsiType getSetterArgumentType(@NotNull PsiMethod method) {
    return getCachedValue(method, () -> {
      final PsiParameter[] parameters = method.getParameterList().getParameters();
      if (!method.hasModifierProperty(STATIC) && parameters.length == 1) {
        final PsiType argumentType = eraseFreeTypeParameters(parameters[0].getType(), method);
        return create(argumentType, JAVA_STRUCTURE_MODIFICATION_COUNT);
      }
      return create(null, JAVA_STRUCTURE_MODIFICATION_COUNT);
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
    return getCachedValue(method, () -> {
      final PsiType returnType = eraseFreeTypeParameters(method.getReturnType(), method);
      return create(returnType, JAVA_STRUCTURE_MODIFICATION_COUNT);
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
