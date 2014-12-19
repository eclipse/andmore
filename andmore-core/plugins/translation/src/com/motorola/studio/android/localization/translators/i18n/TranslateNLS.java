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

package com.motorola.studio.android.localization.translators.i18n;

import org.eclipse.osgi.util.NLS;

public class TranslateNLS extends NLS {
	static {
		NLS.initializeMessages("org.eclipse.andmore.android.localization.translators.i18n.translateNLS", //$NON-NLS-1$
				TranslateNLS.class);
	}

	public static String GoogleTranslator_ChangeAPIkeyLabel;

	public static String GoogleTranslator_Error_CannotConnectToServer;

	public static String GoogleTranslator_Error_HTTPRequestError;

	public static String GoogleTranslator_Error_NoAvailableData;

	public static String GoogleTranslator_Error_QueryTooBig;

	public static String GoogleTranslator_Error_ToAndFromLanguagesAreEmpty;

	public static String GoogleTranslator_Error_UnsupportedEncoding;

	public static String GoogleTranslator_ErrorMessageExecutingRequest;

	public static String GoogleTranslator_ErrorMessageNoValidTranslationReturned;

	public static String GoogleTranslator_ErrorNoAPIkeySet;

	public static String AndroidPreferencePage_googleApiKey_GroupLabel;

	public static String AndroidPreferencePage_googleApiKey_Label;

	public static String AndroidPreferencePage_googleApiKey_Note;

	public static String AndroidPreferencePage_googleApiKey_Tooltip;

}
