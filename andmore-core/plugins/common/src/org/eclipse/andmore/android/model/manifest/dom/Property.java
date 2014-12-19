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

/**
 * Class that represents a property of a node on AndroidManifest.xml file. For
 * now, it will be only used by the BuildingBlockExplorer view.
 */
public class Property {
	/**
	 * Property Label
	 * 
	 * For example: "android:name"
	 */
	private String propertyLabel;

	/**
	 * Property Value
	 * 
	 * For example: ".MainActivity"
	 */
	private String propertyValue;

	/**
	 * Parent node of the property.
	 */
	private AndroidManifestNode parent;

	/**
	 * Constructor
	 * 
	 * @param propertyLabel
	 *            The property label
	 * @param propertyValue
	 *            The property value
	 * @param parent
	 *            The property node parent
	 */
	public Property(String propertyLabel, String propertyValue, AndroidManifestNode parent) {
		this.propertyLabel = propertyLabel;
		this.propertyValue = propertyValue;
		this.parent = parent;
	}

	/**
	 * Get the label of the property.
	 * 
	 * @return Property label
	 */
	public String getPropertyLabel() {
		return propertyLabel;
	}

	/**
	 * Set the label of the property.
	 * 
	 * @param propertyLabel
	 */
	public void setPropertyLabel(String propertyLabel) {
		this.propertyLabel = propertyLabel;
	}

	/**
	 * Get the value of the property.
	 * 
	 * @return Property value
	 */
	public String getPropertyValue() {
		return propertyValue;
	}

	/**
	 * Set the value of the property.
	 * 
	 * @param propertyValue
	 */
	public void setPropertyValue(String propertyValue) {
		this.propertyValue = propertyValue;
	}

	/**
	 * @return the parent
	 */
	public AndroidManifestNode getParent() {
		return parent;
	}

	/**
	 * @param parent
	 *            the parent to set
	 */
	public void setParent(AndroidManifestNode parent) {
		this.parent = parent;
	}

}
