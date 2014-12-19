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
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodRef;
import org.eclipse.jdt.core.dom.MethodRefParameter;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.util.NLS;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

/**
 * Abstract class that contains the basic structure to create java classes
 * programatically
 */
public abstract class JavaClass {
	protected static Annotation OVERRIDE_ANNOTATION;

	static {
		AST tempAST = AST.newAST(AST.JLS3);

		OVERRIDE_ANNOTATION = tempAST.newMarkerAnnotation();
		OVERRIDE_ANNOTATION.setTypeName(tempAST.newSimpleName("Override"));

		tempAST = null;
	}

	protected CompilationUnit compUnit;

	protected AST ast;

	protected TypeDeclaration classDecl = null;

	protected IDocument document;

	protected String className;

	protected String[] packageName;

	protected String[] superClass;

	/**
	 * Class constructor. Creates the basic elements for the class: package name
	 * and class declaration based on a super class.
	 * 
	 * @param className
	 *            The simple class name
	 * @param packageName
	 *            The full-qualified class package name
	 * @param superClass
	 *            The full-qualified super class name
	 */
	@SuppressWarnings("unchecked")
	protected JavaClass(String className, String packageName, String superClass) {
		// It is expected that the parameters have been validated
		// by the UI
		Assert.isNotNull(className);
		Assert.isNotNull(packageName);
		Assert.isNotNull(superClass);

		this.className = className;
		this.packageName = getFQNAsArray(packageName);
		this.superClass = getFQNAsArray(superClass);

		// The package name must have two identifiers at least, according to
		// the Android specifications
		Assert.isLegal(packageName.length() > 1);
		// So, the superclass must have at least two identifiers plus the name
		Assert.isLegal(superClass.length() > 2);

		ast = AST.newAST(AST.JLS3);
		compUnit = ast.newCompilationUnit();

		Type superClassType = null;

		// Sets the package name to the class
		PackageDeclaration pd = ast.newPackageDeclaration();
		QualifiedName qPackageName = ast.newQualifiedName(ast.newName(getQualifier(this.packageName)),
				ast.newSimpleName(getName(this.packageName)));
		pd.setName(qPackageName);
		compUnit.setPackage(pd);

		// Imports the super class
		ImportDeclaration id = ast.newImportDeclaration();
		id.setName(ast.newName(superClass));
		compUnit.imports().add(id);
		superClassType = ast.newSimpleType(ast.newName(getName(this.superClass)));

		// Creates the class
		classDecl = ast.newTypeDeclaration();
		classDecl.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		if (superClassType != null) {
			classDecl.setSuperclassType(superClassType);
		}

		classDecl.setName(ast.newSimpleName(className));
		compUnit.types().add(classDecl);

		document = new Document(compUnit.toString());
	}

	/**
	 * Gets the class content
	 * 
	 * @return an IDocument object containing the class content
	 */
	public IDocument getClassContent() throws AndroidException {
		String content = compUnit.toString();
		document = new Document(content);

		// Formats the code using the Eclipse settings
		CodeFormatter codeFormatter = ToolFactory.createCodeFormatter(DefaultCodeFormatterConstants
				.getEclipseDefaultSettings());

		TextEdit textEdit = codeFormatter.format(CodeFormatter.K_COMPILATION_UNIT | CodeFormatter.F_INCLUDE_COMMENTS,
				content, 0, content.length(), 0, null);

		try {
			textEdit.apply(document);
		} catch (MalformedTreeException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_JavaClass_ErrorFormattingSourceCode, className);

			StudioLogger.error(JavaClass.class, errMsg, e);
			throw new AndroidException(errMsg);
		} catch (BadLocationException e) {
			String errMsg = NLS.bind(CodeUtilsNLS.EXC_JavaClass_ErrorFormattingSourceCode, className);

			StudioLogger.error(JavaClass.class, errMsg, e);
			throw new AndroidException(errMsg);
		}

		addComments();

