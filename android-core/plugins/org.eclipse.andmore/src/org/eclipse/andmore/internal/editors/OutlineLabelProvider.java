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

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ATTR_LAYOUT;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.ATTR_SRC;
import static com.android.SdkConstants.ATTR_TEXT;
import static com.android.SdkConstants.DRAWABLE_PREFIX;
import static com.android.SdkConstants.LAYOUT_RESOURCE_PREFIX;
import static com.android.SdkConstants.VIEW;
import static com.android.SdkConstants.VIEW_TAG;

import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.xml.ui.internal.contentoutline.JFaceNodeLabelProvider;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

/**
 * Label provider for the XML outlines and quick outlines: Use our own icons,
 * when available, and and include the most important attribute (id, name, or
 * text)
 */
@SuppressWarnings("restriction")
// XML UI API
class OutlineLabelProvider extends JFaceNodeLabelProvider {
    @Override
    public Image getImage(Object element) {
        if (element instanceof Element) {
            Element e = (Element) element;
            String tagName = e.getTagName();
            if (VIEW_TAG.equals(tagName)) {
                // Can't have both view.png and View.png; issues on case sensitive vs
                // case insensitive file systems
                tagName = VIEW;
            }
            IconFactory factory = IconFactory.getInstance();
            Image img = factory.getIcon(tagName, null);
            if (img != null) {
                return img;
            }
        }
        return super.getImage(element);
    }

    @Override
    public String getText(Object element) {
        String text = super.getText(element);
        if (element instanceof Element) {
            Element e = (Element) element;
            String id = getAttributeNS(e, ANDROID_URI, ATTR_ID);
            if (id == null || id.length() == 0) {
                id = getAttributeNS(e, ANDROID_URI, ATTR_NAME);
                if (id == null || id.length() == 0) {
                    id = e.getAttribute(ATTR_NAME);
                    if (id == null || id.length() == 0) {
                        id = getAttributeNS(e, ANDROID_URI, ATTR_TEXT);
                        if (id != null && id.length() > 15) {
                            id = id.substring(0, 12) + "...";
                        }
                        if (id == null || id.length() == 0) {
                            id = getAttributeNS(e, ANDROID_URI, ATTR_SRC);
                            if (id != null && id.length() > 0) {
                                if (id.startsWith(DRAWABLE_PREFIX)) {
                                    id = id.substring(DRAWABLE_PREFIX.length());
                                }
                            } else {
                                id = e.getAttribute(ATTR_LAYOUT);
                                if (id != null && id.length() > 0) {
                                    if (id.startsWith(LAYOUT_RESOURCE_PREFIX)) {
                                        id = id.substring(LAYOUT_RESOURCE_PREFIX.length());
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (id != null && id.length() > 0) {
                return text + ": " + id; //$NON-NLS-1$
            }
        }
        return text;
    }

    /**
     * Wrapper around {@link Element#getAttributeNS(String, String)}.
     * <p/>
     * The implementation used in Eclipse's XML editor sometimes internally
     * throws an NPE instead of politely returning null.
     *
     * @see Element#getAttributeNS(String, String)
     */
    private String getAttributeNS(Element e, String uri, String name) throws DOMException {
        try {
            return e.getAttributeNS(uri, name);
        } catch (NullPointerException ignore) {
            return null;
        }
    }
}
