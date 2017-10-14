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
package org.eclipse.andmore.android.wizards.elements;

import org.eclipse.andmore.android.AndroidPlugin;
import org.eclipse.andmore.android.SdkUtils;
import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.andmore.android.model.AndroidProject;
import org.eclipse.andmore.android.model.IWizardModel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.android.sdklib.IAndroidTarget;

/**
 * SDK Selector for New Android Project Wizards
 */
public class SdkTargetSelector extends Composite {
	private Table table;

	private Label mDescription;

	final private AndroidProject project;

	private IAndroidTarget selection = null;

	/**
	 * A selection listener that will check/uncheck items when they are
	 * double-clicked
	 */
	private final SelectionListener listener = new SelectionListener() {
		/** Default selection means double-click on "most" platforms */
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			if (e.item instanceof TableItem) {
				TableItem i = (TableItem) e.item;
				i.setChecked(!i.getChecked());
				enforceSingleSelection(i);
				updateDescription(i);
				IAndroidTarget newSelection = getSelection();
				project.setSdkTarget(newSelection);

				if (newSelection != null) {
					project.setMinSdkVersion(getSelection().getVersion().getApiString());
				}
				notifyListeners(IWizardModel.MODIFIED, new Event());
			}

		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			if (e.item instanceof TableItem) {
				TableItem i = (TableItem) e.item;
				enforceSingleSelection(i);
				updateDescription(i);
				IAndroidTarget newSelection = getSelection();
				project.setSdkTarget(newSelection);
				selection = newSelection;
				project.setSample(null);

				/*
				 * if ((newSelection != null) &&
				 * !selection.getFullName().equals(newSelection.getFullName()))
				 * {
				 * 
				 * }
				 */

				notifyListeners(IWizardModel.MODIFIED, new Event());
			}

		}

