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
import java.util.Iterator;
import java.util.List;

import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.installer.InstallerException;
import org.eclipse.andmore.android.installer.InstallerPlugin;
import org.eclipse.andmore.android.installer.i18n.InstallerNLS;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.ILicense;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.operations.ProfileModificationJob;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Utilities class with P2 auxiliary methods
 */
class P2Utilities {
	/**
	 * Verify if a given IInstallableUnit is installed on the system (iterates
	 * over all known profiles)
	 * 
	 * @param iu
	 *            IInstallableUnit to be checked
	 * @return true if iu is installed, false otherwise.
	 * @throws InstallerException
	 */
	protected static boolean isInstalled(IInstallableUnit iu, IProgressMonitor progressMonitor)
			throws InstallerException {
		return isInstalled(iu.getId(), null, progressMonitor);
	}

	/**
	 * Verify if a InstallableUnit with the given id and version is installed on
	 * the system (iterates over all known profiles)
	 * 
	 * @param iuId
	 *            id of the InstallableUnit to be checked
	 * @param version
	 *            version of the wanted installableUnit
	 * @return true if iu is installed, false otherwise.
	 * @throws InstallerException
	 */
	protected static boolean isInstalled(String iuId, Version version, IProgressMonitor progressMonitor)
			throws InstallerException {
		IQuery<IInstallableUnit> query = QueryUtil.createIUQuery(iuId, version);
		return isInstalled(query, progressMonitor);
	}

