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
import org.eclipse.andmore.android.model.WidgetProvider;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

/**
 * Class used to create an Android Widget Provider building block class
 */
public class WidgetProviderClass extends JavaClass {

	private static final String[] INTENT_CLASS = getFQNAsArray("android.content.Intent");

	private static final String[] CONTEXT_CLASS = getFQNAsArray("android.content.Context");

	private static final String[] APP_WIDGET_MANAGER_CLASS = getFQNAsArray("android.appwidget.AppWidgetManager");

	private static final String ON_UPDATE_METHOD_NAME = "onUpdate";

	private static final String ON_DELETED_METHOD_NAME = "onDeleted";

	private static final String ON_ENABLED_METHOD_NAME = "onEnabled";

	private static final String ON_DISABLED_METHOD_NAME = "onDisabled";

	private static final String ON_RECEIVE_METHOD_NAME = "onReceive";

	private ASTRewrite rewrite = null;

	/**
	 * The constructor
	 *
	 * @param className
	 *            The simple class name
	 * @param packageName
	 *            The full-qualified class package name
	 */
	public WidgetProviderClass(String className, String packageName) {
		super(className, packageName, WidgetProvider.WIDGET_PROVIDER_SUPER_CLASS);

		addBasicWidgetProviderInfo();
	}

	/**
	 * Adds basic information to the Widget Provider class
	 */
	@SuppressWarnings("unchecked")
	private void addBasicWidgetProviderInfo() {
		// Adds import declarations
		ImportDeclaration intentImport = ast.newImportDeclaration();
		intentImport.setName(ast.newName(INTENT_CLASS));
		compUnit.imports().add(intentImport);

		ImportDeclaration contextImport = ast.newImportDeclaration();
		contextImport.setName(ast.newName(CONTEXT_CLASS));
		compUnit.imports().add(contextImport);

		ImportDeclaration widgetManagerImport = ast.newImportDeclaration();
		widgetManagerImport.setName(ast.newName(APP_WIDGET_MANAGER_CLASS));
		compUnit.imports().add(widgetManagerImport);

		// Add override methods
		addOnUpdateMethod();
		addOnDeletedMethod();
		addOnEnabledMethod();
		addOnDisabledMethod();
		addOnReceiveMethod();

	}

	/**
	 * Adds the onUpdate method to the widget provider class
	 */
	@SuppressWarnings("unchecked")
	private void addOnUpdateMethod() {
		final String WIDGET_IDS_PARAM = "appWidgetIds";

		// Method declaration
		MethodDeclaration onUpdateMethod = ast.newMethodDeclaration();
		onUpdateMethod.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		onUpdateMethod.setReturnType2(ast.newPrimitiveType(PrimitiveType.VOID));
		onUpdateMethod.setName(ast.newSimpleName(ON_UPDATE_METHOD_NAME));

		// Parameters
		addMethodParameter(onUpdateMethod, getName(CONTEXT_CLASS).toLowerCase(),
				ast.newSimpleType(ast.newSimpleName(getName(CONTEXT_CLASS))));
		addMethodParameter(onUpdateMethod, getName(APP_WIDGET_MANAGER_CLASS).toLowerCase(),
				ast.newSimpleType(ast.newSimpleName(getName(APP_WIDGET_MANAGER_CLASS))));
		addMethodParameter(onUpdateMethod, WIDGET_IDS_PARAM, intArrayType());
		addEmptyBlock(onUpdateMethod);
		classDecl.bodyDeclarations().add(onUpdateMethod);

	}

	/**
	 * Adds the onDelete method to the widget provider class
	 */
	@SuppressWarnings("unchecked")
	private void addOnDeletedMethod() {
		final String WIDGET_IDS_PARAM = "appWidgetIds";

		// Method declaration
		MethodDeclaration onDeletedMethod = ast.newMethodDeclaration();
		onDeletedMethod.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		onDeletedMethod.setReturnType2(ast.newPrimitiveType(PrimitiveType.VOID));
		onDeletedMethod.setName(ast.newSimpleName(ON_DELETED_METHOD_NAME));

		// Parameters
		addMethodParameter(onDeletedMethod, getName(CONTEXT_CLASS).toLowerCase(),
				ast.newSimpleType(ast.newSimpleName(getName(CONTEXT_CLASS))));
		addMethodParameter(onDeletedMethod, WIDGET_IDS_PARAM, intArrayType());
		addEmptyBlock(onDeletedMethod);
		classDecl.bodyDeclarations().add(onDeletedMethod);
	}

