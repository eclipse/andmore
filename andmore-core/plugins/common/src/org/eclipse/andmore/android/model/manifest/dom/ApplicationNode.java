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
package org.eclipse.andmore.android.model.manifest.dom;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;

/**
 * Class that represents an <application> node on AndroidManifest.xml file
 */
public class ApplicationNode extends AbstractIconLabelNameNode {
	static {
		defaultProperties.add(PROP_ALLOWCLEARUSERDATA);
		defaultProperties.add(PROP_ALLOWTASKREPARENTING);
		defaultProperties.add(PROP_DEBUGGABLE);
		defaultProperties.add(PROP_DESCRIPTION);
		defaultProperties.add(PROP_ENABLED);
		defaultProperties.add(PROP_HASCODE);
		defaultProperties.add(PROP_MANAGESPACEACTIVITY);
		defaultProperties.add(PROP_PERMISSION);
		defaultProperties.add(PROP_PERSISTENT);
		defaultProperties.add(PROP_PROCESS);
		defaultProperties.add(PROP_TASKAFFINITY);
		defaultProperties.add(PROP_THEME);
	}

	/**
	 * The allowClearUserData property
	 */
	private Boolean propAllowClearUserData = null;

	/**
	 * The allowTaskReparenting property
	 */
	private Boolean propAllowTaskReparenting = null;

	/**
	 * The debuggable property
	 */
	private Boolean propDebuggable = null;

	/**
	 * The description property
	 */
	private String propDescription = null;

	/**
	 * The enabled property
	 */
	private Boolean propEnabled = null;

	/**
	 * The hasCode property
	 */
	private Boolean propHasCode = null;

	/**
	 * The manageSpaceActivity property
	 */
	private String propManageSpaceActivity = null;

	/**
	 * The permission property
	 */
	private String propPermission = null;

	/**
	 * The persistent property
	 */
	private Boolean propPersistent = null;

	/**
	 * The process property
	 */
	private String propProcess = null;

	/**
	 * The taskAffinity property
	 */
	private String propTaskAffinity = null;

	/**
	 * The theme property
	 */
	private String propTheme = null;

