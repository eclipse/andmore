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
 * Responsible to add methods to deal with SeekBar events
 */
public class SeekBarCodeGenerator extends AbstractLayoutCodeGenerator {
	/**
	 * @param codeGeneratorData
	 * @param onCreateDeclaration
	 * @param typeDeclaration
	 */
	public SeekBarCodeGenerator(CodeGeneratorDataBasedOnLayout codeGeneratorData,
			MethodDeclaration onCreateDeclaration, TypeDeclaration typeDeclaration) {
		super(codeGeneratorData, onCreateDeclaration, typeDeclaration);
	}

	@Override
	public void generateCode(IProgressMonitor monitor) throws JavaModelException {
		addOnSeekBarChangeListener(monitor);
	}

	/**
	 * Adds methods for SeekBar's events
	 * 
	 * <br>
	 * GENERATED_CODE_FORMAT: <br>
	 * $GUI_ID.setOnSeekBarChangeListener(new OnSeekBarChangeListener() { <br>
	 * void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
	 * {} <br>
	 * void onStartTrackingTouch(SeekBar seekBar) {} <br>
	 * void onStopTrackingTouch(SeekBar seekBar) {} <br>
	 * });
	 */
	private void addOnSeekBarChangeListener(IProgressMonitor monitor) throws JavaModelException {
		SubMonitor subMonitor = SubMonitor.convert(monitor);
		subMonitor.beginTask(CodeUtilsNLS.JavaViewBasedOnLayoutModifier_AddingSeekbarHandler, codeGeneratorData
				.getGuiItems().size());
		for (LayoutNode node : codeGeneratorData.getGuiItems()) {
			if (node.shouldInsertCode() && node.getNodeType().equals(JavaViewBasedOnLayoutModifierConstants.SEEK_BAR)) {
				boolean containMethodDeclared = checkIfInvokeMethodIsDeclared(node,
						JavaViewBasedOnLayoutModifierConstants.SET_ON_SEEK_BAR_CHANGE_LISTENER);
				if (!containMethodDeclared) {
					List<MethodDeclaration> declarations = new ArrayList<MethodDeclaration>();

					List<SingleVariableDeclaration> parameters1 = new ArrayList<SingleVariableDeclaration>();
					SingleVariableDeclaration param1 = createVariableDeclarationFromStrings(
							JavaViewBasedOnLayoutModifierConstants.SEEK_BAR,
							JavaViewBasedOnLayoutModifierConstants.SEEK_BAR_VARIABLE);
					parameters1.add(param1);
					SingleVariableDeclaration param2 = createVariableDeclarationPrimitiveCode(PrimitiveType.INT,
							JavaViewBasedOnLayoutModifierConstants.PROGRESS_VARIABLE);
					parameters1.add(param2);
					SingleVariableDeclaration param3 = createVariableDeclarationPrimitiveCode(PrimitiveType.BOOLEAN,
							JavaViewBasedOnLayoutModifierConstants.FROM_USER_VARIABLE);
					parameters1.add(param3);
					MethodDeclaration methodDeclaration1 = addMethodDeclaration(ModifierKeyword.PUBLIC_KEYWORD,
							JavaViewBasedOnLayoutModifierConstants.ON_PROGRESS_CHANGED, PrimitiveType.VOID, parameters1);
					Block block1 = onCreateDeclaration.getAST().newBlock();
					methodDeclaration1.setBody(block1);
					declarations.add(methodDeclaration1);

					List<SingleVariableDeclaration> parameters2 = new ArrayList<SingleVariableDeclaration>();
					SingleVariableDeclaration param1Method2 = createVariableDeclarationFromStrings(
							JavaViewBasedOnLayoutModifierConstants.SEEK_BAR,
							JavaViewBasedOnLayoutModifierConstants.SEEK_BAR_VARIABLE);
					parameters2.add(param1Method2);
					MethodDeclaration methodDeclaration2 = addMethodDeclaration(ModifierKeyword.PUBLIC_KEYWORD,
							JavaViewBasedOnLayoutModifierConstants.ON_START_TRACKING_TOUCH, PrimitiveType.VOID,
							parameters2);
					Block block2 = onCreateDeclaration.getAST().newBlock();
					methodDeclaration2.setBody(block2);
					declarations.add(methodDeclaration2);

					List<SingleVariableDeclaration> parameters3 = new ArrayList<SingleVariableDeclaration>();
					SingleVariableDeclaration param1Method3 = createVariableDeclarationFromStrings(
							JavaViewBasedOnLayoutModifierConstants.SEEK_BAR,
							JavaViewBasedOnLayoutModifierConstants.SEEK_BAR_VARIABLE);
					parameters3.add(param1Method3);
					MethodDeclaration methodDeclaration3 = addMethodDeclaration(ModifierKeyword.PUBLIC_KEYWORD,
							JavaViewBasedOnLayoutModifierConstants.ON_STOP_TRACKING_TOUCH, PrimitiveType.VOID,
							parameters3);
					Block block3 = onCreateDeclaration.getAST().newBlock();
					methodDeclaration3.setBody(block3);
					declarations.add(methodDeclaration3);

					addMethodInvocationToListenerHandler(node.getNodeId(),
							JavaViewBasedOnLayoutModifierConstants.SET_ON_SEEK_BAR_CHANGE_LISTENER,
							JavaViewBasedOnLayoutModifierConstants.SEEK_BAR,
							JavaViewBasedOnLayoutModifierConstants.ON_SEEK_BAR_CHANGE_LISTENER, declarations);
				}
			}
			subMonitor.worked(1);
		}
	}

}
