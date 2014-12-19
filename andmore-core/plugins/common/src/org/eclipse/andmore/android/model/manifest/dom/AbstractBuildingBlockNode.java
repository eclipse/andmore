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
 * Abstract class used to define the building blocks node classes
 */
public abstract class AbstractBuildingBlockNode extends AbstractIconLabelNameNode {
	static {
		defaultProperties.add(PROP_ENABLED);
		defaultProperties.add(PROP_EXPORTED);
		defaultProperties.add(PROP_PERMISSION);
		defaultProperties.add(PROP_PROCESS);
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
	 * The process property
	 */
	private String propProcess = null;

	/**
	 * Default constructor
	 * 
	 * @param name
	 *            the name property. It must not be null.
	 */
	protected AbstractBuildingBlockNode(String name) {
		super(name);
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
		if (propEnabled != null) {
			properties.put(PROP_ENABLED, propEnabled.toString());
		}

		if (propExported != null) {
			properties.put(PROP_EXPORTED, propExported.toString());
		}

		if (propPermission != null) {
			properties.put(PROP_PERMISSION, propPermission);
		}

		if (propProcess != null) {
			properties.put(PROP_PROCESS, propProcess);
		}
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
}
