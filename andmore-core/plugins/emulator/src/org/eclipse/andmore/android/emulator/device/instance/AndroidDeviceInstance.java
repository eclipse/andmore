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
package org.eclipse.andmore.android.emulator.device.instance;

import static org.eclipse.andmore.android.common.log.AndmoreLogger.error;
import static org.eclipse.andmore.android.common.log.AndmoreLogger.info;
import static org.eclipse.andmore.android.common.log.AndmoreLogger.warn;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.andmore.android.DDMSFacade;
import org.eclipse.andmore.android.ISerialNumbered;
import org.eclipse.andmore.android.SdkUtils;
import org.eclipse.andmore.android.emulator.EmulatorPlugin;
import org.eclipse.andmore.android.emulator.core.exception.InstanceStopException;
import org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance;
import org.eclipse.andmore.android.emulator.core.model.IInputLogic;
import org.eclipse.andmore.android.emulator.device.IDevicePropertiesConstants;
import org.eclipse.andmore.android.emulator.device.definition.AndroidEmuDefMgr;
import org.eclipse.andmore.android.emulator.device.instance.options.StartupOptionsMgt;
import org.eclipse.andmore.android.emulator.i18n.EmulatorNLS;
import org.eclipse.andmore.android.emulator.logic.AbstractStartAndroidEmulatorLogic;
import org.eclipse.andmore.android.emulator.logic.AndroidLogicUtils;
import org.eclipse.andmore.android.emulator.logic.IAndroidLogicInstance;
import org.eclipse.andmore.android.emulator.logic.stop.AndroidEmulatorStopper;
import org.eclipse.andmore.android.nativeos.NativeUIUtils;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.sequoyah.device.framework.model.AbstractMobileInstance;
import org.eclipse.sequoyah.device.framework.model.handler.ServiceHandler;
import org.eclipse.sequoyah.vnc.protocol.PluginProtocolActionDelegate;
import org.eclipse.sequoyah.vnc.protocol.lib.ProtocolHandle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.model.IWorkbenchAdapter;

import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.internal.avd.AvdInfo;
import com.android.sdklib.repository.targets.AndroidTargetManager;

/**
 * DESCRIPTION: This class represents a Android Emulator instance
 *
 * RESPONSIBILITY: - Hold all attributes of an Android Emulator instance -
 * Provide methods for testing if started and to stop the instance
 *
 * COLABORATORS: None.
 *
 * USAGE: This class is not meant to be used directly by the user. All commands
 * to the Android Emulator instance shall be provided through the device
 * framework.
 */
