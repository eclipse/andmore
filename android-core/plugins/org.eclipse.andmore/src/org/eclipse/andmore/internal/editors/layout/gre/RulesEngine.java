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

import static com.android.SdkConstants.ANDROID_WIDGET_PREFIX;
import static com.android.SdkConstants.VIEW_MERGE;
import static com.android.SdkConstants.VIEW_TAG;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.api.DropFeedback;
import com.android.ide.common.api.IDragElement;
import com.android.ide.common.api.IGraphics;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.IViewRule;
import com.android.ide.common.api.InsertType;
import com.android.ide.common.api.Point;
import com.android.ide.common.api.Rect;
import com.android.ide.common.api.RuleAction;
import com.android.ide.common.api.SegmentType;
import com.android.sdklib.IAndroidTarget;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.common.layout.ViewRule;
import org.eclipse.andmore.internal.editors.AndroidXmlEditor;
import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.editors.layout.descriptors.ViewElementDescriptor;
import org.eclipse.andmore.internal.editors.layout.gle2.GCWrapper;
import org.eclipse.andmore.internal.editors.layout.gle2.GraphicalEditorPart;
import org.eclipse.andmore.internal.editors.layout.gle2.SimpleElement;
import org.eclipse.andmore.internal.editors.layout.uimodel.UiViewElementNode;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.resources.IProject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * The rule engine manages the layout rules and interacts with them.
 * There's one {@link RulesEngine} instance per layout editor.
 * Each instance has 2 sets of rules: the static ADT rules (shared across all instances)
 * and the project specific rules (local to the current instance / layout editor).
 */
public class RulesEngine {
    private final IProject mProject;
    private final Map<Object, IViewRule> mRulesCache = new HashMap<Object, IViewRule>();

    /**
     * The type of any upcoming node manipulations performed by the {@link IViewRule}s.
     * When actions are performed in the tool (like a paste action, or a drag from palette,
     * or a drag move within the canvas, etc), these are different types of inserts,
     * and we don't want to have the rules track them closely (and pass them back to us
     * in the {@link INode#insertChildAt} methods etc), so instead we track the state
     * here on behalf of the currently executing rule.
     */
    private InsertType mInsertType = InsertType.CREATE;

    /**
     * Per-project loader for custom view rules
     */
    private RuleLoader mRuleLoader;
    private ClassLoader mUserClassLoader;

    /**
     * The editor which owns this {@link RulesEngine}
     */
    private final GraphicalEditorPart mEditor;

    /**
     * Creates a new {@link RulesEngine} associated with the selected project.
     * <p/>
     * The rules engine will look in the project for a tools jar to load custom view rules.
     *
     * @param editor the editor which owns this {@link RulesEngine}
     * @param project A non-null open project.
     */
    public RulesEngine(GraphicalEditorPart editor, IProject project) {
        mProject = project;
        mEditor = editor;

        mRuleLoader = RuleLoader.get(project);
    }

     /**
     * Returns the {@link IProject} on which the {@link RulesEngine} was created.
     */
    public IProject getProject() {
        return mProject;
    }

    /**
     * Returns the {@link GraphicalEditorPart} for which the {@link RulesEngine} was
     * created.
     *
     * @return the associated editor
     */
    public GraphicalEditorPart getEditor() {
        return mEditor;
    }

    /**
     * Called by the owner of the {@link RulesEngine} when it is going to be disposed.
     * This frees some resources, such as the project's folder monitor.
     */
    public void dispose() {
        clearCache();
    }

    /**
     * Invokes {@link IViewRule#getDisplayName()} on the rule matching the specified element.
     *
     * @param element The view element to target. Can be null.
     * @return Null if the rule failed, there's no rule or the rule does not want to override
     *   the display name. Otherwise, a string as returned by the rule.
     */
    public String callGetDisplayName(UiViewElementNode element) {
        // try to find a rule for this element's FQCN
        IViewRule rule = loadRule(element);

        if (rule != null) {
            try {
                return rule.getDisplayName();

            } catch (Exception e) {
                AdtPlugin.log(e, "%s.getDisplayName() failed: %s",
                        rule.getClass().getSimpleName(),
                        e.toString());
            }
        }

        return null;
    }

