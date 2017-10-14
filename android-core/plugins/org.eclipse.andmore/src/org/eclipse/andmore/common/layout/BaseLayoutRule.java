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

package org.eclipse.andmore.common.layout;

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ATTR_LAYOUT_ABOVE;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_BASELINE;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_BOTTOM;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_LEFT;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_PARENT_BOTTOM;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_PARENT_LEFT;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_PARENT_RIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_PARENT_TOP;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_RIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_TOP;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_WITH_PARENT_MISSING;
import static com.android.SdkConstants.ATTR_LAYOUT_BELOW;
import static com.android.SdkConstants.ATTR_LAYOUT_CENTER_HORIZONTAL;
import static com.android.SdkConstants.ATTR_LAYOUT_CENTER_IN_PARENT;
import static com.android.SdkConstants.ATTR_LAYOUT_CENTER_VERTICAL;
import static com.android.SdkConstants.ATTR_LAYOUT_COLUMN;
import static com.android.SdkConstants.ATTR_LAYOUT_COLUMN_SPAN;
import static com.android.SdkConstants.ATTR_LAYOUT_GRAVITY;
import static com.android.SdkConstants.ATTR_LAYOUT_HEIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN;
import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN_BOTTOM;
import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN_LEFT;
import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN_RIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN_TOP;
import static com.android.SdkConstants.ATTR_LAYOUT_ROW;
import static com.android.SdkConstants.ATTR_LAYOUT_ROW_SPAN;
import static com.android.SdkConstants.ATTR_LAYOUT_TO_LEFT_OF;
import static com.android.SdkConstants.ATTR_LAYOUT_TO_RIGHT_OF;
import static com.android.SdkConstants.ATTR_LAYOUT_WIDTH;
import static com.android.SdkConstants.ATTR_LAYOUT_X;
import static com.android.SdkConstants.ATTR_LAYOUT_Y;
import static com.android.SdkConstants.VALUE_FILL_PARENT;
import static com.android.SdkConstants.VALUE_MATCH_PARENT;
import static com.android.SdkConstants.VALUE_WRAP_CONTENT;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.api.DrawingStyle;
import com.android.ide.common.api.DropFeedback;
import com.android.ide.common.api.IAttributeInfo;
import com.android.ide.common.api.IClientRulesEngine;
import com.android.ide.common.api.IDragElement;
import com.android.ide.common.api.IDragElement.IDragAttribute;
import com.android.ide.common.api.IFeedbackPainter;
import com.android.ide.common.api.IGraphics;
import com.android.ide.common.api.IMenuCallback;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.INodeHandler;
import com.android.ide.common.api.IViewRule;
import com.android.ide.common.api.MarginType;
import com.android.ide.common.api.Point;
import com.android.ide.common.api.Rect;
import com.android.ide.common.api.RuleAction;
import com.android.ide.common.api.RuleAction.ChoiceProvider;
import com.android.ide.common.api.Segment;
import com.android.ide.common.api.SegmentType;
import com.android.utils.Pair;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A {@link IViewRule} for all layouts.
 */
public class BaseLayoutRule extends BaseViewRule {
    private static final String ACTION_FILL_WIDTH = "_fillW";  //$NON-NLS-1$
    private static final String ACTION_FILL_HEIGHT = "_fillH"; //$NON-NLS-1$
    private static final String ACTION_MARGIN = "_margin";     //$NON-NLS-1$
    private static final URL ICON_MARGINS =
        BaseLayoutRule.class.getResource("margins.png"); //$NON-NLS-1$
    private static final URL ICON_GRAVITY =
        BaseLayoutRule.class.getResource("gravity.png"); //$NON-NLS-1$
    private static final URL ICON_FILL_WIDTH =
        BaseLayoutRule.class.getResource("fillwidth.png"); //$NON-NLS-1$
    private static final URL ICON_FILL_HEIGHT =
        BaseLayoutRule.class.getResource("fillheight.png"); //$NON-NLS-1$

    // ==== Layout Actions support ====

