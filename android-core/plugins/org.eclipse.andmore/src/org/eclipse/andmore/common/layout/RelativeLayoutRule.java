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
import static com.android.SdkConstants.ATTR_GRAVITY;
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
import static com.android.SdkConstants.ATTR_LAYOUT_RESOURCE_PREFIX;
import static com.android.SdkConstants.ATTR_LAYOUT_TO_LEFT_OF;
import static com.android.SdkConstants.ATTR_LAYOUT_TO_RIGHT_OF;
import static com.android.SdkConstants.VALUE_TRUE;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.api.DropFeedback;
import com.android.ide.common.api.IDragElement;
import com.android.ide.common.api.IGraphics;
import com.android.ide.common.api.IMenuCallback;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.INodeHandler;
import com.android.ide.common.api.IViewRule;
import com.android.ide.common.api.InsertType;
import com.android.ide.common.api.Point;
import com.android.ide.common.api.Rect;
import com.android.ide.common.api.RuleAction;
import com.android.ide.common.api.SegmentType;
import com.android.utils.Pair;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.andmore.common.layout.relative.ConstraintPainter;
import org.eclipse.andmore.common.layout.relative.DeletionHandler;
import org.eclipse.andmore.common.layout.relative.GuidelinePainter;
import org.eclipse.andmore.common.layout.relative.MoveHandler;
import org.eclipse.andmore.common.layout.relative.ResizeHandler;

/**
 * An {@link IViewRule} for android.widget.RelativeLayout and all its derived
 * classes.
 */
public class RelativeLayoutRule extends BaseLayoutRule {
    private static final String ACTION_SHOW_STRUCTURE = "_structure"; //$NON-NLS-1$
    private static final String ACTION_SHOW_CONSTRAINTS = "_constraints"; //$NON-NLS-1$
    private static final String ACTION_CENTER_VERTICAL = "_centerVert"; //$NON-NLS-1$
    private static final String ACTION_CENTER_HORIZONTAL = "_centerHoriz"; //$NON-NLS-1$
    private static final URL ICON_CENTER_VERTICALLY =
        RelativeLayoutRule.class.getResource("centerVertically.png"); //$NON-NLS-1$
    private static final URL ICON_CENTER_HORIZONTALLY =
        RelativeLayoutRule.class.getResource("centerHorizontally.png"); //$NON-NLS-1$
    private static final URL ICON_SHOW_STRUCTURE =
        BaseLayoutRule.class.getResource("structure.png"); //$NON-NLS-1$
    private static final URL ICON_SHOW_CONSTRAINTS =
        BaseLayoutRule.class.getResource("constraints.png"); //$NON-NLS-1$

    public static boolean sShowStructure = false;
    public static boolean sShowConstraints = true;

    // ==== Selection ====

    @Override
    public List<String> getSelectionHint(@NonNull INode parentNode, @NonNull INode childNode) {
        List<String> infos = new ArrayList<String>(18);
        addAttr(ATTR_LAYOUT_ABOVE, childNode, infos);
        addAttr(ATTR_LAYOUT_BELOW, childNode, infos);
        addAttr(ATTR_LAYOUT_TO_LEFT_OF, childNode, infos);
        addAttr(ATTR_LAYOUT_TO_RIGHT_OF, childNode, infos);
        addAttr(ATTR_LAYOUT_ALIGN_BASELINE, childNode, infos);
        addAttr(ATTR_LAYOUT_ALIGN_TOP, childNode, infos);
        addAttr(ATTR_LAYOUT_ALIGN_BOTTOM, childNode, infos);
        addAttr(ATTR_LAYOUT_ALIGN_LEFT, childNode, infos);
        addAttr(ATTR_LAYOUT_ALIGN_RIGHT, childNode, infos);
        addAttr(ATTR_LAYOUT_ALIGN_PARENT_TOP, childNode, infos);
        addAttr(ATTR_LAYOUT_ALIGN_PARENT_BOTTOM, childNode, infos);
        addAttr(ATTR_LAYOUT_ALIGN_PARENT_LEFT, childNode, infos);
        addAttr(ATTR_LAYOUT_ALIGN_PARENT_RIGHT, childNode, infos);
        addAttr(ATTR_LAYOUT_ALIGN_WITH_PARENT_MISSING, childNode, infos);
        addAttr(ATTR_LAYOUT_CENTER_HORIZONTAL, childNode, infos);
        addAttr(ATTR_LAYOUT_CENTER_IN_PARENT, childNode, infos);
        addAttr(ATTR_LAYOUT_CENTER_VERTICAL, childNode, infos);

        return infos;
    }

