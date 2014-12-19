/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.andmore.android.generateviewbylayout.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.andmore.android.codeutils.CodeUtilsActivator;
import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.generateviewbylayout.codegenerators.SaveStateCodeGenerator;
import org.eclipse.andmore.android.generateviewbylayout.model.LayoutNode;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;

/**
 * GUI to enable selection of which Android items will be inserted into activity
 * or fragment
 */
public class ChooseLayoutItemsDialog extends AbstractLayoutItemsDialog {
	private final String DIALOG_HELP = CodeUtilsActivator.PLUGIN_ID + ".generate-code-from-layout-dialog"; //$NON-NLS-1$

	private Button generateListeners;

	private boolean hasGuiItemsWithoutId = false;

	private final Map<TableItem, TableEditor> itemToEditorMap;

	private static final String WIZARD_IMAGE_PATH = "icons/wizban/fill_activity_ban.png"; //$NON-NLS-1$

	public ChooseLayoutItemsDialog(Shell parentShell) {
		super(parentShell, CodeUtilsNLS.ChooseLayoutItemsDialog_DefaultMessage,
				CodeUtilsNLS.UI_ChooseLayoutItemsDialog_Dialog_Title,
				CodeUtilsNLS.ChooseLayoutItemsDialog_FillActivityBasedOnLayout, CodeUtilsActivator.getImageDescriptor(
						WIZARD_IMAGE_PATH).createImage());
		setHelpID(DIALOG_HELP);
		itemToEditorMap = new HashMap<TableItem, TableEditor>();
	}

