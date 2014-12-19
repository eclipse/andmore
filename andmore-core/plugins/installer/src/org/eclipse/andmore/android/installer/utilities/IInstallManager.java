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

import org.eclipse.andmore.android.installer.InstallerException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

public interface IInstallManager {

	/**
	 * What kind of backend technology should be used to read the links and find
	 * updates
	 */
	public enum BACKEND {
		P2, HTTP, LIBRARY
	}

	/**
	 * Which category should be used for filter the results
	 */
	public enum CATEGORY {
		NDK, LANG_PACKS, UPDATE_STUDIO, OTHER_COMPONENTS, LIBRARY
	}

	/**
	 * Checks for updates on the given link and using the given backend
	 * 
	 * @param listToFill
	 * @param links
	 * @param category
	 * @param backEnd
	 * @param monitor
	 * @return
	 * @throws InstallerException
	 */
	public IStatus listAllAvailableUpdates(Collection<InstallableItem> listToFill, List<URI> links, CATEGORY category,
			BACKEND backEnd, IProgressMonitor monitor) throws InstallerException;

	/**
	 * Update entire studio, combines list, download and Install) downloading
	 * all that is required for an update on studio
	 * 
	 * @param links
	 * @param usedBackEnd
	 * @return
	 */
	public IStatus updateStudio(List<URI> links, BACKEND backEnd, IProgressMonitor monitor);

}