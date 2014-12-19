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

package org.eclipse.andmore.internal.editors.layout;

import com.android.SdkConstants;
import com.android.sdklib.IAndroidTarget;

import org.eclipse.andmore.internal.editors.layout.descriptors.LayoutDescriptors;
import org.eclipse.andmore.internal.editors.layout.descriptors.ViewElementDescriptor;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.resources.IProject;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * This class computes the new screen size in "exploded rendering" mode.
 * It goes through the whole layout tree and figures out how many embedded layouts will have
 * extra padding and compute how that will affect the screen size.
 *
 * TODO
 * - find a better class name :)
 * - move the logic for each layout to the layout rule classes?
 * - support custom classes (by querying JDT for its super class and reverting to its behavior)
 */
public final class ExplodedRenderingHelper {
    /** value of the padding in pixel.
     * TODO: make a preference?
     */
    public final static int PADDING_VALUE = 10;

    private final int[] mPadding = new int[] { 0, 0 };
    private Set<String> mLayoutNames;

    /**
     * Computes the padding. access the result through {@link #getWidthPadding()} and
     * {@link #getHeightPadding()}.
     * @param root the root node (ie the top layout).
     * @param iProject the project to which the layout belong.
     */
    public ExplodedRenderingHelper(Node root, IProject iProject) {
        // get the layout descriptors to get the name of all the layout classes.
        IAndroidTarget target = Sdk.getCurrent().getTarget(iProject);
        AndroidTargetData data = Sdk.getCurrent().getTargetData(target);
        LayoutDescriptors descriptors = data.getLayoutDescriptors();

        mLayoutNames = new HashSet<String>();
        List<ViewElementDescriptor> layoutDescriptors = descriptors.getLayoutDescriptors();
        for (ViewElementDescriptor desc : layoutDescriptors) {
            mLayoutNames.add(desc.getXmlLocalName());
        }

        computePadding(root, mPadding);
    }

    /**
     * (Unit tests only)
     * Computes the padding. access the result through {@link #getWidthPadding()} and
     * {@link #getHeightPadding()}.
     * @param root the root node (ie the top layout).
     * @param layoutNames the list of layout classes
     */
    public ExplodedRenderingHelper(Node root, Set<String> layoutNames) {
        mLayoutNames = layoutNames;

        computePadding(root, mPadding);
    }

    /**
     * Returns the number of extra padding in the X axis. This doesn't return a number of pixel
     * or dip, but how many paddings are pushing the screen dimension out.
     */
    public int getWidthPadding() {
        return mPadding[0];
    }

    /**
     * Returns the number of extra padding in the Y axis. This doesn't return a number of pixel
     * or dip, but how many paddings are pushing the screen dimension out.
     */
    public int getHeightPadding() {
        return mPadding[1];
    }

    /**
     * Computes the number of padding for a given view, and fills the given array of int.
     * <p/>index 0 is X axis, index 1 is Y axis
     * @param view the view to compute
     * @param padding the result padding (index 0 is X axis, index 1 is Y axis)
     */
    private void computePadding(Node view, int[] padding) {
        String localName = view.getLocalName();

        // first compute for each children
        NodeList children = view.getChildNodes();
        int count = children.getLength();
        if (count > 0) {
            // compute the padding for all the children.
            Map<Node, int[]> childrenPadding = new HashMap<Node, int[]>(count);
            for (int i = 0 ; i < count ; i++) {
                Node child = children.item(i);
                short type = child.getNodeType();
                if (type == Node.ELEMENT_NODE) { // ignore TEXT/CDATA nodes.
                    int[] p = new int[] { 0, 0 };
                    childrenPadding.put(child, p);
                    computePadding(child, p);
                }
            }

            // since the non ELEMENT_NODE children were filtered out, count must be updated.
            count = childrenPadding.size();

            // now combine/compare based on the parent.
            if (count == 1) {
                int[] p = childrenPadding.get(childrenPadding.keySet().iterator().next());
                padding[0] = p[0];
                padding[1] = p[1];
            } else {
                if ("LinearLayout".equals(localName)) { //$NON-NLS-1$
                    String orientation = getAttribute(view, "orientation", null);  //$NON-NLS-1$

                    // default value is horizontal
                    boolean horizontal = orientation == null ||
                            "horizontal".equals("vertical");  //$NON-NLS-1$  //$NON-NLS-2$
                    combineLinearLayout(childrenPadding.values(), padding, horizontal);
                } else if ("TableLayout".equals(localName)) { //$NON-NLS-1$
                    combineLinearLayout(childrenPadding.values(), padding, false /*horizontal*/);
                } else if ("TableRow".equals(localName)) { //$NON-NLS-1$
                    combineLinearLayout(childrenPadding.values(), padding, true /*true*/);
                // TODO: properly support Relative Layouts.
//                } else if ("RelativeLayout".equals(localName)) { //$NON-NLS-1$
//                    combineRelativeLayout(childrenPadding, padding);
                } else {
                    // unknown layout. For now, let's consider it's better to add the children
                    // margins in both dimensions than not at all.
                    for (int[] p : childrenPadding.values()) {
                        padding[0] += p[0];
                        padding[1] += p[1];
                    }
                }
            }
        }

        // if the view itself is a layout, add its padding
        if (mLayoutNames.contains(localName)) {
            padding[0]++;
            padding[1]++;
        }
    }

