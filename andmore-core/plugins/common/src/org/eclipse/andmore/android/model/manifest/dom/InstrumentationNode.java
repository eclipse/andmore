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

import java.util.List;

import org.eclipse.core.runtime.IStatus;

/**
 * Class that represents an <instrumentation> node on AndroidManifest.xml file
 */
public class InstrumentationNode extends AbstractIconLabelNameNode {
	static {
		defaultProperties.add(PROP_FUNCTIONALTEST);
		defaultProperties.add(PROP_HANDLEPROFILING);
		defaultProperties.add(PROP_TARGETPACKAGE);
	}

	/**
	 * The functionalTest property
	 */
	private Boolean propFunctionalTest = null;

	/**
	 * The handleProfiling property
	 */
	private Boolean propHandleProfiling = null;

	/**
	 * The targetPackage property
	 */
	private String propTargetPackage = null;

	/**
	 * Default constructor
	 * 
	 * @param name
	 *            The name property. It must not be null.
	 */
	public InstrumentationNode(String name) {
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
		// Always returns false. This node can not contain children.
		return false;
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
		if (propFunctionalTest != null) {
			properties.put(PROP_FUNCTIONALTEST, propFunctionalTest.toString());
		}

		if (propHandleProfiling != null) {
			properties.put(PROP_HANDLEPROFILING, propHandleProfiling.toString());
		}

		if ((propTargetPackage != null) && (propTargetPackage.trim().length() > 0)) {
			properties.put(PROP_TARGETPACKAGE, propTargetPackage);
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
		return NodeType.Instrumentation;
	}

	/**
	 * Gets the functionalTest property value
	 * 
	 * @return the functionalTest property value
	 */
	public Boolean getFunctionalTest() {
		return propFunctionalTest;
	}

	/**
	 * Sets the functionalTest property value. Set it to null to remove it.
	 * 
	 * @param functionalTest
	 *            the functionalTest property value
	 */
	public void setFunctionalTest(Boolean functionalTest) {
		this.propFunctionalTest = functionalTest;
	}

	/**
	 * Gets the handleProfiling property value
	 * 
	 * @return the handleProfiling property value
	 */
	public Boolean getHandleProfiling() {
		return propHandleProfiling;
	}

	/**
	 * Sets the handleProfiling property value. Set it to null to remove it.
	 * 
	 * @param handleProfiling
	 *            the handleProfiling property value
	 */
	public void setHandleProfiling(Boolean handleProfiling) {
		this.propHandleProfiling = handleProfiling;
	}

	/**
	 * Gets the targetPackage property value
	 * 
	 * @return the targetPackage property value
	 */
	public String getTargetPackage() {
		return propTargetPackage;
	}

	/**
	 * Sets the targetPackage property value. Set it to null to remove it.
	 * 
	 * @param targetPackage
	 *            the targetPackage property value
	 */
	public void setTargetPackage(String targetPackage) {
		this.propTargetPackage = targetPackage;
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
