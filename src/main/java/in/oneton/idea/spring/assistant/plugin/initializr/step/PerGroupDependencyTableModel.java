package in.oneton.idea.spring.assistant.plugin.initializr.step;

import com.intellij.ui.BooleanTableCellRenderer;
import com.intellij.ui.ColoredTableCellRenderer;
import com.intellij.ui.TableSpeedSearch;
import com.intellij.ui.TableUtil;
import com.intellij.ui.table.JBTable;
import in.oneton.idea.spring.assistant.plugin.initializr.ProjectCreationRequest;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.DependencyComposite.DependencyGroup;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.InitializerMetadata.DependencyComposite.DependencyGroup.Dependency;
import in.oneton.idea.spring.assistant.plugin.initializr.metadata.io.spring.initializr.util.Version;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;

import static com.intellij.ui.SimpleTextAttributes.GRAY_ATTRIBUTES;
import static com.intellij.ui.speedSearch.SpeedSearchUtil.applySpeedSearchHighlighting;
import static com.intellij.util.ui.UIUtil.isDescendingFrom;
import static java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager;
import static java.awt.event.KeyEvent.VK_SPACE;
import static javax.swing.KeyStroke.getKeyStroke;
import static javax.swing.ListSelectionModel.SINGLE_SELECTION;

public class PerGroupDependencyTableModel extends AbstractTableModel {

  private static final int CHECKBOX_COL_INDEX = 0;
  private static final int DEPENDENCY_NAME_COL_INDEX = 1;

  private final ProjectCreationRequest request;
  private Version bootVersion;
  private DependencyGroup dependencyGroup;
  private Set<Integer> incompatibleDependencyIndexes;

  PerGroupDependencyTableModel(JBTable table, ProjectCreationRequest request,
      DependencyGroup dependencyGroup, Version bootVersion) {
    this.request = request;
    this.dependencyGroup = dependencyGroup;
    this.bootVersion = bootVersion;
    this.incompatibleDependencyIndexes =
        dependencyGroup.getIncompatibleDependencyIndexes(bootVersion);

    table.setModel(this);
    table.setRowMargin(0);
    table.setShowColumns(false);
    table.setShowGrid(false);
    table.setShowVerticalLines(false);
    table.setCellSelectionEnabled(false);
    table.setRowSelectionAllowed(true);
    table.setSelectionMode(SINGLE_SELECTION);

    TableColumnModel columnModel = table.getColumnModel();
    columnModel.setColumnMargin(0);
    TableColumn checkBoxColumn = columnModel.getColumn(CHECKBOX_COL_INDEX);
    TableUtil.setupCheckboxColumn(checkBoxColumn);
    checkBoxColumn.setCellRenderer(new BooleanTableCellRenderer());
    TableColumn dependencyColumn = columnModel.getColumn(DEPENDENCY_NAME_COL_INDEX);
    dependencyColumn.setCellRenderer(new ColoredTableCellRenderer() {
      @Override
      protected void customizeCellRenderer(JTable table, @Nullable Object value, boolean selected,
          boolean hasFocus, int row, int column) {
        Component focusOwner = getCurrentKeyboardFocusManager().getFocusOwner();
        boolean tableHasFocus = focusOwner != null && isDescendingFrom(focusOwner, table);
        setPaintFocusBorder(tableHasFocus && table.isRowSelected(row));
        if (value != null) {
          Dependency dependency = Dependency.class.cast(value);
          boolean selectable = isCellEditable(row, CHECKBOX_COL_INDEX);
          if (selectable) {
            append(dependency.getName());
          } else {
            append(dependency.getName(), GRAY_ATTRIBUTES);
          }
        }
        // Enable search highlighting. This in conjunction with TableSpeedSearch(below) enables type to search capability of intellij
        applySpeedSearchHighlighting(table, this, true, selected);
      }
    });

    new TableSpeedSearch(table, value -> {
      if (value instanceof Dependency) {
        return Dependency.class.cast(value).getName();
      }
      return "";
    });

    // Add listeners

    // Allow user to select via keyboard
    table.getActionMap().put("select_deselect_dependency", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        toggleSelectionIfApplicable(table.getSelectedRow());
      }
    });
    table.getInputMap().put(getKeyStroke(VK_SPACE, 0), "select_deselect_dependency");
    // Allow user to toggle via double click
    table.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent event) {
        if (event.getClickCount() == 2 && event.getButton() == MouseEvent.BUTTON1) {
          int columIndex = table.columnAtPoint(event.getPoint());
          if (columIndex == DEPENDENCY_NAME_COL_INDEX) {
            toggleSelectionIfApplicable(table.getSelectedRow());
          }
        }
      }
    });

    table.getSelectionModel().addListSelectionListener(e -> {
      // TODO: Update the description panel
    });
  }

  @Override
  public int getRowCount() {
    return dependencyGroup.getDependencies().size();
  }

  @Override
  public int getColumnCount() {
    // checkbox & dependency name
    return 2;
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    if (columnIndex == CHECKBOX_COL_INDEX) {
      return Boolean.class;
    } else {
      return String.class;
    }
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return columnIndex == CHECKBOX_COL_INDEX && !incompatibleDependencyIndexes.contains(rowIndex);
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    Dependency dependency = dependencyGroup.getDependencies().get(rowIndex);
    return columnIndex == CHECKBOX_COL_INDEX ?
        request.getDependencies().contains(dependency) :
        dependency;
  }

  @Override
  public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    if (columnIndex == CHECKBOX_COL_INDEX) {
      Dependency dependency = dependencyGroup.getDependencies().get(rowIndex);
      Boolean selected = Boolean.class.cast(aValue);
      if (selected) {
        request.getDependencies().add(dependency);
        // TODO: Update Selected dependencies panel
      } else {
        request.getDependencies().remove(dependency);
      }
    }
    fireTableCellUpdated(rowIndex, columnIndex);
  }

  public void update(DependencyGroup dependencyGroup) {
    this.dependencyGroup = dependencyGroup;
    this.incompatibleDependencyIndexes =
        dependencyGroup.getIncompatibleDependencyIndexes(bootVersion);
    fireTableDataChanged();
  }

  public void update(Version bootVersion) {
    this.bootVersion = bootVersion;
    this.incompatibleDependencyIndexes =
        dependencyGroup.getIncompatibleDependencyIndexes(bootVersion);

    // if any of the existing selections are no longer compatible with newer boot version, lets just remove them from current selection
    this.incompatibleDependencyIndexes.stream().map(this.dependencyGroup.getDependencies()::get)
        .forEach(request.getDependencies()::remove);
    fireTableDataChanged();
  }

  private void toggleSelectionIfApplicable(int row) {
    if (isCellEditable(row, CHECKBOX_COL_INDEX)) {
      boolean selected = (Boolean) getValueAt(row, CHECKBOX_COL_INDEX);
      setValueAt(!selected, row, CHECKBOX_COL_INDEX);
    }
  }

}