    // The Margin layout parameters are available for LinearLayout, FrameLayout, RelativeLayout,
    // and their subclasses.
    protected final RuleAction createMarginAction(final INode parentNode,
            final List<? extends INode> children) {

        final List<? extends INode> targets = children == null || children.size() == 0 ?
                Collections.singletonList(parentNode)
                : children;
        final INode first = targets.get(0);

        IMenuCallback actionCallback = new IMenuCallback() {
            @Override
            public void action(@NonNull RuleAction action,
                    @NonNull List<? extends INode> selectedNodes,
                    final @Nullable String valueId,
                    final @Nullable Boolean newValue) {
                parentNode.editXml("Change Margins", new INodeHandler() {
                    @Override
                    public void handle(@NonNull INode n) {
                        String uri = ANDROID_URI;
                        String all = first.getStringAttr(uri, ATTR_LAYOUT_MARGIN);
                        String left = first.getStringAttr(uri, ATTR_LAYOUT_MARGIN_LEFT);
                        String right = first.getStringAttr(uri, ATTR_LAYOUT_MARGIN_RIGHT);
                        String top = first.getStringAttr(uri, ATTR_LAYOUT_MARGIN_TOP);
                        String bottom = first.getStringAttr(uri, ATTR_LAYOUT_MARGIN_BOTTOM);
                        String[] margins = mRulesEngine.displayMarginInput(all, left,
                                right, top, bottom);
                        if (margins != null) {
                            assert margins.length == 5;
                            for (INode child : targets) {
                                child.setAttribute(uri, ATTR_LAYOUT_MARGIN, margins[0]);
                                child.setAttribute(uri, ATTR_LAYOUT_MARGIN_LEFT, margins[1]);
                                child.setAttribute(uri, ATTR_LAYOUT_MARGIN_RIGHT, margins[2]);
                                child.setAttribute(uri, ATTR_LAYOUT_MARGIN_TOP, margins[3]);
                                child.setAttribute(uri, ATTR_LAYOUT_MARGIN_BOTTOM, margins[4]);
                            }
                        }
                    }
                });
            }
        };

        return RuleAction.createAction(ACTION_MARGIN, "Change Margins...", actionCallback,
                ICON_MARGINS, 40, false);
    }

    // Both LinearLayout and RelativeLayout have a gravity (but RelativeLayout applies it
    // to the parent whereas for LinearLayout it's on the children)
    protected final RuleAction createGravityAction(final List<? extends INode> targets, final
            String attributeName) {
        if (targets != null && targets.size() > 0) {
            final INode first = targets.get(0);
            ChoiceProvider provider = new ChoiceProvider() {
                @Override
                public void addChoices(@NonNull List<String> titles, @NonNull List<URL> iconUrls,
                        @NonNull List<String> ids) {
                    IAttributeInfo info = first.getAttributeInfo(ANDROID_URI, attributeName);
                    if (info != null) {
                        // Generate list of possible gravity value constants
                        assert info.getFormats().contains(IAttributeInfo.Format.FLAG);
                        for (String name : info.getFlagValues()) {
                            titles.add(getAttributeDisplayName(name));
                            ids.add(name);
                        }
                    }
                }
            };

            return RuleAction.createChoices("_gravity", "Change Gravity", //$NON-NLS-1$
                    new PropertyCallback(targets, "Change Gravity", ANDROID_URI,
                            attributeName),
                    provider,
                    first.getStringAttr(ANDROID_URI, attributeName), ICON_GRAVITY,
                    43, false);
        }

        return null;
    }

