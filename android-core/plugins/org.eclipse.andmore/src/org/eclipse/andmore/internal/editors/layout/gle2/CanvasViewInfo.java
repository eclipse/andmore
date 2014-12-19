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

package org.eclipse.andmore.internal.editors.layout.gle2;

import static com.android.SdkConstants.FQCN_SPACE;
import static com.android.SdkConstants.FQCN_SPACE_V7;
import static com.android.SdkConstants.GESTURE_OVERLAY_VIEW;
import static com.android.SdkConstants.VIEW_MERGE;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.api.Margins;
import com.android.ide.common.api.Rect;
import com.android.ide.common.rendering.api.Capability;
import com.android.ide.common.rendering.api.MergeCookie;
import com.android.ide.common.rendering.api.ViewInfo;
import com.android.utils.Pair;

import org.eclipse.andmore.common.layout.GridLayoutRule;
import org.eclipse.andmore.internal.editors.descriptors.AttributeDescriptor;
import org.eclipse.andmore.internal.editors.layout.UiElementPullParser;
import org.eclipse.andmore.internal.editors.layout.uimodel.UiViewElementNode;
import org.eclipse.andmore.internal.editors.uimodel.UiAttributeNode;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.IPropertySource;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Maps a {@link ViewInfo} in a structure more adapted to our needs.
 * The only large difference is that we keep both the original bounds of the view info
 * and we pre-compute the selection bounds which are absolute to the rendered image
 * (whereas the original bounds are relative to the parent view.)
 * <p/>
 * Each view also knows its parent and children.
 * <p/>
 * We can't alter {@link ViewInfo} as it is part of the LayoutBridge and needs to
 * have a fixed API.
 * <p/>
 * The view info also implements {@link IPropertySource}, which enables a linked
 * {@link IPropertySheetPage} to display the attributes of the selected element.
 * This class actually delegates handling of {@link IPropertySource} to the underlying
 * {@link UiViewElementNode}, if any.
 */
public class CanvasViewInfo implements IPropertySource {

    /**
     * Minimal size of the selection, in case an empty view or layout is selected.
     */
    public static final int SELECTION_MIN_SIZE = 6;

    private final Rectangle mAbsRect;
    private final Rectangle mSelectionRect;
    private final String mName;
    private final Object mViewObject;
    private final UiViewElementNode mUiViewNode;
    private CanvasViewInfo mParent;
    private ViewInfo mViewInfo;
    private final List<CanvasViewInfo> mChildren = new ArrayList<CanvasViewInfo>();

    /**
     * Is this view info an individually exploded view? This is the case for views
     * that were specially inflated by the {@link UiElementPullParser} and assigned
     * fixed padding because they were invisible and somebody requested visibility.
     */
    private boolean mExploded;

    /**
     * Node sibling. This is usually null, but it's possible for a single node in the
     * model to have <b>multiple</b> separate views in the canvas, for example
     * when you {@code <include>} a view that has multiple widgets inside a
     * {@code <merge>} tag. In this case, all the views have the same node model,
     * the include tag, and selecting the include should highlight all the separate
     * views that are linked to this node. That's what this field is all about: it is
     * a <b>circular</b> list of all the siblings that share the same node.
     */
    private List<CanvasViewInfo> mNodeSiblings;

    /**
     * Constructs a {@link CanvasViewInfo} initialized with the given initial values.
     */
    private CanvasViewInfo(CanvasViewInfo parent, String name,
            Object viewObject, UiViewElementNode node, Rectangle absRect,
            Rectangle selectionRect, ViewInfo viewInfo) {
        mParent = parent;
        mName = name;
        mViewObject = viewObject;
        mViewInfo = viewInfo;
        mUiViewNode  = node;
        mAbsRect = absRect;
        mSelectionRect = selectionRect;
    }

    /**
     * Returns the original {@link ViewInfo} bounds in absolute coordinates
     * over the whole graphic.
     *
     * @return the bounding box in absolute coordinates
     */
    @NonNull
    public Rectangle getAbsRect() {
        return mAbsRect;
    }

    /**
     * Returns the absolute selection bounds of the view info as a rectangle.
     * The selection bounds will always have a size greater or equal to
     * {@link #SELECTION_MIN_SIZE}.
     * The width/height is inclusive (i.e. width = right-left-1).
     * This is in absolute "screen" coordinates (relative to the rendered bitmap).
     *
     * @return the absolute selection bounds
     */
    @NonNull
    public Rectangle getSelectionRect() {
        return mSelectionRect;
    }

    /**
     * Returns the view node. Could be null, although unlikely.
     * @return An {@link UiViewElementNode} that uniquely identifies the object in the XML model.
     * @see ViewInfo#getCookie()
     */
    @Nullable
    public UiViewElementNode getUiViewNode() {
        return mUiViewNode;
    }

    /**
     * Returns the parent {@link CanvasViewInfo}.
     * It is null for the root and non-null for children.
     *
     * @return the parent {@link CanvasViewInfo}, which can be null
     */
    @Nullable
    public CanvasViewInfo getParent() {
        return mParent;
    }

