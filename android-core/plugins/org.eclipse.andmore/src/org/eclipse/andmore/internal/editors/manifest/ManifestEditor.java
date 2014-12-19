/*
 * Copyright (C) 2007 The Android Open Source Project
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

package org.eclipse.andmore.internal.editors.manifest;

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_NAME;
import static org.eclipse.andmore.internal.editors.manifest.descriptors.AndroidManifestDescriptors.USES_PERMISSION;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.AndroidXmlEditor;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.editors.manifest.descriptors.AndroidManifestDescriptors;
import org.eclipse.andmore.internal.editors.manifest.pages.ApplicationPage;
import org.eclipse.andmore.internal.editors.manifest.pages.InstrumentationPage;
import org.eclipse.andmore.internal.editors.manifest.pages.OverviewPage;
import org.eclipse.andmore.internal.editors.manifest.pages.PermissionPage;
import org.eclipse.andmore.internal.editors.uimodel.UiAttributeNode;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.andmore.internal.lint.EclipseLintClient;
import org.eclipse.andmore.internal.resources.manager.GlobalProjectMonitor;
import org.eclipse.andmore.internal.resources.manager.GlobalProjectMonitor.IFileListener;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Collection;
import java.util.List;

/**
 * Multi-page form editor for AndroidManifest.xml.
 */
@SuppressWarnings("restriction")
public final class ManifestEditor extends AndroidXmlEditor {

    public static final String ID = AdtConstants.EDITORS_NAMESPACE + ".manifest.ManifestEditor"; //$NON-NLS-1$

    private final static String EMPTY = ""; //$NON-NLS-1$

    /** Root node of the UI element hierarchy */
    private UiElementNode mUiManifestNode;
    /** The Application Page tab */
    private ApplicationPage mAppPage;
    /** The Overview Manifest Page tab */
    private OverviewPage mOverviewPage;
    /** The Permission Page tab */
    private PermissionPage mPermissionPage;
    /** The Instrumentation Page tab */
    private InstrumentationPage mInstrumentationPage;

    private IFileListener mMarkerMonitor;


    /**
     * Creates the form editor for AndroidManifest.xml.
     */
    public ManifestEditor() {
        super();
        addDefaultTargetListener();
    }

    @Override
    public void dispose() {
        super.dispose();

        GlobalProjectMonitor.getMonitor().removeFileListener(mMarkerMonitor);
    }

    @Override
    public void activated() {
        super.activated();
        clearActionBindings(false);
    }

    @Override
    public void deactivated() {
        super.deactivated();
        updateActionBindings();
    }

    @Override
    protected void pageChange(int newPageIndex) {
        super.pageChange(newPageIndex);
        if (newPageIndex == mTextPageIndex) {
            updateActionBindings();
        } else {
            clearActionBindings(false);
        }
    }

    @Override
    protected int getPersistenceCategory() {
        return CATEGORY_MANIFEST;
    }

    /**
     * Return the root node of the UI element hierarchy, which here
     * is the "manifest" node.
     */
    @Override
    public UiElementNode getUiRootNode() {
        return mUiManifestNode;
    }

    /**
     * Returns the Manifest descriptors for the file being edited.
     */
    public AndroidManifestDescriptors getManifestDescriptors() {
        AndroidTargetData data = getTargetData();
        if (data != null) {
            return data.getManifestDescriptors();
        }

        return null;
    }

    // ---- Base Class Overrides ----

    /**
     * Returns whether the "save as" operation is supported by this editor.
     * <p/>
     * Save-As is a valid operation for the ManifestEditor since it acts on a
     * single source file.
     *
     * @see IEditorPart
     */
    @Override
    public boolean isSaveAsAllowed() {
        return true;
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        // Look up the current (pre-save) values of minSdkVersion and targetSdkVersion
        int prevMinSdkVersion = -1;
        int prevTargetSdkVersion = -1;
        IProject project = null;
        ManifestInfo info = null;
        try {
            project = getProject();
            if (project != null) {
                info = ManifestInfo.get(project);
                prevMinSdkVersion = info.getMinSdkVersion();
                prevTargetSdkVersion = info.getTargetSdkVersion();
                info.clear();
            }
        } catch (Throwable t) {
            // We don't expect exceptions from the above calls, but we *really*
            // need to make sure that nothing can prevent the save function from
            // getting called!
            AdtPlugin.log(t, null);
        }

        // Actually save
        super.doSave(monitor);

        // If the target/minSdkVersion has changed, clear all lint warnings (since many
        // of them are tied to the min/target sdk levels), in order to avoid showing stale
        // results
        try {
            if (info != null) {
                int newMinSdkVersion = info.getMinSdkVersion();
                int newTargetSdkVersion = info.getTargetSdkVersion();
                if (newMinSdkVersion != prevMinSdkVersion
                        || newTargetSdkVersion != prevTargetSdkVersion) {
                    assert project != null;
                    EclipseLintClient.clearMarkers(project);
                }
            }
        } catch (Throwable t) {
            AdtPlugin.log(t, null);
        }
    }