    private void addAttr(String propertyName, INode childNode, List<String> infos) {
        String a = childNode.getStringAttr(ANDROID_URI, propertyName);
        if (a != null && a.length() > 0) {
            // Display the layout parameters without the leading layout_ prefix
            // and id references without the @+id/ prefix
            if (propertyName.startsWith(ATTR_LAYOUT_RESOURCE_PREFIX)) {
                propertyName = propertyName.substring(ATTR_LAYOUT_RESOURCE_PREFIX.length());
            }
            a = stripIdPrefix(a);
            String s = propertyName + ": " + a;
            infos.add(s);
        }
    }

    @Override
    public void paintSelectionFeedback(@NonNull IGraphics graphics, @NonNull INode parentNode,
            @NonNull List<? extends INode> childNodes, @Nullable Object view) {
        super.paintSelectionFeedback(graphics, parentNode, childNodes, view);

        boolean showDependents = true;
        if (sShowStructure) {
            childNodes = Arrays.asList(parentNode.getChildren());
            // Avoid painting twice - both as incoming and outgoing
            showDependents = false;
        } else if (!sShowConstraints) {
            return;
        }

        ConstraintPainter.paintSelectionFeedback(graphics, parentNode, childNodes, showDependents);
    }

    // ==== Drag'n'drop support ====

    @Override
    public DropFeedback onDropEnter(@NonNull INode targetNode, @Nullable Object targetView,
            @Nullable IDragElement[] elements) {
        return new DropFeedback(new MoveHandler(targetNode, elements, mRulesEngine),
                new GuidelinePainter());
    }

    @Override
    public DropFeedback onDropMove(@NonNull INode targetNode, @NonNull IDragElement[] elements,
            @Nullable DropFeedback feedback, @NonNull Point p) {
        if (elements == null || elements.length == 0 || feedback == null) {
            return null;
        }

        MoveHandler state = (MoveHandler) feedback.userData;
        int offsetX = p.x + (feedback.dragBounds != null ? feedback.dragBounds.x : 0);
        int offsetY = p.y + (feedback.dragBounds != null ? feedback.dragBounds.y : 0);
        state.updateMove(feedback, elements, offsetX, offsetY, feedback.modifierMask);

        // Or maybe only do this if the results changed...
        feedback.requestPaint = true;

        return feedback;
    }

    @Override
    public void onDropLeave(@NonNull INode targetNode, @NonNull IDragElement[] elements,
            @Nullable DropFeedback feedback) {
    }

    @Override
    public void onDropped(final @NonNull INode targetNode, final @NonNull IDragElement[] elements,
            final @Nullable DropFeedback feedback, final @NonNull Point p) {
        if (feedback == null) {
            return;
        }

        final MoveHandler state = (MoveHandler) feedback.userData;

        final Map<String, Pair<String, String>> idMap = getDropIdMap(targetNode, elements,
                feedback.isCopy || !feedback.sameCanvas);

        targetNode.editXml("Dropped", new INodeHandler() {
            @Override
            public void handle(@NonNull INode n) {
                int index = -1;

                // Remove cycles
                state.removeCycles();

                // Now write the new elements.
                INode previous = null;
                for (IDragElement element : elements) {
                    String fqcn = element.getFqcn();

                    // index==-1 means to insert at the end.
                    // Otherwise increment the insertion position.
                    if (index >= 0) {
                        index++;
                    }

                    INode newChild = targetNode.insertChildAt(fqcn, index);

                    // Copy all the attributes, modifying them as needed.
                    addAttributes(newChild, element, idMap, BaseLayoutRule.DEFAULT_ATTR_FILTER);
                    addInnerElements(newChild, element, idMap);

                    if (previous == null) {
                        state.applyConstraints(newChild);
                        previous = newChild;
                    } else {
                        // Arrange the nodes next to each other, depending on which
                        // edge we are attaching to. For example, if attaching to the
                        // top edge, arrange the subsequent nodes in a column below it.
                        //
                        // TODO: Try to do something smarter here where we detect
                        // constraints between the dragged edges, and we preserve these.
                        // We have to do this carefully though because if the
                        // constraints go through some other nodes not part of the
                        // selection, this doesn't work right, and you might be
                        // dragging several connected components, which we'd then
                        // need to stitch together such that they are all visible.

                        state.attachPrevious(previous, newChild);
                        previous = newChild;
                    }
                }
            }
        });
    }