	/**
	 * Adds the onEnabled method to the widget provider class
	 */
	@SuppressWarnings("unchecked")
	private void addOnEnabledMethod() {
		// Method declaration
		MethodDeclaration onEnabledMethod = ast.newMethodDeclaration();
		onEnabledMethod.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		onEnabledMethod.setReturnType2(ast.newPrimitiveType(PrimitiveType.VOID));
		onEnabledMethod.setName(ast.newSimpleName(ON_ENABLED_METHOD_NAME));

		// Parameters
		addMethodParameter(onEnabledMethod, getName(CONTEXT_CLASS).toLowerCase(),
				ast.newSimpleType(ast.newSimpleName(getName(CONTEXT_CLASS))));

		addEmptyBlock(onEnabledMethod);
		classDecl.bodyDeclarations().add(onEnabledMethod);
	}

	/**
	 * Adds the onDisabled method to the widget provider class
	 */
	@SuppressWarnings("unchecked")
	private void addOnDisabledMethod() {
		// Method declaration
		MethodDeclaration onDisabledMethod = ast.newMethodDeclaration();
		onDisabledMethod.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		onDisabledMethod.setReturnType2(ast.newPrimitiveType(PrimitiveType.VOID));
		onDisabledMethod.setName(ast.newSimpleName(ON_DISABLED_METHOD_NAME));

		// Parameters
		addMethodParameter(onDisabledMethod, getName(CONTEXT_CLASS).toLowerCase(),
				ast.newSimpleType(ast.newSimpleName(getName(CONTEXT_CLASS))));

		addEmptyBlock(onDisabledMethod);
		classDecl.bodyDeclarations().add(onDisabledMethod);
	}

	/**
	 * Adds the onReceive method to the widget provider class
	 */
	@SuppressWarnings("unchecked")
	private void addOnReceiveMethod() {
		// Method declaration
		MethodDeclaration onReceiveMethod = ast.newMethodDeclaration();
		onReceiveMethod.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		onReceiveMethod.setReturnType2(ast.newPrimitiveType(PrimitiveType.VOID));
		onReceiveMethod.setName(ast.newSimpleName(ON_RECEIVE_METHOD_NAME));

		// Parameters
		addMethodParameter(onReceiveMethod, getName(CONTEXT_CLASS).toLowerCase(),
				ast.newSimpleType(ast.newSimpleName(getName(CONTEXT_CLASS))));
		addMethodParameter(onReceiveMethod, getName(INTENT_CLASS).toLowerCase(),
				ast.newSimpleType(ast.newSimpleName(getName(INTENT_CLASS))));

		addEmptyBlock(onReceiveMethod);
		classDecl.bodyDeclarations().add(onReceiveMethod);
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

		TypeDeclaration widgetProviderClass = (TypeDeclaration) compUnit.types().get(0);
		MethodDeclaration method;
		Block block;

		// Adds the Override annotation and ToDo comment to all overridden
		// methods
		for (int i = 0; i < widgetProviderClass.bodyDeclarations().size(); i++) {
			method = (MethodDeclaration) widgetProviderClass.bodyDeclarations().get(i);

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

			StudioLogger.error(BroadcastReceiverClass.class, errMsg, e);
			throw new AndroidException(errMsg);
		} catch (MalformedTreeException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_JavaClass_ErrorApplyingCommentsToCode, className);

			StudioLogger.error(BroadcastReceiverClass.class, errMsg, e);
			throw new AndroidException(errMsg);
		} catch (BadLocationException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_JavaClass_ErrorApplyingCommentsToCode, className);

			StudioLogger.error(BroadcastReceiverClass.class, errMsg, e);
			throw new AndroidException(errMsg);
		}
	}

}