    /**
     * Returns the list of children of this {@link CanvasViewInfo}.
     * The list is never null. It can be empty.
     * By contract, this.getChildren().get(0..n-1).getParent() == this.
     *
     * @return the children, never null
     */
    @NonNull
    public List<CanvasViewInfo> getChildren() {
        return mChildren;
    }

    /**
     * For nodes that have multiple views rendered from a single node, such as the
     * children of a {@code <merge>} tag included into a separate layout, return the
     * "primary" view, the first view that is rendered
     */
    @Nullable
    private CanvasViewInfo getPrimaryNodeSibling() {
        if (mNodeSiblings == null || mNodeSiblings.size() == 0) {
            return null;
        }

        return mNodeSiblings.get(0);
    }

    /**
     * Returns true if this view represents one view of many linked to a single node, and
     * where this is the primary view. The primary view is the one that will be shown
     * in the outline for example (since we only show nodes, not views, in the outline,
     * and therefore don't want repetitions when a view has more than one view info.)
     *
     * @return true if this is the primary view among more than one linked to a single
     *         node
     */
    private boolean isPrimaryNodeSibling() {
        return getPrimaryNodeSibling() == this;
    }

    /**
     * Returns the list of node sibling of this view (which <b>will include this
     * view</b>). For most views this is going to be null, but for views that share a
     * single node (such as widgets inside a {@code <merge>} tag included into another
     * layout), this will provide all the views that correspond to the node.
     *
     * @return a non-empty list of siblings (including this), or null
     */
    @Nullable
    public List<CanvasViewInfo> getNodeSiblings() {
        return mNodeSiblings;
    }

    /**
     * Returns all the children of the canvas view info where each child corresponds to a
     * unique node that the user can see and select. This is intended for use by the
     * outline for example, where only the actual nodes are displayed, not the views
     * themselves.
     * <p>
     * Most views have their own nodes, so this is generally the same as
     * {@link #getChildren}, except in the case where you for example include a view that
     * has multiple widgets inside a {@code <merge>} tag, where all these widgets have the
     * same node (the {@code <merge>} tag).
     *
     * @return list of {@link CanvasViewInfo} objects that are children of this view,
     *         never null
     */
    @NonNull
    public List<CanvasViewInfo> getUniqueChildren() {
        boolean haveHidden = false;

        for (CanvasViewInfo info : mChildren) {
            if (info.mNodeSiblings != null) {
                // We have secondary children; must create a new collection containing
                // only non-secondary children
                List<CanvasViewInfo> children = new ArrayList<CanvasViewInfo>();
                for (CanvasViewInfo vi : mChildren) {
                    if (vi.mNodeSiblings == null) {
                        children.add(vi);
                    } else if (vi.isPrimaryNodeSibling()) {
                        children.add(vi);
                    }
                }
                return children;
            }

            haveHidden |= info.isHidden();
        }

        if (haveHidden) {
            List<CanvasViewInfo> children = new ArrayList<CanvasViewInfo>(mChildren.size());
            for (CanvasViewInfo vi : mChildren) {
                if (!vi.isHidden()) {
                    children.add(vi);
                }
            }

            return children;
        }

        return mChildren;
    }

    /**
     * Returns true if the specific {@link CanvasViewInfo} is a parent
     * of this {@link CanvasViewInfo}. It can be a direct parent or any
     * grand-parent higher in the hierarchy.
     *
     * @param potentialParent the view info to check
     * @return true if the given info is a parent of this view
     */
    public boolean isParent(@NonNull CanvasViewInfo potentialParent) {
        CanvasViewInfo p = mParent;
        while (p != null) {
            if (p == potentialParent) {
                return true;
            }
            p = p.getParent();
        }
        return false;
    }

    /**
     * Returns the name of the {@link CanvasViewInfo}.
     * Could be null, although unlikely.
     * Experience shows this is the full qualified Java name of the View.
     * TODO: Rename this method to getFqcn.
     *
     * @return the name of the view info
     *
     * @see ViewInfo#getClassName()
     */
    @NonNull
    public String getName() {
        return mName;
    }

    /**
     * Returns the View object associated with the {@link CanvasViewInfo}.
     * @return the view object or null.
     */
    @Nullable
    public Object getViewObject() {
        return mViewObject;
    }

    /**
     * Returns the baseline of this object, or -1 if it does not support a baseline
     *
     * @return the baseline or -1
     */
    public int getBaseline() {
        if (mViewInfo != null) {
            int baseline = mViewInfo.getBaseLine();
            if (baseline != Integer.MIN_VALUE) {
                return baseline;
            }
        }

        return -1;
    }

    /**
     * Returns the {@link Margins} for this {@link CanvasViewInfo}
     *
     * @return the {@link Margins} for this {@link CanvasViewInfo}
     */
    @Nullable
    public Margins getMargins() {
        if (mViewInfo != null) {
            int leftMargin = mViewInfo.getLeftMargin();
            int topMargin = mViewInfo.getTopMargin();
            int rightMargin = mViewInfo.getRightMargin();
            int bottomMargin = mViewInfo.getBottomMargin();
            return new Margins(
                leftMargin != Integer.MIN_VALUE ? leftMargin : 0,
                rightMargin != Integer.MIN_VALUE ? rightMargin : 0,
                topMargin != Integer.MIN_VALUE ? topMargin : 0,
                bottomMargin != Integer.MIN_VALUE ? bottomMargin : 0
            );
        }

        return null;
    }

