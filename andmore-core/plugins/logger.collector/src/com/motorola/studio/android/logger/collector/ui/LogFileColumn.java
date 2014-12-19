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
package com.motorola.studio.android.logger.collector.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.android.logger.collector.core.ILogFile;
import org.eclipse.andmore.android.logger.collector.core.internal.CollectLogFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.motorola.studio.android.logger.collector.util.LoggerCollectorMessages;
import com.motorola.studio.android.logger.collector.util.PlatformException;
import com.motorola.studio.android.logger.collector.util.WidgetsUtil;

/**
 * This class visually represents a log file Table View.
 */
public class LogFileColumn extends Composite {

	/**
	 * The table of log files.
	 */
	private Table tableLogFile = null;

	/**
     * 
     */
	CollectLogFile collectLogFile = null;

	/**
	 * Public Constructor.
	 * 
	 * @param parent
	 *            Parent this Composite.
	 * @param style
	 *            Composite Style.
	 */
	public LogFileColumn(Composite parent, int style) {
		super(parent, style);
		collectLogFile = new CollectLogFile();
		initialize();
	}

	/**
	 * This method contains property of this composite.
	 */
	private void initialize() {
		GridLayout gridLayoutTableView = new GridLayout();
		gridLayoutTableView.horizontalSpacing = 0;
		gridLayoutTableView.marginWidth = 0;
		gridLayoutTableView.marginHeight = 0;
		gridLayoutTableView.verticalSpacing = 0;
		GridData gridDatagridLayout = new GridData(GridData.FILL_VERTICAL | SWT.TOP);
		this.setBackgroundMode(SWT.NONE);
		this.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		this.setLayoutData(gridDatagridLayout);
		this.setLayout(gridLayoutTableView);

		GridData gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_BOTH);
		gridData.widthHint = 230;
		gridData.heightHint = 300;
		this.setLayoutData(gridData);

		refresh();
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void refresh() {
		GridData gridDataTableView = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_BOTH);
		gridDataTableView.widthHint = 236;
		gridDataTableView.heightHint = 300;
		if (tableLogFile == null) {
			tableLogFile = new Table(this, SWT.CHECK | SWT.BORDER);
			tableLogFile.setLayoutData(gridDataTableView);
		}
		tableLogFile.removeAll();
		try {
			for (ILogFile log : collectLogFile.getLogFileList()) {
				TableItem tableItem = new TableItem(tableLogFile, SWT.NONE);
				tableItem.setText(log.getLogName());
				tableItem.setData(log);
			}
		} catch (Exception e) {
			MessageDialog.openError(getShell(),
					LoggerCollectorMessages.getInstance().getString("logger.collector.wizard.page.title"), //$NON-NLS-1$
					LoggerCollectorMessages.getInstance().getString("error.logger.collector.mount.tableview")); //$NON-NLS-1$
		}
		packTableColumns();
		checkAll(true);
	}

	/**
	 * This method is responsible to pack tree columns
	 */
	private void packTableColumns() {
		// Pack the columns
		TableColumn[] columns = tableLogFile.getColumns();
		for (int i = 0, n = columns.length; i < n; i++) {
			columns[i].pack();
		}
	}

	/**
	 * This method collects and zips selected log files.
	 * 
	 * @param directory
	 *            The output directory
	 * @param filename
	 *            The output file name
	 * @return if collects successfully
	 * @throws PlatformException
	 */
	public boolean collect(String filename) throws PlatformException {
		return new CollectLogFile().zipLogFiles(filename, WidgetsUtil.getCheckedLeafItems(this.tableLogFile));
	}

	/**
	 * This method tests if the table view has nodes and if there is selected
	 * nodes.
	 * 
	 * @return true if the table view has nodes and if there is selected nodes.
	 */
	public boolean hasNodeSelected() {
		return WidgetsUtil.getCheckedLeafItems(this.tableLogFile).size() > 0;
	}

	/**
	 * This method collects and compacts selected log files.
	 * 
	 * @param directory
	 *            The output directory
	 * @param filename
	 *            The output file name
	 * @return if collects successfully
	 */
	public ArrayList<String> selectedLogFilesExist() {
		List<TableItem> list = WidgetsUtil.getCheckedLeafItems(this.tableLogFile);
		ArrayList<String> notFoundItems = new ArrayList<String>();
		for (TableItem tableItem : list) {
			Object data = tableItem.getData();
			if (data instanceof ILogFile) {
				ILogFile logFile = (ILogFile) data;
				for (IPath path : logFile.getLogFilePath()) {
					if (!WidgetsUtil.fileExist(path.toOSString())) {
						notFoundItems.add(logFile.getLogName());
					}

				}
			}
		}
		return notFoundItems;
	}

	/**
	 * This method adds a TableListener
	 * 
	 * @param eventType
	 *            Type of event
	 * @param listener
	 *            Listener
	 */
	public void addTableListener(int eventType, Listener listener) {
		this.tableLogFile.addListener(eventType, listener);
	}

	public void checkAll(boolean selectionValue) {
		for (TableItem item : tableLogFile.getItems()) {
			item.setChecked(selectionValue);
		}

	}
}
