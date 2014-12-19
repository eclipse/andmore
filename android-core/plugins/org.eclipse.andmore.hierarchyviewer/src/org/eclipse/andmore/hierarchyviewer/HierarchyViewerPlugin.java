/*
 * Copyright (C) 2010 The Android Open Source Project
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

package org.eclipse.andmore.hierarchyviewer;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.AndroidDebugBridge.IDebugBridgeChangeListener;
import com.android.ddmlib.Log;
import com.android.ddmlib.Log.ILogOutput;
import com.android.ddmlib.Log.LogLevel;
import com.android.hierarchyviewerlib.HierarchyViewerDirector;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import java.util.Calendar;

/**
 * The activator class controls the plug-in life cycle
 */
public class HierarchyViewerPlugin extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "org.eclipse.andmore.hierarchyviewer"; //$NON-NLS-1$

	public static final String ADB_LOCATION = PLUGIN_ID + ".adb"; //$NON-NLS-1$

	// The shared instance
	private static HierarchyViewerPlugin sPlugin;

	private Color mRedColor;

	/**
	 * The constructor
	 */
	public HierarchyViewerPlugin() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		sPlugin = this;

		// set the consoles.
		final MessageConsole messageConsole = new MessageConsole("Hierarchy Viewer", null); //$NON-NLS-1$
		ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { messageConsole });

		final MessageConsoleStream consoleStream = messageConsole.newMessageStream();
		final MessageConsoleStream errorConsoleStream = messageConsole.newMessageStream();
		mRedColor = new Color(Display.getDefault(), 0xFF, 0x00, 0x00);

		// because this can be run, in some cases, by a non UI thread, and
		// because
		// changing the console properties update the UI, we need to make this
		// change
		// in the UI thread.
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				errorConsoleStream.setColor(mRedColor);
			}
		});

		// set up the ddms log to use the ddms console.
		Log.setLogOutput(new ILogOutput() {
			@Override
			public void printLog(LogLevel logLevel, String tag, String message) {
				if (logLevel.getPriority() >= LogLevel.ERROR.getPriority()) {
					printToStream(errorConsoleStream, tag, message);
					ConsolePlugin.getDefault().getConsoleManager().showConsoleView(messageConsole);
				} else {
					printToStream(consoleStream, tag, message);
				}
			}

			@Override
			public void printAndPromptLog(final LogLevel logLevel, final String tag, final String message) {
				printLog(logLevel, tag, message);
				// dialog box only run in UI thread..
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						Shell shell = Display.getDefault().getActiveShell();
						if (logLevel == LogLevel.ERROR) {
							MessageDialog.openError(shell, tag, message);
						} else {
							MessageDialog.openWarning(shell, tag, message);
						}
					}
				});
			}

		});

		final HierarchyViewerDirector director = HierarchyViewerPluginDirector.createDirector();
		director.startListenForDevices();

		// make the director receive change in ADB.
		AndroidDebugBridge.addDebugBridgeChangeListener(new IDebugBridgeChangeListener() {
			@Override
			public void bridgeChanged(AndroidDebugBridge bridge) {
				director.acquireBridge(bridge);
			}
		});

		// get the current ADB if any
		director.acquireBridge(AndroidDebugBridge.getBridge());

		// populate the UI with current devices (if any) in a thread
		new Thread() {
			@Override
			public void run() {
				director.populateDeviceSelectionModel();
			}
		}.start();
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
		sPlugin = null;
		super.stop(context);

		mRedColor.dispose();

		HierarchyViewerDirector director = HierarchyViewerDirector.getDirector();
		director.stopListenForDevices();
		director.stopDebugBridge();
		director.terminate();
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static HierarchyViewerPlugin getPlugin() {
		return sPlugin;
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
			return String.format("[%1$tF %1$tT]", c); //$NON-NLS-1$
		}

		return String.format("[%1$tF %1$tT - %2$s]", c, tag); //$NON-NLS-1$
	}
}