    // ---- Implementation of IPropertySource
    // TODO: Get rid of this once the old propertysheet implementation is fully gone

    @Override
    public Object getEditableValue() {
        UiViewElementNode uiView = getUiViewNode();
        if (uiView != null) {
            return ((IPropertySource) uiView).getEditableValue();
        }
        return null;
    }

    @Override
    public IPropertyDescriptor[] getPropertyDescriptors() {
        UiViewElementNode uiView = getUiViewNode();
        if (uiView != null) {
            return ((IPropertySource) uiView).getPropertyDescriptors();
        }
        return null;
    }

    @Override
    public Object getPropertyValue(Object id) {
        UiViewElementNode uiView = getUiViewNode();
        if (uiView != null) {
            return ((IPropertySource) uiView).getPropertyValue(id);
        }
        return null;
    }

    @Override
    public boolean isPropertySet(Object id) {
        UiViewElementNode uiView = getUiViewNode();
        if (uiView != null) {
            return ((IPropertySource) uiView).isPropertySet(id);
        }
        return false;
    }

    @Override
    public void resetPropertyValue(Object id) {
        UiViewElementNode uiView = getUiViewNode();
        if (uiView != null) {
            ((IPropertySource) uiView).resetPropertyValue(id);
        }
    }

    @Override
    public void setPropertyValue(Object id, Object value) {
        UiViewElementNode uiView = getUiViewNode();
        if (uiView != null) {
            ((IPropertySource) uiView).setPropertyValue(id, value);
        }
    }

    /**
     * Returns the XML node corresponding to this info, or null if there is no
     * such XML node.
     *
     * @return The XML node corresponding to this info object, or null
     */
    @Nullable
    public Node getXmlNode() {
        UiViewElementNode uiView = getUiViewNode();
        if (uiView != null) {
            return uiView.getXmlNode();
        }

        return null;
    }

    /**
     * Returns true iff this view info corresponds to a root element.
     *
     * @return True iff this is a root view info.
     */
    public boolean isRoot() {
        // Select the visual element -- unless it's the root.
        // The root element is the one whose GRAND parent
        // is null (because the parent will be a -document-
        // node).

        // Special case: a gesture overlay is sometimes added as the root, but for all intents
        // and purposes it is its layout child that is the real root so treat that one as the
        // root as well (such that the whole layout canvas does not highlight as part of hovers
        // etc)
        if (mParent != null
                && mParent.mName.endsWith(GESTURE_OVERLAY_VIEW)
                && mParent.isRoot()
                && mParent.mChildren.size() == 1) {
            return true;
        }

        return mUiViewNode == null || mUiViewNode.getUiParent() == null ||
            mUiViewNode.getUiParent().getUiParent() == null;
    }

    /**
     * Returns true if this {@link CanvasViewInfo} represents an invisible widget that
     * should be highlighted when selected.  This is the case for any layout that is less than the minimum
     * threshold ({@link #SELECTION_MIN_SIZE}), or any other view that has -0- bounds.
     *
     * @return True if this is a tiny layout or invisible view
     */
    public boolean isInvisible() {
        if (isHidden()) {
            // Don't expand and highlight hidden widgets
            return false;
        }

        if (mAbsRect.width < SELECTION_MIN_SIZE || mAbsRect.height < SELECTION_MIN_SIZE) {
            return mUiViewNode != null && (mUiViewNode.getDescriptor().hasChildren() ||
                    mAbsRect.width <= 0 || mAbsRect.height <= 0);
        }

        return false;
    }

    /**
     * Returns true if this {@link CanvasViewInfo} represents a widget that should be
     * hidden, such as a {@code <Space>} which are typically not manipulated by the user
     * through dragging etc.
     *
     * @return true if this is a hidden view
     */
    public boolean isHidden() {
        if (GridLayoutRule.sDebugGridLayout) {
            return false;
        }

        return FQCN_SPACE.equals(mName) || FQCN_SPACE_V7.equals(mName);
    }

    /**
     * Is this {@link CanvasViewInfo} a view that has had its padding inflated in order to
     * make it visible during selection or dragging? Note that this is NOT considered to
     * be the case in the explode-all-views mode where all nodes have their padding
     * increased; it's only used for views that individually exploded because they were
     * requested visible and they returned true for {@link #isInvisible()}.
     *
     * @return True if this is an exploded node.
     */
    public boolean isExploded() {
        return mExploded;
    }

    /**
     * Mark this {@link CanvasViewInfo} as having been exploded or not. See the
     * {@link #isExploded()} method for details on what this property means.
     *
     * @param exploded New value of the exploded property to mark this info with.
     */
    void setExploded(boolean exploded) {
        mExploded = exploded;
    }