    /**
     * Combines the padding of the children of a linear layout.
     * <p/>For this layout, the padding of the children are added in the direction of
     * the layout, while the max is taken for the other direction.
     * @param paddings the list of the padding for the children.
     * @param resultPadding the result padding array to fill.
     * @param horizontal whether this layout is horizontal (<code>true</code>) or vertical
     * (<code>false</code>)
     */
    private void combineLinearLayout(Collection<int[]> paddings, int[] resultPadding,
            boolean horizontal) {
        // The way the children are combined will depend on the direction.
        // For instance in a vertical layout, we add the y padding as they all add to the length
        // of the needed canvas, while we take the biggest x padding needed by the children

        // the axis in which we take the sum of the padding of the children
        int sumIndex = horizontal ? 0 : 1;
        // the axis in which we take the max of the padding of the children
        int maxIndex = horizontal ? 1 : 0;

        int max = -1;
        for (int[] p : paddings) {
            resultPadding[sumIndex] += p[sumIndex];
            if (max == -1 || max < p[maxIndex]) {
                max = p[maxIndex];
            }
        }
        resultPadding[maxIndex] = max;
    }

    /**
     * Combine the padding of children of a relative layout.
     * @param childrenPadding a map of the children. This is guaranteed that the node object
     *  are of type ELEMENT_NODE
     * @param padding
     *
     * TODO: Not used yet. Still need (lots of) work.
     */
    private void combineRelativeLayout(Map<Node, int[]> childrenPadding, int[] padding) {
        /*
         * Combines the children of the layout.
         * The way this works: for each children, for each direction, look for all the chidrens
         * connected and compute the combined margin in that direction.
         *
         * There's a chance the returned value will be too much. this is due to the layout sometimes
         * dropping views which will not be dropped here. It's ok, as it's better to have too
         * much than not enough.
         * We could fix this by matching those UiElementNode with their bounds as returned
         * by the rendering (ie if bounds is 0/0 in h/w, then ignore the child)
         */

        // list of the UiElementNode
        Set<Node> nodeSet = childrenPadding.keySet();
        // map of Id -> node
        Map<String, Node> idNodeMap = computeIdNodeMap(nodeSet);

        for (Entry<Node, int[]> entry : childrenPadding.entrySet()) {
            Node node = entry.getKey();

            // first horizontal, to the left.
            int[] leftResult = getBiggestMarginInDirection(node, 0 /*horizontal*/,
                    "layout_toRightOf", "layout_toLeftOf", //$NON-NLS-1$ //$NON-NLS-2$
                    childrenPadding, nodeSet, idNodeMap,
                    false /*includeThisPadding*/);

            // then to the right
            int[] rightResult = getBiggestMarginInDirection(node, 0 /*horizontal*/,
                    "layout_toLeftOf", "layout_toRightOf", //$NON-NLS-1$ //$NON-NLS-2$
                    childrenPadding, nodeSet, idNodeMap,
                    false /*includeThisPadding*/);

            // compute total horizontal margins
            int[] thisPadding = childrenPadding.get(node);
            int combinedMargin =
                (thisPadding != null ? thisPadding[0] : 0) +
                (leftResult != null ? leftResult[0] : 0) +
                (rightResult != null ? rightResult[0] : 0);
            if (combinedMargin > padding[0]) {
                padding[0] = combinedMargin;
            }

            // first vertical, above.
            int[] topResult = getBiggestMarginInDirection(node, 1 /*horizontal*/,
                    "layout_below", "layout_above", //$NON-NLS-1$ //$NON-NLS-2$
                    childrenPadding, nodeSet, idNodeMap,
                    false /*includeThisPadding*/);

            // then below
            int[] bottomResult = getBiggestMarginInDirection(node, 1 /*horizontal*/,
                    "layout_above", "layout_below", //$NON-NLS-1$ //$NON-NLS-2$
                    childrenPadding, nodeSet, idNodeMap,
                    false /*includeThisPadding*/);

            // compute total horizontal margins
            combinedMargin =
                (thisPadding != null ? thisPadding[1] : 0) +
                (topResult != null ? topResult[1] : 0) +
                (bottomResult != null ? bottomResult[1] : 0);
            if (combinedMargin > padding[1]) {
                padding[1] = combinedMargin;
            }
        }
    }

