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

import java.net.URI;
import java.util.Collection;
import java.util.List;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.installer.InstallerException;
import org.eclipse.andmore.android.installer.InstallerPlugin;
import org.eclipse.andmore.android.installer.i18n.InstallerNLS;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Utility methods for downloading and installing updates
 * 
 */
public class InstallManager implements IInstallManager {

	private static InstallManager instance;

	P2Installer p2Installer = new P2Installer();

	/*
	 * This class is a singleton.
	 */
	private InstallManager() {
		// Do nothing, singleton class.
	}

	/**
	 * Returns an instance of the Install Manager
	 * 
	 * @return
	 */
	public static synchronized IInstallManager getInstance() {
		if (instance == null) {
			instance = new InstallManager();
		}
		return instance;
	}

	/**
	 * Updates studio in one single operation based in a link to look for the
	 * available updates. The backend parameter tells the manager how to look
	 * for updates on the site (for instance a P2 update site).
	 * 
	 * IMPORTANT: the method listAllAvailableUpdates MUST be called first
	 */
	@Override
	public IStatus updateStudio(List<URI> links, BACKEND backEnd, IProgressMonitor monitor) {
		Status status = new Status(IStatus.ERROR, InstallerPlugin.PLUGIN_ID, 0,
				InstallerNLS.InstallManager_Could_Not_Find_Proper_Backend, null);

		switch (backEnd) {
		case P2: {
			return p2Installer.updateStudio(monitor);
		}

		default: {
			StudioLogger.debug(this, "updateStudio felt back to default.");
			break;
		}
		}

		return status;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.installer.utilities.IInstallManager#
	 * listAllAvailableUpdates(java.util.Collection, java.util.List,
	 * com.motorola.studio.android.installer.utilities.IInstallManager.CATEGORY,
	 * com.motorola.studio.android.installer.utilities.IInstallManager.BACKEND,
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public IStatus listAllAvailableUpdates(Collection<InstallableItem> listToFill, List<URI> links, CATEGORY category,
			BACKEND backEnd, IProgressMonitor monitor) throws InstallerException {

		IStatus status = null;
		switch (backEnd) {
		case P2:
			status = p2Installer.listAllAvailableUpdates(listToFill, links, category, backEnd, monitor);
			break;

		default: {
			StudioLogger.debug(this, "listAllAvailableUpdates felt back to default.");
			break;
		}
		}
		return status;

	}

}