    /**
     * Invokes {@link IViewRule#addContextMenuActions(List, INode)} on the rule matching the specified element.
     *
     * @param selectedNode The node selected. Never null.
     * @return Null if the rule failed, there's no rule or the rule does not provide
     *   any custom menu actions. Otherwise, a list of {@link RuleAction}.
     */
    @Nullable
    public List<RuleAction> callGetContextMenu(NodeProxy selectedNode) {
        // try to find a rule for this element's FQCN
        IViewRule rule = loadRule(selectedNode.getNode());

        if (rule != null) {
            try {
                mInsertType = InsertType.CREATE;
                List<RuleAction> actions = new ArrayList<RuleAction>();
                rule.addContextMenuActions(actions, selectedNode);
                Collections.sort(actions);

                return actions;
            } catch (Exception e) {
                AdtPlugin.log(e, "%s.getContextMenu() failed: %s",
                        rule.getClass().getSimpleName(),
                        e.toString());
            }
        }

        return null;
    }

    /**
     * Calls the selected node to return its default action
     *
     * @param selectedNode the node to apply the action to
     * @return the default action id
     */
    public String callGetDefaultActionId(@NonNull NodeProxy selectedNode) {
        // try to find a rule for this element's FQCN
        IViewRule rule = loadRule(selectedNode.getNode());

        if (rule != null) {
            try {
                mInsertType = InsertType.CREATE;
                return rule.getDefaultActionId(selectedNode);
            } catch (Exception e) {
                AdtPlugin.log(e, "%s.getDefaultAction() failed: %s",
                        rule.getClass().getSimpleName(),
                        e.toString());
            }
        }

        return null;
    }

    /**
     * Invokes {@link IViewRule#addLayoutActions(List, INode, List)} on the rule
     * matching the specified element.
     *
     * @param actions The list of actions to add layout actions into
     * @param parentNode The layout node
     * @param children The selected children of the node, if any (used to
     *            initialize values of child layout controls, if applicable)
     * @return Null if the rule failed, there's no rule or the rule does not
     *         provide any custom menu actions. Otherwise, a list of
     *         {@link RuleAction}.
     */
    public List<RuleAction> callAddLayoutActions(List<RuleAction> actions,
            NodeProxy parentNode, List<NodeProxy> children ) {
        // try to find a rule for this element's FQCN
        IViewRule rule = loadRule(parentNode.getNode());

        if (rule != null) {
            try {
                mInsertType = InsertType.CREATE;
                rule.addLayoutActions(actions, parentNode, children);
            } catch (Exception e) {
                AdtPlugin.log(e, "%s.getContextMenu() failed: %s",
                        rule.getClass().getSimpleName(),
                        e.toString());
            }
        }

        return null;
    }

    /**
     * Invokes {@link IViewRule#getSelectionHint(INode, INode)}
     * on the rule matching the specified element.
     *
     * @param parentNode The parent of the node selected. Never null.
     * @param childNode The child node that was selected. Never null.
     * @return a list of strings to be displayed, or null or empty to display nothing
     */
    public List<String> callGetSelectionHint(NodeProxy parentNode, NodeProxy childNode) {
        // try to find a rule for this element's FQCN
        IViewRule rule = loadRule(parentNode.getNode());

        if (rule != null) {
            try {
                return rule.getSelectionHint(parentNode, childNode);

            } catch (Exception e) {
                AdtPlugin.log(e, "%s.getSelectionHint() failed: %s",
                        rule.getClass().getSimpleName(),
                        e.toString());
            }
        }

        return null;
    }

    public void callPaintSelectionFeedback(GCWrapper gcWrapper, NodeProxy parentNode,
            List<? extends INode> childNodes, Object view) {
        // try to find a rule for this element's FQCN
        IViewRule rule = loadRule(parentNode.getNode());

        if (rule != null) {
            try {
                rule.paintSelectionFeedback(gcWrapper, parentNode, childNodes, view);

            } catch (Exception e) {
                AdtPlugin.log(e, "%s.callPaintSelectionFeedback() failed: %s",
                        rule.getClass().getSimpleName(),
                        e.toString());
            }
        }
    }

