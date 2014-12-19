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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.andmore.android.wizards.elements.sorting.TableItemStringComparator.SortDirection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * This selection listener sorts the columns of a {@link Table}. It assumes the
 * columns being sorted are {@link String}s.
 */
public class TableItemSortStringSetActionListener implements SelectionListener {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse
	 * .swt.events.SelectionEvent)
	 */
	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		// sort the table
		sortTable(e);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt
	 * .events.SelectionEvent)
	 */
	@Override
	public void widgetSelected(SelectionEvent e) {
		// sort the table
		sortTable(e);
	}

	/**
	 * Execute the {@link Table} sorting.
	 * 
	 * @param e
	 *            Event object of the sort.
	 */
	private void sortTable(SelectionEvent e) {
		// get column to be sorted
		TableColumn column = (TableColumn) e.widget;
		// get table belonging to to sorted column
		Table table = column.getParent();

		// get selected items because the sort messes with them, so they will be
		// restored later
		TableItem[] selectedItems = table.getSelection();
		// create a list of new selected items - items which will be selected
		// after the sorting
		List<TableItem> selectedItemsAfterSorting = new ArrayList<TableItem>();

		// get the sort direction from the table
		int sortDirection = table.getSortDirection();
		// determine the new sorting direction according to the old one
		sortDirection = sortDirection == SWT.UP ? SWT.DOWN : SWT.UP;

		// get Table items
		TableItem[] items = table.getItems();
		if ((items != null) && (items.length > 0)) {
			// sort the items - assume you always sort them by String
			Arrays.sort(items, new TableItemStringComparator(table.indexOf(column),
					sortDirection == SWT.UP ? SortDirection.ASCENDING : SortDirection.DESCENDING));

			// rearrange the table according to the sorted items - the iteration
			// is in the new sorted order
			TableItem newItem = null;
			TableItem item = null;
			for (int rowIndex = 0; rowIndex < items.length; rowIndex++) {
				// get current item - to be removed
				item = items[rowIndex];
				// create a new table item to be inserted in the table
				newItem = new TableItem(table, SWT.NONE, rowIndex);
				// set the text for this new item based on the item in the order
				newItem.setText(getTableItemData(item));
				// in case the current item is a selected one, put the new one
				// in the selection list
				if (isElementInArray(item, selectedItems)) {
					selectedItemsAfterSorting.add(newItem);
				}
				// remove the current item
				item.dispose();
			}
		}
		// set the new selection items
		TableItem[] selectedItemsAfterSortingArray = new TableItem[selectedItemsAfterSorting.size()];
		for (int rowIndex = 0; rowIndex < selectedItemsAfterSortingArray.length; rowIndex++) {
			selectedItemsAfterSortingArray[rowIndex] = selectedItemsAfterSorting.get(rowIndex);
		}
		table.setSelection(selectedItemsAfterSortingArray);

		// set the new sorting direction and sorted column
		table.setSortDirection(sortDirection);
		table.setSortColumn((column));
		table.setRedraw(true);
	}

	/**
	 * Verify whether an element belongs to an array.
	 * 
	 * @param <E>
	 *            Any type of object or primitive
	 * @param element
	 *            Element to be verified whether it is in a certain array.
	 * @param array
	 *            Array which the element will be verified to be within.
	 * 
	 * @return Returns <code>true</code> in case the element is in the array,
	 *         <code>false</code> otherwise.
	 */
	private <E> boolean isElementInArray(E element, E[] array) {
		boolean hasElement = false;

		// first all items must not be null and the list not empty
		if ((element != null) && (array != null) && (array.length > 0)) {
			// verify whether the element is in the array
			for (E arrayItem : array) {
				// the element was found, set the flag and quit the loop
				if (arrayItem.equals(element)) {
					hasElement = true;
					break;
				}
			}
		}

		return hasElement;
	}

	/**
	 * Get an array of string holding the data of a {@link TableItem}.
	 * 
	 * @param tableItem
	 *            Table item in which data will be retrieved.
	 * 
	 * @return Array of data from the Table Item.
	 */
	private String[] getTableItemData(TableItem tableItem) {
		String[] itemArray = new String[0];
		// get the related table
		Table table = tableItem.getParent();
		if (table != null) {
			// get the number of columns in the table
			int columnCount = table.getColumnCount();
			if (columnCount > 0) {
				// set a string of data to be returned, based on the number of
				// columns
				itemArray = new String[columnCount];
				// for each column item put in the array to be returned
				for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
					itemArray[columnIndex] = tableItem.getText(columnIndex);
				}
			}
		}
		return itemArray;
	}
}
