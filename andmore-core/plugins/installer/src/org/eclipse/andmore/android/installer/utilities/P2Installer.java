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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.installer.InstallerException;
import org.eclipse.andmore.android.installer.InstallerPlugin;
import org.eclipse.andmore.android.installer.i18n.InstallerNLS;
import org.eclipse.andmore.android.installer.utilities.IInstallManager.BACKEND;
import org.eclipse.andmore.android.installer.utilities.IInstallManager.CATEGORY;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.operations.Update;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;

/**
 * Implements the methods for using the INstall framework with P2
 */
class P2Installer {

	private final String LANGUAGE_PACK_QUERY = "this.id ~= /com.motorola.studio.android.feature.nl*/";

	private final String NKD_QUERY = "org.eclipse.sequoyah.android.cdt.feature.feature.group";

	private final String SUBVERSION_QUERY = "org.eclipse.team.svn.feature.group";

	private final String EGIT_QUERY = "org.eclipse.egit.feature.group";

	private final String MYLYN_QUERY1 = "org.eclipse.mylyn.context_feature.feature.group";

	private final String MYLYN_QUERY2 = "org.eclipse.mylyn_feature.feature.group";

	private final String CVS_QUERY = "org.eclipse.cvs.feature.group";

	private final HashMap<URI, IMetadataRepository> mainRepositories = new HashMap<URI, IMetadataRepository>();

	// will be used to record an instance of a InstallOperation. If it did not
	// exist, we would have to instantiate
	// the InstallOperation on validate and download methods
	private InstallOperation installOp;

	private UpdateOperation up;

	/**
     * 
     */
	public void resetP2Installer() {
		mainRepositories.clear();
		installOp = null;
	}

	private boolean isAllRepositoriesLoaded(Collection<URI> links) {

		boolean isAllLoaded = false;

		if (!mainRepositories.isEmpty()) {
			isAllLoaded = true;
			for (URI uri : links) {
				if (!mainRepositories.containsKey(uri)) {
					isAllLoaded = false;
					break;
				}
			}
		}
		return isAllLoaded;
	}

	/**
	 * Load the P2 repositories. Initially the method verifies if the
	 * repositories were already instantiated and is on the metadata and arfacts
	 * maps (metadataRepositoriesMap, artifactRepositoriesMap). If yes, they
	 * should not be instantiated again. Otherwise, the repository and its
	 * references will be loaded.
	 * 
	 * If the global maps did not exist, every time that methods
	 * validateInstallation, listAvailableUpdates and downloadAndInstall were
	 * called, all the repositories would be loaded again.
	 */
	private IStatus loadRepositories(List<URI> links, IProgressMonitor monitor) {
		StudioLogger.debug(this, "loading repositories...");
		IStatus status = Status.OK_STATUS;

		StudioLogger.debug(this, "there are not loaded repositories");
		SubMonitor submonitor = SubMonitor.convert(monitor);

		int reposSize = 0;
		if (links != null) {
			reposSize += links.size();
		}

		submonitor.beginTask(InstallerNLS.AbstractConfigurationPage_LoadingRepositoriesTask, reposSize * 100);

		boolean isAllRepositoriesLoaded = isAllRepositoriesLoaded(links);

		if (!isAllRepositoriesLoaded) {
			P2RepositoriesFactory p2RepositoriesFactory = P2RepositoriesFactory.getInstance();
			for (URI uri : links) {
				IMetadataRepository metadataRepository = null;
				// loads metadata repositories from URIs
				try {
					metadataRepository = p2RepositoriesFactory.getMetadataRepository(uri, true,
							submonitor.newChild(100));
					mainRepositories.put(uri, metadataRepository);

				} catch (Exception e) {
					status = new Status(IStatus.WARNING, InstallerPlugin.PLUGIN_ID, e.getMessage());
					StudioLogger.error(this.getClass(), "could not instantiate repository from URI " + uri);
				}

			}
		} else {
			submonitor.done();
		}

		return status;
	}

