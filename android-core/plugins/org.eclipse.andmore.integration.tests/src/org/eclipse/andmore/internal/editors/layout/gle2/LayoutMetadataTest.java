/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.andmore.internal.editors.layout.gle2;

import static org.junit.Assert.*;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ID_PREFIX;
import static com.android.SdkConstants.NEW_ID_PREFIX;
import static com.android.SdkConstants.TOOLS_PREFIX;
import static com.android.SdkConstants.TOOLS_URI;

import org.eclipse.andmore.common.layout.BaseLayoutRule;
import org.eclipse.andmore.common.layout.BaseViewRule;
import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.AndroidXmlEditor;
import org.eclipse.andmore.internal.editors.layout.gle2.LayoutMetadata;
import org.eclipse.andmore.internal.editors.layout.refactoring.AdtProjectTest;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;

import com.android.utils.Pair;
import com.android.utils.XmlUtils;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 */
@SuppressWarnings({ "restriction", "javadoc", "deprecation" })
// XML DOM model
@Ignore
public class LayoutMetadataTest extends AdtProjectTest {

	@Test
	public void testMetadata1() throws Exception {
		Pair<IDocument, UiElementNode> pair = getNode("metadata.xml", "listView1");
		UiElementNode uiNode = pair.getSecond();
		Node node = uiNode.getXmlNode();

		assertNull(LayoutMetadata.getProperty(node, "foo"));

		Element element = (Element) node;
		String prefix = XmlUtils.lookupNamespacePrefix(element, TOOLS_URI, null, false);
		if (prefix == null) {
			// Add in new prefix...
			prefix = XmlUtils.lookupNamespacePrefix(element, TOOLS_URI, TOOLS_PREFIX, true);
		}
		element.setAttribute(prefix + ':' + "foo", "bar");
	}

	// ==== Test utilities ====

	private static String getText(IDocument document, Node node) throws Exception {
		IndexedRegion region = (IndexedRegion) node;
		// This often returns the wrong value:
		// int length = region.getLength();
		int length = region.getEndOffset() - region.getStartOffset();
		return document.get(region.getStartOffset(), length);
	}

	private Pair<IDocument, UiElementNode> getNode(String filename, String targetId) throws Exception,
			PartInitException {
		IFile file = getLayoutFile(getProject(), filename);
		AdtPlugin.openFile(file, null);
		IEditorPart newEditor = AdtUtils.getActiveEditor();
		assertTrue(newEditor instanceof AndroidXmlEditor);
		AndroidXmlEditor xmlEditor = (AndroidXmlEditor) newEditor;
		IStructuredDocument document = xmlEditor.getStructuredDocument();
		UiElementNode root = xmlEditor.getUiRootNode();
		assertNotNull(root);
		UiElementNode node = findById(root, targetId);
		assertNotNull(node);
		Pair<IDocument, UiElementNode> pair = Pair.<IDocument, UiElementNode> of(document, node);
		return pair;
	}

	private static UiElementNode findById(UiElementNode node, String targetId) {
		assertFalse(targetId.startsWith(NEW_ID_PREFIX));
		assertFalse(targetId.startsWith(ID_PREFIX));

		String id = node.getAttributeValue(ATTR_ID);
		if (id != null && targetId.equals(BaseViewRule.stripIdPrefix(id))) {
			return node;
		}

		for (UiElementNode child : node.getUiChildren()) {
			UiElementNode result = findById(child, targetId);
			if (result != null) {
				return result;
			}
		}

		return null;
	}
}