    /**
     * Called when the d'n'd starts dragging over the target node.
     * If interested, returns a DropFeedback passed to onDrop/Move/Leave/Paint.
     * If not interested in drop, return false.
     * Followed by a paint.
     */
    public DropFeedback callOnDropEnter(NodeProxy targetNode,
            Object targetView, IDragElement[] elements) {
        // try to find a rule for this element's FQCN
        IViewRule rule = loadRule(targetNode.getNode());

        if (rule != null) {
            try {
                return rule.onDropEnter(targetNode, targetView, elements);

            } catch (Exception e) {
                AdtPlugin.log(e, "%s.onDropEnter() failed: %s",
                        rule.getClass().getSimpleName(),
                        e.toString());
            }
        }

        return null;
    }

    /**
     * Called after onDropEnter.
     * Returns a DropFeedback passed to onDrop/Move/Leave/Paint (typically same
     * as input one).
     */
    public DropFeedback callOnDropMove(NodeProxy targetNode,
            IDragElement[] elements,
            DropFeedback feedback,
            Point where) {
        // try to find a rule for this element's FQCN
        IViewRule rule = loadRule(targetNode.getNode());

        if (rule != null) {
            try {
                return rule.onDropMove(targetNode, elements, feedback, where);

            } catch (Exception e) {
                AdtPlugin.log(e, "%s.onDropMove() failed: %s",
                        rule.getClass().getSimpleName(),
                        e.toString());
            }
        }

        return null;
    }

    /**
     * Called when drop leaves the target without actually dropping
     */
    public void callOnDropLeave(NodeProxy targetNode,
            IDragElement[] elements,
            DropFeedback feedback) {
        // try to find a rule for this element's FQCN
        IViewRule rule = loadRule(targetNode.getNode());

        if (rule != null) {
            try {
                rule.onDropLeave(targetNode, elements, feedback);

            } catch (Exception e) {
                AdtPlugin.log(e, "%s.onDropLeave() failed: %s",
                        rule.getClass().getSimpleName(),
                        e.toString());
            }
        }
    }

    /**
     * Called when drop is released over the target to perform the actual drop.
     */
    public void callOnDropped(NodeProxy targetNode,
            IDragElement[] elements,
            DropFeedback feedback,
            Point where,
            InsertType insertType) {
        // try to find a rule for this element's FQCN
        IViewRule rule = loadRule(targetNode.getNode());

        if (rule != null) {
            try {
                mInsertType = insertType;
                rule.onDropped(targetNode, elements, feedback, where);

            } catch (Exception e) {
                AdtPlugin.log(e, "%s.onDropped() failed: %s",
                        rule.getClass().getSimpleName(),
                        e.toString());
            }
        }
    }

    /**
     * Called when a paint has been requested via DropFeedback.
     */
    public void callDropFeedbackPaint(IGraphics gc,
            NodeProxy targetNode,
            DropFeedback feedback) {
        if (gc != null && feedback != null && feedback.painter != null) {
            try {
                feedback.painter.paint(gc, targetNode, feedback);
            } catch (Exception e) {
                AdtPlugin.log(e, "DropFeedback.painter failed: %s",
                        e.toString());
            }
        }
    }