    @Override
    public void addLayoutActions(
            @NonNull List<RuleAction> actions,
            final @NonNull INode parentNode,
            final @NonNull List<? extends INode> children) {
        super.addLayoutActions(actions, parentNode, children);

        final List<? extends INode> targets = children == null || children.size() == 0 ?
                Collections.singletonList(parentNode)
                : children;
        final INode first = targets.get(0);

        // Shared action callback
        IMenuCallback actionCallback = new IMenuCallback() {
            @Override
            public void action(
                    @NonNull RuleAction action,
                    @NonNull List<? extends INode> selectedNodes,
                    final @Nullable String valueId,
                    final @Nullable Boolean newValue) {
                final String actionId = action.getId();
                final String undoLabel;
                if (actionId.equals(ACTION_FILL_WIDTH)) {
                    undoLabel = "Change Width Fill";
                } else if (actionId.equals(ACTION_FILL_HEIGHT)) {
                    undoLabel = "Change Height Fill";
                } else {
                    return;
                }
                parentNode.editXml(undoLabel, new INodeHandler() {
                    @Override
                    public void handle(@NonNull INode n) {
                        String attribute = actionId.equals(ACTION_FILL_WIDTH)
                                ? ATTR_LAYOUT_WIDTH : ATTR_LAYOUT_HEIGHT;
                        String value;
                        if (newValue) {
                            if (supportsMatchParent()) {
                                value = VALUE_MATCH_PARENT;
                            } else {
                                value = VALUE_FILL_PARENT;
                            }
                        } else {
                            value = VALUE_WRAP_CONTENT;
                        }
                        for (INode child : targets) {
                            child.setAttribute(ANDROID_URI, attribute, value);
                        }
                    }
                });
            }
        };

        actions.add(RuleAction.createToggle(ACTION_FILL_WIDTH, "Toggle Fill Width",
                isFilled(first, ATTR_LAYOUT_WIDTH), actionCallback, ICON_FILL_WIDTH, 10, false));
        actions.add(RuleAction.createToggle(ACTION_FILL_HEIGHT, "Toggle Fill Height",
                isFilled(first, ATTR_LAYOUT_HEIGHT), actionCallback, ICON_FILL_HEIGHT, 20, false));
    }

    // ==== Paste support ====

    /**
     * The default behavior for pasting in a layout is to simulate a drop in the
     * top-left corner of the view.
     * <p/>
     * Note that we explicitly do not call super() here -- the BaseViewRule.onPaste handler
     * will call onPasteBeforeChild() instead.
     * <p/>
     * Derived layouts should override this behavior if not appropriate.
     */
    @Override
    public void onPaste(@NonNull INode targetNode, @Nullable Object targetView,
            @NonNull IDragElement[] elements) {
        DropFeedback feedback = onDropEnter(targetNode, targetView, elements);
        if (feedback != null) {
            Point p = targetNode.getBounds().getTopLeft();
            feedback = onDropMove(targetNode, elements, feedback, p);
            if (feedback != null) {
                onDropLeave(targetNode, elements, feedback);
                onDropped(targetNode, elements, feedback, p);
            }
        }
    }

    /**
     * The default behavior for pasting in a layout with a specific child target
     * is to simulate a drop right above the top left of the given child target.
     * <p/>
     * This method is invoked by BaseView when onPaste() is called --
     * views don't generally accept children and instead use the target node as
     * a hint to paste "before" it.
     *
     * @param parentNode the parent node we're pasting into
     * @param parentView the view object for the parent layout, or null
     * @param targetNode the first selected node
     * @param elements the elements being pasted
     */
    public void onPasteBeforeChild(INode parentNode, Object parentView, INode targetNode,
            IDragElement[] elements) {
        DropFeedback feedback = onDropEnter(parentNode, parentView, elements);
        if (feedback != null) {
            Point parentP = parentNode.getBounds().getTopLeft();
            Point targetP = targetNode.getBounds().getTopLeft();
            if (parentP.y < targetP.y) {
                targetP.y -= 1;
            }

            feedback = onDropMove(parentNode, elements, feedback, targetP);
            if (feedback != null) {
                onDropLeave(parentNode, elements, feedback);
                onDropped(parentNode, elements, feedback, targetP);
            }
        }
    }

    // ==== Utility methods used by derived layouts ====

    /**
     * Draws the bounds of the given elements and all its children elements in the canvas
     * with the specified offset.
     *
     * @param gc the graphics context
     * @param element the element to be drawn
     * @param offsetX a horizontal delta to add to the current bounds of the element when
     *            drawing it
     * @param offsetY a vertical delta to add to the current bounds of the element when
     *            drawing it
     */
    public void drawElement(IGraphics gc, IDragElement element, int offsetX, int offsetY) {
        Rect b = element.getBounds();
        if (b.isValid()) {
            gc.drawRect(b.x + offsetX, b.y + offsetY, b.x + offsetX + b.w, b.y + offsetY + b.h);
        }

        for (IDragElement inner : element.getInnerElements()) {
            drawElement(gc, inner, offsetX, offsetY);
        }
    }

