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
package org.eclipse.andmore.internal.editors.layout.gle2;

import static com.android.SdkConstants.ANDROID_NS_NAME;
import static com.android.SdkConstants.NS_RESOURCES;
import static com.android.SdkConstants.XMLNS_URI;

import com.android.ide.common.api.IDragElement;
import com.android.ide.common.api.IDragElement.IDragAttribute;
import com.android.ide.common.api.INode;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.descriptors.DescriptorsUtils;
import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.editors.layout.descriptors.ViewElementDescriptor;
import org.eclipse.andmore.internal.editors.layout.gre.NodeProxy;
import org.eclipse.andmore.internal.editors.layout.gre.RulesEngine;
import org.eclipse.andmore.internal.editors.layout.uimodel.UiViewElementNode;
import org.eclipse.andmore.internal.editors.uimodel.UiDocumentNode;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Composite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@link ClipboardSupport} class manages the native clipboard, providing operations
 * to copy, cut and paste view items, and can answer whether the clipboard contains
 * a transferable we care about.
 */
public class ClipboardSupport {
    private static final boolean DEBUG = false;

    /** SWT clipboard instance. */
    private Clipboard mClipboard;
    private LayoutCanvas mCanvas;

    /**
     * Constructs a new {@link ClipboardSupport} tied to the given
     * {@link LayoutCanvas}.
     *
     * @param canvas The {@link LayoutCanvas} to provide clipboard support for.
     * @param parent The parent widget in the SWT hierarchy of the canvas.
     */
    public ClipboardSupport(LayoutCanvas canvas, Composite parent) {
        mCanvas = canvas;

        mClipboard = new Clipboard(parent.getDisplay());
    }

    /**
     * Frees up any resources held by the {@link ClipboardSupport}.
     */
    public void dispose() {
        if (mClipboard != null) {
            mClipboard.dispose();
            mClipboard = null;
        }
    }

    /**
     * Perform the "Copy" action, either from the Edit menu or from the context
     * menu.
     * <p/>
     * This sanitizes the selection, so it must be a copy. It then inserts the
     * selection both as text and as {@link SimpleElement}s in the clipboard.
     * (If there is selected text in the error label, then the error is used
     * as the text portion of the transferable.)
     *
     * @param selection A list of selection items to add to the clipboard;
     *            <b>this should be a copy already - this method will not make a
     *            copy</b>
     */
    public void copySelectionToClipboard(List<SelectionItem> selection) {
        SelectionManager.sanitize(selection);

        // The error message area shares the copy action with the canvas. Invoking the
        // copy action when there are errors visible *AND* the user has selected text there,
        // should include the error message as the text transferable.
        String message = null;
        GraphicalEditorPart graphicalEditor = mCanvas.getEditorDelegate().getGraphicalEditor();
        StyledText errorLabel = graphicalEditor.getErrorLabel();
        if (errorLabel.getSelectionCount() > 0) {
            message = errorLabel.getSelectionText();
        }

        if (selection.isEmpty()) {
            if (message != null) {
                mClipboard.setContents(
                        new Object[] { message },
                        new Transfer[] { TextTransfer.getInstance() }
                );
            }
            return;
        }

        Object[] data = new Object[] {
                SelectionItem.getAsElements(selection),
                message != null ? message : SelectionItem.getAsText(mCanvas, selection)
        };

        Transfer[] types = new Transfer[] {
                SimpleXmlTransfer.getInstance(),
                TextTransfer.getInstance()
        };

        mClipboard.setContents(data, types);
    }

    /**
     * Perform the "Cut" action, either from the Edit menu or from the context
     * menu.
     * <p/>
     * This sanitizes the selection, so it must be a copy. It uses the
     * {@link #copySelectionToClipboard(List)} method to copy the selection to
     * the clipboard. Finally it uses {@link #deleteSelection(String, List)} to
     * delete the selection with a "Cut" verb for the title.
     *
     * @param selection A list of selection items to add to the clipboard;
     *            <b>this should be a copy already - this method will not make a
     *            copy</b>
     */
    public void cutSelectionToClipboard(List<SelectionItem> selection) {
        copySelectionToClipboard(selection);
        deleteSelection(mCanvas.getCutLabel(), selection);
    }