		/**
		 * If we're not in multiple selection mode, uncheck all other items when
		 * this one is selected.
		 */
		private void enforceSingleSelection(TableItem item) {
			if (item.getChecked()) {
				Table parentTable = item.getParent();
				for (TableItem i2 : parentTable.getItems()) {
					if ((i2 != item) && i2.getChecked()) {
						i2.setChecked(false);
					}
				}
			}
		}
	};

	/**
	 * Table Tool Tip Listener
	 */
	private final Listener toolTipListener = new Listener() {
		@Override
		public void handleEvent(Event event) {
			switch (event.type) {
			case SWT.MouseHover:
				updateDescription(table.getItem(new Point(event.x, event.y)));
				break;
			case SWT.Selection:
				if (event.item instanceof TableItem) {
					updateDescription((TableItem) event.item);
				}
				break;
			default:
				return;
			}
		}
	};

	/**
	 * Creates a new SDK Target Selector.
	 *
	 * @param parent
	 *            The parent composite where the selector will be added.
	 * @param project
	 *            the android project
	 */
	public SdkTargetSelector(Composite parent, AndroidProject project) {
		super(parent, SWT.NONE);
		this.project = project;

		createContents(parent);
	}

	/**
	 * Create Contents
	 * 
	 * @param parent
	 */
	private void createContents(Composite parent) {
		setLayout(new GridLayout());
		setLayoutData(new GridData(GridData.FILL_BOTH));
		setFont(parent.getFont());

		table = new Table(this, SWT.CHECK | SWT.FULL_SELECTION | SWT.SINGLE | SWT.BORDER);
		table.setHeaderVisible(true);
		table.setLinesVisible(false);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));

		mDescription = new Label(this, SWT.WRAP);
		mDescription.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		createColumns(table);
		table.addSelectionListener(listener);
		fillTable();
		setupTooltip(table);
	}

	/**
	 * Create Table Columns
	 * 
	 * @param table
	 */
	private void createColumns(final Table table) {
		// create the table columns
		final TableColumn nameColumn = new TableColumn(table, SWT.NONE);
		nameColumn.setText(AndroidNLS.UI_SdkTargetSelector_SdkTargetNameColumn);
		final TableColumn vendorColumn = new TableColumn(table, SWT.NONE);
		vendorColumn.setText(AndroidNLS.UI_SdkTargetSelector_VendorNameColumn);
		final TableColumn apiColumn = new TableColumn(table, SWT.NONE);
		apiColumn.setText(AndroidNLS.UI_SdkTargetSelector_APILevelColumn);
		final TableColumn sdkColumn = new TableColumn(table, SWT.NONE);
		sdkColumn.setText(AndroidNLS.UI_SdkTargetSelector_SDKVersionColumn);

		table.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				Rectangle r = table.getClientArea();
				nameColumn.setWidth((r.width * 25) / 100); // 25%
				vendorColumn.setWidth((r.width * 50) / 100); // 50%
				apiColumn.setWidth((r.width * 15) / 100); // 15%
				sdkColumn.setWidth((r.width * 10) / 100); // 10%
			}
		});
	}

	/**
	 * Return table selection.
	 * 
	 * @return
	 */
	protected IAndroidTarget getSelection() {
		IAndroidTarget selectedItem = null;
		for (TableItem item : table.getItems()) {
			Object data = item.getData();
			if (item.getChecked() && (data instanceof IAndroidTarget)) {
				selectedItem = (IAndroidTarget) data;
				break;
			}
		}
		return selectedItem;
	}

	/**
	 * Fills the table with all SDK targets.
	 */
	private void fillTable() {
		// get the targets from the sdk
		IAndroidTarget[] targets = null;
		if (SdkUtils.getCurrentSdk() != null) {
			targets = SdkUtils.getAllTargets();
		} else {
			final Runnable listener = new Runnable() {
				@Override
				public void run() {
					table.getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							table.removeAll();
							fillTable();
							AndroidPlugin.getDefault().removeSDKLoaderListener(this);
						}
					});
				}
			};
			AndroidPlugin.getDefault().addSDKLoaderListener(listener);
			table.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent e) {
					AndroidPlugin.getDefault().removeSDKLoaderListener(listener);
				}
			});
		}

		if ((targets != null) && (targets.length > 0)) {
			table.setEnabled(true);
			for (IAndroidTarget target : targets) {
				TableItem item = new TableItem(table, SWT.NONE);
				item.setData(target);
				item.setText(0, target.getName());
				item.setText(1, target.getVendor());
				item.setText(2, target.getVersion().getApiString());
				item.setText(3, target.getVersionName() == null ? target.getVersion().getApiString() : target.getVersionName());
				if (target == project.getSdkTarget()) {
					item.setChecked(true);
					selection = target;
				}
			}
		} else {
			table.setEnabled(false);
			TableItem item = new TableItem(table, SWT.NONE);
			item.setData(null);
			item.setText(0, AndroidNLS.UI_SdkTargetSelector_EmptyValue);
			item.setText(1, AndroidNLS.UI_SdkTargetSelector_NoTargetAvailable);
			item.setText(2, AndroidNLS.UI_SdkTargetSelector_EmptyValue);
			item.setText(3, AndroidNLS.UI_SdkTargetSelector_EmptyValue);
		}
	}

	/**
	 * Add Tool tip for table
	 * 
	 * @param table
	 */
	private void setupTooltip(final Table table) {
		table.addListener(SWT.Dispose, toolTipListener);
		table.addListener(SWT.MouseHover, toolTipListener);
		table.addListener(SWT.MouseMove, toolTipListener);
		table.addListener(SWT.KeyDown, toolTipListener);
	}

	/**
	 * Updates the description label
	 */
	private void updateDescription(TableItem item) {
		if (item != null) {
			Object data = item.getData();
			if (data instanceof IAndroidTarget) {
				String newTooltip = ((IAndroidTarget) data).getDescription();
				mDescription.setText(newTooltip == null ? "" : newTooltip); //$NON-NLS-1$
			}
		} else {
			mDescription.setText("");
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (this.getEnabled() != enabled) {
			table.setEnabled(enabled);
			mDescription.setEnabled(enabled);
			super.setEnabled(enabled);
		}
	}
}
