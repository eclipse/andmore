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

package org.eclipse.andmore.android.certmanager.packaging;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

import org.eclipse.andmore.android.certmanager.CertificateManagerActivator;
import org.eclipse.andmore.android.common.IAndroidConstants;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.AndroidUtils;
import org.eclipse.andmore.android.common.utilities.FileUtil;
import org.eclipse.core.runtime.IBundleGroup;
import org.eclipse.core.runtime.IBundleGroupProvider;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

/**
 * This class is an in-memory package file representation.
 */
public class PackageFile {
	private static final String ANDROID_FEATURE = "org.eclipse.andmore.android.feature";

	/*
	 * Map of entries contained in this package file
	 */
	private final Map<String, File> entryMap = new HashMap<String, File>();

	/*
	 * Map of temporary entries contained in this package file (it duplicates
	 * the entries on entryMap)
	 */
	private final Map<String, File> tempEntryMap = new HashMap<String, File>();

	/*
	 * Package manifest
	 */
	private Manifest manifest;

	private int apiVersion;

	private String targetName;

	private Set<String> rawFiles = new HashSet<String>();

	/**
	 * Creates an empty PackageFile.
	 * 
	 * @param createdBy
	 *            Created-By manifest attribute
	 */
	public PackageFile(String createdBy) {
		targetName = "";
		apiVersion = -1;
		manifest = createManifest(createdBy);
	}

	/**
	 * Creates an empty PackageFile
	 * 
	 * @param createdBy
	 *            Created-By manifest attribute
	 */
	public PackageFile(String createdBy, String targetName, int apiVersion) {
		this.targetName = targetName;
		this.apiVersion = apiVersion;
		manifest = createManifest(createdBy);
	}

	/**
	 * Creates a PackageFile from an existing JarFile
	 * 
	 * @param jarFile
	 *            the base jar file
	 * @param apiVersion
	 * @param targetName
	 * @throws IOException
	 *             if an I/O error occurs when reading the contents of the base
	 *             jar file
	 */
	public PackageFile(JarFile jarFile) throws IOException {
		this(jarFile, "", -1);
	}

	/**
	 * Creates a PackageFile from an existing JarFile
	 * 
	 * @param jarFile
	 *            the base jar file
	 * @param apiVersion
	 * @param targetName
	 * @throws IOException
	 *             if an I/O error occurs when reading the contents of the base
	 *             jar file
	 */
	public PackageFile(JarFile jarFile, String targetName, int apiVersion) throws IOException {
		manifest = jarFile.getManifest();
		this.targetName = targetName;
		this.apiVersion = apiVersion;
		String createdBy = generateStudioFingerprint();
		if (manifest == null) {
			manifest = createManifest(createdBy);
		}

		// go through all the entries in the base jar file
		Enumeration<JarEntry> entryEnum = jarFile.entries();

		while (entryEnum.hasMoreElements()) {
			JarEntry entry = entryEnum.nextElement();
			if (!entry.getName().equalsIgnoreCase(
					CertificateManagerActivator.METAFILES_DIR + CertificateManagerActivator.JAR_SEPARATOR
							+ CertificateManagerActivator.MANIFEST_FILE_NAME)) {
				// create a temporary file for this entry
				InputStream is = jarFile.getInputStream(entry);
				File tempFile = File.createTempFile(CertificateManagerActivator.TEMP_FILE_PREFIX, null);
				tempFile.deleteOnExit();

				// copy contents from the original file to the temporary file
				BufferedInputStream bis = null;
				BufferedOutputStream bos = null;

				try {
					bis = new BufferedInputStream(is);
					bos = new BufferedOutputStream(new FileOutputStream(tempFile));

					int c;
					while ((c = bis.read()) >= 0) {
						bos.write(c);
					}
				} finally {
					if (bis != null) {
						bis.close();
					}

					if (bos != null) {
						bos.close();
					}
				}

				// add the temporary file to the package file
				setTempEntryFile(entry.getName(), tempFile);

				// check if the entry is not compressed to keep it this way
				if (entry.getMethod() == ZipEntry.STORED) {
					rawFiles.add(entry.getName());
				}
			}
		}
	}

