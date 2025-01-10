/********************************************************************************
 * Copyright (c) 2009-2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Marcelo Marzola Bossoni (Eldorado)
 * 
 * Contributors:
 * Marcelo Marzola Bossoni (Eldorado) -  Bug [289146] - Performance and Usability Issues
 ********************************************************************************/
package org.eclipse.sequoyah.localization.editor.datatype;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.sequoyah.localization.editor.StringEditorPlugin;

/**
 * This class represents a row of the editor
 */
public class RowInfo {

	/*
	 * This row key
	 */
	private String key;

	private final List<RowInfoLeaf> children = new LinkedList<RowInfoLeaf>();

	/*
	 * This row status
	 */
	private MultiStatus rowStatus;

	/**
	 * Create a new row with a key and initial cells
	 * 
	 * @param key
	 * @param cells
	 */
	public RowInfo(String key) {
		this.key = key;
		this.rowStatus = new MultiStatus(StringEditorPlugin.PLUGIN_ID, 0, null,
				null);
	}

	/**
	 * get this row key
	 * 
	 * @return
	 */
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public void addStatus(IStatus status) {
		this.rowStatus.merge(status);
	}

	public void cleanStatus() {
		this.rowStatus = new MultiStatus(StringEditorPlugin.PLUGIN_ID, 0, null,
				null);
	}

	public MultiStatus getStatus() {
		return rowStatus;
	}

	public void addChild(RowInfoLeaf child, Integer index) {
		children.add(index, child);
		for (int i = index + 1; i < children.size(); i++) {
			children.get(i).setPosition(children.get(i).getPosition() + 1);
		}
	}

	public void removeChild(int index) {
		children.remove(index);
		for (int i = index; i < children.size(); i++) {
			children.get(i).setPosition(children.get(i).getPosition() - 1);
		}
	}

	public List<RowInfoLeaf> getChildren() {
		return children;
	}

	@Override
	public String toString() {
		return "key = " + key + "; Children count = " //$NON-NLS-1$ //$NON-NLS-2$
				+ (children != null ? children.size() : "0"); //$NON-NLS-1$
	}
}
