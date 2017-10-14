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
package org.eclipse.andmore.android.emulator.device.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.eclipse.andmore.android.SdkUtils;
import org.eclipse.andmore.android.common.log.AndmoreLogger;
import org.eclipse.andmore.android.emulator.device.IAndroidDeviceConstants;
import org.eclipse.andmore.android.emulator.device.IDevicePropertiesConstants;
import org.eclipse.andmore.android.emulator.device.definition.AndroidEmuDefMgr;
import org.eclipse.andmore.android.emulator.i18n.EmulatorNLS;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
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
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.dialogs.WorkbenchPreferenceDialog;

import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.ISystemImage;
import com.android.SdkConstants;
import com.android.repository.testframework.FakeProgressIndicator;
import com.android.sdklib.internal.avd.AvdInfo;
import com.android.sdklib.internal.avd.AvdManager;
import com.android.sdklib.repository.IdDisplay;
import com.android.sdklib.repository.targets.SystemImage;
import com.android.sdklib.repository.targets.SystemImageManager;

/**
 * DESCRIPTION: <br>
 * This class implements the UI for showing all Android Emulator Device Instance
 * main information, such as its name, description, etc. <br>
 * It extends the AbstractPropertiesComposite so as to use its common
 * functionalities. <br>
 * RESPONSIBILITY: <br>
 * - Show Android Emulator Device Instance main information on the UI <br>
 * COLABORATORS: <br>
 * AbstractPropertiesComposite: extends this class <br>
 * USAGE: <br>
 * This class should be added as a regular composite whenever main information
 * on Android Emulator Device Instance is necessary to be shown and edited on
 * the UI. It can either allow of stop the editing of the name of the instance,
 * while other information is always editable.
 */
// @SuppressWarnings("restriction")
@SuppressWarnings("restriction")
public class PropertiesMainComposite extends AbstractPropertiesComposite {
	private static final String ORG_ECLIPSE_UI_NET_NET_PREFERENCES = "org.eclipse.ui.net.NetPreferences"; //$NON-NLS-1$

	private final AndroidEmuDefMgr emuDefMgr = AndroidEmuDefMgr.getInstance();

	private String name = ""; //$NON-NLS-1$

	private String skinId = ""; //$NON-NLS-1$

	private String timeout = ""; //$NON-NLS-1$

	private boolean useVnc;

	private boolean useProxy;

	private boolean useSnapshots;

	// VM Settings
	private IAndroidTarget vmTarget = null;

	private String vmSkin = ""; //$NON-NLS-1$

	private String sdCardType = ""; //$NON-NLS-1$

	private String sdCardValue = ""; //$NON-NLS-1$

	private String vmPath = ""; //$NON-NLS-1$

	private boolean usingDefaultVmPath;

	private String abiType = "";

	/*
	 * SD Card
	 */
	private static final String SDCARD_TYPE_NONE = "NONE"; //$NON-NLS-1$

	private static final String SDCARD_TYPE_PATH = "PATH"; //$NON-NLS-1$

	private static final String SDCARD_TYPE_SIZE = "SIZE"; //$NON-NLS-1$

	private static final String SDCARD_PATH_EXTENSION = ".img"; //$NON-NLS-1$

	private static final String[] SDCARD_SIZE_UNITS = new String[] { "KB", "MB" //$NON-NLS-1$ //$NON-NLS-2$
	};

	private Button saveSnapshotButton;

	private Button startFromSnapshotButton;

	private boolean saveSnapshots;

	private boolean startFromSnapshots;

	private Combo abiTypeCombo;

	/**
	 * Creates a PropertiesMainComposite object.
	 * 
	 * @param parent
	 *            the parent composite
	 * @param name
	 *            the instance name
	 * @param description
	 *            the instance description
	 * @param emulatorDefId
	 *            the instance emulator definition id
	 * @param isNameEditable
	 *            whether the name should be editable or not
	 * @param areOtherFieldsEditable
	 *            True if the user will be able to edit other data in the
	 *            composite; false otherwise
	 */
	public PropertiesMainComposite(Composite parent, String name, String emulatorDefId, String timeout, boolean useVnc,
			boolean useProxy, boolean useSnapshot, boolean saveSnapshot, boolean startFromSnapshot,
			IAndroidTarget vmTarget, String vmSkin, String vmPath, String abiType, boolean showName,
			boolean areVmSettingsEditable, boolean areOtherFieldsEditable)

