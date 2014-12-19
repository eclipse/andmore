/*
 * Copyright (C) 2011 The Android Open Source Project
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

package org.eclipse.andmore.gltrace;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.prefs.BackingStoreException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Dialog displaying all the trace options before the user initiates tracing. */
public class GLTraceOptionsDialog extends TitleAreaDialog {
	private static final String TITLE = "OpenGL ES Trace Options";
	private static final String DEFAULT_MESSAGE = "Provide the application and activity to be traced.";

	private static final String PREF_APP_PACKAGE = "gl.trace.apppackage"; //$NON-NLS-1$
	private static final String PREF_ACTIVITY = "gl.trace.activity"; //$NON-NLS-1$
	private static final String PREF_TRACEFILE = "gl.trace.destfile"; //$NON-NLS-1$
	private static final String PREF_DEVICE = "gl.trace.device"; //$NON-NLS-1$
	private String mLastUsedDevice;

	private static String sSaveToFolder = System.getProperty("user.home"); //$NON-NLS-1$

	private Button mOkButton;

	private Combo mDeviceCombo;
	private Text mAppPackageToTraceText;
	private Text mActivityToTraceText;
	private Button mIsActivityFullyQualifiedButton;
	private Text mTraceFilePathText;

	private String mSelectedDevice = "";
	private String mAppPackageToTrace = "";
	private String mActivityToTrace = "";
	private String mTraceFilePath = "";
	private boolean mAllowAppSelection;

	private static boolean sCollectFbOnEglSwap = true;
	private static boolean sCollectFbOnGlDraw = false;
	private static boolean sCollectTextureData = false;
	private static boolean sIsActivityFullyQualified = false;
	private IDevice[] mDevices;

	public GLTraceOptionsDialog(Shell parentShell) {
		this(parentShell, true, null);
	}

	/**
	 * Constructs a dialog displaying options for the tracer.
	 * 
	 * @param allowAppSelection
	 *            true if user can change the application to trace
	 * @param appToTrace
	 *            default application package to trace
	 */
	public GLTraceOptionsDialog(Shell parentShell, boolean allowAppSelection, String appToTrace) {
		super(parentShell);
		loadPreferences();

		mAllowAppSelection = allowAppSelection;
		if (appToTrace != null) {
			mAppPackageToTrace = appToTrace;
		}
	}

	@Override
	protected Control createDialogArea(Composite shell) {
		setTitle(TITLE);
		setMessage(DEFAULT_MESSAGE);

		Composite parent = (Composite) super.createDialogArea(shell);
		Composite c = new Composite(parent, SWT.BORDER);
		c.setLayout(new GridLayout(2, false));
		c.setLayoutData(new GridData(GridData.FILL_BOTH));

		createLabel(c, "Device:");
		mDevices = AndroidDebugBridge.getBridge().getDevices();
		createDeviceDropdown(c, mDevices);

		createSeparator(c);

		createLabel(c, "Application Package:");
		createAppToTraceText(c, "e.g. com.example.package");

		createLabel(c, "Activity to launch:");
		createActivityToTraceText(c, "Leave blank to launch default activity");

		createLabel(c, "");
		createIsFullyQualifedActivityButton(c, "Activity name is fully qualified, do not prefix with package name");

		if (!mAllowAppSelection) {
			mAppPackageToTraceText.setEnabled(false);
			mActivityToTraceText.setEnabled(false);
			mIsActivityFullyQualifiedButton.setEnabled(false);
		}

		createSeparator(c);

		createLabel(c, "Data Collection Options:");
		createCaptureImageOptions(c);

		createSeparator(c);

		createLabel(c, "Destination File: ");
		createSaveToField(c);

		return c;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);

		mOkButton = getButton(IDialogConstants.OK_ID);
		mOkButton.setText("Trace");

