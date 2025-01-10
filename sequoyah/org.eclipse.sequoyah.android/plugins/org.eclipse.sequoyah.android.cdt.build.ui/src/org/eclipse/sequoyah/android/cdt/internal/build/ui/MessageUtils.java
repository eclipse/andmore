/*******************************************************************************
 * Copyright (c) 2010 Motorola, Inc. All rights reserved.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial contributors:
 * Carlos Alberto Souto Junior (Eldorado)
 *******************************************************************************/

package org.eclipse.sequoyah.android.cdt.internal.build.ui;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class MessageUtils

{

    public static void showErrorDialog(final String title, final String message)
    {
        Display.getDefault().asyncExec(new Runnable()
        {
            public void run()
            {
                IWorkbenchWindow ww = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                MessageDialog.openError(ww.getShell(), title, message);
            }
        });
    }

    public static void showInformationDialog(final String title, final String message)
    {
        Display.getDefault().asyncExec(new Runnable()
        {
            public void run()
            {
                IWorkbench workbench = PlatformUI.getWorkbench();
                IWorkbenchWindow ww = workbench.getActiveWorkbenchWindow();
                Shell shell = ww.getShell();
                MessageDialog.openInformation(shell, title, message);
            }
        });
    }
}
