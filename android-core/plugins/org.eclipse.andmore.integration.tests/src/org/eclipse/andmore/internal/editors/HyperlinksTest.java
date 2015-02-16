/*
 * Copyright (C) 2010 The Android Open Source Project
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
package org.eclipse.andmore.internal.editors;

import static com.android.SdkConstants.FD_SOURCES;
import static org.junit.Assert.*;

import com.android.ide.common.resources.ResourceFile;

import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.AndroidXmlEditor;
import org.eclipse.andmore.internal.editors.Hyperlinks;
import org.eclipse.andmore.internal.editors.Hyperlinks.ResourceLink;
import org.eclipse.andmore.internal.editors.Hyperlinks.XmlResolver;
import org.eclipse.andmore.internal.editors.layout.refactoring.AdtProjectTest;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.ErrorEditorPart;
import org.eclipse.ui.internal.browser.WebBrowserEditor;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.eclipse.wst.sse.ui.internal.StructuredTextViewer;
import org.eclipse.wst.xml.ui.internal.tabletree.XMLMultiPageEditorPart;
import org.junit.Ignore;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@SuppressWarnings({ "restriction", "javadoc" })
@Ignore
public class HyperlinksTest extends AdtProjectTest {

	@Test
	public void testFqnRegexp() throws Exception {
		assertTrue(Hyperlinks.isViewClassName("com.android.Foo"));
		assertTrue(Hyperlinks.isViewClassName("com.android.pk_g.Foo_Bar1"));
		assertTrue(Hyperlinks.isViewClassName("com.android.Foo$Inner"));

		// Should we allow non-standard packages and class names?
		// For now, we're allowing it -- see how this works out in practice.
		// assertFalse(XmlHyperlinkResolver.isViewClassName("Foo.bar"));
		assertTrue(Hyperlinks.isViewClassName("Foo.bar"));

		assertFalse(Hyperlinks.isViewClassName("LinearLayout"));
		assertFalse(Hyperlinks.isViewClassName("."));
		assertFalse(Hyperlinks.isViewClassName(".F"));
		assertFalse(Hyperlinks.isViewClassName("f."));
		assertFalse(Hyperlinks.isViewClassName("Foo"));
		assertFalse(Hyperlinks.isViewClassName("com.android.1Foo"));
		assertFalse(Hyperlinks.isViewClassName("1com.Foo"));
	}

	@Test
	public void testNavigate1() throws Exception {
		// Check navigating to a local resource
		checkXmlNavigation("navigation1.xml", "res/layout/navigation1.xml", "android:text=\"@string/app^_name\"");
	}

	@Test
	public void testNavigate2() throws Exception {
		// Check navigating to a framework resource
		checkXmlNavigation("navigation1.xml", "res/layout/navigation1.xml",
				"marginLeft=\"@android:dimen/app_ico^n_size\"");
	}

	@Test
	public void testNavigate3() throws Exception {
		// Check navigating to a style
		checkXmlNavigation("navigation1.xml", "res/layout/navigation1.xml", "style=\"@android:style/Widget.B^utton\"");
	}

	@Test
	@Ignore
	public void testNavigate4() throws Exception {
		// Check navigating to resource with many resolutions
		checkXmlNavigation("navigation1.xml", "res/layout/navigation1.xml", "android:text=\"@android:st^ring/ok\"");
	}

	@Test
	public void testNavigate5() throws Exception {
		// Check navigating to styles
		checkXmlNavigation("navigationstyles.xml", "res/values/navigationstyles.xml",
				"parent=\"android:Theme.Li^ght\">");
	}

	@Test
	public void testNavigate6() throws Exception {
		// Check navigating to a portion of a style (this should pick
		// android:Theme, not
		// android:Theme.Light
		checkXmlNavigation("navigationstyles.xml", "res/values/navigationstyles.xml",
				"parent=\"android:The^me.Light\">");
	}

	@Test
	public void testNavigate7() throws Exception {
		// Check navigating to a resource inside text content
		checkXmlNavigation("navigationstyles.xml", "res/values/navigationstyles.xml",
				"popupBackground\">@android:drawable/spinner_dr^opdown_background</item>");
	}

	@Test
	@Ignore
	public void testNavigate8() throws Exception {
		// Check navigating to a resource inside text content where there is
		// space around
		// the URL
		checkXmlNavigation("navigationstyles.xml", "res/values/navigationstyles.xml",
				"colorBackground\"> @color/cust^om_theme_color </item>");
	}

	@Test
	public void testNavigate9a() throws Exception {
		// Check navigating to a an activity
		checkXmlNavigation("manifest.xml", "AndroidManifest.xml", "<activity android:name=\".Test^Activity\"");
	}

	/*
	 * Not yet implemented public void testNavigate9b() throws Exception { //
	 * Check navigating to a an activity - clicking on the activity element
	 * should // work too checkXmlNavigation("manifest.xml",
	 * "AndroidManifest.xml", "<acti^vity android:name=\".TestActivity\""); }
	 */

	@Test
	public void testNavigate10() throws Exception {
		// Check navigating to a permission
		checkXmlNavigation("manifest.xml", "AndroidManifest.xml",
				"<uses-permission android:name=\"android.permission.AC^CESS_NETWORK_STATE\" />");
	}

	@Test
	public void testNavigate11a() throws Exception {
		// Check navigating to an intent
		checkXmlNavigation("manifest.xml", "AndroidManifest.xml",
				"<action android:name=\"android.intent.ac^tion.MAIN\" />");
	}

	@Test
	public void testNavigate11g() throws Exception {
		// Check navigating to an intent
		checkXmlNavigation("manifest.xml", "AndroidManifest.xml",
				"<category android:name=\"android.intent.category.LA^UNCHER\" />");
	}

	@Test
	public void testNavigate12() throws Exception {
		// Check navigating to a custom view class
		checkXmlNavigation("navigation1.xml", "res/layout/navigation1.xml", "<my.Cust^omView></my.CustomView>");
	}

	@Test
	public void testNavigate13() throws Exception {
		// Check jumping to classes pointed to by fragments

		getTestDataFile(getProject(), "TestFragment.java.txt",
				FD_SOURCES + "/" + TEST_PROJECT_PACKAGE.replace('.', '/') + "/TestFragment.java");
		checkXmlNavigation("fragmentlayout.xml", "res/layout/fragmentlayout.xml",
				"android:name=\"com.android.ecl^ipse.tests.TestFragment\"");
	}

	@Test
	public void testNavigate14() throws Exception {
		// Check jumping to classes pointed to by fragments

		getTestDataFile(getProject(), "TestFragment.java.txt",
				FD_SOURCES + "/" + TEST_PROJECT_PACKAGE.replace('.', '/') + "/TestFragment.java");
		checkXmlNavigation("fragmentlayout.xml", "res/layout/fragmentlayout.xml",
				"class=\"com.and^roid.eclipse.tests.TestFragment\"");
	}

	@Test
	public void testNavigate15() throws Exception {
		// Check navigating to a theme resource
		checkXmlNavigation("navigation1.xml", "res/layout/navigation1.xml", "?android:attr/alert^DialogStyle");
	}

	@Test
	public void testNavigate16() throws Exception {
		// Check navigating to a theme resource
		checkXmlNavigation("navigation1.xml", "res/layout/navigation1.xml", "?android:alert^DialogStyle");
	}

	// Left to test:
	// onClick handling
	// class attributes
	// Test that the correct file is actually opened!

	private void checkXmlNavigation(String basename, String targetPath, String caretLocation) throws Exception {
		IFile file = getTestDataFile(getProject(), basename, targetPath, true);

		// Determine the offset
		int offset = getCaretOffset(file, caretLocation);

		// Open file
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		assertNotNull(page);
		IEditorPart editor = IDE.openEditor(page, file);
		assertTrue(editor.getClass().getName(), editor instanceof AndroidXmlEditor);
		AndroidXmlEditor layoutEditor = (AndroidXmlEditor) editor;
		ISourceViewer viewer = layoutEditor.getStructuredSourceViewer();

		XmlResolver resolver = new Hyperlinks.XmlResolver();
		IHyperlink[] links = resolver.detectHyperlinks(viewer, new Region(offset, 0), true);
		assertNotNull(links);

		StringBuilder sb = new StringBuilder(1000);
		sb.append("Go To Declaration in " + basename + " for " + caretLocation + ":\n");
		for (IHyperlink link : links) {
			sb.append(link.getHyperlinkText());
			sb.append(" : ");
			sb.append(" [");
			IRegion region = link.getHyperlinkRegion();
			sb.append(viewer.getDocument().get(region.getOffset(), region.getLength()));
			sb.append("]");
			if (link instanceof Hyperlinks.ResourceLink) {
				Hyperlinks.ResourceLink resourceLink = (ResourceLink) link;
				sb.append("\n    ");
				ResourceFile resourceFile = resourceLink.getFile();
				String desc = resourceFile.toString();
				desc = desc.replace('\\', '/');
				// For files in the SDK folder, strip out file prefix
				int dataRes = desc.indexOf("data/res");
				if (dataRes != -1) {
					desc = desc.substring(dataRes);
				}
				desc = removeSessionData(desc);
				sb.append(desc);
			}
			sb.append('\n');
		}

		// Open the first link
		IHyperlink link = links[0];
		link.open();
		IEditorPart newEditor = AdtUtils.getActiveEditor();
		// Ensure that this isn't an invalid file (e.g. opening the SDK platform
		// files
		// with incorrect content binding could cause this)
		assertTrue(!(newEditor instanceof ErrorEditorPart));

		IDocument document = null;
		Point selection = null;

		if (newEditor instanceof AndroidXmlEditor) {
			AndroidXmlEditor xmlEditor = (AndroidXmlEditor) newEditor;
			document = xmlEditor.getStructuredSourceViewer().getDocument();
			selection = xmlEditor.getStructuredSourceViewer().getSelectedRange();
		} else if (newEditor instanceof XMLMultiPageEditorPart) {
			XMLMultiPageEditorPart xmlEditor = (XMLMultiPageEditorPart) newEditor;
			Field field = xmlEditor.getClass().getDeclaredField("fTextEditor");
			field.setAccessible(true);
			StructuredTextEditor ste = (StructuredTextEditor) field.get(newEditor);
			if (ste == null) {
				Method method = xmlEditor.getClass().getMethod("getTextEditor", new Class[0]);
				ste = (StructuredTextEditor) method.invoke(newEditor, new Object[0]);
			}
			StructuredTextViewer textViewer = ste.getTextViewer();
			document = textViewer.getDocument();
			selection = textViewer.getSelectedRange();
		} else if (newEditor instanceof WebBrowserEditor) {
			WebBrowserEditor browser = (WebBrowserEditor) newEditor;
			Field field = browser.getClass().getDeclaredField("initialURL");
			field.setAccessible(true);
			String initialUrl = (String) field.get(newEditor);
			int index = initialUrl.indexOf("reference");
			if (index != -1) {
				initialUrl = initialUrl.substring(index);
			}
			initialUrl = initialUrl.replace('\\', '/');
			sb.append("\n\nAfter open, a browser is shown with this URL:\n");
			sb.append("  ");
			sb.append(initialUrl);
			sb.append("\n");
		} else if (newEditor instanceof JavaEditor) {
			JavaEditor javaEditor = (JavaEditor) newEditor;
			document = javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
			IRegion range = javaEditor.getHighlightRange();
			selection = new Point(range.getOffset(), range.getLength());
		} else {
			fail("Unhandled editor type: " + newEditor.getClass().getName());
			return;
		}

		if (document != null && selection != null) {
			int lineStart = document.getLineInformationOfOffset(selection.x).getOffset();
			IRegion lineEndInfo = document.getLineInformationOfOffset(selection.x + selection.y);
			int lineEnd = lineEndInfo.getOffset() + lineEndInfo.getLength();
			String text = document.get(lineStart, lineEnd - lineStart);
			int selectionStart = selection.x - lineStart;
			int selectionEnd = selectionStart + selection.y;
			if (selectionEnd > selectionStart) {
				// Selection range
				text = text.substring(0, selectionStart) + "[^" + text.substring(selectionStart, selectionEnd) + "]"
						+ text.substring(selectionEnd);
			} else {
				text = text.substring(0, selectionStart) + "^" + text.substring(selectionStart);
			}
			text = removeSessionData(text);

			sb.append("\n\nAfter open, the selected text is:\n");
			sb.append(text);
			sb.append("\n");
		}

		assertEqualsGolden(basename, sb.toString(), "txt");
	}
}
