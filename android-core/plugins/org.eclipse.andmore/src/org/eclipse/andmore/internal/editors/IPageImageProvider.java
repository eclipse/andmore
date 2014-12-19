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

package org.eclipse.andmore.internal.editors;

import org.eclipse.swt.graphics.Image;

/**
 * Interface that editor pages can implement to provide an icon
 * for the page tab in the XML editor.
 */
public interface IPageImageProvider {

    /**
     * Returns an {@link Image} that the editor will display in the page's tab.
     *
     * @return An {@link Image} for the editor tab for this page. Null for no image.
     */
    Image getPageImage();
}
