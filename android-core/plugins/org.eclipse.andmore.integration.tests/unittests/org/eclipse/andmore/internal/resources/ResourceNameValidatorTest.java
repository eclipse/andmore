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

package org.eclipse.andmore.internal.resources;

import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;

import org.eclipse.core.resources.IProject;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

@SuppressWarnings("javadoc")
public class ResourceNameValidatorTest extends TestCase {
    public void testValidator() throws Exception {
        // Valid
        ResourceNameValidator validator = ResourceNameValidator.create(true,
                ResourceFolderType.VALUES);
        assertTrue(validator.isValid("foo") == null);
        assertTrue(validator.isValid("foo.xml") == null);
        assertTrue(validator.isValid("Foo123_$") == null);
        assertTrue(validator.isValid("foo.xm") == null); // For non-file types, . => _

        // Invalid
        assertTrue(validator.isValid("") != null);
        assertTrue(validator.isValid(" ") != null);
        assertTrue(validator.isValid("foo bar") != null);
        assertTrue(validator.isValid("1foo") != null);
        assertTrue(validator.isValid("foo%bar") != null);
        assertTrue(ResourceNameValidator.create(true, Collections.singleton("foo"),
                ResourceType.STRING).isValid("foo") != null);
        assertTrue(ResourceNameValidator.create(true,
                ResourceFolderType.DRAWABLE).isValid("foo.xm") != null);
        assertTrue(ResourceNameValidator.create(false,
                ResourceFolderType.DRAWABLE).isValid("foo.xm") != null);

        // Only lowercase chars allowed in file-based resource names
        assertTrue(ResourceNameValidator.create(true, ResourceFolderType.LAYOUT)
                .isValid("Foo123_$") != null);
        assertTrue(ResourceNameValidator.create(true, ResourceFolderType.LAYOUT)
                .isValid("foo123_") == null);

        // Can't start with _ in file-based resource names, is okay for value based resources
        assertTrue(ResourceNameValidator.create(true, ResourceFolderType.VALUES)
                .isValid("_foo") == null);
        assertTrue(ResourceNameValidator.create(true, ResourceFolderType.LAYOUT)
                .isValid("_foo") != null);
        assertTrue(ResourceNameValidator.create(true, ResourceFolderType.DRAWABLE)
                .isValid("_foo") != null);
    }

    public void testIds() throws Exception {
        ResourceNameValidator validator = ResourceNameValidator.create(false, (IProject) null,
                ResourceType.ID);
        assertTrue(validator.isValid("foo") == null);
        assertTrue(validator.isValid(" foo") != null);
        assertTrue(validator.isValid("foo ") != null);
        assertTrue(validator.isValid("foo@") != null);
    }

    public void testUniqueOrExists() throws Exception {
        Set<String> existing = new HashSet<String>();
        existing.add("foo1");
        existing.add("foo2");
        existing.add("foo3");

        ResourceNameValidator validator = ResourceNameValidator.create(true, existing,
                ResourceType.ID);
        validator.unique();

        assertNull(validator.isValid("foo")); // null: ok (no error message)
        assertNull(validator.isValid("foo4"));
        assertNotNull(validator.isValid("foo1"));
        assertNotNull(validator.isValid("foo2"));
        assertNotNull(validator.isValid("foo3"));

        validator.exist();
        assertNotNull(validator.isValid("foo"));
        assertNotNull(validator.isValid("foo4"));
        assertNull(validator.isValid("foo1"));
        assertNull(validator.isValid("foo2"));
        assertNull(validator.isValid("foo3"));
    }
}
