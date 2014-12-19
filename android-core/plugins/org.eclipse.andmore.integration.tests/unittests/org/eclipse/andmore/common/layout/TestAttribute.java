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

import com.android.annotations.NonNull;
import com.android.ide.common.api.IDragElement.IDragAttribute;
import com.android.ide.common.api.INode.IAttribute;
import com.android.ide.common.xml.XmlAttributeSortOrder;

/** Test/mock implementation of {@link IAttribute} and {@link IDragAttribute} */
public class TestAttribute implements IAttribute, IDragAttribute {
    private String mUri;

    private String mName;

    private String mValue;

    public TestAttribute(String mUri, String mName, String mValue) {
        super();
        this.mName = mName;
        this.mUri = mUri;
        this.mValue = mValue;
    }

    @Override
    public @NonNull String getName() {
        return mName;
    }

    @Override
    public @NonNull String getUri() {
        return mUri;
    }

    @Override
    public @NonNull String getValue() {
        return mValue;
    }

    @Override
    public String toString() {
        return "TestAttribute [name=" + mName + ", uri=" + mUri + ", value=" + mValue + "]";
    }

    public int compareTo(IDragAttribute o) {
        return XmlAttributeSortOrder.compareAttributes(mName, o.getName());
    }
}