/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2018 SonarSource SA
 * sonarlint@sonarsource.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.ui.internal.properties;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PropertyPage;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.utils.PreferencesUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;
import org.sonarlint.eclipse.ui.internal.server.actions.JobUtils;
import org.sonarsource.sonarlint.core.client.api.common.RuleKey;

public class RuleExclusionsPage extends PropertyPage implements IWorkbenchPreferencePage {

  private Button removeButton;

  private List<RuleKey> excludedRules = new ArrayList<>();
  private TableViewer table;

  public RuleExclusionsPage() {
    setTitle("Rules configuration");
  }

  @Override
  public void init(IWorkbench workbench) {
    setDescription("Configure rules to be excluded from analysis in standalone mode");
    setPreferenceStore(SonarLintUiPlugin.getDefault().getPreferenceStore());
  }

  @Override
  protected Control createContents(Composite parent) {
    this.excludedRules = loadExclusions();

    // define container & its layout
    Font font = parent.getFont();
    Composite pageComponent = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    pageComponent.setLayout(layout);

    GridData data = new GridData(GridData.FILL_BOTH);
    pageComponent.setLayoutData(data);
    pageComponent.setFont(font);

    // layout the table & its buttons
    int tableStyle = SWT.BORDER | SWT.FULL_SELECTION;

    Composite tableComposite = new Composite(pageComponent, SWT.NONE);
    data = new GridData(SWT.FILL, SWT.FILL, true, true);
    data.grabExcessHorizontalSpace = true;
    data.grabExcessVerticalSpace = true;
    tableComposite.setLayoutData(data);

    table = new TableViewer(tableComposite, tableStyle);
    table.getTable().setFont(font);
    ColumnViewerToolTipSupport.enableFor(table, ToolTip.NO_RECREATE);

    TableViewerColumn ruleKeyColumn = new TableViewerColumn(table, SWT.NONE);
    ruleKeyColumn.setLabelProvider(new RuleKeyLabelProvider());
    ruleKeyColumn.getColumn().setText("Rule Key");

    TableViewerColumn descriptionColumn = new TableViewerColumn(table, SWT.NONE);
    descriptionColumn.setLabelProvider(new DescriptionLabelProvider());
    descriptionColumn.getColumn().setText("Description");

    TableColumnLayout tableLayout = new TableColumnLayout();
    tableComposite.setLayout(tableLayout);

    tableLayout.setColumnData(ruleKeyColumn.getColumn(), new ColumnWeightData(150));
    tableLayout.setColumnData(descriptionColumn.getColumn(), new ColumnWeightData(280));

    table.getTable().setHeaderVisible(true);
    data = new GridData(GridData.FILL_BOTH);
    data.heightHint = table.getTable().getItemHeight() * 7;
    table.getTable().setLayoutData(data);
    table.getTable().setFont(font);

    table.getTable().setToolTipText(null);
    table.setContentProvider(new ContentProvider());
    table.setInput(this);

    createButtons(pageComponent);
    updateButtons();

    table.addSelectionChangedListener(e -> updateButtons());
    return pageComponent;
  }

  private void remove() {
    IStructuredSelection selection = (IStructuredSelection) table.getSelection();

    int idx = table.getTable().getSelectionIndex();
    Iterator<?> elements = selection.iterator();
    while (elements.hasNext()) {
      RuleKey data = (RuleKey) elements.next();
      excludedRules.remove(data);
    }
    table.refresh();

    int count = table.getTable().getItemCount();
    if (count > 0) {
      if (idx < 0) {
        table.getTable().select(0);
      } else if (idx < count) {
        table.getTable().select(idx);
      } else {
        table.getTable().select(count - 1);
      }
    }
    updateButtons();
  }

  protected Composite createButtons(Composite innerParent) {
    GridLayout layout;
    Composite buttons = new Composite(innerParent, SWT.NONE);
    buttons.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
    layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    buttons.setLayout(layout);

    removeButton = new Button(buttons, SWT.PUSH);
    removeButton.setText("Remove");
    removeButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    removeButton.addListener(SWT.Selection, e -> remove());
    return buttons;
  }

  protected void updateButtons() {
    IStructuredSelection selection = (IStructuredSelection) table.getSelection();
    int index = excludedRules.indexOf(selection.getFirstElement());

    removeButton.setEnabled(index >= 0);
  }

  private List<RuleKey> loadExclusions() {
    return new ArrayList<>(PreferencesUtils.getExcludedRules());
  }

  @Override
  public boolean performOk() {
    PreferencesUtils.setExcludedRules(this.excludedRules);
    // TODO what should be the trigger type?
    JobUtils.scheduleAnalysisOfOpenFiles((ISonarLintProject) null, TriggerType.EDITOR_CHANGE);
    return true;
  }

  @Override
  protected void performDefaults() {
    this.excludedRules = new ArrayList<>();
    table.refresh();
  }

  private class ContentProvider implements IStructuredContentProvider {
    @Override
    public Object[] getElements(Object inputElement) {
      return excludedRules.toArray();
    }
  }

  private static class RuleKeyLabelProvider extends CellLabelProvider {
    @Override
    public void update(ViewerCell cell) {
      RuleKey ruleKey = (RuleKey) cell.getElement();
      cell.setText(ruleKey.toString());
    }
  }

  private static class DescriptionLabelProvider extends CellLabelProvider {
    @Override
    public void update(ViewerCell cell) {
      // TODO get the rule description somehow
      cell.setText("TODO");
    }
  }
}
