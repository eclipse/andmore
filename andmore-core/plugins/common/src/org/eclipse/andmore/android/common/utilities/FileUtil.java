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
package org.eclipse.andmore.android.common.utilities;

import static org.eclipse.andmore.android.common.log.StudioLogger.warn;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.andmore.android.common.CommonPlugin;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.i18n.UtilitiesNLS;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;

/**
 * DESCRIPTION: This class provides utility methods to handle files, like
 * copying and deleting directories sub-trees.
 * 
 * USAGE: See public methods
 */
public class FileUtil {
	public static final int OS_WINDOWS = 0;

	public static final int OS_LINUX = 1;

	public static final char[] MAC_SPECIAL_CHAR = { '\\', ' ', '\'', '"', '!', '@', '$', '&', '*', '(', ')', '=', '`',
			'[', ']', '{', '}', '^', '<', '>', ':', ';', '?', '|' };

	public static final char[] LINUX_SPECIAL_CHAR = { '\\', ' ', '\'', '"', '!', '$', '&', '*', '(', ')', '=', '`',
			'[', ']', '{', '}', '^', '<', '>', ':', ';', '?', '|' };

	public static final char ESCAPE_CHAR = '\\';

	private static final int BUFFER_SIZE = 1024;

	/**
	 * Copy full list of contents from a directory to another. The source
	 * directory is not created within the target one.
	 *
	 * @param fromDir
	 *            Source directory.
	 * @param toDir
	 *            Target directory.
	 * 
	 * @param IOException
	 *            if I/O occurs
	 */
	public static void copyDir(File fromDir, File toDir) throws IOException {
		if ((fromDir != null) && fromDir.isDirectory() && fromDir.canRead() && (toDir != null) && toDir.isDirectory()
				&& toDir.canWrite()) {
			for (File child : fromDir.listFiles()) {
				if (child.isFile()) {
					copyFile(child, new File(toDir, child.getName()));
				} else {
					// create directory and copy its children recursively
					File newDir = new File(toDir.getAbsolutePath(), child.getName());
					newDir.mkdir();
					copyDir(child, newDir);
				}
			}

			StudioLogger.info("The directory " + fromDir.getName() + " was successfully copied to " //$NON-NLS-1$ //$NON-NLS-2$
					+ toDir.getName() + "."); //$NON-NLS-1$

		} else {
			// error detected
			String errorMessage = ""; //$NON-NLS-1$
			if (fromDir == null) {
				errorMessage = "Null pointer for source directory."; //$NON-NLS-1$
			} else {
				if (!fromDir.isDirectory()) {
					errorMessage = fromDir.getName() + " is not a directory."; //$NON-NLS-1$
				} else {
					if (!fromDir.canRead()) {
						errorMessage = "Cannot read from " + fromDir.getName() + "."; //$NON-NLS-1$ //$NON-NLS-2$
					} else {
						if (toDir == null) {
							errorMessage = "Null pointer for destination directory."; //$NON-NLS-1$
						} else {
							if (!toDir.isDirectory()) {
								errorMessage = toDir.getName() + " is not a directory."; //$NON-NLS-1$
							} else {
								if (!toDir.canWrite()) {
									errorMessage = "Cannot write to" + toDir.getName() + "."; //$NON-NLS-1$ //$NON-NLS-2$
								}
							}
						}
					}
				}
			}
			StudioLogger.error(errorMessage);
			throw new IOException("Error copying directory: " + errorMessage); //$NON-NLS-1$
		}
	}

	/**
	 * Copies the source file to the given target.
	 *
	 * @param source
	 *            - the absolute path of the source file.
	 * @param target
	 *            - the absolute path of the target file.
	 */
	public static void copyFile(File source, File target) throws IOException {
		copyFile(source.getAbsolutePath(), target.getAbsolutePath());
	}

