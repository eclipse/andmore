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
package org.eclipse.andmore.android.generateviewbylayout.codegenerators;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.generatecode.AbstractCodeGenerator;
import org.eclipse.andmore.android.generateviewbylayout.JavaViewBasedOnLayoutModifierConstants;
import org.eclipse.andmore.android.generateviewbylayout.model.CodeGeneratorDataBasedOnLayout;
import org.eclipse.andmore.android.generateviewbylayout.model.LayoutNode;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

/**
 * Class that have common methods to generate code based on layout (for GUI
 * handlers, findViewById, attributes)
 */
public abstract class AbstractLayoutCodeGenerator extends AbstractCodeGenerator {
	protected CodeGeneratorDataBasedOnLayout codeGeneratorData;

	protected MethodDeclaration onCreateDeclaration;

	/**
	 * @param codeGeneratorData
	 *            input where to get the information (generally a xml, for
	 *            example, it could be menu or layout)
	 * @param onCreateDeclaration
	 *            method onCreate (for Activity) or onCreateView (for Fragment)
	 * @param typeDeclaration
	 *            AST type representing Activity or Fragment Android class
	 */
	public AbstractLayoutCodeGenerator(CodeGeneratorDataBasedOnLayout codeGeneratorData,
			MethodDeclaration onCreateDeclaration, TypeDeclaration typeDeclaration) {
		super(typeDeclaration);
		this.codeGeneratorData = codeGeneratorData;
		this.onCreateDeclaration = onCreateDeclaration;
	}

	/**
	 * @return the codeGeneratorData
	 */
	protected CodeGeneratorDataBasedOnLayout getCodeGeneratorData() {
		return codeGeneratorData;
	}

	/**
	 * Adds method invocation that instantiates an anonymous class to deal with
	 * the event
	 */
	protected void addMethodInvocationToListenerHandler(String callerId, String invocationMethod, String classType,
			String listenerClazzName, MethodDeclaration methodDeclaration) throws JavaModelException {
		List<MethodDeclaration> declarations = new ArrayList<MethodDeclaration>();
		declarations.add(methodDeclaration);
		addMethodInvocationToListenerHandler(callerId, invocationMethod, classType, listenerClazzName, declarations);
	}

	/**
	 * Adds method invocation that instantiates an anonymous class to deal with
	 * the event
	 */
	@SuppressWarnings("unchecked")
	protected void addMethodInvocationToListenerHandler(String callerId, String invocationMethod, String classType,
			String listenerClazzName, List<MethodDeclaration> methodDeclarations) throws JavaModelException {
		MethodInvocation methodInvocation = onCreateDeclaration.getAST().newMethodInvocation();
		SimpleName listenerInvocationName = onCreateDeclaration.getAST().newSimpleName(invocationMethod);
		SimpleName listenerOptionalName = onCreateDeclaration.getAST().newSimpleName(callerId);
		FieldAccess fieldAccess = onCreateDeclaration.getAST().newFieldAccess();
		fieldAccess.setExpression(onCreateDeclaration.getAST().newThisExpression());
		fieldAccess.setName(listenerOptionalName);

		methodInvocation.setName(listenerInvocationName);
		methodInvocation.setExpression(fieldAccess);

		ClassInstanceCreation classInstanceCreation = onCreateDeclaration.getAST().newClassInstanceCreation();
		SimpleType listenerType = getListenerSimpleType(classType, listenerClazzName);
		classInstanceCreation.setType(listenerType);

		AnonymousClassDeclaration classDeclaration = onCreateDeclaration.getAST().newAnonymousClassDeclaration();

		for (MethodDeclaration methodDeclaration : methodDeclarations) {
			classDeclaration.bodyDeclarations().add(methodDeclaration);
		}
		classInstanceCreation.setAnonymousClassDeclaration(classDeclaration);

		methodInvocation.arguments().add(classInstanceCreation);

		ExpressionStatement expressionStatement = onCreateDeclaration.getAST().newExpressionStatement(methodInvocation);
		insertStatementsAtOnCreateDeclaration(expressionStatement, false);
	}