	/**
	 * Execute a query on every profile found, looking for InstallableUnits
	 * 
	 * @param query
	 *            IQuery<IInstallableUnit> to be executed.
	 * @return true if query results are available, false otherwise.
	 * @throws InstallerException
	 */
	protected static boolean isInstalled(IQuery<IInstallableUnit> query, IProgressMonitor progressMonitor)
			throws InstallerException {
		IProvisioningAgent agent = getProvisioningAgent();
		IProfileRegistry profileRegistry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
		IProfile[] profiles = profileRegistry.getProfiles();
		SubMonitor subMonitor = SubMonitor.convert(progressMonitor, profiles.length * 100);
		for (IProfile profile : profiles) {
			IQueryResult<IInstallableUnit> available = profile.query(query, subMonitor.newChild(100));
			if (!available.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get the IProvisioningAgent for the current context.
	 * 
	 * @return
	 * @throws InstallerException
	 */
	private static IProvisioningAgent getProvisioningAgent() throws InstallerException {
		BundleContext context = InstallerPlugin.getContext();
		IProvisioningAgent agent = getProvisioningAgent(context);
		return agent;
	}

	/**
	 * Loads every IInstallableUnit that matches the criteria specified by
	 * filter on a given IMetadataRepository
	 * 
	 * @param metadatarepository
	 *            Repository containing the IUs
	 * @param query
	 *            criteria to be used to filter the InstallableUnits on the
	 *            given MetadataRepository, if null them all IUs available are
	 *            returned
	 * @param progressMonitor
	 * @return collection containing all IInstallableUnits from the given
	 *         repository.
	 * @throws InstallerException
	 */
	protected static Collection<IInstallableUnit> getInstallableUnits(IMetadataRepository metadataRepository,
			IQuery<IInstallableUnit> query, IProgressMonitor progressMonitor) throws InstallerException {
		SubMonitor subMonitor = SubMonitor.convert(progressMonitor);
		subMonitor.beginTask(InstallerNLS.P2Utilities_LoadingUnits, 100);
		Collection<IInstallableUnit> installableUnits;
		installableUnits = null;
		if (metadataRepository != null) {
			if (query == null) {
				query = QueryUtil.createIUAnyQuery();
			}
			IQueryResult<IInstallableUnit> queryResult = metadataRepository.query(query, subMonitor.newChild(100));
			installableUnits = queryResult.toSet();
		}

		return installableUnits;
	}

	/**
	 * Retrieves the IMetadataRepository object based on the repository URI
	 * 
	 * @param metadatarepoUri
	 *            URI pointing to the MetadataRepository
	 * @param progressMonitor
	 * @return the IMetadataRepository object, if there's no repository on the
	 *         given URI null will be returned.
	 */
	protected static IMetadataRepository getMetadataRepository(URI metadatarepoUri, IProgressMonitor progressMonitor)
			throws InstallerException, OperationCanceledException {
		SubMonitor subMonitor = SubMonitor.convert(progressMonitor);
		subMonitor.beginTask(InstallerNLS.P2Utilities_Preparing, 100);
		IMetadataRepository metadataRepository = null;

		IProvisioningAgent agent = getProvisioningAgent();
		subMonitor.worked(10);
		IMetadataRepositoryManager metadataRepositoryManagermanager = (IMetadataRepositoryManager) agent
				.getService(IMetadataRepositoryManager.SERVICE_NAME);
		subMonitor.worked(15);

		try {
			metadataRepository = metadataRepositoryManagermanager.loadRepository(metadatarepoUri,
					subMonitor.newChild(75));
		} catch (ProvisionException e) {
			InstallerException installerException;

			if (e.getStatus().getCode() == ProvisionException.REPOSITORY_FAILED_AUTHENTICATION) {
				installerException = new InstallerException(InstallerNLS.P2Utilities_AuthenticationFailed);
			} else {
				installerException = new InstallerException(e);
			}

			throw installerException;
		}

		return metadataRepository;
	}

	/**
	 * Retrieves the IArtifactRepository object based on the repository URI
	 * 
	 * @param artifactRepoUri
	 *            URI pointing to the MetadataRepository
	 * @param progressMonitor
	 * @return the IArtifactRepository object, if there's no repository on the
	 *         given URI null will be returned.
	 */
	protected static IArtifactRepository getArtifactRepository(URI artifactRepoUri, IProgressMonitor progressMonitor)
			throws InstallerException {
		SubMonitor subMonitor = SubMonitor.convert(progressMonitor, 175);
		subMonitor.beginTask(InstallerNLS.P2Utilities_Preparing, 25);

		IArtifactRepository artifactRepository = null;

		IProvisioningAgent agent = getProvisioningAgent();
		subMonitor.worked(10);

		IArtifactRepositoryManager artifactRepositoryManagermanager = (IArtifactRepositoryManager) agent
				.getService(IArtifactRepositoryManager.SERVICE_NAME);
		subMonitor.worked(15);

		try {
			artifactRepository = artifactRepositoryManagermanager.loadRepository(artifactRepoUri,
					subMonitor.newChild(150));
		} catch (ProvisionException e) {
			throw new InstallerException(e);
		} catch (OperationCanceledException e) {
			throw new InstallerException(e);
		}

		return artifactRepository;
	}

	/**
	 * Retrieves the InstallOperation that is necessary to install the given IUS
	 * on the current Profile.
	 * 
	 * @param installableUnits
	 *            to be installed.
	 * @param metadataRepositories
	 *            metadataRepositories to be used
	 * @param artifactRepositories
	 *            artifactRepositories to be used
	 * @param progressMonitor
	 * @return the InstallOperation
	 * @throws InstallerException
	 *             if any error occurs
	 */
	protected static InstallOperation getInstallOperation(Collection<IInstallableUnit> installableUnits,
			Collection<IMetadataRepository> metadataRepositories, IProgressMonitor progressMonitor)
			throws InstallerException {
		SubMonitor subMonitor = SubMonitor.convert(progressMonitor);
		subMonitor.beginTask(InstallerNLS.P2Utilities_PreparingEnvironment, 200);

		final ProvisioningUI provisioningUI = ProvisioningUI.getDefaultUI();

		ProvisioningSession session = provisioningUI.getSession();

		subMonitor.worked(50);

		final InstallOperation op = new InstallOperation(session, installableUnits);
		if (metadataRepositories != null) {
			List<URI> metadataRepositoriesURIs = new ArrayList<URI>();
			List<URI> artifactRepositoriesURIs = new ArrayList<URI>();
			for (IMetadataRepository repository : metadataRepositories) {
				metadataRepositoriesURIs.add(repository.getLocation());
				artifactRepositoriesURIs.add(repository.getLocation());
				Collection<IRepositoryReference> references = repository.getReferences();
				for (IRepositoryReference reference : references) {
					if ((reference.getOptions() == IRepository.ENABLED)
							&& (reference.getType() == IRepository.TYPE_METADATA)) {
						metadataRepositoriesURIs.add(reference.getLocation());
					} else if ((reference.getOptions() == IRepository.ENABLED)
							&& (reference.getType() == IRepository.TYPE_ARTIFACT)) {
						artifactRepositoriesURIs.add(reference.getLocation());
					}
				}
			}
			op.getProvisioningContext().setMetadataRepositories(metadataRepositoriesURIs.toArray(new URI[0]));
			op.getProvisioningContext().setArtifactRepositories(artifactRepositoriesURIs.toArray(new URI[0]));
		}

		subMonitor.worked(50);
		op.resolveModal(subMonitor.newChild(80));
		final boolean[] canContinue = new boolean[] { true };
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				canContinue[0] = provisioningUI.getPolicy().continueWorkingWithOperation(op,
						PlatformUI.getWorkbench().getModalDialogShellProvider().getShell());
			}
		});
		subMonitor.done();
		return canContinue[0] ? op : null;
	}

