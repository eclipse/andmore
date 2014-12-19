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

/** Eclipse variables that are understood by the NDK while launching programs. */
public class NdkVariables {
	/** Variable that expands to the full path of NDK's ABI specific gdb. */
	public static final String NDK_GDB = "NdkGdb";

	/**
	 * Variable that expands to point to the full path of the project used in
	 * the launch configuration.
	 */
	public static final String NDK_PROJECT = "NdkProject";

	/**
	 * Variable that indicates the ABI that is compatible between the device and
	 * the application being launched.
	 */
	public static final String NDK_COMPAT_ABI = "NdkCompatAbi";
}