    /**
     * Deletes the given selection.
     *
     * @param verb A translated verb for the action. Will be used for the
     *            undo/redo title. Typically this should be
     *            {@link Action#getText()} for either the cut or the delete
     *            actions in the canvas.
     * @param selection The selection. Must not be null. Can be empty, in which
     *            case nothing happens. The selection list will be sanitized so
     *            the caller should pass in a copy.
     */
    public void deleteSelection(String verb, final List<SelectionItem> selection) {
        SelectionManager.sanitize(selection);

        if (selection.isEmpty()) {
            return;
        }

        // If all selected items have the same *kind* of parent, display that in the undo title.
        String title = null;
        for (SelectionItem cs : selection) {
            CanvasViewInfo vi = cs.getViewInfo();
            if (vi != null && vi.getParent() != null) {
                CanvasViewInfo parent = vi.getParent();
                assert parent != null;
                if (title == null) {
                    title = parent.getName();
                } else if (!title.equals(parent.getName())) {
                    // More than one kind of parent selected.
                    title = null;
                    break;
                }
            }
        }

        if (title != null) {
            // Typically the name is an FQCN. Just get the last segment.
            int pos = title.lastIndexOf('.');
            if (pos > 0 && pos < title.length() - 1) {
                title = title.substring(pos + 1);
            }
        }
        boolean multiple = mCanvas.getSelectionManager().hasMultiSelection();
        if (title == null) {
            title = String.format(
                        multiple ? "%1$s elements" : "%1$s element",
                        verb);
        } else {
            title = String.format(
                        multiple ? "%1$s elements from %2$s" : "%1$s element from %2$s",
                        verb, title);
        }

        // Implementation note: we don't clear the internal selection after removing
        // the elements. An update XML model event should happen when the model gets released
        // which will trigger a recompute of the layout, thus reloading the model thus
        // resetting the selection.
        mCanvas.getEditorDelegate().getEditor().wrapUndoEditXmlModel(title, new Runnable() {
            @Override
            public void run() {
                // Segment the deleted nodes into clusters of siblings
                Map<NodeProxy, List<INode>> clusters =
                        new HashMap<NodeProxy, List<INode>>();
                for (SelectionItem cs : selection) {
                    NodeProxy node = cs.getNode();
                    if (node == null) {
                        continue;
                    }
                    INode parent = node.getParent();
                    if (parent != null) {
                        List<INode> children = clusters.get(parent);
                        if (children == null) {
                            children = new ArrayList<INode>();
                            clusters.put((NodeProxy) parent, children);
                        }
                        children.add(node);
                    }
                }

                // Notify parent views about children getting deleted
                RulesEngine rulesEngine = mCanvas.getRulesEngine();
                for (Map.Entry<NodeProxy, List<INode>> entry : clusters.entrySet()) {
                    NodeProxy parent = entry.getKey();
                    List<INode> children = entry.getValue();
                    assert children != null && children.size() > 0;
                    rulesEngine.callOnRemovingChildren(parent, children);
                    parent.applyPendingChanges();
                }

                for (SelectionItem cs : selection) {
                    CanvasViewInfo vi = cs.getViewInfo();
                    // You can't delete the root element
                    if (vi != null && !vi.isRoot()) {
                        UiViewElementNode ui = vi.getUiViewNode();
                        if (ui != null) {
                            ui.deleteXmlNode();
                        }
                    }
                }
            }
        });
    }

    /**
     * Perform the "Paste" action, either from the Edit menu or from the context
     * menu.
     *
     * @param selection A list of selection items to add to the clipboard;
     *            <b>this should be a copy already - this method will not make a
     *            copy</b>
     */
    public void pasteSelection(List<SelectionItem> selection) {

        SimpleXmlTransfer sxt = SimpleXmlTransfer.getInstance();
        final SimpleElement[] pasted = (SimpleElement[]) mClipboard.getContents(sxt);

        if (pasted == null || pasted.length == 0) {
            return;
        }

        CanvasViewInfo lastRoot = mCanvas.getViewHierarchy().getRoot();
        if (lastRoot == null) {
            // Pasting in an empty document. Only paste the first element.
            pasteInEmptyDocument(pasted[0]);
            return;
        }

        // Otherwise use the current selection, if any, as a guide where to paste
        // using the first selected element only. If there's no selection use
        // the root as the insertion point.
        SelectionManager.sanitize(selection);
        final CanvasViewInfo target;
        if (selection.size() > 0) {
            SelectionItem cs = selection.get(0);
            target = cs.getViewInfo();
        } else {
            target = lastRoot;
        }

        final NodeProxy targetNode = mCanvas.getNodeFactory().create(target);
        mCanvas.getEditorDelegate().getEditor().wrapUndoEditXmlModel("Paste", new Runnable() {
            @Override
            public void run() {
                RulesEngine engine = mCanvas.getRulesEngine();
                NodeProxy node = engine.callOnPaste(targetNode, target.getViewObject(), pasted);
                node.applyPendingChanges();
            }
        });
    }

