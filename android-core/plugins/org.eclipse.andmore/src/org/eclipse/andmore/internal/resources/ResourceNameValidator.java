/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.andmore.internal.resources;

import static com.android.SdkConstants.DOT_XML;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.resources.ResourceItem;
import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.layout.gle2.ImageUtils;
import org.eclipse.andmore.internal.resources.manager.ProjectResources;
import org.eclipse.andmore.internal.resources.manager.ResourceManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jface.dialogs.IInputValidator;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Validator which ensures that new Android resource names are valid.
 */
public class ResourceNameValidator implements IInputValidator {
    /** Set of existing names to check for conflicts with */
    private Set<String> mExisting;

    /** If true, the validated name must be unique */
    private boolean mUnique = true;

    /** If true, the validated name must exist */
    private boolean mExist;

    /**
     * True if the resource name being considered is a "file" based resource (where the
     * resource name is the actual file name, rather than just a value attribute inside an
     * XML file name of arbitrary name
     */
    private boolean mIsFileType;

    /**
     * True if the resource type can point to image resources
     */
    private boolean mIsImageType;

    /** If true, allow .xml as a name suffix */
    private boolean mAllowXmlExtension;

    private ResourceNameValidator(boolean allowXmlExtension, Set<String> existing,
            boolean isFileType, boolean isImageType) {
        mAllowXmlExtension = allowXmlExtension;
        mExisting = existing;
        mIsFileType = isFileType;
        mIsImageType = isImageType;
    }

    /**
     * Makes the resource name validator require that names are unique.
     *
     * @return this, for construction chaining
     */
    public ResourceNameValidator unique() {
        mUnique = true;
        mExist = false;

        return this;
    }

    /**
     * Makes the resource name validator require that names already exist
     *
     * @return this, for construction chaining
     */
    public ResourceNameValidator exist() {
        mExist = true;
        mUnique = false;

        return this;
    }

    @Override
    public String isValid(String newText) {
        // IValidator has the same interface as SWT's IInputValidator
        try {
            if (newText == null || newText.trim().length() == 0) {
                return "Enter a new name";
            }

            if (mAllowXmlExtension && newText.endsWith(DOT_XML)) {
                newText = newText.substring(0, newText.length() - DOT_XML.length());
            }

            if (mAllowXmlExtension && mIsImageType
                    && ImageUtils.hasImageExtension(newText)) {
                newText = newText.substring(0, newText.lastIndexOf('.'));
            }

            if (!mIsFileType) {
                newText = newText.replace('.', '_');
            }

            if (newText.indexOf('.') != -1 && !newText.endsWith(DOT_XML)) {
                if (mIsImageType) {
                    return "The filename must end with .xml or .png";
                } else {
                    return "The filename must end with .xml";
                }
            }

            // Resource names must be valid Java identifiers, since they will
            // be represented as Java identifiers in the R file:
            if (!Character.isJavaIdentifierStart(newText.charAt(0))) {
                return "The resource name must begin with a character";
            }
            for (int i = 1, n = newText.length(); i < n; i++) {
                char c = newText.charAt(i);
                if (!Character.isJavaIdentifierPart(c)) {
                    return String.format("'%1$c' is not a valid resource name character", c);
                }
            }

            if (mIsFileType) {
                char first = newText.charAt(0);
                if (!(first >= 'a' && first <= 'z')) {
                    return String.format(
                            "File-based resource names must start with a lowercase letter.");
                }

                // AAPT only allows lowercase+digits+_:
                // "%s: Invalid file name: must contain only [a-z0-9_.]","
                for (int i = 0, n = newText.length(); i < n; i++) {
                    char c = newText.charAt(i);
                    if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_')) {
                        return String.format(
                          "File-based resource names must contain only lowercase a-z, 0-9, or _.");
                    }
                }
            }

            String level = "1.5"; //$NON-NLS-1$
            IStatus validIdentifier = JavaConventions.validateIdentifier(newText, level, level);
            if (!validIdentifier.isOK()) {
                return String.format("%1$s is not a valid name (reserved Java keyword)", newText);
            }


            if (mExisting != null && (mUnique || mExist)) {
                boolean exists = mExisting.contains(newText);
                if (mUnique && exists) {
                    return String.format("%1$s already exists", newText);
                } else if (mExist && !exists) {
                    return String.format("%1$s does not exist", newText);
                }
            }

            return null;
        } catch (Exception e) {
            AdtPlugin.log(e, "Validation failed: %s", e.toString());
            return ""; //$NON-NLS-1$
        }
    }

    /**
     * Creates a new {@link ResourceNameValidator}
     *
     * @param allowXmlExtension if true, allow .xml to be entered as a suffix for the
     *            resource name
     * @param type the resource type of the resource name being validated
     * @return a new {@link ResourceNameValidator}
     */
    public static ResourceNameValidator create(boolean allowXmlExtension,
            ResourceFolderType type) {
        boolean isFileType = type != ResourceFolderType.VALUES;
        return new ResourceNameValidator(allowXmlExtension, null, isFileType,
                type == ResourceFolderType.DRAWABLE);
    }

    /**
     * Creates a new {@link ResourceNameValidator}
     *
     * @param allowXmlExtension if true, allow .xml to be entered as a suffix for the
     *            resource name
     * @param existing An optional set of names that already exist (and therefore will not
     *            be considered valid if entered as the new name)
     * @param type the resource type of the resource name being validated
     * @return a new {@link ResourceNameValidator}
     */
    public static ResourceNameValidator create(boolean allowXmlExtension, Set<String> existing,
            ResourceType type) {
        boolean isFileType = ResourceHelper.isFileBasedResourceType(type);
        return new ResourceNameValidator(allowXmlExtension, existing, isFileType,
                type == ResourceType.DRAWABLE).unique();
    }

    /**
     * Creates a new {@link ResourceNameValidator}. By default, the name will need to be
     * unique in the project.
     *
     * @param allowXmlExtension if true, allow .xml to be entered as a suffix for the
     *            resource name
     * @param project the project to validate new resource names for
     * @param type the resource type of the resource name being validated
     * @return a new {@link ResourceNameValidator}
     */
    public static ResourceNameValidator create(boolean allowXmlExtension,
            @Nullable IProject project,
            @NonNull ResourceType type) {
        Set<String> existing = null;
        if (project != null) {
            existing = new HashSet<String>();
            ResourceManager manager = ResourceManager.getInstance();
            ProjectResources projectResources = manager.getProjectResources(project);
            Collection<ResourceItem> items = projectResources.getResourceItemsOfType(type);
            for (ResourceItem item : items) {
                existing.add(item.getName());
            }
        }

        boolean isFileType = ResourceHelper.isFileBasedResourceType(type);
        return new ResourceNameValidator(allowXmlExtension, existing, isFileType,
                type == ResourceType.DRAWABLE);
    }
}
