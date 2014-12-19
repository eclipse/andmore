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
package org.eclipse.andmore.internal.build;

import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;

import java.net.URL;

/**
 * Dialog shown by the {@link ConvertSwitchQuickFixProcessor}. This is a custom
 * dialog rather than a plain {@link MessageDialog} such that we can show a link
 * and point to a web page for more info.
 */
class ConvertSwitchDialog extends TitleAreaDialog implements SelectionListener {
    /** URL containing more info */
    private static final String URL = "http://tools.android.com/tips/non-constant-fields"; //$NON-NLS-1$

    private final String mField;

    private Link mLink;

    /**
     * Create the dialog.
     * @param parentShell the parent shell
     * @param field the field name we're warning about
     */
    public ConvertSwitchDialog(Shell parentShell, String field) {
        super(parentShell);
        mField = field;
        Image image = IconFactory.getInstance().getIcon("android-64"); //$NON-NLS-1$
        setTitleImage(image);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        String text = String.format(
            "As of ADT 14, the resource fields (such as %1$s) are no longer constants " +
            "when defined in library projects. This is necessary to make library " +
            "projects reusable without recompiling them.\n" +
            "\n" +
            "One consequence of this is that you can no longer use the fields directly " +
            "in switch statements. You must use an if-else chain instead.\n" +
            "\n" +
            "Eclipse can automatically convert from a switch statement to an if-else " +
            "statement. Just place the caret on the switch keyword and invoke " +
            "Quick Fix (Ctrl-1 on Windows and Linux, Cmd-1 on Mac), then select " +
            "\"Convert 'switch' to 'if-else'\".\n" +
            "\n" +
            "For more information, see <a href=\"" + URL + "\">" + URL + "</a>",
            mField);

        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayout(new GridLayout(1, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        mLink = new Link(container, SWT.NONE);
        mLink.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, true, 1, 1));
        mLink.setText(text);
        mLink.addSelectionListener(this);

        setMessage("Non-Constant Expressions: Migration Necessary", IMessageProvider.INFORMATION);

        return area;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.HELP_ID, IDialogConstants.HELP_LABEL, false);
    }

    @Override
    protected Point getInitialSize() {
        return new Point(500, 400);
    }

    private void showWebPage() {
        try {
            IWorkbench workbench = PlatformUI.getWorkbench();
            IWebBrowser browser = workbench.getBrowserSupport().getExternalBrowser();
            browser.openURL(new URL(URL));
        } catch (Exception e) {
            String message = String.format("Could not open browser. Vist\n%1$s\ninstead.",
                    URL);
            MessageDialog.openError(getShell(), "Browser Error", message);
        }

    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.HELP_ID) {
            showWebPage();
        } else {
            super.buttonPressed(buttonId);
        }
    }

    // ---- Implements SelectionListener ----

    @Override
    public void widgetSelected(SelectionEvent e) {
        if (e.getSource() == mLink) {
            showWebPage();
        }
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
    }
}
