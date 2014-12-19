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
package com.motorola.studio.android.logger.collector.ui.wizard;

import java.util.ArrayList;
import java.util.Calendar;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import com.motorola.studio.android.logger.collector.ui.LogFileColumn;
import com.motorola.studio.android.logger.collector.util.LoggerCollectorConstants;
import com.motorola.studio.android.logger.collector.util.LoggerCollectorMessages;
import com.motorola.studio.android.logger.collector.util.WidgetsFactory;
import com.motorola.studio.android.logger.collector.util.WidgetsUtil;

/**
 * This class contains the design of collect log files wizard page.
 */
public class LoggerCollectorWizardPage extends WizardPage {

	/**
	 * Directory field to output compacted file
	 */
	private DirectoryFieldEditor logDirecotryField = null;

	private Text filenameText = null;

	public static final String LOGGER_COLLECTOR_HELP_ID = "org.eclipse.andmore.platform.logger.collector.collectlogs";

	/**
	 * Composite used to show the Log File TableColumn.
	 */
	private LogFileColumn logFileColumn = null;

	private Button selectAll = null;

	private Button unselectAll = null;

	/**
	 * Wizard composite
	 */
	private Composite composite;

	/**
	 * Field to check if user has changed the wizard
	 */
	private boolean userChangedWizard = false;

	/**
	 * The default constructor
	 * 
	 * @param pageName
	 *            The page name
	 */
	protected LoggerCollectorWizardPage(String pageName) {
		super(pageName);
		setTitle(LoggerCollectorMessages.getInstance().getString("logger.collector.wizard.page.title")); //$NON-NLS-1$
		setDescription(LoggerCollectorMessages.getInstance().getString("logger.collector.wizard.page.description")); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets
	 * .Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		composite = getComposite(parent);
		composite.setLayout(new GridLayout(1, false));
		composite.setLayoutData(new GridData(GridData.FILL_VERTICAL | SWT.TOP));

		Composite compositeTop = WidgetsFactory.createComposite(composite, 3);
		logDirecotryField = new DirectoryFieldEditor("logFileNameField", //$NON-NLS-1$
				LoggerCollectorMessages.getInstance().getString("logger.collector.wizard.page.directory") + ":", //$NON-NLS-1$ //$NON-NLS-2$
				compositeTop);

		logDirecotryField.getTextControl(compositeTop).addListener(SWT.Modify, listener);
		logDirecotryField.getTextControl(compositeTop).setTextLimit(200);

		Label filenameLabel = new Label(compositeTop, SWT.NONE);
		filenameLabel.setText(LoggerCollectorMessages.getInstance().getString("logger.collector.wizard.page.file"));//$NON-NLS-1$
		GridData layoutData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		filenameLabel.setLayoutData(layoutData);

		filenameText = new Text(compositeTop, SWT.BORDER);

		layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		filenameText.setLayoutData(layoutData);

		Composite compositeBottom = WidgetsFactory.createComposite(composite, 2);

		Label l = new Label(compositeBottom, SWT.NONE);
		l.setText(LoggerCollectorMessages.getInstance().getString("logger.collector.tableview.root"));//$NON-NLS-1$
		GridData d = new GridData(SWT.LEFT, SWT.NONE, true, false, 2, 1);
		l.setLayoutData(d);
		logFileColumn = new LogFileColumn(compositeBottom, SWT.NONE);
		logFileColumn.addTableListener(SWT.Selection, listener);

		Composite buttons = new Composite(compositeBottom, SWT.NONE);
		d = new GridData(SWT.NONE, SWT.TOP, false, true, 1, 1);
		buttons.setLayoutData(d);
		FillLayout layout = new FillLayout(SWT.VERTICAL);
		layout.spacing = 2;
		buttons.setLayout(layout);

		selectAll = new Button(buttons, SWT.PUSH);
		selectAll.setText(LoggerCollectorMessages.getInstance().getString("logger.collector.wizard.page.selectAll"));
		selectAll.addSelectionListener(new ButtonSelectionListener(true));
		selectAll.addListener(SWT.Selection, listener);
		unselectAll = new Button(buttons, SWT.PUSH);
		unselectAll
				.setText(LoggerCollectorMessages.getInstance().getString("logger.collector.wizard.page.unselectAll"));
		unselectAll.addSelectionListener(new ButtonSelectionListener(false));
		unselectAll.addListener(SWT.Selection, listener);
		setControl(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), LOGGER_COLLECTOR_HELP_ID);

