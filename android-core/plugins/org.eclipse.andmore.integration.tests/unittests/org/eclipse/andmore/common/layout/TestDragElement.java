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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.api.IDragElement;
import com.android.ide.common.api.INode;
import com.android.ide.common.api.Rect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Test/mock implementation of {@link IDragElement} */
public class TestDragElement implements IDragElement {
    private Rect mRect;

    private final String mFqcn;

    private Map<String, TestAttribute> mAttributes = new HashMap<String, TestAttribute>();

    private List<TestDragElement> mChildren = new ArrayList<TestDragElement>();

    private TestDragElement mParent;

    public TestDragElement(String mFqcn, Rect mRect, List<TestDragElement> mChildren,
            TestDragElement mParent) {
        super();
        this.mRect = mRect;
        this.mFqcn = mFqcn;
        this.mChildren = mChildren;
        this.mParent = mParent;
    }

    public TestDragElement(String fqn) {
        this(fqn, null, null, null);
    }

    public TestDragElement setBounds(Rect bounds) {
        this.mRect = bounds;

        return this;
    }

    // Builder stuff
    public TestDragElement set(String uri, String name, String value) {
        if (mAttributes == null) {
            mAttributes = new HashMap<String, TestAttribute>();
        }

        mAttributes.put(uri + name, new TestAttribute(uri, name, value));

        return this;
    }

    public TestDragElement add(TestDragElement... children) {
        if (mChildren == null) {
            mChildren = new ArrayList<TestDragElement>();
        }

        for (TestDragElement child : children) {
            mChildren.add(child);
            child.mParent = this;
        }

        return this;
    }

    public TestDragElement id(String id) {
        return set(ANDROID_URI, ATTR_ID, id);
    }

    public static TestDragElement create(String fqn, Rect bounds) {
        return create(fqn).setBounds(bounds);
    }

    public static TestDragElement create(String fqn) {
        return new TestDragElement(fqn);
    }

    public static IDragElement[] create(TestDragElement... elements) {
        return elements;
    }

    // ==== IDragElement ====

    @Override
    public IDragAttribute getAttribute(@Nullable String uri, @NonNull String localName) {
        if (mAttributes == null) {
            return new TestAttribute(uri, localName, "");
        }

        return mAttributes.get(uri + localName);
    }

    @Override
    public @NonNull IDragAttribute[] getAttributes() {
        return mAttributes.values().toArray(new IDragAttribute[mAttributes.size()]);
    }

    @Override
    public @NonNull Rect getBounds() {
        return mRect;
    }

    @Override
    public @NonNull String getFqcn() {
        return mFqcn;
    }

    @Override
    public @NonNull IDragElement[] getInnerElements() {
        if (mChildren == null) {
            return new IDragElement[0];
        }

        return mChildren.toArray(new IDragElement[mChildren.size()]);
    }

    @Override
    public @NonNull Rect getParentBounds() {
        return mParent != null ? mParent.getBounds() : null;
    }

    @Override
    public String getParentFqcn() {
        return mParent != null ? mParent.getFqcn() : null;
    }

    @Override
    public String toString() {
        return "TestDragElement [fqn=" + mFqcn + ", attributes=" + mAttributes + ", bounds="
                + mRect + "]";
    }

    @Override
    public boolean isSame(INode node) {
        return node.getBounds().equals(getBounds());
    }
}