	/**
	 * Copies the source file to the given target.
	 *
	 * @param source
	 *            - the absolute path of the source file.
	 * @param target
	 *            - the absolute path of the target file.
	 */
	private static void copyFile(String source, String target) throws IOException {
		FileChannel sourceFileChannel = null;
		FileChannel targetFileChannel = null;
		FileInputStream sourceFileInStream = null;
		FileOutputStream targetFileOutStream = null;
		try {
			sourceFileInStream = new FileInputStream(source);
			sourceFileChannel = sourceFileInStream.getChannel();
			targetFileOutStream = new FileOutputStream(target);
			targetFileChannel = targetFileOutStream.getChannel();
			targetFileChannel.transferFrom(sourceFileChannel, 0, sourceFileChannel.size());
			StudioLogger.info("The file " + source + " was successfully copied to " + target + "."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		} catch (IOException e) {
			StudioLogger.error("Error copying file" + source + "to " + target + "."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			throw e;
		} finally {
			try {
				if (sourceFileChannel != null) {
					sourceFileChannel.close();
				}
			} catch (IOException e) {
				StudioLogger.error("Error closing file " + source + "."); //$NON-NLS-1$ //$NON-NLS-2$
				throw e;
			}

			try {
				if (targetFileChannel != null) {
					targetFileChannel.close();
				}
			} catch (IOException e) {
				StudioLogger.error("Error closing file" + target + "."); //$NON-NLS-1$ //$NON-NLS-2$
				throw e;
			}

			try {
				if (sourceFileInStream != null) {
					sourceFileInStream.close();
				}
			} catch (IOException e) {
				StudioLogger.error("Error closing file" + source + "."); //$NON-NLS-1$ //$NON-NLS-2$
				throw e;
			}

			try {
				if (targetFileOutStream != null) {
					targetFileOutStream.close();
				}
			} catch (IOException e) {
				StudioLogger.error("Error closing file" + target + "."); //$NON-NLS-1$ //$NON-NLS-2$
				throw e;
			}

		}
	}

	/**
	 * This method deletes the directory, all files and all subdirectories under
	 * it. If a deletion fails, the method stops attempting to delete and
	 * returns false.
	 *
	 * @param directory
	 *            The directory to be deleted
	 * @return Returns true if all deletions were successful. If the directory
	 *         doesn't exist returns false.
	 * @throws IOException
	 *             When the parameter isn't a directory
	 */
	public static boolean deleteDirRecursively(File directory) throws IOException {
		String dirName = ""; //$NON-NLS-1$

		boolean success = true;

		if (directory.exists()) {
			if (directory.isDirectory()) {
				dirName = directory.getName();
				File[] children = directory.listFiles();

				for (File element : children) {
					if (element.isFile()) {
						success = success && element.delete();
					} else {
						success = success && deleteDirRecursively(element);
					}
				}

				success = success && directory.delete();
			} else {
				String errorMessage = directory.getName() + " is not a diretory."; //$NON-NLS-1$
				StudioLogger.error(errorMessage);
				throw new IOException(errorMessage);
			}
		} else {
			String errorMessage = "The directory does not exist."; //$NON-NLS-1$
			StudioLogger.error(errorMessage);
			success = false;
			throw new IOException(errorMessage);
		}

		if ((success) && (!dirName.equals(""))) //$NON-NLS-1$
		{
			StudioLogger.info("The directory " + dirName + "was successfully deleted."); //$NON-NLS-1$ //$NON-NLS-2$
		}

		return success;
	}

	/**
	 * Delete a single file from the filesystem.
	 *
	 * @param fileToDelete
	 *            A <code>File</code> object representing the file to be
	 *            deleted.
	 * @throws IOException
	 *             if any problem occurs deleting the file.
	 */
	public static void deleteFile(File fileToDelete) throws IOException {
		if ((fileToDelete != null) && fileToDelete.exists() && fileToDelete.isFile() && fileToDelete.canWrite()) {
			fileToDelete.delete();
			StudioLogger.info("The file " + fileToDelete.getName() + "was successfully deleted."); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			String errorMessage = ""; //$NON-NLS-1$
			if (fileToDelete == null) {
				errorMessage = "Null pointer for file to delete."; //$NON-NLS-1$
			} else {
				if (!fileToDelete.exists()) {
					errorMessage = "The file " + fileToDelete.getName() + " does not exist."; //$NON-NLS-1$ //$NON-NLS-2$
				} else {
					if (!fileToDelete.isFile()) {
						errorMessage = fileToDelete.getName() + " is not a file."; //$NON-NLS-1$
					} else {
						if (!fileToDelete.canWrite()) {
							errorMessage = "Cannot write to " + fileToDelete.getName(); //$NON-NLS-1$
						}
					}
				}

			}

			StudioLogger.error(errorMessage);
			throw new IOException("Cannot delete file: " + errorMessage); //$NON-NLS-1$
		}
	}

	/**
	 * Delete a list of files from the filesystem.
	 *
	 * @param filesToDelete
	 *            A list of <code>File</code> objects representing the files to
	 *            be deleted.
	 * @throws IOException
	 *             if any problem occurs deleting the files.
	 */
	public static void deleteFilesOnList(List<File> filesToDelete) throws IOException {
		for (File element : filesToDelete) {
			if (element.exists()) {
				deleteFile((element));
			}
		}
	}

	/**
	 * Return the File Size in Bytes.
	 * 
	 * @param root
	 *            The root File, it can be a directory
	 * @return The size of the file in bytes
	 * @throws IOException
	 */
	public static int getFileSize(File root) throws IOException {
		int size = 0;
		if (root.isDirectory()) {
			for (File child : root.listFiles()) {
				size += FileUtil.getFileSize(child);
			}
		} else if (root.isFile()) {
			FileInputStream fis = new FileInputStream(root);
			int available;
			try {
				available = fis.available();
			} finally {
				try {
					fis.close();
				} catch (IOException e) {
					// Do thing.
				}
			}
			size = available;
		}
		return size;
	}

	/**
	 * getExtension(String fileName)
	 *
	 * @param fileName
	 *            returns the extension of a given file. "extension" here means
	 *            the final part of the string after the last dot.
	 *
	 * @return String containing the extension
	 */
	public static String getExtension(String fileName) {
		if (fileName != null) {
			int i = fileName.lastIndexOf(".") + 1; //$NON-NLS-1$
			return (i == 0) ? "" : fileName.substring(i); //$NON-NLS-1$
		} else {
			StudioLogger.error("The file " + fileName + " does not exist."); //$NON-NLS-1$ //$NON-NLS-2$
			return null;
		}
	}

	/**
	 * Get the list of all File objects that compose the path to the given File
	 * object
	 *
	 * @param aFile
	 *            the file whose path must be retrieved.
	 * @return a List with all the File objects that compose the path to the
	 *         given File object.
	 */
	public static List<File> getFilesComposingPath(File aFile) {
		List<File> fileList;

		if (aFile == null) {
			fileList = new ArrayList<File>();
		} else {
			fileList = getFilesComposingPath(aFile.getParentFile());
			fileList.add(aFile);
		}

		return fileList;
	}

	/**
	 * Retrieve the relative filename to access a targetFile from a homeFile
	 * parent directory. Notice that to actualy use a relative File object you
	 * must use the following new File(homeDir, relativeFilename) because using
	 * only new File(relativeFilename) would give you a file whose directory is
	 * the one set in the "user.dir" property.
	 *
	 * @param homeDir
	 *            the directory from where you want to access the targetFile
	 * @param targetFile
	 *            the absolute file or dir that you want to access via relative
	 *            filename from the homeFile
	 * @return the relative filename that describes the location of the
	 *         targetFile referenced from the homeFile dir
	 * @throws IOException
	 */
	public static String getRelativeFilename(File homeDir, File targetFile) throws IOException {
		StringBuffer relativePath = new StringBuffer();

		List<File> homeDirList = getFilesComposingPath(getCanonicalFile(homeDir));
		List<File> targetDirList = getFilesComposingPath(getCanonicalFile(targetFile));

		if (homeDirList.size() == 0) {
			StudioLogger.info("Home Dir has no parent."); //$NON-NLS-1$
		}

		if (targetDirList.size() == 0) {
			StudioLogger.info("Target Dir has no parent."); //$NON-NLS-1$
		}

		// get the index of the last common directory between sourceFile and
		// targetFile
		int commonIndex = -1;

		for (int i = 0; (i < homeDirList.size()) && (i < targetDirList.size()); i++) {
			File aHomeDir = homeDirList.get(i);
			File aTargetDir = targetDirList.get(i);

			if (aHomeDir.equals(aTargetDir)) {
				commonIndex = i;
			} else {
				break;
			}
		}

		// return from all remaining directories of the homeFile
		for (int i = commonIndex + 1; i < homeDirList.size(); i++) {
			relativePath.append(".."); //$NON-NLS-1$
			relativePath.append(File.separatorChar);
		}

		// enter into all directories of the target file
		// stops when reachs the file name and extension
		for (int i = commonIndex + 1; i < targetDirList.size(); i++) {
			File targetDir = targetDirList.get(i);
			relativePath.append(targetDir.getName());

			if (i != (targetDirList.size() - 1)) {
				relativePath.append(File.separatorChar);
			}
		}

		return relativePath.toString();
	}

	/**
	 * Return a list of file absolute paths under "baseDir" and under its
	 * subdirectories, recursively.
	 *
	 * @param baseDirToList
	 *            A string that represents the BaseDir to initial search.
	 * @return A List of filepaths of files under the "baseDir".
	 * @throws IOException
	 *             If the "baseDir" can not be read.
	 */
	public static List<String> listFilesRecursively(String baseDirToList) throws IOException {
		File baseDirToListFiles = new File(baseDirToList);
		List<String> listOfFiles = listFilesRecursively(baseDirToListFiles);

		return listOfFiles;
	}

	/**
	 * Return a list of file absolute paths under "baseDir" and under its
	 * subdirectories, recursively.
	 *
	 * @param baseDirToList
	 *            A file object that represents the "baseDir".
	 * @return A List of filepaths of files under the "baseDir".
	 * @throws IOException
	 *             If the "baseDir" can not be read.
	 */
	public static List<String> listFilesRecursively(File baseDirToList) throws IOException {
		List<String> listOfFiles = new ArrayList<String>();

		if (baseDirToList.exists() && baseDirToList.isDirectory() && baseDirToList.canRead()) {
			File[] children = baseDirToList.listFiles();

			for (File child : children) {
				if (child.isFile()) {
					listOfFiles.add(child.getAbsolutePath());
				} else {
					List<String> temporaryList = listFilesRecursively(child);
					listOfFiles.addAll(temporaryList);
				}
			}
		} else {
			String errorMessage = ""; //$NON-NLS-1$
			if (!baseDirToList.exists()) {
				errorMessage = "The base dir does not exist."; //$NON-NLS-1$
			} else {
				if (!baseDirToList.isDirectory()) {
					errorMessage = baseDirToList.getName() + "is not a directory."; //$NON-NLS-1$
				} else {
					if (!baseDirToList.canRead()) {
						errorMessage = "Cannot fread from " + baseDirToList.getName() + "."; //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			}

			StudioLogger.error(errorMessage);
			throw new IOException("Error listing files: " + errorMessage); //$NON-NLS-1$
		}

		return listOfFiles;
	}

	/**
	 * Calculate the canonical (an absolute filename without "\.\" and "\..\")
	 * that describe the file described by the absoluteFilename.
	 * 
	 * @param absoluteFilename
	 *            a file name that describe the full path of the file to use.
	 * @return the canonical File objecta
	 */
	public static File getCanonicalFile(String absoluteFilename) {
		return getCanonicalFile(new File(absoluteFilename));
	}

	/**
	 * Calculate the canonical (an absolute filename without "\.\" and "\..\")
	 * that describe the file described by the given location and filename.
	 * 
	 * @param location
	 *            the directory of the file to be used
	 * @param filename
	 *            (or a relative filename) of the file to be used
	 * @return the canonical File objecta
	 */
	public static File getCanonicalFile(File location, String filename) {
		return getCanonicalFile(new File(location, filename));
	}

	/**
	 * Calculate the canonical (an absolute filename without "\.\" and "\..\")
	 * that describe the given file.
	 * 
	 * @param aFile
	 *            the file whose cannonical path will be calculated
	 * @return the canonical File objecta
	 */
	public static File getCanonicalFile(File aFile) {
		File f = null;

		try {
			f = aFile.getCanonicalFile();
		} catch (IOException e) {
			// this should never happens
			StudioLogger.error(FileUtil.class, "FileUtil.getCanonicalFile: IOException e", e); //$NON-NLS-1$

			// since it's not possible to read from filesystem, return a File
			// using String
			String filename = aFile.getAbsolutePath();

			StringTokenizer st = new StringTokenizer(filename, File.separator);

			StringBuffer sb = new StringBuffer();

			while (st.hasMoreTokens()) {
				String token = (String) st.nextElement();

				if (token.equals("..")) //$NON-NLS-1$
				{
					int lastDirIndex = sb.lastIndexOf(File.separator);

					// do not go back currently on the root directory
					if (lastDirIndex > 2) {
						sb.delete(lastDirIndex, sb.length());
					}
				} else if (!token.equals(".")) //$NON-NLS-1$
				{
					if (sb.length() > 0) {
						sb.append(File.separator);
					}

					sb.append(token);

					if (token.endsWith(":")) //$NON-NLS-1$
					{
						sb.append(File.separator);
					}
				}
			}

			f = new File(sb.toString());
		}

		return f;
	}

	/**
	 * Returns which is the OS.
	 * 
	 * @return a code corresponding to the proper OS
	 */
	public static int getOS() {
		int result = -1;

		String osName = System.getProperty("os.name").toLowerCase(); //$NON-NLS-1$
		if (osName.indexOf("linux") > -1) //$NON-NLS-1$
		{
			result = OS_LINUX;
		} else if (osName.indexOf("windows") > -1) //$NON-NLS-1$
		{
			result = OS_WINDOWS;
		}

		return result;
	}

	/**
	 * Returns true if the OS is windows
	 * 
	 * @return true if the OS is windows
	 */
	public static boolean isWindows() {
		return getOS() == OS_WINDOWS;
	}

	/**
	 * Opens the stream;
	 *
	 * @param stream
	 *            File Stream
	 *
	 * @return StringBuffer with the file content
	 *
	 * @throws IOException
	 */
	public static StringBuffer openFile(InputStream stream) throws IOException {
		InputStreamReader streamReader = null;
		StringBuffer fileBuffer = new StringBuffer();
		BufferedReader reader = null;
		try {
			streamReader = new InputStreamReader(stream);
			reader = new BufferedReader(streamReader);
			char[] buffer = new char[1024];
			int line = reader.read(buffer);

			while (line > 0) {
				fileBuffer.append(buffer, 0, line);
				line = reader.read(buffer);
			}
		} finally {
			if (streamReader != null) {
				try {
					streamReader.close();
				} catch (Exception e) {
					// Do nothing.
				}
			}
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception e) {
					// Do nothing.
				}
			}
		}

		return fileBuffer;
	}

	/**
	 * Reads a file into a string array
	 * 
	 * @param filename
	 *            The file name
	 * @return The file contents as a string array
	 * @throws IOException
	 */
	public static String[] readFileAsArray(String filename) throws IOException {
		LinkedList<String> file = new LinkedList<String>();
		String[] lines = new String[0];
		String line;
		FileReader reader = null;
		LineNumberReader lineReader = null;

		try {
			reader = new FileReader(filename);
			lineReader = new LineNumberReader(reader);

			while ((line = lineReader.readLine()) != null) {
				file.add(line);
			}

			lines = new String[file.size()];
			lines = file.toArray(lines);
		} finally {
			try {
				lineReader.close();
				reader.close();
			} catch (Exception e) {
				// Do nothing
			}
		}

		return lines;
	}

	/**
	 * Reads a file on workspace and returns an IDocument object with its
	 * content
	 * 
	 * @param file
	 *            The file to read
	 * @return The IDocument object containing the file contents
	 * 
	 * @throws CoreException
	 */
	public static IDocument readFile(IFile file) throws CoreException {
		if (!canRead(file)) {
			String errMsg = NLS.bind(UtilitiesNLS.EXC_FileUtil_TheFileCannotBeRead, file.getName());
			IStatus status = new Status(IStatus.ERROR, CommonPlugin.PLUGIN_ID, errMsg);

			throw new CoreException(status);
		}

		TextFileDocumentProvider documentProvider = new TextFileDocumentProvider();
		IDocument document = new Document();

		documentProvider.connect(file);
		document = documentProvider.getDocument(file);
		documentProvider.disconnect(file);

		return document;
	}

	/**
	 * Saves the content of an IDocument object to a file on workspace
	 * 
	 * @param file
	 *            The file
	 * @param document
	 *            The IDocument object
	 * @param encoding
	 *            The file encoding
	 * @param overwrite
	 *            If the file can be overwritten
	 * @throws CoreException
	 */
	public static void saveFile(IFile file, IDocument document, String encoding, boolean overwrite)
			throws CoreException {
		if (file.exists() && !overwrite) {
			String errMsg = NLS.bind(UtilitiesNLS.EXC_FileUtil_CannotOverwriteTheFile, file.getName());
			IStatus status = new Status(IStatus.ERROR, CommonPlugin.PLUGIN_ID, errMsg);

			throw new CoreException(status);
		}

		if (!canWrite(file)) {
			String errMsg = NLS.bind(UtilitiesNLS.EXC_FileUtil_ErrorWritingTheFile, file.getName());
			IStatus status = new Status(IStatus.ERROR, CommonPlugin.PLUGIN_ID, errMsg);

			throw new CoreException(status);
		}

		ByteArrayInputStream bais = null;

		try {
			bais = new ByteArrayInputStream(document.get().getBytes(encoding));
			file.setCharset(encoding, new NullProgressMonitor());
			file.setContents(bais, true, false, new NullProgressMonitor());
		} catch (UnsupportedEncodingException e1) {
			String errMsg = NLS.bind(UtilitiesNLS.EXC_FileUtil_ErrorSettingTheFileEncoding, file.getName());
			IStatus status = new Status(IStatus.ERROR, CommonPlugin.PLUGIN_ID, errMsg);

			throw new CoreException(status);
		} finally {
			if (bais != null) {
				try {
					bais.close();
				} catch (IOException e) {
					// Do nothing.
				}
			}
		}
	}

	/**
	 * Checks if a file can be read
	 * 
	 * @param file
	 *            The file to be checked
	 * @return true if the file can be read or false otherwise
	 */
	public static boolean canRead(IFile file) {
		boolean canRead = true;
		InputStream is = null;

		try {
			if (file.exists()) {
				file.refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());
				is = file.getContents();
				is.read();
			}
		} catch (CoreException e) {
			canRead = false;
		} catch (IOException e) {
			canRead = false;
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					// do nothing
				}
			}
		}

		return canRead;
	}

