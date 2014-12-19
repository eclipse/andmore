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
package org.eclipse.andmore.internal.editors.layout.gle2;

/**
 * The {@linkplain RenderPreviewMode} records what type of configurations to
 * render in the layout editor
 */
public enum RenderPreviewMode {
    /** Generate a set of default previews with maximum variation */
    DEFAULT,

    /** Preview all the locales */
    LOCALES,

    /** Preview all the screen sizes */
    SCREENS,

    /** Preview layout as included in other layouts */
    INCLUDES,

    /** Preview all the variations of this layout */
    VARIATIONS,

    /** Show a manually configured set of previews */
    CUSTOM,

    /** No previews */
    NONE;
}
