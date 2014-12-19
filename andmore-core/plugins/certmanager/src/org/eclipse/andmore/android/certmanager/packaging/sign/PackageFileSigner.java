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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.jar.Attributes;

import org.bouncycastle.util.encoders.Base64Encoder;
import org.eclipse.andmore.android.certmanager.CertificateManagerActivator;
import org.eclipse.andmore.android.certmanager.exception.KeyStoreManagerException;
import org.eclipse.andmore.android.certmanager.packaging.PackageFile;
import org.eclipse.andmore.android.certmanager.ui.model.IKeyStoreEntry;
import org.eclipse.andmore.android.common.log.StudioLogger;

/**
 * Utility class used to sign package files.
 */
public class PackageFileSigner {
	public static final String MOTODEV_STUDIO = "MOTODEV Studio";

	/**
	 * Signs a package file
	 * 
	 * @param packageFile
	 *            the package file to sign
	 * @param certificateAlias
	 *            the signing certificate alias
	 * @param createdBy
	 *            Created-By manifest attribute
	 * @throws SignException
	 *             if a processing error occurs during the signing process
	 * @throws UnrecoverableKeyException
	 */
	public static void signPackage(PackageFile packageFile, IKeyStoreEntry keystoreEntry, String keyEntryPassword,
			String createdBy) throws SignException, UnrecoverableKeyException {

		try {
			Base64Encoder encoder = new Base64Encoder();
			MessageDigest messageDigest = MessageDigest.getInstance(ISignConstants.SHA1);

			addFilesDigestsToManifest(packageFile, encoder, messageDigest);

			addSignatureFiles(packageFile, keystoreEntry, keyEntryPassword, encoder, createdBy);
		} catch (UnrecoverableKeyException e) {
			throw e;
		} catch (Exception e) {
			StudioLogger.error(PackageFileSigner.class, "Error signing package", e);
			throw new SignException(e.getMessage(), e);
		}

	}

	/**
	 * Remove package signature files
	 * 
	 * @param packageFile
	 * @throws IOException
	 */
	public static void removePackageSignature(PackageFile packageFile) throws IOException {
		packageFile.removeMetaEntryFiles();
	}

	/**
	 * Generates the digests for all the files in the package and puts them in
	 * the manifest
	 * 
	 * @param packageFile
	 *            the package file being signed
	 * @param encoder
	 *            the BASE64 encoder
	 * @param messageDigest
	 *            the message digest
	 * @throws IOException
	 *             if an I/O error occurs when reading the files contained in
	 *             the package
	 */
	private static void addFilesDigestsToManifest(PackageFile packageFile, Base64Encoder encoder,
			MessageDigest messageDigest) throws IOException {
		InputStream fileInputStream = null;
		ReadableByteChannel rc = null;
		ByteArrayOutputStream encodedStream = null;

		// for each entry in the package file
		for (String entryName : packageFile.getEntryNames()) {
			File file = packageFile.getEntryFile(entryName);
			if (file.isFile()) {
				try {
					// read the file contents
					fileInputStream = new FileInputStream(file);
					rc = Channels.newChannel(fileInputStream);
					ByteBuffer byteBuffer = ByteBuffer.allocate((int) file.length());
					rc.read(byteBuffer);

					// compute the digest
					messageDigest.reset();
					byte[] digestedArray = messageDigest.digest(byteBuffer.array());

					encodedStream = new ByteArrayOutputStream();
					encoder.encode(digestedArray, 0, digestedArray.length, encodedStream);
					String digestedMessage = encodedStream.toString();

					// put the digest in the manifest file
					Attributes jarEntryAttributes = new Attributes();
					jarEntryAttributes.putValue(ISignConstants.SHA1_DIGEST, digestedMessage);
					packageFile.getManifest().getEntries().put(entryName, jarEntryAttributes);
				} finally {
					try {
						if (encodedStream != null) {
							encodedStream.close();
						}
						if (rc != null) {
							rc.close();
						}
						if (fileInputStream != null) {
							fileInputStream.close();
						}
					} catch (IOException e) {
						StudioLogger.error("Could not close stream while signing package. " + e.getMessage());
					}
				}
			}
		}
	}

	/**
	 * Adds the signature file and the signature block file to the package
	 * 
	 * @param packageFile
	 *            the package file being signed
	 * @param certificateAlias
	 *            the signing certificate alias
	 * @param encoder
	 *            the BASE64 encoder
	 * @param messageDigest
	 *            the message digest
	 * @param createdBy
	 *            Created-By manifest attribute
	 * @throws SignException
	 *             if a processing error occurs during the signing process
	 * @throws IOException
	 *             if an I/O error occurs during the signing process
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreManagerException
	 * @throws KeyStoreException
	 * @throws UnrecoverableKeyException
	 */
	private static void addSignatureFiles(PackageFile packageFile, IKeyStoreEntry keystoreEntry,
			String keyEntryPassword, Base64Encoder encoder, String createdBy) throws IOException, SignException,
			UnrecoverableKeyException, KeyStoreException, KeyStoreManagerException, NoSuchAlgorithmException {
		// signature file
		SignatureFile signatureFile = new SignatureFile(packageFile, keystoreEntry.getAlias(), encoder, createdBy);

		File sigFile = File.createTempFile(CertificateManagerActivator.TEMP_FILE_PREFIX, null);
		FileOutputStream sigFileOutStream = null;

		try {
			sigFileOutStream = new FileOutputStream(sigFile);
			signatureFile.write(sigFileOutStream);
		} finally {
			if (sigFileOutStream != null) {
				try {
					sigFileOutStream.close();
				} catch (IOException e) {
					StudioLogger.error("Could not close stream while adding signature files to package. "
							+ e.getMessage());
				}
			}
		}

		packageFile.setTempEntryFile(signatureFile.toString(), sigFile);

		// signature block file
		SignatureBlockFile signatureBlockFile = new SignatureBlockFile(signatureFile, keystoreEntry, keyEntryPassword);

		File sigBlockFile = File.createTempFile(CertificateManagerActivator.TEMP_FILE_PREFIX, null);
		FileOutputStream sigBlockFileOutStream = null;

		try {
			sigBlockFileOutStream = new FileOutputStream(sigBlockFile);
			signatureBlockFile.write(sigBlockFileOutStream);
		} finally {
			if (sigBlockFileOutStream != null) {
				try {
					sigBlockFileOutStream.close();
				} catch (IOException e) {
					StudioLogger.error("Could not close stream while adding signature files to package. "
							+ e.getMessage());
				}
			}
		}

		packageFile.setTempEntryFile(signatureBlockFile.toString(), sigBlockFile);
	}
}
