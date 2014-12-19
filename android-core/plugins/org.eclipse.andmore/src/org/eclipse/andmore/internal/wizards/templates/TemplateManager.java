/*
 * Copyright (C) 2012 The Android Open Source Project
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
package org.eclipse.andmore.internal.wizards.templates;

import static com.android.SdkConstants.FD_EXTRAS;
import static com.android.SdkConstants.FD_TEMPLATES;
import static com.android.SdkConstants.FD_TOOLS;
import static org.eclipse.andmore.internal.wizards.templates.TemplateHandler.TEMPLATE_XML;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.layout.gle2.DomUtilities;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.w3c.dom.Document;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Handles locating templates and providing template metadata */
public class TemplateManager {
	private static final Set<String> EXCLUDED_CATEGORIES = Sets.newHashSet("Folder", "Google");
	private static final Set<String> EXCLUDED_FORMFACTORS = Sets.newHashSet("Wear", "TV");
	
    TemplateManager() {
    }

    /** @return the root folder containing templates */
    @Nullable
    public static File getTemplateRootFolder() {
        String location = AdtPrefs.getPrefs().getOsSdkFolder();
        if (location != null) {
            File folder = new File(location, FD_TOOLS + File.separator + FD_TEMPLATES);
            if (folder.isDirectory()) {
                return folder;
            }
        }

        return null;
    }

    /** @return the root folder containing extra templates */
    @NonNull
    public static List<File> getExtraTemplateRootFolders() {
        List<File> folders = new ArrayList<File>();
        String location = AdtPrefs.getPrefs().getOsSdkFolder();
        if (location != null) {
            File extras = new File(location, FD_EXTRAS);
            if (extras.isDirectory()) {
                for (File vendor : AdtUtils.listFiles(extras)) {
                    if (!vendor.isDirectory()) {
                        continue;
                    }
                    for (File pkg : AdtUtils.listFiles(vendor)) {
                        if (pkg.isDirectory()) {
                            File folder = new File(pkg, FD_TEMPLATES);
                            if (folder.isDirectory()) {
                                folders.add(folder);
                            }
                        }
                    }
                }

                // Legacy
                File folder = new File(extras, FD_TEMPLATES);
                if (folder.isDirectory()) {
                    folders.add(folder);
                }
            }
        }

        return folders;
    }

    /**
     * Returns a template file under the given root, if it exists
     *
     * @param root the root folder
     * @param relativePath the relative path
     * @return a template file under the given root, if it exists
     */
    @Nullable
    public static File getTemplateLocation(@NonNull File root, @NonNull String relativePath) {
        File templateRoot = getTemplateRootFolder();
        if (templateRoot != null) {
            String rootPath = root.getPath();
            File templateFile = new File(templateRoot,
                    rootPath.replace('/', File.separatorChar) + File.separator
                    + relativePath.replace('/', File.separatorChar));
            if (templateFile.exists()) {
                return templateFile;
            }
        }

        return null;
    }

    /**
     * Returns a template file under one of the available roots, if it exists
     *
     * @param relativePath the relative path
     * @return a template file under one of the available roots, if it exists
     */
    @Nullable
    public static File getTemplateLocation(@NonNull String relativePath) {
        File templateRoot = getTemplateRootFolder();
        if (templateRoot != null) {
            File templateFile = new File(templateRoot,
                    relativePath.replace('/', File.separatorChar));
            if (templateFile.exists()) {
                return templateFile;
            }
        }

        return null;

    }

    /**
     * Returns all the templates with the given prefix
     *
     * @param folder the folder prefix
     * @return the available templates
     */
    @NonNull
    List<File> getTemplates(@NonNull String folder) {
        List<File> templates = new ArrayList<File>();
        Map<String, File> templateNames = Maps.newHashMap();
        File root = getTemplateRootFolder();
        if (root != null) {
            File[] files = new File(root, folder).listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) { // Avoid .DS_Store etc
                        templates.add(file);
                        templateNames.put(file.getName(), file);
                    }
                }
            }
        }

        // Add in templates from extras/ as well.
        for (File extra : getExtraTemplateRootFolders()) {
            File[] files = new File(extra, folder).listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        File replaces = templateNames.get(file.getName());
                        if (replaces != null) {
                            int compare = compareTemplates(replaces, file);
                            if (compare > 0) {
                                int index = templates.indexOf(replaces);
                                if (index != -1) {
                                    templates.set(index, file);
                                } else {
                                    templates.add(file);
                                }
                            }
                        } else {
                            templates.add(file);
                        }
                    }
                }
            }
        }

        // Sort by file name (not path as is File's default)
        if (templates.size() > 1) {
            Collections.sort(templates, new Comparator<File>() {
                @Override
                public int compare(File file1, File file2) {
                    return file1.getName().compareTo(file2.getName());
                }
            });
        }

        return templates;
    }

    /**
     * Compare two files, and return the one with the HIGHEST revision, and if
     * the same, most recently modified
     */
    private int compareTemplates(File file1, File file2) {
        TemplateMetadata template1 = getTemplate(file1);
        TemplateMetadata template2 = getTemplate(file2);

        if (template1 == null) {
            return 1;
        } else if (template2 == null) {
            return -1;
        } else {
            int delta = template2.getRevision() - template1.getRevision();
            if (delta == 0) {
                delta = (int) (file2.lastModified() - file1.lastModified());
            }
            return delta;
        }
    }

    /** Cache for {@link #getTemplate()} */
    private Map<File, TemplateMetadata> mTemplateMap;

    @Nullable
    TemplateMetadata getTemplate(File templateDir) {
        if (mTemplateMap != null) {
            TemplateMetadata metadata = mTemplateMap.get(templateDir);
            if (metadata != null) {
                return metadata;
            }
        } else {
            mTemplateMap = Maps.newHashMap();
        }

        try {
            File templateFile = new File(templateDir, TEMPLATE_XML);
            if (templateFile.isFile()) {
                String xml = Files.toString(templateFile, Charsets.UTF_8);
                Document doc = DomUtilities.parseDocument(xml, true);
                if (doc != null && doc.getDocumentElement() != null) {
                    TemplateMetadata metadata = new TemplateMetadata(doc);
                    if (EXCLUDED_CATEGORIES.contains(metadata.getCategory()) ||
                    	EXCLUDED_FORMFACTORS.contains(metadata.getFormFactor())) {
                    	return null;
                    }
                    mTemplateMap.put(templateDir, metadata);
                    return metadata;
                }
            }
        } catch (IOException e) {
            AdtPlugin.log(e, null);
        }

        return null;
    }
}
