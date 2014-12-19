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
package org.eclipse.andmore.internal.editors.layout.refactoring;

import static com.android.SdkConstants.ANDROID_WIDGET_PREFIX;
import static com.android.SdkConstants.DOT_XML;

import com.android.ide.common.rendering.api.ViewInfo;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.layout.gle2.CanvasViewInfo;
import org.eclipse.andmore.internal.editors.layout.gle2.DomUtilities;
import org.eclipse.andmore.internal.editors.layout.refactoring.VisualRefactoring;
import org.eclipse.andmore.internal.editors.layout.uimodel.UiViewElementNode;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.w3c.dom.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("restriction")
public class RefactoringTest extends AdtProjectTest {

	protected boolean autoFormat() {
		return true;
	}

	@Override
	protected void setUp() throws Exception {

		// Ensure that the defaults are initialized so for example formatting
		// options are
		// initialized properly
		IPreferenceStore store = AdtPlugin.getDefault().getPreferenceStore();
		AdtPrefs.init(store);
		AdtPrefs prefs = AdtPrefs.getPrefs();
		prefs.initializeStoreWithDefaults(store);

		store.setValue(AdtPrefs.PREFS_FORMAT_GUI_XML, autoFormat());

		prefs.loadValues(null);

		super.setUp();
	}

	protected static Element findElementById(Element root, String id) {
		if (id.equals(VisualRefactoring.getId(root))) {
			return root;
		}

		for (Element child : DomUtilities.getChildren(root)) {
			Element result = findElementById(child, id);
			if (result != null) {
				return result;
			}
		}

		return null;
	}

	protected static List<Element> getElements(Element root, String... ids) {
		List<Element> selectedElements = new ArrayList<Element>();
		for (String id : ids) {
			Element element = findElementById(root, id);
			assertNotNull(element);
			selectedElements.add(element);
		}
		return selectedElements;
	}

	protected void checkEdits(String basename, List<Change> changes) throws BadLocationException, IOException {
		IDocument document = new Document();

		String xml = readTestFile(basename, false);
		if (xml == null) { // New file
			xml = ""; //$NON-NLS-1$
		}
		document.set(xml);

		for (Change change : changes) {
			if (change instanceof TextFileChange) {
				TextFileChange tf = (TextFileChange) change;
				TextEdit edit = tf.getEdit();
				IFile file = tf.getFile();
				String contents = AdtPlugin.readFile(file);
				assertEquals(contents, xml);
				if (edit instanceof MultiTextEdit) {
					MultiTextEdit edits = (MultiTextEdit) edit;
					edits.apply(document);
				} else {
					edit.apply(document);
				}
			} else {
				System.out.println("Ignoring non-textfilechange in refactoring result");
			}
		}

		String actual = document.get();

		// Ensure that the document is still valid to make sure the edits don't
		// mangle it:
		org.w3c.dom.Document doc = DomUtilities.parseDocument(actual, true);
		assertNotNull(actual, doc);

		assertEqualsGolden(basename, actual);
	}

	protected void checkEdits(List<Change> changes, Map<IPath, String> fileToGoldenName) throws BadLocationException,
			IOException {
		checkEdits(changes, fileToGoldenName, false);
	}

	protected void checkEdits(List<Change> changes, Map<IPath, String> fileToGoldenName, boolean createDiffs)
			throws BadLocationException, IOException {
		for (Change change : changes) {
			if (change instanceof TextFileChange) {
				TextFileChange tf = (TextFileChange) change;
				IFile file = tf.getFile();
				assertNotNull(file);
				IPath path = file.getProjectRelativePath();
				String goldenName = fileToGoldenName.get(path);
				assertNotNull("Not found: " + path.toString(), goldenName);

				String xml = readTestFile(goldenName, false);
				if (xml == null) { // New file
					xml = ""; //$NON-NLS-1$
				}
				IDocument document = new Document();
				document.set(xml);

				String before = document.get();

				TextEdit edit = tf.getEdit();
				if (edit instanceof MultiTextEdit) {
					MultiTextEdit edits = (MultiTextEdit) edit;
					edits.apply(document);
				} else {
					edit.apply(document);
				}

				String actual = document.get();

				if (createDiffs) {
					// Use a diff as the golden file instead of the after
					actual = getDiff(before, actual);
					if (goldenName.endsWith(DOT_XML)) {
						goldenName = goldenName.substring(0, goldenName.length() - DOT_XML.length()) + ".diff";
					}
				}

				assertEqualsGolden(goldenName, actual);
			} else {
				System.out.println("Ignoring non-textfilechange in refactoring result");
				assertNull(change.getAffectedObjects());
			}
		}
	}

	protected UiViewElementNode createModel(UiViewElementNode parent, Element element) {
		List<Element> children = DomUtilities.getChildren(element);
		String fqcn = ANDROID_WIDGET_PREFIX + element.getTagName();
		boolean hasChildren = children.size() > 0;
		UiViewElementNode node = createNode(parent, fqcn, hasChildren);
		node.setXmlNode(element);
		for (Element child : children) {
			createModel(node, child);
		}

		return node;
	}

