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
import org.eclipse.andmore.android.generatecode.JavaCodeModifier;
import org.eclipse.andmore.android.generateviewbylayout.JavaModifierBasedOnLayout;
import org.eclipse.andmore.android.generateviewbylayout.model.CheckboxNode;
import org.eclipse.andmore.android.generateviewbylayout.model.CodeGeneratorDataBasedOnLayout;
import org.eclipse.andmore.android.generateviewbylayout.model.EditTextNode;
import org.eclipse.andmore.android.generateviewbylayout.model.LayoutNode;
import org.eclipse.andmore.android.generateviewbylayout.model.RadioButtonNode;
import org.eclipse.andmore.android.generateviewbylayout.model.SeekBarNode;
import org.eclipse.andmore.android.generateviewbylayout.model.SpinnerNode;
import org.eclipse.andmore.android.generateviewbylayout.model.LayoutNode.ViewProperties;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

/**
 * Generate code to save/restore GUI state (e.g.: EditText, ComboBox, etc)
 */
public class SaveStateCodeGenerator extends AbstractLayoutCodeGenerator

{

	public static LayoutNode[] saveStateNodeTypes = new LayoutNode[] { new CheckboxNode(), new RadioButtonNode(),
			new EditTextNode(), new SpinnerNode(), new SeekBarNode() };

	/*
	 * Constants (types, methods and variable names)
	 */
	private static final String COMMIT = "commit";

	private static final String ANDROID_CONTENT_IMPORT = "android.content.*";

	private static final String MODE_PRIVATE = "MODE_PRIVATE";

	private static final String GET_PREFERENCES = "getPreferences";

	private static final String PREFERENCES = "preferences";

	private static final String EDITOR_CAPITAL_LETTER = "Editor";

	private static final String SHARED_PREFERENCES = "SharedPreferences";

	private static final String EDITOR = "editor";

	private static final String EDIT = "edit";

	private static final String TO_STRING = "toString";

	private static final String PROTECTED_VOID_ON_PAUSE = "protected void onPause()"; //$NON-NLS-1$

	private static final String PROTECTED_VOID_ON_RESUME = "protected void onResume()"; //$NON-NLS-1$

	private static final String ON_PAUSE = "onPause"; //$NON-NLS-1$

	private static final String ON_RESUME = "onResume"; //$NON-NLS-1$

