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
import java.util.HashSet;
import java.util.Set;

import org.eclipse.andmore.android.ISerialNumbered;
import org.eclipse.andmore.android.SdkUtils;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.emulator.device.handlers.OpenNewDeviceWizardHandler;
import org.eclipse.andmore.android.emulator.device.refresh.InstancesListRefresh;
import org.eclipse.andmore.android.launch.ILaunchConfigurationConstants;
import org.eclipse.andmore.android.launch.LaunchPlugin;
import org.eclipse.andmore.android.launch.LaunchUtils;
import org.eclipse.andmore.android.launch.i18n.LaunchNLS;
import org.eclipse.andmore.io.IFolderWrapper;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.sequoyah.device.framework.events.IInstanceListener;
import org.eclipse.sequoyah.device.framework.events.InstanceEvent;
import org.eclipse.sequoyah.device.framework.events.InstanceEventManager;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.android.ide.common.xml.AndroidManifestParser;
import com.android.ide.common.xml.ManifestData;
import com.android.ide.common.xml.ManifestData.Activity;

/**
 * DESCRIPTION: This class implements the tab that is shown when the user is
 * editing the configuration to run a MOTODEV Studio for Android application
 * 
 * RESPONSIBILITY: User interface to allow the user to enter information to
 * launch the application.
 * 
 * COLABORATORS: This class is one of the tabs of the
 * LaunchConfigurationTabGroup
 * 
 * USAGE: This class should be created/used by the LaunchConfigurationTabGroup
 * only.
 */
@SuppressWarnings("restriction")
public class LaunchConfigurationTab extends AbstractLaunchConfigurationTab {
	private static final String NAME = LaunchNLS.UI_LaunchConfigurationTab_Tab_Name;

	private static final Object UPDATE_WIDGETS_EVENT = new Object();

	private Composite mainComposite;

	private String projectName = ""; //$NON-NLS-1$

	private String activityName = ""; //$NON-NLS-1$

	private String deviceName = ""; //$NON-NLS-1$

	private boolean activitySpecified = false;

	private boolean runDefaultActivity = true;

	private final String LAUNCH_DIALOG_HELP = LaunchPlugin.PLUGIN_ID + ".mainLaunchTab"; //$NON-NLS-1$

	private Button defaultLauncherButton = null;

	private Button vdlLauncherButton = null;

	private Button deviceNameBrowseButton = null;

