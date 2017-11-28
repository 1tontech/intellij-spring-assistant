package in.oneton.idea.spring.boot.config.autosuggest.plugin.insert.handler;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import in.oneton.idea.spring.boot.config.autosuggest.plugin.model.MetadataNode;
import in.oneton.idea.spring.boot.config.autosuggest.plugin.model.Suggestion;
import in.oneton.idea.spring.boot.config.autosuggest.plugin.model.ValueType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLElementGenerator;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import static in.oneton.idea.spring.boot.config.autosuggest.plugin.Util.getCodeStyleIntent;

public class YamlKeyInsertHandler implements InsertHandler<LookupElement> {

  private final MetadataNode ref;
  private final ClassLoader classLoader;

  public YamlKeyInsertHandler(MetadataNode ref, ClassLoader classLoader) {
    this.ref = ref;
    this.classLoader = classLoader;
  }

  @Override
  public void handleInsert(final InsertionContext context, final LookupElement item) {
    ValueType valueType;
    boolean leafDefaultValueNonObject = false;
    if (ref.isLeaf()) {
      assert ref.getProperty() != null;
      valueType = ValueType.parse(ref.getProperty().getType(), classLoader);
      leafDefaultValueNonObject = ref.getProperty().hasNonObjectDefaultValue();
    } else if (ref.isGroup()) {
      assert ref.getGroup() != null;
      valueType = ValueType.parse(ref.getGroup().getType(), classLoader);
    } else {
      valueType = ValueType.OBJECT;
    }
    if (!nextCharAfterSpacesAndQuotesIsColon(getStringAfterAutoCompletedValue(context))) {
      String existingIndentation = getExistingIndentation(context, item);
      Suggestion suggestion = (Suggestion) item.getObject();
      String indent = getCodeStyleIntent(context);
      String insertedText = suggestion.getRef()
          .getSuggestionReplacement(existingIndentation, indent, suggestion.getMaxDepth());
      String additionalIndent = suggestion.getRef()
          .getNewOverallIndent(existingIndentation, indent, suggestion.getMaxDepth());

      final String suggestionWithCaret = insertedText + (ref.isLeaf() ?
          valueType.getPlaceholderSufix(context, additionalIndent, leafDefaultValueNonObject) :
          valueType.getPlaceholderSufixForObject(context, additionalIndent));
      final String suggestionWithoutCaret = suggestionWithCaret.replace(ValueType.CARET, "");

      PsiElement currentElement = context.getFile().findElementAt(context.getStartOffset());

      assert currentElement != null : "no element at " + context.getStartOffset();

      YAMLDocument holdingDocument =
          PsiTreeUtil.getParentOfType(currentElement, YAMLDocument.class);

      assert holdingDocument != null;

      this.deleteLookupTextAndRetrieveOldValue(context, currentElement);

      EditorModificationUtil
          .insertStringAtCaret(context.getEditor(), suggestionWithoutCaret, false, true,
              getCaretIndex(suggestionWithCaret));
    }
  }

  private int getCaretIndex(final String suffix) {
    return suffix.indexOf(ValueType.CARET);
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

  @Nullable
  private void deleteLookupTextAndRetrieveOldValue(InsertionContext context,
      @NotNull PsiElement elementAtCaret) {
    if (elementAtCaret.getNode().getElementType() != YAMLTokenTypes.SCALAR_KEY) {
      deleteLookupPlain(context);
    } else {
      YAMLKeyValue keyValue = PsiTreeUtil.getParentOfType(elementAtCaret, YAMLKeyValue.class);
      assert keyValue != null;
      context.commitDocument();
      if (keyValue.getValue() != null) {
        YAMLKeyValue dummyKV =
            YAMLElementGenerator.getInstance(context.getProject()).createYamlKeyValue("foo", "b");
        dummyKV.setValue(keyValue.getValue());
      }

      context.setTailOffset(keyValue.getTextRange().getEndOffset());
      WriteCommandAction.runWriteCommandAction(context.getProject(), () -> {
        keyValue.getParentMapping().deleteKeyValue(keyValue);
      });
    }
  }

  private void deleteLookupPlain(InsertionContext context) {
    Document document = context.getDocument();
    document.deleteString(context.getStartOffset(), context.getTailOffset());
    context.commitDocument();
  }

}
