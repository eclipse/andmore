/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Inc. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributor:
 * Fabio Fantato (Motorola)
 *
 * Contributors:
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.device.common.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


/**
 * This class provides utility methods to handle files, like copying and
 * deleting directories sub-trees.
 */
public class FileUtil {
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
		if ((fromDir != null) && fromDir.isDirectory() && fromDir.canRead()
				&& (toDir != null) && toDir.isDirectory() && toDir.canWrite()) {
			for (File child : fromDir.listFiles()) {
				if (child.isFile()) {
					copyFile(child, new File(toDir, child.getName()));
				} else {
					// create directory and copy its children recursively
					File newDir = new File(toDir.getAbsolutePath(), child
							.getName());
					newDir.mkdir();
					copyDir(child, newDir);
				}
			}

//			log(FileUtil.class).info(
//					"The directory " + fromDir.getName()
//							+ " was successfully copied to " + toDir.getName()
//							+ ".");
		} else {
			// error detected
			String errorMessage = ""; //$NON-NLS-1$
			if (fromDir == null) {
				errorMessage = "Null pointer for source directory."; //$NON-NLS-1$
			} else if (!fromDir.isDirectory()) {
				errorMessage = fromDir.getName() + " is not a directory."; //$NON-NLS-1$
			} else if (!fromDir.canRead()) {
				errorMessage = "Cannot read from " + fromDir.getName() + "."; //$NON-NLS-1$ //$NON-NLS-2$
			} else if (toDir == null) {
				errorMessage = "Null pointer for destination directory."; //$NON-NLS-1$
			} else if (!toDir.isDirectory()) {
				errorMessage = toDir.getName() + " is not a directory."; //$NON-NLS-1$
			} else if (!toDir.canWrite()) {
				errorMessage = "Cannot write to" + toDir.getName() + "."; //$NON-NLS-1$ //$NON-NLS-2$
			}

//			log(FileUtil.class).error(errorMessage);
			throw new IOException("Error copying directory: " + errorMessage); //$NON-NLS-1$
		}
	}

	/**
	 * Copies the source file to the given target.
	 * 
	 * @param source -
	 *            the absolute path of the source file.
	 * @param target -
	 *            the absolute path of the target file.
	 */
	public static void copyFile(File source, File target) throws IOException {
		copyFile(source.getAbsolutePath(), target.getAbsolutePath());
	}

	/**
	 * Copies the source file to the given target.
	 * 
	 * @param source -
	 *            the absolute path of the source file.
	 * @param target -
	 *            the absolute path of the target file.
	 */
	private static void copyFile(String source, String target)
			throws IOException {
		FileChannel sourceFileChannel = null;
		FileChannel targetFileChannel = null;

		try {
			sourceFileChannel = new FileInputStream(source).getChannel();
			targetFileChannel = new FileOutputStream(target).getChannel();
			targetFileChannel.transferFrom(sourceFileChannel, 0,
					sourceFileChannel.size());
		} finally {
			try {
				if (sourceFileChannel != null) {
					sourceFileChannel.close();
				}
			} catch (IOException e) {
				BasePlugin.logError("Could not close stream: "+e.getMessage());
			}
			try {
				if (targetFileChannel != null) {
					targetFileChannel.close();
				}
			} catch (IOException e) {
				BasePlugin.logError("Could not close stream: "+e.getMessage());
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
	public boolean deleteDirRecursively(File directory) throws IOException {
		boolean success = true;

		if (directory.exists()) {
			if (directory.isDirectory()) {
				File[] children = directory.listFiles();

				for (int i = 0; i < children.length; i++) {
					if (children[i].isFile()) {
						success = success && children[i].delete();
					} else {
						success = success && deleteDirRecursively(children[i]);
					}
				}

				success = success && directory.delete();
			} else {
				String errorMessage = directory.getName()
						+ " is not a diretory."; //$NON-NLS-1$
				throw new IOException(errorMessage);
			}
		} else {
			String errorMessage = "The directory does not exist."; //$NON-NLS-1$
			success = false;
			throw new IOException(errorMessage);
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
	public void deleteFile(File fileToDelete) throws IOException {
		if ((fileToDelete != null) && fileToDelete.exists()
				&& fileToDelete.isFile() && fileToDelete.canWrite()) {
			fileToDelete.delete();
//			log(FileUtil.class).info(
//					"The file " + fileToDelete.getName()
//							+ "was successfully deleted.");
		} else {
			String errorMessage = ""; //$NON-NLS-1$
			if (fileToDelete == null) {
				errorMessage = "Null pointer for file to delete."; //$NON-NLS-1$
			} else {
				if (!fileToDelete.exists()) {
					errorMessage = "The file " + fileToDelete.getName() //$NON-NLS-1$
							+ " does not exist."; //$NON-NLS-1$
				} else {
					if (!fileToDelete.isFile()) {
						errorMessage = fileToDelete.getName()
								+ " is not a file."; //$NON-NLS-1$
					} else {
						if (!fileToDelete.canWrite())
							errorMessage = "Cannot write to " //$NON-NLS-1$
									+ fileToDelete.getName();
					}
				}

			}

//			log(FileUtil.class).error(errorMessage);
			throw new IOException("Cannot delete file: " + errorMessage); //$NON-NLS-1$
		}
	}

	/**
	 * Delete a list of files from the filesystem.
	 * 
	 * @param filesToDelete
	 *            An array of <code>File</code> objects representing the files
	 *            to be deleted.
	 * @throws IOException
	 *             if any problem occurs deleting the files.
	 */
	public void deleteFilesOnList(File[] filesToDelete) throws IOException {
		for (int index = 0; index < filesToDelete.length; index++) {
			deleteFile((filesToDelete[index]));
		}
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
//			log(FileUtil.class).error("The file does not exist.");
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
	public static String getRelativeFilename(File homeDir, File targetFile)
			throws IOException {
		StringBuffer relativePath = new StringBuffer();

		List<File> homeDirList = getFilesComposingPath(getCanonicalFile(homeDir));
		List<File> targetDirList = getFilesComposingPath(getCanonicalFile(targetFile));

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
	public List<String> listFilesRecursively(String baseDirToList)
			throws IOException {
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
	public List<String> listFilesRecursively(File baseDirToList)
			throws IOException {
		List<String> listOfFiles = new ArrayList<String>();

		if (baseDirToList.exists() && baseDirToList.isDirectory()
				&& baseDirToList.canRead()) {
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
					errorMessage = baseDirToList.getName()
							+ "is not a directory."; //$NON-NLS-1$
				} else {
					if (!baseDirToList.canRead()) {
						errorMessage = "Cannot fread from " //$NON-NLS-1$
								+ baseDirToList.getName() + "."; //$NON-NLS-1$
					}
				}
			}

			//log(FileUtil.class).error(errorMessage);
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
	 * @return the canonical File object
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
	 * @return the canonical File object
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
	 * @return the canonical File object
	 */
	public static File getCanonicalFile(File aFile) {
		File f = null;

		try {
			f = aFile.getCanonicalFile();
		} catch (IOException e) {
			// this should never happens
			//log(FileUtil.class).error(
			//		"FileUtils.getCanonicalFile: IOException e", e);
			//e.printStackTrace();

			// since it's not possible to read from filesystem, return a File
			// using String
			String filename = aFile.getAbsolutePath();

			StringTokenizer st = new StringTokenizer(filename, File.separator);

			StringBuffer sb = new StringBuffer();

			while (st.hasMoreTokens()) {
				String token = (String) st.nextElement();

				if (token.equals("..")) { //$NON-NLS-1$
					int lastDirIndex = sb.lastIndexOf(File.separator);

					// do not go back currently on the root directory
					if (lastDirIndex > 2) {
						sb.delete(lastDirIndex, sb.length());
					}
				} else if (!token.equals(".")) { //$NON-NLS-1$
					if (sb.length() > 0) {
						sb.append(File.separator);
					}

					sb.append(token);

					if (token.endsWith(":")) { //$NON-NLS-1$
						sb.append(File.separator);
					}
				}
			}

			f = new File(sb.toString());
		}

		return f;
	}

}
