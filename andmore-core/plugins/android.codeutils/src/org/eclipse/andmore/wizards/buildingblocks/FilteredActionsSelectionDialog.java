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
package org.eclipse.andmore.wizards.buildingblocks;

import java.util.Comparator;
import java.util.Set;

import org.eclipse.andmore.android.codeutils.CodeUtilsActivator;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;

/**
 * Class that implements a filter dialog to be used by NewLauncherWizardPage
 * class
 */
class FilteredActionsSelectionDialog extends FilteredItemsSelectionDialog {
	private final Set<String> categorySet;

	public FilteredActionsSelectionDialog(Shell shell, Set<String> categorySet) {
		super(shell, true);
		this.categorySet = categorySet;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#createExtendedContentArea
	 * (org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createExtendedContentArea(Composite parent) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#createFilter()
	 */
	@Override
	protected ItemsFilter createFilter() {
		return new ItemsFilter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.ItemsFilter
			 * #isConsistentItem(java.lang.Object)
			 */
			@Override
			public boolean isConsistentItem(Object item) {
				return true;
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.ItemsFilter
			 * #matchItem(java.lang.Object)
			 */
			@Override
			public boolean matchItem(Object item) {
				if (!(item instanceof String)) {
					return false;
				}
				return matches((String) item);
			}

		};
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#fillContentProvider
	 * (org
	 * .eclipse.ui.dialogs.FilteredItemsSelectionDialog.AbstractContentProvider,
	 * org.eclipse.ui.dialogs.FilteredItemsSelectionDialog.ItemsFilter,
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void fillContentProvider(AbstractContentProvider contentProvider, ItemsFilter itemsFilter,
			IProgressMonitor progressMonitor) throws CoreException {
		for (String action : categorySet) {
			contentProvider.add(action, itemsFilter);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#getDialogSettings()
	 */
	@Override
	protected IDialogSettings getDialogSettings() {
		return CodeUtilsActivator.getDefault().getDialogSettings();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#getElementName(java
	 * .lang.Object)
	 */
	@Override
	public String getElementName(Object item) {
		return (String) item;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#getItemsComparator()
	 */
	@Override
	protected Comparator<String> getItemsComparator() {
		return new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				return o1.compareToIgnoreCase(o2);
			}
		};
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#validateItem(java
	 * .lang.Object)
	 */
	@Override
	protected IStatus validateItem(Object item) {
		IStatus status;
		if (categorySet.contains(item)) {
			status = new Status(IStatus.OK, CodeUtilsActivator.PLUGIN_ID, "");
		} else {
			status = new Status(IStatus.ERROR, CodeUtilsActivator.PLUGIN_ID, "Select an item.");
		}
		return status;
	}
}
