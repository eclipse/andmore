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
package org.eclipse.andmore.android.wizards.elements.sorting;

import java.text.Collator;
import java.util.Comparator;

import org.eclipse.swt.widgets.TableItem;

/**
 * This class compares {@link TableItem}�s which have {@link String}s within
 * them.
 */
public class TableItemStringComparator implements Comparator<TableItem> {
	/**
	 * Enumerator representing the sort direction for
	 * {@link TableItemStringComparator}.
	 */
	public enum SortDirection {
		/**
		 * Ascending direction
		 */
		ASCENDING(1),

		/**
		 * Descending direction
		 */
		DESCENDING(-1);

		int sortDirection = 1;

		SortDirection(int sortDirection) {
			this.sortDirection = sortDirection;
		}

		public int getSortDirectionIntValue() {
			return sortDirection;
		}
	}

	int columnIndex = 0;

	SortDirection sortDirection = SortDirection.ASCENDING;

	/**
	 * Instantiates a {@link Comparator} for a {@link TableItem} which compares
	 * {@link String}s. The index of the column to be compared must be entered.
	 * Moreover, the sort direction must also be provided.
	 * 
	 * @param columnIndex
	 *            Column index which will be compared.
	 * @param sortDirection
	 *            Sort direction - ascending or descending
	 */
	public TableItemStringComparator(int columnIndex, SortDirection sortDirection) {
		// set fields
		this.columnIndex = columnIndex;
		this.sortDirection = sortDirection;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(TableItem first, TableItem second) {
		// get table�s items
		TableItem firstItem = first;
		TableItem secondItem = second;
		// get their determined column textx
		String firstString = (firstItem.getText(columnIndex));
		String secondString = (secondItem.getText(columnIndex));
		// compare the strings multiplying by the "sort direction" factor, for
		// ascending or descending purposes
		return Collator.getInstance().compare(firstString, secondString) * sortDirection.getSortDirectionIntValue();
	}

}
