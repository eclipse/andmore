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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.andmore.android.StudioAndroidEventManager.EventType;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.log.UsageDataConstants;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.common.utilities.FileUtil;
import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.andmore.android.wizards.installapp.DeployWizard;
import org.eclipse.andmore.android.wizards.installapp.UninstallAppWizard;
import org.eclipse.andmore.android.wizards.installapp.DeployWizard.INSTALL_TYPE;
import org.eclipse.andmore.android.wizards.mat.DumpHPROFWizard;
import org.eclipse.andmore.android.wizards.monkey.IMonkeyConfigurationConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IOConsoleOutputStream;

import com.android.ddmlib.FileListingService;
import com.android.ddmlib.FileListingService.FileEntry;
import com.android.ddmlib.IDevice;
import com.android.ddmuilib.ScreenShotDialog;
import com.android.sdklib.IAndroidTarget;

public class DDMSUtils {
	private static final Map<String, FileListingService> deviceFileListingServiceMap = new HashMap<String, FileListingService>();

	/**
	 * The APK extension
	 */
	private static final String APK_FILE_EXTENSION = "apk";

	/**
	 * Parameter for overwriting existing applications, if any
	 */
	private static final String ADB_INSTALL_OVERWRITE = "-r";

	/**
	 * Options to be used with adb to indicate package manager application
	 */
	private static final String PM_CMD = "pm";

	/**
	 * Options to be used with adb to run monkey application
	 */
	private static final String MONKEY_CMD = "monkey";

	/**
	 * Uninstall directive to the package manager
	 */
	private static final String PM_UNINSTALL_DIRECTIVE = "uninstall";

	/**
	 * List packages directive
	 */
	private static final String PM_LIST_DIRECTIVE = "list";

	/**
	 * List packages directive
	 */
	private static final String PM_PACKAGES_DIRECTIVE = "packages";

	/**
	 * List packages force directive
	 */
	private static final String PM_PACKAGES_DIRECTIVE_FORCE = "-f";

	/**
	 * Monkey packages directive
	 */
	private static final String MONKEY_PACKAGES_DIRECTIVE = " -p ";

	/**
	 * Options to be used with adb to indicate install operation
	 */
	private static final String INSTALL_CMD = "install";

	/**
	 * This word must exist in the adb install commmand answer to indicate
	 * succefull installation
	 */
	private static final String SUCCESS_CONSTANT = "Success";

	private static final DdmsRunnable disconnectedListener = new DdmsRunnable() {
		@Override
		public void run(String serialNumber) {
			synchronized (deviceFileListingServiceMap) {
				deviceFileListingServiceMap.remove(serialNumber);
			}
		}
	};

	static {
		StudioAndroidEventManager.asyncAddDeviceChangeListeners(null, disconnectedListener);
	}