	/**
	 * Insert statements on create declaration depending if it is activity /
	 * fragment
	 * 
	 * @param expr
	 * @param insertInTheBeginningOfMethod
	 *            true, if must insert after setContentView (activity) or after
	 *            inflate (fragment); false if must insert in the of the method
	 *            (activity), last statement before return (fragment)
	 * @throws JavaModelException
	 *             if fragment can not be modified because it is not in the
	 *             format appropriate
	 */
	@SuppressWarnings("unchecked")
	protected void insertStatementsAtOnCreateDeclaration(Statement expr, boolean insertInTheBeginningOfMethod)
			throws JavaModelException {
		int size = onCreateDeclaration.getBody().statements().size();
		if (getCodeGeneratorData().getAssociatedType().equals(CodeGeneratorDataBasedOnLayout.TYPE.ACTIVITY)) {
			if (insertInTheBeginningOfMethod) {
				int foundIndex = findSetContentViewIndexInsideStatement();
				// if activity => add after second statement (after
				// setContentView)
				if (foundIndex >= 0) {
					// it should have super.onCreate and setContentView => add
					// after them
					onCreateDeclaration.getBody().statements().add(foundIndex + 1, expr);
				}
			} else {
				// last statement
				onCreateDeclaration.getBody().statements().add(size, expr);
			}
		} else if (getCodeGeneratorData().getAssociatedType().equals(CodeGeneratorDataBasedOnLayout.TYPE.FRAGMENT)) {
			if (size <= 1) {
				throw new JavaModelException(new IllegalArgumentException(
						CodeUtilsNLS.MethodVisitor_InvalidFormatForFragmentOnCreateView), IStatus.ERROR);
			}
			if (insertInTheBeginningOfMethod) {
				if (size >= 2) {
					int foundIndex = findInflateIndexAtStatement();
					if (foundIndex >= 0) {
						// if fragment => add after first statement (after
						// inflater.inflate)
						// it should have inflater.inflate and return statement
						// => add between them
						onCreateDeclaration.getBody().statements().add(foundIndex + 1, expr);
					}
				}
			} else {
				// last statement before return
				onCreateDeclaration.getBody().statements().add(size - 1, expr);
			}
		}
	}

