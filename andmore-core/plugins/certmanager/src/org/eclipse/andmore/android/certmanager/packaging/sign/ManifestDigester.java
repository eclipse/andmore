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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.eclipse.andmore.android.common.log.StudioLogger;

/**
 * This class is a Manifest digester. It generates digested hashes for each
 * entry in the manifest file.
 */
public class ManifestDigester {
	private final Manifest manifest;

	private HashMap<String, ManifestEntry> entries = null;

	/**
	 * Create a new manifest digester with the given manifest
	 * 
	 * @param manifest
	 */
	public ManifestDigester(final Manifest manifest) {
		this.manifest = manifest;
		initialize();
	}

	/**
	 * Initialize the digester creating internal entries for each manifest entry
	 */
	private void initialize() {
		Map<String, Attributes> manifestEntries = manifest.getEntries();

		// initialize internal entries list
		entries = new HashMap<String, ManifestEntry>(manifestEntries.size());
		for (String entryName : manifestEntries.keySet()) {
			entries.put(entryName, new ManifestEntry(entryName, manifestEntries.get(entryName)));
		}
	}

	/**
	 * Get this Manifest file digested
	 * 
	 * @throws IOException
	 *             if some error occurs during entries encoding
	 */
	public String getDigestedString() throws IOException {
		StringBuilder builder = new StringBuilder();
		for (ManifestEntry entry : entries.values()) {
			builder.append(entry.toDigestedManifestEntry());
			builder.append(ManifestEntry.MANIFEST_NEW_LINE);
		}
		return builder.toString();
	}

	public HashMap<String, ManifestEntry> getEntries() {
		return entries;
	}

	/**
	 * Computes the digest for the main manifest attributes
	 * 
	 * @return the digest of the main manifest attributes or null otherwise
	 * @throws SignException
	 *             if a processing error occurs when computing the digest
	 */
	public byte[] getDigestedManifestMainAttributes() throws SignException {
		// create an auxiliary manifest that contain only the main attributes
		// of the original manifest
		Manifest auxManifest = new Manifest();
		byte[] result;
		Attributes auxMainAttributes = auxManifest.getMainAttributes();
		Attributes mainAttributes = manifest.getMainAttributes();
		for (Object attributeKey : mainAttributes.keySet()) {
			String name = attributeKey.toString();
			String value = mainAttributes.getValue(name);
			auxMainAttributes.putValue(name, value);
		}

		result = getDigestedManifest(auxManifest);

		StudioLogger.info(SignatureFile.class, "Created digest for main manifest attributes");

		return result;
	}

	/**
	 * Get this Manifest digested
	 * 
	 * @return
	 * @throws SignException
	 */
	public byte[] getDigestedManifest() throws SignException {
		return getDigestedManifest(manifest);
	}

	/**
	 * Computes the digest for the manifest
	 * 
	 * @param manifest
	 *            the manifest to be digested
	 * @return the digest of the entire manifest or null otherwise
	 * @throws SignException
	 *             if a processing error occurs when computing the digest
	 */
	public static byte[] getDigestedManifest(Manifest manifest) throws SignException {
		byte[] digestedManifestBytes = null;
		ByteArrayOutputStream baos = null;
		try {
			MessageDigest messageDigest = MessageDigest.getInstance(ISignConstants.SHA1);
			baos = new ByteArrayOutputStream();
			manifest.write(baos);

			messageDigest.reset();
			digestedManifestBytes = messageDigest.digest(baos.toByteArray());
		} catch (IOException e) {
			StudioLogger.error(SignatureFile.class, "I/O error encoding manifest digest: " + e.getMessage());

			throw new SignException("I/O error encoding manifest digest", e);
		} catch (NoSuchAlgorithmException e) {
			StudioLogger.error(SignatureFile.class, "Error getting message digester");

			throw new SignException("Could digest the manifest");
		} finally {
			try {
				if (baos != null) {
					baos.close();
				}
			} catch (IOException e) {
				// do nothing
			}
		}

		if (digestedManifestBytes == null) {
			StudioLogger.error(SignatureFile.class, "Error encoding manifest digest");

			throw new SignException("Could not encode manifest digest");
		}

		StudioLogger.info(SignatureFile.class, "Created manifest digest");

		return digestedManifestBytes;
	}
}