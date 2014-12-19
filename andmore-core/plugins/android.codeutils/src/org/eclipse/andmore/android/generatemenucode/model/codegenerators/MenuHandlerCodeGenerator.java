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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.generatemenucode.model.MenuItemNode;
import org.eclipse.andmore.android.generateviewbylayout.JavaViewBasedOnLayoutModifierConstants;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IJavaModelStatus;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.osgi.util.NLS;

/**
 * Responsible to create menu handlers (Android code) for activities / fragments
 */
public class MenuHandlerCodeGenerator extends AbstractMenuCodeGenerator {
	/*
	 * Constants (method bindings to avoid repetitive code)
	 */
	private static final String ON_CREATE_OPTIONS_MENU_MENU_METHODBINDING = "public boolean onCreateOptionsMenu(android.view.Menu)"; //$NON-NLS-1$

	private static final String ON_CREATE_OPTIONS_MENU_MENU_METHODBINDING_FRAG = "public void onCreateOptionsMenu(android.view.Menu, android.view.MenuInflater)"; //$NON-NLS-1$

	private static final String ON_OPTIONS_ITEM_SELECTED_MENU_ITEM_METHODBINDING = "public boolean onOptionsItemSelected(android.view.MenuItem)"; //$NON-NLS-1$

	private static final String ON_CREATE_METHODBINDING = "public void onCreate(android.os.Bundle)"; //$NON-NLS-1$

	/**
	 * @param codeGeneratorData
	 * @param typeDeclaration
	 */
	public MenuHandlerCodeGenerator(CodeGeneratorDataBasedOnMenu codeGeneratorData, TypeDeclaration typeDeclaration) {
		super(codeGeneratorData, typeDeclaration);
	}

	@Override
	public void generateCode(IProgressMonitor monitor) throws JavaModelException {
		if (getCodeGeneratorData().getAssociatedType().equals(CodeGeneratorDataBasedOnMenu.TYPE.FRAGMENT)) {
			// for fragments, it is required to change onCreate to add
			// setHasOptionMenu invocation
			createOnCreateAndSetHasOptionMenu(monitor);
		}
		insertMethodToInflateMenu(monitor);
		addMethodToHandleMenu(monitor);
	}

	/**
	 * Calls method that enables menu contribution for fragments <br>
	 * GENERATED_CODE_FORMAT: <br>
	 * <br>
	 * <code>
	 *      public void onCreate (Bundle savedInstanceState) {     
	 * <br>
	 *      setHasOptionMenu(true);
	 * <br>
	 *      super.onCreate(savedInstanceState);
	 * <br>
	 *  }
	 *  </code>
	 */
	@SuppressWarnings("unchecked")
	private void createOnCreateAndSetHasOptionMenu(IProgressMonitor monitor) {
		MethodDeclaration onCreateMethodDeclaration = createOnCreateMethod(monitor);
		MethodDeclaration foundMethod = isMethodAlreadyDeclared(onCreateMethodDeclaration, ON_CREATE_METHODBINDING);
		if (foundMethod != null) {
			// method onCreateOptionsMenu is already created => use the found
			// method instead of the new created one
			onCreateMethodDeclaration = foundMethod;
		}
		MethodInvocation setHasOptionMenuInvoke = createMethodInvocation(null,
				CodeGeneratorBasedOnMenuConstants.SET_HAS_OPTIONS_MENU);
		BooleanLiteral defaultValue = typeDeclaration.getAST().newBooleanLiteral(true);
		setHasOptionMenuInvoke.arguments().add(defaultValue);
		ExpressionStatement statement = typeDeclaration.getAST().newExpressionStatement(setHasOptionMenuInvoke);
		addStatementIfNotFound(onCreateMethodDeclaration, statement, false);

		List<String> arguments = new ArrayList<String>();
		arguments.add(CodeGeneratorBasedOnMenuConstants.SAVED_INSTANCE_STATE);
		// super.onCreate(savedInstanceState);
		insertSuperInvocation(onCreateMethodDeclaration, CodeGeneratorBasedOnMenuConstants.ON_CREATE, arguments);
		if (foundMethod == null) {
			// method onCreateOptionsMenu was not yet declared
			typeDeclaration.bodyDeclarations().add(onCreateMethodDeclaration);
		}
	}

	private MethodDeclaration createOnCreateMethod(IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor);
		subMonitor.beginTask(CodeUtilsNLS.MenuHandlerCodeGenerator_AddingOnCreateAndSetHasOptionMenu, 1);

