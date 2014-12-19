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

import java.util.List;

/**
 * Information for the current alternate selection, i.e. the possible selected items
 * that are located at the same x/y as the original view, either sibling or parents.
 */
/* package */ class CanvasAlternateSelection {
    private final CanvasViewInfo mOriginatingView;
    private final List<CanvasViewInfo> mAltViews;
    private int mIndex;

    /**
     * Creates a new alternate selection based on the given originating view and the
     * given list of alternate views. Both cannot be null.
     */
    public CanvasAlternateSelection(CanvasViewInfo originatingView, List<CanvasViewInfo> altViews) {
        assert originatingView != null;
        assert altViews != null;
        mOriginatingView = originatingView;
        mAltViews = altViews;
        mIndex = altViews.size() - 1;
    }

    /** Returns the list of alternate views. Cannot be null. */
    public List<CanvasViewInfo> getAltViews() {
        return mAltViews;
    }

    /** Returns the originating view. Cannot be null. */
    public CanvasViewInfo getOriginatingView() {
        return mOriginatingView;
    }

    /**
     * Returns the current alternate view to select.
     * Initially this is the top-most view.
     */
    public CanvasViewInfo getCurrent() {
        return mIndex >= 0 ? mAltViews.get(mIndex) : null;
    }

    /**
     * Changes the current view to be the next one and then returns it.
     * This loops through the alternate views.
     */
    public CanvasViewInfo getNext() {
        if (mIndex == 0) {
            mIndex = mAltViews.size() - 1;
        } else if (mIndex > 0) {
            mIndex--;
        }

        return getCurrent();
    }
}