    /**
     * Returns the info represented as a {@link SimpleElement}.
     *
     * @return A {@link SimpleElement} wrapping this info.
     */
    @NonNull
    SimpleElement toSimpleElement() {

        UiViewElementNode uiNode = getUiViewNode();

        String fqcn = SimpleXmlTransfer.getFqcn(uiNode.getDescriptor());
        String parentFqcn = null;
        Rect bounds = SwtUtils.toRect(getAbsRect());
        Rect parentBounds = null;

        UiElementNode uiParent = uiNode.getUiParent();
        if (uiParent != null) {
            parentFqcn = SimpleXmlTransfer.getFqcn(uiParent.getDescriptor());
        }
        if (getParent() != null) {
            parentBounds = SwtUtils.toRect(getParent().getAbsRect());
        }

        SimpleElement e = new SimpleElement(fqcn, parentFqcn, bounds, parentBounds);

        for (UiAttributeNode attr : uiNode.getAllUiAttributes()) {
            String value = attr.getCurrentValue();
            if (value != null && value.length() > 0) {
                AttributeDescriptor attrDesc = attr.getDescriptor();
                SimpleAttribute a = new SimpleAttribute(
                        attrDesc.getNamespaceUri(),
                        attrDesc.getXmlLocalName(),
                        value);
                e.addAttribute(a);
            }
        }

        for (CanvasViewInfo childVi : getChildren()) {
            SimpleElement e2 = childVi.toSimpleElement();
            if (e2 != null) {
                e.addInnerElement(e2);
            }
        }

        return e;
    }

    /**
     * Returns the layout url attribute value for the closest surrounding include or
     * fragment element parent, or null if this {@link CanvasViewInfo} is not rendered as
     * part of an include or fragment tag.
     *
     * @return the layout url attribute value for the surrounding include tag, or null if
     *         not applicable
     */
    @Nullable
    public String getIncludeUrl() {
        CanvasViewInfo curr = this;
        while (curr != null) {
            if (curr.mUiViewNode != null) {
                Node node = curr.mUiViewNode.getXmlNode();
                if (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
                    String nodeName = node.getNodeName();
                    if (node.getNamespaceURI() == null
                            && SdkConstants.VIEW_INCLUDE.equals(nodeName)) {
                        // Note: the layout attribute is NOT in the Android namespace
                        Element element = (Element) node;
                        String url = element.getAttribute(SdkConstants.ATTR_LAYOUT);
                        if (url.length() > 0) {
                            return url;
                        }
                    } else if (SdkConstants.VIEW_FRAGMENT.equals(nodeName)) {
                        String url = FragmentMenu.getFragmentLayout(node);
                        if (url != null) {
                            return url;
                        }
                    }
                }
            }
            curr = curr.mParent;
        }

        return null;
    }

    /** Adds the given {@link CanvasViewInfo} as a new last child of this view */
    private void addChild(@NonNull CanvasViewInfo child) {
        mChildren.add(child);
    }

    /** Adds the given {@link CanvasViewInfo} as a child at the given index */
    private void addChildAt(int index, @NonNull CanvasViewInfo child) {
        mChildren.add(index, child);
    }

    /**
     * Removes the given {@link CanvasViewInfo} from the child list of this view, and
     * returns true if it was successfully removed
     *
     * @param child the child to be removed
     * @return true if it was a child and was removed
     */
    public boolean removeChild(@NonNull CanvasViewInfo child) {
        return mChildren.remove(child);
    }

    @Override
    public String toString() {
        return "CanvasViewInfo [name=" + mName + ", node=" + mUiViewNode + "]";
    }

    // ---- Factory functionality ----

    /**
     * Creates a new {@link CanvasViewInfo} hierarchy based on the given {@link ViewInfo}
     * hierarchy. Note that this will not necessarily create one {@link CanvasViewInfo}
     * for each {@link ViewInfo}. It will generally only create {@link CanvasViewInfo}
     * objects for {@link ViewInfo} objects that contain a reference to an
     * {@link UiViewElementNode}, meaning that it corresponds to an element in the XML
     * file for this layout file. This is not always the case, such as in the following
     * scenarios:
     * <ul>
     * <li>we link to other layouts with {@code <include>}
     * <li>the current view is rendered within another view ("Show Included In") such that
     * the outer file does not correspond to elements in the current included XML layout
     * <li>on older platforms that don't support {@link Capability#EMBEDDED_LAYOUT} there
     * is no reference to the {@code <include>} tag
     * <li>with the {@code <merge>} tag we don't get a reference to the corresponding
     * element
     * <ul>
     * <p>
     * This method will build up a set of {@link CanvasViewInfo} that corresponds to the
     * actual <b>selectable</b> views (which are also shown in the Outline).
     *
     * @param layoutlib5 if true, the {@link ViewInfo} hierarchy was created by layoutlib
     *    version 5 or higher, which means this algorithm can make certain assumptions
     *    (for example that {@code <merge>} siblings will provide {@link MergeCookie}
     *    references, so we don't have to search for them.)
     * @param root the root {@link ViewInfo} to build from
     * @return a {@link CanvasViewInfo} hierarchy
     */
    @NonNull
    public static Pair<CanvasViewInfo,List<Rectangle>> create(ViewInfo root, boolean layoutlib5) {
        return new Builder(layoutlib5).create(root);
    }