	/**
	 * Checks if a file can be written
	 * 
	 * @param file
	 *            the file to be checked
	 * @return true if the file can be written or false otherwise
	 */
	public static boolean canWrite(IFile file) {
		boolean canWrite = true;

		if (file.exists() && canRead(file)) {
			canWrite = !file.isReadOnly();
		} else {
			IFolder parent = (IFolder) file.getParent();

			if (!parent.isAccessible()) {
				canWrite = false;
			} else {
				try {
					if (parent.members() == null) {
						canWrite = false;
					} else {
						NullProgressMonitor nullProgressMonitor = new NullProgressMonitor();
						file.create(null, true, nullProgressMonitor);
						file.refreshLocal(IResource.DEPTH_ZERO, nullProgressMonitor);
						file.delete(true, nullProgressMonitor);
					}
				} catch (CoreException e) {
					canWrite = false;
				}
			}
		}

		return canWrite;
	}

	/**
	 * Checks if a File object can be read
	 * 
	 * @param file
	 *            the File object
	 * 
	 * @return true if the File object can be read or false otherwise
	 */
	public static boolean canRead(File file) {
		boolean canRead = false;

		if ((file != null) && file.exists()) {
			FileInputStream fis = null;

			try {
				if (file.isFile()) {
					fis = new FileInputStream(file);
					fis.read();
					canRead = true;
				} else {
					String[] children = file.list();

					if (children != null) {
						canRead = true;
					}
				}
			} catch (Exception e) {
				// Do nothing. canRead is false already
			} finally {
				try {
					if (fis != null) {
						fis.close();
					}
				} catch (IOException e) {
					// Do nothing
				}
			}
		}

		return canRead;
	}

