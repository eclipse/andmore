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
package org.eclipse.andmore.android.nativeos.linux.gtk;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;

import org.eclipse.andmore.android.AndroidPlugin;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.graphics.Point;
import org.osgi.framework.Bundle;

public class GtkBridge {
	private static final String EXEC = getExecCommad();

	private static final String DISPLAY = getDisplayNum();

	public static String getDisplayNum() {
		String display = System.getenv("DISPLAY");
		return display;
	}

	public static String getExecCommad() {
		String absoluteFile = "";
		try {
			Bundle bundle = AndroidPlugin.getDefault().getBundle();
			URL locationUrl = FileLocator.find(bundle, new Path("gtk_bridge_app"), null);
			URL fileUrl = FileLocator.toFileURL(locationUrl);
			File file = new File(fileUrl.getFile());
			absoluteFile = file.getAbsolutePath();
			StudioLogger.info(GtkBridge.class, "Using gtk bridge app at: " + absoluteFile);
			checkExecutionPermission(absoluteFile);
		} catch (Exception e) {
			StudioLogger.error(GtkBridge.class, "Failed to retrieve gtk bridge app", e);
		}
		return absoluteFile;
	}

	private static void checkExecutionPermission(String executable) throws IOException {
		String[] command = new String[] { "chmod", "+x", executable };
		Runtime.getRuntime().exec(command);
	}

	public static synchronized String exec(String[] command) {
		String lastline = "";
		Process p = null;
		BufferedReader reader = null;
		try {
			p = Runtime.getRuntime().exec(command);
			reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String returnedValue = "";
			do {
				returnedValue = reader.readLine();
				if (returnedValue != null) {
					lastline = returnedValue;
				}
			} while (returnedValue != null);
			int exitCode = p.waitFor();
			if (exitCode != 0) {
				StudioLogger.debug(GtkBridge.class, "Command " + Arrays.toString(command)
						+ " finished with error code:" + exitCode);
			}
		} catch (Exception e) {
			StudioLogger.error(GtkBridge.class, "Failed to execute command :" + Arrays.toString(command), e);
		} finally {
			if (p != null) {
				try {
					p.getInputStream().close();
				} catch (IOException e) {
					// Do Nothing.
				}
				try {
					p.getOutputStream().close();
				} catch (IOException e) {
					// Do Nothing.
				}
				try {
					p.getErrorStream().close();
				} catch (IOException e) {
					// Do Nothing.
				}
			}
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					// Do Nothing.
				}
			}
		}

		return lastline;
	}

	private static long exec_toLong(String[] command) {
		long response = 0;
		try {
			String returnedValue = exec(command);
			if (!"".equals(command)) {
				response = Long.parseLong(returnedValue);
			}
		} catch (Exception e) {
			StudioLogger.error(GtkBridge.class, "Failed to execute command", e);
		}
		return response;
	}

	public static long getWindowHandle(String windowName) {
		StudioLogger.debug(GtkBridge.class, "getWindowHandle " + windowName);

		String[] command = { EXEC, "getWindowHandler", DISPLAY, windowName };
		long handle = exec_toLong(command);
		StudioLogger.debug(GtkBridge.class, "getWindowHandle handle=" + handle);
		return handle;
	}

	public static long embedNativeWindow(long parentHandle, long windowHandle) {
		StudioLogger.debug(GtkBridge.class, "embedNativeWindow parent=" + parentHandle + " window=" + windowHandle);
		String[] arguments = { EXEC, "embedWindow", DISPLAY, Long.toString(parentHandle), Long.toString(windowHandle) };
		long handle = exec_toLong(arguments);
		return handle;
	}

	public static void unembedNativeWindow(long parentHandle, long formerParentHandle) {
		StudioLogger.debug(GtkBridge.class, "unembedNativeWindow " + parentHandle);
		String[] arguments = { EXEC, "unembedWindow", DISPLAY, Long.toString(parentHandle),
				Long.toString(formerParentHandle) };
		exec(arguments);
	}

	public static Point getWindowSize(long handle) {
		String[] arguments = { EXEC, "getWindowSize", DISPLAY, Long.toString(handle) };
		String size = exec(arguments);
		Point point;
		try {
			String[] pointStr = size.split(",");
			int x = Integer.parseInt(pointStr[0]);
			int y = Integer.parseInt(pointStr[1]);
			point = new Point(x, y);
		} catch (Exception e) {
			point = new Point(0, 0);
		}
		return point;
	}

	public static void pressKey(long windowHandle, int key) {
		String[] arguments = { EXEC, "pressKey", DISPLAY, Long.toString(windowHandle), Integer.toString(key) };
		exec(arguments);
	}

	public static void releaseKey(long windowHandle, int key) {
		String[] arguments = { EXEC, "releaseKey", DISPLAY, Long.toString(windowHandle), Integer.toString(key) };
		exec(arguments);
	}

	public static void showWindow(long windowHandle) {
		StudioLogger.debug(GtkBridge.class, "showWindow " + windowHandle);
		String[] arguments = { EXEC, "showWindow", DISPLAY, Long.toString(windowHandle) };
		exec(arguments);
	}

	public static void hideWindow(long windowHandle) {
		StudioLogger.debug(GtkBridge.class, "hideWindow " + windowHandle);
		String[] arguments = { EXEC, "hideWindow", DISPLAY, Long.toString(windowHandle) };
		exec(arguments);
	}

	public static void restoreWindow(long windowHandle) {
		StudioLogger.debug(GtkBridge.class, "restoreWindow " + windowHandle);
		String[] arguments = { EXEC, "restoreWindow", DISPLAY, Long.toString(windowHandle) };
		exec(arguments);
	}

}