		return document;
	}

	/**
	 * Adds comments to the code. As we cannot add comments when we are making
	 * the AST, comments need to be insert after the class creation
	 */
	protected abstract void addComments() throws AndroidException;

	/**
	 * Adds a parameter to a method
	 * 
	 * @param method
	 *            The method object
	 * @param parameterName
	 *            The parameter name
	 * @param parameterType
	 *            The parameter type (only the single name, not the qualified)
	 */
	@SuppressWarnings("unchecked")
	protected void addMethodParameter(MethodDeclaration method, String parameterName, Type parameterType) {
		SingleVariableDeclaration vd = ast.newSingleVariableDeclaration();
		vd.setName(ast.newSimpleName(parameterName));
		vd.setType(parameterType);
		method.parameters().add(vd);
	}

	/**
	 * Adds a comment to a BodyDeclaration object. For now, this method does
	 * nothing.
	 * 
	 * @param element
	 *            The element to add the comment
	 * @param comment
	 *            The comment
	 */
	// @SuppressWarnings("unchecked")
	protected void addComment(BodyDeclaration element, String comment) {
		// TODO These comments will be reviewed for the Phase B
		/*
		 * Javadoc javadoc = element.getJavadoc(); TextElement textElement =
		 * ast.newTextElement(); TagElement tagElement = ast.newTagElement();
		 * 
		 * if (javadoc == null) { javadoc = ast.newJavadoc();
		 * element.setJavadoc(javadoc); }
		 * 
		 * textElement.setText(comment);
		 * tagElement.fragments().add(textElement);
		 * javadoc.tags().add(tagElement);
		 */
	}

	/**
	 * Adds documentation reference to a method (the see tag to the javadoc)
	 * 
	 * @param element
	 *            The method declaration object
	 * @param qualifiedClassName
	 *            The full qualified class name to refer
	 * @param methodName
	 *            The method to refer
	 * @param parameters
	 *            The method parameters
	 */
	@SuppressWarnings("unchecked")
	protected void addMethodReference(MethodDeclaration element, String qualifiedClassName, String methodName,
			Type[] parameters) {
		String[] fqnArray = getFQNAsArray(qualifiedClassName);

		MethodRef methodRef = ast.newMethodRef();
		methodRef.setQualifier(ast.newQualifiedName(ast.newName(getQualifier(fqnArray)),
				ast.newSimpleName(getName(fqnArray))));

		methodRef.setName(ast.newSimpleName(methodName));

		if ((parameters != null) && (parameters.length > 0)) {
			for (Type param : parameters) {
				MethodRefParameter methodParam = ast.newMethodRefParameter();
				methodParam.setType(param);
				methodRef.parameters().add(methodParam);
			}
		}

		Javadoc javadoc = element.getJavadoc();
		TagElement tagElement = ast.newTagElement();
		tagElement.setTagName(TagElement.TAG_SEE);

		if (javadoc == null) {
			javadoc = ast.newJavadoc();
			element.setJavadoc(javadoc);
		}

		tagElement.fragments().add(methodRef);
		javadoc.tags().add(tagElement);
	}

	/**
	 * Adds an empty block to a method declaration
	 * 
	 * @param method
	 *            The method declaration
	 * @param returnType
	 *            The method return type. If the method does not have one, use
	 *            null
	 */
	@SuppressWarnings("unchecked")
	protected void addEmptyBlock(MethodDeclaration method) {
		Expression expression = null;
		Block emptyBlock = ast.newBlock();
		ReturnStatement returnStatement = ast.newReturnStatement();
		Type returnType = method.getReturnType2();

		if (returnType instanceof PrimitiveType) {
			PrimitiveType pType = (PrimitiveType) returnType;
			if (pType.getPrimitiveTypeCode() == PrimitiveType.BOOLEAN) {
				expression = ast.newBooleanLiteral(false);
			} else if (pType.getPrimitiveTypeCode() != PrimitiveType.VOID) {
				expression = ast.newNumberLiteral("0");
			}
		} else {
			expression = ast.newNullLiteral();
		}

		if (expression != null) {
			returnStatement.setExpression(expression);
			emptyBlock.statements().add(returnStatement);
		}

		method.setBody(emptyBlock);
	}

	/**
	 * Creates a new string Type object
	 * 
	 * @return a new string Type object
	 */
	protected Type stringType() {
		return ast.newSimpleType(ast.newSimpleName(String.class.getSimpleName()));
	}

	/**
	 * Creates a new string array Type object
	 * 
	 * @return a new string array Type object
	 */
	protected Type stringArrayType() {
		return ast.newArrayType(ast.newSimpleType(ast.newName(String.class.getSimpleName())));
	}

	/**
	 * Creates a new int array Type object
	 * 
	 * @return a new int array Type object
	 */
	protected Type intArrayType() {
		return ast.newArrayType(ast.newPrimitiveType(PrimitiveType.INT));
	}

	/**
	 * Returns a full qualified class name as a array
	 * 
	 * @param fqn
	 *            The full qualified class name
	 * @return the full qualified class name as a array
	 */
	protected static String[] getFQNAsArray(String fqn) {
		String[] parts;

		if (fqn.contains(".")) {
			parts = fqn.split("\\.");
		} else {
			parts = new String[] { fqn };
		}

		return parts;
	}

	/**
	 * Retrieves the qualifier for a full qualified name. Example:
	 * com.motorola.studio.android.MyClass
	 * 
	 * The qualifier for the class is com.motorola.studio.android
	 * 
	 * @param qualifiedName
	 *            The full qualified name
	 * @return The qualifier
	 */
	protected static String[] getQualifier(String[] qualifiedName) {
		String[] qualifier;

		if (qualifiedName.length > 1) {
			qualifier = new String[qualifiedName.length - 1];

			System.arraycopy(qualifiedName, 0, qualifier, 0, qualifiedName.length - 1);
		} else {
			qualifier = qualifiedName;
		}

		return qualifier;
	}

	/**
	 * Gets the name part from a full qualified name
	 * 
	 * @param qualifiedName
	 *            The full qualified name
	 * 
	 * @return The name part from a full qualified name
	 */
	protected static String getName(String[] qualifiedName) {
		String name = null;

		if ((qualifiedName != null) && (qualifiedName.length > 0)) {
			name = qualifiedName[qualifiedName.length - 1];
		}

		return name;
	}

}
