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

package org.eclipse.andmore.ddms;

import com.android.ddmlib.Log.LogLevel;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class LogCatMonitorDialog extends TitleAreaDialog {
	private static final String TITLE = "Auto Monitor Logcat";
	private static final String DEFAULT_MESSAGE = "Would you like ADT to automatically monitor logcat \n"
			+ "output for messages from applications in the workspace?";

	private boolean mShouldMonitor = true;

	private static final String[] LOG_PRIORITIES = new String[] { LogLevel.VERBOSE.getStringValue(),
			LogLevel.DEBUG.getStringValue(), LogLevel.INFO.getStringValue(), LogLevel.WARN.getStringValue(),
			LogLevel.ERROR.getStringValue(), LogLevel.ASSERT.getStringValue(), };
	private static final int ERROR_PRIORITY_INDEX = 4;

	private String mMinimumLogPriority = LOG_PRIORITIES[ERROR_PRIORITY_INDEX];

	public LogCatMonitorDialog(Shell parentShell) {
		super(parentShell);
		setHelpAvailable(false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle(TITLE);
		setMessage(DEFAULT_MESSAGE);

		parent = (Composite) super.createDialogArea(parent);
		Composite c = new Composite(parent, SWT.BORDER);
		c.setLayout(new GridLayout(2, false));
		GridData gd_c = new GridData(GridData.FILL_BOTH);
		gd_c.grabExcessVerticalSpace = false;
		gd_c.grabExcessHorizontalSpace = false;
		c.setLayoutData(gd_c);

		final Button disableButton = new Button(c, SWT.RADIO);
		disableButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		disableButton.setText("No, do not monitor logcat output.");

		final Button enableButton = new Button(c, SWT.RADIO);
		enableButton.setText("Yes, monitor logcat and display logcat view if there are\n"
				+ "messages with priority higher than:");
		enableButton.setSelection(true);

		final Combo levelCombo = new Combo(c, SWT.READ_ONLY | SWT.DROP_DOWN);
		levelCombo.setItems(LOG_PRIORITIES);
		levelCombo.select(ERROR_PRIORITY_INDEX);

		SelectionListener s = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (e.getSource() == enableButton) {
					mShouldMonitor = enableButton.getSelection();
					levelCombo.setEnabled(mShouldMonitor);
				} else if (e.getSource() == levelCombo) {
					mMinimumLogPriority = LOG_PRIORITIES[levelCombo.getSelectionIndex()];
				}
			}
		};

		levelCombo.addSelectionListener(s);
		enableButton.addSelectionListener(s);

		return parent;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// Only need OK button
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}

	public boolean shouldMonitor() {
		return mShouldMonitor;
	}

	public String getMinimumPriority() {
		return mMinimumLogPriority;
	}
}
