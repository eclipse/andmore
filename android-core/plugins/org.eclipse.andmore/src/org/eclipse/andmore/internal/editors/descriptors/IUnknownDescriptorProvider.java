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

package org.eclipse.andmore.internal.editors.descriptors;

import org.eclipse.andmore.internal.editors.values.uimodel.UiItemElementNode;

/**
 * {@link UiItemElementNode} is the main class that creates the UI Model hierarchy based
 * on an XML DOM hierarchy, matching XML names to the {@link ElementDescriptor} names.
 * <p/>
 * This interface declares a provider that can provide an {@link ElementDescriptor}
 * for an unknown XML local name.
 */
public interface IUnknownDescriptorProvider {

    /**
     * Returns an instance of {@link ElementDescriptor} matching the given XML Local Name.
     *
     * @param xmlLocalName The XML local name.
     * @return A new or existing {@link ElementDescriptor} or derived instance. Must not be null.
     */
    ElementDescriptor getDescriptor(String xmlLocalName);

}