	/**
	 * Installs the given InstallableUnits, from the given repositories, on the
	 * current profile.
	 * 
	 * @param installableUnits
	 *            to be installed
	 * @param metadataRepositories
	 *            metadataRepositories to be used
	 * @param artifactRepositories
	 *            artifactRepositories to be used
	 * @param progressMonitor
	 * @return
	 * @throws InstallerException
	 */
	protected static IStatus installIu(Collection<IInstallableUnit> installableUnits,
			InstallOperation installOperation, IProgressMonitor progressMonitor) throws InstallerException {
		IStatus result = installOperation.getResolutionResult();

		if (installOperation.hasResolved() && installOperation.getResolutionResult().isOK()) {
			ProvisioningJob provisioningJob = installOperation.getProvisioningJob(progressMonitor);

			if (provisioningJob instanceof ProfileModificationJob) {
				((ProfileModificationJob) provisioningJob).setRestartPolicy(ProvisioningJob.RESTART_NONE);
			}

			ProvisioningUI.getDefaultUI().manageJob(provisioningJob, ProvisioningJob.RESTART_NONE);
			provisioningJob.schedule();

			try {
				provisioningJob.join();
				result = provisioningJob.getResult();
				if (result.getSeverity() == IStatus.ERROR) {
					String installableItensNames = "";
					for (IInstallableUnit iu : installableUnits) {
						if (installableItensNames.equals("")) {
							installableItensNames = P2Utilities.getIUExternalizedValue(iu, IInstallableUnit.PROP_NAME);
						} else {
							installableItensNames = installableItensNames + ", "
									+ P2Utilities.getIUExternalizedValue(iu, IInstallableUnit.PROP_NAME);
						}
					}
					result = new Status(IStatus.ERROR, InstallerPlugin.PLUGIN_ID, "Some components ("
							+ installableItensNames + ") could not be downloaded.");
				}
			} catch (InterruptedException e) {
				StudioLogger.error("Error while trying to launch p2 job");
				result = new Status(IStatus.ERROR, InstallerPlugin.PLUGIN_ID, 0,
						InstallerNLS.P2Utilities_ErrorWhileLaunchingP2Job, null);
			}

		}

		return result;
	}

	/**
	 * Installs the given InstallableUnits, from the given repositories, on the
	 * current profile.
	 * 
	 * @param installableUnits
	 *            to be installed
	 * @param metadataRepositories
	 *            metadataRepositories to be used
	 * @param artifactRepositories
	 *            artifactRepositories to be used
	 * @param progressMonitor
	 * @return
	 * @throws InstallerException
	 */
	protected static IStatus installIu(Collection<IInstallableUnit> installableUnits,
			Collection<IMetadataRepository> metadataRepositories, IProgressMonitor progressMonitor)
			throws InstallerException {
		final InstallOperation op = getInstallOperation(installableUnits, metadataRepositories, progressMonitor);
		IStatus result = null;
		if (op != null) {

			result = op.getResolutionResult();

			if (op.hasResolved() && op.getResolutionResult().isOK()) {
				ProvisioningUI defaultUI = ProvisioningUI.getDefaultUI();
				ProvisioningJob provisioningJob = op.getProvisioningJob(progressMonitor);

				if (provisioningJob instanceof ProfileModificationJob) {
					((ProfileModificationJob) provisioningJob).setRestartPolicy(ProvisioningJob.RESTART_NONE);
				}

				defaultUI.manageJob(provisioningJob, ProvisioningJob.RESTART_NONE);
				provisioningJob.schedule();

				try {
					provisioningJob.join();
					result = provisioningJob.getResult();
					if (result.getSeverity() == IStatus.ERROR) {
						String installableItensNames = "";
						for (IInstallableUnit iu : installableUnits) {
							if (installableItensNames.equals("")) {
								installableItensNames = P2Utilities.getIUExternalizedValue(iu,
										IInstallableUnit.PROP_NAME);
							} else {
								installableItensNames = installableItensNames + ", "
										+ P2Utilities.getIUExternalizedValue(iu, IInstallableUnit.PROP_NAME);
							}
						}
						result = new Status(IStatus.ERROR, InstallerPlugin.PLUGIN_ID, "Some components ("
								+ installableItensNames + ") could not be downloaded.");
					}
				} catch (InterruptedException e) {
					StudioLogger.error("Error while trying to launch p2 job");
					result = new Status(IStatus.ERROR, InstallerPlugin.PLUGIN_ID, 0,
							InstallerNLS.P2Utilities_ErrorWhileLaunchingP2Job, null);
				}

			}
		} else {
			result = Status.CANCEL_STATUS;
		}

		return result;
	}

