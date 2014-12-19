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
package org.eclipse.andmore.android.certmanager.ui.model;

import java.util.Calendar;
import java.util.Date;

public class CertificateDetailsInfo {
	private String alias;

	/**
	 * Issuer name
	 */
	private String commonName;

	/**
	 * Owner name
	 */
	private String organization;

	private String organizationUnit;

	private String locality;

	private String country;

	private String state;

	private String entryPassword; // this is NOT the keystore password

	private Date expirationDate;

	public CertificateDetailsInfo(String alias, String commonName, String organization, String organizationUnit,
			String locality, String country, String state, String validity, String entryPassword) {
		this.alias = alias;
		this.commonName = commonName;
		this.organization = organization;
		this.organizationUnit = organizationUnit;
		this.locality = locality;
		this.country = country;
		this.state = state;
		this.entryPassword = entryPassword;

		int validityYears = Integer.parseInt(validity);

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, validityYears);

		this.expirationDate = cal.getTime();
	}

	/**
	 * @return the alias
	 */
	public String getAlias() {
		return alias;
	}

	/**
	 * @return the commonName
	 */
	public String getCommonName() {
		return commonName;
	}

	/**
	 * @return the organization
	 */
	public String getOrganization() {
		return organization;
	}

	/**
	 * @return the organizationUnit
	 */
	public String getOrganizationUnit() {
		return organizationUnit;
	}

	/**
	 * @return the locality
	 */
	public String getLocality() {
		return locality;
	}

	/**
	 * @return the country
	 */
	public String getCountry() {
		return country;
	}

	/**
	 * @return the expirationDate
	 */
	public Date getExpirationDate() {
		return expirationDate;
	}

	/**
	 * @return the state
	 */
	public String getState() {
		return state;
	}

	/**
	 * @return the keyPassword
	 */
	public String getEntryPassword() {
		return entryPassword;
	}

}
