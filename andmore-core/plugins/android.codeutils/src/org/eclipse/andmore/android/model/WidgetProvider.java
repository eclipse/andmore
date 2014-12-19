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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.android.codeutils.CodeUtilsActivator;
import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.log.UsageDataConstants;
import org.eclipse.andmore.android.common.utilities.FileUtil;
import org.eclipse.andmore.android.manifest.AndroidProjectManifestFile;
import org.eclipse.andmore.android.model.java.WidgetProviderClass;
import org.eclipse.andmore.android.model.manifest.AndroidManifestFile;
import org.eclipse.andmore.android.model.manifest.dom.ActionNode;
import org.eclipse.andmore.android.model.manifest.dom.ApplicationNode;
import org.eclipse.andmore.android.model.manifest.dom.IntentFilterNode;
import org.eclipse.andmore.android.model.manifest.dom.ManifestNode;
import org.eclipse.andmore.android.model.manifest.dom.MetadataNode;
import org.eclipse.andmore.android.model.manifest.dom.ReceiverNode;
import org.eclipse.andmore.android.model.manifest.dom.UsesPermissionNode;
import org.eclipse.andmore.android.model.resources.ResourceFile;
import org.eclipse.andmore.android.model.resources.types.ResourcesNode;
import org.eclipse.andmore.android.model.resources.types.StringNode;
import org.eclipse.andmore.android.model.resources.types.AbstractResourceNode.NodeType;
import org.eclipse.andmore.android.resources.AndroidProjectResources;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.osgi.util.NLS;

/**
 * Android WidgetProvider abstraction.
 */
public class WidgetProvider extends Launcher {

	/**
	 * Constant used to define the name of the super class of the widget
	 * provider building block. We can't use the AndroidConstansts class since
	 * there's no definition there for this yet.
	 */
	public static final String WIDGET_PROVIDER_SUPER_CLASS = "android.appwidget.AppWidgetProvider";

	// Constants for monitor progress
	private static final int MANIFEST_UPDATING_STEPS = 6;

	private static final int RESOURCES_UPDATING_STEPS = 3;

	private static final String COPY_WIDGET_TEMPLATES_TASK_NAME = "Copying widget template files.";

	// Metadata node name
	private static final String METADATA_NODE_NAME = "android.appwidget.provider";

	// Action node name
	private static final String ACTION_NODE_NAME = "android.appwidget.action.APPWIDGET_UPDATE";

	// Widget info xml file
	private static final String WIDGET_INFO_FILE_NAME = "widget_info";

	private static final String WIDGET_PROVIDER_RESOURCE_LABEL_SUFFIX = "WidgetProviderLabel";

	// Directory constants
	private static final String RES_DIR = "res/";

	private static final String XML_DIR = "xml/";

	private static final String LAYOUT_DIR = "layout/";

	private static final String DRAWABLE = "drawable";

	private static final String WIDGET_TEMPLATE_FOLDER = "templates/widget_project/";

	private static final String WIDGET_INITIAL_LAYOUT_XML = "widget_initial_layout.xml";

	private static final String WIDGET_INFO_XML = "widget_info.xml";

	private static final String ICON = "icon.png";

	private static final String WIDGET_ICON_PATH = "icons/obj16/plate16.png";

	/**
	 * Constructor
	 */
	public WidgetProvider() {
		super(WIDGET_PROVIDER_SUPER_CLASS);
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
	 * Create the wigdet class and add it to the manifest file.
	 * 
	 * @return True if the widget class was successfully create and added to the
	 *         manifest file. Otherwise, returns false.
	 * */
	@Override
	public boolean save(IWizardContainer container, IProgressMonitor monitor) throws AndroidException {
		boolean classCreated = createWidgetProviderClass(monitor);
		boolean addedOnManifest = false;
		boolean createdWidgetInfoFiles = false;

		if (classCreated) {
			addedOnManifest = createWidgetProviderOnManifest(monitor);

			// Logs to UDC the widget provider creation
			StudioLogger.collectUsageData(UsageDataConstants.WHAT_WIDGETPROVIDER_CREATED,
					UsageDataConstants.KIND_WIDGETPROVIDER, "", //$NON-NLS-1$
					CodeUtilsActivator.PLUGIN_ID, CodeUtilsActivator.getDefault().getBundle().getVersion().toString());

		}

		if (addedOnManifest) {
			createdWidgetInfoFiles = createWidgetInfoFiles(monitor);
		}

		// Logs all permissions used in UDC log
		super.save(container, monitor);

		return classCreated && addedOnManifest && createdWidgetInfoFiles;
	}

	/*
	 * Creates the WidgetProvider java class
	 * 
	 * @param monitor the progress monitor
	 * 
	 * @return true if the class has been created or false otherwise
	 * 
	 * @throws AndroidException
	 */
	private boolean createWidgetProviderClass(IProgressMonitor monitor) throws AndroidException {
		boolean created = false;

		monitor.subTask(CodeUtilsNLS.UI_WidgetProvider_CreatingTheWidgetProviderJavaClass);

		WidgetProviderClass widgetProviderClass = new WidgetProviderClass(getName(), getPackageFragment()
				.getElementName());

		try {
			createJavaClassFile(widgetProviderClass, monitor);
			created = true;
		} catch (JavaModelException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_Receiver_CannotCreateTheReceiverClass, getName(),
					e.getLocalizedMessage());

			throw new AndroidException(errMsg);
		} catch (AndroidException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_Receiver_CannotCreateTheReceiverClass, getName(),
					e.getLocalizedMessage());
			throw new AndroidException(errMsg);
		}

