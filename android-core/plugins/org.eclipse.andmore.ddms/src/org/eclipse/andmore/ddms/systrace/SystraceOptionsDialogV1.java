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

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.io.File;

public class SystraceOptionsDialogV1 extends TitleAreaDialog implements ISystraceOptionsDialog {
	private static final String TITLE = "Android System Trace";
	private static final String DEFAULT_MESSAGE = "Settings to use while capturing system level trace";
	private static final String DEFAULT_TRACE_FNAME = "trace.html"; //$NON-NLS-1$

	private Text mDestinationText;
	private String mDestinationPath;
	private Text mTraceDurationText;
	private Text mTraceBufferSizeText;

	private static String sSaveToFolder = System.getProperty("user.home"); //$NON-NLS-1$
	private static String sTraceDuration = "";
	private static String sTraceBufferSize = "";

	private Button mTraceCpuFreqBtn;
	private Button mTraceCpuIdleBtn;
	private Button mTraceCpuLoadBtn;
	private Button mTraceDiskIoBtn;
	private Button mTraceKernelWorkqueuesBtn;
	private Button mTraceCpuSchedulerBtn;

	private static boolean sTraceCpuFreq;
	private static boolean sTraceCpuIdle;
	private static boolean sTraceCpuLoad;
	private static boolean sTraceDiskIo;
	private static boolean sTraceKernelWorkqueues;
	private static boolean sTraceCpuScheduler;

	private Button mGfxTagBtn;
	private Button mInputTagBtn;
	private Button mViewTagBtn;
	private Button mWebViewTagBtn;
	private Button mWmTagBtn;
	private Button mAmTagBtn;
	private Button mSyncTagBtn;
	private Button mAudioTagBtn;
	private Button mVideoTagBtn;
	private Button mCameraTagBtn;

	private static boolean sGfxTag;
	private static boolean sInputTag;
	private static boolean sViewTag;
	private static boolean sWebViewTag;
	private static boolean sWmTag;
	private static boolean sAmTag;
	private static boolean sSyncTag;
	private static boolean sAudioTag;
	private static boolean sVideoTag;
	private static boolean sCameraTag;

	private final SystraceOptions mOptions = new SystraceOptions();

