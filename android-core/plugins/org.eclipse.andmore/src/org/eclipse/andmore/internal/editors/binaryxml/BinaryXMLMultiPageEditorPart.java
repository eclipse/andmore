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

package org.eclipse.andmore.internal.editors.binaryxml;

import com.android.SdkConstants;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.internal.core.JarEntryFile;
import org.eclipse.jdt.internal.ui.javaeditor.JarEntryEditorInput;
import org.eclipse.ui.IEditorInput;
import org.eclipse.wst.xml.ui.internal.tabletree.XMLMultiPageEditorPart;

import java.io.File;

/**
 * The XML editor is an editor that open Android xml files from the android.jar file
 * <p>
 * The editor checks if the file is contained in jar and is so,
 * convert editor input to XmlStorageEditorInput that handles
 * corresponding file from Android SDK.
 *
 */
public class BinaryXMLMultiPageEditorPart extends XMLMultiPageEditorPart {

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.part.EditorPart#setInput(org.eclipse.ui.IEditorInput)
     */
    @Override
    protected void setInput(IEditorInput input) {
        if (input instanceof JarEntryEditorInput) {
            JarEntryEditorInput jarInput = (JarEntryEditorInput) input;
            IStorage storage = jarInput.getStorage();
            if (storage instanceof JarEntryFile) {
                JarEntryFile jarEntryFile = (JarEntryFile) storage;
                IPackageFragmentRoot fragmentRoot = jarEntryFile.getPackageFragmentRoot();
                if (fragmentRoot == null) {
                    super.setInput(input);
                    return;
                }
                IPath path = fragmentRoot.getPath();
                if (path == null) {
                    super.setInput(input);
                    return;
                }
                path = path.removeLastSegments(1);
                IPath filePath = path.append(SdkConstants.FD_DATA).append(
                        jarEntryFile.getFullPath().toPortableString());
                File file = new File(filePath.toOSString());
                if (!(file.isFile())) {
                    super.setInput(input);
                    return;
                }
                try {
                    XmlStorageEditorInput newInput = new XmlStorageEditorInput(
                            new FileStorage(file));
                    super.setInput(newInput);
                    return;
                } catch (Exception e) {
                    AdtPlugin.log(e, e.getMessage(), null);
                }
            }
        }
        super.setInput(input);
    }

}
