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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.android.generateviewbylayout.JavaViewBasedOnLayoutModifierConstants;
import org.eclipse.andmore.android.generateviewbylayout.model.LayoutNode;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.PrimitiveType.Code;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.WildcardType;

/**
 * Abstract code generator class that has common methods to generate code (for
 * Menu, GUI handlers, findViewById, attributes).
 */
public abstract class AbstractCodeGenerator {
	protected static final String THIZ = "this.";

	protected TypeDeclaration typeDeclaration;

	/**
	 * Default constructor
	 * 
	 * @param typeDeclaration
	 *            AST for the type to modify
	 */
	public AbstractCodeGenerator(TypeDeclaration typeDeclaration) {
		this.typeDeclaration = typeDeclaration;
	}

	/**
	 * Generates code by changing AST (Abstract Syntax tree) for a given Android
	 * class
	 * 
	 * @param monitor
	 *            concrete implementation should use
	 *            <code>SubMonitor.convert(monitor)</code> to display status
	 *            messages during code generation
	 * @throws JavaModelException
	 *             if any error occurs to create new/modified AST to be written
	 */
	public abstract void generateCode(IProgressMonitor monitor) throws JavaModelException;

	/**
	 * Constructs a new method declaration on type declaration (for a single
	 * parameter). Warning: Calling methods need to fill the body and add the
	 * item into typeDeclaration
	 * 
	 * @param node
	 */
	protected MethodDeclaration addMethodDeclaration(ModifierKeyword modifierKeyword, String methodNameStr,
			Code returnType, String parameterClazzType, String parameterVariableName) {
		SingleVariableDeclaration singleVarDecl = createVariableDeclarationFromStrings(parameterClazzType,
				parameterVariableName);
		List<SingleVariableDeclaration> parameters = new ArrayList<SingleVariableDeclaration>();
		parameters.add(singleVarDecl);
		MethodDeclaration methodDeclaration = addMethodDeclaration(modifierKeyword, methodNameStr, returnType,
				parameters);
		return methodDeclaration;
	}

	/**
	 * Constructs a new method declaration on type declaration (for multiple
	 * parameters). Warning: Calling methods need to fill the body and add the
	 * item into typeDeclaration
	 * 
	 * @param node
	 */
	@SuppressWarnings("unchecked")
	protected MethodDeclaration addMethodDeclaration(ModifierKeyword modifierKeyword, String methodNameStr,
			Code returnType, List<SingleVariableDeclaration> parameters) {
		MethodDeclaration methodDeclaration = typeDeclaration.getAST().newMethodDeclaration();
		Modifier mod = typeDeclaration.getAST().newModifier(modifierKeyword);
		methodDeclaration.modifiers().add(mod);
		SimpleName methodName = typeDeclaration.getAST().newSimpleName(methodNameStr);
		methodDeclaration.setName(methodName);
		PrimitiveType voidType = typeDeclaration.getAST().newPrimitiveType(returnType);
		methodDeclaration.setReturnType2(voidType);

		if (parameters != null) {
			for (SingleVariableDeclaration param : parameters) {
				methodDeclaration.parameters().add(param);
			}
		}
		return methodDeclaration;
	}

	/**
	 * 
	 * @param parameterClazzType
	 * @param parameterVariableName
	 * @return
	 */
	protected SingleVariableDeclaration createVariableDeclarationFromStrings(String parameterClazzType,
			String parameterVariableName) {
		SingleVariableDeclaration singleVarDecl = typeDeclaration.getAST().newSingleVariableDeclaration();
		singleVarDecl.setType(typeDeclaration.getAST().newSimpleType(getViewName(parameterClazzType)));
		SimpleName variableName = getVariableName(parameterVariableName);
		singleVarDecl.setName(variableName);
		return singleVarDecl;
	}

