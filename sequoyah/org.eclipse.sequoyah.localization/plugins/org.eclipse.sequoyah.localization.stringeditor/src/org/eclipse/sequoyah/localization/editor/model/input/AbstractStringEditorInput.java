/********************************************************************************
 * Copyright (c) 2009-2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Marcelo Marzola Bossoni (Eldorado)
 * Matheus Tait Lima (Eldorado)
 * 
 * Contributors:
 * Vinicius Rigoni Hernandes (Eldorado) - Bug [289885] - Localization Editor doesn't recognize external file changes
 * Marcel Gorri (Eldorado) - Adapting localization framework for automatic translations
 * Marcelo Marzola Bossoni (Eldorado) - Fix erroneous externalized strings/make this editor a multipage one
 * Paulo Faria (Eldorado) - Bug [326793] -  Fixing undo/redo edit for array items
 * Marcelo Marzola Bossoni (Instituto de Pesquisas Eldorado) - Bug [353518] - Return messages from translator errors 
 ********************************************************************************/
package org.eclipse.sequoyah.localization.editor.model.input;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahException;
import org.eclipse.sequoyah.localization.editor.datatype.CellInfo;
import org.eclipse.sequoyah.localization.editor.datatype.ColumnInfo;
import org.eclipse.sequoyah.localization.editor.datatype.RowInfo;
import org.eclipse.sequoyah.localization.editor.datatype.TranslationInfo;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;

public abstract class AbstractStringEditorInput implements IEditorInput {

	private final List<IInputChangeListener> inputChangeListeners = new ArrayList<IInputChangeListener>();

	private final List<IEditorChangeListener> fileChangeListeners = new ArrayList<IEditorChangeListener>();

	public abstract void init(IProject project) throws Exception;

	/**
	 * Return the Editor Title
	 * 
	 * @return the Editor Title
	 */
	public abstract String getTitle();

	/**
	 * Get the columns IDs to be shown inside this editor
	 * 
	 * @return a list with all available columns.
	 */
	public abstract List<ColumnInfo> getColumns();

	/**
	 * Translate from a source column ID to a destination ID
	 * 
	 * @param srcColumnID
	 * @param newColumnInfo
	 * @param monitor
	 * @return true if the translation was succefull
	 */
	public abstract IStatus translateColumn(String srcColumnID,
			TranslationInfo newColumnInfo, IProgressMonitor monitor);

	/**
	 * Translate from a source column ID to a destination ID
	 * 
	 * @param srcColumnID
	 * @param newColumnInfo
	 * @param monitor
	 * @return true if the translation was succefull
	 */
	public abstract IStatus translateCells(String srcColumnID,
			TranslationInfo[] newColumnInfo, IProgressMonitor monitor);

	/**
	 * Add a new column ID to the editor input
	 */
	public abstract void addColumn(String ID);

	/**
	 * Remove the follow column ID of the input
	 * 
	 * @param id
	 *            the column id
	 */
	public abstract void removeColumn(String id);

	/**
	 * Set a translation value to the following key (array or single string) in
	 * the following language
	 * 
	 * @param columnID
	 *            the column id
	 * @param key
	 *            the key
	 * @param value
	 *            the value
	 * 
	 * @throws SequoyahException
	 */
	public abstract void setValue(String columnID, String key, String value)
			throws SequoyahException;

	/**
	 * Set a translation value to the following key (array item) in the
	 * following language
	 * 
	 * @param columnID
	 *            the column id
	 * @param key
	 *            the key
	 * @param value
	 *            the value
	 * @param index
	 *            index in the give array
	 * 
	 * @throws SequoyahException
	 */
	public abstract void setValue(String columnID, String key, String value,
			int index) throws SequoyahException;

	/**
	 * Get the value of a certain key at following column
	 * 
	 * @param columnID
	 *            the column ID
	 * @param key
	 *            the key
	 * @return the value of the key in the column
	 */
	public abstract CellInfo getValue(String columnID, String key);

	/**
	 * Get the values of a column
	 * 
	 * @param columnID
	 *            the column ID
	 * @return a map of key values for the column
	 */
	public abstract Map<String, CellInfo> getValues(String columnID);

	/**
	 * 
	 * @param columnID
	 *            the column ID
	 * @return a list with all available keys inside of that
	 */
	public abstract List<CellInfo> getAvailableKeysForColumn(String columnID);

	/**
	 * Check if this input can be saved
	 * 
	 * @return true if can be saved, false otherwise
	 */
	public abstract boolean canSave();

	/**
	 * Save this editor input
	 * 
	 * @return true if successfully saved, false otherwise
	 */
	public abstract boolean save();

	/**
	 * Check if this input has unsaved changes
	 * 
	 * @return true if input has unsaved changes, false otherwise
	 */
	public abstract boolean isDirty();

	/**
	 * revert all input to a saved state
	 * 
	 * @return true if operation succeded, false otherwise
	 */
	public abstract boolean revert();

	/**
	 * Revert the following columnID to a saved state
	 * 
	 * @param columnID
	 * @return true if ok, false otherwise
	 * @throws IOException
	 */
	public abstract boolean revert(String columnID) throws IOException;

	/**
	 * 
	 * @return true if columns can be reverted individually
	 */
	public abstract boolean canRevertByColumn();

	/**
	 * Remove the top level row with the following key from all columns
	 * 
	 * @param key
	 */
	public abstract void removeRow(String key);

