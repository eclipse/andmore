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
package org.eclipse.andmore.common.resources.platform;

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.DOT_XML;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.ide.common.api.IAttributeInfo.Format;
import com.android.ide.common.resources.ResourceItem;
import com.android.ide.common.resources.ResourceRepository;
import com.android.io.IAbstractFolder;
import com.android.io.IAbstractResource;
import com.android.resources.ResourceType;
import com.android.utils.StdLogger;
import com.google.common.base.Charsets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import junit.framework.TestCase;

import org.eclipse.andmore.internal.editors.layout.gle2.DomUtilities;
import org.eclipse.andmore.mock.Mocks;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("javadoc")
public class AttributeInfoTest extends TestCase {
    public void testSimple() throws Exception {
        AttributeInfo info = new AttributeInfo("test", EnumSet.noneOf(Format.class));
        assertTrue(info.isValid("", null, null));
        assertTrue(info.isValid("a b c", null, null));
        assertTrue(info.isValid("@android foo bar", null, null));
    }

    public void testIsValidString() throws Exception {
        AttributeInfo info = new AttributeInfo("test", Format.STRING_SET);
        assertTrue(info.isValid("", null, null));
        assertTrue(info.isValid("a b c", null, null));
        assertTrue(info.isValid("@android foo bar", null, null));
    }

    public void testIsValidBoolean() throws Exception {
        AttributeInfo info = new AttributeInfo("test", Format.BOOLEAN_SET);
        assertTrue(info.isValid("true", null, null));
        assertTrue(info.isValid("false", null, null));
        assertFalse(info.isValid("", null, null));
        assertTrue(info.isValid("TRUE", null, null));
        assertTrue(info.isValid("True", null, null));
        assertTrue(info.isValid("FALSE", null, null));
        assertTrue(info.isValid("False", null, null));
    }

    public void testIsValidInteger() throws Exception {
        AttributeInfo info = new AttributeInfo("test", Format.INTEGER_SET);
        assertTrue(info.isValid("0", null, null));
        assertTrue(info.isValid("1", null, null));
        assertTrue(info.isValid("10", null, null));
        assertTrue(info.isValid("-10", null, null));
        assertTrue(info.isValid(Integer.toString(Integer.MAX_VALUE), null, null));

        assertFalse(info.isValid("", null, null));
        assertFalse(info.isValid("a", null, null));
        assertFalse(info.isValid("a1", null, null));
        assertFalse(info.isValid("1a", null, null));
        assertFalse(info.isValid("1.0", null, null));
    }

    public void testIsValidFloat() throws Exception {
        AttributeInfo info = new AttributeInfo("test", Format.FLOAT_SET);
        assertTrue(info.isValid("0", null, null));
        assertTrue(info.isValid("1", null, null));
        assertTrue(info.isValid("10", null, null));
        assertTrue(info.isValid("-10", null, null));
        assertTrue(info.isValid("-10.1234", null, null));
        assertTrue(info.isValid(".1", null, null));
        assertTrue(info.isValid("-.1", null, null));
        assertTrue(info.isValid("1.5e22", null, null));
        assertTrue(info.isValid(Integer.toString(Integer.MAX_VALUE), null, null));

        assertFalse(info.isValid("", null, null));
        assertFalse(info.isValid(".", null, null));
        assertFalse(info.isValid("-.", null, null));
        assertFalse(info.isValid("a", null, null));
        assertFalse(info.isValid("a1", null, null));
        assertFalse(info.isValid("1a", null, null));
    }

    public void testIsValidDimension() throws Exception {
        AttributeInfo info = new AttributeInfo("test", Format.DIMENSION_SET);
        assertTrue(info.isValid("0dp", null, null));
        assertTrue(info.isValid("1dp", null, null));
        assertTrue(info.isValid("10dip", null, null));
        assertTrue(info.isValid("-10px", null, null));
        assertTrue(info.isValid("-10.1234mm", null, null));
        assertTrue(info.isValid("14sp", null, null));
        assertTrue(info.isValid("72pt", null, null));

        assertFalse(info.isValid("", null, null));
        assertFalse(info.isValid("5", null, null));
        assertFalse(info.isValid("50ps", null, null));
        // Since we allow resources even when not specified in format, don't assert
        // this:
        //assertFalse(info.isValid("@dimen/foo"));
    }

    public void testIsValidColor() throws Exception {
        AttributeInfo info = new AttributeInfo("test", Format.COLOR_SET);
        assertTrue(info.isValid("#fff", null, null));
        assertTrue(info.isValid("#ffffff", null, null));
        assertTrue(info.isValid("#12345678", null, null));
        assertTrue(info.isValid("#abcdef00", null, null));

        assertFalse(info.isValid("", null, null));
        assertFalse(info.isValid("#fffffffff", null, null));
        assertFalse(info.isValid("red", null, null));
        assertFalse(info.isValid("rgb(1,2,3)", null, null));
    }

