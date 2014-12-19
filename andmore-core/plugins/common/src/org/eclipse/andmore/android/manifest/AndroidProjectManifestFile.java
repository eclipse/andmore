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
package org.eclipse.andmore.android.manifest;

import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.utilities.FileUtil;
import org.eclipse.andmore.android.common.utilities.i18n.UtilitiesNLS;
import org.eclipse.andmore.android.model.manifest.AndroidManifestFile;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.util.NLS;

/**
 * Class that contains methods to deal with AndroidManifest.xml file in projects
 */
public class AndroidProjectManifestFile {
	/**
	 * The AndroidManifest.xml file name
	 */
	private static final String ANDROID_MANIFEST_FILENAME = "AndroidManifest.xml";

	/**
	 * Retrieves the project AndroidManifest.xml file
	 * 
	 * @param project
	 *            The project
	 * @return the AndroidManifestFile object representing the file
	 * @throws AndroidException
	 * @throws CoreException
	 */
	public static AndroidManifestFile getFromProject(IProject project) throws AndroidException, CoreException {
		Assert.isLegal(project != null);

		AndroidManifestFile androidManifestFile = null;

		IResource resManifest = project.findMember(IPath.SEPARATOR + ANDROID_MANIFEST_FILENAME);

		if ((resManifest != null) && (resManifest instanceof IFile)) {
			if (resManifest.exists()) {
				IFile manifestFile = (IFile) resManifest;
				IDocument document = FileUtil.readFile(manifestFile);

				androidManifestFile = new AndroidManifestFile();
				androidManifestFile.parseDocument(document);
			} else {
				String errMsg = NLS.bind(UtilitiesNLS.ERR_AndroidProjectManifest_AndroidManifestDoesNotExist,
						project.getName());

				throw new AndroidException(errMsg);
			}
		} else {
			String errMsg = NLS.bind(UtilitiesNLS.ERR_AndroidProjectManifest_AndroidManifestDoesNotExist,
					project.getName());

			throw new AndroidException(errMsg);
		}

		return androidManifestFile;
	}

	/**
	 * Saves an AndroidManifestFile object to the AndroidManifest.xml file
	 * 
	 * @param project
	 *            The project
	 * @param androidManifestFile
	 *            The AndroidManifestFile object
	 * @param overwrite
	 *            If the file must be overwritten
	 * @throws AndroidException
	 * @throws CoreException
	 */
	public static void saveToProject(IProject project, AndroidManifestFile androidManifestFile, boolean overwrite)
			throws AndroidException, CoreException {
		Assert.isLegal(project != null);
		Assert.isLegal(androidManifestFile != null);

		final String UTF8_ENCODING = "UTF-8"; //$NON-NLS-1$

		IFile manifestFile = project.getFile(IPath.SEPARATOR + ANDROID_MANIFEST_FILENAME);
		IDocument document = androidManifestFile.getContent();

		FileUtil.saveFile(manifestFile, document, UTF8_ENCODING, overwrite);
	}
}
