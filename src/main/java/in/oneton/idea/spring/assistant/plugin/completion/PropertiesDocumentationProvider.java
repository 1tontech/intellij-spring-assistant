package in.oneton.idea.spring.assistant.plugin.completion;

public class PropertiesDocumentationProvider {
  //    extends AbstractDocumentationProvider {
  //  @Override
  //  public String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
  //    if (element instanceof DocumentationProxyElement) {
  //      DocumentationProxyElement proxyElement = DocumentationProxyElement.class.cast(element);
  //      SuggestionNode target = proxyElement.target;
  //      boolean requestedForTargetValue = proxyElement.requestedForTargetValue;
  //      String value = proxyElement.value;
  //
  //      // Only group & leaf are expected to have documentation
  //      if (target != null && (target.isGroup() || target.isLeaf(findModuleForPsiElement(element)))) {
  //        if (requestedForTargetValue) {
  //          return target.getDocumentationForValue(value);
  //        } else {
  //          return target.getDocumentationForKey();
  //        }
  //      }
  //    }
  //    return super.generateDoc(element, originalElement);
  //  }
  //
  //  /*
  //   * This will called if the user tries to lookup documentation for the choices being shown (ctrl+q within suggestion dialog)
  //   */
  //  @Override
  //  public PsiElement getDocumentationElementForLookupItem(PsiManager psiManager, Object object,
  //      @Nullable PsiElement element) {
  //    if (object instanceof Suggestion) {
  //      Suggestion suggestion = Suggestion.class.cast(object);
  //      boolean requestedForTargetValue = suggestion.isForValue();
  //      String text = null;
  //      if (element != null) {
  //        text = element.getText();
  //      }
  //      return new DocumentationProxyElement(psiManager, JavaLanguage.INSTANCE, target,
  //          requestedForTargetValue, text);
  //    }
  //    return super.getDocumentationElementForLookupItem(psiManager, object, element);
  //  }
  //
  //  @Nullable
  //  @Override
  //  public PsiElement getCustomDocumentationElement(@NotNull Editor editor, @NotNull PsiFile file,
  //      @Nullable PsiElement contextElement) {
  //    if (contextElement != null) {
  //      MetadataNonPropertySuggestionNode target;
  //      boolean requestedForTargetValue;
  //      String value;
  //
  //      SuggestionService service =
  //          ServiceManager.getService(contextElement.getProject(), SuggestionService.class);
  //
  //      Project project = contextElement.getProject();
  //      Module module = ModuleUtil.findModuleForPsiElement(contextElement);
  //
  //      List<String> containerElements = new ArrayList<>();
  //      Optional<String> keyNameIfKey = getKeyNameOfObject(contextElement);
  //      keyNameIfKey.ifPresent(containerElements::add);
  //      Property property = getParentOfType(contextElement, Property.class);
  //
  //      if (property != null) {
  //        assert property.getKey() != null;
  //        containerElements.add(0, truncateIdeaDummyIdentifier(property.getKey()));
  //      }
  //
  //      requestedForTargetValue = false;
  //      value = contextElement.getText();
  //
  //      if (containerElements.size() > 0) {
  //        if (module == null) {
  //          //          target = service.findDeepestSuggestionNode(project, containerElements);
  //        } else {
  //          //          target = service.findDeepestSuggestionNode(project, module, containerElements);
  //        }
  //        if (target != null && target.isLeaf()) {
  //          requestedForTargetValue = GenericUtil.isValue(contextElement);
  //        }
  //
  //        if (target != null) {
  //          return new DocumentationProxyElement(file.getManager(), file.getLanguage(), target,
  //              requestedForTargetValue, value);
  //        }
  //      }
  //    }
  //    return super.getCustomDocumentationElement(editor, file, contextElement);
  //  }
  //
  //  @ToString(of = "target")
  //  private static class DocumentationProxyElement extends LightElement {
  //    private final SuggestionNode target;
  //    @Nullable
  //    private final String value;
  //    /**
  //     * Documentation can be requested for the key (or) value. Value would be `false` if the documentation is requested for target. `true` if the request if for value
  //     */
  //    private boolean requestedForTargetValue;
  //
  //    DocumentationProxyElement(@NotNull final PsiManager manager, @NotNull final Language language,
  //        @NotNull final SuggestionNode target, boolean requestedForTargetValue,
  //        @Nullable String value) {
  //      super(manager, language);
  //      this.target = target;
  //      this.requestedForTargetValue = requestedForTargetValue;
  //      this.value = value;
  //    }
  //
  //    @Override
  //    public String getText() {
  //      return target.getFullPath();
  //    }
  //  }

}