	/**
	 * Retrieves a provisioning agent for the given context.
	 * 
	 * @param context
	 * @return the IprovisioningAgent
	 * @throws InstallerException
	 */
	protected static IProvisioningAgent getProvisioningAgent(BundleContext context) throws InstallerException {
		ServiceReference<?> agentProviderRef = context.getServiceReference(IProvisioningAgentProvider.SERVICE_NAME);
		IProvisioningAgentProvider agentProvider = null;
		if (agentProviderRef != null) {
			agentProvider = (IProvisioningAgentProvider) context.getService(agentProviderRef);
		}
		IProvisioningAgent agent = null;
		try {
			agent = agentProvider.createAgent(null);
		} catch (ProvisionException e) {
			throw new InstallerException(e);
		}
		return agent;
	}

	/**
	 * Get the possible localized iu name. Return the iu property name if none
	 * translatable name was found
	 * 
	 * @param iu
	 * @return the name of the IU
	 */
	protected static String getIUExternalizedValue(IInstallableUnit iu, String property) {
		String iuNameProperty = iu.getProperty(property, null);
		String iuName = iuNameProperty;

		if (iuNameProperty == null) {
			String iuNameTemp = null;
			String currentLang = Platform.getNL();
			if (currentLang.contains("_")) {
				currentLang = currentLang.split("_")[0];
				iuNameTemp = iu.getProperty(currentLang + "." + iuNameProperty);
			}

			if (iuNameTemp == null) {
				iuNameTemp = iu.getProperty("df_LT." + iuNameProperty);
			}

			if (iuNameTemp != null) {
				iuName = iuNameTemp;
			} else {
				iuName = iu.getProperty(property);
			}

		}

		return iuName;
	}

	/**
	 * Retrieves a collection containing all the licences ILicense given an
	 * Installable Unit
	 * 
	 * @param iu
	 * @return
	 */
	protected static Collection<ILicense> getLicenses(IInstallableUnit iu) {
		Collection<ILicense> licenses = iu.getLicenses(null);

		if (licenses.isEmpty()) {
			String currentLang = Platform.getNL();
			if (currentLang.contains("_")) {
				currentLang = currentLang.split("_")[0];
				licenses = iu.getLicenses(currentLang);
			}

			if (licenses.isEmpty()) {
				licenses = iu.getLicenses("df_LT");
			}

			if (licenses.isEmpty()) {
				licenses = iu.getLicenses();
			}
		}

		return licenses;
	}

	/**
	 * Check if the installable unit is a group IU
	 * 
	 * @param unit
	 * @return true if it contains the group property, false otherwise
	 */
	protected static boolean isGroup(IInstallableUnit unit) {
		return unit.getProperty(QueryUtil.PROP_TYPE_GROUP) != null ? unit.getProperty(QueryUtil.PROP_TYPE_GROUP)
				.equals("true") : false; //$NON-NLS-1$
	}

	/**
	 * Given a Installable Unit, retrieves a list of the requirements
	 * descriptions.
	 * 
	 * @param iu
	 * @return
	 */
	static List<String> getRequirements(IInstallableUnit iu) {
		List<String> results = new ArrayList<String>();

		Collection<IRequirement> requirements = iu.getRequirements();
		for (Iterator<IRequirement> iterator = requirements.iterator(); iterator.hasNext();) {
			IRequirement iRequirement = iterator.next();
			results.add(iRequirement.getDescription());
		}
		return results;
	}

	/**
	 * Mounts a only string with the all the licenses texts. Uses the '\n' char
	 * for new lines between licenses
	 * 
	 * @param iu
	 * @return
	 */
	static String getLicenseText(IInstallableUnit iu) {
		StringBuffer buffer = new StringBuffer();
		if (iu != null) {
			Collection<ILicense> licenses = P2Utilities.getLicenses(iu);
			for (ILicense license : licenses) {
				buffer.append(license.getBody());
				buffer.append("\n\n\n"); //$NON-NLS-1$
			}
		}
		return buffer.toString();
	}

