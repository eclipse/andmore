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
package org.eclipse.andmore.android.installer.utilities;

import java.util.List;

public abstract class InstallableItem {
	/**
	 * Example: IInstallableUnit iu
	 */
	private Object data;

	/**
	 * (will be the bundle_id in case of Eclipse installable components)
	 */
	private String bundleID;

	private Integer version;

	private boolean isInstalled;

	private String license;

	private String displayName;

	private String description;

	private List<String> requirementsIds;

	private int sizeInBytes;

	private String provider;

	private boolean canBeInstalled = true;

	private String reasonNotToInstall = "";

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public String getBundleID() {
		return bundleID;
	}

	public void setBundleID(String bundleID) {
		this.bundleID = bundleID;
	}

	public boolean isInstalled() {
		return isInstalled;
	}

	public void setInstalled(boolean isInstalled) {
		this.isInstalled = isInstalled;
	}

	public String getLicense() {
		return license;
	}

	public void setLicense(String license) {
		this.license = license;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<String> getRequirementsIds() {
		return requirementsIds;
	}

	public void setRequirementsIds(List<String> requirementsIds) {
		this.requirementsIds = requirementsIds;
	}

	public int getSizeInBytes() {
		return sizeInBytes;
	}

	public void setSizeInBytes(int sizeInBytes) {
		this.sizeInBytes = sizeInBytes;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	/**
	 * @return true if any of the licenses are NOT accepted, false otherwise
	 */
	public abstract boolean hasLicenseNotAccepted();

	/**
	 * Do any action required after license is accepted
	 */
	public abstract void acceptLicenses();

	/**
	 * Gets the maximum buffer size for a reading using streams.
	 * 
	 * @return Returns the maximum buffer size.
	 */
	public abstract int getMaxBufferSize();

	/**
	 * @param canBeInstalled
	 *            the canBeInstalled to set
	 */
	public void setCanBeInstalled(boolean canBeInstalled) {
		this.canBeInstalled = canBeInstalled;
	}

	/**
	 * @return the canBeInstalled
	 */
	public boolean canBeInstalled() {
		return canBeInstalled;
	}

	/**
	 * @param reasonNotToInstall
	 *            the reasonNotToInstall to set
	 */
	public void setReasonNotToInstall(String reasonNotToInstall) {
		this.reasonNotToInstall = reasonNotToInstall;
	}

	/**
	 * @return the reasonNotToInstall
	 */
	public String getReasonNotToInstall() {
		return reasonNotToInstall;
	}

	/**
	 * This method analyzes whether all pre-requirements have been fulfilled for
	 * this {@link InstallableItem}. In case it does, <code>true</code> is
	 * returned, otherwise <code>false</code> is given back.
	 * 
	 * @return Returns <code>true</code> in case all pre-requirements have been
	 *         fulfilled, <code>false</code> otherwise.
	 */
	public abstract boolean hasPrerequirementsFulfilled();

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return (obj != null) && (obj instanceof InstallableItem) && (this.bundleID != null)
				&& (((InstallableItem) obj).bundleID != null) && this.bundleID.equals(((InstallableItem) obj).bundleID);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return bundleID != null ? bundleID.hashCode() : 3;
	}
}
