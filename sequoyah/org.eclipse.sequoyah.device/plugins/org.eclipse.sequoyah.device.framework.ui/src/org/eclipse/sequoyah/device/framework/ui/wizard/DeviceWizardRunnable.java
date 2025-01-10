/********************************************************************************
 * Copyright (c) 2009-2010 Motorola Inc.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributors:
 * Daniel Barboza Franco (Instituto Eldorado)
 * [268887] - Cannot access IWizardPage from IRunnableWithProgress
 * 
 * Contributors:
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/

package org.eclipse.sequoyah.device.framework.ui.wizard;

import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;

public abstract class DeviceWizardRunnable implements IRunnableWithProgress{
	
	private IWizardPage page;
  
	public void setPage(IWizardPage page)
    {
        this.page = page;
    }
	
	public IWizardPage getWizardPage() {
		return page;
	}
	
}
