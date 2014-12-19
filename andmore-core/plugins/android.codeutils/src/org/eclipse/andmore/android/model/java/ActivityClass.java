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
package org.eclipse.andmore.android.model.java;

import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

/**
 * Class used to create an Android Activity building block class
 */
public class ActivityClass extends JavaClass {
	public static final String ACTIVITY_SUPERCLASS = "android.app.Activity";

	private static final String[] BUNDLE_CLASS = getFQNAsArray("android.os.Bundle");

	protected static final String ONCREATE_METHOD_NAME = "onCreate";

	protected static final String ONSTART_METHOD_NAME = "onStart";

	private ASTRewrite rewrite = null;

	/**
	 * The constructor
	 * 
	 * @param className
	 *            The simple class name
	 * @param packageName
	 *            The full-qualified class package name
	 * @param addOnStart
	 *            If true, adds the OnStart method to the activity class
	 */
	public ActivityClass(String className, String packageName, boolean addOnStart) {
		super(className, packageName, ACTIVITY_SUPERCLASS);
		addBasicActivityInfo();

		if (addOnStart) {
			addOnStartMethod();
		}
	}

	/**
	 * Adds basic information to the activity class
	 */
	private void addBasicActivityInfo() {
		addOnCreateMethod();

		// Adds JavaDoc to elements
		addComment(classDecl, CodeUtilsNLS.MODEL_ActivityClass_ActivityDescription);
	}

	/**
	 * Adds the onCreate method to the activity class
	 */
	@SuppressWarnings("unchecked")
	private void addOnCreateMethod() {
		// Adds import for Bundle
		ImportDeclaration intentImport = ast.newImportDeclaration();
		intentImport.setName(ast.newName(BUNDLE_CLASS));
		compUnit.imports().add(intentImport);

		MethodDeclaration onCreateMethod = ast.newMethodDeclaration();
		onCreateMethod.modifiers().add(ast.newModifier(ModifierKeyword.PROTECTED_KEYWORD));
		onCreateMethod.setReturnType2(ast.newPrimitiveType(PrimitiveType.VOID));
		onCreateMethod.setName(ast.newSimpleName(ONCREATE_METHOD_NAME));
		addMethodParameter(onCreateMethod, "savedInstanceState",
				ast.newSimpleType(ast.newSimpleName(getName(BUNDLE_CLASS))));
		addEmptyBlock(onCreateMethod);
		addSuperInvocation(onCreateMethod);
		classDecl.bodyDeclarations().add(onCreateMethod);

		// Adds JavaDoc to the method
		addComment(onCreateMethod, CodeUtilsNLS.MODEL_ActivityClass_OnCreateMethodDescription);
		addMethodReference(onCreateMethod, ACTIVITY_SUPERCLASS, ONCREATE_METHOD_NAME,
				new Type[] { ast.newSimpleType(ast.newSimpleName(getName(BUNDLE_CLASS))) });
	}

	/**
	 * Adds the onStart method to the activity class
	 */
	@SuppressWarnings("unchecked")
	private void addOnStartMethod() {
		MethodDeclaration onStartMethod = ast.newMethodDeclaration();
		onStartMethod.modifiers().add(ast.newModifier(ModifierKeyword.PROTECTED_KEYWORD));
		onStartMethod.setReturnType2(ast.newPrimitiveType(PrimitiveType.VOID));
		onStartMethod.setName(ast.newSimpleName(ONSTART_METHOD_NAME));
		addEmptyBlock(onStartMethod);
		addSuperInvocation(onStartMethod);
		classDecl.bodyDeclarations().add(onStartMethod);

		// Adds JavaDoc to the method
		addComment(onStartMethod, CodeUtilsNLS.MODEL_ActivityClass_OnStartMethodDescription);
		addMethodReference(onStartMethod, ACTIVITY_SUPERCLASS, ONSTART_METHOD_NAME, null);
	}

	/**
	 * Adds a "super.method(arguments...)" statement inside the given method
	 * body
	 * 
	 * @param method
	 *            The method declaration
	 */
	@SuppressWarnings("unchecked")
	private void addSuperInvocation(MethodDeclaration method) {
		SuperMethodInvocation superInv = ast.newSuperMethodInvocation();
		superInv.setName(ast.newSimpleName(method.getName().toString()));
		for (Object param : method.parameters()) {
			if (param instanceof SingleVariableDeclaration) {
				SingleVariableDeclaration vd = (SingleVariableDeclaration) param;
				String varName = vd.getName().toString();
				superInv.arguments().add(ast.newSimpleName(varName));
			}
		}
		method.getBody().statements().add(ast.newExpressionStatement(superInv));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.studio.android.model.java.JavaClass#addComments()
	 */
	@Override
	protected void addComments() throws AndroidException {
		ASTNode todoComment;

		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setSource(document.get().toCharArray());

		compUnit = (CompilationUnit) parser.createAST(null);
		ast = compUnit.getAST();
		rewrite = ASTRewrite.create(ast);

		todoComment = rewrite.createStringPlaceholder(CodeUtilsNLS.MODEL_Common_ToDoPutYourCodeHere,
				ASTNode.EMPTY_STATEMENT);

		TypeDeclaration activityClass = (TypeDeclaration) compUnit.types().get(0);
		MethodDeclaration method;
		Block block;

		// Adds the Override annotation and ToDo comment to all overridden
		// methods
		for (int i = 0; i < activityClass.bodyDeclarations().size(); i++) {
			method = (MethodDeclaration) activityClass.bodyDeclarations().get(i);

			// Adds the Override annotation
			rewrite.getListRewrite(method, method.getModifiersProperty()).insertFirst(OVERRIDE_ANNOTATION, null);

			// Adds the ToDo comment
			block = method.getBody();
			rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY).insertLast(todoComment, null);
		}

		try {
			// Writes the modifications
			TextEdit modifications = rewrite.rewriteAST(document, null);
			modifications.apply(document);
		} catch (IllegalArgumentException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_JavaClass_ErrorApplyingCommentsToCode, className);

			StudioLogger.error(ActivityClass.class, errMsg, e);
			throw new AndroidException(errMsg);
		} catch (MalformedTreeException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_JavaClass_ErrorApplyingCommentsToCode, className);

			StudioLogger.error(ActivityClass.class, errMsg, e);
			throw new AndroidException(errMsg);
		} catch (BadLocationException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_JavaClass_ErrorApplyingCommentsToCode, className);

			StudioLogger.error(ActivityClass.class, errMsg, e);
			throw new AndroidException(errMsg);
		}
	}
}
