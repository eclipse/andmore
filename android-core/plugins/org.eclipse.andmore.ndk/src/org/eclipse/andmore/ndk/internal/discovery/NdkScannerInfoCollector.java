/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.andmore.ndk.internal.discovery;

import org.eclipse.cdt.make.core.scannerconfig.IDiscoveredPathManager.IDiscoveredPathInfo;
import org.eclipse.cdt.make.core.scannerconfig.IScannerInfoCollector3;
import org.eclipse.cdt.make.core.scannerconfig.IScannerInfoCollectorCleaner;
import org.eclipse.cdt.make.core.scannerconfig.InfoContext;
import org.eclipse.cdt.make.core.scannerconfig.ScannerInfoTypes;
import org.eclipse.cdt.managedbuilder.scannerconfig.IManagedScannerInfoCollector;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import java.util.List;
import java.util.Map;

public class NdkScannerInfoCollector implements IScannerInfoCollector3, IScannerInfoCollectorCleaner,
		IManagedScannerInfoCollector {

	private NdkDiscoveredPathInfo mPathInfo;

	@Override
	public void contributeToScannerConfig(Object resource, Map scannerInfo) {
		throw new Error("Not implemented"); //$NON-NLS-1$
	}

	@Override
	public List getCollectedScannerInfo(Object resource, ScannerInfoTypes type) {
		throw new Error("Not implemented"); //$NON-NLS-1$
	}

	@Override
	public void setProject(IProject project) {
		throw new Error("Not implemented"); //$NON-NLS-1$
	}

	@Override
	public void updateScannerConfiguration(IProgressMonitor monitor) throws CoreException {
		mPathInfo.update(monitor);
	}

	@Override
	public IDiscoveredPathInfo createPathInfoObject() {
		return mPathInfo;
	}

	@Override
	public Map<String, String> getDefinedSymbols() {
		throw new Error("Not implemented"); //$NON-NLS-1$
	}

	@Override
	public List getIncludePaths() {
		throw new Error("Not implemented"); //$NON-NLS-1$
	}

	@Override
	public void setInfoContext(InfoContext context) {
		mPathInfo = new NdkDiscoveredPathInfo(context.getProject());
	}

	@Override
	public void deleteAllPaths(IResource resource) {
		throw new Error("Not implemented"); //$NON-NLS-1$
	}

	@Override
	public void deleteAllSymbols(IResource resource) {
		throw new Error("Not implemented"); //$NON-NLS-1$
	}

	@Override
	public void deletePath(IResource resource, String path) {
		throw new Error("Not implemented"); //$NON-NLS-1$
	}

	@Override
	public void deleteSymbol(IResource resource, String symbol) {
		throw new Error("Not implemented"); //$NON-NLS-1$
	}

	@Override
	public void deleteAll(IResource resource) {
		mPathInfo.delete();
	}

}