		List<SingleVariableDeclaration> parameters = new ArrayList<SingleVariableDeclaration>();
		SingleVariableDeclaration param1 = createVariableDeclarationFromStrings(
				CodeGeneratorBasedOnMenuConstants.BUNDLE, CodeGeneratorBasedOnMenuConstants.SAVED_INSTANCE_STATE);
		parameters.add(param1);
		// public void onCreate (Bundle savedInstanceState)
		MethodDeclaration methodDeclaration = addMethodDeclaration(ModifierKeyword.PUBLIC_KEYWORD,
				CodeGeneratorBasedOnMenuConstants.ON_CREATE, PrimitiveType.VOID, parameters);
		Block block = typeDeclaration.getAST().newBlock();
		methodDeclaration.setBody(block);

		subMonitor.worked(1);
		return methodDeclaration;
	}

	/**
	 * Adds method to inflate menu <br>
	 * GENERATED_CODE_FORMAT (for activity): <br>
	 * <br>
	 * <code>
	 *      public boolean onCreateOptionsMenu(Menu menu) {
	 * <br>
	 *      MenuInflater inflater = getMenuInflater();
	 *  <br>
	 *      inflater.inflate(R.menu.<menu_id>, menu);
	 *  <br>
	 *      return true;
	 *  <br>
	 *  }
	 *  
	 *  <br>
	 *  GENERATED_CODE_FORMAT (for fragment):
	 * <br>
	 * <br>
	 * <code>
	 *      public boolean onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
	 *  <br>
	 *      inflater.inflate(R.menu.<menu_id>, menu);
	 *  <br>
	 *      return true;
	 *  <br>
	 *  }
	 *  </code>
	 * 
	 * @param monitor
	 *            to indicate progress when adding method declaration
	 */
	@SuppressWarnings("unchecked")
	private void insertMethodToInflateMenu(IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor);
		// need to look at each GUI item and them create 1 method
		subMonitor.beginTask(CodeUtilsNLS.MenuHandlerCodeGenerator_AddingOnCreateOptionsMenu, 1);

		MethodDeclaration methodDeclaration = null;
		MethodDeclaration foundMethod = null;
		if (getCodeGeneratorData().getAssociatedType().equals(CodeGeneratorDataBasedOnMenu.TYPE.ACTIVITY)) {
			// declare method
			methodDeclaration = declareOnCreateOptionsMenuMethod();
			foundMethod = isMethodAlreadyDeclared(methodDeclaration, ON_CREATE_OPTIONS_MENU_MENU_METHODBINDING);
			if (foundMethod != null) {
				// method onCreateOptionsMenu is already created => use the
				// found method instead of the new created one
				methodDeclaration = foundMethod;
			}
			// declare inflater variable
			declareInflaterVariable(methodDeclaration);
			// call inflate method
			callsInflateMethod(methodDeclaration);
			// add return statement
			createReturnStatement(methodDeclaration);
		} else if (getCodeGeneratorData().getAssociatedType().equals(CodeGeneratorDataBasedOnMenu.TYPE.FRAGMENT)) {
			// declare method
			methodDeclaration = declareOnCreateOptionsMenuMethodFragment();
			foundMethod = isMethodAlreadyDeclared(methodDeclaration, ON_CREATE_OPTIONS_MENU_MENU_METHODBINDING_FRAG);
			if (foundMethod != null) {
				// method onCreateOptionsMenu is already created => use the
				// found method instead of the new created one
				methodDeclaration = foundMethod;
			}
			// call inflate method
			callsInflateMethod(methodDeclaration);
			// add return statement
			createReturnStatementFragment(methodDeclaration);
		}

		if (foundMethod == null) {
			// method onCreateOptionsMenu was not yet declared
			typeDeclaration.bodyDeclarations().add(methodDeclaration);
		}
		subMonitor.worked(1);
	}

	/**
	 * @param methodDeclaration
	 */
	protected void createReturnStatementFragment(MethodDeclaration methodDeclaration) {

		List<String> arguments = new ArrayList<String>();
		arguments.add(CodeGeneratorBasedOnMenuConstants.MENU_VARIABLE);
		arguments.add(CodeGeneratorBasedOnMenuConstants.INFLATER_VARIABLE);
		insertSuperInvocation(methodDeclaration, CodeGeneratorBasedOnMenuConstants.ON_CREATE_OPTIONS_MENU, arguments);
	}

	/**
	 * Generates the following code:
	 * <code>public boolean onCreateOptionsMenu(Menu menu){}</code>
	 * 
	 * @return
	 */
	protected MethodDeclaration declareOnCreateOptionsMenuMethod() {
		List<SingleVariableDeclaration> parameters = new ArrayList<SingleVariableDeclaration>();
		SingleVariableDeclaration param1 = createVariableDeclarationFromStrings(
				CodeGeneratorBasedOnMenuConstants.MENU_TYPE, CodeGeneratorBasedOnMenuConstants.MENU_VARIABLE);
		parameters.add(param1);
		MethodDeclaration methodDeclaration = addMethodDeclaration(ModifierKeyword.PUBLIC_KEYWORD,
				CodeGeneratorBasedOnMenuConstants.ON_CREATE_OPTIONS_MENU, PrimitiveType.BOOLEAN, parameters);
		Block block = typeDeclaration.getAST().newBlock();
		methodDeclaration.setBody(block);
		return methodDeclaration;
	}

	/**
	 * Generates the following code:
	 * <code>public void onCreateOptionsMenu(Menu menu, Inflater inflate){}</code>
	 * 
	 * @return
	 */
	protected MethodDeclaration declareOnCreateOptionsMenuMethodFragment() {
		List<SingleVariableDeclaration> parameters = new ArrayList<SingleVariableDeclaration>();
		SingleVariableDeclaration param1 = createVariableDeclarationFromStrings(
				CodeGeneratorBasedOnMenuConstants.MENU_TYPE, CodeGeneratorBasedOnMenuConstants.MENU_VARIABLE);
		parameters.add(param1);
		SingleVariableDeclaration param2 = createVariableDeclarationFromStrings(
				CodeGeneratorBasedOnMenuConstants.MENU_INFLATER_VARIABLE,
				CodeGeneratorBasedOnMenuConstants.INFLATER_VARIABLE);
		parameters.add(param2);
		// public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		MethodDeclaration methodDeclaration = addMethodDeclaration(ModifierKeyword.PUBLIC_KEYWORD,
				CodeGeneratorBasedOnMenuConstants.ON_CREATE_OPTIONS_MENU, PrimitiveType.VOID, parameters);
		Block block = typeDeclaration.getAST().newBlock();
		methodDeclaration.setBody(block);
		return methodDeclaration;
	}

	/**
	 * Generates the following code:
	 * <code>inflater.inflate(R.menu.<menu_id>, menu);</code>
	 * 
	 * @param methodDeclaration
	 */
	@SuppressWarnings("unchecked")
	protected void callsInflateMethod(MethodDeclaration methodDeclaration) {
		MethodInvocation inflateInvoke = createMethodInvocation(CodeGeneratorBasedOnMenuConstants.INFLATER_VARIABLE,
				CodeGeneratorBasedOnMenuConstants.INFLATE_METHOD);
		SimpleName r = typeDeclaration.getAST().newSimpleName(CodeGeneratorBasedOnMenuConstants.R);
		SimpleName menu = typeDeclaration.getAST().newSimpleName(CodeGeneratorBasedOnMenuConstants.MENU_VARIABLE);
		SimpleName menuIdName = typeDeclaration.getAST().newSimpleName(
				getCodeGeneratorData().getMenuFile().getNameWithoutExtension());
		QualifiedName rMenu = typeDeclaration.getAST().newQualifiedName(r, menu);
		QualifiedName menuId = typeDeclaration.getAST().newQualifiedName(rMenu, menuIdName);
		inflateInvoke.arguments().add(menuId);
		SimpleName menuArg = typeDeclaration.getAST().newSimpleName(CodeGeneratorBasedOnMenuConstants.MENU_VARIABLE);
		inflateInvoke.arguments().add(menuArg);
		ExpressionStatement inflateExprStatement = typeDeclaration.getAST().newExpressionStatement(inflateInvoke);

		addStatementIfNotFound(methodDeclaration, inflateExprStatement, false);
	}

	/**
	 * Generates the following code:
	 * <code>MenuInflater inflater = getMenuInflater();</code>
	 * 
	 * @param methodDeclaration
	 */
	protected void declareInflaterVariable(MethodDeclaration methodDeclaration) {
		MethodInvocation getMenuInflaterInvoke = createMethodInvocation(null,
				CodeGeneratorBasedOnMenuConstants.GET_MENU_INFLATER);
		VariableDeclarationFragment declarationFragment = typeDeclaration.getAST().newVariableDeclarationFragment();
		SimpleName inflater = typeDeclaration.getAST().newSimpleName(
				CodeGeneratorBasedOnMenuConstants.INFLATER_VARIABLE);
		declarationFragment.setName(inflater);
		declarationFragment.setInitializer(getMenuInflaterInvoke);
		VariableDeclarationStatement declarationStatement = typeDeclaration.getAST().newVariableDeclarationStatement(
				declarationFragment);
		SimpleName menuInflaterName = typeDeclaration.getAST().newSimpleName(
				CodeGeneratorBasedOnMenuConstants.MENU_INFLATER_VARIABLE);
		SimpleType menuInflaterType = typeDeclaration.getAST().newSimpleType(menuInflaterName);
		declarationStatement.setType(menuInflaterType);

		addStatementIfNotFound(methodDeclaration, declarationStatement, false);
	}

	/**
	 * Adds method to declare menu events handler
	 * 
	 * @param monitor
	 * @throws JavaModelException
	 * 
	 * <br>
	 *             GENERATED_CODE_FORMAT: <br>
	 * <br>
	 *             <code>
	 *   public boolean onOptionsItemSelected(MenuItem item) {
	 * <br>  
	 *       if (item.getItemId() == $MENUITEM_ID1) {
	 * <br>
	 *       } else if (item.getItemId() == $MENUITEM_ID2) {
	 * <br>      
	 *       } else {
	 * <br>
	 *          return super.onOptionsItemSelected(item);        
	 * <br>
	 *       }
	 * <br>
	 *       return true;       
	 * <br>
	 *   }
	 * <br>
	 * </code>
	 */
	private void addMethodToHandleMenu(IProgressMonitor monitor) throws JavaModelException {
		SubMonitor subMonitor = SubMonitor.convert(monitor);
		// need to look at each GUI item and them create 1 method
		subMonitor.beginTask(CodeUtilsNLS.MenuHandlerCodeGenerator_AddingOnOptionsItemSelected, codeGeneratorData
				.getMenuItemsNodes().size() + 1);
		IfStatement ifStatement = null;
		for (MenuItemNode node : codeGeneratorData.getMenuItemsNodes()) {
			if ((node.getOnClickMethod() == null) || node.getOnClickMethod().equals("")) //$NON-NLS-1$
			{
				// does not have on click declared
				if (ifStatement == null) {
					// create method in the first time, after that, start to add
					// new items in the same method
					ifStatement = createOnOptionsItemSelectedForMenuItems(node);
				} else {
					// already have onOptionsItemSelected method declared =>
					// append new item into the "else if" chain
					addElseIfForEachMenuItemId(ifStatement, node);
				}
			} else {
				// has on click declared
				createMenuItemHandlerForOnClick(node);
			}
			subMonitor.worked(1);
		}

	}

	/**
	 * Creates method to handle menu item if android:onClick is defined on
	 * menu.xml <code>public void $myMethodName(MenuItem item)</code>
	 * 
	 * @param node
	 */
	@SuppressWarnings("unchecked")
	private void createMenuItemHandlerForOnClick(MenuItemNode node) throws JavaModelException {
		if (node.getOnClickMethod() != null) {
			int i = 0;
			// check if the onClick is valid
			String invalidChar = null;
			boolean validMethodName = true;
			for (char ch : node.getOnClickMethod().toCharArray()) {
				if ((i <= 0) && !Character.isJavaIdentifierStart(ch)) {
					invalidChar = "" + ch; //$NON-NLS-1$
					validMethodName = false;
					break;
				} else if ((i > 0) && !Character.isJavaIdentifierPart(ch)) {
					// i>0
					invalidChar = "" + ch; //$NON-NLS-1$
					validMethodName = false;
					break;
				}
				i++;
			}
			if (!validMethodName) {
				Object[] bindings = new Object[] { node.getOnClickMethod(),
						getCodeGeneratorData().getMenuFile().getFile().getName(), invalidChar };
				String msg = NLS.bind(
						CodeUtilsNLS.MenuHandlerCodeGenerator_InvalidJavaCharacterInAndroidOnClickAttribute, bindings);
				throw new JavaModelException(new IllegalArgumentException(msg), IStatus.ERROR);
			}

			MethodDeclaration methodDeclaration = addMethodDeclaration(ModifierKeyword.PUBLIC_KEYWORD,
					node.getOnClickMethod(), PrimitiveType.VOID, CodeGeneratorBasedOnMenuConstants.MENU_ITEM,
					CodeGeneratorBasedOnMenuConstants.ITEM);
			String methodBinding = "public void " + node.getOnClickMethod() + "(android.view.MenuItem)"; //$NON-NLS-1$ //$NON-NLS-2$
			MethodDeclaration foundMethod = isMethodAlreadyDeclared(methodDeclaration, methodBinding);
			if (foundMethod == null) {
				// method public void $myMethodName(MenuItem item) was not yet
				// declared
				Block block = typeDeclaration.getAST().newBlock();
				methodDeclaration.setBody(block);
				typeDeclaration.bodyDeclarations().add(methodDeclaration);
			}
		}
	}

	/**
	 * Creates method with handle for menu items (if android:onClick) is not
	 * defined on menu.xml
	 * <code>public boolean onOptionsItemSelected(MenuItem item)</code>
	 */
	@SuppressWarnings("unchecked")
	private IfStatement createOnOptionsItemSelectedForMenuItems(MenuItemNode node) {
		IfStatement ifSt;
		// declare method
		MethodDeclaration methodDeclaration = addMethodDeclaration(ModifierKeyword.PUBLIC_KEYWORD,
				CodeGeneratorBasedOnMenuConstants.ON_OPTIONS_ITEM_SELECTED, PrimitiveType.BOOLEAN,
				CodeGeneratorBasedOnMenuConstants.MENU_ITEM, CodeGeneratorBasedOnMenuConstants.ITEM);
		MethodDeclaration foundMethod = isMethodAlreadyDeclared(methodDeclaration,
				ON_OPTIONS_ITEM_SELECTED_MENU_ITEM_METHODBINDING);
		Block block = null;
		if (foundMethod != null) {
			// method onOptionsItemSelected is already created => use the found
			// method instead of the new created one
			methodDeclaration = foundMethod;
			block = methodDeclaration.getBody();
		} else {
			// method onOptionsItemSelected not found => create block to insert
			// statements
			block = typeDeclaration.getAST().newBlock();
			methodDeclaration.setBody(block);
		}
		// create if and else-if's
		ifSt = createElseIfForEachMenuItemId(node);
		IfStatement foundIfStatement = (IfStatement) findIfStatementAlreadyDeclared(ifSt, true, block.statements());
		if (foundIfStatement != null) {
			ifSt = foundIfStatement;
		} else {
			// if not existent yet then:
			// 1-add else
			addingElseExpression(ifSt, methodDeclaration);
			// 2-add if statement only if there is not another one
			addStatementIfNotFound(block, ifSt, true);
		}
		createReturnStatement(methodDeclaration);
		if (foundMethod == null) {
			// method onOptionsItemSelected was not yet declared
			typeDeclaration.bodyDeclarations().add(methodDeclaration);
		}
		return ifSt;
	}

	/**
	 * @param ifSt
	 * @param methodDeclaration
	 */
	private void addingElseExpression(IfStatement ifSt, MethodDeclaration methodDeclaration) {
		Block block = typeDeclaration.getAST().newBlock();
		ReturnStatement returnStatement = typeDeclaration.getAST().newReturnStatement();
		List<String> arguments = new ArrayList<String>();
		arguments.add(CodeGeneratorBasedOnMenuConstants.ITEM);
		SuperMethodInvocation superMethodInvocation = createSuperMethodInvocation(
				CodeGeneratorBasedOnMenuConstants.ON_OPTIONS_ITEM_SELECTED, arguments);
		returnStatement.setExpression(superMethodInvocation);
		addStatementIfNotFound(block, returnStatement, false);
		ifSt.setElseStatement(block);
	}

	/**
	 * @return "else if" chain for each menu item id
	 */
	private IfStatement createElseIfForEachMenuItemId(MenuItemNode node) {
		IfStatement ifStatement = typeDeclaration.getAST().newIfStatement();
		addElseIfForEachMenuItemId(ifStatement, node);
		return ifStatement;
	}

	/**
	 * Creates else if and else statements for each menu item node
	 * 
	 * @param ifSt
	 *            If statement where the next "else if" will be appended
	 * @param node
	 *            Menu node
	 */
	private void addElseIfForEachMenuItemId(IfStatement ifSt, MenuItemNode node) {
		MethodInvocation invocation = typeDeclaration.getAST().newMethodInvocation();
		invocation.setExpression(getVariableName(CodeGeneratorBasedOnMenuConstants.ITEM));
		SimpleName getIdName = typeDeclaration.getAST().newSimpleName(CodeGeneratorBasedOnMenuConstants.GET_ITEM_ID);
		invocation.setName(getIdName);

		SimpleName r = typeDeclaration.getAST().newSimpleName(JavaViewBasedOnLayoutModifierConstants.R);
		SimpleName id = typeDeclaration.getAST().newSimpleName(JavaViewBasedOnLayoutModifierConstants.ID);
		QualifiedName rid = typeDeclaration.getAST().newQualifiedName(r, id);
		SimpleName guiId = typeDeclaration.getAST().newSimpleName(node.getId());
		QualifiedName guiQN = typeDeclaration.getAST().newQualifiedName(rid, guiId);
		createElseIfAndElseStatements(ifSt, invocation, guiQN);
	}
}