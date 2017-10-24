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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Control;

/**
 * A little helper to create a new {@link GridData} and set its properties.
 * <p/>
 * Example of usage: <br/>
 * <code>
 *   GridDataHelper.create(myControl).hSpan(2).hAlignCenter().fill();
 * </code>
 */
public final class GridDataBuilder {

    private GridData mGD;

    private GridDataBuilder() {
        mGD = new GridData();
    }

    /**
     * Creates new {@link GridData} and associates it on the <code>control</code> composite.
     */
    static public GridDataBuilder create(Control control) {
        GridDataBuilder gdh = new GridDataBuilder();
        control.setLayoutData(gdh.mGD);
        return gdh;
    }

    /** Sets <code>widthHint</code> to <code>w</code>. */
    public GridDataBuilder wHint(int w) {
        mGD.widthHint = w;
        return this;
    }

    /** Sets <code>heightHint</code> to <code>h</code>. */
    public GridDataBuilder hHint(int h) {
        mGD.heightHint = h;
        return this;
    }

    /** Sets <code>horizontalIndent</code> to <code>h</code>. */
    public GridDataBuilder hIndent(int h) {
        mGD.horizontalIndent = h;
        return this;
    }

    /** Sets <code>horizontalSpan</code> to <code>h</code>. */
    public GridDataBuilder hSpan(int h) {
        mGD.horizontalSpan = h;
        return this;
    }

    /** Sets <code>verticalSpan</code> to <code>v</code>. */
    public GridDataBuilder vSpan(int v) {
        mGD.verticalSpan = v;
        return this;
    }

    /** Sets <code>horizontalAlignment</code> to {@link SWT#CENTER}. */
    public GridDataBuilder hCenter() {
        mGD.horizontalAlignment = SWT.CENTER;
        return this;
    }

    /** Sets <code>verticalAlignment</code> to {@link SWT#CENTER}. */
    public GridDataBuilder vCenter() {
        mGD.verticalAlignment = SWT.CENTER;
        return this;
    }

    /** Sets <code>verticalAlignment</code> to {@link SWT#TOP}. */
    public GridDataBuilder vTop() {
        mGD.verticalAlignment = SWT.TOP;
        return this;
    }

    /** Sets <code>verticalAlignment</code> to {@link SWT#BOTTOM}. */
    public GridDataBuilder vBottom() {
        mGD.verticalAlignment = SWT.BOTTOM;
        return this;
    }

    /** Sets <code>horizontalAlignment</code> to {@link SWT#LEFT}. */
    public GridDataBuilder hLeft() {
        mGD.horizontalAlignment = SWT.LEFT;
        return this;
    }

    /** Sets <code>horizontalAlignment</code> to {@link SWT#RIGHT}. */
    public GridDataBuilder hRight() {
        mGD.horizontalAlignment = SWT.RIGHT;
        return this;
    }

    /** Sets <code>horizontalAlignment</code> to {@link GridData#FILL}. */
    public GridDataBuilder hFill() {
        mGD.horizontalAlignment = GridData.FILL;
        return this;
    }

    /** Sets <code>verticalAlignment</code> to {@link GridData#FILL}. */
    public GridDataBuilder vFill() {
        mGD.verticalAlignment = GridData.FILL;
        return this;
    }

    /**
     * Sets both <code>horizontalAlignment</code> and <code>verticalAlignment</code>
     * to {@link GridData#FILL}.
     */
    public GridDataBuilder fill() {
        mGD.horizontalAlignment = GridData.FILL;
        mGD.verticalAlignment = GridData.FILL;
        return this;
    }

    /** Sets <code>grabExcessHorizontalSpace</code> to true. */
    public GridDataBuilder hGrab() {
        mGD.grabExcessHorizontalSpace = true;
        return this;
    }

    /** Sets <code>grabExcessVerticalSpace</code> to true. */
    public GridDataBuilder vGrab() {
        mGD.grabExcessVerticalSpace = true;
        return this;
    }

    /**
     * Sets both <code>grabExcessHorizontalSpace</code> and
     * <code>grabExcessVerticalSpace</code> to true.
     */
    public GridDataBuilder grab() {
        mGD.grabExcessHorizontalSpace = true;
        mGD.grabExcessVerticalSpace = true;
        return this;
    }

}
