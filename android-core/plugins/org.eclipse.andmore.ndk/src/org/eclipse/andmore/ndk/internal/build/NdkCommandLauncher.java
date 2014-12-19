/*
 * Copyright (C) 2011 The Android Open Source Project
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

package org.eclipse.andmore.ndk.internal.build;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.ndk.internal.NdkManager;
import org.eclipse.cdt.core.CommandLauncher;
import org.eclipse.cdt.utils.CygPath;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("restriction")
// for AdtPlugin internal classes
public class NdkCommandLauncher extends CommandLauncher {
	private static CygPath sCygPath = null;

	private static final List<String> WINDOWS_NATIVE_EXECUTABLES = Arrays.asList("exe", //$NON-NLS-1$
			"cmd", //$NON-NLS-1$
			"bat" //$NON-NLS-1$
	);

	static {
		if (Platform.OS_WIN32.equals(Platform.getOS())) {
			try {
				sCygPath = new CygPath();
			} catch (IOException e) {
				AdtPlugin.printErrorToConsole("Unable to launch cygpath. Is Cygwin on the path?", e);
			}
		}
	}

	@Override
	public Process execute(IPath commandPath, String[] args, String[] env, IPath changeToDirectory,
			IProgressMonitor monitor) throws CoreException {
		if (!commandPath.isAbsolute())
			commandPath = new Path(NdkManager.getNdkLocation()).append(commandPath);

		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			// convert cygwin paths to standard paths
			if (sCygPath != null && commandPath.toString().startsWith("/cygdrive")) { //$NON-NLS-1$
				try {
					String path = sCygPath.getFileName(commandPath.toString());
					commandPath = new Path(path);
				} catch (IOException e) {
					AdtPlugin.printErrorToConsole("Unexpected error while transforming cygwin path.", e);
				}
			}

			if (isWindowsExecutable(commandPath)) {
				// append necessary extension
				commandPath = appendExecutableExtension(commandPath);
			} else {
				// Invoke using Cygwin shell if this is not a native windows
				// executable
				String[] newargs = new String[args.length + 1];
				newargs[0] = commandPath.toOSString();
				System.arraycopy(args, 0, newargs, 1, args.length);

				commandPath = new Path("sh"); //$NON-NLS-1$
				args = newargs;
			}
		}

		return super.execute(commandPath, args, env, changeToDirectory, monitor);
	}

	private boolean isWindowsExecutable(IPath commandPath) {
		String ext = commandPath.getFileExtension();
		if (isWindowsExecutableExtension(ext)) {
			return true;
		}

		ext = findWindowsExecutableExtension(commandPath);
		if (ext != null) {
			return true;
		}

		return false;
	}

	private IPath appendExecutableExtension(IPath commandPath) {
		if (isWindowsExecutableExtension(commandPath.getFileExtension())) {
			return commandPath;
		}

		String ext = findWindowsExecutableExtension(commandPath);
		if (ext != null) {
			return commandPath.addFileExtension(ext);
		}

		return commandPath;
	}

	private String findWindowsExecutableExtension(IPath command) {
		for (String e : WINDOWS_NATIVE_EXECUTABLES) {
			File exeFile = command.addFileExtension(e).toFile();
			if (exeFile.exists()) {
				return e;
			}
		}

		return null;
	}

	private boolean isWindowsExecutableExtension(String extension) {
		return extension != null && WINDOWS_NATIVE_EXECUTABLES.contains(extension);
	}
}
