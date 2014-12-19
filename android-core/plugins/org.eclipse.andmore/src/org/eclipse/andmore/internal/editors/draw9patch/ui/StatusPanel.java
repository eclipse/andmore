/*
 * Copyright (C) 2013 The Android Open Source Project
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

package org.eclipse.andmore.internal.editors.draw9patch.ui;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;

/**
 * Status and control pane.
 */
public class StatusPanel extends Composite implements KeyListener {

    public static final int SCALE_MIN = 2;
    public static final int SCALE_MAX = 6;

    public static final int ZOOM_MIN = 100;
    public static final int ZOOM_MAX = 800;

    public static final int PADDING_TOP = 12;
    public static final int PADDING_RIGHT = 0;
    public static final int PADDING_BOTTOM = 5;
    public static final int PADDING_LEFT = 10;

    public static final int MIN_WIDTH = 800;

    private Button mShowLock = null;
    private Button mShowPatches = null;
    private Button mShowBadPatches = null;
    private Button mShowContent = null;

    private Label mHelpLabel = null;

    private Label mXPosLabel = null;
    private Label mYPosLabel = null;

    private ZoomControl mZoomControl = null;
    private ZoomControl mScaleControl = null;

    private StatusChangedListener mListener = null;

    public void setStatusChangedListener(StatusChangedListener l) {
        mListener = l;
    }

    public void setHelpText(String text) {
        Point size = getSize();
        // check window width
        if (MIN_WIDTH < size.x) {
            mHelpLabel.setText(text);
            mHelpLabel.setVisible(true);
        } else {
            mHelpLabel.setText("N/A");
            mHelpLabel.setVisible(false);
        }
    }

    /**
     * Set mouse cursor position.
     */
    public void setPosition(int x, int y) {
        mXPosLabel.setText(String.format("X:  %4d px", x));
        mYPosLabel.setText(String.format("Y:  %4d px", y));
    }

    public StatusPanel(Composite parent, int style) {
        super(parent, style);
        setLayout(new FormLayout());

        final Composite container = new Composite(this, SWT.NULL);
        container.setLayout(new FormLayout());

        FormData innerForm = new FormData();
        innerForm.left = new FormAttachment(0, PADDING_LEFT);
        innerForm.top = new FormAttachment(0, PADDING_TOP);
        innerForm.right = new FormAttachment(100, PADDING_RIGHT);
        innerForm.bottom = new FormAttachment(100, -PADDING_BOTTOM);
        container.setLayoutData(innerForm);

        buildPosition(container);

        Composite zoomPanels = new Composite(container, SWT.NULL);
        zoomPanels.setLayout(new GridLayout(3, false));

        buildZoomControl(zoomPanels);
        buildScaleControl(zoomPanels);

        Composite checkPanel = new Composite(container, SWT.NULL);
        checkPanel.setLayout(new GridLayout(2, false));
        FormData checkPanelForm = new FormData();
        checkPanelForm.left = new FormAttachment(zoomPanels, 0);
        checkPanelForm.bottom = new FormAttachment(100, -PADDING_BOTTOM);
        checkPanel.setLayoutData(checkPanelForm);

        buildCheckboxes(checkPanel);

        mHelpLabel = new Label(container, SWT.BORDER_SOLID | SWT.BOLD | SWT.WRAP);
        mHelpLabel.setBackground(new Color(AdtPlugin.getDisplay(), 0xFF, 0xFF, 0xFF));
        FormData hintForm = new FormData();
        hintForm.left = new FormAttachment(checkPanel, 5);
        hintForm.right = new FormAttachment(mXPosLabel, -10);
        hintForm.top = new FormAttachment(PADDING_TOP);
        hintForm.bottom = new FormAttachment(100, -PADDING_BOTTOM);
        mHelpLabel.setLayoutData(hintForm);

        /*
         * If the window width is not much, the "help label" will break the window.
         * Because that is wrapped automatically.
         *
         * This listener catch resized events and reset help text.
         *
         * setHelpText method checks window width.
         * If window is too narrow, help text will be set invisible.
         */
        container.addControlListener(new ControlListener() {
            @Override
            public void controlResized(ControlEvent event) {
                // reset text
                setHelpText(ImageViewer.HELP_MESSAGE_KEY_TIPS);
            }
            @Override
            public void controlMoved(ControlEvent event) {
            }
        });

    }

    private void buildPosition(Composite parent) {
        mXPosLabel = new Label(parent, SWT.NULL);
        mYPosLabel = new Label(parent, SWT.NULL);

        mXPosLabel.setText(String.format("X:  %4d px", 1000));
        mYPosLabel.setText(String.format("Y:  %4d px", 1000));

        FormData bottomRight = new FormData();
        bottomRight.bottom = new FormAttachment(100, 0);
        bottomRight.right = new FormAttachment(100, 0);
        mYPosLabel.setLayoutData(bottomRight);

        FormData aboveYPosLabel = new FormData();
        aboveYPosLabel.bottom = new FormAttachment(mYPosLabel);
        aboveYPosLabel.right = new FormAttachment(100, 0);
        mXPosLabel.setLayoutData(aboveYPosLabel);
    }

