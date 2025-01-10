/********************************************************************************
 * Copyright (c) 2009-2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Marcelo Marzola Bossoni (Eldorado)
 * Vinicius Rigoni Hernandes (Eldorado)
 * 
 * Contributors:
 * Vinicius Rigoni Hernandes (Eldorado) - Bug [289885] - Localization Editor doesn't recognize external file changes
 ********************************************************************************/
package org.eclipse.sequoyah.localization.editor.model.input;

/**
 * Listener that aim to be notified when there is a change in the editor input
 * contents
 * 
 * Notifies changes from Input to EditorPart
 */
public interface IInputChangeListener {

	public void columnChanged(String columnID);

}
