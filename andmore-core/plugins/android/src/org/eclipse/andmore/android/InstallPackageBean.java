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

package org.eclipse.andmore.android;

import org.eclipse.andmore.android.wizards.installapp.DeployWizard.INSTALL_TYPE;

public class InstallPackageBean {

	private String packagePath = null;

	private INSTALL_TYPE canOverwrite = null;

	public String getPackagePath() {
		return packagePath;
	}

	public void setPackagePath(String packagePath) {
		this.packagePath = packagePath;
	}

	public INSTALL_TYPE getCanOverwrite() {
		return canOverwrite;
	}

	public void setCanOverwrite(INSTALL_TYPE canOverwrite) {
		this.canOverwrite = canOverwrite;
	}

}
