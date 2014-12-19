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

package org.eclipse.andmore.base;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

public class InstallDetails {
	private static final String ADT_PLUGIN_ID = "org.eclipse.andmore"; //$NON-NLS-1$
	private static final String ECLIPSE_PLATFORM_PLUGIN_ID = "org.eclipse.platform"; //$NON-NLS-1$

	/**
	 * Returns true if the ADT plugin is available in the current platform. This
	 * is useful for distinguishing between specific RCP applications vs. ADT +
	 * Eclipse.
	 */
	public static boolean isAdtInstalled() {
		Bundle b = Platform.getBundle(ADT_PLUGIN_ID);
		return b != null;
	}

	/** Returns the version of current eclipse platform. */
	public static Version getPlatformVersion() {
		Bundle b = Platform.getBundle(ECLIPSE_PLATFORM_PLUGIN_ID);
		return b == null ? Version.emptyVersion : b.getVersion();
	}
}