	/**
	 * @param codeGeneratorData
	 * @param onCreateDeclaration
	 * @param typeDeclaration
	 */
	public SaveStateCodeGenerator(CodeGeneratorDataBasedOnLayout codeGeneratorData,
			MethodDeclaration onCreateDeclaration, TypeDeclaration typeDeclaration) {
		super(codeGeneratorData, onCreateDeclaration, typeDeclaration);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void generateCode(IProgressMonitor monitor) throws JavaModelException {
		SubMonitor subMonitor = SubMonitor.convert(monitor);
		subMonitor.beginTask(CodeUtilsNLS.SaveStateCodeGenerator_AddingCodeSaveRestoreUIState, codeGeneratorData
				.getGuiItems().size());

		boolean alreadyFoundItemToSaveState = false;
		MethodDeclaration onPauseFoundMethod = null;
		MethodDeclaration onResumeFoundMethod = null;
		SimpleName editorVarName = null;
		SimpleName preferencesVarName = null;
		for (LayoutNode node : codeGeneratorData.getGUIItems(false)) {
			if (canGenerateSaveStateCode(node) && node.getSaveState()
					&& (node.isAlreadyDeclaredInCode() || node.shouldInsertCode())) {
				if (!alreadyFoundItemToSaveState) {
					onPauseFoundMethod = insertOnPause();

					onResumeFoundMethod = insertOnResume();

					String superMethodNameOnPause = ON_PAUSE;
					insertSuperInvocation(onPauseFoundMethod, superMethodNameOnPause, null);

					String superMethodNameOnResume = ON_RESUME;
					insertSuperInvocation(onResumeFoundMethod, superMethodNameOnResume, null);

					JavaCodeModifier.IMPORT_LIST.add(ANDROID_CONTENT_IMPORT);

					preferencesVarName = getPreferenceVariable(onPauseFoundMethod);

					editorVarName = insertPreferencesEditor(onPauseFoundMethod, preferencesVarName);

					getPreferenceVariable(onResumeFoundMethod);

					// need to add method declaration and variables only once
					alreadyFoundItemToSaveState = true;
				}
				String putMethodName = node.getProperty(ViewProperties.PreferenceSetMethod);
				String getUIStateMethodName = node.getProperty(ViewProperties.ViewStateGetMethod);
				String getter = node.getProperty(ViewProperties.PreferenceGetMethod);
				String setMethodName = node.getProperty(ViewProperties.ViewStateSetMethod);

				MethodInvocation getGUIStateInvocation = null;
				if (node.getNodeType().equals(LayoutNode.LayoutNodeViewType.EditText.name())) {
					// Add code for save state:
					// savedInstanceState.putString("MyEditTextId",
					// edittext.getText());
					getGUIStateInvocation = onPauseFoundMethod.getAST().newMethodInvocation();
					String getGUIStateToStringName = TO_STRING;

					MethodInvocation editTextMethod = addMethodToRetrieveUIState(onPauseFoundMethod, node,
							getUIStateMethodName);

					SimpleName toStringName = onPauseFoundMethod.getAST().newSimpleName(getGUIStateToStringName);
					getGUIStateInvocation.setName(toStringName);
					getGUIStateInvocation.setExpression(editTextMethod);

					insertPutMethod(onPauseFoundMethod, node, putMethodName, editorVarName.getIdentifier(),
							getGUIStateInvocation);

					// Add code for restore state:
					// edittext.setText(savedInstanceState.getString("MyEditTextId"));
					MethodInvocation getBundleStateInvocation = onResumeFoundMethod.getAST().newMethodInvocation();

					SimpleName getName = onPauseFoundMethod.getAST().newSimpleName(getter);
					getBundleStateInvocation.setName(getName);
					SimpleName getExpr = onPauseFoundMethod.getAST().newSimpleName(preferencesVarName.getIdentifier());
					getBundleStateInvocation.setExpression(getExpr);
					StringLiteral id = onPauseFoundMethod.getAST().newStringLiteral();
					id.setLiteralValue(node.getNodeId());
					getBundleStateInvocation.arguments().add(id);

					StringLiteral defaultValue = onPauseFoundMethod.getAST().newStringLiteral();
					defaultValue.setLiteralValue("");
					getBundleStateInvocation.arguments().add(defaultValue);

					insertSetMethod(onResumeFoundMethod, node, setMethodName, getBundleStateInvocation);
				} else {
					// Add code to save state:
					// savedInstanceState.putBoolean("MyCheckboxId",
					// checkbox.getEnabled());
					// Add code to restore state:
					// checkbox.setEnabled(savedInstanceState.getBoolean("MyCheckbox"));

					insertSaveRestoreCode(onPauseFoundMethod, onResumeFoundMethod, editorVarName.getIdentifier(),
							preferencesVarName.getIdentifier(), node, putMethodName, setMethodName, getter,
							getUIStateMethodName);
				}

			}
			subMonitor.worked(1);
		}

		if (alreadyFoundItemToSaveState) {
			// Add code: editor.commit();
			invokeMethod(onPauseFoundMethod, EDITOR, COMMIT);
		}
	}

	/**
	 * @param node
	 *            layout node being analyzed
	 * @return true if it is in the array of saveStateNodeTypes (the current
	 *         supported view items to save/restore state), false otherwise
	 */
	public static boolean canGenerateSaveStateCode(LayoutNode node) {
		boolean canSaveCode = false;

		if ((node != null) && (node.getNodeType() != null)) {
			int i = 0;
			while (!canSaveCode && (i < saveStateNodeTypes.length)) {
				if (node.getNodeType().equals(saveStateNodeTypes[i].getNodeType())) {
					canSaveCode = true;
				}
				i++;
			}
		}

		return canSaveCode;
	}

	/**
	 * Inserts SharedPreferences.Editor statement into onPause method
	 * 
	 * @param onPauseFoundMethod
	 * @param preferencesVarName
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public SimpleName insertPreferencesEditor(MethodDeclaration onPauseFoundMethod, SimpleName preferencesVarName) {
		SimpleName editorVarName = onPauseFoundMethod.getAST().newSimpleName(EDITOR);
		boolean alreadyAddedVariable = false;
		if (onPauseFoundMethod.getBody() != null) {
			outer: for (Object s : onPauseFoundMethod.getBody().statements()) {
				if (s instanceof VariableDeclarationStatement) {
					VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement) s;
					if (variableDeclarationStatement.getType().toString()
							.equals(SHARED_PREFERENCES + "." + EDITOR_CAPITAL_LETTER)) {
						for (Object f : variableDeclarationStatement.fragments()) {
							VariableDeclarationFragment frag = (VariableDeclarationFragment) f;
							if (frag.getName().toString().equals(EDITOR)) {
								alreadyAddedVariable = true;
								break outer;
							}
						}
					}
				}
			}
		}
		if (!alreadyAddedVariable) {
			VariableDeclarationFragment getEditorPreference = onPauseFoundMethod.getAST()
					.newVariableDeclarationFragment();
			MethodInvocation getEditorInvoke = onPauseFoundMethod.getAST().newMethodInvocation();
			SimpleName editInvokeName = onPauseFoundMethod.getAST().newSimpleName(EDIT);
			getEditorInvoke.setName(editInvokeName);
			SimpleName editInvokeVariable = onPauseFoundMethod.getAST().newSimpleName(
					preferencesVarName.getIdentifier());
			getEditorInvoke.setExpression(editInvokeVariable);
			getEditorPreference.setInitializer(getEditorInvoke);

			getEditorPreference.setName(editorVarName);
			VariableDeclarationStatement getEditorVariableDeclarationStatement = onPauseFoundMethod.getAST()
					.newVariableDeclarationStatement(getEditorPreference);
			SimpleName sharedPreferencesName1 = onPauseFoundMethod.getAST().newSimpleName(SHARED_PREFERENCES);
			SimpleType type = onPauseFoundMethod.getAST().newSimpleType(sharedPreferencesName1);
			SimpleName sharedPreferencesName2 = onPauseFoundMethod.getAST().newSimpleName(EDITOR_CAPITAL_LETTER);
			QualifiedType editorPreferencesType = onPauseFoundMethod.getAST().newQualifiedType(type,
					sharedPreferencesName2);
			getEditorVariableDeclarationStatement.setType(editorPreferencesType);
			onPauseFoundMethod.getBody().statements().add(getEditorVariableDeclarationStatement);
		}
		return editorVarName;
	}

	/**
	 * Creates onResume method if it does not exist yet
	 * 
	 * @return {@link MethodDeclaration} for void onResume() method
	 */
	@SuppressWarnings("unchecked")
	public MethodDeclaration insertOnResume() {
		// Add protected void onResume()
		ModifierKeyword keyword = ModifierKeyword.PUBLIC_KEYWORD;
		if (getCodeGeneratorData().getAssociatedType().equals(CodeGeneratorDataBasedOnLayout.TYPE.FRAGMENT)) {
			keyword = ModifierKeyword.PUBLIC_KEYWORD;
		} else if (getCodeGeneratorData().getAssociatedType().equals(CodeGeneratorDataBasedOnLayout.TYPE.ACTIVITY)) {
			keyword = ModifierKeyword.PROTECTED_KEYWORD;
		}
		MethodDeclaration onResumeMethodDeclaration = addMethodDeclaration(keyword, ON_RESUME, PrimitiveType.VOID,
				new ArrayList<SingleVariableDeclaration>());
		Block onResumeMethodBlock = onCreateDeclaration.getAST().newBlock();
		MethodDeclaration onResumeFoundMethod = isMethodAlreadyDeclared(onResumeMethodDeclaration,
				PROTECTED_VOID_ON_RESUME);
		if (onResumeFoundMethod == null) {
			// add method onRestore if it does not exist yet
			onResumeMethodDeclaration.setBody(onResumeMethodBlock);
			typeDeclaration.bodyDeclarations().add(onResumeMethodDeclaration);
			onResumeFoundMethod = onResumeMethodDeclaration;
		}
		return onResumeFoundMethod;
	}

	/**
	 * Creates onPause method if it does not exist yet
	 * 
	 * @return {@link MethodDeclaration} for void onPause() method
	 */
	@SuppressWarnings("unchecked")
	public MethodDeclaration insertOnPause() {
		// Add protected void onPause()
		ModifierKeyword keyword = ModifierKeyword.PUBLIC_KEYWORD;
		if (getCodeGeneratorData().getAssociatedType().equals(CodeGeneratorDataBasedOnLayout.TYPE.FRAGMENT)) {
			keyword = ModifierKeyword.PUBLIC_KEYWORD;
		} else if (getCodeGeneratorData().getAssociatedType().equals(CodeGeneratorDataBasedOnLayout.TYPE.ACTIVITY)) {
			keyword = ModifierKeyword.PROTECTED_KEYWORD;
		}
		MethodDeclaration onPauseMethodDeclaration = addMethodDeclaration(keyword, ON_PAUSE, PrimitiveType.VOID,
				new ArrayList<SingleVariableDeclaration>());
		Block onPauseMethodBlock = onCreateDeclaration.getAST().newBlock();
		MethodDeclaration onPauseFoundMethod = isMethodAlreadyDeclared(onPauseMethodDeclaration,
				PROTECTED_VOID_ON_PAUSE);
		if (onPauseFoundMethod == null) {
			// add method onPause if it does not exist yet
			onPauseMethodDeclaration.setBody(onPauseMethodBlock);
			typeDeclaration.bodyDeclarations().add(onPauseMethodDeclaration);
			onPauseFoundMethod = onPauseMethodDeclaration;
		}
		return onPauseFoundMethod;
	}

	/**
	 * @param method
	 * @return {@link SimpleName} for the variable with SharedPreferences type
	 *         inside the method body
	 */
	@SuppressWarnings("unchecked")
	public SimpleName getPreferenceVariable(MethodDeclaration method) {
		SimpleName preferencesVarName = method.getAST().newSimpleName(PREFERENCES);
		boolean alreadyAddedVariable = false;
		if (method.getBody() != null) {
			outer: for (Object s : method.getBody().statements()) {
				if (s instanceof VariableDeclarationStatement) {
					VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement) s;
					if (variableDeclarationStatement.getType().toString().equals(SHARED_PREFERENCES)) {
						for (Object f : variableDeclarationStatement.fragments()) {
							VariableDeclarationFragment frag = (VariableDeclarationFragment) f;
							if (frag.getName().toString().equals(PREFERENCES)) {
								alreadyAddedVariable = true;
								break outer;
							}
						}
					}
				}
			}
		}
		if (!alreadyAddedVariable) {
			VariableDeclarationFragment getPreferencefragment = method.getAST().newVariableDeclarationFragment();
			MethodInvocation invoke = method.getAST().newMethodInvocation();
			SimpleName invokeName = method.getAST().newSimpleName(GET_PREFERENCES);
			invoke.setName(invokeName);
			if (getCodeGeneratorData().getAssociatedType().equals(CodeGeneratorDataBasedOnLayout.TYPE.ACTIVITY)) {
				SimpleName invokeMode = method.getAST().newSimpleName(MODE_PRIVATE);
				invoke.arguments().add(invokeMode);
			} else if (getCodeGeneratorData().getAssociatedType().equals(CodeGeneratorDataBasedOnLayout.TYPE.FRAGMENT)) {
				SimpleName invokeMode = method.getAST().newSimpleName(MODE_PRIVATE);
				SimpleName activityRef = method.getAST().newSimpleName("Activity");
				QualifiedName qName = method.getAST().newQualifiedName(activityRef, invokeMode);

				MethodInvocation activityInvoke = method.getAST().newMethodInvocation();
				SimpleName activityMethodName = method.getAST().newSimpleName("getActivity");
				activityInvoke.setName(activityMethodName);
				invoke.setExpression(activityInvoke);

				invoke.arguments().add(qName);
			}

			getPreferencefragment.setInitializer(invoke);
			getPreferencefragment.setName(preferencesVarName);
			VariableDeclarationStatement variableDeclarationStatement = method.getAST()
					.newVariableDeclarationStatement(getPreferencefragment);
			SimpleName sharedPreferencesName = method.getAST().newSimpleName(SHARED_PREFERENCES);
			SimpleType sharedPreferencesType = method.getAST().newSimpleType(sharedPreferencesName);
			variableDeclarationStatement.setType(sharedPreferencesType);
			method.getBody().statements().add(variableDeclarationStatement);
		}
		return preferencesVarName;
	}