    private void buildScaleControl(Composite parent) {
        mScaleControl = new ZoomControl(parent);
        mScaleControl.maxLabel.setText("6x");
        mScaleControl.minLabel.setText("2x");
        mScaleControl.scale.setMinimum(SCALE_MIN);
        mScaleControl.scale.setMaximum(SCALE_MAX);
        mScaleControl.scale.setSelection(2);
        mScaleControl.scale.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                super.widgetSelected(event);
                if (mListener != null) {
                    Scale scale = (Scale) event.widget;
                    mListener.scaleChanged(scale.getSelection());
                }
            }
        });
    }

    private void buildZoomControl(Composite parent) {
        mZoomControl = new ZoomControl(parent);
        mZoomControl.maxLabel.setText("800%");
        mZoomControl.minLabel.setText("100%");
        mZoomControl.scale.setMinimum(ZOOM_MIN);
        mZoomControl.scale.setMaximum(ZOOM_MAX - ZOOM_MIN);
        mZoomControl.scale.setSelection(400);
        mZoomControl.scale.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                super.widgetSelected(event);
                if (mListener != null) {
                    Scale scale = (Scale) event.widget;
                    mListener.zoomChanged(scale.getSelection() + ZOOM_MIN);
                }
            }
        });

    }

    private void buildCheckboxes(Composite parent) {
        // check lock
        mShowLock = new Button(parent, SWT.CHECK);
        mShowLock.setText("show Lock");
        mShowLock.setSelection(true);
        mShowLock.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                super.widgetSelected(event);
                if (mListener != null) {
                    mListener.lockVisibilityChanged(mShowLock.getSelection());
                }
            }
        });

        // check patches
        mShowPatches = new Button(parent, SWT.CHECK);
        mShowPatches.setText("show Patches");
        mShowPatches.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                super.widgetSelected(event);
                if (mListener != null) {
                    mListener.patchesVisibilityChanged(mShowPatches.getSelection());
                }
            }
        });

        // check patches
        mShowBadPatches = new Button(parent, SWT.CHECK);
        mShowBadPatches.setText("show Bad patches");
        mShowBadPatches.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                super.widgetSelected(event);
                if (mListener != null) {
                    mListener.badPatchesVisibilityChanged(mShowBadPatches.getSelection());
                }
            }
        });

        // check contents(padding)
        mShowContent = new Button(parent, SWT.CHECK);
        mShowContent.setText("show Contents");
        mShowContent.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                super.widgetSelected(event);
                if (mListener != null) {
                    mListener.contentAreaVisibilityChanged(mShowContent.getSelection());
                }
            }
        });
    }

    @Override
    public void keyPressed(KeyEvent event) {
        switch (event.character) {
            case 'c':
                mShowContent.setSelection(!mShowContent.getSelection());
                if (mListener != null) {
                    mListener.contentAreaVisibilityChanged(mShowContent.getSelection());
                }
                break;
            case 'l':
                mShowLock.setSelection(!mShowLock.getSelection());
                if (mListener != null) {
                    mListener.lockVisibilityChanged(mShowLock.getSelection());
                }
                break;
            case 'p':
                mShowPatches.setSelection(!mShowPatches.getSelection());
                if (mListener != null) {
                    mListener.patchesVisibilityChanged(mShowPatches.getSelection());
                }
                break;
            case 'b':
                mShowBadPatches.setSelection(!mShowBadPatches.getSelection());
                if (mListener != null) {
                    mListener.badPatchesVisibilityChanged(mShowBadPatches.getSelection());
                }
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent event) {
    }

    private static class ZoomControl {

        private Label minLabel;
        private Label maxLabel;
        Scale scale;

        public ZoomControl(Composite composite) {
            minLabel = new Label(composite, SWT.RIGHT);
            scale = new Scale(composite, SWT.HORIZONTAL);
            maxLabel = new Label(composite, SWT.LEFT);
        }
    }

    /**
     * Status changed events listener.
     */
    public interface StatusChangedListener {
        /**
         * Zoom level has been changed.
         * @param zoom
         */
        public void zoomChanged(int zoom);

        /**
         * Scale has been changed.
         * @param scale
         */
        public void scaleChanged(int scale);

        /**
         * Lock visibility has been changed.
         * @param visible
         */
        public void lockVisibilityChanged(boolean visible);

        /**
         * Patches visibility has been changed.
         * @param visible
         */
        public void patchesVisibilityChanged(boolean visible);

        /**
         * BadPatches visibility has been changed.
         * @param visible
         */
        public void badPatchesVisibilityChanged(boolean visible);

        /**
         * Content visibility has been changed.
         * @param visible
         */
        public void contentAreaVisibilityChanged(boolean visible);
    }
}