	{
		super(parent);

		this.name = name;
		this.timeout = timeout;
		this.useVnc = useVnc;
		this.useProxy = useProxy;
		this.abiType = abiType;
		skinId = emuDefMgr.getSkinId(emulatorDefId);

		this.vmTarget = vmTarget;
		this.vmSkin = vmSkin;
		this.vmPath = vmPath;
		this.useSnapshots = useSnapshot;
		this.saveSnapshots = saveSnapshot;
		this.startFromSnapshots = startFromSnapshot;

		createUI(showName, areVmSettingsEditable, areOtherFieldsEditable);

		// Set context Help (not available yet)
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IAndroidDeviceConstants.MAIN_PAGE_HELP);
	}

	/**
	 * Creates a PropertiesMainComposite object with instance name editing not
	 * allowed.
	 * 
	 * @param parent
	 *            the parent composite
	 * @param name
	 *            the instance name
	 * @param description
	 *            the instance description
	 * @param emulatorDefId
	 *            the instance emulator definition name
	 */
	public PropertiesMainComposite(Composite parent, String emulatorDefId, String timeout, boolean useVnc,
			boolean useProxy, boolean useSnapshot, boolean saveSnapshot, boolean startFromSnapshot,
			IAndroidTarget vmTarget, String vmSkin, String vmPath, String abiType) {
		this(parent, "", emulatorDefId, timeout, useVnc, useProxy, useSnapshot, saveSnapshot, startFromSnapshot,
				vmTarget, vmSkin, vmPath, abiType, false, true, true);

		// Set context Help (not available yet)
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IAndroidDeviceConstants.MAIN_PAGE_HELP);
	}

	/**
	 * @param showName
	 * @param areVmSettingsEditable
	 * @param areOtherFieldsEditable
	 */
	private void createUI(boolean showName, boolean areVmSettingsEditable, boolean areOtherFieldsEditable) {
		GridData data;
		Composite mainComposite = this;
		Collection<Control> otherFields = new HashSet<Control>();

		setMainLayout(2);

		if (showName) {
			Label nameLabel = new Label(mainComposite, SWT.READ_ONLY);
			nameLabel.setText(EmulatorNLS.UI_PropertiesMainComposite_NameLabel);
			data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
			nameLabel.setLayoutData(data);

			Text nameTextLabel = new Text(mainComposite, SWT.WRAP | SWT.READ_ONLY);
			nameTextLabel.setText(name);
			data = new GridData(SWT.FILL, SWT.NULL, true, false);
			nameTextLabel.setLayoutData(data);
		}

		/*
		 * (Not) Editable area
		 */
		if (areVmSettingsEditable) {
			createEditableVmUI();
		} else {
			createNotEditableVmUI();
		}

		/*
		 * Emulator proxy settings
		 */
		Group proxyGroup = new Group(mainComposite, SWT.SHADOW_OUT);
		proxyGroup.setText(EmulatorNLS.PropertiesMainComposite_ProxySettings_GroupTitle);
		data = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		proxyGroup.setLayoutData(data);
		proxyGroup.setLayout(new GridLayout(3, false));

		final Button proxyChkbox = new Button(proxyGroup, SWT.CHECK);
		proxyChkbox.setText(EmulatorNLS.PropertiesMainComposite_ProxySettings_CheckboxLabel);
		data = new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1);
		proxyChkbox.setSelection(useProxy);
		proxyChkbox.setLayoutData(data);
		proxyChkbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				useProxy = proxyChkbox.getSelection();
				notifyCompositeChangeListeners();
			}
		});

		Link networkSettings = new Link(proxyGroup, SWT.NONE);
		networkSettings.setText(EmulatorNLS.PropertiesMainComposite_ProxySettings_LinkToPreference);
		data = new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1);
		networkSettings.setLayoutData(data);

		networkSettings.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openNetworkPreferences();
			}
		});

		// Snapshot group

		Group snapshotGroup = new Group(mainComposite, SWT.SHADOW_OUT);
		snapshotGroup.setText(EmulatorNLS.PropertiesMainComposite_SnapshotSettings);
		data = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		snapshotGroup.setLayoutData(data);
		snapshotGroup.setLayout(new GridLayout(3, false));

		final Button enableSnapshotButton = new Button(snapshotGroup, SWT.CHECK);
		enableSnapshotButton.setText(EmulatorNLS.PropertiesMainComposite_UseSnapshot);
		data = new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1);
		enableSnapshotButton.setLayoutData(data);
		enableSnapshotButton.setEnabled(areVmSettingsEditable);
		enableSnapshotButton.setSelection(useSnapshots);
		enableSnapshotButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				useSnapshots = enableSnapshotButton.getSelection();
				notifyCompositeChangeListeners();
				startFromSnapshotButton.setEnabled(useSnapshots);
				saveSnapshotButton.setEnabled(useSnapshots);
			}
		});

		startFromSnapshotButton = new Button(snapshotGroup, SWT.CHECK);
		startFromSnapshotButton.setText(EmulatorNLS.PropertiesMainComposite_startFromSnapshot);
		data = new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1);
		startFromSnapshotButton.setLayoutData(data);
		startFromSnapshotButton.setEnabled(useSnapshots);
		startFromSnapshotButton.setSelection(startFromSnapshots);

		startFromSnapshotButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				startFromSnapshots = startFromSnapshotButton.getSelection();
				notifyCompositeChangeListeners();
			}
		});

		saveSnapshotButton = new Button(snapshotGroup, SWT.CHECK);
		saveSnapshotButton.setText(EmulatorNLS.PropertiesMainComposite_SaveSnapshot);
		data = new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1);
		saveSnapshotButton.setLayoutData(data);
		saveSnapshotButton.setEnabled(useSnapshots);
		saveSnapshotButton.setSelection(saveSnapshots);

		saveSnapshotButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				saveSnapshots = saveSnapshotButton.getSelection();
				notifyCompositeChangeListeners();
			}
		});

		otherFields.add(proxyGroup);
		otherFields.add(proxyChkbox);
		otherFields.add(networkSettings);
		otherFields.add(snapshotGroup);

		/*
		 * Emulator Window mode area
		 */

		// Only for windows and linux platforms
		if (!Platform.getOS().equals(Platform.OS_MACOSX)) {
			Group windowGroup = new Group(mainComposite, SWT.SHADOW_OUT);
			windowGroup.setText(EmulatorNLS.UI_PropertiesMainComposite_EmulatorWindowMode_GroupTitle);
			data = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
			windowGroup.setLayoutData(data);
			windowGroup.setLayout(new GridLayout(3, false));

			final Button nativeRadio = new Button(windowGroup, SWT.RADIO);
			nativeRadio.setText(EmulatorNLS.UI_PropertiesMainComposite_EmulatorWindowMode_NativeLabel);
			nativeRadio.setSelection(!useVnc);
			data = new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1);
			nativeRadio.setLayoutData(data);
			nativeRadio.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					useVnc = !nativeRadio.getSelection();
					notifyCompositeChangeListeners();
				}
			});

			final Button vncRadio = new Button(windowGroup, SWT.RADIO);
			vncRadio.setText(EmulatorNLS.UI_PropertiesMainComposite_EmulatorWindowMode_VncLabel);
			vncRadio.setSelection(useVnc);
			data = new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1);
			vncRadio.setLayoutData(data);

			otherFields.add(windowGroup);
			otherFields.add(nativeRadio);
			otherFields.add(vncRadio);
		}

		/*
		 * Timeout
		 */
		Label timeoutLabel = new Label(mainComposite, SWT.READ_ONLY);
		timeoutLabel.setText(EmulatorNLS.UI_PropertiesMainComposite_TimeoutLabel);
		data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		timeoutLabel.setLayoutData(data);

		final Text timeoutText = new Text(mainComposite, SWT.SINGLE | SWT.BORDER);
		otherFields.add(timeoutText);
		timeoutText.setText(timeout);
		data = new GridData(SWT.FILL, SWT.TOP, true, false);
		timeoutText.setLayoutData(data);

		timeoutText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				timeout = timeoutText.getText().trim();
				notifyCompositeChangeListeners();
			}
		});

		addInstanceListener(otherFields);
		updateWidgetEnableStatus(areOtherFieldsEditable, otherFields);
	}

	@SuppressWarnings("unchecked")
	protected void openNetworkPreferences() {
		// Makes the network preferences dialog manager
		PreferenceManager manager = PlatformUI.getWorkbench().getPreferenceManager();
		IPreferenceNode networkNode = null;
		for (IPreferenceNode node : manager.getElements(PreferenceManager.PRE_ORDER)) {
			if (node.getId().equals(ORG_ECLIPSE_UI_NET_NET_PREFERENCES)) {
				networkNode = node;
				break;
			}
		}
		PreferenceManager prefMan = new PreferenceManager();
		if (networkNode != null) {
			prefMan.addToRoot(networkNode);
		}
		PreferenceDialog networkPreferencesDialog = new WorkbenchPreferenceDialog(getShell(), prefMan);
		networkPreferencesDialog.create();
		networkPreferencesDialog.open();
	}

	/**
	 * Creates the not editable UI for the VM settings.
	 */
	private void createNotEditableVmUI() {
		GridData data;
		Composite mainComposite = this;

		Label tagetLabel = new Label(mainComposite, SWT.READ_ONLY);
		tagetLabel.setText(EmulatorNLS.UI_PropertiesMainComposite_TargetLabel);
		data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		tagetLabel.setLayoutData(data);

		final Text targetText = new Text(mainComposite, SWT.WRAP | SWT.READ_ONLY);
		targetText.setText(vmTarget.getName());
		data = new GridData(SWT.FILL, SWT.TOP, true, false);
		targetText.setLayoutData(data);

		Label abiTypeLabel = new Label(mainComposite, SWT.READ_ONLY);
		abiTypeLabel.setText(EmulatorNLS.PropertiesMainComposite_ABITypeLabel);
		data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		abiTypeLabel.setLayoutData(data);

		AvdManager manager = SdkUtils.getVmManager();

		AvdInfo avd = manager.getAvd(name, true);

		final Text abiTypeText = new Text(mainComposite, SWT.WRAP | SWT.READ_ONLY);
		abiTypeText.setText(AvdInfo.getPrettyAbiType(avd));
		data = new GridData(SWT.FILL, SWT.TOP, true, false);
		abiTypeText.setLayoutData(data);

		Label skinLabel = new Label(mainComposite, SWT.READ_ONLY);
		skinLabel.setText(EmulatorNLS.UI_PropertiesMainComposite_SkinLabel);
		data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		skinLabel.setLayoutData(data);

		final Text vmSkinText = new Text(mainComposite, SWT.WRAP | SWT.READ_ONLY);
		vmSkinText.setText(vmSkin);
		data = new GridData(SWT.FILL, SWT.TOP, true, false);
		vmSkinText.setLayoutData(data);

		Label pathLabel = new Label(mainComposite, SWT.READ_ONLY);
		pathLabel.setText(EmulatorNLS.UI_PropertiesMainComposite_PathLabel);
		data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		pathLabel.setLayoutData(data);

		final Text pathText = new Text(mainComposite, SWT.WRAP | SWT.READ_ONLY);
		pathText.setText(vmPath);
		data = new GridData(SWT.FILL, SWT.TOP, true, false);
		data.widthHint = 100;
		pathText.setLayoutData(data);

		/*
		 * SD Card info
		 */
		AvdInfo vmInfo = SdkUtils.getValidVm(name);
		Properties configFile = new Properties();
		BufferedReader input = null;
		try {

			// it's necessary to read the file instead of load the Properties
			// file because
			// there are "\" in the path, which are removed by the Properties
			// class load method, since
			// they mean line break in a properties file
			input = new BufferedReader(new FileReader(vmInfo.getConfigFile()));

			String line = null;
			String[] property = null;
			while ((line = input.readLine()) != null) {
				property = line.split("="); //$NON-NLS-1$
				if ((property[0] != null) && (property[1] != null)) {
					if ((!property[0].equals("")) && (!property[1].equals(""))) //$NON-NLS-1$ //$NON-NLS-2$
					{
						configFile.put(property[0], property[1]);
					}
				}
			}

			if (configFile.getProperty(IDevicePropertiesConstants.configSDCardPath) != null) {

				Label sdCardPathLabel = new Label(mainComposite, SWT.READ_ONLY);
				sdCardPathLabel.setText(EmulatorNLS.UI_PropertiesMainComposite_SDCardPathLabel);
				data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
				sdCardPathLabel.setLayoutData(data);

				final Text sdCardPathText = new Text(mainComposite, SWT.WRAP | SWT.READ_ONLY);
				sdCardPathText.setText(configFile.getProperty(IDevicePropertiesConstants.configSDCardPath));
				data = new GridData(SWT.FILL, SWT.TOP, true, false);
				data.widthHint = 100;
				sdCardPathText.setLayoutData(data);

			}

			if (configFile.getProperty(IDevicePropertiesConstants.configSDCardSize) != null) {
				Label sdCardPathLabel = new Label(mainComposite, SWT.READ_ONLY);
				sdCardPathLabel.setText(EmulatorNLS.UI_PropertiesMainComposite_SDCardSizeLabel);
				data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
				sdCardPathLabel.setLayoutData(data);

				final Text sdCardPathText = new Text(mainComposite, SWT.WRAP | SWT.READ_ONLY);
				sdCardPathText.setText(configFile.getProperty(IDevicePropertiesConstants.configSDCardSize));
				data = new GridData(SWT.FILL, SWT.TOP, true, false);
				sdCardPathText.setLayoutData(data);
			}

		} catch (FileNotFoundException e) {
			AndmoreLogger.error("Could not find config file for AVD: " + name); //$NON-NLS-1$
		} catch (IOException e) {
			AndmoreLogger.error("Could not read config file for AVD: " + name); //$NON-NLS-1$
		} finally {
			try {
				if (input != null) {
					input.close();
				}
			} catch (IOException e) {
				AndmoreLogger.error("Could not close input stream: ", e.getMessage()); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Creates the editable UI for the VM settings.
	 */
	private void createEditableVmUI() {
		GridData data;
		Composite mainComposite = this;

		Label tagetLabel = new Label(mainComposite, SWT.READ_ONLY);
		tagetLabel.setText(EmulatorNLS.UI_PropertiesMainComposite_TargetLabel);
		data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		tagetLabel.setLayoutData(data);

		final Combo targetCombo = new Combo(mainComposite, SWT.READ_ONLY);
		populateTargetCombo(targetCombo);
		data = new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1);
		targetCombo.setLayoutData(data);

		Label skinLabel = new Label(mainComposite, SWT.READ_ONLY);
		skinLabel.setText(EmulatorNLS.UI_PropertiesMainComposite_SkinLabel);
		data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		skinLabel.setLayoutData(data);

		final Combo vmSkinCombo = new Combo(mainComposite, SWT.READ_ONLY);
		populateSkinCombo(vmSkinCombo);
		data = new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1);
		vmSkinCombo.setLayoutData(data);

		vmSkinCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if ((vmSkinCombo != null) && !"".equals(vmSkinCombo.getText())) //$NON-NLS-1$
				{
					vmSkin = (String) vmSkinCombo.getData(vmSkinCombo.getText());

				} else {
					vmSkin = ""; //$NON-NLS-1$
				}
				notifyCompositeChangeListeners();

			}
		});

		Label abiTypeLabel = new Label(mainComposite, SWT.READ_ONLY);
		abiTypeLabel.setText(EmulatorNLS.PropertiesMainComposite_ABITypeLabel);
		data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		abiTypeLabel.setLayoutData(data);

		abiTypeCombo = new Combo(mainComposite, SWT.READ_ONLY);
		populateAbiTypeCombo(abiTypeCombo);
		data = new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1);
		abiTypeCombo.setLayoutData(data);

		abiTypeCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if ((vmSkinCombo != null) && !"".equals(vmSkinCombo.getText())) //$NON-NLS-1$
				{
					abiType = (String) abiTypeCombo.getData(abiTypeCombo.getText());

				} else {
					abiType = SdkConstants.ABI_ARMEABI;
				}
				notifyCompositeChangeListeners();

			}
		});

		targetCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IAndroidTarget newTarget = null;
				if ((targetCombo != null) && !"".equals(targetCombo.getText())) //$NON-NLS-1$
				{
					newTarget = (IAndroidTarget) targetCombo.getData(targetCombo.getText());
				}

				if (newTarget != vmTarget) {
					vmTarget = newTarget;
					populateSkinCombo(vmSkinCombo);
					populateAbiTypeCombo(abiTypeCombo);
					notifyCompositeChangeListeners();
				}
			}

		});

		Group vmPathGroup = new Group(mainComposite, SWT.SHADOW_OUT);
		vmPathGroup.setText(EmulatorNLS.UI_PropertiesMainComposite_PathGroupTitle);
		data = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		vmPathGroup.setLayoutData(data);
		vmPathGroup.setLayout(new GridLayout(3, false));

		usingDefaultVmPath = vmPath.equals(IDevicePropertiesConstants.defaultVmPath);

		final Button vmPathCheckbox = new Button(vmPathGroup, SWT.CHECK);
		vmPathCheckbox.setText(EmulatorNLS.UI_PropertiesMainComposite_UseDefaultPath);
		vmPathCheckbox.setSelection(usingDefaultVmPath);
		data = new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1);
		vmPathCheckbox.setLayoutData(data);

		Label pathLabel = new Label(vmPathGroup, SWT.READ_ONLY);
		pathLabel.setText(EmulatorNLS.UI_PropertiesMainComposite_PathLabel);
		data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		pathLabel.setLayoutData(data);

		final Text pathText = new Text(vmPathGroup, SWT.SINGLE | SWT.BORDER);
		pathText.setText(vmPath);
		pathText.setEnabled(!usingDefaultVmPath);
		data = new GridData(SWT.FILL, SWT.TOP, true, false);
		data.widthHint = 100;
		pathText.setLayoutData(data);

		final Button pathBrowseButton = new Button(vmPathGroup, SWT.PUSH);
		pathBrowseButton.setText(EmulatorNLS.UI_General_BrowseButtonLabel);
		pathBrowseButton.setEnabled(!usingDefaultVmPath);
		data = new GridData(SWT.FILL, SWT.FILL, false, false);
		pathBrowseButton.setLayoutData(data);

		vmPathCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				usingDefaultVmPath = vmPathCheckbox.getSelection();
				pathText.setEnabled(!usingDefaultVmPath);
				pathBrowseButton.setEnabled(!usingDefaultVmPath);
				if (usingDefaultVmPath) {
					vmPath = IDevicePropertiesConstants.defaultVmPath;
				}
				notifyCompositeChangeListeners();
			}
		});

		pathText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				vmPath = pathText.getText().trim();
				notifyCompositeChangeListeners();
			}
		});

		pathBrowseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dirDialog = new DirectoryDialog(getShell(), SWT.OPEN);

				if (!vmPath.trim().equals("")) //$NON-NLS-1$
				{
					dirDialog.setFilterPath(vmPath);
				} else {
					dirDialog.setFilterPath("/"); //$NON-NLS-1$
				}

				dirDialog.setText(EmulatorNLS.UI_PropertiesMainComposite_PathGroupTitle);

				String vmPath = dirDialog.open();

				if (vmPath != null) {
					pathText.setText(vmPath);
					notifyCompositeChangeListeners();
				}
			}
		});

		/*
		 * SD Card Area
		 */

		sdCardType = SDCARD_TYPE_NONE;

		Group sdCardGroup = new Group(mainComposite, SWT.SHADOW_OUT);
		sdCardGroup.setText(EmulatorNLS.UI_PropertiesMainComposite_SDCardLabel);
		data = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		sdCardGroup.setLayoutData(data);
		sdCardGroup.setLayout(new GridLayout(3, false));

		// none
		final Button noneSDCardCheckbox = new Button(sdCardGroup, SWT.RADIO);
		noneSDCardCheckbox.setText(EmulatorNLS.UI_PropertiesMainComposite_SDCardNoneLabel);
		noneSDCardCheckbox.setSelection(true);
		data = new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1);
		noneSDCardCheckbox.setLayoutData(data);
		noneSDCardCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				sdCardType = SDCARD_TYPE_NONE;
				sdCardValue = ""; //$NON-NLS-1$
				notifyCompositeChangeListeners();
			}
		});

		// existing
		final Button existingSDCardCheckbox = new Button(sdCardGroup, SWT.RADIO);
		existingSDCardCheckbox.setText(EmulatorNLS.UI_PropertiesMainComposite_SDCardExistingLabel);
		final Text existingSDCardPath = new Text(sdCardGroup, SWT.SINGLE | SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.TOP, true, false);
		data.widthHint = 100;
		existingSDCardPath.setLayoutData(data);
		existingSDCardPath.setEnabled(false);
		final Button existingSDCardButton = new Button(sdCardGroup, SWT.PUSH);
		existingSDCardButton.setText(EmulatorNLS.UI_General_BrowseButtonLabel);
		data = new GridData(SWT.FILL, SWT.FILL, false, false);
		existingSDCardButton.setLayoutData(data);
		existingSDCardButton.setEnabled(false);

		existingSDCardCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				sdCardType = SDCARD_TYPE_PATH;
				existingSDCardPath.setEnabled(existingSDCardCheckbox.getSelection());
				existingSDCardButton.setEnabled(existingSDCardCheckbox.getSelection());
				sdCardValue = existingSDCardPath.getText();
				notifyCompositeChangeListeners();
			}
		});

		existingSDCardPath.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				sdCardValue = existingSDCardPath.getText();
				notifyCompositeChangeListeners();
			}
		});

		existingSDCardButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String selectedPath = null;

				FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);
				String[] filterExtensions = { "*" + SDCARD_PATH_EXTENSION //$NON-NLS-1$
				};

				fileDialog.setFilterExtensions(filterExtensions);
				selectedPath = fileDialog.open();

				if (selectedPath != null) {
					existingSDCardPath.setText(selectedPath);
				}
			}
		});

		// new
		final Button newSDCardCheckbox = new Button(sdCardGroup, SWT.RADIO);
		newSDCardCheckbox.setText(EmulatorNLS.UI_PropertiesMainComposite_SDCardNewLabel);
		final Text newSDCardSize = new Text(sdCardGroup, SWT.SINGLE | SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.TOP, true, false);
		data.widthHint = 100;
		newSDCardSize.setLayoutData(data);
		newSDCardSize.setEnabled(false);
		final Combo newSDCardUnit = new Combo(sdCardGroup, SWT.READ_ONLY);
		for (String unit : SDCARD_SIZE_UNITS) {
			newSDCardUnit.add(unit);
		}
		newSDCardUnit.select(0);
		newSDCardUnit.setEnabled(false);
		data = new GridData(SWT.FILL, SWT.FILL, false, false);
		newSDCardUnit.setLayoutData(data);

		newSDCardCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				sdCardType = SDCARD_TYPE_SIZE;
				sdCardValue = newSDCardSize.getText();
				newSDCardSize.setEnabled(newSDCardCheckbox.getSelection());
				newSDCardUnit.setEnabled(newSDCardCheckbox.getSelection());
				sdCardValue = newSDCardSize.getText() + newSDCardUnit.getText().charAt(0);
				notifyCompositeChangeListeners();
			}
		});

		ModifyListener newSDCardListener = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				sdCardValue = newSDCardSize.getText() + newSDCardUnit.getText().charAt(0);
				notifyCompositeChangeListeners();
			}
		};

		newSDCardSize.addModifyListener(newSDCardListener);
		newSDCardUnit.addModifyListener(newSDCardListener);
	}

	private void populateAbiTypeCombo(Combo abiTypeCombo) {
		// System Images represents the ABI types
		// TODO these IdDisplay are almost certainly wrong.
		Collection<SystemImage> images = Sdk.getCurrent().getAndroidSdkHandler().getSystemImageManager(new FakeProgressIndicator())
				.lookup(IdDisplay.create(vmTarget.getName(), vmTarget.getName()), vmTarget.getVersion(),
						IdDisplay.create(vmTarget.getVendor(), vmTarget.getVendor()));

		// in case no images are found, get try its parent
		if ((images == null) || ((images.size() == 0) && !vmTarget.isPlatform())) {
			IAndroidTarget parent = vmTarget.getParent();
			images = Sdk.getCurrent().getAndroidSdkHandler().getSystemImageManager(new FakeProgressIndicator()).lookup(
					IdDisplay.create(parent.getName(), parent.getName()), parent.getVersion(),
					IdDisplay.create(parent.getVendor(), parent.getVendor()));
		}

		// always clean abi combo since it will be reloaded
		abiTypeCombo.removeAll();

		int i = 0;
		if ((images != null) && (images.size() > 0)) {
			for (ISystemImage image : images) {
				String prettyAbiName = AvdInfo.getPrettyAbiType(image);
				abiTypeCombo.add(prettyAbiName);
				abiTypeCombo.setData(prettyAbiName, image.getAbiType());
				if (image.getAbiType().equals(abiType)) {
					abiTypeCombo.select(i);
				}
				i++;
			}

			if (abiTypeCombo.getSelectionIndex() == -1) {
				abiTypeCombo.select(0);
				abiType = (String) abiTypeCombo.getData(abiTypeCombo.getItem(0));
			}
		}

	}

	/**
	 * Populate VM Target combo box.
	 * 
	 * @param targetCombo
	 */
	private void populateTargetCombo(Combo targetCombo) {
		IAndroidTarget[] targets = SdkUtils.getAllTargets();
		if ((targets != null) && (targets.length > 0)) {
			for (int i = 0; i < targets.length; i++) {
				String label = targets[i].isPlatform() ? targets[i].getName() : targets[i].getName()
						+ " (" + targets[i].getParent().getName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
				targetCombo.add(label);
				targetCombo.setData(label, targets[i]);

				if (targets[i].getName().equals(vmTarget.getName())) {
					targetCombo.select(i);
				}
			}
		}
		// Set context Help (not available yet)
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IAndroidDeviceConstants.MAIN_PAGE_HELP);
	}

	/**
	 * Populate VM Skin combo box.
	 * 
	 * @param skinCombo
	 */
	private void populateSkinCombo(Combo skinCombo) {
		skinCombo.removeAll();
		skinCombo.clearSelection();

		if (vmTarget != null) {
			File[] skins = vmTarget.getSkins();
			File defaultSkin = vmTarget.getDefaultSkin();

			for (int i = 0; i < skins.length; i++) {
				skinCombo.add(skins[i].getName());
				skinCombo.setData(skins[i].getName(), skins[i].getName());

				if (skins[i].equals(defaultSkin)) {
					skinCombo.select(i);
					vmSkin = defaultSkin.getName();
				}
			}

			// if there is no selection, select the first
			if (skinCombo.getSelectionIndex() < 0) {
				if (skinCombo.getItemCount() > 0) {
					skinCombo.select(0);
					vmSkin = skinCombo.getItem(0);
				}
			}

			skinCombo.setEnabled(true);
		} else {
			skinCombo.setEnabled(false);
		}

		// Set context Help (not available yet)
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IAndroidDeviceConstants.MAIN_PAGE_HELP);
	}

	public String getSkinId() {
		return skinId;
	}

	/**
	 * Retrieves the timeout.
	 * 
	 * @return the timeout
	 */
	public String getTimeout() {
		return timeout;
	}

	public String getUseVnc() {
		return (useVnc ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
	}

	public String getUseProxy() {
		return (useProxy ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
	}

	/**
	 * Retrieves the error message associated to this composites current state.
	 * The order of precedence of error is the same as the fields displayed on
	 * the UI, which means errors on fields drawn first are shown with a higher
	 * precedence than the errors of fields drawn last. The instance description
	 * field is the only non required field.
	 * 
	 * @return the error message, or <code>null</code> if there are no errors
	 */
	@Override
	public String getErrorMessage() {
		String errorMessage = null;

		// VM Settings Check
		if ("".equals(vmTarget)) //$NON-NLS-1$
		{
			// Check if Target isn't empty
			errorMessage = EmulatorNLS.ERR_PropertiesMainComposite_VmTargetEmpty;
		} else if ("".equals(vmSkin)) //$NON-NLS-1$
		{
			// Check if Skin isn't empty
			errorMessage = EmulatorNLS.ERR_PropertiesMainComposite_VmSkinEmpty;
		} else {
			if (!usingDefaultVmPath) {
				// Check if Path is valid ("" is valid too)
				File vmPathLocation = new File(vmPath);
				if ((!vmPathLocation.exists()) || (!vmPathLocation.isDirectory())) {
					errorMessage = EmulatorNLS.ERR_PropertiesMainComposite_VmPathInvalid;
				}

			}
		}

		// ABI Type
		if (errorMessage == null) {
			if ((abiTypeCombo != null)
					&& ((abiTypeCombo.getItemCount() == 0) || (abiTypeCombo.getSelectionIndex() == -1))) {
				// no item available or not ABI selected
				errorMessage = EmulatorNLS.ERR_PropertiesMainComposite_ABINotAvailable;
			}
		}

		// SD Card
		if (errorMessage == null) {
			if (sdCardType.equalsIgnoreCase(SDCARD_TYPE_PATH)) {
				if (sdCardValue == null) {
					errorMessage = EmulatorNLS.ERR_PropertiesMainComposite_MissingSDCardPath;
				} else if (!isValidSDCard(sdCardValue)) {
					errorMessage = EmulatorNLS.ERR_PropertiesMainComposite_SDCardPathIsNotValid;
				}
			} else if (sdCardType.equalsIgnoreCase(SDCARD_TYPE_SIZE)) {

				int sdcardSize = -1;
				String unit = sdCardValue.length() > 0 ? sdCardValue.substring(sdCardValue.length() - 1).toLowerCase()
						: "k"; //$NON-NLS-1$

				if ((sdCardValue == null) || (sdCardValue.equals(""))) //$NON-NLS-1$
				{
					errorMessage = EmulatorNLS.ERR_PropertiesMainComposite_MissingSDCardSize;
				} else {

					try {
						sdcardSize = Integer.parseInt(sdCardValue.substring(0, sdCardValue.length() - 1));
					} catch (NumberFormatException e) {
						// do nothing
					}

					if ((unit.equals("m") && (sdcardSize < 9)) //$NON-NLS-1$
							|| (unit.equals("k") && (sdcardSize < (9 * 1024)))) //$NON-NLS-1$
					{
						errorMessage = EmulatorNLS.ERR_PropertiesMainComposite_SDCardSizeIsNotPositiveInteger;
					}
				}
			}
		}

		// Timeout
		if (errorMessage == null) {
			if (timeout.equals("")) //$NON-NLS-1$
			{
				errorMessage = EmulatorNLS.ERR_PropertiesMainComposite_MissingTimeoutValue;
			} else {
				if (!isPositiveInteger(timeout)) {
					errorMessage = EmulatorNLS.ERR_PropertiesMainComposite_TimeoutValueIsNotPositiveInteger;
				}
			}
		}

		return errorMessage;
	}

	/**
	 * Check if a string is a valid SD Card Path
	 * 
	 * @param text
	 *            the string to be analyzed
	 * @return true if the string is a valid SD Card path, false otherwise
	 */
	private boolean isValidSDCard(String text) {
		boolean result = true;

		File file = new File(text);

		if ((!file.exists()) || (file.isDirectory())
				|| (!SDCARD_PATH_EXTENSION.equals("." + (new Path(text)).getFileExtension()))) //$NON-NLS-1$
		{
			result = false;
		}

		return result;
	}

	/**
	 * Check if a string is a positive integer
	 * 
	 * @param text
	 *            the string to be analyzed
	 * @return true if the string is a positive integer, false otherwise
	 */
	private boolean isPositiveInteger(String text) {
		int intValue = 0;
		boolean isPositive = true;

		try {
			intValue = Integer.parseInt(text);
		} catch (NumberFormatException e) {
			isPositive = false;
		}

		if (intValue <= 0) {
			isPositive = false;
		}

		return isPositive;
	}

	/**
	 * Retrieves the VM Target.
	 * 
	 * @return the vmTarget
	 */
	public IAndroidTarget getVmTarget() {
		return vmTarget;
	}

	/**
	 * Retrieves the Abi Type.
	 * 
	 * @return the VM Abi Type
	 */
	public String getAbiType() {
		return abiType != null ? abiType : SdkConstants.ABI_ARMEABI;
	}

	/**
	 * Retrieves the VM Skin.
	 * 
	 * @return the vmSkin
	 */
	public String getVmSkin() {
		return vmSkin;
	}

	/**
	 * Retrieves the VM Path.
	 * 
	 * @return the vmPath
	 */
	public String getVmPath() {
		return vmPath + File.separator + name + IDevicePropertiesConstants.defaultVmFolderSuffix;
	}

	/**
	 * Retrieves the SD Card info
	 * 
	 * @return the sdCard
	 */
	public String getSDCard() {
		return sdCardValue;
	}

	/**
	 * Set the name of the AVD
	 * 
	 * @param name
	 *            : a not null String with the name of the AVD
	 */
	public void setName(String name) {
		this.name = name == null ? "" : name; //$NON-NLS-1$
	}

	/**
	 * @return
	 */
	public String getUseSnapshot() {
		return (useSnapshots ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
	}

	/**
	 * @return
	 */
	public String getstartFromSnapshot() {
		return (startFromSnapshots ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
	}

	/**
	 * @return
	 */
	public String getSaveSnapshot() {
		return (saveSnapshots ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
	}
}