		return created;
	}

	/*
	 * Creates the Widget Provider class entry on AndroidManifest.xml file
	 * 
	 * @param monitor the progress monitor
	 * 
	 * @return true if the entry has been added or false otherwise
	 * 
	 * @throws AndroidException
	 */
	private boolean createWidgetProviderOnManifest(IProgressMonitor monitor) throws AndroidException {
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

				boolean widgetProviderExists = false;

				String classQualifier = (getPackageFragment().getElementName().equals(manifestNode.getPackageName()) ? ""
						: getPackageFragment().getElementName())
						+ ".";

				// Check if the building block already exists in the manifest
				// file
				for (ReceiverNode receiverNode : applicationNode.getReceiverNodes()) {
					if (receiverNode.getName().equals(getName())) {
						widgetProviderExists = true;
						break;
					}
				}

				monitor.worked(1);

				// Create the receiver node that declares the widget provider
				if (!widgetProviderExists) {
					ReceiverNode widgetProviderNode = new ReceiverNode(classQualifier + getName());

					String widgetProviderLabel = createWidgetProviderLabel(monitor);

					if (widgetProviderLabel != null) {
						widgetProviderNode.setLabel(AndroidProjectResources.STRING_CALL_PREFIX + widgetProviderLabel);
					}

					// Add a intent filter node with the correct action to the
					// receiver node
					IntentFilterNode intentFilterNode = new IntentFilterNode();
					ActionNode actionNode = new ActionNode(ACTION_NODE_NAME);
					intentFilterNode.addActionNode(actionNode);
					widgetProviderNode.addIntentFilterNode(intentFilterNode);

					// Add a metadada node to the receiver node
					MetadataNode metadataNode = new MetadataNode(METADATA_NODE_NAME);
					metadataNode.setResource(AndroidProjectResources.XML_CALL_PREFIX + WIDGET_INFO_FILE_NAME);
					widgetProviderNode.addMetadataNode(metadataNode);

					applicationNode.addReceiverNode(widgetProviderNode);

					monitor.worked(1);

					monitor.subTask(CodeUtilsNLS.UI_Common_SavingTheAndroidManifestXMLFile);

					AndroidProjectManifestFile.saveToProject(getProject(), androidManifestFile, true);

					created = true;

					monitor.worked(1);
				}
			}
		} catch (AndroidException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_Receiver_CannotUpdateTheManifestFile, getName(),
					e.getLocalizedMessage());
			throw new AndroidException(errMsg);
		} catch (CoreException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_Receiver_CannotUpdateTheManifestFile, getName(),
					e.getLocalizedMessage());
			throw new AndroidException(errMsg);
		} finally {
			monitor.done();
		}

		return created;
	}

	/*
	 * Adds the Widget label value on the strings resource file
	 * 
	 * @param monitor the progress monitor
	 * 
	 * @return The label value if it has been added to the strings resource file
	 * or null otherwise
	 * 
	 * @throws AndroidException
	 */
	private String createWidgetProviderLabel(IProgressMonitor monitor) throws AndroidException {
		String resLabel = null;

		if ((getLabel() != null) && (getLabel().trim().length() > 0)) {
			try {
				monitor.subTask(CodeUtilsNLS.UI_Common_UpdatingTheStringsResourceFile);

				ResourceFile stringsFile = AndroidProjectResources.getResourceFile(getProject(), NodeType.String);

				monitor.worked(1);

				if (stringsFile.getResourcesNode() == null) {
					stringsFile.addResourceEntry(new ResourcesNode());
				}

				resLabel = stringsFile.getNewResourceName(getName() + WIDGET_PROVIDER_RESOURCE_LABEL_SUFFIX);

				StringNode strNode = new StringNode(resLabel);
				strNode.setNodeValue(getLabel());

				stringsFile.getResourcesNode().addChildNode(strNode);

				monitor.worked(1);

				AndroidProjectResources.saveResourceFile(getProject(), stringsFile, NodeType.String);

				monitor.worked(1);
			} catch (CoreException e) {
				String errMsg = NLS.bind(CodeUtilsNLS.EXC_Activity_CannotCreateTheActivityLabel,
						e.getLocalizedMessage());
				throw new AndroidException(errMsg);
			} catch (AndroidException e) {
				String errMsg = NLS.bind(CodeUtilsNLS.EXC_Activity_CannotCreateTheActivityLabel,
						e.getLocalizedMessage());
				throw new AndroidException(errMsg);
			}
		}

		return resLabel;
	}

	private boolean createWidgetInfoFiles(IProgressMonitor monitor) throws AndroidException {
		boolean created = false;

		try {
			CodeUtilsActivator.getDefault();

			monitor.beginTask("", 100);

			monitor.subTask(COPY_WIDGET_TEMPLATES_TASK_NAME);

			monitor.worked(10);

			IFolder resFolder = getProject().getFolder(RES_DIR);

			// get project folders
			IResource[] resList = resFolder.members(IResource.FOLDER);

			// looks forward icon.png resource
			boolean iconExist = false;
			for (int i = 0; i < resList.length; i++) {
				// inside drawable folders
				if (resList[i].getName().indexOf(DRAWABLE) >= 0) {
					IFile iconFile = ((IFolder) resList[i]).getFile(ICON);

					if (iconFile.exists()) {
						iconExist = true;
						break;
					}
				}
			}
			// creates the icon if it does not exist
			if (!iconExist) {
				IFile imageFile = getProject().getFile(RES_DIR + IPath.SEPARATOR + DRAWABLE + IPath.SEPARATOR + ICON);

				URL imgUrl = CodeUtilsActivator.getDefault().getBundle().getEntry(WIDGET_ICON_PATH);

				if (imgUrl != null) {
					IFolder drawablefolder = getProject().getFolder(RES_DIR + DRAWABLE + IPath.SEPARATOR);

					// creates drawable folder if it does not exist
					if (!drawablefolder.exists()) {
						FileUtil.createProjectFolder(getProject(), RES_DIR, DRAWABLE + IPath.SEPARATOR, monitor);
					}
					imageFile.create(imgUrl.openStream(), IResource.NONE, new SubProgressMonitor(monitor, 1000));
				}
			}

			// Create an "xml" folder inside the "res" folder of the project
			FileUtil.createProjectFolder(getProject(), RES_DIR, XML_DIR, monitor);

			// Copy the "widget_info" file to the xml folder and the
			// "widget_initial_layout" file to the layout folder
			IFolder layoutfolder = getProject().getFolder(RES_DIR + LAYOUT_DIR);
			IFolder xmlFolder = getProject().getFolder(RES_DIR + XML_DIR);

			if (!layoutfolder.exists()) {
				FileUtil.createProjectFolder(getProject(), RES_DIR, LAYOUT_DIR + IPath.SEPARATOR, monitor);
			}

			if (!xmlFolder.exists()) {
				FileUtil.createProjectFolder(getProject(), RES_DIR, XML_DIR + IPath.SEPARATOR, monitor);
			}

			IFile initialLayoutFile = layoutfolder.getFile(WIDGET_INITIAL_LAYOUT_XML);
			if (!initialLayoutFile.exists()) {

				// Retrieve template file and create it at destination
				URL templateURL = CodeUtilsActivator.getDefault().getBundle()
						.getEntry(WIDGET_TEMPLATE_FOLDER + WIDGET_INITIAL_LAYOUT_XML);
				if (templateURL != null) {
					initialLayoutFile.create(templateURL.openStream(), false, monitor);
				} else {
					throw new AndroidException();
				}

				monitor.worked(20);

			}

			IFile widgetInfoFile = xmlFolder.getFile(WIDGET_INFO_XML);
			if (!widgetInfoFile.exists()) {
				// Retrieve template file and create it at destination
				URL templateURL = CodeUtilsActivator.getDefault().getBundle()
						.getEntry(WIDGET_TEMPLATE_FOLDER + WIDGET_INFO_XML);

				if (templateURL != null) {
					widgetInfoFile.create(templateURL.openStream(), false, monitor);
				} else {
					throw new AndroidException();
				}

				monitor.worked(20);
			}

			created = true;

		} catch (Exception e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_WidgetProvider_CannotCopyTemplateFiles, getName(),
					e.getLocalizedMessage());
			throw new AndroidException(errMsg);
		} finally {
			monitor.done();
		}

		return created;

	}
}
