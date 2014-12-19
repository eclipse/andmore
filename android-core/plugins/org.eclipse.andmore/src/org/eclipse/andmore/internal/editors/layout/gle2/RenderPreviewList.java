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
package org.eclipse.andmore.internal.editors.layout.gle2;

import com.android.annotations.NonNull;
import com.android.sdklib.devices.Device;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.formatting.EclipseXmlPrettyPrinter;
import org.eclipse.andmore.internal.editors.layout.configuration.Configuration;
import org.eclipse.andmore.internal.editors.layout.configuration.ConfigurationChooser;
import org.eclipse.andmore.internal.editors.layout.configuration.ConfigurationDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** A list of render previews */
class RenderPreviewList {
    /** Name of file saved in project directory storing previews */
    private static final String PREVIEW_FILE_NAME = "previews.xml"; //$NON-NLS-1$

    /** Qualified name for the per-project persistent property include-map */
    private final static QualifiedName PREVIEW_LIST = new QualifiedName(AdtPlugin.PLUGIN_ID,
            "previewlist");//$NON-NLS-1$

    private final IProject mProject;
    private final List<ConfigurationDescription> mList = Lists.newArrayList();

    private RenderPreviewList(@NonNull IProject project) {
        mProject = project;
    }

    /**
     * Returns the {@link RenderPreviewList} for the given project
     *
     * @param project the project the list is associated with
     * @return a {@link RenderPreviewList} for the given project, never null
     */
    @NonNull
    public static RenderPreviewList get(@NonNull IProject project) {
        RenderPreviewList list = null;
        try {
            list = (RenderPreviewList) project.getSessionProperty(PREVIEW_LIST);
        } catch (CoreException e) {
            // Not a problem; we will just create a new one
        }

        if (list == null) {
            list = new RenderPreviewList(project);
            try {
                project.setSessionProperty(PREVIEW_LIST, list);
            } catch (CoreException e) {
                AdtPlugin.log(e, null);
            }
        }

        return list;
    }

    private File getManualFile() {
        return new File(AdtUtils.getAbsolutePath(mProject).toFile(), PREVIEW_FILE_NAME);
    }

    void load(Collection<Device> deviceList) throws IOException {
        File file = getManualFile();
        if (file.exists()) {
            load(file, deviceList);
        }
    }

    void save() throws IOException {
        deleteFile();
        if (!mList.isEmpty()) {
            File file = getManualFile();
            save(file);
        }
    }

    private void save(File file) throws IOException {
        //Document document = DomUtilities.createEmptyPlainDocument();
        Document document = DomUtilities.createEmptyDocument();
        if (document != null) {
            for (ConfigurationDescription description : mList) {
                description.toXml(document);
            }
            String xml = EclipseXmlPrettyPrinter.prettyPrint(document, true);
            Files.write(xml, file, Charsets.UTF_8);
        }
    }

    void load(File file, Collection<Device> deviceList) throws IOException {
        mList.clear();

        String xml = Files.toString(file, Charsets.UTF_8);
        Document document = DomUtilities.parseDocument(xml, true);
        if (document == null || document.getDocumentElement() == null) {
            return;
        }
        List<Element> elements = DomUtilities.getChildren(document.getDocumentElement());
        for (Element element : elements) {
            ConfigurationDescription description = ConfigurationDescription.fromXml(
                    mProject, element, deviceList);
            if (description != null) {
                mList.add(description);
            }
        }
    }

    /**
     * Create a list of previews for the given canvas that matches the internal
     * configuration preview list
     *
     * @param canvas the associated canvas
     * @return a new list of previews linked to the given canvas
     */
    @NonNull
    List<RenderPreview> createPreviews(LayoutCanvas canvas) {
        if (mList.isEmpty()) {
            return new ArrayList<RenderPreview>();
        }
        List<RenderPreview> previews = Lists.newArrayList();
        RenderPreviewManager manager = canvas.getPreviewManager();
        ConfigurationChooser chooser = canvas.getEditorDelegate().getGraphicalEditor()
                .getConfigurationChooser();

        Configuration chooserConfig = chooser.getConfiguration();
        for (ConfigurationDescription description : mList) {
            Configuration configuration = Configuration.create(chooser);
            configuration.setDisplayName(description.displayName);
            configuration.setActivity(description.activity);
            configuration.setLocale(
                    description.locale != null ? description.locale : chooserConfig.getLocale(),
                            true);
            // TODO: Make sure this layout isn't in some v-folder which is incompatible
            // with this target!
            configuration.setTarget(
                    description.target != null ? description.target : chooserConfig.getTarget(),
                            true);
            configuration.setTheme(
                description.theme != null ? description.theme : chooserConfig.getTheme());
            configuration.setDevice(
                description.device != null ? description.device : chooserConfig.getDevice(),
                        true);
            configuration.setDeviceState(
                description.state != null ? description.state : chooserConfig.getDeviceState(),
                        true);
            configuration.setNightMode(
                description.nightMode != null ? description.nightMode
                        : chooserConfig.getNightMode(), true);
            configuration.setUiMode(
                description.uiMode != null ? description.uiMode : chooserConfig.getUiMode(), true);

            //configuration.syncFolderConfig();
            configuration.getFullConfig().set(description.folder);

            RenderPreview preview = RenderPreview.create(manager, configuration);

            preview.setDescription(description);
            previews.add(preview);
        }

        return previews;
    }

    void remove(@NonNull RenderPreview preview) {
        ConfigurationDescription description = preview.getDescription();
        if (description != null) {
            mList.remove(description);
        }
    }

    boolean isEmpty() {
        return mList.isEmpty();
    }

    void add(@NonNull RenderPreview preview) {
        Configuration configuration = preview.getConfiguration();
        ConfigurationDescription description =
                ConfigurationDescription.fromConfiguration(mProject, configuration);
        // RenderPreviews can have display names that aren't reflected in the configuration
        description.displayName = preview.getDisplayName();
        mList.add(description);
        preview.setDescription(description);
    }

    void delete() {
        mList.clear();
        deleteFile();
    }

    private void deleteFile() {
        File file = getManualFile();
        if (file.exists()) {
            file.delete();
        }
    }
}