    /**
     * Paste a new root into an empty XML layout.
     * <p/>
     * In case of error (unknown FQCN, document not empty), silently do nothing.
     * In case of success, the new element will have some default attributes set (xmlns:android,
     * layout_width and height). The edit is wrapped in a proper undo.
     * <p/>
     * Implementation is similar to {@link #createDocumentRoot} except we also
     * copy all the attributes and inner elements recursively.
     */
    private void pasteInEmptyDocument(final IDragElement pastedElement) {
        String rootFqcn = pastedElement.getFqcn();

        // Need a valid empty document to create the new root
        final LayoutEditorDelegate delegate = mCanvas.getEditorDelegate();
        final UiDocumentNode uiDoc = delegate.getUiRootNode();
        if (uiDoc == null || uiDoc.getUiChildren().size() > 0) {
            debugPrintf("Failed to paste document root for %1$s: document is not empty", rootFqcn);
            return;
        }

        // Find the view descriptor matching our FQCN
        final ViewElementDescriptor viewDesc = delegate.getFqcnViewDescriptor(rootFqcn);
        if (viewDesc == null) {
            // TODO this could happen if pasting a custom view not known in this project
            debugPrintf("Failed to paste document root, unknown FQCN %1$s", rootFqcn);
            return;
        }

        // Get the last segment of the FQCN for the undo title
        String title = rootFqcn;
        int pos = title.lastIndexOf('.');
        if (pos > 0 && pos < title.length() - 1) {
            title = title.substring(pos + 1);
        }
        title = String.format("Paste root %1$s in document", title);

        delegate.getEditor().wrapUndoEditXmlModel(title, new Runnable() {
            @Override
            public void run() {
                UiElementNode uiNew = uiDoc.appendNewUiChild(viewDesc);

                // A root node requires the Android XMLNS
                uiNew.setAttributeValue(ANDROID_NS_NAME, XMLNS_URI, NS_RESOURCES,
                        true /*override*/);

                // Copy all the attributes from the pasted element
                for (IDragAttribute attr : pastedElement.getAttributes()) {
                    uiNew.setAttributeValue(
                            attr.getName(),
                            attr.getUri(),
                            attr.getValue(),
                            true /*override*/);
                }

                // Adjust the attributes, adding the default layout_width/height
                // only if they are not present (the original element should have
                // them though.)
                DescriptorsUtils.setDefaultLayoutAttributes(uiNew, false /*updateLayout*/);

                uiNew.createXmlNode();

                // Now process all children
                for (IDragElement childElement : pastedElement.getInnerElements()) {
                    addChild(uiNew, childElement);
                }
            }

            private void addChild(UiElementNode uiParent, IDragElement childElement) {
                String childFqcn = childElement.getFqcn();
                final ViewElementDescriptor childDesc =
                    delegate.getFqcnViewDescriptor(childFqcn);
                if (childDesc == null) {
                    // TODO this could happen if pasting a custom view
                    debugPrintf("Failed to paste element, unknown FQCN %1$s", childFqcn);
                    return;
                }

                UiElementNode uiChild = uiParent.appendNewUiChild(childDesc);

                // Copy all the attributes from the pasted element
                for (IDragAttribute attr : childElement.getAttributes()) {
                    uiChild.setAttributeValue(
                            attr.getName(),
                            attr.getUri(),
                            attr.getValue(),
                            true /*override*/);
                }

                // Adjust the attributes, adding the default layout_width/height
                // only if they are not present (the original element should have
                // them though.)
                DescriptorsUtils.setDefaultLayoutAttributes(
                        uiChild, false /*updateLayout*/);

                uiChild.createXmlNode();

                // Now process all grand children
                for (IDragElement grandChildElement : childElement.getInnerElements()) {
                    addChild(uiChild, grandChildElement);
                }
            }
        });
    }

    /**
     * Returns true if we have a a simple xml transfer data object on the
     * clipboard.
     *
     * @return True if and only if the clipboard contains one of XML element
     *         objects.
     */
    public boolean hasSxtOnClipboard() {
        // The paste operation is only available if we can paste our custom type.
        // We do not currently support pasting random text (e.g. XML). Maybe later.
        SimpleXmlTransfer sxt = SimpleXmlTransfer.getInstance();
        for (TransferData td : mClipboard.getAvailableTypes()) {
            if (sxt.isSupportedType(td)) {
                return true;
            }
        }

        return false;
    }

    private void debugPrintf(String message, Object... params) {
        if (DEBUG) AdtPlugin.printToConsole("Clipboard", String.format(message, params));
    }

}
