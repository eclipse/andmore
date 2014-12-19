/*
 * Copyright (C) 2012 The Android Open Source Project
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

import static com.android.SdkConstants.ATTR_ID;

import org.eclipse.andmore.common.layout.BaseLayoutRule;
import org.eclipse.andmore.common.layout.BaseViewRule;
import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.editors.layout.uimodel.UiViewElementNode;
import org.eclipse.core.resources.IMarker;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import java.util.List;

/** Actual tooltip showing multiple lines for various widgets that have lint errors */
class LintTooltip extends Shell {
    private final LayoutCanvas mCanvas;
    private final List<UiViewElementNode> mNodes;

    LintTooltip(LayoutCanvas canvas, List<UiViewElementNode> nodes) {
        super(canvas.getDisplay(), SWT.ON_TOP | SWT.NO_FOCUS | SWT.TOOL);
        mCanvas = canvas;
        mNodes = nodes;

        createContents();
    }

    protected void createContents() {
        Display display = getDisplay();
        Color fg = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);
        Color bg = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
        setBackground(bg);
        GridLayout gridLayout = new GridLayout(2, false);
        setLayout(gridLayout);

        LayoutEditorDelegate delegate = mCanvas.getEditorDelegate();

        boolean first = true;
        for (UiViewElementNode node : mNodes) {
            IMarker marker = delegate.getIssueForNode(node);
            if (marker != null) {
                String message = marker.getAttribute(IMarker.MESSAGE, null);
                if (message != null) {
                    Label icon = new Label(this, SWT.NONE);
                    icon.setForeground(fg);
                    icon.setBackground(bg);
                    icon.setImage(node.getIcon());

                    Label label = new Label(this, SWT.WRAP);
                    if (first) {
                        label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
                        first = false;
                    }

                    String id = BaseViewRule.stripIdPrefix(node.getAttributeValue(ATTR_ID));
                    if (id.isEmpty()) {
                        if (node.getXmlNode() != null) {
                            id = node.getXmlNode().getNodeName();
                        } else {
                            id = node.getDescriptor().getUiName();
                        }
                    }

                    label.setText(String.format("%1$s: %2$s", id, message));
                }
            }
        }
    }

    @Override
    protected void checkSubclass() {
        // Disable the check that prevents subclassing of SWT components
    }
}
