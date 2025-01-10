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

package org.eclipse.andmore.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.andmore.AndmoreAndroidPlugin;
import org.eclipse.andmore.android.AndmoreEventManager.EventType;
import org.eclipse.andmore.android.common.IAndroidConstants;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.log.AndmoreLogger;
import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.andmore.android.utilities.TelnetFrameworkAndroid;
import org.eclipse.andmore.ddms.DdmsPlugin;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.andmore.service.AdtStartupService;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.AndroidDebugBridge.IClientChangeListener;
import com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener;
import com.android.ddmlib.Client;
import com.android.ddmlib.ClientData;
import com.android.ddmlib.ClientData.HprofData;
import com.android.ddmlib.EmulatorConsole;
import com.android.ddmlib.FileListingService;
import com.android.ddmlib.FileListingService.FileEntry;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.IDevice.DeviceState;
import com.android.ddmlib.MultiLineReceiver;
import com.android.ddmlib.SyncException;
import com.android.ddmlib.SyncService;
import com.android.ddmlib.SyncService.ISyncProgressMonitor;

/**
 * DESCRIPTION: This class is the common interface to functionalities from DDMS
 *
 * RESPONSIBILITY: Centralizes the access to DDMS.
 *
 * COLABORATORS: None.
 *
 * USAGE: Use the public methods to use DDMS
 */
public class DeviceMonitor implements IClientChangeListener, IDeviceChangeListener {
	/**
	 * Defines a stop condition
	 */
	interface IStopCondition {
		public boolean canStop();
	}

	/**
	 * The plug-in ID
	 */
	public static final String PLUGIN_ID = "org.eclipse.andmore.android";

	/**
	 * Command for switching back to USB connection mode.
	 */
	private static final String USB_SWITCH_BACK_COMMAND = "usb"; //$NON-NLS-1$

	/**
	 * Argument which indicates the device to apply a certain command.
	 */
	private static final String DEVICE_ID_INDICATOR = "-s"; //$NON-NLS-1$

	private static final String DEFAULT_WIRELESS_DEVICE_PROPERTY = "tiwlan0"; //$NON-NLS-1$

	/**
	 * Folder located inside the SDK folder containing some sdk tools.
	 */
	final static String TOOLS_FOLDER = IAndroidConstants.FD_TOOLS;

	/**
	 * Folder located inside the SDK folder and containing the ADB.
	 */
	final static String PLATFORM_TOOLS_FOLDER = IAndroidConstants.FD_PLATFORM_TOOLS;

	/**
	 * adb (android debug bridge) command.
	 */
	final static String ADB_COMMAND = "adb"; //$NON-NLS-1$

	/**
	 * Command to concatenate with "adb" to have the device shell.
	 */
	final static String SHELL_CMD = "shell"; //$NON-NLS-1$

	/**
	 * Options to be used with adb to indicate run operation.
	 */
	private static final String AM_CMD = "am"; //$NON-NLS-1$

	/**
	 * Command to concatenate with "am" to have an activity executed at the
	 * device.
	 */
	private static final String START_CMD = "start"; //$NON-NLS-1$

	/**
	 * Parameter for running in debug mode.
	 */
	private static final String ADB_AM_DEBUG = "-D"; //$NON-NLS-1$

	/**
	 * Parameter provided before the application package/name.
	 */
	private static final String ADB_AM_NAME = "-n"; //$NON-NLS-1$

	/**
	 * Parameter for selecting emulator instance.
	 */
	final static String ADB_INSTANCE_PARAMETER = DEVICE_ID_INDICATOR;

	/**
	 * Folder for the SDK.
	 */
	private static final String SDCARD_FOLDER = "sdcard"; //$NON-NLS-1$

	/**
	 * Folder for the SDK.
	 */
	private static final String MNT_SDCARD_FOLDER = "mnt/sdcard"; //$NON-NLS-1$

	/*
	 * TCP/IP
	 */
	private static final String CONNECT_TCPIP_CMD = "connect"; //$NON-NLS-1$

	private static final String DISCONNECT_TCPIP_CMD = "disconnect"; //$NON-NLS-1$

	private static final String TCPIP_CMD = "tcpip"; //$NON-NLS-1$

	private static final String IFCONFIG_CMD = "ifconfig"; //$NON-NLS-1$

	/**
	 * Property from device which represents the wi-fi value to use ipconfig
	 * command.
	 */
	private static final String WIFI_INTERFACE_DEVICE_PROPERTY = "wifi.interface"; //$NON-NLS-1$

	// IP validation
	private static final String ZERO_TO_255_PATTERN = "((\\d)|(\\d\\d)|([0-1]\\d\\d)|(2[0-4]\\d)|(25[0-5]))"; //$NON-NLS-1$

	private static final String IP_PATTERN = "(" + ZERO_TO_255_PATTERN + "\\." //$NON-NLS-1$ //$NON-NLS-2$
			+ ZERO_TO_255_PATTERN + "\\." + ZERO_TO_255_PATTERN + "\\." + ZERO_TO_255_PATTERN //$NON-NLS-1$ //$NON-NLS-2$
			+ ")+"; //$NON-NLS-1$

	/** Maximum time to wait for notification */
	private static final long MAX_WAIT_TIME = 20000;
	private static final IDevice NULL_DEVICE;

	private static final String NULL_SERIAL_NUMBER = "%s called with null serial number";

	private static DeviceMonitor singleton;
	
	static 
	{
		NULL_DEVICE = new NullDevice();
	}
	
	private String sdkPath;
	
	/**
	 * Map containing all connected devices. It is being kept for us not to
	 * depend on ADT every time we need one, preventing deadlocks.
	 */
	private final Map<String, IDevice> connectedDevices = new ConcurrentHashMap<String, IDevice>();

	/**
	 * Set containing the serial numbers of the devices completely loaded. A
	 * device is considered completely loaded if it has already loaded the HOME
	 * application.
	 */
	private final Set<String> completelyUpDevices = new HashSet<String>();

	private Map<String, String> avdNameMap = new HashMap<String, String>();
	/** Set of client PIDs used for notification */
	private final HashMap<Integer,Integer> clientPidSet = new HashMap<>();
	private Object consoleLock = new Object();

	/**
	 * Private constructor to enforce singletion
	 */
	private DeviceMonitor() {
		
	}
	
	/**
	 * Get all connected device serial numbers
	 * 
	 * @return
	 */
	public Collection<String> getConnectedSerialNumbers() {
		List<String> deviceList = new ArrayList<>();
		deviceList.addAll(connectedDevices.keySet());
		return deviceList;
	}

	/**
	 * Get the Device associated with the given serial number
	 * 
	 * @param serialNumber
	 *            Serial number of the device to retrieve
	 * @return Device associated with the given serial number
	 */
	public IDevice getDeviceBySerialNumber(String serialNumber) {
		if (serialNumber == null) {
			AndmoreLogger.error(String.format(NULL_SERIAL_NUMBER, "getDeviceBySerialNumber"));
			return NULL_DEVICE;
		}
		return connectedDevices.get(serialNumber);
	}

	/**
	 * Runs an activity at the given device
	 * 
	 * @param serialNumber
	 *            The serial number of the device to have the activity executed
	 * @param activityName
	 *            The activity to execute
	 * @param debugMode
	 *            Whether the activity shall be run in debug mode or not
	 * @param processOut
	 *            The output stream of the process running "adb"
	 * 
	 * @return An IStatus object with the result of the operation
	 */
	public IStatus runActivity(String serialNumber, String activityName, boolean debugMode,
			OutputStream processOut) {
		IStatus status = Status.OK_STATUS;

		// Return if no instance is selected
		if (serialNumber == null) {
			AndmoreLogger.error("Abort run operation. Serial number is null."); //$NON-NLS-1$
			return new Status(IStatus.ERROR, PLUGIN_ID, AndroidNLS.ERR_DDMSFacade_SerialNumberNullPointer);
		}

		// Return if instance is not started
		if (!isDeviceOnline(serialNumber)) {
			AndmoreLogger.error("Abort run operation. Device is not online."); //$NON-NLS-1$
			return new Status(IStatus.ERROR, PLUGIN_ID, ""); //$NON-NLS-1$
		}

		try {
			String[] cmd = createRunCommand(serialNumber, activityName, debugMode);
			executeCommand(cmd, processOut);
		} catch (IOException e) {
			AndmoreLogger.error("Deploy: Could not execute adb install command."); //$NON-NLS-1$
			status = new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage());
		}

