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
package org.eclipse.andmore.android.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.andmore.android.codeutils.CodeUtilsActivator;
import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.common.IAndroidConstants;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.log.UsageDataConstants;
import org.eclipse.andmore.android.manifest.AndroidProjectManifestFile;
import org.eclipse.andmore.android.model.java.ContentProviderClass;
import org.eclipse.andmore.android.model.manifest.AndroidManifestFile;
import org.eclipse.andmore.android.model.manifest.dom.ApplicationNode;
import org.eclipse.andmore.android.model.manifest.dom.IntentFilterNode;
import org.eclipse.andmore.android.model.manifest.dom.ManifestNode;
import org.eclipse.andmore.android.model.manifest.dom.ProviderNode;
import org.eclipse.andmore.android.model.manifest.dom.UsesPermissionNode;
import org.eclipse.andmore.android.model.resources.ResourceFile;
import org.eclipse.andmore.android.model.resources.types.ResourcesNode;
import org.eclipse.andmore.android.model.resources.types.StringNode;
import org.eclipse.andmore.android.model.resources.types.AbstractResourceNode.NodeType;
import org.eclipse.andmore.android.resources.AndroidProjectResources;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.osgi.util.NLS;

/**
 * ContentProvider Model
 */
public class ContentProvider extends BuildingBlockModel {
	private static final String PROVIDER_RESOURCE_LABEL_SUFFIX = "ContentProviderLabel";

	private final List<String> authoritiesList = new LinkedList<String>();

	private static final int MANIFEST_UPDATING_STEPS = 6;

	private static final int RESOURCES_UPDATING_STEPS = 3;

