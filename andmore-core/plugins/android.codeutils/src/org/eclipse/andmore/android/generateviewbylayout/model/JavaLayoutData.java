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
package org.eclipse.andmore.android.generateviewbylayout.model;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.andmore.android.generatecode.JDTUtils;
import org.eclipse.andmore.android.generateviewbylayout.GenerateCodeBasedOnLayoutVisitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;

/**
 * Class encapsulating data from java code such as already declared IDs and the
 * compilation unit. It serves as a bridge to generate code inside
 * activity/fragment based on layout
 */
public class JavaLayoutData {
	private final Set<String> declaredViewIdsOnCode = new HashSet<String>();

	private final Set<String> savedViewIds = new HashSet<String>();

	private final Set<String> restoredViewIds = new HashSet<String>();

	private ICompilationUnit compUnit;

	private CompilationUnit compUnitAstNode;

	/**
	 * If type is fragment, there may be an inflated view name. This will be
	 * used to call findViewById inside fragments.
	 * 
	 * Null in these cases: 1) if fragment does not use it (nofify it is not
	 * possible to fill fragment by layout), or 2) if it is an activity.
	 */
	private String inflatedViewName = null;

	private GenerateCodeBasedOnLayoutVisitor visitor;

	/**
	 * @return the inflatedViewName
	 */
	public String getInflatedViewName() {
		return inflatedViewName;
	}

	/**
	 * @param inflatedViewName
	 *            the inflatedViewName to set
	 */
	public void setInflatedViewName(String inflatedViewName) {
		this.inflatedViewName = inflatedViewName;
	}

	/**
	 * @param declaredViewIdsOnCode
	 *            the declaredViewIdsOnCode to set
	 */
	public void setDeclaredViewIdsOnCode(Set<String> declaredViewIdsOnCode) {
		this.declaredViewIdsOnCode.clear();
		this.declaredViewIdsOnCode.addAll(declaredViewIdsOnCode);
	}

	/**
	 * @return the declaredViewIdsOnCode
	 */
	public Set<String> getDeclaredViewIdsOnCode() {
		return declaredViewIdsOnCode;
	}

	/**
	 * @return the visitor
	 */
	public GenerateCodeBasedOnLayoutVisitor getVisitor() {
		return visitor;
	}

	/**
	 * @param visitor
	 *            the visitor to set
	 */
	public void setVisitor(GenerateCodeBasedOnLayoutVisitor visitor) {
		this.visitor = visitor;
	}

	/**
	 * @return the compUnit
	 */
	public ICompilationUnit getCompUnit() {
		return compUnit;
	}

	/**
	 * Item required to write AST
	 * 
	 * @param compUnit
	 *            the compUnit to set
	 */
	public void setCompUnit(ICompilationUnit compUnit) {
		this.compUnit = compUnit;
	}

	/**
	 * @return the compUnitAstNode
	 */
	public CompilationUnit getCompUnitAstNode() {
		return compUnitAstNode;
	}

	/**
	 * @param compUnitAstNode
	 *            the compUnitAstNode to set
	 */
	public void setCompUnitAstNode(CompilationUnit compUnitAstNode) {
		this.compUnitAstNode = compUnitAstNode;
	}

	/**
	 * 
	 * @return true if AST have at least one error (warnings are not
	 *         considered), false otherwise
	 */
	public boolean hasErrorInCompilationUnitAst() {
		CompilationUnit cpUnit = getCompUnitAstNode();
		return JDTUtils.hasErrorInCompilationUnitAstUtils(cpUnit);
	}

	public void setSavedViewIds(Set<String> savedViewIds) {
		this.savedViewIds.clear();
		this.savedViewIds.addAll(savedViewIds);

	}

	public void setRestoredViewIds(Set<String> restoredViewIds) {
		this.restoredViewIds.clear();
		this.restoredViewIds.addAll(restoredViewIds);
	}

	public Set<String> getSavedViewIds() {
		return savedViewIds;
	}

	public Set<String> getRestoredViewIds() {
		return restoredViewIds;
	}
}
