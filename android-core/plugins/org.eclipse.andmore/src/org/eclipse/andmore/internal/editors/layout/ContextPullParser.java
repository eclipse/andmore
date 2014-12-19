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

package org.eclipse.andmore.internal.editors.layout;

import static com.android.SdkConstants.ATTR_IGNORE;
import static com.android.SdkConstants.ATTR_LAYOUT;
import static com.android.SdkConstants.ATTR_LAYOUT_HEIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_WIDTH;
import static com.android.SdkConstants.EXPANDABLE_LIST_VIEW;
import static com.android.SdkConstants.GRID_VIEW;
import static com.android.SdkConstants.LIST_VIEW;
import static com.android.SdkConstants.SPINNER;
import static com.android.SdkConstants.TOOLS_URI;
import static com.android.SdkConstants.VALUE_FILL_PARENT;
import static com.android.SdkConstants.VALUE_MATCH_PARENT;
import static com.android.SdkConstants.VIEW_FRAGMENT;
import static com.android.SdkConstants.VIEW_INCLUDE;
import static org.eclipse.andmore.internal.editors.layout.gle2.LayoutMetadata.KEY_FRAGMENT_LAYOUT;

import com.android.SdkConstants;
import com.android.ide.common.rendering.api.ILayoutPullParser;
import com.android.ide.common.rendering.api.IProjectCallback;
import com.android.ide.common.res2.ValueXmlHelper;
import com.google.common.collect.Maps;

import org.eclipse.andmore.internal.editors.layout.gle2.LayoutMetadata;
import org.kxml2.io.KXmlParser;

import java.io.File;
import java.util.Map;

/**
 * Modified {@link KXmlParser} that adds the methods of {@link ILayoutPullParser}, and
 * performs other layout-specific parser behavior like translating fragment tags into
 * include tags.
 * <p/>
 * It will return a given parser when queried for one through
 * {@link ILayoutPullParser#getParser(String)} for a given name.
 *
 */
public class ContextPullParser extends KXmlParser implements ILayoutPullParser {
    private static final String COMMENT_PREFIX = "<!--"; //$NON-NLS-1$
    private static final String COMMENT_SUFFIX = "-->"; //$NON-NLS-1$
    /** The callback to request parsers from */
    private final IProjectCallback mProjectCallback;
    /** The {@link File} for the layout currently being parsed */
    private File mFile;
    /** The layout to be shown for the current {@code <fragment>} tag. Usually null. */
    private String mFragmentLayout = null;

    /**
     * Creates a new {@link ContextPullParser}
     *
     * @param projectCallback the associated callback
     * @param file the file to be parsed
     */
    public ContextPullParser(IProjectCallback projectCallback, File file) {
        super();
        mProjectCallback = projectCallback;
        mFile = file;
    }

    // --- Layout lib API methods

    @Override
    /**
     * this is deprecated but must still be implemented for older layout libraries.
     * @deprecated use {@link IProjectCallback#getParser(String)}.
     */
    @Deprecated
    public ILayoutPullParser getParser(String layoutName) {
        return mProjectCallback.getParser(layoutName);
    }

    @Override
    public Object getViewCookie() {
        String name = super.getName();
        if (name == null) {
            return null;
        }

        // Store tools attributes if this looks like a layout we'll need adapter view
        // bindings for in the ProjectCallback.
        if (LIST_VIEW.equals(name)
                || EXPANDABLE_LIST_VIEW.equals(name)
                || GRID_VIEW.equals(name)
                || SPINNER.equals(name)) {
            Map<String, String> map = null;
            int count = getAttributeCount();
            for (int i = 0; i < count; i++) {
                String namespace = getAttributeNamespace(i);
                if (namespace != null && namespace.equals(TOOLS_URI)) {
                    String attribute = getAttributeName(i);
                    if (attribute.equals(ATTR_IGNORE)) {
                        continue;
                    }
                    if (map == null) {
                        map = Maps.newHashMapWithExpectedSize(4);
                    }
                    map.put(attribute, getAttributeValue(i));
                }
            }

            return map;
        }

        return null;
    }

    // --- KXMLParser override

    @Override
    public String getName() {
        String name = super.getName();

        // At designtime, replace fragments with includes.
        if (name.equals(VIEW_FRAGMENT)) {
            mFragmentLayout = LayoutMetadata.getProperty(this, KEY_FRAGMENT_LAYOUT);
            if (mFragmentLayout != null) {
                return VIEW_INCLUDE;
            }
        } else {
            mFragmentLayout = null;
        }


        return name;
    }

    @Override
    public String getAttributeValue(String namespace, String localName) {
        if (ATTR_LAYOUT.equals(localName) && mFragmentLayout != null) {
            return mFragmentLayout;
        }

        String value = super.getAttributeValue(namespace, localName);

        // on the fly convert match_parent to fill_parent for compatibility with older
        // platforms.
        if (VALUE_MATCH_PARENT.equals(value) &&
                (ATTR_LAYOUT_WIDTH.equals(localName) ||
                        ATTR_LAYOUT_HEIGHT.equals(localName)) &&
                SdkConstants.NS_RESOURCES.equals(namespace)) {
            return VALUE_FILL_PARENT;
        }

        // Handle unicode escapes etc
        value = ValueXmlHelper.unescapeResourceString(value, false, false);

        return value;
    }
}
