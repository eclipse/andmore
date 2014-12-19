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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * Main frame.
 */
public class MainFrame extends Composite implements ImageViewer.StatusChangedListener {

    private final StatusPanel mStatusPanel;
    private final ImageEditorPanel mImageEditorPanel;

    public StatusPanel getStatusPanel() {
        return mStatusPanel;
    }

    public ImageEditorPanel getImageEditorPanel() {
        return mImageEditorPanel;
    }

    public MainFrame(Composite parent, int style) {
        super(parent, style);
        setLayout(new FormLayout());

        mStatusPanel = new StatusPanel(this, SWT.NULL);

        FormData bottom = new FormData();
        bottom.bottom = new FormAttachment(100, 0);
        bottom.left = new FormAttachment(0, 0);
        bottom.right = new FormAttachment(100, 0);
        mStatusPanel.setLayoutData(bottom);

        mImageEditorPanel = new ImageEditorPanel(this, SWT.NULL);
        mImageEditorPanel.getImageViewer().setStatusChangedListener(this);

        mStatusPanel.setStatusChangedListener(mImageEditorPanel);

        FormData top = new FormData();
        top.top = new FormAttachment(0);
        top.left = new FormAttachment(0);
        top.right = new FormAttachment(100);
        top.bottom = new FormAttachment(mStatusPanel);
        mImageEditorPanel.setLayoutData(top);

        addKeyListener(mStatusPanel);
        addKeyListener(mImageEditorPanel.getImageViewer());
    }

    @Override
    public void cursorPositionChanged(int x, int y) {
        mStatusPanel.setPosition(x, y);
    }

    @Override
    public void helpTextChanged(String text) {
        mStatusPanel.setHelpText(text);
    }

}
