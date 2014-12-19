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
import org.eclipse.andmore.android.common.IAndroidConstants;
import org.eclipse.andmore.android.common.utilities.AndroidUtils;
import org.eclipse.andmore.android.generatecode.JDTUtils;
import org.eclipse.andmore.android.generateviewbylayout.JavaViewBasedOnLayoutModifierConstants;
import org.eclipse.andmore.android.generateviewbylayout.model.CodeGeneratorDataBasedOnLayout;
import org.eclipse.andmore.android.generateviewbylayout.model.LayoutNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
 * Responsible to create findViewById statements
 */
public class FindViewByIdCodeGenerator extends AbstractLayoutCodeGenerator {

	protected IFile javaFile;

	/**
	 * @param codeGeneratorData
	 * @param onCreateDeclaration
	 * @param typeDeclaration
	 * @param javaFile
	 *            file used to get API version number (it is necessary to
	 *            differentiate between Activity or Fragment in the findViewById
	 *            method invocation)
	 */
	public FindViewByIdCodeGenerator(CodeGeneratorDataBasedOnLayout codeGeneratorData,
			MethodDeclaration onCreateDeclaration, TypeDeclaration typeDeclaration, IFile javaFile) {
		super(codeGeneratorData, onCreateDeclaration, typeDeclaration);
		this.javaFile = javaFile;
	}

	@Override
	public void generateCode(IProgressMonitor monitor) throws JavaModelException {
		addFindByIdStatement(monitor);
	}

	/**
	 * Field Assigment to find item inside layout xml
	 * 
	 * <br>
	 * GENERATED_CODE_FORMAT: <br>
	 * $GUI_ID = $GUI_TYPE findViewById(R.id.$GUI_ID);
	 */
	private void addFindByIdStatement(IProgressMonitor monitor) throws JavaModelException {
		SubMonitor subMonitor = SubMonitor.convert(monitor);
		subMonitor.beginTask(CodeUtilsNLS.JavaViewBasedOnLayoutModifier_AddingFindingViewById, codeGeneratorData
				.getGuiItems().size());
		/*
		 * AST To be written ExpressionStatement Assignment leftHand
		 * SimpleName(button1) Assignment.Operator = rightHand CastExpression
		 * expression MethodInvocation arguments = QualifiedName(R.id.button1)
		 * SimpleName = findViewById type SimpleType (Button)
		 */

		for (LayoutNode node : codeGeneratorData.getGuiItems()) {
			if ((node.getNodeId() != null) && node.shouldInsertCode()) {
				boolean containsFindViewByIdDeclaration = checkIfFindViewByIdAlreadyDeclared(node);
				if (!containsFindViewByIdDeclaration) {
					// avoid to call method twice
					if (!node.isFragmentPlaceholder()) {
						// not fragment
						addAssignmentStatement(node, null, JavaViewBasedOnLayoutModifierConstants.FIND_VIEW_BY_ID);
					} else {
						// is fragment placeholder
						MethodInvocation optionalInvocation = onCreateDeclaration.getAST().newMethodInvocation();
						String methodName = null;
						try {
							if ((AndroidUtils.getApiVersionNumberForProject(javaFile.getProject()) < 11)
									&& JDTUtils.isCompatibilityFragmentActivitySubclass(javaFile)) {
								// if compatibility package (before 3.0)
								methodName = JavaViewBasedOnLayoutModifierConstants.GET_SUPPORT_FRAGMENT_MANAGER;
							} else if ((AndroidUtils.getApiVersionNumberForProject(javaFile.getProject()) < IAndroidConstants.API_LEVEL_FOR_PLATFORM_VERSION_3_0_0)
									&& !JDTUtils.isCompatibilityFragmentActivitySubclass(javaFile)) {
								// if compatibility package (before 3.0)
								// but user is trying to use compatibility
								// package with Activity (instead of
								// FragmentActivity)
								throw new JavaModelException(
										new IllegalArgumentException(
												CodeUtilsNLS.FindViewByIdCodeGenerator_CompatibilityModeClassNeedToExtendFragmentActivityError),
										IStatus.ERROR);
							} else if ((AndroidUtils.getApiVersionNumberForProject(javaFile.getProject()) >= IAndroidConstants.API_LEVEL_FOR_PLATFORM_VERSION_3_0_0)) {
								// if after 3.0 (API level >=11)
								methodName = JavaViewBasedOnLayoutModifierConstants.GET_FRAGMENT_MANAGER;
							}
							if (methodName != null) {
								SimpleName managerMethod = onCreateDeclaration.getAST().newSimpleName(methodName);
								optionalInvocation.setName(managerMethod);
								addAssignmentStatement(node, optionalInvocation,
										JavaViewBasedOnLayoutModifierConstants.FIND_FRAGMENT_BY_ID);
							}
						} catch (Exception e) {
							throw new JavaModelException(e, IStatus.ERROR);
						}

					}
				}
			}
			subMonitor.worked(1);
		}
	}
}