	/**
	 * Add code to save state: $bundleNameOnSaveMethod.$putMethodName("$nodeId",
	 * $nodeId.$getUIStateMethodName()); Add code to restore state:
	 * $nodeId.$setMethodName
	 * ($bundleNameOnRestoreMethod.$getBundleState("$nodeId"));
	 */
	@SuppressWarnings("unchecked")
	public void insertSaveRestoreCode(MethodDeclaration onSaveInstanceStateFoundMethod,
			MethodDeclaration onRestoreInstanceFoundMethod, String bundleNameOnSaveMethod,
			String bundleNameOnRestoreMethod, LayoutNode node, String putMethodName, String setMethodName,
			String getBundleState, String getUIStateMethodName) {
		MethodInvocation getGUIStateInvocation;
		getGUIStateInvocation = addMethodToRetrieveUIState(onSaveInstanceStateFoundMethod, node, getUIStateMethodName);

		insertPutMethod(onSaveInstanceStateFoundMethod, node, putMethodName, bundleNameOnSaveMethod,
				getGUIStateInvocation);

		MethodInvocation getBundleStateInvocation = onRestoreInstanceFoundMethod.getAST().newMethodInvocation();

		SimpleName getName = onSaveInstanceStateFoundMethod.getAST().newSimpleName(getBundleState);
		getBundleStateInvocation.setName(getName);
		SimpleName getExpr = onSaveInstanceStateFoundMethod.getAST().newSimpleName(bundleNameOnRestoreMethod);
		getBundleStateInvocation.setExpression(getExpr);
		StringLiteral id = onSaveInstanceStateFoundMethod.getAST().newStringLiteral();
		id.setLiteralValue(node.getNodeId());
		getBundleStateInvocation.arguments().add(id);

		String stateType = node.getProperty(ViewProperties.ViewStateValueType);

		if (stateType != null) {
			if (stateType.equals(Integer.class.toString())) {
				NumberLiteral defaultValue = onSaveInstanceStateFoundMethod.getAST().newNumberLiteral();
				getBundleStateInvocation.arguments().add(defaultValue);
			} else if (stateType.equals(Boolean.class.toString())) {
				BooleanLiteral defaultValue = onSaveInstanceStateFoundMethod.getAST().newBooleanLiteral(false);
				getBundleStateInvocation.arguments().add(defaultValue);
			}
		}

		insertSetMethod(onRestoreInstanceFoundMethod, node, setMethodName, getBundleStateInvocation);
	}

