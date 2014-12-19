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
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
 * Class that deals with GUI that use on click as listener (Button, ImageButton,
 * ToggleButton, Checkbox)
 */
public class OnClickGUIsCodeGenerator extends AbstractLayoutCodeGenerator {

	/**
	 * @param codeGeneratorData
	 * @param onCreateDeclaration
	 * @param typeDeclaration
	 */
	public OnClickGUIsCodeGenerator(CodeGeneratorDataBasedOnLayout codeGeneratorData,
			MethodDeclaration onCreateDeclaration, TypeDeclaration typeDeclaration) {
		super(codeGeneratorData, onCreateDeclaration, typeDeclaration);
	}

	@Override
	public void generateCode(IProgressMonitor monitor) throws JavaModelException {
		onClickMethodsForButtonsFromLayoutXML(monitor);
		addOnClickHandler(monitor);
	}

	/**
	 * Adds methods declared to deal with Buttons' events (declared on layout
	 * XML)
	 * 
	 * NOTE: we exclude fragments because there is an exception that do not let
	 * them to be handled this way
	 * 
	 * <br>
	 * GENERATED_CODE_FORMAT: <br>
	 * public void $ONCLICK_ATTRIBUTE_FROMXML(View target) { }
	 * 
	 */
	@SuppressWarnings("unchecked")
	private void onClickMethodsForButtonsFromLayoutXML(IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor);
		subMonitor.beginTask(CodeUtilsNLS.JavaViewBasedOnLayoutModifier_AddingOnClickMethodFromXML, codeGeneratorData
				.getGuiItems().size());
		for (LayoutNode node : codeGeneratorData.getGuiItems()) {
			if (node.getNodeType().equals(LayoutNode.LayoutNodeViewType.Button.name()) && (node.getOnClick() != null)
					&& node.shouldInsertCode()
					&& codeGeneratorData.getAssociatedType().equals(CodeGeneratorDataBasedOnLayout.TYPE.ACTIVITY)) {
				boolean containMethodDeclared = onClickFromXmlAlreadyDeclared(node);
				if (!containMethodDeclared) {
					// avoid to declare method twice

					// generate for buttons which have onClick declaration
					MethodDeclaration methodDeclaration = addMethodDeclaration(ModifierKeyword.PUBLIC_KEYWORD,
							node.getOnClick(), PrimitiveType.VOID, JavaViewBasedOnLayoutModifierConstants.VIEW_CLASS,
							JavaViewBasedOnLayoutModifierConstants.VIEW_VARIABLE_NAME);
					Block block = typeDeclaration.getAST().newBlock();
					// empty block
					methodDeclaration.setBody(block);
					typeDeclaration.bodyDeclarations().add(methodDeclaration);
				}
			}

			subMonitor.worked(1);
		}
	}

	/**
	 * Add methods to on click handler
	 * 
	 * NOTE: We include fragments that have onClick declared on XML (because it
	 * is an exception in the framework)
	 * 
	 * <br>
	 * GENERATED_CODE_FORMAT: <br>
	 * $GUI_ID.setOnClickListener(new View.OnClickListener() { <br>
	 * public void onClick(View target) { <br>
	 * } <br>
	 * });
	 */
	private void addOnClickHandler(IProgressMonitor monitor) throws JavaModelException {
		SubMonitor subMonitor = SubMonitor.convert(monitor);
		subMonitor.beginTask(CodeUtilsNLS.JavaViewBasedOnLayoutModifier_AddingOnClickHandler, codeGeneratorData
				.getGuiItems().size());
		for (LayoutNode node : codeGeneratorData.getGuiItems()) {
			boolean containMethodDeclared = checkIfInvokeMethodIsDeclared(node,
					JavaViewBasedOnLayoutModifierConstants.SET_ON_CLICK_LISTENER);
			if (!containMethodDeclared && node.shouldInsertCode()) {
				if (node.getNodeType().equals(LayoutNode.LayoutNodeViewType.CheckBox.name())
						|| node.getNodeType().equals(LayoutNode.LayoutNodeViewType.ImageButton.name())
						|| node.getNodeType().equals(LayoutNode.LayoutNodeViewType.ToggleButton.name())
						|| (node.getNodeType().equals(LayoutNode.LayoutNodeViewType.Button.name()) && (codeGeneratorData
								.getAssociatedType().equals(CodeGeneratorDataBasedOnLayout.TYPE.FRAGMENT) || (node
								.getOnClick() == null)))) {
					MethodDeclaration methodDeclaration = addMethodDeclaration(ModifierKeyword.PUBLIC_KEYWORD,
							JavaViewBasedOnLayoutModifierConstants.METHOD_NAME_ON_CLICK, PrimitiveType.VOID,
							JavaViewBasedOnLayoutModifierConstants.VIEW_CLASS,
							JavaViewBasedOnLayoutModifierConstants.VIEW_VARIABLE_NAME);
					Block block = onCreateDeclaration.getAST().newBlock();
					methodDeclaration.setBody(block);
					addMethodInvocationToListenerHandler(node.getNodeId(),
							JavaViewBasedOnLayoutModifierConstants.SET_ON_CLICK_LISTENER,
							JavaViewBasedOnLayoutModifierConstants.VIEW_CLASS,
							JavaViewBasedOnLayoutModifierConstants.METHOD_ON_CLICK_LISTENER, methodDeclaration);
				}
			}
			subMonitor.worked(1);
		}
	}
}