	/**
	 * Creates a variable declaration when it required a List with a
	 * parameterized type
	 * 
	 * @param parameterClazzType
	 * @param typeArgument
	 *            used to create variables such as List<T>
	 * @param parameterVariableName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected SingleVariableDeclaration createWildcardTypeVariableDeclarationFromStrings(String parameterClazzType,
			String parameterVariableName) {
		SingleVariableDeclaration singleVarDecl = typeDeclaration.getAST().newSingleVariableDeclaration();
		SimpleType type = typeDeclaration.getAST().newSimpleType(getViewName(parameterClazzType));
		ParameterizedType paramType = typeDeclaration.getAST().newParameterizedType(type);
		WildcardType wildcardType = typeDeclaration.getAST().newWildcardType();
		paramType.typeArguments().add(wildcardType);
		singleVarDecl.setType(paramType);
		SimpleName variableName = getVariableName(parameterVariableName);
		singleVarDecl.setName(variableName);
		return singleVarDecl;
	}

	/**
	 * @param parameterClazzType
	 * @param parameterVariableName
	 * @return AST
	 */
	protected SingleVariableDeclaration createVariableDeclarationPrimitiveCode(Code code, String parameterVariableName) {
		SingleVariableDeclaration singleVarDecl = typeDeclaration.getAST().newSingleVariableDeclaration();
		singleVarDecl.setType(typeDeclaration.getAST().newPrimitiveType(code));
		SimpleName variableName = getVariableName(parameterVariableName);
		singleVarDecl.setName(variableName);
		return singleVarDecl;
	}

	/**
	 * @return AST representation for the name
	 */
	protected SimpleName getVariableName(String name) {
		SimpleName variableName = typeDeclaration.getAST().newSimpleName(name);
		return variableName;
	}

	/**
	 * @return AST for simple type
	 */
	protected SimpleType getListenerSimpleType(String clazzType, String methodListenerMethodName) {
		SimpleName listenerName = typeDeclaration.getAST().newSimpleName(methodListenerMethodName);
		SimpleName viewName = getViewName(clazzType);
		QualifiedName listenerQualifiedName = typeDeclaration.getAST().newQualifiedName(viewName, listenerName);
		SimpleType listenerType = typeDeclaration.getAST().newSimpleType(listenerQualifiedName);
		return listenerType;
	}

	/**
	 * @return AST Simple Name for the clazz type
	 */
	protected SimpleName getViewName(String clazzType) {
		SimpleName viewName = typeDeclaration.getAST().newSimpleName(clazzType);
		return viewName;
	}

	/**
	 * Returns the index of the inflate invocation
	 * 
	 * @param index
	 * @param expression
	 */
	protected int findInflateIndexAtStatement(int index, Expression expression) {
		int foundIndex = -1;
		if (expression instanceof MethodInvocation) {
			MethodInvocation inflateInvocation = (MethodInvocation) expression;
			if ((inflateInvocation.getName() != null) && inflateInvocation.getName().getIdentifier().equals("inflate")) {
				foundIndex = index;
			}
		}
		return foundIndex;
	}

	/**
	 * Checks if onClick is already declared in Java Activity (based on layout
	 * XML declaration)
	 * 
	 * @param node
	 * @return true if declared, false otherwise
	 */
	protected boolean onClickFromXmlAlreadyDeclared(LayoutNode node) {
		boolean containMethodDeclared = false;
		if (typeDeclaration.bodyDeclarations() != null) {
			// check if method already declared
			for (Object bd : typeDeclaration.bodyDeclarations()) {
				if (bd instanceof MethodDeclaration) {
					MethodDeclaration md = (MethodDeclaration) bd;
					if ((md.getName() != null) && md.getName().toString().equals(node.getOnClick())) {
						containMethodDeclared = true;
						break;
					}
				}
			}
		}
		return containMethodDeclared;
	}

	/**
	 * Checks if a method is already declared
	 * 
	 * @param methodToCheck
	 *            method to verify if already existent in the code
	 * @param bindingString
	 * @return null if there method not declared yet, or the the method found
	 *         (if already declared)
	 */
	protected MethodDeclaration isMethodAlreadyDeclared(MethodDeclaration methodToCheck, String bindingString) {
		MethodDeclaration result = null;
		if (typeDeclaration.bodyDeclarations() != null) {
			// check if method already declared
			for (Object bd : typeDeclaration.bodyDeclarations()) {
				if (bd instanceof MethodDeclaration) {
					MethodDeclaration md = (MethodDeclaration) bd;
					IMethodBinding binding = md.resolveBinding();
					if ((binding != null) && (bindingString != null)
							&& binding.toString().trim().equals(bindingString.trim())) {
						result = md;
						break;
					}
				}
			}
		}
		return result;
	}

