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
import java.util.List;

import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.common.IAndroidConstants;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.manifest.AndroidProjectManifestFile;
import org.eclipse.andmore.android.model.java.ServiceClass;
import org.eclipse.andmore.android.model.manifest.AndroidManifestFile;
import org.eclipse.andmore.android.model.manifest.dom.ApplicationNode;
import org.eclipse.andmore.android.model.manifest.dom.IntentFilterNode;
import org.eclipse.andmore.android.model.manifest.dom.ManifestNode;
import org.eclipse.andmore.android.model.manifest.dom.ServiceNode;
import org.eclipse.andmore.android.model.manifest.dom.UsesPermissionNode;
import org.eclipse.andmore.android.model.resources.ResourceFile;
import org.eclipse.andmore.android.model.resources.types.ResourcesNode;
import org.eclipse.andmore.android.model.resources.types.StringNode;
import org.eclipse.andmore.android.model.resources.types.AbstractResourceNode.NodeType;
import org.eclipse.andmore.android.resources.AndroidProjectResources;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.osgi.util.NLS;

/**
 * Class that provides a service model to be used for the service wizard.
 */
public class Service extends Launcher {
	private static final String SERVICE_RESOURCE_LABEL_SUFFIX = "ServiceLabel"; //$NON-NLS-1$

	private static final int MANIFEST_UPDATING_STEPS = 6;

	private static final int RESOURCES_UPDATING_STEPS = 3;

	private boolean onStartMethod;

	private boolean onCreateMethod;