	/**
	 * Constructor for Content Provider.
	 */
	public ContentProvider() {
		super(IAndroidConstants.CLASS_CONTENTPROVIDER);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.BuildingBlockModel#getStatus()
	 */
	@Override
	public IStatus getStatus() {
		return getMostSevere(new IStatus[] { super.getStatus(), getAuthorityStatus() });
	}

	/**
	 * Return Authority Status
	 * 
	 * @see #getStatus()
	 * @return
	 */
	private IStatus getAuthorityStatus() {
		IStatus status = new Status(IStatus.OK, CodeUtilsActivator.PLUGIN_ID, null);
		if (authoritiesList.isEmpty()) {
			status = new Status(IStatus.ERROR, CodeUtilsActivator.PLUGIN_ID,
					CodeUtilsNLS.ERR_ContentProvider_InvalidAuthoritySelection);
		}
		return status;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.IWizardModel#needMoreInformation()
	 */
	@Override
	public boolean needMoreInformation() {
		return false;
	}

	/**
	 * Create the content provider class and add it to the manifest file.
	 * 
	 * @return True if the content provider class was successfully create and
	 *         added to the manifest file. Otherwise, return false.
	 * */
	@Override
	public boolean save(IWizardContainer container, IProgressMonitor monitor) throws AndroidException {
		boolean classCreated = createProviderClass(monitor);
		boolean addedOnManifest = false;

		if (classCreated) {
			addedOnManifest = createProviderOnManifest(monitor);
		}

		// Logs all permissions used in UDC log
		saveUsedPermissions(container, monitor);
		return classCreated && addedOnManifest;

	}

	/*
	 * Logs to UDC all permissions used
	 */
	private boolean saveUsedPermissions(IWizardContainer container, IProgressMonitor monitor) throws AndroidException {

		StringBuffer permissionList = new StringBuffer("Added building block permissions: ");
		int selectedPermissionsSize = getIntentFilterPermissionsAsArray().length;

		for (int i = 0; i < selectedPermissionsSize; i++) {

			String permission = getIntentFilterPermissionsAsArray()[i];
			permissionList.append(permission);

			if (i < (selectedPermissionsSize - 1)) {
				permissionList.append(", ");
			}
		}

		if (selectedPermissionsSize > 0) {

			// Logs to UDC the permissions selected
			StudioLogger.collectUsageData(UsageDataConstants.WHAT_BUILDINGBLOCK_PERMISSION,
					UsageDataConstants.KIND_BUILDINGBLOCK_PERMISSION, "permissions: " + permissionList.toString(), //$NON-NLS-1$
					CodeUtilsActivator.PLUGIN_ID, CodeUtilsActivator.getDefault().getBundle().getVersion().toString());
		}
		return true;
	}

	/**
	 * Return default authority.
	 * 
	 * @return
	 */
	public String getDefaultAuthority() {
		IPackageFragment packageFragment = getPackageFragment();
		String name = getName();
		String defaultAuthority = "";
		if ((name != null) && (packageFragment != null)) {
			defaultAuthority = packageFragment.getElementName() + "." + name.toLowerCase();
		}

		return defaultAuthority;
	}

	/**
	 * Remove Authority
	 * 
	 * @param authority
	 */
	public void removeAuthority(String authority) {
		authoritiesList.remove(authority);
	}

	/**
	 * Add Authority
	 * 
	 * @param authority
	 */
	public void addAuthority(String authority) {
		authoritiesList.add(authority);
	}

	/**
	 * Return Authorities
	 * 
	 * @return
	 */
	public List<String> getAuthoritiesList() {
		return authoritiesList;
	}

	/*
	 * Creates the Content Provider java class
	 * 
	 * @param monitor the progress monitor
	 * 
	 * @return true if the class has been created or false otherwise
	 * 
	 * @throws AndroidException
	 */
	private boolean createProviderClass(IProgressMonitor monitor) throws AndroidException {
		boolean created = false;
		String firstAuthority = "";

		monitor.subTask(CodeUtilsNLS.UI_ContentProvider_CreatingTheContentProviderJavaClass);

		for (String auth : getAuthoritiesList()) {
			firstAuthority = auth;
			break;
		}

		ContentProviderClass providerClass = new ContentProviderClass(getName(), getPackageFragment().getElementName(),
				firstAuthority);

		try {
			createJavaClassFile(providerClass, monitor);
			created = true;
		} catch (JavaModelException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_ContentProvider_CannotCreateTheContentProviderClass, getName(),
					e.getLocalizedMessage());

			throw new AndroidException(errMsg);
		} catch (AndroidException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_ContentProvider_CannotCreateTheContentProviderClass, getName(),
					e.getLocalizedMessage());
			throw new AndroidException(errMsg);
		}

		return created;
	}

