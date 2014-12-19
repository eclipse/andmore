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

package org.eclipse.andmore.ndk.internal.launch;

import org.eclipse.andmore.ndk.internal.Activator;
import org.eclipse.andmore.ndk.internal.NdkVariables;

public class NdkLaunchConstants {
	private static final String PREFIX = Activator.PLUGIN_ID + ".ndklaunch."; //$NON-NLS-1$

	public static final String ATTR_NDK_GDB = PREFIX + "gdb"; //$NON-NLS-1$
	public static final String ATTR_NDK_GDBINIT = PREFIX + "gdbinit"; //$NON-NLS-1$
	public static final String ATTR_NDK_SOLIB = PREFIX + "solib"; //$NON-NLS-1$

	public static final String DEFAULT_GDB_PORT = "5039"; //$NON-NLS-1$
	public static final String DEFAULT_GDB = getVar(NdkVariables.NDK_GDB);
	public static final String DEFAULT_GDBINIT = ""; //$NON-NLS-1$
	public static final String DEFAULT_PROGRAM = String.format("%1$s/obj/local/%2$s/app_process", //$NON-NLS-1$
			getVar(NdkVariables.NDK_PROJECT), getVar(NdkVariables.NDK_COMPAT_ABI));
	public static final String DEFAULT_SOLIB_PATH = String.format("%1$s/obj/local/%2$s/", //$NON-NLS-1$
			getVar(NdkVariables.NDK_PROJECT), getVar(NdkVariables.NDK_COMPAT_ABI));

	private static String getVar(String varName) {
		return "${" + varName + '}'; //$NON-NLS-1$
	}
}
