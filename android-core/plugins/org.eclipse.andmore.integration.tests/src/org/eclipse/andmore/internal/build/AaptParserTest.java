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

package org.eclipse.andmore.internal.build;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.build.AaptParser;
import org.eclipse.andmore.internal.editors.layout.refactoring.AdtProjectTest;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class AaptParserTest extends AdtProjectTest {

	public void testBasic() throws Exception {
		// Test the "at 'property' with value 'value' range matching included
		// with most aapt errors
		checkRanges("quickfix1.xml", "res/layout/quickfix1.xml",
				"quickfix1.xml:7: error: Error: No resource found that matches the given name (at"
						+ " 'text' with value '@string/firststring').", "android:text=\"^@string/firststring\"",
				"android:text=\"@string/firststring^\"");
	}

	public void testRange1() throws Exception {
		// Check that when the actual aapt error occurs on a line later than the
		// original error
		// line, the forward search which looks for a value match does not stop
		// on an
		// earlier line that happens to have the same value prefix
		checkRanges("aapterror1.xml", "res/layout/aapterror1.xml",
				"aapterror1.xml:5: error: Error: Integer types not allowed (at "
						+ "'layout_marginBottom' with value '50').", "marginBottom=\"^50\"", "marginBottom=\"50^\"");
	}

	public void testRange2() throws Exception {
		// Check that when we have a duplicate resource error, we highlight both
		// the original
		// property and the original definition.
		// This tests the second, duplicate declaration ration.
		checkRanges("aapterror2.xml", "res/values/aapterror2.xml",
				"aapterror2.xml:7: error: Resource entry repeatedStyle1 already has bag item " + "android:gravity.",
				"<item name=\"^android:gravity\">bottom</item>", "<item name=\"android:gravity^\">bottom</item>");
	}

	public void testRange3() throws Exception {
		// Check that when we have a duplicate resource error, we highlight both
		// the original
		// property and the original definition.
		// This tests the original definition. Note that we don't have enough
		// position info
		// so we simply highlight the whitespace portion of the line.
		checkRanges("aapterror2.xml", "res/values/aapterror2.xml", "aapterror2.xml:4: Originally defined here.",
				"^<item name=\"android:gravity\">left</item>", "<item name=\"android:gravity\">left</item>^");
	}

	public void testRange4() throws Exception {
		// Check for aapt error which occurs when the attribute name in an item
		// style declaration
		// is nonexistent
		checkRanges("aapterror3.xml", "res/values/aapterror3.xml",
				"aapterror3.xml:4: error: Error: No resource found that matches the given name: "
						+ "attr 'nonexistent'.", "<item name=\"^nonexistent\">5</item>",
				"<item name=\"nonexistent^\">5</item>");
	}

	public void testRange5() throws Exception {
		// Test missing resource name
		checkRanges("aapterror4.xml", "res/values/aapterror4.xml",
				"aapterror4.xml:3: error: A 'name' attribute is required for <style>", "<^style>", "<style^>");
	}

	public void testRange6() throws Exception {
		checkRanges("aapterror4.xml", "res/values/aapterror4.xml",
				"aapterror4.xml:6: error: A 'type' attribute is required for <item>", "<^item></item>",
				"<item^></item>");
	}

	public void testRange7() throws Exception {
		// Test missing resource name
		checkRanges("aapterror4.xml", "res/values/aapterror4.xml",
				"aapterror4.xml:6: error: A 'name' attribute is required for <item>", "<^item></item>",
				"<item^></item>");
	}

	// This test is disabled because I can't find a useful scenario for handling
	// this error
	// message. When this error occurs, we will also get a warning on a missing
	// attribute, and
	// that warning already underlines the element name.
	// public void testRange8() throws Exception {
	// // Test missing resource name
	// checkRanges("aapterror4.xml", "res/values/aapterror4.xml",
	// "aapterror4.xml:4: error: Error: Resource id cannot be an empty string: attr ''.",
	// "        ^<item />",
	// "        <item />^");
	// }

	public void testRange9() throws Exception {
		// Test missing resource name
		checkRanges("aapterror5.xml", "res/values/aapterror5.xml",
				"aapterror5.xml:4: error: Error: String types not allowed (at "
						+ "'android:layout_width' with value '').",
				"        <item name=\"^android:layout_width\"></item>",
				"        <item name=\"android:layout_width^\"></item>");
	}

	public void testRange10() throws Exception {
		// Test missing resource name
		checkRanges("aapterror6.xml", "res/layout/aapterror6.xml",
				"aapterror6.xml:5: error: Error: String types not allowed (at 'layout_marginTop'" + " with value '').",
				"android:layout_marginTop=^\"\"", "android:layout_marginTop=\"\"^");
	}

	public void testRange11() throws Exception {
		// Test missing resource name
		checkRanges(
				"aapterror6.xml",
				"res/layout/aapterror6.xml",
				"aapterror1.xml:5: error: Error: String types not allowed (at 'layout_marginLeft'" + " with value '').",
				"android:layout_marginLeft=^''", "android:layout_marginLeft=''^");
	}

	public void testRange12() throws Exception {
		// Test missing resource name
		checkRanges("aapterror7.xml", "res/layout/aapterror7.xml",
				"aapterror7.xml:5: error: Error: String types not allowed (at 'id'" + " with value '').",
				"android:id=^\"\"", "android:id=\"\"^");
	}

	private void checkRanges(String name, String destPath, String aaptError, String expectCaretBegin,
			String expectCaretEnd) throws Exception {
		IProject project = getProject();
		IFile file = getTestDataFile(project, name, destPath);

		// Make file paths absolute
		String osRoot = project.getLocation().toOSString();
		String fileRelativePath = file.getProjectRelativePath().toPortableString();
		String filePath = osRoot + File.separator + fileRelativePath;
		String originalError = filePath + aaptError.substring(aaptError.indexOf(':'));
		List<String> errors = Collections.singletonList(originalError);

		// Remove anything already placed there by the project create/build
		// automatic
		// (this usually only happens while debugging so the background thread
		// has a chance
		// to get things going)
		IMarker[] markers = file.findMarkers(AdtConstants.MARKER_AAPT_COMPILE, true, IResource.DEPTH_ZERO);
		for (IMarker marker : markers) {
			marker.delete();
		}

		AaptParser.parseOutput(errors, project);
		markers = file.findMarkers(AdtConstants.MARKER_AAPT_COMPILE, true, IResource.DEPTH_ZERO);
		assertNotNull(markers);
		assertEquals(1, markers.length);

		String fileContents = AdtPlugin.readFile(file);
		int rangeBegin = getCaretOffset(file, expectCaretBegin);
		int rangeEnd = getCaretOffset(file, expectCaretEnd);

		// Check text range
		IMarker marker = markers[0];
		String message = marker.getAttribute(IMarker.MESSAGE, ""); //$NON-NLS-1$
		String simplerMessage = aaptError.substring(aaptError.indexOf(' ') + 1);
		assertEquals(simplerMessage, message);
		int start = marker.getAttribute(IMarker.CHAR_START, 0);
		int end = marker.getAttribute(IMarker.CHAR_END, 0);

		assertEquals(
				"Wrong start offset, expected " + expectCaretBegin + " but was " + getCaretContext(fileContents, start),
				rangeBegin, start);
		assertEquals("Wrong end offset, expected " + expectCaretEnd + " but was " + getCaretContext(fileContents, end),
				rangeEnd, end);
	}
}