    /**
     * Called when pasting elements in an existing document on the selected target.
     *
     * @param targetNode The first node selected.
     * @param targetView The view object for the target node, or null if not known
     * @param pastedElements The elements being pasted.
     * @return the parent node the paste was applied into
     */
    public NodeProxy callOnPaste(NodeProxy targetNode, Object targetView,
            SimpleElement[] pastedElements) {

        // Find a target which accepts children. If you for example select a button
        // and attempt to paste, this will reselect the parent of the button as the paste
        // target. (This is a loop rather than just checking the direct parent since
        // we will soon ask each child whether they are *willing* to accept the new child.
        // A ScrollView for example, which only accepts one child, might also say no
        // and delegate to its parent in turn.
        INode parent = targetNode;
        while (parent instanceof NodeProxy) {
            NodeProxy np = (NodeProxy) parent;
            if (np.getNode() != null && np.getNode().getDescriptor() != null) {
                ElementDescriptor descriptor = np.getNode().getDescriptor();
                if (descriptor.hasChildren()) {
                    targetNode = np;
                    break;
                }
            }
            parent = parent.getParent();
        }

        // try to find a rule for this element's FQCN
        IViewRule rule = loadRule(targetNode.getNode());

        if (rule != null) {
            try {
                mInsertType = InsertType.PASTE;
                rule.onPaste(targetNode, targetView, pastedElements);

            } catch (Exception e) {
                AdtPlugin.log(e, "%s.onPaste() failed: %s",
                        rule.getClass().getSimpleName(),
                        e.toString());
            }
        }

        return targetNode;
    }

    // ---- Resize operations ----

    public DropFeedback callOnResizeBegin(NodeProxy child, NodeProxy parent, Rect newBounds,
            SegmentType horizontalEdge, SegmentType verticalEdge, Object childView,
            Object parentView) {
        IViewRule rule = loadRule(parent.getNode());

        if (rule != null) {
            try {
                return rule.onResizeBegin(child, parent, horizontalEdge, verticalEdge,
                        childView, parentView);
            } catch (Exception e) {
                AdtPlugin.log(e, "%s.onResizeBegin() failed: %s", rule.getClass().getSimpleName(),
                        e.toString());
            }
        }

        return null;
    }

    public void callOnResizeUpdate(DropFeedback feedback, NodeProxy child, NodeProxy parent,
            Rect newBounds, int modifierMask) {
        IViewRule rule = loadRule(parent.getNode());

        if (rule != null) {
            try {
                rule.onResizeUpdate(feedback, child, parent, newBounds, modifierMask);
            } catch (Exception e) {
                AdtPlugin.log(e, "%s.onResizeUpdate() failed: %s", rule.getClass().getSimpleName(),
                        e.toString());
            }
        }
    }

    public void callOnResizeEnd(DropFeedback feedback, NodeProxy child, NodeProxy parent,
            Rect newBounds) {
        IViewRule rule = loadRule(parent.getNode());

        if (rule != null) {
            try {
                rule.onResizeEnd(feedback, child, parent, newBounds);
            } catch (Exception e) {
                AdtPlugin.log(e, "%s.onResizeEnd() failed: %s", rule.getClass().getSimpleName(),
                        e.toString());
            }
        }
    }

    // ---- Creation customizations ----

    /**
     * Invokes the create hooks ({@link IViewRule#onCreate},
     * {@link IViewRule#onChildInserted} when a new child has been created/pasted/moved, and
     * is inserted into a given parent. The parent may be null (for example when rendering
     * top level items for preview).
     *
     * @param editor the XML editor to apply edits to the model for (performed by view
     *            rules)
     * @param parentNode the parent XML node, or null if unknown
     * @param childNode the XML node of the new node, never null
     * @param overrideInsertType If not null, specifies an explicit insert type to use for
     *            edits made during the customization
     */
    public void callCreateHooks(
        AndroidXmlEditor editor,
        NodeProxy parentNode, NodeProxy childNode,
        InsertType overrideInsertType) {
        IViewRule parentRule = null;

        if (parentNode != null) {
            UiViewElementNode parentUiNode = parentNode.getNode();
            parentRule = loadRule(parentUiNode);
        }

        if (overrideInsertType != null) {
            mInsertType = overrideInsertType;
        }

        UiViewElementNode newUiNode = childNode.getNode();
        IViewRule childRule = loadRule(newUiNode);
        if (childRule != null || parentRule != null) {
            callCreateHooks(editor, mInsertType, parentRule, parentNode,
                    childRule, childNode);
        }
    }