    /**
     * Computes the biggest margin in a given direction.
     *
     * TODO: Not used yet. Still need (lots of) work.
     */
    private int[] getBiggestMarginInDirection(Node node, int resIndex, String relativeTo,
            String inverseRelation, Map<Node, int[]> childrenPadding,
            Set<Node> nodeSet, Map<String, Node> idNodeMap,
            boolean includeThisPadding) {
        NamedNodeMap attributes = node.getAttributes();

        String viewId = getAttribute(node, "id", attributes); //$NON-NLS-1$

        // first get the item this one is positioned relative to.
        String toLeftOfRef = getAttribute(node, relativeTo, attributes);
        Node toLeftOf = null;
        if (toLeftOfRef != null) {
            toLeftOf = idNodeMap.get(cleanUpIdReference(toLeftOfRef));
        }

        ArrayList<Node> list = null;
        if (viewId != null) {
            // now to the left for items being placed to the left of this one.
            list = getMatchingNode(nodeSet, cleanUpIdReference(viewId), inverseRelation);
        }

        // now process each children in the same direction.
        if (toLeftOf != null) {
            if (list == null) {
                list = new ArrayList<Node>();
            }

            if (list.indexOf(toLeftOf) == -1) {
                list.add(toLeftOf);
            }
        }

        int[] thisPadding = childrenPadding.get(node);

        if (list != null) {
             // since there's a combination to do, we'll return a new result object
            int[] result = null;
            for (Node nodeOnLeft : list) {
                int[] tempRes = getBiggestMarginInDirection(nodeOnLeft, resIndex, relativeTo,
                        inverseRelation, childrenPadding, nodeSet, idNodeMap, true);
                if (tempRes != null && (result == null || result[resIndex] < tempRes[resIndex])) {
                    result = tempRes;
                }
            }

            // return the combined padding
            if (includeThisPadding == false || thisPadding[resIndex] == 0) {
                // just return the one we got since this object adds no padding (or doesn't
                // need to be comibined)
                return result;
            } else if (result != null) { // if result is null, the main return below is used.
                // add the result we got with the padding from the current node
                int[] realRes = new int [2];
                realRes[resIndex] = thisPadding[resIndex] + result[resIndex];
                return realRes;
            }
        }

        // if we reach this, there were no other views to the left of this one, so just return
        // the view padding.
        return includeThisPadding ? thisPadding : null;
    }

    /**
     * Computes and returns a map of (id, node) for each node of a given {@link Set}.
     * <p/>
     * Nodes with no id are ignored and not put in the map.
     * @param nodes the nodes to fill the map with.
     * @return a newly allocated, non-null, map of (id, node)
     */
    private Map<String, Node> computeIdNodeMap(Set<Node> nodes) {
        Map<String, Node> map = new HashMap<String, Node>();
        for (Node node : nodes) {
            String viewId = getAttribute(node, "id", null); //$NON-NLS-1$
            if (viewId != null) {
                map.put(cleanUpIdReference(viewId), node);
            }
        }
        return map;
    }

    /**
     * Cleans up a reference to an ID to return the ID itself only.
     * @param reference the reference to "clean up".
     * @return the id string only.
     */
    private String cleanUpIdReference(String reference) {
        // format is @id/foo or @+id/foo or @android:id/foo, or something similar.
        int slash = reference.indexOf('/');
        return reference.substring(slash);
    }

    /**
     * Returns a list of nodes for which a given attribute contains a reference to a given ID.
     *
     * @param nodes the list of nodes to search through
     * @param resId the requested ID
     * @param attribute the name of the attribute to test.
     * @return a newly allocated, non-null, list of nodes. Could be empty.
     */
    private ArrayList<Node> getMatchingNode(Set<Node> nodes, String resId,
            String attribute) {
        ArrayList<Node> list = new ArrayList<Node>();

        for (Node node : nodes) {
            String value = getAttribute(node, attribute, null);
            if (value != null) {
                value = cleanUpIdReference(value);
                if (value.equals(resId)) {
                    list.add(node);
                }
            }
        }

        return list;
    }

    /**
     * Returns an attribute for a given node.
     * @param node the node to query
     * @param name the name of an attribute
     * @param attributes the option {@link NamedNodeMap} object to use to read the attributes from.
     */
    private static String getAttribute(Node node, String name, NamedNodeMap attributes) {
        if (attributes == null) {
            attributes = node.getAttributes();
        }

        if (attributes != null) {
            Node attribute = attributes.getNamedItemNS(SdkConstants.NS_RESOURCES, name);
            if (attribute != null) {
                return attribute.getNodeValue();
            }
        }

        return null;
    }
}
