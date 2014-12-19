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

package org.eclipse.andmore.android.launch.ui;

import java.util.Collection;

import org.eclipse.andmore.android.AndroidPlugin;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.emulator.core.devfrm.DeviceFrameworkManager;
import org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance;
import org.eclipse.andmore.android.launch.ILaunchConfigurationConstants;
import org.eclipse.andmore.android.launch.LaunchPlugin;
import org.eclipse.andmore.android.launch.LaunchUtils;
import org.eclipse.andmore.android.launch.i18n.LaunchNLS;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * This class shows a dialog to the user with online AVDs compatibles with
 * prefferedAvd. The user can choose an online AVD and click Ok, ignore the
 * dialog or abort the ongoing action.
 */
public class StartedInstancesDialog extends TitleAreaDialog {

	Collection<IAndroidEmulatorInstance> compatibleStartedInstances = null;

	private TableViewer viewer;

	private IAndroidEmulatorInstance selectedInstance;

	private Button okButton;

	private Button ignoreButton;

	private Button abortButton;

	private final IAndroidEmulatorInstance preferredAvd;

	private IProject project = null;

	private ILaunchConfiguration configuration = null;

	private boolean isUpdateConfigurationSelected = false;

	private static String DIALOG_IMAGE = "icons/choose_compatible_avd_instance.png";

	private static final String STARTED_INSTANCES_HELP_ID = AndroidPlugin.PLUGIN_ID
			+ ".started_instances_selection_dialog";

	/**
	 * @param parentShell
	 * @throws CoreException
	 */
	public StartedInstancesDialog(Shell parentShell, Collection<IAndroidEmulatorInstance> compatibleStartedInstances,
			ILaunchConfiguration configuration, IProject project) throws CoreException {
		super(parentShell);

		this.configuration = configuration;
		this.compatibleStartedInstances = compatibleStartedInstances;
		this.project = project;

		final String instanceName = configuration.getAttribute(ILaunchConfigurationConstants.ATTR_DEVICE_INSTANCE_NAME,
				(String) null);

		this.preferredAvd = DeviceFrameworkManager.getInstance().getInstanceByName(instanceName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.TitleAreaDialog#createContents(org.eclipse.
	 * swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		Control control = super.createContents(parent);

		getShell().setText(LaunchNLS.UI_StartedInstancesDialog_WindowTitle);

		setTitle(NLS.bind(LaunchNLS.UI_StartedInstancesDialog_Title, preferredAvd.getName()));
		setMessage(LaunchNLS.UI_StartedInstancesDialog_Message);
		setTitleImage(AbstractUIPlugin.imageDescriptorFromPlugin(LaunchPlugin.PLUGIN_ID, DIALOG_IMAGE).createImage());

		enableOkButton();

		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, STARTED_INSTANCES_HELP_ID);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(control, STARTED_INSTANCES_HELP_ID);

		return control;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse
	 * .swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK, Ignore and Abort buttons
		okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		ignoreButton = createButton(parent, IDialogConstants.IGNORE_ID, IDialogConstants.IGNORE_LABEL, false);
		abortButton = createButton(parent, IDialogConstants.ABORT_ID, IDialogConstants.ABORT_LABEL, false);

		ignoreButton.addMouseListener(new MouseListener() {

			@Override
			public void mouseUp(MouseEvent e) {
				setReturnCode(IDialogConstants.IGNORE_ID);
				close();
			}

			@Override
			public void mouseDown(MouseEvent e) {
				// do nothing
			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				// do nothing
			}
		});

		abortButton.addMouseListener(new MouseListener() {

			@Override
			public void mouseUp(MouseEvent e) {
				setReturnCode(IDialogConstants.ABORT_ID);
				close();
			}

			@Override
			public void mouseDown(MouseEvent e) {
				// do nothing
			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				// do nothing
			}
		});
	}