	/**
	 * Insert method in the format $nodeId.$getUIStateMethodName()
	 * 
	 * @param onSaveInstanceStateFoundMethod
	 * @param node
	 * @param getUIStateMethodName
	 * @return
	 */
	public MethodInvocation addMethodToRetrieveUIState(MethodDeclaration onSaveInstanceStateFoundMethod,
			LayoutNode node, String getUIStateMethodName) {
		MethodInvocation retrieveUIStateMethod = onSaveInstanceStateFoundMethod.getAST().newMethodInvocation();
		SimpleName guiId = onSaveInstanceStateFoundMethod.getAST().newSimpleName(node.getNodeId());
		retrieveUIStateMethod.setExpression(guiId);
		SimpleName getText = onSaveInstanceStateFoundMethod.getAST().newSimpleName(getUIStateMethodName);
		retrieveUIStateMethod.setName(getText);
		return retrieveUIStateMethod;
	}

	/**
	 * Add method in the format savedInstanceState.putXXXX($nodeid,
	 * $getGUIStateInvocation);
	 * 
	 * @param onSaveInstanceStateFoundMethod
	 * @param node
	 * @param methodName
	 * @param bundleName
	 * @param getGUIStateInvocation
	 */
	@SuppressWarnings("unchecked")
	public void insertPutMethod(MethodDeclaration onSaveInstanceStateFoundMethod, LayoutNode node, String methodName,
			String bundleName, MethodInvocation getGUIStateInvocation) {
		MethodInvocation putMethod = onSaveInstanceStateFoundMethod.getAST().newMethodInvocation();
		SimpleName putMethodName = onSaveInstanceStateFoundMethod.getAST().newSimpleName(methodName);
		putMethod.setName(putMethodName);
		SimpleName bundle = onSaveInstanceStateFoundMethod.getAST().newSimpleName(bundleName);
		putMethod.setExpression(bundle);
		StringLiteral id = onSaveInstanceStateFoundMethod.getAST().newStringLiteral();
		id.setLiteralValue(node.getNodeId());
		putMethod.arguments().add(id);

		putMethod.arguments().add(getGUIStateInvocation);

		ExpressionStatement exprSt = onSaveInstanceStateFoundMethod.getAST().newExpressionStatement(putMethod);
		int commitPosition = findMethodInvocation(onSaveInstanceStateFoundMethod.getBody().statements(), bundle,
				"commit");
		onSaveInstanceStateFoundMethod.getBody().statements().add(commitPosition, exprSt);
	}