	/**
	 * Remove the child row with the following key, index from all columns
	 * 
	 * @param key
	 */
	public abstract void removeChildRow(String key, int index);

	/**
	 * Remove the cell with the following key from the following column
	 * 
	 * @param key
	 */
	public abstract void removeCell(String key, String columnID);

	/**
	 * Remove the cell with the following key from the following column into the
	 * given position
	 * 
	 * @param key
	 * @param columnID
	 * @param position
	 */
	public abstract void removeCell(String key, String columnID, int position);

	/**
	 * Add a new row based on the following {@link RowInfo}
	 * 
	 * @param row
	 */
	public abstract RowInfo addRow(RowInfo row);

	/**
	 * dispose this editor input. This method is called when editor is being
	 * disposed
	 */
	public abstract void dispose();

	/**
	 * Set the cell tooltip
	 * 
	 * @param columnID
	 * @param key
	 * @param tooltip
	 * @throws SequoyahException
	 */
	public abstract void setCellTooltip(String columnID, String key,
			String tooltip) throws SequoyahException;

	/**
	 * Set the cell tooltip
	 * 
	 * @param columnID
	 * @param key
	 * @param tooltip
	 * @throws SequoyahException
	 */

	public abstract void setCellTooltip(String text, String key,
			String tooltipText, Integer position) throws SequoyahException;

	/**
	 * Do a Lightwight validation of the entire input This validation shall not
	 * took much time to avoid lack of editor performance Preferentially,
	 * validate top level errors, not doing a "bitwise verification"
	 * 
	 * @return the IStatus object of the validation results
	 */

	public abstract IStatus validate();

	/**
	 * Check if this Input can handle the file passed as argument
	 * 
	 * @param file
	 *            the file to be handled
	 * @return true if can handle the file, false otherwise
	 */
	public abstract boolean canHandle(IFile file);

	/**
	 * Add a listener that will be notified when there is a change in the editor
	 * input
	 * 
	 * @param inputChangeListener
	 *            IInputChangeListener object
	 */
	public void addInputChangeListener(IInputChangeListener inputChangeListener) {
		if (!inputChangeListeners.contains(inputChangeListener)) {
			inputChangeListeners.add(inputChangeListener);
		}
	}

	/**
	 * Remove a listener from the list of listeners which are notified when
	 * there is a change in the editor input
	 * 
	 * @param inputChangeListener
	 *            IInputChangeListener object
	 */
	public void removeInputChangeListener(
			IInputChangeListener inputChangeListener) {
		inputChangeListeners.remove(inputChangeListener);
	}

	/**
	 * Notify a change in the editor input to the editor part
	 * 
	 * @param columnID
	 *            the column that changed
	 */
	public void notifyInputChanged(String columnID) {
		for (IInputChangeListener inputChangeListener : inputChangeListeners) {
			inputChangeListener.columnChanged(columnID);
		}
	}

	/**
	 * Add a listener that will be notified when there is a change in the editor
	 * input
	 * 
	 * @param inputChangeListener
	 *            IInputChangeListener object
	 */
	public void addEditorChangeListener(
			IEditorChangeListener inputChangeListener) {
		if (!fileChangeListeners.contains(inputChangeListener)) {
			fileChangeListeners.add(inputChangeListener);
		}
	}

	/**
	 * Remove a listener from the list of listeners which are notified when
	 * there is a change in the editor input
	 * 
	 * @param inputChangeListener
	 *            IInputChangeListener object
	 */
	public void removeEditorChangeListener(
			IEditorChangeListener inputChangeListener) {
		fileChangeListeners.remove(inputChangeListener);
	}

	/**
	 * Notify a change in the editor input to the editor part
	 * 
	 * @param columnID
	 *            the column that changed
	 */
	public void notifyEditorChanged(IEditorInput fileChanged, String newContent) {
		for (IEditorChangeListener fileChangeListener : fileChangeListeners) {
			fileChangeListener.editorContentChanged(fileChanged, newContent);
		}
	}

	/**
	 * Get the list of files used by this input. This list will be used to
	 * populate the text pages of the editor If these input files are not
	 * visible/editable through a text editor return null
	 * 
	 * @return the list of files used by the editor or null if you don't want to
	 *         show text pages (or if your files aren't text one)
	 */
	public abstract List<IFile> getFiles();

	/**
	 * Get the source page name for the given file.
	 * 
	 * @param file
	 * @return the name of the page to the file
	 */
	public abstract String getSourcePageNameForFile(IFile file);

	/**
	 * Get the content of this input associated with the editor input At this
	 * time the content of the "UI" page and the source page may differ, so the
	 * input shall give their current state as text to set to the source editor.
	 * NOTE: This method will only be called if source pages exists
	 * 
	 * @param editorInput
	 *            the input associated with the source editor
	 * @return the String representation of the actual input state
	 */
	public abstract String getContentForFileAsText(IFileEditorInput editorInput);

	/**
	 * Rename a key to a new name
	 * 
	 * @param oldName
	 *            the old name of the key
	 * @param newName
	 *            the new name of the key
	 * @throws SequoyahException
	 */
	public abstract void renameKey(String oldName, String newName)
			throws SequoyahException;
	
	/**
	 * Returns if the column can be reverted to saved
	 * 
	 * @param columnID
	 * @return
	 */
	public abstract boolean isRevertable(String columnID);
}
