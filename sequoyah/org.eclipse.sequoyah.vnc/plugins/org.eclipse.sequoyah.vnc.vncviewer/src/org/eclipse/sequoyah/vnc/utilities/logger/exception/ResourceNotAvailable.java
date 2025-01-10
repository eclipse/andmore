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

package org.eclipse.sequoyah.vnc.utilities.logger.exception;

import java.io.File;

@SuppressWarnings("serial")
public class ResourceNotAvailable extends Exception
{  
   private static final String NOT_AVAILABLE_DUE_TO_I_O_ISSUES = " not available due to I/O issues."; //$NON-NLS-1$
   private static final String RESOURCE = "Resource"; //$NON-NLS-1$

   /**
    *  
    * @param resource 
    * @param arg0
    */
   public ResourceNotAvailable(String resource, Throwable arg0)
   {
      super(RESOURCE + resource + NOT_AVAILABLE_DUE_TO_I_O_ISSUES, arg0);      
   }   
   
   /**
    * 
    * @param resource
    * @param arg0
    */
   public ResourceNotAvailable(File resource, Throwable arg0)
   {
      super(RESOURCE + resource + NOT_AVAILABLE_DUE_TO_I_O_ISSUES, arg0);      
   }   
}
