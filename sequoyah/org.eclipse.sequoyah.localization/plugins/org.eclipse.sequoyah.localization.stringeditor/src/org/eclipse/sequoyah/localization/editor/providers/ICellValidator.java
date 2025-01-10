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
 * Paulo Faria (Eldorado) - Bug [326793] - Starting new LFE workflow improvements (validate key) 
 ********************************************************************************/
package org.eclipse.sequoyah.localization.editor.providers;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;

/**
 * Check if the cell is valid
 */
public interface ICellValidator {
	public void init(IProject project) throws Exception;

	public IStatus isCellValid(String columnID, String key, String value);

	/**
	 * Validates if the key is valid
	 * 
	 * @param key
	 * @return
	 */
	public IStatus isKeyValid(String key);
}
