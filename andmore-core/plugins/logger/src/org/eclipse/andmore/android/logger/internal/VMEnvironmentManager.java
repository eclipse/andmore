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
package org.eclipse.andmore.android.logger.internal;

import org.eclipse.andmore.android.logger.Logger;

/**
 * This class logs the Environment according to VM information.
 */
public class VMEnvironmentManager implements EnvironmentManager {

	// Constants --------------------------------------------

	private static final String[] property = { "os.name", //$NON-NLS-1$
			"os.arch", //$NON-NLS-1$
			"os.version", //$NON-NLS-1$
			"java.version", //$NON-NLS-1$
			"java.vendor", //$NON-NLS-1$
			"java.vendor.url", //$NON-NLS-1$
			"java.home", //$NON-NLS-1$
			"java.vm.specification.name", //$NON-NLS-1$
			"java.vm.specification.vendor", //$NON-NLS-1$
			"java.vm.specification.version", //$NON-NLS-1$
			"java.class.path", //$NON-NLS-1$
			"java.library.path" //$NON-NLS-1$
	};

	/**
	 * Logs the environment based on the System VM properties.
	 */
	@Override
	public void logEnvironment() {
		Logger envLogger = Logger.getLogger("org.eclipse.andmore.environment"); //$NON-NLS-1$
		/* Navigates looking for the neccessary information. */
		for (int i = 0; i < property.length; i++) {
			String value = System.getProperty(property[i]);
			if (value != null) {
				envLogger.info(property[i] + " - " + value); //$NON-NLS-1$
			}
		}
	}
}