    /** Builder object which walks over a tree of {@link ViewInfo} objects and builds
     * up a corresponding {@link CanvasViewInfo} hierarchy. */
    private static class Builder {
        public Builder(boolean layoutlib5) {
            mLayoutLib5 = layoutlib5;
        }

        /**
         * The mapping from nodes that have a {@code <merge>} as a parent in the node
         * model to their corresponding views
         */
        private Map<UiViewElementNode, List<CanvasViewInfo>> mMergeNodeMap;

        /**
         * Whether the ViewInfos are provided by a layout library that is version 5 or
         * later, since that will allow us to take several shortcuts
         */
        private boolean mLayoutLib5;

        /**
         * Creates a hierarchy of {@link CanvasViewInfo} objects and merge bounding
         * rectangles from the given {@link ViewInfo} hierarchy
         */
        private Pair<CanvasViewInfo,List<Rectangle>> create(ViewInfo root) {
            Object cookie = root.getCookie();
            if (cookie == null) {
                // Special case: If the root-most view does not have a view cookie,
                // then we are rendering some outer layout surrounding this layout, and in
                // that case we must search down the hierarchy for the (possibly multiple)
                // sub-roots that correspond to elements in this layout, and place them inside
                // an outer view that has no node. In the outline this item will be used to
                // show the inclusion-context.
                CanvasViewInfo rootView = createView(null, root, 0, 0);
                addKeyedSubtrees(rootView, root, 0, 0);

                List<Rectangle> includedBounds = new ArrayList<Rectangle>();
                for (CanvasViewInfo vi : rootView.getChildren()) {
                    if (vi.getNodeSiblings() == null || vi.isPrimaryNodeSibling()) {
                        includedBounds.add(vi.getAbsRect());
                    }
                }

                // There are <merge> nodes here; see if we can insert it into the hierarchy
                if (mMergeNodeMap != null) {
                    // Locate all the nodes that have a <merge> as a parent in the node model,
                    // and where the view sits at the top level inside the include-context node.
                    UiViewElementNode merge = null;
                    List<CanvasViewInfo> merged = new ArrayList<CanvasViewInfo>();
                    for (Map.Entry<UiViewElementNode, List<CanvasViewInfo>> entry : mMergeNodeMap
                            .entrySet()) {
                        UiViewElementNode node = entry.getKey();
                        if (!hasMergeParent(node)) {
                            continue;
                        }
                        List<CanvasViewInfo> views = entry.getValue();
                        assert views.size() > 0;
                        CanvasViewInfo view = views.get(0); // primary
                        if (view.getParent() != rootView) {
                            continue;
                        }
                        UiElementNode parent = node.getUiParent();
                        if (merge != null && parent != merge) {
                            continue;
                        }
                        merge = (UiViewElementNode) parent;
                        merged.add(view);
                    }
                    if (merged.size() > 0) {
                        // Compute a bounding box for the merged views
                        Rectangle absRect = null;
                        for (CanvasViewInfo child : merged) {
                            Rectangle rect = child.getAbsRect();
                            if (absRect == null) {
                                absRect = rect;
                            } else {
                                absRect = absRect.union(rect);
                            }
                        }

                        CanvasViewInfo mergeView = new CanvasViewInfo(rootView, VIEW_MERGE, null,
                                merge, absRect, absRect, null /* viewInfo */);
                        for (CanvasViewInfo view : merged) {
                            if (rootView.removeChild(view)) {
                                mergeView.addChild(view);
                            }
                        }
                        rootView.addChild(mergeView);
                    }
                }

                return Pair.of(rootView, includedBounds);
            } else {
                // We have a view key at the top, so just go and create {@link CanvasViewInfo}
                // objects for each {@link ViewInfo} until we run into a null key.
                CanvasViewInfo rootView = addKeyedSubtrees(null, root, 0, 0);

                // Special case: look to see if the root element is really a <merge>, and if so,
                // manufacture a view for it such that we can target this root element
                // in drag & drop operations, such that we can show it in the outline, etc
                if (rootView != null && hasMergeParent(rootView.getUiViewNode())) {
                    CanvasViewInfo merge = new CanvasViewInfo(null, VIEW_MERGE, null,
                            (UiViewElementNode) rootView.getUiViewNode().getUiParent(),
                            rootView.getAbsRect(), rootView.getSelectionRect(),
                            null /* viewInfo */);
                    // Insert the <merge> as the new real root
                    rootView.mParent = merge;
                    merge.addChild(rootView);
                    rootView = merge;
                }

                return Pair.of(rootView, null);
            }
        }

        private boolean hasMergeParent(UiViewElementNode rootNode) {
            UiElementNode rootParent = rootNode.getUiParent();
            return (rootParent instanceof UiViewElementNode
                    && VIEW_MERGE.equals(rootParent.getDescriptor().getXmlName()));
        }