	/*
	 * Creates the Content Provider class entry on AndroidManifest.xml file
	 * 
	 * @param monitor the progress monitor
	 * 
	 * @return true if the entry has been added or false otherwise
	 * 
	 * @throws AndroidException
	 */
	private boolean createProviderOnManifest(IProgressMonitor monitor) throws AndroidException {
		boolean created = false;

		try {
			int totalWork = MANIFEST_UPDATING_STEPS + RESOURCES_UPDATING_STEPS;

			monitor.beginTask("", totalWork);

			monitor.subTask(CodeUtilsNLS.UI_Common_UpdatingTheAndroidManifestXMLFile);

			AndroidManifestFile androidManifestFile = AndroidProjectManifestFile.getFromProject(getProject());

			monitor.worked(1);

			ManifestNode manifestNode = androidManifestFile != null ? androidManifestFile.getManifestNode() : null;
			ApplicationNode applicationNode = manifestNode != null ? manifestNode.getApplicationNode() : null;

			monitor.worked(1);

			if (applicationNode != null) {

				// Adds the added permission nodes to manifest file
				List<String> permissionsNames = new ArrayList<String>();
				for (UsesPermissionNode i : manifestNode.getUsesPermissionNodes()) {
					permissionsNames.add(i.getName());
				}

				for (String intentFilterPermission : getIntentFilterPermissionsAsArray()) {
					if (!permissionsNames.contains(intentFilterPermission)) {
						manifestNode.addChild(new UsesPermissionNode(intentFilterPermission));
					}
				}

				boolean providerExists = false;

				String classQualifier = (getPackageFragment().getElementName().equals(manifestNode.getPackageName()) ? ""
						: getPackageFragment().getElementName())
						+ ".";

				for (ProviderNode providerNode : applicationNode.getProviderNodes()) {
					if (providerNode.getName().equals(getName())) {
						providerExists = true;
						break;
					}
				}

				monitor.worked(1);

				String[] authorities = new String[getAuthoritiesList().size()];
				authorities = getAuthoritiesList().toArray(authorities);

				if (!providerExists) {
					ProviderNode providerNode = new ProviderNode(classQualifier + getName(), authorities[0]);

					String providerLabel = createProviderLabel(monitor);

					if (providerLabel != null) {
						providerNode.setLabel(AndroidProjectResources.STRING_CALL_PREFIX + providerLabel);
					}

					IntentFilterNode intentFilterNode = new IntentFilterNode();

					for (int i = 1; i < authorities.length; i++) {
						providerNode.addAuthority(authorities[i]);
					}

					if (intentFilterNode.getChildren().length > 0) {
						providerNode.addIntentFilterNode(intentFilterNode);
					}

					applicationNode.addProviderNode(providerNode);

					monitor.worked(1);

					monitor.subTask(CodeUtilsNLS.UI_Common_SavingTheAndroidManifestXMLFile);

					AndroidProjectManifestFile.saveToProject(getProject(), androidManifestFile, true);
					created = true;

					monitor.worked(1);
				}
			}
		} catch (AndroidException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_ContentProvider_CannotUpdateTheManifestFile, getName(),
					e.getLocalizedMessage());
			throw new AndroidException(errMsg);
		} catch (CoreException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_ContentProvider_CannotUpdateTheManifestFile, getName(),
					e.getLocalizedMessage());
			throw new AndroidException(errMsg);
		} finally {
			monitor.done();
		}

		return created;
	}

	/*
	 * Adds the Content Provider label value on the strings resource file
	 * 
	 * @param monitor the progress monitor
	 * 
	 * @return The label value if it has been added to the strings resource file
	 * or null otherwise
	 * 
	 * @throws AndroidException
	 */
	private String createProviderLabel(IProgressMonitor monitor) throws AndroidException {
		String resLabel = null;

		if ((getLabel() != null) && (getLabel().trim().length() > 0)) {
			try {
				monitor.subTask(CodeUtilsNLS.UI_Common_UpdatingTheStringsResourceFile);

				ResourceFile stringsFile = AndroidProjectResources.getResourceFile(getProject(), NodeType.String);

				monitor.worked(1);

				if (stringsFile.getResourcesNode() == null) {
					stringsFile.addResourceEntry(new ResourcesNode());
				}

				resLabel = stringsFile.getNewResourceName(getName() + PROVIDER_RESOURCE_LABEL_SUFFIX);

				StringNode strNode = new StringNode(resLabel);
				strNode.setNodeValue(getLabel());

				stringsFile.getResourcesNode().addChildNode(strNode);

				monitor.worked(1);

				AndroidProjectResources.saveResourceFile(getProject(), stringsFile, NodeType.String);

				monitor.worked(1);
			} catch (CoreException e) {
				String errMsg = NLS.bind(CodeUtilsNLS.EXC_ContentProvider_CannotCreateTheContentProviderLabel,
						e.getLocalizedMessage());
				throw new AndroidException(errMsg);
			} catch (AndroidException e) {
				String errMsg = NLS.bind(CodeUtilsNLS.EXC_ContentProvider_CannotCreateTheContentProviderLabel,
						e.getLocalizedMessage());
				throw new AndroidException(errMsg);
			}
		}

		return resLabel;
	}

	/**
	 * Check if contains authorities.
	 * 
	 * @param authority
	 * @return
	 */
	public boolean containsAuthority(String authority) {
		return authoritiesList.contains(authority);
	}
}