    @Override
    public void onChildInserted(@NonNull INode node, @NonNull INode parent,
            @NonNull InsertType insertType) {
        // TODO: Handle more generically some way to ensure that widgets with no
        // intrinsic size get some minimum size until they are attached on multiple
        // opposing sides.
        //String fqcn = node.getFqcn();
        //if (fqcn.equals(FQCN_EDIT_TEXT)) {
        //    node.setAttribute(ANDROID_URI, ATTR_LAYOUT_WIDTH, "100dp"); //$NON-NLS-1$
        //}
    }

    @Override
    public void onRemovingChildren(@NonNull List<INode> deleted, @NonNull INode parent,
            boolean moved) {
        super.onRemovingChildren(deleted, parent, moved);

        if (!moved) {
            DeletionHandler handler = new DeletionHandler(deleted, Collections.<INode>emptyList(),
                    parent);
            handler.updateConstraints();
        }
    }

    // ==== Resize Support ====

    @Override
    public DropFeedback onResizeBegin(@NonNull INode child, @NonNull INode parent,
            @Nullable SegmentType horizontalEdgeType, @Nullable SegmentType verticalEdgeType,
            @Nullable Object childView, @Nullable Object parentView) {
        ResizeHandler state = new ResizeHandler(parent, child, mRulesEngine,
                horizontalEdgeType, verticalEdgeType);
        return new DropFeedback(state, new GuidelinePainter());
    }

    @Override
    public void onResizeUpdate(@Nullable DropFeedback feedback, @NonNull INode child,
            @NonNull INode parent, @NonNull Rect newBounds,
            int modifierMask) {
        if (feedback == null) {
            return;
        }

        ResizeHandler state = (ResizeHandler) feedback.userData;
        state.updateResize(feedback, child, newBounds, modifierMask);
    }

    @Override
    public void onResizeEnd(@Nullable DropFeedback feedback, @NonNull INode child,
            @NonNull INode parent, final @NonNull Rect newBounds) {
        if (feedback == null) {
            return;
        }
        final ResizeHandler state = (ResizeHandler) feedback.userData;

        child.editXml("Resize", new INodeHandler() {
            @Override
            public void handle(@NonNull INode n) {
                state.removeCycles();
                state.applyConstraints(n);
            }
        });
    }

    // ==== Layout Actions Bar ====

    @Override
    public void addLayoutActions(
            @NonNull List<RuleAction> actions,
            final @NonNull INode parentNode,
            final @NonNull List<? extends INode> children) {
        super.addLayoutActions(actions, parentNode, children);

        actions.add(createGravityAction(Collections.<INode>singletonList(parentNode),
                ATTR_GRAVITY));
        actions.add(RuleAction.createSeparator(25));
        actions.add(createMarginAction(parentNode, children));

        IMenuCallback callback = new IMenuCallback() {
            @Override
            public void action(@NonNull RuleAction action,
                    @NonNull List<? extends INode> selectedNodes,
                    final @Nullable String valueId,
                    final @Nullable Boolean newValue) {
                final String id = action.getId();
                if (id.equals(ACTION_CENTER_VERTICAL)|| id.equals(ACTION_CENTER_HORIZONTAL)) {
                    parentNode.editXml("Center", new INodeHandler() {
                        @Override
                        public void handle(@NonNull INode n) {
                            if (id.equals(ACTION_CENTER_VERTICAL)) {
                                for (INode child : children) {
                                    centerVertically(child);
                                }
                            } else if (id.equals(ACTION_CENTER_HORIZONTAL)) {
                                for (INode child : children) {
                                    centerHorizontally(child);
                                }
                            }
                            mRulesEngine.redraw();
                        }

                    });
                } else if (id.equals(ACTION_SHOW_CONSTRAINTS)) {
                    sShowConstraints = !sShowConstraints;
                    mRulesEngine.redraw();
                } else {
                    assert id.equals(ACTION_SHOW_STRUCTURE);
                    sShowStructure = !sShowStructure;
                    mRulesEngine.redraw();
                }
            }
        };

        // Centering actions
        if (children != null && children.size() > 0) {
                        actions.add(RuleAction.createSeparator(150));
            actions.add(RuleAction.createAction(ACTION_CENTER_VERTICAL, "Center Vertically",
                    callback, ICON_CENTER_VERTICALLY, 160, false));
            actions.add(RuleAction.createAction(ACTION_CENTER_HORIZONTAL, "Center Horizontally",
                    callback, ICON_CENTER_HORIZONTALLY, 170, false));
        }

        actions.add(RuleAction.createSeparator(80));
        actions.add(RuleAction.createToggle(ACTION_SHOW_CONSTRAINTS, "Show Constraints",
                sShowConstraints, callback, ICON_SHOW_CONSTRAINTS, 180, false));
        actions.add(RuleAction.createToggle(ACTION_SHOW_STRUCTURE, "Show All Relationships",
                sShowStructure, callback, ICON_SHOW_STRUCTURE, 190, false));
    }

