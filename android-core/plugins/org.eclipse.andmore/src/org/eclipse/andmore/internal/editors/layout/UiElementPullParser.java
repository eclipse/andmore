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

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_LAYOUT;
import static com.android.SdkConstants.ATTR_LAYOUT_HEIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_WIDTH;
import static com.android.SdkConstants.ATTR_PADDING;
import static com.android.SdkConstants.AUTO_URI;
import static com.android.SdkConstants.UNIT_DIP;
import static com.android.SdkConstants.UNIT_DP;
import static com.android.SdkConstants.UNIT_IN;
import static com.android.SdkConstants.UNIT_MM;
import static com.android.SdkConstants.UNIT_PT;
import static com.android.SdkConstants.UNIT_PX;
import static com.android.SdkConstants.UNIT_SP;
import static com.android.SdkConstants.VALUE_FILL_PARENT;
import static com.android.SdkConstants.VALUE_MATCH_PARENT;
import static com.android.SdkConstants.VIEW_FRAGMENT;
import static com.android.SdkConstants.VIEW_INCLUDE;

import com.android.ide.common.rendering.api.ILayoutPullParser;
import com.android.ide.common.rendering.api.ViewInfo;
import com.android.ide.common.res2.ValueXmlHelper;
import com.android.resources.Density;
import com.android.sdklib.IAndroidTarget;

import org.eclipse.andmore.internal.editors.layout.descriptors.LayoutDescriptors;
import org.eclipse.andmore.internal.editors.layout.descriptors.ViewElementDescriptor;
import org.eclipse.andmore.internal.editors.layout.gle2.FragmentMenu;
import org.eclipse.andmore.internal.editors.uimodel.UiAttributeNode;
import org.eclipse.andmore.internal.editors.uimodel.UiElementNode;
import org.eclipse.andmore.internal.sdk.AndroidTargetData;
import org.eclipse.andmore.internal.sdk.Sdk;
import org.eclipse.core.resources.IProject;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xmlpull.v1.XmlPullParserException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link ILayoutPullParser} implementation on top of {@link UiElementNode}.
 * <p/>
 * It's designed to work on layout files, and will most likely not work on other resource files.
 * <p/>
 * This pull parser generates {@link ViewInfo}s which key is a {@link UiElementNode}.
 */
public class UiElementPullParser extends BasePullParser {
    private final static Pattern FLOAT_PATTERN = Pattern.compile("(-?[0-9]+(?:\\.[0-9]+)?)(.*)"); //$NON-NLS-1$

    private final int[] sIntOut = new int[1];

    private final ArrayList<UiElementNode> mNodeStack = new ArrayList<UiElementNode>();
    private UiElementNode mRoot;
    private final boolean mExplodedRendering;
    private boolean mZeroAttributeIsPadding = false;
    private boolean mIncreaseExistingPadding = false;
    private LayoutDescriptors mDescriptors;
    private final Density mDensity;

    /**
     * Number of pixels to pad views with in exploded-rendering mode.
     */
    private static final String DEFAULT_PADDING_VALUE =
        ExplodedRenderingHelper.PADDING_VALUE + UNIT_PX;

    /**
     * Number of pixels to pad exploded individual views with. (This is HALF the width of the
     * rectangle since padding is repeated on both sides of the empty content.)
     */
    private static final String FIXED_PADDING_VALUE = "20px"; //$NON-NLS-1$

    /**
     * Set of nodes that we want to auto-pad using {@link #FIXED_PADDING_VALUE} as the padding
     * attribute value. Can be null, which is the case when we don't want to perform any
     * <b>individual</b> node exploding.
     */
    private final Set<UiElementNode> mExplodeNodes;

