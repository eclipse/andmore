/********************************************************************************
 * Copyright (c) 2010 Motorola Mobility, Inc.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Pablo Leite (Eldorado) - Bug: 329548
 * 
 * Contributors:
 ********************************************************************************/
package org.eclipse.sequoyah.device.framework.model;

public interface IParallelService extends IService
{
    boolean isParallelized();
    void setParallelized(boolean parallelized);
    void setInterval(int integer);
    int  getInterval();
}