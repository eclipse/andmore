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
import java.util.HashMap;
import java.util.List;

import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.common.IAndroidConstants;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.manifest.AndroidProjectManifestFile;
import org.eclipse.andmore.android.model.java.ActivityClass;
import org.eclipse.andmore.android.model.manifest.AndroidManifestFile;
import org.eclipse.andmore.android.model.manifest.dom.ActionNode;
import org.eclipse.andmore.android.model.manifest.dom.ActivityNode;
import org.eclipse.andmore.android.model.manifest.dom.ApplicationNode;
import org.eclipse.andmore.android.model.manifest.dom.CategoryNode;
import org.eclipse.andmore.android.model.manifest.dom.IntentFilterNode;
import org.eclipse.andmore.android.model.manifest.dom.ManifestNode;
import org.eclipse.andmore.android.model.manifest.dom.UsesPermissionNode;
import org.eclipse.andmore.android.model.resources.ResourceFile;
import org.eclipse.andmore.android.model.resources.types.ResourcesNode;
import org.eclipse.andmore.android.model.resources.types.AbstractResourceNode.NodeType;
import org.eclipse.andmore.android.resources.AndroidProjectResources;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.osgi.util.NLS;

/**
 * Activity Controller Model. As part of a MVC architecture, this class should
 * communicate with the Wizard UI to provide all needed information to create a
 * functional Activity.
 */
public class Activity extends Launcher {
	private static final String INTENT_ACTION_MAIN_NAME = "android.intent.action.MAIN";

	private static final String INTENT_CATEGORY_LAUNCHER_NAME = "android.intent.category.LAUNCHER";

	private static final String ACTIVITY_RESOURCE_LABEL_SUFFIX = "ActivityLabel"; //$NON-NLS-1$

	private static final int MANIFEST_UPDATING_STEPS = 6;

	private static final int RESOURCES_UPDATING_STEPS = 3;

	private boolean onStart = false;

	HashMap<String, String> workspaceResourceNames = null;

	/**
	 * Boolean flag to tell if the Activity will be set as MAIN or not in the
	 * AndroidManifest.
	 */
	private boolean isMainActivity = false;

	/**
	 * Check if the onStart Method should be created
	 * 
	 * @return
	 */
	public boolean isOnStart() {
		return onStart;
	}

	/**
	 * Change the onStart create property
	 * 
	 * @param onStart
	 */
	public void setOnStart(boolean onStart) {
		this.onStart = onStart;
	}

	/**
	 * Constructor for the Activity.
	 */
	public Activity() {
		super(IAndroidConstants.CLASS_ACTIVITY);
	}

	/*
	 * Enables finish button when page is filled.
	 */
	@Override
	public boolean needMoreInformation() {
		boolean needInfo = false;
		IStatus status = getStatus();

		if (status.getSeverity() > IStatus.WARNING) {
			needInfo = true;
		}

		return needInfo;
	}

	/**
	 * Package name (based on project name declared on manifest)
	 * 
	 * @param project
	 * @return
	 * @throws CoreException
	 *             Exception thrown in case there are problems handling the
	 *             android project
	 * @throws AndroidException
	 *             Exception thrown in case there are problems handling the
	 *             android project
	 */
	protected String getManifestPackageName(IProject project) throws AndroidException, CoreException {
		// get android application name
		AndroidManifestFile androidManifestFile = AndroidProjectManifestFile.getFromProject(project);
		String appNamespace = "";
		if (androidManifestFile != null) {
			ManifestNode manifestNode = androidManifestFile.getManifestNode();
			appNamespace = manifestNode.getPackageName();
		}
		// return the android application name along with a persistence constant
		return appNamespace;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.model.IWizardModel#save(org.eclipse.jface
	 * .wizard.IWizardContainer, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public boolean save(IWizardContainer container, IProgressMonitor monitor) throws AndroidException {
		workspaceResourceNames = new HashMap<String, String>();

		boolean classCreated = createActivityClass(monitor);

		boolean addedOnManifest = false;

		if (classCreated) {
			addedOnManifest = createActivityOnManifest(monitor);
		}

		// Logs all permissions used in UDC log
		super.save(container, monitor);

		try {
			ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, monitor);
		} catch (CoreException e) {
			// do nothing
		}
		return classCreated && addedOnManifest;

	}

