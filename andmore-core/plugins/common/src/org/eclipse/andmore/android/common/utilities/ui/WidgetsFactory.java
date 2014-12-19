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
package org.eclipse.andmore.android.common.utilities.ui;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.TreeItem;

/**
 * Class factory to create widgets.
 */
public class WidgetsFactory {

	public static final int GROUP_MARGIN_HEIGHT = 10;

	public static final int GROUP_MARGIN_WIDTH = 10;

	/**
	 * Create a new composite with two columns.
	 * 
	 * @param parent
	 *            The parent composite.
	 * @return A composite with two columns.
	 */
	public static Composite createComposite(Composite parent) {
		Composite toReturn = new Composite(parent, SWT.NULL);
		toReturn.setLayout(createGridLayout());
		toReturn.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL));

		return toReturn;
	}

	/**
	 * Create a new composite with the given column count.
	 * 
	 * @param parent
	 *            The parent composite.
	 * @param numColumns
	 *            The column count for the new composite.
	 * @return A composite with the given column count.
	 */
	public static Composite createComposite(Composite parent, int numColumns) {
		Composite toReturn = createComposite(parent);
		((GridLayout) toReturn.getLayout()).numColumns = numColumns;

		return toReturn;
	}

	/**
	 * Creates a new line.
	 * 
	 * @param parent
	 *            The parent composite.
	 * @return A label with a line.
	 */
	public static Label createLine(Composite parent) {
		Label toReturn = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.BOLD);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = ((GridLayout) parent.getLayout()).numColumns;
		toReturn.setLayoutData(gridData);

		return toReturn;
	}

	/**
	 * Creates a new GridLayout with two columns.
	 * 
	 * @return A new GridLayout with two columns.
	 */
	public static GridLayout createGridLayout() {
		GridLayout toReturn = new GridLayout();
		toReturn.numColumns = 2;
		toReturn.makeColumnsEqualWidth = false;
		toReturn.marginWidth = 0;
		toReturn.marginHeight = 0;

		return toReturn;
	}

	/**
	 * Creates a new GridLayout with the given number of columns.
	 * 
	 * @return A new GridLayout with the given number of columns.
	 */
	public static GridLayout createGridLayout(int numColumns) {
		GridLayout toReturn = new GridLayout();
		toReturn.numColumns = numColumns;
		toReturn.makeColumnsEqualWidth = false;
		toReturn.marginWidth = 0;
		toReturn.marginHeight = 0;

		return toReturn;
	}

	/**
	 * Creates a new GridLayout with the given number of columns for the given
	 * composite.
	 * 
	 * @return A new GridLayout with the given number of columns for the given
	 *         composite.
	 */
	public static GridLayout createGridLayout(int numColumns, Composite composite) {
		GridLayout toReturn = new GridLayout();
		toReturn.numColumns = numColumns;
		toReturn.makeColumnsEqualWidth = false;
		toReturn.marginWidth = 0;
		toReturn.marginHeight = 0;

		composite.setLayout(toReturn);
		return toReturn;
	}

	/**
	 * Creates a new label.
	 * 
	 * @param parent
	 *            The parent composite.
	 * @param text
	 *            Text used in label.
	 * @return A new label
	 */
	public static Label createLabel(Composite parent, String text) {
		return createLabel(parent, text, SWT.NONE);
	}

	/**
	 * Creates a new label.
	 * 
	 * @param parent
	 *            The parent composite.
	 * @param text
	 *            Text used in label.
	 * @param style
	 *            The style used in label.
	 * @return A new label
	 */
	public static Label createLabel(Composite parent, String text, int style) {
		Label toReturn = new Label(parent, style);
		if (text != null) {
			toReturn.setText(text);
		}
		toReturn.setFont(parent.getFont());

		return toReturn;
	}

	/**
	 * Creates a new combo.
	 * 
	 * @param parent
	 *            The parent composite.
	 * @return The new combo
	 */
	public static Combo createCombo(Composite parent) {
		Combo toReturn = new Combo(parent, SWT.READ_ONLY);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		data.horizontalSpan = 1;
		toReturn.setLayoutData(data);

		return toReturn;
	}

	/**
	 * Creates two labels used to show a read only value in screens.
	 * 
	 * @param parent
	 *            The parent composite.
	 * @param text
	 *            The text used in both labels.
	 * @return The label that will show a read only value.
	 */
	public static Label createValueLabel(Composite parent, String text) {
		createLabel(parent, text);

		return createLabel(parent, null);
	}

	/**
	 * Creates a new button.
	 * 
	 * @param parent
	 *            The parent composite.
	 * @param text
	 *            The text of the button.
	 * @return A new button
	 */
	public static Button createButton(Composite parent, String text) {
		Button toReturn = new Button(parent, SWT.PUSH);
		toReturn.setFont(parent.getFont());
		toReturn.setText(text);
		toReturn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		return toReturn;
	}

	/**
	 * Creates a new table widget.
	 * 
	 * @param parent
	 *            The parent composite.
	 * @return The new table
	 */
	public static Table createTable(Composite parent) {
		Table toReturn = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		data.heightHint = toReturn.getItemHeight();
		data.horizontalSpan = 1;
		toReturn.setLayoutData(data);

		return toReturn;
	}

	/**
	 * Creates a new table widget.
	 * 
	 * @param parent
	 *            The parent composite.
	 * @return The new table
	 */
	public static Table createTableMultiSelection(Composite parent) {
		Table toReturn = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER | SWT.MULTI);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		data.heightHint = toReturn.getItemHeight();
		data.horizontalSpan = 1;
		toReturn.setLayoutData(data);

		return toReturn;
	}

	/**
	 * Creates a new table column.
	 * 
	 * @param table
	 *            The table of the column.
	 * @param text
	 *            The column text.
	 * @return A new table column
	 */
	public static TableColumn createTableColumn(Table table, String text) {
		TableColumn toReturn = new TableColumn(table, SWT.NONE);
		toReturn.setText(text);

		return toReturn;
	}

	/**
	 * Creates a new table item.
	 * 
	 * @param table
	 *            The table of the table item.
	 * @param image
	 *            The image of the item.
	 * @param s
	 *            The text of the item.
	 * @return The new table item.
	 */
	public static TableItem createTableItem(Table table, Image image, String s) {
		TableItem toReturn = new TableItem(table, SWT.NONE);
		toReturn.setText(s);
		if (image != null) {
			toReturn.setImage(image);
		}

		return toReturn;
	}

	/**
	 * Creates a collection of table items.
	 * 
	 * @param table
	 *            The table of the table item.
	 * @param image
	 *            The image of the item.
	 * @param s
	 *            The text of the item.
	 * @return The new table item.
	 */
	public static TableItem createTableItem(Table table, Image image, String... s) {
		TableItem toReturn = new TableItem(table, SWT.NONE);
		toReturn.setText(s);
		if (image != null) {
			toReturn.setImage(image);
		}

		return toReturn;
	}

	/**
	 * Creates a new text.
	 * 
	 * @param parent
	 *            The parent composite.
	 * @return The new text.
	 */
	public static Text createText(Composite parent) {
		Text toReturn = new Text(parent, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		toReturn.setLayoutData(data);

		return toReturn;
	}

	/**
	 * Creates a new tree item. If the text parameter is null, it will appear is
	 * after the field parameter.
	 * 
	 * @param itemParent
	 *            The parent tree item
	 * @param image
	 *            The image of the tree item.
	 * @param field
	 *            The field.
	 * @param text
	 *            The text of the tree item.
	 * @return The new tree item.
	 */
	public static TreeItem createTreeItem(TreeItem itemParent, Image image, String field, String text) {
		TreeItem toReturn = new TreeItem(itemParent, 0);
		toReturn.setText((text == null) ? field : field + " (" + text + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		toReturn.setImage(image);
		toReturn.setExpanded(true);

		return toReturn;
	}

	/**
	 * Creates a new horizontal tool bar.
	 * 
	 * @param parent
	 *            The parent tool bar item.
	 * @return A new tool bar item.
	 */
	public static ToolBar createHorizontalToolBar(Composite parent) {
		return new ToolBar(parent, SWT.HORIZONTAL);
	}

	/**
	 * Creates a new tool item like a push button
	 * 
	 * @param toolBar
	 *            The parent tool bar item.
	 * @return A new tool item.
	 */
	public static ToolItem createPushToolItem(ToolBar toolBar) {
		return new ToolItem(toolBar, SWT.PUSH);
	}

	/**
	 * Creates a titled group.
	 * 
	 * @param parent
	 *            The parent composite
	 * @param title
	 *            The desired title.
	 * @return The titled group
	 */
	public static Group createTitledGroup(Composite parent, String title) {
		Group toReturn = new Group(parent, SWT.SHADOW_NONE);
		toReturn.setText(title);
		toReturn.setLayout(new GridLayout());
		toReturn.setLayoutData(new GridData(GridData.FILL_BOTH));
		return toReturn;
	}

	/**
	 * Creates a titled group with the given number of columns.
	 * 
	 * @param parent
	 *            The parent composite.
	 * @param title
	 *            The group title.
	 * @param numColumns
	 *            The number of columns.
	 * @return The created group.
	 */
	public static Group createTitledGroup(Composite parent, String title, int numColumns) {
		Group toReturn = new Group(parent, SWT.SHADOW_NONE);
		toReturn.setText(title);

		toReturn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		GridLayout layout = createGridLayout(numColumns);
		layout.marginHeight = GROUP_MARGIN_HEIGHT;
		layout.marginWidth = GROUP_MARGIN_WIDTH;
		toReturn.setLayout(layout);
		return toReturn;
	}

	/**
	 * Creates a group
	 * 
	 * @param parent
	 *            The parent composite
	 * @return The group
	 */
	public static Group createGroup(Composite parent) {
		Group toReturn = new Group(parent, SWT.SHADOW_NONE);
		return toReturn;
	}

	/**
	 * Creates a radio button
	 * 
	 * @param parent
	 *            The parent composite
	 * @param text
	 *            The given text.
	 * @return The radio button
	 */
	public static Button createRadioButton(Composite parent, String text) {
		Button toReturn = new Button(parent, SWT.RADIO);
		toReturn.setText(text);
		toReturn.setLayoutData(new GridData(GridData.FILL_BOTH));
		return toReturn;
	}

	/**
	 * Creates a list.
	 * 
	 * @param parent
	 *            The parent composite.
	 * @return The list.
	 */
	public static List createList(Composite parent) {
		return new List(parent, SWT.BORDER | SWT.SINGLE);
	}

}
