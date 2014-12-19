/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.andmore.ndk.internal;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.ndk.internal.launch.NdkLaunchConstants;
import org.eclipse.cdt.core.CommandLauncher;
import org.eclipse.cdt.core.ICommandLauncher;
import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.dsf.gdb.IGDBLaunchConfigurationConstants;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("restriction")
public class NdkHelper {
	private static final String MAKE = "make"; //$NON-NLS-1$
	private static final String CORE_MAKEFILE_PATH = "/build/core/build-local.mk"; //$NON-NLS-1$

	/**
	 * Obtain the ABI's the application is compatible with. The ABI's are
	 * obtained by reading the result of the following command: make
	 * --no-print-dir -f ${NdkRoot}/build/core/build-local.mk -C <project-root>
	 * DUMP_APP_ABI
	 */
	public static Collection<NativeAbi> getApplicationAbis(IProject project, IProgressMonitor monitor) {
		ICommandLauncher launcher = new CommandLauncher();
		launcher.setProject(project);
		String[] args = new String[] { "--no-print-dir", //$NON-NLS-1$
				"-f", //$NON-NLS-1$
				NdkManager.getNdkLocation() + CORE_MAKEFILE_PATH, "-C", //$NON-NLS-1$
				project.getLocation().toOSString(), "DUMP_APP_ABI", //$NON-NLS-1$
		};
		try {
			launcher.execute(getPathToMake(), args, null, project.getLocation(), monitor);
		} catch (CoreException e) {
			AdtPlugin.printErrorToConsole(e.getLocalizedMessage());
			return Collections.emptyList();
		}

		ByteArrayOutputStream stdout = new ByteArrayOutputStream();
		ByteArrayOutputStream stderr = new ByteArrayOutputStream();
		launcher.waitAndRead(stdout, stderr, monitor);

		String abis = stdout.toString().trim();
		Set<NativeAbi> nativeAbis = EnumSet.noneOf(NativeAbi.class);
		for (String abi : abis.split(" ")) { //$NON-NLS-1$
			if (abi.equals("all")) { //$NON-NLS-1$
				return EnumSet.allOf(NativeAbi.class);
			}

			try {
				nativeAbis.add(NativeAbi.getByString(abi));
			} catch (IllegalArgumentException e) {
				AdtPlugin.printErrorToConsole(project, "Unknown Application ABI: ", abi);
			}
		}

		return nativeAbis;
	}

	/**
	 * Obtain the toolchain prefix to use for given project and abi. The prefix
	 * is obtained by reading the result of: make --no-print-dir -f
	 * ${NdkRoot}/build/core/build-local.mk \ -C <project-root> \
	 * DUMP_TOOLCHAIN_PREFIX APP_ABI=abi
	 */
	public static String getToolchainPrefix(IProject project, NativeAbi abi, IProgressMonitor monitor) {
		ICommandLauncher launcher = new CommandLauncher();
		launcher.setProject(project);
		String[] args = new String[] { "--no-print-dir", //$NON-NLS-1$
				"-f", //$NON-NLS-1$
				NdkManager.getNdkLocation() + CORE_MAKEFILE_PATH, "-C", //$NON-NLS-1$
				project.getLocation().toOSString(), "DUMP_TOOLCHAIN_PREFIX", //$NON-NLS-1$
				"APP_ABI=" + abi.getAbi(), //$NON-NLS-1$
		};
		try {
			launcher.execute(getPathToMake(), args, null, project.getLocation(), monitor);
		} catch (CoreException e) {
			AdtPlugin.printErrorToConsole(e.getLocalizedMessage());
			return null;
		}

		ByteArrayOutputStream stdout = new ByteArrayOutputStream();
		ByteArrayOutputStream stderr = new ByteArrayOutputStream();
		launcher.waitAndRead(stdout, stderr, monitor);
		return stdout.toString().trim();
	}

	private static IPath getPathToMake() {
		return getFullPathTo(MAKE);
	}

	/**
	 * Obtain a path to the utilities prebuilt folder in NDK. This is typically
	 * "${NdkRoot}/prebuilt/<platform>/bin/". If the executable is not found, it
	 * simply returns the name of the executable (which is equal to assuming
	 * that it is available on the path).
	 */
	private static synchronized IPath getFullPathTo(String executable) {
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			executable += ".exe";
		}

		IPath ndkRoot = new Path(NdkManager.getNdkLocation());
		IPath prebuilt = ndkRoot.append("prebuilt"); //$NON-NLS-1$
		if (!prebuilt.toFile().exists() || !prebuilt.toFile().canRead()) {
			return new Path(executable);
		}

		File[] platforms = prebuilt.toFile().listFiles();
		if (platforms != null) {
			for (File p : platforms) {
				IPath exePath = prebuilt.append(p.getName()).append("bin") //$NON-NLS-1$
						.append(executable);
				if (exePath.toFile().exists()) {
					return exePath;
				}
			}
		}

		return new Path(executable);
	}

	public static void setLaunchConfigDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IGDBLaunchConfigurationConstants.ATTR_REMOTE_TCP, true);
		config.setAttribute(NdkLaunchConstants.ATTR_NDK_GDB, NdkLaunchConstants.DEFAULT_GDB);
		config.setAttribute(IGDBLaunchConfigurationConstants.ATTR_GDB_INIT, NdkLaunchConstants.DEFAULT_GDBINIT);
		config.setAttribute(IGDBLaunchConfigurationConstants.ATTR_PORT, NdkLaunchConstants.DEFAULT_GDB_PORT);
		config.setAttribute(IGDBLaunchConfigurationConstants.ATTR_HOST, "localhost"); //$NON-NLS-1$
		config.setAttribute(ICDTLaunchConfigurationConstants.ATTR_DEBUGGER_STOP_AT_MAIN, false);
		config.setAttribute(ICDTLaunchConfigurationConstants.ATTR_DEBUGGER_START_MODE,
				IGDBLaunchConfigurationConstants.DEBUGGER_MODE_REMOTE_ATTACH);
		config.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_NAME, NdkLaunchConstants.DEFAULT_PROGRAM);

		config.setAttribute(ICDTLaunchConfigurationConstants.ATTR_DEBUGGER_START_MODE,
				IGDBLaunchConfigurationConstants.DEBUGGER_MODE_REMOTE);
		config.setAttribute(ICDTLaunchConfigurationConstants.ATTR_DEBUGGER_ID, "gdbserver"); //$NON-NLS-1$

		List<String> solibPaths = new ArrayList<String>(2);
		solibPaths.add(NdkLaunchConstants.DEFAULT_SOLIB_PATH);
		config.setAttribute(NdkLaunchConstants.ATTR_NDK_SOLIB, solibPaths);
	}
}
