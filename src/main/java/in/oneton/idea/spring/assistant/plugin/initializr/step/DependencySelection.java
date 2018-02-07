package in.oneton.idea.spring.assistant.plugin.initializr.step;

import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.ListSpeedSearch;
import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import com.intellij.util.Function;
import in.oneton.idea.spring.assistant.plugin.initializr.ProjectCreationRequest;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.DependencyComposite.DependencyGroup;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.IdAndName;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.IdAndNameComposite;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.io.spring.initializr.util.Version;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

import static com.intellij.ui.components.JBList.createDefaultListModel;
import static com.intellij.ui.speedSearch.SpeedSearchUtil.applySpeedSearchHighlighting;
import static in.oneton.idea.spring.assistant.plugin.initializr.misc.FormUtil.newCollectionComboBoxModel;
import static java.util.Objects.requireNonNull;

public class DependencySelection {

  private JTextField filter;
  private JComboBox<IdAndName> bootVersion;
  private JPanel rootPanel;
  private JPanel rightPanel;
  private JPanel leftpanel;
  private JSplitPane leftRightSeperator;
  private JPanel leftTopPanel;
  private JPanel leftBottomPanel;
  private JSplitPane groupChildSeperator;
  private JPanel groupPanel;
  private JPanel childPanel;
  private JSplitPane perGroupDependenciesAndDetailSeperator;
  private JPanel perGroupDependenciesPanel;
  private JPanel detailPanel;
  private JPanel selectedDependenciesPanel;
  private JBTable perGroupDependencyTable;
  private JBList<DependencyGroup> groups;

  private PerGroupDependencyTableModel perGroupDependencyTableModel;

  public void init(ProjectCreationRequest request) {
    InitializerMetadata metadata = request.getMetadata();

    IdAndNameComposite bootVersionComposite = metadata.getBootVersionComposite();
    CollectionComboBoxModel<IdAndName> bootVersionComboBoxModel =
        newCollectionComboBoxModel(bootVersionComposite.getValues(),
            bootVersionComposite.getDefaultValue());
    bootVersion.setModel(bootVersionComboBoxModel);

    List<DependencyGroup> dependencyGroups = metadata.getDependencyComposite().getGroups();
    groups.setModel(createDefaultListModel(dependencyGroups));
    ColoredListCellRenderer<DependencyGroup> categoryRenderer =
        new ColoredListCellRenderer<DependencyGroup>() {
          protected void customizeCellRenderer(@NotNull JList<? extends DependencyGroup> list,
              DependencyGroup value, int index, boolean selected, boolean hasFocus) {
            append(value.getName());
            applySpeedSearchHighlighting(groups, this, true, selected);
          }
        };
    groups.setCellRenderer(categoryRenderer);

    new ListSpeedSearch<>(groups,
        (Function<DependencyGroup, String>) value -> DependencyGroup.class.cast(value).getName());

    // select first group
    groups.setSelectedIndex(0);

    // per group dependencies setup
    perGroupDependencyTableModel =
        new PerGroupDependencyTableModel(perGroupDependencyTable, request, getGroupSelection(),
            getBootVersion());

    // add all listeners
    groups.addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) {
        perGroupDependencyTableModel.update(getGroupSelection());
      }
    });
    bootVersion.addActionListener(e -> perGroupDependencyTableModel.update(getBootVersion()));
  }

  public JPanel getRootPanel() {
    return rootPanel;
  }

  private DependencyGroup getGroupSelection() {
    return groups.getSelectedValue();
  }

  private Version getBootVersion() {
    return IdAndName.class.cast(requireNonNull(bootVersion.getSelectedItem())).getVersion();
  }

}
