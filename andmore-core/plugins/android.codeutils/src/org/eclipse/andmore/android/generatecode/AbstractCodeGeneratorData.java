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

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;

/**
 * Model representing the code generator data needed to generate code (for
 * layout, for menu, or other new future purpose).
 */
public abstract class AbstractCodeGeneratorData {

	public enum TYPE {
		ACTIVITY, FRAGMENT;
	}

	private TYPE associatedType;

	public TYPE getAssociatedType() {
		return associatedType;
	}

	public void setAssociatedType(TYPE associatedType) {
		this.associatedType = associatedType;
	}

	/**
	 * @return the java file that will be modified
	 */
	public abstract IResource getResource();

	/**
	 * Necessary to apply the text modification
	 * 
	 * @return {@link ICompilationUnit} to be modified
	 */
	public abstract ICompilationUnit getICompilationUnit();

	/**
	 * Necessary to record AST changes
	 * 
	 * @return {@link CompilationUnit} to be modified
	 */
	public abstract CompilationUnit getCompilationUnit();

	/**
	 * Visitor to get data from the java file (e.g.: inflate or setContentView)
	 * return {@link BasicCodeVisitor} or a subclass that can collect data about
	 * the java file being visited
	 */
	public abstract BasicCodeVisitor getAbstractCodeVisitor();
}