	/**
	 * Default constructor
	 * 
	 * @param name
	 *            The name property. It must not be null.
	 */
	public ApplicationNode(String name) {
		super(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.manifest.dom.AndroidManifestNode#
	 * canContains
	 * (com.motorola.studio.android.model.manifest.dom.AndroidManifestNode
	 * .NodeType)
	 */
	@Override
	protected boolean canContains(NodeType nodeType) {
		return (nodeType == NodeType.Activity) || (nodeType == NodeType.ActivityAlias)
				|| (nodeType == NodeType.Service) || (nodeType == NodeType.Receiver) || (nodeType == NodeType.Provider)
				|| (nodeType == NodeType.UsesLibrary) || (nodeType == NodeType.Comment)
				|| (nodeType == NodeType.MetaData);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.motorola.studio.android.model.manifest.dom.AbstractIconLabelNameNode
	 * #addAdditionalProperties()
	 */
	@Override
	protected void addAdditionalProperties() {
		if (propAllowClearUserData != null) {
			properties.put(PROP_ALLOWCLEARUSERDATA, propAllowClearUserData.toString());
		}

		if (propAllowTaskReparenting != null) {
			properties.put(PROP_ALLOWTASKREPARENTING, propAllowTaskReparenting.toString());
		}

		if (propDebuggable != null) {
			properties.put(PROP_DEBUGGABLE, propDebuggable.toString());
		}

		if (propDescription != null) {
			properties.put(PROP_DESCRIPTION, propDescription);
		}

		if (propEnabled != null) {
			properties.put(PROP_ENABLED, propEnabled.toString());
		}

		if (propHasCode != null) {
			properties.put(PROP_HASCODE, propHasCode.toString());
		}

		if (propManageSpaceActivity != null) {
			properties.put(PROP_MANAGESPACEACTIVITY, propManageSpaceActivity);
		}

		if (propPermission != null) {
			properties.put(PROP_PERMISSION, propPermission);
		}

		if (propPersistent != null) {
			properties.put(PROP_PERSISTENT, propPersistent.toString());
		}

		if (propProcess != null) {
			properties.put(PROP_PROCESS, propProcess);
		}

		if (propTaskAffinity != null) {
			properties.put(PROP_TASKAFFINITY, propTaskAffinity);
		}

		if (propTheme != null) {
			properties.put(PROP_THEME, propTheme);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.manifest.dom.AndroidManifestNode#
	 * getNodeType()
	 */
	@Override
	public NodeType getNodeType() {
		return NodeType.Application;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.manifest.dom.AndroidManifestNode#
	 * isNodeValid()
	 */
	@Override
	protected boolean isNodeValid() {
		return super.isNodeValid();
	}

	/**
	 * Gets the allowClearUserData property value
	 * 
	 * @return the allowClearUserData property value
	 */
	public Boolean getAllowClearUserData() {
		return propAllowClearUserData;
	}

	/**
	 * Sets the allowClearUserData property value. Set it to null to remove it.
	 * 
	 * @param allowClearUserData
	 *            the allowClearUserData property value
	 */
	public void setAllowClearUserData(Boolean allowClearUserData) {
		this.propAllowClearUserData = allowClearUserData;
	}

	/**
	 * Gets the allowTaskReparenting property value
	 * 
	 * @return the allowTaskReparenting property value
	 */
	public Boolean getAllowTaskReparenting() {
		return propAllowTaskReparenting;
	}

	/**
	 * Sets the allowTaskReparenting property value. Set it to null to remove
	 * it.
	 * 
	 * @param allowTaskReparenting
	 *            the allowTaskReparenting property value
	 */
	public void setAllowTaskReparenting(Boolean allowTaskReparenting) {
		this.propAllowTaskReparenting = allowTaskReparenting;
	}

	/**
	 * Gets the debuggable property value
	 * 
	 * @return the debuggable property value
	 */
	public Boolean getDebuggable() {
		return propDebuggable;
	}

	/**
	 * Sets the debuggable property value. Set it to null to remove it.
	 * 
	 * @param debuggable
	 *            the debuggable property value
	 */
	public void setDebuggable(Boolean debuggable) {
		this.propDebuggable = debuggable;
	}

	/**
	 * Gets the description property value
	 * 
	 * @return the description property value
	 */
	public String getDescription() {
		return propDescription;
	}

	/**
	 * Sets the description property value. Set it to null to remove it.
	 * 
	 * @param description
	 *            the description property value
	 */
	public void setDescription(String description) {
		this.propDescription = description;
	}

	/**
	 * Gets the enabled property value
	 * 
	 * @return the enabled property value
	 */
	public Boolean getEnabled() {
		return propEnabled;
	}

	/**
	 * Sets the enabled property value. Set it to null to remove it.
	 * 
	 * @param enabled
	 *            the enabled property value
	 */
	public void setEnabled(Boolean enabled) {
		this.propEnabled = enabled;
	}

	/**
	 * Gets the hasCode property value
	 * 
	 * @return the hasCode property value
	 */
	public Boolean getHasCode() {
		return propHasCode;
	}

	/**
	 * Sets the hasCode property value. Set it to null to remove it.
	 * 
	 * @param hasCode
	 *            the hasCode property value
	 */
	public void setHasCode(Boolean hasCode) {
		this.propHasCode = hasCode;
	}

	/**
	 * Gets the manageSpaceActivity property value
	 * 
	 * @return the manageSpaceActivity property value
	 */
	public String getManageSpaceActivity() {
		return propManageSpaceActivity;
	}

	/**
	 * Sets the manageSpaceActivity property value. Set it to null to remove it.
	 * 
	 * @param manageSpaceActivity
	 *            the manageSpaceActivity property value
	 */
	public void setManageSpaceActivity(String manageSpaceActivity) {
		this.propManageSpaceActivity = manageSpaceActivity;
	}

	/**
	 * Gets the permission property value
	 * 
	 * @return the permission property value
	 */
	public String getPermission() {
		return propPermission;
	}

	/**
	 * Sets the permission property value. Set it to null to remove it.
	 * 
	 * @param permission
	 *            the permission property value
	 */
	public void setPermission(String permission) {
		this.propPermission = permission;
	}

	/**
	 * Gets the persistent property value
	 * 
	 * @return the persistent property value
	 */
	public Boolean getPersistent() {
		return propPersistent;
	}

	/**
	 * Sets the persistent property value. Set it to null to remove it.
	 * 
	 * @param persistent
	 *            the persistent property value
	 */
	public void setPersistent(Boolean persistent) {
		this.propPersistent = persistent;
	}

	/**
	 * Gets the process property value
	 * 
	 * @return the process property value
	 */
	public String getProcess() {
		return propProcess;
	}

	/**
	 * Sets the process property value. Set it to null to remove it.
	 * 
	 * @param process
	 *            the process property value
	 */
	public void setProcess(String process) {
		this.propProcess = process;
	}

	/**
	 * Gets the taskAffinity property value
	 * 
	 * @return the taskAffinity property value
	 */
	public String getTaskAffinity() {
		return propTaskAffinity;
	}

	/**
	 * Sets the taskAffinity property value. Set it to null to remove it.
	 * 
	 * @param taskAffinity
	 *            the taskAffinity property value
	 */
	public void setTaskAffinity(String taskAffinity) {
		this.propTaskAffinity = taskAffinity;
	}

	/**
	 * Gets the theme property value
	 * 
	 * @return the theme property value
	 */
	public String getTheme() {
		return propTheme;
	}

	/**
	 * Sets the theme property value. Set it to null to remove it.
	 * 
	 * @param theme
	 *            the theme property value
	 */
	public void setTheme(String theme) {
		this.propTheme = theme;
	}

	/**
	 * Adds an Activity Node to the Application Node
	 * 
	 * @param activity
	 *            The Activity Node
	 */
	public void addActivityNode(ActivityNode activity) {
		if (activity != null) {
			if (!children.contains(activity)) {
				children.add(activity);
			}
		}
	}

	/**
	 * Retrieves all Activity Nodes from the Application Node
	 * 
	 * @return all Activity Nodes from the Application Node
	 */
	public List<ActivityNode> getActivityNodes() {
		List<ActivityNode> activities = new LinkedList<ActivityNode>();

		for (AndroidManifestNode node : getAllChildrenFromType(NodeType.Activity)) {
			activities.add((ActivityNode) node);
		}

		return activities;
	}

	/**
	 * Removes an Activity Node from the Application Node
	 * 
	 * @param activity
	 *            the Activity Node to be removed
	 */
	public void removeActivityNode(ActivityNode activity) {
		if (activity != null) {
			children.remove(activity);
		}
	}

	/**
	 * Adds an Activity Alias Node to the Application Node
	 * 
	 * @param activityAlias
	 *            The Activity Alias Node
	 */
	public void addActivityAliasNode(ActivityAliasNode activityAlias) {
		if (activityAlias != null) {
			if (!children.contains(activityAlias)) {
				children.add(activityAlias);
			}
		}
	}

	/**
	 * Retrieves all Activity Alias Nodes from the Application Node
	 * 
	 * @return all Activity Alias Nodes from the Application Node
	 */
	public List<ActivityAliasNode> getActivityAliasNodes() {
		List<ActivityAliasNode> activityAliases = new LinkedList<ActivityAliasNode>();

		for (AndroidManifestNode node : getAllChildrenFromType(NodeType.ActivityAlias)) {
			activityAliases.add((ActivityAliasNode) node);
		}

		return activityAliases;
	}

	/**
	 * Removes an Activity Alias Node from the Application Node
	 * 
	 * @param activityAlias
	 *            the Activity Alias Node to be removed
	 */
	public void removeActivityAliasNode(ActivityAliasNode activityAlias) {
		if (activityAlias != null) {
			children.remove(activityAlias);
		}
	}

	/**
	 * Adds a Service Node to the Application Node
	 * 
	 * @param service
	 *            The Service Node
	 */
	public void addServiceNode(ServiceNode service) {
		if (service != null) {
			if (!children.contains(service)) {
				children.add(service);
			}
		}
	}

	/**
	 * Retrieves all Service Nodes from the Application Node
	 * 
	 * @return all Service Nodes from the Application Node
	 */
	public List<ServiceNode> getServiceNodes() {
		List<ServiceNode> services = new LinkedList<ServiceNode>();

		for (AndroidManifestNode node : getAllChildrenFromType(NodeType.Service)) {
			services.add((ServiceNode) node);
		}

		return services;
	}

	/**
	 * Removes a Service Node from the Application Node
	 * 
	 * @param service
	 *            the Service Node to be removed
	 */
	public void removeServiceNode(ServiceNode service) {
		if (service != null) {
			children.remove(service);
		}
	}

	/**
	 * Adds a Receiver Node to the Application Node
	 * 
	 * @param receiver
	 *            The Receiver Node
	 */
	public void addReceiverNode(ReceiverNode receiver) {
		if (receiver != null) {
			if (!children.contains(receiver)) {
				children.add(receiver);
			}
		}
	}

	/**
	 * Retrieves all Receiver Nodes from the Application Node
	 * 
	 * @return all Receiver Nodes from the Application Node
	 */
	public List<ReceiverNode> getReceiverNodes() {
		List<ReceiverNode> receivers = new LinkedList<ReceiverNode>();

		for (AndroidManifestNode node : getAllChildrenFromType(NodeType.Receiver)) {
			receivers.add((ReceiverNode) node);
		}

		return receivers;
	}

	/**
	 * Removes a Receiver Node from the Application Node
	 * 
	 * @param receiver
	 *            the Receiver Node to be removed
	 */
	public void removeReceiverNode(ReceiverNode receiver) {
		if (receiver != null) {
			children.remove(receiver);
		}
	}

	/**
	 * Adds a Provider Node to the Application Node
	 * 
	 * @param provider
	 *            The Provider Node
	 */
	public void addProviderNode(ProviderNode provider) {
		if (provider != null) {
			if (!children.contains(provider)) {
				children.add(provider);
			}
		}
	}

	/**
	 * Retrieves all Provider Nodes from the Application Node
	 * 
	 * @return all Provider Nodes from the Application Node
	 */
	public List<ProviderNode> getProviderNodes() {
		List<ProviderNode> providers = new LinkedList<ProviderNode>();

		for (AndroidManifestNode node : getAllChildrenFromType(NodeType.Provider)) {
			providers.add((ProviderNode) node);
		}

		return providers;
	}

	/**
	 * Removes a Provider Node from the Application Node
	 * 
	 * @param provider
	 *            the Provider Node to be removed
	 */
	public void removeProviderNode(ProviderNode provider) {
		if (provider != null) {
			children.remove(provider);
		}
	}

	/**
	 * Adds an Uses Library Node to the Application Node
	 * 
	 * @param usesLibrary
	 *            The Uses Library Node
	 */
	public void addUsesLibraryNode(UsesLibraryNode usesLibrary) {
		if (usesLibrary != null) {
			if (!children.contains(usesLibrary)) {
				children.add(usesLibrary);
			}
		}
	}

	/**
	 * Retrieves all Uses Library Nodes from the Application Node
	 * 
	 * @return all Uses Library Nodes from the Application Node
	 */
	public List<UsesLibraryNode> getUsesLibraryNodes() {
		List<UsesLibraryNode> usesLibraries = new LinkedList<UsesLibraryNode>();

		for (AndroidManifestNode node : getAllChildrenFromType(NodeType.UsesLibrary)) {
			usesLibraries.add((UsesLibraryNode) node);
		}

		return usesLibraries;
	}

	/**
	 * Removes an Uses Library Node from the Application Node
	 * 
	 * @param usesLibrary
	 *            the Uses Library Node to be removed
	 */
	public void removeUsesLibraryNode(UsesLibraryNode usesLibrary) {
		if (usesLibrary != null) {
			children.remove(usesLibrary);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.manifest.dom.AndroidManifestNode#
	 * getSpecificNodeErrors()
	 */
	@Override
	protected List<IStatus> getSpecificNodeProblems() {
		return null;
	}
}