public class AndroidDeviceInstance extends AbstractMobileInstance implements IAndroidLogicInstance, IWorkbenchAdapter,
		ISerialNumbered {
	/**
	 * The protocol that is executed by this instance
	 */
	private ProtocolHandle handle;

	/**
	 * True if this instance has and supports CLI display; false otherwise
	 */
	private boolean hasCli = false;

	/**
	 * Current layout of this instance
	 */
	private String currentLayout;

	private Process process;

	private long windowHandle;

	private Composite composite;

	/**
	 * Tests if this instance is in started or stopped state
	 *
	 * @return true if started, false if stopped
	 */
	@Override
	public boolean isStarted() {
		boolean instanceStarted = false;
		String status = getStatus();
		if (EmulatorPlugin.STATUS_ONLINE_ID.equals(status)) {
			instanceStarted = true;
		}

		return instanceStarted;
	}

	@Override
	public boolean isConnected() {
		if (super.getProperties().getProperty(IDevicePropertiesConstants.useVnc, NativeUIUtils.getDefaultUseVnc())
				.equals("true")) {
			ProtocolHandle protocolHandle = getProtocolHandle();
			if (protocolHandle != null) {
				return PluginProtocolActionDelegate.isProtocolRunning(protocolHandle);
			}
			return false;
		} else {
			return isStarted();
		}

	}

	/**
	 * Method used by the emulator core to stop the instance on errors
	 * 
	 * @see IAndroidEmulatorInstance#stop(boolean)
	 * 
	 * @param force
	 *            True if no interaction with the user is desired to perform the
	 *            stop operation; false otherwise
	 */
	@Override
	public void stop(boolean force) throws InstanceStopException {

		info("Stopping the Android Emulator instance: " + this);

		// FIRST SCENARIO: THE INSTANCE IS STARTED AND FAILED DURING OPERATION
		// If the instance is in started state, the stop handler is called to
		// delegate the state
		// maintenance to TmL, which is correct.
		if (isStarted()) {
			if (getStateMachineHandler().isTransitioning()) {
				// Free other threads to continue their jobs if one is already
				// running the procedure
				info("The instance is already executing a stop process:  " + this);
				throw new InstanceStopException("The instance is already executing a stop process");
			} else {
				info("Instance is started. Run the regular transition to stopped status: " + this);
				ServiceHandler stopHandler = EmulatorPlugin.getStopServiceHandler();
				if (stopHandler != null) {
					try {
						Map<Object, Object> args = new HashMap<Object, Object>();
						args.put(EmulatorPlugin.FORCE_ATTR, true);
						stopHandler.run(this, args);
					} catch (Exception e) {
						// Should not enter here, because the state has been
						// tested before
						error("The instance is not in an appropriate state for stopping");
					}
					info("Finished stop process: " + this);
				}
			}
		}
		// SECOND SCENARIO: THE INSTANCE WAS STARTING AND FAILED DURING THE
		// PROCESS
		// If the instance is not in started state, TmL will not allow the stop
		// service to run. In
		// this case, the methods must be called manually, and the state does
		// not need maintenance
		// (it has never been updated, as updating happens in the end of a
		// transition)
		else {
			if (getStateMachineHandler().isTransitioning()) {

				info("Instance is not fully started yet. Execute a stop process directly..." + this);
				final Job haltJob = new Job(EmulatorNLS.UI_AndroidDeviceInstance_StopInstanceJob) {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						AndroidEmulatorStopper.stopInstance(AndroidDeviceInstance.this, true, true, monitor);

						if (getStatus().equals(EmulatorPlugin.STATUS_OFFLINE_NO_DATA)) {
							info("Instance was initially in stopped/clean status. Rollback if needed." + this);

							File userdataFile = getUserdata();
							if ((userdataFile != null) && userdataFile.exists()) {
								info("Deleted data created during the start tentative." + userdataFile);
								userdataFile.delete();
							}
						}

						info("Finished stop process: " + AndroidDeviceInstance.this);
						return Status.OK_STATUS;
					}
				};
				haltJob.schedule();
			}
		}
	}

	/**
     * 
     */
	@Override
	public Properties getProperties() {
		Properties properties = super.getProperties();

		// synchronize instance properties data with current sdk...
		File fromSdk = SdkUtils.getUserdataDir(getName());
		if (fromSdk != null) {
			properties.put(IDevicePropertiesConstants.vmPath, fromSdk.getAbsolutePath());
		}

		return properties;
	}

	/**
	 * @see IAndroidEmulatorInstance#getHasCli()
	 */
	@Override
	public boolean getHasCli() {
		return hasCli;
	}

	/**
	 * @see IAndroidEmulatorInstance#getInstanceIdentifier()
	 */
	@Override
	public String getInstanceIdentifier() {
		return getSerialNumber();
	}

	/**
	 * @see IAndroidEmulatorInstance#getProtocolHandle()
	 */
	@Override
	public ProtocolHandle getProtocolHandle() {
		return handle;
	}

	/**
	 * @see IAndroidEmulatorInstance#getEmulatorDefId()
	 */
	public String getEmulatorDefId() {
		return getProperties().getProperty(IDevicePropertiesConstants.emulatorDefId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance
	 * #getCurrentLayout()
	 */
	@Override
	public String getCurrentLayout() {
		return currentLayout;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance
	 * #setCurrentLayout(java.lang.String)
	 */
	@Override
	public void setCurrentLayout(String layoutName) {
		currentLayout = layoutName;
	}

	/**
	 * @see IAndroidEmulatorInstance#setHasCli(boolean)
	 */
	@Override
	public void setHasCli(boolean hasCli) {
		this.hasCli = hasCli;
	}

	/**
	 * @see IAndroidEmulatorInstance#setProtocolHandle(ProtocolHandle)
	 */
	@Override
	public void setProtocolHandle(ProtocolHandle handle) {
		this.handle = handle;
	}

	/**
	 * Resets all the previous runtime state to clean them for next execution
	 */
	void resetRuntimeVariables() {
		handle = null;
		hasCli = false;
		currentLayout = null;
	}

	/**
	 * This version of getAdapter needs to assure that only an Android Device
	 * instance is compatible with itself
	 * 
	 * @see IAdaptable#getAdapter(Class)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class adapter) {
		if (adapter.isInstance(this)) {
			return this;
		} else {
			return null;
		}
	}

	/**
	 * Retrieves the default properties to be used upon instance creation
	 * 
	 * @param defaultProperties
	 *            property object to be filled
	 * @param vmSkin
	 *            Android VM skin
	 */
	public static void populateWithDefaultProperties(Properties defaultProperties) {

		AndroidEmuDefMgr emuDefMgr = AndroidEmuDefMgr.getInstance();

		// When removing the emulator definition extension, remove this
		// hardcoded set as
		// well as the constant declaration from the Activator. If the Mot skin
		// plugin is used,
		// find another way to set this variable
		String emuDefId = EmulatorPlugin.DEFAULT_EMULATOR_DEFINITION;

		// Emulator Definition
		defaultProperties.setProperty(IDevicePropertiesConstants.emulatorDefId, emuDefId);

		// skin from Emulator Definition
		defaultProperties.setProperty(IDevicePropertiesConstants.skinId, emuDefMgr.getSkinId(emuDefId));

		// command line arguments from Emulator Definition
		defaultProperties.setProperty(IDevicePropertiesConstants.commandline,
				emuDefMgr.getCommandLineArgumentsForEmuDefinition(emuDefId));

		// default timeout
		defaultProperties.setProperty(IDevicePropertiesConstants.timeout,
				IDevicePropertiesConstants.defaultTimeoutValue);

		// default useVnc
		defaultProperties.setProperty(IDevicePropertiesConstants.useVnc, NativeUIUtils.getDefaultUseVnc());

		// default useProxy
		defaultProperties.setProperty(IDevicePropertiesConstants.useProxy,
				IDevicePropertiesConstants.defaultUseProxyValue);
	}

	/**
	 * Populate VM Target and VM skin
	 * 
	 * @param instanceName
	 * @param instanceProperties
	 */
	public static void populateWithVMInfo(String instanceName, Properties instanceProperties) {
		AvdInfo vmInfo = SdkUtils.getValidVm(instanceName);

		if (vmInfo != null) {
			// VM target
			instanceProperties.setProperty(IDevicePropertiesConstants.vmTarget, SdkUtils.getCurrentSdk().getAndroidTargetFor(vmInfo).getName());

			// ABI Type
			instanceProperties.setProperty(IDevicePropertiesConstants.abiType, vmInfo.getAbiType());

			// VM skin
			instanceProperties.setProperty(IDevicePropertiesConstants.vmSkin, SdkUtils.getSkin(vmInfo));

			// VM path
			instanceProperties.setProperty(IDevicePropertiesConstants.vmPath, vmInfo.getDataFolderPath());
			String useSnapShot = vmInfo.getProperties().get("snapshot.present");
			if (useSnapShot == null) {
				useSnapShot = "false";
			}

			instanceProperties.setProperty(IDevicePropertiesConstants.useSnapshots, useSnapShot);
			if (instanceProperties.getProperty(IDevicePropertiesConstants.startFromSnapshot) == null) {
				instanceProperties.setProperty(IDevicePropertiesConstants.startFromSnapshot, useSnapShot);
			}
			if (instanceProperties.getProperty(IDevicePropertiesConstants.saveSnapshot) == null) {
				instanceProperties.setProperty(IDevicePropertiesConstants.saveSnapshot, useSnapShot);
			}
		}
	}

	@Override
	public String getSkinId() {
		return getProperties().getProperty(IDevicePropertiesConstants.skinId);
	}

	@Override
	@SuppressWarnings("restriction")
	public File getSkinPath() {
		File skinFile = null;

		AvdInfo avdInfo = SdkUtils.getValidVm(getName());
		if (avdInfo != null) {
			String skinPath = avdInfo.getProperties().get("skin.path");
			skinPath = new File(SdkUtils.getCurrentSdk().getSdkFileLocation(), skinPath).getAbsolutePath();
			
			IAndroidTarget target = SdkUtils.getCurrentSdk().getAndroidTargetFor(avdInfo);
			File candidateFile = new File(skinPath);
			// If path specified on the skin does not exist, try to retrieve it
			// from the target.
			if (!candidateFile.exists()) {
				candidateFile = SdkUtils.getSkinFolder(SdkUtils.getSkin(avdInfo), target);
			}
			if (!target.isPlatform()) {
				if (!candidateFile.isDirectory()) {
					IAndroidTarget baseTarget = target.getParent();
					skinPath = getSkinFolderPath(baseTarget);
					skinFile = new File(skinPath);
				} else {
					skinFile = candidateFile;
				}
			} else {
				skinFile = candidateFile;
			}
		}
		return skinFile;
	}

	private String getSkinFolderPath(IAndroidTarget target) {
		String vmSkin = getProperties().getProperty(IDevicePropertiesConstants.vmSkin);
		return target.getLocation() + File.separator + "skins" + File.separator + vmSkin;
	}

	/**
	 * Get the timeout in milliseconds
	 * 
	 * @see org.eclipse.andmore.android.emulator.logic.IAndroidLogicInstance#getTimeout()
	 */
	@Override
	public int getTimeout() {
		int timeout = 0;
		String timeoutString = null;
		try {
			info("Try to get Timeout property from " + this);
			Properties instanceProps = getProperties();
			timeoutString = instanceProps.getProperty(IDevicePropertiesConstants.timeout);
			timeout = Integer.parseInt(timeoutString) * 1000; // convert to
																// milis
		} catch (Exception e) {
			warn("Unnable to parse timeout string:" + timeoutString);
			timeout = Integer.parseInt(IDevicePropertiesConstants.defaultTimeoutValue) * 1000;
		}

		return timeout;
	}

	public IAndroidTarget getAndroidTarget() {
		IAndroidTarget result = null;
		AvdInfo avdInfo = SdkUtils.getValidVm(getName());
		if (avdInfo != null) {
			result = SdkUtils.getCurrentSdk().getAndroidTargetFor(avdInfo);
		}
		return result;
	}

	@Override
	public String getTarget() {
		String result = null;
		IAndroidTarget target = getAndroidTarget();
		if (target != null) {
			result = target.getName();
		}
		return result;
	}

	@Override
	public int getAPILevel() {
		int result = -1;
		IAndroidTarget target = getAndroidTarget();
		if (target != null) {
			result = target.getVersion().getApiLevel();
		}
		return result;
	}

	@Override
	public String getCommandLineArguments() {
		return getProperties().getProperty(IDevicePropertiesConstants.commandline,
				NativeUIUtils.getDefaultCommandLine());
	}

	@Override
	public AbstractStartAndroidEmulatorLogic getStartLogic() {
		String emuDefinition = getEmulatorDefId();
		AndroidEmuDefMgr definitionManager = AndroidEmuDefMgr.getInstance();
		return definitionManager.getStartLogic(emuDefinition);
	}

	@Override
	public IInputLogic getInputLogic() {
		String emuDefinition = getEmulatorDefId();
		AndroidEmuDefMgr definitionManager = AndroidEmuDefMgr.getInstance();
		return definitionManager.getInputLogic(emuDefinition, this);
	}

	@Override
	public boolean hasDevice() {
		return (DDMSFacade.getDeviceBySerialNumber(getSerialNumber()) != null);
	}

	@Override
	public void changeOrientation(final String parameters) {

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					DDMSFacade.execRemoteApp(getSerialNumber(),
							AndroidLogicUtils.ORIENTATION_BASE_COMMAND + parameters, new NullProgressMonitor());
				} catch (IOException e) {
					error("Failed to send the command to change the emulator display orientation to portrait.");
				}

			}
		}).start();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.emulator.logic.IAndroidLogicInstance#getUserdata
	 * ()
	 */
	@Override
	public File getUserdata() {
		return SdkUtils.getUserdataFile(getName());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.emulator.logic.IAndroidLogicInstance#
	 * getSnapshotOriginalFilePath()
	 */
	@Override
	public File getSnapshotOriginalFilePath() {
		String snapshotFilePath;
		File snapshotOriginalFile = null;
		snapshotFilePath = SdkUtils.getSdkToolsPath() + "lib" + File.separator + "emulator" + File.separator
				+ "snapshots.img";
		snapshotOriginalFile = new File(snapshotFilePath);
		if (!snapshotOriginalFile.exists()) {
			snapshotOriginalFile = null;
		}
		return snapshotOriginalFile;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.emulator.logic.IAndroidLogicInstance#getStateData
	 * ()
	 */
	@Override
	public List<File> getStateData() {
		return SdkUtils.getStateDataFiles(getName());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.emulator.logic.IAndroidLogicInstance#isClean
	 * ()
	 */
	@Override
	public boolean isClean() {
		boolean userdataExists = false;
		File userdataFile = getUserdata();
		if ((userdataFile != null) && (userdataFile.exists())) {
			userdataExists = true;
		}

		return !userdataExists;
	}

	@Override
	public String toString() {
		// Do not use getInstanceIdentifier method here (it is used by several
		// logs - high exposure - and leads to synchronized methods that may
		// cause deadlocks).
		return getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance
	 * #getProcess()
	 */
	@Override
	public Process getProcess() {
		return process;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance
	 * #setProcess(java.lang.Process)
	 */
	@Override
	public void setProcess(Process process) {
		this.process = process;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance
	 * #setWindowHandle(int)
	 */
	@Override
	public long getWindowHandle() {
		return windowHandle;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance
	 * #getWindowHandle()
	 */
	@Override
	public void setWindowHandle(long handle) {
		windowHandle = handle;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.adt.ISerialNumbered#getDeviceName()
	 */
	@Override
	public String getDeviceName() {
		return getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance
	 * #getFullName()
	 */
	@Override
	public String getFullName() {
		String suffix = getNameSuffix();
		if (suffix != null) {
			return getName() + " (" + suffix + ")";
		} else {
			return getName();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object o) {
		return new Object[0];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.model.IWorkbenchAdapter#getImageDescriptor(java.lang.Object
	 * )
	 */
	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getLabel(java.lang.Object)
	 */
	@Override
	public String getLabel(Object o) {
		return getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object o) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.adt.ISerialNumbered#getSerialNumber()
	 */
	@Override
	public String getSerialNumber() {
		return DDMSFacade.getSerialNumberByName(getName());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.emulator.core.model.IAndroidEmulatorInstance
	 * #isAvailable()
	 */
	@Override
	public boolean isAvailable() {
		return !getStatus().equals(EmulatorPlugin.STATUS_NOT_AVAILABLE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.emulator.logic.IAndroidLogicInstance#
	 * getCommandLineArgumentsAsProperties()
	 */
	@Override
	public Properties getCommandLineArgumentsAsProperties() {
		return StartupOptionsMgt.parseCommandLine(getCommandLineArguments());

	}

	@Override
	public Composite getComposite() {
		return composite;
	}

	@Override
	public void setComposite(Composite composite) {
		this.composite = composite;
	}

}