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

package com.android.sdkuilib.internal.widgets;


import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

/**
 * A label that can display 2 images depending on its enabled/disabled state.
 * This acts as a button by firing the {@link SWT#Selection} listener.
 */
public class ImgDisabledButton extends ToggleButton {
    public ImgDisabledButton(
            Composite parent,
            int style,
            Image imageEnabled,
            Image imageDisabled,
            String tooltipEnabled,
            String tooltipDisabled) {
        super(parent,
                style,
                imageEnabled,
                imageDisabled,
                tooltipEnabled,
                tooltipDisabled);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        updateImageAndTooltip();
        redraw();
    }

    @Override
    public void setState(int state) {
        throw new UnsupportedOperationException(); // not available for this type of button
    }

    @Override
    public int getState() {
        return (isDisposed() || !isEnabled()) ? 1 : 0;
    }
}
