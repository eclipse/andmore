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
package org.eclipse.andmore.internal.lint;

import static com.android.SdkConstants.UNIT_PX;
import static com.android.SdkConstants.VALUE_N_DP;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("restriction") // DOM model
final class ConvertToDpFix extends DocumentFix implements IInputValidator {
    private ConvertToDpFix(String id, IMarker marker) {
        super(id, marker);
    }

    @Override
    public boolean needsFocus() {
        return false;
    }

    @Override
    public boolean isCancelable() {
        return true;
    }

    @Override
    protected void apply(IDocument document, IStructuredModel model, Node node, int start,
            int end) {
        Shell shell = AdtPlugin.getShell();
        InputDensityDialog densityDialog = new InputDensityDialog(shell);
        if (densityDialog.open() == Window.OK) {
            int dpi = densityDialog.getDensity();
            Element element = (Element) node;
            Pattern pattern = Pattern.compile("(\\d+)px"); //$NON-NLS-1$
            NamedNodeMap attributes = element.getAttributes();
            for (int i = 0, n = attributes.getLength(); i < n; i++) {
                Attr attribute = (Attr) attributes.item(i);
                String value = attribute.getValue();
                if (value.endsWith(UNIT_PX)) {
                    Matcher matcher = pattern.matcher(value);
                    if (matcher.matches()) {
                        String numberString = matcher.group(1);
                        try {
                            int px = Integer.parseInt(numberString);
                            int dp = px * 160 / dpi;
                            String newValue = String.format(VALUE_N_DP, dp);
                            attribute.setNodeValue(newValue);
                        } catch (NumberFormatException nufe) {
                            AdtPlugin.log(nufe, null);
                        }
                    }
                }
            }
        }
    }

    @Override
    public String getDisplayString() {
        return "Convert to \"dp\"...";
    }

    @Override
    public Image getImage() {
        return AdtPlugin.getAndroidLogo();
    }

    // ---- Implements IInputValidator ----

    @Override
    public String isValid(String input) {
        if (input == null || input.length() == 0)
            return " "; //$NON-NLS-1$

        try {
            int i = Integer.parseInt(input);
            if (i <= 0 || i > 1000) {
                return "Invalid range";
            }
        } catch (NumberFormatException x) {
            return "Enter a valid number";
        }

        return null;
    }
}