    public void testIsValidFraction() throws Exception {
        AttributeInfo info = new AttributeInfo("test", EnumSet.<Format>of(Format.FRACTION));
        assertTrue(info.isValid("5%", null, null));
        assertTrue(info.isValid("25%p", null, null));

        // We don't validate fractions accurately yet
        //assertFalse(info.isValid(""));
        //assertFalse(info.isValid("50%%"));
        //assertFalse(info.isValid("50"));
        //assertFalse(info.isValid("-2%"));
    }

    public void testIsValidReference() throws Exception {
        AttributeInfo info = new AttributeInfo("test", Format.REFERENCE_SET);
        assertTrue(info.isValid("@android:string/foo", null, null));
        assertTrue(info.isValid("@string/foo", null, null));
        assertTrue(info.isValid("@dimen/foo", null, null));
        assertTrue(info.isValid("@color/foo", null, null));
        assertTrue(info.isValid("@animator/foo", null, null));
        assertTrue(info.isValid("@anim/foo", null, null));
        assertTrue(info.isValid("?android:attr/textAppearanceMedium", null, null));
        assertTrue(info.isValid("?textAppearanceMedium", null, null));

        assertFalse(info.isValid("", null, null));
        assertFalse(info.isValid("foo", null, null));
        assertFalse(info.isValid("3.4", null, null));
    }

    public void testIsValidEnum() throws Exception {
        AttributeInfo info = new AttributeInfo("test", Format.ENUM_SET);
        info.setEnumValues(new String[] { "wrap_content", "match_parent" });
        assertTrue(info.isValid("wrap_content", null, null));
        assertTrue(info.isValid("match_parent", null, null));
        assertFalse(info.isValid("", null, null));
        assertFalse(info.isValid("other", null, null));
        assertFalse(info.isValid("50", null, null));
    }

    public void testIsValidFlag() throws Exception {
        AttributeInfo info = new AttributeInfo("test", Format.FLAG_SET);
        info.setFlagValues(new String[] { "left", "top", "right", "bottom" });
        assertTrue(info.isValid("left", null, null));
        assertTrue(info.isValid("top", null, null));
        assertTrue(info.isValid("left|top", null, null));
        assertTrue(info.isValid("", null, null));

        assertFalse(info.isValid("other", null, null));
        assertFalse(info.isValid("50", null, null));
    }

    public void testCombined1() throws Exception {
        AttributeInfo info = new AttributeInfo("test", EnumSet.<Format>of(Format.INTEGER,
                Format.REFERENCE));
        assertTrue(info.isValid("1", null, null));
        assertTrue(info.isValid("@dimen/foo", null, null));
        assertFalse(info.isValid("foo", null, null));
    }

    public void testCombined2() throws Exception {
        AttributeInfo info = new AttributeInfo("test", EnumSet.<Format>of(Format.COLOR,
                Format.REFERENCE));
        assertTrue(info.isValid("#ff00ff00", null, null));
        assertTrue(info.isValid("@color/foo", null, null));
        assertFalse(info.isValid("foo", null, null));
    }

    public void testCombined3() throws Exception {
        AttributeInfo info = new AttributeInfo("test", EnumSet.<Format>of(Format.STRING,
                Format.REFERENCE));
        assertTrue(info.isValid("test", null, null));
        assertTrue(info.isValid("@color/foo", null, null));
    }

    public void testCombined4() throws Exception {
        AttributeInfo info = new AttributeInfo("test", EnumSet.<Format>of(Format.ENUM,
                Format.DIMENSION));
        info.setEnumValues(new String[] { "wrap_content", "match_parent" });
        assertTrue(info.isValid("wrap_content", null, null));
        assertTrue(info.isValid("match_parent", null, null));
        assertTrue(info.isValid("50dp", null, null));
        assertFalse(info.isValid("50", null, null));
        assertFalse(info.isValid("test", null, null));
    }

    public void testResourcesExist() throws Exception {
        IAbstractFolder folder = Mocks.createAbstractFolder(
                SdkConstants.FD_RESOURCES, new IAbstractResource[0]);

        AttributeInfo info = new AttributeInfo("test", Format.REFERENCE_SET);
        TestResourceRepository projectResources = new TestResourceRepository(folder,false);
        projectResources.addResource(ResourceType.STRING, "mystring");
        projectResources.addResource(ResourceType.DIMEN, "mydimen");
        TestResourceRepository frameworkResources = new TestResourceRepository(folder, true);
        frameworkResources.addResource(ResourceType.LAYOUT, "mylayout");

        assertTrue(info.isValid("@string/mystring", null, null));
        assertTrue(info.isValid("@dimen/mydimen", null, null));
        assertTrue(info.isValid("@android:layout/mylayout", null, null));
        assertTrue(info.isValid("?android:attr/listPreferredItemHeigh", null, null));

        assertTrue(info.isValid("@string/mystring", projectResources, frameworkResources));
        assertTrue(info.isValid("@dimen/mydimen", projectResources, frameworkResources));
        assertTrue(info.isValid("@android:layout/mylayout", projectResources, frameworkResources));

        assertFalse(info.isValid("@android:string/mystring", projectResources,
                frameworkResources));
        assertFalse(info.isValid("@android:dimen/mydimen", projectResources, frameworkResources));
        assertFalse(info.isValid("@layout/mylayout", projectResources, frameworkResources));
        assertFalse(info.isValid("@layout/foo", projectResources, frameworkResources));
        assertFalse(info.isValid("@anim/foo", projectResources, frameworkResources));
        assertFalse(info.isValid("@android:anim/foo", projectResources, frameworkResources));
    }

