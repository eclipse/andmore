/********************************************************************************
 * Copyright (c) 2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Marcelo Marzola Bossoni (Eldorado)
 * 
 * Contributors:
 * 
 ********************************************************************************/
package org.eclipse.sequoyah.localization.editor.model.input;

import org.eclipse.ui.IEditorInput;

/**
 * Listener that aim to be notified when there is a change in the editor
 * contents
 * 
 * Notifies changes from EditorPart to Input
 */
public interface IEditorChangeListener {

	/**
	 * The content of the source editor input was changed
	 * 
	 * @param input
	 *            the source editor input
	 * @param newContent
	 *            the new content, available only on the editor (not yet passed
	 *            to filesystem)
	 */
	public void editorContentChanged(IEditorInput input, String newContent);
}
