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
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

/**
 * Class used to create an Android Service building block class
 */
public class ServiceClass extends JavaClass {
	private static final String SERVICE_SUPERCLASS = "android.app.Service";

	private static final String[] INTENT_CLASS = getFQNAsArray("android.content.Intent");

	private static final String[] IBINDER_CLASS = getFQNAsArray("android.os.IBinder");

	private static final String ONBIND_METHOD_NAME = "onBind";

	private static final String ONCREATE_METHOD_NAME = "onCreate";

	private static final String ONSTART_METHOD_NAME = "onStart";

	private ASTRewrite rewrite = null;

	/**
	 * The constructor
	 * 
	 * @param className
	 *            The simple class name
	 * @param packageName
	 *            The full-qualified class package name
	 * @param addOnCreate
	 *            If true, adds the OnCreate method to the service class
	 * @param addOnStart
	 *            If true, adds the OnStart method to the service class
	 */
	public ServiceClass(String className, String packageName, boolean addOnCreate, boolean addOnStart) {
		super(className, packageName, SERVICE_SUPERCLASS);
		addBasicServiceInfo();

		if (addOnCreate) {
			addOnCreateMethod();
		}

		if (addOnStart) {
			addOnStartMethod();
		}
	}

	/**
	 * Adds basic information to the service class
	 */
	@SuppressWarnings("unchecked")
	private void addBasicServiceInfo() {
		// Adds import for Intent
		ImportDeclaration intentImport = ast.newImportDeclaration();
		intentImport.setName(ast.newName(INTENT_CLASS));
		compUnit.imports().add(intentImport);

		// Adds import for IBinder
		ImportDeclaration ibinderImport = ast.newImportDeclaration();
		ibinderImport.setName(ast.newName(IBINDER_CLASS));
		compUnit.imports().add(ibinderImport);

		// Adds onBind method
		MethodDeclaration onBindMethod = ast.newMethodDeclaration();
		onBindMethod.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		onBindMethod.setReturnType2(ast.newSimpleType(ast.newSimpleName(IBINDER_CLASS[2])));
		onBindMethod.setName(ast.newSimpleName(ONBIND_METHOD_NAME));
		addMethodParameter(onBindMethod, getName(INTENT_CLASS).toLowerCase(),
				ast.newSimpleType(ast.newSimpleName(getName(INTENT_CLASS))));
		addEmptyBlock(onBindMethod);
		classDecl.bodyDeclarations().add(onBindMethod);

		// Adds JavaDoc to elements
		addComment(classDecl, CodeUtilsNLS.MODEL_ServiceClass_ServiceDescription);
		addComment(onBindMethod, CodeUtilsNLS.MODEL_ServiceClass_OnBindMethodDescription);
		addMethodReference(onBindMethod, SERVICE_SUPERCLASS, ONBIND_METHOD_NAME,
				new Type[] { ast.newSimpleType(ast.newSimpleName(getName(INTENT_CLASS))) });
	}

	/**
	 * Adds the onCreate method to the service class
	 */
	@SuppressWarnings("unchecked")
	private void addOnCreateMethod() {
		MethodDeclaration onCreateMethod = ast.newMethodDeclaration();
		onCreateMethod.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		onCreateMethod.setReturnType2(ast.newPrimitiveType(PrimitiveType.VOID));
		onCreateMethod.setName(ast.newSimpleName(ONCREATE_METHOD_NAME));
		addEmptyBlock(onCreateMethod);
		classDecl.bodyDeclarations().add(onCreateMethod);

		// Adds JavaDoc to the method
		addComment(onCreateMethod, CodeUtilsNLS.MODEL_ServiceClass_OnCreateMethodDescription);
		addMethodReference(onCreateMethod, SERVICE_SUPERCLASS, ONCREATE_METHOD_NAME, null);
	}

	/**
	 * Adds the onStart method to the service class
	 */
	@SuppressWarnings("unchecked")
	private void addOnStartMethod() {
		MethodDeclaration onStartMethod = ast.newMethodDeclaration();
		onStartMethod.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		onStartMethod.setReturnType2(ast.newPrimitiveType(PrimitiveType.VOID));
		onStartMethod.setName(ast.newSimpleName(ONSTART_METHOD_NAME));
		addMethodParameter(onStartMethod, getName(INTENT_CLASS).toLowerCase(),
				ast.newSimpleType(ast.newSimpleName(getName(INTENT_CLASS))));
		addMethodParameter(onStartMethod, "startId", ast.newPrimitiveType(PrimitiveType.INT));
		addEmptyBlock(onStartMethod);
		classDecl.bodyDeclarations().add(onStartMethod);

		// Adds JavaDoc to the method
		addComment(onStartMethod, CodeUtilsNLS.MODEL_ServiceClass_OnStartMethodDescription);
		addMethodReference(
				onStartMethod,
				SERVICE_SUPERCLASS,
				ONSTART_METHOD_NAME,
				new Type[] { ast.newSimpleType(ast.newSimpleName(getName(INTENT_CLASS))),
						ast.newPrimitiveType(PrimitiveType.INT) });
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

		TypeDeclaration serviceClass = (TypeDeclaration) compUnit.types().get(0);
		MethodDeclaration method;
		Block block;

		// Adds the Override annotation and ToDo comment to all abstract methods
		for (int i = 0; i < serviceClass.bodyDeclarations().size(); i++) {
			method = (MethodDeclaration) serviceClass.bodyDeclarations().get(i);

			// Adds the Override annotation
			rewrite.getListRewrite(method, method.getModifiersProperty()).insertFirst(OVERRIDE_ANNOTATION, null);

			// Adds the ToDo comment
			block = method.getBody();
			rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY).insertFirst(todoComment, null);
		}

		try {
			// Writes the modifications
			TextEdit modifications = rewrite.rewriteAST(document, null);
			modifications.apply(document);
		} catch (IllegalArgumentException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_JavaClass_ErrorApplyingCommentsToCode, className);

			StudioLogger.error(ServiceClass.class, errMsg, e);
			throw new AndroidException(errMsg);
		} catch (MalformedTreeException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_JavaClass_ErrorApplyingCommentsToCode, className);

			StudioLogger.error(ServiceClass.class, errMsg, e);
			throw new AndroidException(errMsg);
		} catch (BadLocationException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_JavaClass_ErrorApplyingCommentsToCode, className);

			StudioLogger.error(ServiceClass.class, errMsg, e);
			throw new AndroidException(errMsg);
		}
	}
}
