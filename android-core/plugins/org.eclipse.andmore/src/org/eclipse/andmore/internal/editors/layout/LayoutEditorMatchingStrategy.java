/*
 * Copyright (C) 2008 The Android Open Source Project
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

package org.eclipse.andmore.internal.editors.layout;

import com.android.ide.common.resources.ResourceFolder;
import com.android.resources.ResourceFolderType;

import org.eclipse.andmore.internal.editors.common.CommonXmlEditor;
import org.eclipse.andmore.internal.resources.manager.ResourceManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorMatchingStrategy;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.FileEditorInput;

/**
 * Matching strategy for the Layout Editor. This is used to open all configurations of a layout
 * in the same editor.
 */
public class LayoutEditorMatchingStrategy implements IEditorMatchingStrategy {

    @Override
    public boolean matches(IEditorReference editorRef, IEditorInput input) {
        // first check that the file being opened is a layout file.
        if (input instanceof FileEditorInput) {
            FileEditorInput fileInput = (FileEditorInput)input;

            // get the IFile object and check it's in one of the layout folders.
            IFile file = fileInput.getFile();
            ResourceManager manager = ResourceManager.getInstance();
            ResourceFolder resFolder = manager.getResourceFolder(file);

            // Per the IEditorMatchingStrategy documentation, editorRef.getEditorInput()
            // is expensive so try exclude files that definitely don't match, such
            // as those with the wrong extension or wrong file name
            if (!file.getName().equals(editorRef.getName()) ||
                    !editorRef.getId().equals(CommonXmlEditor.ID)) {
                return false;
            }

            // if it's a layout, we now check the name of the fileInput against the name of the
            // file being currently edited by the editor since those are independent of the config.
            if (resFolder != null && resFolder.getType() == ResourceFolderType.LAYOUT) {
                try {
                    IEditorInput editorInput = editorRef.getEditorInput();
                    if (editorInput instanceof FileEditorInput) {
                        FileEditorInput editorFileInput = (FileEditorInput)editorInput;
                        IFile editorFile = editorFileInput.getFile();

                        ResourceFolder editorFolder = manager.getResourceFolder(editorFile);
                        if (editorFolder == null
                                || editorFolder.getType() != ResourceFolderType.LAYOUT) {
                            return false;
                        }

                        return editorFile.getProject().equals(file.getProject())
                            && editorFile.getName().equals(file.getName());
                    }
                } catch (PartInitException e) {
                    // we do nothing, we'll just return false.
                }
            }
        }
        return false;
    }
}