	/**
	 * Checks if a File object can be written
	 * 
	 * @param file
	 *            the File object
	 * 
	 * @return true if the File object can be written or false otherwise
	 */
	public static boolean canWrite(File file) {
		boolean canWrite = false;

		if (file != null) {
			FileOutputStream fos = null;

			try {
				if (!file.exists()) {
					canWrite = file.createNewFile();

					if (canWrite) {
						file.delete();
					}
				} else if (file.isDirectory()) {
					File tempFile = File.createTempFile("StudioForAndroidFSChecking", null, file); //$NON-NLS-1$

					if (tempFile.exists()) {
						canWrite = true;
						tempFile.delete();
					}
				} else if (file.isFile()) {
					fos = new FileOutputStream(file);
					fos.getFD();
					canWrite = true;
				}
			} catch (Exception e) {
				// Do nothing. canWrite is false already
			} finally {
				if (fos != null) {
					try {
						fos.close();
					} catch (IOException e) {
						// Do nothing
					}
				}
			}
		}

		return canWrite;
	}

	/**
	 * Unpack a zip file.
	 * 
	 * @param file
	 *            the file
	 * @param destination
	 *            the destination path or null to unpack at the same directory
	 *            of file
	 * @return true if unpacked, false otherwise
	 */
	public static boolean unpackZipFile(File file, String destination, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor);
		ZipFile zipFile = null;