	/**
	 * Creates the Activity java class
	 * 
	 * @param monitor
	 *            the progress monitor
	 * 
	 * @return true if the class has been created or false otherwise
	 * @throws AndroidException
	 */
	private boolean createActivityClass(IProgressMonitor monitor) throws AndroidException {
		boolean created = false;

		monitor.subTask(CodeUtilsNLS.UI_Activity_CreatingTheActivityJavaClass);

		ActivityClass activityClass = null;

		try {
			activityClass = new ActivityClass(getName(), getPackageFragment().getElementName(), onStart);
			createJavaClassFile(activityClass, monitor);

			created = true;
		} catch (JavaModelException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_Activity_CannotCreateTheActivityClass, getName(),
					e.getLocalizedMessage());

			throw new AndroidException(errMsg);
		} catch (AndroidException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_Activity_CannotCreateTheActivityClass, getName(),
					e.getLocalizedMessage());
			throw new AndroidException(errMsg);
		}

		return created;
	}

	/**
	 * Creates the Activity class entry on AndroidManifest.xml file
	 * 
	 * @param monitor
	 *            the progress monitor
	 * 
	 * @return true if the entry has been added or false otherwise
	 * @throws AndroidException
	 */
	private boolean createActivityOnManifest(IProgressMonitor monitor) throws AndroidException {
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
					if (!permissionsNames.contains(i.getName())) {
						permissionsNames.add(i.getName());
					}
				}

				for (String intentFilterPermission : getIntentFilterPermissionsAsArray()) {
					if (!permissionsNames.contains(intentFilterPermission)) {
						manifestNode.addChild(new UsesPermissionNode(intentFilterPermission));
					}
				}

				boolean activityExists = false;

				// Existing activity, if exists
				ActivityNode existingActivity = null;

				String classQualifier = (getPackageFragment().getElementName().equals(manifestNode.getPackageName()) ? "" : getPackageFragment() //$NON-NLS-1$
								.getElementName())
						+ "."; //$NON-NLS-1$

				for (ActivityNode activityNode : applicationNode.getActivityNodes()) {
					if (activityNode.getName().substring(activityNode.getName().lastIndexOf('.') + 1).equals(getName())) {
						activityExists = true;
						existingActivity = activityNode;
						break;
					}
				}

				if (isMainActivity) {
					boolean actionRemoved = false;

					// check if there is a main activity. If so, removes actions
					// and intent filter
					for (ActivityNode activityNode : applicationNode.getActivityNodes()) {
						if ((existingActivity != null) && existingActivity.equals(activityNode)) {
							continue;
						}

						List<IntentFilterNode> intentList = activityNode.getIntentFilterNodes();
						for (IntentFilterNode currentIntent : intentList) {
							actionRemoved = false;
							List<ActionNode> actionList = currentIntent.getActionNodes();
							for (ActionNode currentAction : actionList) {
								if (currentAction.getName().equals(INTENT_ACTION_MAIN_NAME)) {
									currentIntent.removeActionNode(currentAction);
									actionRemoved = true;
								}
							}
							// if INTENT_ACTION_MAIN_NAME is found remove
							// INTENT_CATEGORY_LAUNCHER_NAME too
							if (actionRemoved) {
								List<CategoryNode> categoryList = currentIntent.getCategoryNodes();
								for (CategoryNode currentCategory : categoryList) {
									if (currentCategory.getName().equals(INTENT_CATEGORY_LAUNCHER_NAME)) {
										currentIntent.removeCategoryNode(currentCategory);
									}
								}
							}
							// remove intent filter if empty
							if (actionRemoved && (currentIntent.getChildren().length == 0)) {
								activityNode.removeIntentFilterNode(currentIntent);
							}
						}
					}
				}

				monitor.worked(1);

				if (!activityExists) {
					ActivityNode activityNode = new ActivityNode(classQualifier + getName());

					String activityLabel = createActivityLabel(monitor);

					if (activityLabel != null) {
						activityNode.setLabel(AndroidProjectResources.STRING_CALL_PREFIX + activityLabel);
					}

					IntentFilterNode intentFilterNode = new IntentFilterNode();

					for (String intentFilterAction : getIntentFilterActionsAsArray()) {
						intentFilterNode.addActionNode(new ActionNode(intentFilterAction));
					}

					for (String intentFilterCategory : getIntentFilterCategoriesAsArray()) {
						intentFilterNode.addCategoryNode(new CategoryNode(intentFilterCategory));
					}

					// Check if we need to insert a filter action and filter
					// category setting this activity as MAIN
					if (isMainActivity) {
						intentFilterNode.addActionNode(new ActionNode(INTENT_ACTION_MAIN_NAME));
						intentFilterNode.addCategoryNode(new CategoryNode(INTENT_CATEGORY_LAUNCHER_NAME));

					}

					if (intentFilterNode.getChildren().length > 0) {
						activityNode.addIntentFilterNode(intentFilterNode);
					}

					applicationNode.addActivityNode(activityNode);

					monitor.worked(1);

					monitor.subTask(CodeUtilsNLS.UI_Common_SavingTheAndroidManifestXMLFile);

					AndroidProjectManifestFile.saveToProject(getProject(), androidManifestFile, true);
					created = true;

					monitor.worked(1);
				} else {
					if (isMainActivity) {
						boolean hasMainAction = false;
						boolean hasLauncherCategory = false;

						// Check if the existing activity already has the MAIN
						// and LAUNCHER intents
						if (existingActivity != null) {
							// Retrieve list of intent nodes
							List<IntentFilterNode> intentFilterNodeList = existingActivity.getIntentFilterNodes();

							// Create a intent filter in case it does not exist
							if (intentFilterNodeList.size() < 1) {
								IntentFilterNode intentNode = new IntentFilterNode();
								intentFilterNodeList.add(intentNode);
								existingActivity.addIntentFilterNode(intentNode);
							}

							for (IntentFilterNode intentFilterNode : intentFilterNodeList) {
								// Retrieve a list of intent actions
								List<ActionNode> actionNodes = intentFilterNode.getActionNodes();

								for (ActionNode actionNode : actionNodes) {
									if (actionNode.getName().equals(INTENT_ACTION_MAIN_NAME)) {
										hasMainAction = true;
									}
									break;

								}

								// Retrieve a list of intent categories
								List<CategoryNode> categoryNodes = intentFilterNode.getCategoryNodes();

								for (CategoryNode categoryNode : categoryNodes) {
									if (categoryNode.getName().equals(INTENT_CATEGORY_LAUNCHER_NAME)) {
										hasLauncherCategory = true;
									}
									break;
								}

								// If both the action and launcher are missing,
								// insert them and break the loop to avoid
								// duplicates
								if (!hasMainAction && !hasLauncherCategory) {
									intentFilterNode.addActionNode(new ActionNode(INTENT_ACTION_MAIN_NAME));
									intentFilterNode.addCategoryNode(new CategoryNode(INTENT_CATEGORY_LAUNCHER_NAME));
									break;
								}
							}
						}

						monitor.subTask(CodeUtilsNLS.UI_Common_SavingTheAndroidManifestXMLFile);

						AndroidProjectManifestFile.saveToProject(getProject(), androidManifestFile, true);
						created = true;

						monitor.worked(1);

					} else {
						created = true;
					}
				}

			}
		} catch (AndroidException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_Activity_CannotUpdateTheManifestFile, getName(),
					e.getLocalizedMessage());
			throw new AndroidException(errMsg);
		} catch (CoreException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_Activity_CannotUpdateTheManifestFile, getName(),
					e.getLocalizedMessage());
			throw new AndroidException(errMsg);
		} finally {
			monitor.done();
		}

		return created;
	}

	/**
	 * Adds the Activity label value on the strings resource file
	 * 
	 * @param monitor
	 *            the progress monitor
	 * 
	 * @return The label value if it has been added to the strings resource file
	 *         or null otherwise
	 * @throws AndroidException
	 */
	private String createActivityLabel(IProgressMonitor monitor) throws AndroidException {
		String resLabel = null;

		if ((getLabel() != null) && (getLabel().trim().length() > 0)) {
			try {
				monitor.subTask(CodeUtilsNLS.UI_Common_UpdatingTheStringsResourceFile);

				ResourceFile stringsFile = AndroidProjectResources.getResourceFile(getProject(), NodeType.String);

				monitor.worked(1);

				if (stringsFile.getResourcesNode() == null) {
					stringsFile.addResourceEntry(new ResourcesNode());
				}

				resLabel = stringsFile.getNewResourceName(getName() + ACTIVITY_RESOURCE_LABEL_SUFFIX);

				org.eclipse.andmore.android.model.resources.types.StringNode strNode = new org.eclipse.andmore.android.model.resources.types.StringNode(
						resLabel);
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

	/**
	 * @return The default onStart() method signature including return value and
	 *         visibility level.
	 */
	public String getOnStartMessage() {
		return "protected void onStart()"; //$NON-NLS-1$
	}

	/**
	 * @return True if this activity is to be set as main activity. Otherwise,
	 *         returns false.
	 */
	public boolean isMainActivity() {
		return isMainActivity;
	}

	/**
	 * @param isMainActivity
	 *            Set to true if this activity is to be set as main activity.
	 */
	public void setMainActivity(boolean isMainActivity) {
		this.isMainActivity = isMainActivity;
	}
}