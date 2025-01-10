/********************************************************************************
 * Copyright (c) 2009-2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Matheus Tait Lima (Eldorado)
 * Vinicius Hernandes (Motorola)
 * 
 * Contributors:
 * Marcelo Marzola Bossoni (Eldorado) - Bug [289146] - Performance and Usability Issues
 * Marcelo Marzola Bossoni (Eldorado) - Bug (289282) - NullPointer adding keyNullPointer adding key
 * Vinicius Rigoni Hernandes (Eldorado) - Bug [289885] - Localization Editor doesn't recognize external file changes
 * Matheus Tait Lima (Eldorado) - Bug [300351] - Optimizing performance when translating a whole column at once
 * Marcelo Marzola Bossoni (Eldorado) - Fix erroneous externalized strings/provide implementation to the new methods
 * Fabricio Violin (Eldorado) - Bug [316029] - Fix array behavior when switching between tabs
 * Fabricio Violin (Eldorado) - Bug [317065] - Localization file initialization bug
 * Marcel Augusto Gorri (Eldorado) - Bug 323036 - Add support to other Localizable Resources 
 * Paulo Faria (Eldorado) - Bug [326793] -  Improvements on the String Arrays handling
 * Thiago Junqueira (Eldorado) - Bug [326793] - Initial implementation of the renameKey method. 
 * Fabricio Nallin Violin(Eldorado) - Bug [326793] - Added translation support to array items.
 * Carlos Alberto Souto Junior (Eldorado) - Bug [326793] - Added new tooltip support method for StringArrayItem
 * Paulo Faria (Eldorado) - Bug 326793 -  Fix: Array item was moving from one line to the other in non-default languages
 * Daniel Drigo Pastore (Eldorado) - Bug [326793] - Remove column: mark file for deletion based on filename
 * Marcelo Marzola Bossoni (Instituto de Pesquisas Eldorado) - Bug [353518] - Return messages from translator errors
 ********************************************************************************/
package org.eclipse.sequoyah.localization.tools.editor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.sequoyah.device.common.utilities.BasePlugin;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahException;
import org.eclipse.sequoyah.device.common.utilities.exception.SequoyahExceptionStatus;
import org.eclipse.sequoyah.localization.editor.datatype.CellInfo;
import org.eclipse.sequoyah.localization.editor.datatype.ColumnInfo;
import org.eclipse.sequoyah.localization.editor.datatype.RowInfo;
import org.eclipse.sequoyah.localization.editor.datatype.RowInfoLeaf;
import org.eclipse.sequoyah.localization.editor.datatype.TranslationInfo;
import org.eclipse.sequoyah.localization.editor.model.input.AbstractStringEditorInput;
import org.eclipse.sequoyah.localization.editor.model.input.IEditorChangeListener;
import org.eclipse.sequoyah.localization.tools.LocalizationToolsPlugin;
import org.eclipse.sequoyah.localization.tools.datamodel.LocaleInfo;
import org.eclipse.sequoyah.localization.tools.datamodel.LocalizationFile;
import org.eclipse.sequoyah.localization.tools.datamodel.LocalizationFileBean;
import org.eclipse.sequoyah.localization.tools.datamodel.StringLocalizationFile;
import org.eclipse.sequoyah.localization.tools.datamodel.node.NodeComment;
import org.eclipse.sequoyah.localization.tools.datamodel.node.StringArrayItemNode;
import org.eclipse.sequoyah.localization.tools.datamodel.node.StringArrayNode;
import org.eclipse.sequoyah.localization.tools.datamodel.node.StringNode;
import org.eclipse.sequoyah.localization.tools.datamodel.node.TranslationResult;
import org.eclipse.sequoyah.localization.tools.extensions.classes.ILocalizationSchema;
import org.eclipse.sequoyah.localization.tools.extensions.classes.ITranslator;
import org.eclipse.sequoyah.localization.tools.i18n.Messages;
import org.eclipse.sequoyah.localization.tools.managers.LocalizationManager;
import org.eclipse.sequoyah.localization.tools.managers.LocalizationManager.IFileChangeListener;
import org.eclipse.sequoyah.localization.tools.managers.ProjectLocalizationManager;
import org.eclipse.sequoyah.localization.tools.managers.TranslatorManager;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPersistableElement;

/***
 * 
 * This class implements a IStringEditorInput and provides the necessary methods
 * for the Localization Files Editor to work with (edit, save, translate, etc)
 * localization files.
 * 
 */
public class StringEditorInput extends AbstractStringEditorInput {

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.sequoyah.localization.editor.editor.input.
	 * IStringEditorInput #getTitle()
	 */
	@Override
	public String getTitle() {
		return projectLocalizationManager.getLocalizationProject().getProject()
				.getName();
	}

	/*
	 * The Project Localization Manager used as a source to get all information
	 * provided by this class
	 */
	private ProjectLocalizationManager projectLocalizationManager = null;

	private final IFileChangeListener fileChangeListener = new IFileChangeListener() {

		public IProject getProject() {
			return projectLocalizationManager.getLocalizationProject()
					.getProject();
		}

		public void fileChanged(IFile file) {
			notifyInputChanged(getColumnID(file));
		}
	};

	private final IEditorChangeListener editorChangeListener = new IEditorChangeListener() {

		public void editorContentChanged(IEditorInput input, String newContent) {
			IFileEditorInput fileInput = input instanceof IFileEditorInput ? (IFileEditorInput) input
					: null;
			if (fileInput != null) {
				LocalizationFile locFile = projectLocalizationManager
						.getLocalizationProject().getLocalizationFile(
								fileInput.getFile());
				try {
					projectLocalizationManager.getProjectLocalizationSchema()
							.updateLocalizationFileContent(locFile, newContent);
					notifyInputChanged(getColumnID(locFile.getFile()));
				} catch (SequoyahException e) {
					BasePlugin.logError(
							"Impossible to update file content for file: " //$NON-NLS-1$
									+ locFile.getFile().getFullPath(), e);
				}
			}
		}
	};

