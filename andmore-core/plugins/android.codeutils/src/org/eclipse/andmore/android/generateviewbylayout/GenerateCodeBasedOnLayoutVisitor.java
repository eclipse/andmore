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
package org.eclipse.andmore.android.generateviewbylayout;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.andmore.android.codeutils.i18n.CodeUtilsNLS;
import org.eclipse.andmore.android.generatecode.BasicCodeVisitor;
import org.eclipse.andmore.android.generateviewbylayout.model.CodeGeneratorDataBasedOnLayout;
import org.eclipse.andmore.android.generateviewbylayout.model.LayoutNode;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

/**
 * Visitor for class method declarations to find onCreate methods inside
 * activity / fragment. It calls BodyVisitor to continue extracting information
 * about the Android code.
 */
public class GenerateCodeBasedOnLayoutVisitor extends BasicCodeVisitor {
	/*
	 * Constants
	 */
	private static final String ACTIVITY_ON_CREATE_DECLARATION = "void onCreate(android.os.Bundle)"; //$NON-NLS-1$

	private static final String FRAGMENT_ON_CREATE_DECLARATION = "android.view.View onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)"; //$NON-NLS-1$

	private static final String ACTIVITY_ON_CREATE = "onCreate"; //$NON-NLS-1$

	private static final String FRAGMENT_ON_CREATE = "onCreateView"; //$NON-NLS-1$

	private static final String ACTIVITY_ON_PAUSE_DECLARATION = "void onPause()";

	private static final String ACTIVITY_ON_RESUME_DECLARATION = "void onResume()";

	private static final String ACTIVITY_ON_PAUSE = "onPause";

	private static final String ACTIVITY_ON_RESUME = "onResume";

	private MethodDeclaration onCreateDeclaration;

	private CodeGeneratorDataBasedOnLayout.TYPE typeAssociatedToLayout;

	/**
	 * If type is fragment, there may be an inflated view name. This will be
	 * used to call findViewById inside fragments
	 */
	private String inflatedViewName;

	private final Set<String> declaredViewIds = new HashSet<String>();

	private final Set<String> savedViewIds = new HashSet<String>();

	private final Set<String> restoredViewIds = new HashSet<String>();

	private String layoutName;

	/**
	 * @return method declaration reference if there an onCreate method
	 *         declared, null if not found
	 */
	public MethodDeclaration getOnCreateDeclaration() {
		return onCreateDeclaration;
	}

	public void setOnCreateDeclaration(MethodDeclaration onCreateDeclaration) {

		this.onCreateDeclaration = onCreateDeclaration;
	}

	/**
	 * @return name of the layout being visited
	 */
	public String getLayoutName() {
		return layoutName;
	}

	public void setLayoutName(String layoutName) {
		this.layoutName = layoutName;
	}

	/**
	 * Visit method declaration, searching for instructions onCreate for
	 * activity or fragment
	 */
	@Override
	public boolean visit(MethodDeclaration node) {
		// Fill Method information
		SimpleName name = node.getName();
		if (name.getIdentifier().equals(ACTIVITY_ON_CREATE) || name.getIdentifier().equals(FRAGMENT_ON_CREATE)) {
			IMethodBinding binding = node.resolveBinding();
			if (binding != null) {
				if (binding.toString().trim().contains(ACTIVITY_ON_CREATE_DECLARATION)) {
					visitMethodBodyToIdentifyLayout(node);
				} else if (binding.toString().trim().contains(FRAGMENT_ON_CREATE_DECLARATION)) {
					if (node.getBody().statements().size() <= 1) {
						throw new IllegalArgumentException(
								CodeUtilsNLS.MethodVisitor_InvalidFormatForFragmentOnCreateView);
					} else {
						visitMethodBodyToIdentifyLayout(node);
					}
				} else {
					// for each method visit to identify views already declared
					visitToIdentifyViewsAlreadyDeclared(node);
				}
			}
		} else if (name.getIdentifier().equals(ACTIVITY_ON_PAUSE) || name.getIdentifier().equals(ACTIVITY_ON_RESUME)) {
			IMethodBinding binding = node.resolveBinding();
			if (binding != null) {
				// find declared save state
				if (binding.toString().trim().contains(ACTIVITY_ON_PAUSE_DECLARATION)) {
					findSavedViews(node);
				}
				// find declared restore state
				else if (binding.toString().trim().contains(ACTIVITY_ON_RESUME_DECLARATION)) {
					findRestoredViews(node);
				}

			}
		} else {
			// for each method visit to identify views already declared
			visitToIdentifyViewsAlreadyDeclared(node);
		}
		return super.visit(node);
	}

