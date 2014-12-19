/*
 * Copyright (C) 2012 The Android Open Source Project
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
package org.eclipse.andmore.internal.editors.layout.properties;

import static com.android.SdkConstants.ANDROID_URI;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.api.IAttributeInfo;
import com.android.ide.common.api.IAttributeInfo.Format;

import org.eclipse.andmore.common.layout.TestAttributeInfo;
import org.eclipse.andmore.internal.editors.common.CommonXmlEditor;
import org.eclipse.andmore.internal.editors.descriptors.AttributeDescriptor;
import org.eclipse.andmore.internal.editors.descriptors.TextAttributeDescriptor;
import org.eclipse.jface.fieldassist.IContentProposal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import junit.framework.TestCase;

@SuppressWarnings("javadoc")
public class ValueCompleterTest extends TestCase {
    private void checkCompletion(String text, int offset,
            String property, EnumSet<Format> formats, String[] values,
            List<String> expected) {
        assertTrue(text.length() >= offset);

        TestAttributeInfo info =
                new TestAttributeInfo(property, formats, "unittest", values, values, null);
        TestValueCompleter completer = new TestValueCompleter(
                new TestTextAttributeDescriptor(property, info));
        IContentProposal[] proposals = completer.getProposals(text, offset);
        List<String> actual = new ArrayList<String>();
        for (IContentProposal proposal : proposals) {
            String content = proposal.getContent();
            actual.add(content);
        }
        assertEquals(expected.toString(), actual.toString());
    }

    public void test() throws Exception {
        checkCompletion("@android:string", 3, "text",
                EnumSet.of(Format.REFERENCE), null,
                Arrays.asList(new String[] { }));
    }

    public void test1a() throws Exception {
        checkCompletion("matc", 4, "layout_width",
                EnumSet.of(Format.DIMENSION, Format.ENUM),
                new String[] { "fill_parent", "match_parent", "wrap_content" },

                Arrays.asList(new String[] { "match_parent", "fill_parent", "wrap_content" }));
    }

    public void test1b() throws Exception {
        checkCompletion("fi", 2, "layout_width",
                EnumSet.of(Format.DIMENSION, Format.ENUM),
                new String[] { "fill_parent", "match_parent", "wrap_content" },

                Arrays.asList(new String[] { "fill_parent", "match_parent", "wrap_content" }));
    }

    public void test2() throws Exception {
        checkCompletion("50", 2, "layout_width",
                EnumSet.of(Format.DIMENSION, Format.ENUM),
                new String[] { "fill_parent", "match_parent", "wrap_content" },

                Arrays.asList(new String[] { "50dp", "fill_parent", "match_parent",
                        "wrap_content" }));
    }

    public void test3() throws Exception {
        checkCompletion("42", 2, "textSize",
                EnumSet.of(Format.DIMENSION),
                null,

                Arrays.asList(new String[] { "42sp", "42dp" }));
    }

    public void test4() throws Exception {
        checkCompletion("", 0, "gravity",
                EnumSet.of(Format.FLAG),
                new String[] { "top", "bottom", "left", "right", "center" },

                Arrays.asList(new String[] { "top", "bottom", "left", "right", "center" }));
    }

    public void test5() throws Exception {
        checkCompletion("left", 4, "gravity",
                EnumSet.of(Format.FLAG),
                new String[] { "top", "bottom", "left", "right", "center" },

                Arrays.asList(new String[] {
                        "left", "left|top", "left|bottom", "left|right", "left|center" }));
    }

    public void test6() throws Exception {
        checkCompletion("left|top", 8, "gravity",
                EnumSet.of(Format.FLAG),
                new String[] { "top", "bottom", "left", "right", "center" },

                Arrays.asList(new String[] {
                        "left|top", "left|top|bottom", "left|top|right", "left|top|center" }));
    }

    // TODO ?android

    private class TestTextAttributeDescriptor extends TextAttributeDescriptor {
        public TestTextAttributeDescriptor(String xmlLocalName, IAttributeInfo attrInfo) {
            super(xmlLocalName, ANDROID_URI, attrInfo);
        }
    }

    private class TestValueCompleter extends ValueCompleter {
        private final AttributeDescriptor mDescriptor;

        TestValueCompleter(AttributeDescriptor descriptor) {
            mDescriptor = descriptor;
            assert descriptor.getAttributeInfo() != null;
        }

        @Override
        @Nullable
        protected CommonXmlEditor getEditor() {
            return null;
        }

        @Override
        @NonNull
        protected AttributeDescriptor getDescriptor() {
            return mDescriptor;
        }
    }
}
