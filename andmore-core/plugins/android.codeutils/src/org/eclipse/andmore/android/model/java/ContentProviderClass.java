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
import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

/**
 * Class used to create an Android Content Provider building block class
 */
public class ContentProviderClass extends JavaClass {
	private static final String CP_SUPERCLASS = "android.content.ContentProvider";

	private static final String[] URI_CLASS = getFQNAsArray("android.net.Uri");

	private static final String[] CONTENTVALUES_CLASS = getFQNAsArray("android.content.ContentValues");

	private static final String[] CURSOR_CLASS = getFQNAsArray("android.database.Cursor");

	private static final String DELETE_METHOD_NAME = "delete";

	private static final String GETTYPE_METHOD_NAME = "getType";

	private static final String INSERT_METHOD_NAME = "insert";

	private static final String ONCREATE_METHOD_NAME = "onCreate";

	private static final String QUERY_METHOD_NAME = "query";

	private static final String UPDATE_METHOD_NAME = "update";

	private static final String CONTENT_SCHEME = "content://";

	private static final String CONTENT_URI_NAME = "CONTENT_URI";

	private ASTRewrite rewrite;

	private String authority;

	/**
	 * Default constructor
	 * 
	 * @param className
	 *            The simple class name
	 * @param packageName
	 *            The full-qualified class package name
	 */
	public ContentProviderClass(String className, String packageName, String authority) {
		super(className, packageName, CP_SUPERCLASS);

		Assert.isNotNull(authority);
		this.authority = authority;

		addBasicCPInfo();
	}

	/**
	 * Adds basic information to the content provider class
	 */
	@SuppressWarnings("unchecked")
	private void addBasicCPInfo() {
		ImportDeclaration importDecl;

		// Adds import for Uri
		importDecl = ast.newImportDeclaration();
		importDecl.setName(ast.newName(URI_CLASS));
		compUnit.imports().add(importDecl);

		// Adds import for ContentValues
		importDecl = ast.newImportDeclaration();
		importDecl.setName(ast.newName(CONTENTVALUES_CLASS));
		compUnit.imports().add(importDecl);

		// Adds import for Cursor
		importDecl = ast.newImportDeclaration();
		importDecl.setName(ast.newName(CURSOR_CLASS));
		compUnit.imports().add(importDecl);

		// Adds the authorities constants
		addAuthority();

		// Adds the delete method
		addDeleteMethod();

		// Adds the getType method
		addGetTypeMethod();

		// Adds the insert method
		addInsertMethod();

		// Adds the onCreate method
		addOnCreateMethod();

		// Adds the query method
		addQueryMethod();

		// Adds the update method
		addUpdateMethod();

		// Adds JavaDoc to elements
		addComment(classDecl, CodeUtilsNLS.MODEL_ContentProviderClass_ContentProviderDescription);
	}