	private String generateStudioFingerprint() {
		IBundleGroupProvider[] providers = Platform.getBundleGroupProviders();
		List<IBundleGroup> groups = new ArrayList<IBundleGroup>();
		if (providers != null) {
			for (int i = 0; i < providers.length; ++i) {
				IBundleGroup[] bundleGroups = providers[i].getBundleGroups();
				groups.addAll(Arrays.asList(bundleGroups));
			}
		}
		String version = "";
		for (IBundleGroup group : groups) {
			if (group.getIdentifier().equals(ANDROID_FEATURE)) {
				version = group.getVersion();
				break;
			}
		}

		StringBuilder stringBuilder = new StringBuilder(CertificateManagerActivator.CREATED_BY_FIELD_VALUE);
		stringBuilder.append(" v");
		stringBuilder.append(version);
		stringBuilder.append(" - ");
		stringBuilder.append(Platform.getOS());
		stringBuilder.append(", ");
		stringBuilder.append(Platform.getOSArch());
		stringBuilder.append(". ");
		if (targetName.trim().length() > 0) {
			stringBuilder.append("Android target - ");
			stringBuilder.append(targetName);
			stringBuilder.append(", ");
		}
		if (apiVersion >= 0) {
			stringBuilder.append("API version - ");
			stringBuilder.append(apiVersion);
			stringBuilder.append(".");
		}
		return stringBuilder.toString();
	}

	/**
	 * Gets the names for all the entries in this package file
	 * 
	 * @return Set containing the names for all the entries in this package file
	 */
	public Set<String> getEntryNames() {
		return Collections.unmodifiableSet(entryMap.keySet());
	}

	/**
	 * Gets the File object for a given entry
	 * 
	 * @param entryName
	 *            the entry name
	 * @return the File object corresponding to entryName
	 */
	public File getEntryFile(String entryName) {
		return entryMap.get(entryName);
	}

	/**
	 * Puts a File object as a named entry in this package file
	 * 
	 * @param entryName
	 *            the entry name
	 * @param file
	 *            the File object corresponding to entryName
	 */
	public void setEntryFile(String entryName, File file) {
		entryMap.put(entryName, file);
	}

	/**
	 * Puts a Temporary File object as a named entry in this package file
	 * 
	 * @param entryName
	 *            the entry name
	 * @param tempFile
	 *            the temporary file object corresponding to entryName
	 */
	public void setTempEntryFile(String entryName, File tempFile) {
		entryMap.put(entryName, tempFile);
		tempEntryMap.put(entryName, tempFile);
	}

	/**
	 * Remove the named entry of files map of this package
	 * 
	 * @param entryName
	 *            the name of entry to be removed
	 * @throws IOException
	 */
	public void removeEntryFile(String entryName) throws IOException {
		File entryFile = entryMap.get(entryName);
		if (entryFile != null) {
			entryMap.remove(entryName);
			if (tempEntryMap.containsKey(entryName)) {
				tempEntryMap.remove(entryName);
				deleteFile(entryFile);
			}
		}
	}

	/**
	 * Remove the meta entry files (files under META-INF folder)
	 * 
	 * @throws IOException
	 */
	public void removeMetaEntryFiles() throws IOException {
		String createdBy = manifest.getMainAttributes().getValue(CertificateManagerActivator.CREATED_BY_FIELD);
		Set<String> entries = new HashSet<String>(getEntryNames());
		for (String entry : entries) {
			if (entry.startsWith(CertificateManagerActivator.METAFILES_DIR)) {
				removeEntryFile(entry);
			}
		}
		Manifest cleanManifest = new Manifest();
		cleanManifest.getMainAttributes().putAll(manifest.getMainAttributes());
		if ("".equals(targetName) && (apiVersion <= 0)) // Just removing
														// signatures.
		{
			cleanManifest.getMainAttributes().putValue(CertificateManagerActivator.CREATED_BY_FIELD, createdBy);
		} else {
			cleanManifest.getMainAttributes().putValue(CertificateManagerActivator.CREATED_BY_FIELD,
					generateStudioFingerprint());
		}
		manifest = cleanManifest;
	}

