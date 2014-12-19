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
 * Class used to create an Android Broadcast Receiver building block class
 */
public class BroadcastReceiverClass extends JavaClass {
	private static final String BROADCAST_RECEIVER_SUPERCLASS = "android.content.BroadcastReceiver";

	private static final String[] INTENT_CLASS = getFQNAsArray("android.content.Intent");

	private static final String[] CONTEXT_CLASS = getFQNAsArray("android.content.Context");

	private static final String ONRECEIVE_METHOD_NAME = "onReceive";

	private ASTRewrite rewrite = null;

	/**
	 * The constructor
	 *
	 * @param className
	 *            The simple class name
	 * @param packageName
	 *            The full-qualified class package name
	 */
	public BroadcastReceiverClass(String className, String packageName) {
		super(className, packageName, BROADCAST_RECEIVER_SUPERCLASS);

		addBasicBroadcastReceiverInfo();
	}

	/**
	 * Adds basic information to the broadcast receiver class
	 */
	@SuppressWarnings("unchecked")
	private void addBasicBroadcastReceiverInfo() {
		// Adds import for Intent
		ImportDeclaration intentImport = ast.newImportDeclaration();
		intentImport.setName(ast.newName(INTENT_CLASS));
		compUnit.imports().add(intentImport);

		ImportDeclaration contextImport = ast.newImportDeclaration();
		contextImport.setName(ast.newName(CONTEXT_CLASS));
		compUnit.imports().add(contextImport);

		addOnReceiveMethod();

		// Adds JavaDoc to elements
		addComment(classDecl, CodeUtilsNLS.MODEL_BroadcastReceiverClass_BroadcastReceiverDescription);

	}

	/**
	 * Adds the onReceive method to the broadcast receiver class
	 */
	@SuppressWarnings("unchecked")
	private void addOnReceiveMethod() {
		MethodDeclaration onReceiveMethod = ast.newMethodDeclaration();
		onReceiveMethod.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		onReceiveMethod.setReturnType2(ast.newPrimitiveType(PrimitiveType.VOID));
		onReceiveMethod.setName(ast.newSimpleName(ONRECEIVE_METHOD_NAME));
		addMethodParameter(onReceiveMethod, getName(CONTEXT_CLASS).toLowerCase(),
				ast.newSimpleType(ast.newSimpleName(getName(CONTEXT_CLASS))));
		addMethodParameter(onReceiveMethod, getName(INTENT_CLASS).toLowerCase(),
				ast.newSimpleType(ast.newSimpleName(getName(INTENT_CLASS))));
		addEmptyBlock(onReceiveMethod);
		classDecl.bodyDeclarations().add(onReceiveMethod);

		// Adds JavaDoc to the method
		addComment(onReceiveMethod, CodeUtilsNLS.MODEL_BroadcastReceiverClass_onReceiveMethodDescription);
		addMethodReference(
				onReceiveMethod,
				BROADCAST_RECEIVER_SUPERCLASS,
				ONRECEIVE_METHOD_NAME,
				new Type[] { ast.newSimpleType(ast.newSimpleName(getName(CONTEXT_CLASS))),
						ast.newSimpleType(ast.newSimpleName(getName(INTENT_CLASS))) });
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

		TypeDeclaration receiverClass = (TypeDeclaration) compUnit.types().get(0);
		MethodDeclaration method;
		Block block;

		// Adds the Override annotation and ToDo comment to all overridden
		// methods
		for (int i = 0; i < receiverClass.bodyDeclarations().size(); i++) {
			method = (MethodDeclaration) receiverClass.bodyDeclarations().get(i);

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