    private static void callCreateHooks(
            final AndroidXmlEditor editor, final InsertType insertType,
            final IViewRule parentRule, final INode parentNode,
            final IViewRule childRule, final INode newNode) {
        // Notify the parent about the new child in case it wants to customize it
        // (For example, a ScrollView parent can go and set all its children's layout params to
        // fill the parent.)
        if (!editor.isEditXmlModelPending()) {
            editor.wrapEditXmlModel(new Runnable() {
                @Override
                public void run() {
                    callCreateHooks(editor, insertType,
                            parentRule, parentNode, childRule, newNode);
                }
            });
            return;
        }

        if (parentRule != null) {
            parentRule.onChildInserted(newNode, parentNode, insertType);
        }

        // Look up corresponding IViewRule, and notify the rule about
        // this create action in case it wants to customize the new object.
        // (For example, a rule for TabHosts can go and create a default child tab
        // when you create it.)
        if (childRule != null) {
            childRule.onCreate(newNode, parentNode, insertType);
        }

        if (parentNode != null) {
            ((NodeProxy) parentNode).applyPendingChanges();
        }
    }

    /**
     * Set the type of insert currently in progress
     *
     * @param insertType the insert type to use for the next operation
     */
    public void setInsertType(InsertType insertType) {
        mInsertType = insertType;
    }

    /**
     * Return the type of insert currently in progress
     *
     * @return the type of insert currently in progress
     */
    public InsertType getInsertType() {
        return mInsertType;
    }

    // ---- Deletion ----

    public void callOnRemovingChildren(NodeProxy parentNode,
            List<INode> children) {
        if (parentNode != null) {
            UiViewElementNode parentUiNode = parentNode.getNode();
            IViewRule parentRule = loadRule(parentUiNode);
            if (parentRule != null) {
                try {
                    parentRule.onRemovingChildren(children, parentNode,
                            mInsertType == InsertType.MOVE_WITHIN);
                } catch (Exception e) {
                    AdtPlugin.log(e, "%s.onDispose() failed: %s",
                            parentRule.getClass().getSimpleName(),
                            e.toString());
                }
            }
        }
    }

    // ---- private ---

    /**
     * Returns the descriptor for the base View class.
     * This could be null if the SDK or the given platform target hasn't loaded yet.
     */
    private ViewElementDescriptor getBaseViewDescriptor() {
        Sdk currentSdk = Sdk.getCurrent();
        if (currentSdk != null) {
            IAndroidTarget target = currentSdk.getTarget(mProject);
            if (target != null) {
                AndroidTargetData data = currentSdk.getTargetData(target);
                return data.getLayoutDescriptors().getBaseViewDescriptor();
            }
        }
        return null;
    }

    /**
     * Clear the Rules cache. Calls onDispose() on each rule.
     */
    private void clearCache() {
        // The cache can contain multiple times the same rule instance for different
        // keys (e.g. the UiViewElementNode key vs. the FQCN string key.) So transfer
        // all values to a unique set.
        HashSet<IViewRule> rules = new HashSet<IViewRule>(mRulesCache.values());

        mRulesCache.clear();

        for (IViewRule rule : rules) {
            if (rule != null) {
                try {
                    rule.onDispose();
                } catch (Exception e) {
                    AdtPlugin.log(e, "%s.onDispose() failed: %s",
                            rule.getClass().getSimpleName(),
                            e.toString());
                }
            }
        }
    }

    /**
     * Checks whether the project class loader has changed, and if so
     * unregisters any view rules that use classes from the old class loader. It
     * then returns the class loader to be used.
     */
    private ClassLoader updateClassLoader() {
        ClassLoader classLoader = mRuleLoader.getClassLoader();
        if (mUserClassLoader != null && classLoader != mUserClassLoader) {
            // We have to unload all the IViewRules from the old class
            List<Object> dispose = new ArrayList<Object>();
            for (Map.Entry<Object, IViewRule> entry : mRulesCache.entrySet()) {
                IViewRule rule = entry.getValue();
                if (rule.getClass().getClassLoader() == mUserClassLoader) {
                    dispose.add(entry.getKey());
                }
            }
            for (Object object : dispose) {
                mRulesCache.remove(object);
            }
        }

        mUserClassLoader = classLoader;
        return mUserClassLoader;
    }