	/**
	 * Adds a statement into methodDeclaration if the statement is not already
	 * declared. If the last statement is a {@link ReturnStatement}, it inserts
	 * before it, otherwise it inserts as the last statement in the block
	 * 
	 * @param methodDeclaration
	 * @param declarationStatement
	 * @param sameClass
	 *            true, it will compare if there is the same statement class in
	 *            the body of the methodDeclaration (but the content may be
	 *            different), false it will ignore the class and it will compare
	 *            if the content is the same (given by toString.equals())
	 */
	protected void addStatementIfNotFound(MethodDeclaration methodDeclaration, Statement declarationStatement,
			boolean sameClass) {
		addStatementIfNotFound(methodDeclaration.getBody(), declarationStatement, sameClass);
	}

	/**
	 * Adds a statement into block if the statement is not already declared. If
	 * the last statement is a {@link ReturnStatement}, it inserts before it,
	 * otherwise it inserts as the last statement in the block
	 * 
	 * @param block
	 * @param declarationStatement
	 * @param sameClass
	 *            true, it will compare if there is the same statement class in
	 *            the body of the methodDeclaration (but the content may be
	 *            different), false it will ignore the class and it will compare
	 *            if the content is the same (given by toString.equals())
	 */
	@SuppressWarnings("unchecked")
	protected void addStatementIfNotFound(Block block, Statement declarationStatement, boolean sameClass) {
		boolean alreadyDeclared = false;
		List<Statement> statements = block.statements();
		alreadyDeclared = isStatementAlreadyDeclared(declarationStatement, sameClass, statements);
		if (!alreadyDeclared) {
			Statement statement = block.statements().size() > 0 ? (Statement) block.statements().get(
					block.statements().size() - 1) : null;
			if ((statement instanceof ReturnStatement) && !(declarationStatement instanceof ReturnStatement)) {
				// need to insert before return
				block.statements().add(block.statements().size() - 1, declarationStatement);
			} else {
				block.statements().add(declarationStatement);
			}
		}
	}

	/**
	 * Checks if the given declarationg statement is already available in the
	 * list of statements
	 * 
	 * @param declarationStatement
	 * @param sameClass
	 *            true, it will compare if there is the same statement class in
	 *            the body of the methodDeclaration (but the content may be
	 *            different), false it will ignore the class and it will compare
	 *            if the content is the same (given by toString.equals())
	 * @param alreadyDeclared
	 * @param statements
	 * @return true if statement found, false otherwise
	 */
	protected boolean isStatementAlreadyDeclared(Statement declarationStatement, boolean sameClass,
			List<Statement> statements) {
		boolean alreadyDeclared = false;
		if (statements != null) {
			for (Statement statement : statements) {
				if ((!sameClass && declarationStatement.toString().equals(statement.toString()))
						|| (sameClass && statement.getClass().equals(declarationStatement.getClass()))) {
					alreadyDeclared = true;
					break;
				}
			}
		}
		return alreadyDeclared;
	}

	/**
	 * Finds a statement if already declared
	 * 
	 * @param declarationStatement
	 * @param sameClass
	 *            true, it will compare if there is the same statement class in
	 *            the body of the methodDeclaration (but the content may be
	 *            different), false it will ignore the class and it will compare
	 *            if the content is the same (given by toString.equals())
	 * @param alreadyDeclared
	 * @param statements
	 * @return null if not found, the reference to the statement if it is found
	 */
	protected Statement findIfStatementAlreadyDeclared(Statement declarationStatement, boolean sameClass,
			List<Statement> statements) {
		Statement foundStatement = null;
		if (statements != null) {
			for (Statement statement : statements) {
				if ((!sameClass && declarationStatement.toString().equals(statement.toString()))
						|| (sameClass && statement.getClass().equals(declarationStatement.getClass()))) {
					foundStatement = statement;
					break;
				}
			}
		}
		return foundStatement;
	}

