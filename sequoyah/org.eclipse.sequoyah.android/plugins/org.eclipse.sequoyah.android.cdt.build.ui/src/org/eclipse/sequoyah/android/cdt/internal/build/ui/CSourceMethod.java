/*******************************************************************************
 * Copyright (c) 2010 Motorola, Inc. All rights reserved.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial contributors:
 * Paulo Renato de Faria (Eldorado)
 *******************************************************************************/
package org.eclipse.sequoyah.android.cdt.internal.build.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents one method signature (signatureName, returnType and parameters)
 * on C source file
 * @author Paulo Renato de Faria
 *
 */
public class CSourceMethod
{
    /**
     * Signature name
     */
    private String signature;

    private String returnType;

    /**
     * Parameter type (does not include variable name)
     */
    private List<String> parameterTypes = new ArrayList<String>();

    public CSourceMethod()
    {
        super();
    }

    /**
     * 
     * @param signature
     * @param returnType
     */
    public CSourceMethod(String signature, String returnType)
    {
        super();
        this.signature = signature;
        this.returnType = returnType;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((parameterTypes == null) ? 0 : parameterTypes.hashCode());
        result = prime * result + ((returnType == null) ? 0 : returnType.hashCode());
        result = prime * result + ((signature == null) ? 0 : signature.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        CSourceMethod other = (CSourceMethod) obj;
        if (parameterTypes == null)
        {
            if (other.parameterTypes != null)
            {
                return false;
            }
        }
        else if (!parameterTypes.equals(other.parameterTypes))
        {
            return false;
        }
        if (returnType == null)
        {
            if (other.returnType != null)
            {
                return false;
            }
        }
        else if (!returnType.equals(other.returnType))
        {
            return false;
        }
        if (signature == null)
        {
            if (other.signature != null)
            {
                return false;
            }
        }
        else if (!signature.equals(other.signature))
        {
            return false;
        }
        return true;
    }

    public List<String> getParameterTypes()
    {
        return parameterTypes;
    }

    public boolean add(String parameter)
    {
        return parameterTypes.add(parameter);
    }

    public boolean contains(Object parameter)
    {
        return parameterTypes.contains(parameter);
    }

    public boolean removeAll(Collection<?> paremeters)
    {
        return parameterTypes.removeAll(paremeters);
    }

    @Override
    public String toString()
    {
        return "CSourceModel [parameterTypes=" + parameterTypes + ", returnType=" + returnType
                + ", signature=" + signature + "]";
    }

    public String getSignature()
    {
        return signature;
    }

    public void setSignature(String signature)
    {
        this.signature = signature;
    }

    public String getReturnType()
    {
        return returnType;
    }

    public void setReturnType(String returnType)
    {
        this.returnType = returnType;
    }

}