    private void centerHorizontally(INode node) {
        // Clear horizontal-oriented attributes from the node
        node.setAttribute(ANDROID_URI, ATTR_LAYOUT_ALIGN_PARENT_LEFT, null);
        node.setAttribute(ANDROID_URI, ATTR_LAYOUT_ALIGN_LEFT, null);
        node.setAttribute(ANDROID_URI, ATTR_LAYOUT_TO_RIGHT_OF, null);
        node.setAttribute(ANDROID_URI, ATTR_LAYOUT_CENTER_HORIZONTAL, null);
        node.setAttribute(ANDROID_URI, ATTR_LAYOUT_ALIGN_PARENT_RIGHT, null);
        node.setAttribute(ANDROID_URI, ATTR_LAYOUT_ALIGN_RIGHT, null);
        node.setAttribute(ANDROID_URI, ATTR_LAYOUT_TO_LEFT_OF, null);
        node.setAttribute(ANDROID_URI, ATTR_LAYOUT_CENTER_HORIZONTAL, null);

        if (VALUE_TRUE.equals(node.getStringAttr(ANDROID_URI, ATTR_LAYOUT_CENTER_IN_PARENT))) {
            // Already done
        } else if (VALUE_TRUE.equals(node.getStringAttr(ANDROID_URI,
                ATTR_LAYOUT_CENTER_VERTICAL))) {
            node.setAttribute(ANDROID_URI, ATTR_LAYOUT_CENTER_VERTICAL, null);
            node.setAttribute(ANDROID_URI, ATTR_LAYOUT_CENTER_IN_PARENT, VALUE_TRUE);
        } else {
            node.setAttribute(ANDROID_URI, ATTR_LAYOUT_CENTER_HORIZONTAL, VALUE_TRUE);
        }
    }

    private void centerVertically(INode node) {
        // Clear vertical-oriented attributes from the node
        node.setAttribute(ANDROID_URI, ATTR_LAYOUT_ALIGN_PARENT_TOP, null);
        node.setAttribute(ANDROID_URI, ATTR_LAYOUT_ALIGN_TOP, null);
        node.setAttribute(ANDROID_URI, ATTR_LAYOUT_BELOW, null);
        node.setAttribute(ANDROID_URI, ATTR_LAYOUT_ALIGN_PARENT_BOTTOM, null);
        node.setAttribute(ANDROID_URI, ATTR_LAYOUT_ALIGN_BOTTOM, null);
        node.setAttribute(ANDROID_URI, ATTR_LAYOUT_ABOVE, null);
        node.setAttribute(ANDROID_URI, ATTR_LAYOUT_CENTER_VERTICAL, null);
        node.setAttribute(ANDROID_URI, ATTR_LAYOUT_ALIGN_BASELINE, null);

        // Center vertically
        if (VALUE_TRUE.equals(node.getStringAttr(ANDROID_URI, ATTR_LAYOUT_CENTER_IN_PARENT))) {
            // ALready done
        } else if (VALUE_TRUE.equals(node.getStringAttr(ANDROID_URI,
                ATTR_LAYOUT_CENTER_HORIZONTAL))) {
            node.setAttribute(ANDROID_URI, ATTR_LAYOUT_CENTER_HORIZONTAL, null);
            node.setAttribute(ANDROID_URI, ATTR_LAYOUT_CENTER_IN_PARENT, VALUE_TRUE);
        } else {
            node.setAttribute(ANDROID_URI, ATTR_LAYOUT_CENTER_VERTICAL, VALUE_TRUE);
        }
    }
}
