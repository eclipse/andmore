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

import static com.android.SdkConstants.ANDROID_NS_NAME_PREFIX;
import static com.android.SdkConstants.ANDROID_URI;

import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.andmore.internal.editors.layout.descriptors.ViewElementDescriptor;
import org.eclipse.andmore.internal.editors.layout.gle2.SimpleAttribute;
import org.eclipse.andmore.internal.editors.layout.gle2.SimpleElement;
import org.eclipse.swt.graphics.Image;
import org.w3c.dom.Element;

/**
 * Special version of {@link ViewElementDescriptor} which is initialized by the palette
 * with specific metadata for how to instantiate particular variations of an existing
 * {@link ViewElementDescriptor} with initial values.
 */
public class PaletteMetadataDescriptor extends ViewElementDescriptor {
    private String mInitString;
    private String mIconName;

    public PaletteMetadataDescriptor(ViewElementDescriptor descriptor, String displayName,
            String initString, String iconName) {
        super(descriptor.getXmlName(),
                displayName,
                descriptor.getFullClassName(),
                descriptor.getTooltip(),
                descriptor.getSdkUrl(),
                descriptor.getAttributes(),
                descriptor.getLayoutAttributes(),
                descriptor.getChildren(), descriptor.getMandatory() == Mandatory.MANDATORY);
        mInitString = initString;
        mIconName = iconName;
        setSuperClass(descriptor.getSuperClassDesc());
    }

    /**
     * Returns a String which contains a comma separated list of name=value tokens,
     * where the name can start with "android:" to indicate a property in the android namespace,
     * or no prefix for plain attributes.
     *
     * @return the initialization string, which can be empty but never null
     */
    public String getInitializedAttributes() {
        return mInitString != null ? mInitString : ""; //$NON-NLS-1$
    }

    @Override
    public Image getGenericIcon() {
        if (mIconName != null) {
            IconFactory factory = IconFactory.getInstance();
            Image icon = factory.getIcon(mIconName);
            if (icon != null) {
                return icon;
            }
        }

        return super.getGenericIcon();
    }

    /**
     * Initializes a new {@link SimpleElement} with the palette initialization
     * configuration
     *
     * @param element the new element to initialize
     */
    public void initializeNew(SimpleElement element) {
        initializeNew(element, null);
    }

    /**
     * Initializes a new {@link Element} with the palette initialization configuration
     *
     * @param element the new element to initialize
     */
    public void initializeNew(Element element) {
        initializeNew(null, element);
    }

    private void initializeNew(SimpleElement simpleElement, Element domElement) {
        String initializedAttributes = mInitString;
        if (initializedAttributes != null && initializedAttributes.length() > 0) {
            for (String s : initializedAttributes.split(",")) { //$NON-NLS-1$
                String[] nameValue = s.split("="); //$NON-NLS-1$
                String name = nameValue[0];
                String value = nameValue[1];
                String nameSpace = ""; //$NON-NLS-1$
                if (name.startsWith(ANDROID_NS_NAME_PREFIX)) {
                    name = name.substring(ANDROID_NS_NAME_PREFIX.length());
                    nameSpace = ANDROID_URI;
                }

                if (simpleElement != null) {
                    SimpleAttribute attr = new SimpleAttribute(nameSpace, name, value);
                    simpleElement.addAttribute(attr);
                }

                if (domElement != null) {
                    domElement.setAttributeNS(nameSpace, name, value);
                }
            }
        }
    }
}
