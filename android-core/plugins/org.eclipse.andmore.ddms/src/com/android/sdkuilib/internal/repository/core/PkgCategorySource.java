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

import com.android.sdklib.internal.repository.sources.SdkRepoSource;
import com.android.sdklib.internal.repository.sources.SdkSource;
import com.android.sdkuilib.internal.repository.SwtUpdaterData;
import com.android.sdkuilib.internal.repository.ui.PackagesPageIcons;


public class PkgCategorySource extends PkgCategory {

    /**
     * A special {@link SdkSource} object that represents the locally installed
     * items, or more exactly a lack of remote source.
     */
    public final static SdkSource UNKNOWN_SOURCE =
        new SdkRepoSource("http://no.source", "Local Packages");
    private final SdkSource mSource;

    /**
     * Creates a new {@link PkgCategorySource}.
     * This uses {@link SdkSource#toString()} to get the source's description.
     * Note that if the name of the source isn't known, the description will use its URL.
     */
    public PkgCategorySource(SdkSource source, SwtUpdaterData swtUpdaterData) {
        super(
            source, // the source is the key and it can be null
            source == UNKNOWN_SOURCE ? "Local Packages" : source.toString(),
            source == UNKNOWN_SOURCE ?
                swtUpdaterData.getImageFactory()
                              .getImageByName(PackagesPageIcons.ICON_PKG_INSTALLED) :
                source);
        mSource = source;
    }

    @Override
    public String toString() {
        return String.format("%s <source=%s, #items=%d>",
                this.getClass().getSimpleName(),
                mSource.toString(),
                getItems().size());
    }

    public SdkSource getSource() {
        return mSource;
    }

    /** Sets the label to match the source's UI name if the label wasn't already set. */
    public void adjustLabel(SdkSource source) {
        if (getLabel() == null || getLabel().startsWith("http")) {  //$NON-NLS-1$
            setLabel(source == UNKNOWN_SOURCE ? "Local Packages" : source.toString());
        }
    }

}