    private class TestResourceRepository extends ResourceRepository {
        private Multimap<ResourceType, String> mResources = ArrayListMultimap.create();

        protected TestResourceRepository(IAbstractFolder resFolder, boolean isFrameworkRepository) {
            super(resFolder, isFrameworkRepository);
        }

        void addResource(ResourceType type, String name) {
            mResources.put(type, name);
        }

        @Override
        @NonNull
        protected ResourceItem createResourceItem(@NonNull String name) {
            fail("Not used in test");
            return null;
        }

        @Override
        public boolean hasResourceItem(@NonNull ResourceType type, @NonNull String name) {
            Collection<String> names = mResources.get(type);
            if (names != null) {
                return names.contains(name);
            }

            return false;
        }
    };


    public void testIsValid() throws Exception {
        // This test loads the full attrs.xml file and then processes a bunch of platform
        // resource file and makes sure that they're all considered valid. This helps
        // make sure that isValid() closely matches what aapt accepts.
        String sdkPath = System.getenv("ADT_SDK_SOURCE_PATH");
        assertNotNull("This test requires ADT_SDK_SOURCE_PATH to be set to point to the" +
                "SDK git repository", sdkPath);
        File sdk = new File(sdkPath);
        assertNotNull("$ADT_SDK_SOURCE_PATH (" + sdk.getPath() + ") is not a directory",
                sdk.isDirectory());
        File git = sdk.getParentFile();
        File attrsPath = new File(git, "frameworks" + File.separator + "base"
                + File.separator + "core" + File.separator + "res" + File.separator + "res"
                + File.separator + "values" + File.separator + "attrs.xml");
        assertTrue(attrsPath.getPath(), attrsPath.exists());
        AttrsXmlParser parser = new AttrsXmlParser(attrsPath.getPath(),
                new StdLogger(StdLogger.Level.VERBOSE), 1100);
        parser.preload();
        Map<String, AttributeInfo> attributeMap = parser.getAttributeMap();
        assertNotNull(attributeMap);
        assertNotNull(attributeMap.get("layout_width"));
        Set<String> seen = Sets.newHashSet();

        checkDir(new File(git, "packages" + File.separator + "apps"), false, attributeMap, seen);
    }

    private void checkDir(File dir, boolean isResourceDir,
            Map<String, AttributeInfo> map, Set<String> seen) throws IOException {
        assertTrue(dir.isDirectory());
        File[] list = dir.listFiles();
        if (list != null) {
            for (File file : list) {
                if (isResourceDir && file.isFile() && file.getPath().endsWith(DOT_XML)) {
                    checkXmlFile(file, map, seen);
                } else if (file.isDirectory()) {
                    checkDir(file, isResourceDir || file.getName().equals("res"), map, seen);
                }
            }
        }
    }

    private void checkXmlFile(File file, Map<String, AttributeInfo> map,
            Set<String> seen) throws IOException {
        String xml = Files.toString(file, Charsets.UTF_8);
        if (xml != null) {
            //Document doc = DomUtilities.parseStructuredDocument(xml);
            Document doc = DomUtilities.parseDocument(xml, false);
            if (doc != null && doc.getDocumentElement() != null) {
                checkElement(file, doc.getDocumentElement(), map, seen);
            }
        }
    }

    private void checkElement(File file, Element element, Map<String, AttributeInfo> map,
            Set<String> seen) {
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0, n = attributes.getLength(); i < n; i++) {
            Attr attribute = (Attr) attributes.item(i);

            String uri = attribute.getNamespaceURI();
            String name = attribute.getLocalName();
            String value = attribute.getValue();
            if (ANDROID_URI.equals(uri)) {
                AttributeInfo info = map.get(name);
                if (info == null) {
                    System.out.println("Warning: Unknown attribute '" + name + "' in " + file);
                            return;
                }
                if (!info.isValid(value, null, null)) {
                    if (name.equals("duration") || name.equals("exitFadeDuration")) {
                        // Already known
                        return;
                    }
                    String message = "In file " +
                            file.getPath() + ":\nCould not validate value \"" + value
                            + "\" for attribute '"
                            + name + "' where the attribute info has formats " + info.getFormats()
                            + "\n";
                    System.out.println("\n" + message);
                    fail(message);
                }
                if ((value.startsWith("@") || value.startsWith("?")) &&
                        !info.getFormats().contains(Format.REFERENCE)) {
                    // Print out errors in attrs.xml

                    if (!seen.contains(name)) {
                        seen.add(name);
                        System.out.println("\"" + name + "\" with formats " + info.getFormats()
                                + " was passed a reference (" + value + ") in file " + file);
                    }
                }
            }
        }
    }
}
