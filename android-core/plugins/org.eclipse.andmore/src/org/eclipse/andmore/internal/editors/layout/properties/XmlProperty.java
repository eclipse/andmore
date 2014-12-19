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
package org.eclipse.andmore.internal.editors.layout.properties;

import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN;
import static com.android.SdkConstants.ATTR_LAYOUT_RESOURCE_PREFIX;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.api.IAttributeInfo;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.common.CommonXmlEditor;
import org.eclipse.andmore.internal.editors.descriptors.AttributeDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.DescriptorsUtils;
import org.eclipse.andmore.internal.editors.layout.gle2.GraphicalEditorPart;
import org.eclipse.andmore.internal.editors.layout.gle2.ViewHierarchy;
import org.eclipse.andmore.internal.editors.layout.uimodel.UiViewElementNode;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.wb.internal.core.model.property.Property;
import org.eclipse.wb.internal.core.model.property.editor.PropertyEditor;
import org.eclipse.wb.internal.core.model.property.table.PropertyTooltipProvider;
import org.eclipse.wb.internal.core.model.property.table.PropertyTooltipTextProvider;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import java.util.Map;

/**
 * An Android XML property
 */
class XmlProperty extends Property {
    private PropertyFactory mFactory;
    final AttributeDescriptor mDescriptor;
    private UiViewElementNode mNode;
    private Property mParent;

    XmlProperty(
            @NonNull PropertyEditor editor,
            @NonNull PropertyFactory factory,
            @NonNull UiViewElementNode node,
            @NonNull AttributeDescriptor descriptor) {
        super(editor);
        mFactory = factory;
        mNode = node;
        mDescriptor = descriptor;
    }

    @NonNull
    public PropertyFactory getFactory() {
        return mFactory;
    }

    @NonNull
    public UiViewElementNode getNode() {
        return mNode;
    }

    @NonNull
    public AttributeDescriptor getDescriptor() {
        return mDescriptor;
    }

    @Override
    @NonNull
    public String getName() {
        return mDescriptor.getXmlLocalName();
    }

    @Override
    @NonNull
    public String getTitle() {
        String name = mDescriptor.getXmlLocalName();
        int nameLength = name.length();

        if (name.startsWith(ATTR_LAYOUT_RESOURCE_PREFIX)) {
            if (name.startsWith(ATTR_LAYOUT_MARGIN)
                    && nameLength > ATTR_LAYOUT_MARGIN.length()) {
                name = name.substring(ATTR_LAYOUT_MARGIN.length());
            } else {
                name = name.substring(ATTR_LAYOUT_RESOURCE_PREFIX.length());
            }
        }

        // Capitalize
        name = DescriptorsUtils.capitalize(name);

        // If we're nested within a complex property, say "Line Spacing", don't
        // include "Line Spacing " as a prefix for each property here
        if (mParent != null) {
            String parentTitle = mParent.getTitle();
            if (name.startsWith(parentTitle)) {
                int parentTitleLength = parentTitle.length();
                if (parentTitleLength < nameLength) {
                    if (nameLength > parentTitleLength &&
                            Character.isWhitespace(name.charAt(parentTitleLength))) {
                        parentTitleLength++;
                    }
                    name = name.substring(parentTitleLength);
                }
            }
        }

        return name;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        // tooltip
        if (adapter == PropertyTooltipProvider.class) {
            return adapter.cast(new PropertyTooltipTextProvider() {
                @Override
                protected String getText(Property p) throws Exception {
                    if (mDescriptor instanceof IPropertyDescriptor) {
                        IPropertyDescriptor d = (IPropertyDescriptor) mDescriptor;
                        return d.getDescription();
                    }

                    return null;
                }
            });
        } else if (adapter == IContentProposalProvider.class) {
            IAttributeInfo info = mDescriptor.getAttributeInfo();
            if (info != null) {
                return adapter.cast(new PropertyValueCompleter(this));
            }
            // Fallback: complete values on resource values
            return adapter.cast(new ResourceValueCompleter(this));
        } else if (adapter == ILabelProvider.class) {
            return adapter.cast(new LabelProvider() {
                @Override
              public Image getImage(Object element) {
                  return AdtPlugin.getAndroidLogo();
              }

              @Override
              public String getText(Object element) {
                  return ((IContentProposal) element).getLabel();
              }
            });
        }
        return super.getAdapter(adapter);
    }

    @Override
    public boolean isModified() throws Exception {
        Object s = null;
        try {
            Element element = (Element) mNode.getXmlNode();
            if (element == null) {
                return false;
            }
            String name = mDescriptor.getXmlLocalName();
            String uri = mDescriptor.getNamespaceUri();
            if (uri != null) {
                return element.hasAttributeNS(uri, name);
            } else {
                return element.hasAttribute(name);
            }
        } catch (Exception e) {
            // pass
        }
        return s != null && s.toString().length() > 0;
    }

    @Nullable
    public String getStringValue() {
        Element element = (Element) mNode.getXmlNode();
        if (element == null) {
            return null;
        }
        String name = mDescriptor.getXmlLocalName();
        String uri = mDescriptor.getNamespaceUri();
        Attr attr;
        if (uri != null) {
            attr = element.getAttributeNodeNS(uri, name);
        } else {
            attr = element.getAttributeNode(name);
        }
        if (attr != null) {
            return attr.getValue();
        }

        Object viewObject = getFactory().getCurrentViewObject();
        if (viewObject != null) {
            GraphicalEditorPart graphicalEditor = getGraphicalEditor();
            if (graphicalEditor == null) {
                return null;
            }
            ViewHierarchy views = graphicalEditor.getCanvasControl().getViewHierarchy();
            Map<String, String> defaultProperties = views.getDefaultProperties(viewObject);
            if (defaultProperties != null) {
                return defaultProperties.get(name);
            }
        }

        return null;
    }

    @Override
    @Nullable
    public Object getValue() throws Exception {
        return getStringValue();
    }

    @Override
    public void setValue(Object value) throws Exception {
        CommonXmlEditor editor = getXmlEditor();
        if (editor == null) {
            return;
        }
        final String attribute = mDescriptor.getXmlLocalName();
        final String xmlValue = value != null && value != UNKNOWN_VALUE ? value.toString() : null;
        editor.wrapUndoEditXmlModel(
                String.format("Set \"%1$s\" to \"%2$s\"", attribute, xmlValue),
                new Runnable() {
            @Override
            public void run() {
                mNode.setAttributeValue(attribute,
                        mDescriptor.getNamespaceUri(), xmlValue, true /*override*/);
                mNode.commitDirtyAttributesToXml();
            }
        });
    }

    @Override
    @NonNull
    public Property getComposite(Property[] properties) {
        return XmlPropertyComposite.create(properties);
    }

    @Nullable
    GraphicalEditorPart getGraphicalEditor() {
        return mFactory.getGraphicalEditor();
    }

    @Nullable
    CommonXmlEditor getXmlEditor() {
        GraphicalEditorPart graphicalEditor = getGraphicalEditor();
        if (graphicalEditor != null) {
            return graphicalEditor.getEditorDelegate().getEditor();
        }

        return null;
    }

    @Nullable
    public Property getParent() {
        return mParent;
    }

    public void setParent(@Nullable Property parent) {
        mParent = parent;
    }

    @Override
    public String toString() {
        return getName() + ":" + getPriority();
    }
}
