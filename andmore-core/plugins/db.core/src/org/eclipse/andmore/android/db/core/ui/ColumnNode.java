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
import org.eclipse.andmore.android.db.core.model.DbModel;
import org.eclipse.datatools.modelbase.sql.tables.Column;
import org.eclipse.jface.resource.ImageDescriptor;

public class ColumnNode extends AbstractTreeNode implements IDataSampler {
	private boolean isPrimKey = false;

	private final Column column;

	private final DbModel model;

	/**
	 * @param column
	 * @param tableNode
	 */
	public ColumnNode(Column column, DbModel model, ITreeNode parent) {
		super(parent);
		this.column = column;
		this.model = model;
		setId(column.getName());

		StringBuilder nameBuilder = column.getDataType() != null ? new StringBuilder(column.getName() + " [" //$NON-NLS-1$
				+ column.getDataType().getName()) : new StringBuilder(column.getName()
				+ " [" + DbCoreNLS.ColumnNode_UnknownType); //$NON-NLS-1$
		if (column.isPartOfPrimaryKey()) {
			nameBuilder.append(" PK"); //$NON-NLS-1$
			isPrimKey = true;
		}
		nameBuilder.append("]"); //$NON-NLS-1$
		setName(nameBuilder.toString());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.AbstractTreeNode#refresh()
	 */
	@Override
	public void refresh() {
		// Do nothing!
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.andmore.android.db.core.ui.AbstractTreeNode#getIcon()
	 */
	@Override
	public ImageDescriptor getIcon() {
		String iconPath;
		if (isPrimKey) {
			iconPath = "icons/pkColumn.gif"; //$NON-NLS-1$
		} else {
			iconPath = "icons/columns.gif"; //$NON-NLS-1$
		}
		return getSpecificIcon("org.eclipse.datatools.connectivity.sqm.core.ui", //$NON-NLS-1$
				iconPath);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.andmore.android.db.core.ui.IDataSampler#sampleDbContents()
	 */
	@Override
	public void sampleDbContents() {
		model.sampleContents(column);
	}

}
