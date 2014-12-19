/*
 * Copyright (C) 2007 The Android Open Source Project
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

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.api.IAttributeInfo;
import com.android.ide.common.api.IAttributeInfo.Format;

import org.eclipse.andmore.internal.editors.ui.TextValueCellEditor;
import org.eclipse.andmore.internal.editors.uimodel.UiAttributeNode;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.andmore.internal.editors.uimodel.UiTextAttributeNode;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.views.properties.IPropertyDescriptor;

import java.util.EnumSet;
import java.util.Locale;


/**
 * Describes a textual XML attribute.
 * <p/>
 * Such an attribute has a tooltip and would typically be displayed by
 * {@link UiTextAttributeNode} using a label widget and text field.
 * <p/>
 * This is the "default" kind of attribute. If in doubt, use this.
 */
public class TextAttributeDescriptor extends AttributeDescriptor implements IPropertyDescriptor {
    public static final String DEPRECATED_CATEGORY = "Deprecated";

    private String mUiName;
    private String mTooltip;
    private boolean mRequired;

    /**
     * Creates a new {@link TextAttributeDescriptor}
     *
     * @param xmlLocalName The XML name of the attribute (case sensitive)
     * @param nsUri The URI of the attribute. Can be null if attribute has no namespace.
     *              See {@link SdkConstants#NS_RESOURCES} for a common value.
     * @param attrInfo The {@link IAttributeInfo} of this attribute. Can't be null.
     */
    public TextAttributeDescriptor(
            String xmlLocalName,
            String nsUri,
            IAttributeInfo attrInfo) {
        super(xmlLocalName, nsUri, attrInfo);
    }

    /**
     * @return The UI name of the attribute. Cannot be an empty string and cannot be null.
     */
    @NonNull
    public String getUiName() {
        if (mUiName == null) {
            IAttributeInfo info = getAttributeInfo();
            if (info != null) {
                mUiName = DescriptorsUtils.prettyAttributeUiName(info.getName());
                if (mRequired) {
                    mUiName += "*"; //$NON-NLS-1$
                }
            } else {
                mUiName = getXmlLocalName();
            }
        }

        return mUiName;
    }


    /**
     * Sets the UI name to be associated with this descriptor. This is usually
     * computed lazily from the {@link #getAttributeInfo()} data, but for some
     * hardcoded/builtin descriptor this is manually initialized.
     *
     * @param uiName the new UI name to be used
     * @return this, for constructor setter chaining
     */
    public TextAttributeDescriptor setUiName(String uiName) {
        mUiName = uiName;

        return this;
    }

    /**
     * Sets the tooltip to be associated with this descriptor. This is usually
     * computed lazily from the {@link #getAttributeInfo()} data, but for some
     * hardcoded/builtin descriptor this is manually initialized.
     *
     * @param tooltip the new tooltip to be used
     * @return this, for constructor setter chaining
     */
    public TextAttributeDescriptor setTooltip(String tooltip) {
        mTooltip = tooltip;

        return this;
    }

    /**
     * Sets whether this attribute is required
     *
     * @param required whether this attribute is required
     * @return this, for constructor setter chaining
     */
    public TextAttributeDescriptor setRequired(boolean required) {
        mRequired = required;

        return this;
    }

    /**
     * Returns whether this attribute is required
     *
     * @return whether this attribute is required
     */
    public boolean isRequired() {
        return mRequired;
    }

    /**
     * The tooltip string is either null or a non-empty string.
     * <p/>
     * The tooltip is based on the Javadoc of the attribute and already processed via
     * {@link DescriptorsUtils#formatTooltip(String)} to be displayed right away as
     * a UI tooltip.
     * <p/>
     * An empty string is converted to null, to match the behavior of setToolTipText() in
     * {@link Control}.
     *
     * @return A non-empty tooltip string or null
     */
    @Nullable
    public String getTooltip() {
        if (mTooltip == null) {
            IAttributeInfo info = getAttributeInfo();
            if (info == null) {
                mTooltip = "";
                return mTooltip;
            }

            String tooltip = null;
            String rawTooltip = info.getJavaDoc();
            if (rawTooltip == null) {
                rawTooltip = "";
            }

            String deprecated = info.getDeprecatedDoc();
            if (deprecated != null) {
                if (rawTooltip.length() > 0) {
                    rawTooltip += "@@"; //$NON-NLS-1$ insert a break
                }
                rawTooltip += "* Deprecated";
                if (deprecated.length() != 0) {
                    rawTooltip += ": " + deprecated;                            //$NON-NLS-1$
                }
                if (deprecated.length() == 0 || !deprecated.endsWith(".")) {    //$NON-NLS-1$
                    rawTooltip += ".";                                          //$NON-NLS-1$
                }
            }

            // Add the known types to the tooltip
            EnumSet<Format> formats_list = info.getFormats();
            int flen = formats_list.size();
            if (flen > 0) {
                StringBuilder sb = new StringBuilder();
                if (rawTooltip != null && rawTooltip.length() > 0) {
                    sb.append(rawTooltip);
                    sb.append(" ");     //$NON-NLS-1$
                }
                if (sb.length() > 0) {
                    sb.append("@@");    //$NON-NLS-1$  @@ inserts a break before the types
                }
                sb.append("[");         //$NON-NLS-1$
                boolean isFirst = true;
                for (Format f : formats_list) {
                    if (isFirst) {
                        isFirst = false;
                    } else {
                        sb.append(", ");
                    }
                    sb.append(f.toString().toLowerCase(Locale.US));
                }
                // The extra space at the end makes the tooltip more readable on Windows.
                sb.append("]"); //$NON-NLS-1$

                if (mRequired) {
                    // Note: this string is split in 2 to make it translatable.
                    sb.append(".@@");          //$NON-NLS-1$ @@ inserts a break and is not translatable
                    sb.append("* Required.");
                }

                // The extra space at the end makes the tooltip more readable on Windows.
                sb.append(" "); //$NON-NLS-1$

                rawTooltip = sb.toString();
                tooltip = DescriptorsUtils.formatTooltip(rawTooltip);
            }

            if (tooltip == null) {
                tooltip = DescriptorsUtils.formatTooltip(rawTooltip);
            }
            mTooltip = tooltip;
        }

        return mTooltip.isEmpty() ? null : mTooltip;
    }

    /**
     * @return A new {@link UiTextAttributeNode} linked to this descriptor.
     */
    @Override
    public UiAttributeNode createUiNode(UiElementNode uiParent) {
        return new UiTextAttributeNode(this, uiParent);
    }

    // ------- IPropertyDescriptor Methods

    @Override
    public CellEditor createPropertyEditor(Composite parent) {
        return new TextValueCellEditor(parent);
    }

    @Override
    public String getCategory() {
        if (isDeprecated()) {
            return DEPRECATED_CATEGORY;
        }

        ElementDescriptor parent = getParent();
        if (parent != null) {
            return parent.getUiName();
        }

        return null;
    }

    @Override
    public String getDescription() {
        return getTooltip();
    }

    @Override
    public String getDisplayName() {
        return getUiName();
    }

    @Override
    public String[] getFilterFlags() {
        return null;
    }

    @Override
    public Object getHelpContextIds() {
        return null;
    }

    @Override
    public Object getId() {
        return this;
    }

    @Override
    public ILabelProvider getLabelProvider() {
        return AttributeDescriptorLabelProvider.getProvider();
    }

    @Override
    public boolean isCompatibleWith(IPropertyDescriptor anotherProperty) {
        return anotherProperty == this;
    }
}
