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

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.envvar.IBuildEnvironmentVariable;
import org.eclipse.cdt.managedbuilder.envvar.IConfigurationEnvironmentVariableSupplier;
import org.eclipse.cdt.managedbuilder.envvar.IEnvironmentVariableProvider;
import org.eclipse.core.runtime.Platform;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class NdkEnvSupplier implements IConfigurationEnvironmentVariableSupplier {

	private static Map<String, IBuildEnvironmentVariable> mEnvVars;

	private synchronized void init() {
		if (mEnvVars != null)
			return;

		mEnvVars = new HashMap<String, IBuildEnvironmentVariable>();

		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			// For Windows, need to add a shell to the path
			IBuildEnvironmentVariable path = new IBuildEnvironmentVariable() {
				@Override
				public String getName() {
					return "PATH"; //$NON-NLS-1$
				}

				@Override
				public String getValue() {
					// I'm giving MSYS precedence over Cygwin. I'm biased that
					// way :)
					// TODO using the default paths for now, need smarter ways
					// to get at them
					// Alternatively the user can add the bin to their path
					// themselves.
					File bin = new File("C:\\MinGW\\msys\\1.0\\bin"); //$NON-NLS-1$
					if (bin.isDirectory()) {
						return bin.getAbsolutePath();
					}

					bin = new File("C:\\cygwin\\bin"); //$NON-NLS-1$
					if (bin.isDirectory())
						return bin.getAbsolutePath();

					return null;
				}

				@Override
				public int getOperation() {
					return ENVVAR_PREPEND;
				}

				@Override
				public String getDelimiter() {
					return ";"; //$NON-NLS-1$
				}
			};
			if (path.getValue() != null)
				mEnvVars.put(path.getName(), path);

			// Since we're using real paths, need to tell cygwin it's OK
			IBuildEnvironmentVariable cygwin = new IBuildEnvironmentVariable() {
				@Override
				public String getName() {
					return "CYGWIN"; //$NON-NLS-1$
				}

				@Override
				public String getValue() {
					return "nodosfilewarning"; //$NON-NLS-1$
				}

				@Override
				public int getOperation() {
					return ENVVAR_REPLACE;
				}

				@Override
				public String getDelimiter() {
					return null;
				}
			};

			mEnvVars.put(cygwin.getName(), cygwin);
		}
	}

	@Override
	public IBuildEnvironmentVariable getVariable(String variableName, IConfiguration configuration,
			IEnvironmentVariableProvider provider) {
		init();
		return mEnvVars.get(variableName);
	}

	@Override
	public IBuildEnvironmentVariable[] getVariables(IConfiguration configuration, IEnvironmentVariableProvider provider) {
		init();
		return mEnvVars.values().toArray(new IBuildEnvironmentVariable[mEnvVars.size()]);
	}

}