		logDirecotryField.setStringValue(System.getProperty("user.home"));

		Calendar c = Calendar.getInstance();
		filenameText.setText("studio_andr_" + c.get(Calendar.YEAR) + c.get(Calendar.MONTH)
				+ c.get(Calendar.DAY_OF_MONTH) + c.get(Calendar.HOUR) + c.get(Calendar.MINUTE) + "."
				+ LoggerCollectorConstants.ZIP_FILE_EXTENSION);

		setErrorMessage(null);
	}

	/**
	 * Listener of collect log files finish button
	 */
	private final Listener listener = new Listener() {

		@Override
		public void handleEvent(Event event) {
			userChangedWizard = true;
			setPageComplete(isPageComplete());
			setErrorMessage(getErrorMessage());
		}
	};

	/**
	 * Return the composite used in the wizard.
	 * 
	 * @param parent
	 *            The parent content composite.
	 * @return The composite used in the wizard.
	 */
	protected Composite getComposite(Composite parent) {
		if (this.composite == null) {
			this.composite = WidgetsFactory.createComposite(parent);
		}
		return this.composite;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
	 */
	@Override
	public boolean isPageComplete() {
		return (getErrorMessage() == null) && !WidgetsUtil.isNullOrEmpty(this.logDirecotryField)
				&& logFileColumn.hasNodeSelected();
	}

	/**
	 * Returns the error message, null if there is no error.
	 * 
	 * @return an error message
	 */
	@Override
	public String getErrorMessage() {
		String message = null;
		String outputDir = Path.fromOSString(this.logDirecotryField.getStringValue()).toOSString();
		ArrayList<String> notFoundLogs = null;

		if (userChangedWizard) {
			if (WidgetsUtil.isNullOrEmpty(this.logDirecotryField)) {
				message = LoggerCollectorMessages.getInstance().getString("error.logger.collector.directory.empty"); //$NON-NLS-1$
			} else if (!WidgetsUtil.fileExist(outputDir)) {
				message = LoggerCollectorMessages.getInstance().getString("error.logger.collector.directory.not.found"); //$NON-NLS-1$
			} else if ((logFileColumn != null) && !logFileColumn.hasNodeSelected()) {
				message = LoggerCollectorMessages.getInstance().getString("error.logger.collector.log.not.selected"); //$NON-NLS-1$
			} else if ((logFileColumn != null) && ((notFoundLogs = logFileColumn.selectedLogFilesExist()).size() > 0)) {
				StringBuilder messageBuilder = new StringBuilder(LoggerCollectorMessages.getInstance().getString(
						"error.logger.collector.log.not.found"));
				messageBuilder.append(": ");
				for (String log : notFoundLogs) {
					messageBuilder.append(log);
					messageBuilder.append(",");
				}
				messageBuilder.deleteCharAt(messageBuilder.length() - 1);

				message = messageBuilder.toString();
			}
		}
		return message;
	}

	/**
	 * Gets the log file column (table view)
	 * 
	 * @return log file column
	 */
	public LogFileColumn getLogFileColumn() {
		return this.logFileColumn;
	}

	/**
	 * Gets the file name text
	 * 
	 * @return the file name text
	 */
	public String getFilename() {
		IPath fileName = new Path(filenameText.getText());
		if ((fileName.getFileExtension() == null)
				|| !fileName.getFileExtension().equalsIgnoreCase(LoggerCollectorConstants.ZIP_FILE_EXTENSION)) {
			fileName.addFileExtension(LoggerCollectorConstants.ZIP_FILE_EXTENSION);
		}

		return new Path(this.logDirecotryField.getStringValue()).append(fileName).toOSString();
	}

	final class ButtonSelectionListener implements SelectionListener {
		private final boolean selectionValue;

		public ButtonSelectionListener(boolean selectionValue) {
			this.selectionValue = selectionValue;
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			// do nothing
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			logFileColumn.checkAll(selectionValue);
		}

	};
}