	private int findMethodInvocation(List<?> statements, final SimpleName bundle, final String methodName) {
		int position = -1;
		int i = 0;
		while ((i < statements.size()) && (position == -1)) {
			Statement statement = (Statement) statements.get(i);
			if (statement instanceof ExpressionStatement) {
				ExpressionStatement expressionSt = (ExpressionStatement) statement;
				Expression expression = expressionSt.getExpression();
				if (expression instanceof MethodInvocation) {
					MethodInvocation method = (MethodInvocation) expression;
					if (method.getName().getIdentifier().equals(methodName)
							&& (method.getExpression() instanceof SimpleName)) {
						SimpleName name = (SimpleName) method.getExpression();
						if (name.getIdentifier().equals(bundle.getIdentifier())) {
							position = i;
						}

					}

				}

			}
			i++;
		}
		if (position == -1) {
			position = i;
		}

		return position;
	}

	/**
	 * Add method in the format $nodeId.setXXXXX($getBundleStateInvocation);
	 * 
	 * @param onRestoreInstanceStateFoundMethod
	 * @param node
	 * @param methodName
	 * @param bundleName
	 * @param getBundleStateInvocation
	 */
	@SuppressWarnings("unchecked")
	public void insertSetMethod(MethodDeclaration onRestoreInstanceStateFoundMethod, LayoutNode node,
			String methodName, MethodInvocation getBundleStateInvocation) {
		MethodInvocation setMethod = onRestoreInstanceStateFoundMethod.getAST().newMethodInvocation();
		SimpleName setMethodName = onRestoreInstanceStateFoundMethod.getAST().newSimpleName(methodName);
		setMethod.setName(setMethodName);
		SimpleName id = onRestoreInstanceStateFoundMethod.getAST().newSimpleName(node.getNodeId());
		setMethod.setExpression(id);

		// add in the end of the method
		setMethod.arguments().add(getBundleStateInvocation);

		ExpressionStatement exprSt = onRestoreInstanceStateFoundMethod.getAST().newExpressionStatement(setMethod);
		onRestoreInstanceStateFoundMethod.getBody().statements().add(exprSt);
	}
}