        /** Creates a {@link CanvasViewInfo} for a given {@link ViewInfo} but does not recurse */
        private CanvasViewInfo createView(CanvasViewInfo parent, ViewInfo root, int parentX,
                int parentY) {
            Object cookie = root.getCookie();
            UiViewElementNode node = null;
            if (cookie instanceof UiViewElementNode) {
                node = (UiViewElementNode) cookie;
            } else if (cookie instanceof MergeCookie) {
                cookie = ((MergeCookie) cookie).getCookie();
                if (cookie instanceof UiViewElementNode) {
                    node = (UiViewElementNode) cookie;
                    CanvasViewInfo view = createView(parent, root, parentX, parentY, node);
                    if (root.getCookie() instanceof MergeCookie && view.mNodeSiblings == null) {
                        List<CanvasViewInfo> v = mMergeNodeMap == null ?
                                null : mMergeNodeMap.get(node);
                        if (v != null) {
                            v.add(view);
                        } else {
                            v = new ArrayList<CanvasViewInfo>();
                            v.add(view);
                            if (mMergeNodeMap == null) {
                                mMergeNodeMap =
                                    new HashMap<UiViewElementNode, List<CanvasViewInfo>>();
                            }
                            mMergeNodeMap.put(node, v);
                        }
                        view.mNodeSiblings = v;
                    }

                    return view;
                }
            }

            return createView(parent, root, parentX, parentY, node);
        }

        /**
         * Creates a {@link CanvasViewInfo} for a given {@link ViewInfo} but does not recurse.
         * This method specifies an explicit {@link UiViewElementNode} to use rather than
         * relying on the view cookie in the info object.
         */
        private CanvasViewInfo createView(CanvasViewInfo parent, ViewInfo root, int parentX,
                int parentY, UiViewElementNode node) {

            int x = root.getLeft();
            int y = root.getTop();
            int w = root.getRight() - x;
            int h = root.getBottom() - y;

            x += parentX;
            y += parentY;

            Rectangle absRect = new Rectangle(x, y, w - 1, h - 1);

            if (w < SELECTION_MIN_SIZE) {
                int d = (SELECTION_MIN_SIZE - w) / 2;
                x -= d;
                w += SELECTION_MIN_SIZE - w;
            }

            if (h < SELECTION_MIN_SIZE) {
                int d = (SELECTION_MIN_SIZE - h) / 2;
                y -= d;
                h += SELECTION_MIN_SIZE - h;
            }

            Rectangle selectionRect = new Rectangle(x, y, w - 1, h - 1);

            return new CanvasViewInfo(parent, root.getClassName(), root.getViewObject(), node,
                    absRect, selectionRect, root);
        }

