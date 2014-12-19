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

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.Client;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.IDevice.DeviceUnixSocketNamespace;
import com.android.ddmlib.IShellOutputReceiver;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;
import com.google.common.io.Closeables;
import com.google.common.util.concurrent.SimpleTimeLimiter;

import org.eclipse.andmore.gltrace.editors.GLFunctionTraceViewer;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.ide.IDE;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CollectTraceAction implements IWorkbenchWindowActionDelegate {
	/** Abstract Unix Domain Socket Name used by the gltrace device code. */
	private static final String GLTRACE_UDS = "gltrace"; //$NON-NLS-1$

	/**
	 * Local port that is forwarded to the device's {@link #GLTRACE_UDS} socket.
	 */
	private static final int LOCAL_FORWARDED_PORT = 6039;

	/**
	 * Activity name to use for a system activity that has already been
	 * launched.
	 */
	private static final String SYSTEM_APP = "system"; //$NON-NLS-1$

	/** Time to wait for the application to launch (seconds) */
	private static final int LAUNCH_TIMEOUT = 15;

	/** Time to wait for the application to die (seconds) */
	private static final int KILL_TIMEOUT = 5;

	private static final int MIN_API_LEVEL = 16;

	@Override
	public void run(IAction action) {
		connectToDevice();
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public void init(IWorkbenchWindow window) {
	}

	private void connectToDevice() {
		Shell shell = Display.getDefault().getActiveShell();
		GLTraceOptionsDialog dlg = new GLTraceOptionsDialog(shell);
		if (dlg.open() != Window.OK) {
			return;
		}

		TraceOptions traceOptions = dlg.getTraceOptions();

		IDevice device = getDevice(traceOptions.device);
		String apiLevelString = device.getProperty(IDevice.PROP_BUILD_API_LEVEL);
		int apiLevel;
		try {
			apiLevel = Integer.parseInt(apiLevelString);
		} catch (NumberFormatException e) {
			apiLevel = MIN_API_LEVEL;
		}
		if (apiLevel < MIN_API_LEVEL) {
			MessageDialog.openError(shell, "GL Trace", String.format(
					"OpenGL Tracing is only supported on devices at API Level %1$d."
							+ "The selected device '%2$s' provides API level %3$s.", MIN_API_LEVEL,
					traceOptions.device, apiLevelString));
			return;
		}

		try {
			setupForwarding(device, LOCAL_FORWARDED_PORT);
		} catch (Exception e) {
			MessageDialog.openError(shell, "Setup GL Trace",
					"Error while setting up port forwarding: " + e.getMessage());
			return;
		}

		try {
			if (!SYSTEM_APP.equals(traceOptions.appToTrace)) {
				startActivity(device, traceOptions.appToTrace, traceOptions.activityToTrace,
						traceOptions.isActivityNameFullyQualified);
			}
		} catch (Exception e) {
			MessageDialog.openError(shell, "Setup GL Trace", "Error while launching application: " + e.getMessage());
			return;
		}

		// if everything went well, the app should now be waiting for the gl
		// debugger
		// to connect
		startTracing(shell, traceOptions, LOCAL_FORWARDED_PORT);

		// once tracing is complete, remove port forwarding
		disablePortForwarding(device, LOCAL_FORWARDED_PORT);

		// and finally open the editor to view the file
		openInEditor(shell, traceOptions.traceDestination);
	}

	public static void openInEditor(Shell shell, String traceFilePath) {
		final IFileStore fileStore = EFS.getLocalFileSystem().getStore(new Path(traceFilePath));
		if (!fileStore.fetchInfo().exists()) {
			return;
		}

		final IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		if (window == null) {
			return;
		}

		IWorkbenchPage page = window.getActivePage();
		if (page == null) {
			return;
		}

		try {
			workbench.showPerspective("org.eclipse.andmore.gltrace.perspective", window);
		} catch (WorkbenchException e) {
		}

		// if there is a editor already open, then refresh its model
		GLFunctionTraceViewer viewer = getOpenTraceViewer(page, traceFilePath);
		if (viewer != null) {
			viewer.setInput(shell, traceFilePath);
		}

		// open the editor (if not open), or bring it to foreground if it is
		// already open
		try {
			IDE.openEditorOnFileStore(page, fileStore);
		} catch (PartInitException e) {
			GlTracePlugin.getDefault().logMessage("Unexpected error while opening gltrace file in editor: " + e);
			return;
		}
	}

	/**
	 * Returns the editor part that has the provided file path open.
	 * 
	 * @param page
	 *            page containing editors
	 * @param traceFilePath
	 *            file that should be open in an editor
	 * @return if given trace file is already open, then a reference to that
	 *         editor part, null otherwise
	 */
	private static GLFunctionTraceViewer getOpenTraceViewer(IWorkbenchPage page, String traceFilePath) {
		IEditorReference[] editorRefs = page.getEditorReferences();
		for (IEditorReference ref : editorRefs) {
			String id = ref.getId();
			if (!GLFunctionTraceViewer.ID.equals(id)) {
				continue;
			}

			IEditorInput input = null;
			try {
				input = ref.getEditorInput();
			} catch (PartInitException e) {
				continue;
			}

			if (!(input instanceof IURIEditorInput)) {
				continue;
			}

			if (traceFilePath.equals(((IURIEditorInput) input).getURI().getPath())) {
				return (GLFunctionTraceViewer) ref.getEditor(true);
			}
		}

		return null;
	}

	@SuppressWarnings("resource")
	// Closeables.closeQuietly
	public static void startTracing(Shell shell, TraceOptions traceOptions, int port) {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(traceOptions.traceDestination, false);
		} catch (FileNotFoundException e) {
			// input path is valid, so this cannot occur
		}

		Socket socket = new Socket();
		DataInputStream traceDataStream = null;
		DataOutputStream traceCommandsStream = null;
		try {
			socket.connect(new java.net.InetSocketAddress("127.0.0.1", port)); //$NON-NLS-1$
			socket.setTcpNoDelay(true);
			traceDataStream = new DataInputStream(socket.getInputStream());
			traceCommandsStream = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			MessageDialog.openError(shell, "OpenGL Trace",
					"Unable to connect to remote GL Trace Server: " + e.getMessage());
			Closeables.closeQuietly(fos);
			return;
		}

		// create channel to send trace commands to device
		TraceCommandWriter traceCommandWriter = new TraceCommandWriter(traceCommandsStream);
		try {
			traceCommandWriter.setTraceOptions(traceOptions.collectFbOnEglSwap, traceOptions.collectFbOnGlDraw,
					traceOptions.collectTextureData);
		} catch (IOException e) {
			MessageDialog.openError(shell, "OpenGL Trace",
					"Unexpected error while setting trace options: " + e.getMessage());
			closeSocket(socket);
			Closeables.closeQuietly(fos);
			return;
		}

		// create trace writer that writes to a trace file
		TraceFileWriter traceFileWriter = new TraceFileWriter(fos, traceDataStream);
		traceFileWriter.start();

		GLTraceCollectorDialog dlg = new GLTraceCollectorDialog(shell, traceFileWriter, traceCommandWriter,
				traceOptions);
		dlg.open();

		traceFileWriter.stopTracing();
		traceCommandWriter.close();
		closeSocket(socket);
	}

	private static void closeSocket(Socket socket) {
		try {
			socket.close();
		} catch (IOException e) {
			// ignore error while closing socket
		}
	}

	private void startActivity(IDevice device, String appPackage, String activity, boolean isActivityNameFullyQualified)
			throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException,
			InterruptedException {
		killApp(device, appPackage); // kill app if it is already running
		waitUntilAppKilled(device, appPackage, KILL_TIMEOUT);

		StringBuilder activityPath = new StringBuilder(appPackage);
		if (!activity.isEmpty()) {
			activityPath.append('/');
			if (!isActivityNameFullyQualified) {
				activityPath.append('.');
			}
			activityPath.append(activity);
		}
		String startAppCmd = String.format(
				"am start --opengl-trace %s -a android.intent.action.MAIN -c android.intent.category.LAUNCHER", //$NON-NLS-1$
				activityPath.toString());

		Semaphore launchCompletionSempahore = new Semaphore(0);
		StartActivityOutputReceiver receiver = new StartActivityOutputReceiver(launchCompletionSempahore);
		device.executeShellCommand(startAppCmd, receiver);

		// wait until shell finishes launch command
		launchCompletionSempahore.acquire();

		// throw exception if there was an error during launch
		String output = receiver.getOutput();
		if (output.contains("Error")) { //$NON-NLS-1$
			throw new RuntimeException(output);
		}

		// wait until the app itself has been launched
		waitUntilAppLaunched(device, appPackage, LAUNCH_TIMEOUT);
	}

	private void killApp(IDevice device, String appName) {
		Client client = device.getClient(appName);
		if (client != null) {
			client.kill();
		}
	}

	private void waitUntilAppLaunched(final IDevice device, final String appName, int timeout) {
		Callable<Boolean> c = new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				Client client;
				do {
					client = device.getClient(appName);
				} while (client == null);

				return Boolean.TRUE;
			}
		};
		try {
			new SimpleTimeLimiter().callWithTimeout(c, timeout, TimeUnit.SECONDS, true);
		} catch (Exception e) {
			throw new RuntimeException("Timed out waiting for application to launch.");
		}

		// once the app has launched, wait an additional couple of seconds
		// for it to start up
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// ignore
		}
	}

	private void waitUntilAppKilled(final IDevice device, final String appName, int timeout) {
		Callable<Boolean> c = new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				Client client;
				while ((client = device.getClient(appName)) != null) {
					client.kill();
				}
				return Boolean.TRUE;
			}
		};
		try {
			new SimpleTimeLimiter().callWithTimeout(c, timeout, TimeUnit.SECONDS, true);
		} catch (Exception e) {
			throw new RuntimeException("Timed out waiting for running application to die.");
		}
	}

	public static void setupForwarding(IDevice device, int i) throws TimeoutException, AdbCommandRejectedException,
			IOException {
		device.createForward(i, GLTRACE_UDS, DeviceUnixSocketNamespace.ABSTRACT);
	}

	public static void disablePortForwarding(IDevice device, int port) {
		try {
			device.removeForward(port, GLTRACE_UDS, DeviceUnixSocketNamespace.ABSTRACT);
		} catch (Exception e) {
			// ignore exceptions;
		}
	}

	private IDevice getDevice(String deviceName) {
		IDevice[] devices = AndroidDebugBridge.getBridge().getDevices();

		for (IDevice device : devices) {
			if (device.getName().equals(deviceName)) {
				return device;
			}
		}

		return null;
	}

	private static class StartActivityOutputReceiver implements IShellOutputReceiver {
		private Semaphore mSemaphore;
		private StringBuffer sb = new StringBuffer(300);

		public StartActivityOutputReceiver(Semaphore s) {
			mSemaphore = s;
		}

		@Override
		public void addOutput(byte[] data, int offset, int length) {
			String d = new String(data, offset, length);
			sb.append(d);
		}

		@Override
		public void flush() {
			mSemaphore.release();
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		public String getOutput() {
			return sb.toString();
		}
	}
}
