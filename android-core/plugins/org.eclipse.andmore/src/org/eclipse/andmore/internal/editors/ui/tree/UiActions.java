/*
 * Copyright (C) 2008 The Android Open Source Project
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


package org.eclipse.andmore.internal.editors.ui.tree;

import org.eclipse.andmore.internal.editors.descriptors.DescriptorsUtils;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor.Mandatory;
import org.eclipse.andmore.internal.editors.uimodel.UiDocumentNode;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Shell;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.List;

/**
 * Performs basic actions on an XML tree: add node, remove node, move up/down.
 */
public abstract class UiActions implements ICommitXml {

    public UiActions() {
    }

    //---------------------
    // Actual implementations must override these to provide specific hooks

    /** Returns the UiDocumentNode for the current model. */
    abstract protected UiElementNode getRootNode();

    /** Commits pending data before the XML model is modified. */
    @Override
    abstract public void commitPendingXmlChanges();

    /**
     * Utility method to select an outline item based on its model node
     *
     * @param uiNode The node to select. Can be null (in which case nothing should happen)
     */
    abstract protected void selectUiNode(UiElementNode uiNode);

    //---------------------

    /**
     * Called when the "Add..." button next to the tree view is selected.
     * <p/>
     * This simplified version of doAdd does not support descriptor filters and creates
     * a new {@link UiModelTreeLabelProvider} for each call.
     */
    public void doAdd(UiElementNode uiNode, Shell shell) {
        doAdd(uiNode, null /* descriptorFilters */, shell, new UiModelTreeLabelProvider());
    }

    /**
     * Called when the "Add..." button next to the tree view is selected.
     *
     * Displays a selection dialog that lets the user select which kind of node
     * to create, depending on the current selection.
     */
    public void doAdd(UiElementNode uiNode,
            ElementDescriptor[] descriptorFilters,
            Shell shell, ILabelProvider labelProvider) {
        // If the root node is a document with already a root, use it as the root node
        UiElementNode rootNode = getRootNode();
        if (rootNode instanceof UiDocumentNode && rootNode.getUiChildren().size() > 0) {
            rootNode = rootNode.getUiChildren().get(0);
        }

        NewItemSelectionDialog dlg = new NewItemSelectionDialog(
                shell,
                labelProvider,
                descriptorFilters,
                uiNode, rootNode);
        dlg.open();
        Object[] results = dlg.getResult();
        if (results != null && results.length > 0) {
            addElement(dlg.getChosenRootNode(), null, (ElementDescriptor) results[0],
                    true /*updateLayout*/);
        }
    }

    /**
     * Adds a new XML element based on the {@link ElementDescriptor} to the given parent
     * {@link UiElementNode}, and then select it.
     * <p/>
     * If the parent is a document root which already contains a root element, the inner
     * root element is used as the actual parent. This ensure you can't create a broken
     * XML file with more than one root element.
     * <p/>
     * If a sibling is given and that sibling has the same parent, the new node is added
     * right after that sibling. Otherwise the new node is added at the end of the parent
     * child list.
     *
     * @param uiParent An existing UI node or null to add to the tree root
     * @param uiSibling An existing UI node before which to insert the new node. Can be null.
     * @param descriptor The descriptor of the element to add
     * @param updateLayout True if layout attributes should be set
     * @return The new {@link UiElementNode} or null.
     */
    public UiElementNode addElement(UiElementNode uiParent,
            UiElementNode uiSibling,
            ElementDescriptor descriptor,
            boolean updateLayout) {
        if (uiParent instanceof UiDocumentNode && uiParent.getUiChildren().size() > 0) {
            uiParent = uiParent.getUiChildren().get(0);
        }
        if (uiSibling != null && uiSibling.getUiParent() != uiParent) {
            uiSibling = null;
        }

        UiElementNode uiNew = addNewTreeElement(uiParent, uiSibling, descriptor, updateLayout);
        selectUiNode(uiNew);

        return uiNew;
    }

    /**
     * Called when the "Remove" button is selected.
     *
     * If the tree has a selection, remove it.
     * This simply deletes the XML node attached to the UI node: when the XML model fires the
     * update event, the tree will get refreshed.
     */
    public void doRemove(final List<UiElementNode> nodes, Shell shell) {

        if (nodes == null || nodes.size() == 0) {
            return;
        }

        final int len = nodes.size();

        StringBuilder sb = new StringBuilder();
        for (UiElementNode node : nodes) {
            sb.append("\n- "); //$NON-NLS-1$
            sb.append(node.getBreadcrumbTrailDescription(false /* include_root */));
        }

        if (MessageDialog.openQuestion(shell,
                len > 1 ? "Remove elements from Android XML"  // title
                        : "Remove element from Android XML",
                String.format("Do you really want to remove %1$s?", sb.toString()))) {
            commitPendingXmlChanges();
            getRootNode().getEditor().wrapEditXmlModel(new Runnable() {
                @Override
                public void run() {
                    UiElementNode previous = null;
                    UiElementNode parent = null;

                    for (int i = len - 1; i >= 0; i--) {
                        UiElementNode node = nodes.get(i);
                        previous = node.getUiPreviousSibling();
                        parent = node.getUiParent();

                        // delete node
                        node.deleteXmlNode();
                    }

                    // try to select the last previous sibling or the last parent
                    if (previous != null) {
                        selectUiNode(previous);
                    } else if (parent != null) {
                        selectUiNode(parent);
                    }
                }
            });
        }
    }

