/*******************************************************************************
 * Copyright (c) 2010 Wind River Systems and others.
 * Copyright (c) 2010 Motorola, Inc. All rights reserved.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Doug Schaefer (WRS) - Initial API and implementation
 * Carlos Alberto Souto Junior (Eldorado) - [315122] Improvements in the Android NDK support UI
 * Carlos Alberto Souto Junior (Eldorado) - [317327] Major UI bugfixes and improvements in Android Native support
 *******************************************************************************/

package org.eclipse.sequoyah.android.cdt.internal.build.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.build.core.scannerconfig.CfgInfoContext;
import org.eclipse.cdt.build.core.scannerconfig.ICfgScannerConfigBuilderInfo2Set;
import org.eclipse.cdt.build.internal.core.scannerconfig2.CfgScannerConfigProfileManager;
import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IPathEntry;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationData;
import org.eclipse.cdt.make.core.scannerconfig.IScannerConfigBuilderInfo2;
import org.eclipse.cdt.managedbuilder.core.BuildException;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.internal.core.Configuration;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedProject;
import org.eclipse.cdt.managedbuilder.internal.core.ToolChain;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.sequoyah.android.cdt.build.core.INDKService;
import org.eclipse.sequoyah.android.cdt.build.core.NDKUtils;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Implementation of the NDK service.
 */
public class NDKService implements INDKService {

	private static final String NDK_LOCATION = "ndkLocation";
	
	public String getNDKLocation() {
		return CorePlugin.getPreferenceStore().get(NDK_LOCATION, null);
	}

	public void setNDKLocation(String location) {
		IEclipsePreferences prefs = new InstanceScope().getNode(CorePlugin.PLUGIN_ID);
		prefs.put(NDK_LOCATION, location);
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			CorePlugin.getDefault().getLog().log(new Status(IStatus.ERROR, CorePlugin.PLUGIN_ID, e.getLocalizedMessage(), e));
		}
	}

	public void addNativeSupport(final IProject project, final String libraryName) {
		// The operation to do all the dirty work
		IWorkspaceRunnable op = new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				// Convert to CDT project
				CCorePlugin.getDefault().createCDTProject(project.getDescription(), project, monitor);
				// TODO we should make C++ optional
				CCProjectNature.addCCNature(project, new SubProgressMonitor(monitor, 1));
							
				// Set up build information
				ICProjectDescriptionManager pdMgr = CoreModel.getDefault().getProjectDescriptionManager();
				ICProjectDescription projDesc = pdMgr.createProjectDescription(project, false);
				ManagedBuildInfo info = ManagedBuildManager.createBuildInfo(project);
				ManagedProject mProj = new ManagedProject(projDesc);
				info.setManagedProject(mProj);

				String toolChainId = "com.android.toolchain.gcc";
				IToolChain toolChain = null;
				for (IToolChain tc : ManagedBuildManager.getRealToolChains())
					if (tc.getId().equals(toolChainId)) {
						toolChain = tc;
						break;
					}
				Configuration config = new Configuration(mProj, (ToolChain)toolChain, ManagedBuildManager.calculateChildId(toolChainId, null), "Default");
				projDesc.createConfiguration(ManagedBuildManager.CFG_DATA_PROVIDER_ID, config.getConfigurationData());
				pdMgr.setProjectDescription(project, projDesc);
				
				// Create the source and output folders
				IFolder sourceFolder = project.getFolder(NDKUtils.DEFAULT_JNI_FOLDER_NAME);
				if (!sourceFolder.exists())
					sourceFolder.create(true, true, monitor);
				IPathEntry sourceEntry = CoreModel.newSourceEntry(sourceFolder.getFullPath());
				
				IFolder libFolder = project.getFolder("libs");
				if (!libFolder.exists())
					libFolder.create(true, true, monitor);
				IPathEntry libEntry = CoreModel.newOutputEntry(libFolder.getFullPath());
				
				// Set up the path entries for the source and output folders
				CoreModel model = CCorePlugin.getDefault().getCoreModel();
				ICProject cproject = model.create(project);
				IPathEntry[] pathEntries = cproject.getRawPathEntries();
				List<IPathEntry> newEntries = new ArrayList<IPathEntry>(pathEntries.length + 2);
				for (IPathEntry pathEntry : pathEntries) {
					// remove the old source and output entries
					if (pathEntry.getEntryKind() != IPathEntry.CDT_SOURCE
							&& pathEntry.getEntryKind() != IPathEntry.CDT_OUTPUT) {
						newEntries.add(pathEntry);
					}
				}
				newEntries.add(sourceEntry);
				newEntries.add(libEntry);
				cproject.setRawPathEntries(newEntries.toArray(new IPathEntry[newEntries.size()]), monitor);
				
				try {
					// Generate the Android.mk file
					Map<String, String> map = new HashMap<String, String>();
					map.put("lib", libraryName);
					
					URL makefileURL = CorePlugin.getFile(new Path("templates/" + NDKUtils.MAKEFILE_FILE_NAME));
					InputStream makefileIn = makefileURL.openStream();
					InputStream templateIn = new TemplatedInputStream(makefileIn, map);

					IFile makefile = sourceFolder.getFile(NDKUtils.MAKEFILE_FILE_NAME);
					if (!makefile.exists()) {
						makefile.create(templateIn, true, monitor);
					
						// Copy over initial source file
						// TODO we should allow C or C++ files
						URL srcURL = CorePlugin.getFile(new Path("templates/template.cpp"));
						InputStream srcIn = srcURL.openStream();
						IFile srcFile = sourceFolder.getFile(libraryName + ".cpp");
						if (!srcFile.exists())
							srcFile.create(srcIn, true, monitor);
						
						NDKUtils.addSourceFileToMakefile(makefile, srcFile.getName());
						
						// refresh project resources
						project.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 10));
					}
				} catch (IOException e) {
					throw new CoreException(new Status(IStatus.ERROR, CorePlugin.PLUGIN_ID, e.getLocalizedMessage(), e));
				} 
			}
		};
		
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		try {
			workspace.run(op, workspace.getRoot(), 0, null);
		} catch (CoreException e) {
			CorePlugin.getDefault().getLog().log(e.getStatus());
		}
	}
	
}