	private final IInstanceListener instanceListener = new IInstanceListener() {

		private void fireUpdate() {
			Display currentDisplay = PlatformUI.getWorkbench().getDisplay();
			if (!currentDisplay.isDisposed()) {
				currentDisplay.syncExec(new Runnable() {

					@Override
					public void run() {
						updateDeviceChooserButton();
						updateLaunchConfigurationDialog();
					}
				});
			}
		}

		@Override
		public void instanceUpdated(InstanceEvent instanceevent) {
			fireUpdate();
		}

		@Override
		public void instanceUnloaded(InstanceEvent instanceevent) {
			fireUpdate();
		}

		@Override
		public void instanceTransitioned(InstanceEvent instanceevent) {
			fireUpdate();
		}

		@Override
		public void instanceLoaded(InstanceEvent instanceevent) {
			fireUpdate();
		}

		@Override
		public void instanceDeleted(InstanceEvent instanceevent) {
			fireUpdate();
		}

		@Override
		public void instanceCreated(InstanceEvent instanceevent) {
			fireUpdate();
		}

		@Override
		public void instanceAboutToTransition(InstanceEvent instanceevent) {
			fireUpdate();
		}
	};

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);

		GridLayout layout = new GridLayout(1, false);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 430;
		gd.heightHint = 130;
		main.setLayout(layout);
		main.setLayoutData(gd);

		createMainInfoGroup(main);
		setControl(main);
	}

	private void updateDeviceChooserButton() {
		// button is always enabled
		if (!deviceNameBrowseButton.isDisposed()) {
			deviceNameBrowseButton.setEnabled(true);
		}
	}

	/**
	 * Create the main information selection group
	 * 
	 * @param mainComposite
	 *            : the parent composite
	 */
	private void createMainInfoGroup(Composite mainComposite) {
		this.mainComposite = mainComposite;

		// create destination group
		Group destinationGroup = new Group(mainComposite, SWT.NONE);
		GridLayout layout = new GridLayout(3, false);
		destinationGroup.setLayout(layout);
		GridData defaultDestGridData = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
		destinationGroup.setLayoutData(defaultDestGridData);
		destinationGroup.setText(LaunchNLS.LaunchComposite_UI_LaunchComposite_DestinationGroupText);

		// Project Name Label
		Label projectNameLabel = new Label(destinationGroup, SWT.NONE);
		projectNameLabel.setText(LaunchNLS.UI_LaunchComposite_ProjectNameLabel);
		GridData folderGridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		projectNameLabel.setLayoutData(folderGridData);

		// Project Name Text
		final Text projectNameText = new Text(destinationGroup, SWT.SINGLE | SWT.BORDER);
		projectNameText.setText(projectName);
		folderGridData = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		projectNameText.setLayoutData(folderGridData);
		projectNameText.addModifyListener(new ModifyListener() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.
			 * swt.events.ModifyEvent)
			 */
			@Override
			public void modifyText(ModifyEvent e) {
				if (e.data == UPDATE_WIDGETS_EVENT) {
					projectNameText.setText(projectName);
				} else {
					projectName = projectNameText.getText();
					updateLaunchConfigurationDialog();
				}
			}
		});

		// Project Name Browse Button
		Button projectNameBrowseButton = new Button(destinationGroup, SWT.PUSH);
		folderGridData = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		projectNameBrowseButton.setLayoutData(folderGridData);
		projectNameBrowseButton.setText(LaunchNLS.UI_LaunchComposite_BrowseButton);
		projectNameBrowseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				AndroidProjectsSelectionDialog dialog = new AndroidProjectsSelectionDialog(getShell());
				int result = dialog.open();
				if (result == Window.OK) {
					Object resultProject = dialog.getFirstResult();
					if (resultProject instanceof IProject) {
						IProject project = (IProject) resultProject;
						projectNameText.setText(project.getName());
					}
				}
			}

		});

		Group activityGroup = new Group(mainComposite, SWT.NONE);
		GridLayout activityLayout = new GridLayout(3, false);
		activityGroup.setLayout(activityLayout);
		GridData activityGrid = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
		activityGroup.setLayoutData(activityGrid);
		activityGroup.setText(LaunchNLS.UI_LaunchComposite_ActivityGroupLabel);

		final Button defaultActivityButton = new Button(activityGroup, SWT.RADIO);
		defaultActivityButton.setText(LaunchNLS.UI_LaunchComposite_ActivityDefaultButton);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 3;
		defaultActivityButton.setLayoutData(gridData);

		// Activity Name Button
		final Button specificActivityButton = new Button(activityGroup, SWT.RADIO);
		specificActivityButton.setText(LaunchNLS.LaunchConfigurationTab_LaunchButton);
		GridData activityData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		specificActivityButton.setLayoutData(activityData);

		// Activity Name Text
		final Text activityNameText = new Text(activityGroup, SWT.SINGLE | SWT.BORDER);
		activityNameText.setText(activityName);
		activityData = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		activityNameText.setLayoutData(activityData);
		activityNameText.addModifyListener(new ModifyListener() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.
			 * swt.events.ModifyEvent)
			 */
			@Override
			public void modifyText(ModifyEvent e) {
				if (e.data == UPDATE_WIDGETS_EVENT) {
					activityNameText.setText(activityName);
				} else {
					activityName = activityNameText.getText();
					updateLaunchConfigurationDialog();
				}
			}
		});

		// Activity Name Browse Button
		final Button activityNameBrowseButton = new Button(activityGroup, SWT.PUSH);
		activityData = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		activityNameBrowseButton.setLayoutData(activityData);
		activityNameBrowseButton.setText(LaunchNLS.UI_LaunchComposite_BrowseButton);
		activityNameBrowseButton.addSelectionListener(new SelectionAdapter() {
			/**
			 * Retrieve all activities of a given project
			 * 
			 * @return All the activities of a given project
			 */
			private Set<String> getAllActivities(String projectName) {
				String[] tempActivities = null;
				Set<String> activities = new HashSet<String>();

				if (projectName.length() != 0) {
					IProject selectedProject = LaunchUtils.getProject(projectName);

					tempActivities = LaunchUtils.getProjectActivities(selectedProject);
					for (String s : tempActivities) {
						activities.add(s);
					}
				}
				return activities;
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse
			 * .swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (projectName.length() == 0) {
					IWorkbenchWindow ww = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
					MessageDialog.openInformation(ww.getShell(), LaunchNLS.UI_LaunchComposite_ProjectRequiredTitle,
							LaunchNLS.UI_LaunchComposite_ProjectRequiredMessage);
				} else {

					ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), new LabelProvider() {
						@Override
						public String getText(Object element) {
							String activity = (String) element;
							return activity;
						}
					}) {
						/*
						 * (non-Javadoc)
						 * 
						 * @see
						 * org.eclipse.ui.dialogs.ElementListSelectionDialog
						 * #createDialogArea(org.eclipse.swt.widgets.Composite)
						 */
						@Override
						protected Control createDialogArea(Composite parent) {
							PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, ACTIVITY_SELECTION_DIALOG_HELPID);
							return super.createDialogArea(parent);
						}

					};

					dialog.setTitle(LaunchNLS.UI_LaunchComposite_SelectActivityScreenTitle);
					dialog.setMessage(LaunchNLS.UI_LaunchComposite_SelectActivityScreenMessage);

					Object[] allActivities = getAllActivities(projectNameText.getText()).toArray();
					if (allActivities.length == 0) {
						activityNameText.setText(""); //$NON-NLS-1$
					} else {
						dialog.setElements(getAllActivities(projectNameText.getText()).toArray());

						int buttonId = dialog.open();
						if (buttonId == IDialogConstants.OK_ID) {
							String activity = (String) dialog.getFirstResult();
							activityNameText.setText(activity);

						}
					}
				}
			}

			protected static final String ACTIVITY_SELECTION_DIALOG_HELPID = "org.eclipse.andmore.android.launch.activitySelectionDialog"; //$NON-NLS-1$
		});

		final Button noActivityButton = new Button(activityGroup, SWT.RADIO);
		noActivityButton.setText(LaunchNLS.LaunchConfigurationTab_DoNothingButton);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 3;
		noActivityButton.setLayoutData(gridData);

		defaultActivityButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (e.data == UPDATE_WIDGETS_EVENT) {
					defaultActivityButton.setSelection(!activitySpecified && runDefaultActivity);
					activityNameText.setEnabled(activitySpecified);
					activityNameBrowseButton.setEnabled(activitySpecified);
				} else {
					// handle variables
					handleActivityLauncherTypeVariables(defaultActivityButton, specificActivityButton,
							activityNameText, activityNameBrowseButton);
				}
			}
		});

		specificActivityButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (e.data == UPDATE_WIDGETS_EVENT) {
					specificActivityButton.setSelection(activitySpecified && !runDefaultActivity);
					activityNameText.setEnabled(activitySpecified);
					activityNameBrowseButton.setEnabled(activitySpecified);
				} else {
					// handle variables
					handleActivityLauncherTypeVariables(defaultActivityButton, specificActivityButton,
							activityNameText, activityNameBrowseButton);
				}
			}
		});

		noActivityButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (e.data == UPDATE_WIDGETS_EVENT) {
					noActivityButton.setSelection(!activitySpecified && !runDefaultActivity);
					activityNameText.setEnabled(activitySpecified);
					activityNameBrowseButton.setEnabled(activitySpecified);
				} else {
					// handle variables
					handleActivityLauncherTypeVariables(defaultActivityButton, specificActivityButton,
							activityNameText, activityNameBrowseButton);
				}
			}
		});

		// Device Name Label
		Label deviceNameLabel = new Label(destinationGroup, SWT.NONE);
		deviceNameLabel.setText(LaunchNLS.UI_LaunchComposite_DeviceNameLabel);
		GridData deviceGridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		deviceNameLabel.setLayoutData(deviceGridData);

		// Device Name Text
		final Text deviceNameText = new Text(destinationGroup, SWT.SINGLE | SWT.BORDER);
		deviceNameText.setText(deviceName);
		deviceGridData = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		deviceNameText.setLayoutData(deviceGridData);
		deviceNameText.addModifyListener(new ModifyListener() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.
			 * swt.events.ModifyEvent)
			 */
			@Override
			public void modifyText(ModifyEvent e) {
				if (e.data == UPDATE_WIDGETS_EVENT) {
					deviceNameText.setText(deviceName);
				} else {
					deviceName = deviceNameText.getText();
					updateLaunchConfigurationDialog();
				}
			}
		});

		// Device Name Browse Button
		deviceNameBrowseButton = new Button(destinationGroup, SWT.PUSH);
		deviceGridData = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		deviceNameBrowseButton.setLayoutData(deviceGridData);
		deviceNameBrowseButton.setText(LaunchNLS.UI_LaunchComposite_BrowseButton);
		deviceNameBrowseButton.addSelectionListener(new SelectionAdapter() {

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse
			 * .swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent e) {
				IProject selectedProject = LaunchUtils.getProject(projectNameText.getText());
				DeviceSelectionDialog dialog = new DeviceSelectionDialog(getShell(),
						LaunchNLS.UI_LaunchComposite_SelectDeviceScreenMessage, selectedProject);
				dialog.setTitle(LaunchNLS.UI_LaunchComposite_SelectDeviceScreenTitle);
				dialog.setMultipleSelection(false);
				dialog.setValidator(new ISelectionStatusValidator() {

					@Override
					public IStatus validate(Object[] selection) {
						IStatus status = new Status(IStatus.OK, LaunchPlugin.PLUGIN_ID, ""); //$NON-NLS-1$
						if (selection.length == 0) {
							status = new Status(IStatus.ERROR, LaunchPlugin.PLUGIN_ID, "No selected instance"); //$NON-NLS-1$
						}
						return status;
					}
				});
				int res = dialog.open();
				if (res == IDialogConstants.OK_ID) {
					ISerialNumbered serialNumbered = (ISerialNumbered) dialog.getFirstResult();
					String selectedDevice = ((IInstance) serialNumbered).getName();
					deviceNameText.setText(selectedDevice);
				}
			}

		});

		InstanceEventManager.getInstance().addInstanceListener(instanceListener);

		Link createNewAvdLink = new Link(destinationGroup, SWT.NONE);
		deviceGridData = new GridData(SWT.RIGHT, SWT.CENTER, true, false, 3, 1);
		createNewAvdLink.setLayoutData(deviceGridData);
		createNewAvdLink.setText(LaunchNLS.LaunchConfigurationTab_CreateNewAVDLink);
		createNewAvdLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				OpenNewDeviceWizardHandler handler = new OpenNewDeviceWizardHandler();
				try {
					handler.execute(new ExecutionEvent());
				} catch (ExecutionException exception) {
					// do nothing
				}
			}
		});

		mainComposite.addListener(SWT.Modify, new Listener() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.
			 * widgets.Event)
			 */
			@Override
			public void handleEvent(Event e) {
				projectNameText.notifyListeners(SWT.Modify, e);
				activityNameText.notifyListeners(SWT.Modify, e);
				deviceNameText.notifyListeners(SWT.Modify, e);
				defaultActivityButton.notifyListeners(SWT.Selection, e);
				specificActivityButton.notifyListeners(SWT.Selection, e);
				noActivityButton.notifyListeners(SWT.Selection, e);

				if (defaultLauncherButton != null) {
					defaultLauncherButton.notifyListeners(SWT.Selection, e);
				}
				if (vdlLauncherButton != null) {
					vdlLauncherButton.notifyListeners(SWT.Selection, e);
				}
			}
		});

		PlatformUI.getWorkbench().getHelpSystem().setHelp(mainComposite, LAUNCH_DIALOG_HELP);
	}

	/**
	 * Handle the variables regarding Activity Launcher options.
	 * 
	 * @param defaultActivityButton
	 *            {@link Button} for Default Activity.
	 * @param specificActivityButton
	 *            {@link Button} for Specific Activity.
	 * @param activityNameText
	 *            {@link Text} holding the Activity to be launched name.
	 * @param activityNameBrowseButton
	 *            Activity browser {@link Button}.
	 */
	private void handleActivityLauncherTypeVariables(final Button defaultActivityButton,
			final Button specificActivityButton, final Text activityNameText, final Button activityNameBrowseButton) {
		activitySpecified = specificActivityButton.getSelection();
		runDefaultActivity = defaultActivityButton.getSelection();
		activityNameText.setEnabled(activitySpecified);
		activityNameBrowseButton.setEnabled(activitySpecified);
		updateLaunchConfigurationDialog();
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	@Override
	public String getName() {
		return LaunchConfigurationTab.NAME;
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
	 */
	@Override
	public Image getImage() {
		return AbstractUIPlugin.imageDescriptorFromPlugin(LaunchPlugin.PLUGIN_ID,
				ILaunchConfigurationConstants.MOTODEV_APP_ICO).createImage();
	}

	@Override
	public void dispose() {
		InstanceEventManager.getInstance().removeInstanceListener(instanceListener);
		super.dispose();
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		// Assure that when loading the configuration, the TmL devices are in
		// sync with the
		// AVD available at the SDK
		InstancesListRefresh.refresh();

		try {
			projectName = configuration.getAttribute(ILaunchConfigurationConstants.ATTR_PROJECT_NAME,
					ILaunchConfigurationConstants.DEFAULT_VALUE);

			activityName = configuration.getAttribute(ILaunchConfigurationConstants.ATTR_ACTIVITY,
					ILaunchConfigurationConstants.DEFAULT_VALUE);

			activitySpecified = (configuration.getAttribute(ILaunchConfigurationConstants.ATTR_LAUNCH_ACTION,
					ILaunchConfigurationConstants.ATTR_LAUNCH_ACTION_ACTIVITY)) == ILaunchConfigurationConstants.ATTR_LAUNCH_ACTION_ACTIVITY;

			runDefaultActivity = (configuration.getAttribute(ILaunchConfigurationConstants.ATTR_LAUNCH_ACTION,
					ILaunchConfigurationConstants.ATTR_LAUNCH_ACTION_ACTIVITY)) == ILaunchConfigurationConstants.ATTR_LAUNCH_ACTION_DEFAULT;

			deviceName = configuration.getAttribute(ILaunchConfigurationConstants.ATTR_DEVICE_INSTANCE_NAME,
					ILaunchConfigurationConstants.DEFAULT_VALUE);

			Event e = new Event();
			e.type = SWT.Modify;
			e.data = UPDATE_WIDGETS_EVENT;
			mainComposite.notifyListeners(SWT.Modify, e);
		} catch (CoreException e) {
			// Do nothing for now
		}
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(ILaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName);
		configuration.setAttribute(ILaunchConfigurationConstants.ATTR_ACTIVITY, activityName);

		// For now we are not preventing the device chooser dialog to appear if
		// the user choose a
		// handset in the device field. However, if the user chooses an AVD, we
		// set the preferred
		// AVD field so that we force the launch to happen in the selected AVD
		// without asking the
		// user.
		Collection<String> validAvds = SdkUtils.getAllValidVmNames();
		if (validAvds.contains(deviceName)) {
			configuration.setAttribute(ILaunchConfigurationConstants.ATTR_DEVICE_INSTANCE_NAME, deviceName);
			configuration.setAttribute(ILaunchConfigurationConstants.ATTR_ADT_DEVICE_INSTANCE_NAME, deviceName);
		} else {
			configuration.setAttribute(ILaunchConfigurationConstants.ATTR_DEVICE_INSTANCE_NAME, deviceName);
			configuration.removeAttribute(ILaunchConfigurationConstants.ATTR_ADT_DEVICE_INSTANCE_NAME);
		}

		if (activitySpecified) {
			configuration.setAttribute(ILaunchConfigurationConstants.ATTR_LAUNCH_ACTION,
					ILaunchConfigurationConstants.ATTR_LAUNCH_ACTION_ACTIVITY);
		} else if (runDefaultActivity) {
			configuration.setAttribute(ILaunchConfigurationConstants.ATTR_LAUNCH_ACTION,
					ILaunchConfigurationConstants.ATTR_LAUNCH_ACTION_DEFAULT);
		} else {
			configuration.setAttribute(ILaunchConfigurationConstants.ATTR_LAUNCH_ACTION,
					ILaunchConfigurationConstants.ATTR_LAUNCH_ACTION_DO_NOTHING);
		}

		LaunchUtils.updateLaunchConfigurationDefaults(configuration);

		IProject project = LaunchUtils.getProject(projectName);
		IResource[] mappedResources = null;
		if (project != null) {
			mappedResources = new IResource[] { project };
		}

		configuration.setMappedResources(mappedResources);
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(ILaunchConfigurationConstants.ATTR_PROJECT_NAME,
				ILaunchConfigurationConstants.DEFAULT_VALUE);
		configuration.setAttribute(ILaunchConfigurationConstants.ATTR_ACTIVITY,
				ILaunchConfigurationConstants.DEFAULT_VALUE);
		configuration.setAttribute(ILaunchConfigurationConstants.ATTR_LAUNCH_ACTION,
				ILaunchConfigurationConstants.ATTR_LAUNCH_ACTION_DEFAULT);
		// It is default not to exist Preferred AVD attribute, so we just set
		// the Studio's
		// device instance name attribute here
		configuration.setAttribute(ILaunchConfigurationConstants.ATTR_DEVICE_INSTANCE_NAME,
				ILaunchConfigurationConstants.DEFAULT_VALUE);

		LaunchUtils.setADTLaunchConfigurationDefaults(configuration);

		projectName = ILaunchConfigurationConstants.DEFAULT_VALUE;
		activityName = ILaunchConfigurationConstants.DEFAULT_VALUE;
		deviceName = ILaunchConfigurationConstants.DEFAULT_VALUE;
		activitySpecified = ILaunchConfigurationConstants.DEFAULT_BOOL_VALUE;
		runDefaultActivity = !ILaunchConfigurationConstants.DEFAULT_BOOL_VALUE;

		if (mainComposite != null) {
			Event e = new Event();
			e.type = SWT.Modify;
			e.data = UPDATE_WIDGETS_EVENT;
			mainComposite.notifyListeners(SWT.Modify, e);
		}
	}

	/**
	 * @see ILaunchConfigurationTab#isValid(ILaunchConfiguration)
	 */
	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		boolean isValid = true;
		boolean hasWarning = false;

		String projectName = ""; //$NON-NLS-1$
		String instanceName = ""; //$NON-NLS-1$
		String activityName = ""; //$NON-NLS-1$

		try {
			projectName = launchConfig.getAttribute(ILaunchConfigurationConstants.ATTR_PROJECT_NAME,
					ILaunchConfigurationConstants.DEFAULT_VALUE);
			instanceName = launchConfig.getAttribute(ILaunchConfigurationConstants.ATTR_DEVICE_INSTANCE_NAME,
					(String) null);
			activityName = launchConfig.getAttribute(ILaunchConfigurationConstants.ATTR_ACTIVITY,
					ILaunchConfigurationConstants.DEFAULT_VALUE);
		} catch (CoreException e) {
			StudioLogger.error(LaunchConfigurationTab.class,
					"Error validating launch configuration " + launchConfig.getName(), e); //$NON-NLS-1$
		}

		/* Validate current project */

		IProject project = null;

		if (isValid && (projectName.length() > 0)) {
			Path projectPath = new Path(projectName);
			if (!projectPath.isValidSegment(projectName)) {
				isValid = false;
				setErrorMessage(LaunchNLS.UI_LaunchConfigurationTab_ERR_PROJECT_NOT_EXIST);
			}
			project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
			if ((project != null) && !project.exists()) {
				isValid = false;
				setErrorMessage(LaunchNLS.UI_LaunchConfigurationTab_ERR_PROJECT_NOT_EXIST);
			} else if ((project != null) && SdkUtils.isLibraryProject(project)) {
				isValid = false;
				setErrorMessage(LaunchNLS.UI_LaunchConfigurationTab_ERR_PROJECT_IS_LIBRARY);
			} else if (project == null) {
				isValid = false;
				setErrorMessage(LaunchNLS.UI_LaunchConfigurationTab_ERR_PROJECT_NOT_EXIST);
			}
		} else if (isValid && (projectName.length() == 0)) {
			setErrorMessage(null);
		}

		// if we have a chosen project, enable/disable the device selection
		if (project != null) {
			updateDeviceChooserButton();
		}

		/* Validate current device instance */
		if (isValid && (instanceName != null) && (instanceName.length() > 0)) {
			IStatus compatible = LaunchUtils.isCompatible(project, instanceName);
			if (compatible == null) {
				setErrorMessage(LaunchNLS.UI_LaunchConfigurationTab_ERR_DEVICE_INEXISTENT);
				isValid = false;
			} else if (compatible.getSeverity() == IStatus.ERROR) {
				setErrorMessage(compatible.getMessage());
				isValid = false;
			} else if (compatible.getSeverity() == IStatus.WARNING) {
				setMessage(compatible.getMessage());
				hasWarning = true;
			}
		} else if (isValid && (instanceName != null) && (instanceName.length() == 0)) {
			setErrorMessage(null);
		}

		/* Validate current activity */
		if (isValid && (activityName.length() > 0) && activitySpecified) {
			/*
			 * Check if the activity is valid in the current METAINF project
			 * file
			 */

			Activity[] currentActivities = null;
			boolean activityValid = false;

			ManifestData manifestParser = null;
			try {
				manifestParser = AndroidManifestParser.parse(new IFolderWrapper(project));
			} catch (Exception e) {
				StudioLogger.error(LaunchUtils.class, "An error occurred trying to parse AndroidManifest", e); //$NON-NLS-1$
			}
			if (manifestParser != null) {
				currentActivities = manifestParser.getActivities();
			} else {
				// There's a problem with the manifest file / parser. Invalidate
				// current settings.
				isValid = false;
				setErrorMessage(LaunchNLS.UI_LaunchConfigurationTab_ERR_INVALID_ACTIVITY);
			}

			/* See if the chosen activity is there */

			for (Activity s : currentActivities) {
				if (s.getName().equals(activityName)) {
					activityValid = true;
				}
			}

			if (!activityValid) {
				isValid = false;
				setErrorMessage(LaunchNLS.UI_LaunchConfigurationTab_ERR_ACTIVITY_NOT_EXIST);
			}

		} else if (isValid && ((activityName.length() == 0) && activitySpecified)) {
			setErrorMessage(null);
		}

		/* Wrap up validation */
		if (isValid
				&& ((projectName.length() == 0) || ((activitySpecified) && (activityName.length() == 0)) || (instanceName
						.length() == 0))) {
			isValid = false;

			if (projectName.length() == 0) {
				setMessage(LaunchNLS.UI_LaunchConfigurationTab_InfoSelectProject);
			} else if (instanceName.length() == 0) {
				setMessage(LaunchNLS.UI_LaunchConfigurationTab_InfoSelectInstance);
			} else if (activityName.length() == 0) {
				setMessage(LaunchNLS.UI_LaunchConfigurationTab_InfoSelectActivity);
			}

		}

		if (isValid) {
			setErrorMessage(null);
			if (!hasWarning) {
				setMessage(null);
			}
		}

		return isValid;
	}
}
