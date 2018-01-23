/********************************************************************************
 * Copyright (c) 2009-2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Marcelo Marzola Bossoni (Eldorado)
 * 
 * Contributors:
 * name (company) - description.
 ********************************************************************************/
package org.eclipse.sequoyah.localization.editor.model.operations;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.sequoyah.localization.editor.datatype.RowInfo;
import org.eclipse.sequoyah.localization.editor.model.StringEditorPart;

/**
 * The operation of adding a new key (row) to the editor.
 */
public class AddKeyOperation extends EditorOperation
{

    private final RowInfo row;

    private boolean doRefresh = true;

    public AddKeyOperation(String label, StringEditorPart editor, RowInfo row)
    {
        super(label, editor);
        this.row = row;
    }

    /**
     * 
     * @param label
     * @param editor
     * @param row
     * @param doRefresh set to false when adding multiple strings.
     */
    public AddKeyOperation(String label, StringEditorPart editor, RowInfo row, boolean doRefresh)
    {
        this(label, editor, row);
        this.doRefresh = doRefresh;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.core.commands.operations.AbstractOperation#execute(org.eclipse
     * .core.runtime.IProgressMonitor, org.eclipse.core.runtime.IAdaptable)
     */
    @Override
    public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException
    {
        return redo(monitor, info);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.core.commands.operations.AbstractOperation#redo(org.eclipse
     * .core.runtime.IProgressMonitor, org.eclipse.core.runtime.IAdaptable)
     */
    @Override
    public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException
    {
        getEditor().addRow(row);
        if (doRefresh)
        {
            getEditor().refresh();
        }
        return Status.OK_STATUS;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.core.commands.operations.AbstractOperation#undo(org.eclipse
     * .core.runtime.IProgressMonitor, org.eclipse.core.runtime.IAdaptable)
     */
    @Override
    public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException
    {
        getEditor().removeRow(row.getKey());
        if (doRefresh)
        {
            getEditor().refresh();
        }
        return Status.OK_STATUS;
    }

}
