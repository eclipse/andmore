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
package org.eclipse.andmore.android.common.utilities;

import org.eclipse.andmore.android.common.CommonPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Status for Android Plugin.
 * 
 * @deprecated use the standard {@link Status}.
 */
@Deprecated
public class AndroidStatus extends Status {
	/**
	 * Constructor for "OK" Status
	 */
	public AndroidStatus() {
		super(IStatus.OK, CommonPlugin.PLUGIN_ID, null);
	}

	/**
	 * Constructor for others status.
	 * 
	 * @param severity
	 * @param msg
	 */
	public AndroidStatus(int severity, String msg) {
		super(severity, CommonPlugin.PLUGIN_ID, msg);
	}

}