        /** Create a subtree recursively until you run out of keys */
        private CanvasViewInfo createSubtree(CanvasViewInfo parent, ViewInfo viewInfo,
                int parentX, int parentY) {
            assert viewInfo.getCookie() != null;

            CanvasViewInfo view = createView(parent, viewInfo, parentX, parentY);
            // Bug workaround: Ensure that we never have a child node identical
            // to its parent node: this can happen for example when rendering a
            // ZoomControls view where the merge cookies point to the parent.
            if (parent != null && view.mUiViewNode == parent.mUiViewNode) {
                return null;
            }

            // Process children:
            parentX += viewInfo.getLeft();
            parentY += viewInfo.getTop();

            List<ViewInfo> children = viewInfo.getChildren();

            if (mLayoutLib5) {
                for (ViewInfo child : children) {
                    Object cookie = child.getCookie();
                    if (cookie instanceof UiViewElementNode || cookie instanceof MergeCookie) {
                        CanvasViewInfo childView = createSubtree(view, child,
                                parentX, parentY);
                        if (childView != null) {
                            view.addChild(childView);
                        }
                    } // else: null cookies, adapter item references, etc: No child views.
                }

                return view;
            }

            // See if we have any missing keys at this level
            int missingNodes = 0;
            int mergeNodes = 0;
            for (ViewInfo child : children) {
                // Only use children which have a ViewKey of the correct type.
                // We can't interact with those when they have a null key or
                // an incompatible type.
                Object cookie = child.getCookie();
                if (!(cookie instanceof UiViewElementNode)) {
                    if (cookie instanceof MergeCookie) {
                        mergeNodes++;
                    } else {
                        missingNodes++;
                    }
                }
            }

            if (missingNodes == 0 && mergeNodes == 0) {
                // No missing nodes; this is the normal case, and we can just continue to
                // recursively add our children
                for (ViewInfo child : children) {
                    CanvasViewInfo childView = createSubtree(view, child,
                            parentX, parentY);
                    view.addChild(childView);
                }

                // TBD: Emit placeholder views for keys that have no views?
            } else {
                // We don't have keys for one or more of the ViewInfos. There are many
                // possible causes: we are on an SDK platform that does not support
                // embedded_layout rendering, or we are including a view with a <merge>
                // as the root element.

                UiViewElementNode uiViewNode = view.getUiViewNode();
                String containerName = uiViewNode != null
                    ? uiViewNode.getDescriptor().getXmlLocalName() : ""; //$NON-NLS-1$
                if (containerName.equals(SdkConstants.VIEW_INCLUDE)) {
                    // This is expected -- we don't WANT to get node keys for the content
                    // of an include since it's in a different file and should be treated
                    // as a single unit that cannot be edited (hence, no CanvasViewInfo
                    // children)
                } else {
                    // We are getting children with null keys where we don't expect it;
                    // this usually means that we are dealing with an Android platform
                    // that does not support {@link Capability#EMBEDDED_LAYOUT}, or
                    // that there are <merge> tags which are doing surprising things
                    // to the view hierarchy
                    LinkedList<UiViewElementNode> unused = new LinkedList<UiViewElementNode>();
                    if (uiViewNode != null) {
                        for (UiElementNode child : uiViewNode.getUiChildren()) {
                            if (child instanceof UiViewElementNode) {
                                unused.addLast((UiViewElementNode) child);
                            }
                        }
                    }
                    for (ViewInfo child : children) {
                        Object cookie = child.getCookie();
                        if (mergeNodes > 0 && cookie instanceof MergeCookie) {
                            cookie = ((MergeCookie) cookie).getCookie();
                        }
                        if (cookie != null) {
                            unused.remove(cookie);
                        }
                    }

                    if (unused.size() > 0 || mergeNodes > 0) {
                        if (unused.size() == missingNodes) {
                            // The number of unmatched elements and ViewInfos are identical;
                            // it's very likely that they match one to one, so just use these
                            for (ViewInfo child : children) {
                                if (child.getCookie() == null) {
                                    // Only create a flat (non-recursive) view
                                    CanvasViewInfo childView = createView(view, child, parentX,
                                            parentY, unused.removeFirst());
                                    view.addChild(childView);
                                } else {
                                    CanvasViewInfo childView = createSubtree(view, child, parentX,
                                            parentY);
                                    view.addChild(childView);
                                }
                            }
                        } else {
                            // We have an uneven match. In this case we might be dealing
                            // with <merge> etc.
                            // We have no way to associate elements back with the
                            // corresponding <include> tags if there are more than one of
                            // them. That's not a huge tragedy since visually you are not
                            // allowed to edit these anyway; we just need to make a visual
                            // block for these for selection and outline purposes.
                            addMismatched(view, parentX, parentY, children, unused);
                        }
                    } else {
                        // No unused keys, but there are views without keys.
                        // We can't represent these since all views must have node keys
                        // such that you can operate on them. Just ignore these.
                        for (ViewInfo child : children) {
                            if (child.getCookie() != null) {
                                CanvasViewInfo childView = createSubtree(view, child,
                                        parentX, parentY);
                                view.addChild(childView);
                            }
                        }
                    }
                }
            }

            return view;
        }

