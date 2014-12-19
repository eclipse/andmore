/*
 * Copyright (C) 2009 The Android Open Source Project
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

package org.eclipse.andmore.internal.editors.layout.gre;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.api.IAttributeInfo;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.INodeHandler;
import com.android.ide.common.api.Margins;
import com.android.ide.common.api.Rect;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.common.resources.platform.AttributeInfo;
import org.eclipse.andmore.internal.editors.AndroidXmlEditor;
import org.eclipse.andmore.internal.editors.descriptors.AttributeDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.DescriptorsUtils;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.editors.layout.descriptors.ViewElementDescriptor;
import org.eclipse.andmore.internal.editors.layout.gle2.CanvasViewInfo;
import org.eclipse.andmore.internal.editors.layout.gle2.SimpleAttribute;
import org.eclipse.andmore.internal.editors.layout.gle2.SwtUtils;
import org.eclipse.andmore.internal.editors.layout.gle2.ViewHierarchy;
import org.eclipse.andmore.internal.editors.layout.uimodel.UiViewElementNode;
import org.eclipse.andmore.internal.editors.uimodel.UiAttributeNode;
import org.eclipse.andmore.internal.editors.uimodel.UiDocumentNode;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.andmore.internal.project.SupportLibraryHelper;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.graphics.Rectangle;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class NodeProxy implements INode {
    private static final Margins NO_MARGINS = new Margins(0, 0, 0, 0);
    private final UiViewElementNode mNode;
    private final Rect mBounds;
    private final NodeFactory mFactory;
    /** Map from URI to Map(key=>value) (where no namespace uses "" as a key) */
    private Map<String, Map<String, String>> mPendingAttributes;

    /**
     * Creates a new {@link INode} that wraps an {@link UiViewElementNode} that is
     * actually valid in the current UI/XML model. The view may not be part of the canvas
     * yet (e.g. if it has just been dynamically added and the canvas hasn't reloaded yet.)
     * <p/>
     * This method is package protected. To create a node, please use {@link NodeFactory} instead.
     *
     * @param uiNode The node to wrap.
     * @param bounds The bounds of a the view in the canvas. Must be either: <br/>
     *   - a valid rect for a view that is actually in the canvas <br/>
     *   - <b>*or*</b> null (or an invalid rect) for a view that has just been added dynamically
     *   to the model. We never store a null bounds rectangle in the node, a null rectangle
     *   will be converted to an invalid rectangle.
     * @param factory A {@link NodeFactory} to create unique children nodes.
     */
    /*package*/ NodeProxy(UiViewElementNode uiNode, Rectangle bounds, NodeFactory factory) {
        mNode = uiNode;
        mFactory = factory;
        if (bounds == null) {
            mBounds = new Rect();
        } else {
            mBounds = SwtUtils.toRect(bounds);
        }
    }

    @Override
    public @NonNull Rect getBounds() {
        return mBounds;
    }

    @Override
    public @NonNull Margins getMargins() {
        ViewHierarchy viewHierarchy = mFactory.getCanvas().getViewHierarchy();
        CanvasViewInfo view = viewHierarchy.findViewInfoFor(this);
        if (view != null) {
            Margins margins = view.getMargins();
            if (margins != null) {
                return margins;
            }
        }

        return NO_MARGINS;
    }


    @Override
    public int getBaseline() {
        ViewHierarchy viewHierarchy = mFactory.getCanvas().getViewHierarchy();
        CanvasViewInfo view = viewHierarchy.findViewInfoFor(this);
        if (view != null) {
            return view.getBaseline();
        }

        return -1;
    }

    /**
     * Updates the bounds of this node proxy. Bounds cannot be null, but it can be invalid.
     * This is a package-protected method, only the {@link NodeFactory} uses this method.
     */
    /*package*/ void setBounds(Rectangle bounds) {
        SwtUtils.set(mBounds, bounds);
    }

    /**
     * Returns the {@link UiViewElementNode} corresponding to this
     * {@link NodeProxy}.
     *
     * @return The {@link UiViewElementNode} corresponding to this
     *         {@link NodeProxy}
     */
    public UiViewElementNode getNode() {
        return mNode;
    }

    @Override
    public @NonNull String getFqcn() {
        if (mNode != null) {
            ElementDescriptor desc = mNode.getDescriptor();
            if (desc instanceof ViewElementDescriptor) {
                return ((ViewElementDescriptor) desc).getFullClassName();
            }
        }

        return "";
    }


    // ---- Hierarchy handling ----


    @Override
    public INode getRoot() {
        if (mNode != null) {
            UiElementNode p = mNode.getUiRoot();
            // The node root should be a document. Instead what we really mean to
            // return is the top level view element.
            if (p instanceof UiDocumentNode) {
                List<UiElementNode> children = p.getUiChildren();
                if (children.size() > 0) {
                    p = children.get(0);
                }
            }

            // Cope with a badly structured XML layout
            while (p != null && !(p instanceof UiViewElementNode)) {
                p = p.getUiNextSibling();
            }

            if (p == mNode) {
                return this;
            }
            if (p instanceof UiViewElementNode) {
                return mFactory.create((UiViewElementNode) p);
            }
        }

        return null;
    }

    @Override
    public INode getParent() {
        if (mNode != null) {
            UiElementNode p = mNode.getUiParent();
            if (p instanceof UiViewElementNode) {
                return mFactory.create((UiViewElementNode) p);
            }
        }

        return null;
    }

    @Override
    public @NonNull INode[] getChildren() {
        if (mNode != null) {
            List<UiElementNode> uiChildren = mNode.getUiChildren();
            List<INode> nodes = new ArrayList<INode>(uiChildren.size());
            for (UiElementNode uiChild : uiChildren) {
                if (uiChild instanceof UiViewElementNode) {
                    nodes.add(mFactory.create((UiViewElementNode) uiChild));
                }
            }

            return nodes.toArray(new INode[nodes.size()]);
        }

        return new INode[0];
    }


    // ---- XML Editing ---

    @Override
    public void editXml(@NonNull String undoName, final @NonNull INodeHandler c) {
        final AndroidXmlEditor editor = mNode.getEditor();

        if (editor != null) {
            // Create an undo edit XML wrapper, which takes a runnable
            editor.wrapUndoEditXmlModel(
                    undoName,
                    new Runnable() {
                        @Override
                        public void run() {
                            // Here editor.isEditXmlModelPending returns true and it
                            // is safe to edit the model using any method from INode.

                            // Finally execute the closure that will act on the XML
                            c.handle(NodeProxy.this);
                            applyPendingChanges();
                        }
                    });
        }
    }

    private void checkEditOK() {
        final AndroidXmlEditor editor = mNode.getEditor();
        if (!editor.isEditXmlModelPending()) {
            throw new RuntimeException("Error: XML edit call without using INode.editXml!");
        }
    }

    @Override
    public @NonNull INode appendChild(@NonNull String viewFqcn) {
        return insertOrAppend(viewFqcn, -1);
    }

    @Override
    public @NonNull INode insertChildAt(@NonNull String viewFqcn, int index) {
        return insertOrAppend(viewFqcn, index);
    }

    @Override
    public void removeChild(@NonNull INode node) {
        checkEditOK();

        ((NodeProxy) node).mNode.deleteXmlNode();
    }

    private INode insertOrAppend(String viewFqcn, int index) {
        checkEditOK();

        AndroidXmlEditor editor = mNode.getEditor();
        if (editor != null) {
            // Possibly replace the tag with a compatibility version if the
            // minimum SDK requires it
            IProject project = editor.getProject();
            if (project != null) {
                viewFqcn = SupportLibraryHelper.getTagFor(project, viewFqcn);
            }
        }

        // Find the descriptor for this FQCN
        ViewElementDescriptor vd = getFqcnViewDescriptor(viewFqcn);
        if (vd == null) {
            warnPrintf("Can't create a new %s element", viewFqcn);
            return null;
        }

        final UiElementNode uiNew;
        if (index == -1) {
            // Append at the end.
            uiNew = mNode.appendNewUiChild(vd);
        } else {
            // Insert at the requested position or at the end.
            int n = mNode.getUiChildren().size();
            if (index < 0 || index >= n) {
                uiNew = mNode.appendNewUiChild(vd);
            } else {
                uiNew = mNode.insertNewUiChild(index, vd);
            }
        }

        // Set default attributes -- but only for new widgets (not when moving or copying)
        RulesEngine engine = null;
        LayoutEditorDelegate delegate = LayoutEditorDelegate.fromEditor(editor);
        if (delegate != null) {
            engine = delegate.getRulesEngine();
        }
        if (engine == null || engine.getInsertType().isCreate()) {
            // TODO: This should probably use IViewRule#getDefaultAttributes() at some point
            DescriptorsUtils.setDefaultLayoutAttributes(uiNew, false /*updateLayout*/);
        }

        Node xmlNode = uiNew.createXmlNode();

        if (!(uiNew instanceof UiViewElementNode) || xmlNode == null) {
            // Both things are not supposed to happen. When they do, we're in big trouble.
            // We don't really know how to revert the state at this point and the UI model is
            // now out of sync with the XML model.
            // Panic ensues.
            // The best bet is to abort now. The edit wrapper will release the edit and the
            // XML/UI should get reloaded properly (with a likely invalid XML.)
            warnPrintf("Failed to create a new %s element", viewFqcn);
            throw new RuntimeException("XML node creation failed."); //$NON-NLS-1$
        }

        UiViewElementNode uiNewView = (UiViewElementNode) uiNew;
        NodeProxy newNode = mFactory.create(uiNewView);

        if (engine != null) {
            engine.callCreateHooks(editor, this, newNode, null);
        }

        return newNode;
    }

    @Override
    public boolean setAttribute(
            @Nullable String uri,
            @NonNull String name,
            @Nullable String value) {
        checkEditOK();
        UiAttributeNode attr = mNode.setAttributeValue(name, uri, value, true /* override */);

        if (uri == null) {
            uri = ""; //$NON-NLS-1$
        }

        Map<String, String> map = null;
        if (mPendingAttributes == null) {
            // Small initial size: we don't expect many different namespaces
            mPendingAttributes = new HashMap<String, Map<String, String>>(3);
        } else {
            map = mPendingAttributes.get(uri);
        }
        if (map == null) {
            map = new HashMap<String, String>();
            mPendingAttributes.put(uri, map);
        }
        map.put(name, value);

        return attr != null;
    }

    @Override
    public String getStringAttr(@Nullable String uri, @NonNull String attrName) {
        UiElementNode uiNode = mNode;

        if (attrName == null) {
            return null;
        }

        if (mPendingAttributes != null) {
            Map<String, String> map = mPendingAttributes.get(uri == null ? "" : uri); //$NON-NLS-1$
            if (map != null) {
                String value = map.get(attrName);
                if (value != null) {
                    return value;
                }
            }
        }

        if (uiNode.getXmlNode() != null) {
            Node xmlNode = uiNode.getXmlNode();
            if (xmlNode != null) {
                NamedNodeMap nodeAttributes = xmlNode.getAttributes();
                if (nodeAttributes != null) {
                    Node attr = nodeAttributes.getNamedItemNS(uri, attrName);
                    if (attr != null) {
                        return attr.getNodeValue();
                    }
                }
            }
        }
        return null;
    }

    @Override
    public IAttributeInfo getAttributeInfo(@Nullable String uri, @NonNull String attrName) {
        UiElementNode uiNode = mNode;

        if (attrName == null) {
            return null;
        }

        for (AttributeDescriptor desc : uiNode.getAttributeDescriptors()) {
            String dUri = desc.getNamespaceUri();
            String dName = desc.getXmlLocalName();
            if ((uri == null && dUri == null) || (uri != null && uri.equals(dUri))) {
                if (attrName.equals(dName)) {
                    return desc.getAttributeInfo();
                }
            }
        }

        return null;
    }

    @Override
    public @NonNull IAttributeInfo[] getDeclaredAttributes() {

        AttributeDescriptor[] descs = mNode.getAttributeDescriptors();
        int n = descs.length;
        IAttributeInfo[] infos = new AttributeInfo[n];

        for (int i = 0; i < n; i++) {
            infos[i] = descs[i].getAttributeInfo();
        }

        return infos;
    }

    @Override
    public @NonNull List<String> getAttributeSources() {
        ElementDescriptor descriptor = mNode.getDescriptor();
        if (descriptor instanceof ViewElementDescriptor) {
            return ((ViewElementDescriptor) descriptor).getAttributeSources();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public @NonNull IAttribute[] getLiveAttributes() {
        UiElementNode uiNode = mNode;

        if (uiNode.getXmlNode() != null) {
            Node xmlNode = uiNode.getXmlNode();
            if (xmlNode != null) {
                NamedNodeMap nodeAttributes = xmlNode.getAttributes();
                if (nodeAttributes != null) {

                    int n = nodeAttributes.getLength();
                    IAttribute[] result = new IAttribute[n];
                    for (int i = 0; i < n; i++) {
                        Node attr = nodeAttributes.item(i);
                        String uri = attr.getNamespaceURI();
                        String name = attr.getLocalName();
                        String value = attr.getNodeValue();

                        result[i] = new SimpleAttribute(uri, name, value);
                    }
                    return result;
                }
            }
        }

        return new IAttribute[0];

    }

    @Override
    public String toString() {
        return "NodeProxy [node=" + mNode + ", bounds=" + mBounds + "]";
    }

    // --- internal helpers ---

    /**
     * Helper methods that returns a {@link ViewElementDescriptor} for the requested FQCN.
     * Will return null if we can't find that FQCN or we lack the editor/data/descriptors info
     * (which shouldn't really happen since at this point the SDK should be fully loaded and
     * isn't reloading, or we wouldn't be here editing XML for a layout rule.)
     */
    private ViewElementDescriptor getFqcnViewDescriptor(String fqcn) {
        LayoutEditorDelegate delegate = LayoutEditorDelegate.fromEditor(mNode.getEditor());
        if (delegate != null) {
            return delegate.getFqcnViewDescriptor(fqcn);
        }

        return null;
    }

    private void warnPrintf(String msg, Object...params) {
        AdtPlugin.printToConsole(
                mNode == null ? "" : mNode.getDescriptor().getXmlLocalName(),
                String.format(msg, params)
                );
    }

    /**
     * If there are any pending changes in these nodes, apply them now
     *
     * @return true if any modifications were made
     */
    public boolean applyPendingChanges() {
        boolean modified = false;

        // Flush all pending attributes
        if (mPendingAttributes != null) {
            mNode.commitDirtyAttributesToXml();
            modified = true;
            mPendingAttributes = null;

        }
        for (INode child : getChildren()) {
            modified |= ((NodeProxy) child).applyPendingChanges();
        }

        return modified;
    }
}
