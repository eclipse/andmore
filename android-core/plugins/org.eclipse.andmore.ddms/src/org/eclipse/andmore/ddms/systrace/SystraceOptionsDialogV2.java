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

package org.eclipse.andmore.ddms.systrace;

import com.android.ddmuilib.TableHelper;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SystraceOptionsDialogV2 extends TitleAreaDialog implements ISystraceOptionsDialog {
	private static final String TITLE = "Android System Trace";
	private static final String DEFAULT_MESSAGE = "Settings to use while capturing system level trace";
	private static final String DEFAULT_TRACE_FNAME = "trace.html"; //$NON-NLS-1$

	private Text mDestinationText;
	private String mDestinationPath;
	private Text mTraceDurationText;
	private Text mTraceBufferSizeText;
	private Combo mTraceAppCombo;

	private static String sSaveToFolder = System.getProperty("user.home"); //$NON-NLS-1$
	private static String sTraceDuration = "";
	private static String sTraceBufferSize = "";
	private static Set<String> sEnabledTags = new HashSet<String>();
	private static String sLastSelectedApp = null;

	private final List<SystraceTag> mSupportedTags;
	private final List<String> mCurrentApps;

	private final SystraceOptions mOptions = new SystraceOptions();
	private Table mTable;

	public SystraceOptionsDialogV2(Shell parentShell, List<SystraceTag> tags, List<String> apps) {
		super(parentShell);
		mSupportedTags = tags;
		mCurrentApps = apps;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle(TITLE);
		setMessage(DEFAULT_MESSAGE);

		Composite c = new Composite(parent, SWT.BORDER);
		c.setLayout(new GridLayout(3, false));
		c.setLayoutData(new GridData(GridData.FILL_BOTH));

		Label l = new Label(c, SWT.NONE);
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		l.setText("Destination File: ");

		mDestinationText = new Text(c, SWT.BORDER);
		mDestinationText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		mDestinationText.setText(sSaveToFolder + File.separator + DEFAULT_TRACE_FNAME);

		final Button browse = new Button(c, SWT.NONE);
		browse.setText("Browse...");
		browse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String path = openBrowseDialog(browse.getShell());
				if (path != null)
					mDestinationText.setText(path);
			}
		});

		Label lblTraceDurationseconds = new Label(c, SWT.NONE);
		lblTraceDurationseconds.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblTraceDurationseconds.setText("Trace duration (seconds): ");

		mTraceDurationText = new Text(c, SWT.BORDER);
		mTraceDurationText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		mTraceDurationText.setText(sTraceDuration);

		Label lblTraceBufferSize = new Label(c, SWT.NONE);
		lblTraceBufferSize.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblTraceBufferSize.setText("Trace Buffer Size (kb): ");

		mTraceBufferSizeText = new Text(c, SWT.BORDER);
		mTraceBufferSizeText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		mTraceBufferSizeText.setText(sTraceBufferSize);

		Label lblTraceAppName = new Label(c, SWT.NONE);
		lblTraceAppName.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblTraceAppName.setText("Enable Application Traces from: ");

		mTraceAppCombo = new Combo(c, SWT.DROP_DOWN | SWT.READ_ONLY);
		mTraceAppCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		String[] items = new String[mCurrentApps.size() + 1];
		items[0] = "None";
		for (int i = 0; i < mCurrentApps.size(); i++) {
			items[i + 1] = mCurrentApps.get(i);
		}
		mTraceAppCombo.setItems(items);
		if (sLastSelectedApp != null) {
			mTraceAppCombo.setText(sLastSelectedApp);
		} else {
			mTraceAppCombo.select(0);
		}

		Label separator = new Label(c, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		separator.setLayoutData(gd);

		ModifyListener m = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				validateFields();
			}
		};

		mDestinationText.addModifyListener(m);
		mTraceBufferSizeText.addModifyListener(m);
		mTraceDurationText.addModifyListener(m);

		l = new Label(c, SWT.NONE);
		l.setText("Select tags to enable: ");
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));

		mTable = new Table(c, SWT.CHECK | SWT.BORDER);
		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		mTable.setLayoutData(gd);
		mTable.setHeaderVisible(false);
		mTable.setLinesVisible(false);

		for (SystraceTag tag : mSupportedTags) {
			TableItem item = new TableItem(mTable, SWT.NONE);
			item.setText(tag.info);
			item.setChecked(sEnabledTags.contains(tag.tag));
		}

		TableHelper.createTableColumn(mTable, "TagHeaderNotDisplayed", //$NON-NLS-1$
				SWT.LEFT, "SampleTagForColumnLengthCalculation", //$NON-NLS-1$
				null, null);

		return c;
	}

	private void validateFields() {
		// validate trace destination path
		String msg = validatePath(mDestinationText.getText());
		if (msg != null) {
			setErrorMessage(msg);
			getButton(OK).setEnabled(false);
			return;
		}

		// validate the trace duration
		if (!validateInteger(mTraceDurationText.getText())) {
			setErrorMessage("Trace Duration should be a valid integer (seconds)");
			getButton(OK).setEnabled(false);
			return;
		}

		// validate the trace buffer size
		if (!validateInteger(mTraceBufferSizeText.getText())) {
			setErrorMessage("Trace Buffer Size should be a valid integer (kilobytes)");
			getButton(OK).setEnabled(false);
			return;
		}

		getButton(OK).setEnabled(true);
		setErrorMessage(null);
	}

	private boolean validateInteger(String text) {
		if (text == null || text.isEmpty()) {
			return true;
		}

		try {
			Integer.parseInt(text);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private String validatePath(String path) {
		if (path == null || path.isEmpty()) {
			return null;
		}

		File f = new File(path);
		if (f.isDirectory()) {
			return String.format("The path '%s' points to a folder", path);
		}

		if (!f.exists()) { // if such a file doesn't exist, make sure the parent
							// folder is valid
			if (!f.getParentFile().isDirectory()) {
				return String.format("That path '%s' is not a valid folder.", f.getParent());
			}
		}

		return null;
	}

	private String openBrowseDialog(Shell parentShell) {
		FileDialog fd = new FileDialog(parentShell, SWT.SAVE);

		fd.setText("Save To");
		fd.setFileName(DEFAULT_TRACE_FNAME);

		fd.setFilterPath(sSaveToFolder);
		fd.setFilterExtensions(new String[] { "*.html" }); //$NON-NLS-1$

		String fname = fd.open();
		if (fname == null || fname.trim().length() == 0) {
			return null;
		}

		sSaveToFolder = fd.getFilterPath();
		return fname;
	}

	@Override
	protected void okPressed() {
		mDestinationPath = mDestinationText.getText().trim();

		sTraceDuration = mTraceDurationText.getText();
		if (!sTraceDuration.isEmpty()) {
			mOptions.mTraceDuration = Integer.parseInt(sTraceDuration);
		}

		sTraceBufferSize = mTraceBufferSizeText.getText();
		if (!sTraceBufferSize.isEmpty()) {
			mOptions.mTraceBufferSize = Integer.parseInt(sTraceBufferSize);
		}

		if (mTraceAppCombo.getSelectionIndex() != 0) {
			mOptions.mTraceApp = sLastSelectedApp = mTraceAppCombo.getText();
		}

		sEnabledTags.clear();
		for (int i = 0; i < mTable.getItemCount(); i++) {
			TableItem it = mTable.getItem(i);
			if (it.getChecked()) {
				sEnabledTags.add(mSupportedTags.get(i).tag);
			}
		}

		super.okPressed();
	}

	@Override
	public ISystraceOptions getSystraceOptions() {
		return mOptions;
	}

	@Override
	public String getTraceFilePath() {
		return mDestinationPath;
	}

	private class SystraceOptions implements ISystraceOptions {
		private int mTraceBufferSize;
		private int mTraceDuration;
		private String mTraceApp;

		@Override
		public String getTags() {
			return null;
		}

		@Override
		public String getOptions() {
			StringBuilder sb = new StringBuilder(5 * mSupportedTags.size());

			if (mTraceApp != null) {
				sb.append("-a "); //$NON-NLS-1$
				sb.append(mTraceApp);
				sb.append(' ');
			}

			if (mTraceDuration > 0) {
				sb.append("-t"); //$NON-NLS-1$
				sb.append(mTraceDuration);
				sb.append(' ');
			}

			if (mTraceBufferSize > 0) {
				sb.append("-b "); //$NON-NLS-1$
				sb.append(mTraceBufferSize);
				sb.append(' ');
			}

			for (String s : sEnabledTags) {
				sb.append(s);
				sb.append(' ');
			}

			return sb.toString().trim();
		}
	}
}