        /**
         * We have various {@link ViewInfo} children with null keys, and/or nodes in
         * the corresponding UI model that are not referenced by any of the {@link ViewInfo}
         * objects. This method attempts to account for this, by matching the views in
         * the right order.
         */
        private void addMismatched(CanvasViewInfo parentView, int parentX, int parentY,
                List<ViewInfo> children, LinkedList<UiViewElementNode> unused) {
            UiViewElementNode afterNode = null;
            UiViewElementNode beforeNode = null;
            // We have one important clue we can use when matching unused nodes
            // with views: if we have a view V1 with node N1, and a view V2 with node N2,
            // then we can only match unknown node UN with unknown node UV if
            // V1 < UV < V2 and N1 < UN < N2.
            // We can use these constraints to do the matching, for example by
            // a simple DAG traversal. However, since the number of unmatched nodes
            // will typically be very small, we'll just do a simple algorithm here
            // which checks forwards/backwards whether a match is valid.
            for (int index = 0, size = children.size(); index < size; index++) {
                ViewInfo child = children.get(index);
                if (child.getCookie() != null) {
                    CanvasViewInfo childView = createSubtree(parentView, child, parentX, parentY);
                    if (childView != null) {
                        parentView.addChild(childView);
                    }
                    if (child.getCookie() instanceof UiViewElementNode) {
                        afterNode = (UiViewElementNode) child.getCookie();
                    }
                } else {
                    beforeNode = nextViewNode(children, index);

                    // Find first eligible node from unused
                    // TOD: What if there are more eligible? We need to process ALL views
                    // and all nodes in one go here

                    UiViewElementNode matching = null;
                    for (UiViewElementNode candidate : unused) {
                        if (afterNode == null || isAfter(afterNode, candidate)) {
                            if (beforeNode == null || isBefore(beforeNode, candidate)) {
                                matching = candidate;
                                break;
                            }
                        }
                    }

                    if (matching != null) {
                        unused.remove(matching);
                        CanvasViewInfo childView = createView(parentView, child, parentX, parentY,
                                matching);
                        parentView.addChild(childView);
                        afterNode = matching;
                    } else {
                        // We have no node for the view -- what do we do??
                        // Nothing - we only represent stuff in the outline that is in the
                        // source model, not in the render
                    }
                }
            }

            // Add zero-bounded boxes for all remaining nodes since they need to show
            // up in the outline, need to be selectable so you can press Delete, etc.
            if (unused.size() > 0) {
                Map<UiViewElementNode, Integer> rankMap =
                    new HashMap<UiViewElementNode, Integer>();
                Map<UiViewElementNode, CanvasViewInfo> infoMap =
                    new HashMap<UiViewElementNode, CanvasViewInfo>();
                UiElementNode parent = unused.get(0).getUiParent();
                if (parent != null) {
                    int index = 0;
                    for (UiElementNode child : parent.getUiChildren()) {
                        UiViewElementNode node = (UiViewElementNode) child;
                        rankMap.put(node, index++);
                    }
                    for (CanvasViewInfo child : parentView.getChildren()) {
                        infoMap.put(child.getUiViewNode(), child);
                    }
                    List<Integer> usedIndexes = new ArrayList<Integer>();
                    for (UiViewElementNode node : unused) {
                        Integer rank = rankMap.get(node);
                        if (rank != null) {
                            usedIndexes.add(rank);
                        }
                    }
                    Collections.sort(usedIndexes);
                    for (int i = usedIndexes.size() - 1; i >= 0; i--) {
                        Integer rank = usedIndexes.get(i);
                        UiViewElementNode found = null;
                        for (UiViewElementNode node : unused) {
                            if (rankMap.get(node) == rank) {
                                found = node;
                                break;
                            }
                        }
                        if (found != null) {
                            Rectangle absRect = new Rectangle(parentX, parentY, 0, 0);
                            String name = found.getDescriptor().getXmlLocalName();
                            CanvasViewInfo v = new CanvasViewInfo(parentView, name, null, found,
                                    absRect, absRect, null /* viewInfo */);
                            // Find corresponding index in the parent view
                            List<CanvasViewInfo> siblings = parentView.getChildren();
                            int insertPosition = siblings.size();
                            for (int j = siblings.size() - 1; j >= 0; j--) {
                                CanvasViewInfo sibling = siblings.get(j);
                                UiViewElementNode siblingNode = sibling.getUiViewNode();
                                if (siblingNode != null) {
                                    Integer siblingRank = rankMap.get(siblingNode);
                                    if (siblingRank != null && siblingRank < rank) {
                                        insertPosition = j + 1;
                                        break;
                                    }
                                }
                            }
                            parentView.addChildAt(insertPosition, v);
                            unused.remove(found);
                        }
                    }
                }
                // Add in any remaining
                for (UiViewElementNode node : unused) {
                    Rectangle absRect = new Rectangle(parentX, parentY, 0, 0);
                    String name = node.getDescriptor().getXmlLocalName();
                    CanvasViewInfo v = new CanvasViewInfo(parentView, name, null, node, absRect,
                            absRect, null /* viewInfo */);
                    parentView.addChild(v);
                }
            }
        }

        private boolean isBefore(UiViewElementNode beforeNode, UiViewElementNode candidate) {
            UiElementNode parent = candidate.getUiParent();
            if (parent != null) {
                for (UiElementNode sibling : parent.getUiChildren()) {
                    if (sibling == beforeNode) {
                        return false;
                    } else if (sibling == candidate) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean isAfter(UiViewElementNode afterNode, UiViewElementNode candidate) {
            UiElementNode parent = candidate.getUiParent();
            if (parent != null) {
                for (UiElementNode sibling : parent.getUiChildren()) {
                    if (sibling == afterNode) {
                        return true;
                    } else if (sibling == candidate) {
                        return false;
                    }
                }
            }
            return false;
        }

        private UiViewElementNode nextViewNode(List<ViewInfo> children, int index) {
            int size = children.size();
            for (; index < size; index++) {
                ViewInfo child = children.get(index);
                if (child.getCookie() instanceof UiViewElementNode) {
                    return (UiViewElementNode) child.getCookie();
                }
            }

            return null;
        }

        /** Search for a subtree with valid keys and add those subtrees */
        private CanvasViewInfo addKeyedSubtrees(CanvasViewInfo parent, ViewInfo viewInfo,
                int parentX, int parentY) {
            // We don't include MergeCookies when searching down for the first non-null key,
            // since this means we are in a "Show Included In" context, and the include tag itself
            // (which the merge cookie is pointing to) is still in the including-document rather
            // than the included document. Therefore, we only accept real UiViewElementNodes here,
            // not MergeCookies.
            if (viewInfo.getCookie() != null) {
                CanvasViewInfo subtree = createSubtree(parent, viewInfo, parentX, parentY);
                if (parent != null && subtree != null) {
                    parent.mChildren.add(subtree);
                }
                return subtree;
            } else {
                for (ViewInfo child : viewInfo.getChildren()) {
                    addKeyedSubtrees(parent, child, parentX + viewInfo.getLeft(), parentY
                            + viewInfo.getTop());
                }

                return null;
            }
        }
    }
}
