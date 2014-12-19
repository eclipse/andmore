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
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
 * Responsible to add methods to deal with Gallery events
 */
public class GalleryCodeGenerator extends AbstractLayoutCodeGenerator {

	/**
	 * @param codeGeneratorData
	 * @param onCreateDeclaration
	 * @param typeDeclaration
	 */
	public GalleryCodeGenerator(CodeGeneratorDataBasedOnLayout codeGeneratorData,
			MethodDeclaration onCreateDeclaration, TypeDeclaration typeDeclaration) {
		super(codeGeneratorData, onCreateDeclaration, typeDeclaration);
	}

	@Override
	public void generateCode(IProgressMonitor monitor) throws JavaModelException {
		addOnItemClickListenerGalleryHandler(monitor);
	}

	/**
	 * Adds methods to deal with Gallery events
	 * 
	 * <br>
	 * GENERATED_CODE_FORMAT: <br>
	 * $GUI_ID.setOnItemClickListener(new OnItemClickListener() { <br>
	 * public void onItemClick(AdapterView parent, View v, int position, long
	 * id) { <br>
	 * } <br>
	 * });
	 */
	private void addOnItemClickListenerGalleryHandler(IProgressMonitor monitor) throws JavaModelException {
		SubMonitor subMonitor = SubMonitor.convert(monitor);
		subMonitor.beginTask(CodeUtilsNLS.JavaViewBasedOnLayoutModifier_AddingGalleryHandler, codeGeneratorData
				.getGuiItems().size());
		for (LayoutNode node : codeGeneratorData.getGuiItems()) {
			if (node.shouldInsertCode() && node.getNodeType().equals(LayoutNode.LayoutNodeViewType.Gallery.name())) {
				boolean containMethodDeclared = checkIfInvokeMethodIsDeclared(node,
						JavaViewBasedOnLayoutModifierConstants.SET_ON_ITEM_CLICK_LISTENER);
				if (!containMethodDeclared) {
					List<SingleVariableDeclaration> parameters1 = new ArrayList<SingleVariableDeclaration>();
					SingleVariableDeclaration param1 = createWildcardTypeVariableDeclarationFromStrings(
							JavaViewBasedOnLayoutModifierConstants.ADAPTER_VIEW,
							JavaViewBasedOnLayoutModifierConstants.PARENT_VIEW);
					parameters1.add(param1);
					SingleVariableDeclaration param2 = createVariableDeclarationFromStrings(
							JavaViewBasedOnLayoutModifierConstants.VIEW_CLASS,
							JavaViewBasedOnLayoutModifierConstants.SELECTED_ITEM_VIEW);
					parameters1.add(param2);
					SingleVariableDeclaration param3 = createVariableDeclarationPrimitiveCode(PrimitiveType.INT,
							JavaViewBasedOnLayoutModifierConstants.POSITION);
					parameters1.add(param3);
					SingleVariableDeclaration param4 = createVariableDeclarationPrimitiveCode(PrimitiveType.LONG,
							JavaViewBasedOnLayoutModifierConstants.ROW);
					parameters1.add(param4);
					MethodDeclaration methodDeclaration1 = addMethodDeclaration(ModifierKeyword.PUBLIC_KEYWORD,
							JavaViewBasedOnLayoutModifierConstants.ON_ITEM_CLICK, PrimitiveType.VOID, parameters1);
					Block block1 = onCreateDeclaration.getAST().newBlock();
					methodDeclaration1.setBody(block1);

					addMethodInvocationToListenerHandler(node.getNodeId(),
							JavaViewBasedOnLayoutModifierConstants.SET_ON_ITEM_CLICK_LISTENER,
							JavaViewBasedOnLayoutModifierConstants.ADAPTER_VIEW,
							JavaViewBasedOnLayoutModifierConstants.ON_ITEM_CLICK_LISTENER, methodDeclaration1);
				}
			}
			subMonitor.worked(1);
		}
	}
}
