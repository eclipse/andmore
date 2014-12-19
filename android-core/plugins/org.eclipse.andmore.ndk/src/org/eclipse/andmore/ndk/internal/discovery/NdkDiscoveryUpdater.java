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

package org.eclipse.andmore.ndk.internal.discovery;

import org.eclipse.andmore.ndk.internal.Activator;
import org.eclipse.andmore.ndk.internal.build.NdkCommandLauncher;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.managedbuilder.core.IBuilder;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class NdkDiscoveryUpdater {
	private final NdkDiscoveredPathInfo mPathInfo;
	private final IProject mProject;

	private boolean mCPlusPlus = false;
	private String mCommand;
	private List<String> mArguments = new ArrayList<String>();

	public NdkDiscoveryUpdater(NdkDiscoveredPathInfo pathInfo) {
		mPathInfo = pathInfo;
		mProject = pathInfo.getProject();
	}

	public void runUpdate(IProgressMonitor monitor) throws CoreException {
		try {
			// Run ndk-build -nB to get the list of commands
			IPath commandPath = new Path("ndk-build"); //$NON-NLS-1$
			String[] args = { "-nB" }; //$NON-NLS-1$
			String[] env = calcEnvironment();
			File projectDir = new File(mProject.getLocationURI());
			IPath changeToDirectory = new Path(projectDir.getAbsolutePath());
			Process proc = new NdkCommandLauncher().execute(commandPath, args, env, changeToDirectory, monitor);
			if (proc == null)
				// proc failed to start
				return;
			BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line = reader.readLine();
			while (line != null) {
				checkBuildLine(line);
				line = reader.readLine();
			}

			if (mCommand == null) {
				return;
			}

			// Run the unique commands with special gcc options to extract the
			// symbols and paths
			// -E -P -v -dD
			mArguments.add("-E"); //$NON-NLS-1$
			mArguments.add("-P"); //$NON-NLS-1$
			mArguments.add("-v"); //$NON-NLS-1$
			mArguments.add("-dD"); //$NON-NLS-1$

			URL url = Activator.findFile(new Path("discovery/" + (mCPlusPlus ? "test.cpp" : "test.c"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			File testFile = new File(FileLocator.toFileURL(url).toURI());
			String testFileName = testFile.getAbsolutePath().replace('\\', '/');
			mArguments.add(testFileName);

			args = mArguments.toArray(new String[mArguments.size()]);
			proc = new NdkCommandLauncher().execute(new Path(mCommand), args, env, changeToDirectory, monitor);
			// Error stream has the includes
			final InputStream errStream = proc.getErrorStream();
			new Thread() {
				@Override
				public void run() {
					checkIncludes(errStream);
				};
			}.start();

			// Input stream has the defines
			checkDefines(proc.getInputStream());
		} catch (IOException e) {
			throw new CoreException(Activator.newStatus(e));
		} catch (URISyntaxException e) {
			throw new CoreException(Activator.newStatus(e));
		}
	}

	private String[] calcEnvironment() throws CoreException {
		IManagedBuildInfo info = ManagedBuildManager.getBuildInfo(mProject);
		IBuilder builder = info.getDefaultConfiguration().getBuilder();
		HashMap<String, String> envMap = new HashMap<String, String>();
		if (builder.appendEnvironment()) {
			ICConfigurationDescription cfgDes = ManagedBuildManager.getDescriptionForConfiguration(builder.getParent()
					.getParent());
			IEnvironmentVariableManager mngr = CCorePlugin.getDefault().getBuildEnvironmentManager();
			IEnvironmentVariable[] vars = mngr.getVariables(cfgDes, true);
			for (IEnvironmentVariable var : vars) {
				envMap.put(var.getName(), var.getValue());
			}
		}
		// Add variables from build info
		Map<String, String> builderEnv = builder.getExpandedEnvironment();
		if (builderEnv != null)
			envMap.putAll(builderEnv);
		List<String> strings = new ArrayList<String>(envMap.size());
		for (Entry<String, String> entry : envMap.entrySet()) {
			StringBuffer buffer = new StringBuffer(entry.getKey());
			buffer.append('=').append(entry.getValue());
			strings.add(buffer.toString());
		}
		return strings.toArray(new String[strings.size()]);
	}

	private static class Line {
		private final String line;
		private int pos;

		public Line(String line) {
			this.line = line;
		}

		public Line(String line, int pos) {
			this(line);
			this.pos = pos;
		}

		public String getToken() {
			skipWhiteSpace();
			if (pos == line.length())
				return null;

			int start = pos;
			boolean inQuote = false;

			while (true) {
				char c = line.charAt(pos);
				if (c == ' ') {
					if (!inQuote)
						return line.substring(start, pos);
				} else if (c == '"') {
					inQuote = !inQuote;
				}

				if (++pos == line.length())
					return null;
			}

		}

		private String getRemaining() {
			if (pos == line.length())
				return null;

			skipWhiteSpace();
			String rc = line.substring(pos);
			pos = line.length();
			return rc;
		}

		private void skipWhiteSpace() {
			while (true) {
				if (pos == line.length())
					return;
				char c = line.charAt(pos);
				if (c == ' ')
					pos++;
				else
					return;
			}
		}
	}

	private void checkBuildLine(String text) {
		Line line = new Line(text);
		String cmd = line.getToken();
		if (cmd == null) {
			return;
		} else if (cmd.endsWith("g++")) { //$NON-NLS-1$
			if (mCommand == null || !mCPlusPlus) {
				mCommand = cmd;
				mCPlusPlus = true;
			}
			gatherOptions(line);
		} else if (cmd.endsWith("gcc")) { //$NON-NLS-1$
			if (mCommand == null)
				mCommand = cmd;
			gatherOptions(line);
		}
	}

	private void gatherOptions(Line line) {
		for (String option = line.getToken(); option != null; option = line.getToken()) {
			if (option.startsWith("-")) { //$NON-NLS-1$
				// only look at options
				if (option.equals("-I")) { //$NON-NLS-1$
					String dir = line.getToken();
					if (dir != null)
						addArg(option + dir);
				} else if (option.startsWith("-I")) { //$NON-NLS-1$
					addArg(option);
				} else if (option.equals("-D")) { //$NON-NLS-1$
					String def = line.getToken();
					if (def != null)
						addArg(option + def);
				} else if (option.startsWith("-D")) { //$NON-NLS-1$
					addArg(option);
				} else if (option.startsWith("-f")) { //$NON-NLS-1$
					addArg(option);
				} else if (option.startsWith("-m")) { //$NON-NLS-1$
					addArg(option);
				} else if (option.startsWith("--sysroot")) { //$NON-NLS-1$
					addArg(option);
				}
			}
		}
	}

	private void addArg(String arg) {
		if (!mArguments.contains(arg))
			mArguments.add(arg);
	}

	private void checkIncludes(InputStream in) {
		try {
			List<String> includes = new ArrayList<String>();
			boolean inIncludes1 = false;
			boolean inIncludes2 = false;
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line = reader.readLine();
			while (line != null) {
				if (!inIncludes1) {
					if (line.equals("#include \"...\" search starts here:")) //$NON-NLS-1$
						inIncludes1 = true;
				} else {
					if (!inIncludes2) {
						if (line.equals("#include <...> search starts here:")) //$NON-NLS-1$
							inIncludes2 = true;
						else
							includes.add(line.trim());
					} else {
						if (line.equals("End of search list.")) { //$NON-NLS-1$
							mPathInfo.setIncludePaths(includes);
						} else {
							includes.add(line.trim());
						}
					}
				}
				line = reader.readLine();
			}
		} catch (IOException e) {
			Activator.log(e);
		}
	}

	private void checkDefines(InputStream in) {
		try {
			Map<String, String> defines = new HashMap<String, String>();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line = reader.readLine();
			while (line != null) {
				if (line.startsWith("#define")) { //$NON-NLS-1$
					Line l = new Line(line, 7);
					String var = l.getToken();
					if (var == null)
						continue;
					String value = l.getRemaining();
					if (value == null)
						value = ""; //$NON-NLS-1$
					defines.put(var, value);
				}
				line = reader.readLine();
			}
			mPathInfo.setSymbols(defines);
		} catch (IOException e) {
			Activator.log(e);
		}
	}

}
