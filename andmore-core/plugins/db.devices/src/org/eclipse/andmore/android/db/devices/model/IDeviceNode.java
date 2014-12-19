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
package org.eclipse.andmore.android.db.devices.model;

import org.eclipse.andmore.android.db.core.ui.ITreeNode;

public interface IDeviceNode extends ITreeNode {

	/**
	 * enables/disables the filter apps with db
	 * 
	 * @param filterAppWithDb
	 */
	void setFilterAppWithDb(boolean filterAppWithDb);

	/**
	 * @return the state of the filter apps with db, true for enabled, false
	 *         otherwise
	 */
	boolean mustFilterAppsWithDb();

	/**
	 * @return the device serial number
	 */
	String getSerialNumber();

}
