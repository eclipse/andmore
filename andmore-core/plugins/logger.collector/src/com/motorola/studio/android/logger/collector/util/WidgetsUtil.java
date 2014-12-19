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

package com.motorola.studio.android.logger.collector.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;

/**
 * Class with useful methods for widgets.
 */
public class WidgetsUtil {

	/**
	 * This method test if a given String is null or empty.
	 * 
	 * @param s
	 *            The String
	 * @return <code>true</code> if the String is null or empty,
	 *         <code>false</code> otherwise.
	 */
	private static boolean isNullOrEmpty(String s) {
		return ((s != null) && s.trim().equals("")); //$NON-NLS-1$
	}

	/**
	 * The method verify if the file exist.
	 * 
	 * @param fileName
	 *            The full path for file.
	 * @return <code>true</code> if the file exist, <code>false</code>
	 *         otherwise.
	 */
	public static boolean fileExist(String fileName) {
		return !isNullOrEmpty(fileName) && new File(fileName).exists();
	}

	/**
	 * This method test if some StringFieldEditor value of the given collection
	 * is null or empty.
	 * 
	 * @param editors
	 * @return <code>true</code> if some StringFieldEditor value is null or
	 *         empty, <code>false</code> otherwise.
	 */
	public static boolean isNullOrEmpty(StringFieldEditor... editors) {
		for (StringFieldEditor editor : editors) {
			if (isNullOrEmpty(editor)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * This method test if a StringFieldEditor value is null or empty.
	 * 
	 * @param editor
	 *            The StringFieldEditor
	 * @return <code>true</code> if the StringFieldEditor value is null or
	 *         empty, <code>false</code> otherwise.
	 */
	public static boolean isNullOrEmpty(StringFieldEditor editor) {
		return ((editor != null) && isNullOrEmpty(editor.getStringValue()));
	}

	/**
	 * This method test if a StringFieldEditor value contains a invalid
	 * character.
	 * 
	 * @param editor
	 *            The StringFieldEditor
	 * @return <code>true</code> if the StringFieldEditor value contains invalid
	 *         character, <code>false</code> otherwise.
	 */
	public static boolean checkExistInvalidCharacter(StringFieldEditor editor, String invalidChars) {
		for (int i = 0; i < invalidChars.length(); i++) {
			String invalidChar = invalidChars.substring(i, i + 1);
			if (editor.getStringValue().contains(invalidChar)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * This method test if a Text value is null or empty.
	 * 
	 * @param text
	 *            The Text
	 * @return <code>true</code> if the Text value is null or empty,
	 *         <code>false</code> otherwise.
	 */
	public static boolean isNullOrEmpty(Text text) {
		return ((text != null) && isNullOrEmpty(text.getText()));
	}

	/**
	 * This method test if a FileFieldEditor value is null or empty.
	 * 
	 * @param editor
	 *            The FileFieldEditor
	 * @return <code>true</code> if the FileFieldEditor value is null or empty,
	 *         <code>false</code> otherwise.
	 */
	public static boolean isEmpty(FileFieldEditor editor) {
		return isNullOrEmpty(editor.getStringValue());
	}

	/**
	 * This method test if a Combo value is null or empty.
	 * 
	 * @param combo
	 *            The Combo
	 * @return <code>true</code> if the Combo value is null or not selected,
	 *         <code>false</code> otherwise.
	 */
	public static boolean isNullOrDeselected(Combo combo) {
		return ((combo != null) && (combo.getSelectionIndex() == -1));
	}

	/**
	 * Returns the size of file.
	 * 
	 * @param fileName
	 *            The file name.
	 * @return The size of file.
	 */
	public static long fileSize(String fileName) {
		return new File(fileName).length();
	}

	/**
	 * This method test if a Table has one or more lines.
	 * 
	 * @param table
	 *            The table
	 * @return <code>true</code> if the Table has one or more lines,
	 *         <code>false</code> otherwise.
	 */
	public static boolean isNullOrEmpty(Table table) {
		return table.getItemCount() > 0;
	}

	/**
	 * Executes a wizard.
	 * 
	 * @param wizard
	 *            The wizard.
	 * @return <code>true</code> if the Wizard dialog has constant OK,
	 *         <code>false</code> otherwise .
	 */
	public static boolean runWizard(IWizard wizard) {
		Shell activeShell = Display.getCurrent().getActiveShell();
		WizardDialog dialog = new WizardDialog(activeShell, wizard);

		try {
			dialog.create();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		centerDialog(dialog);
		return dialog.open() == Window.OK;
	}

	/**
	 * Opens the Eclipse preferences dialog and selects the page of the given
	 * id.
	 * 
	 * @param shell
	 *            The shell.
	 * @param selectedNode
	 *            The preferences page to selec.
	 * @return <code>true</code> if the Wizard dialog has constant OK,
	 *         <code>false</code> otherwise .
	 */
	public static boolean runPreferencePage(Shell shell, String selectedNode) {
		PreferenceManager manager = PlatformUI.getWorkbench().getPreferenceManager();
		PreferenceDialog dialog = new PreferenceDialog(shell, manager);
		dialog.setSelectedNode(selectedNode);
		WidgetsUtil.centerDialog(shell);
		return dialog.open() == Window.OK;
	}

	/**
	 * Center the dialog.
	 * 
	 * @param shell
	 *            The shell.
	 */
	public static void centerDialog(Shell shell) {
		Monitor primary = shell.getDisplay().getPrimaryMonitor();
		Rectangle bounds = primary.getBounds();
		Rectangle rect = shell.getBounds();
		int x = bounds.x + (bounds.width - rect.width) / 2;
		int y = bounds.y + (bounds.height - rect.height) / 2;
		shell.setLocation(x, y);
	}

	/**
	 * Center the dialog.
	 * 
	 * @param dialog
	 *            The dialog.
	 */
	public static void centerDialog(Dialog dialog) {
		centerDialog(dialog.getShell());
	}

	/**
	 * Check the leaf items of the given tree.
	 * 
	 * @param tree
	 *            The tree.
	 * @return A collection containing the leaf tree items.
	 */
	public static List<TreeItem> getCheckedLeafItems(Tree tree) {
		List<TreeItem> toReturn = new ArrayList<TreeItem>();
		selectCheckedLeafItems(tree.getItems(), toReturn);
		return toReturn;
	}

	/**
	 * Returns a list of the leaf nodes that are checked.
	 * 
	 * @param items
	 *            The parent items.
	 * @param list
	 *            A list of the leaf nodes that are checked.
	 */
	private static void selectCheckedLeafItems(TreeItem[] items, List<TreeItem> list) {
		int len = items.length;
		for (int i = 0; i < len; i++) {
			if (items[i].getItemCount() > 0) {
				selectCheckedLeafItems(items[i].getItems(), list);
			} else if (items[i].getChecked()) {
				list.add(items[i]);
			}
		}
	}

	/**
	 * Expand all the given tree items.
	 * 
	 * @param items
	 *            The tree items.
	 */
	public static void expandAll(TreeItem[] items) {
		for (int i = 0; i < items.length; i++) {
			if (items[i].getItems().length > 0) {
				items[i].setExpanded(true);
				expandAll(items[i].getItems());
			}
		}
	}

	/**
	 * Returns the full path of a given tree item.
	 * 
	 * @param item
	 *            The tree item.
	 * @return The full path of a given tree item.
	 */
	public static String getFullPathTreeItem(TreeItem item) {
		String toReturn = item.getText();
		if (item != null) {
			if (item.getParentItem() != null) {
				toReturn = getFullPathTreeItem(item.getParentItem()) + "." + toReturn; //$NON-NLS-1$
			}
		}
		return toReturn;
	}

	/**
	 * This method verifies if a given file can be read.
	 * 
	 * @param fileName
	 *            the full file path.
	 * @return <code>true</code> if read permission is granted,
	 *         <code>false</code> otherwise.
	 */
	public static boolean canRead(String fileName) {
		return !isNullOrEmpty(fileName) && new File(fileName).canRead();
	}

	/**
	 * This method verifies if a given file has the read and write permissions
	 * granted.
	 * 
	 * @param fileName
	 *            The file
	 * @return <code>true</code> if permissions are granted, <code>false</code>
	 *         otherwise.
	 */
	public static boolean canReadWrite(String fileName) {
		File file = new File(fileName);
		return file.canRead() && file.canWrite();
	}

	/**
	 * This method simulates a refresh in a Composite object.
	 * 
	 * @param composite
	 *            A composite object.
	 */
	public static void refreshComposite(Composite composite) {
		for (Composite parent = composite.getParent(); parent != null; parent = parent.getParent()) {
			parent.layout();
		}
	}

	/**
	 * Check the leaf items of the given table.
	 * 
	 * @param table
	 *            The table.
	 * @return A collection containing the leaf table items.
	 */
	public static List<TableItem> getCheckedLeafItems(Table table) {
		List<TableItem> toReturn = new ArrayList<TableItem>();
		selectCheckedLeafItems(table.getItems(), toReturn);
		return toReturn;
	}

	/**
	 * Returns a list of the leaf nodes that are checked.
	 * 
	 * @param items
	 *            The parent items.
	 * @param list
	 *            A list of the leaf nodes that are checked.
	 */
	private static void selectCheckedLeafItems(TableItem[] items, List<TableItem> list) {
		int len = items.length;
		for (int i = 0; i < len; i++) {
			if (items[i].getChecked()) {
				list.add(items[i]);
			}
		}
	}
}
