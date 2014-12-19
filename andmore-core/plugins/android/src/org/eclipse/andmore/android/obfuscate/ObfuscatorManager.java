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
package org.eclipse.andmore.android.obfuscate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

import org.eclipse.andmore.android.AndroidPlugin;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.log.UsageDataConstants;
import org.eclipse.andmore.android.common.utilities.AndroidUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.osgi.framework.Bundle;

/**
 * Manager to set Proguard files inside project Manipulates the file on
 * <project_root>/default.properties Copy proguard.cfg
 */
public class ObfuscatorManager {

	private static final String PROGUARD_PATH = "/files/proguard.cfg";

	private static final String PROGUARD_SDK_PATH = "tools/lib/proguard.cfg";

	private static final String PROGUARD_FILENAME = "proguard.cfg";

	private static final String PROGUARD_CONFIG_STATEMENT = "proguard.config=proguard.cfg";

	private static final String DEFAULT_PROPERTIES_FILENAME = "default.properties";

	private static final String PROJECT_PROPERTIES_FILENAME = "project.properties";

	private static final String NL = System.getProperty("line.separator");

	/**
	 * Prepares the project with the Proguard settings to obfuscate when the
	 * project is exported in release mode
	 * 
	 * @param project
	 * @param monitor
	 * @return
	 */
	public static IStatus obfuscate(IProject project, IProgressMonitor monitor) {
		IStatus status = Status.OK_STATUS;

		/*
		 * Project properties file is the new filename for ADT
		 */
		File projectPropertiesFile = project.getFile(PROJECT_PROPERTIES_FILENAME).getLocation().toFile();
		File defaultPropertiesFile = project.getFile(DEFAULT_PROPERTIES_FILENAME).getLocation().toFile();
		if (projectPropertiesFile.canWrite()) {
			defaultPropertiesFile = projectPropertiesFile;
		}

		File proguardFile = project.getFile(PROGUARD_FILENAME).getLocation().toFile();

		try {
			addProguardLine(defaultPropertiesFile);
			if (!fileExists(proguardFile)) {
				copyProguardFile(project, monitor);
			}
			try {
				project.refreshLocal(IResource.DEPTH_ONE, null);
			} catch (CoreException e) {
				// Do nothing, user just have to press F5
			}
		} catch (Exception e) {
			StudioLogger.error(ObfuscatorManager.class, "Error while setting Proguard to obfuscate", e);
			status = new Status(IStatus.ERROR, AndroidPlugin.PLUGIN_ID, "Could not set Proguard to obfuscate", e);
		}

		StudioLogger.collectUsageData(UsageDataConstants.WHAT_OBFUSCATE, UsageDataConstants.KIND_OBFUSCATE,
				UsageDataConstants.DESCRIPTION_DEFAULT, AndroidPlugin.PLUGIN_ID, AndroidPlugin.getDefault().getBundle()
						.getVersion().toString());

		return status;
	}

	/**
	 * Removes Proguard settings (Project will not be obfuscated when the is
	 * exported in release mode)
	 * 
	 * @param project
	 * @return
	 */
	public static IStatus unobfuscate(IProject project) {
		IStatus status = Status.OK_STATUS;

		try {
			File projectPropertiesFile = project.getFile(PROJECT_PROPERTIES_FILENAME).getLocation().toFile();
			File defaultPropertiesFile = project.getFile(DEFAULT_PROPERTIES_FILENAME).getLocation().toFile();
			if (isProguardSet(defaultPropertiesFile)) {
				removeProguardLine(defaultPropertiesFile);
			}
			if (isProguardSet(projectPropertiesFile)) {
				removeProguardLine(projectPropertiesFile);
			}
			try {
				project.refreshLocal(IResource.DEPTH_ONE, null);
			} catch (CoreException e) {
				// Do nothing, user just have to press F5
			}
		} catch (Exception e) {
			StudioLogger.error(ObfuscatorManager.class, "Error while removing Proguard settings", e);
			status = new Status(IStatus.ERROR, AndroidPlugin.PLUGIN_ID, "Could not remove Proguard settings", e);
		}

		StudioLogger.collectUsageData(UsageDataConstants.WHAT_OBFUSCATE, UsageDataConstants.KIND_DESOBFUSCATE,
				UsageDataConstants.DESCRIPTION_DEFAULT, AndroidPlugin.PLUGIN_ID, AndroidPlugin.getDefault().getBundle()
						.getVersion().toString());

		return status;
	}

