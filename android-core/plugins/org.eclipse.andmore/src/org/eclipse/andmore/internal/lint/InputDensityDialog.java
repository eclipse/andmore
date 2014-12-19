/*
 * Copyright (C) 2011 The Android Open Source Project
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

import com.android.resources.Density;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import java.util.ArrayList;
import java.util.List;

class InputDensityDialog extends Dialog {
    private Combo mCombo;
    /**
     * Density value being chosen - static to keep most recently chosen value
     * across repeated invocations
     */
    private static int sDpi = Density.DEFAULT_DENSITY;

    InputDensityDialog(Shell parentShell) {
        super(parentShell);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout(1, false));

        Label lblWhatIsThe = new Label(container, SWT.WRAP);
        lblWhatIsThe.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
        lblWhatIsThe.setText("What is the screen density the current px value works with?");

        mCombo = new Combo(container, SWT.READ_ONLY);
        GridData gdCombo = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
        gdCombo.widthHint = 200;
        mCombo.setLayoutData(gdCombo);
        int initialIndex = 0;
        List<String> s = new ArrayList<String>();
        int index = 0;
        for (Density density : Density.values()) {
            if (density == Density.NODPI) {
                continue;
            }
            if (density.getDpiValue() == sDpi) {
                initialIndex = index;
            }
            s.add(getLabel(density));
            index++;
        }
        String[] items = s.toArray(new String[s.size()]);
        mCombo.setItems(items);
        mCombo.select(initialIndex);

        return container;
    }

    private static String getLabel(Density density) {
        return String.format("%1$s (%2$d)", density.getShortDisplayValue(), density.getDpiValue());
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected Point getInitialSize() {
        return new Point(450, 150);
    }

    @Override
    public boolean close() {
        String description = mCombo.getItem(mCombo.getSelectionIndex());

        for (Density density : Density.values()) {
            if (description.equals(getLabel(density))) {
                sDpi = density.getDpiValue();
                break;
            }
        }

        return super.close();
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Choose Density");
    }

    int getDensity() {
        return sDpi;
    }
}
