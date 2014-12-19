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
 * Class that represents a <service> node on AndroidManifest.xml file
 */
public class ServiceNode extends AbstractBuildingBlockNode {
	public ServiceNode(String name) {
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
		return (nodeType == NodeType.IntentFilter) || (nodeType == NodeType.MetaData) || (nodeType == NodeType.Comment);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.manifest.dom.AndroidManifestNode#
	 * getNodeType()
	 */
	@Override
	public NodeType getNodeType() {
		return NodeType.Service;
	}

	/**
	 * Adds an Intent Filter Node to the Service Node
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
	 * Retrieves all Intent Filter Nodes from the Service Node
	 * 
	 * @return all Intent Filter Nodes from the Service Node
	 */
	public List<IntentFilterNode> getIntentFilterNodes() {
		List<IntentFilterNode> intentFilters = new LinkedList<IntentFilterNode>();

		for (AndroidManifestNode node : getAllChildrenFromType(NodeType.IntentFilter)) {
			intentFilters.add((IntentFilterNode) node);
		}

		return intentFilters;
	}

	/**
	 * Removes an Intent Filter Node from the Service Node
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
	 * Adds a Metadata Node to the Service Node
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
	 * Retrieves all Metadata Nodes from the Service Node
	 * 
	 * @return all Metadata Nodes from the Service Node
	 */
	public List<MetadataNode> getMetadataNodes() {
		List<MetadataNode> metadatas = new LinkedList<MetadataNode>();

		for (AndroidManifestNode node : getAllChildrenFromType(NodeType.MetaData)) {
			metadatas.add((MetadataNode) node);
		}

		return metadatas;
	}

	/**
	 * Removes a Metadata Node from the Service Node
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