    /**
     * Collect all the "android:id" IDs from the dropped elements. When moving
     * objects within the same canvas, that's all there is to do. However if the
     * objects are moved to a different canvas or are copied then set
     * createNewIds to true to find the existing IDs under targetNode and create
     * a map with new non-conflicting unique IDs as needed. Returns a map String
     * old-id => tuple (String new-id, String fqcn) where fqcn is the FQCN of
     * the element.
     */
    protected static Map<String, Pair<String, String>> getDropIdMap(INode targetNode,
            IDragElement[] elements, boolean createNewIds) {
        Map<String, Pair<String, String>> idMap = new LinkedHashMap<String, Pair<String, String>>();

        if (createNewIds) {
            collectIds(idMap, elements);
            // Need to remap ids if necessary
            idMap = remapIds(targetNode, idMap);
        }

        return idMap;
    }

    /**
     * Fills idMap with a map String id => tuple (String id, String fqcn) where
     * fqcn is the FQCN of the element (in case we want to generate new IDs
     * based on the element type.)
     *
     * @see #getDropIdMap
     */
    protected static Map<String, Pair<String, String>> collectIds(
            Map<String, Pair<String, String>> idMap,
            IDragElement[] elements) {
        for (IDragElement element : elements) {
            IDragAttribute attr = element.getAttribute(ANDROID_URI, ATTR_ID);
            if (attr != null) {
                String id = attr.getValue();
                if (id != null && id.length() > 0) {
                    idMap.put(id, Pair.of(id, element.getFqcn()));
                }
            }

            collectIds(idMap, element.getInnerElements());
        }

        return idMap;
    }