	/**
	 * Default constructor.
	 * */
	public Service() {
		super(IAndroidConstants.CLASS_SERVICE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.BuildingBlockModel#getStatus()
	 */
	@Override
	public IStatus getStatus() {
		return super.getStatus();
	}

	/**
	 * Create a new service class and add it to the manifest file.
	 * 
	 * @return True if the service class was successfully created and added to
	 *         the manifest file. Otherwise, returns false.
	 * */
	@Override
	public boolean save(IWizardContainer container, IProgressMonitor monitor) throws AndroidException {
		boolean classCreated = createServiceClass(monitor);
		boolean addedOnManifest = false;

		if (classCreated) {
			addedOnManifest = createServiceOnManifest(monitor);
		}

		// Logs all permissions used in UDC log
		super.save(container, monitor);

		return classCreated && addedOnManifest;
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

	/*
	 * Creates the Service java class
	 * 
	 * @param monitor the progress monitor
	 * 
	 * @return true if the class has been created or false otherwise
	 * 
	 * @throws AndroidException
	 */
	private boolean createServiceClass(IProgressMonitor monitor) throws AndroidException {
		boolean created = false;

		monitor.subTask(CodeUtilsNLS.UI_Service_CreatingTheServiceJavaClass);

		ServiceClass serviceClass = new ServiceClass(getName(), getPackageFragment().getElementName(), onCreateMethod,
				onStartMethod);

		try {
			createJavaClassFile(serviceClass, monitor);
			created = true;
		} catch (JavaModelException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_Service_CannotCreateTheServiceClass, getName(),
					e.getLocalizedMessage());

			throw new AndroidException(errMsg);
		} catch (AndroidException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_Service_CannotCreateTheServiceClass, getName(),
					e.getLocalizedMessage());
			throw new AndroidException(errMsg);
		}

		return created;
	}

	/*
	 * Creates the Service class entry on AndroidManifest.xml file
	 * 
	 * @param monitor the progress monitor
	 * 
	 * @return true if the entry has been added or false otherwise
	 * 
	 * @throws AndroidException
	 */
	private boolean createServiceOnManifest(IProgressMonitor monitor) throws AndroidException {
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

				boolean serviceExists = false;

				String classQualifier = (getPackageFragment().getElementName().equals(manifestNode.getPackageName()) ? "" : getPackageFragment() //$NON-NLS-1$
								.getElementName())
						+ "."; //$NON-NLS-1$

				for (ServiceNode serviceNode : applicationNode.getServiceNodes()) {
					if (serviceNode.getName().equals(getName())) {
						serviceExists = true;
						break;
					}
				}

				monitor.worked(1);

				if (!serviceExists) {
					ServiceNode serviceNode = new ServiceNode(classQualifier + getName());

					String serviceLabel = createServiceLabel(monitor);

					if (serviceLabel != null) {
						serviceNode.setLabel(AndroidProjectResources.STRING_CALL_PREFIX + serviceLabel);
					}

					IntentFilterNode intentFilterNode = new IntentFilterNode();

					if (intentFilterNode.getChildren().length > 0) {
						serviceNode.addIntentFilterNode(intentFilterNode);
					}

					applicationNode.addServiceNode(serviceNode);

					monitor.worked(1);

					monitor.subTask(CodeUtilsNLS.UI_Common_SavingTheAndroidManifestXMLFile);

					AndroidProjectManifestFile.saveToProject(getProject(), androidManifestFile, true);
					created = true;

					monitor.worked(1);
				}
			}
		} catch (AndroidException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_Service_CannotUpdateTheManifestFile, getName(),
					e.getLocalizedMessage());
			throw new AndroidException(errMsg);
		} catch (CoreException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_Service_CannotUpdateTheManifestFile, getName(),
					e.getLocalizedMessage());
			throw new AndroidException(errMsg);
		} finally {
			monitor.done();
		}

		return created;
	}

	/*
	 * Adds the Service label value on the strings resource file
	 * 
	 * @param monitor The progress monitor
	 * 
	 * @return The label value if it has been added to the strings resource file
	 * or null otherwise
	 * 
	 * @throws AndroidException
	 */
	private String createServiceLabel(IProgressMonitor monitor) throws AndroidException {
		String resLabel = null;

		if ((getLabel() != null) && (getLabel().trim().length() > 0)) {
			try {
				monitor.subTask(CodeUtilsNLS.UI_Common_UpdatingTheStringsResourceFile);

				ResourceFile stringsFile = AndroidProjectResources.getResourceFile(getProject(), NodeType.String);

				monitor.worked(1);

				if (stringsFile.getResourcesNode() == null) {
					stringsFile.addResourceEntry(new ResourcesNode());
				}

				resLabel = stringsFile.getNewResourceName(getName() + SERVICE_RESOURCE_LABEL_SUFFIX);

				StringNode strNode = new StringNode(resLabel);
				strNode.setNodeValue(getLabel());

				stringsFile.getResourcesNode().addChildNode(strNode);

				monitor.worked(1);

				AndroidProjectResources.saveResourceFile(getProject(), stringsFile, NodeType.String);

				monitor.worked(1);
			} catch (CoreException e) {
				String errMsg = NLS.bind(CodeUtilsNLS.EXC_Service_CannotCreateTheServiceLabel, e.getLocalizedMessage());
				throw new AndroidException(errMsg);
			} catch (AndroidException e) {
				String errMsg = NLS.bind(CodeUtilsNLS.EXC_Service_CannotCreateTheServiceLabel, e.getLocalizedMessage());
				throw new AndroidException(errMsg);
			}
		}

		return resLabel;
	}

	/**
	 * @return True if the default onStart() method of the service class should
	 *         be created. Otherwise, returns false.
	 */
	public boolean isOnStartMethod() {
		return onStartMethod;
	}

	/**
	 * @param onStartMethod
	 *            Set to true to have the default onStart() method of the
	 *            service class automatically created.
	 */
	public void setOnStartMethod(boolean onStartMethod) {
		this.onStartMethod = onStartMethod;
	}

	/**
	 * @return True if the default onCreate() method of the service class should
	 *         be created. Otherwise, returns false.
	 */
	public boolean isOnCreateMethod() {
		return onCreateMethod;
	}

	/**
	 * @param onStartMethod
	 *            Set to true to have the default onCreate() method of the
	 *            service class automatically created.
	 */
	public void setOnCreateMethod(boolean onCreateMethod) {
		this.onCreateMethod = onCreateMethod;
	}

	/**
	 * @return The default onStart() method signature including return value and
	 *         visibility level.
	 */
	public String getOnStartMessage() {
		final String ON_START_METHOD = "public void onStart(Intent intent, int startId)";

		return ON_START_METHOD;
	}

	/**
	 * @return The default onCreate() method signature including return value
	 *         and visibility level.
	 */
	public String getOnCreateMessage() {
		final String ON_CREATE_METHOD = "public void onCreate()";

		return ON_CREATE_METHOD;
	}
}
