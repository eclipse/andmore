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

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
 * AST visitor base class for activity / fragment classes.
 */
public class BasicCodeVisitor extends ASTVisitor {
	protected TypeDeclaration typeDeclaration;

	/**
	 * @return the typeDeclaration
	 */
	public TypeDeclaration getTypeDeclaration() {
		return typeDeclaration;
	}

	/**
	 * @param typeDeclaration
	 *            the typeDeclaration to set
	 */
	public void setTypeDeclaration(TypeDeclaration typeDeclaration) {
		this.typeDeclaration = typeDeclaration;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.
	 * TypeDeclarationStatement)
	 */
	@Override
	public boolean visit(TypeDeclaration node) {
		boolean result = super.visit(node);
		if (node.isPackageMemberTypeDeclaration()) {
			// only keep if it is the top level class
			typeDeclaration = node;
		}
		return result;
	}
}
