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

package org.eclipse.andmore.internal.editors.common;

import static com.android.SdkConstants.FD_RES_LAYOUT;

import com.android.ide.common.resources.ResourceFolder;
import com.android.resources.ResourceFolderType;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.layout.LayoutEditorMatchingStrategy;
import org.eclipse.andmore.internal.preferences.AdtPrefs;
import org.eclipse.andmore.internal.resources.manager.ResourceManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorMatchingStrategy;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.FileEditorInput;

/**
 * Matching strategy for the editors.
 * This finds the right MatchingStrategy and delegates to it.
 */
public class CommonMatchingStrategy implements IEditorMatchingStrategy {

    @Override
    public boolean matches(IEditorReference editorRef, IEditorInput input) {
        if (input instanceof FileEditorInput) {
            FileEditorInput fileInput = (FileEditorInput)input;

            // get the IFile object and check it's in one of the layout folders.
            IFile file = fileInput.getFile();
            if (file.getParent().getName().startsWith(FD_RES_LAYOUT)) {
                ResourceFolder resFolder = ResourceManager.getInstance().getResourceFolder(file);
                if (resFolder != null && resFolder.getType() == ResourceFolderType.LAYOUT) {
                    if (AdtPrefs.getPrefs().isSharedLayoutEditor()) {
                        LayoutEditorMatchingStrategy m = new LayoutEditorMatchingStrategy();
                        return m.matches(editorRef, fileInput);
                    } else {
                        // Skip files that don't match by name (see below). However, for
                        // layout files we can't just use editorRef.getName(), since
                        // the name sometimes includes the parent folder name (when the
                        // files are in layout- folders.
                        if (!(editorRef.getName().endsWith(file.getName()) &&
                                editorRef.getId().equals(CommonXmlEditor.ID))) {
                            return false;
                        }
                    }
                }
            } else {
                // Per the IEditorMatchingStrategy documentation, editorRef.getEditorInput()
                // is expensive so try exclude files that definitely don't match, such
                // as those with the wrong extension or wrong file name
                if (!(file.getName().equals(editorRef.getName()) &&
                        editorRef.getId().equals(CommonXmlEditor.ID))) {
                    return false;
                }
            }

            try {
                return input.equals(editorRef.getEditorInput());
            } catch (PartInitException e) {
                AdtPlugin.log(e, null);
            }
        }

        return false;
    }
}