	/**
	 * Inserts method in the format super.$superMethodName($list_params); and
	 * inserts it into the methodDeclaration (if not already available)
	 * 
	 * @param methodDeclaration
	 * @param superMethodName
	 * @param arguments
	 *            null if not necessary or a list of arguments to pass for
	 *            method
	 */
	@SuppressWarnings("unchecked")
	public void insertSuperInvocation(MethodDeclaration methodDeclaration, String superMethodName,
			List<String> arguments) {
		boolean alreadyHaveMethod = false;
		if (methodDeclaration.getBody() != null) {
			// check if method already declared
			for (Object bd : methodDeclaration.getBody().statements()) {
				if (bd instanceof ExpressionStatement) {
					ExpressionStatement es = (ExpressionStatement) bd;
					Expression ex = es.getExpression();
					if (ex instanceof SuperMethodInvocation) {
						SuperMethodInvocation smi = (SuperMethodInvocation) ex;
						if (smi.getName().toString().equals(superMethodName)) {
							alreadyHaveMethod = true;
							break;
						}
					}
				}
			}
		}
		if (!alreadyHaveMethod) {
			SuperMethodInvocation superInvoke = createSuperMethodInvocation(superMethodName, arguments);
			ExpressionStatement exprSt = methodDeclaration.getAST().newExpressionStatement(superInvoke);
			methodDeclaration.getBody().statements().add(exprSt);
		}
	}

	/**
	 * Creates a method in the format super.$superMethodName($list_params);
	 * 
	 * @param superMethodName
	 * @param arguments
	 *            null if not necessary or a list of arguments to pass for
	 *            method
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected SuperMethodInvocation createSuperMethodInvocation(String superMethodName, List<String> arguments) {
		SuperMethodInvocation superInvoke = typeDeclaration.getAST().newSuperMethodInvocation();
		SimpleName onSaveStateName = typeDeclaration.getAST().newSimpleName(superMethodName);
		superInvoke.setName(onSaveStateName);
		if (arguments != null) {
			for (String a : arguments) {
				SimpleName arg = typeDeclaration.getAST().newSimpleName(a);
				superInvoke.arguments().add(arg);
			}
		}
		return superInvoke;
	}

	@SuppressWarnings("unchecked")
	/**
	 * Generates AST to invoke a method with the given structure <code>prefix.methodName(){}</code>. 
	 * This code avoids method invocation be duplicated in the {@link MethodDeclaration}.
	 * @param method declared method to insert the invocation
	 * @param prefix
	 * @param methodName
	 */
	protected void invokeMethod(MethodDeclaration method, String prefix, String methodName) {
		boolean alreadyHaveMethod = false;
		if (method.getBody() != null) {
			// check if method already declared
			for (Object bd : method.getBody().statements()) {
				if (bd instanceof ExpressionStatement) {
					ExpressionStatement es = (ExpressionStatement) bd;
					Expression ex = es.getExpression();
					if (ex instanceof MethodInvocation) {
						MethodInvocation mi = (MethodInvocation) ex;
						if (mi.getName().toString().equals(methodName) && mi.getExpression().toString().equals(prefix)) {
							alreadyHaveMethod = true;
							break;
						}
					}
				}
			}
		}
		if (!alreadyHaveMethod) {
			MethodInvocation invoke = createMethodInvocation(prefix, methodName);
			ExpressionStatement commitExpr = method.getAST().newExpressionStatement(invoke);
			method.getBody().statements().add(commitExpr);
		}
	}

	/**
	 * Create a method invocation in the format <code>prefix.methodName()</code>
	 * 
	 * @param prefix
	 *            null if does not have
	 * @param methodName
	 * @return {@link MethodInvocation}
	 */
	protected MethodInvocation createMethodInvocation(String prefix, String methodName) {
		MethodInvocation invoke = typeDeclaration.getAST().newMethodInvocation();
		SimpleName methodInvokeName = typeDeclaration.getAST().newSimpleName(methodName);
		invoke.setName(methodInvokeName);
		if (prefix != null) {
			SimpleName prefixName = typeDeclaration.getAST().newSimpleName(prefix);
			invoke.setExpression(prefixName);
		}
		return invoke;
	}