		DialogStatus status = validateDialog();
		mOkButton.setEnabled(status.valid);
	}

	private void createSeparator(Composite c) {
		Label l = new Label(c, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		l.setLayoutData(gd);
	}

	private void createSaveToField(Composite parent) {
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout(2, false));
		c.setLayoutData(new GridData(GridData.FILL_BOTH));

		mTraceFilePathText = new Text(c, SWT.BORDER);
		mTraceFilePathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		mTraceFilePathText.setText(mTraceFilePath);
		mTraceFilePathText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				validateAndSetMessage();
			}
		});

		Button browse = new Button(c, SWT.PUSH);
		browse.setText("Browse...");
		browse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String fName = openBrowseDialog();
				if (fName == null) {
					return;
				}

				mTraceFilePathText.setText(fName);
				validateAndSetMessage();
			}
		});
	}

	private String openBrowseDialog() {
		FileDialog fd = new FileDialog(Display.getDefault().getActiveShell(), SWT.SAVE);

		fd.setText("Save To");
		fd.setFileName("trace1.gltrace");

		fd.setFilterPath(sSaveToFolder);
		fd.setFilterExtensions(new String[] { "*.gltrace" });

		String fname = fd.open();
		if (fname == null || fname.trim().length() == 0) {
			return null;
		}

		sSaveToFolder = fd.getFilterPath();
		return fname;
	}

	/** Options controlling when the FB should be captured. */
	private void createCaptureImageOptions(Composite parent) {
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout(1, false));
		c.setLayoutData(new GridData(GridData.FILL_BOTH));

		final Button readFbOnEglSwapCheckBox = new Button(c, SWT.CHECK);
		readFbOnEglSwapCheckBox.setText("Read back framebuffer 0 on eglSwapBuffers()");
		readFbOnEglSwapCheckBox.setSelection(sCollectFbOnEglSwap);

		final Button readFbOnGlDrawCheckBox = new Button(c, SWT.CHECK);
		readFbOnGlDrawCheckBox.setText("Read back currently bound framebuffer On glDraw*()");
		readFbOnGlDrawCheckBox.setSelection(sCollectFbOnGlDraw);

		final Button readTextureDataCheckBox = new Button(c, SWT.CHECK);
		readTextureDataCheckBox.setText("Collect texture data submitted using glTexImage*()");
		readTextureDataCheckBox.setSelection(sCollectTextureData);

		SelectionListener l = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				sCollectFbOnEglSwap = readFbOnEglSwapCheckBox.getSelection();
				sCollectFbOnGlDraw = readFbOnGlDrawCheckBox.getSelection();
				sCollectTextureData = readTextureDataCheckBox.getSelection();
			}
		};

		readFbOnEglSwapCheckBox.addSelectionListener(l);
		readFbOnGlDrawCheckBox.addSelectionListener(l);
		readTextureDataCheckBox.addSelectionListener(l);
	}

	private Text createAppToTraceText(Composite parent, String defaultMessage) {
		mAppPackageToTraceText = new Text(parent, SWT.BORDER);
		mAppPackageToTraceText.setMessage(defaultMessage);
		mAppPackageToTraceText.setText(mAppPackageToTrace);

		mAppPackageToTraceText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		mAppPackageToTraceText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				validateAndSetMessage();
			}
		});

		return mActivityToTraceText;
	}

	private Text createActivityToTraceText(Composite parent, String defaultMessage) {
		mActivityToTraceText = new Text(parent, SWT.BORDER);
		mActivityToTraceText.setMessage(defaultMessage);
		mActivityToTraceText.setText(mActivityToTrace);

		mActivityToTraceText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		mActivityToTraceText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				validateAndSetMessage();
			}
		});

		return mActivityToTraceText;
	}

	private Button createIsFullyQualifedActivityButton(Composite parent, String message) {
		mIsActivityFullyQualifiedButton = new Button(parent, SWT.CHECK);
		mIsActivityFullyQualifiedButton.setText(message);
		mIsActivityFullyQualifiedButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		mIsActivityFullyQualifiedButton.setSelection(sIsActivityFullyQualified);

		return mIsActivityFullyQualifiedButton;
	}

	private void validateAndSetMessage() {
		DialogStatus status = validateDialog();
		mOkButton.setEnabled(status.valid);
		setErrorMessage(status.message);
	}

	private Combo createDeviceDropdown(Composite parent, IDevice[] devices) {
		mDeviceCombo = new Combo(parent, SWT.READ_ONLY | SWT.BORDER);

		List<String> items = new ArrayList<String>(devices.length);
		for (IDevice d : devices) {
			items.add(d.getName());
		}
		mDeviceCombo.setItems(items.toArray(new String[items.size()]));

		int index = 0;
		if (items.contains(mLastUsedDevice)) {
			index = items.indexOf(mLastUsedDevice);
		}
		if (index >= 0 && index < items.size()) {
			mDeviceCombo.select(index);
		}
		return mDeviceCombo;
	}

	private void createLabel(Composite parent, String text) {
		Label l = new Label(parent, SWT.NONE);
		l.setText(text);
		GridData gd = new GridData();
		gd.horizontalAlignment = SWT.RIGHT;
		gd.verticalAlignment = SWT.CENTER;
		l.setLayoutData(gd);
	}

	/**
	 * A tuple that specifies whether the current state of the inputs on the
	 * dialog is valid or not. If it is not valid, the message field stores the
	 * reason why it isn't.
	 */
	private final class DialogStatus {
		final boolean valid;
		final String message;

		private DialogStatus(boolean isValid, String errMessage) {
			valid = isValid;
			message = errMessage;
		}
	}

	private DialogStatus validateDialog() {
		if (mDevices.length == 0) {
			return new DialogStatus(false, "No connected devices.");
		}

		if (mAppPackageToTraceText.getText().trim().isEmpty()) {
			return new DialogStatus(false, "Provide an application name");
		}

		String traceFile = mTraceFilePathText.getText().trim();
		if (traceFile.isEmpty()) {
			return new DialogStatus(false, "Specify the location where the trace will be saved.");
		}

		File f = new File(traceFile).getParentFile();
		if (f != null && !f.exists()) {
			return new DialogStatus(false, String.format("Folder %s does not exist", f.getAbsolutePath()));
		}

		return new DialogStatus(true, null);
	}

	@Override
	protected void okPressed() {
		mAppPackageToTrace = mAppPackageToTraceText.getText().trim();
		mActivityToTrace = mActivityToTraceText.getText().trim();
		if (mActivityToTrace.startsWith(".")) { //$NON-NLS-1$
			mActivityToTrace = mActivityToTrace.substring(1);
		}
		sIsActivityFullyQualified = mIsActivityFullyQualifiedButton.getSelection();
		mTraceFilePath = mTraceFilePathText.getText().trim();
		mSelectedDevice = mDeviceCombo.getText();

		savePreferences();

		super.okPressed();
	}

	private void savePreferences() {
		IEclipsePreferences prefs = new InstanceScope().getNode(GlTracePlugin.PLUGIN_ID);
		prefs.put(PREF_APP_PACKAGE, mAppPackageToTrace);
		prefs.put(PREF_ACTIVITY, mActivityToTrace);
		prefs.put(PREF_TRACEFILE, mTraceFilePath);
		prefs.put(PREF_DEVICE, mSelectedDevice);
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			// ignore issues while persisting preferences
		}
	}

	private void loadPreferences() {
		IEclipsePreferences prefs = new InstanceScope().getNode(GlTracePlugin.PLUGIN_ID);
		mAppPackageToTrace = prefs.get(PREF_APP_PACKAGE, "");
		mActivityToTrace = prefs.get(PREF_ACTIVITY, "");
		mTraceFilePath = prefs.get(PREF_TRACEFILE, "");
		mLastUsedDevice = prefs.get(PREF_DEVICE, "");
	}

	public TraceOptions getTraceOptions() {
		return new TraceOptions(mSelectedDevice, mAppPackageToTrace, mActivityToTrace, sIsActivityFullyQualified,
				mTraceFilePath, sCollectFbOnEglSwap, sCollectFbOnGlDraw, sCollectTextureData);
	}
}
