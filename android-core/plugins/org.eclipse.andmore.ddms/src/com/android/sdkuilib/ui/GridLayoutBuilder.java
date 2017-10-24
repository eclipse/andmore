/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.sdkuilib.ui;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * A little helper to create a new {@link GridLayout}, associate to a {@link Composite}
 * and set its common attributes.
 * <p/>
 * Example of usage: <br/>
 * <code>
 *    GridLayoutHelper.create(myComposite).noMargins().vSpacing(0).columns(2);
 * </code>
 */
public final class GridLayoutBuilder {

    private GridLayout mGL;

    private GridLayoutBuilder() {
        mGL = new GridLayout();
    }

    /**
     * Creates new {@link GridLayout} and associates it on the <code>parent</code> composite.
     */
    static public GridLayoutBuilder create(Composite parent) {
        GridLayoutBuilder glh = new GridLayoutBuilder();
        parent.setLayout(glh.mGL);
        return glh;
    }

    /** Sets all margins to 0. */
    public GridLayoutBuilder noMargins() {
        mGL.marginHeight = 0;
        mGL.marginWidth = 0;
        mGL.marginLeft = 0;
        mGL.marginTop = 0;
        mGL.marginRight = 0;
        mGL.marginBottom = 0;
        return this;
    }

    /** Sets all margins to <code>n</code>. */
    public GridLayoutBuilder margins(int n) {
        mGL.marginHeight = n;
        mGL.marginWidth = n;
        mGL.marginLeft = n;
        mGL.marginTop = n;
        mGL.marginRight = n;
        mGL.marginBottom = n;
        return this;
    }

    /** Sets <code>numColumns</code> to <code>n</code>. */
    public GridLayoutBuilder columns(int n) {
        mGL.numColumns = n;
        return this;
    }

    /** Sets <code>makeColumnsEqualWidth</code> to true. */
    public GridLayoutBuilder columnsEqual() {
        mGL.makeColumnsEqualWidth = true;
        return this;
    }

    /** Sets <code>verticalSpacing</code> to <code>v</code>. */
    public GridLayoutBuilder vSpacing(int v) {
        mGL.verticalSpacing = v;
        return this;
    }

    /** Sets <code>horizontalSpacing</code> to <code>h</code>. */
    public GridLayoutBuilder hSpacing(int h) {
        mGL.horizontalSpacing = h;
        return this;
    }

    /**
     * Sets <code>horizontalSpacing</code> and <code>verticalSpacing</code>
     * to <code>s</code>.
     */
    public GridLayoutBuilder spacing(int s) {
        mGL.verticalSpacing = s;
        mGL.horizontalSpacing = s;
        return this;
    }
}
