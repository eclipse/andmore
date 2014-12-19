/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.andmore.android.db.core.ui;

import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.IActionFilter;

/**
 * Interface for nodes that are responsible for mapping databases.
 */
public interface IDbMapperNode extends ITreeNode, IActionFilter {

	public static final String UNMAP_ACTIONFILTER_NAME = "org.eclipse.andmore.android.db.core.mappedDatabases"; //$NON-NLS-1$

	public static final String UNMAP_ACTIONFILTER_VALUE = "org.eclipse.andmore.android.db.core.atLeastOne"; //$NON-NLS-1$

	public IStatus map(IPath dbFilePath);

	public IStatus unmap(ITreeNode dbNode);

	public IStatus unmap(List<ITreeNode> dbNodeList);
}