	/**
	 * Builds up a ViewInfo hierarchy for the given model. This is done by
	 * reading .info dump files which record the exact pixel sizes of each
	 * ViewInfo object. These files are assumed to match up exactly with the
	 * model objects. This is done rather than rendering an actual layout
	 * hierarchy to insulate the test from pixel difference (in say font size)
	 * among platforms, as well as tying the test to particulars about relative
	 * sizes of things which may change with theme adjustments etc.
	 * <p>
	 * Each file can be generated by the dump method in the ViewHierarchy.
	 */
	protected ViewInfo createInfos(UiElementNode model, String relativePath) throws IOException {
		String basename = relativePath.substring(0, relativePath.lastIndexOf('.') + 1);
		String relative = basename + "info"; //$NON-NLS-1$
		String info = readTestFile(relative, true);
		// Parse the info file and build up a model from it
		// Each line contains a new info.
		// If indented it is a child of the parent.
		String[] lines = info.split("\n"); //$NON-NLS-1$

		// Iteration order for the info file should match exactly the UI model
		// so
		// we can just advance the line index sequentially as we traverse

		return create(model, Arrays.asList(lines).iterator());
	}

	protected ViewInfo create(UiElementNode node, Iterator<String> lineIterator) {
		// android.widget.LinearLayout [0,36,240,320]
		Pattern pattern = Pattern.compile("(\\s*)(\\S+) \\[(\\d+),(\\d+),(\\d+),(\\d+)\\].*");
		assertTrue(lineIterator.hasNext());
		String description = lineIterator.next();
		Matcher matcher = pattern.matcher(description);
		assertTrue(matcher.matches());
		// String indent = matcher.group(1);
		// String fqcn = matcher.group(2);
		String left = matcher.group(3);
		String top = matcher.group(4);
		String right = matcher.group(5);
		String bottom = matcher.group(6);

		ViewInfo view = new ViewInfo(node.getXmlNode().getLocalName(), node, Integer.parseInt(left),
				Integer.parseInt(top), Integer.parseInt(right), Integer.parseInt(bottom));

		List<UiElementNode> childNodes = node.getUiChildren();
		if (childNodes.size() > 0) {
			List<ViewInfo> children = new ArrayList<ViewInfo>();
			for (UiElementNode child : childNodes) {
				children.add(create(child, lineIterator));
			}
			view.setChildren(children);
		}

		return view;
	}

	protected TestContext setupTestContext(IFile file, String relativePath) throws Exception {
		IStructuredModel structuredModel = null;
		org.w3c.dom.Document domDocument = null;
		IStructuredDocument structuredDocument = null;
		Element element = null;

		try {
			IModelManager modelManager = StructuredModelManager.getModelManager();
			structuredModel = modelManager.getModelForRead(file);
			if (structuredModel instanceof IDOMModel) {
				IDOMModel domModel = (IDOMModel) structuredModel;
				domDocument = domModel.getDocument();
				element = domDocument.getDocumentElement();
				structuredDocument = structuredModel.getStructuredDocument();
			}
		} finally {
			if (structuredModel != null) {
				structuredModel.releaseFromRead();
			}
		}

		assertNotNull(structuredModel);
		assertNotNull(domDocument);
		assertNotNull(element);
		assertNotNull(structuredDocument);
		assertTrue(element instanceof IndexedRegion);

		UiViewElementNode model = createModel(null, element);
		ViewInfo info = createInfos(model, relativePath);
		CanvasViewInfo rootView = CanvasViewInfo.create(info, true /* layoutlib5 */).getFirst();
		TestLayoutEditorDelegate layoutEditor = new TestLayoutEditorDelegate(file, structuredDocument, null);

		TestContext testInfo = createTestContext();
		testInfo.mFile = file;
		testInfo.mStructuredModel = structuredModel;
		testInfo.mStructuredDocument = structuredDocument;
		testInfo.mElement = element;
		testInfo.mDomDocument = domDocument;
		testInfo.mUiModel = model;
		testInfo.mViewInfo = info;
		testInfo.mRootView = rootView;
		testInfo.mLayoutEditorDelegate = layoutEditor;

		return testInfo;
	}

	protected TestContext createTestContext() {
		return new TestContext();
	}

	protected static class TestContext {
		protected IFile mFile;
		protected IStructuredModel mStructuredModel;
		protected IStructuredDocument mStructuredDocument;
		protected org.w3c.dom.Document mDomDocument;
		protected Element mElement;
		protected UiViewElementNode mUiModel;
		protected ViewInfo mViewInfo;
		protected CanvasViewInfo mRootView;
		protected TestLayoutEditorDelegate mLayoutEditorDelegate;
	}

	@Override
	public void testDummy() {
		// To avoid JUnit warning that this class contains no tests, even though
		// this is an abstract class and JUnit shouldn't try
	}
}