	private void findRestoredViews(MethodDeclaration node) {
		SaveStateVisitor visitor = new SaveStateVisitor();
		node.accept(visitor);
		restoredViewIds.addAll(visitor.getViewIds());
	}

	private void findSavedViews(MethodDeclaration node) {
		SaveStateVisitor visitor = new SaveStateVisitor();
		node.accept(visitor);
		savedViewIds.addAll(visitor.getViewIds());
	}

	/**
	 * @param node
	 */
	protected synchronized void visitToIdentifyViewsAlreadyDeclared(MethodDeclaration node) {
		Block body = node.getBody();
		if (body != null) {
			MethodBodyVisitor visitor = new MethodBodyVisitor();
			visitAndUpdateDeclaredViewsBasedOnFindViewById(body, visitor);
		}
	}

	/**
	 * Visit method body from onCreate declaration to identify layout used (it
	 * also verifies views already declared)
	 * 
	 * @param node
	 * @throws JavaModelException
	 */
	protected void visitMethodBodyToIdentifyLayout(MethodDeclaration node) {
		// Navigate through statements...
		setOnCreateDeclaration(node);
		Block body = node.getBody();
		if (body != null) {
			identifyLayout(body);
		}
	}

	/**
	 * Navigates in a Block and extract layout name, if class associated to
	 * layout is activity or fragment, and, in case of fragment only, the name
	 * of the view inflated.
	 */
	private void identifyLayout(Block body) {
		MethodBodyVisitor visitor = new MethodBodyVisitor();
		visitAndUpdateDeclaredViewsBasedOnFindViewById(body, visitor);
		setLayoutName(visitor.getLayoutName());
		typeAssociatedToLayout = visitor.getTypeAssociatedToLayout();
		setInflatedViewName(visitor.getInflatedViewName());
	}

	/**
	 * Visit method body to identify view ids already declared
	 * 
	 * @param body
	 * @param visitor
	 */
	public void visitAndUpdateDeclaredViewsBasedOnFindViewById(Block body, MethodBodyVisitor visitor) {
		body.accept(visitor);
		synchronized (declaredViewIds) {
			declaredViewIds.addAll(visitor.getDeclaredViewIds());
		}

	}

	/**
	 * Check if there is an attribute already declared with the name given.
	 * 
	 * @param node
	 * @param considerType
	 *            false, if must not consider the type in the analysis
	 * @return true if there a variable declared with the node.getNodeId()
	 *         independent on variable type, false otherwise
	 */
	public boolean checkIfAttributeAlreadyDeclared(LayoutNode node, boolean considerType) {
		boolean containFieldDeclared = false;
		if (typeDeclaration.bodyDeclarations() != null) {
			// check if attribute already declared
			for (Object bd : typeDeclaration.bodyDeclarations()) {
				if (bd instanceof FieldDeclaration) {
					FieldDeclaration fd = (FieldDeclaration) bd;
					if (fd.getParent() instanceof TypeDeclaration) {
						TypeDeclaration type = (TypeDeclaration) fd.getParent();
						if (typeDeclaration.equals(type)) {
							// only considers attributes from main class inside
							// the file
							for (Object fragment : fd.fragments()) {
								if (fragment instanceof VariableDeclarationFragment) {
									VariableDeclarationFragment frag = (VariableDeclarationFragment) fragment;
									if ((frag.getName() != null) && frag.getName().toString().equals(node.getNodeId())) {
										if (considerType) {
											if ((fd.getType() != null)
													&& !fd.getType().toString().equals(node.getNodeType())) {
												containFieldDeclared = true;
												break;
											}
										} else {
											containFieldDeclared = true;
											break;
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return containFieldDeclared;
	}

	/**
	 * @return the typeAssociatedToLayout
	 */
	public CodeGeneratorDataBasedOnLayout.TYPE getTypeAssociatedToLayout() {
		return typeAssociatedToLayout;
	}

	/**
	 * @return the inflatedViewName
	 */
	public String getInflatedViewName() {
		return inflatedViewName;
	}

	/**
	 * @param inflatedViewName
	 *            the inflatedViewName to set
	 */
	public void setInflatedViewName(String inflatedViewName) {
		this.inflatedViewName = inflatedViewName;
	}

	/**
	 * @return the list of declared layout ids (as specified in layout.xml under
	 *         android:id attribute)
	 */
	public synchronized Set<String> getDeclaredViewIds() {
		return declaredViewIds;
	}

	/**
	 * @return the list of view ids that have code to restore state (using
	 *         SharedPreferences)
	 */
	public Set<String> getRestoredViewIds() {
		return restoredViewIds;
	}

	/**
	 * @return the list of view ids that have code to save state (using
	 *         SharedPreferences)
	 */
	public Set<String> getSavedViewIds() {
		return savedViewIds;
	}

}
