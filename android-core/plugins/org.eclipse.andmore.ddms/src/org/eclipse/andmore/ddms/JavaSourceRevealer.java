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

package org.eclipse.andmore.ddms;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

import java.util.ArrayList;
import java.util.List;

public class JavaSourceRevealer {
	private static final String SOURCE_REVEALER_EXTENSION_ID = "org.eclipse.andmore.ddms.sourceRevealer"; //$NON-NLS-1$

	private static List<ISourceRevealer> sSourceRevealers = instantiateSourceRevealers();

	/**
	 * Instantiate all providers of the {@link #SOURCE_REVEALER_EXTENSION_ID}
	 * extension.
	 */
	private static List<ISourceRevealer> instantiateSourceRevealers() {
		IConfigurationElement[] configElements = DdmsPlugin.getDefault().findConfigElements(
				SOURCE_REVEALER_EXTENSION_ID);

		List<ISourceRevealer> providers = new ArrayList<ISourceRevealer>();

		for (IConfigurationElement configElement : configElements) {
			// instantiate the class
			Object obj = null;
			try {
				obj = configElement.createExecutableExtension("class"); //$NON-NLS-1$
			} catch (CoreException e) {
				// ignore exception while instantiating this class.
			}

			if (obj instanceof ISourceRevealer) {
				providers.add((ISourceRevealer) obj);
			}
		}

		return providers;
	}

	public static boolean reveal(String applicationName, String className, int line) {
		for (ISourceRevealer revealer : sSourceRevealers) {
			try {
				if (revealer.reveal(applicationName, className, line)) {
					return true;
				}
			} catch (Throwable t) {
				// ignore, we'll just not use this implementation.
			}
		}

		return false;
	}

	public static boolean revealMethod(String fqmn, String fileName, int linenumber, String perspective) {
		for (ISourceRevealer revealer : sSourceRevealers) {
			try {
				if (revealer.revealMethod(fqmn, fileName, linenumber, perspective)) {
					return true;
				}
			} catch (Throwable t) {
				// ignore, we'll just not use this implementation.
			}
		}

		return false;
	}
}
