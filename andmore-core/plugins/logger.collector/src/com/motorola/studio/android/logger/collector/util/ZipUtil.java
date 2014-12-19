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
package com.motorola.studio.android.logger.collector.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Class with useful method to compact (zip) files and directories.
 */
public class ZipUtil {

	/**
	 * The default buffer.
	 */
	private static final int BUFFER = 2048;

	/**
	 * The output file.
	 */
	private File outputFile = null;

	/**
	 * This file represents the current directory.
	 */
	private File directory = null;

	/**
	 * A ZipOutputStream object.
	 */
	private ZipOutputStream zos = null;

	/**
	 * The path of current directory.
	 */
	private final String currentDirectory;

	/**
	 * Class constructor.
	 * 
	 * @param outputFile
	 *            The output to create the zip file.
	 * @param directory
	 *            The directory that will be compacted.
	 * @throws IOException
	 */
	public ZipUtil(String outputFile, String directory) throws IOException {
		this(new File(outputFile), new File(directory));
	}

	/**
	 * Class constructor.
	 * 
	 * @param outputFile
	 *            The file where the zip file will be created.
	 * @param directory
	 *            The directory that will be compacted.
	 * @throws IOException
	 */
	public ZipUtil(File outputFile, File directory) throws IOException {
		this.outputFile = outputFile;
		this.directory = directory;
		this.currentDirectory = directory.getAbsolutePath();
	}

	/**
	 * Compact the content.
	 * 
	 * @throws IOException
	 */
	public final void zip() throws IOException {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(outputFile);
			zos = new ZipOutputStream(fos);
			zipDir(directory);
			zos.flush();
		} finally {
			try {
				zos.close();
				fos.close();
			} catch (IOException io) {
				io.printStackTrace();
			}
		}
	}

	/**
	 * Compact directory the content.
	 * 
	 * @param dir
	 *            The directory
	 * @throws IOException
	 */
	private final void zipDir(File dir) throws IOException {
		if (!dir.getPath().equals(currentDirectory)) {
			String entryName = dir.getPath().substring(currentDirectory.length() + 1);
			entryName = entryName.replace('\\', '/');
			ZipEntry ze = new ZipEntry(entryName + "/"); //$NON-NLS-1$
			if ((dir != null) && dir.exists()) {
				ze.setTime(dir.lastModified());
			} else {
				ze.setTime(System.currentTimeMillis());
			}
			ze.setSize(0);
			ze.setMethod(ZipEntry.STORED);
			ze.setCrc(new CRC32().getValue());
			zos.putNextEntry(ze);
		}

		if (dir.exists() && dir.isDirectory()) {
			File[] fileList = dir.listFiles();
			for (int i = 0; i < fileList.length; i++) {
				if (fileList[i].isDirectory()) {
					zipDir(fileList[i]);
				}
				if (fileList[i].isFile()) {
					zipFile(fileList[i]);
				}
			}
		}
	}

	/**
	 * Compact the file content.
	 * 
	 * @param file
	 *            The file
	 * @throws IOException
	 */
	private void zipFile(File file) throws IOException {
		if (!file.equals(this.outputFile)) {
			BufferedInputStream bis = null;
			try {
				bis = new BufferedInputStream(new FileInputStream(file), BUFFER);

				String entryName = file.getPath().substring(currentDirectory.length() + 1);
				entryName = entryName.replace('\\', '/');
				ZipEntry fileEntry = new ZipEntry(entryName);
				zos.putNextEntry(fileEntry);

				byte[] data = new byte[BUFFER];
				int byteCount;
				while ((byteCount = bis.read(data, 0, BUFFER)) != -1) {
					zos.write(data, 0, byteCount);
				}

			} finally {
				bis.close();
			}
		}
	}

	/**
	 * Unpacks a zip file to the target directory.
	 * 
	 * @param zipFilePath
	 *            The zip file.
	 * @param destDirPath
	 *            The destination directory.
	 * @throws IOException
	 */
	public static void unzip(String zipFilePath, String destDirPath) throws IOException {
		try {
			File zipFile = new File(zipFilePath);
			File folder = new File(destDirPath);
			ZipFile zip = new ZipFile(zipFile);
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				unzipEntry(zip, entry, folder);
			}
		} catch (Exception e) {
			throw new RuntimeException("Error while trying to unzip jar file"); //$NON-NLS-1$
		}
	}

	/**
	 * Validate and write the stream of file.
	 * 
	 * @param zipFile
	 *            The zip file
	 * @param zipEntry
	 *            The zip entry
	 * @param folder
	 *            The folder
	 * @throws IOException
	 */
	private static void unzipEntry(ZipFile zipFile, ZipEntry zipEntry, File folder) throws IOException {
		if (zipEntry.isDirectory()) {
			org.eclipse.andmore.android.common.utilities.FileUtil.mkdir(new File(folder, zipEntry.getName()).getPath());
		} else {
			File outputFile = new File(folder, zipEntry.getName());
			if (!outputFile.getParentFile().exists()) {
				org.eclipse.andmore.android.common.utilities.FileUtil.mkdir(outputFile.getParentFile().getPath());
			}
			BufferedInputStream is = new BufferedInputStream(zipFile.getInputStream(zipEntry));
			BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(outputFile));
			try {
				org.eclipse.andmore.android.common.utilities.FileUtil.copy(new File(zipFile.getName()), outputFile);
			} finally {
				os.close();
				is.close();
			}
		}
	}
}