	public SystraceOptionsDialogV1(Shell parentShell) {
		super(parentShell);
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

		Label separator = new Label(c, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		separator.setLayoutData(gd);

		Group grpTraceEvents = new Group(c, SWT.BORDER);
		grpTraceEvents.setLayout(new GridLayout(3, false));
		grpTraceEvents.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
		grpTraceEvents.setText("Trace Events");

		mTraceCpuFreqBtn = new Button(grpTraceEvents, SWT.CHECK);
		mTraceCpuFreqBtn.setText("CPU Frequency Changes");
		mTraceCpuFreqBtn.setSelection(sTraceCpuFreq);

		mTraceCpuIdleBtn = new Button(grpTraceEvents, SWT.CHECK);
		mTraceCpuIdleBtn.setText("CPU Idle Events");
		mTraceCpuIdleBtn.setSelection(sTraceCpuIdle);

		mTraceCpuLoadBtn = new Button(grpTraceEvents, SWT.CHECK);
		mTraceCpuLoadBtn.setText("CPU Load");
		mTraceCpuLoadBtn.setSelection(sTraceCpuLoad);

		mTraceCpuSchedulerBtn = new Button(grpTraceEvents, SWT.CHECK);
		mTraceCpuSchedulerBtn.setText("CPU Scheduler");
		mTraceCpuSchedulerBtn.setSelection(sTraceCpuScheduler);

		Group grpTraceRootEvents = new Group(c, SWT.BORDER);
		grpTraceRootEvents.setLayout(new GridLayout(2, false));
		grpTraceRootEvents.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
		grpTraceRootEvents.setText("Trace Events that require root privileges on device");

		mTraceDiskIoBtn = new Button(grpTraceRootEvents, SWT.CHECK);
		mTraceDiskIoBtn.setText("Disk I/O");
		mTraceDiskIoBtn.setSelection(sTraceDiskIo);

		mTraceKernelWorkqueuesBtn = new Button(grpTraceRootEvents, SWT.CHECK);
		mTraceKernelWorkqueuesBtn.setText("Kernel Workqueues (requires root)");
		mTraceKernelWorkqueuesBtn.setSelection(sTraceKernelWorkqueues);

		Group grpTraceTags = new Group(c, SWT.BORDER);
		grpTraceTags.setLayout(new GridLayout(5, false));
		grpTraceTags.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
		grpTraceTags.setText("Trace Tags");

		mGfxTagBtn = new Button(grpTraceTags, SWT.CHECK);
		mGfxTagBtn.setText("gfx");
		mGfxTagBtn.setSelection(sGfxTag);

		mInputTagBtn = new Button(grpTraceTags, SWT.CHECK);
		mInputTagBtn.setText("input");
		mInputTagBtn.setSelection(sInputTag);

		mViewTagBtn = new Button(grpTraceTags, SWT.CHECK);
		mViewTagBtn.setText("view");
		mViewTagBtn.setSelection(sViewTag);

		mWebViewTagBtn = new Button(grpTraceTags, SWT.CHECK);
		mWebViewTagBtn.setText("webview");
		mWebViewTagBtn.setSelection(sWebViewTag);

		mWmTagBtn = new Button(grpTraceTags, SWT.CHECK);
		mWmTagBtn.setText("wm");
		mWmTagBtn.setSelection(sWmTag);

		mAmTagBtn = new Button(grpTraceTags, SWT.CHECK);
		mAmTagBtn.setText("am");
		mAmTagBtn.setSelection(sAmTag);

		mSyncTagBtn = new Button(grpTraceTags, SWT.CHECK);
		mSyncTagBtn.setText("sync");
		mSyncTagBtn.setSelection(sSyncTag);

		mAudioTagBtn = new Button(grpTraceTags, SWT.CHECK);
		mAudioTagBtn.setText("audio");
		mAudioTagBtn.setSelection(sAudioTag);

		mVideoTagBtn = new Button(grpTraceTags, SWT.CHECK);
		mVideoTagBtn.setText("video");
		mVideoTagBtn.setSelection(sVideoTag);

		mCameraTagBtn = new Button(grpTraceTags, SWT.CHECK);
		mCameraTagBtn.setText("camera");
		mCameraTagBtn.setSelection(sCameraTag);

		Label lblTraceTagsWarning = new Label(grpTraceTags, SWT.NONE);
		lblTraceTagsWarning
				.setText("Changes to trace tags will likely need a restart of the Android framework to take effect:\n"
						+ "    $ adb shell stop\n" + "    $ adb shell start");
		lblTraceTagsWarning.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 5, 1));

