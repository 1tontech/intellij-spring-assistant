package in.oneton.idea.spring.assistant.plugin.insert.handler;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.Suggestion;
import in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLElementGenerator;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import static com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction;
import static com.intellij.openapi.editor.EditorModificationUtil.insertStringAtCaret;
import static in.oneton.idea.spring.assistant.plugin.Util.getCodeStyleIntent;
import static in.oneton.idea.spring.assistant.plugin.model.suggestion.SuggestionNodeType.CARET;

public class YamlKeyInsertHandler implements InsertHandler<LookupElement> {

  private final Suggestion suggestion;

  public YamlKeyInsertHandler(Suggestion suggestion) {
    this.suggestion = suggestion;
  }

  @Override
  public void handleInsert(final InsertionContext context, final LookupElement item) {
    SuggestionNodeType type = suggestion.getSuggestionType();

    if (!nextCharAfterSpacesAndQuotesIsColon(getStringAfterAutoCompletedValue(context))) {
      String existingIndentation = getExistingIndentation(context, item);
      Suggestion suggestion = (Suggestion) item.getObject();
      String indent = getCodeStyleIntent(context);
      String insertedText = suggestion.getSuggestionReplacement(existingIndentation, indent);
      String additionalIndent = suggestion.getNewOverallIndent(existingIndentation, indent);

      final String suggestionWithCaret =
          insertedText + type.getPlaceholderSufixForKey(context, additionalIndent);
      final String suggestionWithoutCaret = suggestionWithCaret.replace(CARET, "");

      PsiElement currentElement = context.getFile().findElementAt(context.getStartOffset());
      assert currentElement != null : "no element at " + context.getStartOffset();

      this.deleteLookupTextAndRetrieveOldValue(context, currentElement);

      insertStringAtCaret(context.getEditor(), suggestionWithoutCaret, false, true,
          getCaretIndex(suggestionWithCaret));
    }
  }

  private int getCaretIndex(final String suggestionWithCaret) {
    return suggestionWithCaret.indexOf(CARET);
  }

  private String getExistingIndentation(final InsertionContext context, final LookupElement item) {
    final String stringBeforeAutoCompletedValue = getStringBeforeAutoCompletedValue(context, item);
    return getExistingIndentationInRowStartingFromEnd(stringBeforeAutoCompletedValue);
  }

  @NotNull
  private String getStringAfterAutoCompletedValue(final InsertionContext context) {
    return context.getDocument().getText().substring(context.getTailOffset());
  }

  @NotNull
  private String getStringBeforeAutoCompletedValue(final InsertionContext context,
      final LookupElement item) {
    return context.getDocument().getText()
        .substring(0, context.getTailOffset() - item.getLookupString().length());
  }

  private boolean nextCharAfterSpacesAndQuotesIsColon(final String string) {
    for (int i = 0; i < string.length(); i++) {
      final char c = string.charAt(i);
      if (c != ' ' && c != '"') {
        return c == ':';
      }
    }
    return false;
  }

  private String getExistingIndentationInRowStartingFromEnd(final String string) {
    int count = 0;
    for (int i = string.length() - 1; i >= 0; i--) {
      final char c = string.charAt(i);
      if (c != '\t' && c != ' ') {
        break;
      }
      count++;
    }
    return string.substring(string.length() - count, string.length());
  }

  private void deleteLookupTextAndRetrieveOldValue(InsertionContext context,
      @NotNull PsiElement elementAtCaret) {
    if (elementAtCaret.getNode().getElementType() != YAMLTokenTypes.SCALAR_KEY) {
      deleteLookupPlain(context);
    } else {
      YAMLKeyValue keyValue = PsiTreeUtil.getParentOfType(elementAtCaret, YAMLKeyValue.class);
      assert keyValue != null;
      context.commitDocument();

      // TODO: Whats going on here?
      if (keyValue.getValue() != null) {
        YAMLKeyValue dummyKV =
            YAMLElementGenerator.getInstance(context.getProject()).createYamlKeyValue("foo", "b");
        dummyKV.setValue(keyValue.getValue());
      }

      context.setTailOffset(keyValue.getTextRange().getEndOffset());
      runWriteCommandAction(context.getProject(),
          () -> keyValue.getParentMapping().deleteKeyValue(keyValue));
    }
  }

  private void deleteLookupPlain(InsertionContext context) {
    Document document = context.getDocument();
    document.deleteString(context.getStartOffset(), context.getTailOffset());
    context.commitDocument();
  }

}
