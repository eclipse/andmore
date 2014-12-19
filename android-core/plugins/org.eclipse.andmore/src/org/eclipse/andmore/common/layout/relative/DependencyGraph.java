/*
 * Copyright (C) 2011 The Android Open Source Project
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
package org.eclipse.andmore.common.layout.relative;

import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ATTR_LAYOUT_RESOURCE_PREFIX;
import static com.android.SdkConstants.VALUE_TRUE;

import static com.android.SdkConstants.ANDROID_URI;

import com.android.ide.common.api.IDragElement;
import com.android.ide.common.api.IDragElement.IDragAttribute;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.INode.IAttribute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.andmore.common.layout.BaseLayoutRule;
import org.eclipse.andmore.common.layout.BaseViewRule;

/**
 * Data structure about relative layout relationships which makes it possible to:
 * <ul>
 * <li> Quickly determine not just the dependencies on other nodes, but which nodes
 *    depend on this node such that they can be visualized for the selection
 * <li> Determine if there are cyclic dependencies, and whether a potential move
 *    would result in a cycle
 * <li> Determine the "depth" of a given node (in terms of how many connections it
 *     is away from a parent edge) such that we can prioritize connections which
 *     minimizes the depth
 * </ul>
 */
class DependencyGraph {
    /** Format to chain include cycles in: a=>b=>c=>d etc */
    static final String CHAIN_FORMAT = "%1$s=>%2$s"; //$NON-NLS-1$

    /** Format to chain constraint dependencies: button 1 above button2 etc */
    private static final String DEPENDENCY_FORMAT = "%1$s %2$s %3$s"; //$NON-NLS-1$

    private final Map<String, ViewData> mIdToView = new HashMap<String, ViewData>();
    private final Map<INode, ViewData> mNodeToView = new HashMap<INode, ViewData>();

    /** Constructs a new {@link DependencyGraph} for the given relative layout */
    DependencyGraph(INode layout) {
        INode[] nodes = layout.getChildren();

        // Parent view:
        String parentId = layout.getStringAttr(ANDROID_URI, ATTR_ID);
        if (parentId != null) {
            parentId = BaseViewRule.stripIdPrefix(parentId);
        } else {
            parentId = "RelativeLayout"; // For display purposes; we never reference
            // the parent id from a constraint, only via parent-relative params
            // like centerInParent
        }
        ViewData parentView = new ViewData(layout, parentId);
        mNodeToView.put(layout, parentView);
        if (parentId != null) {
            mIdToView.put(parentId, parentView);
        }

        for (INode child : nodes) {
            String id = child.getStringAttr(ANDROID_URI, ATTR_ID);
            if (id != null) {
                id = BaseViewRule.stripIdPrefix(id);
            }
            ViewData view = new ViewData(child, id);
            mNodeToView.put(child, view);
            if (id != null) {
                mIdToView.put(id, view);
            }
        }

        for (ViewData view : mNodeToView.values()) {
            for (IAttribute attribute : view.node.getLiveAttributes()) {
                String name = attribute.getName();
                ConstraintType type = ConstraintType.fromAttribute(name);
                if (type != null) {
                    String value = attribute.getValue();

                    if (type.targetParent) {
                        if (value.equals(VALUE_TRUE)) {
                            Constraint constraint = new Constraint(type, view, parentView);
                            view.dependsOn.add(constraint);
                            parentView.dependedOnBy.add(constraint);
                        }
                    } else {
                        // id-based constraint.
                        // NOTE: The id could refer to some widget that is NOT a sibling!
                        String targetId = BaseViewRule.stripIdPrefix(value);
                        ViewData target = mIdToView.get(targetId);
                        if (target == view) {
                            // Self-reference. RelativeLayout ignores these so it's
                            // not an error like a deeper cycle (where RelativeLayout
                            // will throw an exception), but we might as well warn
                            // the user about it.
                            // TODO: Where do we emit this error?
                        } else if (target != null) {
                            Constraint constraint = new Constraint(type, view, target);
                            view.dependsOn.add(constraint);
                            target.dependedOnBy.add(constraint);
                        } else {
                            // This is valid but we might want to warn...
                            //System.out.println("Warning: no view data found for " + targetId);
                        }
                    }
                }
            }
        }
    }

    public ViewData getView(IDragElement element) {
        IDragAttribute attribute = element.getAttribute(ANDROID_URI, ATTR_ID);
        if (attribute != null) {
            String id = attribute.getValue();
            id = BaseViewRule.stripIdPrefix(id);
            return getView(id);
        }

        return null;
    }

    public ViewData getView(String id) {
        return mIdToView.get(id);
    }

    public ViewData getView(INode node) {
        return mNodeToView.get(node);
    }

