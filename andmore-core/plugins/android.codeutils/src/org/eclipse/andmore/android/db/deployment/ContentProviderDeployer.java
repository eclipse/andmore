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

package org.eclipse.andmore.android.db.deployment;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.eclipse.andmore.android.codeutils.db.utils.DatabaseUtils;
import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.common.IAndroidConstants;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.utilities.FileUtil;
import org.eclipse.andmore.android.manifest.AndroidProjectManifestFile;
import org.eclipse.andmore.android.model.manifest.AndroidManifestFile;
import org.eclipse.andmore.android.model.manifest.dom.ApplicationNode;
import org.eclipse.andmore.android.model.manifest.dom.ManifestNode;
import org.eclipse.andmore.android.model.manifest.dom.ProviderNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;

/**
 * Creates Content Provider that have methods to copy .db file.
 */
public class ContentProviderDeployer {

	/*
	 * Plug-in Identifier
	 */
	private static final String PLUGIN_IDENTIFIER = "org.eclipse.andmore.android.codeutils";

	/**
	 * Constant representing the location of the DataaseHelper class.
	 */
	public static final String CONTENT_PROVIDER_HELPER_CLASS_LOCATION = "resources/databaseDeploy/ContentProviderHelperjava.txt"; //$NON-NLS-1$

	/**
	 * This constant represents the value of newly-create Application Package
	 * Name for the Content Provider.
	 */
	public static final String APPLICATION_PACKAGE_NAMESPACE = "#applicationPackageNamespace#";

	/**
	 * Directory of the ContentProvider Helper in the android project.
	 */
	public static final String ANDROID_PROJECT_PACKAGE_NAME = "#packageName#"; //$NON-NLS-1$

	/**
	 * File name of the ContentProvider Helper in the android project.
	 */
	public static final String CONTENT_PROVIDER_CLASS_NAME = "#className#"; //$NON-NLS-1$

	public static final String CONTENT_PROVIDER_AUTHORITY = "#authority#"; //$NON-NLS-1$

	/**
	 * Copy the ContentProvider Helper deploy file to the newly-created-android
	 * project.
	 * 
	 * @param project
	 *            Project where the files will be copied to
	 * @param parameters
	 *            Copy parameters
	 * @param templateLocation
	 *            The template (origin) location
	 * @param needToAddOnManifest
	 *            <code>true</code> in case the manifest is to be updated, false
	 *            otherwise
	 * @param overrideConentProviderIfExists
	 *            <code>true</code> in case the Content Providers must be
	 *            overridden in case they exist
	 * @param monitor
	 *            UI monitor
	 * 
	 * @throws IOException
	 *             Exception thrown in case there are problems accessing data of
	 *             a file.
	 * @throws CoreException
	 *             Exception thrown in case there are problems accessing a file
	 *             itself.
	 */
	public static void copyContentProviderHelperClassToProject(IProject project, Map<String, String> parameters,
			String templateLocation, boolean needToAddOnManifest, boolean overrideConentProviderIfExists,
			IProgressMonitor monitor) throws IOException, CoreException {
		// get the destination folder
		IFolder destinationFolder = project.getFolder(IAndroidConstants.WS_ROOT + IAndroidConstants.FD_SOURCES);
		// split the new sequence of folders - they will be created, one by one
		String[] folders = parameters.get(ANDROID_PROJECT_PACKAGE_NAME).split(IAndroidConstants.RE_DOT);
		// iterate through the folders and create them
		if ((folders != null) && (folders.length > 0)) {
			// iterate
			for (String folder : folders) {
				// get destination folder with the next one to be created
				destinationFolder = destinationFolder.getFolder(folder);

				// proceed in case the destination folder does not exists and
				// can be created
				if (!destinationFolder.exists()) {
					// create the folder
					destinationFolder.create(true, true, monitor);
				}
			}
		}

		// get the destination file
		IFile destinationFile = destinationFolder.getFile(parameters.get(CONTENT_PROVIDER_CLASS_NAME)
				+ IAndroidConstants.DOT_JAVA);
		// refresh to avoid inconsistency
		destinationFile.getParent().refreshLocal(IResource.DEPTH_INFINITE, monitor);
		// proceed in case it does not exist and can be created
		if ((!destinationFile.exists() || overrideConentProviderIfExists) && FileUtil.canWrite(destinationFile)) {
			// get the Database Helper class as a text
			String databaseHelperText = readContentProviderHelperClassFile(project, parameters, templateLocation);

			// transform it in a stream
			InputStream stream = null;

			try {
				stream = new ByteArrayInputStream(databaseHelperText.getBytes("UTF-8")); //$NON-NLS-1$
				// if the file exists, delete it
				if (destinationFile.exists()) {
					destinationFile.delete(true, monitor);
				}
				// create the destination file
				destinationFile.create(stream, true, monitor);

				DatabaseUtils.formatCode(destinationFile, databaseHelperText, monitor);
			} finally {
				if (stream != null) {
					stream.close();
				}
			}
		}
		if (needToAddOnManifest) {
			try {
				String className = parameters.get(CONTENT_PROVIDER_CLASS_NAME);
				String packageName = parameters.get(ANDROID_PROJECT_PACKAGE_NAME);
				createProviderOnManifest(project, className, packageName, monitor);
			} catch (AndroidException e) {
				throw new IOException(e.getMessage());
			}
		}
	}

