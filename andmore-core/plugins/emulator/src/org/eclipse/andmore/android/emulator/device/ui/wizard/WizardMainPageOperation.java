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
package org.eclipse.andmore.android.emulator.device.ui.wizard;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

import org.eclipse.andmore.android.SdkUtils;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.andmore.android.emulator.device.refresh.InstancesListRefresh;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.sequoyah.device.framework.ui.wizard.DefaultDeviceTypeMenuWizardPage;
import org.eclipse.sequoyah.device.framework.ui.wizard.DeviceWizardRunnable;

/**
 * This class performs the wizard finish operation for the WizardMainPage,
 * according to the extension point
 * org.eclipse.sequoyah.device.framework.ui.newDeviceWizardPages
 */
public class WizardMainPageOperation extends DeviceWizardRunnable {
	/**
	 * Action executed on the wizard finish
	 */
	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		// Get wizard pages
		WizardMainPage page = (WizardMainPage) this.getWizardPage();
		DefaultDeviceTypeMenuWizardPage tmlPage = (DefaultDeviceTypeMenuWizardPage) page.getPreviousPage();

		// Create VM
		try {
			// TML should provide some instance name changed listener
			if (!tmlPage.getInstanceName().equals(page.getName())) {
				page.setInstanceName(tmlPage.getInstanceName());
			}

			SdkUtils.createVm(page.getVmPath(), tmlPage.getInstanceName(), page.getVmTarget(), page

			.getAbiType(), page.getVmSkin(), page.getUseSnapshot(),
					(page.getSDCard().length() == 0 ? null : page.getSDCard()));

		} catch (CoreException e) {
			EclipseUtils.showErrorDialog("Could not create instance ", e.getStatus().getMessage());

			StudioLogger.error(WizardMainPageOperation.class, "Could not create AVD: " + tmlPage.getInstanceName(), e);
		}

		Collection<String> vmInstances = SdkUtils.getAllValidVmNames();
		InstancesListRefresh.refreshStatus(vmInstances, tmlPage.getInstanceName());

	}
}