    /**
     * Constructs a new {@link UiElementPullParser}, a parser dedicated to the special case of
     * parsing a layout resource files, and handling "exploded rendering" - adding padding on views
     * to make them easier to see and operate on.
     *
     * @param top The {@link UiElementNode} for the root node.
     * @param explodeRendering When true, add padding to <b>all</b> nodes in the hierarchy. This
     *            will add rather than replace padding of a node.
     * @param explodeNodes A set of individual nodes that should be assigned a fixed amount of
     *            padding ({@link #FIXED_PADDING_VALUE}). This is intended for use with nodes that
     *            (without padding) would be invisible. This parameter can be null, in which case
     *            nodes are not individually exploded (but they may all be exploded with the
     *            explodeRendering parameter.
     * @param density the density factor for the screen.
     * @param project Project containing this layout.
     */
    public UiElementPullParser(UiElementNode top, boolean explodeRendering,
            Set<UiElementNode> explodeNodes,
            Density density, IProject project) {
        super();
        mRoot = top;
        mExplodedRendering = explodeRendering;
        mExplodeNodes = explodeNodes;
        mDensity = density;
        if (mExplodedRendering) {
            // get the layout descriptor
            IAndroidTarget target = Sdk.getCurrent().getTarget(project);
            AndroidTargetData data = Sdk.getCurrent().getTargetData(target);
            mDescriptors = data.getLayoutDescriptors();
        }
        push(mRoot);
    }

    protected UiElementNode getCurrentNode() {
        if (mNodeStack.size() > 0) {
            return mNodeStack.get(mNodeStack.size()-1);
        }

        return null;
    }

    private Node getAttribute(int i) {
        if (mParsingState != START_TAG) {
            throw new IndexOutOfBoundsException();
        }

        // get the current uiNode
        UiElementNode uiNode = getCurrentNode();

        // get its xml node
        Node xmlNode = uiNode.getXmlNode();

        if (xmlNode != null) {
            return xmlNode.getAttributes().item(i);
        }

        return null;
    }

    private void push(UiElementNode node) {
        mNodeStack.add(node);

        mZeroAttributeIsPadding = false;
        mIncreaseExistingPadding = false;

        if (mExplodedRendering) {
            // first get the node name
            String xml = node.getDescriptor().getXmlLocalName();
            ViewElementDescriptor descriptor = mDescriptors.findDescriptorByTag(xml);
            if (descriptor != null) {
                NamedNodeMap attributes = node.getXmlNode().getAttributes();
                Node padding = attributes.getNamedItemNS(ANDROID_URI, ATTR_PADDING);
                if (padding == null) {
                    // we'll return an extra padding
                    mZeroAttributeIsPadding = true;
                } else {
                    mIncreaseExistingPadding = true;
                }
            }
        }
    }

    private UiElementNode pop() {
        return mNodeStack.remove(mNodeStack.size()-1);
    }

    // ------------- IXmlPullParser --------

    /**
     * {@inheritDoc}
     * <p/>
     * This implementation returns the underlying DOM node of type {@link UiElementNode}.
     * Note that the link between the GLE and the parsing code depends on this being the actual
     * type returned, so you can't just randomly change it here.
     * <p/>
     * Currently used by:
     * - private method GraphicalLayoutEditor#updateNodeWithBounds(ILayoutViewInfo).
     * - private constructor of LayoutCanvas.CanvasViewInfo.
     */
    @Override
    public Object getViewCookie() {
        return getCurrentNode();
    }

    /**
     * This implementation does nothing for now as all the embedded XML will use a normal KXML
     * parser.
     */
    @Override
    public ILayoutPullParser getParser(String layoutName) {
        return null;
    }

    // ------------- XmlPullParser --------

    @Override
    public String getPositionDescription() {
        return "XML DOM element depth:" + mNodeStack.size();
    }

    /*
     * This does not seem to be called by the layoutlib, but we keep this (and maintain
     * it) just in case.
     */
    @Override
    public int getAttributeCount() {
        UiElementNode node = getCurrentNode();

        if (node != null) {
            Collection<UiAttributeNode> attributes = node.getAllUiAttributes();
            int count = attributes.size();

            return count + (mZeroAttributeIsPadding ? 1 : 0);
        }

        return 0;
    }

    /*
     * This does not seem to be called by the layoutlib, but we keep this (and maintain
     * it) just in case.
     */
    @Override
    public String getAttributeName(int i) {
        if (mZeroAttributeIsPadding) {
            if (i == 0) {
                return ATTR_PADDING;
            } else {
                i--;
            }
        }

        Node attribute = getAttribute(i);
        if (attribute != null) {
            return attribute.getLocalName();
        }

        return null;
    }

