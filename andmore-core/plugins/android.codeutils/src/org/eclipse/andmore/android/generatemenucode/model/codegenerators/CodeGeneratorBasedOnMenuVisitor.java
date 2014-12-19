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

import java.util.List;

import org.eclipse.andmore.android.generatecode.BasicCodeVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;

/**
 * Visitor to collect information on activity/fragment class (about menu
 * creation methods or variables). It serves to avoid code duplication and fill
 * the menu declared for this activity/fragment.
 */
public class CodeGeneratorBasedOnMenuVisitor extends BasicCodeVisitor {
	/*
	 * Constants
	 */
	private static final String ACTIVITY_ON_CREATE_MENU_DECLARATION = "public boolean onCreateOptionsMenu(android.view.Menu)"; //$NON-NLS-1$

	private static final String FRAGMENT_ON_CREATE_MENU_DECLARATION = "public void onCreateOptionsMenu(android.view.Menu, android.view.MenuInflater)"; //$NON-NLS-1$

	private static final String ACTIVITY_ON_CREATE_MENU = "onCreateOptionsMenu"; //$NON-NLS-1$

	private static final String FRAGMENT_ON_CREATE_MENU = "onCreateOptionsMenu"; //$NON-NLS-1$

	private static final String INFLATE_METHOD = "inflate";

	/**
	 * If type is fragment, there may be an inflated menu name.
	 */
	private String inflatedMenuName;

	/**
	 * Visit method declaration, searching for instructions onCreate for
	 * activity or fragment
	 */
	@Override
	public boolean visit(MethodDeclaration node) {
		// Fill Method information
		SimpleName name = node.getName();
		if (name.getIdentifier().equals(ACTIVITY_ON_CREATE_MENU)
				|| name.getIdentifier().equals(FRAGMENT_ON_CREATE_MENU)) {
			IMethodBinding binding = node.resolveBinding();
			if (binding != null) {
				if (binding.toString().trim().contains(ACTIVITY_ON_CREATE_MENU_DECLARATION)
						|| binding.toString().trim().contains(FRAGMENT_ON_CREATE_MENU_DECLARATION)) {
					visitMethodBodyToIdentifyMenu(node);
				}
			}
		}

		return super.visit(node);
	}

	/**
	 * Visit method body from onCreateOptionsMenu declaration to the inflated
	 * menu
	 * 
	 * @param node
	 */
	protected void visitMethodBodyToIdentifyMenu(MethodDeclaration node) {
		// Navigate through statements...

		Block body = node.getBody();

		if (body != null) {

			List<?> statements = body.statements();
			if (statements != null) {
				for (Object statement : statements) {

					if ((statement != null) && (statement instanceof ExpressionStatement)) {
						Expression argumentExpression = ((ExpressionStatement) statement).getExpression();
						if ((argumentExpression != null) && (argumentExpression instanceof MethodInvocation)) {
							String methodSimpleName = ((MethodInvocation) argumentExpression).getName().toString();
							if ((methodSimpleName != null) && (methodSimpleName.equals(INFLATE_METHOD))) {
								if ((((MethodInvocation) argumentExpression).arguments() != null)
										&& (((MethodInvocation) argumentExpression).arguments().size() > 0)) {
									String menuBeingInflated = ((MethodInvocation) argumentExpression).arguments()
											.get(0).toString();
									if ((menuBeingInflated != null) && (menuBeingInflated.indexOf('.') > 0)) {
										setInflatedMenuName(menuBeingInflated.substring(
												menuBeingInflated.lastIndexOf('.') + 1, menuBeingInflated.length()));
									}
									break;
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * @return the inflatedMenuName
	 */
	public String getInflatedMenuName() {
		return inflatedMenuName;
	}

	/**
	 * @param inflatedMenuName
	 *            the inflatedMenuName to set
	 */
	public void setInflatedMenuName(String inflatedMenuName) {
		this.inflatedMenuName = inflatedMenuName;
	}

}
