/*******************************************************************************
 * Copyright (c) 2008-2010 MontaVista Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Yu-Fen Kuo (MontaVista) - initial API and implementation
 * Yu-Fen Kuo (MontaVista)  - [236476] - provide a generic device type
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 *******************************************************************************/
package org.eclipse.sequoyah.device.framework.ui.model;

import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;

public interface IDeviceTypeCustomWizardPage {

	public String getId();

	public void setId(String id);

	public IWizardPage getPageClass();

	public void setPageClass(IWizardPage pageClass);

	public IRunnableWithProgress getOperationClass();

	public void setOperationClass(IRunnableWithProgress operationClass);
}