    /**
     * Load a rule using its descriptor. This will try to first load the rule using its
     * actual FQCN and if that fails will find the first parent that works in the view
     * hierarchy.
     */
    private IViewRule loadRule(UiViewElementNode element) {
        if (element == null) {
            return null;
        }

        String targetFqcn = null;
        ViewElementDescriptor targetDesc = null;

        ElementDescriptor d = element.getDescriptor();
        if (d instanceof ViewElementDescriptor) {
            targetDesc = (ViewElementDescriptor) d;
        }
        if (d == null || !(d instanceof ViewElementDescriptor)) {
            // This should not happen. All views should have some kind of *view* element
            // descriptor. Maybe the project is not complete and doesn't build or something.
            // In this case, we'll use the descriptor of the base android View class.
            targetDesc = getBaseViewDescriptor();
        }

        // Check whether any of the custom view .jar files have changed and if so
        // unregister previously cached view rules to force a new view rule to be loaded.
        updateClassLoader();

        // Return the rule if we find it in the cache, even if it was stored as null
        // (which means we didn't find it earlier, so don't look for it again)
        IViewRule rule = mRulesCache.get(targetDesc);
        if (rule != null || mRulesCache.containsKey(targetDesc)) {
            return rule;
        }

        // Get the descriptor and loop through the super class hierarchy
        for (ViewElementDescriptor desc = targetDesc;
                desc != null;
                desc = desc.getSuperClassDesc()) {

            // Get the FQCN of this View
            String fqcn = desc.getFullClassName();
            if (fqcn == null) {
                // Shouldn't be happening.
                return null;
            }

            // The first time we keep the FQCN around as it's the target class we were
            // initially trying to load. After, as we move through the hierarchy, the
            // target FQCN remains constant.
            if (targetFqcn == null) {
                targetFqcn = fqcn;
            }

            if (fqcn.indexOf('.') == -1) {
                // Deal with unknown descriptors; these lack the full qualified path and
                // elements in the layout without a package are taken to be in the
                // android.widget package.
                fqcn = ANDROID_WIDGET_PREFIX + fqcn;
            }

            // Try to find a rule matching the "real" FQCN. If we find it, we're done.
            // If not, the for loop will move to the parent descriptor.
            rule = loadRule(fqcn, targetFqcn);
            if (rule != null) {
                // We found one.
                // As a side effect, loadRule() also cached the rule using the target FQCN.
                return rule;
            }
        }

        // Memorize in the cache that we couldn't find a rule for this descriptor
        mRulesCache.put(targetDesc, null);
        return null;
    }