    /**
     * Called when the "Up" button is selected.
     * <p/>
     * If the tree has a selection, move it up, either in the child list or as the last child
     * of the previous parent.
     */
    public void doUp(
            final List<UiElementNode> uiNodes,
            final ElementDescriptor[] descriptorFilters) {
        if (uiNodes == null || uiNodes.size() < 1) {
            return;
        }

        final Node[]          selectXmlNode = { null };
        final UiElementNode[] uiLastNode    = { null };
        final UiElementNode[] uiSearchRoot  = { null };

        commitPendingXmlChanges();
        getRootNode().getEditor().wrapEditXmlModel(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < uiNodes.size(); i++) {
                    UiElementNode uiNode = uiLastNode[0] = uiNodes.get(i);
                    doUpInternal(
                            uiNode,
                            descriptorFilters,
                            selectXmlNode,
                            uiSearchRoot,
                            false /*testOnly*/);
                }
            }
        });

        assert uiLastNode[0] != null; // tell Eclipse this can't be null below

        if (selectXmlNode[0] == null) {
            // The XML node has not been moved, we can just select the same UI node
            selectUiNode(uiLastNode[0]);
        } else {
            // The XML node has moved. At this point the UI model has been reloaded
            // and the XML node has been affected to a new UI node. Find that new UI
            // node and select it.
            if (uiSearchRoot[0] == null) {
                uiSearchRoot[0] = uiLastNode[0].getUiRoot();
            }
            if (uiSearchRoot[0] != null) {
                selectUiNode(uiSearchRoot[0].findXmlNode(selectXmlNode[0]));
            }
        }
    }

    /**
     * Checks whether the "up" action can be performed on all items.
     *
     * @return True if the up action can be carried on *all* items.
     */
    public boolean canDoUp(
            List<UiElementNode> uiNodes,
            ElementDescriptor[] descriptorFilters) {
        if (uiNodes == null || uiNodes.size() < 1) {
            return false;
        }

        final Node[]          selectXmlNode = { null };
        final UiElementNode[] uiSearchRoot  = { null };

        commitPendingXmlChanges();

        for (int i = 0; i < uiNodes.size(); i++) {
            if (!doUpInternal(
                    uiNodes.get(i),
                    descriptorFilters,
                    selectXmlNode,
                    uiSearchRoot,
                    true /*testOnly*/)) {
                return false;
            }
        }

        return true;
    }

    private boolean doUpInternal(
            UiElementNode uiNode,
            ElementDescriptor[] descriptorFilters,
            Node[] outSelectXmlNode,
            UiElementNode[] outUiSearchRoot,
            boolean testOnly) {
        // the node will move either up to its parent or grand-parent
        outUiSearchRoot[0] = uiNode.getUiParent();
        if (outUiSearchRoot[0] != null && outUiSearchRoot[0].getUiParent() != null) {
            outUiSearchRoot[0] = outUiSearchRoot[0].getUiParent();
        }
        Node xmlNode = uiNode.getXmlNode();
        ElementDescriptor nodeDesc = uiNode.getDescriptor();
        if (xmlNode == null || nodeDesc == null) {
            return false;
        }
        UiElementNode uiParentNode = uiNode.getUiParent();
        Node xmlParent = uiParentNode == null ? null : uiParentNode.getXmlNode();
        if (xmlParent == null) {
            return false;
        }

        UiElementNode uiPrev = uiNode.getUiPreviousSibling();

        // Only accept a sibling that has an XML attached and
        // is part of the allowed descriptor filters.
        while (uiPrev != null &&
                (uiPrev.getXmlNode() == null || !matchDescFilter(descriptorFilters, uiPrev))) {
            uiPrev = uiPrev.getUiPreviousSibling();
        }

        if (uiPrev != null && uiPrev.getXmlNode() != null) {
            // This node is not the first one of the parent.
            Node xmlPrev = uiPrev.getXmlNode();
            if (uiPrev.getDescriptor().acceptChild(nodeDesc)) {
                // If the previous sibling can accept this child, then it
                // is inserted at the end of the children list.
                if (testOnly) {
                    return true;
                }
                xmlPrev.appendChild(xmlParent.removeChild(xmlNode));
                outSelectXmlNode[0] = xmlNode;
            } else {
                // This node is not the first one of the parent, so it can be
                // removed and then inserted before its previous sibling.
                if (testOnly) {
                    return true;
                }
                xmlParent.insertBefore(
                        xmlParent.removeChild(xmlNode),
                        xmlPrev);
                outSelectXmlNode[0] = xmlNode;
            }
        } else if (uiParentNode != null && !(xmlParent instanceof Document)) {
            UiElementNode uiGrandParent = uiParentNode.getUiParent();
            Node xmlGrandParent = uiGrandParent == null ? null : uiGrandParent.getXmlNode();
            ElementDescriptor grandDesc =
                uiGrandParent == null ? null : uiGrandParent.getDescriptor();

            if (xmlGrandParent != null &&
                    !(xmlGrandParent instanceof Document) &&
                    grandDesc != null &&
                    grandDesc.acceptChild(nodeDesc)) {
                // If the node is the first one of the child list of its
                // parent, move it up in the hierarchy as previous sibling
                // to the parent. This is only possible if the parent of the
                // parent is not a document.
                // The parent node must actually accept this kind of child.

                if (testOnly) {
                    return true;
                }
                xmlGrandParent.insertBefore(
                        xmlParent.removeChild(xmlNode),
                        xmlParent);
                outSelectXmlNode[0] = xmlNode;
            }
        }

        return false;
    }

    private boolean matchDescFilter(
            ElementDescriptor[] descriptorFilters,
            UiElementNode uiNode) {
        if (descriptorFilters == null || descriptorFilters.length < 1) {
            return true;
        }

        ElementDescriptor desc = uiNode.getDescriptor();

        for (ElementDescriptor filter : descriptorFilters) {
            if (filter.equals(desc)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Called when the "Down" button is selected.
     *
     * If the tree has a selection, move it down, either in the same child list or as the
     * first child of the next parent.
     */
    public void doDown(
            final List<UiElementNode> nodes,
            final ElementDescriptor[] descriptorFilters) {
        if (nodes == null || nodes.size() < 1) {
            return;
        }

        final Node[]          selectXmlNode = { null };
        final UiElementNode[] uiLastNode    = { null };
        final UiElementNode[] uiSearchRoot  = { null };

        commitPendingXmlChanges();
        getRootNode().getEditor().wrapEditXmlModel(new Runnable() {
            @Override
            public void run() {
                for (int i = nodes.size() - 1; i >= 0; i--) {
                    final UiElementNode node = uiLastNode[0] = nodes.get(i);
                    doDownInternal(
                            node,
                            descriptorFilters,
                            selectXmlNode,
                            uiSearchRoot,
                            false /*testOnly*/);
                }
            }
        });

        assert uiLastNode[0] != null; // tell Eclipse this can't be null below

        if (selectXmlNode[0] == null) {
            // The XML node has not been moved, we can just select the same UI node
            selectUiNode(uiLastNode[0]);
        } else {
            // The XML node has moved. At this point the UI model has been reloaded
            // and the XML node has been affected to a new UI node. Find that new UI
            // node and select it.
            if (uiSearchRoot[0] == null) {
                uiSearchRoot[0] = uiLastNode[0].getUiRoot();
            }
            if (uiSearchRoot[0] != null) {
                selectUiNode(uiSearchRoot[0].findXmlNode(selectXmlNode[0]));
            }
        }
    }

    /**
     * Checks whether the "down" action can be performed on all items.
     *
     * @return True if the down action can be carried on *all* items.
     */
    public boolean canDoDown(
            List<UiElementNode> uiNodes,
            ElementDescriptor[] descriptorFilters) {
        if (uiNodes == null || uiNodes.size() < 1) {
            return false;
        }

        final Node[]          selectXmlNode = { null };
        final UiElementNode[] uiSearchRoot  = { null };

        commitPendingXmlChanges();

        for (int i = 0; i < uiNodes.size(); i++) {
            if (!doDownInternal(
                    uiNodes.get(i),
                    descriptorFilters,
                    selectXmlNode,
                    uiSearchRoot,
                    true /*testOnly*/)) {
                return false;
            }
        }

        return true;
    }

    private boolean doDownInternal(
            UiElementNode uiNode,
            ElementDescriptor[] descriptorFilters,
            Node[] outSelectXmlNode,
            UiElementNode[] outUiSearchRoot,
            boolean testOnly) {
        // the node will move either down to its parent or grand-parent
        outUiSearchRoot[0] = uiNode.getUiParent();
        if (outUiSearchRoot[0] != null && outUiSearchRoot[0].getUiParent() != null) {
            outUiSearchRoot[0] = outUiSearchRoot[0].getUiParent();
        }

        Node xmlNode = uiNode.getXmlNode();
        ElementDescriptor nodeDesc = uiNode.getDescriptor();
        if (xmlNode == null || nodeDesc == null) {
            return false;
        }
        UiElementNode uiParentNode = uiNode.getUiParent();
        Node xmlParent = uiParentNode == null ? null : uiParentNode.getXmlNode();
        if (xmlParent == null) {
            return false;
        }

        UiElementNode uiNext = uiNode.getUiNextSibling();

        // Only accept a sibling that has an XML attached and
        // is part of the allowed descriptor filters.
        while (uiNext != null &&
                (uiNext.getXmlNode() == null || !matchDescFilter(descriptorFilters, uiNext))) {
            uiNext = uiNext.getUiNextSibling();
        }

        if (uiNext != null && uiNext.getXmlNode() != null) {
            // This node is not the last one of the parent.
            Node xmlNext = uiNext.getXmlNode();
            // If the next sibling is a node that can have children, though,
            // then the node is inserted as the first child.
            if (uiNext.getDescriptor().acceptChild(nodeDesc)) {
                if (testOnly) {
                    return true;
                }
                // Note: insertBefore works as append if the ref node is
                // null, i.e. when the node doesn't have children yet.
                xmlNext.insertBefore(
                        xmlParent.removeChild(xmlNode),
                        xmlNext.getFirstChild());
                outSelectXmlNode[0] = xmlNode;
            } else {
                // This node is not the last one of the parent, so it can be
                // removed and then inserted after its next sibling.

                if (testOnly) {
                    return true;
                }
                // Insert "before after next" ;-)
                xmlParent.insertBefore(
                        xmlParent.removeChild(xmlNode),
                        xmlNext.getNextSibling());
                outSelectXmlNode[0] = xmlNode;
            }
        } else if (uiParentNode != null && !(xmlParent instanceof Document)) {
            UiElementNode uiGrandParent = uiParentNode.getUiParent();
            Node xmlGrandParent = uiGrandParent == null ? null : uiGrandParent.getXmlNode();
            ElementDescriptor grandDesc =
                uiGrandParent == null ? null : uiGrandParent.getDescriptor();

            if (xmlGrandParent != null &&
                    !(xmlGrandParent instanceof Document) &&
                    grandDesc != null &&
                    grandDesc.acceptChild(nodeDesc)) {
                // This node is the last node of its parent.
                // If neither the parent nor the grandparent is a document,
                // then the node can be inserted right after the parent.
                // The parent node must actually accept this kind of child.
                if (testOnly) {
                    return true;
                }
                xmlGrandParent.insertBefore(
                        xmlParent.removeChild(xmlNode),
                        xmlParent.getNextSibling());
                outSelectXmlNode[0] = xmlNode;
            }
        }

        return false;
    }

    //---------------------

    /**
     * Adds a new element of the given descriptor's type to the given UI parent node.
     *
     * This actually creates the corresponding XML node in the XML model, which in turn
     * will refresh the current tree view.
     *
     * @param uiParent An existing UI node or null to add to the tree root
     * @param uiSibling An existing UI node to insert right before. Can be null.
     * @param descriptor The descriptor of the element to add
     * @param updateLayout True if layout attributes should be set
     * @return The {@link UiElementNode} that has been added to the UI tree.
     */
    private UiElementNode addNewTreeElement(UiElementNode uiParent,
            UiElementNode uiSibling,
            ElementDescriptor descriptor,
            final boolean updateLayout) {
        commitPendingXmlChanges();

        List<UiElementNode> uiChildren = uiParent.getUiChildren();
        int n = uiChildren.size();

        // The default is to append at the end of the list.
        int index = n;

        if (uiSibling != null) {
            // Try to find the requested sibling.
            index = uiChildren.indexOf(uiSibling);
            if (index < 0) {
                // This sibling didn't exist. Should not happen but compensate
                // by simply adding to the end of the list.
                uiSibling = null;
                index = n;
            }
        }

        if (uiSibling == null) {
            // If we don't require any specific position, make sure to insert before the
            // first mandatory_last descriptor's position, if any.

            for (int i = 0; i < n; i++) {
                UiElementNode uiChild = uiChildren.get(i);
                if (uiChild.getDescriptor().getMandatory() == Mandatory.MANDATORY_LAST) {
                    index = i;
                    break;
                }
            }
        }

        final UiElementNode uiNew = uiParent.insertNewUiChild(index, descriptor);
        UiElementNode rootNode = getRootNode();

        rootNode.getEditor().wrapEditXmlModel(new Runnable() {
            @Override
            public void run() {
                DescriptorsUtils.setDefaultLayoutAttributes(uiNew, updateLayout);
                uiNew.createXmlNode();
            }
        });
        return uiNew;
    }
}