    /**
     * Returns the set of views that depend on the given node in either the horizontal or
     * vertical direction
     *
     * @param nodes the set of nodes that we want to compute the transitive dependencies
     *            for
     * @param vertical if true, look for vertical edge dependencies, otherwise look for
     *            horizontal edge dependencies
     * @return the set of nodes that directly or indirectly depend on the given nodes in
     *         the given direction
     */
    public Set<INode> dependsOn(Collection<? extends INode> nodes, boolean vertical) {
        List<ViewData> reachable = new ArrayList<ViewData>();

        // Traverse the graph of constraints and determine all nodes affected by
        // this node
        Set<ViewData> visiting = new HashSet<ViewData>();
        for (INode node : nodes) {
            ViewData view = mNodeToView.get(node);
            if (view != null) {
                findBackwards(view, visiting, reachable, vertical, view);
            }
        }

        Set<INode> dependents = new HashSet<INode>(reachable.size());

        for (ViewData v : reachable) {
            dependents.add(v.node);
        }

        return dependents;
    }

    private void findBackwards(ViewData view,
            Set<ViewData> visiting, List<ViewData> reachable,
            boolean vertical, ViewData start) {
        visiting.add(view);
        reachable.add(view);

        for (Constraint constraint : view.dependedOnBy) {
            if (vertical && !constraint.type.verticalEdge) {
                continue;
            } else  if (!vertical && !constraint.type.horizontalEdge) {
                continue;
            }

            assert constraint.to == view;
            ViewData from = constraint.from;
            if (visiting.contains(from)) {
                // Cycle - what do we do to highlight this?
                List<Constraint> path = getPathTo(start.node, view.node, vertical);
                if (path != null) {
                    // TODO: display to the user somehow. We need log access for the
                    // view rules.
                    System.out.println(Constraint.describePath(path, null, null));
                }
            } else {
                findBackwards(from, visiting, reachable, vertical, start);
            }
        }

        visiting.remove(view);
    }

    public List<Constraint> getPathTo(INode from, INode to, boolean vertical) {
        // Traverse the graph of constraints and determine all nodes affected by
        // this node
        Set<ViewData> visiting = new HashSet<ViewData>();
        List<Constraint> path = new ArrayList<Constraint>();
        ViewData view = mNodeToView.get(from);
        if (view != null) {
            return findForwards(view, visiting, path, vertical, to);
        }

        return null;
    }

    private List<Constraint> findForwards(ViewData view, Set<ViewData> visiting,
            List<Constraint> path, boolean vertical, INode target) {
        visiting.add(view);

        for (Constraint constraint : view.dependsOn) {
            if (vertical && !constraint.type.verticalEdge) {
                continue;
            } else  if (!vertical && !constraint.type.horizontalEdge) {
                continue;
            }

            try {
                path.add(constraint);

                if (constraint.to.node == target) {
                    return new ArrayList<Constraint>(path);
                }

                assert constraint.from == view;
                ViewData to = constraint.to;
                if (visiting.contains(to)) {
                    // CYCLE!
                    continue;
                }

                List<Constraint> chain = findForwards(to, visiting, path, vertical, target);
                if (chain != null) {
                    return chain;
                }
            } finally {
                path.remove(constraint);
            }
        }

        visiting.remove(view);

        return null;
    }

    /**
     * Info about a specific widget child of a relative layout and its constraints. This
     * is a node in the dependency graph.
     */
    static class ViewData {
        public final INode node;
        public final String id;
        public final List<Constraint> dependsOn = new ArrayList<Constraint>(4);
        public final List<Constraint> dependedOnBy = new ArrayList<Constraint>(8);

        ViewData(INode node, String id) {
            this.node = node;
            this.id = id;
        }
    }

    /**
     * Info about a specific constraint between two widgets in a relative layout. This is
     * an edge in the dependency graph.
     */
    static class Constraint {
        public final ConstraintType type;
        public final ViewData from;
        public final ViewData to;

        // TODO: Initialize depth -- should be computed independently for top, left, etc.
        // We can use this in GuidelineHandler.MatchComparator to prefer matches that
        // are closer to a parent edge:
        //public int depth;

        Constraint(ConstraintType type, ViewData from, ViewData to) {
            this.type = type;
            this.from = from;
            this.to = to;
        }

        static String describePath(List<Constraint> path, String newName, String newId) {
            String s = "";
            for (int i = path.size() - 1; i >= 0; i--) {
                Constraint constraint = path.get(i);
                String suffix = (i == path.size() -1) ? constraint.to.id : s;
                s = String.format(DEPENDENCY_FORMAT, constraint.from.id,
                        stripLayoutAttributePrefix(constraint.type.name), suffix);
            }

            if (newName != null) {
                s = String.format(DEPENDENCY_FORMAT, s, stripLayoutAttributePrefix(newName),
                        BaseViewRule.stripIdPrefix(newId));
            }

            return s;
        }

        private static String stripLayoutAttributePrefix(String name) {
            if (name.startsWith(ATTR_LAYOUT_RESOURCE_PREFIX)) {
                return name.substring(ATTR_LAYOUT_RESOURCE_PREFIX.length());
            }

            return name;
        }
    }
}