    /*
     * This does not seem to be called by the layoutlib, but we keep this (and maintain
     * it) just in case.
     */
    @Override
    public String getAttributeNamespace(int i) {
        if (mZeroAttributeIsPadding) {
            if (i == 0) {
                return ANDROID_URI;
            } else {
                i--;
            }
        }

        Node attribute = getAttribute(i);
        if (attribute != null) {
            return attribute.getNamespaceURI();
        }
        return ""; //$NON-NLS-1$
    }

    /*
     * This does not seem to be called by the layoutlib, but we keep this (and maintain
     * it) just in case.
     */
    @Override
    public String getAttributePrefix(int i) {
        if (mZeroAttributeIsPadding) {
            if (i == 0) {
                // figure out the prefix associated with the android namespace.
                Document doc = mRoot.getXmlDocument();
                return doc.lookupPrefix(ANDROID_URI);
            } else {
                i--;
            }
        }

        Node attribute = getAttribute(i);
        if (attribute != null) {
            return attribute.getPrefix();
        }
        return null;
    }

    /*
     * This does not seem to be called by the layoutlib, but we keep this (and maintain
     * it) just in case.
     */
    @Override
    public String getAttributeValue(int i) {
        if (mZeroAttributeIsPadding) {
            if (i == 0) {
                return DEFAULT_PADDING_VALUE;
            } else {
                i--;
            }
        }

        Node attribute = getAttribute(i);
        if (attribute != null) {
            String value = attribute.getNodeValue();
            if (mIncreaseExistingPadding && ATTR_PADDING.equals(attribute.getLocalName()) &&
                    ANDROID_URI.equals(attribute.getNamespaceURI())) {
                // add the padding and return the value
                return addPaddingToValue(value);
            }
            return value;
        }

        return null;
    }

    /*
     * This is the main method used by the LayoutInflater to query for attributes.
     */
    @Override
    public String getAttributeValue(String namespace, String localName) {
        if (mExplodeNodes != null && ATTR_PADDING.equals(localName) &&
                ANDROID_URI.equals(namespace)) {
            UiElementNode node = getCurrentNode();
            if (node != null && mExplodeNodes.contains(node)) {
                return FIXED_PADDING_VALUE;
            }
        }

        if (mZeroAttributeIsPadding && ATTR_PADDING.equals(localName) &&
                ANDROID_URI.equals(namespace)) {
            return DEFAULT_PADDING_VALUE;
        }

        // get the current uiNode
        UiElementNode uiNode = getCurrentNode();

        // get its xml node
        Node xmlNode = uiNode.getXmlNode();

        if (xmlNode != null) {
            if (ATTR_LAYOUT.equals(localName) && VIEW_FRAGMENT.equals(xmlNode.getNodeName())) {
                String layout = FragmentMenu.getFragmentLayout(xmlNode);
                if (layout != null) {
                    return layout;
                }
            }

            Node attribute = xmlNode.getAttributes().getNamedItemNS(namespace, localName);

            // Auto-convert http://schemas.android.com/apk/res-auto resources. The lookup
            // will be for the current application's resource package, e.g.
            // http://schemas.android.com/apk/res/foo.bar, but the XML document will
            // be using http://schemas.android.com/apk/res-auto in library projects:
            if (attribute == null && namespace != null && !namespace.equals(ANDROID_URI)) {
                attribute = xmlNode.getAttributes().getNamedItemNS(AUTO_URI, localName);
            }

            if (attribute != null) {
                String value = attribute.getNodeValue();
                if (mIncreaseExistingPadding && ATTR_PADDING.equals(localName) &&
                        ANDROID_URI.equals(namespace)) {
                    // add the padding and return the value
                    return addPaddingToValue(value);
                }

                // on the fly convert match_parent to fill_parent for compatibility with older
                // platforms.
                if (VALUE_MATCH_PARENT.equals(value) &&
                        (ATTR_LAYOUT_WIDTH.equals(localName) ||
                                ATTR_LAYOUT_HEIGHT.equals(localName)) &&
                        ANDROID_URI.equals(namespace)) {
                    return VALUE_FILL_PARENT;
                }

                // Handle unicode escapes etc
                value = ValueXmlHelper.unescapeResourceString(value, false, false);

                return value;
            }
        }

        return null;
    }