	/**
	 * Returns the last statement of inflate inside onCreate from Fragment
	 * 
	 * @return -1 if inflate not found, value >=0 if statement found
	 */
	private int findInflateIndexAtStatement() {
		int foundIndex = -1;
		int index = 0;
		while (index < onCreateDeclaration.getBody().statements().size()) {
			Object st = onCreateDeclaration.getBody().statements().get(index);
			Expression expression = null;
			if (st instanceof VariableDeclarationStatement) {
				VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement) st;
				for (Object f : variableDeclarationStatement.fragments()) {
					VariableDeclarationFragment frag = (VariableDeclarationFragment) f;
					expression = frag.getInitializer();
				}
			} else if (st instanceof ExpressionStatement) {
				ExpressionStatement expressionStatement = (ExpressionStatement) st;
				if (expressionStatement.getExpression() instanceof Assignment) {
					Assignment assignment = (Assignment) expressionStatement.getExpression();
					expression = assignment.getRightHandSide();
				}
			}
			if (expression != null) {
				int aux = findInflateIndexAtStatement(index, expression);
				if (aux >= 0) {
					foundIndex = aux;
				}
			}
			index++;
		}
		return foundIndex;
	}

	/**
	 * Returns the last statement of setContentView inside onCreate from
	 * Activity
	 * 
	 * @return -1 if setContentView not found, value >=0 if statement found
	 */
	private int findSetContentViewIndexInsideStatement() {
		int foundIndex = -1;
		int index = 0;
		while (index < onCreateDeclaration.getBody().statements().size()) {
			Object st = onCreateDeclaration.getBody().statements().get(index);
			if (st instanceof ExpressionStatement) {
				ExpressionStatement expressionStatement = (ExpressionStatement) st;
				if (expressionStatement.getExpression() instanceof MethodInvocation) {
					MethodInvocation setContentInvocation = (MethodInvocation) expressionStatement.getExpression();
					if ((setContentInvocation.getName() != null)
							&& setContentInvocation.getName().getIdentifier().equals("setContentView")) {
						foundIndex = index;
					}
				}
			}
			index++;
		}
		return foundIndex;
	}

	/**
	 * Checks if the method is already invoked in the body of onCreate method
	 * 
	 * @param node
	 * @return
	 */
	public boolean checkIfInvokeMethodIsDeclared(LayoutNode node, String method) {
		boolean containMethodDeclared = false;
		if (onCreateDeclaration.getBody() != null) {
			// check if method invocation already declared
			for (Object bodystat : onCreateDeclaration.getBody().statements()) {
				if (bodystat instanceof ExpressionStatement) {
					ExpressionStatement exprStatement = (ExpressionStatement) bodystat;
					if (exprStatement.getExpression() instanceof MethodInvocation) {
						MethodInvocation invoke = (MethodInvocation) exprStatement.getExpression();
						if ((invoke.getName() != null) && invoke.getName().toString().equals(method)) {
							if ((invoke.getExpression() != null)
									&& invoke.getExpression().toString().equals(THIZ + node.getNodeId())) {
								containMethodDeclared = true;
								break;
							}
						}
					}
				}
			}
		}
		return containMethodDeclared;
	}

	/**
	 * @param node
	 * @return true if there is one findViewById with the give node.getNodeId(),
	 *         false otherwise
	 */
	public boolean checkIfFindViewByIdAlreadyDeclared(LayoutNode node) {
		return codeGeneratorData.getJavaLayoutData().getDeclaredViewIdsOnCode().contains(node.getNodeId());
	}

	/**
	 * Creates an assigment statement. The format follows the example:
	 * 
	 * <br>
	 * <br>
	 * <code>Button b = (Button) v.findViewById($nodeId);</code>
	 * 
	 * 
	 * @param node
	 * @param optionalExpression
	 *            if invocation was nested (e.g.: getFragmentManager())
	 * @param methodToBeCalled
	 * @throws JavaModelException
	 */
	@SuppressWarnings("unchecked")
	public void addAssignmentStatement(LayoutNode node, Expression optionalExpression, String methodToBeCalled)
			throws JavaModelException {
		SimpleName guiName;
		try {
			guiName = getNodeVariableTypeBasedOnLayoutNode(node);
		} catch (CoreException e) {
			throw new JavaModelException(e);
		}
		SimpleType guiType = onCreateDeclaration.getAST().newSimpleType(guiName);

		SimpleName method = onCreateDeclaration.getAST().newSimpleName(methodToBeCalled);
		SimpleName rId1 = onCreateDeclaration.getAST().newSimpleName(JavaViewBasedOnLayoutModifierConstants.R);
		SimpleName rId2 = onCreateDeclaration.getAST().newSimpleName(JavaViewBasedOnLayoutModifierConstants.ID);
		QualifiedName rQualified1 = onCreateDeclaration.getAST().newQualifiedName(rId1, rId2);

		SimpleName guiId = onCreateDeclaration.getAST().newSimpleName(node.getNodeId());
		QualifiedName rQualified2 = onCreateDeclaration.getAST().newQualifiedName(rQualified1, guiId);

		MethodInvocation invocation = onCreateDeclaration.getAST().newMethodInvocation();
		invocation.setName(method);
		if (optionalExpression != null) {
			invocation.setExpression(optionalExpression);
		}
		if (getCodeGeneratorData().getAssociatedType().equals(CodeGeneratorDataBasedOnLayout.TYPE.FRAGMENT)) {
			if (!node.isFragmentPlaceholder()) {
				invocation.setExpression(onCreateDeclaration.getAST().newSimpleName(
						getCodeGeneratorData().getJavaLayoutData().getInflatedViewName()));
			}
		}
		invocation.arguments().add(rQualified2);

		CastExpression castExpr = onCreateDeclaration.getAST().newCastExpression();
		castExpr.setExpression(invocation);
		castExpr.setType(guiType);

		Assignment assign = onCreateDeclaration.getAST().newAssignment();
		SimpleName variableId = onCreateDeclaration.getAST().newSimpleName(node.getNodeId());

		FieldAccess fieldAccess = onCreateDeclaration.getAST().newFieldAccess();
		fieldAccess.setExpression(onCreateDeclaration.getAST().newThisExpression());
		fieldAccess.setName(variableId);

		assign.setLeftHandSide(fieldAccess);
		assign.setOperator(Assignment.Operator.ASSIGN);
		assign.setRightHandSide(castExpr);

		ExpressionStatement expr = onCreateDeclaration.getAST().newExpressionStatement(assign);
		insertStatementsAtOnCreateDeclaration(expr, true);
	}

	/**
	 * Gets the name of the variable based on the type declared in the layout
	 * xml
	 * 
	 * @param node
	 * @return
	 * @throws CoreException
	 */
	public SimpleName getNodeVariableTypeBasedOnLayoutNode(LayoutNode node) throws CoreException {
		SimpleName guiName = null;
		String clazzName = node.getClazzName();
		if (node.isFragmentPlaceholder() && (clazzName != null)) {
			// use type defined in the xml
			IStatus nameStatus = JavaConventions.validateIdentifier(clazzName, "5", "5");
			if (nameStatus.isOK()) {
				guiName = onCreateDeclaration.getAST().newSimpleName(clazzName);
			} else {
				throw new CoreException(nameStatus);
			}
		} else {
			guiName = onCreateDeclaration.getAST().newSimpleName(node.getNodeType());
		}
		return guiName;
	}
}
