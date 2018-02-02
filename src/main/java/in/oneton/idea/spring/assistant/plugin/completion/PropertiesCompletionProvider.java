package in.oneton.idea.spring.assistant.plugin.completion;

class PropertiesCompletionProvider {
  //    extends CompletionProvider<CompletionParameters> {
  //  @Override
  //  protected void addCompletions(@NotNull final CompletionParameters completionParameters,
  //      final ProcessingContext processingContext, @NotNull final CompletionResultSet resultSet) {
  //
  //    PsiElement element = completionParameters.getPosition();
  //    if (element instanceof PsiComment) {
  //      return;
  //    }
  //
  //    Project project = element.getProject();
  //    Module module = findModule(element);
  //
  //    SuggestionService service = ServiceManager.getService(project, SuggestionService.class);
  //
  //    if ((module == null || !service.canProvideSuggestions(project, module))) {
  //      return;
  //    }
  //
  //    Property property = getParentOfType(element, Property.class);
  //
  //    List<LookupElementBuilder> suggestions;
  //    // For top level element, since there is no parent keyValue would be null
  //    String queryWithDotDelimitedPrefixes = truncateIdeaDummyIdentifier(element);
  //
  //    if (property == null) {
  //      suggestions =
  //          service.computeSuggestions(project, module, element, null, queryWithDotDelimitedPrefixes);
  //    } else {
  //      assert property.getKey() != null;
  //      List<String> containerElements =
  //          singletonList(truncateIdeaDummyIdentifier(property.getKey()));
  //
  //      suggestions = service.computeSuggestions(project, module, element, containerElements,
  //          queryWithDotDelimitedPrefixes);
  //    }
  //
  //    if (suggestions != null) {
  //      suggestions.forEach(resultSet::addElement);
  //    }
  //  }

}
