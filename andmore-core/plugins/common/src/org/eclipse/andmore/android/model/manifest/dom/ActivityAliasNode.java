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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;

/**
 * Class that represents an <activity-alias> node on AndroidManifest.xml file
 */
public class ActivityAliasNode extends AbstractIconLabelNameNode {
	static {
		defaultProperties.add(PROP_ENABLED);
		defaultProperties.add(PROP_EXPORTED);
		defaultProperties.add(PROP_PERMISSION);
		defaultProperties.add(PROP_TARGETACTIVITY);
	}

	/**
	 * The enabled property
	 */
	private Boolean propEnabled = null;

	/**
	 * The exported property
	 */
	private Boolean propExported = null;

	/**
	 * The permission property
	 */
	private String propPermission = null;

	/**
	 * The targetActivity property
	 */
	private String propTargetActivity = null;

	/**
	 * Default constructor
	 * 
	 * @param name
	 *            the name property (must not be null)
	 * @param targetActivity
	 *            the targetActivity property (must not be null)
	 */
	public ActivityAliasNode(String name, String targetActivity) {
		super(name);

		Assert.isLegal(targetActivity != null);
		this.propTargetActivity = targetActivity;
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
		return (nodeType == NodeType.IntentFilter) || (nodeType == NodeType.MetaData) || (nodeType == NodeType.Comment);
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
		properties.put(PROP_TARGETACTIVITY, propTargetActivity);

		if (propEnabled != null) {
			properties.put(PROP_ENABLED, propEnabled.toString());
		}

		if (propExported != null) {
			properties.put(PROP_EXPORTED, propExported.toString());
		}

		if (propPermission != null) {
			properties.put(PROP_PERMISSION, propPermission);
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
		return NodeType.ActivityAlias;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.manifest.dom.AndroidManifestNode#
	 * isNodeValid()
	 */
	@Override
	protected boolean isNodeValid() {
		return super.isNodeValid() && (propTargetActivity.trim().length() > 0);
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
	 * Gets the exported property value
	 * 
	 * @return the exported property value
	 */
	public Boolean getExported() {
		return propExported;
	}

	/**
	 * Sets the exported property value. Set it to null to remove it.
	 * 
	 * @param exported
	 *            the exported property value
	 */
	public void setExported(Boolean exported) {
		this.propExported = exported;
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
	 * Gets the targetActivity property value
	 * 
	 * @return the targetActivity property value
	 */
	public String getTargetActivity() {
		return propTargetActivity;
	}

	/**
	 * Sets the targetActivity property value.
	 * 
	 * @param targetActivity
	 *            the targetActivity property value
	 */
	public void setTargetActivity(String targetActivity) {
		Assert.isLegal(targetActivity != null);
		this.propTargetActivity = targetActivity;
	}

	/**
	 * Adds an Intent Filter Node to the Activity Alias Node
	 * 
	 * @param intentFilter
	 *            The Intent Filter Node
	 */
	public void addIntentFilterNode(IntentFilterNode intentFilter) {
		if (intentFilter != null) {
			if (!children.contains(intentFilter)) {
				children.add(intentFilter);
			}
		}
	}

	/**
	 * Retrieves all Intent Filter Nodes from the Activity Alias Node
	 * 
	 * @return all Intent Filter Nodes from the Activity Alias Node
	 */
	public List<IntentFilterNode> getIntentFilterNodes() {
		List<IntentFilterNode> intentFilters = new LinkedList<IntentFilterNode>();

		for (AndroidManifestNode node : getAllChildrenFromType(NodeType.IntentFilter)) {
			intentFilters.add((IntentFilterNode) node);
		}

		return intentFilters;
	}

	/**
	 * Removes an Intent Filter Node from the Activity Alias Node
	 * 
	 * @param intentFilter
	 *            the Intent Filter Node to be removed
	 */
	public void removeIntentFilterNode(IntentFilterNode intentFilter) {
		if (intentFilter != null) {
			children.remove(intentFilter);
		}
	}

	/**
	 * Adds a Metadata Node to the Activity Alias Node
	 * 
	 * @param metadata
	 *            The Metadata Node
	 */
	public void addMetadataNode(MetadataNode metadata) {
		if (metadata != null) {
			if (!children.contains(metadata)) {
				children.add(metadata);
			}
		}
	}

	/**
	 * Retrieves all Metadata Nodes from the Activity Alias Node
	 * 
	 * @return all Metadata Nodes from the Activity Alias Node
	 */
	public List<MetadataNode> getMetadataNodes() {
		List<MetadataNode> metadatas = new LinkedList<MetadataNode>();

		for (AndroidManifestNode node : getAllChildrenFromType(NodeType.MetaData)) {
			metadatas.add((MetadataNode) node);
		}

		return metadatas;
	}

	/**
	 * Removes a Metadata Node from the Activity Alias Node
	 * 
	 * @param metadata
	 *            the Metadata Node to be removed
	 */
	public void removeMetadataNode(MetadataNode metadata) {
		if (metadata != null) {
			children.remove(metadata);
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
