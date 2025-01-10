/********************************************************************************
 * Copyright (c) 2007-2010 Motorola Inc. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributor:
 * Fabio Fantato (Motorola)
 *
 * Contributors:
 * Daniel Pastore (Eldorado) - [289870] Moving and renaming Tml to Sequoyah
 ********************************************************************************/


package org.eclipse.sequoyah.device.common.utilities.exception;

import org.eclipse.core.runtime.CoreException;


@SuppressWarnings("serial")
public class SequoyahException extends CoreException
{
	
    public SequoyahException(AbstractExceptionStatus status)
    {
        super(status.getStatus());        
    }
    
    public SequoyahException()
    {
        super(ExceptionStatus.ERROR_STATUS_DEFAULT);        
    }    
    
}