	/**
	 * Adds the default content provider Uri
	 */
	@SuppressWarnings("unchecked")
	private void addAuthority() {
		final String URI_PARSE_METHOD = "parse";

		String contentUriValue = CONTENT_SCHEME + authority;
		StringLiteral stringLiteral = ast.newStringLiteral();
		stringLiteral.setLiteralValue(contentUriValue);

		Name uri = ast.newSimpleName(getName(URI_CLASS));

		MethodInvocation parse = ast.newMethodInvocation();
		parse.setExpression(uri);
		parse.setName(ast.newSimpleName(URI_PARSE_METHOD));
		parse.arguments().add(stringLiteral);

		VariableDeclarationFragment contentUri = ast.newVariableDeclarationFragment();

		contentUri.setName(ast.newSimpleName(CONTENT_URI_NAME));
		contentUri.setInitializer(parse);

		FieldDeclaration fieldDeclaration = ast.newFieldDeclaration(contentUri);
		fieldDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		fieldDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.STATIC_KEYWORD));
		fieldDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.FINAL_KEYWORD));
		fieldDeclaration.setType(uriType());

		classDecl.bodyDeclarations().add(fieldDeclaration);

		addComment(fieldDeclaration, CodeUtilsNLS.MODEL_ContentProviderClass_ContentUriDescription);
	}

	/**
	 * Adds the delete method to the content provider class
	 */
	@SuppressWarnings("unchecked")
	private void addDeleteMethod() {
		final String SELECTION_PARAM = "selection";
		final String SELECTION_ARGS_PARAM = "selectionArgs";

		MethodDeclaration method = ast.newMethodDeclaration();
		method.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		method.setReturnType2(ast.newPrimitiveType(PrimitiveType.INT));
		method.setName(ast.newSimpleName(DELETE_METHOD_NAME));
		addMethodParameter(method, getName(URI_CLASS).toLowerCase(), uriType());
		addMethodParameter(method, SELECTION_PARAM, stringType());
		addMethodParameter(method, SELECTION_ARGS_PARAM, stringArrayType());
		addEmptyBlock(method);
		classDecl.bodyDeclarations().add(method);

		addComment(method, CodeUtilsNLS.MODEL_ContentProviderClass_DeleteMethodDescription);
		addMethodReference(method, CP_SUPERCLASS, DELETE_METHOD_NAME, new Type[] { uriType(), stringType(),
				stringArrayType() });
	}

	/**
	 * Adds the getType method to the content provider class
	 */
	@SuppressWarnings("unchecked")
	private void addGetTypeMethod() {
		MethodDeclaration method = ast.newMethodDeclaration();
		method.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		method.setReturnType2(stringType());
		method.setName(ast.newSimpleName(GETTYPE_METHOD_NAME));
		addMethodParameter(method, getName(URI_CLASS).toLowerCase(), uriType());

		addEmptyBlock(method);
		classDecl.bodyDeclarations().add(method);

		addComment(method, CodeUtilsNLS.MODEL_ContentProviderClass_GetTypeMethodDescription);
		addMethodReference(method, CP_SUPERCLASS, GETTYPE_METHOD_NAME, new Type[] { uriType() });
	}

	/**
	 * Adds the insert method to the content provider class
	 */
	@SuppressWarnings("unchecked")
	private void addInsertMethod() {
		final String VALUES_PARAM = "values";

		MethodDeclaration method = ast.newMethodDeclaration();
		method.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		method.setReturnType2(uriType());
		method.setName(ast.newSimpleName(INSERT_METHOD_NAME));
		addMethodParameter(method, getName(URI_CLASS).toLowerCase(), uriType());
		addMethodParameter(method, VALUES_PARAM, contentValuesType());
		addEmptyBlock(method);
		classDecl.bodyDeclarations().add(method);

		addComment(method, CodeUtilsNLS.MODEL_ContentProviderClass_InsertMethodDescription);
		addMethodReference(method, CP_SUPERCLASS, INSERT_METHOD_NAME, new Type[] { uriType(), contentValuesType() });
	}

	/**
	 * Adds the onCreate method to the content provider class
	 */
	@SuppressWarnings("unchecked")
	private void addOnCreateMethod() {
		MethodDeclaration method = ast.newMethodDeclaration();
		method.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		method.setReturnType2(ast.newPrimitiveType(PrimitiveType.BOOLEAN));
		method.setName(ast.newSimpleName(ONCREATE_METHOD_NAME));
		addEmptyBlock(method);
		classDecl.bodyDeclarations().add(method);

		addComment(method, CodeUtilsNLS.MODEL_ContentProviderClass_OnCreateMethodDescription);
		addMethodReference(method, CP_SUPERCLASS, ONCREATE_METHOD_NAME, null);
	}

	/**
	 * Adds the query method to the content provider class
	 */
	@SuppressWarnings("unchecked")
	private void addQueryMethod() {
		final String PROJECTION_PARAM = "projection";
		final String SELECTION_PARAM = "selection";
		final String SELECTION_ARGS_PARAM = "selectionArgs";
		final String SORT_ORDER_PARAM = "sortOrder";

		MethodDeclaration method = ast.newMethodDeclaration();
		method.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		method.setReturnType2(cursorType());
		method.setName(ast.newSimpleName(QUERY_METHOD_NAME));
		addMethodParameter(method, getName(URI_CLASS).toLowerCase(), uriType());
		addMethodParameter(method, PROJECTION_PARAM, stringArrayType());
		addMethodParameter(method, SELECTION_PARAM, stringType());
		addMethodParameter(method, SELECTION_ARGS_PARAM, stringArrayType());
		addMethodParameter(method, SORT_ORDER_PARAM, stringType());

		addEmptyBlock(method);
		classDecl.bodyDeclarations().add(method);

		addComment(method, CodeUtilsNLS.MODEL_ContentProviderClass_QueryMethodDescription);
		addMethodReference(method, CP_SUPERCLASS, QUERY_METHOD_NAME, new Type[] { uriType(), stringArrayType(),
				stringType(), stringArrayType(), stringType() });
	}

	/**
	 * Adds the update method to the content provider class
	 */
	@SuppressWarnings("unchecked")
	private void addUpdateMethod() {
		final String VALUES_PARAM = "values";
		final String SELECTION_PARAM = "selection";
		final String SELECTION_ARGS_PARAM = "selectionArgs";

		MethodDeclaration method = ast.newMethodDeclaration();
		method.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		method.setReturnType2(ast.newPrimitiveType(PrimitiveType.INT));
		method.setName(ast.newSimpleName(UPDATE_METHOD_NAME));
		addMethodParameter(method, getName(URI_CLASS).toLowerCase(), uriType());
		addMethodParameter(method, VALUES_PARAM, contentValuesType());
		addMethodParameter(method, SELECTION_PARAM, stringType());
		addMethodParameter(method, SELECTION_ARGS_PARAM, stringArrayType());

		addEmptyBlock(method);
		classDecl.bodyDeclarations().add(method);

		addComment(method, CodeUtilsNLS.MODEL_ContentProviderClass_UpdateMethodDescription);
		addMethodReference(method, CP_SUPERCLASS, UPDATE_METHOD_NAME, new Type[] { uriType(), contentValuesType(),
				stringType(), stringArrayType() });
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

		TypeDeclaration cpClass = (TypeDeclaration) compUnit.types().get(0);
		MethodDeclaration method;
		Block block;

		// Adds the Override annotation and ToDo comment to all abstract methods
		for (int i = 0; i < cpClass.bodyDeclarations().size(); i++) {
			BodyDeclaration bodyDecl = (BodyDeclaration) cpClass.bodyDeclarations().get(i);

			if (bodyDecl instanceof MethodDeclaration) {
				method = (MethodDeclaration) bodyDecl;

				// Adds the Override annotation
				rewrite.getListRewrite(method, method.getModifiersProperty()).insertFirst(OVERRIDE_ANNOTATION, null);

				// Adds the ToDo comment
				block = method.getBody();
				rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY).insertFirst(todoComment, null);
			}
		}

		try {
			// Writes the modifications
			TextEdit modifications = rewrite.rewriteAST(document, null);
			modifications.apply(document);
		} catch (IllegalArgumentException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_JavaClass_ErrorApplyingCommentsToCode, className);

			StudioLogger.error(ContentProviderClass.class, errMsg, e);
			throw new AndroidException(errMsg);
		} catch (MalformedTreeException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_JavaClass_ErrorApplyingCommentsToCode, className);

			StudioLogger.error(ContentProviderClass.class, errMsg, e);
			throw new AndroidException(errMsg);
		} catch (BadLocationException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_JavaClass_ErrorApplyingCommentsToCode, className);

			StudioLogger.error(ContentProviderClass.class, errMsg, e);
			throw new AndroidException(errMsg);
		}
	}

	/**
	 * Returns a new Uri type
	 * 
	 * @return a new Uri type
	 */
	private Type uriType() {
		return ast.newSimpleType(ast.newSimpleName(getName(URI_CLASS)));
	}

	/**
	 * Returns a new ContentValues type
	 * 
	 * @return a new ContentValues type
	 */
	private Type contentValuesType() {
		return ast.newSimpleType(ast.newSimpleName(getName(CONTENTVALUES_CLASS)));
	}

	/**
	 * Returns a new Cursor type
	 * 
	 * @return a new Cursor type
	 */
	private Type cursorType() {
		return ast.newSimpleType(ast.newSimpleName(getName(CURSOR_CLASS)));
	}

}
