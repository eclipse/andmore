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

import java.util.List;

import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.generateviewbylayout.JavaViewBasedOnLayoutModifierConstants;
import org.eclipse.andmore.android.generateviewbylayout.model.CodeGeneratorDataBasedOnLayout;
import org.eclipse.andmore.android.generateviewbylayout.model.LayoutNode;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

/**
 * Responsible to add methods to deal with RadioButtons events
 */
public class RadioButtonCodeGenerator extends AbstractLayoutCodeGenerator {
	/**
	 * @param codeGeneratorData
	 * @param onCreateDeclaration
	 * @param typeDeclaration
	 */
	public RadioButtonCodeGenerator(CodeGeneratorDataBasedOnLayout codeGeneratorData,
			MethodDeclaration onCreateDeclaration, TypeDeclaration typeDeclaration) {
		super(codeGeneratorData, onCreateDeclaration, typeDeclaration);
	}

	@Override
	public void generateCode(IProgressMonitor monitor) throws JavaModelException {
		addSetOnClickListener(monitor);
		addMethodToDeclareRadioButtonHandlerNotDeclaredOnLayoutXML(monitor);
	}

	/**
	 * Add method handlers for RadioButtons
	 * 
	 * <br>
	 * GENERATED_CODE_FORMAT: <br>
	 * this.$GUI_ID.setOnClickListener(onClickHandler);
	 */
	@SuppressWarnings("unchecked")
	private void addSetOnClickListener(IProgressMonitor monitor) throws JavaModelException {
		SubMonitor subMonitor = SubMonitor.convert(monitor);
		subMonitor.beginTask(CodeUtilsNLS.JavaViewBasedOnLayoutModifier_AddingSetOnClickListener, codeGeneratorData
				.getGuiItems().size());
		/*
		 * ExpressionStatement expression MethodInvocation arguments handler
		 * methodName SimpleName(setOnClickListener) optionalExpression
		 * SimpleName(button1)
		 */
		for (LayoutNode node : codeGeneratorData.getGuiItems()) {
			if (node.shouldInsertCode() && node.getNodeType().equals(LayoutNode.LayoutNodeViewType.RadioButton.name())) {
				boolean containMethodDeclared = checkIfInvokeMethodIsDeclared(node,
						JavaViewBasedOnLayoutModifierConstants.SET_ON_CLICK_LISTENER);
				if (!containMethodDeclared) {
					SimpleName method = onCreateDeclaration.getAST().newSimpleName(
							JavaViewBasedOnLayoutModifierConstants.SET_ON_CLICK_LISTENER);
					SimpleName bId = onCreateDeclaration.getAST().newSimpleName(node.getNodeId());
					FieldAccess fieldAccess = onCreateDeclaration.getAST().newFieldAccess();
					fieldAccess.setExpression(onCreateDeclaration.getAST().newThisExpression());
					fieldAccess.setName(bId);

					SimpleName handler = onCreateDeclaration.getAST().newSimpleName(
							JavaViewBasedOnLayoutModifierConstants.HANDLER_ONCLICK_LISTENER);

					MethodInvocation mI = onCreateDeclaration.getAST().newMethodInvocation();
					mI.arguments().add(handler);
					mI.setName(method);
					mI.setExpression(fieldAccess);

					ExpressionStatement expr = onCreateDeclaration.getAST().newExpressionStatement(mI);
					if (!onCreateDeclaration.toString().contains(expr.toString())) {
						// avoid to duplicate statement
						insertStatementsAtOnCreateDeclaration(expr, false);
					}
				}
			}
			subMonitor.worked(1);
		}
	}