    /**
     * Creates the various form pages.
     */
    @Override
    protected void createFormPages() {
        try {
            addPage(mOverviewPage = new OverviewPage(this));
            addPage(mAppPage = new ApplicationPage(this));
            addPage(mPermissionPage = new PermissionPage(this));
            addPage(mInstrumentationPage = new InstrumentationPage(this));
        } catch (PartInitException e) {
            AdtPlugin.log(e, "Error creating nested page"); //$NON-NLS-1$
        }
    }

    /* (non-java doc)
     * Change the tab/title name to include the project name.
     */
    @Override
    protected void setInput(IEditorInput input) {
        super.setInput(input);
        IFile inputFile = getInputFile();
        if (inputFile != null) {
            startMonitoringMarkers();
            setPartName(String.format("%1$s Manifest", inputFile.getProject().getName()));
        }
    }

    /**
     * Processes the new XML Model, which XML root node is given.
     *
     * @param xml_doc The XML document, if available, or null if none exists.
     */
    @Override
    protected void xmlModelChanged(Document xml_doc) {
        // create the ui root node on demand.
        initUiRootNode(false /*force*/);

        loadFromXml(xml_doc);
    }

    private void loadFromXml(Document xmlDoc) {
        mUiManifestNode.setXmlDocument(xmlDoc);
        Node node = getManifestXmlNode(xmlDoc);

        if (node != null) {
            // Refresh the manifest UI node and all its descendants
            mUiManifestNode.loadFromXmlNode(node);
        }
    }

    private Node getManifestXmlNode(Document xmlDoc) {
        if (xmlDoc != null) {
            ElementDescriptor manifestDesc = mUiManifestNode.getDescriptor();
            String manifestXmlName = manifestDesc == null ? null : manifestDesc.getXmlName();
            assert manifestXmlName != null;

            if (manifestXmlName != null) {
                Node node = xmlDoc.getDocumentElement();
                if (node != null && manifestXmlName.equals(node.getNodeName())) {
                    return node;
                }

                for (node = xmlDoc.getFirstChild();
                     node != null;
                     node = node.getNextSibling()) {
                    if (node.getNodeType() == Node.ELEMENT_NODE &&
                            manifestXmlName.equals(node.getNodeName())) {
                        return node;
                    }
                }
            }
        }

        return null;
    }

    private void onDescriptorsChanged() {
        IStructuredModel model = getModelForRead();
        if (model != null) {
            try {
                Node node = getManifestXmlNode(getXmlDocument(model));
                mUiManifestNode.reloadFromXmlNode(node);
            } finally {
                model.releaseFromRead();
            }
        }

        if (mOverviewPage != null) {
            mOverviewPage.refreshUiApplicationNode();
        }

        if (mAppPage != null) {
            mAppPage.refreshUiApplicationNode();
        }

        if (mPermissionPage != null) {
            mPermissionPage.refreshUiNode();
        }

        if (mInstrumentationPage != null) {
            mInstrumentationPage.refreshUiNode();
        }
    }

    /**
     * Reads and processes the current markers and adds a listener for marker changes.
     */
    private void startMonitoringMarkers() {
        final IFile inputFile = getInputFile();
        if (inputFile != null) {
            updateFromExistingMarkers(inputFile);

            mMarkerMonitor = new IFileListener() {
                @Override
                public void fileChanged(@NonNull IFile file, @NonNull IMarkerDelta[] markerDeltas,
                        int kind, @Nullable String extension, int flags, boolean isAndroidProject) {
                    if (isAndroidProject && file.equals(inputFile)) {
                        processMarkerChanges(markerDeltas);
                    }
                }
            };

            GlobalProjectMonitor.getMonitor().addFileListener(
                    mMarkerMonitor, IResourceDelta.CHANGED);
        }
    }

