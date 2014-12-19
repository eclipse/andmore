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
package org.eclipse.andmore.android.generatecode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.generatemenucode.model.codegenerators.CodeGeneratorDataBasedOnMenu;
import org.eclipse.andmore.android.generateviewbylayout.model.CodeGeneratorDataBasedOnLayout;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractTextEditor;

/**
 * Manager responsible to modify activity / fragment.
 */
public abstract class JavaCodeModifier {
	protected List<AbstractCodeGenerator> codeGenerators = new ArrayList<AbstractCodeGenerator>();

	protected AbstractCodeGeneratorData codeGeneratorData;

	public static final List<String> IMPORT_LIST = new ArrayList<String>();

	protected TypeDeclaration typeDeclaration;

	/**
	 * Insert code into the class (activity / fragment) and adds imports if
	 * necessary.
	 * 
	 * @throws JavaModelException
	 *             Thrown if there were problems parsing the java file.
	 */
	public void insertCode(IProgressMonitor monitor, IEditorPart editor) throws JavaModelException {
		final SubMonitor theMonitor = SubMonitor.convert(monitor);
		IResource resource = codeGeneratorData.getResource();
		if (resource instanceof IFile) {
			IFile java = (IFile) resource;
			StudioLogger
					.info("Trying to insert code for class: " + java.getFullPath() + " based  on resource " + getDataResource()); //$NON-NLS-1$
			IDocument document = null;
			try {
				document = ((AbstractTextEditor) editor).getDocumentProvider().getDocument(editor.getEditorInput());
				final ICompilationUnit compUnit = getCodeGeneratorData().getICompilationUnit();
				CompilationUnit cpU = getCodeGeneratorData().getCompilationUnit();

				try {
					cpU.recordModifications();

					initVariables();

					codeGenerators.clear();
					codeGenerators = populateListOfCodeGenerators(getCodeGeneratorData());

					theMonitor.beginTask(CodeUtilsNLS.JavaViewBasedOnLayoutModifier_InsertingCode,
							1000 * getNumberOfTasks());

					callCodeGenerators(theMonitor, java);

					addImportsIfRequired(theMonitor, cpU);
					Map<?, ?> mapOptions = JavaCore.create(java.getProject()).getOptions(true);
					final TextEdit edit = cpU.rewrite(document, mapOptions);
					PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

						@Override
						public void run() {
							try {
								compUnit.applyTextEdit(edit, theMonitor);
							} catch (JavaModelException e) {
								StudioLogger.error(this.getClass(), "Error applying changes: " + e.getMessage(), e); //$NON-NLS-1$
							}

						}
					});
				} catch (CoreException e) {
					StudioLogger.error(this.getClass(), "Error changing AST activity/fragment: " + e.getMessage()); //$NON-NLS-1$
					throw new JavaModelException(e);
				} catch (RuntimeException rte) {
					StudioLogger.error(this.getClass(), "Error changing AST activity/fragment: " + rte.getMessage()); //$NON-NLS-1$
					throw new JavaModelException(rte, IJavaModelStatusConstants.CORE_EXCEPTION);
				}
			} catch (CoreException e) {
				StudioLogger.error(this.getClass(),
						"Error creating IDocument from java file: " + java + " message: " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
				throw new JavaModelException(e, IJavaModelStatusConstants.CORE_EXCEPTION);
			} finally {
				theMonitor.done();
			}
		}
	}

	/**
	 * Init variables required by the code modifier (default behaviour init only
	 * typeDeclaration variable, override if necessary to add other variables)
	 */
	protected void initVariables() {
		typeDeclaration = getCodeGeneratorData().getAbstractCodeVisitor().getTypeDeclaration();
	}

	/**
	 * @return file representing the path to data resource (e.g.: layout or
	 *         menu)
	 */
	protected abstract File getDataResource();

	/**
	 * Calls code generators (override it if you have a special condition flag
	 * to generate code). <br>
	 * It iterates over {@link JavaCodeModifier#codeGenerators} list and calls
	 * {@link AbstractCodeGenerator#generateCode(IProgressMonitor)}
	 * 
	 * @param theMonitor
	 * @param java
	 *            file being modified
	 * @throws JavaModelException
	 */
	protected void callCodeGenerators(final SubMonitor theMonitor, IFile java) throws JavaModelException {
		for (AbstractCodeGenerator codeGenerator : codeGenerators) {
			codeGenerator.generateCode(theMonitor);
		}
	}

	/**
	 * Adds imports if they were not added yet
	 * 
	 * @param theMonitor
	 * @param compUnit
	 * @throws JavaModelException
	 */
	public void addImportsIfRequired(SubMonitor theMonitor, CompilationUnit compUnit) throws JavaModelException {

		for (String importString : IMPORT_LIST) {
			String importName = "";
			boolean onDemand = false;
			if (importString.endsWith(".*")) {
				importName = importString.substring(0, importString.length() - 2);
				onDemand = true;
			} else {
				importName = importString;
			}
			boolean exists = false;
			for (Object importDecl : compUnit.imports()) {
				ImportDeclaration declaration = (ImportDeclaration) importDecl;
				String name = declaration.getName().getFullyQualifiedName();
				if (importName.equals(name)) {
					exists = true;
					break;
				}
			}
			if (!exists) {
				createImport(importName, compUnit, onDemand);
			}
		}

	}

	@SuppressWarnings("unchecked")
	private void createImport(String name, CompilationUnit compUnit, boolean onDemand) {
		AST ast = compUnit.getAST();
		ImportDeclaration importDeclaration = ast.newImportDeclaration();
		importDeclaration.setName(ast.newName(name));
		importDeclaration.setOnDemand(onDemand);
		compUnit.imports().add(importDeclaration);
	}

	/**
	 * Creates the necessary imports listed in
	 * {@link JavaCodeModifier#IMPORT_LIST}.
	 * 
	 * @throws JavaModelException
	 *             Thrown if there were problems during the insertion of imports
	 *             in the java file.
	 */
	protected void createImports(IProgressMonitor monitor, ICompilationUnit compilationUnit) throws JavaModelException {
		SubMonitor subMonitor = SubMonitor.convert(monitor);
		// need to look at each GUI item and them create 1 method
		subMonitor.beginTask(CodeUtilsNLS.JavaViewBasedOnLayoutModifier_CreatingImports, IMPORT_LIST.size());
		if (IMPORT_LIST != null) {
			for (String importItem : IMPORT_LIST) {
				compilationUnit.createImport(importItem, null, subMonitor);
			}
		}
		subMonitor.worked(IMPORT_LIST.size());
	}

	/**
	 * Sets the code generator input data (for example:
	 * {@link CodeGeneratorDataBasedOnLayout} or
	 * {@link CodeGeneratorDataBasedOnMenu}) to be used for the java code
	 * modifier
	 * 
	 * @param codeGeneratorData
	 *            the codeGeneratorData to set.
	 */
	public void setCodeGeneratorData(AbstractCodeGeneratorData codeGeneratorData) {
		this.codeGeneratorData = codeGeneratorData;
	}

	/**
	 * @return the codeGeneratorData
	 */
	public AbstractCodeGeneratorData getCodeGeneratorData() {
		return codeGeneratorData;
	}

	/**
	 * Populates the list of code generators that the modifier will use to
	 * change the code.
	 * 
	 * @param codeGeneratorDataBasedOnLayout
	 *            the data source to use into the modification
	 * @return list of code generators.
	 */
	public abstract List<AbstractCodeGenerator> populateListOfCodeGenerators(
			AbstractCodeGeneratorData abstractCodeGeneratorData);

	/**
	 * @return The number of tasks based on the number of code generators.
	 */
	protected int getNumberOfTasks() {
		return codeGenerators.size();
	}

}