		return status;
	}

	public String executeCommand(String[] cmd, OutputStream out) throws IOException {
		return executeCommand(cmd, out, null);
	}

	/**
	 * Check if the device is Online (i.e. if it's possible to communicate with
	 * it) Notice it is a verification of the status of the Device which may be
	 * different than the status of the Tml Instance.
	 * 
	 * @param serialNumber
	 * @return true if the Device is online, false otherwise
	 */
	public boolean isDeviceOnline(String serialNumber) {
		if (serialNumber == null) {
			AndmoreLogger.error(String.format(NULL_SERIAL_NUMBER, "isDeviceOnline"));
			return false;
		}
		IDevice device = getDeviceBySerialNumber(serialNumber);
		if ((device == null) || !device.isOnline()) {
			return false;
		}
		return true;
	}

	/**
	 * Return true if the Device is being shown on the OFFLINE state.
	 * 
	 * @param serialNumber
	 *            Device�s serial number.
	 * 
	 * @return <code>true</code> in case the Device if offline,
	 *         <code>false</code> otherwise.
	 */
	public boolean isDeviceOffline(String serialNumber) {
		if (serialNumber == null) {
			AndmoreLogger.error(String.format(NULL_SERIAL_NUMBER, "isDeviceOffline"));
			return true;
		}
		IDevice device = getDeviceBySerialNumber(serialNumber);
		return ((device == null) || ((device != null) && device.isOffline()));
	}

	/**
	 * Check if the device is completely loaded A device is completely loaded
	 * when it loads the HOME application
	 * 
	 * @param serialNumber
	 * @return true if the Device has completely loaded; false otherwise
	 */
	public boolean isDeviceCompletelyLoaded(String serialNumber) {
		if (serialNumber == null) {
			AndmoreLogger.error(String.format(NULL_SERIAL_NUMBER, "isDeviceComplietelyLoaded"));
			return false;
		}
		boolean isCompletelyLoaded = false;
		synchronized (completelyUpDevices) {
			isCompletelyLoaded = completelyUpDevices.contains(serialNumber);
		}
		return isCompletelyLoaded;
	}

	/**
	 * Tests if the device represented by the serial number (if it exists) is an
	 * emulator
	 * 
	 * @param serialNumber
	 * @return true if it is an emulator, false if not or non existent
	 */
	public boolean isEmulator(String serialNumber) {
		if (serialNumber == null) {
			AndmoreLogger.error(String.format(NULL_SERIAL_NUMBER, "isEmulator"));
			return false;
		}
		IDevice device = getDeviceBySerialNumber(serialNumber);
		if ((device != null) && device.isEmulator()) {
			return true;
		}
		return false;
	}

	public boolean isRemote(String serialNumber) {
		if (serialNumber == null) {
			AndmoreLogger.error(String.format(NULL_SERIAL_NUMBER, "isRemote"));
			return false;
		}
		// firstly, test if the serial number has the format "anything:digits"
		Pattern p = Pattern.compile("(.)+:(\\d)+"); //$NON-NLS-1$
		Matcher m = p.matcher(serialNumber);
		if (m.matches()) {
			IDevice device = getDeviceBySerialNumber(serialNumber);
			if ((device != null) && !device.isEmulator()) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Execute an app in the Device
	 * 
	 * @param serialNumber
	 *            Serial number of the device where to execute the command
	 * @param remoteCommand
	 *            command to be executed remotely on the Device
	 * @param monitor
	 *            monitor associated with the operation
	 * 
	 * @return The lines read from the command output
	 * 
	 * @throws IOException
	 */
	public Collection<String> execRemoteApp(String serialNumber, String remoteCommand,
			final IProgressMonitor monitor) throws IOException {
		return executeShellCmd(serialNumber, remoteCommand, monitor);
	}

	/**
	 * Execute an app in the Device
	 * 
	 * @param serialNumber
	 *            Serial number of the device where to execute the command
	 * @param remoteCommands
	 *            commands to be executed remotely on the Device
	 * @param monitor
	 *            monitor associated with the operation
	 * 
	 * @throws IOException
	 */
	public Map<String, Collection<String>> execRemoteApp(String serialNumber, Collection<String> remoteCommands,
			final IProgressMonitor monitor) throws IOException {
		if (serialNumber == null) {
			AndmoreLogger.error(String.format(NULL_SERIAL_NUMBER, "execRemoteApp"));
			return Collections.emptyMap();
		}
		Map<String, Collection<String>> cmdAnswers = new LinkedHashMap<String, Collection<String>>();
		for (String remoteCommand : remoteCommands) {
			AndmoreLogger.debug(remoteCommand);
			Collection<String> answers = executeShellCmd(serialNumber, remoteCommand, monitor);
			cmdAnswers.put(remoteCommand, answers);
		}

		return cmdAnswers;
	}

	/**
	 * DOCUMENT ME!!
	 * 
	 * @param cmd
	 * @param out
	 * @param serialNumber
	 * @return
	 * @throws IOException
	 */
	public String executeCommand(String[] cmd, OutputStream out, String serialNumber) throws IOException {
		String fullCmd = ""; //$NON-NLS-1$
		if (out != null) {
			for (String cmdArg : cmd) {
				fullCmd += cmdArg + " "; //$NON-NLS-1$
			}
			out.write(fullCmd.getBytes());
			out.write("\n".getBytes()); //$NON-NLS-1$
		}

		Runtime r = Runtime.getRuntime();
		Process p = r.exec(cmd);

		String command_results = ""; //$NON-NLS-1$
		InputStream processIn = p.getInputStream();
		final BufferedReader br = new BufferedReader(new InputStreamReader(processIn));
		String line;
		try {
			while ((line = br.readLine()) != null) {
				command_results += line;
				command_results += "\n"; //$NON-NLS-1$
				if (out != null) {
					if (serialNumber != null) {
						out.write((serialNumber + ": ").getBytes()); //$NON-NLS-1$
					}
					out.write(line.getBytes());
					out.write("\n".getBytes()); //$NON-NLS-1$
				}
			}
		} finally {
			br.close();
		}

		return command_results;
	}

	public void deleteFile(String serialNumber, String path) throws IOException {
		execRemoteApp(serialNumber, "rm " + path, //$NON-NLS-1$
				new NullProgressMonitor());
	}


	/**
	 * Retrieves all properties from the device with provided serial number.
	 * 
	 * @param serialNumber
	 * @return
	 */
	public Properties getDeviceProperties(String serialNumber) {
		Properties instanceProperties = new Properties();
		if (serialNumber != null) {
			String key = ""; //$NON-NLS-1$
			String value = ""; //$NON-NLS-1$
			Collection<String> lines;
			try {
				lines = execRemoteApp(serialNumber, "getprop", new NullProgressMonitor()); //$NON-NLS-1$

				for (String line : lines) {
					String[] split = line.split("]:"); //$NON-NLS-1$
					if (split.length >= 2) {
						if (!split[0].equals("")) //$NON-NLS-1$
						{
							key = split[0].substring(1, split[0].length());
							if (!split[1].equals("")) //$NON-NLS-1$
							{
								value = split[1].substring(2, split[1].length() - 1);
								instanceProperties.setProperty(key, value);
							}
						}

					}
				}
			} catch (IOException e) {
				AndmoreLogger.error("IOException while executing an app on device. " + e.getMessage());
			}
		}
		return instanceProperties;
	}

	/**
	 * Retrieves a given property from the device with provided serial number.
	 * 
	 * @param serialNumber
	 * @param propertyName
	 * 
	 * @return
	 */
	public String getDeviceProperty(String serialNumber, String propertyName) {
		if (serialNumber == null) {
			AndmoreLogger.error(String.format(NULL_SERIAL_NUMBER, "getDeviceProperty"));
			return "";
		}
		String result = null;
		IDevice device = getDeviceBySerialNumber(serialNumber);
		if (device != null) {
			result = device.getProperty(propertyName);
		}
		return result;
	}

    public void start() throws InterruptedException {
		Job adbJob = new Job("Start Device Monitor") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				AndroidDebugBridge adb = AndroidDebugBridge.getBridge();
				if (adb == null) {
					AndroidDebugBridge.disconnectBridge();
					DdmsPlugin.setToolsLocation(AndmoreAndroidPlugin.getOsAbsoluteAdb(), true, AndmoreAndroidPlugin.getOsAbsoluteHprofConv(),
							AndmoreAndroidPlugin.getOsAbsoluteTraceview());
				}

				if ((adb != null) && adb.hasInitialDeviceList()) {
					IDevice[] x = adb.getDevices();
					IDevice[] newDevices = x;
					List<IDevice> oldDevList = new ArrayList<IDevice>(connectedDevices.values());

					for (IDevice newDev : newDevices) {
						String serialNum = newDev.getSerialNumber();
						if (connectedDevices.containsKey(serialNum)) {
							IDevice oldDev = connectedDevices.get(serialNum);
							oldDevList.remove(oldDev);
							if (oldDev.getState().compareTo((newDev).getState()) != 0) {
								if ((newDev).getState() == DeviceState.OFFLINE) {
									deviceDisconnected(newDev);
								} else if ((newDev).getState() == DeviceState.ONLINE) {
									deviceConnected(newDev);
								}
							}
						} else {
							deviceConnected(newDev);
						}
					}
					for (IDevice oldDev : oldDevList) {
						deviceDisconnected(oldDev);
					}
				}
		    	sdkPath = AndmoreAndroidPlugin.getOsSdkFolder();
				return Status.OK_STATUS;
			}
		};
		adbJob.setPriority(Job.BUILD);
		AdtStartupService.instance().put(adbJob);
    }

	/**
	 * Create port forward for a given VM
	 * 
	 * @param serialNumber
	 *            Android serial number
	 * @param from
	 *            port number from
	 * @param to
	 *            port number to
	 * @return true is the port forward was successful, false otherwise
	 */
	public boolean createForward(String serialNumber, int from, int to) {
		if (serialNumber == null) {
			AndmoreLogger.error(String.format(NULL_SERIAL_NUMBER, "createForward"));
			return false;
		}
		boolean ok = true;
		IDevice device = getDeviceBySerialNumber(serialNumber);
		try {
			device.createForward(from, to);
		} catch (Exception e) {
			AndmoreLogger.error(DeviceMonitor.class, "Error creating forward of device: " //$NON-NLS-1$
					+ serialNumber + " from " + from + " to " + to, e); //$NON-NLS-1$ //$NON-NLS-2$
			ok = false;
		}
		return ok;
	}

	/**
	 * Kill the communication channel
	 * 
	 * @param serialNumber
	 *            The serial number of the device to kill
	 */
	public void kill(String serialNumber) {
		if (serialNumber == null) {
			AndmoreLogger.error(String.format(NULL_SERIAL_NUMBER, "kill"));
		}
		if (isDeviceOnline(serialNumber)) {
			IDevice deviceToKill = getDeviceBySerialNumber(serialNumber);
			if (deviceToKill != null) {
				synchronized (consoleLock) {
					EmulatorConsole console = EmulatorConsole.getConsole(deviceToKill);
					if (console != null) {
						console.kill();
					}
				}
			}
		}
	}

	/**
	 * Push files to device
	 * 
	 * @param serialNumber
	 *            Android device serial number
	 * @param localDir
	 *            local folder path
	 * @param fileNames
	 *            files to transfer
	 * @param remoteDir
	 *            destination folder path
	 * @param timeout
	 *            timeout for the operation
	 * @param monitor
	 *            monitor associated with the operation
	 */
	public IStatus pushFiles(String serialNumber, String localDir, Collection<String> fileNames,
			String remoteDir, int timeout, final IProgressMonitor monitor, OutputStream outputStream) {
		return transferFiles(true, serialNumber, localDir, fileNames, remoteDir, timeout, monitor, outputStream);
	}

	/**
	 * Push files to device
	 * 
	 * @param serialNumber
	 *            Android device serial number
	 * @param localFiles
	 *            destination local files
	 * @param remoteFiles
	 *            remote files to transfer as localFiles to desktop
	 * @param timeout
	 *            timeout for the operation
	 * @param monitor
	 *            monitor associated with the operation
	 */
	public IStatus pushFiles(String serialNumber, List<File> localFiles, List<String> remoteFiles, int timeout,
			final IProgressMonitor monitor, OutputStream outputStream) {
		return transferFiles(true, serialNumber, localFiles, remoteFiles, timeout, monitor, outputStream);
	}

	/**
	 * Pull files from device
	 * 
	 * @param serialNumber
	 *            Android device serial number
	 * @param localDir
	 *            local folder path
	 * @param fileNames
	 *            files to transfer
	 * @param remoteDir
	 *            destination folder path
	 * @param timeout
	 *            timeout for the operation
	 * @param monitor
	 *            monitor associated with the operation
	 */
	public IStatus pullFiles(String serialNumber, String localDir, Collection<String> fileNames,
			String remoteDir, int timeout, final IProgressMonitor monitor, OutputStream outputStream) {
		return transferFiles(false, serialNumber, localDir, fileNames, remoteDir, timeout, monitor, outputStream);
	}

	/**
	 * Pull files from device
	 * 
	 * @param serialNumber
	 *            Android device serial number
	 * @param localFiles
	 *            local files to transfer as remoteFiles to device
	 * @param remoteFiles
	 *            destination remote files
	 * @param timeout
	 *            timeout for the operation
	 * @param monitor
	 *            monitor associated with the operation
	 */
	public IStatus pullFiles(String serialNumber, List<File> localFiles, List<String> remoteFiles, int timeout,
			final IProgressMonitor monitor, OutputStream outputStream) {
		return transferFiles(false, serialNumber, localFiles, remoteFiles, timeout, monitor, outputStream);
	}

	/**
	 * Check if the application is running in the device with specified serial
	 * number
	 * 
	 * @param serialNumber
	 * @param applicationName
	 * @return true if it is running, false otherwise
	 */
	public boolean isApplicationRunning(String serialNumber, String applicationName) {
		if (serialNumber == null) {
			AndmoreLogger.error(String.format(NULL_SERIAL_NUMBER, "isApplicationRunning"));
			return false;
		}
		IDevice dev = null;
		boolean running = false;
		dev = connectedDevices.get(serialNumber);
		if (dev != null) {
			running = dev.getClient(applicationName) != null;
		}
		return running;
	}

	/**
	 * Connect to a Remote Device given its IP/Port
	 * 
	 * @param device
	 *            the Remote Device Instance
	 * @param host
	 *            device host (IP)
	 * @param port
	 *            device port
	 * @param timeout
	 *            the maximum time allowed to successfully connect to the device
	 * @param monitor
	 *            the monitor of the operation
	 * @return the status of the operation
	 * @throws IOException
	 */
	public IStatus connectTcpIp(final ISerialNumbered device, String host, String port, int timeout,
			IProgressMonitor monitor) throws IOException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 1000);

		subMonitor.beginTask(AndroidNLS.DDMSFacade_MsgConnectingToDeviceViaTCPIP, 10);

		IStatus status = Status.OK_STATUS;

		String serialNumber = device.getSerialNumber();
		if (!isDeviceOnline(serialNumber)) // check if it's already connected
		{
			String[] cmd = createConnectTcpIpCommand(host, port);

			status = executeRemoteDevicesCommand(cmd, null, timeout,
					NLS.bind(AndroidNLS.ERR_RemoteDevice_TimeoutWhileConnecting, device.getDeviceName()),
					new IStopCondition() {

						@Override
						public boolean canStop() {
							String serialNumber = device.getSerialNumber();
							if (serialNumber != null) {
								return isDeviceOnline(serialNumber);
							} else {
								return false;
							}
						}
					}, subMonitor.newChild(1000));

		}

		subMonitor.worked(1000);

		return status;
	}

	/**
	 * Method which switches the device connection mode from TCP/IP to USB.
	 * 
	 * @param device
	 *            {@link ISerialNumbered} device to have its connection mode
	 *            changed.
	 * @param host
	 *            The IP of the device.
	 * @param port
	 *            The port in which the TCP/IP connection is established.
	 * @param timeout
	 *            The maximum time which the switching operation is attempted.
	 * @param monitor
	 *            The {@link IProgressMonitor} which this operation is being
	 *            computed.
	 * 
	 * @return Returns the {@link IStatus} of this operation.
	 * 
	 * @throws IOException
	 *             Exception thrown in case something goes wrong while trying to
	 *             switch the device connection mode from TCP/IP to USB.
	 */
	public IStatus switchFromTCPConnectionModeToUSBConnectionMode(final ISerialNumbered device, String host,
			String port, int timeout, IProgressMonitor monitor) throws IOException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 1000);

		subMonitor.beginTask(AndroidNLS.DDMSFacade_MsgSwitchingDeviceFromTCPIPToUSB, 10);

		IStatus status = Status.OK_STATUS;

		String serialNumber = device.getSerialNumber();
		if (isDeviceOnline(serialNumber)) // check if it's already connected
		{
			String[] cmd = createSwitchToUSBConnectionModeCommand(host, port);

			status = executeRemoteDevicesCommand(cmd, null, timeout,
					NLS.bind(AndroidNLS.DDMSFacade_MsgTimeoutReachedSwitchingFromTCPToUSB, device.getDeviceName()),
					new IStopCondition() {

						@Override
						public boolean canStop() {
							String serialNumber = device.getSerialNumber();
							if (serialNumber != null) {
								return isDeviceOnline(serialNumber);
							} else {
								return false;
							}
						}
					}, subMonitor.newChild(1000));
		}
		subMonitor.worked(1000);
		return status;
	}

	/**
	 * Get the wireless ip from the connected handset
	 * 
	 * @param serialNumber
	 * @param monitor
	 * @return the ip or null if not possible to retrieve it
	 */
	public String getWirelessIPfromHandset(String serialNumber, IProgressMonitor monitor) {
		String handset_wireless_ip = null;
		IDevice device = null;
		if (serialNumber == null) {
			AndmoreLogger.error(String.format(NULL_SERIAL_NUMBER, "getWirelessIOfromHandset"));
			return "";
		}
		device = connectedDevices.get(serialNumber);
		if (device != null) {
			// get the wi-fi name for executing the ipconfig command
			String wifiProperty = device.getProperty(WIFI_INTERFACE_DEVICE_PROPERTY);
			if (wifiProperty == null) {
				wifiProperty = DEFAULT_WIRELESS_DEVICE_PROPERTY;
			}
			// execute ipconfig command
			Collection<String> answers = executeShellCmd(serialNumber, IFCONFIG_CMD + " " + wifiProperty, monitor); //$NON-NLS-1$

			// Success message - for example
			// [tiwlan0: ip 192.168.0.174 mask 255.255.255.0 flags [up broadcast
			// running multicast]]

			if (answers != null) {
				String result = answers.toString();
				if (result != null) {
					// splits the result of the shell command and gets the third
					// position
					// that should be the IP number
					String[] result_splited = result.split(" "); //$NON-NLS-1$
					if (result_splited.length >= 3) {
						// check whether there is an IP
						Pattern pattern = Pattern.compile(IP_PATTERN);
						Matcher matcher = pattern.matcher(result);
						if (matcher.find()) {
							handset_wireless_ip = result_splited[2];
						}
					}
				}
			}
		}
		return handset_wireless_ip;
	}

	/**
	 * Switch adb connection mode of an specific device to TCPIP
	 * 
	 * @param deviceName
	 *            name of the handset instance
	 * @param host
	 *            wireless ip of the handset instance
	 * @param port
	 *            number of the port to be using during the connection
	 * @param timeout
	 *            the maximum time allowed to successfully connect to the device
	 * @param monitor
	 *            the monitor of the operation
	 * @return the status of the operation
	 * 
	 * @throws IOException
	 *             Exception thrown in case there are problems switching the
	 *             device.
	 */
	public IStatus switchUSBtoTcpIp(String deviceName, final String serialNumber, String port, int timeout,
			IProgressMonitor monitor) throws IOException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 1000);

		subMonitor.beginTask(AndroidNLS.DDMSFacade_MsgSwitchingFromUSBConnection, 10);
		if (serialNumber == null) {
			AndmoreLogger.error(String.format(NULL_SERIAL_NUMBER, "switchUSBtoTcpIp"));
			return Status.CANCEL_STATUS;
		}
		IStatus status = Status.OK_STATUS;

		if (isDeviceOnline(serialNumber)) {
			String[] cmd = createSwitchToTcpIpCommand(serialNumber, port);

			status = executeRemoteDevicesCommand(cmd, null, timeout,
					NLS.bind(AndroidNLS.ERR_WirelessRemoteDevice_TimeoutWhileConnecting, deviceName),
					new IStopCondition() {

						@Override
						public boolean canStop() {
							return isDeviceOffline(serialNumber);
						}
					}, subMonitor.newChild(1000));
		}
		monitor.worked(1000);
		return status;
	}

	/**
	 * Disconnect from a Remote Device given its IP/Port
	 * 
	 * @param device
	 *            the Remote Device Instance
	 * @param host
	 *            device host (IP)
	 * @param port
	 *            device port
	 * @param timeout
	 *            the maximum time allowed to successfully disconnect from the
	 *            device
	 * @param monitor
	 *            the monitor of the operation
	 * @return the status of the operation
	 * @throws IOException
	 */
	public IStatus disconnectTcpIp(final ISerialNumbered device, String host, String port, int timeout,
			IProgressMonitor monitor) throws IOException {
		IStatus status = Status.OK_STATUS;

		String serialNumber = device.getSerialNumber();
		if (isDeviceOnline(serialNumber)) // check if it's already disconnected
		{
			String[] cmd = createDisconnectTcpIpCommand(host, port);

			status = executeRemoteDevicesCommand(cmd, null, timeout,
					NLS.bind(AndroidNLS.ERR_RemoteDevice_TimeoutWhileDisconnecting, device.getDeviceName()),
					new IStopCondition() {

						@Override
						public boolean canStop() {
							String serialNumber = device.getSerialNumber();
							return !isDeviceOnline(serialNumber);
						}
					}, monitor);

		}

		return status;
	}

	/**
	 * Get the Device associated with the Android VM
	 * 
	 * @param vmName
	 *            Android VM name
	 * @return Device associated with the given Android VM
	 */
	public IDevice getDeviceWithVmName(String vmName) {
		IDevice toReturn = null;
		if (vmName != null) {
			Collection<IDevice> devices = connectedDevices.values();
			for (IDevice d : devices) {
				if (d.isEmulator()) {
					String deviceVmName = getVmName(d);
					if (vmName.equals(deviceVmName)) {
						toReturn = d;
						break;
					}
				}
			}
			if (toReturn == null)
				AndmoreLogger.error("Device with VM name \"" + vmName + "\" not found");
		} else {
			AndmoreLogger.error("getDeviceWithVmName called with null VM name");
		}
		return toReturn == null ? NULL_DEVICE : toReturn;
	}

	/**
	 * Securely get the name of the AVD associated to the given device.
	 * 
	 * @param serialNumber
	 *            The serialNumber of the device that we want the AVD name for
	 * @return the name of the AVD used by the emulator running with the given
	 *         device, or null if the vmname could be retrieved.
	 */
	public String getNameBySerialNumber(String serialNumber) {
		if (serialNumber == null) {
			AndmoreLogger.error(String.format(NULL_SERIAL_NUMBER, "getNameBySerialNumber"));
			return "";
		}
		String avdName = null;
		IDevice d = getDeviceBySerialNumber(serialNumber);
		avdName = getNameByDevice(d);

		return avdName;
	}

	/**
	 * Securely get the serial number of the given instance.
	 * 
	 * @param instanceName
	 *            The name of the instance we want the serial number of
	 * @return the serial number of the given instance, or <code>null</code> if
	 *         no instance with the given name is online
	 */
	public String getSerialNumberByName(String instanceName) {
		String serialNumber = null;
		if (instanceName != null) {
			// Using ConcurrentHashMap iterator is safe if used only on a single thread
			for (IDevice dev : connectedDevices.values()) {
				if (instanceName.equals(getNameByDevice(dev))) {
					serialNumber = dev.getSerialNumber();
					break;
				}
			}
		}
		return serialNumber;
	}

	public Collection<String> getRunningApplications(String serialNumber) {
		Collection<String> apps = new ArrayList<String>();
		if (serialNumber != null) {
			IDevice dev = getDeviceBySerialNumber(serialNumber);
			if (dev != null) {
				Client[] clients = dev.getClients();
				if ((clients != null) && (clients.length > 0)) {
					for (Client c : clients) {
						apps.add(c.getClientData().getClientDescription());
					}
				}
			}
		}
		return apps;
	}

	/**
	 * Check if a device identified by the serial number has a mounted SDCard
	 * 
	 * @param serialNumber
	 *            the serial number
	 * @return true if the device has a SDCard
	 * @throws IOException
	 */
	public boolean hasSDCard(String serialNumber) throws IOException {
		if (serialNumber == null) {
			AndmoreLogger.error(String.format(NULL_SERIAL_NUMBER, "hasSDCard"));
			return false;
		}
		boolean hasSdCard = false;
		File tempSdCardFile = File.createTempFile("SDcheck", ".tmp"); //$NON-NLS-1$ //$NON-NLS-2$
		boolean tempCopiedOnSdCardFile = pushFileToDevice(serialNumber, SDCARD_FOLDER, tempSdCardFile);

		if (tempCopiedOnSdCardFile) {
			// trying to write on /sdcard folder (it works for phones previous
			// from Platform 2.2)
			if (!deleteFileFromDevice(serialNumber, tempSdCardFile.getName(), SDCARD_FOLDER)) {
				AndmoreLogger
						.error("DDMSFacade: Could not delete tempfile from /sdcard when checking if card is enabled"); //$NON-NLS-1$
			}
			hasSdCard = true;
			tempSdCardFile.delete();
		} else {

			File tempMntFile = File.createTempFile("SDcheck", ".tmp"); //$NON-NLS-1$ //$NON-NLS-2$
			boolean tempCopiedOnMntFile = pushFileToDevice(serialNumber, MNT_SDCARD_FOLDER, tempSdCardFile);

			if (tempCopiedOnMntFile) {
				// trying to write on /mnt/sdcard folder (it works for phones
				// since Platform 2.2)
				if (!deleteFileFromDevice(serialNumber, tempMntFile.getName(), MNT_SDCARD_FOLDER)) {
					AndmoreLogger
							.error("DDMSFacade: Could not delete tempfile from /mnt/sdcard when checking if card is enabled"); //$NON-NLS-1$
				}
				hasSdCard = true;
				tempMntFile.delete();
			}

		}

		return hasSdCard;
	}

	/**
	 * Dumps a HPROF file based on a client description and a device serial
	 * number
	 * 
	 * @param clientDescription
	 *            A client description of a running application
	 */
	public IStatus dumpHprofFile(String clientDescription, String serialNumber, IProgressMonitor monitor) {
		IStatus status = Status.OK_STATUS;
		monitor.beginTask(AndroidNLS.DumpHprofFile_GeneratingMemoryAnalysisOutput, 100);

		// Retrive running apps
		monitor.setTaskName(AndroidNLS.DumpHprofFile_GettingRunningApplications);
		IDevice dev = getDeviceBySerialNumber(serialNumber);
		Client[] clients = dev.getClients();
		monitor.worked(25);

		// Store the shell
		final Shell[] shell = new Shell[1];

		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {

				shell[0] = new Shell(PlatformUI.getWorkbench().getDisplay());

			}
		});

		AndmoreHProfDumpHandler hprofHandler = new AndmoreHProfDumpHandler(shell[0], monitor);
		monitor.setTaskName(AndroidNLS.DumpHprofFile_SettingApplicationToAnalyse);
		hprofHandler.setSelectedApp(clientDescription);
		monitor.worked(25);

		try {
			// Find a client with matching description and dum the HPROF file
			for (Client client : clients) {
				if (client.getClientData().getClientDescription().equals(clientDescription)) {
					// Set our handler as the HprofDumpHandler
					monitor.setTaskName(AndroidNLS.DumpHprofFile_DumpingHprofFile);
					Integer clientPid = client.getClientData().getPid();
					synchronized(clientPidSet) {
						if (!clientPidSet.keySet().contains(clientPid))
							clientPidSet.put(clientPid, clientPid);
					}
					client.dumpHprof();
					synchronized (clientPid) {
						while (client.getClientData().getHprofData() == null)
							clientPid.wait(MAX_WAIT_TIME);
					}
					HprofData hprofData = client.getClientData().getHprofData();
					if (hprofData == null) {
						hprofHandler.onEndFailure(client, null);
						status = Status.CANCEL_STATUS;
					}
					else {
						if (hprofData.type == HprofData.Type.DATA)
							hprofHandler.onSuccess(hprofData.data, client);
						else
							hprofHandler.onSuccess(hprofData.filename, client);
					}
					monitor.worked(50);
				}
			}
		} catch (Exception e) {
			// Status not ok
			status = Status.CANCEL_STATUS;
		} finally {
			monitor.done();
		}

		return status;

	}

	@Override
	public void clientChanged(Client client, int changeMask) {
		if ((changeMask & Client.CHANGE_NAME) == Client.CHANGE_NAME) {
			final Client finalClient = client;
			Job job = new Job("Change client name"){

				@Override
				protected IStatus run(IProgressMonitor arg0) {
					String applicationName = finalClient.getClientData().getClientDescription();
					if (applicationName != null) {
						IPreferenceStore store = AndmoreAndroidPlugin.getDefault().getPreferenceStore();
						String home = store.getString(AdtPrefs.PREFS_HOME_PACKAGE);
						if (home.equals(applicationName)) {
							String serialNum = finalClient.getDevice().getSerialNumber();
							AndmoreLogger.debug("Completely Up Device: " + serialNum); //$NON-NLS-1$
							synchronized (completelyUpDevices) {
								completelyUpDevices.add(serialNum);
							}
						}
					}
					return Status.OK_STATUS;
				}};
			job.setPriority(Job.BUILD);
			job.schedule();
		} else if ((changeMask & Client.CHANGE_HPROF) == Client.CHANGE_HPROF) {
			Integer clientPid = client.getClientData().getPid();
			synchronized(clientPidSet) {
				if (clientPidSet.keySet().contains(clientPid))
					clientPid = clientPidSet.get(clientPid);
				else // This is not expected
					clientPid = null;
			}
			if (clientPid != null)
				synchronized(clientPid) {
					clientPid.notifyAll();
				}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener#deviceChanged
	 * (com.android.ddmlib.Device, int)
	 */
	@Override
	public void deviceChanged(IDevice device, int i) {
		if (i == IDevice.CHANGE_STATE) {
			// a handset should only be instantiated when its state change from
			// OFFLINE to ONLINE
			// to avoid the problem of a remote device on the OFFLINE state be
			// presented as an ONLINE handset
			if ((device.getState() == DeviceState.ONLINE) && (!device.isEmulator())) {
				deviceConnected(device);
			}
			deviceStatusChanged(device);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener#deviceConnected
	 * (com.android.ddmlib.Device)
	 */
	@Override
	public void deviceConnected(IDevice device) {
		// handsets should not be instantiated right after connection because at
		// that time
		// they appear on the OFFLINE state
		final String serialNumber = device.getSerialNumber();
		if (device.isEmulator()) {
			AndmoreLogger.debug("Device connected: " + serialNumber); //$NON-NLS-1$
			connectedDevices.put(serialNumber, device);
		}
		if (!device.isEmulator() && !device.hasClients()) {
			Job job = new Job("Device client wait"){

				@Override
				protected IStatus run(IProgressMonitor arg0) {
					boolean timeout = false;
					long startTime = System.currentTimeMillis();
					int maxInterval = 10000;
					do {
						try {
							Thread.sleep(250);
						} catch (InterruptedException e) {
							return Status.CANCEL_STATUS;
						}
						long currentTime = System.currentTimeMillis();
						timeout = ((startTime + maxInterval) < currentTime);

					} while (!device.hasClients() && !timeout && (device.getState() == DeviceState.ONLINE));
					if (timeout) {
						synchronized (completelyUpDevices) {
							// put the device up anyway.
							completelyUpDevices.add(serialNumber);
						}
					}
					completeDeviceConnection(device, serialNumber);
					return Status.OK_STATUS;
				}};
				job.setPriority(Job.BUILD);
				job.schedule();
		} else
			completeDeviceConnection(device, serialNumber);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener#
	 * deviceDisconnected(com.android.ddmlib.Device)
	 */
	@Override
	public void deviceDisconnected(IDevice device) {
		final String serialNumber = device.getSerialNumber();
		AndmoreLogger.debug("Device disconnected: " + serialNumber); //$NON-NLS-1$
		synchronized (completelyUpDevices) {
			completelyUpDevices.remove(serialNumber);
		}
		connectedDevices.remove(serialNumber);
		Job job = new Job("Notify device " + serialNumber + " connected"){

			@Override
			protected IStatus run(IProgressMonitor arg0) {
				// Fire events in worker thread to prevent blocking caller thread
				AndmoreEventManager.fireEvent(EventType.DEVICE_DISCONNECTED, serialNumber);
				synchronized (avdNameMap) {
					avdNameMap.remove(device.getSerialNumber());
				}
				return Status.OK_STATUS;
			}};
		job.setPriority(Job.BUILD);
		job.schedule();
	}

	private void deviceStatusChanged(IDevice device) {
		AndmoreLogger.debug("Device changed: " + device.getSerialNumber()); //$NON-NLS-1$
		connectedDevices.put(device.getSerialNumber(), device);
		if ((device).getState() == DeviceState.ONLINE) {
			IPreferenceStore store = AndmoreAndroidPlugin.getDefault().getPreferenceStore();
			String home = store.getString(AdtPrefs.PREFS_HOME_PACKAGE);
			if (device.getClient(home) != null) {
				synchronized (completelyUpDevices) {
					AndmoreLogger.debug("Completely Up Device: " + device.getSerialNumber()); //$NON-NLS-1$
					if (!completelyUpDevices.contains(device.getSerialNumber())) {
						completelyUpDevices.add(device.getSerialNumber());
					}
				}
			}
		}
	}

	/**
	 * Get the name of the VM associated to the emulator running in the given
	 * deviceSerial identification.
	 * 
	 * @param device  emulator whose vm name must be retrieved.
	 * @return the name of the VM used by the emulator running with the given
	 *         id, or null if the vmname could be retrieved.
	 */
	private String getVmName(final IDevice device) {
		String vmName = null;
		String serialNumber = device.getSerialNumber();
		int MAX_TRIES = 120;
		int tries = 0;

		while ((vmName == null) && (tries < MAX_TRIES)) {
			synchronized (avdNameMap) {
				vmName = avdNameMap.get(serialNumber);
			}

			if (vmName == null) {
				vmName = device.getAvdName();
			}

			if (vmName == null) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// do nothing
				} finally {
					tries++;
				}
			}
            // TODO - Fix or remove
			// try to get vmname by telnet as last option
			//if (vmName == null) {
			//	vmName = getVmNameByTelnet(serialNumber);
			//}

		}

		if (vmName != null) {
			synchronized (avdNameMap) {
				if (avdNameMap.get(serialNumber) == null) {
					avdNameMap.put(serialNumber, vmName);
				}
			}
		}
		return vmName;
	}

	/**
	 * Get the service used to transfer files to the Device
	 * 
	 * @param device
	 *            Device
	 * @param timeout
	 *            timeout for the operation
	 * @param monitor
	 *            monitor associated with the operation
	 * @return The service used to transfer files to the Device
	 * 
	 * @throws AndroidException
	 */
	private SyncService getSyncService(IDevice device, int timeout, final IProgressMonitor monitor)
			throws AndroidException {

		SyncService service = null;
		long timeoutLimit = System.currentTimeMillis() + timeout;
		do {
			if ((device != null) && (device.isOnline())) {
				try {
					service = device.getSyncService();
				} catch (IOException e) {
					AndmoreLogger.debug("Couldn't get sync service; cause: " + e.getMessage()); //$NON-NLS-1$
				} catch (com.android.ddmlib.TimeoutException e) {
					AndmoreLogger.debug("Couldn't get sync service; cause: " + e.getMessage()); //$NON-NLS-1$
				} catch (AdbCommandRejectedException e) {
					AndmoreLogger.debug("Couldn't get sync service; cause: " + e.getMessage()); //$NON-NLS-1$
				}
			}

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// do nothing
			}

			if (monitor.isCanceled()) {
				AndmoreLogger.info("Operation canceled by the user"); //$NON-NLS-1$
				return null;
			}

			if (System.currentTimeMillis() > timeoutLimit) {
				AndmoreLogger.error("The emulator was not up within the set timeout"); //$NON-NLS-1$
				throw new AndroidException("Timeout while preparing to transfer files to the Device. " + device); //$NON-NLS-1$
			}
		} while (service == null);

		return service;
	}

	private IStatus transferFiles(boolean isPush, String serialNumber, String localDir,
			Collection<String> fileNames, String remoteDir, int timeout, final IProgressMonitor monitor,
			OutputStream outputStream) {
		List<File> localList = new ArrayList<File>();
		List<String> remoteList = new ArrayList<String>();
		for (String name : fileNames) {
			localList.add(new File(localDir, name));
			remoteList.add(remoteDir + "/" + name); //$NON-NLS-1$
		}
		return transferFiles(isPush, serialNumber, localList, remoteList, timeout, monitor, outputStream);
	}

	private IStatus transferFiles(boolean isPush, String serialNumber, List<File> localFiles,
			List<String> remoteFiles, int timeout, final IProgressMonitor monitor, OutputStream outputStream) {
		if (localFiles.size() != remoteFiles.size()) {
			return new Status(IStatus.ERROR, PLUGIN_ID, AndroidNLS.ERR_DDMSFacade_IncompatibleFileLists);
		}

		IStatus status = Status.OK_STATUS;
		IDevice device = getDeviceBySerialNumber(serialNumber);

		SyncService service = null;
		try {
			service = getSyncService(device, timeout, monitor);
			if (service == null) {
				status = Status.CANCEL_STATUS;
			} else {
				final ISyncProgressMonitor syncMonitor = new ISyncProgressMonitor() {
					@Override
					public void start(int i) {
						// do nothing
					}

					@Override
					public void stop() {
						// do nothing
					}

					@Override
					public void advance(int i) {
						// do nothing
					}

					@Override
					public boolean isCanceled() {
						return monitor.isCanceled();
					}

					@Override
					public void startSubTask(String s) {
						// do nothing
					}
				};

				FileListingService flService = device.getFileListingService();

				for (int i = 0; i < localFiles.size(); i++) {
					File localFile = localFiles.get(i);
					String remotePath = remoteFiles.get(i);
					String absLocalFile = localFile.getAbsolutePath();

					String resultMessage = null;
					if (isPush) {
						AndmoreLogger.debug("Push " + absLocalFile + " to " + remotePath); //$NON-NLS-1$ //$NON-NLS-2$
						try {
							service.pushFile(absLocalFile, remotePath, syncMonitor);
						} catch (SyncException e1) {
							AndmoreLogger.debug("Push result: " + "SyncException occured " + e1.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
							resultMessage = NLS.bind(AndroidNLS.CON_ConsolePush, absLocalFile, remotePath)
									+ ": " + e1.getLocalizedMessage(); //$NON-NLS-1$
						} catch (FileNotFoundException e1) {
							AndmoreLogger.debug("Push result: " + "FileNotFoundException occured " //$NON-NLS-1$ //$NON-NLS-2$
									+ e1.getMessage());
							resultMessage = NLS.bind(AndroidNLS.CON_ConsolePush, absLocalFile, remotePath)
									+ ": " + e1.getLocalizedMessage(); //$NON-NLS-1$
						} catch (IOException e1) {
							AndmoreLogger.debug("Push result: " + "IOException occured " + e1.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
							resultMessage = NLS.bind(AndroidNLS.CON_ConsolePush, absLocalFile, remotePath)
									+ ": " + e1.getLocalizedMessage(); //$NON-NLS-1$
						} catch (com.android.ddmlib.TimeoutException e1) {
							AndmoreLogger.debug("Push result: " + "TimeoutException occured " + e1.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
							resultMessage = NLS.bind(AndroidNLS.CON_ConsolePush, absLocalFile, remotePath)
									+ ": " + e1.getLocalizedMessage(); //$NON-NLS-1$
						}

						if ((outputStream != null) && (resultMessage != null)) {
							try {
								outputStream.write(resultMessage.getBytes());
								outputStream.write('\n');
								outputStream.flush();
							} catch (Exception e) {
								// Do nothing
							}
						}

					} else {
						FileEntry f1 = null;
						FileEntry f2 = null;

						f2 = flService.getRoot();
						flService.getChildren(f2, false, null);
						String[] dirs = remotePath.split("/"); //$NON-NLS-1$

						for (int j = 1; j < (dirs.length - 1); j++) {
							f1 = f2.findChild(dirs[j]);
							flService.getChildren(f1, false, null);
							f2 = f1;
						}

						final FileEntry fileToPull = f2.findChild(dirs[dirs.length - 1]);

						if (fileToPull != null) {
							try {
								service.pullFile(fileToPull, absLocalFile, syncMonitor);
							} catch (FileNotFoundException e) {
								resultMessage = e.getLocalizedMessage();
								AndmoreLogger.debug("Pull result: " + e.getMessage()); //$NON-NLS-1$
							} catch (SyncException e) {
								resultMessage = e.getLocalizedMessage();
								AndmoreLogger.debug("Pull result: " + e.getMessage()); //$NON-NLS-1$
							} catch (IOException e) {
								resultMessage = e.getLocalizedMessage();
								AndmoreLogger.debug("Pull result: " + e.getMessage()); //$NON-NLS-1$
							} catch (com.android.ddmlib.TimeoutException e) {
								resultMessage = e.getLocalizedMessage();
								AndmoreLogger.debug("Pull result: " + e.getMessage()); //$NON-NLS-1$
							}

							if ((outputStream != null) && (resultMessage != null)) {
								String message = NLS.bind(AndroidNLS.CON_ConsolePull, fileToPull.getFullPath(),
										absLocalFile) + ": " + resultMessage; //$NON-NLS-1$
								try {
									outputStream.write(message.getBytes());
									outputStream.write('\n');
									outputStream.flush();
								} catch (IOException e) {
									// do nothing
								}
							}
						} else {
							resultMessage = NLS.bind(AndroidNLS.DDMSFacade_Remote_File_Not_Found, remotePath);
							AndmoreLogger.debug("Pull result: File not found " + remotePath); //$NON-NLS-1$
						}
					}

					if (resultMessage != null) {
						status = new Status(IStatus.ERROR, PLUGIN_ID, resultMessage);
					}
					if (syncMonitor.isCanceled()) {
						status = Status.CANCEL_STATUS;
						break;
					}
				}
			}
		} catch (AndroidException e) {
			status = new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage());
		} catch (NullPointerException e1) {
			status = new Status(IStatus.ERROR, PLUGIN_ID, AndroidNLS.ERR_DDMSFacade_FileNotFound);
		} finally {
			if (service != null) {
				service.close();
			}
		}
		return status;
	}

	/**
	 * See http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html to
	 * understand how Process.exec works and its problems
	 * 
	 * @param cmd
	 *            Command to be executed.
	 * @param out
	 *            Output Stream.
	 * @param timeout
	 *            Timeout (secs.)
	 * @param monitor
	 *            {@link IProgressMonitor}
	 * 
	 * @return the {@link IStatus} of this process execution.
	 * 
	 * @throws IOException
	 *             Exception thrown in case there is any problem executing the
	 *             command.
	 */
	private IStatus executeRemoteDevicesCommand(String[] cmd, OutputStream out, int timeout, String timeoutMsg,
			IStopCondition stopCondition, IProgressMonitor monitor) throws IOException {

		IStatus status = Status.OK_STATUS;
		long timeoutLimit = -1;
		if (timeout != 0) {
			timeoutLimit = System.currentTimeMillis() + (timeout * 1000);
		}

		String fullCmd = ""; //$NON-NLS-1$
		for (String cmdArg : cmd) {
			fullCmd += cmdArg + " "; //$NON-NLS-1$
		}
		if (out != null) {
			out.write(fullCmd.getBytes());
			out.write("\n".getBytes()); //$NON-NLS-1$
		}
		Runtime r = Runtime.getRuntime();
		Process p = r.exec(cmd);

		int errorCode = 0;

		// inputStream / errorStream;
		String[] commandResults = new String[] { "", "" //$NON-NLS-1$ //$NON-NLS-2$
		};

		commandResults = readCmdOutputFromStreams(commandResults[0], commandResults[1], p.getInputStream(),
				p.getErrorStream(), out);

		while (!stopCondition.canStop()) {
			if ((monitor != null) && (monitor.isCanceled())) {
				p.destroy();
				return Status.CANCEL_STATUS;
			}

			try {
				errorCode = p.exitValue();
				if (errorCode != 0) {
					break;
				}

			} catch (IllegalThreadStateException e) {
				// Process is still running... Proceed with loop
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				AndmoreLogger.error("Execute command: thread has been interrupted"); //$NON-NLS-1$
			}

			if (timeout > 0) {
				try {
					testTimeout(timeoutLimit, ((timeoutMsg != null) ? timeoutMsg : AndroidNLS.ERR_GenericTimeout));
				} catch (TimeoutException e) {
					p.destroy();
					AndmoreLogger.debug("The timeout " + timeout //$NON-NLS-1$
							+ " has been reached when executing the command " + fullCmd); //$NON-NLS-1$
					return new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e);
				}
			}

		}
		commandResults = readCmdOutputFromStreams(commandResults[0], commandResults[1], p.getInputStream(),
				p.getErrorStream(), out);

		if (errorCode != 0) {
			AndmoreLogger.debug("Command " + cmd + " returned an error code: " + errorCode); //$NON-NLS-1$ //$NON-NLS-2$
			status = new Status(IStatus.ERROR, PLUGIN_ID,
					NLS.bind(AndroidNLS.ERR_CommandError, errorCode) + "\n" //$NON-NLS-1$
							+ ((!commandResults[1].equals("")) ? commandResults[1] //$NON-NLS-1$
									: commandResults[0]));
		} else {
			status = new Status(IStatus.OK, PLUGIN_ID, commandResults[0]);
		}
		return status;
	}

	/**
	 * @param commandResults
	 * @param errorResults
	 * @param inputStream
	 * @param errorStream
	 * @param out
	 */
	private String[] readCmdOutputFromStreams(String commandResults, String errorResults,
			InputStream inputStream, InputStream errorStream, OutputStream out) {
		String[] results = new String[2];
		String line = ""; //$NON-NLS-1$

		BufferedReader brInput = new BufferedReader(new InputStreamReader(inputStream));
		BufferedReader brError = new BufferedReader(new InputStreamReader(errorStream));

		try {

			// input stream
			if (brInput.ready()) {
				while ((line = brInput.readLine()) != null) {
					commandResults += line;
					commandResults += "\n"; //$NON-NLS-1$
					if (out != null) {
						out.write(line.getBytes());
						out.write("\n".getBytes()); //$NON-NLS-1$
					}
				}
			}

			// error stream
			if (brError.ready()) {
				while ((line = brError.readLine()) != null) {
					errorResults += "\n"; //$NON-NLS-1$
					if (out != null) {
						out.write(line.getBytes());
						out.write("\n".getBytes()); //$NON-NLS-1$
					}
				}
			}
		} catch (IOException e) {
			AndmoreLogger.error("Cannot read command outputs"); //$NON-NLS-1$
		} finally {
			try {
				brInput.close();
				brError.close();
			} catch (IOException e) {
				AndmoreLogger.error("Could not close console stream: " + e.getMessage());
			}
		}

		results[0] = commandResults;
		results[1] = errorResults;

		return results;

	}

	/**
	 * Checks if the timeout limit has reached.
	 * 
	 * @param timeoutLimit
	 *            The system time limit that cannot be overtaken, in
	 *            milliseconds.
	 * @throws StartTimeoutException
	 *             When the system time limit is overtaken.
	 */
	private void testTimeout(long timeoutLimit, String timeoutErrorMessage) throws TimeoutException {
		if (System.currentTimeMillis() > timeoutLimit) {
			throw new TimeoutException(timeoutErrorMessage);
		}
	}

	/**
	 * Creates a string with the command that should be called in order to run
	 * the application.
	 */
	private String[] createRunCommand(String serialNumber, String activityName, boolean debugMode) {
		String cmd[];

		// The tools folder should exist and be here, but double-cheking
		// once more wont kill
		File f = new File(sdkPath + PLATFORM_TOOLS_FOLDER + File.separator);
		if (!f.exists()) {
			AndmoreLogger.error("Run: Could not find tools folder on " + sdkPath + PLATFORM_TOOLS_FOLDER //$NON-NLS-1$
					+ File.separator);
		} else {
			if (!f.isDirectory()) {
				AndmoreLogger.error("Run: Invalid tools folder " + sdkPath + PLATFORM_TOOLS_FOLDER //$NON-NLS-1$
						+ File.separator);
			}
		}

		String completeAppPath = activityName.substring(0, activityName.lastIndexOf(".")) + "/" + activityName; //$NON-NLS-1$ //$NON-NLS-2$
		if (debugMode) {
			// If debugMode option is checked, create command with the -D
			// paramater
			String cmdTemp[] = { sdkPath + PLATFORM_TOOLS_FOLDER + File.separator + ADB_COMMAND,
					ADB_INSTANCE_PARAMETER, serialNumber, SHELL_CMD, AM_CMD, START_CMD, ADB_AM_DEBUG, ADB_AM_NAME,
					completeAppPath };
			cmd = cmdTemp;
		} else {
			// If debugMode option is unchecked, create command without the -D
			// paramater
			String cmdTemp[] = { sdkPath + PLATFORM_TOOLS_FOLDER + File.separator + ADB_COMMAND,
					ADB_INSTANCE_PARAMETER, serialNumber, SHELL_CMD, AM_CMD, START_CMD, ADB_AM_NAME, completeAppPath };
			cmd = cmdTemp;
		}

		return cmd;
	}

	private Collection<String> executeShellCmd(String serialNumber, final String cmd,
			final IProgressMonitor monitor) {
		final Collection<String> results = new ArrayList<String>();
		IDevice device = getDeviceBySerialNumber(serialNumber);
		if (device != null) {
			try {
				device.executeShellCommand(cmd, new MultiLineReceiver() {
					@Override
					public boolean isCancelled() {
						return monitor.isCanceled();
					}

					@Override
					public void processNewLines(String[] lines) {
						for (String line : lines) {
							if ((!line.equals("")) && (!line.equals(cmd))) //$NON-NLS-1$
							{
								results.add(line);
							}
						}
					}
				}, 0, TimeUnit.SECONDS);
			} catch (Exception e) {
				AndmoreLogger.error(DeviceMonitor.class, "Error executing shell command " + cmd //$NON-NLS-1$
						+ " at device " + serialNumber, e); //$NON-NLS-1$
			}
		}
		return results;
	}

	private String getVmNameByTelnet(String serialNumber) {
		Pattern pattern = Pattern.compile("emulator-([0-9]+)"); //$NON-NLS-1$
		TelnetFrameworkAndroid telnet = new TelnetFrameworkAndroid();
		Matcher matcher = pattern.matcher(serialNumber);
		matcher.matches();
		String avdName = null;

		try {
			Integer telnetPort = Integer.valueOf(matcher.group(1));
			telnet.connect("localhost", telnetPort); //$NON-NLS-1$
			String avdNameRaw = telnet.write("avd name\r\n", new String[] //$NON-NLS-1$
					{ "KO" //$NON-NLS-1$
					});

			String split = avdNameRaw.contains("\r\n") ? "\r\n" : "\n";

			String[] outputArray = avdNameRaw.split(split);
			if (outputArray.length > 2) {
				avdName = outputArray[2];
			}

			if (avdName != null) {
				synchronized(avdNameMap) {
					avdNameMap.put(serialNumber, avdName);
				}
			}
		} catch (NumberFormatException e) {
			avdName = serialNumber;
		} catch (IOException e) {
			avdName = serialNumber;
		} finally {
			try {
				telnet.disconnect();
			} catch (IOException e) {
				// Do nothing.
			}
		}
		return avdName;
	}

	/**
	 * Gets the AVD name of the device
	 * 
	 * @param d
	 *            The device to be searched for the AVD name
	 * 
	 * @return The AVD name
	 */
	private String getNameByDevice(final IDevice d) {
		String name = null;
		if (d != null) {
			if (d.isEmulator()) {

				name = getVmName(d);
			} else {
				name = d.getSerialNumber();
			}
		}
		return name;
	}

	/**
	 * Creates a string with the command that should be called in order to
	 * connect to an IP/Port
	 * 
	 * @param host
	 *            device host (IP)
	 * @param port
	 *            device port
	 * @return the command to be used to connect to an IP/Port
	 */
	private String[] createConnectTcpIpCommand(String host, String port) {

		String hostPort = host + ":" + port; //$NON-NLS-1$
		String cmd[] = { sdkPath + PLATFORM_TOOLS_FOLDER + File.separator + ADB_COMMAND, CONNECT_TCPIP_CMD, hostPort };
		return cmd;
	}

	/**
	 * Creates a string with the command switches a device from the TCP/IP
	 * connection mode to the USB connection mode.
	 * 
	 * @param host
	 *            Device host (IP).
	 * @param port
	 *            Device port.
	 * 
	 * @return The command to be used to switch back to USB connection mode.
	 */
	private String[] createSwitchToUSBConnectionModeCommand(String host, String port) {

		String hostPort = host + ":" + port; //$NON-NLS-1$

		String cmd[] = { sdkPath + PLATFORM_TOOLS_FOLDER + File.separator + ADB_COMMAND, DEVICE_ID_INDICATOR, hostPort,
				USB_SWITCH_BACK_COMMAND };
		return cmd;
	}

	/**
	 * Creates a string with the command that should be called in order to
	 * switch adb connection from USB to TPCIP mode
	 * 
	 * @param serialNumber
	 *            device serial number
	 * @param port
	 *            device port
	 * @return the command to be used to switch adb connection to TCPIP mode
	 */
	private String[] createSwitchToTcpIpCommand(String serialNumber, String port) {
		String cmd[] = { sdkPath + PLATFORM_TOOLS_FOLDER + File.separator + ADB_COMMAND, ADB_INSTANCE_PARAMETER,
				serialNumber, TCPIP_CMD, port };
		return cmd;
	}

	/**
	 * Creates a string with the command that should be called to delete a file
	 * from device
	 * 
	 * @param serialNumber
	 * @param file
	 * @param folder
	 * @return
	 */
	private String[] createDeleteFileFromDeviceCommand(String serialNumber, String file, String folder) {
		// The tools folder should exist and be here, but double-cheking
		// once more wont kill
		File f = new File(sdkPath + PLATFORM_TOOLS_FOLDER + File.separator);
		if (!f.exists()) {
			AndmoreLogger.error("Run: Could not find tools folder on " + sdkPath + PLATFORM_TOOLS_FOLDER //$NON-NLS-1$
					+ File.separator);
		} else {
			if (!f.isDirectory()) {
				AndmoreLogger.error("Run: Invalid tools folder " + sdkPath + PLATFORM_TOOLS_FOLDER //$NON-NLS-1$
						+ File.separator);
			}
		}
		String cmd[] = { sdkPath + PLATFORM_TOOLS_FOLDER + File.separator + ADB_COMMAND, ADB_INSTANCE_PARAMETER,
				serialNumber, SHELL_CMD, "rm /" + folder + "/" + file //$NON-NLS-1$ //$NON-NLS-2$
		};
		return cmd;
	}

	/**
	 * Uses the ADB shell command to remove a file from the device
	 * 
	 * @param serialNumber
	 * @param fileName
	 * @param sdCardFolder
	 * @return
	 * @throws IOException
	 */
	private boolean deleteFileFromDevice(String serialNumber, String fileName, String folder) throws IOException {

		String command[] = createDeleteFileFromDeviceCommand(serialNumber, fileName, folder);
		IStatus status = executeRemoteDevicesCommand(command, null, 1000, "", new IStopCondition() //$NON-NLS-1$
				{
					@Override
					public boolean canStop() {
						return true;
					}
				}, null);
		return status.isOK();
	}

	/**
	 * 
	 * @param serialNumber
	 * @param sdCardFolder
	 * @param tempFile
	 * @return true if manages to push file into the folder specified on device
	 */
	private boolean pushFileToDevice(String serialNumber, String folder, File file) {
		Collection<String> files = new ArrayList<String>();
		files.add(file.getName());
		Path path = new Path(file.getAbsolutePath());

		IStatus status = pushFiles(serialNumber, path.removeLastSegments(1).toString(), files, folder, 2000,
				new NullProgressMonitor(), null);

		return status.isOK();
	}

	/**
	 * Creates a string with the command that should be called in order to
	 * disconnect from an IP/Port
	 * 
	 * @param host
	 *            device host (IP)
	 * @param port
	 *            device port
	 * @return the command to be used to disconnect from an IP/Port
	 */
	private String[] createDisconnectTcpIpCommand(String host, String port) {

		String hostPort = host + ":" + port; //$NON-NLS-1$

		String cmd[] = { sdkPath + PLATFORM_TOOLS_FOLDER + File.separator + ADB_COMMAND, DISCONNECT_TCPIP_CMD, hostPort };

		return cmd;
	}

	private void completeDeviceConnection(IDevice device, final String serialNumber) {
		if (device.hasClients()) {
			// When a device is connected, look for the HOME application and add
			// the device serial number to a collection if it is already
			// running.
			IPreferenceStore store = AndmoreAndroidPlugin.getDefault().getPreferenceStore();
			String home = store.getString(AdtPrefs.PREFS_HOME_PACKAGE);
			if (device.getClient(home) != null) {
				AndmoreLogger.debug("Completely Up Device: " + serialNumber); //$NON-NLS-1$
				synchronized (completelyUpDevices) {
					completelyUpDevices.add(serialNumber);
				}
			}
		}
		Job job = new Job("Notify device " + serialNumber + " connected"){

			@Override
			protected IStatus run(IProgressMonitor arg0) {
				if (device.isEmulator()) {
					// Getting VM name first time may block for up to 10 seconds
					String vmName = null;
					synchronized (avdNameMap) {
						vmName = avdNameMap.get(serialNumber);
					}
					if (vmName == null)
						vmName = getVmName(device);
					if (vmName == null) {
						AndmoreLogger.error("AVD name not available for device " + serialNumber);
						return Status.CANCEL_STATUS;
					}
				}
				// Fire events in worker thread to prevent blocking caller thread
				AndmoreEventManager.fireEvent(EventType.DEVICE_CONNECTED, serialNumber);
				return Status.OK_STATUS;
			}};
		job.setPriority(Job.BUILD);
		job.schedule();
	}
	

    public static DeviceMonitor instance() {
    	if (singleton == null) {
    		synchronized(DeviceMonitor.class) {
    			singleton = new DeviceMonitor();
				AndroidDebugBridge.addDeviceChangeListener(singleton);
		    	// Adds listener for the HOME application. It adds the serial number of the
				// device to a collection when it identifies that the HOME application has loaded
				AndroidDebugBridge.addClientChangeListener(singleton);
    		}
    	}
    	return singleton;
    }

    public static void dispose() {
    	if (singleton != null) {
    		AndroidDebugBridge.removeDeviceChangeListener(singleton);
    		AndroidDebugBridge.removeClientChangeListener(singleton);
    	}
    }
}
