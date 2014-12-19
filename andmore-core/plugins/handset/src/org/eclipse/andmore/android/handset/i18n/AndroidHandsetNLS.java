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
package org.eclipse.andmore.android.handset.i18n;

import org.eclipse.osgi.util.NLS;

/**
 * Class that contains the localized messages to be used through the Android
 * Handset Plugin
 */
public class AndroidHandsetNLS extends NLS {
	static {
		NLS.initializeMessages("org.eclipse.andmore.android.handset.i18n.androidHandsetNLS", AndroidHandsetNLS.class);
	}

	/*
	 * Generic strings
	 */

	public static String AndroidPropertiesPage_AndroidVersionLabel;

	public static String AndroidPropertiesPage_APIVersionLabel;

	public static String AndroidPropertiesPage_NA;

	public static String AndroidPropertiesPage_SerialNumberLabel;

	public static String DummyServiceHandler_androidSuffix;

	public static String DummyServiceHandler_VERSION_NA;

	/*
	 * Exception strings
	 */

	/*
	 * Error strings
	 */

	/*
	 * Information strings
	 */
}
