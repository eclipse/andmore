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

import com.android.SdkConstants;
import com.android.ide.common.rendering.api.ILayoutPullParser;
import com.android.layoutlib.api.ILayoutResult.ILayoutViewInfo;

import org.eclipse.andmore.AdtConstants;
import org.eclipse.andmore.internal.editors.layout.descriptors.ViewElementDescriptor;
import org.xmlpull.v1.XmlPullParserException;

/**
 * {@link ILayoutPullParser} implementation to render android widget bitmap.
 * <p/>
 * The parser emulates a layout that contains just one widget, described by the
 * {@link ViewElementDescriptor} passed in the constructor.
 * <p/>
 * This pull parser generates {@link ILayoutViewInfo}s which key is a {@link ViewElementDescriptor}.
 */
public class WidgetPullParser extends BasePullParser {

    private final ViewElementDescriptor mDescriptor;
    private String[][] mAttributes = new String[][] {
            { "text", null },
            { "layout_width", "wrap_content" },
            { "layout_height", "wrap_content" },
    };

    public WidgetPullParser(ViewElementDescriptor descriptor) {
        mDescriptor = descriptor;

        String[] segments = mDescriptor.getFullClassName().split(AdtConstants.RE_DOT);
        mAttributes[0][1] = segments[segments.length-1];
    }

    @Override
    public Object getViewCookie() {
        // we need a viewKey or the ILayoutResult will not contain any ILayoutViewInfo
        return mDescriptor;
    }

    /**
     * Legacy method required by {@link com.android.layoutlib.api.IXmlPullParser}
     */
    @Override
    public Object getViewKey() {
        return getViewCookie();
    }

    @Override
    public ILayoutPullParser getParser(String layoutName) {
        // there's no embedded layout for a single widget.
        return null;
    }

    @Override
    public int getAttributeCount() {
        return mAttributes.length; // text attribute
    }

    @Override
    public String getAttributeName(int index) {
        if (index < mAttributes.length) {
            return mAttributes[index][0];
        }

        return null;
    }

    @Override
    public String getAttributeNamespace(int index) {
        return SdkConstants.NS_RESOURCES;
    }

    @Override
    public String getAttributePrefix(int index) {
        // pass
        return null;
    }

    @Override
    public String getAttributeValue(int index) {
        if (index < mAttributes.length) {
            return mAttributes[index][1];
        }

        return null;
    }

    @Override
    public String getAttributeValue(String ns, String name) {
        if (SdkConstants.NS_RESOURCES.equals(ns)) {
            for (String[] attribute : mAttributes) {
                if (name.equals(attribute[0])) {
                    return attribute[1];
                }
            }
        }

        return null;
    }

    @Override
    public int getDepth() {
        // pass
        return 0;
    }

    @Override
    public String getName() {
        return mDescriptor.getXmlLocalName();
    }

    @Override
    public String getNamespace() {
        // pass
        return null;
    }

    @Override
    public String getPositionDescription() {
        // pass
        return null;
    }

    @Override
    public String getPrefix() {
        // pass
        return null;
    }

    @Override
    public boolean isEmptyElementTag() throws XmlPullParserException {
        if (mParsingState == START_TAG) {
            return true;
        }

        throw new XmlPullParserException("Call to isEmptyElementTag while not in START_TAG",
                this, null);
    }

    @Override
    public void onNextFromStartDocument() {
        // just go to start_tag
        mParsingState = START_TAG;
    }

    @Override
    public void onNextFromStartTag() {
        // since we have no children, just go to end_tag
        mParsingState = END_TAG;
    }

    @Override
    public void onNextFromEndTag() {
        // just one tag. we are done.
        mParsingState = END_DOCUMENT;
    }
}
