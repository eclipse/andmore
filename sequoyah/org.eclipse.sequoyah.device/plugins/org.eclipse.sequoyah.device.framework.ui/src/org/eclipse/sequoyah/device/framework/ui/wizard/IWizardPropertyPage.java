/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Inc.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Fabio Fantato (Motorola)
 * 
 * Contributors:
 * Fabio Fantato (Motorola) - bug#221736 - new instance wizard
 * Fabio Fantato (Instituto Eldorado) - [263188] - Create new examples to support tutorial presentation
 * Fabio Fantato (Instituto Eldorado) - [243494] Change the reference implementation to work on Galileo
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.device.framework.ui.wizard;

import java.util.Properties;

public interface IWizardPropertyPage {

	public Properties getProperties();
}
