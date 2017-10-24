/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.sdkuilib.internal.repository.core;


import com.android.sdklib.internal.repository.updater.PkgItem;

import java.util.ArrayList;
import java.util.List;

public abstract class PkgCategory {
    private final Object mKey;
    private final Object mIconRef;
    private final List<PkgItem> mItems = new ArrayList<PkgItem>();
    private String mLabel;

    public PkgCategory(Object key, String label, Object iconRef) {
        mKey = key;
        mLabel = label;
        mIconRef = iconRef;
    }

    public Object getKey() {
        return mKey;
    }

    public String getLabel() {
        return mLabel;
    }

    public void setLabel(String label) {
        mLabel = label;
    }

    public Object getIconRef() {
        return mIconRef;
    }

    public List<PkgItem> getItems() {
        return mItems;
    }

    @Override
    public String toString() {
        return String.format("%s <key=%s, label=%s, #items=%d>",
                this.getClass().getSimpleName(),
                mKey == null ? "null" : mKey.toString(),
                mLabel,
                mItems.size());
    }

    /** {@link PkgCategory}s are equal if their internal keys are equal. */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mKey == null) ? 0 : mKey.hashCode());
        return result;
    }

    /** {@link PkgCategory}s are equal if their internal keys are equal. */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        PkgCategory other = (PkgCategory) obj;
        if (mKey == null) {
            if (other.mKey != null) return false;
        } else if (!mKey.equals(other.mKey)) return false;
        return true;
    }
}