	@Override
	protected void createCustomContentArea(Composite parent) {

		Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false, 2, 1));

		createCheckboxArea(parent);

	}

	@Override
	protected void createColumns(TableViewer viewer) {
		super.createColumns(viewer);
		TableViewerColumn column = createTableViewerColumn(viewer, CodeUtilsNLS.ChooseLayoutItemsDialog_SaveState, 80,
				3);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return null;
			}
		});
		column.getColumn().setToolTipText(CodeUtilsNLS.ChooseLayoutItemsDialog_SaveStateTooltip);

	}

	@Override
	protected void populateViewer() {
		super.populateViewer();
		populateSaveStateColumn();
	}

	private void populateSaveStateColumn() {
		if (getViewer() != null) {
			itemToEditorMap.clear();
			for (final TableItem item : getViewer().getTable().getItems()) {
				LayoutNode node = (LayoutNode) item.getData();

				if (SaveStateCodeGenerator.canGenerateSaveStateCode(node)) {

					final TableEditor editor = new TableEditor(getViewer().getTable());
					editor.setColumn(3);
					editor.horizontalAlignment = SWT.CENTER;
					editor.grabHorizontal = false;
					editor.minimumWidth = getViewer().getTable().getColumn(3).getWidth() < 20 ? getViewer().getTable()
							.getColumn(3).getWidth() : 20;
					getViewer().getTable().getColumn(3).addControlListener(new ControlAdapter() {

						@Override
						public void controlResized(ControlEvent e) {
							editor.minimumWidth = getViewer().getTable().getColumn(3).getWidth() < 20 ? getViewer()
									.getTable().getColumn(3).getWidth() : 20;
						}
					});
					final Button checkbox = new Button(getViewer().getTable(), SWT.CHECK);
					checkbox.setEnabled(false);
					checkbox.pack();
					final SelectionListener listener = new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							if ((e.detail & SWT.CHECK) != 0) {
								if ((e.item instanceof TableItem) && (editor.getItem() == e.item)) {
									checkbox.setEnabled(((TableItem) e.item).getChecked());
								}
							}
						}
					};
					checkbox.setSelection(node.getSaveState());
					getViewer().getTable().addSelectionListener(listener);
					checkbox.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							LayoutNode node = (LayoutNode) editor.getItem().getData();
							node.setSaveState(checkbox.getSelection());
						}
					});
					editor.setEditor(checkbox, item, 3);

					item.addDisposeListener(new DisposeListener() {

						@Override
						public void widgetDisposed(DisposeEvent e) {
							getViewer().getTable().removeSelectionListener(listener);
							editor.getEditor().dispose();
							editor.dispose();
						}
					});
					itemToEditorMap.put(item, editor);
				}
			}
		}
	}

	@Override
	protected void itemCheckStateChanged(TableItem item) {
		super.itemCheckStateChanged(item);
		if (itemToEditorMap.get(item) != null) {
			((Button) itemToEditorMap.get(item).getEditor()).setEnabled(item.getChecked());
		}
	}

	@Override
	protected List<LayoutNode> getGuiItemsList() {
		List<LayoutNode> completeGuiItemsList = getCodeGeneratorData().getGUIItemsForUI();
		List<LayoutNode> processedGuiItemsList = new ArrayList<LayoutNode>();
		hasGuiItemsWithoutId = false;

		for (LayoutNode guiItem : completeGuiItemsList) {
			if ((guiItem.getNodeId() != null) && (guiItem.getNodeId().length() > 0)) {
				processedGuiItemsList.add(guiItem);
			} else {
				hasGuiItemsWithoutId = true;
			}
		}

		return processedGuiItemsList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets
	 * .Shell)
	 */
	@Override
	protected void configureShell(Shell newShell) {
		newShell.setSize(640, 480);
		super.configureShell(newShell);
	}

	/**
	 * Creates GUI items for choosing whether code for listeners should be
	 * auto-generated.
	 * 
	 * @param optionsComposite
	 */
	private void createCheckboxArea(Composite parent) {
		generateListeners = new Button(parent, SWT.CHECK);
		generateListeners.setText(CodeUtilsNLS.ChooseLayoutItemsDialog_GenerateDefaultListeners);
		generateListeners.setSelection(true);
		generateListeners.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false, 2, 1));
	}

	/**
	 * Handles the enablement of the Ok button. It will only be enabled when at
	 * least one table item is checked.
	 */
	@Override
	protected void validate() {
		super.validate();
		if ((getViewer() != null) && (getErrorMessage() == null)) {

			// set the appropriate message
			String message = ""; //$NON-NLS-1$
			int messageType = IMessageProvider.NONE;

			// check if at least one table item was selected
			for (TableItem item : getViewer().getTable().getItems()) {
				LayoutNode node = (LayoutNode) item.getData();
				if (item.getChecked()
						&& (getCodeGeneratorData() != null)
						&& getCodeGeneratorData().getJavaLayoutData().getVisitor()
								.checkIfAttributeAlreadyDeclared(node, true)) {
					message = NLS.bind(CodeUtilsNLS.ChooseLayoutItemsDialog_VariableNameInUse_Error, node.getNodeId());
					messageType = IMessageProvider.ERROR;
					break;
				}
			}

			if (messageType == IMessageProvider.NONE) {

				if (getViewer().getTable().getItemCount() == 0) {
					message = CodeUtilsNLS.UI_ChooseLayoutItemsDialog_No_Gui_Items_Available;
					messageType = IMessageProvider.INFORMATION;
				} else if (hasGuiItemsWithoutId) {
					message = CodeUtilsNLS.ChooseLayoutItemsDialog_Gui_Items_Available_No_Id;
					messageType = IMessageProvider.INFORMATION;
				} else {
					message = CodeUtilsNLS.ChooseLayoutItemsDialog_DefaultMessage;
				}
			}
			this.setMessage(message, messageType);

		}
	}

	/**
	 * Each table item refers to a LayoutNode object. When user press Ok, the
	 * insertCode status of these objects are set accordingly to table
	 * selections.
	 */
	@Override
	protected void okPressed() {
		// set the insertCode for each layoutNode accordingly
		for (TableItem item : getViewer().getTable().getItems()) {
			if (item.getData() instanceof LayoutNode) {
				LayoutNode node = (LayoutNode) item.getData();
				node.setInsertCode(item.getChecked());
			}
		}
		getModifier().setGenerateDefaultListeners(generateListeners.getSelection());
		getModifier().setCodeGeneratorData(getCodeGeneratorData());
		super.okPressed();
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		getViewer().getControl().setFocus();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 */
	@Override
	protected boolean isResizable() {
		return true;
	}
}
