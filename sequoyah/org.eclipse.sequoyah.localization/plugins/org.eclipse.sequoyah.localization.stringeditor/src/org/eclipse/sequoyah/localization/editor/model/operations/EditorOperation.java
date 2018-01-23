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

import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.sequoyah.localization.editor.model.StringEditorPart;
import org.eclipse.sequoyah.localization.editor.model.StringEditorViewerModel;
import org.eclipse.sequoyah.localization.editor.model.input.AbstractStringEditorInput;

/**
 * This class is the base for all the operations executed inside the editor.
 */
public abstract class EditorOperation extends AbstractOperation {

	private final StringEditorPart editor;

	/**
	 * Default constructor.
	 * 
	 * @param label
	 *            - the label which identifies the new operation.
	 * @param editor
	 *            - the target editor for this operation.
	 */
	public EditorOperation(String label, StringEditorPart editor) {
		super(label);
		this.editor = editor;
	}

	/**
	 * Get the editor where to apply the changes
	 * 
	 * @return the String editor to be updated by this operation
	 */
	public StringEditorPart getEditor() {
		return editor;
	}

	/**
	 * Get the model for the table based editor.
	 * 
	 * @return the model (that is also the input) used by the editor.
	 */
	public StringEditorViewerModel getModel() {
		return editor.getModel();
	}

	/**
	 * Get the editor input.
	 * 
	 * @return return the input used by the editor of this operation.
	 */
	public AbstractStringEditorInput getEditorInput() {
		return (getEditor().getEditorInput());
	}
}
