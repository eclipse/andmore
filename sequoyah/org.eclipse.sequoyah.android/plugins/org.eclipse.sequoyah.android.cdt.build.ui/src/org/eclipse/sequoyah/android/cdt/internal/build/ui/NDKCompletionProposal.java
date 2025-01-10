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

import java.lang.reflect.Constructor;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;

public class NDKCompletionProposal implements IJavaCompletionProposal
{

    private IQuickAssistInvocationContext invocationContext;

    public NDKCompletionProposal(IQuickAssistInvocationContext context)
    {
        this.invocationContext = context;
    }

    public void apply(IDocument document)
    {
        try
        {

            Bundle bundle = Platform.getBundle("org.eclipse.sequoyah.android.cdt.build.ui"); //$NON-NLS-1$
            if (bundle != null)
            {
                Class c =
                        bundle.loadClass("org.eclipse.sequoyah.android.cdt.internal.build.ui.AddNativeWizard"); //$NON-NLS-1$

                Class[] classes =
                {
                        IWorkbenchWindow.class, IProject.class
                };
                Object[] params =
                        {
                                PlatformUI.getWorkbench().getActiveWorkbenchWindow(),
                                ((IInvocationContext) invocationContext).getCompilationUnit()
                                        .getJavaProject().getProject()
                        };

                Constructor construct = c.getConstructor(classes);
                Wizard wizard = (Wizard) construct.newInstance(params);
                WizardDialog dialog =
                        new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                                .getShell(), wizard);
                dialog.open();
            }

        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public Point getSelection(IDocument document)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getAdditionalProposalInfo()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getDisplayString()
    {
        // TODO Auto-generated method stub
        return Messages.NDKCompletionProposal_QuickFixProposal0;
    }

    public Image getImage()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public IContextInformation getContextInformation()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public int getRelevance()
    {
        // TODO Auto-generated method stub
        return 0;
    }

}
