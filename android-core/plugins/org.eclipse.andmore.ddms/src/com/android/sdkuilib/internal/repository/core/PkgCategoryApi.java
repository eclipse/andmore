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

import com.android.sdklib.AndroidVersion;


public class PkgCategoryApi extends PkgCategory {

    /** Platform name, in the form "Android 1.2". Can be null if we don't have the name. */
    private String mPlatformName;

    // When sorting by Source, key is the hash of the source's name.
    // When storing by API, key is the AndroidVersion (API level >=1 + optional codename).
    // We always want categories in order tools..platforms..extras; to achieve that tools
    // and extras have the special values so they get "naturally" sorted the way we want
    // them.
    // (Note: don't use integer.max to avoid integers wrapping in comparisons. We can
    // revisit the day we get 2^30 platforms.)
    public final static AndroidVersion KEY_TOOLS = new AndroidVersion(Integer.MAX_VALUE / 2, null);
    public final static AndroidVersion KEY_TOOLS_PREVIEW =
                                               new AndroidVersion(Integer.MAX_VALUE / 2 - 1, null);
    public final static AndroidVersion KEY_EXTRA = new AndroidVersion(-1, null);

    public PkgCategoryApi(AndroidVersion version, String platformName, Object iconRef) {
        super(version, null /*label*/, iconRef);
        setPlatformName(platformName);
    }

    public String getPlatformName() {
        return mPlatformName;
    }

    public void setPlatformName(String platformName) {
        if (platformName != null) {
            // Normal case for actual platform categories
            mPlatformName = String.format("Android %1$s", platformName);
            super.setLabel(null);
        }
    }

    public String getApiLabel() {
        AndroidVersion key = (AndroidVersion) getKey();
        if (key.equals(KEY_TOOLS)) {
            return "TOOLS";             //$NON-NLS-1$ // for internal debug use only
        } else if (key.equals(KEY_TOOLS_PREVIEW)) {
                return "TOOLS-PREVIEW"; //$NON-NLS-1$ // for internal debug use only
        } else if (key.equals(KEY_EXTRA)) {
            return "EXTRAS";            //$NON-NLS-1$ // for internal debug use only
        } else {
            return key.toString();
        }
    }

    @Override
    public String getLabel() {
        String label = super.getLabel();
        if (label == null) {
            AndroidVersion key = (AndroidVersion) getKey();

            if (key.equals(KEY_TOOLS)) {
                label = "Tools";
            } else if (key.equals(KEY_TOOLS_PREVIEW)) {
                label = "Tools (Preview Channel)";
            } else if (key.equals(KEY_EXTRA)) {
                label = "Extras";
            } else {
                if (mPlatformName != null) {
                    label = String.format("%1$s (%2$s)", mPlatformName, getApiLabel());
                } else {
                    label = getApiLabel();
                }
            }
            super.setLabel(label);
        }
        return label;
    }

    @Override
    public void setLabel(String label) {
        throw new UnsupportedOperationException("Use setPlatformName() instead.");
    }

    @Override
    public String toString() {
        return String.format("%s <API=%s, label=%s, #items=%d>",
                this.getClass().getSimpleName(),
                getApiLabel(),
                getLabel(),
                getItems().size());
    }
}
