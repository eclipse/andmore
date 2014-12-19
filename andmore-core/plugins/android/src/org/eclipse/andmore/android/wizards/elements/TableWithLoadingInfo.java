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
package org.eclipse.andmore.android.wizards.elements;

import java.util.Collection;

import org.eclipse.andmore.android.i18n.AndroidNLS;
import org.eclipse.andmore.android.wizards.mat.DumpHPROFTable;
import org.eclipse.andmore.android.wizards.mat.DumpHPROFWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * <p>
 * This widget displays a table with the ability of getting data asynchronously
 * and showing it. Basically, while retrieving data, a Displaying info... is
 * displayed. After the data is retrieved, its info is shown in the Table.
 * </p>
 * <p>
 * To use this component, first one must implement it and at least fill in the
 * methods {@link #callServiceForRetrievingDataToPopulateTable(Object)} and
 * {@link #addTableData(Collection)}. The former aims to call the service and
 * get its data. Note that the asynchronous process is left to this widget. The
 * latter adds data to the widget�s table.
 * </p>
 * <p>
 * When using this widget in the class, firstly is necessary to instantiate it,
 * add its columns and finally call the method
 * {@link #populateTableAsynchronously(Object)}. Note that the object is the
 * parameter(s) for the service calling in method
 * {@link #callServiceForRetrievingDataToPopulateTable(Object)};
 * </p>
 * <p>
 * One should also note that there are several {@link Table} methods wrapped by
 * this component. In case it is needed, one may add more. The {@link Table}
 * itself is exposed by the method {@link #getTable()}. The only restriction
 * that should be observed is that no data should be added manually, only by the
 * method {@link #addTableData(Collection)}.
 * </p>
 * <p>
 * For an example of this widget in action, please consult
 * {@link DumpHPROFTable} and {@link DumpHPROFWizardPage}.
 * </p>
 * 
 * @param <P>
 *            Class which is the type of the Calling Page, retrieved by the
 *            method {@link TableWithLoadingInfo#getCallingPage()}.
 * @param <E>
 *            Class which is the type of the Element list retrieved by the
 *            asynchronous service. This service is called by the method
 *            {@link TableWithLoadingInfo#callServiceForRetrievingDataToPopulateTable(Object)}
 *            .
 * @param <V>
 *            The type of value which represents the parameter entered as a
 *            parameter to call the asynchronous service:
 *            {@link TableWithLoadingInfo#callServiceForRetrievingDataToPopulateTable(Object)}
 *            .
 */
public abstract class TableWithLoadingInfo<P, E, V> extends Composite {
	/**
	 * This thread displays the Loading info... message in the table.
	 */
	private final class LoadingInfoAnimationThread extends Thread {
		private TableItem item;

		private final Table table;

		/**
		 * Instantiates the Loading info message.
		 * 
		 * @param threadName
		 *            Thread name.
		 * @param table
		 *            Table which holds the Loading Info... message.
		 * @param item
		 *            Table item which holds the Loading Info... message.
		 */
		private LoadingInfoAnimationThread(String threadName, Table table, TableItem item) {
			super(threadName);
			this.table = table;
			this.item = item;
		}

		/**
		 * Add animated text of Loading Data...
		 */
		@Override
		public void run() {
			int i = 0;
			StringBuffer text;
			while (!isInterrupted()) {
				text = new StringBuffer(animatedTextLabel != null ? animatedTextLabel
						: AndroidNLS.TableWithLoadingInfo__UI_LoadingData);
				for (int j = 0; j < (i % 4); j++) {
					text = text.append("."); //$NON-NLS-1$
				}
				final StringBuffer finalText = new StringBuffer(text);
				Display.getDefault().syncExec(new Runnable() {
					@Override
					public void run() {
						// in case the item is disposed, re-create it.
						if (item.isDisposed()) {
							item = new TableItem(table, SWT.NONE);
						}
						item.setText(0, finalText.toString());
					}
				});
				i++;
				try {
					sleep(1000);
				} catch (InterruptedException e) {
					break;
				}
			}
		}
	}

	/**
	 * This listener implements the functionality of deselecting all data from a
	 * determined table.
	 */
	private class DeselectListener implements SelectionListener {
		Table table;

		/**
		 * Instantiates the Listener for deselecting all data from the entered
		 * table.
		 * 
		 * @param table
		 *            Table which all rows will be deselected.
		 */
		public DeselectListener(Table table) {
			this.table = table;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org
		 * .eclipse.swt.events.SelectionEvent)
		 */
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			this.table.deselectAll();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse
		 * .swt.events.SelectionEvent)
		 */
		@Override
		public void widgetSelected(SelectionEvent e) {
			this.table.deselectAll();
		}

	}

	E elementList;

	private Thread animatedText;

	private String animatedTextLabel = null;

	private final SelectionListener deselectionItem;

	private final Table table;

	private P callingPage;

	/**
	 * Set the page which called this widget.
	 * 
	 * @param The
	 *            page which called this widget.
	 */
	public void setCallingPage(P callingPage) {
		this.callingPage = callingPage;
	}

	/**
	 * Get the page which called this widget.
	 * 
	 * @return The page which called this widget.
	 */
	public P getCallingPage() {
		return callingPage;
	}

	/**
	 * Get the table which is populated. It is strongly recommended that the
	 * insertion of data in this table should be done by the method
	 * {@link TableWithLoadingInfo#addTableData(Collection)}. This table is
	 * exposed in order to allow its customization.
	 * 
	 * @return The table to be populated.
	 */
	public Table getTable() {
		return table;
	}

	/**
	 * Get the collection of data of this table, after the table is populated by
	 * calling the service asynchronously.
	 * 
	 * @return List of elements retrieved by the service called by the method
	 *         {@link #populateTableAsynchronously()}.
	 * 
	 * @see TableWithLoadingInfo#callServiceForRetrievingDataToPopulateTable()
	 * @see TableWithLoadingInfo#populateTableAsynchronously()
	 */
	public E getElementList() {
		return elementList;
	}

	/**
	 * Set the "collection" of elements for populating this table. When this
	 * method is called, the Loading info... message is replaced by the list of
	 * elements to be inserted, in case this list is not null. If null is
	 * entered as a parameter, the Loading info... message is displayed.
	 * 
	 * @param elementList
	 *            The Set of elements to populate this table to set
	 */
	public void populateTable(E elementList) {
		this.elementList = elementList;
		// stop the animation
		if (animatedText != null) {
			animatedText.interrupt();
		}
		// populate and display the list of elements
		Display.getDefault().syncExec(new Runnable() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see java.lang.Runnable#run()
			 */
			@Override
			public void run() {
				if (!table.isDisposed()) {
					// remove the Loading info... message
					table.removeAll();
					// populate the table and display the data
					populateTable();
				}
			}
		});

		this.elementList = elementList;
	}

	/**
	 * Instantiates a {@link Table} with Loading info component. It holds the
	 * composite parent the the {@link SWT} style. This constructor can call the
	 * method for starting the {@link Table}�s population process.
	 * 
	 * @param parent
	 *            Composite parent.
	 * @param style
	 *            {@link SWT} style.
	 * @param animatedTextLabel
	 *            Text to be displayed when data is being loaded.
	 * @param callingPage
	 *            The page which called this widget.
	 */
	public TableWithLoadingInfo(Composite parent, int style, String animatedTextLabel, P callingPage) {
		super(parent, SWT.FILL);
		// create the layout
		Layout layout = new GridLayout();
		this.setLayout(layout);

		// add table
		this.table = new Table(this, style);
		this.table.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

		// set attributes
		this.animatedTextLabel = animatedTextLabel;
		this.setCallingPage(callingPage);

		// instantiate listeners
		deselectionItem = new DeselectListener(table);
	}

	/**
	 * Remove all items from the {@link Table}.
	 * 
	 * @see Table#removeAll()
	 */
	public void removeAllTableItems() {
		table.removeAll();
	}

	/**
	 * Add a column to the {@link Table} of this component.
	 * 
	 * @param columnStyle
	 *            Column style.
	 * @param tableIndex
	 *            {@link Table} index where the column will be added.
	 * 
	 * @return The added {@link TableColumn}
	 * 
	 * @see TableColumn#TableColumn(Table, int, int)
	 */
	public TableColumn addTableColumn(int columnStyle, int tableIndex) {
		return new TableColumn(table, columnStyle, tableIndex);
	}

	/**
	 * Add a column the the {@link Table} of this component.
	 * 
	 * @param columnStyle
	 *            Column style.
	 * 
	 * @return The added {@link TableColumn}
	 * 
	 * @see TableColumn#TableColumn(Table, int)
	 */
	public TableColumn addTableColumn(int columnStyle) {
		return new TableColumn(table, columnStyle);
	}

	/**
	 * Add a {@link SelectionListener} to the table of this component.
	 * 
	 * @param selectionListener
	 *            Selection Listener to be added.
	 * 
	 * @see Table#addSelectionListener(SelectionListener)
	 */
	public void addTableSelectionListener(SelectionListener selectionListener) {
		table.addSelectionListener(selectionListener);
	}

	/**
	 * Get the number of selected elements in the table.
	 * 
	 * @return Number of selected elements.
	 * 
	 * @see Table#getSelectionCount()
	 */
	public int getTableSelectionCont() {
		return table.getSelectionCount();
	}

	/**
	 * Get the index of a selected item.
	 * 
	 * @return Index of a selected item
	 * 
	 * @see Table#getSelectionIndex()
	 */
	public int getTableSelectionIndex() {
		return table.getSelectionIndex();
	}

	/**
	 * The the list of selected indices.
	 * 
	 * @return List of selection indices.
	 * 
	 * @see Table#getSelectionIndices()
	 */
	public int[] getSelectionIndices() {
		return table.getSelectionIndices();
	}

	/**
	 * The the list of selected elements.
	 * 
	 * @return List of selected Elements
	 * 
	 * @see Table#getSelection()
	 */
	public TableItem[] getTableSelection() {
		return table.getSelection();
	}

	/**
	 * Get all Table Items from a {@link Table}.
	 * 
	 * @return Array of Table Items ({@link TableItem}).
	 * 
	 * @see Table#getItems()
	 */
	public TableItem[] getTableItems() {
		return table.getItems();
	}

	/**
	 * Get a {@link TableItem} based on its index on the {@link Table}.
	 * 
	 * @param index
	 *            Index on the table to retrieve the {@link TableItem}.
	 * 
	 * @return Retrieved {@link TableItem}.
	 * 
	 * @see Table#getItem(int)
	 */
	public TableItem getTableItem(int index) {
		return table.getItem(index);
	}

	/**
	 * <p>
	 * Marks the {@link Table}�s receiver's lines as visible if the argument is
	 * true, and marks it invisible otherwise. Note that some platforms draw
	 * grid lines while others may draw alternating row colors.
	 * </p>
	 * <p>
	 * If one of the receiver's ancestors is not visible or some other condition
	 * makes
	 * </p>
	 * 
	 * @param The
	 *            new visibility state.
	 * 
	 *            see {@link Table#setLinesVisible(boolean)}
	 */
	public void setTableLinesVisible(boolean show) {
		table.setLinesVisible(show);
	}

	/**
	 * Makes the {@link Table}�s header visible, is <code>true</code> is set.
	 * Otherwise, mark it not visible.
	 * 
	 * @param show
	 *            Parameter which determines whether tha {@link Table} will be
	 *            marked as visible. It will be so, if <code>true</code> is
	 *            entered.
	 * 
	 * @see Table#setHeaderVisible(boolean)
	 */
	public void setTableHeaderVisible(boolean show) {
		table.setHeaderVisible(show);
	}

	/**
	 * Get the number of selected items in a {@link Table}.
	 * 
	 * @return Number of selected items in a {@link Table}.
	 * 
	 * @see Table#getSelectionCount()
	 */
	public int getTableSelectionCount() {
		return table.getSelectionCount();
	}

	/**
	 * Deselect all items from a {@link Table}.
	 * 
	 * @see Table#deselectAll()
	 */
	public void deselectAllTableItems() {
		table.deselectAll();
	}

	/**
	 * Set all items entered as selected.
	 * 
	 * @param item
	 *            Items to be selected.
	 * 
	 * @see Table#setSelection(TableItem[])
	 */
	public void setTableSelection(TableItem[] item) {
		table.setSelection(item);
	}

	/**
	 * This method is responsible for calling the service which will populate
	 * the {@link Table}. Note that no asynchronous task is to be done here.
	 * This feature is implemented by this widget. Thus, basically this method
	 * should simply call the service for populating the {@link Table} and
	 * return its data as this method�s parameter.
	 * 
	 * @param parameters
	 *            Parameters used by the service
	 * 
	 * @return The data retrieved by the service.
	 */
	protected abstract E callServiceForRetrievingDataToPopulateTable(V parameters);

	/**
	 * This method is responsible for adding data to the {@link Table}. Here the
	 * user has to worry only with inserting elements in the table. The
	 * mechanism for creating the Loading info... is taken care by this table
	 * implementation. The data in inserted in the table refereed by the object
	 * retrieved by the method {@link TableWithLoadingInfo#getTable()}.
	 */
	protected abstract void addTableData(E elementList);

	/**
	 * This method executes needed operations after the table is populated.
	 * Here, thinks like {@link WizardPage#setPageComplete(boolean)} and layout
	 * wrap ups ought to be executed. Note that this method can be empty.
	 * Moreover, observe that this method can be called even if the
	 * {@link Table} is empty, because the service could not yet have been
	 * called. It means this method could be called twice, firstly when the
	 * Loading info... message is displayed and secondly when the data is
	 * retrieved from the asynchronous service and populates the table.
	 */
	protected abstract void executeOperationsAfterTableIsPopulated();

	/**
	 * This method populates the Table asynchronously. It means, that it call
	 * the service implemented by the method
	 * {@link TableWithLoadingInfo#callServiceForRetrievingDataToPopulateTable(V)}
	 * and while nothing is retrieved, the Loading info... message is displayed.
	 * When the service retrieves data, its value populates the table.
	 * 
	 * @param asynchronousServiceParameters
	 *            The parameters to be passed to be asynchronous service which
	 *            will be called.
	 */
	public void populateTableAsynchronously(final V asynchronousServiceParameters) {
		// reset element list
		elementList = null;
		// populate the table with nothing
		populateTable();
		// call the service for populating the table asynchronously
		Thread callServiceThread = new Thread("listPackages") {
			/**
			 * Call the service implemented by
			 * {@link TableWithLoadingInfo#callServiceForRetrievingDataToPopulateTable(V)}
			 * and populate the Table in this widget.
			 */
			@Override
			public void run() {
				// retrieve the list of running processes in the device
				E elementList = callServiceForRetrievingDataToPopulateTable(asynchronousServiceParameters);
				// populate the table
				populateTable(elementList);
			};
		};
		// start thread for retrieving data and populating the table
		callServiceThread.start();
	}

	/**
	 * Populate the table with data provided by
	 * {@link TableWithLoadingInfo#addTableData()}. This method is responsible
	 * for creating the Loading info... within the table. In the end, the method
	 * {@link TableWithLoadingInfo#executeOperationsAfterTableIsPopulated()} is
	 * executed in order to perform operations needed after data population is
	 * performed. This method, of course, can be empty.
	 */
	private synchronized void populateTable() {
		// add data to the table in case there are elements to do so
		if (elementList != null) {
			// clear table
			table.clearAll();
			if (deselectionItem != null) {
				table.removeSelectionListener(deselectionItem);
			}
			// add data to the table
			addTableData(elementList);
		}
		// since there is no data yet retrieve, add the animation
		else {
			// add listeners
			table.addSelectionListener(deselectionItem);

			// add animated text
			TableItem item = new TableItem(table, SWT.NONE);
			animatedText = new LoadingInfoAnimationThread("TextAnimator", table, item);
			animatedText.start();
		}
		// execute final operations
		executeOperationsAfterTableIsPopulated();
	}
}
