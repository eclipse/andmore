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

package org.eclipse.andmore.internal.editors.animator;

import static org.eclipse.andmore.AdtConstants.EDITORS_NAMESPACE;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceFolderType;

import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.common.CommonXmlDelegate;
import org.eclipse.andmore.internal.editors.common.CommonXmlEditor;
import org.eclipse.andmore.internal.editors.descriptors.DocumentDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Editor for /res/animator XML files.
 */
@SuppressWarnings("restriction")
public class AnimationEditorDelegate extends CommonXmlDelegate {

    public static class Creator implements IDelegateCreator {
        @Override
        @SuppressWarnings("unchecked")
        public AnimationEditorDelegate createForFile(
                @NonNull CommonXmlEditor delegator,
                @Nullable ResourceFolderType type) {
            if (ResourceFolderType.ANIM == type || ResourceFolderType.ANIMATOR == type) {
                return new AnimationEditorDelegate(delegator);
            }

            return null;
        }
    }

    /**
     * Old standalone-editor ID.
     * Use {@link CommonXmlEditor#ID} instead.
     */
    public static final String LEGACY_EDITOR_ID =
        EDITORS_NAMESPACE + ".animator.AnimationEditor"; //$NON-NLS-1$

    /** The tag used at the root */
    private String mRootTag;

    private AnimationEditorDelegate(CommonXmlEditor editor) {
        super(editor, new AnimationContentAssist());
        editor.addDefaultTargetListener();
    }

    @Override
    public void delegateCreateFormPages() {
        /* Disabled for now; doesn't work quite right
        try {
            addPage(new AnimatorTreePage(this));
        } catch (PartInitException e) {
            AdtPlugin.log(IStatus.ERROR, "Error creating nested page"); //$NON-NLS-1$
            AdtPlugin.getDefault().getLog().log(e.getStatus());
        }
        */
    }

    /**
     * Processes the new XML Model.
     *
     * @param xmlDoc The XML document, if available, or null if none exists.
     */
    @Override
    public void delegateXmlModelChanged(Document xmlDoc) {
        Element rootElement = xmlDoc.getDocumentElement();
        if (rootElement != null) {
            mRootTag = rootElement.getTagName();
        }

        // create the ui root node on demand.
        delegateInitUiRootNode(false /*force*/);

        if (mRootTag != null
                && !mRootTag.equals(getUiRootNode().getDescriptor().getXmlLocalName())) {
            AndroidTargetData data = getEditor().getTargetData();
            if (data != null) {
                ElementDescriptor descriptor;
                if (getFolderType() == ResourceFolderType.ANIM) {
                    descriptor = data.getAnimDescriptors().getElementDescriptor(mRootTag);
                } else {
                    descriptor = data.getAnimatorDescriptors().getElementDescriptor(mRootTag);
                }
                // Replace top level node now that we know the actual type

                // Disconnect from old
                getUiRootNode().setEditor(null);
                getUiRootNode().setXmlDocument(null);

                // Create new
                setUiRootNode(descriptor.createUiNode());
                getUiRootNode().setXmlDocument(xmlDoc);
                getUiRootNode().setEditor(getEditor());
            }
        }

        if (getUiRootNode().getDescriptor() instanceof DocumentDescriptor) {
            getUiRootNode().loadFromXmlNode(xmlDoc);
        } else {
            getUiRootNode().loadFromXmlNode(rootElement);
        }
    }

    @Override
    public void delegateInitUiRootNode(boolean force) {
        // The manifest UI node is always created, even if there's no corresponding XML node.
        if (getUiRootNode() == null || force) {
            ElementDescriptor descriptor;
            boolean reload = false;
            AndroidTargetData data = getEditor().getTargetData();
            if (data == null) {
                descriptor = new DocumentDescriptor("temp", null /*children*/);
            } else {
                if (getFolderType() == ResourceFolderType.ANIM) {
                    descriptor = data.getAnimDescriptors().getElementDescriptor(mRootTag);
                } else {
                    descriptor = data.getAnimatorDescriptors().getElementDescriptor(mRootTag);
                }
                reload = true;
            }
            setUiRootNode(descriptor.createUiNode());
            getUiRootNode().setEditor(getEditor());

            if (reload) {
                onDescriptorsChanged();
            }
        }
    }

    private ResourceFolderType getFolderType() {
        String folderName = AdtUtils.getParentFolderName(getEditor().getEditorInput());
        if (folderName.length() > 0) {
            return ResourceFolderType.getFolderType(folderName);
        }
        return ResourceFolderType.ANIMATOR;
    }

    private void onDescriptorsChanged() {
        IStructuredModel model = getEditor().getModelForRead();
        if (model != null) {
            try {
                Node node = getEditor().getXmlDocument(model).getDocumentElement();
                getUiRootNode().reloadFromXmlNode(node);
            } finally {
                model.releaseFromRead();
            }
        }
    }
}
