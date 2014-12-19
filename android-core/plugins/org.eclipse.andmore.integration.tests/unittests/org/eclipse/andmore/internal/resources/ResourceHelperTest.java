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

package org.eclipse.andmore.internal.resources;

import com.android.ide.common.resources.ResourceDeltaKind;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.ide.common.resources.configuration.ResourceQualifier;
import com.android.resources.ResourceType;

import org.eclipse.core.resources.IResourceDelta;

import junit.framework.TestCase;


/**
 * Test ResourceHelper
 */
@SuppressWarnings("javadoc")
public class ResourceHelperTest extends TestCase {

    /**
     * temp fake qualifier class.
     */
    private static class FakeQualifierClass extends ResourceQualifier {

        @Override
        public boolean checkAndSet(String value, FolderConfiguration config) {
            return false;
        }

        @Override
        public boolean equals(Object object) {
            return false;
        }

        @Override
        public String getFolderSegment() {
            return null;
        }

        @Override
        public String getLongDisplayValue() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String getShortDisplayValue() {
            return null;
        }

        @Override
        public String getShortName() {
            return null;
        }

        @Override
        public boolean hasFakeValue() {
            return false;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public int since() {
            return 0;
        }
    }

    public void testGetIcon() throws Exception {
        // check that the method returns null for an unknown qualifier class
        assertNull(ResourceHelper.getIcon(FakeQualifierClass.class));

        // find all the qualifiers through FolderConfiguration.createdefault()
        FolderConfiguration config = new FolderConfiguration();
        config.createDefault();
        final int count = FolderConfiguration.getQualifierCount();
        for (int i = 0 ; i < count ; i++) {
            ResourceQualifier qual = config.getQualifier(i);
            assertNotNull(qual);
            assertNotNull(qual.getClass().getCanonicalName(),
                    ResourceHelper.getIcon(qual.getClass()));
        }
    }

    public void testGetResourceDeltaKind() {
        assertEquals(ResourceDeltaKind.ADDED,
                ResourceHelper.getResourceDeltaKind(IResourceDelta.ADDED));
        assertEquals(ResourceDeltaKind.REMOVED,
                ResourceHelper.getResourceDeltaKind(IResourceDelta.REMOVED));
        assertEquals(ResourceDeltaKind.CHANGED,
                ResourceHelper.getResourceDeltaKind(IResourceDelta.CHANGED));

        assertNull(ResourceHelper.getResourceDeltaKind(IResourceDelta.ADDED_PHANTOM));
    }

    public void testIsFileBasedResourceType() throws Exception {
        assertTrue(ResourceHelper.isFileBasedResourceType(ResourceType.ANIMATOR));
        assertTrue(ResourceHelper.isFileBasedResourceType(ResourceType.LAYOUT));

        assertFalse(ResourceHelper.isFileBasedResourceType(ResourceType.STRING));
        assertFalse(ResourceHelper.isFileBasedResourceType(ResourceType.DIMEN));
        assertFalse(ResourceHelper.isFileBasedResourceType(ResourceType.ID));

        // Both:
        assertTrue(ResourceHelper.isFileBasedResourceType(ResourceType.DRAWABLE));
        assertTrue(ResourceHelper.isFileBasedResourceType(ResourceType.COLOR));
    }

    public void testIsValueBasedResourceType() throws Exception {
        assertTrue(ResourceHelper.isValueBasedResourceType(ResourceType.STRING));
        assertTrue(ResourceHelper.isValueBasedResourceType(ResourceType.DIMEN));
        assertTrue(ResourceHelper.isValueBasedResourceType(ResourceType.ID));

        assertFalse(ResourceHelper.isValueBasedResourceType(ResourceType.LAYOUT));

        // These can be both:
        assertTrue(ResourceHelper.isValueBasedResourceType(ResourceType.DRAWABLE));
        assertTrue(ResourceHelper.isValueBasedResourceType(ResourceType.COLOR));
    }

    public void testCanCreateResource() throws Exception {
        assertTrue(ResourceHelper.canCreateResource("@layout/foo"));
        assertTrue(ResourceHelper.canCreateResource("@string/foo"));
        assertTrue(ResourceHelper.canCreateResource("@dimen/foo"));
        assertTrue(ResourceHelper.canCreateResource("@color/foo"));

        assertFalse(ResourceHelper.canCreateResource("@typo/foo")); // nonexistent type
        assertFalse(ResourceHelper.canCreateResource("@layout/foo bar")); // space
        assertFalse(ResourceHelper.canCreateResource("@layout/new")); // keyword
        assertFalse(ResourceHelper.canCreateResource("@android:string/foo")); // framework
        assertFalse(ResourceHelper.canCreateResource("@android:dimen/foo"));
        assertFalse(ResourceHelper.canCreateResource("@android:color/foo"));
    }

    public void testCanCreateResourceType() throws Exception {
        assertTrue(ResourceHelper.canCreateResourceType(ResourceType.LAYOUT));
        assertTrue(ResourceHelper.canCreateResourceType(ResourceType.STRING));
        assertTrue(ResourceHelper.canCreateResourceType(ResourceType.DIMEN));
        assertTrue(ResourceHelper.canCreateResourceType(ResourceType.COLOR));

        assertFalse(ResourceHelper.canCreateResourceType(ResourceType.RAW));
        assertFalse(ResourceHelper.canCreateResourceType(ResourceType.XML));
    }

    public void testStyleToTheme() throws Exception {
        assertEquals("Foo", ResourceHelper.styleToTheme("Foo"));
        assertEquals("Theme", ResourceHelper.styleToTheme("@android:style/Theme"));
        assertEquals("LocalTheme", ResourceHelper.styleToTheme("@style/LocalTheme"));
        //assertEquals("LocalTheme", ResourceHelper.styleToTheme("@foo.bar:style/LocalTheme"));
    }

    public void testIsProjectStyle() throws Exception {
        assertFalse(ResourceHelper.isProjectStyle("@android:style/Theme"));
        assertTrue(ResourceHelper.isProjectStyle("@style/LocalTheme"));
    }
}
