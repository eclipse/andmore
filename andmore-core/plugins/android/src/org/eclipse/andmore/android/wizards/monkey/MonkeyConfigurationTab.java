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
package org.eclipse.andmore.android.wizards.monkey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.andmore.android.AndroidPlugin;
import org.eclipse.andmore.android.DDMSFacade;
import org.eclipse.andmore.android.ISerialNumbered;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.devices.DevicesManager;
import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.andmore.android.wizards.elements.sorting.TableItemSortStringSetActionListener;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.sequoyah.device.framework.events.IInstanceListener;
import org.eclipse.sequoyah.device.framework.events.InstanceEvent;
import org.eclipse.sequoyah.device.framework.events.InstanceEventManager;
import org.eclipse.sequoyah.device.framework.model.IInstance;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * Implements the Main tab of the Monkey Launch Configuration.
 */
public class MonkeyConfigurationTab extends AbstractLaunchConfigurationTab {
	private MonkeyConfigurationTabTable packageTable;

	private Map<String, String> availablePackages = null;

	private static final String contextId = AndroidPlugin.PLUGIN_ID + ".monkey";

	private final boolean filterSystem;

	private static final Object UPDATE_WIDGETS_EVENT = new Object();

	private Text countCombo = null;

	private String deviceName = "";

	private String eventCount = "";

	private Button deviceNameBrowseButton = null;

	private final Button defaultLauncherButton = null;

	private Composite mainComposite;

	private List<String> selectedPackagesPreference = null;

	/**
	 * Determines whether the filter is active.
	 * 
	 * @return Returns whether the filter is active.
	 */
	boolean isFilterSystem() {
		return filterSystem;
	}

	/**
	 * Get the list of selected packages preference.
	 * 
	 * @return Returns the list of selected packages preference.
	 */
	List<?> getSelectedPackagesPreference() {
		return selectedPackagesPreference;
	}

	/**
	 * Call the method {@link #updateLaunchConfigurationDialog}.
	 */
	void callUpdateLaunchConfigurationDialog() {
		updateLaunchConfigurationDialog();
	}

	private boolean firstTime = false;

	/*
	 * This listener follows changes made on devices and fires updates.
	 */
	private final IInstanceListener instanceListener = new IInstanceListener() {

		private void fireUpdate() {
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

				@Override
				public void run() {
					updateDeviceChooserButton();
					updateLaunchConfigurationDialog();
				}
			});
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

	private void updateDeviceChooserButton() {
		// button is always enabled
		deviceNameBrowseButton.setEnabled(true);

	}

	public MonkeyConfigurationTab(boolean filterSystem) {
		this.filterSystem = filterSystem;
	}