    /**
     * Try to load a rule given a specific FQCN. This looks for an exact match in either
     * the ADT scripts or the project scripts and does not look at parent hierarchy.
     * <p/>
     * Once a rule is found (or not), it is stored in a cache using its target FQCN
     * so we don't try to reload it.
     * <p/>
     * The real FQCN is the actual rule class we're loading, e.g. "android.view.View"
     * where target FQCN is the class we were initially looking for, which might be the same as
     * the real FQCN or might be a derived class, e.g. "android.widget.TextView".
     *
     * @param realFqcn The FQCN of the rule class actually being loaded.
     * @param targetFqcn The FQCN of the class actually processed, which might be different from
     *          the FQCN of the rule being loaded.
     */
    IViewRule loadRule(String realFqcn, String targetFqcn) {
        if (realFqcn == null || targetFqcn == null) {
            return null;
        }

        // Return the rule if we find it in the cache, even if it was stored as null
        // (which means we didn't find it earlier, so don't look for it again)
        IViewRule rule = mRulesCache.get(realFqcn);
        if (rule != null || mRulesCache.containsKey(realFqcn)) {
            return rule;
        }

        // Look for class via reflection
        try {
            // For now, we package view rules for the builtin Android views and
            // widgets with the tool in a special package, so look there rather
            // than in the same package as the widgets.
            String ruleClassName;
            ClassLoader classLoader;
            if (realFqcn.startsWith("android.") || //$NON-NLS-1$
                    realFqcn.equals(VIEW_MERGE) ||
                    realFqcn.endsWith(".GridLayout") || //$NON-NLS-1$ // Temporary special case
                    // FIXME: Remove this special case as soon as we pull
                    // the MapViewRule out of this code base and bundle it
                    // with the add ons
                    realFqcn.startsWith("com.google.android.maps.")) { //$NON-NLS-1$
                // This doesn't handle a case where there are name conflicts
                // (e.g. where there are multiple different views with the same
                // class name and only differing in package names, but that's a
                // really bad practice in the first place, and if that situation
                // should come up in the API we can enhance this algorithm.
                String packageName = ViewRule.class.getName();
                packageName = packageName.substring(0, packageName.lastIndexOf('.'));
                classLoader = RulesEngine.class.getClassLoader();
                int dotIndex = realFqcn.lastIndexOf('.');
                String baseName = realFqcn.substring(dotIndex+1);
                // Capitalize rule class name to match naming conventions, if necessary (<merge>)
                if (Character.isLowerCase(baseName.charAt(0))) {
                    if (baseName.equals(VIEW_TAG)) {
                        // Hack: ViewRule is generic for the "View" class, so we can't use it
                        // for the special XML "view" tag (lowercase); instead, the rule is
                        // named "ViewTagRule" instead.
                        baseName = "ViewTag"; //$NON-NLS-1$
                    }
                    baseName = Character.toUpperCase(baseName.charAt(0)) + baseName.substring(1);
                }
                ruleClassName = packageName + "." + //$NON-NLS-1$
                    baseName + "Rule"; //$NON-NLS-1$
            } else {
                // Initialize the user-classpath for 3rd party IViewRules, if necessary
                classLoader = updateClassLoader();
                if (classLoader == null) {
                    // The mUserClassLoader can be null; this is the typical scenario,
                    // when the user is only using builtin layout rules.
                    // This means however we can't resolve this fqcn since it's not
                    // in the name space of the builtin rules.
                    mRulesCache.put(realFqcn, null);
                    return null;
                }

                // For other (3rd party) widgets, look in the same package (though most
                // likely not in the same jar!)
                ruleClassName = realFqcn + "Rule"; //$NON-NLS-1$
            }

            Class<?> clz = Class.forName(ruleClassName, true, classLoader);
            rule = (IViewRule) clz.newInstance();
            return initializeRule(rule, targetFqcn);
        } catch (ClassNotFoundException ex) {
            // Not an unexpected error - this means that there isn't a helper for this
            // class.
        } catch (InstantiationException e) {
            // This is NOT an expected error: fail.
            AdtPlugin.log(e, "load rule error (%s): %s", realFqcn, e.toString());
        } catch (IllegalAccessException e) {
            // This is NOT an expected error: fail.
            AdtPlugin.log(e, "load rule error (%s): %s", realFqcn, e.toString());
        }

        // Memorize in the cache that we couldn't find a rule for this real FQCN
        mRulesCache.put(realFqcn, null);
        return null;
    }

    /**
     * Initialize a rule we just loaded. The rule has a chance to examine the target FQCN
     * and bail out.
     * <p/>
     * Contract: the rule is not in the {@link #mRulesCache} yet and this method will
     * cache it using the target FQCN if the rule is accepted.
     * <p/>
     * The real FQCN is the actual rule class we're loading, e.g. "android.view.View"
     * where target FQCN is the class we were initially looking for, which might be the same as
     * the real FQCN or might be a derived class, e.g. "android.widget.TextView".
     *
     * @param rule A rule freshly loaded.
     * @param targetFqcn The FQCN of the class actually processed, which might be different from
     *          the FQCN of the rule being loaded.
     * @return The rule if accepted, or null if the rule can't handle that FQCN.
     */
    private IViewRule initializeRule(IViewRule rule, String targetFqcn) {

        try {
            if (rule.onInitialize(targetFqcn, new ClientRulesEngine(this, targetFqcn))) {
                // Add it to the cache and return it
                mRulesCache.put(targetFqcn, rule);
                return rule;
            } else {
                rule.onDispose();
            }
        } catch (Exception e) {
            AdtPlugin.log(e, "%s.onInit() failed: %s",
                    rule.getClass().getSimpleName(),
                    e.toString());
        }

        return null;
    }
}