	/*
	 * @param propertiesFile file to write
	 * 
	 * @param newContent content to write (replaces entire file)
	 * 
	 * @throws IOException
	 */
	private static void write(File propertiesFile, String newContent) throws IOException {
		Writer out = new OutputStreamWriter(new FileOutputStream(propertiesFile));
		try {
			out.write(newContent);
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	/*
	 * @param ignoreProguardStatement true it does not return the line with
	 * proguard.config=proguard.cfg
	 * 
	 * @return
	 * 
	 * @throws IOException
	 */
	private static String read(File propertiesFile, boolean ignoreProguardStatement) throws IOException {
		StringBuilder text = new StringBuilder();
		Scanner scanner = null;
		FileInputStream stream = null;
		try {
			stream = new FileInputStream(propertiesFile);
			scanner = new Scanner(stream);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (!ignoreProguardStatement || !line.contains(PROGUARD_CONFIG_STATEMENT)) {
					text.append(line + NL);
				}
			}
		} finally {
			try {
				if (scanner != null) {
					scanner.close();
				}
				if (stream != null) {
					stream.close();
				}
			} catch (IOException e) {
				StudioLogger.info("Could not close stream while reading obfuscator properties file. " + e.getMessage());
			}
		}
		return text.toString();
	}

	private static boolean fileExists(File proguardFile) {
		return (proguardFile != null) && proguardFile.exists();
	}

	/**
	 * Checks if default.properties have Proguard statement and if the
	 * proguard.cfg exists in the project
	 * 
	 * @param project
	 * @return
	 */
	public static boolean isProguardSet(IProject project) {
		File projectPropertiesFile = project.getFile(PROJECT_PROPERTIES_FILENAME).getLocation().toFile();
		File defaultPropertiesFile = project.getFile(DEFAULT_PROPERTIES_FILENAME).getLocation().toFile();
		File proguardFile = project.getFile(PROGUARD_FILENAME).getLocation().toFile();

		return (isProguardSet(projectPropertiesFile) || isProguardSet(defaultPropertiesFile))
				&& fileExists(proguardFile);
	}

	/**
	 * Checks if default.properties have the Proguard statement
	 * 
	 * @param propertiesFile
	 * @return
	 */
	private static boolean isProguardSet(File propertiesFile) {
		String defaultPropertiesContent = null;
		try {
			defaultPropertiesContent = read(propertiesFile, false);
		} catch (IOException e) {
			StudioLogger.error(ObfuscatorManager.class, e.getMessage(), e);
		}
		return ((defaultPropertiesContent != null) && defaultPropertiesContent.contains(PROGUARD_CONFIG_STATEMENT));
	}

	/**
	 * Add the following line to file proguard.config=proguard.cfg if it does
	 * not exist yet
	 * 
	 * @param project
	 * @throws IOException
	 */
	private static void addProguardLine(File propertiesFile) throws IOException {
		String currentContent = null;
		currentContent = read(propertiesFile, false);
		if (!currentContent.toString().contains(PROGUARD_CONFIG_STATEMENT)) {
			String newContent = currentContent.endsWith(NL) ? currentContent + PROGUARD_CONFIG_STATEMENT
					: currentContent + NL + PROGUARD_CONFIG_STATEMENT;
			write(propertiesFile, newContent);
		}
	}

	/**
	 * Remove the following line to file proguard.config=proguard.cfg if it
	 * exists
	 * 
	 * @param project
	 * @throws IOException
	 */
	private static void removeProguardLine(File propertiesFile) throws IOException {
		String contentWithoutProguardStatement = null;
		contentWithoutProguardStatement = read(propertiesFile, true);
		write(propertiesFile, contentWithoutProguardStatement);
	}

	private static void copyProguardFile(IProject project, IProgressMonitor monitor) throws IOException, CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor);

		URL proguardURL = getProguardFileURL();
		if (proguardURL != null) {
			InputStream is = null;
			try {
				is = proguardURL.openStream();
				IFile destFile = project.getFile(PROGUARD_FILENAME);
				destFile.create(is, IResource.NONE, subMonitor);
			} finally {
				if (is != null) {
					is.close();
				}
			}
		}
	}

	/**
	 * Get the most suitable proguard file, either from SDK or our plug-in
	 * 
	 * @return the URL of the best proguard file found
	 */
	private static URL getProguardFileURL() {
		String sdkPath = AndroidUtils.getSDKPathByPreference();
		File sdkPathFile = sdkPath.isEmpty() ? new File(sdkPath) : null;
		File proguardFromSDK = null;
		if (sdkPathFile != null) {
			proguardFromSDK = new File(sdkPathFile, PROGUARD_SDK_PATH);
		}

		URL fileURL = null;
		// copy newest proguard file from SDK
		if ((proguardFromSDK != null) && proguardFromSDK.canRead()) {
			try {
				fileURL = proguardFromSDK.toURI().toURL();
			} catch (MalformedURLException e) {
				StudioLogger.warn(ObfuscatorManager.class, "Exception converting proguard template file to URL", e);
			}
		}
		// copy file bundled with android plugin
		else {
			Bundle bundle = AndroidPlugin.getDefault().getBundle();
			fileURL = bundle.getEntry(PROGUARD_PATH);
		}

		return fileURL;
	}
}
