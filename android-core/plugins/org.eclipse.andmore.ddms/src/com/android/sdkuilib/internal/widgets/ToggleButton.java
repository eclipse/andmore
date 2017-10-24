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
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

/**
 * A label that can display 2 images depending on its internal state.
 * This acts as a button by firing the {@link SWT#Selection} listener.
 */
public class ToggleButton extends CLabel {
    private Image[] mImage = new Image[2];
    private String[] mTooltip = new String[2];
    private boolean mMouseIn;
    private int mState = 0;


    public ToggleButton(
            Composite parent,
            int style,
            Image image1,
            Image image2,
            String tooltip1,
            String tooltip2) {
        super(parent, style);
        mImage[0] = image1;
        mImage[1] = image2;
        mTooltip[0] = tooltip1;
        mTooltip[1] = tooltip2;
        updateImageAndTooltip();

        addMouseListener(new MouseListener() {
            @Override
            public void mouseDown(MouseEvent e) {
                // pass
            }

            @Override
            public void mouseUp(MouseEvent e) {
                // We select on mouse-up, as it should be properly done since this is the
                // only way a user can cancel a button click by moving out of the button.
                if (mMouseIn && e.button == 1) {
                    notifyListeners(SWT.Selection, new Event());
                }
            }

            @Override
            public void mouseDoubleClick(MouseEvent e) {
                if (mMouseIn && e.button == 1) {
                    notifyListeners(SWT.DefaultSelection, new Event());
                }
            }
        });

        addMouseTrackListener(new MouseTrackListener() {
            @Override
            public void mouseExit(MouseEvent e) {
                if (mMouseIn) {
                    mMouseIn = false;
                    redraw();
                }
            }

            @Override
            public void mouseEnter(MouseEvent e) {
                if (!mMouseIn) {
                    mMouseIn = true;
                    redraw();
                }
            }

            @Override
            public void mouseHover(MouseEvent e) {
                // pass
            }
        });
    }

    @Override
    public int getStyle() {
        int style = super.getStyle();
        if (mMouseIn) {
            style |= SWT.SHADOW_IN;
        }
        return style;
    }

    /**
     * Sets current state.
     * @param state A value 0 or 1.
     */
    public void setState(int state) {
        assert state == 0 || state == 1;
        mState = state;
        updateImageAndTooltip();
        redraw();
    }

    /**
     * Returns the current state
     * @return Returns the current state, either 0 or 1.
     */
    public int getState() {
        return mState;
    }

    protected void updateImageAndTooltip() {
        setImage(mImage[getState()]);
        setToolTipText(mTooltip[getState()]);
    }
}