    @Override
    public int getDepth() {
        return mNodeStack.size();
    }

    @Override
    public String getName() {
        if (mParsingState == START_TAG || mParsingState == END_TAG) {
            String name = getCurrentNode().getDescriptor().getXmlLocalName();

            if (name.equals(VIEW_FRAGMENT)) {
                // Temporarily translate <fragment> to <include> (and in getAttribute
                // we will also provide a layout-attribute for the corresponding
                // fragment name attribute)
                String layout = FragmentMenu.getFragmentLayout(getCurrentNode().getXmlNode());
                if (layout != null) {
                    return VIEW_INCLUDE;
                }
            }

            return name;
        }

        return null;
    }

    @Override
    public String getNamespace() {
        if (mParsingState == START_TAG || mParsingState == END_TAG) {
            return getCurrentNode().getDescriptor().getNamespace();
        }

        return null;
    }

    @Override
    public String getPrefix() {
        if (mParsingState == START_TAG || mParsingState == END_TAG) {
            Document doc = mRoot.getXmlDocument();
            return doc.lookupPrefix(getCurrentNode().getDescriptor().getNamespace());
        }

        return null;
    }

    @Override
    public boolean isEmptyElementTag() throws XmlPullParserException {
        if (mParsingState == START_TAG) {
            return getCurrentNode().getUiChildren().size() == 0;
        }

        throw new XmlPullParserException("Call to isEmptyElementTag while not in START_TAG",
                this, null);
    }

    @Override
    public void onNextFromStartDocument() {
        onNextFromStartTag();
    }

    @Override
    public void onNextFromStartTag() {
        // get the current node, and look for text or children (children first)
        UiElementNode node = getCurrentNode();
        List<UiElementNode> children = node.getUiChildren();
        if (children.size() > 0) {
            // move to the new child, and don't change the state.
            push(children.get(0));

            // in case the current state is CURRENT_DOC, we set the proper state.
            mParsingState = START_TAG;
        } else {
            if (mParsingState == START_DOCUMENT) {
                // this handles the case where there's no node.
                mParsingState = END_DOCUMENT;
            } else {
                mParsingState = END_TAG;
            }
        }
    }

    @Override
    public void onNextFromEndTag() {
        // look for a sibling. if no sibling, go back to the parent
        UiElementNode node = getCurrentNode();
        node = node.getUiNextSibling();
        if (node != null) {
            // to go to the sibling, we need to remove the current node,
            pop();
            // and add its sibling.
            push(node);
            mParsingState = START_TAG;
        } else {
            // move back to the parent
            pop();

            // we have only one element left (mRoot), then we're done with the document.
            if (mNodeStack.size() == 1) {
                mParsingState = END_DOCUMENT;
            } else {
                mParsingState = END_TAG;
            }
        }
    }

    // ------- TypedValue stuff
    // This is adapted from com.android.layoutlib.bridge.ResourceHelper
    // (but modified to directly take the parsed value and convert it into pixel instead of
    // storing it into a TypedValue)
    // this was originally taken from platform/frameworks/base/libs/utils/ResourceTypes.cpp

    private static final class DimensionEntry {
        String name;
        int type;

        DimensionEntry(String name, int unit) {
            this.name = name;
            this.type = unit;
        }
    }

    /** {@link DimensionEntry} complex unit: Value is raw pixels. */
    private static final int COMPLEX_UNIT_PX = 0;
    /** {@link DimensionEntry} complex unit: Value is Device Independent
     *  Pixels. */
    private static final int COMPLEX_UNIT_DIP = 1;
    /** {@link DimensionEntry} complex unit: Value is a scaled pixel. */
    private static final int COMPLEX_UNIT_SP = 2;
    /** {@link DimensionEntry} complex unit: Value is in points. */
    private static final int COMPLEX_UNIT_PT = 3;
    /** {@link DimensionEntry} complex unit: Value is in inches. */
    private static final int COMPLEX_UNIT_IN = 4;
    /** {@link DimensionEntry} complex unit: Value is in millimeters. */
    private static final int COMPLEX_UNIT_MM = 5;

