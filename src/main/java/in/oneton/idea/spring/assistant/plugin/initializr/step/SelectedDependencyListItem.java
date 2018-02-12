package in.oneton.idea.spring.assistant.plugin.initializr.step;

import com.intellij.openapi.ui.popup.IconButton;
import com.intellij.ui.InplaceButton;

import javax.swing.*;

import static com.intellij.icons.AllIcons.Modules.DeleteContentFolder;
import static com.intellij.icons.AllIcons.Modules.DeleteContentFolderRollover;

public class SelectedDependencyListItem {
  private SelectedDependencyListItemListener listener;

  private InplaceButton deleteButton;
  private JLabel nameLabel;
  private JPanel root;

  public void init(String name, SelectedDependencyListItemListener listener) {
    this.listener = listener;
    nameLabel.setText(name);
  }

  private void createUIComponents() {
    deleteButton = new InplaceButton(
        new IconButton("Click to delete", DeleteContentFolder, DeleteContentFolderRollover),
        e -> listener.onDeleteClicked());
  }

  public JPanel getRoot() {
    return root;
  }

  interface SelectedDependencyListItemListener {
    void onDeleteClicked();
  }

}
