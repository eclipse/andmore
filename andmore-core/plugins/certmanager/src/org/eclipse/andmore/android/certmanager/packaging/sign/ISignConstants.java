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

package org.eclipse.andmore.android.certmanager.packaging.sign;

public interface ISignConstants {
	/**
	 * md5 algorithm
	 */
	public final String MD5 = "MD5";

	/**
	 * rsa algorithm
	 */
	public final String RSA = "RSA";

	/**
	 * dsa algorithm
	 */
	public final String DSA = "DSA";

	/**
	 * sha1 algorithm
	 */
	public final String SHA1 = "SHA1";

	/**
	 * algorithm connector
	 */
	public final String ALGORITHM_CONNECTOR = "with";

	/**
	 * Signature file name extension
	 */
	public final String SIGNATURE_FILE_NAME_EXTENSION = ".SF";

	/**
	 * Signature Version
	 */
	public final String SIGNATURE_VERSION_KEY = "Signature-Version";

	public final String SIGNATURE_VERSION_VALUE = "1.0";

	/**
	 * Digest algorithm entry in manifest
	 */
	public final String SHA1_DIGEST = "SHA1-Digest";

	/**
	 * sha1-digest-manifest signature entry
	 */
	public final String SHA1_DIGEST_MANIFEST = "SHA1-Digest-Manifest";

	/**
	 * manifest main attributes signature entry
	 */
	public final String SHA1_DIGEST_MANIFEST_MAIN = "SHA1-Digest-Manifest-Main-Attributes";

	/**
	 * Name of the signature files (.SF and .RSA)
	 */
	public final String SIGNATURE_FILE_NAME = "CERT";
}