	public static void takeScreenshot(final String serialNumber) {
		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				ScreenShotDialog sshot = new ScreenShotDialog(new Shell(shell));
				sshot.open(DDMSFacade.getDeviceBySerialNumber(serialNumber));
			}
		});
	}

	public static void getApplicationDatabases(String serialNumber, String applicationName,
			IDatabaseListingListener listener) {
		LinkedList<String> pathSegments = new LinkedList<String>();
		pathSegments.add("data");
		pathSegments.add("data");
		pathSegments.add(applicationName);
		pathSegments.add("databases");

		FileListingService fileListing = getFileListingService(serialNumber);

		if (fileListing != null) {
			FileEntry root = fileListing.getRoot();
			FileEntry[] children = null;

			children = fileListing
					.getChildren(root, true, new FileListingReceiver(pathSegments, fileListing, listener));
			/*
			 * If children isn't null means that file listing service found the
			 * files in it cache to speed up the operation
			 */
			if (children != null) {
				List<String> databases = new ArrayList<String>();
				FileEntry temp1 = null, temp2 = root;
				// start from root
				while ((children != null)) {
					// if we have something to search for
					if (pathSegments.size() > 0) {
						String theSegment = pathSegments.remove(0);
						temp1 = temp2.findChild(theSegment);

						if (temp1 != null) {
							temp2 = temp1;
							children = fileListing.getChildren(temp2, true, new FileListingReceiver(pathSegments,
									fileListing, listener));
						} else {
							children = null;
							listener.databasesFound(databases);
						}
					}
					// else just notify the listener
					else {
						if (children != null) {
							for (FileEntry child : children) {
								if (child.getName().endsWith(".db")) {
									databases.add(child.getName());
								}
							}
							children = null;
						}
						listener.databasesFound(databases);
					}
				}
			}
		}
	}

	public static List<String> getApplicationDatabases(String serialNumber, String applicationName) throws IOException {
		List<String> dbs = new ArrayList<String>();

		String appDbPath = "/data/data/" + applicationName + "/databases/";

		Collection<String> commandOutput = DDMSFacade.execRemoteApp(serialNumber, "ls " + appDbPath,
				new NullProgressMonitor());
		List<String> dbPathCandidates = new ArrayList(commandOutput.size() + 10);

		for (String commandOutline : commandOutput) {
			String[] strings = commandOutline.split(" ");
			for (String string : strings) {
				if (string.trim().endsWith(".db")) {
					dbPathCandidates.add(string);
				}
			}
		}

		return dbPathCandidates;
	}

	/**
	 * @param serialNumber
	 * @return
	 */
	private static FileListingService getFileListingService(String serialNumber) {
		FileListingService fileListing = null;
		IDevice dev = DDMSFacade.getDeviceBySerialNumber(serialNumber);
		if (dev != null) {
			synchronized (dev) {
				fileListing = deviceFileListingServiceMap.get(serialNumber);
			}
			if (fileListing == null) {
				fileListing = dev.getFileListingService();
				synchronized (deviceFileListingServiceMap) {
					deviceFileListingServiceMap.put(serialNumber, fileListing);
				}

			}
		}
		return fileListing;
	}

	/**
	 * This method returns the current language and country in use by given
	 * emulation instance.
	 * 
	 * @param serialNumber
	 *            The serial number of emulation instance
	 * @return An array of Strings containing the command results.
	 */
	public static String[] getCurrentEmulatorLanguageAndCountry(final String serialNumber) {
		ArrayList<String[]> commands = createCurrentEmulatorLanguageAndCountryCommand(serialNumber);
		String[] responses = new String[2];
		String[] languageCommand = commands.get(0);
		String[] countryCommand = commands.get(1);
		String languageCommandResult = null;
		String countryCommandResult = null;

		try {
			// Do not write the command output to the console
			languageCommandResult = DDMSFacade.executeCommand(languageCommand, null);
			countryCommandResult = DDMSFacade.executeCommand(countryCommand, null);
			responses[0] = languageCommandResult.replaceAll("\\n$", "");
			responses[1] = countryCommandResult.replaceAll("\\n$", "");
		} catch (IOException e) {
			StudioLogger.error("Deploy: Could not execute adb current language command.");
		}
		return responses;
	}

	public static InstallPackageBean installPackageWizard() {

		final InstallPackageBean bean = new InstallPackageBean();

		final Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			@Override
			public void run() {
				try {
					String defaultPath = null;
					DeployWizard wizard;
					IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
					if (window != null) {
						ISelection selection = window.getSelectionService().getSelection();
						if (selection instanceof IStructuredSelection) {
							IStructuredSelection workbenchSSelection = (IStructuredSelection) selection;
							for (Object o : workbenchSSelection.toList()) {
								if (o instanceof IFile) {
									IFile file = (IFile) o;
									if (file.getFileExtension().equalsIgnoreCase(APK_FILE_EXTENSION)) {
										defaultPath = file.getLocation().toOSString();
									}
								}
							}
						}
					}
					wizard = new DeployWizard(defaultPath);
					wizard.init(PlatformUI.getWorkbench(), new StructuredSelection());
					WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow()
							.getShell(), wizard);
					dialog.setPageSize(500, 200);
					if (dialog.open() == IDialogConstants.OK_ID) {
						bean.setPackagePath(wizard.getPackagePath());
						bean.setCanOverwrite(wizard.canOverwrite());
					}
				} catch (Throwable e) {
					StudioLogger.error(DDMSFacade.class, "Error executing deploy wizard", e);
				}
			}
		});

		return bean;
	}

	public static IStatus installPackage(final String serialNumber, InstallPackageBean bean) {
		IStatus status = Status.CANCEL_STATUS;

		if ((bean.getPackagePath() != null) && (bean.getCanOverwrite() != null)) {
			OutputStream consoleOut = null;
			try {
				consoleOut = EclipseUtils.getStudioConsoleOutputStream(true);
				status = installPackage(serialNumber, bean.getPackagePath(), bean.getCanOverwrite(), consoleOut);
			} finally {
				try {
					if (consoleOut != null) {
						consoleOut.close();
					}
				} catch (IOException e) {
					StudioLogger.error("Install App: could not close console stream" + e.getMessage());
				}
			}
		}

		if (status.isOK()) {
			StudioAndroidEventManager.fireEvent(EventType.PACKAGE_INSTALLED, serialNumber);
		}

		return status;
	}

	/**
	 * Install an application on an emulator instance
	 * 
	 * @param serialNumber
	 *            The serial number of the device where the application will be
	 *            installed
	 * @param path
	 *            Path of the package containing the application to be installed
	 * @param installationType
	 *            one of the {@link INSTALL_TYPE} install types available
	 * @param force
	 *            Perform the operation without asking for user intervention
	 * 
	 * @return the status of the operation (OK, Cancel or Error+ErrorMessage)
	 */
	public static IStatus installPackage(String serialNumber, String path, INSTALL_TYPE installationType,
			OutputStream processOut) {
		IStatus status = null;

		if (installationType.equals(INSTALL_TYPE.UNINSTALL)) {
			status = uninstallPackage(new File(path), serialNumber, processOut);
		}

		boolean overwrite = installationType.equals(INSTALL_TYPE.OVERWRITE);
		status = installPackage(serialNumber, path, overwrite, processOut);

		return status;
	}

	/**
	 * Uninstall the given package (if installed) in the given serialNumber
	 * device
	 * 
	 * @param path
	 *            the package path
	 * @param serialNumber
	 *            the device serial number
	 */
	public static IStatus uninstallPackage(File path, String serialNumber, OutputStream processOut) {
		IStatus returnStatus = null;
		if ((path != null) && path.exists() && path.isFile()) {
			IDevice dev = DDMSFacade.getDeviceBySerialNumber(serialNumber);
			String apiLevel = dev.getProperty("ro.build.version.sdk");
			IAndroidTarget target = SdkUtils.getTargetByAPILevel(Integer.parseInt(apiLevel));
			String aaptPath = SdkUtils.getTargetAAPTPath(target);
			if (aaptPath != null) {

				// resolve package name
				String[] aaptComm = new String[] { aaptPath, "d", "badging", path.toString() };

				InputStreamReader reader = null;
				BufferedReader bufferedReader = null;

				try {
					Process aapt = Runtime.getRuntime().exec(aaptComm);

					reader = new InputStreamReader(aapt.getInputStream());
					bufferedReader = new BufferedReader(reader);
					String infoLine = bufferedReader.readLine();

					infoLine = infoLine.split(" ")[1].split("=")[1].replaceAll("'", "");
					returnStatus = uninstallPackage(serialNumber, infoLine, processOut);

				} catch (Exception e) {
					returnStatus = new Status(IStatus.ERROR, AndroidPlugin.PLUGIN_ID,
							AndroidNLS.ERR_DDMSFacade_UninstallPackageException, e);
				} finally {
					try {
						if (reader != null) {
							reader.close();
						}
						if (bufferedReader != null) {
							bufferedReader.close();
						}
					} catch (IOException e) {
						StudioLogger.error("Uninstall app could not close stream. " + e.getMessage());
					}

				}
			} else {
				StudioLogger.error(DDMSFacade.class,
						"Impossible to check APK package name. No android targets found inside SDK");
			}

		} else {
			returnStatus = new Status(IStatus.ERROR, AndroidPlugin.PLUGIN_ID,
					AndroidNLS.ERR_DDMSFacade_UninstallPackage);
		}
		return returnStatus;
	}

	/**
	 * Uninstall the given package within device with given serial number
	 * 
	 * @param serialNumber
	 * @param packageName
	 * @param processOutput
	 *            outputStream to write output of the process
	 */
	public static IStatus uninstallPackage(String serialNumber, String packageName, OutputStream processOutput) {
		IStatus status = Status.OK_STATUS;
		String command[] = createUninstallCommand(serialNumber, packageName);

		try {
			String commandResult = DDMSFacade.executeCommand(command, processOutput);
			if (!commandResult.toLowerCase().contains(SUCCESS_CONSTANT.toLowerCase())) {
				status = new Status(IStatus.ERROR, AndroidPlugin.PLUGIN_ID,
						AndroidNLS.ERR_DDMSFacade_UninstallPackageError + ": " + packageName);
			}

		} catch (Exception e) {
			status = new Status(IStatus.ERROR, AndroidPlugin.PLUGIN_ID,
					AndroidNLS.ERR_DDMSFacade_UninstallPackageException, e);
			StudioLogger.error(DDMSFacade.class, "Failed to remove package: " + packageName + ". " + e.getMessage());
		}
		return status;
	}

	/**
	 * Run the Monkey command for the given package within device with given
	 * serial number
	 * 
	 * @param serialNumber
	 * @param packageName
	 * @param processOutput
	 * @param otherCmd
	 * @return the status of the monkey process
	 */
	public static IStatus runMonkey(String serialNumber, String allPackages, OutputStream processOutput, String otherCmd) {
		IStatus status = Status.OK_STATUS;
		String command[] = createMonkeyCommand(serialNumber, allPackages, otherCmd);

		try {
			DDMSFacade.executeCommand(command, processOutput);

		} catch (Exception e) {
			EclipseUtils.showErrorDialog(AndroidNLS.UI_MonkeyError_Title, AndroidNLS.UI_MonkeyError_Msg);
			StudioLogger.error(DDMSFacade.class, "Failed to run monkey command: " + command + " " + e.getMessage());
		}
		return status;
	}

	/**
	 * Uninstall packages from the given serialNumber device
	 * 
	 * @param serialNumber
	 * @param packagesToUninstall
	 * @param outputStream
	 * @return the status of the uninstall process or null if no packages were
	 *         uninstalled
	 */
	private static IStatus uninstallPackages(String serialNumber, ArrayList<String> packagesToUninstall,
			OutputStream outputStream) {

		IStatus returnStatus = null;
		for (String packageToUninstall : packagesToUninstall) {
			StudioLogger.info(DDMSUtils.class, "Removing package: " + packageToUninstall);
			IStatus temp = uninstallPackage(serialNumber, packageToUninstall, outputStream);
			if (!temp.isOK()) {
				if (returnStatus == null) {
					returnStatus = new MultiStatus(AndroidPlugin.PLUGIN_ID, 0,
							AndroidNLS.ERR_DDMSFacade_UninstallPackageError, null);
				}

				((MultiStatus) returnStatus).add(temp);
			}
		}
		return returnStatus == null ? Status.OK_STATUS : returnStatus;
	}

	/**
	 * Run monkey command on the given packages with the specified commands.
	 * 
	 * @param serialNumber
	 * @param packagesToRunMonkey
	 * @param outputStream
	 * @param otherCmds
	 * @return the status of the monkey process or null if the process was not
	 *         successful
	 */
	private static IStatus runMonkey(String serialNumber, ArrayList<String> packagesToRunMonkey,
			OutputStream outputStream, String otherCmds) {

		IStatus returnStatus = null;
		String allPackages = "";
		for (String packageToRunMonkey : packagesToRunMonkey) {
			allPackages += MONKEY_PACKAGES_DIRECTIVE + packageToRunMonkey;
		}
		StudioLogger.info(DDMSUtils.class, "Running monkey for: " + allPackages);
		IStatus temp = runMonkey(serialNumber, allPackages, outputStream, otherCmds);
		if (!temp.isOK()) {
			if (returnStatus == null) {
				returnStatus = new MultiStatus(AndroidPlugin.PLUGIN_ID, 0, AndroidNLS.ERR_DDMSFacade_MonkeyError, null);
			}

			((MultiStatus) returnStatus).add(temp);
		}
		return returnStatus == null ? Status.OK_STATUS : returnStatus;
	}

	/**
	 * Uninstall packages from the given device. A Wizard will be opened asking
	 * the user which packages to uninstall
	 * 
	 * @param serialNumber
	 * @return the status of the operation
	 */
	public static IStatus uninstallPackage(final String serialNumber) {
		final ArrayList<String> packagesToUninstall = new ArrayList<String>();
		IStatus status = null;
		// wrap the dialog within a final variable
		final UninstallAppWizard[] wizard = new UninstallAppWizard[1];

		// do package listing async
		Thread listingThread = new Thread("listPackages") {

			@Override
			public void run() {
				Map<String, String> installedPackages = null;
				try {
					installedPackages = listInstalledPackages(serialNumber);
				} catch (IOException e1) {
					installedPackages = new HashMap<String, String>(0);
				}

				while (wizard[0] == null) {
					try {
						sleep(100);
					} catch (InterruptedException e) {
						// do nothing
					}
				}
				wizard[0].setAvailablePackages(installedPackages);
			};
		};

		listingThread.start();

		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				UninstallAppWizard unAppWiz = new UninstallAppWizard();
				WizardDialog dialog = new WizardDialog(new Shell(PlatformUI.getWorkbench().getActiveWorkbenchWindow()
						.getShell()), unAppWiz);
				wizard[0] = unAppWiz;
				dialog.open();
				List<String> selectedPackages = wizard[0].getSelectedPackages();
				if (selectedPackages != null) {
					for (String aPackage : selectedPackages) {
						packagesToUninstall.add(aPackage);
					}
				}
			}
		});

		if (packagesToUninstall.size() > 0) {
			OutputStream consoleOut = null;

			try {
				consoleOut = EclipseUtils.getStudioConsoleOutputStream(true);
				status = uninstallPackages(serialNumber, packagesToUninstall, consoleOut);
			} finally {
				try {
					if (consoleOut != null) {
						consoleOut.close();
					}
				} catch (IOException e) {
					StudioLogger.error("Uninstall App: could not close console stream" + e.getMessage());
				}
			}
		}
		if (status != null) {
			if (status.isOK()) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
								AndroidNLS.UI_UninstallApp_SucessDialogTitle, AndroidNLS.UI_UninstallApp_Message);
					}
				});
				StudioAndroidEventManager.fireEvent(EventType.PACKAGE_UNINSTALLED, serialNumber);
			} else {
				EclipseUtils.showErrorDialog(AndroidNLS.UI_UninstallApp_ERRDialogTitle,
						AndroidNLS.UI_UninstallApp_ERRUninstallApp, status);
			}
		}
		// all return is successful since operations are async
		return Status.OK_STATUS;

	}

	/**
	 * If there is no Monkey Launch configuration for the given deviceName, it
	 * is created.
	 * 
	 * @param deviceName
	 */
	private static ILaunchConfiguration createLaunchConfiguration(String deviceName) {
		ILaunchConfiguration config = null;
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType motodevLaunchType = launchManager
				.getLaunchConfigurationType(IMonkeyConfigurationConstants.LAUNCH_CONFIGURATION_TYPE_EXTENSION_ID);
		String launchConfigurationName = launchManager
				.generateUniqueLaunchConfigurationNameFrom(IMonkeyConfigurationConstants.NEW_CONFIGURATION_NAME);
		try {
			ILaunchConfigurationWorkingCopy workingCopy = motodevLaunchType.newInstance(null, launchConfigurationName);
			workingCopy.setAttribute(IMonkeyConfigurationConstants.ATTR_DEVICE_INSTANCE_NAME, deviceName);
			config = workingCopy.doSave();
		} catch (CoreException e) {
			EclipseUtils.showErrorDialog(AndroidNLS.UI_MonkeyError_Title, e.getMessage());
		}
		return config;

	}

	/**
	 * Run adb monkey.
	 * 
	 * @param serialNumber
	 * @param deviceName
	 * @return the status of the operation
	 */
	public static IStatus runMonkey(final String serialNumber, final String deviceName) {

		ILaunchConfigurationType type = DebugPlugin.getDefault().getLaunchManager()
				.getLaunchConfigurationType(IMonkeyConfigurationConstants.LAUNCH_CONFIGURATION_TYPE_EXTENSION_ID);
		ILaunchConfiguration[] launchs;
		try {
			launchs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(type);

			ILaunchConfiguration launchConfig = null;
			for (int i = 0; i < launchs.length; i++) {
				launchConfig = launchs[i];
				String configDeviceName = launchConfig.getAttribute(
						IMonkeyConfigurationConstants.ATTR_DEVICE_INSTANCE_NAME, "");
				if (configDeviceName.equals(deviceName)) {
					break;
				} else {
					launchConfig = null;
				}

			}
			if (launchConfig == null) {
				launchConfig = createLaunchConfiguration(deviceName);

			}

			final ILaunchGroup lc = DebugUITools.getLaunchGroup(launchConfig, ILaunchManager.RUN_MODE);

			final IStructuredSelection selection = new StructuredSelection(launchConfig);

			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {

					DebugUITools.openLaunchConfigurationDialogOnGroup(new Shell(PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getShell()), selection, lc.getIdentifier(), null);

				}
			});

		} catch (CoreException e) {
			StudioLogger.error("Monkey: could not open the launch configuration dialog " + e.getMessage());

		}

		// UDC log for monkey execution
		StudioLogger.collectUsageData(UsageDataConstants.WHAT_MONKEY_EXEC, UsageDataConstants.KIND_MONKEY_EXEC,
				"Monkey executed", //$NON-NLS-1$
				AndroidPlugin.PLUGIN_ID, AndroidPlugin.getDefault().getBundle().getBundleContext().getBundle()
						.getVersion().toString());

		// all return is successful since operations are async
		return Status.OK_STATUS;

	}

	/**
	 * Run adb monkey.
	 * 
	 * @param serialNumber
	 * @param packagesToRunMonkey
	 * @param otherCmds
	 * @return the status of the operation
	 */
	public static IStatus runMonkey(final String serialNumber, ArrayList<String> packagesToRunMonkey, String otherCmds) {

		if (packagesToRunMonkey.size() > 0) {
			OutputStream consoleOut = null;

			try {
				consoleOut = EclipseUtils.getStudioConsoleOutputStream(true);
				runMonkey(serialNumber, packagesToRunMonkey, consoleOut, otherCmds);
			} finally {
				try {
					if (consoleOut != null) {
						consoleOut.close();
					}
				} catch (IOException e) {
					StudioLogger.error("Monkey: could not close console stream" + e.getMessage());
				}
			}
		}

		// all return is successful since operations are async
		return Status.OK_STATUS;

	}

	/**
	 * List the installed packages in the device with the serial number Each
	 * package entry carries their package location
	 * 
	 * @param serialNumber
	 * @return a HashMap where keys are the package names and values are package
	 *         location
	 * @throws IOException
	 */
	public static Map<String, String> listInstalledPackages(String serialNumber) throws IOException {
		Map<String, String> packages = new LinkedHashMap<String, String>();
		String sdkPath = SdkUtils.getSdkPath();
		String command[] = new String[] {
				sdkPath + DDMSFacade.PLATFORM_TOOLS_FOLDER + File.separator + DDMSFacade.ADB_COMMAND,
				DDMSFacade.ADB_INSTANCE_PARAMETER, serialNumber, DDMSFacade.SHELL_CMD, PM_CMD, PM_LIST_DIRECTIVE,
				PM_PACKAGES_DIRECTIVE, PM_PACKAGES_DIRECTIVE_FORCE };

		String commResult = DDMSFacade.executeCommand(command, null);
		String[] packageList = null;
		if ((commResult != null) && (commResult.length() > 0) && !commResult.contains("system running?")) {
			packageList = commResult.trim().replaceAll("\n\n", "\n").split("\n");
			int i = 0;
			String aPackage = null;
			String[] packageUnit = null;
			while (i < packageList.length) {
				if (packageList[i].toLowerCase().contains("package:")) {
					String[] splittedPackage = packageList[i].split(":");
					if (splittedPackage.length >= 2) {
						aPackage = splittedPackage[1].trim();
						packageUnit = aPackage.split("=");
						if (packageUnit.length > 1) {
							packages.put(packageUnit[1], packageUnit[0]);
						}
					}
				}
				i++;
			}
		}
		return packages;
	}

	/**
	 * Install an application on an emulator instance
	 * 
	 * @param serialNumber
	 *            The serial number of the device where the application will be
	 *            installed
	 * @param path
	 *            Path of the package containing the application to be installed
	 * @param canOverwrite
	 *            If the application will be installed even if there is a
	 *            previous installation
	 * @param force
	 *            Perform the operation without asking for user intervention
	 * 
	 * @return the status of the operation (OK, Cancel or Error+ErrorMessage)
	 */
	public static IStatus installPackage(String serialNumber, String path, boolean canOverwrite, OutputStream processOut) {
		IStatus status = Status.OK_STATUS;

		// Return if no instance is selected
		if (serialNumber == null) {
			StudioLogger.error("Abort deploy operation. Serial number is null.");
			status = new Status(IStatus.ERROR, AndroidPlugin.PLUGIN_ID,
					AndroidNLS.ERR_DDMSFacade_SerialNumberNullPointer);
		}

		// Return if instance is not started
		if (status.isOK() && !DDMSFacade.isDeviceOnline(serialNumber)) {
			StudioLogger.error("Abort deploy operation. Device is not online.");
			status = new Status(IStatus.ERROR, AndroidPlugin.PLUGIN_ID, "");
		}

		String command_results = "";
		if (status.isOK()) {
			try {
				String[] cmd = createInstallCommand(canOverwrite, path, serialNumber);
				command_results = DDMSFacade.executeCommand(cmd, processOut, serialNumber);

				// Check if the result has a success message
				if (!command_results.contains(SUCCESS_CONSTANT)) {
					status = new Status(IStatus.ERROR, AndroidPlugin.PLUGIN_ID,
							"Error executing the operation. Execution results: " + command_results);
				}
			} catch (IOException e) {
				StudioLogger.error("Deploy: Could not execute adb install command.");
				status = new Status(IStatus.ERROR, AndroidPlugin.PLUGIN_ID, e.getMessage());
			}
		}

		return status;
	}

	/**
	 * Change the emulator language
	 * 
	 * @param serialNumber
	 *            The serial number of the device
	 * @param language
	 *            the language id
	 * @param country
	 *            the country id
	 * @return the status of the operation (OK, Error+ErrorMessage)
	 */
	public static void changeLanguage(final String serialNumber, final String language, final String country) {

		if ((language != null) || (country != null)) {
			/*
			 * A new thread is used since this command takes too long to be
			 * executed
			 */
			Thread thead = new Thread(new Runnable() {

				@Override
				public void run() {

					String[] cmd = createChangeLanguageCommand(serialNumber, language, country);
					try {

						IOConsoleOutputStream consoleOut = EclipseUtils.getStudioConsoleOutputStream(true);
						if (language != null) {
							consoleOut.write(AndroidNLS.UI_ChangeLang_Language + " " + language + "\n");
						}
						if (country != null) {
							consoleOut.write(AndroidNLS.UI_ChangeLang_Country + " " + country + "\n");
						}

						DDMSFacade.executeCommand(cmd, consoleOut);
						consoleOut.write("\n " + serialNumber + ":" + AndroidNLS.UI_ChangeLang_Restart_Device_Manually
								+ "\n\n");
						StudioAndroidEventManager.fireEvent(EventType.LANGUAGE_CHANGED, serialNumber);
					} catch (IOException e) {
						StudioLogger.error("Language: Could not execute adb change language command.");
					}

				}
			});
			thead.start();
		}

	}

	/**
	 * Creates a string with the command that should be called in order to
	 * change the device language
	 * 
	 * @param serialNumber
	 *            The serial number of the device
	 * @param language
	 *            the language id
	 * @param country
	 *            the country id
	 * @return the command to be used to change the device language
	 */
	private static String[] createChangeLanguageCommand(String serialNumber, String language, String country) {
		String cmd[];
		String sdkPath = SdkUtils.getSdkPath();

		String CHANGE_LANGUAGE_CMD = "";
		if (language != null) {
			CHANGE_LANGUAGE_CMD += "setprop persist.sys.language " + language;
		}
		if (country != null) {
			CHANGE_LANGUAGE_CMD += ((CHANGE_LANGUAGE_CMD.length() > 0) ? ";" : "") + "setprop persist.sys.country "
					+ country;
		}

		// The tools folder should exist and be here, but double-checking
		// once more wont kill
		File f = new File(sdkPath + DDMSFacade.PLATFORM_TOOLS_FOLDER + File.separator);
		if (!f.exists()) {
			StudioLogger.error("Language: Could not find tools folder on " + sdkPath + DDMSFacade.PLATFORM_TOOLS_FOLDER
					+ File.separator);
		} else {
			if (!f.isDirectory()) {
				StudioLogger.error("Language: Invalid tools folder " + sdkPath + DDMSFacade.PLATFORM_TOOLS_FOLDER
						+ File.separator);
			}
		}

		String cmdTemp[] = { sdkPath + DDMSFacade.PLATFORM_TOOLS_FOLDER + File.separator + DDMSFacade.ADB_COMMAND,
				DDMSFacade.ADB_INSTANCE_PARAMETER, serialNumber, "shell", CHANGE_LANGUAGE_CMD };
		cmd = cmdTemp;

		return cmd;
	}

	/**
	 * Creates strings with the command that should be called in order to
	 * retrieve the current language and country in use by given emulator
	 * instance.
	 * 
	 * @param serialNumber
	 *            The serial number of the device
	 * @return An ArrayList with command strings to be used to get the current
	 *         emulator language and country.
	 */
	private static ArrayList<String[]> createCurrentEmulatorLanguageAndCountryCommand(String serialNumber) {
		String languageCommand[];
		String countryCommand[];
		String sdkPath = SdkUtils.getSdkPath();
		String GET_LANGUAGE_CMD = "getprop persist.sys.language";
		String GET_COUNTRY_CMD = "getprop persist.sys.country";
		// The tools folder should exist and be here, but double-cheking
		// once more wont kill
		File f = new File(sdkPath + DDMSFacade.PLATFORM_TOOLS_FOLDER + File.separator);
		if (!f.exists()) {
			StudioLogger.error("Language: Could not find tools folder on " + sdkPath + DDMSFacade.PLATFORM_TOOLS_FOLDER
					+ File.separator);
		} else {
			if (!f.isDirectory()) {
				StudioLogger.error("Language: Invalid tools folder " + sdkPath + DDMSFacade.PLATFORM_TOOLS_FOLDER
						+ File.separator);
			}
		}
		String langCmdTemp[] = { sdkPath + DDMSFacade.PLATFORM_TOOLS_FOLDER + File.separator + DDMSFacade.ADB_COMMAND,
				DDMSFacade.ADB_INSTANCE_PARAMETER, serialNumber, "shell", GET_LANGUAGE_CMD };
		String countryCmdTemp[] = {
				sdkPath + DDMSFacade.PLATFORM_TOOLS_FOLDER + File.separator + DDMSFacade.ADB_COMMAND,
				DDMSFacade.ADB_INSTANCE_PARAMETER, serialNumber, "shell", GET_COUNTRY_CMD };
		languageCommand = langCmdTemp;
		countryCommand = countryCmdTemp;

		ArrayList<String[]> commands = new ArrayList<String[]>();
		commands.add(0, languageCommand);
		commands.add(1, countryCommand);
		return commands;
	}

	/**
	 * Creates a string with the command that should be called in order to
	 * install the application
	 * 
	 * @param canOverwrite
	 *            If true, than existent application will be overwritten
	 * @param path
	 *            Location of the package containing the application
	 * @param serialNumber
	 *            The serial number of the device to be targeted
	 * 
	 * @return
	 */
	private static String[] createInstallCommand(boolean canOverwrite, String path, String serialNumber) {
		String cmd[];
		String sdkPath = SdkUtils.getSdkPath();

		// The tools folder should exist and be here, but double-checking
		// once more wont kill
		File f = new File(sdkPath + DDMSFacade.PLATFORM_TOOLS_FOLDER + File.separator);
		if (!f.exists()) {
			StudioLogger.error("Deploy: Could not find tools folder on " + sdkPath + DDMSFacade.PLATFORM_TOOLS_FOLDER
					+ File.separator);
		} else {
			if (!f.isDirectory()) {
				StudioLogger.error("Deploy: Invalid tools folder " + sdkPath + DDMSFacade.PLATFORM_TOOLS_FOLDER
						+ File.separator);
			}
		}

		if (canOverwrite) {
			// If overwrite option is checked, create command with the -r
			// paramater
			String cmdTemp[] = { sdkPath + DDMSFacade.PLATFORM_TOOLS_FOLDER + File.separator + DDMSFacade.ADB_COMMAND,
					DDMSFacade.ADB_INSTANCE_PARAMETER, serialNumber, INSTALL_CMD, ADB_INSTALL_OVERWRITE, path };
			cmd = cmdTemp;
		} else {
			// If overwrite option is unchecked, create command without the -r
			// paramater
			String cmdTemp[] = { sdkPath + DDMSFacade.PLATFORM_TOOLS_FOLDER + File.separator + DDMSFacade.ADB_COMMAND,
					DDMSFacade.ADB_INSTANCE_PARAMETER, serialNumber, INSTALL_CMD, path };
			cmd = cmdTemp;
		}

		return cmd;
	}

	private static String[] createUninstallCommand(String serialNumber, String packageName) {
		String sdkPath = SdkUtils.getSdkPath();
		// The tools folder should exist and be here, but double-checking
		// once more wont kill
		File f = new File(sdkPath + DDMSFacade.PLATFORM_TOOLS_FOLDER + File.separator);
		if (!f.exists()) {
			StudioLogger.error("Run: Could not find tools folder on " + sdkPath + DDMSFacade.PLATFORM_TOOLS_FOLDER
					+ File.separator);
		} else {
			if (!f.isDirectory()) {
				StudioLogger.error("Run: Invalid tools folder " + sdkPath + DDMSFacade.PLATFORM_TOOLS_FOLDER
						+ File.separator);
			}
		}

		String cmd[] = { sdkPath + DDMSFacade.PLATFORM_TOOLS_FOLDER + File.separator + DDMSFacade.ADB_COMMAND,
				DDMSFacade.ADB_INSTANCE_PARAMETER, serialNumber, DDMSFacade.SHELL_CMD, PM_CMD, PM_UNINSTALL_DIRECTIVE,
				packageName };

		return cmd;

	}

	/**
	 * Mount the adb monkey command based on the given parameters.
	 * 
	 * @param serialNumber
	 * @param packagesName
	 * @param otherCmd
	 * @return the array of strings containing the monkey command to be
	 *         executed.
	 */
	private static String[] createMonkeyCommand(String serialNumber, String packagesName, String otherCmd) {
		String sdkPath = SdkUtils.getSdkPath();
		// The tools folder should exist and be here, but double-checking
		// once more wont kill
		File f = new File(sdkPath + DDMSFacade.PLATFORM_TOOLS_FOLDER + File.separator);
		if (!f.exists()) {
			StudioLogger.error("Run: Could not find tools folder on " + sdkPath + DDMSFacade.PLATFORM_TOOLS_FOLDER
					+ File.separator);
		} else {
			if (!f.isDirectory()) {
				StudioLogger.error("Run: Invalid tools folder " + sdkPath + DDMSFacade.PLATFORM_TOOLS_FOLDER
						+ File.separator);
			}
		}

		String cmd[] = { sdkPath + DDMSFacade.PLATFORM_TOOLS_FOLDER + File.separator + DDMSFacade.ADB_COMMAND,
				DDMSFacade.ADB_INSTANCE_PARAMETER, serialNumber, DDMSFacade.SHELL_CMD, MONKEY_CMD, packagesName,
				otherCmd };

		return cmd;

	}

	/**
	 * Dump HPROF service implementation
	 * 
	 * @param serialNumber
	 *            The device serial number
	 * @param monitor
	 * @return A IStatus describing if the service was successful or not
	 */
	public static IStatus dumpHPROF(final String serialNumber, IProgressMonitor monitor) {
		IStatus status;

		// Selected app. The array should have only 1 element
		final String[] selectedAppSet = new String[1];

		// Instantiate the wizard and populate it with the retrieved process
		// list
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				DumpHPROFWizard dumpHPROFWizard = new DumpHPROFWizard(serialNumber);
				WizardDialog dialog = new WizardDialog(new Shell(PlatformUI.getWorkbench().getActiveWorkbenchWindow()
						.getShell()), dumpHPROFWizard);
				dialog.open();

				// Get the selected application
				selectedAppSet[0] = dumpHPROFWizard.getSelectedApp();

			}
		});

		if (selectedAppSet[0] != null) {
			// Dump HPROF file based on the selected application
			status = DDMSFacade.dumpHprofFile(selectedAppSet[0], serialNumber, monitor);
		} else {
			status = Status.CANCEL_STATUS;
		}

		return status;
	}

	public static int getDeviceApiVersion(String serialNumber) {
		int deviceSdkVersion = -1;
		String deviceProperty = DDMSFacade.getDeviceProperty(serialNumber, "ro.build.version.sdk");
		if (deviceProperty != null) {
			deviceSdkVersion = Integer.parseInt(deviceProperty);
		}

		return deviceSdkVersion;
	}

	public static boolean remoteFileExists(String serialNumber, String remotePath) throws IOException {
		boolean found = false;
		Collection<String> results = DDMSFacade.execRemoteApp(serialNumber,
				"ls " + FileUtil.getEscapedPath(remotePath, Platform.OS_LINUX), new NullProgressMonitor());
		for (String result : results) {
			if (result.equals(remotePath)) {
				found = true;
				break;
			}
		}
		return found;
	}
}
