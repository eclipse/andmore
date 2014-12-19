/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.andmore.internal.wizards.newproject;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.workingsets.IWorkingSetIDs;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.dialogs.WorkingSetConfigurationBlock;

/**
 * Copied from
 * org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageOne$WorkingSetGroup
 *
 * Creates the working set group with controls that allow
 * the selection of working sets
 */
@SuppressWarnings("restriction")
public class WorkingSetGroup {

    private WorkingSetConfigurationBlock fWorkingSetBlock;
    private Button mEnableButton;

    public WorkingSetGroup() {
        String[] workingSetIds = new String[] {
                IWorkingSetIDs.JAVA, IWorkingSetIDs.RESOURCE
        };
        fWorkingSetBlock = new WorkingSetConfigurationBlock(workingSetIds, JavaPlugin.getDefault()
                .getDialogSettings());
    }

    public Composite createControl(Composite composite) {
        Group workingSetGroup = new Group(composite, SWT.NONE);
        workingSetGroup.setFont(composite.getFont());
        workingSetGroup.setText(NewWizardMessages.NewJavaProjectWizardPageOne_WorkingSets_group);
        workingSetGroup.setLayout(new GridLayout(1, false));

        fWorkingSetBlock.createContent(workingSetGroup);

        // WorkingSetGroup is implemented in such a way that the checkbox it contains
        // can only be programmatically set if there's an existing working set associated
        // *before* we construct the control. However the control is created when the
        // wizard is opened, not when the page is first shown.
        //
        // One choice is to duplicate the class in our project.
        // Or find the checkbox we want and trigger it manually.
        mEnableButton = findCheckbox(workingSetGroup);

        return workingSetGroup;
    }

    public void setWorkingSets(IWorkingSet[] workingSets) {
        fWorkingSetBlock.setWorkingSets(workingSets);
    }

    public IWorkingSet[] getSelectedWorkingSets() {
        try {
            return fWorkingSetBlock.getSelectedWorkingSets();
        } catch (Throwable t) {
            // Test scenarios; no UI is created, which the fWorkingSetBlock assumes
            // (it dereferences the enabledButton)
            return new IWorkingSet[0];
        }
    }

    public boolean isChecked() {
        return mEnableButton == null ? false : mEnableButton.getSelection();
    }

    public void setChecked(boolean state) {
        if (mEnableButton != null) {
            mEnableButton.setSelection(state);
        }
    }

    /**
     * Finds the first button of style Checkbox in the given parent composite.
     * Returns null if not found.
     */
    private Button findCheckbox(Composite parent) {
        for (Control control : parent.getChildren()) {
            if (control instanceof Button && (control.getStyle() & SWT.CHECK) == SWT.CHECK) {
                return (Button) control;
            } else if (control instanceof Composite) {
                Button found = findCheckbox((Composite) control);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }
}