	/**
	 * Loads InstallableItems based on category and links. They are put into the
	 * collection listToFill
	 * 
	 * @param listToFill
	 * @param links
	 * @param category
	 * @param monitor
	 * @return
	 * @throws InstallerException
	 */
	public IStatus listAllAvailableInstallItems(Collection<InstallableItem> listToFill, List<URI> uriList,
			CATEGORY category, IProgressMonitor monitor) throws InstallerException {

		StudioLogger.debug(this, "listing available installable items...");
		IStatus status = new Status(IStatus.ERROR, InstallerPlugin.PLUGIN_ID, 0,
				InstallerNLS.P2Installer_Could_Not_Find_Proper_Backend, null);

		// this links list is created because the method loadRepositories remove
		// some items of the list
		// if the repositorie is already loaded.
		List<URI> links = new ArrayList<URI>();
		links.addAll(uriList);

		SubMonitor submonitor = SubMonitor.convert(monitor);
		submonitor.beginTask(InstallerNLS.P2Installer_Loading_Repositories, 100);

		status = loadRepositories(links, submonitor.newChild(20));

		Collection<IInstallableUnit> units = new HashSet<IInstallableUnit>();

		IQuery<IInstallableUnit> query = null;

		// category is used to create the correct query
		switch (category) {
		case LANG_PACKS: {
			// Filter IUs in order to receive only lang packs
			query = QueryUtil.createMatchQuery(LANGUAGE_PACK_QUERY);
			break;
		}

		case NDK: {
			// Filter IUs in order to receive only NDK related
			query = QueryUtil.createIUQuery(NKD_QUERY);
			break;
		}

		case UPDATE_STUDIO: {
			// No special query needed
			break;
		}

		case OTHER_COMPONENTS: {
			Collection<IQuery<IInstallableUnit>> queries = new ArrayList<IQuery<IInstallableUnit>>();
			queries.add(QueryUtil.createIUQuery(SUBVERSION_QUERY));
			queries.add(QueryUtil.createIUQuery(MYLYN_QUERY1));
			queries.add(QueryUtil.createIUQuery(MYLYN_QUERY2));
			queries.add(QueryUtil.createIUQuery(CVS_QUERY));
			queries.add(QueryUtil.createIUQuery(EGIT_QUERY));

			query = QueryUtil.createCompoundQuery(queries, false);
			break;
		}
		default: {
			// No specific query to use as filter, download them all!
			break;
		}
		}

		int monitorWorkSize = 0;
		try {
			monitorWorkSize = 40 / mainRepositories.values().size();
		} catch (ArithmeticException e) {
			// Do nothing
		}
		for (Iterator<IMetadataRepository> iterator = mainRepositories.values().iterator(); iterator.hasNext();) {
			IMetadataRepository repository = iterator.next();
			try {

				Collection<IInstallableUnit> ius = P2Utilities.getInstallableUnits(repository, query,
						submonitor.newChild(monitorWorkSize));
				units.addAll(ius);

				status = Status.OK_STATUS;
			} catch (InstallerException e) {
				StudioLogger.error(this.getClass(), "could not retrieve installable units");
				status = new Status(IStatus.ERROR, InstallerPlugin.PLUGIN_ID, 0,
						"Error retrieving available installable units", null);

			}
		}

		monitorWorkSize = 0;
		try {
			monitorWorkSize = 40 / units.size();
		} catch (ArithmeticException e) {
			// Do nothing
		}
		for (Iterator<IInstallableUnit> iterator = units.iterator(); iterator.hasNext();) {
			IInstallableUnit iInstallableUnit = iterator.next();

			if (P2Utilities.isGroup(iInstallableUnit)) {
				// I'm only returning groups since only groups
				// are listed for the user to select what to install
				InstallableItem item = iu2InstallableItem(iInstallableUnit, monitor);
				listToFill.add(item);
			}

		}

		if (status.getMessage().equals("org.eclipse.core.runtime.OperationCanceledException")) {
			StudioLogger.debug(this, "operation was canceled");
			status = new Status(IStatus.CANCEL, status.getPlugin(), status.getCode(), status.getMessage(),
					status.getException());
		}

		submonitor.done();

		return status;
	}

	// Translates a P2 installable unit to a InstallableItem object
	private InstallableItem iu2InstallableItem(IInstallableUnit unit, IProgressMonitor monitor)
			throws InstallerException {
		InstallableItem item = new P2InstallableItem();
		item.setData(unit);
		item.setBundleID(unit.getId());
		item.setInstalled(P2Utilities.isInstalled(unit, monitor));
		item.setLicense(P2Utilities.getLicenseText(unit));
		item.setDisplayName(P2Utilities.getIUExternalizedValue(unit, IInstallableUnit.PROP_NAME));
		item.setDescription(P2Utilities.getIUExternalizedValue(unit, IInstallableUnit.PROP_DESCRIPTION));
		item.setProvider(P2Utilities.getIUExternalizedValue(unit, IInstallableUnit.PROP_PROVIDER));
		item.setRequirementsIds(P2Utilities.getRequirements(unit));
		return item;
	}