		ModifyListener m = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				validateFields();
			}
		};

		mDestinationText.addModifyListener(m);
		mTraceBufferSizeText.addModifyListener(m);
		mTraceDurationText.addModifyListener(m);

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

		mOptions.mTraceCpuFreq = mTraceCpuFreqBtn.getSelection();
		mOptions.mTraceCpuIdle = mTraceCpuIdleBtn.getSelection();
		mOptions.mTraceCpuLoad = mTraceCpuLoadBtn.getSelection();
		mOptions.mTraceDiskIo = mTraceDiskIoBtn.getSelection();
		mOptions.mTraceKernelWorkqueues = mTraceKernelWorkqueuesBtn.getSelection();
		mOptions.mTraceCpuScheduler = mTraceCpuSchedulerBtn.getSelection();

		if (mGfxTagBtn.getSelection())
			mOptions.enableTag(SystraceOptions.TAG_GFX);
		if (mInputTagBtn.getSelection())
			mOptions.enableTag(SystraceOptions.TAG_INPUT);
		if (mViewTagBtn.getSelection())
			mOptions.enableTag(SystraceOptions.TAG_VIEW);
		if (mWebViewTagBtn.getSelection())
			mOptions.enableTag(SystraceOptions.TAG_WEBVIEW);
		if (mWmTagBtn.getSelection())
			mOptions.enableTag(SystraceOptions.TAG_WM);
		if (mAmTagBtn.getSelection())
			mOptions.enableTag(SystraceOptions.TAG_AM);
		if (mSyncTagBtn.getSelection())
			mOptions.enableTag(SystraceOptions.TAG_SYNC);
		if (mAudioTagBtn.getSelection())
			mOptions.enableTag(SystraceOptions.TAG_AUDIO);
		if (mVideoTagBtn.getSelection())
			mOptions.enableTag(SystraceOptions.TAG_VIDEO);
		if (mCameraTagBtn.getSelection())
			mOptions.enableTag(SystraceOptions.TAG_CAMERA);

		// save current selections to be restored if the dialog is invoked again
		sTraceCpuFreq = mTraceCpuFreqBtn.getSelection();
		sTraceCpuIdle = mTraceCpuIdleBtn.getSelection();
		sTraceCpuLoad = mTraceCpuLoadBtn.getSelection();
		sTraceDiskIo = mTraceDiskIoBtn.getSelection();
		sTraceKernelWorkqueues = mTraceKernelWorkqueuesBtn.getSelection();
		sTraceCpuScheduler = mTraceCpuSchedulerBtn.getSelection();

		sGfxTag = mGfxTagBtn.getSelection();
		sInputTag = mInputTagBtn.getSelection();
		sViewTag = mViewTagBtn.getSelection();
		sWebViewTag = mWebViewTagBtn.getSelection();
		sWmTag = mWmTagBtn.getSelection();
		sAmTag = mAmTagBtn.getSelection();
		sSyncTag = mSyncTagBtn.getSelection();
		sAudioTag = mAudioTagBtn.getSelection();
		sVideoTag = mVideoTagBtn.getSelection();
		sCameraTag = mCameraTagBtn.getSelection();

		super.okPressed();
	}

	@Override
	public SystraceOptions getSystraceOptions() {
		return mOptions;
	}

	@Override
	public String getTraceFilePath() {
		return mDestinationPath;
	}

	private class SystraceOptions implements ISystraceOptions {
		// This list is based on the tags in
		// frameworks/native/include/utils/Trace.h
		private static final int TAG_GFX = 1 << 1;
		private static final int TAG_INPUT = 1 << 2;
		private static final int TAG_VIEW = 1 << 3;
		private static final int TAG_WEBVIEW = 1 << 4;
		private static final int TAG_WM = 1 << 5;
		private static final int TAG_AM = 1 << 6;
		private static final int TAG_SYNC = 1 << 7;
		private static final int TAG_AUDIO = 1 << 8;
		private static final int TAG_VIDEO = 1 << 9;
		private static final int TAG_CAMERA = 1 << 10;

		private int mTraceBufferSize;
		private int mTraceDuration;

		private boolean mTraceCpuFreq;
		private boolean mTraceCpuIdle;
		private boolean mTraceCpuLoad;
		private boolean mTraceDiskIo;
		private boolean mTraceKernelWorkqueues;
		private boolean mTraceCpuScheduler;

		private int mTag;

		private void enableTag(int tag) {
			mTag |= tag;
		}

		@Override
		public String getTags() {
			return mTag == 0 ? null : "0x" + Integer.toHexString(mTag);
		}

		@Override
		public String getOptions() {
			StringBuilder sb = new StringBuilder(20);

			if (mTraceCpuFreq)
				sb.append("-f "); //$NON-NLS-1$
			if (mTraceCpuIdle)
				sb.append("-i "); //$NON-NLS-1$
			if (mTraceCpuLoad)
				sb.append("-l "); //$NON-NLS-1$
			if (mTraceDiskIo)
				sb.append("-d "); //$NON-NLS-1$
			if (mTraceKernelWorkqueues)
				sb.append("-w "); //$NON-NLS-1$
			if (mTraceCpuScheduler)
				sb.append("-s "); //$NON-NLS-1$

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

			return sb.toString().trim();
		}
	}
}
