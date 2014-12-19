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
package org.eclipse.andmore.android.generateviewbylayout.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.andmore.android.common.exception.AndroidException;
import org.eclipse.andmore.android.generatecode.AbstractCodeGeneratorData;
import org.eclipse.andmore.android.generatecode.BasicCodeVisitor;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;

/**
 * Model representing the code generator data needed to generate code for layout
 * You MUST call init before using the object.
 */
public class CodeGeneratorDataBasedOnLayout extends AbstractCodeGeneratorData {

	private LayoutFile layoutFile;

	private JavaLayoutData javaLayoutData;

	/**
	 * Creates {@link LayoutFile} representation for the layout xml
	 * 
	 * @param layoutName
	 *            name of the layout (to appear on dialog to generate code)
	 * @param layout
	 *            full file path to layout xml
	 * @throws AndroidException
	 *             if an error occurs parsing layout xml
	 */
	public void init(String layoutName, File layout) throws AndroidException {
		layoutFile = new LayoutFile(layoutName, layout);
	}

	private void refreshDeclared() {
		for (LayoutNode node : layoutFile.getNodes()) {
			node.setAlreadyDeclaredInCode(getJavaLayoutData().getDeclaredViewIdsOnCode().contains(node.getNodeId()));
			node.setAlreadySaved(getJavaLayoutData().getSavedViewIds().contains(node.getNodeId()));
			node.setAlreadyRestored(getJavaLayoutData().getRestoredViewIds().contains(node.getNodeId()));
		}
	}

	/**
	 * Get GUI items (not layouts or fragment placeholders) that are not
	 * declared in the code yet
	 * 
	 * @return list of GUI items from layout file (only ones with id set).
	 */
	public List<LayoutNode> getGuiItems() {
		List<LayoutNode> guiItems = getGUIItems(true);
		return guiItems;
	}

	/**
	 * Get the list of layout nodes available into layout xml
	 * 
	 * @param doNotshowAlreadyDeclared
	 *            if true, remove the items already declared, if false include
	 *            them in the result list
	 * @return list of layout nodes
	 */
	public List<LayoutNode> getGUIItems(boolean doNotshowAlreadyDeclared) {
		List<LayoutNode> guiItems = new ArrayList<LayoutNode>();

		for (LayoutNode node : layoutFile.getNodes()) {
			if (node.isGUIItem() && (node.getNodeId() != null)) {
				if (doNotshowAlreadyDeclared) {
					if (!node.isAlreadyDeclaredInCode()) {
						guiItems.add(node);
					} else {
						// do not inserted already declared item
						node.setInsertCode(false);
					}
				} else {
					guiItems.add(node);
				}
			}
		}
		return guiItems;
	}

	/**
	 * Get GUI items (not layouts) that are not declared in the code yet for the
	 * dialog UI.
	 * 
	 * @return list of GUI items from layout file (with or without id set).
	 */
	public List<LayoutNode> getGUIItemsForUI() {
		List<LayoutNode> guiItems = new ArrayList<LayoutNode>();

		for (LayoutNode node : layoutFile.getNodes()) {
			if (node.isGUIItem()) {
				if (!node.isAlreadyDeclaredInCode()) {
					guiItems.add(node);
				} else {
					// do not inserted already declared item
					node.setInsertCode(false);
				}
			}
		}
		return guiItems;
	}

	/**
	 * Get fragments (not layouts or GUI items) that are not declared in the
	 * code yet
	 * 
	 * @return
	 */
	public List<LayoutNode> getFragments() {
		List<LayoutNode> fragmentItems = getFragments(true);
		return fragmentItems;
	}

	private List<LayoutNode> getFragments(boolean doNotshowAlreadyDeclared) {
		List<LayoutNode> fragmentItems = new ArrayList<LayoutNode>();
		for (LayoutNode node : layoutFile.getNodes()) {
			if (node.isFragmentPlaceholder() && (node.getNodeId() != null)) {
				if (doNotshowAlreadyDeclared) {
					if (!node.isAlreadyDeclaredInCode()) {
						fragmentItems.add(node);
					} else {
						// do not inserted already declared item
						node.setInsertCode(false);
					}
				} else {
					fragmentItems.add(node);
				}
			}
		}
		return fragmentItems;
	}

	/**
	 * Get layout items (not GUI items or fragment placeholders) that are not
	 * declared in the code yet
	 * 
	 * @return
	 */
	public List<LayoutNode> getLayoutItems() {
		List<LayoutNode> layoutItems = getLayoutItems(true);
		return layoutItems;
	}

	private List<LayoutNode> getLayoutItems(boolean doNotshowAlreadyDeclared) {
		List<LayoutNode> layoutItems = new ArrayList<LayoutNode>();
		for (LayoutNode node : layoutFile.getNodes()) {
			if (node.isLayout() && (node.getNodeId() != null)) {
				if (doNotshowAlreadyDeclared) {
					if (!node.isAlreadyDeclaredInCode()) {
						layoutItems.add(node);
					} else {
						// do not inserted already declared item
						node.setInsertCode(false);
					}
				} else {
					layoutItems.add(node);
				}
			}
		}
		return layoutItems;
	}

	/**
	 * @return the javaLayoutData
	 */
	public JavaLayoutData getJavaLayoutData() {
		return javaLayoutData;
	}

	/**
	 * @param javaLayoutData
	 *            the javaLayoutData to set
	 */
	public void setJavaLayoutData(JavaLayoutData javaLayoutData) {
		this.javaLayoutData = javaLayoutData;
		refreshDeclared();
	}

	public LayoutFile getLayoutFile() {
		return layoutFile;
	}

	@Override
	public IResource getResource() {
		return getJavaLayoutData().getCompUnitAstNode().getJavaElement().getResource();
	}

	@Override
	public ICompilationUnit getICompilationUnit() {
		return getJavaLayoutData().getCompUnit();
	}

	@Override
	public CompilationUnit getCompilationUnit() {
		return getJavaLayoutData().getCompUnitAstNode();
	}

	@Override
	public BasicCodeVisitor getAbstractCodeVisitor() {
		return getJavaLayoutData().getVisitor();
	}
}
