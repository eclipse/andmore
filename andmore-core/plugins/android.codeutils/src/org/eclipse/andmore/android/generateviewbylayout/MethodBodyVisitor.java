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
package org.eclipse.andmore.android.generateviewbylayout;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.andmore.android.generatecode.JDTUtils;
import org.eclipse.andmore.android.generateviewbylayout.model.CodeGeneratorDataBasedOnLayout;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

/**
 * Visitor responsible to search method invocations inside a method body and set
 * the layout name associated with the activity / fragment
 */
public class MethodBodyVisitor extends ASTVisitor {
	/*
	 * Constants
	 */
	private static final String INFLATE = "inflate"; //$NON-NLS-1$

	private static final String VIEW = "View"; //$NON-NLS-1$

	private static final String ACTIVITY_METHOD_TO_SET_LAYOUT = "setContentView"; //$NON-NLS-1$

	private static final String FRAGMENT_METHOD_TO_SET_LAYOUT = INFLATE;

	private String layoutName;

	/**
	 * If type is fragment, there may be an inflated view name. This will be
	 * used to call findViewById inside fragments
	 */
	private String inflatedViewName;

	private CodeGeneratorDataBasedOnLayout.TYPE typeAssociatedToLayout;

	private Set<String> declaredViewIds = new HashSet<String>();

	public String getLayoutName() {
		return layoutName;
	}

	public void setLayoutName(String layoutName) {
		this.layoutName = layoutName;
	}

	/**
	 * Visits statements to find inflate method called on fragments to get the
	 * name of the view declared (this info will be used on findViewById, for
	 * fragments only)
	 */
	@Override
	public boolean visit(VariableDeclarationStatement node) {
		if (node.getType().toString().equals(VIEW)) {
			VariableDeclarationFragment frag = (VariableDeclarationFragment) node.fragments().get(0);
			if (frag.getInitializer() instanceof MethodInvocation) {
				MethodInvocation invoke = (MethodInvocation) frag.getInitializer();
				String invokeName = invoke.getName().toString();
				if (invoke.getExpression() instanceof SimpleName) {
					if ((invokeName != null) && invokeName.equals(INFLATE)) {
						inflatedViewName = frag.getName().getFullyQualifiedName();
					}
				}
			}
		}
		return super.visit(node);
	}

	/**
	 * Visit method invocations to find layout name set on activity/fragment
	 */
	@Override
	public boolean visit(MethodInvocation node) {
		// Fill invoked method model.
		MethodInvocation invoked = node;
		IMethodBinding methodBinding = invoked.resolveMethodBinding();
		if (methodBinding != null) {
			String methodSimpleName = methodBinding.getName();
			// Retrieve parameter types and look for R constants used within
			// method arguments
			List<?> arguments = invoked.arguments();
			for (Object argument : arguments) {
				Expression argumentExpression = (Expression) argument;
				if (argumentExpression instanceof QualifiedName) /*
																 * Can be a
																 * constant
																 * access
																 */
				{
					QualifiedName qualifiedName = (QualifiedName) argumentExpression;
					String layoutName = qualifiedName.getName().getIdentifier();
					if (methodSimpleName != null) {
						if (methodSimpleName.equals(ACTIVITY_METHOD_TO_SET_LAYOUT)) {
							typeAssociatedToLayout = CodeGeneratorDataBasedOnLayout.TYPE.ACTIVITY;
							setLayoutName(layoutName);
						} else if (methodSimpleName.equals(FRAGMENT_METHOD_TO_SET_LAYOUT)) {
							typeAssociatedToLayout = CodeGeneratorDataBasedOnLayout.TYPE.FRAGMENT;
							setLayoutName(layoutName);
							checkInflatedViewNameOnFields(node);
						} else if (methodSimpleName.equals(JavaViewBasedOnLayoutModifierConstants.FIND_VIEW_BY_ID)
								|| methodSimpleName.equals(JavaViewBasedOnLayoutModifierConstants.FIND_FRAGMENT_BY_ID)) {
							// findViewById
							String viewId = qualifiedName.getName().toString();
							getDeclaredViewIds().add(viewId);
						}

					}
				}
			}
		}
		return super.visit(node);
	}

	private void checkInflatedViewNameOnFields(MethodInvocation node) {
		// check if this method invocation is binded to an assignment
		ASTNode nodeParent = node.getParent();
		while ((nodeParent != null) && (inflatedViewName == null)) {
			if (nodeParent instanceof Assignment) {
				Assignment assignment = (Assignment) nodeParent;
				Expression lhs = assignment.getLeftHandSide();
				ITypeBinding binding = lhs.resolveTypeBinding();
				IJavaElement javaElement = binding.getJavaElement();
				if ((javaElement != null) && (lhs instanceof SimpleName)) {
					IType type = (IType) javaElement.getAdapter(IType.class);
					if (type != null) {
						try {
							if (JDTUtils.isSubclass(type, "android.view.View")) {
								inflatedViewName = ((SimpleName) lhs).getFullyQualifiedName();
							}
						} catch (JavaModelException e) {
							// do nothing
						}
					}

				}
			}
			nodeParent = nodeParent.getParent();
		}

	}

	/**
	 * @return the typeAssociatedToLayout
	 */
	public CodeGeneratorDataBasedOnLayout.TYPE getTypeAssociatedToLayout() {
		return typeAssociatedToLayout;
	}

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
	 * @return the declaredLayoutIds
	 */
	public synchronized Set<String> getDeclaredViewIds() {
		return declaredViewIds;
	}
}
