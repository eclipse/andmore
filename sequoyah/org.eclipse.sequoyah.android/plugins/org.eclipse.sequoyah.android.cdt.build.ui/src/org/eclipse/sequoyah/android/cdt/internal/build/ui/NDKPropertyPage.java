/*******************************************************************************
 * Copyright (c) 2010 Motorola, Inc. All rights reserved.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial contributors:
 * Carlos Alberto Souto Junior (Eldorado)
 * 
 * Contributors:
 * Carlos Alberto Souto Junior (Eldorado) - [317327] Major UI bugfixes and improvements in Android Native support
 *******************************************************************************/

package org.eclipse.sequoyah.android.cdt.internal.build.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.sequoyah.android.cdt.build.core.INDKService;
import org.eclipse.sequoyah.android.cdt.build.core.NDKUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * DESCRIPTION: 
 * This class represents the properties page for Android NDK.
 * It gives the user the option to set gcc version, platform, 
 * source object and library paths.
 * @author Carlos Alberto Souto Junior
 * */
public class NDKPropertyPage extends PropertyPage implements IWorkbenchPropertyPage
{

    //UI fields
    private Text libraryTxt;

    //selected project
    private IProject project = null;

    @Override
    protected Control createContents(Composite parent)
    {
        IProjectNature nature = null;
        IAdaptable apt = getElement();
        //get selected project
        if (apt instanceof IJavaProject)
        {
            IJavaProject resource = (IJavaProject) getElement();
            project = resource.getProject();
        }
        else if (apt instanceof IProject)
        {
            project = (IProject) apt;
        }

        try
        {
            nature = project.getNature("org.eclipse.cdt.core.cnature");
        }
        catch (CoreException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Composite main = new Composite(parent, SWT.FILL);
        main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        main.setLayout(new GridLayout(1, false));

        Composite auxComp = new Composite(main, SWT.FILL);
        auxComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        auxComp.setLayout(new GridLayout(6, true));

        //platform fields
        Label libraryLabel = new Label(auxComp, SWT.FILL);
        libraryLabel.setText("Native Library Name");
        libraryLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        libraryTxt = new Text(auxComp, SWT.FILL | SWT.BORDER);
        libraryTxt.setEditable(true);
        libraryTxt.setText(getInitialValue(INDKService.libName));
        libraryTxt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        if (nature == null)
        {
            libraryTxt.setEnabled(false);
        }
        else
        {
            libraryTxt.setEnabled(true);
        }

        
        return main;
    }

    /**initialize properties values
     * 
     * @param propertyId
     * @return
     */
    private String getInitialValue(QualifiedName propertyId)
    {
        String returnValue = "";
        try
        {
            if (project.getPersistentProperty(propertyId) != null)
            {
                returnValue = project.getPersistentProperty(propertyId);
            }
        }
        catch (CoreException e1)
        {
            returnValue = "";
        }
        return returnValue;
    }

    @Override
    /**called when apply button is pressed
     * 
     */
    public void performApply()
    {
        try
        {
            // Check if the value was changed and generate a new Android make file accordingly
            if ((project.getPersistentProperty(INDKService.libName) == null)
                    || !project.getPersistentProperty(INDKService.libName).equals(
                            libraryTxt.getText().trim()))
            {
                project.setPersistentProperty(INDKService.libName, libraryTxt.getText().trim());
                // Generate a new makefile
                NDKUtils.generateAndroidMakeFile(project, libraryTxt.getText().trim());
            }
        }
        catch (CoreException e1)
        {

        }
    }

    @Override
    /**called when ok button is pressed
     * 
     */
    public boolean performOk()
    {

        performApply();

        return super.performOk();
    }
}