	/**
	 * Read the ContentProvider Helper class file, replace the parameter maps
	 * and return it as a String.
	 * 
	 * @param project
	 *            Project where the ContentProviderHelper is retrieved.
	 * @param parameters
	 *            Parameters for replacement in the ContentProviderHelper class
	 *            file.
	 * 
	 * @return a String representing the ContentProviderHelper class file.
	 * 
	 * @throws IOException
	 *             Exception thrown when there are problems reading elements
	 *             from the ContentProviderHelper class file.
	 */
	private static String readContentProviderHelperClassFile(IProject project, Map<String, String> parameters,
			String templateLocation) throws IOException {
		URL url = Platform.getBundle(PLUGIN_IDENTIFIER).getEntry(templateLocation);
		url = FileLocator.toFileURL(url);

		// string buffer which holds the file as a text
		StringBuffer contentProviderHelperBuffer = new StringBuffer("");
		// reader
		BufferedReader contentProviderHelperReader = null;

		// get the file to be read
		File f = null;
		try {
			f = new File(url.getFile());
			// create the reader to manipulate the file
			contentProviderHelperReader = new BufferedReader(new FileReader(f));

			// read the Database Helper class file in steps (using buffer)
			String buffer = null;

			// iterate while there is stuff to read
			while ((buffer = contentProviderHelperReader.readLine()) != null) {
				// read and put the content in the string buffer element
				contentProviderHelperBuffer.append(buffer);
				contentProviderHelperBuffer.append(System.getProperty("line.separator"));
			}
		} finally {
			// close reader
			if (contentProviderHelperReader != null) {
				contentProviderHelperReader.close();
			}
		}

		// string holding the "text" of the ContentProvider class
		String contentProviderHelperText = "";
		// proceed in case there is stuff in the buffer to read
		if (contentProviderHelperBuffer != null) {
			contentProviderHelperText = contentProviderHelperBuffer.toString();

			// iterate through the parameters and replace the parameters in the
			// map
			if (parameters != null) {
				for (String key : parameters.keySet()) {
					// replace all the keys
					contentProviderHelperText = contentProviderHelperText.replaceAll(key, parameters.get(key));
				}
			}
		}
		// return the file as a text
		return contentProviderHelperText;
	}

	/**
	 * Creates the Content Provider class entry on AndroidManifest.xml file
	 * 
	 * @param monitor
	 *            the progress monitor
	 * 
	 * @return true if the entry has been added or false otherwise
	 * @throws AndroidException
	 */
	private static boolean createProviderOnManifest(IProject project, String className, String packageName,
			IProgressMonitor monitor) throws AndroidException {
		boolean created = false;

		try {
			int manifestUpdatingSteps = 4;
			int totalWork = manifestUpdatingSteps;

			monitor.beginTask("", totalWork);

			monitor.subTask(CodeUtilsNLS.UI_Common_UpdatingTheAndroidManifestXMLFile);

			AndroidManifestFile androidManifestFile = AndroidProjectManifestFile.getFromProject(project);

			monitor.worked(1);

			ManifestNode manifestNode = androidManifestFile != null ? androidManifestFile.getManifestNode() : null;
			ApplicationNode applicationNode = manifestNode != null ? manifestNode.getApplicationNode() : null;

			monitor.worked(1);

			if (applicationNode != null) {
				ProviderNode providerNode = new ProviderNode(className, packageName + "." + className.toLowerCase());

				String authority = packageName + "." + className.toLowerCase();
				providerNode.addAuthority(authority);
				String name = packageName + "." + className;
				providerNode.setName(name);

				// get all provider nodes
				List<ProviderNode> providerNodes = applicationNode.getProviderNodes();
				// see whether the node to be inserted is already in the list -
				// in case it is not, add it, otherwise do nothing
				boolean hasProviderNode = false;
				// verify in case the list of nodes is not empty
				if ((providerNodes != null) && (providerNodes.size() > 0)) {
					// iterate through the list
					for (ProviderNode retrievedProvidedNode : providerNodes) {
						// match the nodes
						if (retrievedProvidedNode.getName().equals(providerNode.getName())) {
							// in case a match is found, set the flag and leave
							// the loop
							hasProviderNode = true;
							break;
						}
					}
				}
				// in case there is not this provider node, add it
				if (!hasProviderNode) {
					applicationNode.addProviderNode(providerNode);
				}

				monitor.worked(1);

				monitor.subTask(CodeUtilsNLS.UI_Common_SavingTheAndroidManifestXMLFile);

				AndroidProjectManifestFile.saveToProject(project, androidManifestFile, true);
				created = true;

				monitor.worked(1);
			}
		} catch (AndroidException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_ContentProvider_CannotUpdateTheManifestFile, className,
					e.getLocalizedMessage());
			throw new AndroidException(errMsg);
		} catch (CoreException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_ContentProvider_CannotUpdateTheManifestFile, className,
					e.getLocalizedMessage());
			throw new AndroidException(errMsg);
		} finally {
			monitor.done();
		}

		return created;
	}
}
