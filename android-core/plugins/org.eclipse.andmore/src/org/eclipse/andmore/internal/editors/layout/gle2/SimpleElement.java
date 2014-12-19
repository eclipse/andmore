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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.api.IDragElement;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.Rect;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an XML element with a name, attributes and inner elements.
 * <p/>
 * The semantic of the element name is to be a fully qualified class name of a View to inflate.
 * The element name is not expected to have a name space.
 * <p/>
 * For a more detailed explanation of the purpose of this class,
 * please see {@link SimpleXmlTransfer}.
 */
public class SimpleElement implements IDragElement {

    /** Version number of the internal serialized string format. */
    private static final String FORMAT_VERSION = "3";

    private final String mFqcn;
    private final String mParentFqcn;
    private final Rect mBounds;
    private final Rect mParentBounds;
    private final List<IDragAttribute> mAttributes = new ArrayList<IDragAttribute>();
    private final List<IDragElement> mElements = new ArrayList<IDragElement>();

    private IDragAttribute[] mCachedAttributes = null;
    private IDragElement[] mCachedElements = null;
    private SelectionItem mSelectionItem;

    /**
     * Creates a new {@link SimpleElement} with the specified element name.
     *
     * @param fqcn A fully qualified class name of a View to inflate, e.g.
     *             "android.view.Button". Must not be null nor empty.
     * @param parentFqcn The fully qualified class name of the parent of this element.
     *                   Can be null but not empty.
     * @param bounds The canvas bounds of the originating canvas node of the element.
     *               If null, a non-null invalid rectangle will be assigned.
     * @param parentBounds The canvas bounds of the parent of this element. Can be null.
     */
    public SimpleElement(String fqcn, String parentFqcn, Rect bounds, Rect parentBounds) {
        mFqcn = fqcn;
        mParentFqcn = parentFqcn;
        mBounds = bounds == null ? new Rect() : bounds.copy();
        mParentBounds = parentBounds == null ? new Rect() : parentBounds.copy();
    }

    /**
     * Returns the element name, which must match a fully qualified class name of
     * a View to inflate.
     */
    @Override
    public @NonNull String getFqcn() {
        return mFqcn;
    }

    /**
     * Returns the bounds of the element's node, if it originated from an existing
     * canvas. The rectangle is invalid and non-null when the element originated
     * from the object palette (unless it successfully rendered a preview)
     */
    @Override
    public @NonNull Rect getBounds() {
        return mBounds;
    }

    /**
     * Returns the fully qualified class name of the parent, if the element originated
     * from an existing canvas. Returns null if the element has no parent, such as a top
     * level element or an element originating from the object palette.
     */
    @Override
    public String getParentFqcn() {
        return mParentFqcn;
    }

    /**
     * Returns the bounds of the element's parent, absolute for the canvas, or null if there
     * is no suitable parent. This is null when {@link #getParentFqcn()} is null.
     */
    @Override
    public @NonNull Rect getParentBounds() {
        return mParentBounds;
    }

    @Override
    public @NonNull IDragAttribute[] getAttributes() {
        if (mCachedAttributes == null) {
            mCachedAttributes = mAttributes.toArray(new IDragAttribute[mAttributes.size()]);
        }
        return mCachedAttributes;
    }

    @Override
    public IDragAttribute getAttribute(@Nullable String uri, @NonNull String localName) {
        for (IDragAttribute attr : mAttributes) {
            if (attr.getUri().equals(uri) && attr.getName().equals(localName)) {
                return attr;
            }
        }

        return null;
    }

    @Override
    public @NonNull IDragElement[] getInnerElements() {
        if (mCachedElements == null) {
            mCachedElements = mElements.toArray(new IDragElement[mElements.size()]);
        }
        return mCachedElements;
    }

    public void addAttribute(SimpleAttribute attr) {
        mCachedAttributes = null;
        mAttributes.add(attr);
    }

    public void addInnerElement(SimpleElement e) {
        mCachedElements = null;
        mElements.add(e);
    }

    @Override
    public boolean isSame(@NonNull INode node) {
        if (mSelectionItem != null) {
            return node == mSelectionItem.getNode();
        } else {
            return node.getBounds().equals(mBounds);
        }
    }

    void setSelectionItem(@Nullable SelectionItem selectionItem) {
        mSelectionItem = selectionItem;
    }

    @Nullable
    SelectionItem getSelectionItem() {
        return mSelectionItem;
    }

    @Nullable
    static SimpleElement findPrimary(SimpleElement[] elements, SelectionItem primary) {
        if (elements == null || elements.length == 0) {
            return null;
        }

        if (elements.length == 1 || primary == null) {
            return elements[0];
        }

        for (SimpleElement element : elements) {
            if (element.getSelectionItem() == primary) {
                return element;
            }
        }

        return elements[0];
    }

    // reader and writer methods

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{V=").append(FORMAT_VERSION);
        sb.append(",N=").append(mFqcn);
        if (mParentFqcn != null) {
            sb.append(",P=").append(mParentFqcn);
        }
        if (mBounds != null && mBounds.isValid()) {
            sb.append(String.format(",R=%d %d %d %d", mBounds.x, mBounds.y, mBounds.w, mBounds.h));
        }
        if (mParentBounds != null && mParentBounds.isValid()) {
            sb.append(String.format(",Q=%d %d %d %d",
                    mParentBounds.x, mParentBounds.y, mParentBounds.w, mParentBounds.h));
        }
        sb.append('\n');
        for (IDragAttribute a : mAttributes) {
            sb.append(a.toString());
        }
        for (IDragElement e : mElements) {
            sb.append(e.toString());
        }
        sb.append("}\n"); //$NON-NLS-1$
        return sb.toString();
    }

