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

import org.eclipse.andmore.android.db.core.i18n.DbCoreNLS;
import org.eclipse.jface.resource.ImageDescriptor;

public final class LoadingNode extends AbstractTreeNode {

	public static String ID = "LOADING_NODE"; //$NON-NLS-1$

	public LoadingNode(ITreeNode parent) {
		super(parent);
		setName(DbCoreNLS.LoadingNode_nodeName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.AbstractTreeNode#getIcon()
	 */
	@Override
	public ImageDescriptor getIcon() {
		return getSpecificIcon("org.eclipse.datatools.connectivity.sqm.core.ui", //$NON-NLS-1$
				"icons/refresh.gif"); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.AbstractTreeNode#refresh()
	 */
	@Override
	public void refresh() {
		// Does Nothing!
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.AbstractTreeNode#isLeaf()
	 */
	@Override
	public boolean isLeaf() {
		return true;
	}

}