	/**
	 * Method used to install the installable items. The repositories and
	 * installOp should be already loaded, otherwise they will be.
	 * 
	 * @param links
	 * @param itemsToDownloadAndInstall
	 * @param monitor
	 * @return
	 */
	public IStatus downloadAndInstall(List<URI> links, Collection<InstallableItem> itemsToDownloadAndInstall,
			IProgressMonitor monitor) {
		StudioLogger.debug(this, "downloadAndInstall: installing selected installable items");
		IStatus status = new Status(IStatus.ERROR, InstallerPlugin.PLUGIN_ID, 0,
				InstallerNLS.P2Installer_Could_Not_Install_Selected_Items, null);

		if ((itemsToDownloadAndInstall != null) && (!itemsToDownloadAndInstall.isEmpty())) {

			final List<IInstallableUnit> installableUnits = new ArrayList<IInstallableUnit>();

			for (InstallableItem item : itemsToDownloadAndInstall) {
				IInstallableUnit unit = (IInstallableUnit) item.getData();
				installableUnits.add(unit);
			}

			try {
				if (installOp == null) {
					status = loadRepositories(links, monitor);

					status = P2Utilities.installIu(installableUnits, mainRepositories.values(), monitor);
				} else {
					status = P2Utilities.installIu(installableUnits, installOp, monitor);
				}
			} catch (InstallerException e) {
				StudioLogger.error(this.getClass(), "could not install selected installable unit");
				status = new Status(IStatus.WARNING, InstallerPlugin.PLUGIN_ID, e.getMessage());
			}

			// clean installOp and maps. After the installation has occurred,
			// they can be cleaned.
			installOp = null;
			mainRepositories.clear();

			return status;
		}
		return status;
	}

	/**
	 * Updates studio. IMPORTANT: the method listAllAvailableUpdates MUST be
	 * called first
	 * 
	 * @param monitor
	 * @return
	 */
	public IStatus updateStudio(IProgressMonitor monitor) {
		StudioLogger.debug(this, "updateStudio: installing selected installable items");
		IStatus status = new Status(IStatus.ERROR, InstallerPlugin.PLUGIN_ID, 0,
				InstallerNLS.P2Installer_Could_Not_Install_Selected_Items, null);

		try {
			status = P2Utilities.updateIu(up, monitor);

		} catch (Exception e) {
			StudioLogger.error(this.getClass(), "could not install selected installable unit.", e);
			status = new Status(IStatus.WARNING, InstallerPlugin.PLUGIN_ID, e.getMessage());
		}

		return status;
	}

	/**
	 * @param itemsToDownloadAndInstall
	 * @param backEnd
	 * @param monitor
	 * @return
	 */
	public IStatus validateInstallation(List<URI> links, Collection<InstallableItem> itemsToDownloadAndInstall,
			BACKEND backEnd, IProgressMonitor monitor) {
		// installOp will be loaded as global variable because will be used on
		// method downloadAndInstall.
		// It must not be instantiated twice if it were already validated.
		installOp = null;

		List<URI> allURIs = new ArrayList<URI>(mainRepositories.keySet());
		for (Iterator<URI> iterator = allURIs.iterator(); iterator.hasNext();) {
			URI uri = iterator.next();
			if (!links.contains(uri)) {
				mainRepositories.remove(uri);
			}

		}

		Collection<IInstallableUnit> temp = new HashSet<IInstallableUnit>();

		loadRepositories(links, monitor);

		for (Iterator<InstallableItem> iterator = itemsToDownloadAndInstall.iterator(); iterator.hasNext();) {
			InstallableItem iInstallableItem = iterator.next();
			temp.add((IInstallableUnit) iInstallableItem.getData());

		}
		if ((itemsToDownloadAndInstall != null) && (itemsToDownloadAndInstall.size() > 0)) {
			try {
				installOp = P2Utilities.getInstallOperation(temp, mainRepositories.values(), monitor);
			} catch (InstallerException e) {
				StudioLogger.error(this.getClass(), "Could not retrieve install operation");

				return new Status(IStatus.ERROR, InstallerPlugin.PLUGIN_ID, 0, e.getMessage(), null);

			}

		}

		return installOp != null ? installOp.getResolutionResult() : Status.CANCEL_STATUS;

	}

	/**
	 * Lists all available updates given a link. After this method the method
	 * updateStudio can be invoked
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
			BACKEND backEnd, IProgressMonitor monitor) throws InstallerException {
		IStatus result = Status.OK_STATUS;

		try {
			up = P2Utilities.getUpdateOperation(links, monitor);
			if (up != null) {

				result = up.getResolutionResult();
				Update[] updates = up.getSelectedUpdates();

				if (listToFill != null) {
					for (int i = 0; i < updates.length; i++) {
						InstallableItem item = iu2InstallableItem(updates[i].replacement, monitor);
						listToFill.add(item);
					}
				}
			} else {
				result = Status.CANCEL_STATUS;
			}
		} catch (Exception e) {
			StudioLogger.error(this.getClass(), "Error looking for updates. ", e);
			result = new Status(IStatus.ERROR, InstallerPlugin.PLUGIN_ID, 0, e.getMessage(), null);
		}

		return result;
	}

}
