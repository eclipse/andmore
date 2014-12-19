/*
 * Copyright (C) 2007 The Android Open Source Project
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

package org.eclipse.andmore.ddms;

import com.android.annotations.NonNull;
import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener;
import com.android.ddmlib.Client;
import com.android.ddmlib.DdmPreferences;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.Log;
import com.android.ddmlib.Log.ILogOutput;
import com.android.ddmlib.Log.LogLevel;
import com.android.ddmuilib.DdmUiPreferences;
import com.android.ddmuilib.DevicePanel.IUiSelectionListener;
import com.android.ddmuilib.StackTracePanel;
import com.android.ddmuilib.console.DdmConsole;
import com.android.ddmuilib.console.IDdmConsole;

import org.eclipse.andmore.ddms.i18n.Messages;
import org.eclipse.andmore.ddms.preferences.PreferenceInitializer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * The activator class controls the plug-in life cycle
 */
public final class DdmsPlugin extends AbstractUIPlugin implements IDeviceChangeListener, IUiSelectionListener,
		com.android.ddmuilib.StackTracePanel.ISourceRevealer {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.andmore.ddms"; //$NON-NLS-1$

	/** The singleton instance */
	private static DdmsPlugin sPlugin;

	/** Location of the adb command line executable */
	private static String sAdbLocation;
	private static String sToolsFolder;
	private static String sHprofConverter;

	private boolean mHasDebuggerConnectors;
	/**
	 * debugger connectors for already running apps. Initialized from an
	 * extension point.
	 */
	private IDebuggerConnector[] mDebuggerConnectors;
	private ITraceviewLauncher[] mTraceviewLaunchers;
	private List<IClientAction> mClientSpecificActions = null;

	/** Console for DDMS log message */
	private MessageConsole mDdmsConsole;

	private IDevice mCurrentDevice;
	private Client mCurrentClient;
	private boolean mListeningToUiSelection = false;

	private final ArrayList<ISelectionListener> mListeners = new ArrayList<ISelectionListener>();

	private Color mRed;

	/**
	 * Classes which implement this interface provide methods that deals with
	 * {@link IDevice} and {@link Client} selectionchanges.
	 */
	public interface ISelectionListener {

		/**
		 * Sent when a new {@link Client} is selected.
		 * 
		 * @param selectedClient
		 *            The selected client. If null, no clients are selected.
		 */
		public void selectionChanged(Client selectedClient);

		/**
		 * Sent when a new {@link IDevice} is selected.
		 * 
		 * @param selectedDevice
		 *            the selected device. If null, no devices are selected.
		 */
		public void selectionChanged(IDevice selectedDevice);
	}

	/**
	 * The constructor
	 */
	public DdmsPlugin() {
		sPlugin = this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);

		final Display display = getDisplay();

		// get the eclipse store
		final IPreferenceStore eclipseStore = getPreferenceStore();

		AndroidDebugBridge.addDeviceChangeListener(this);

		DdmUiPreferences.setStore(eclipseStore);

		// DdmUiPreferences.displayCharts();

		// set the consoles.
		mDdmsConsole = new MessageConsole("DDMS", null); //$NON-NLS-1$
		ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { mDdmsConsole });

		final MessageConsoleStream consoleStream = mDdmsConsole.newMessageStream();
		final MessageConsoleStream errorConsoleStream = mDdmsConsole.newMessageStream();
		mRed = new Color(display, 0xFF, 0x00, 0x00);

		// because this can be run, in some cases, by a non UI thread, and
		// because
		// changing the console properties update the UI, we need to make this
		// change
		// in the UI thread.
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				errorConsoleStream.setColor(mRed);
			}
		});

		// set up the ddms log to use the ddms console.
		Log.setLogOutput(new ILogOutput() {
			@Override
			public void printLog(LogLevel logLevel, String tag, String message) {
				if (logLevel.getPriority() >= LogLevel.ERROR.getPriority()) {
					printToStream(errorConsoleStream, tag, message);
					showConsoleView(mDdmsConsole);
				} else {
					printToStream(consoleStream, tag, message);
				}
			}

			@Override
			public void printAndPromptLog(final LogLevel logLevel, final String tag, final String message) {
				printLog(logLevel, tag, message);
				// dialog box only run in UI thread..
				display.asyncExec(new Runnable() {
					@Override
					public void run() {
						Shell shell = display.getActiveShell();
						if (logLevel == LogLevel.ERROR) {
							MessageDialog.openError(shell, tag, message);
						} else {
							MessageDialog.openWarning(shell, tag, message);
						}
					}
				});
			}

		});

		// set up the ddms console to use this objects
		DdmConsole.setConsole(new IDdmConsole() {
			@Override
			public void printErrorToConsole(String message) {
				printToStream(errorConsoleStream, null, message);
				showConsoleView(mDdmsConsole);
			}

			@Override
			public void printErrorToConsole(String[] messages) {
				for (String m : messages) {
					printToStream(errorConsoleStream, null, m);
				}
				showConsoleView(mDdmsConsole);
			}

			@Override
			public void printToConsole(String message) {
				printToStream(consoleStream, null, message);
			}

			@Override
			public void printToConsole(String[] messages) {
				for (String m : messages) {
					printToStream(consoleStream, null, m);
				}
			}
		});

		// set the listener for the preference change
		eclipseStore.addPropertyChangeListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				// get the name of the property that changed.
				String property = event.getProperty();

				if (PreferenceInitializer.ATTR_DEBUG_PORT_BASE.equals(property)) {
					DdmPreferences.setDebugPortBase(eclipseStore.getInt(PreferenceInitializer.ATTR_DEBUG_PORT_BASE));
				} else if (PreferenceInitializer.ATTR_SELECTED_DEBUG_PORT.equals(property)) {
					DdmPreferences.setSelectedDebugPort(eclipseStore
							.getInt(PreferenceInitializer.ATTR_SELECTED_DEBUG_PORT));
				} else if (PreferenceInitializer.ATTR_THREAD_INTERVAL.equals(property)) {
					DdmUiPreferences.setThreadRefreshInterval(eclipseStore
							.getInt(PreferenceInitializer.ATTR_THREAD_INTERVAL));
				} else if (PreferenceInitializer.ATTR_LOG_LEVEL.equals(property)) {
					DdmPreferences.setLogLevel(eclipseStore.getString(PreferenceInitializer.ATTR_LOG_LEVEL));
				} else if (PreferenceInitializer.ATTR_TIME_OUT.equals(property)) {
					DdmPreferences.setTimeOut(eclipseStore.getInt(PreferenceInitializer.ATTR_TIME_OUT));
				} else if (PreferenceInitializer.ATTR_USE_ADBHOST.equals(property)) {
					DdmPreferences.setUseAdbHost(eclipseStore.getBoolean(PreferenceInitializer.ATTR_USE_ADBHOST));
				} else if (PreferenceInitializer.ATTR_ADBHOST_VALUE.equals(property)) {
					DdmPreferences.setAdbHostValue(eclipseStore.getString(PreferenceInitializer.ATTR_ADBHOST_VALUE));
				}
			}
		});

		// do some last initializations

		// set the preferences.
		PreferenceInitializer.setupPreferences();

		// this class is set as the main source revealer and will look at all
		// the implementations
		// of the extension point. see #reveal(String, String, int)
		StackTracePanel.setSourceRevealer(this);

		/*
		 * Load the extension point implementations. The first step is to load
		 * the IConfigurationElement representing the implementations. The 2nd
		 * step is to use these objects to instantiate the implementation
		 * classes.
		 * 
		 * Because the 2nd step will trigger loading the plug-ins providing the
		 * implementations, and those plug-ins could access DDMS classes (like
		 * ADT), this 2nd step should be done in a Job to ensure that DDMS is
		 * loaded, so that the other plug-ins can load.
		 * 
		 * Both steps could be done in the 2nd step but some of DDMS UI rely on
		 * knowing if there is an implementation or not (DeviceView), so we do
		 * the first steps in start() and, in some case, record it.
		 */

		// get the IConfigurationElement for the debuggerConnector right away.
		final IConfigurationElement[] dcce = findConfigElements("org.eclipse.andmore.ddms.debuggerConnector"); //$NON-NLS-1$
		mHasDebuggerConnectors = dcce.length > 0;

		// get the other configElements and instantiante them in a Job.
		new Job(Messages.DdmsPlugin_DDMS_Post_Create_Init) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					// init the lib
					AndroidDebugBridge.init(true /* debugger support */);

					// get the available adb locators
					IConfigurationElement[] elements = findConfigElements("org.eclipse.andmore.ddms.toolsLocator"); //$NON-NLS-1$

					IToolsLocator[] locators = instantiateToolsLocators(elements);

					for (IToolsLocator locator : locators) {
						try {
							String adbLocation = locator.getAdbLocation();
							String traceviewLocation = locator.getTraceViewLocation();
							String hprofConvLocation = locator.getHprofConvLocation();
							if (adbLocation != null && traceviewLocation != null && hprofConvLocation != null) {
								// checks if the location is valid.
								if (setToolsLocation(adbLocation, hprofConvLocation, traceviewLocation)) {

									AndroidDebugBridge.createBridge(sAdbLocation, true /* forceNewBridge */);

									// no need to look at the other locators.
									break;
								}
							}
						} catch (Throwable t) {
							// ignore, we'll just not use this implementation.
						}
					}

					// get the available debugger connectors
					mDebuggerConnectors = instantiateDebuggerConnectors(dcce);

					// get the available Traceview Launchers.
					elements = findConfigElements("org.eclipse.andmore.ddms.traceviewLauncher"); //$NON-NLS-1$
					mTraceviewLaunchers = instantiateTraceviewLauncher(elements);

					return Status.OK_STATUS;
				} catch (CoreException e) {
					return e.getStatus();
				}
			}
		}.schedule();
	}

	private void showConsoleView(MessageConsole console) {
		ConsolePlugin.getDefault().getConsoleManager().showConsoleView(console);
	}

	/**
	 * Obtain a list of configuration elements that extend the given extension
	 * point.
	 */
	IConfigurationElement[] findConfigElements(String extensionPointId) {
		// get the adb location from an implementation of the ADB Locator
		// extension point.
		IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = extensionRegistry.getExtensionPoint(extensionPointId);
		if (extensionPoint != null) {
			return extensionPoint.getConfigurationElements();
		}

		// shouldn't happen or it means the plug-in is broken.
		return new IConfigurationElement[0];
	}

	/**
	 * Finds if any other plug-in is extending the exposed Extension Point
	 * called adbLocator.
	 *
	 * @return an array of all locators found, or an empty array if none were
	 *         found.
	 */
	private IToolsLocator[] instantiateToolsLocators(IConfigurationElement[] configElements) throws CoreException {
		ArrayList<IToolsLocator> list = new ArrayList<IToolsLocator>();

		if (configElements.length > 0) {
			// only use the first one, ignore the others.
			IConfigurationElement configElement = configElements[0];

			// instantiate the class
			Object obj = configElement.createExecutableExtension("class"); //$NON-NLS-1$
			if (obj instanceof IToolsLocator) {
				list.add((IToolsLocator) obj);
			}
		}

		return list.toArray(new IToolsLocator[list.size()]);
	}

	/**
	 * Finds if any other plug-in is extending the exposed Extension Point
	 * called debuggerConnector.
	 *
	 * @return an array of all locators found, or an empty array if none were
	 *         found.
	 */
	private IDebuggerConnector[] instantiateDebuggerConnectors(IConfigurationElement[] configElements)
			throws CoreException {
		ArrayList<IDebuggerConnector> list = new ArrayList<IDebuggerConnector>();

		if (configElements.length > 0) {
			// only use the first one, ignore the others.
			IConfigurationElement configElement = configElements[0];

			// instantiate the class
			Object obj = configElement.createExecutableExtension("class"); //$NON-NLS-1$
			if (obj instanceof IDebuggerConnector) {
				list.add((IDebuggerConnector) obj);
			}
		}

		return list.toArray(new IDebuggerConnector[list.size()]);
	}

	/**
	 * Finds if any other plug-in is extending the exposed Extension Point
	 * called traceviewLauncher.
	 *
	 * @return an array of all locators found, or an empty array if none were
	 *         found.
	 */
	private ITraceviewLauncher[] instantiateTraceviewLauncher(IConfigurationElement[] configElements)
			throws CoreException {
		ArrayList<ITraceviewLauncher> list = new ArrayList<ITraceviewLauncher>();

		if (configElements.length > 0) {
			// only use the first one, ignore the others.
			IConfigurationElement configElement = configElements[0];

			// instantiate the class
			Object obj = configElement.createExecutableExtension("class"); //$NON-NLS-1$
			if (obj instanceof ITraceviewLauncher) {
				list.add((ITraceviewLauncher) obj);
			}
		}

		return list.toArray(new ITraceviewLauncher[list.size()]);
	}

	/**
	 * Returns the classes that implement {@link IClientAction} in each of the
	 * extensions that extend clientAction extension point.
	 * 
	 * @throws CoreException
	 */
	private List<IClientAction> instantiateClientSpecificActions(IConfigurationElement[] elements) throws CoreException {
		if (elements == null || elements.length == 0) {
			return Collections.emptyList();
		}

		List<IClientAction> extensions = new ArrayList<IClientAction>(1);

		for (IConfigurationElement e : elements) {
			Object o = e.createExecutableExtension("class"); //$NON-NLS-1$
			if (o instanceof IClientAction) {
				extensions.add((IClientAction) o);
			}
		}

		return extensions;
	}

	public static Display getDisplay() {
		IWorkbench bench = sPlugin.getWorkbench();
		if (bench != null) {
			return bench.getDisplay();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		AndroidDebugBridge.removeDeviceChangeListener(this);

		AndroidDebugBridge.terminate();

		mRed.dispose();

		sPlugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static DdmsPlugin getDefault() {
		return sPlugin;
	}

	public static String getAdb() {
		return sAdbLocation;
	}

	public static File getPlatformToolsFolder() {
		return new File(sAdbLocation).getParentFile();
	}

	public static String getToolsFolder() {
		return sToolsFolder;
	}

	public static String getHprofConverter() {
		return sHprofConverter;
	}

	/**
	 * Stores the adb location. This returns true if the location is an existing
	 * file.
	 */
	private static boolean setToolsLocation(String adbLocation, String hprofConvLocation, String traceViewLocation) {

		File adb = new File(adbLocation);
		File hprofConverter = new File(hprofConvLocation);
		File traceview = new File(traceViewLocation);

		String missing = "";
		if (adb.isFile() == false) {
			missing += adb.getAbsolutePath() + " ";
		}
		if (hprofConverter.isFile() == false) {
			missing += hprofConverter.getAbsolutePath() + " ";
		}
		if (traceview.isFile() == false) {
			missing += traceview.getAbsolutePath() + " ";
		}

		if (missing.length() > 0) {
			String msg = String.format("DDMS files not found: %1$s", missing);
			Log.e("DDMS", msg);
			Status status = new Status(IStatus.ERROR, PLUGIN_ID, msg, null /* exception */);
			getDefault().getLog().log(status);
			return false;
		}

		sAdbLocation = adbLocation;
		sHprofConverter = hprofConverter.getAbsolutePath();
		DdmUiPreferences.setTraceviewLocation(traceview.getAbsolutePath());

		sToolsFolder = traceview.getParent();

		return true;
	}

	/**
	 * Set the location of the adb executable and optionally starts adb
	 * 
	 * @param adb
	 *            location of adb
	 * @param startAdb
	 *            flag to start adb
	 */
	public static void setToolsLocation(String adbLocation, boolean startAdb, String hprofConvLocation,
			String traceViewLocation) {

		if (setToolsLocation(adbLocation, hprofConvLocation, traceViewLocation)) {
			// starts the server in a thread in case this is blocking.
			if (startAdb) {
				new Thread() {
					@Override
					public void run() {
						// create and start the bridge
						try {
							AndroidDebugBridge.createBridge(sAdbLocation, false /* forceNewBridge */);
						} catch (Throwable t) {
							Status status = new Status(IStatus.ERROR, PLUGIN_ID, "Failed to create AndroidDebugBridge",
									t);
							getDefault().getLog().log(status);
						}
					}
				}.start();
			}
		}
	}

	/**
	 * Returns whether there are implementations of the debuggerConnectors
	 * extension point.
	 * <p/>
	 * This is guaranteed to return the correct value as soon as the plug-in is
	 * loaded.
	 */
	public boolean hasDebuggerConnectors() {
		return mHasDebuggerConnectors;
	}

	/**
	 * Returns the implementations of {@link IDebuggerConnector}.
	 * <p/>
	 * There may be a small amount of time right after the plug-in load where
	 * this can return null even if there are implementation.
	 * <p/>
	 * Since the use of the implementation likely require user input, the UI can
	 * use {@link #hasDebuggerConnectors()} to know if there are implementations
	 * before they are loaded.
	 */
	public IDebuggerConnector[] getDebuggerConnectors() {
		return mDebuggerConnectors;
	}

	public synchronized void addSelectionListener(ISelectionListener listener) {
		mListeners.add(listener);

		// notify the new listener of the current selection
		listener.selectionChanged(mCurrentDevice);
		listener.selectionChanged(mCurrentClient);
	}

	public synchronized void removeSelectionListener(ISelectionListener listener) {
		mListeners.remove(listener);
	}

	public synchronized void setListeningState(boolean state) {
		mListeningToUiSelection = state;
	}

	/**
	 * Sent when the a device is connected to the {@link AndroidDebugBridge}.
	 * <p/>
	 * This is sent from a non UI thread.
	 * 
	 * @param device
	 *            the new device.
	 *
	 * @see IDeviceChangeListener#deviceConnected(IDevice)
	 */
	@Override
	public void deviceConnected(IDevice device) {
		// if we are listening to selection coming from the ui, then we do
		// nothing, as
		// any change in the devices/clients, will be handled by the UI, and
		// we'll receive
		// selection notification through our implementation of
		// IUiSelectionListener.
		if (mListeningToUiSelection == false) {
			if (mCurrentDevice == null) {
				handleDefaultSelection(device);
			}
		}
	}

	/**
	 * Sent when the a device is disconnected to the {@link AndroidDebugBridge}.
	 * <p/>
	 * This is sent from a non UI thread.
	 * 
	 * @param device
	 *            the new device.
	 *
	 * @see IDeviceChangeListener#deviceDisconnected(IDevice)
	 */
	@Override
	public void deviceDisconnected(IDevice device) {
		// if we are listening to selection coming from the ui, then we do
		// nothing, as
		// any change in the devices/clients, will be handled by the UI, and
		// we'll receive
		// selection notification through our implementation of
		// IUiSelectionListener.
		if (mListeningToUiSelection == false) {
			// test if the disconnected device was the default selection.
			if (mCurrentDevice == device) {
				// try to find a new device
				AndroidDebugBridge bridge = AndroidDebugBridge.getBridge();
				if (bridge != null) {
					// get the device list
					IDevice[] devices = bridge.getDevices();

					// check if we still have devices
					if (devices.length == 0) {
						handleDefaultSelection((IDevice) null);
					} else {
						handleDefaultSelection(devices[0]);
					}
				} else {
					handleDefaultSelection((IDevice) null);
				}
			}
		}
	}

	/**
	 * Sent when a device data changed, or when clients are started/terminated
	 * on the device.
	 * <p/>
	 * This is sent from a non UI thread.
	 * 
	 * @param device
	 *            the device that was updated.
	 * @param changeMask
	 *            the mask indicating what changed.
	 *
	 * @see IDeviceChangeListener#deviceChanged(IDevice)
	 */
	@Override
	public void deviceChanged(IDevice device, int changeMask) {
		// if we are listening to selection coming from the ui, then we do
		// nothing, as
		// any change in the devices/clients, will be handled by the UI, and
		// we'll receive
		// selection notification through our implementation of
		// IUiSelectionListener.
		if (mListeningToUiSelection == false) {

			// check if this is our device
			if (device == mCurrentDevice) {
				if (mCurrentClient == null) {
					handleDefaultSelection(device);
				} else {
					// get the clients and make sure ours is still in there.
					Client[] clients = device.getClients();
					boolean foundClient = false;
					for (Client client : clients) {
						if (client == mCurrentClient) {
							foundClient = true;
							break;
						}
					}

					// if we haven't found our client, lets look for a new one
					if (foundClient == false) {
						mCurrentClient = null;
						handleDefaultSelection(device);
					}
				}
			}
		}
	}

	/**
	 * Sent when a new {@link IDevice} and {@link Client} are selected.
	 * 
	 * @param selectedDevice
	 *            the selected device. If null, no devices are selected.
	 * @param selectedClient
	 *            The selected client. If null, no clients are selected.
	 */
	@Override
	public synchronized void selectionChanged(IDevice selectedDevice, Client selectedClient) {
		if (mCurrentDevice != selectedDevice) {
			mCurrentDevice = selectedDevice;

			// notify of the new default device
			for (ISelectionListener listener : mListeners) {
				listener.selectionChanged(mCurrentDevice);
			}
		}

		if (mCurrentClient != selectedClient) {
			mCurrentClient = selectedClient;

			// notify of the new default client
			for (ISelectionListener listener : mListeners) {
				listener.selectionChanged(mCurrentClient);
			}
		}
	}

	/**
	 * Handles a default selection of a {@link IDevice} and {@link Client}.
	 * 
	 * @param device
	 *            the selected device
	 */
	private void handleDefaultSelection(final IDevice device) {
		// because the listener expect to receive this from the UI thread, and
		// this is called
		// from the AndroidDebugBridge notifications, we need to run this in the
		// UI thread.
		try {
			Display display = getDisplay();

			display.asyncExec(new Runnable() {
				@Override
				public void run() {
					// set the new device if different.
					boolean newDevice = false;
					if (mCurrentDevice != device) {
						mCurrentDevice = device;
						newDevice = true;

						// notify of the new default device
						for (ISelectionListener listener : mListeners) {
							listener.selectionChanged(mCurrentDevice);
						}
					}

					if (device != null) {
						// if this is a device switch or the same device but we
						// didn't find a valid
						// client the last time, we go look for a client to use
						// again.
						if (newDevice || mCurrentClient == null) {
							// now get the new client
							Client[] clients = device.getClients();
							if (clients.length > 0) {
								handleDefaultSelection(clients[0]);
							} else {
								handleDefaultSelection((Client) null);
							}
						}
					} else {
						handleDefaultSelection((Client) null);
					}
				}
			});
		} catch (SWTException e) {
			// display is disposed. Do nothing since we're quitting anyway.
		}
	}

	private void handleDefaultSelection(Client client) {
		mCurrentClient = client;

		// notify of the new default client
		for (ISelectionListener listener : mListeners) {
			listener.selectionChanged(mCurrentClient);
		}
	}

	/**
	 * Prints a message, associated with a project to the specified stream
	 * 
	 * @param stream
	 *            The stream to write to
	 * @param tag
	 *            The tag associated to the message. Can be null
	 * @param message
	 *            The message to print.
	 */
	private static synchronized void printToStream(MessageConsoleStream stream, String tag, String message) {
		String dateTag = getMessageTag(tag);

		stream.print(dateTag);
		if (!dateTag.endsWith(" ")) {
			stream.print(" "); //$NON-NLS-1$
		}
		stream.println(message);
	}

	/**
	 * Creates a string containing the current date/time, and the tag
	 * 
	 * @param tag
	 *            The tag associated to the message. Can be null
	 * @return The dateTag
	 */
	private static String getMessageTag(String tag) {
		Calendar c = Calendar.getInstance();

		if (tag == null) {
			return String.format(Messages.DdmsPlugin_Message_Tag_Mask_1, c);
		}

		return String.format(Messages.DdmsPlugin_Message_Tag_Mask_2, c, tag);
	}

	/**
	 * Implementation of com.android.ddmuilib.StackTracePanel.ISourceRevealer.
	 */
	@Override
	public void reveal(String applicationName, String className, int line) {
		JavaSourceRevealer.reveal(applicationName, className, line);
	}

	public boolean launchTraceview(String osPath) {
		if (mTraceviewLaunchers != null) {
			for (ITraceviewLauncher launcher : mTraceviewLaunchers) {
				try {
					if (launcher.openFile(osPath)) {
						return true;
					}
				} catch (Throwable t) {
					// ignore, we'll just not use this implementation.
				}
			}
		}

		return false;
	}

	/**
	 * Returns the list of clients that extend the clientAction extension point.
	 */
	@NonNull
	public synchronized List<IClientAction> getClientSpecificActions() {
		if (mClientSpecificActions == null) {
			// get available client specific action extensions
			IConfigurationElement[] elements = findConfigElements("org.eclipse.andmore.ddms.clientAction"); //$NON-NLS-1$
			try {
				mClientSpecificActions = instantiateClientSpecificActions(elements);
			} catch (CoreException e) {
				mClientSpecificActions = Collections.emptyList();
			}
		}

		return mClientSpecificActions;
	}

	private LogCatMonitor mLogCatMonitor;

	public void startLogCatMonitor(IDevice device) {
		if (mLogCatMonitor == null) {
			mLogCatMonitor = new LogCatMonitor(getDebuggerConnectors(), getPreferenceStore());
		}

		mLogCatMonitor.monitorDevice(device);
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
