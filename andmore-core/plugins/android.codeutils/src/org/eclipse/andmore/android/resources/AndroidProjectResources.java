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
package org.eclipse.andmore.android.resources;

import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.utilities.FileUtil;
import org.eclipse.andmore.android.model.resources.ResourceFile;
import org.eclipse.andmore.android.model.resources.types.AbstractResourceNode.NodeType;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;

/**
 * Class used to deal with project resource files
 */
public class AndroidProjectResources {
	/**
	 * The base path of resource files
	 */
	private static final String BASE_DIR = "/res/values/";

	/**
	 * The strings file
	 */
	private static final String STRINGS_FILE = BASE_DIR + "strings.xml";

	/**
	 * The colors file
	 */
	private static final String COLORS_FILE = BASE_DIR + "colors.xml";

	/**
	 * The drawables file
	 */
	private static final String DRAWABLES_FILE = COLORS_FILE;

	/**
	 * The dimensions file
	 */
	private static final String DIMENS_FILE = BASE_DIR + "dimens.xml";

	/**
	 * Prefix to refer a string on the AndroidManifest.xml file
	 */
	public static final String STRING_CALL_PREFIX = "@string/";

	/**
	 * Prefix to refer a drawable on the AndroidManifest.xml file
	 */
	public static final String DRAWABLE_CALL_PREFIX = "@drawable/";

	/**
	 * Prefix to refer a color on the AndroidManifest.xml file
	 */
	public static final String COLOR_CALL_PREFIX = "@color/";

	/**
	 * Prefix to refer a dimension on the AndroidManifest.xml file
	 */
	public static final String DIMEN_CALL_PREFIX = "@dimen/";

	/**
	 * Prefix to refer a file in the xml folder on the AndroidManifest.xml file
	 */
	public static final String XML_CALL_PREFIX = "@xml/";

	/**
	 * Gets the default file path for a resource type
	 * 
	 * @param resourceType
	 *            The resource type
	 * @return the default file path for a resource type
	 */
	private static String getDefaultResourceFileLocation(NodeType resourceType) {
		String resLocation;

		switch (resourceType) {
		case Color:
			resLocation = COLORS_FILE;
			break;
		case Dimen:
			resLocation = DIMENS_FILE;
			break;
		case Drawable:
			resLocation = DRAWABLES_FILE;
			break;
		case String:
			resLocation = STRINGS_FILE;
			break;
		default:
			resLocation = null;
		}

		return resLocation;
	}

	/**
	 * Retrieves the ResourceFile object related to a resource file.
	 * 
	 * @param project
	 *            The project that contains the resource file
	 * @param resourceType
	 *            The resource file type to be retrieved
	 * @return the ResourceFile object related to the selected resource file
	 *         type.
	 * @throws CoreException
	 * @throws AndroidException
	 */
	public static ResourceFile getResourceFile(IProject project, NodeType resourceType) throws CoreException,
			AndroidException {
		Assert.isLegal(project != null);
		Assert.isLegal(resourceType != null);

		ResourceFile resourceFile = new ResourceFile();
		IFile resFile = (IFile) project.findMember(getDefaultResourceFileLocation(resourceType));

		if (resFile.exists()) {
			IDocument document = FileUtil.readFile(resFile);
			resourceFile.parseDocument(document, getDefaultResourceFileLocation(resourceType));
		}

		return resourceFile;
	}

	/**
	 * Saves a ResourceFile object to a resource file in a project
	 * 
	 * @param project
	 *            The resource file project
	 * @param resourceFile
	 *            The resource file content (a ResourceFile object)
	 * @param resourceType
	 *            The resource type
	 * @throws CoreException
	 * @throws AndroidException
	 */
	public static void saveResourceFile(IProject project, ResourceFile resourceFile, NodeType resourceType)
			throws CoreException, AndroidException {
		Assert.isLegal(project != null);
		Assert.isLegal(resourceFile != null);
		Assert.isLegal(resourceType != null);

		final String UTF8_ENCODING = "UTF-8";

		IFile resFile = (IFile) project.findMember(getDefaultResourceFileLocation(resourceType));

		FileUtil.saveFile(resFile, resourceFile.getContent(), UTF8_ENCODING, true);
	}
}
