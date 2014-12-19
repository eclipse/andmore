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
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.bouncycastle.util.encoders.Base64Encoder;
import org.eclipse.andmore.android.certmanager.CertificateManagerActivator;
import org.eclipse.andmore.android.certmanager.packaging.PackageFile;
import org.eclipse.andmore.android.common.log.StudioLogger;

/**
 * This class implements the package signature file, that follows the jar
 * signing process.
 */
public class SignatureFile {
	/**
	 * The package file
	 */
	private final PackageFile packageFile;

	/**
	 * The base encoder
	 */
	private Base64Encoder encoder = new Base64Encoder();

	/**
	 * Manifest Created-By attribute
	 */
	private final String createdBy;

	/**
	 * Default Constructor
	 * 
	 * @param packageFile
	 *            the signed package file to be signed
	 * @param alias
	 *            the certificate alias
	 * @param encoder
	 *            the BASE64 encoder
	 * @param createdBy
	 *            Created-By manifest attribute
	 */
	public SignatureFile(PackageFile packageFile, String alias, Base64Encoder encoder, String createdBy) {
		this.packageFile = packageFile;
		this.encoder = encoder;
		this.createdBy = createdBy;
	}

	/**
	 * Return the filename with relative path from root (normally
	 * META-INF/alias.SF).
	 */
	@Override
	public String toString() {
		return CertificateManagerActivator.METAFILES_DIR + CertificateManagerActivator.JAR_SEPARATOR
				+ ISignConstants.SIGNATURE_FILE_NAME + ISignConstants.SIGNATURE_FILE_NAME_EXTENSION;

	}

	/**
	 * Writes this file to an output stream.
	 * 
	 * @param outputStream
	 *            the stream to write this file
	 * @throws IOException
	 *             if an I/O error occurs during the signing process
	 * @throws SignException
	 *             if a processing error occurs during the signing process
	 */
	public void write(OutputStream outputStream) throws IOException, SignException {
		// the manifest file
		Manifest manifestFile = this.packageFile.getManifest();

		// the manifest digester
		ManifestDigester manifestDigester = new ManifestDigester(manifestFile);

		// the signature file to be constructed
		Manifest signatureFile = new Manifest();

		// the manifest digested main attributes
		byte[] digestedMainAttributes = manifestDigester.getDigestedManifestMainAttributes();

		// the digest of entire manifest
		byte[] digestedManifest = manifestDigester.getDigestedManifest();

		// put the required main attributes to a valid signature file
		// (Version, CreatedBy, Main Attrib digest, Manifest digest)
		Attributes signatureFileMainAtt = signatureFile.getMainAttributes();
		signatureFileMainAtt.putValue(ISignConstants.SIGNATURE_VERSION_KEY, ISignConstants.SIGNATURE_VERSION_VALUE);
		signatureFileMainAtt.putValue(CertificateManagerActivator.CREATED_BY_FIELD, this.createdBy);

		ByteArrayOutputStream stream = null;

		try {
			stream = new ByteArrayOutputStream();
			encoder.encode(digestedMainAttributes, 0, digestedMainAttributes.length, stream);
			String encodedMainAttributesDigest = stream.toString();

			stream.reset();
			encoder.encode(digestedManifest, 0, digestedManifest.length, stream);
			String encodedManifestDigest = stream.toString();

			signatureFileMainAtt.putValue(ISignConstants.SHA1_DIGEST_MANIFEST_MAIN, encodedMainAttributesDigest);
			signatureFileMainAtt.putValue(ISignConstants.SHA1_DIGEST_MANIFEST, encodedManifestDigest);
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					StudioLogger.error("Could not close stream writing signature file. " + e.getMessage());
				}
			}
		}

		// calculate the digest from each entry of manifest
		ByteArrayOutputStream baos = null;
		try {
			baos = new ByteArrayOutputStream();
			manifestFile.write(baos);

			Map<String, Attributes> manifestEntries = manifestFile.getEntries();
			Map<String, Attributes> signatureFileEntries = signatureFile.getEntries();
			HashMap<String, ManifestEntry> entries = manifestDigester.getEntries();

			for (String manifestEntryKey : manifestEntries.keySet()) {
				ManifestEntry signatureFileEntry = entries.get(manifestEntryKey);

				byte[] digestedArray = signatureFileEntry.digest();

				ByteArrayOutputStream encodedStream = null;

				try {
					encodedStream = new ByteArrayOutputStream();
					this.encoder.encode(digestedArray, 0, digestedArray.length, encodedStream);

					String digestedValue = encodedStream.toString();

					Attributes signatureFileAtt = new Attributes();
					signatureFileAtt.putValue(ISignConstants.SHA1_DIGEST, digestedValue);
					signatureFileEntries.put(manifestEntryKey, signatureFileAtt);
				} finally {
					try {
						if (encodedStream != null) {
							encodedStream.close();
						}
					} catch (IOException e) {
						StudioLogger.error("Could not close stream: " + e.getMessage());
					}
				}
			}
		} catch (IOException e) {
			StudioLogger.error(SignatureFile.class, "I/O error digesting manifest entries: " + e.getMessage());

			throw new SignException("I/O error digesting manifest entries", e);
		} finally {
			try {
				if (baos != null) {
					baos.close();
				}
			} catch (IOException e) {
				StudioLogger.error("Could not close stream: " + e.getMessage());
			}
		}

		// I/O exceptions below are thrown unmodified
		signatureFile.write(outputStream);

		StudioLogger.info(SignatureFile.class, "Signature file was written");
	}
}
