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

import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

//import sun.security.pkcs.ContentInfo;
//import sun.security.pkcs.PKCS7;
//import sun.security.pkcs.SignerInfo;
//import sun.security.x509.AlgorithmId;
//import sun.security.x509.X500Name;

import org.eclipse.andmore.android.certmanager.CertificateManagerActivator;
import org.eclipse.andmore.android.certmanager.exception.KeyStoreManagerException;
import org.eclipse.andmore.android.certmanager.ui.model.IKeyStoreEntry;
import org.eclipse.andmore.android.common.log.StudioLogger;

/**
 * This class implements the signature block file from jar mechanism used in
 * packaging
 */
public class SignatureBlockFile {

	/**
	 * the signature file
	 */
	private SignatureFile signatureFile;

	/**
	 * A certificate from keystore
	 */
	private IKeyStoreEntry keystoreEntry;

	/**
	 * The password of the certificate.
	 */
	private String keyEntryPassword;

	/**
	 * Default Constructor
	 * 
	 * @param signatureFile
	 *            the signature file
	 * @param alias
	 *            the certificate alias
	 */
	public SignatureBlockFile(SignatureFile signatureFile, IKeyStoreEntry keystoreEntry, String keyEntryPassword) {
		this.keyEntryPassword = keyEntryPassword;
		this.keystoreEntry = keystoreEntry;
		this.signatureFile = signatureFile;
	}

	/**
	 * To string method override
	 * 
	 * @return the signature block file name with relative path from root.
	 *         Frequently META-INF/alias.RSA or .DSA
	 */
	@Override
	public String toString() {
		String result = new String();
		try {
			result = new StringBuilder(CertificateManagerActivator.METAFILES_DIR)
					.append(CertificateManagerActivator.JAR_SEPARATOR).append(ISignConstants.SIGNATURE_FILE_NAME)
					.append(".").append(getBlockAlgorithm()).toString();
		} catch (UnrecoverableKeyException e) {
			StudioLogger.error("Could not generate signature block file name.");
		} catch (KeyStoreException e) {
			StudioLogger.error("Could not generate signature block file name.");
		} catch (NoSuchAlgorithmException e) {
			StudioLogger.error("Could not generate signature block file name.");
		} catch (KeyStoreManagerException e) {
			StudioLogger.error("Could not generate signature block file name.");
		}

		return result;
	}

	/**
	 * Gets the block file algorithm
	 * 
	 * @return the signature block file algorithm to be used
	 * @throws KeyStoreManagerException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws UnrecoverableKeyException
	 * @throws
	 */
	private String getBlockAlgorithm() throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException,
			KeyStoreManagerException {
		return keystoreEntry.getKey(this.keyEntryPassword).getAlgorithm();
	}

	/**
	 * Writes this file to an output stream
	 * 
	 * @param outputStream
	 *            the output stream to write the file
	 * @throws IOException
	 *             if an I/O error occurs during the signing process
	 * @throws SignException
	 *             if a processing error occurs during the signing process
	 * @throws KeyStoreManagerException
	 * @throws KeyStoreException
	 * @throws UnrecoverableKeyException
	 * @throws NoSuchAlgorithmException
	 */
	public void write(OutputStream outputStream) throws IOException, SignException, UnrecoverableKeyException,
			KeyStoreException, KeyStoreManagerException, NoSuchAlgorithmException {
		// get certificate from entry
		X509Certificate[] certChain = { keystoreEntry.getX509Certificate() };
		if (certChain.length > 0) {
			// get some info from certificate as issuer, serial and algorithm
			X500Principal issuer = certChain[0].getIssuerX500Principal();
			String serial = certChain[0].getSerialNumber().toString();
			String blockalgorithm = getBlockAlgorithm();

			String digestAlgotithm = null;
			// determine the algorithm to be used to cipher the block file
			if (blockalgorithm.equalsIgnoreCase(ISignConstants.DSA)) {
				digestAlgotithm = ISignConstants.SHA1;
			} else if (blockalgorithm.equalsIgnoreCase(ISignConstants.RSA)) {
				digestAlgotithm = ISignConstants.MD5;
			} else {
				StudioLogger.error(SignatureBlockFile.class,
						"Signing block algorithm not supported. Key algorithm must be DSA or RSA");

				throw new SignException("Signing block algorithm not supported");
			}

			String signatureAlgorithm = digestAlgotithm + ISignConstants.ALGORITHM_CONNECTOR + blockalgorithm;

			// TODO: FIX ME. Migrate to Bouncy Castle instead of using Sun
			// classes directly.
			// AlgorithmId digestAlg;
			// try
			// {
			// digestAlg = AlgorithmId.get(digestAlgotithm);
			// AlgorithmId privateKeyAlg = AlgorithmId.get(blockalgorithm);
			// // write the certificate with signature file and cipher it
			// Signature sig = Signature.getInstance(signatureAlgorithm);
			// sig.initSign(keystoreEntry.getPrivateKey(this.keyEntryPassword));
			//
			// PKCS7 block = null;
			// ByteArrayOutputStream baos = null;
			// ByteArrayOutputStream signature = null;
			//
			// try
			// {
			// baos = new ByteArrayOutputStream();
			// signatureFile.write(baos);
			//
			// ContentInfo contentInfo = new ContentInfo(ContentInfo.DATA_OID,
			// null);
			// sig.update(baos.toByteArray());
			//
			// signature = new ByteArrayOutputStream();
			// signature.write(sig.sign());
			//
			// SignerInfo si =
			// new SignerInfo(new X500Name(issuer.getName()), new
			// BigInteger(serial),
			// digestAlg, privateKeyAlg, signature.toByteArray());
			//
			// AlgorithmId[] algs =
			// {
			// digestAlg
			// };
			// SignerInfo[] infos =
			// {
			// si
			// };
			//
			// block = new PKCS7(algs, contentInfo, certChain, infos);
			// }
			// catch (IOException e)
			// {
			// StudioLogger.error(SignatureBlockFile.class,
			// "I/O error creating signature block file: " + e.getMessage());
			//
			// throw new
			// SignException("I/O error creating signature block file", e);
			// }
			// finally
			// {
			// if (baos != null)
			// {
			// baos.close();
			// }
			// if (signature != null)
			// {
			// signature.close();
			// }
			// }
			//
			// // I/O exceptions below are thrown unmodified
			// block.encodeSignedData(outputStream);
			// }
			// catch (NoSuchAlgorithmException nsae)
			// {
			// StudioLogger.error(SignatureBlockFile.class,
			// "Signing algorithm not supported: "
			// + nsae.getMessage());
			//
			// throw new SignException("Signing algorithm not supported", nsae);
			// }
			// catch (InvalidKeyException ike)
			// {
			// StudioLogger.error(SignatureBlockFile.class,
			// "Invalid key when creating signature block file: " +
			// ike.getMessage());
			//
			// throw new
			// SignException("Invalid key when creating signature block file",
			// ike);
			// }
			// catch (SignatureException se)
			// {
			// StudioLogger.error(SignatureBlockFile.class,
			// "Signature error when creating signature block file: " +
			// se.getMessage());
			//
			// throw new
			// SignException("Signature error creating signature block file",
			// se);
			// }
		}
		StudioLogger.info(SignatureBlockFile.class, "Created signature block file");
	}
}
