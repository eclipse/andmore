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
package org.eclipse.andmore.android.generatemenucode.model.codegenerators;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.generatecode.AbstractCodeGeneratorData;
import org.eclipse.andmore.android.generatecode.BasicCodeVisitor;
import org.eclipse.andmore.android.generatemenucode.model.MenuFile;
import org.eclipse.andmore.android.generatemenucode.model.MenuItemNode;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;

/**
 * Model representing the code generator data needed to generate code for menu.
 * You MUST call init before using the object.
 */
public class CodeGeneratorDataBasedOnMenu extends AbstractCodeGeneratorData {
	private MenuFile menuFile;

	private BasicCodeVisitor codeVisitor;

	private ICompilationUnit iCompilationUnit;

	private CompilationUnit compilationUnit;

	/**
	 * Creates {@link MenuFile} representation for the menu.xml
	 * 
	 * @param menuName
	 *            name of the menu
	 * @param menu
	 *            file (full path) to menu.xml
	 * @throws AndroidException
	 *             if an error occurs parsing menu.xml
	 */
	public void init(String menuName, File menu) throws AndroidException {
		menuFile = new MenuFile(menuName, menu);
	}

	/**
	 * Get Menu Items that are not declared in the code yet
	 * 
	 * @return list of Menu items from menu file (only ones with id set).
	 */
	public List<MenuItemNode> getMenuItemsNodes() {
		return menuFile.getRootMenuNode() != null ? menuFile.getRootMenuNode().getAllMenuItems()
				: new ArrayList<MenuItemNode>(0);
	}

	/**
	 * @return the representation from {@link MenuFile}
	 */
	public MenuFile getMenuFile() {
		return menuFile;
	}

	@Override
	public IResource getResource() {
		return compilationUnit.getJavaElement().getResource();
	}

	@Override
	public ICompilationUnit getICompilationUnit() {
		return iCompilationUnit;
	}

	@Override
	public CompilationUnit getCompilationUnit() {
		return compilationUnit;
	}

	@Override
	public BasicCodeVisitor getAbstractCodeVisitor() {
		return codeVisitor;
	}

	/**
	 * Sets {@link BasicCodeVisitor} responsible to avoid code duplication or
	 * identify menu already inflated
	 * 
	 * @param visitor
	 */
	public void setAbstractCodeVisitor(BasicCodeVisitor visitor) {
		this.codeVisitor = visitor;
	}

	/**
	 * @param iCompilationUnit
	 *            the iCompilationUnit to set
	 */
	public void setICompilationUnit(ICompilationUnit iCompilationUnit) {
		this.iCompilationUnit = iCompilationUnit;
	}

	/**
	 * @param compilationUnit
	 *            the compilationUnit to set
	 */
	public void setCompilationUnit(CompilationUnit compilationUnit) {
		this.compilationUnit = compilationUnit;
	}
}