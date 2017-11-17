package in.oneton.idea.spring.boot.config.autosuggest.plugin.insert.handler;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.editor.EditorModificationUtil;
import in.oneton.idea.spring.boot.config.autosuggest.plugin.model.MetadataNode;
import in.oneton.idea.spring.boot.config.autosuggest.plugin.model.ValueType;
import org.jetbrains.annotations.NotNull;

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
    if (ref.isLeaf()) {
      valueType = ValueType.parse(ref.getProperty().getType(), classLoader);
    } else if (ref.isGroup()) {
      valueType = ValueType.parse(ref.getGroup().getType(), classLoader);
    } else {
      valueType = ValueType.OBJECT;
    }
    if (!nextCharAfterSpacesAndQuotesIsColon(getStringAfterAutoCompletedValue(context))) {
      final String suffixWithCaret = ref.isLeaf() ?
          valueType.getPlaceholderSufix(context, getExistingIndentation(context, item)) :
          valueType.getPlaceholderSufixForObject(context, getExistingIndentation(context, item));
      final String suffixWithoutCaret = suffixWithCaret.replace(ValueType.CARET, "");
      EditorModificationUtil
          .insertStringAtCaret(context.getEditor(), suffixWithoutCaret, false, true,
              getCaretIndex(suffixWithCaret));
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
}
