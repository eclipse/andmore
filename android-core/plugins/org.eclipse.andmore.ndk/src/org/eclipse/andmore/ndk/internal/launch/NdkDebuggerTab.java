/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.andmore.ndk.internal.launch;

import org.eclipse.andmore.ndk.internal.NdkHelper;
import org.eclipse.andmore.ndk.internal.NdkManager;
import org.eclipse.cdt.dsf.gdb.IGDBLaunchConfigurationConstants;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NdkDebuggerTab extends AbstractLaunchConfigurationTab {
	private static String sLastGdbPath;
	private static String sLastSolibPath;

	private Text mGdbPathText;
	private Text mGdbInitPathText;
	private Text mGdbRemotePortText;

	private org.eclipse.swt.widgets.List mSoliblist;
	private Button mAddSolibButton;
	private Button mDeleteSolibButton;

	/**
	 * @wbp.parser.entryPoint (Window Builder Entry Point)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);
		comp.setLayout(new GridLayout(1, false));

		Group grpGdb = new Group(comp, SWT.NONE);
		grpGdb.setText("Launch Options");
		grpGdb.setLayout(new GridLayout(3, false));
		grpGdb.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label lblDebugger = new Label(grpGdb, SWT.NONE);
		lblDebugger.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblDebugger.setText("Debugger:");

		mGdbPathText = new Text(grpGdb, SWT.BORDER);
		mGdbPathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		final Button btnBrowseGdb = new Button(grpGdb, SWT.NONE);
		btnBrowseGdb.setText("Browse...");

		Label lblNewLabel = new Label(grpGdb, SWT.NONE);
		lblNewLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblNewLabel.setText("GDB Command File:");

		mGdbInitPathText = new Text(grpGdb, SWT.BORDER);
		mGdbInitPathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		final Button btnBrowseGdbInit = new Button(grpGdb, SWT.NONE);
		btnBrowseGdbInit.setText("Browse...");

		SelectionListener browseListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Shell shell = ((Control) e.getSource()).getShell();
				if (e.getSource() == btnBrowseGdb) {
					browseForGdb(shell);
				} else {
					browseForGdbInit(shell);
				}
				checkParameters();
				updateLaunchConfigurationDialog();
			}
		};
		btnBrowseGdb.addSelectionListener(browseListener);
		btnBrowseGdbInit.addSelectionListener(browseListener);

		Label lblPort = new Label(grpGdb, SWT.NONE);
		lblPort.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblPort.setText("Port:");

		mGdbRemotePortText = new Text(grpGdb, SWT.BORDER);
		GridData gd_text_2 = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_text_2.widthHint = 100;
		mGdbRemotePortText.setLayoutData(gd_text_2);

		ModifyListener m = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				checkParameters();
				updateLaunchConfigurationDialog();
			}
		};
		mGdbPathText.addModifyListener(m);
		mGdbInitPathText.addModifyListener(m);
		mGdbRemotePortText.addModifyListener(m);

		Group grpSharedLibraries = new Group(comp, SWT.NONE);
		grpSharedLibraries.setText("Shared Libraries");
		grpSharedLibraries.setLayout(new GridLayout(2, false));
		GridData gd_grpSharedLibraries = new GridData(GridData.FILL_BOTH);
		gd_grpSharedLibraries.verticalAlignment = SWT.TOP;
		gd_grpSharedLibraries.grabExcessVerticalSpace = true;
		grpSharedLibraries.setLayoutData(gd_grpSharedLibraries);

		mSoliblist = new org.eclipse.swt.widgets.List(grpSharedLibraries, SWT.BORDER | SWT.V_SCROLL | SWT.SINGLE);
		GridData gd_list = new GridData(GridData.FILL_BOTH);
		gd_list.heightHint = 133;
		gd_list.grabExcessVerticalSpace = false;
		gd_list.verticalSpan = 1;
		mSoliblist.setLayoutData(gd_list);

		Composite composite = new Composite(grpSharedLibraries, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		composite.setLayout(new RowLayout(SWT.VERTICAL));

		mAddSolibButton = new Button(composite, SWT.NONE);
		mAddSolibButton.setText("Add...");

		mDeleteSolibButton = new Button(composite, SWT.NONE);
		mDeleteSolibButton.setText("Remove");
		mDeleteSolibButton.setEnabled(false);

		SelectionListener l = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Control c = (Control) e.getSource();
				if (c == mSoliblist) {
					// enable delete only if there is a selection
					mDeleteSolibButton.setEnabled(mSoliblist.getSelectionCount() > 0);
				} else if (c == mAddSolibButton) {
					addSolib(c.getShell());
				} else {
					// delete current selection
					int index = mSoliblist.getSelectionIndex();
					if (index >= 0) {
						mSoliblist.remove(index);
					}
				}
				updateLaunchConfigurationDialog();
			}
		};

		mSoliblist.addSelectionListener(l);
		mAddSolibButton.addSelectionListener(l);
		mDeleteSolibButton.addSelectionListener(l);
	}

	private void addSolib(Shell shell) {
		DirectoryDialog dd = new DirectoryDialog(shell);
		if (sLastSolibPath != null) {
			dd.setFilterPath(sLastSolibPath);
		}
		String solibPath = dd.open();

		if (solibPath != null) {
			mSoliblist.add(solibPath);
			sLastSolibPath = new File(solibPath).getParent();
		}
	}

	private void browseForGdb(Shell shell) {
		if (sLastGdbPath == null) {
			sLastGdbPath = NdkManager.getNdkLocation();
		}

		FileDialog fd = new FileDialog(shell);
		fd.setFilterPath(sLastGdbPath);

		String gdbPath = fd.open();
		if (gdbPath != null) {
			mGdbPathText.setText(gdbPath);
			sLastGdbPath = new File(gdbPath).getParent();
		}
	}

	private void browseForGdbInit(Shell shell) {
		FileDialog fd = new FileDialog(shell);
		String gdbInit = fd.open();
		if (gdbInit != null) {
			mGdbInitPathText.setText(gdbInit);
		}
	}

	private void checkParameters() {
		// check gdb path
		String gdb = mGdbPathText.getText().trim();
		if (!gdb.equals(NdkLaunchConstants.DEFAULT_GDB)) {
			File f = new File(gdb);
			if (!f.exists() || !f.canExecute()) {
				setErrorMessage("Invalid gdb location.");
				return;
			}
		}

		// check gdb init path
		String gdbInit = mGdbInitPathText.getText().trim();
		if (!gdbInit.isEmpty()) {
			File f = new File(gdbInit);
			if (!f.exists() || !f.isFile()) {
				setErrorMessage("Invalid gdbinit location.");
				return;
			}
		}

		// port should be a valid integer
		String port = mGdbRemotePortText.getText().trim();
		try {
			Integer.parseInt(port, 10);
		} catch (NumberFormatException e) {
			setErrorMessage("Port should be a valid integer");
			return;
		}

		// no errors
		setErrorMessage(null);
		setMessage(null);
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		NdkHelper.setLaunchConfigDefaults(config);
	}

	@Override
	public void initializeFrom(ILaunchConfiguration config) {
		mGdbPathText.setText(getAttribute(config, NdkLaunchConstants.ATTR_NDK_GDB, NdkLaunchConstants.DEFAULT_GDB));
		mGdbInitPathText.setText(getAttribute(config, IGDBLaunchConfigurationConstants.ATTR_GDB_INIT,
				NdkLaunchConstants.DEFAULT_GDBINIT));
		mGdbRemotePortText.setText(getAttribute(config, IGDBLaunchConfigurationConstants.ATTR_PORT,
				NdkLaunchConstants.DEFAULT_GDB_PORT));

		List<String> solibs = getAttribute(config, NdkLaunchConstants.ATTR_NDK_SOLIB, Collections.EMPTY_LIST);
		mSoliblist.removeAll();
		for (String s : solibs) {
			mSoliblist.add(s);
		}
	}

	private String getAttribute(ILaunchConfiguration config, String key, String defaultValue) {
		try {
			return config.getAttribute(key, defaultValue);
		} catch (CoreException e) {
			return defaultValue;
		}
	}

	private List<String> getAttribute(ILaunchConfiguration config, String key, List<String> defaultValue) {
		try {
			return config.getAttribute(key, defaultValue);
		} catch (CoreException e) {
			return defaultValue;
		}
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(NdkLaunchConstants.ATTR_NDK_GDB, mGdbPathText.getText().trim());
		config.setAttribute(IGDBLaunchConfigurationConstants.ATTR_GDB_INIT, mGdbInitPathText.getText().trim());
		config.setAttribute(IGDBLaunchConfigurationConstants.ATTR_PORT, mGdbRemotePortText.getText().trim());
		config.setAttribute(NdkLaunchConstants.ATTR_NDK_SOLIB, Arrays.asList(mSoliblist.getItems()));
	}

	@Override
	public String getName() {
		return "Debugger";
	}
}
