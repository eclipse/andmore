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

package org.eclipse.andmore.internal.editors;


import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.wst.xml.ui.views.contentoutline.XMLContentOutlineConfiguration;

/**
 * Custom version of {@link XMLContentOutlineConfiguration} which adds in icons and
 * details such as id or name, to the labels.
 */
public class AndroidOutlineConfiguration extends XMLContentOutlineConfiguration {
    /** Constructs a new {@link AndroidOutlineConfiguration} */
    public AndroidOutlineConfiguration() {
    }

    @Override
    public ILabelProvider getLabelProvider(TreeViewer viewer) {
        return new OutlineLabelProvider();
    }
}
