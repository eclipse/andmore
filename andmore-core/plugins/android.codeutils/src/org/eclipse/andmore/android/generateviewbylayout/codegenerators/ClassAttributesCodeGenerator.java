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

import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.generateviewbylayout.model.CodeGeneratorDataBasedOnLayout;
import org.eclipse.andmore.android.generateviewbylayout.model.LayoutNode;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

/**
 * Responsible to generate class attributes based on layout type
 */
public class ClassAttributesCodeGenerator extends AbstractLayoutCodeGenerator {
	/**
	 * @param codeGeneratorData
	 * @param onCreateDeclaration
	 * @param typeDeclaration
	 */
	public ClassAttributesCodeGenerator(CodeGeneratorDataBasedOnLayout codeGeneratorData,
			MethodDeclaration onCreateDeclaration, TypeDeclaration typeDeclaration) {
		super(codeGeneratorData, onCreateDeclaration, typeDeclaration);
	}

	@Override
	public void generateCode(IProgressMonitor monitor) throws JavaModelException {
		addAttributes(monitor);
	}

	/**
	 * Add attributes based on the GUI items declared on layout XML <br>
	 * GENERATED_CODE_FORMAT: <br>
	 * private $GUI_TYPE $GUI_ID;
	 * 
	 * @throws JavaModelException
	 */
	@SuppressWarnings("unchecked")
	private void addAttributes(IProgressMonitor monitor) throws JavaModelException {
		SubMonitor subMonitor = SubMonitor.convert(monitor);
		subMonitor.beginTask(CodeUtilsNLS.JavaViewBasedOnLayoutModifier_AddingAttributes, codeGeneratorData
				.getGuiItems().size());
		/*
		 * AST to be written FieldDeclaration: [Javadoc] { ExtendedModifier }
		 * Type VariableDeclarationFragment { , VariableDeclarationFragment } ;
		 */
		for (LayoutNode node : codeGeneratorData.getGuiItems()) {
			if ((node.getNodeId() != null) && node.shouldInsertCode()) {
				boolean containFieldDeclared = getCodeGeneratorData().getJavaLayoutData().getVisitor()
						.checkIfAttributeAlreadyDeclared(node, false);
				if (!containFieldDeclared) {
					// avoid to declare attribute twice
					Modifier privateMod = typeDeclaration.getAST().newModifier(ModifierKeyword.PRIVATE_KEYWORD);
					SimpleName guiName;
					try {
						guiName = getNodeVariableTypeBasedOnLayoutNode(node);
					} catch (CoreException e) {
						throw new JavaModelException(e);
					}
					SimpleType guiType = typeDeclaration.getAST().newSimpleType(guiName);
					VariableDeclarationFragment variableFragment = typeDeclaration.getAST()
							.newVariableDeclarationFragment();
					SimpleName varName = variableFragment.getAST().newSimpleName(node.getNodeId());
					variableFragment.setName(varName);
					FieldDeclaration declaration = typeDeclaration.getAST().newFieldDeclaration(variableFragment);
					declaration.modifiers().add(privateMod);
					declaration.setType(guiType);
					typeDeclaration.bodyDeclarations().add(0, declaration); // add
																			// as
																			// the
																			// first
																			// item
				}
			}
			subMonitor.worked(1);
		}
	}

}
