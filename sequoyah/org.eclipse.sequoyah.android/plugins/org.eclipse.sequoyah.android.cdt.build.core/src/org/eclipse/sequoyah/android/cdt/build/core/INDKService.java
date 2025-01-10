/*******************************************************************************
 * Copyright (c) 2010 Wind River Systems and others.
 * Copyright (c) 2010 Motorola, Inc. All rights reserved.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Doug Schaefer (WRS) - Initial API and implementation
 * Thiago Faustini Junqueira (Eldorado) - [314314] NDK - Re-generate Makefile when project property value is changed
 * Carlos Alberto Souto Junior (Eldorado) - [315122] Improvements in the Android NDK support UI
 *******************************************************************************/
package org.eclipse.sequoyah.android.cdt.build.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.sequoyah.android.cdt.internal.build.core.CorePlugin;

/**
 * Service for getting information about the Android NDK.
 */
public interface INDKService
{

    // Property IDs

    /**
     * Library name property
     */
    public QualifiedName libName = new QualifiedName(CorePlugin.PLUGIN_ID, "libName");

    String getNDKLocation();

    void setNDKLocation(String location);

    void addNativeSupport(IProject project, String libraryName);

}
