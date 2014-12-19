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
package org.eclipse.andmore.android.devices;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Collator;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

public abstract class AbstractDevicePropertyPage extends PropertyPage implements IWorkbenchPropertyPage {

	private Properties propValues;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse
	 * .swt.widgets.Composite)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected Control createContents(Composite parent) {

		final Composite parentComposite = new Composite(parent, SWT.NONE);
		parentComposite.setLayout(new GridLayout(2, false));
		parentComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		final Table propTable = new Table(parentComposite, SWT.FILL | SWT.FULL_SELECTION);
		final GridData data = new GridData();
		data.horizontalSpan = 2;
		data.heightHint = 150;
		data.grabExcessVerticalSpace = true;
		data.grabExcessHorizontalSpace = true;
		data.horizontalAlignment = SWT.FILL;
		data.verticalAlignment = SWT.FILL;

		propTable.setLayoutData(data);
		propTable.setLinesVisible(true);

		final TableColumn keyColumn = new TableColumn(propTable, SWT.LEFT);
		keyColumn.setText(AndroidNLS.AbstractDevicePropertyPage_Property);
		keyColumn.setWidth(200);
		TableColumn valueColumn = new TableColumn(propTable, SWT.LEFT);
		valueColumn.setText(AndroidNLS.AbstractDevicePropertyPage_Value);
		valueColumn.setWidth(200);
		propTable.setHeaderVisible(true);

		propValues = getDeviceProperties();

		if ((propValues != null) && !propValues.isEmpty()) {
			for (Map.Entry entry : propValues.entrySet()) {
				TableItem item = new TableItem(propTable, SWT.NONE);
				item.setText(new String[] { (String) entry.getKey(), (String) entry.getValue() });
			}

		}

		Button button = new Button(parentComposite, SWT.PUSH);
		final GridData buttonData = new GridData();
		buttonData.horizontalSpan = 1;
		buttonData.grabExcessVerticalSpace = false;
		buttonData.grabExcessHorizontalSpace = true;
		buttonData.horizontalAlignment = SWT.END;
		buttonData.verticalAlignment = SWT.CENTER;
		button.setLayoutData(buttonData);
		button.setText(AndroidNLS.AbstractDevicePropertyPage_CVS_Export);
		button.pack();

		if (propValues.isEmpty()) {
			button.setEnabled(false);
		}

		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog selectionDialog = new FileDialog(getShell(), SWT.SAVE | SWT.SHEET);
				selectionDialog.setFilterExtensions(new String[] { "*.csv" //$NON-NLS-1$
				});
				selectionDialog.setFilterPath(ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString());
				String csvPath = selectionDialog.open();
				File csvFile = new File(csvPath);
				String fileName = csvFile.getName();
				if ((csvFile != null) && !"".equals(fileName)) //$NON-NLS-1$
				{
					int extensionIdx = fileName.lastIndexOf("."); //$NON-NLS-1$
					if ((extensionIdx < 0) || "".equals(fileName.substring(extensionIdx))) //$NON-NLS-1$
					{
						csvFile = new File(csvFile.getAbsolutePath() + ".csv"); //$NON-NLS-1$
					}
					BufferedWriter outputWriter = null;
					try {
						outputWriter = new BufferedWriter(new FileWriter(csvFile));
						Iterator keyIt = propValues.keySet().iterator();
						while (keyIt.hasNext()) {
							String key = (String) keyIt.next();
							String value = (String) propValues.get(key);
							outputWriter.append(key);
							outputWriter.append(","); //$NON-NLS-1$
							if (value.contains(",")) //$NON-NLS-1$
							{
								outputWriter.append("\""); //$NON-NLS-1$
								outputWriter.append(value);
								outputWriter.append("\""); //$NON-NLS-1$
							} else {
								outputWriter.append(value);
							}
							outputWriter.newLine();
						}
					} catch (FileNotFoundException fnf) {
						EclipseUtils.showErrorDialog(AndroidNLS.AbstractDevicePropertyPage_Error_Title,
								AndroidNLS.AbstractDevicePropertyPage_Error_Message);
					} catch (IOException ioex) {
						EclipseUtils.showErrorDialog(AndroidNLS.AbstractDevicePropertyPage_Error_Title,
								AndroidNLS.AbstractDevicePropertyPage_Error_Message);
					} finally {
						if (outputWriter != null) {
							try {
								outputWriter.flush();
								outputWriter.close();
							} catch (IOException e1) {
								StudioLogger.error("Could not close stream. " + e1.getMessage());
							}
						}
					}
				} else {
					EclipseUtils.showErrorDialog(AndroidNLS.AbstractDevicePropertyPage_Error_Title,
							AndroidNLS.AbstractDevicePropertyPage_Error_Message_Valid_File);
				}
			}
		});

		propTable.pack();
		parentComposite.pack();

		SelectionAdapter sortListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				TableColumn sortColumn = propTable.getSortColumn();
				TableColumn currentColumn = (TableColumn) e.widget;
				int columnIndex = (currentColumn == keyColumn ? 0 : 1);

				int dir = propTable.getSortDirection();
				if (sortColumn == currentColumn) {
					if (dir == SWT.UP) {
						dir = SWT.DOWN;
					} else {
						dir = SWT.UP;
					}
				} else {
					dir = SWT.UP;
				}

				sortTable(propTable, columnIndex, sortColumn, currentColumn, dir);
			}
		};
		keyColumn.addSelectionListener(sortListener);
		valueColumn.addSelectionListener(sortListener);
		propTable.setSortColumn(keyColumn);
		propTable.setSortDirection(SWT.UP);

		sortTable(propTable, 0, keyColumn, keyColumn, SWT.UP);

		noDefaultAndApplyButton();
		return parentComposite;

	}

	abstract protected Properties getDeviceProperties();

	private void sortTable(Table table, int columnIndex, TableColumn sortColumn, TableColumn currentColumn, int dir) {
		table.setSortDirection(dir);
		TableItem[] items = table.getItems();
		Collator collator = Collator.getInstance(Locale.getDefault());
		int index = columnIndex;
		for (int i = 1; i < items.length; i++) {
			String value1 = items[i].getText(index);
			for (int j = 0; j < i; j++) {
				String value2 = items[j].getText(index);
				if (dir == SWT.UP) {
					if (collator.compare(value1, value2) < 0) {
						String[] values = { items[i].getText(0), items[i].getText(1) };
						items[i].dispose();
						TableItem item = new TableItem(table, SWT.NONE, j);
						item.setText(values);
						items = table.getItems();
						break;
					}
				} else {
					if (collator.compare(value1, value2) > 0) {
						String[] values = { items[i].getText(0), items[i].getText(1) };
						items[i].dispose();
						TableItem item = new TableItem(table, SWT.NONE, j);
						item.setText(values);
						items = table.getItems();
						break;
					}
				}
			}
		}
		table.setSortColumn(currentColumn);
	}
}