	/**
	 * Retrieves the UpdateOperation that is necessary to update available IUS
	 * on the current Profile.
	 * 
	 * @param repositories
	 *            where the update should be searched
	 * @param progressMonitor
	 * @return
	 * @throws InstallerException
	 */
	protected static UpdateOperation getUpdateOperation(Collection<URI> repositories, IProgressMonitor progressMonitor)
			throws InstallerException {

		final ProvisioningUI provisioningUI = ProvisioningUI.getDefaultUI();
		ProvisioningSession session = provisioningUI.getSession();

		final UpdateOperation op = new UpdateOperation(session);

		final boolean[] canContinue = new boolean[] { false };

		ArrayList<IRepositoryReference> references = new ArrayList<IRepositoryReference>();
		ArrayList<URI> metadataRepositories = new ArrayList<URI>();
		ArrayList<URI> artifactRepositories = new ArrayList<URI>();
		IMetadataRepository repository = null;

		for (Iterator<URI> iterator = repositories.iterator(); iterator.hasNext();) {
			URI uri = iterator.next();

			try {
				repository = getMetadataRepository(uri, progressMonitor);
				metadataRepositories.add(uri);
			} catch (OperationCanceledException e) {
				progressMonitor.setCanceled(true);
			}
		}
		if (repository != null) {
			references.addAll(repository.getReferences());
		}

		for (IRepositoryReference reference : references) {
			if (reference.getOptions() == IRepository.ENABLED) {
				if (reference.getType() == IRepository.TYPE_METADATA) {
					metadataRepositories.add(reference.getLocation());
				} else {
					artifactRepositories.add(reference.getLocation());
				}
			}
		}

		if (!progressMonitor.isCanceled()) {

			SubMonitor subMonitor = SubMonitor.convert(progressMonitor);
			subMonitor.beginTask(InstallerNLS.P2Utilities_PreparingEnvironment, 200);

			op.getProvisioningContext().setMetadataRepositories(metadataRepositories.toArray(new URI[0]));

			op.getProvisioningContext().setArtifactRepositories(artifactRepositories.toArray(new URI[0]));

			subMonitor.worked(100);
			op.resolveModal(subMonitor.newChild(100));
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					canContinue[0] = provisioningUI.getPolicy().continueWorkingWithOperation(op,
							PlatformUI.getWorkbench().getModalDialogShellProvider().getShell());
				}
			});
			subMonitor.done();
		} else {
			canContinue[0] = false;
		}

		return canContinue[0] ? op : null;
	}

	/**
	 * Method which will receive an UpdateOperation and will start the update
	 * action.
	 * 
	 * @param up
	 * @param progressMonitor
	 * @return
	 * @throws InstallerException
	 */
	protected static IStatus updateIu(UpdateOperation up, IProgressMonitor progressMonitor) throws InstallerException {

		IStatus result = new Status(IStatus.ERROR, InstallerPlugin.PLUGIN_ID, 0,
				InstallerNLS.P2Utilities_ErrorDuringUpdate, null);

		if ((up != null) && up.hasResolved() && up.getResolutionResult().isOK()) {

			ProvisioningJob provisioningJob = up.getProvisioningJob(progressMonitor);

			if (provisioningJob instanceof ProfileModificationJob) {
				((ProfileModificationJob) provisioningJob).setRestartPolicy(ProvisioningJob.RESTART_NONE);
			}

			try {
				ProvisioningUI.getDefaultUI().manageJob(provisioningJob, ProvisioningJob.RESTART_NONE);
				provisioningJob.schedule();
			} catch (Exception e) {
				StudioLogger.error(P2Utilities.class, "updateIu error when schedulling Job. ", e);
			}

			try {
				provisioningJob.join();
				result = provisioningJob.getResult();
			} catch (InterruptedException e) {

				StudioLogger.error(P2Utilities.class, "Error while trying to launch p2 job.", e);
				result = new Status(IStatus.ERROR, InstallerPlugin.PLUGIN_ID, 0,
						InstallerNLS.P2Utilities_ErrorWhileLaunchingP2Job, null);
			}
		}

		if (!result.isOK()) {
			StudioLogger.error(
					P2Utilities.class,
					"updateIu exiting with status different from ok. " + result.toString() + " - "
							+ result.getMessage());
		}
		return result;
	}

}
