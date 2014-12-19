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
package org.eclipse.andmore.internal.editors.layout.gre;

import com.android.ide.common.api.IViewMetadata.FillPreference;

import java.util.Arrays;

import org.eclipse.andmore.internal.editors.layout.gre.ViewMetadataRepository.RenderMode;

import junit.framework.TestCase;

public class ViewMetadataRepositoryTest extends TestCase {
    public void testSingleton() throws Exception {
        assertSame(ViewMetadataRepository.get(), ViewMetadataRepository.get());
    }

    public void testBasic() throws Exception {
        ViewMetadataRepository repository = ViewMetadataRepository.get();

        assertEquals(FillPreference.WIDTH_IN_VERTICAL,
                repository.getFillPreference("android.widget.Spinner"));
        assertEquals(FillPreference.NONE,
                repository.getFillPreference("foo.bar"));
    }

    // Ensure that all basenames referenced in the metadata refer to other views in the file
    // (e.g. no typos)
    public void testRelatedTo() throws Exception {
        // Make sure unit tests are run with assertions on
        boolean assertionsEnabled = false;
        assert assertionsEnabled = true; // Intentional assignment
        assertTrue("This unit test must be run with assertions enabled (-ea)", assertionsEnabled);

        ViewMetadataRepository repository = ViewMetadataRepository.get();
        for (String fqcn : repository.getAllFqcns()) {
            repository.getRelatedTo(fqcn);
        }
    }

    public void testSkip() throws Exception {
        ViewMetadataRepository repository = ViewMetadataRepository.get();
        assertTrue(repository.getSkip("merge"));
        assertFalse(repository.getSkip("android.widget.Button"));
    }

    public void testRenderMode() throws Exception {
        ViewMetadataRepository repository = ViewMetadataRepository.get();
        assertEquals(RenderMode.NORMAL, repository.getRenderMode("android.widget.Button"));
        assertEquals(RenderMode.SKIP, repository.getRenderMode("android.widget.LinearLayout"));
        assertEquals(RenderMode.ALONE, repository.getRenderMode("android.widget.TabHost"));
    }

    public void testGetTopAttributes() throws Exception {
        ViewMetadataRepository repository = ViewMetadataRepository.get();
        assertEquals(Arrays.asList("id", "text", "style"),
                repository.getTopAttributes("android.widget.RadioButton"));
        assertEquals(Arrays.asList("id", "gravity", "paddingLeft", "paddingRight", "checkMark",
                "textAppearance"),
                repository.getTopAttributes("android.widget.CheckedTextView"));
        assertEquals(Arrays.asList("id"),
                repository.getTopAttributes("android.widget.NonExistent"));
    }

}