	/*
	 * Localization icon
	 */
	private final String LOCALIZATION_ICON = "icons/loc_icon.png"; //$NON-NLS-1$

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.sequoyah.localization.editor.editor.input.
	 * IStringEditorInput #addRow(org
	 * .eclipse.sequoyah.stringeditor.datatype.RowInfo)
	 */
	@Override
	public RowInfo addRow(RowInfo row) {

		RowInfo rowInfo = row;

		String key = row.getKey();
		boolean isArray = false;

		Map<String, CellInfo> cells = null;
		Set<String> columns = null;

		if (row instanceof RowInfoLeaf) {
			// can be array item or single string
			RowInfoLeaf leaf = (RowInfoLeaf) row;
			cells = leaf.getCells();
		} else if (row instanceof RowInfo) {
			// array
			// empty line of cells
			cells = new HashMap<String, CellInfo>();
			isArray = true;
		}
		columns = cells.keySet();

		ILocalizationSchema schema = projectLocalizationManager
				.getProjectLocalizationSchema();

		// add at least to the default column
		if (columns.size() == 0) {
			LocaleInfo locale = schema.getLocaleInfoFromID(schema
					.getDefaultID());
			if (locale != null) {
				LocalizationFile mainFile = projectLocalizationManager
						.getLocalizationProject().getLocalizationFile(locale);
				StringNode stringNode = ((StringLocalizationFile) mainFile)
						.getStringNodeByKey(row.getKey());
				if (stringNode == null) {
					// not found => create a new one
					((StringLocalizationFile) mainFile).createNode(rowInfo, "", //$NON-NLS-1$
							""); //$NON-NLS-1$
				} else if (stringNode instanceof StringArrayNode) {
					// array item need to be inserted
					((StringLocalizationFile) mainFile).createNode(rowInfo, "", //$NON-NLS-1$
							""); //$NON-NLS-1$
				}
			}
		}

		addNodeForEachColumnNonDefault(rowInfo, key, isArray, cells, columns,
				schema);

		return rowInfo;
	}

	private void addNodeForEachColumnNonDefault(RowInfo rowInfo, String key,
			boolean isArray, Map<String, CellInfo> cells, Set<String> columns,
			ILocalizationSchema schema) {
		for (Iterator<String> iterator = columns.iterator(); iterator.hasNext();) {
			String column = iterator.next();

			LocaleInfo info = schema.getLocaleInfoFromID(column);
			LocalizationFile file = projectLocalizationManager
					.getLocalizationProject().getLocalizationFile(info);
			String comment = (cells.get(column)) != null ? (cells.get(column))
					.getComment() : null;

			String value = cells.get(column) != null ? cells.get(column)
					.getValue() : null;
			StringNode newNode = ((StringLocalizationFile) file).createNode(
					rowInfo, value, comment);

			rowInfo.setKey(newNode.getKey());
			if (rowInfo instanceof RowInfoLeaf) {
				// update value on screen (as we modified the node value)
				RowInfoLeaf leaf = (RowInfoLeaf) rowInfo;
				if (leaf.getCells().get(column) != null) {
					leaf.getCells().get(column).setValue(value);
				}
			}
		}
	}

	/**
	 * Returns the project localization manager associated with this editor
	 * input.
	 * 
	 * @return the project localization manager
	 */
	public ProjectLocalizationManager getProjectLocalizationManager() {
		return projectLocalizationManager;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.sequoyah.localization.editor.editor.input.
	 * IStringEditorInput #removeRow (java.lang.String)
	 */
	@Override
	public void removeRow(String key) {
		List<LocalizationFile> files = projectLocalizationManager
				.getLocalizationProject().getLocalizationFiles();

		for (Iterator<LocalizationFile> iterator = files.iterator(); iterator
				.hasNext();) {
			LocalizationFile localizationFile = iterator.next();
			StringNode node = ((StringLocalizationFile) localizationFile)
					.getStringNodeByKey(key);
			if (node != null) {
				((StringLocalizationFile) localizationFile)
						.removeStringNode(node);
			}
		}
	}

	@Override
	public void removeChildRow(String key, int index) {
		List<LocalizationFile> files = projectLocalizationManager
				.getLocalizationProject().getLocalizationFiles();

		for (Iterator<LocalizationFile> iterator = files.iterator(); iterator
				.hasNext();) {
			LocalizationFile localizationFile = iterator.next();
			StringLocalizationFile stringFile = (StringLocalizationFile) localizationFile;
			StringNode node = stringFile.getStringNodeByKey(key);
			if (node instanceof StringArrayNode) {
				StringArrayNode arrayNode = (StringArrayNode) node;
				StringArrayItemNode arrayItemNode = arrayNode
						.getArrayItemByIndex(index);
				if (arrayItemNode != null) {
					stringFile.removeStringArrayItemNode(arrayItemNode);
				}
			}
		}
	}

	/**
	 * Instantiate the Project Localization Manager
	 * 
	 * @throws SequoyahException
	 * 
	 * @see org.eclipse.sequoyah.localization.editor.model.input.AbstractStringEditorInput#init(org.eclipse.core.resources.IProject)
	 */
	@Override
	public void init(final IProject project) throws SequoyahException {
		try {
			projectLocalizationManager = LocalizationManager.getInstance()
					.getProjectLocalizationManager(project, true);

			LocalizationManager.getInstance().addFileChangeListener(
					fileChangeListener);

			addEditorChangeListener(editorChangeListener);

			if (projectLocalizationManager == null) {

				Status status = new Status(Status.ERROR,
						LocalizationToolsPlugin.PLUGIN_ID,
						Messages.StringEditorInput_ErrorInitializingEditor);
				throw new SequoyahException(new SequoyahExceptionStatus(status));
			}
		} catch (IOException ioe) {
			Status status = new Status(Status.ERROR,
					LocalizationToolsPlugin.PLUGIN_ID,
					Messages.StringEditorInput_FileMalformed);
			throw new SequoyahException(new SequoyahExceptionStatus(status));
		}
	}

	private String getColumnID(IFile file) {
		return file.getFullPath().removeLastSegments(1).lastSegment();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.sequoyah.localization.editor.editor.input.
	 * IStringEditorInput #getColumns()
	 */
	@Override
	public List<ColumnInfo> getColumns() {
		List<LocalizationFile> files = projectLocalizationManager
				.getLocalizationProject().getLocalizationFiles();
		List<ColumnInfo> columns = new ArrayList<ColumnInfo>();

		ILocalizationSchema schema = projectLocalizationManager
				.getProjectLocalizationSchema();
		String defaultID = projectLocalizationManager
				.getProjectLocalizationSchema().getDefaultID();
		for (Iterator<LocalizationFile> iterator = files.iterator(); iterator
				.hasNext();) {
			LocalizationFile localizationFile = iterator.next();

			String columnID = getColumnID(localizationFile.getFile());
			String toolTip = schema.getLocaleToolTip(localizationFile.getFile()
					.getFullPath());

			// Iterating over strings
			List<StringNode> localizationNodes = ((StringLocalizationFile) localizationFile)
					.getStringNodes();
			Map<String, CellInfo> cells = new HashMap<String, CellInfo>();
			for (Iterator<StringNode> nodes = localizationNodes.iterator(); nodes
					.hasNext();) {
				StringNode stringNode = nodes.next();
				String comment = ((stringNode.getNodeComment() != null) ? stringNode
						.getNodeComment().getComment() : ""); //$NON-NLS-1$
				CellInfo info = new CellInfo(stringNode.getValue(), comment);
				cells.put(stringNode.getKey(), info);
			}

			// Iterating over arrays
			List<StringArrayNode> arrayNodes = ((StringLocalizationFile) localizationFile)
					.getStringArrays();
			for (Iterator<StringArrayNode> nodes = arrayNodes.iterator(); nodes
					.hasNext();) {
				StringArrayNode stringNode = nodes.next();
				// List<String> contentOfThisArray =
				// stringNode.getStringValues();
				List<StringArrayItemNode> contentOfThisArray = stringNode
						.getValues();
				CellInfo arrayCell = new CellInfo(true);
				for (StringNode item : contentOfThisArray) {
					String comment = (item.getNodeComment() != null) ? item
							.getNodeComment().getComment() : ""; //$NON-NLS-1$
					CellInfo info = new CellInfo(item.getValue(), comment);
					arrayCell.addChild(info);
				}
				cells.put(stringNode.getKey(), arrayCell);
			}

			columns.add(new ColumnInfo(columnID, toolTip, cells, columnID
					.equals(defaultID) ? false : true));

		}

		return columns;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.sequoyah.localization.editor.editor.input.
	 * IStringEditorInput #translate (java.lang.String, TranslatedColumnInfo)
	 */
	@Override
	public IStatus translateColumn(String srcColumnID,
			TranslationInfo destColumnInfo, IProgressMonitor monitor) {

		IStatus result = Status.OK_STATUS;
		ILocalizationSchema schema = projectLocalizationManager
				.getProjectLocalizationSchema();

		LocaleInfo info = schema.getLocaleInfoFromID(srcColumnID);

		LocalizationFile existingFile = projectLocalizationManager
				.getLocalizationProject().getLocalizationFile(info);

		if (existingFile != null) {

			LocaleInfo infoTarget = schema.getLocaleInfoFromID(destColumnInfo
					.getId());
			String path = schema.getPathFromLocaleInfo(infoTarget);
			IFile file = ResourcesPlugin.getWorkspace().getRoot()
					.getFile(new Path(path));
			LocalizationFileBean bean = new LocalizationFileBean("", file, //$NON-NLS-1$
					infoTarget, new ArrayList<StringNode>(), null);
			LocalizationFile newFile = (StringLocalizationFile) projectLocalizationManager
					.getProjectLocalizationSchema()
					.createLocalizationFile(bean);
			newFile.setLocalizationProject(projectLocalizationManager
					.getLocalizationProject());

			result = translateColumn((StringLocalizationFile) existingFile,
					(StringLocalizationFile) newFile, destColumnInfo, monitor);

			if (result.isOK()) {
				projectLocalizationManager.getLocalizationProject()
						.addLocalizationFile((StringLocalizationFile) newFile);
				newFile.setDirty(true);
			}

		} else {
			monitor.setCanceled(true);
			result = new Status(
					IStatus.ERROR,
					LocalizationToolsPlugin.PLUGIN_ID,
					NLS.bind(
							Messages.StringEditorInput_ErrorTranslatingFromFile,
							srcColumnID));
			BasePlugin.logError("Error translating from file '" + srcColumnID //$NON-NLS-1$
					+ ". File does not exist."); //$NON-NLS-1$
		}

		return result;
	}

	/**
	 * @param srcColumnID
	 * @param newColumnsInfo
	 * @param monitor
	 * @return
	 */
	@Override
	public IStatus translateCells(String srcColumnID,
			TranslationInfo[] newColumnsInfo, IProgressMonitor monitor) {

		IStatus status = Status.OK_STATUS;

		try {

			monitor.beginTask(Messages.TranslationProgress_Connecting,
					newColumnsInfo.length);

			ITranslator translator = TranslatorManager.getInstance()
					.getTranslatorByName(newColumnsInfo[0].getTranslator());

			ArrayList<String> words = new ArrayList<String>();
			ArrayList<String> fromLanguages = new ArrayList<String>();
			ArrayList<String> toLanguages = new ArrayList<String>();

			for (TranslationInfo translationInfo : newColumnsInfo) {
				words.add(translationInfo.getFromWord());
				fromLanguages.add(translationInfo.getFromLang());
				toLanguages.add(translationInfo.getToLang());
				monitor.worked(1);
			}

			monitor.beginTask(Messages.TranslationProgress_FetchingInformation,
					100);
			List<TranslationResult> translationResults = translator
					.translateAll(words, fromLanguages, toLanguages, monitor);

			int i = 0;
			monitor.done();

			monitor.beginTask(Messages.ParsingAnswer, newColumnsInfo.length * 3);

			for (TranslationInfo translationInfo : newColumnsInfo) {
				monitor.worked(1);
				String translatedString = translationResults.get(i++)
						.getTranslatedWord();
				monitor.worked(1);
				translationInfo.setToWord(translatedString);
				monitor.worked(1);
			}
			monitor.done();
		} catch (Exception e) {
			status = new Status(IStatus.ERROR,
					LocalizationToolsPlugin.PLUGIN_ID,
					Messages.StringEditorInput_ErrorTranslatingCells, e);
			BasePlugin.logError(e.getMessage());
			monitor.setCanceled(true);
		}

		return status;
	}

	/**
	 * Call the translator for each cell, populating the new column
	 */
	private IStatus translateColumn(StringLocalizationFile source,
			StringLocalizationFile target, TranslationInfo destColumnInfo,
			IProgressMonitor monitor) {

		IStatus status = Status.OK_STATUS;

		if (source.getStringNodes().size() > 0) {
			status = translateStringNodes(source, target, destColumnInfo,
					monitor);
		}

		if (status.isOK()) {
			status = translateStringArrayNodes(source, target, destColumnInfo,
					monitor);
		}

		return status;
	}

	/**
	 * Translate string nodes
	 * 
	 * @param source
	 * @param target
	 * @param destColumnInfo
	 * @param monitor
	 * @return
	 */
	private IStatus translateStringNodes(StringLocalizationFile source,
			StringLocalizationFile target, TranslationInfo destColumnInfo,
			IProgressMonitor monitor) {
		List<StringNode> originalStringNodes = source.getStringNodes();
		ArrayList<String> stringValues = new ArrayList<String>();
		IStatus status = Status.OK_STATUS;

		monitor.beginTask(Messages.TranslationProgress_Connecting,
				originalStringNodes.size());

		for (Iterator<StringNode> iterator = originalStringNodes.iterator(); iterator
				.hasNext();) {
			StringNode originalNode = iterator.next();
			stringValues.add(originalNode.getValue());
			monitor.worked(1);
		}
		monitor.done();

		List<TranslationResult> translationResults = null;
		try {
			ITranslator translator = TranslatorManager.getInstance()
					.getTranslatorByName(destColumnInfo.getTranslator());
			monitor.beginTask(Messages.TranslationProgress_FetchingInformation,
					100);
			translationResults = translator.translateAll(stringValues,
					destColumnInfo.getFromLang(), destColumnInfo.getToLang(),
					monitor);
		} catch (Exception e) {
			status = new Status(IStatus.ERROR,
					LocalizationToolsPlugin.PLUGIN_ID, e.getLocalizedMessage(),
					e);
			BasePlugin.logError(e.getMessage());
			monitor.setCanceled(true);
		}

		if (status.isOK()) {
			monitor.done();

			int i = 0;
			monitor.beginTask(Messages.ParsingAnswer, translationResults.size());
			for (Iterator<TranslationResult> iterator = translationResults
					.iterator(); iterator.hasNext();) {
				monitor.worked(1);
				TranslationResult translationResult = iterator.next();
				String translatedString = translationResult.getTranslatedWord();

				StringNode newNode = null;
				newNode = new StringNode(originalStringNodes.get(i).getKey(),
						translatedString);

				i++;
				target.addStringNode(newNode);
				destColumnInfo.addCell(newNode.getKey(),
						new CellInfo(newNode.getValue(), "")); //$NON-NLS-1$
			}
			monitor.done();
		}

		return status;
	}

	/**
	 * Translate array items keeping its original position
	 * 
	 * @param source
	 * @param target
	 * @param destColumnInfo
	 * @param monitor
	 * @return
	 */
	private IStatus translateStringArrayNodes(StringLocalizationFile source,
			StringLocalizationFile target, TranslationInfo destColumnInfo,
			IProgressMonitor monitor) {

		IStatus status = Status.OK_STATUS;
		List<StringArrayNode> originalStringArrayNodes = source
				.getStringArrays();

		monitor.beginTask(Messages.ParsingAnswer,
				originalStringArrayNodes.size());

		List<StringArrayNode> newStringArrayNodesList = new ArrayList<StringArrayNode>();

		int j = 0;
		for (StringArrayNode currentArray : originalStringArrayNodes) {
			// TODO handle empty cells
			int size = currentArray.getStringValues().size();
			TranslationResult translationResult = null;

			StringArrayNode newStringArrayNode = null;
			CellInfo parentCell = null;

			ITranslator translator = TranslatorManager.getInstance()
					.getTranslatorByName(destColumnInfo.getTranslator());

			for (int k = 0; k < size && status.isOK(); k++) {
				try {
					String oldValue = currentArray.getArrayItemByIndex(k)
							.getValue();

					if ((oldValue != null) && !oldValue.equals("")) { //$NON-NLS-1$
						translationResult = translator.translate(oldValue,
								destColumnInfo.getFromLang(),
								destColumnInfo.getToLang());
					} else {
						translationResult = new TranslationResult("", //$NON-NLS-1$
								translator, "", destColumnInfo.getFromLang(), //$NON-NLS-1$
								destColumnInfo.getToLang(), new Date(), true);
					}
				} catch (Exception e) {
					status = new Status(IStatus.ERROR,
							LocalizationToolsPlugin.PLUGIN_ID,
							e.getLocalizedMessage(), e);
					BasePlugin.logError(e.getMessage());
					monitor.setCanceled(true);
				}
				if (status.isOK()) {
					String translatedString = translationResult
							.getTranslatedWord();

					if (k == 0) {
						newStringArrayNode = new StringArrayNode(
								originalStringArrayNodes.get(j).getKey());
						parentCell = new CellInfo(true);
						destColumnInfo.addCell(originalStringArrayNodes.get(j)
								.getKey(), parentCell);
					}

					newStringArrayNode.addValue(translatedString, k);
					CellInfo info = new CellInfo(translatedString, ""); //$NON-NLS-1$
					parentCell.addChild(info, k, false);
				}

			}
			if (newStringArrayNode != null) {
				newStringArrayNodesList.add(newStringArrayNode);
			}
			monitor.worked(1);
			j++;
		}
		if (status.isOK()) {
			target.setStringArrayNodes(newStringArrayNodesList);

			monitor.done();
		}
		return status;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.sequoyah.localization.editor.editor.input.
	 * IStringEditorInput #addColumn (java.lang.String)
	 */
	@Override
	public void addColumn(String ID) {

		ILocalizationSchema schema = projectLocalizationManager
				.getProjectLocalizationSchema();

		LocaleInfo info = schema.getLocaleInfoFromID(ID);

		LocalizationFile existingFile = projectLocalizationManager
				.getLocalizationProject().getLocalizationFile(info);

		if (existingFile == null) {
			String path = schema.getPathFromLocaleInfo(info);
			IFile file = ResourcesPlugin.getWorkspace().getRoot()
					.getFile(new Path(path));
			LocalizationFileBean bean = new LocalizationFileBean("", file, //$NON-NLS-1$
					info, new ArrayList<StringNode>(), null);
			LocalizationFile newFile = projectLocalizationManager
					.getProjectLocalizationSchema()
					.createLocalizationFile(bean);
			newFile.setLocalizationProject(projectLocalizationManager
					.getLocalizationProject());
			newFile.setDirty(true);
			projectLocalizationManager.getLocalizationProject()
					.addLocalizationFile((StringLocalizationFile) newFile);

		} else {
			existingFile.setToBeDeleted(false);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.sequoyah.localization.editor.editor.input.
	 * IStringEditorInput #removeColumn (java.lang.String)
	 */
	@Override
	public void removeColumn(String columnID) {
		LocaleInfo locale = projectLocalizationManager
				.getProjectLocalizationSchema().getLocaleInfoFromID(columnID);
		if (locale.getLocaleAttributes().isEmpty()) {// locale not found
			List<LocalizationFile> files = projectLocalizationManager
					.getLocalizationProject().getLocalizationFiles();
			for (LocalizationFile file : files) {
				if (file.getFile().toString().contains(columnID)) {
					projectLocalizationManager.markFileForDeletion(file);
					projectLocalizationManager.getLocalizationProject()
							.setDirty(true);
				}
			}
		} else {
			LocalizationFile file = projectLocalizationManager
					.getLocalizationProject().getLocalizationFile(locale);
			projectLocalizationManager.markFileForDeletion(file);
			projectLocalizationManager.getLocalizationProject().setDirty(true);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.sequoyah.localization.editor.editor.input.
	 * IStringEditorInput #setValue (java.lang.String, java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public void setValue(String columnID, String key, String value)
			throws SequoyahException {

		if (projectLocalizationManager == null) {
			Status status = new Status(Status.ERROR,
					LocalizationToolsPlugin.PLUGIN_ID,
					Messages.StringEditorInput_ErrorManagerNotInitialized);
			throw new SequoyahException(new SequoyahExceptionStatus(status));
		}

		LocaleInfo locale = projectLocalizationManager
				.getProjectLocalizationSchema().getLocaleInfoFromID(columnID);
		LocalizationFile file = projectLocalizationManager
				.getLocalizationProject().getLocalizationFile(locale);
		StringNode node = ((StringLocalizationFile) file)
				.getStringNodeByKey(key);
		if (node == null) {
			node = new StringNode(key, value);
			((StringLocalizationFile) file).addStringNode(node);
		}
		node.setValue(value);
	}

	@Override
	public void setValue(String columnID, String key, String value, int index)
			throws SequoyahException {

		if (projectLocalizationManager == null) {
			Status status = new Status(Status.ERROR,
					LocalizationToolsPlugin.PLUGIN_ID,
					Messages.StringEditorInput_ErrorManagerNotInitialized);
			throw new SequoyahException(new SequoyahExceptionStatus(status));
		}
		LocaleInfo locale = projectLocalizationManager
				.getProjectLocalizationSchema().getLocaleInfoFromID(columnID);
		LocalizationFile file = projectLocalizationManager
				.getLocalizationProject().getLocalizationFile(locale);
		StringNode node = ((StringLocalizationFile) file)
				.getStringNodeByKey(key);
		if (node instanceof StringArrayNode) {
			StringArrayNode arrayNode = (StringArrayNode) node;
			StringNode arrayItem = arrayNode.getArrayItemByIndex(index);
			if (arrayItem != null) {
				arrayItem.setLocalizationFile(file);
				arrayItem.setValue(value);
			} else {
				StringNode nodeItem = arrayNode.addValue(value, index);
				nodeItem.setLocalizationFile(file);
			}
		} else if (node == null) {
			StringArrayNode arrayNode = new StringArrayNode(key);
			arrayNode.setLocalizationFile(file);
			StringNode nodeItem = arrayNode.addValue(value, index);
			nodeItem.setLocalizationFile(file);
			((StringLocalizationFile) file).addStringNode(arrayNode);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.sequoyah.localization.editor.editor.input.
	 * IStringEditorInput #getValue (java.lang.String, java.lang.String)
	 */
	@Override
	public CellInfo getValue(String columnID, String key) {
		LocaleInfo localeInfo = projectLocalizationManager
				.getProjectLocalizationSchema().getLocaleInfoFromID(columnID);
		LocalizationFile localizationFile = projectLocalizationManager
				.getLocalizationProject().getLocalizationFile(localeInfo);
		StringNode stringNode = ((StringLocalizationFile) localizationFile)
				.getStringNodeByKey(key);

		if (stringNode == null) {
			return new CellInfo("", ""); //$NON-NLS-1$ //$NON-NLS-2$
			// return null
		}

		return new CellInfo(stringNode.getValue(),
				((stringNode.getNodeComment() != null) ? stringNode
						.getNodeComment().getComment() : null));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.sequoyah.localization.editor.editor.input.
	 * IStringEditorInput #getValues (java.lang.String)
	 */
	@Override
	public Map<String, CellInfo> getValues(String columnID) {

		LocaleInfo localeInfo = projectLocalizationManager
				.getProjectLocalizationSchema().getLocaleInfoFromID(columnID);
		Map<String, CellInfo> keyValueMap = new HashMap<String, CellInfo>();
		keyValueMap.keySet();
		LocalizationFile localizationFile = projectLocalizationManager
				.getLocalizationProject().getLocalizationFile(localeInfo);
		// get string nodes values
		List<StringNode> stringNodes = ((StringLocalizationFile) localizationFile)
				.getStringNodes();
		for (StringNode stringNode : stringNodes) {
			String comment = ""; //$NON-NLS-1$
			if (stringNode.getNodeComment() != null) {
				comment = stringNode.getNodeComment().getComment();
			}
			keyValueMap.put(stringNode.getKey(),
					new CellInfo(stringNode.getValue(), comment));
		}

		// get array nodes values
		List<StringArrayNode> arrayNodes = ((StringLocalizationFile) localizationFile)
				.getStringArrays();
		for (StringArrayNode arrayNode : arrayNodes) {
			CellInfo arrayCellInfo = new CellInfo(true);

			for (StringNode stringNode : arrayNode.getValues()) {
				String comment = ""; //$NON-NLS-1$
				if (stringNode.getNodeComment() != null) {
					comment = stringNode.getNodeComment().getComment();
				}
				arrayCellInfo.addChild(new CellInfo(stringNode.getValue(),
						comment));
			}
			keyValueMap.put(arrayNode.getKey(), arrayCellInfo);
		}

		return keyValueMap;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.sequoyah.stringeditor.editor.input.IStringEditorInput#
	 * getAvailableKeysForColumn(java.lang.String)
	 */
	@Override
	public List<CellInfo> getAvailableKeysForColumn(String columnID) {
		LocaleInfo localeInfo = projectLocalizationManager
				.getProjectLocalizationSchema().getLocaleInfoFromID(columnID);

		List<CellInfo> keysForColumn = new ArrayList<CellInfo>();
		LocalizationFile localizationFile = projectLocalizationManager
				.getLocalizationProject().getLocalizationFile(localeInfo);
		List<StringNode> stringNodes = ((StringLocalizationFile) localizationFile)
				.getStringNodes();

		for (StringNode stringNode : stringNodes) {
			keysForColumn.add(new CellInfo(stringNode.getKey(), stringNode
					.getNodeComment().getComment()));
		}

		return keysForColumn;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.sequoyah.localization.editor.editor.input.
	 * IStringEditorInput #canSave()
	 */
	@Override
	public boolean canSave() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.sequoyah.localization.editor.editor.input.
	 * IStringEditorInput #save()
	 */
	@Override
	public boolean save() {
		boolean result = projectLocalizationManager.saveProject();
		return result;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.sequoyah.localization.editor.editor.input.
	 * IStringEditorInput #isDirty()
	 */
	@Override
	public boolean isDirty() {
		boolean returnValue = false;
		if (projectLocalizationManager != null) {
			returnValue = projectLocalizationManager.getLocalizationProject()
					.isDirty();
		}
		return returnValue;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IEditorInput#exists()
	 */
	public boolean exists() {
		if (projectLocalizationManager.getAvailableLocales().size() > 0) {
			return true;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IEditorInput#getImageDescriptor()
	 */
	public ImageDescriptor getImageDescriptor() {
		return LocalizationToolsPlugin.imageDescriptorFromPlugin(
				LocalizationToolsPlugin.PLUGIN_ID, LOCALIZATION_ICON);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IEditorInput#getName()
	 */
	public String getName() {
		String name = ""; //$NON-NLS-1$
		// Human readable name for this editor (for instance,
		// usually it is the file being edited). For now I'll just
		// return the same text as the tooltip
		if (projectLocalizationManager != null) {
			name = projectLocalizationManager.getProjectLocalizationSchema()
					.getEditorName();
		}
		return name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IEditorInput#getPersistable()
	 */
	public IPersistableElement getPersistable() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	@SuppressWarnings("rawtypes")
	public Object getAdapter(Class adapter) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.sequoyah.stringeditor.editor.input.IStringEditorInput#
	 * canRevertByColumn()
	 */
	@Override
	public boolean canRevertByColumn() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.sequoyah.localization.editor.editor.input.
	 * IStringEditorInput #revert()
	 */
	@Override
	public boolean revert() {
		LocalizationManager.getInstance().unloadProjectLocalizationManager(
				projectLocalizationManager.getLocalizationProject()
						.getProject());
		try {
			LocalizationManager.getInstance().getProjectLocalizationManager(
					projectLocalizationManager.getLocalizationProject()
							.getProject(), false);
		} catch (IOException e) {

		} catch (SequoyahException e) {

		}
		return true;
	}

	public boolean isRevertable(String columnID) {
		boolean isRevertable = false;

		LocaleInfo localeInfo = projectLocalizationManager
				.getProjectLocalizationSchema().getLocaleInfoFromID(columnID);
		LocalizationFile file = projectLocalizationManager
				.getLocalizationProject().getLocalizationFile(localeInfo);
		if (file.getFile().exists()) {
			isRevertable = true;
		}

		return isRevertable;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.sequoyah.localization.editor.editor.input.
	 * IStringEditorInput #revert(java .lang.String)
	 */
	@Override
	public boolean revert(String columnID) throws IOException {

		boolean found = false;
		ILocalizationSchema schema = projectLocalizationManager
				.getProjectLocalizationSchema();
		LocaleInfo localeInfo = projectLocalizationManager
				.getProjectLocalizationSchema().getLocaleInfoFromID(columnID);

		if (localeInfo != null) {

			LocalizationFile file = projectLocalizationManager
					.getLocalizationProject().getLocalizationFile(localeInfo);

			String type = file.getClass().toString(); // type =
														// <Type>LocalizationFile.class
			type = type.substring(6, type.length());

			LocalizationFile newFile = null;
			try {
				newFile = schema.loadFile(type, file.getFile());
			} catch (SequoyahException e) {
				e.printStackTrace();
			}

			projectLocalizationManager.getLocalizationProject()
					.removeLocalizationFile(file);

			projectLocalizationManager.getLocalizationProject()
					.addLocalizationFile(newFile);

			found = true;
		}

		return found;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IEditorInput#getToolTipText()
	 */
	public String getToolTipText() {
		return projectLocalizationManager.getProjectLocalizationSchema()
				.getEditorName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.sequoyah.localization.editor.editor.input.
	 * IStringEditorInput #dispose()
	 */
	@Override
	public void dispose() {
		if (projectLocalizationManager != null) {
			LocalizationManager.getInstance().removeFileChangeListener(
					fileChangeListener);
			LocalizationManager.getInstance().unloadProjectLocalizationManager(
					projectLocalizationManager.getLocalizationProject()
							.getProject());
			projectLocalizationManager = null;
		}
	}

	public static void stopListening() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.sequoyah.localization.editor.editor.input.
	 * IStringEditorInput #removeCell (java.lang.String, java.lang.String)
	 */
	@Override
	public void removeCell(String key, String columnID) {
		ILocalizationSchema schema = projectLocalizationManager
				.getProjectLocalizationSchema();
		LocaleInfo locale = schema.getLocaleInfoFromID(columnID);
		LocalizationFile file = projectLocalizationManager
				.getLocalizationProject().getLocalizationFile(locale);

		// avoid total key deletion
		if ((schema.getDefaultID() != null)
				&& schema.getDefaultID().equals(columnID)) {
			try {
				setValue(columnID, key, ""); //$NON-NLS-1$
			} catch (SequoyahException e) {
				// do nothing
			}
		} else {
			((StringLocalizationFile) file)
					.removeStringNode(((StringLocalizationFile) file)
							.getStringNodeByKey(key));
		}

	}

	public void removeCell(String key, String columnID, int position) {
		ILocalizationSchema schema = projectLocalizationManager
				.getProjectLocalizationSchema();
		LocaleInfo locale = schema.getLocaleInfoFromID(columnID);
		LocalizationFile file = projectLocalizationManager
				.getLocalizationProject().getLocalizationFile(locale);

		// avoid total key deletion
		if ((schema.getDefaultID() != null)
				&& schema.getDefaultID().equals(columnID)) {
			// if is default column
			try {
				if (position >= 0) {
					setValue(columnID, key, "", position); //$NON-NLS-1$	
				} else {
					setValue(columnID, key, ""); //$NON-NLS-1$
				}
			} catch (SequoyahException e) {
				// do nothing
			}
		} else {
			// if non-default column
			StringLocalizationFile strFile = (StringLocalizationFile) file;
			StringArrayItemNode nodeFromNonDefaultColumn = getArrayItemNodeByPosition(
					key, position, strFile);

			if (nodeFromNonDefaultColumn != null) {
				// check if the item is on default locale
				LocaleInfo defaultLocale = schema.getLocaleInfoFromID(schema
						.getDefaultID());
				LocalizationFile defaultFile = projectLocalizationManager
						.getLocalizationProject().getLocalizationFile(
								defaultLocale);
				StringLocalizationFile strDefaultFile = (StringLocalizationFile) defaultFile;
				StringArrayItemNode nodeOnDefaultColumn = getArrayItemNodeByPosition(
						key, position, strDefaultFile);
				if (nodeOnDefaultColumn != null) {
					// node on default column exists => only set "" on item
					try {
						setValue(columnID, key, "", position); //$NON-NLS-1$
					} catch (SequoyahException e) {
						// do nothing
					}
				} else {
					// node on default column was also removed => remove node on
					// non-default column
					strFile.removeStringArrayItemNode(nodeFromNonDefaultColumn);
				}
			}
		}
	}

	private StringArrayItemNode getArrayItemNodeByPosition(String key,
			int position, StringLocalizationFile strFile) {
		StringArrayItemNode itemNode = null;

		StringNode node = strFile.getStringNodeByKey(key);
		if (node instanceof StringArrayNode) {
			StringArrayNode arrNode = (StringArrayNode) node;
			itemNode = arrNode.getArrayItemByIndex(position);
		}
		return itemNode;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.sequoyah.localization.editor.editor.input.
	 * IStringEditorInput #setCellTooltip (java.lang.String, java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public void setCellTooltip(String columnID, String key, String tooltip)
			throws SequoyahException {
		if (projectLocalizationManager == null) {
			Status status = new Status(Status.ERROR,
					LocalizationToolsPlugin.PLUGIN_ID,
					Messages.StringEditorInput_ErrorManagerNotInitialized);
			throw new SequoyahException(new SequoyahExceptionStatus(status));
		}
		LocaleInfo locale = projectLocalizationManager
				.getProjectLocalizationSchema().getLocaleInfoFromID(columnID);
		LocalizationFile file = projectLocalizationManager
				.getLocalizationProject().getLocalizationFile(locale);

		NodeComment comment = ((StringLocalizationFile) file)
				.getStringNodeByKey(key).getNodeComment();

		if (comment == null) {
			comment = new NodeComment();
		}
		((StringLocalizationFile) file).getStringNodeByKey(key).setNodeComment(
				comment);
		comment.setComment(tooltip);

	}

	@Override
	public void setCellTooltip(String columnID, String key, String tooltip,
			Integer index) throws SequoyahException {
		if (projectLocalizationManager == null) {
			Status status = new Status(Status.ERROR,
					LocalizationToolsPlugin.PLUGIN_ID,
					Messages.StringEditorInput_ErrorManagerNotInitialized);
			throw new SequoyahException(new SequoyahExceptionStatus(status));
		}
		LocaleInfo locale = projectLocalizationManager
				.getProjectLocalizationSchema().getLocaleInfoFromID(columnID);
		LocalizationFile file = projectLocalizationManager
				.getLocalizationProject().getLocalizationFile(locale);

		NodeComment comment = ((StringArrayNode) ((StringLocalizationFile) file)
				.getStringNodeByKey(key)).getArrayItemByIndex(index)
				.getNodeComment();

		if (comment == null) {
			comment = new NodeComment();
		}
		((StringArrayNode) ((StringLocalizationFile) file)
				.getStringNodeByKey(key)).getArrayItemByIndex(index)
				.setNodeComment(comment);
		comment.setComment(tooltip);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.sequoyah.localization.editor.editor.input.
	 * IStringEditorInput #validate()
	 */
	@Override
	public IStatus validate() {
		IStatus result = new Status(Status.OK,
				LocalizationToolsPlugin.PLUGIN_ID, ""); //$NON-NLS-1$
		;
		// The project is considered as in a warning state if there is no
		// default localization file
		String defaultID = projectLocalizationManager
				.getProjectLocalizationSchema().getDefaultID();
		if (defaultID != null) {
			LocaleInfo info = projectLocalizationManager
					.getProjectLocalizationSchema().getLocaleInfoFromID(
							defaultID);
			LocalizationFile localizationFile = projectLocalizationManager
					.getLocalizationProject().getLocalizationFile(info);

			if (localizationFile == null) {
				result = new Status(Status.WARNING,
						LocalizationToolsPlugin.PLUGIN_ID,
						Messages.Warning_NoDefaultFile);
			} else {
				if (localizationFile.isToBeDeleted()) {
					result = new Status(Status.WARNING,
							LocalizationToolsPlugin.PLUGIN_ID,
							Messages.Warning_NoDefaultFile);
				}
			}
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.sequoyah.localization.editor.editor.input.
	 * IStringEditorInput #canHandle (org.eclipse.core.resources.IFile)
	 */
	@Override
	public boolean canHandle(IFile file) {
		boolean canHandle = false;
		ILocalizationSchema localizationSchema = LocalizationManager
				.getInstance().getLocalizationSchema(file.getProject());
		if (localizationSchema != null) {
			canHandle = localizationSchema.isLocalizationFile(file);
		}
		return canHandle;
	}

	@Override
	public List<IFile> getFiles() {
		List<IFile> files = new ArrayList<IFile>();
		for (LocalizationFile locFile : projectLocalizationManager
				.getLocalizationProject().getLocalizationFiles()) {
			files.add(locFile.getFile());
		}
		return files;
	}

	@Override
	public String getSourcePageNameForFile(IFile file) {
		IPath filePath = file.getFullPath();
		return filePath.segment(filePath.segmentCount() - 2)
				+ "/" + filePath.lastSegment(); //$NON-NLS-1$
	}

	@Override
	public String getContentForFileAsText(IFileEditorInput editorInput) {
		LocalizationFile locFile = projectLocalizationManager
				.getLocalizationProject().getLocalizationFile(
						editorInput.getFile());
		return (String) projectLocalizationManager
				.getProjectLocalizationSchema().getLocalizationFileContent(
						locFile);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.sequoyah.localization.editor.model.input.
	 * AbstractStringEditorInput#renameKey(java.lang.String, java.lang.String)
	 */
	@Override
	public void renameKey(String oldName, String newName)
			throws SequoyahException {
		if (projectLocalizationManager == null) {
			Status status = new Status(Status.ERROR,
					LocalizationToolsPlugin.PLUGIN_ID,
					Messages.StringEditorInput_ErrorManagerNotInitialized);
			throw new SequoyahException(new SequoyahExceptionStatus(status));
		}

		// Retrieve all localization files for the project and iterate through
		// them, updating the key
		for (LocalizationFile f : projectLocalizationManager
				.getLocalizationProject().getLocalizationFiles()) {
			if (f instanceof StringLocalizationFile) {
				((StringLocalizationFile) f).renameNodeKey(oldName, newName);
			}
		}
	}

}