    /**
     * Processes the markers of the specified {@link IFile} and updates the error status of
     * {@link UiElementNode}s and {@link UiAttributeNode}s.
     * @param inputFile the file being edited.
     */
    private void updateFromExistingMarkers(IFile inputFile) {
        try {
            // get the markers for the file
            IMarker[] markers = inputFile.findMarkers(
                    AdtConstants.MARKER_ANDROID, true, IResource.DEPTH_ZERO);

            AndroidManifestDescriptors desc = getManifestDescriptors();
            if (desc != null) {
                ElementDescriptor appElement = desc.getApplicationElement();

                if (appElement != null && mUiManifestNode != null) {
                    UiElementNode appUiNode = mUiManifestNode.findUiChildNode(
                            appElement.getXmlName());
                    List<UiElementNode> children = appUiNode.getUiChildren();

                    for (IMarker marker : markers) {
                        processMarker(marker, children, IResourceDelta.ADDED);
                    }
                }
            }

        } catch (CoreException e) {
            // findMarkers can throw an exception, in which case, we'll do nothing.
        }
    }

    /**
     * Processes a {@link IMarker} change.
     * @param markerDeltas the list of {@link IMarkerDelta}
     */
    private void processMarkerChanges(IMarkerDelta[] markerDeltas) {
        AndroidManifestDescriptors descriptors = getManifestDescriptors();
        if (descriptors != null && descriptors.getApplicationElement() != null) {
            UiElementNode app_ui_node = mUiManifestNode.findUiChildNode(
                    descriptors.getApplicationElement().getXmlName());
            List<UiElementNode> children = app_ui_node.getUiChildren();

            for (IMarkerDelta markerDelta : markerDeltas) {
                processMarker(markerDelta.getMarker(), children, markerDelta.getKind());
            }
        }
    }

