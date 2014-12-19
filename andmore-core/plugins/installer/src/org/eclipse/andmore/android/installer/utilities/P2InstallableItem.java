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
package org.eclipse.andmore.android.installer.utilities;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.ILicense;
import org.eclipse.equinox.p2.ui.LicenseManager;
import org.eclipse.equinox.p2.ui.ProvisioningUI;

/**
 * Encapsulates the specific items from P2 such as controlling licenses accepted
 */
class P2InstallableItem extends InstallableItem {
	@Override
	public boolean hasLicenseNotAccepted() {
		LicenseManager manager = ProvisioningUI.getDefaultUI().getLicenseManager();
		IInstallableUnit iu = (IInstallableUnit) getData();
		for (ILicense license : iu.getLicenses()) {
			if (!manager.isAccepted(license)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void acceptLicenses() {
		LicenseManager manager = ProvisioningUI.getDefaultUI().getLicenseManager();
		IInstallableUnit iu = (IInstallableUnit) getData();
		for (ILicense license : iu.getLicenses()) {
			manager.accept(license);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.installer.utilities.InstallableItem#
	 * hasPrerequrementsFulfilled()
	 */
	@Override
	public boolean hasPrerequirementsFulfilled() {
		// TODO Auto-generated method stub
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.installer.utilities.InstallableItem#
	 * getMaxBufferSize()
	 */
	@Override
	public int getMaxBufferSize() {
		return 65536;
	}

}