	/**
	 * Add method to declare button events handler (not declared on layout XML)
	 * 
	 * @throws JavaModelException
	 * 
	 * <br>
	 *             GENERATED_CODE_FORMAT: <br>
	 *             private View.OnClickListener onClickHandler = new
	 *             View.OnClickListener() { <br>
	 *             public void onClick(View target) { <br>
	 *             if (target.getId() == $GUI_ID1) { <br>
	 *             } else if (target.getId() == $GUI_ID2) { <br>
	 *             } <br>
	 *             } <br>
	 *             };
	 * 
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void addMethodToDeclareRadioButtonHandlerNotDeclaredOnLayoutXML(IProgressMonitor monitor)
			throws JavaModelException {
		SubMonitor subMonitor = SubMonitor.convert(monitor);
		// need to look at each GUI item and them create 1 method
		subMonitor.beginTask(CodeUtilsNLS.JavaViewBasedOnLayoutModifier_AddingMethodToHandleButton, codeGeneratorData
				.getGuiItems().size() + 1);
		IfStatement ifStatement = null;
		for (LayoutNode node : codeGeneratorData.getGuiItems()) {
			if (node.getNodeType().equals(LayoutNode.LayoutNodeViewType.RadioButton.name()) && node.shouldInsertCode()) {

				if (typeDeclaration.bodyDeclarations() != null) {
					// check if field already declared
					outer: for (Object bd : typeDeclaration.bodyDeclarations()) {
						if (bd instanceof FieldDeclaration) {
							FieldDeclaration fd = (FieldDeclaration) bd;
							if ((fd.fragments() != null)) {
								String listenerType = getListenerSimpleType(
										JavaViewBasedOnLayoutModifierConstants.VIEW_CLASS,
										JavaViewBasedOnLayoutModifierConstants.METHOD_ON_CLICK_LISTENER).toString();
								for (Object fragment : fd.fragments()) {
									if (fragment instanceof VariableDeclarationFragment) {
										VariableDeclarationFragment variableFragment = (VariableDeclarationFragment) fragment;
										if ((variableFragment.getName() != null)
												&& variableFragment
														.getName()
														.toString()
														.equals(JavaViewBasedOnLayoutModifierConstants.HANDLER_ONCLICK_LISTENER)
												&& fd.getType().toString().equals(listenerType)) {
											Expression expr = variableFragment.getInitializer();
											if (expr instanceof ClassInstanceCreation) {
												ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation) expr;
												AnonymousClassDeclaration anonymousClassDeclaration = classInstanceCreation
														.getAnonymousClassDeclaration();
												for (Object bodyDeclaration : anonymousClassDeclaration
														.bodyDeclarations()) {
													if (bodyDeclaration instanceof MethodDeclaration) {
														MethodDeclaration methodDeclaration = (MethodDeclaration) bodyDeclaration;
														List statementsList = methodDeclaration.getBody().statements();
														for (Object statement : statementsList) {
															if (statement instanceof IfStatement) {
																if (statement
																		.toString()
																		.contains(
																				JavaViewBasedOnLayoutModifierConstants.VIEW_VARIABLE_NAME
																						+ "." //$NON-NLS-1$
																						+ JavaViewBasedOnLayoutModifierConstants.METHOD_NAME_GET_ID)) {
																	// found if
																	// statement
																	ifStatement = (IfStatement) statement;
																	break outer;
																}
															}
														}
														if (ifStatement == null) {
															ifStatement = typeDeclaration.getAST().newIfStatement();
															statementsList.add(ifStatement);
														}
													}
												}
											}
										}
									}
								}

							}
						}
					}
				}
				if (ifStatement == null) {
					// create field with onClickListener class instance creation
					// create method in the first time, after that, start to add
					// new items in the same method
					ifStatement = createOnClickListenerForRadioButtons(node);
				} else {
					// already have onClickListener field declared => append new
					// item into the "else if" chain
					addElseIfForEachRadioButtonId(ifStatement, node);
				}
			}
			subMonitor.worked(1);
		}

	}

	/**
	 * Creates field with an anonymous class declaration for radio buttons
	 */
	@SuppressWarnings("unchecked")
	private IfStatement createOnClickListenerForRadioButtons(LayoutNode node) {
		IfStatement ifSt;
		Modifier privateMod = typeDeclaration.getAST().newModifier(ModifierKeyword.PRIVATE_KEYWORD);
		SimpleType listenerType = getListenerSimpleType(JavaViewBasedOnLayoutModifierConstants.VIEW_CLASS,
				JavaViewBasedOnLayoutModifierConstants.METHOD_ON_CLICK_LISTENER);

		VariableDeclarationFragment variableFragment = typeDeclaration.getAST().newVariableDeclarationFragment();
		SimpleName varName = variableFragment.getAST().newSimpleName(
				JavaViewBasedOnLayoutModifierConstants.HANDLER_ONCLICK_LISTENER);
		variableFragment.setName(varName);
		FieldDeclaration declaration = typeDeclaration.getAST().newFieldDeclaration(variableFragment);
		declaration.modifiers().add(privateMod);
		declaration.setType(listenerType);

		ClassInstanceCreation classInstanceCreation = typeDeclaration.getAST().newClassInstanceCreation();

		SimpleType listenerType2 = getListenerSimpleType(JavaViewBasedOnLayoutModifierConstants.VIEW_CLASS,
				JavaViewBasedOnLayoutModifierConstants.METHOD_ON_CLICK_LISTENER);

		classInstanceCreation.setType(listenerType2);
		AnonymousClassDeclaration classDeclaration = typeDeclaration.getAST().newAnonymousClassDeclaration();
		MethodDeclaration methodDeclaration = addMethodDeclaration(ModifierKeyword.PUBLIC_KEYWORD,
				JavaViewBasedOnLayoutModifierConstants.METHOD_NAME_ON_CLICK, PrimitiveType.VOID,
				JavaViewBasedOnLayoutModifierConstants.VIEW_CLASS,
				JavaViewBasedOnLayoutModifierConstants.VIEW_VARIABLE_NAME);
		Block block = typeDeclaration.getAST().newBlock();
		ifSt = createElseIfForEachRadioButtonId(node);
		block.statements().add(ifSt);
		methodDeclaration.setBody(block);
		classDeclaration.bodyDeclarations().add(methodDeclaration);
		classInstanceCreation.setAnonymousClassDeclaration(classDeclaration);
		variableFragment.setInitializer(classInstanceCreation);
		typeDeclaration.bodyDeclarations().add(declaration);
		return ifSt;
	}

