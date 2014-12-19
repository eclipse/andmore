/*
 * Copyright (C) 2008 The Android Open Source Project
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

package org.eclipse.andmore.internal.editors.layout.uimodel;

import static com.android.SdkConstants.ANDROID_NS_NAME;
import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_CLASS;
import static com.android.SdkConstants.ATTR_ORIENTATION;
import static com.android.SdkConstants.FQCN_FRAME_LAYOUT;
import static com.android.SdkConstants.LINEAR_LAYOUT;
import static com.android.SdkConstants.VALUE_VERTICAL;
import static com.android.SdkConstants.VIEW_TAG;

import com.android.sdklib.IAndroidTarget;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.AndroidXmlEditor;
import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.andmore.internal.editors.descriptors.AttributeDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.XmlnsAttributeDescriptor;
import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.editors.layout.descriptors.LayoutDescriptors;
import org.eclipse.andmore.internal.editors.layout.descriptors.ViewElementDescriptor;
import org.eclipse.andmore.internal.editors.uimodel.UiDocumentNode;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.graphics.Image;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Specialized version of {@link UiElementNode} for the {@link ViewElementDescriptor}s.
 */
public class UiViewElementNode extends UiElementNode {

    /** An AttributeDescriptor array that depends on the current UiParent. */
    private AttributeDescriptor[] mCachedAttributeDescriptors;

    public UiViewElementNode(ViewElementDescriptor elementDescriptor) {
        super(elementDescriptor);
    }

    /**
     * Returns an AttributeDescriptor array that depends on the current UiParent.
     * <p/>
     * The array merges both "direct" attributes with the descriptor layout attributes.
     * The array instance is cached and cleared if the UiParent is changed.
     */
    @Override
    public AttributeDescriptor[] getAttributeDescriptors() {
        if (!getDescriptor().syncAttributes()) {
            mCachedAttributeDescriptors = null;
        }
        if (mCachedAttributeDescriptors != null) {
            return mCachedAttributeDescriptors;
        }

        UiElementNode ui_parent = getUiParent();
        AttributeDescriptor[] direct_attrs = super.getAttributeDescriptors();
        mCachedAttributeDescriptors = direct_attrs;

        // Compute layout attributes: These depend on the *parent* this widget is within
        AttributeDescriptor[] layout_attrs = null;
        boolean need_xmlns = false;

        if (ui_parent instanceof UiDocumentNode) {
            // Limitation: right now the layout behaves as if everything was
            // owned by a FrameLayout.
            // TODO replace by something user-configurable.

            IProject project = getEditor().getProject();
            if (project != null) {
                Sdk currentSdk = Sdk.getCurrent();
                if (currentSdk != null) {
                    IAndroidTarget target = currentSdk.getTarget(project);
                    if (target != null) {
                        AndroidTargetData data = currentSdk.getTargetData(target);
                        if (data != null) {
                            LayoutDescriptors descriptors = data.getLayoutDescriptors();
                            ViewElementDescriptor desc =
                                descriptors.findDescriptorByClass(FQCN_FRAME_LAYOUT);
                            if (desc != null) {
                                layout_attrs = desc.getLayoutAttributes();
                                need_xmlns = true;
                            }
                        }
                    }
                }
            }
        } else if (ui_parent instanceof UiViewElementNode) {
            layout_attrs =
                ((ViewElementDescriptor) ui_parent.getDescriptor()).getLayoutAttributes();
        }

        if (layout_attrs == null || layout_attrs.length == 0) {
            return mCachedAttributeDescriptors;
        }

        mCachedAttributeDescriptors =
            new AttributeDescriptor[direct_attrs.length +
                                    layout_attrs.length +
                                    (need_xmlns ? 1 : 0)];
        System.arraycopy(direct_attrs, 0,
                mCachedAttributeDescriptors, 0,
                direct_attrs.length);
        System.arraycopy(layout_attrs, 0,
                mCachedAttributeDescriptors, direct_attrs.length,
                layout_attrs.length);
        if (need_xmlns) {
            AttributeDescriptor desc = new XmlnsAttributeDescriptor(ANDROID_NS_NAME, ANDROID_URI);
            mCachedAttributeDescriptors[direct_attrs.length + layout_attrs.length] = desc;
        }

        return mCachedAttributeDescriptors;
    }

    public Image getIcon() {
        ElementDescriptor desc = getDescriptor();
        if (desc != null) {
            Image img = null;
            // Special case for the common case of vertical linear layouts:
            // show vertical linear icon (the default icon shows horizontal orientation)
            String uiName = desc.getUiName();
            IconFactory icons = IconFactory.getInstance();
            if (uiName.equals(LINEAR_LAYOUT)) {
                Element e = (Element) getXmlNode();
                if (VALUE_VERTICAL.equals(e.getAttributeNS(ANDROID_URI, ATTR_ORIENTATION))) {
                    IconFactory factory = icons;
                    img = factory.getIcon("VerticalLinearLayout"); //$NON-NLS-1$
                }
            } else if (uiName.equals(VIEW_TAG)) {
                Node xmlNode = getXmlNode();
                if (xmlNode instanceof Element) {
                    String className = ((Element) xmlNode).getAttribute(ATTR_CLASS);
                    if (className != null && className.length() > 0) {
                        int index = className.lastIndexOf('.');
                        if (index != -1) {
                            className = "customView"; //$NON-NLS-1$
                        }
                        img = icons.getIcon(className);
                    }
                }

                if (img == null) {
                    // Can't have both view.png and View.png; issues on case sensitive vs
                    // case insensitive file systems
                    img = icons.getIcon("View"); //$NON-NLS-1$
                }
            }
            if (img == null) {
                img = desc.getGenericIcon();
            }

            if (img != null) {
                AndroidXmlEditor editor = getEditor();
                if (editor != null) {
                    LayoutEditorDelegate delegate = LayoutEditorDelegate.fromEditor(editor);
                    if (delegate != null) {
                        IMarker marker = delegate.getIssueForNode(this);
                        if (marker != null) {
                            int severity = marker.getAttribute(IMarker.SEVERITY, 0);
                            if (severity == IMarker.SEVERITY_ERROR) {
                                return icons.addErrorIcon(img);
                            } else {
                                return icons.addWarningIcon(img);
                            }
                        }
                    }
                }

                return img;
            }

            return img;
        }

        return AdtPlugin.getAndroidLogo();
    }

    /**
     * Sets the parent of this UI node.
     * <p/>
     * Also removes the cached AttributeDescriptor array that depends on the current UiParent.
     */
    @Override
    protected void setUiParent(UiElementNode parent) {
        super.setUiParent(parent);
        mCachedAttributeDescriptors = null;
    }
}