	/**
	 * Handles the enablement of the OK button.
	 */
	private void enableOkButton() {
		if (viewer.getTable().getSelectionCount() > 0) {
			okButton.setEnabled(true);
		} else {
			okButton.setEnabled(false);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse
	 * .swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		GridLayout layout = new GridLayout(1, false);
		parent.setLayout(layout);

		createTableArea(parent);

		return parent;
	}

	/**
	 * @param parent
	 */
	private void createTableArea(Composite parent) {
		viewer = new TableViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		viewer.getTable().setHeaderVisible(true);
		viewer.getTable().setLinesVisible(false);

		TableViewerColumn avds = new TableViewerColumn(viewer, SWT.NONE);
		avds.getColumn().setText(LaunchNLS.UI_StartedInstancesDialog_CompatibleAvdsColumnName);
		avds.getColumn().setResizable(true);
		avds.getColumn().setWidth(480);

		viewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				enableOkButton();
			}
		});

		avds.setLabelProvider(new ColumnLabelProvider() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.jface.viewers.ColumnLabelProvider#getText(java.lang
			 * .Object)
			 */
			@Override
			public String getText(Object element) {
				IAndroidEmulatorInstance instance = (IAndroidEmulatorInstance) element;
				return instance.getName() + " (" + instance.getTarget() + ", "
						+ LaunchNLS.UI_StartedInstancesDialog_ApiLevel + " " + instance.getAPILevel() + ")";
			}

			@Override
			public Image getImage(Object element) {

				Image img = null;

				IAndroidEmulatorInstance instance = (IAndroidEmulatorInstance) element;
				IStatus compatible = LaunchUtils.isCompatible(project, instance.getName());

				// notify the warning state
				if (compatible.getSeverity() == IStatus.WARNING) {
					img = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);
				}

				return img;
			}

			@Override
			public String getToolTipText(Object element) {
				String toolTip = null;

				IAndroidEmulatorInstance instance = (IAndroidEmulatorInstance) element;
				IStatus compatible = LaunchUtils.isCompatible(project, instance.getName());

				if (compatible.getSeverity() == IStatus.WARNING) {
					toolTip = LaunchNLS.UI_StartedInstancesDialog_Tooltip;

				}

				return toolTip;
			}

			@Override
			public int getToolTipDisplayDelayTime(Object object) {
				return 500;
			}

			@Override
			public int getToolTipTimeDisplayed(Object object) {
				return 5000;
			}

		});

		ColumnViewerToolTipSupport.enableFor(viewer, ToolTip.NO_RECREATE);

		ArrayContentProvider provider = new ArrayContentProvider();
		viewer.setContentProvider(provider);
		viewer.setInput(compatibleStartedInstances);

		Button checkBox = new Button(parent, SWT.CHECK);
		GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		checkBox.setLayoutData(gridData);
		checkBox.setText(LaunchNLS.UI_StartedInstancesDialog_UpdateRunConfigurarion);
		checkBox.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (isUpdateConfigurationSelected) {
					isUpdateConfigurationSelected = false;
				} else {
					isUpdateConfigurationSelected = true;
				}
			}
		});
	}

	public IAndroidEmulatorInstance getSelectedInstance() {
		return selectedInstance;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	@Override
	protected void okPressed() {
		selectedInstance = null;

		if (viewer.getTable().getSelectionCount() > 0) {
			selectedInstance = (IAndroidEmulatorInstance) viewer.getTable().getSelection()[0].getData();
		}

		if (isUpdateConfigurationSelected) {
			try {
				updateRunConfiguration();
			} catch (CoreException e) {
				StudioLogger.error(StartedInstancesDialog.class,
						"It was not possible to update the current run configuration");
			}
		}

		super.okPressed();
	}

	private void updateRunConfiguration() throws CoreException {
		ILaunchConfigurationWorkingCopy workingCopy = configuration.getWorkingCopy();
		workingCopy.setAttribute(ILaunchConfigurationConstants.ATTR_DEVICE_INSTANCE_NAME, selectedInstance.getName());
		workingCopy.setAttribute(ILaunchConfigurationConstants.ATTR_ADT_DEVICE_INSTANCE_NAME,
				selectedInstance.getName());
		workingCopy.doSave();
	}
}