	/**
	 * Recursive private method to verify if an radio button id is in a
	 * "else if" chain
	 * 
	 * @param ifSt
	 * @param expression
	 * @return
	 */
	protected boolean ifChainContainsExpression(IfStatement ifSt, Expression expression) {

		boolean containsExpression = false;
		Statement elseStatement = ifSt.getElseStatement();

		// verifies if the first if's expression already verifies the current
		// radio button.
		// the characters "(" and ")" are added to avoid that an substring is
		// considered true
		if (ifSt.getExpression().toString().equals(expression.toString())) {
			containsExpression = true;
		} else if ((elseStatement != null) && (elseStatement instanceof IfStatement)) {
			containsExpression = ifChainContainsExpression((IfStatement) elseStatement, expression);
		}

		return containsExpression;
	}

	/**
	 * Recursive private method to retrieve the last if in a "else if" chain
	 * 
	 * @param ifSt
	 * @return
	 */
	protected IfStatement getLastIfStatementInChain(IfStatement ifSt) {

		IfStatement lastStatement = null;
		Statement elseStatement = ifSt.getElseStatement();

		// looks for the if statement which is in a else statement. Will stop
		// when find and if withoud else statement.
		if ((elseStatement != null) && (elseStatement instanceof IfStatement)) {
			lastStatement = getLastIfStatementInChain((IfStatement) elseStatement);
		}
		// lastStatement will receive ifSt because it does not have the else or
		// have an else but it is not and
		else {
			lastStatement = ifSt;
		}

		return lastStatement;
	}

	/**
	 * Creates a chain og else if and else statement for the given if statement
	 * 
	 * @param ifSt
	 * @param invocation
	 * @param guiQN
	 */
	protected void createElseIfAndElseStatements(IfStatement ifSt, MethodInvocation invocation, QualifiedName guiQN) {
		InfixExpression infixExp = typeDeclaration.getAST().newInfixExpression();
		infixExp.setOperator(InfixExpression.Operator.EQUALS);

		infixExp.setLeftOperand(invocation);
		infixExp.setRightOperand(guiQN);

		// first verifies if the expression of the if statement is missing, it
		// means we created it, just need to add the expression.
		// Otherwise, the "else if" chain must be verified before add the new if
		// statement
		if (ifSt.getExpression().toString().equals(JavaViewBasedOnLayoutModifierConstants.EXPRESSION_MISSING)) {
			ifSt.setExpression(infixExp);
		} else {
			boolean expressionAlreadyExists = false;
			// verifies if the first if's expression already verifies the
			// current menu item or radio button
			if (ifChainContainsExpression(ifSt, infixExp)) {
				expressionAlreadyExists = true;
			}

			if (!expressionAlreadyExists) {
				IfStatement lastIfStatement = getLastIfStatementInChain(ifSt);
				if (lastIfStatement != null) {
					IfStatement elseSt = typeDeclaration.getAST().newIfStatement();
					elseSt.setExpression(infixExp);
					if (lastIfStatement.getElseStatement() != null) {
						Statement oldElseStatement = lastIfStatement.getElseStatement();
						elseSt.setElseStatement((Statement) ASTNode.copySubtree(elseSt.getAST(), oldElseStatement));
						lastIfStatement.setElseStatement(elseSt);
					} else {
						lastIfStatement.setElseStatement(elseSt);
					}
				}
			}
		}
	}

	/**
	 * Creates a return statemtn into the method declaration (it only adds the
	 * return if it does not exist yet)
	 * 
	 * @param methodDeclaration
	 *            to add the return statement
	 */
	protected void createReturnStatement(MethodDeclaration methodDeclaration) {
		ReturnStatement returnStatement = typeDeclaration.getAST().newReturnStatement();
		returnStatement.setExpression(typeDeclaration.getAST().newBooleanLiteral(true));
		// try to find a ReturnStatement (may be a different return, but the
		// content may differ)
		addStatementIfNotFound(methodDeclaration, returnStatement, true);
	}
}