    /**
     * Used by #getDropIdMap to find new IDs in case of conflict.
     */
    protected static Map<String, Pair<String, String>> remapIds(INode node,
            Map<String, Pair<String, String>> idMap) {
        // Visit the document to get a list of existing ids
        Set<String> existingIdSet = new HashSet<String>();
        collectExistingIds(node.getRoot(), existingIdSet);

        Map<String, Pair<String, String>> new_map = new HashMap<String, Pair<String, String>>();
        for (Map.Entry<String, Pair<String, String>> entry : idMap.entrySet()) {
            String key = entry.getKey();
            Pair<String, String> value = entry.getValue();

            String id = normalizeId(key);

            if (!existingIdSet.contains(id)) {
                // Not a conflict. Use as-is.
                new_map.put(key, value);
                if (!key.equals(id)) {
                    new_map.put(id, value);
                }
            } else {
                // There is a conflict. Get a new id.
                String new_id = findNewId(value.getSecond(), existingIdSet);
                value = Pair.of(new_id, value.getSecond());
                new_map.put(id, value);
                new_map.put(id.replaceFirst("@\\+", "@"), value); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        return new_map;
    }

    /**
     * Used by #remapIds to find a new ID for a conflicting element.
     */
    protected static String findNewId(String fqcn, Set<String> existingIdSet) {
        // Get the last component of the FQCN (e.g. "android.view.Button" =>
        // "Button")
        String name = fqcn.substring(fqcn.lastIndexOf('.') + 1);

        for (int i = 1; i < 1000000; i++) {
            String id = String.format("@+id/%s%02d", name, i); //$NON-NLS-1$
            if (!existingIdSet.contains(id)) {
                existingIdSet.add(id);
                return id;
            }
        }

        // We'll never reach here.
        return null;
    }

    /**
     * Used by #getDropIdMap to find existing IDs recursively.
     */
    protected static void collectExistingIds(INode root, Set<String> existingIdSet) {
        if (root == null) {
            return;
        }

        String id = root.getStringAttr(ANDROID_URI, ATTR_ID);
        if (id != null) {
            id = normalizeId(id);

            if (!existingIdSet.contains(id)) {
                existingIdSet.add(id);
            }
        }

        for (INode child : root.getChildren()) {
            collectExistingIds(child, existingIdSet);
        }
    }

    /**
     * Transforms @id/name into @+id/name to treat both forms the same way.
     */
    protected static String normalizeId(String id) {
        if (id.indexOf("@+") == -1) { //$NON-NLS-1$
            id = id.replaceFirst("@", "@+"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return id;
    }

    /**
     * For use by {@link BaseLayoutRule#addAttributes} A filter should return a
     * valid replacement string.
     */
    protected static interface AttributeFilter {
        String replace(String attributeUri, String attributeName, String attributeValue);
    }

    private static final String[] EXCLUDED_ATTRIBUTES = new String[] {
        // Common
        ATTR_LAYOUT_GRAVITY,

        // from AbsoluteLayout
        ATTR_LAYOUT_X,
        ATTR_LAYOUT_Y,

        // from RelativeLayout
        ATTR_LAYOUT_ABOVE,
        ATTR_LAYOUT_BELOW,
        ATTR_LAYOUT_TO_LEFT_OF,
        ATTR_LAYOUT_TO_RIGHT_OF,
        ATTR_LAYOUT_ALIGN_BASELINE,
        ATTR_LAYOUT_ALIGN_TOP,
        ATTR_LAYOUT_ALIGN_BOTTOM,
        ATTR_LAYOUT_ALIGN_LEFT,
        ATTR_LAYOUT_ALIGN_RIGHT,
        ATTR_LAYOUT_ALIGN_PARENT_TOP,
        ATTR_LAYOUT_ALIGN_PARENT_BOTTOM,
        ATTR_LAYOUT_ALIGN_PARENT_LEFT,
        ATTR_LAYOUT_ALIGN_PARENT_RIGHT,
        ATTR_LAYOUT_ALIGN_WITH_PARENT_MISSING,
        ATTR_LAYOUT_CENTER_HORIZONTAL,
        ATTR_LAYOUT_CENTER_IN_PARENT,
        ATTR_LAYOUT_CENTER_VERTICAL,

        // From GridLayout
        ATTR_LAYOUT_ROW,
        ATTR_LAYOUT_ROW_SPAN,
        ATTR_LAYOUT_COLUMN,
        ATTR_LAYOUT_COLUMN_SPAN
    };

    /**
     * Default attribute filter used by the various layouts to filter out some properties
     * we don't want to offer.
     */
    public static final AttributeFilter DEFAULT_ATTR_FILTER = new AttributeFilter() {
        Set<String> mExcludes;

        @Override
        public String replace(String uri, String name, String value) {
            if (!ANDROID_URI.equals(uri)) {
                return value;
            }

            if (mExcludes == null) {
                mExcludes = new HashSet<String>(EXCLUDED_ATTRIBUTES.length);
                mExcludes.addAll(Arrays.asList(EXCLUDED_ATTRIBUTES));
            }

            return mExcludes.contains(name) ? null : value;
        }
    };

    /**
     * Copies all the attributes from oldElement to newNode. Uses the idMap to
     * transform the value of all attributes of Format.REFERENCE. If filter is
     * non-null, it's a filter that can rewrite the attribute string.
     */
    protected static void addAttributes(INode newNode, IDragElement oldElement,
            Map<String, Pair<String, String>> idMap, AttributeFilter filter) {

        for (IDragAttribute attr : oldElement.getAttributes()) {
            String uri = attr.getUri();
            String name = attr.getName();
            String value = attr.getValue();

            IAttributeInfo attrInfo = newNode.getAttributeInfo(uri, name);
            if (attrInfo != null) {
                if (attrInfo.getFormats().contains(IAttributeInfo.Format.REFERENCE)) {
                    if (idMap.containsKey(value)) {
                        value = idMap.get(value).getFirst();
                    }
                }
            }

            if (filter != null) {
                value = filter.replace(uri, name, value);
            }
            if (value != null && value.length() > 0) {
                newNode.setAttribute(uri, name, value);
            }
        }
    }

    /**
     * Adds all the children elements of oldElement to newNode, recursively.
     * Attributes are adjusted by calling addAttributes with idMap as necessary,
     * with no closure filter.
     */
    protected static void addInnerElements(INode newNode, IDragElement oldElement,
            Map<String, Pair<String, String>> idMap) {

        for (IDragElement element : oldElement.getInnerElements()) {
            String fqcn = element.getFqcn();
            INode childNode = newNode.appendChild(fqcn);

            addAttributes(childNode, element, idMap, null /* filter */);
            addInnerElements(childNode, element, idMap);
        }
    }

    /**
     * Insert the given elements into the given node at the given position
     *
     * @param targetNode the node to insert into
     * @param elements the elements to insert
     * @param createNewIds if true, generate new ids when there is a conflict
     * @param initialInsertPos index among targetnode's children which to insert the
     *            children
     */
    public static void insertAt(final INode targetNode, final IDragElement[] elements,
            final boolean createNewIds, final int initialInsertPos) {

        // Collect IDs from dropped elements and remap them to new IDs
        // if this is a copy or from a different canvas.
        final Map<String, Pair<String, String>> idMap = getDropIdMap(targetNode, elements,
                createNewIds);

        targetNode.editXml("Insert Elements", new INodeHandler() {

            @Override
            public void handle(@NonNull INode node) {
                // Now write the new elements.
                int insertPos = initialInsertPos;
                for (IDragElement element : elements) {
                    String fqcn = element.getFqcn();

                    INode newChild = targetNode.insertChildAt(fqcn, insertPos);

                    // insertPos==-1 means to insert at the end. Otherwise
                    // increment the insertion position.
                    if (insertPos >= 0) {
                        insertPos++;
                    }

                    // Copy all the attributes, modifying them as needed.
                    addAttributes(newChild, element, idMap, DEFAULT_ATTR_FILTER);
                    addInnerElements(newChild, element, idMap);
                }
            }
        });
    }

    // ---- Resizing ----

    /** Creates a new {@link ResizeState} object to track resize state */
    protected ResizeState createResizeState(INode layout, Object layoutView, INode node) {
        return new ResizeState(this, layout, layoutView, node);
    }

    @Override
    public DropFeedback onResizeBegin(@NonNull INode child, @NonNull INode parent,
            @Nullable SegmentType horizontalEdge, @Nullable SegmentType verticalEdge,
            @Nullable Object childView, @Nullable Object parentView) {
        ResizeState state = createResizeState(parent, parentView, child);
        state.horizontalEdgeType = horizontalEdge;
        state.verticalEdgeType = verticalEdge;

        // Compute preferred (wrap_content) size such that we can offer guidelines to
        // snap to the preferred size
        Map<INode, Rect> sizes = mRulesEngine.measureChildren(parent,
                new IClientRulesEngine.AttributeFilter() {
                    @Override
                    public String getAttribute(@NonNull INode node, @Nullable String namespace,
                            @NonNull String localName) {
                        // Change attributes to wrap_content
                        if (ATTR_LAYOUT_WIDTH.equals(localName)
                                && SdkConstants.NS_RESOURCES.equals(namespace)) {
                            return VALUE_WRAP_CONTENT;
                        }
                        if (ATTR_LAYOUT_HEIGHT.equals(localName)
                                && SdkConstants.NS_RESOURCES.equals(namespace)) {
                            return VALUE_WRAP_CONTENT;
                        }

                        return null;
                    }
                });
        if (sizes != null) {
            state.wrapBounds = sizes.get(child);
        }

        return new DropFeedback(state, new IFeedbackPainter() {
            @Override
            public void paint(@NonNull IGraphics gc, @NonNull INode node,
                    @NonNull DropFeedback feedback) {
                ResizeState resizeState = (ResizeState) feedback.userData;
                if (resizeState != null && resizeState.bounds != null) {
                    paintResizeFeedback(gc, node, resizeState);
                }
            }
        });
    }

    protected void paintResizeFeedback(IGraphics gc, INode node, ResizeState resizeState) {
        gc.useStyle(DrawingStyle.RESIZE_PREVIEW);
        Rect b = resizeState.bounds;
        gc.drawRect(b);

        if (resizeState.horizontalFillSegment != null) {
            gc.useStyle(DrawingStyle.GUIDELINE);
            Segment s = resizeState.horizontalFillSegment;
            gc.drawLine(s.from, s.at, s.to, s.at);
        }
        if (resizeState.verticalFillSegment != null) {
            gc.useStyle(DrawingStyle.GUIDELINE);
            Segment s = resizeState.verticalFillSegment;
            gc.drawLine(s.at, s.from, s.at, s.to);
        }

        if (resizeState.wrapBounds != null) {
            gc.useStyle(DrawingStyle.GUIDELINE);
            int wrapWidth = resizeState.wrapBounds.w;
            int wrapHeight = resizeState.wrapBounds.h;

            // Show the "wrap_content" guideline.
            // If we are showing both the wrap_width and wrap_height lines
            // then we show at most the rectangle formed by the two lines;
            // otherwise we show the entire width of the line
            if (resizeState.horizontalEdgeType != null) {
                int y = -1;
                switch (resizeState.horizontalEdgeType) {
                    case TOP:
                        y = b.y + b.h - wrapHeight;
                        break;
                    case BOTTOM:
                        y = b.y + wrapHeight;
                        break;
                    default: assert false : resizeState.horizontalEdgeType;
                }
                if (resizeState.verticalEdgeType != null) {
                    switch (resizeState.verticalEdgeType) {
                        case LEFT:
                            gc.drawLine(b.x + b.w - wrapWidth, y, b.x + b.w, y);
                            break;
                        case RIGHT:
                            gc.drawLine(b.x, y, b.x + wrapWidth, y);
                            break;
                        default: assert false : resizeState.verticalEdgeType;
                    }
                } else {
                    gc.drawLine(b.x, y, b.x + b.w, y);
                }
            }
            if (resizeState.verticalEdgeType != null) {
                int x = -1;
                switch (resizeState.verticalEdgeType) {
                    case LEFT:
                        x = b.x + b.w - wrapWidth;
                        break;
                    case RIGHT:
                        x = b.x + wrapWidth;
                        break;
                    default: assert false : resizeState.verticalEdgeType;
                }
                if (resizeState.horizontalEdgeType != null) {
                    switch (resizeState.horizontalEdgeType) {
                        case TOP:
                            gc.drawLine(x, b.y + b.h - wrapHeight, x, b.y + b.h);
                            break;
                        case BOTTOM:
                            gc.drawLine(x, b.y, x, b.y + wrapHeight);
                            break;
                        default: assert false : resizeState.horizontalEdgeType;
                    }
                } else {
                    gc.drawLine(x, b.y, x, b.y + b.h);
                }
            }
        }
    }

    /**
     * Returns the maximum number of pixels will be considered a "match" when snapping
     * resize or move positions to edges or other constraints
     *
     * @return the maximum number of pixels to consider for snapping
     */
    public static final int getMaxMatchDistance() {
        // TODO - make constant once we're happy with the feel
        return 20;
    }

    @Override
    public void onResizeUpdate(@Nullable DropFeedback feedback, @NonNull INode child,
            @NonNull INode parent, @NonNull Rect newBounds, int modifierMask) {
        ResizeState state = (ResizeState) feedback.userData;
        state.bounds = newBounds;
        state.modifierMask = modifierMask;

        // Match on wrap bounds
        state.wrapWidth = state.wrapHeight = false;
        if (state.wrapBounds != null) {
            Rect b = state.wrapBounds;
            int maxMatchDistance = getMaxMatchDistance();
            if (state.horizontalEdgeType != null) {
                if (Math.abs(newBounds.h - b.h) < maxMatchDistance) {
                    state.wrapHeight = true;
                    if (state.horizontalEdgeType == SegmentType.TOP) {
                        newBounds.y += newBounds.h - b.h;
                    }
                    newBounds.h = b.h;
                }
            }
            if (state.verticalEdgeType != null) {
                if (Math.abs(newBounds.w - b.w) < maxMatchDistance) {
                    state.wrapWidth = true;
                    if (state.verticalEdgeType == SegmentType.LEFT) {
                        newBounds.x += newBounds.w - b.w;
                    }
                    newBounds.w = b.w;
                }
            }
        }

        // Match on fill bounds
        state.horizontalFillSegment = null;
        state.fillHeight = false;
        if (state.horizontalEdgeType == SegmentType.BOTTOM && !state.wrapHeight) {
            Rect parentBounds = parent.getBounds();
            state.horizontalFillSegment = new Segment(parentBounds.y2(), newBounds.x,
                newBounds.x2(),
                null /*node*/, null /*id*/, SegmentType.BOTTOM, MarginType.NO_MARGIN);
            if (Math.abs(newBounds.y2() - parentBounds.y2()) < getMaxMatchDistance()) {
                state.fillHeight = true;
                newBounds.h = parentBounds.y2() - newBounds.y;
            }
        }
        state.verticalFillSegment = null;
        state.fillWidth = false;
        if (state.verticalEdgeType == SegmentType.RIGHT && !state.wrapWidth) {
            Rect parentBounds = parent.getBounds();
            state.verticalFillSegment = new Segment(parentBounds.x2(), newBounds.y,
                newBounds.y2(),
                null /*node*/, null /*id*/, SegmentType.RIGHT, MarginType.NO_MARGIN);
            if (Math.abs(newBounds.x2() - parentBounds.x2()) < getMaxMatchDistance()) {
                state.fillWidth = true;
                newBounds.w = parentBounds.x2() - newBounds.x;
            }
        }

        feedback.tooltip = getResizeUpdateMessage(state, child, parent,
                newBounds, state.horizontalEdgeType, state.verticalEdgeType);
    }

    @Override
    public void onResizeEnd(@Nullable DropFeedback feedback, @NonNull INode child,
            final @NonNull INode parent, final @NonNull Rect newBounds) {
        final Rect oldBounds = child.getBounds();
        if (oldBounds.w != newBounds.w || oldBounds.h != newBounds.h) {
            final ResizeState state = (ResizeState) feedback.userData;
            child.editXml("Resize", new INodeHandler() {
                @Override
                public void handle(@NonNull INode n) {
                    setNewSizeBounds(state, n, parent, oldBounds, newBounds,
                            state.horizontalEdgeType, state.verticalEdgeType);
                }
            });
        }
    }

    /**
     * Returns the message to display to the user during the resize operation
     *
     * @param resizeState the current resize state
     * @param child the child node being resized
     * @param parent the parent of the resized node
     * @param newBounds the new bounds to resize the child to, in pixels
     * @param horizontalEdge the horizontal edge being resized
     * @param verticalEdge the vertical edge being resized
     * @return the message to display for the current resize bounds
     */
    protected String getResizeUpdateMessage(ResizeState resizeState, INode child, INode parent,
            Rect newBounds, SegmentType horizontalEdge, SegmentType verticalEdge) {
        String width = resizeState.getWidthAttribute();
        String height = resizeState.getHeightAttribute();

        if (horizontalEdge == null) {
            return width;
        } else if (verticalEdge == null) {
            return height;
        } else {
            // U+00D7: Unicode for multiplication sign
            return String.format("%s \u00D7 %s", width, height);
        }
    }

    /**
     * Performs the edit on the node to complete a resizing operation. The actual edit
     * part is pulled out such that subclasses can change/add to the edits and be part of
     * the same undo event
     *
     * @param resizeState the current resize state
     * @param node the child node being resized
     * @param layout the parent of the resized node
     * @param newBounds the new bounds to resize the child to, in pixels
     * @param horizontalEdge the horizontal edge being resized
     * @param verticalEdge the vertical edge being resized
     */
    protected void setNewSizeBounds(ResizeState resizeState, INode node, INode layout,
            Rect oldBounds, Rect newBounds, SegmentType horizontalEdge, SegmentType verticalEdge) {
        if (verticalEdge != null
            && (newBounds.w != oldBounds.w || resizeState.wrapWidth || resizeState.fillWidth)) {
            node.setAttribute(ANDROID_URI, ATTR_LAYOUT_WIDTH, resizeState.getWidthAttribute());
        }
        if (horizontalEdge != null
            && (newBounds.h != oldBounds.h || resizeState.wrapHeight || resizeState.fillHeight)) {
            node.setAttribute(ANDROID_URI, ATTR_LAYOUT_HEIGHT, resizeState.getHeightAttribute());
        }
    }
}
