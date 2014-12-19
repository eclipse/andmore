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
package org.eclipse.andmore.android.certmanager.ui.model;

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;

public class EntryDummyNode extends AbstractTreeNode implements ITreeNode {

	public static final String DUMMY_NODE_ID = "DUMMY_NODE";

	String alias = DUMMY_NODE_ID;

	public EntryDummyNode(ITreeNode keyStoreModel) {
		setParent(keyStoreModel);
		alias = DUMMY_NODE_ID;
	}

	@Override
	public void refresh() {
		// default implementation does nothing.
	}

	@Override
	public String getId() {
		return DUMMY_NODE_ID;
	}

	@Override
	public String getName() {
		return "No Keys found";
	}

	@Override
	public ImageDescriptor getIcon() {
		return null;
	}

	@Override
	public boolean isLeaf() {
		return true;
	}

	@Override
	public List<ITreeNode> getChildren() {
		return Collections.emptyList();
	}
}
