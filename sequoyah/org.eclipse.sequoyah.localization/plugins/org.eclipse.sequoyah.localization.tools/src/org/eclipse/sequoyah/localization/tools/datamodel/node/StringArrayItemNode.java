/********************************************************************************
 * Copyright (c) 2010 Motorola Mobility, Inc.
 * All rights reserved. This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * Julia Martinez Perdigueiro (Eldorado)
 * 
 * Contributors:
 * Name (Company) - [Bug] - Description
 ********************************************************************************/

package org.eclipse.sequoyah.localization.tools.datamodel.node;

public class StringArrayItemNode extends StringNode {

	private StringArrayNode parent;

	private int position = 0;

	public StringArrayItemNode(String value, StringArrayNode parent,
			int position) {
		super(parent.getKey(), value);
		this.parent = parent;
		this.position = position;
	}

	public StringArrayNode getParent() {
		return parent;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	@Override
	public String toString() {
		return position + ": " + value; //$NON-NLS-1$
	}
}