    /** Parses a string containing one or more elements. */
    static SimpleElement[] parseString(String value) {
        ArrayList<SimpleElement> elements = new ArrayList<SimpleElement>();
        String[] lines = value.split("\n");
        int[] index = new int[] { 0 };
        SimpleElement element = null;
        while ((element = parseLines(lines, index)) != null) {
            elements.add(element);
        }
        return elements.toArray(new SimpleElement[elements.size()]);
    }

    /**
     * Parses one element from the input lines array, starting at the inOutIndex
     * and updating the inOutIndex to match the next unread line on output.
     */
    private static SimpleElement parseLines(String[] lines, int[] inOutIndex) {
        SimpleElement e = null;
        int index = inOutIndex[0];
        while (index < lines.length) {
            String line = lines[index++];
            String s = line.trim();
            if (s.startsWith("{")) {                                //$NON-NLS-1$
                if (e == null) {
                    // This is the element's header, it should have
                    // the format "key=value,key=value,..."
                    String version = null;
                    String fqcn = null;
                    String parent = null;
                    Rect bounds = null;
                    Rect pbounds = null;

                    for (String s2 : s.substring(1).split(",")) {   //$NON-NLS-1$
                        int pos = s2.indexOf('=');
                        if (pos <= 0 || pos == s2.length() - 1) {
                            continue;
                        }
                        String key = s2.substring(0, pos).trim();
                        String value = s2.substring(pos + 1).trim();

                        if (key.equals("V")) {                      //$NON-NLS-1$
                            version = value;
                            if (!value.equals(FORMAT_VERSION)) {
                                // Wrong format version. Don't even try to process anything
                                // else and just give up everything.
                                inOutIndex[0] = index;
                                return null;
                            }

                        } else if (key.equals("N")) {               //$NON-NLS-1$
                            fqcn = value;

                        } else if (key.equals("P")) {               //$NON-NLS-1$
                            parent = value;

                        } else if (key.equals("R") || key.equals("Q")) { //$NON-NLS-1$ //$NON-NLS-2$
                            // Parse the canvas bounds
                            String[] sb = value.split(" +");        //$NON-NLS-1$
                            if (sb != null && sb.length == 4) {
                                Rect r = null;
                                try {
                                    r = new Rect();
                                    r.x = Integer.parseInt(sb[0]);
                                    r.y = Integer.parseInt(sb[1]);
                                    r.w = Integer.parseInt(sb[2]);
                                    r.h = Integer.parseInt(sb[3]);

                                    if (key.equals("R")) {
                                        bounds = r;
                                    } else {
                                        pbounds = r;
                                    }
                                } catch (NumberFormatException ignore) {
                                }
                            }
                        }
                    }

                    // We need at least a valid name to recreate an element
                    if (version != null && fqcn != null && fqcn.length() > 0) {
                        e = new SimpleElement(fqcn, parent, bounds, pbounds);
                    }
                } else {
                    // This is an inner element... need to parse the { line again.
                    inOutIndex[0] = index - 1;
                    SimpleElement e2 = SimpleElement.parseLines(lines, inOutIndex);
                    if (e2 != null) {
                        e.addInnerElement(e2);
                    }
                    index = inOutIndex[0];
                }

            } else if (e != null && s.startsWith("@")) {    //$NON-NLS-1$
                SimpleAttribute a = SimpleAttribute.parseString(line);
                if (a != null) {
                    e.addAttribute(a);
                }

            } else if (e != null && s.startsWith("}")) {     //$NON-NLS-1$
                // We're done with this element
                inOutIndex[0] = index;
                return e;
            }
        }
        inOutIndex[0] = index;
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SimpleElement) {
            SimpleElement se = (SimpleElement) obj;

            // Bounds and parentFqcn must be null on both sides or equal.
            if ((mBounds == null && se.mBounds != null) ||
                    (mBounds != null && !mBounds.equals(se.mBounds))) {
                return false;
            }
            if ((mParentFqcn == null && se.mParentFqcn != null) ||
                    (mParentFqcn != null && !mParentFqcn.equals(se.mParentFqcn))) {
                return false;
            }
            if ((mParentBounds == null && se.mParentBounds != null) ||
                    (mParentBounds != null && !mParentBounds.equals(se.mParentBounds))) {
                return false;
            }

            return mFqcn.equals(se.mFqcn) &&
                    mAttributes.size() == se.mAttributes.size() &&
                    mElements.size() == se.mElements.size() &&
                    mAttributes.equals(se.mAttributes) &&
                    mElements.equals(se.mElements);
        }
        return false;
    }

    @Override
    public int hashCode() {
        long c = mFqcn.hashCode();
        // uses the formula defined in java.util.List.hashCode()
        c = 31*c + mAttributes.hashCode();
        c = 31*c + mElements.hashCode();
        if (mParentFqcn != null) {
            c = 31*c + mParentFqcn.hashCode();
        }
        if (mBounds != null && mBounds.isValid()) {
            c = 31*c + mBounds.hashCode();
        }
        if (mParentBounds != null && mParentBounds.isValid()) {
            c = 31*c + mParentBounds.hashCode();
        }

        if (c > 0x0FFFFFFFFL) {
            // wrap any overflow
            c = c ^ (c >> 32);
        }
        return (int)(c & 0x0FFFFFFFFL);
    }
}