		String extractDestination = destination != null ? destination : file.getParent();
		if (!extractDestination.endsWith(File.separator)) {
			extractDestination += File.separator;
		}

		boolean unziped = true;
		try {
			zipFile = new ZipFile(file);
		} catch (Throwable e) {
			unziped = false;
			StudioLogger.error(FileUtil.class, "Error extracting file: " + file.getAbsolutePath() //$NON-NLS-1$
					+ " to " + extractDestination, e); //$NON-NLS-1$

		}
		if (zipFile != null) {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();

			subMonitor.beginTask("Extracting files", Collections.list(entries).size()); //$NON-NLS-1$
			entries = zipFile.entries();
			InputStream input = null;
			OutputStream output = null;
			while (entries.hasMoreElements()) {
				try {
					ZipEntry entry = entries.nextElement();
					File newFile = new File(extractDestination + entry.getName());
					if (entry.isDirectory()) {
						newFile.mkdirs();
					} else {
						newFile.getParentFile().mkdirs();
						if (newFile.createNewFile()) {
							input = zipFile.getInputStream(entry);
							output = new BufferedOutputStream(new FileOutputStream(newFile));
							copyStreams(input, output);
						}
					}
				} catch (Throwable t) {
					unziped = false;
					StudioLogger.error(FileUtil.class, "Error extracting file: " + file.getAbsolutePath() + " to " //$NON-NLS-1$ //$NON-NLS-2$
							+ extractDestination, t);
				} finally {
					try {
						if (input != null) {
							input.close();
						}
						if (output != null) {
							output.close();
						}
					} catch (Throwable t) {
						// do nothing
					}
					subMonitor.worked(1);
				}
			}
		}
		return unziped;
	}

	public static boolean extractZipArchive(File file, File destination, List<String> selectedEntries,
			IProgressMonitor monitor) throws IOException {
		SubMonitor subMonitor = SubMonitor.convert(monitor);
		ZipFile zipFile = null;
		CRC32 crc = new CRC32();
		byte[] buf = new byte[BUFFER_SIZE];

		File extractDestination = destination != null ? destination : file.getParentFile();

		if (!extractDestination.exists()) {
			extractDestination.mkdirs();
		}

		boolean unziped = true;
		try {
			zipFile = new ZipFile(file);
		} catch (Throwable e) {
			unziped = false;
			StudioLogger.error(FileUtil.class, "Error extracting file: " + file.getAbsolutePath() //$NON-NLS-1$
					+ " to " + extractDestination, e); //$NON-NLS-1$

		}
		if (zipFile != null) {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();

			subMonitor.beginTask("Extracting files", Collections.list(entries).size()); //$NON-NLS-1$
			entries = zipFile.entries();
			InputStream input = null;
			FileOutputStream output = null;
			int diagReturn = IDialogConstants.YES_ID;
			while (entries.hasMoreElements()) {
				crc.reset();
				try {
					ZipEntry entry = entries.nextElement();
					if (selectedEntries.contains(entry.getName())) {
						File newFile = new File(extractDestination, entry.getName());
						if ((diagReturn != IDialogConstants.YES_TO_ALL_ID) && newFile.exists()) {
							diagReturn = EclipseUtils.showQuestionYesAllCancelDialog(
									UtilitiesNLS.FileUtil_File_Exists_Title,
									NLS.bind(UtilitiesNLS.FileUtil_File_Exists_Message, newFile.getAbsolutePath()));
						}

						if ((diagReturn == IDialogConstants.YES_ID) || (diagReturn == IDialogConstants.YES_TO_ALL_ID)) {
							newFile.delete();
							if (entry.isDirectory()) {
								newFile.mkdirs();
							} else {
								newFile.getParentFile().mkdirs();
								if (newFile.createNewFile()) {
									input = zipFile.getInputStream(entry);
									output = new FileOutputStream(newFile);
									int length = 0;
									while ((length = input.read(buf, 0, BUFFER_SIZE)) > 1) {
										output.write(buf, 0, length);
										crc.update(buf, 0, length);
									}

									if (crc.getValue() != entry.getCrc()) {
										throw new IOException();
									}

								}
							}
						} else {
							diagReturn = IDialogConstants.YES_ID; // Attempt to
																	// extract
																	// next
																	// entry
						}
					}
				} catch (IOException e) {
					unziped = false;
					StudioLogger.error(FileUtil.class, "Error extracting file: " + file.getAbsolutePath() + " to " //$NON-NLS-1$ //$NON-NLS-2$
							+ extractDestination, e);
					throw e;
				} catch (Throwable t) {
					unziped = false;
					StudioLogger.error(FileUtil.class, "Error extracting file: " + file.getAbsolutePath() + " to " //$NON-NLS-1$ //$NON-NLS-2$
							+ extractDestination, t);
				} finally {
					try {
						if (input != null) {
							input.close();
						}
						if (output != null) {
							output.close();
						}
					} catch (Throwable t) {
						// do nothing
					}
					subMonitor.worked(1);
				}
			}
		}
		return unziped;

	}

	/**
	 * Unpack a tar file.
	 * 
	 * @param file
	 *            the file
	 * @param destination
	 *            the destination path or null to unpack at the same directory
	 *            of file
	 * @return true if unpacked, false otherwise
	 */
	public static boolean unpackTarFile(File artifactFile, String destination) {
		boolean unpacked = true;

		String extractDestination = destination != null ? destination : artifactFile.getParent();
		if (!extractDestination.endsWith(File.separator)) {
			extractDestination += File.separator;
		}

		List<String> commandList = new LinkedList<String>();
		commandList.add("tar"); //$NON-NLS-1$

		String fileName = artifactFile.getName();

		// tar.gz or tgz
		if (fileName.endsWith("gz")) //$NON-NLS-1$
		{
			commandList.add("xzf"); //$NON-NLS-1$
		}
		// tar.bz2
		else if (fileName.endsWith("bz2")) //$NON-NLS-1$
		{
			commandList.add("xjf"); //$NON-NLS-1$
		}
		// tar
		else if (fileName.endsWith("tar")) //$NON-NLS-1$
		{
			commandList.add("xf"); //$NON-NLS-1$
		} else {
			unpacked = false;
		}

		if (unpacked) {
			commandList.add(artifactFile.getAbsolutePath());
			File target = new File(extractDestination);
			if (target.exists() && target.isDirectory() && target.canWrite()) {
				try {
					Process p = Runtime.getRuntime().exec(commandList.toArray(new String[0]), null, target);
					try {
						p.waitFor();
					} catch (InterruptedException e) {
						// do nothing
					}
					if (p.exitValue() != 0) {
						unpacked = false;

					}
				} catch (IOException e) {
					unpacked = false;
				}
			}
		}

		return unpacked;
	}

	/**
	 * Copy the input stream to the output stream
	 * 
	 * @param inputStream
	 * @param outputStream
	 * @throws IOException
	 */
	public static void copyStreams(InputStream inputStream, OutputStream outputStream) throws IOException {
		byte[] buffer = new byte[1024];
		int length;

		while ((length = inputStream.read(buffer)) >= 0) {
			outputStream.write(buffer, 0, length);
		}
	}

	/**
	 * Add a directory to a Project
	 * 
	 * @param project
	 * @param parentFolder
	 * @param folderName
	 * @param monitor
	 * @throws CoreException
	 */
	public static void createProjectFolder(IProject project, String parentFolder, String folderName,
			IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(UtilitiesNLS.UI_Project_Creating_Folder_Task, 100);

		try {
			monitor.setTaskName(UtilitiesNLS.UI_Project_Verifying_Folder_Task);
			if (folderName.length() > 0) {
				monitor.worked(10);
				IFolder folder = project.getFolder(parentFolder + folderName);
				monitor.worked(10);
				if (!folder.exists()) {
					monitor.worked(10);
					if (FileUtil.canWrite(folder.getLocation().toFile())) {
						monitor.worked(10);
						monitor.setTaskName(UtilitiesNLS.UI_Project_Creating_Folder_Task);
						folder.create(true, true, new SubProgressMonitor(monitor, 60));
					} else {
						String errMsg = NLS.bind(UtilitiesNLS.EXC_Project_CannotCreateFolderReadOnlyWorkspace, folder
								.getLocation().toFile().toString());
						IStatus status = new Status(IStatus.ERROR, CommonPlugin.PLUGIN_ID, errMsg);
						throw new CoreException(status);
					}
				}
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Given a directory descriptor represented by a {@link File}, creates it
	 * only if it does not exist. In case it does, try to create another one
	 * with its name plus "-1". If it does exists, try to create it with its
	 * name plus "+2", and so on... <br>
	 * Note that the directory is not fisically created. To do so, on must use
	 * the method {@link File#mkdir()}.
	 * 
	 * @param directory
	 *            Directory to be created.
	 * 
	 * @return Returns the created directory as a {@link File}.
	 */
	public static File createUniqueDirectoryDescriptor(File directory) {
		if (directory.exists()) {
			boolean exists = true;
			int counter = 1;
			String rootPath = directory.getAbsolutePath();
			while (exists) {
				directory = new File(rootPath + "-" + counter); //$NON-NLS-1$
				exists = directory.exists();
				counter++;
			}
		}

		return directory;
	}

	/**
	 * Return path with special characters escaped. Special characters are
	 * system dependent, there is a set for linux and another for mac. If
	 * {@code operationalSystem} is windows, the path is returned unchanged.
	 * 
	 * @param path
	 *            to be escaped.
	 * @param operatingSystem
	 *            the target operation system that the path will be used.
	 * @return path with special characters escaped.
	 * */
	public static String getEscapedPath(String path, String operatingSystem) {
		char[] specialCharSet = null;

		if (operatingSystem.equals(Platform.OS_LINUX)) {
			specialCharSet = LINUX_SPECIAL_CHAR;
		} else if (operatingSystem.equals(Platform.OS_MACOSX)) {
			specialCharSet = MAC_SPECIAL_CHAR;
		}

		if ((path != null) && (specialCharSet != null)) {
			for (char c : specialCharSet) {
				CharSequence target = String.valueOf(c);
				CharSequence replacement = new String("\\" + String.valueOf(c)); //$NON-NLS-1$
				path = path.replace(target, replacement);
			}
		}

		return path;
	}

	/**
	 * Return path with special characters escaped. Special characters are
	 * system dependent, there is a set for linux and another for mac. If the
	 * system is windows, returns the path unchanged.
	 * 
	 * @param path
	 *            to be escaped
	 * @return path with special characters escaped.
	 * */
	public static String getEscapedPath(String path) {
		return getEscapedPath(path, Platform.getOS());
	}

	/**
	 * Return path with special characters unescaped. Special characters are
	 * system dependent, there is a set for linux and another for mac. If the
	 * system is windows, returns the path unchanged.
	 * 
	 * @param path
	 *            to be unescaped
	 * @return path with special characters unescaped.
	 * */
	public static String getUnescapedPath(String path) {
		char[] specialCharSet = null;

		if (Platform.getOS().equals(Platform.OS_LINUX)) {
			specialCharSet = LINUX_SPECIAL_CHAR;
		} else if (Platform.getOS().equals(Platform.OS_MACOSX)) {
			specialCharSet = MAC_SPECIAL_CHAR;
		}

		if ((path != null) && (specialCharSet != null)) {
			for (char c : specialCharSet) {
				CharSequence target = new String("\\") + String.valueOf(c); //$NON-NLS-1$
				CharSequence replacement = String.valueOf(c);
				path = path.replace(target, replacement);
			}
		}

		return path;
	}

	public static String removeUnescapedQuotes(String path, String quoteReplacement) {
		// remove quotes and double quotes
		char quotes[] = { '\'', '"' };

		boolean escaped = false;

		for (int i = 0; i < path.length(); i++) {
			if (escaped == false) {
				if (path.charAt(i) == ESCAPE_CHAR) {
					escaped = true;
				} else {
					for (char quote : quotes) {
						if (path.charAt(i) == quote) {
							// split the string in two parts:
							// - part1: before the quote
							String part1 = path.substring(0, i);
							// - part2: after the quote
							String part2 = path.substring(i + 1, path.length());

							// concatenate part1 and part2 with quoteReplacement
							// in-between
							// if quoteReplacement is the empty string (""),
							// then part1 and part2 are juxtaposed
							path = part1.concat(quoteReplacement).concat(part2);
						}
					}
				}
			} else {
				// current character is escaped, next character can't be escaped
				escaped = false;
			}
		}
		return path;
	}

	/**
	 * Unescape characters and remove quotes and double quotes. Special
	 * characters are system dependent, there is a set for linux and another for
	 * mac. If the system is windows, returns the path unchanged.
	 * 
	 * @param path
	 *            to be cleaned.
	 * @param quoteReplacement
	 *            string that will replace quotes and double quotes.
	 * @return path without quotes, double quotes and special characters
	 *         unescaped.
	 * */
	public static String getCleanPath(String path, String quoteReplacement) {

		path = removeUnescapedQuotes(path, quoteReplacement);
		path = getUnescapedPath(path);

		return path;
	}

	public static String calculateMd5Sum(File file) throws IOException {
		String md5Sum = null;

		BigInteger hash = null;
		FileInputStream fis;
		fis = new FileInputStream(file);
		byte[] buf = new byte[1500000];

		try {
			MessageDigest digest = java.security.MessageDigest.getInstance("MD5"); //$NON-NLS-1$
			int bytesRead = 0;
			while ((bytesRead = fis.read(buf)) > 0) {
				digest.update(buf, 0, bytesRead);
			}

			hash = new BigInteger(1, digest.digest());
			md5Sum = hash.toString(16);
		} catch (NoSuchAlgorithmException e) {
			// This exception should not happen, because we are using a valid
			// hard
			// coded value for the algorithm name. However, if it happens, log
			// it.
			warn("MOTODEV Studio could not find an instance of the MessageDigest for the MD5 algorithm"); //$NON-NLS-1$
			throw new IOException(UtilitiesNLS.FileUtil_Get_MD5_Algorithm_Failed);
		} finally {
			if (fis != null) {
				fis.close();
			}
		}

		if (md5Sum == null) {
			throw new IOException(NLS.bind(UtilitiesNLS.FileUtil_MD5_Calculation_Failed, file.getAbsolutePath()));
		}

		return md5Sum;
	}

	/**
	 * This method is responsible to copy informed source file to informed
	 * target.
	 * 
	 * @param sourceFile
	 * @param targetFile
	 * @throws IOException
	 */
	public static void copy(File sourceFile, File targetFile) throws IOException {
		OutputStream outputStream = new FileOutputStream(targetFile);
		InputStream inputStream = new FileInputStream(sourceFile);
		try {
			int length;
			byte[] buffer = new byte[FileUtil.BUFFER_SIZE];
			while ((length = inputStream.read(buffer)) >= 0) {
				outputStream.write(buffer, 0, length);
			}
		} catch (IOException e) {
			throw new IOException("Error copying file:" + sourceFile.getAbsolutePath() + //$NON-NLS-1$
					" to " + targetFile.getAbsolutePath()); //$NON-NLS-1$
		} finally {
			outputStream.close();
			inputStream.close();
		}
	}

	/**
	 * This method normalize a directory path.
	 * 
	 * @param folder
	 *            Full path to a directory
	 * @return The normalized path.
	 */
	public static String normalizePath(String folder) {
		return folder.endsWith(File.separator) ? folder : folder + File.separator;
	}

	/**
	 * Delete the specified file, recursively as necessary.
	 * 
	 * @param file
	 *            The file to delete
	 */
	public static void delete(File file) {
		if (file.exists()) {
			if (file.isDirectory()) {
				File[] files = file.listFiles();
				for (int i = 0; i < files.length; i++) {
					delete(files[i]);
				}
			}
			file.delete();
		}
	}

	/**
	 * Delete the specified file, recursively as necessary.
	 * 
	 * @param fileName
	 *            The file to delete
	 */
	public static void delete(String fileName) {
		delete(new File(fileName));
	}

	/**
	 * This method creates the specified directory.
	 * 
	 * @param directory
	 *            The directory to create.
	 * @throws IOException
	 */
	public static void mkdir(String directory) throws IOException {
		File f = new File(directory);
		if (f.exists()) {
			if (f.isFile()) {
				throw new IOException("Error creating directory:" + directory); //$NON-NLS-1$
			}
		} else {
			if (!f.mkdirs()) {
				throw new IOException("Error creating directory:" + directory); //$NON-NLS-1$
			}
		}
	}

}