	/**
	 * @return "else if" chain for each radio button id
	 */
	private IfStatement createElseIfForEachRadioButtonId(LayoutNode node) {
		IfStatement ifStatement = typeDeclaration.getAST().newIfStatement();
		addElseIfForEachRadioButtonId(ifStatement, node);
		return ifStatement;
	}

	/**
	 * Creates else if and else statements for each radio button
	 * 
	 * @param ifSt
	 *            If statement where the next "else if" will be appended
	 * @param node
	 *            Layout node
	 */
	private void addElseIfForEachRadioButtonId(IfStatement ifSt, LayoutNode node) {
		if (node.getNodeType().equals(LayoutNode.LayoutNodeViewType.RadioButton.name()) && node.shouldInsertCode()) {
			MethodInvocation invocation = typeDeclaration.getAST().newMethodInvocation();
			invocation.setExpression(getVariableName(JavaViewBasedOnLayoutModifierConstants.VIEW_VARIABLE_NAME));
			SimpleName getIdName = typeDeclaration.getAST().newSimpleName(
					JavaViewBasedOnLayoutModifierConstants.METHOD_NAME_GET_ID);
			invocation.setName(getIdName);

			SimpleName r = typeDeclaration.getAST().newSimpleName(JavaViewBasedOnLayoutModifierConstants.R);
			SimpleName id = typeDeclaration.getAST().newSimpleName(JavaViewBasedOnLayoutModifierConstants.ID);
			QualifiedName rid = typeDeclaration.getAST().newQualifiedName(r, id);
			SimpleName guiId = typeDeclaration.getAST().newSimpleName(node.getNodeId());
			QualifiedName guiQN = typeDeclaration.getAST().newQualifiedName(rid, guiId);
			createElseIfAndElseStatements(ifSt, invocation, guiQN);
		}
	}
}