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
 * Class that represents a <provider> node on AndroidManifest.xml file
 */
public class ProviderNode extends AbstractBuildingBlockNode {
	static {
		defaultProperties.add(PROP_AUTHORITIES);
		defaultProperties.add(PROP_GRANTURIPERMISSIONS);
		defaultProperties.add(PROP_INITORDER);
		defaultProperties.add(PROP_MULTIPROCESS);
		defaultProperties.add(PROP_READPERMISSION);
		defaultProperties.add(PROP_SYNCABLE);
		defaultProperties.add(PROP_WRITEPERMISSION);
	}

	/**
	 * The authorities property
	 */
	private List<String> propAuthorities = null;

	/**
	 * The grantUriPermissions property
	 */
	private Boolean propGrantUriPermissions = null;

	/**
	 * The initOrder property
	 */
	private Integer propInitOrder = null;

	/**
	 * The multiprocess property
	 */
	private Boolean propMultiprocess = null;

	/**
	 * The readPermission property
	 */
	private String propReadPermission = null;

	/**
	 * The syncable property
	 */
	private Boolean propSyncable = null;

	/**
	 * The writePermission property
	 */
	private String propWritePermission = null;

	/**
	 * Default constructor
	 * 
	 * @param name
	 *            The name property. It must not be null.
	 * @param initialAuthority
	 *            The first authority to be added to the provider. It must not
	 *            be null.
	 */
	public ProviderNode(String name, String initialAuthority) {
		super(name);

		Assert.isLegal(initialAuthority != null);
		propAuthorities = new LinkedList<String>();
		propAuthorities.add(initialAuthority);
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
		return (nodeType == NodeType.GrantUriPermission) || (nodeType == NodeType.MetaData)
				|| (nodeType == NodeType.Comment);
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
		super.addAdditionalProperties();

		properties.put(PROP_AUTHORITIES, getAuthoritiesList());

		if (propGrantUriPermissions != null) {
			properties.put(PROP_GRANTURIPERMISSIONS, propGrantUriPermissions.toString());
		}

		if (propInitOrder != null) {
			properties.put(PROP_INITORDER, propInitOrder.toString());
		}

		if (propMultiprocess != null) {
			properties.put(PROP_MULTIPROCESS, propMultiprocess.toString());
		}

		if (propReadPermission != null) {
			properties.put(PROP_READPERMISSION, propReadPermission);
		}

		if (propSyncable != null) {
			properties.put(PROP_SYNCABLE, propSyncable.toString());
		}

		if (propWritePermission != null) {
			properties.put(PROP_WRITEPERMISSION, propWritePermission);
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
		return NodeType.Provider;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.manifest.dom.AndroidManifestNode#
	 * isNodeValid()
	 */
	@Override
	protected boolean isNodeValid() {
		return super.isNodeValid() && (getAuthoritiesList().trim().length() > 0);
	}

	/**
	 * Gets the Content Provider authorities as a list separated by semicolons,
	 * ready to be used on manifest file
	 * 
	 * @return the Content Provider authorities as a list separated by
	 *         semicolons
	 */
	private String getAuthoritiesList() {
		String authorities = "";

		if ((propAuthorities != null) && !propAuthorities.isEmpty()) {
			for (int i = 0; i < (propAuthorities.size() - 1); i++) {
				authorities += propAuthorities.get(i) + ";";
			}

			authorities += propAuthorities.get(propAuthorities.size() - 1);
		}

		return authorities;
	}

	/**
	 * Adds an authority to the Content Provider
	 * 
	 * @param authority
	 *            The authority name
	 * 
	 * @return true if the authority has been added (if it does not exist) or
	 *         false otherwise
	 */
	public boolean addAuthority(String authority) {
		Assert.isLegal(authority != null);

		boolean added = false;
		String auth = authority.trim();

		if (!propAuthorities.contains(auth)) {
			propAuthorities.add(auth);
			added = true;
		}

		return added;
	}

	/**
	 * Retrieves an array containing all authorities
	 * 
	 * @return an array containing all authorities
	 */
	public String[] getAuthorities() {
		String[] authorities = new String[propAuthorities.size()];

		authorities = propAuthorities.toArray(authorities);

		return authorities;
	}

	/**
	 * Removes an authority from the authority list. If the given authority is
	 * the last one, it will not be removed.
	 * 
	 * @param authority
	 *            The authority to be removed
	 * 
	 * @return true if the authority has been removed and false otherwise
	 */
	public boolean removeAuthority(String authority) {
		Assert.isLegal(authority != null);

		boolean removed = false;
		String auth = authority.trim();

		if ((propAuthorities.size() > 1) && propAuthorities.contains(auth)) {
			propAuthorities.remove(auth);
			removed = true;
		}

		return removed;
	}

	/**
	 * Gets the grantUriPermissions property value
	 * 
	 * @return the grantUriPermissions property value
	 */
	public Boolean getGrantUriPermissions() {
		return propGrantUriPermissions;
	}

	/**
	 * Sets the grantUriPermissions property value. Set it to null to remove it.
	 * 
	 * @param grantUriPermissions
	 *            the grantUriPermissions property value
	 */
	public void setGrantUriPermissions(Boolean grantUriPermissions) {
		this.propGrantUriPermissions = grantUriPermissions;
	}

	/**
	 * Gets the initOrder property value
	 * 
	 * @return the initOrder property value
	 */
	public Integer getInitOrder() {
		return propInitOrder;
	}

	/**
	 * Sets the initOrder property value. Set it to null to remove it.
	 * 
	 * @param initOrder
	 *            the initOrder property value
	 */
	public void setInitOrder(Integer initOrder) {
		this.propInitOrder = initOrder;
	}

	/**
	 * Gets the multiprocess property value
	 * 
	 * @return the multiprocess property value
	 */
	public Boolean getMultiprocess() {
		return propMultiprocess;
	}

	/**
	 * Sets the multiprocess property value. Set it to null to remove it.
	 * 
	 * @param multiprocess
	 *            the multiprocess property value
	 */
	public void setMultiprocess(Boolean multiprocess) {
		this.propMultiprocess = multiprocess;
	}

	/**
	 * Gets the readPermission property value
	 * 
	 * @return the readPermission property value
	 */
	public String getReadPermission() {
		return propReadPermission;
	}

	/**
	 * Sets the readPermission property value. Set it to null to remove it.
	 * 
	 * @param readPermission
	 *            the readPermission property value
	 */
	public void setReadPermission(String readPermission) {
		this.propReadPermission = readPermission;
	}

	/**
	 * Gets the syncable property value
	 * 
	 * @return the syncable property value
	 */
	public Boolean getSyncable() {
		return propSyncable;
	}

	/**
	 * Sets the syncable property value. Set it to null to remove it.
	 * 
	 * @param syncable
	 *            the syncable property value
	 */
	public void setSyncable(Boolean syncable) {
		this.propSyncable = syncable;
	}

	/**
	 * Gets the writePermission property value
	 * 
	 * @return the writePermission property value
	 */
	public String getWritePermission() {
		return propWritePermission;
	}

	/**
	 * Sets the writePermission property value. Set it to null to remove it.
	 * 
	 * @param writePermission
	 *            the writePermission property value
	 */
	public void setWritePermission(String writePermission) {
		this.propWritePermission = writePermission;
	}

	/**
	 * Adds a Metadata Node to the Provider Node
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
	 * Retrieves all Metadata Nodes from the Provider Node
	 * 
	 * @return all Metadata Nodes from the Provider Node
	 */
	public List<MetadataNode> getMetadataNodes() {
		List<MetadataNode> metadatas = new LinkedList<MetadataNode>();

		for (AndroidManifestNode node : getAllChildrenFromType(NodeType.MetaData)) {
			metadatas.add((MetadataNode) node);
		}

		return metadatas;
	}

	/**
	 * Removes a Metadata Node from the Provider Node
	 * 
	 * @param metadata
	 *            the Metadata Node to be removed
	 */
	public void removeMetadataNode(MetadataNode metadata) {
		if (metadata != null) {
			children.remove(metadata);
		}
	}

	/**
	 * Adds a Grant Uri Permission Node to the Provider Node
	 * 
	 * @param grantUriPermission
	 *            The Grant Uri Permission Node
	 */
	public void addGrantUriPermissionNode(GrantUriPermissionNode grantUriPermission) {
		if (grantUriPermission != null) {
			if (!children.contains(grantUriPermission)) {
				children.add(grantUriPermission);
			}
		}
	}

	/**
	 * Retrieves all Grant Uri Permission Nodes from the Provider Node
	 * 
	 * @return all Grant Uri Permission Nodes from the Provider Node
	 */
	public List<GrantUriPermissionNode> getGrantUriPermissionsNodes() {
		List<GrantUriPermissionNode> grantUriPermissions = new LinkedList<GrantUriPermissionNode>();

		for (AndroidManifestNode node : getAllChildrenFromType(NodeType.GrantUriPermission)) {
			grantUriPermissions.add((GrantUriPermissionNode) node);
		}

		return grantUriPermissions;
	}

	/**
	 * Removes a Grant Uri Permission Node from the Provider Node
	 * 
	 * @param grantUriPermission
	 *            the Grant Uri Permission Node to be removed
	 */
	public void removeGrantUriPermissionNode(GrantUriPermissionNode grantUriPermission) {
		if (grantUriPermission != null) {
			children.remove(grantUriPermission);
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

	/**
	 * Adds an Intent Filter Node to the Provider Node
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
}