    private final static DimensionEntry[] sDimensions = new DimensionEntry[] {
        new DimensionEntry(UNIT_PX, COMPLEX_UNIT_PX),
        new DimensionEntry(UNIT_DIP, COMPLEX_UNIT_DIP),
        new DimensionEntry(UNIT_DP, COMPLEX_UNIT_DIP),
        new DimensionEntry(UNIT_SP, COMPLEX_UNIT_SP),
        new DimensionEntry(UNIT_PT, COMPLEX_UNIT_PT),
        new DimensionEntry(UNIT_IN, COMPLEX_UNIT_IN),
        new DimensionEntry(UNIT_MM, COMPLEX_UNIT_MM),
    };

    /**
     * Adds padding to an existing dimension.
     * <p/>This will resolve the attribute value (which can be px, dip, dp, sp, pt, in, mm) to
     * a pixel value, add the padding value ({@link ExplodedRenderingHelper#PADDING_VALUE}),
     * and then return a string with the new value as a px string ("42px");
     * If the conversion fails, only the special padding is returned.
     */
    private String addPaddingToValue(String s) {
        int padding = ExplodedRenderingHelper.PADDING_VALUE;
        if (stringToPixel(s)) {
            padding += sIntOut[0];
        }

        return padding + UNIT_PX;
    }

    /**
     * Convert the string into a pixel value, and puts it in {@link #sIntOut}
     * @param s the dimension value from an XML attribute
     * @return true if success.
     */
    private boolean stringToPixel(String s) {
        // remove the space before and after
        s = s.trim();
        int len = s.length();

        if (len <= 0) {
            return false;
        }

        // check that there's no non ASCII characters.
        char[] buf = s.toCharArray();
        for (int i = 0 ; i < len ; i++) {
            if (buf[i] > 255) {
                return false;
            }
        }

        // check the first character
        if (buf[0] < '0' && buf[0] > '9' && buf[0] != '.') {
            return false;
        }

        // now look for the string that is after the float...
        Matcher m = FLOAT_PATTERN.matcher(s);
        if (m.matches()) {
            String f_str = m.group(1);
            String end = m.group(2);

            float f;
            try {
                f = Float.parseFloat(f_str);
            } catch (NumberFormatException e) {
                // this shouldn't happen with the regexp above.
                return false;
            }

            if (end.length() > 0 && end.charAt(0) != ' ') {
                // We only support dimension-type values, so try to parse the unit for dimension
                DimensionEntry dimension = parseDimension(end);
                if (dimension != null) {
                    // convert the value into pixel based on the dimention type
                    // This is similar to TypedValue.applyDimension()
                    switch (dimension.type) {
                        case COMPLEX_UNIT_PX:
                            // do nothing, value is already in px
                            break;
                        case COMPLEX_UNIT_DIP:
                        case COMPLEX_UNIT_SP: // intended fall-through since we don't
                                              // adjust for font size
                            f *= (float)mDensity.getDpiValue() / Density.DEFAULT_DENSITY;
                            break;
                        case COMPLEX_UNIT_PT:
                            f *= mDensity.getDpiValue() * (1.0f / 72);
                            break;
                        case COMPLEX_UNIT_IN:
                            f *= mDensity.getDpiValue();
                            break;
                        case COMPLEX_UNIT_MM:
                            f *= mDensity.getDpiValue() * (1.0f / 25.4f);
                            break;
                    }

                    // store result (converted to int)
                    sIntOut[0] = (int) (f + 0.5);

                    return true;
                }
            }
        }

        return false;
    }

    private static DimensionEntry parseDimension(String str) {
        str = str.trim();

        for (DimensionEntry d : sDimensions) {
            if (d.name.equals(str)) {
                return d;
            }
        }

        return null;
    }
}
