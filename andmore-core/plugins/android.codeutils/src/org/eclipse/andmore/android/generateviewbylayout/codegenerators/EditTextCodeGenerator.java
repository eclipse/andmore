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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.generateviewbylayout.JavaViewBasedOnLayoutModifierConstants;
import org.eclipse.andmore.android.generateviewbylayout.model.CodeGeneratorDataBasedOnLayout;
import org.eclipse.andmore.android.generateviewbylayout.model.LayoutNode;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
 * Responsible to add methods to deal with EditText events
 */
public class EditTextCodeGenerator extends AbstractLayoutCodeGenerator {
	/**
	 * @param codeGeneratorData
	 * @param onCreateDeclaration
	 * @param typeDeclaration
	 */
	public EditTextCodeGenerator(CodeGeneratorDataBasedOnLayout codeGeneratorData,
			MethodDeclaration onCreateDeclaration, TypeDeclaration typeDeclaration) {
		super(codeGeneratorData, onCreateDeclaration, typeDeclaration);
	}

	@Override
	public void generateCode(IProgressMonitor monitor) throws JavaModelException {
		addOnKeyHandler(monitor);
	}

	/**
	 * Add methods to deal with key handler (EditText's event)
	 * 
	 * <br>
	 * GENERATED_CODE_FORMAT: <br>
	 * $GUI_ID.setOnKeyListener(new View.OnKeyListener() { <br>
	 * public boolean onKey(View target, int keyCode, KeyEvent event) { <br>
	 * return true; <br>
	 * } <br>
	 * });
	 */
	@SuppressWarnings("unchecked")
	private void addOnKeyHandler(IProgressMonitor monitor) throws JavaModelException {
		SubMonitor subMonitor = SubMonitor.convert(monitor);
		subMonitor.beginTask(CodeUtilsNLS.JavaViewBasedOnLayoutModifier_AddingOnKeyHandler, codeGeneratorData
				.getGuiItems().size());
		/*
		 * AST to be written:
		 * 
		 * MethodInvocations String methodName = SimpleName("setOnKeyListener")
		 * String optionalExpression = SimpleName(guiId);
		 * arguments=ClassInstanceCreation AnonymousClassDeclaration var
		 * body=MethodDeclaration(); modifier=public methodName =
		 * SimpleName("onKey"); List<SingleVariableDeclaration> List<String>
		 * types converter p/ SimpleType (pode ser PrimitiveType) List<String>
		 * names converter p/ SimpleName ReturnStatement optionalExpression
		 * BooleanLiteral type=SimpleType("listenerName")
		 */
		for (LayoutNode node : codeGeneratorData.getGuiItems()) {
			if (node.shouldInsertCode() && node.getNodeType().equals(LayoutNode.LayoutNodeViewType.EditText.name())) {
				boolean containMethodDeclared = checkIfInvokeMethodIsDeclared(node,
						JavaViewBasedOnLayoutModifierConstants.SET_ON_KEY_LISTENER);
				if (!containMethodDeclared) {
					List<SingleVariableDeclaration> parameters = new ArrayList<SingleVariableDeclaration>();
					SingleVariableDeclaration param1 = createVariableDeclarationFromStrings(
							JavaViewBasedOnLayoutModifierConstants.VIEW_CLASS,
							JavaViewBasedOnLayoutModifierConstants.VIEW_VARIABLE_NAME);
					parameters.add(param1);
					SingleVariableDeclaration param2 = createVariableDeclarationPrimitiveCode(PrimitiveType.INT,
							JavaViewBasedOnLayoutModifierConstants.KEY_CODE);
					parameters.add(param2);
					SingleVariableDeclaration param3 = createVariableDeclarationFromStrings(
							JavaViewBasedOnLayoutModifierConstants.KEY_EVENT,
							JavaViewBasedOnLayoutModifierConstants.EVENT);
					parameters.add(param3);
					MethodDeclaration methodDeclaration = addMethodDeclaration(ModifierKeyword.PUBLIC_KEYWORD,
							JavaViewBasedOnLayoutModifierConstants.ON_KEY, PrimitiveType.BOOLEAN, parameters);
					Block block = onCreateDeclaration.getAST().newBlock();
					methodDeclaration.setBody(block);
					ReturnStatement statement = onCreateDeclaration.getAST().newReturnStatement();
					statement.setExpression(onCreateDeclaration.getAST().newBooleanLiteral(true));
					methodDeclaration.getBody().statements().add(statement);
					addMethodInvocationToListenerHandler(node.getNodeId(),
							JavaViewBasedOnLayoutModifierConstants.SET_ON_KEY_LISTENER,
							JavaViewBasedOnLayoutModifierConstants.VIEW_CLASS,
							JavaViewBasedOnLayoutModifierConstants.ON_KEY_LISTENER, methodDeclaration);
				}
			}
			subMonitor.worked(1);
		}
	}
}