    /**
     * Processes a new/old/updated marker.
     * @param marker The marker being added/removed/changed
     * @param nodeList the list of activity/service/provider/receiver nodes.
     * @param kind the change kind. Can be {@link IResourceDelta#ADDED},
     * {@link IResourceDelta#REMOVED}, or {@link IResourceDelta#CHANGED}
     */
    private void processMarker(IMarker marker, List<UiElementNode> nodeList, int kind) {
        // get the data from the marker
        String nodeType = marker.getAttribute(AdtConstants.MARKER_ATTR_TYPE, EMPTY);
        if (nodeType == EMPTY) {
            return;
        }

        String className = marker.getAttribute(AdtConstants.MARKER_ATTR_CLASS, EMPTY);
        if (className == EMPTY) {
            return;
        }

        for (UiElementNode ui_node : nodeList) {
            if (ui_node.getDescriptor().getXmlName().equals(nodeType)) {
                for (UiAttributeNode attr : ui_node.getAllUiAttributes()) {
                    if (attr.getDescriptor().getXmlLocalName().equals(
                            AndroidManifestDescriptors.ANDROID_NAME_ATTR)) {
                        if (attr.getCurrentValue().equals(className)) {
                            if (kind == IResourceDelta.REMOVED) {
                                attr.setHasError(false);
                            } else {
                                attr.setHasError(true);
                            }
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates the initial UI Root Node, including the known mandatory elements.
     * @param force if true, a new UiManifestNode is recreated even if it already exists.
     */
    @Override
    protected void initUiRootNode(boolean force) {
        // The manifest UI node is always created, even if there's no corresponding XML node.
        if (mUiManifestNode != null && force == false) {
            return;
        }

        AndroidManifestDescriptors manifestDescriptor = getManifestDescriptors();

        if (manifestDescriptor != null) {
            ElementDescriptor manifestElement = manifestDescriptor.getManifestElement();
            mUiManifestNode = manifestElement.createUiNode();
            mUiManifestNode.setEditor(this);

            // Similarly, always create the /manifest/uses-sdk followed by /manifest/application
            // (order of the elements now matters)
            ElementDescriptor element = manifestDescriptor.getUsesSdkElement();
            boolean present = false;
            for (UiElementNode ui_node : mUiManifestNode.getUiChildren()) {
                if (ui_node.getDescriptor() == element) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                mUiManifestNode.appendNewUiChild(element);
            }

            element = manifestDescriptor.getApplicationElement();
            present = false;
            for (UiElementNode ui_node : mUiManifestNode.getUiChildren()) {
                if (ui_node.getDescriptor() == element) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                mUiManifestNode.appendNewUiChild(element);
            }

            onDescriptorsChanged();
        } else {
            // create a dummy descriptor/uinode until we have real descriptors
            ElementDescriptor desc = new ElementDescriptor("manifest", //$NON-NLS-1$
                    "temporary descriptors due to missing decriptors", //$NON-NLS-1$
                    null /*tooltip*/, null /*sdk_url*/, null /*attributes*/,
                    null /*children*/, false /*mandatory*/);
            mUiManifestNode = desc.createUiNode();
            mUiManifestNode.setEditor(this);
        }
    }

    /**
     * Adds the given set of permissions into the manifest file in the suitable
     * location
     *
     * @param permissions permission fqcn's to be added
     * @param show if true, show one or more of the newly added permissions
     */
    public void addPermissions(@NonNull final List<String> permissions, final boolean show) {
        wrapUndoEditXmlModel("Add permissions", new Runnable() {
            @Override
            public void run() {
                // Ensure that the model is current:
                initUiRootNode(true /*force*/);
                UiElementNode root = getUiRootNode();

                ElementDescriptor descriptor = getManifestDescriptors().getUsesPermissionElement();
                boolean shown = false;
                for (String permission : permissions) {
                    // Find the first permission which sorts alphabetically laster than
                    // this permission (or the last permission, if none are after in the alphabet)
                    // and insert it there
                    int lastPermissionIndex = -1;
                    int nextPermissionIndex = -1;
                    int index = 0;
                    for (UiElementNode sibling : root.getUiChildren()) {
                        Node node = sibling.getXmlNode();
                        if (node.getNodeName().equals(USES_PERMISSION)) {
                            lastPermissionIndex = index;
                            String name = ((Element) node).getAttributeNS(ANDROID_URI, ATTR_NAME);
                            if (permission.compareTo(name) < 0) {
                                nextPermissionIndex = index;
                                break;
                            }
                        } else if (node.getNodeName().equals("application")) { //$NON-NLS-1$
                            // permissions should come before the application element
                            nextPermissionIndex = index;
                            break;
                        }
                        index++;
                    }

                    if (nextPermissionIndex != -1) {
                        index = nextPermissionIndex;
                    } else if (lastPermissionIndex != -1) {
                        index = lastPermissionIndex + 1;
                    } else {
                        index = root.getUiChildren().size();
                    }
                    UiElementNode usesPermission = root.insertNewUiChild(index, descriptor);
                    usesPermission.setAttributeValue(ATTR_NAME, ANDROID_URI, permission,
                            true /*override*/);
                    Node node = usesPermission.createXmlNode();
                    if (show && !shown) {
                        shown = true;
                        if (node instanceof IndexedRegion && getInputFile() != null) {
                            IndexedRegion indexedRegion = (IndexedRegion) node;
                            IRegion region = new Region(indexedRegion.getStartOffset(),
                                    indexedRegion.getEndOffset() - indexedRegion.getStartOffset());
                            try {
                                AdtPlugin.openFile(getInputFile(), region, true /*show*/);
                            } catch (PartInitException e) {
                                AdtPlugin.log(e, null);
                            }
                        } else {
                            show(node);
                        }
                    }
                }
            }
        });
    }

    /**
     * Removes the permissions from the manifest editor
     *
     * @param permissions the permission fqcn's to be removed
     */
    public void removePermissions(@NonNull final Collection<String> permissions) {
        wrapUndoEditXmlModel("Remove permissions", new Runnable() {
            @Override
            public void run() {
                // Ensure that the model is current:
                initUiRootNode(true /*force*/);
                UiElementNode root = getUiRootNode();

                for (String permission : permissions) {
                    for (UiElementNode sibling : root.getUiChildren()) {
                        Node node = sibling.getXmlNode();
                        if (node.getNodeName().equals(USES_PERMISSION)) {
                            String name = ((Element) node).getAttributeNS(ANDROID_URI, ATTR_NAME);
                            if (name.equals(permission)) {
                                sibling.deleteXmlNode();
                                break;
                            }
                        }
                    }
                }
            }
        });
    }
}