	@Override
	public void createControl(Composite parent) {
		Composite mainComposite = new Composite(parent, SWT.FILL);

		this.mainComposite = mainComposite;

		mainComposite.setLayout(new GridLayout(3, false));
		/*
		 * Device
		 */
		// Device Name Label
		Label deviceNameLabel = new Label(mainComposite, SWT.NONE);
		deviceNameLabel.setText(AndroidNLS.UI_MonkeyComposite_DeviceNameLabel);
		GridData deviceGridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		deviceNameLabel.setLayoutData(deviceGridData);

		// Device Name Text
		final Text deviceNameText = new Text(mainComposite, SWT.SINGLE | SWT.BORDER);
		deviceNameText.setText(deviceName);
		deviceGridData = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		deviceNameText.setLayoutData(deviceGridData);

		addDeviceNameTextListeners(deviceNameText);

		// Device Name Browse Button
		deviceNameBrowseButton = new Button(mainComposite, SWT.PUSH);
		deviceGridData = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		deviceNameBrowseButton.setLayoutData(deviceGridData);
		deviceNameBrowseButton.setText(AndroidNLS.UI_General_BrowseButtonLabel);
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
				DeviceSelectionDialog dialog = new DeviceSelectionDialog(getShell(),
						AndroidNLS.UI_MonkeyComposite_SelectDeviceScreenMessage);
				dialog.setTitle(AndroidNLS.UI_MonkeyComposite_SelectDeviceScreenTitle);
				dialog.setMultipleSelection(false);
				dialog.setValidator(new ISelectionStatusValidator() {

					@Override
					public IStatus validate(Object[] selection) {
						IStatus status = new Status(IStatus.OK, AndroidPlugin.PLUGIN_ID, "");
						if (selection.length == 0) {
							status = new Status(IStatus.ERROR, AndroidPlugin.PLUGIN_ID, "No selected instance");
						}
						return status;
					}
				});
				int res = dialog.open();
				if (res == IDialogConstants.OK_ID) {
					ISerialNumbered serialNumbered = (ISerialNumbered) dialog.getFirstResult();
					String selectedDevice = ((IInstance) serialNumbered).getName();
					packageTable.removeAllTableItems();
					deviceNameText.setText(selectedDevice);
				}
			}

		});

		InstanceEventManager.getInstance().addInstanceListener(instanceListener);
		/*
		 * Device end
		 */
		packageTable = new MonkeyConfigurationTabTable(mainComposite, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION,
				AndroidNLS.UninstallAppWizardPage_Loading_Applications, this);
		packageTable.setTableHeaderVisible(true);
		GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1);
		packageTable.setLayoutData(layoutData);
		TableColumn packageNameColumn = packageTable.addTableColumn(SWT.CENTER);
		TableColumn isSystemColumn = packageTable.addTableColumn(SWT.CENTER);
		packageNameColumn.setText(AndroidNLS.UninstallAppWizardPage_ColumnPackageName);
		isSystemColumn.setText(AndroidNLS.UninstallAppWizardPage_ColumnPackageKiind);
		packageNameColumn.setWidth(200);
		isSystemColumn.setWidth(200);
		packageNameColumn.addSelectionListener(new TableItemSortStringSetActionListener());
		isSystemColumn.addSelectionListener(new TableItemSortStringSetActionListener());

		packageTable.setTableLinesVisible(false);
		packageTable.pack();

		packageTable.redraw();
		packageTable.addTableSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				firstTime = false;

				if ((packageTable != null)
						&& (packageTable.getTableItems().length > 0)
						&& (!packageTable.getTableItem(0).getText(0)
								.contains(AndroidNLS.UninstallAppWizardPage_Loading_Applications))) {
					selectedPackagesPreference = getPackageList();
					updateLaunchConfigurationDialog();
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
		});

		layoutData = new GridData(SWT.NONE, SWT.CENTER, false, false, 1, 1);
		layoutData.verticalIndent = 3;

		Label label = new Label(mainComposite, SWT.LEFT);
		label.setText(AndroidNLS.MonkeyWizardPage_CountCommand);
		label.setLayoutData(layoutData);

		layoutData = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		layoutData.verticalIndent = 3;
		countCombo = new Text(mainComposite, SWT.SINGLE | SWT.WRAP | SWT.BORDER);

		countCombo.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (e.data == UPDATE_WIDGETS_EVENT) {
					countCombo.setText(eventCount);
				} else {
					eventCount = countCombo.getText();
					updateLaunchConfigurationDialog();
				}
			}

		});
		countCombo.setLayoutData(layoutData);

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

				countCombo.notifyListeners(SWT.Modify, e);

				deviceNameText.notifyListeners(SWT.Modify, e);

				if (defaultLauncherButton != null) {
					defaultLauncherButton.notifyListeners(SWT.Selection, e);
				}

			}
		});

		PlatformUI.getWorkbench().getHelpSystem().setHelp(mainComposite, contextId);
		setControl(mainComposite);

	}

	/**
	 * Add a listener to the deviceNameText field.
	 * 
	 * @param deviceNameText
	 */
	private void addDeviceNameTextListeners(final Text deviceNameText) {
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
					// handling when pressing Revert button
					if ((deviceName != null) && (!deviceName.equals(deviceNameText.getText()))) {
						firstTime = true;
					}
					deviceNameText.setText(deviceName);
				} else {
					if (!deviceName.equals(deviceNameText.getText()) || (firstTime)) {
						deviceName = deviceNameText.getText();
						packageTable.removeAllTableItems();
						if (!deviceName.equals("")) {
							DevicesManager deviceManager = DevicesManager.getInstance();
							if (deviceManager != null) {
								ISerialNumbered serialNumbered = deviceManager.getDeviceByName(deviceName);
								if (serialNumbered != null) {
									final String serialNumber = serialNumbered.getSerialNumber();
									if (serialNumber != null) {
										// initiating Loading thread
										packageTable.removeAllTableItems();
										if (availablePackages != null) {
											availablePackages.clear();
										}
										// call service to populate the table
										packageTable.populateTableAsynchronously(serialNumber);
									}
								}
							}
						}
					}
					updateLaunchConfigurationDialog();
				}

			}

		});
	}

	/**
	 * Verify if the deviceName is a valid online device instance.
	 * 
	 * @param deviceName
	 * @return true if the deviceName is a valid instance.
	 */
	private boolean validDevice(String deviceName) {
		boolean valid = false;
		ISerialNumbered sequoyahInstance = DevicesManager.getInstance().getDeviceByName(deviceName);
		if ((sequoyahInstance != null) && DDMSFacade.isDeviceOnline(sequoyahInstance.getSerialNumber())) {
			valid = true;
		}
		return valid;
	}

	/**
	 * Verify if all required fields are fulfilled with valid values.
	 * 
	 * @return true if the required fields are correctly fulfilled.
	 */
	private boolean validatePage() {
		boolean complete = true;

		setErrorMessage(null);

		setMessage(null);

		if ((!deviceName.equals("")) && (!validDevice(deviceName))) {
			setErrorMessage(AndroidNLS.ERR_MonkeyWizardPage_Device);
			packageTable.removeAllTableItems();
			complete = false;
		}
		if ((complete) && (!countCombo.getText().equals(""))) {
			try {
				int i = Integer.parseInt(countCombo.getText().toString());

				if (i <= 0) {
					complete = false;
				}

			} catch (Exception e) {
				complete = false;
			}
			if (!complete) {
				String msg = NLS.bind(AndroidNLS.ERR_PropertiesMainComposite_Monkey_NumberMustBePositiveInteger,
						AndroidNLS.MonkeyWizardPage_CountCommand);
				setErrorMessage(msg);
			}
		}
		if (complete) {
			if (deviceName.equals("")) {
				setMessage(AndroidNLS.UI_MonkeyComposite_SelectDeviceScreenMessage);
				complete = false;
			} else if ((packageTable != null) && (packageTable.getTableSelectionCount() <= 0)) {
				setMessage(AndroidNLS.ERR_MonkeyWizardPage_Package);
				complete = false;
			} else if (countCombo.getText().equals("")) {
				setMessage(AndroidNLS.ERR_MonkeyWizardPage_CountCommand);
				complete = false;
			}
		}

		return complete;
	}

	/**
	 * get the value for the event count
	 * 
	 * @return
	 */
	public String getCount() {
		return countCombo.getText().toString();
	}

	/**
	 * get the list of selected packages to run monkey
	 * 
	 * @return
	 */
	public List<String> getPackageList() {
		List<String> selectedPackages = new ArrayList<String>();
		if (packageTable != null) {
			for (TableItem item : packageTable.getTableSelection()) {
				selectedPackages.add(item.getText(0));
			}
		}

		return selectedPackages;
	}

	@Override
	public String getName() {
		return AndroidNLS.UI_MonkeyComposite_TabMainName;
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
	 */
	@Override
	public Image getImage() {
		return AbstractUIPlugin.imageDescriptorFromPlugin(AndroidPlugin.PLUGIN_ID,
				IMonkeyConfigurationConstants.MOTODEV_APP_ICO).createImage();
	}

	@Override
	public void dispose() {
		InstanceEventManager.getInstance().removeInstanceListener(instanceListener);

		super.dispose();
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {

		try {

			if (deviceName.equals("")) {
				firstTime = true;
			} else {
				firstTime = false;
			}
			deviceName = configuration.getAttribute(IMonkeyConfigurationConstants.ATTR_DEVICE_INSTANCE_NAME,
					IMonkeyConfigurationConstants.DEFAULT_VALUE);

			eventCount = configuration.getAttribute(IMonkeyConfigurationConstants.ATTR_EVENT_COUNT_NAME,
					IMonkeyConfigurationConstants.DEFAULT_COUNT_VALUE);

			List<String> defaultValue = null;
			selectedPackagesPreference = configuration.getAttribute(
					IMonkeyConfigurationConstants.ATTR_SELECTED_PACKAGES, defaultValue);

			// Handling Revert button effect on the list of packages
			if (((packageTable != null) && (packageTable.getTableItems().length > 0) && (!packageTable.getTableItem(0)
					.getText(0).contains(AndroidNLS.UninstallAppWizardPage_Loading_Applications)))) {
				List<?> selectedPackages = getPackageList();
				if ((selectedPackagesPreference == null) || (!selectedPackagesPreference.containsAll(selectedPackages))) {
					packageTable.deselectAllTableItems();
					if (selectedPackagesPreference != null) {
						TableItem[] itemsP = new TableItem[selectedPackagesPreference.size()];
						int i = 0;
						for (TableItem item : packageTable.getTableItems()) {
							if (selectedPackagesPreference.contains(item.getText(0))) {
								itemsP[i] = item;
								i++;
							}
						}
						packageTable.setTableSelection(itemsP);
					}
				}
			}

			Event e = new Event();
			e.type = SWT.Modify;
			e.data = UPDATE_WIDGETS_EVENT;
			mainComposite.notifyListeners(SWT.Modify, e);
		} catch (CoreException e) {
			StudioLogger.error(MonkeyConfigurationTab.class,
					"Failed to initialize Monkey Launch Configuration:" + e.getMessage());
		}

	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {

		configuration.setAttribute(IMonkeyConfigurationConstants.ATTR_DEVICE_INSTANCE_NAME, deviceName);

		configuration.setAttribute(IMonkeyConfigurationConstants.ATTR_EVENT_COUNT_NAME, eventCount);

		configuration.setAttribute(IMonkeyConfigurationConstants.ATTR_SELECTED_PACKAGES, selectedPackagesPreference);

	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {

		configuration.setAttribute(IMonkeyConfigurationConstants.ATTR_DEVICE_INSTANCE_NAME,
				IMonkeyConfigurationConstants.DEFAULT_VALUE);

		configuration.setAttribute(IMonkeyConfigurationConstants.ATTR_EVENT_COUNT_NAME,
				IMonkeyConfigurationConstants.DEFAULT_COUNT_VALUE);
		List<String> defaultValue = null;
		configuration.setAttribute(IMonkeyConfigurationConstants.ATTR_SELECTED_PACKAGES, defaultValue);

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
		return validatePage();
	}
}