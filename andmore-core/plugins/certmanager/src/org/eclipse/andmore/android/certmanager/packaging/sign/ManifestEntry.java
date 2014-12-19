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
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.jar.Attributes;

import org.bouncycastle.util.encoders.Base64Encoder;
import org.eclipse.andmore.android.common.log.StudioLogger;

/**
 * A Class representing a Manifest Entry.
 */
public class ManifestEntry {
	/**
	 * New line string according Jar specification
	 */
	public static final String MANIFEST_NEW_LINE = "\r\n";

	public static final String ENTRY_NAME_ATTRIBUTE = "Name: ";

	public static final String UTF8_CHARSET = "UTF-8";

	/**
	 * Safe line size limit according Jar Specification
	 */
	public static final int SAFE_LIMIT = 72;

	private final String name;

	private final Attributes attributes;

	/**
	 * Create a new ManifestEntry with the desired name and attributes
	 * 
	 * @param name
	 * @param attr
	 */
	public ManifestEntry(String name, Attributes attr) {
		this.name = name;
		this.attributes = attr;
	}

	/**
	 * Get the manifest as it will be written in the Manifest file
	 * 
	 * @return a byte array representing the manifest entry
	 */
	public byte[] toManifestEntryBytes() {
		byte[] result = null;
		DataOutputStream dataOut = null;
		ByteArrayOutputStream stream = null;
		try {
			stream = new ByteArrayOutputStream();
			dataOut = new DataOutputStream(stream);
			String nameField = wrap72bytes(ENTRY_NAME_ATTRIBUTE + name);
			dataOut.writeBytes(nameField);
			dataOut.writeBytes(MANIFEST_NEW_LINE);
			dataOut.writeBytes(getAttributesString());
			dataOut.writeBytes(MANIFEST_NEW_LINE);
			dataOut.writeBytes(MANIFEST_NEW_LINE);
			result = stream.toString().getBytes(UTF8_CHARSET);

		} catch (IOException e) {
			StudioLogger.error(ManifestEntry.class, "Error getting manifest like bytes");
		} finally {
			try {
				if (dataOut != null) {
					dataOut.close();
				}
				if (stream != null) {
					stream.close();
				}
			} catch (IOException e) {
				StudioLogger.error("Could not close stream while writing manifest" + e.getMessage());
			}
		}

		return result;
	}

	/**
	 * Digest the entire entry
	 * 
	 * @return a byte array with the SHA-1 sum of this entry
	 */
	public byte[] digest() {
		byte[] digested = null;
		try {
			MessageDigest digester = MessageDigest.getInstance("SHA-1");
			digester.reset();
			digester.update(toManifestEntryBytes());
			digested = digester.digest();
		} catch (NoSuchAlgorithmException e) {
			StudioLogger.error(ManifestEntry.class, "Error digesting manifest bytes");
		}
		return digested;
	}

	/**
	 * Get this Entry attributes as it will be written in the Manifest file
	 * 
	 * @return
	 */
	private String getAttributesString() {
		StringBuilder builder = new StringBuilder();
		Iterator<Object> it = attributes.keySet().iterator();
		while (it.hasNext()) {
			Object next = it.next();
			String line = wrap72bytes(next + ": " + attributes.get(next));
			if (it.hasNext()) {
				line += MANIFEST_NEW_LINE;
			}
			builder.append(line);
		}
		return builder.toString();
	}

	/**
	 * Wrap a string into another string with at most 72 bytes per line
	 * According Jar spec, each line must have at most 72 bytes
	 * 
	 * @param line
	 * @return the wrapped string
	 */
	public static String wrap72bytes(String line) {
		String returnString = line;

		if (line.length() > SAFE_LIMIT) {
			int maximumLength = SAFE_LIMIT - MANIFEST_NEW_LINE.length();
			StringBuilder wrapped = new StringBuilder();
			int index = maximumLength;
			String first = line.substring(0, index); // get first SAFE_LIMIT -
														// MANIFEST_NEW_LINE.length()
														// bytes
			first += MANIFEST_NEW_LINE;
			wrapped.append(first);
			while (index < line.length()) {
				String medium = " ";
				if ((index + maximumLength) < line.length()) {
					medium += line.substring(index, index + maximumLength); // get
																			// the
																			// maximum
																			// length
																			// minus
																			// the
																			// blank
																			// space
																			// size
				} else {
					medium += line.substring(index);
				}
				wrapped.append(medium);
				index += maximumLength;
			}
			returnString = wrapped.toString();
		}

		return returnString;
	}

	/**
	 * Get this entry name
	 * 
	 * @return this entry name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get this entry ready to be written in the Signature File
	 * 
	 * @return this entry ready to be written in the Signature File
	 * @throws IOException
	 *             if some error occurs during encoding
	 */
	public String toDigestedManifestEntry() throws IOException {
		Base64Encoder encoder = new Base64Encoder();
		StringBuilder builder = new StringBuilder();
		ByteArrayOutputStream output = null;

		try {
			output = new ByteArrayOutputStream();

			builder.append(wrap72bytes(ENTRY_NAME_ATTRIBUTE + name));
			builder.append(MANIFEST_NEW_LINE);
			builder.append(ISignConstants.SHA1_DIGEST + ": ");

			byte[] digest = digest();
			encoder.encode(digest, 0, digest.length, output);

			builder.append(output.toString());
			builder.append(MANIFEST_NEW_LINE);
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					StudioLogger.error("Could not close stream: " + e.getMessage());
				}
			}
		}
		return builder.toString();
	}
}