	private void writeCompressed(JarOutputStream jarOut, String entryName) throws IOException {
		File file = entryMap.get(entryName);
		if ((file.exists()) && (file.isFile())) {
			JarEntry entry = new JarEntry(entryName);
			jarOut.putNextEntry(entry);
			WritableByteChannel outputChannel = null;
			FileChannel readFromFileChannel = null;
			FileInputStream inputStream = null;
			try {
				outputChannel = Channels.newChannel(jarOut);
				inputStream = new FileInputStream(file);
				readFromFileChannel = inputStream.getChannel();
				readFromFileChannel.transferTo(0, file.length(), outputChannel);
			} finally {
				try {
					if (jarOut != null) {
						jarOut.closeEntry();
					}
					if (readFromFileChannel != null) {
						readFromFileChannel.close();
					}
					if (inputStream != null) {
						inputStream.close();
					}
				} catch (IOException e) {
					StudioLogger.error("Could not close stream: ", e.getMessage()); //$NON-NLS-1$
				}
			}
		}

	}

	private void writeRaw(JarOutputStream jarOut, String entryName) throws IOException {
		FileInputStream inputStream = null;
		File file = entryMap.get(entryName);
		if ((file.exists()) && (file.isFile())) {
			CRC32 crc = new CRC32();
			JarEntry entry = new JarEntry(entryName);
			entry.setMethod(ZipEntry.STORED);
			entry.setSize(file.length());
			WritableByteChannel outputChannel = null;
			FileChannel readFromFileChannel = null;
			try {
				outputChannel = Channels.newChannel(jarOut);
				inputStream = new FileInputStream(file);
				readFromFileChannel = inputStream.getChannel();

				ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024);
				crc.reset();
				while (readFromFileChannel.read(buffer) > 0) {
					buffer.flip();
					byte[] byteArray = new byte[buffer.limit()];
					buffer.get(byteArray, 0, buffer.limit());
					crc.update(byteArray);
					buffer.clear();
				}
				entry.setCrc(crc.getValue());
				jarOut.putNextEntry(entry);
				readFromFileChannel.transferTo(0, file.length(), outputChannel);
			} finally {
				if (inputStream != null) {
					inputStream.close();
				}
				if (readFromFileChannel != null) {
					readFromFileChannel.close();
				}
				jarOut.closeEntry();
			}
		}
	}

	/**
	 * Writes this package file to an output stream
	 * 
	 * @param outputStream
	 *            the stream to write the package to
	 * @throws IOException
	 *             if an I/O error occurs when writing the package contents to
	 *             the output stream
	 */
	public void write(OutputStream outputStream) throws IOException {
		// create a jar output stream
		JarOutputStream jarOut = null;

		try {
			jarOut = new JarOutputStream(outputStream, manifest);

			// for each entry in the package file
			for (String jarEntryName : entryMap.keySet()) {
				if (jarEntryName.contains("raw/") || rawFiles.contains(jarEntryName)) {
					writeRaw(jarOut, jarEntryName);
				} else {
					writeCompressed(jarOut, jarEntryName);
				}
			}
		} finally {
			if (jarOut != null) {
				try {
					jarOut.finish();
					jarOut.close();
				} catch (IOException e) {
					StudioLogger.error("Could not close stream while writing jar file. " + e.getMessage());
				}
			}
		}
	}

	/**
	 * Calculate the package total size returns long
	 */
	public long getTotalSize() {
		long totalSize = 0;

		for (String jarEntryName : entryMap.keySet()) {
			File file = entryMap.get(jarEntryName);
			if ((file.exists()) && (file.isFile())) {
				totalSize += file.length();
			}
		}

		return totalSize;
	}

	/**
	 * Gets the package manifest
	 * 
	 * @return the package manifest
	 */
	public Manifest getManifest() {
		return manifest;
	}

	/**
	 * Remove the temporary entry files
	 * 
	 * @throws IOException
	 */
	public void removeTemporaryEntryFiles() throws IOException {
		Set<String> tempEntries = new HashSet<String>(Collections.unmodifiableSet(tempEntryMap.keySet()));
		for (String tempEntry : tempEntries) {
			removeEntryFile(tempEntry);
		}
	}

	/*
	 * Delete a single file from the filesystem.
	 * 
	 * @param fileToDelete A <code>File</code> object representing the file to
	 * be deleted.
	 * 
	 * @throws IOException if any problem occurs deleting the file.
	 */
	private void deleteFile(File fileToDelete) throws IOException {
		if ((fileToDelete != null) && fileToDelete.exists() && fileToDelete.isFile() && fileToDelete.canWrite()) {
			fileToDelete.delete();
		} else {
			String errorMessage = "";
			if (fileToDelete == null) {
				errorMessage = "Null pointer for file to delete.";
			} else {
				if (!fileToDelete.exists()) {
					errorMessage = "The file " + fileToDelete.getName() + " does not exist.";
				} else {
					if (!fileToDelete.isFile()) {
						errorMessage = fileToDelete.getName() + " is not a file.";
					} else {
						if (!fileToDelete.canWrite()) {
							errorMessage = "Cannot write to " + fileToDelete.getName();
						}
					}
				}

			}
			throw new IOException("Cannot delete file: " + errorMessage);
		}
	}

	/**
	 * Create a new Manifest with the basic values and the created by string
	 * 
	 * @param createdBy
	 *            who is creating this manifest
	 * @return a new Manifest with basic values and desired created by string
	 */
	public Manifest createManifest(String createdBy) {
		Manifest newManifest = new Manifest();
		Attributes mainAttributes = newManifest.getMainAttributes();
		mainAttributes.putValue(Attributes.Name.MANIFEST_VERSION.toString(),
				CertificateManagerActivator.MANIFEST_VERSION);
		mainAttributes.putValue(CertificateManagerActivator.CREATED_BY_FIELD, createdBy);
		return newManifest;
	}

	/**
	 * Execute the zipalign for a certain apk
	 * 
	 * @param apk
	 */
	public static void zipAlign(File apk) {
		// String zipAlign = SdkUtils.getSdkToolsPath();
		String zipAlign = AndroidUtils.getSDKPathByPreference() + IPath.SEPARATOR + IAndroidConstants.FD_TOOLS;
		try {
			File tempFile = File.createTempFile("_tozipalign", ".apk");
			FileUtil.copyFile(apk, tempFile);

			if (!zipAlign.endsWith(File.separator)) {
				zipAlign += File.separator;
			}
			zipAlign += Platform.getOS().equals(Platform.OS_WIN32) ? "zipalign.exe" : "zipalign";

			String[] command = new String[] { zipAlign, "-f", "-v", "4", tempFile.getAbsolutePath(),
					apk.getAbsolutePath() };
			StringBuilder commandLine = new StringBuilder();
			for (String commandPart : command) {
				commandLine.append(commandPart);
				commandLine.append(" ");
			}

			StudioLogger.info(PackageFile.class, "Zipaligning package: " + commandLine.toString());
			Runtime.getRuntime().exec(command);
		} catch (IOException e) {
			StudioLogger.error(PackageFile.class, "Error while zipaligning package", e);
		} catch (Exception e) {
			StudioLogger.error(PackageFile.class, "Zipalign application cannot be executed - insuficient permissions",
					e);
		}
	}
}
