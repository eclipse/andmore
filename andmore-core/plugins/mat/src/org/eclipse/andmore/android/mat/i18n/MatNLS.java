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
package org.eclipse.andmore.android.mat.i18n;

import org.eclipse.osgi.util.NLS;

/**
 * Class that contains the localized messages to be used through the MAT Plugin
 *
 */
public class MatNLS extends NLS {
	static {
		NLS.initializeMessages("org.eclipse.andmore.android.mat.i18n.matNLS", MatNLS.class);
	}

	/*
	 * UI strings
	 */

	public static String Motodev_Pane_Title;

	public static String Action_Open_Motodev_Pane;

	public static String DumpHPROFHandler_DEVICE_NOT_READY;

	public static String DumpHPROFHandler_UNSUPPORTED_DEVICE;

	public static String JOB_Name_Dump_Hprof;

}
