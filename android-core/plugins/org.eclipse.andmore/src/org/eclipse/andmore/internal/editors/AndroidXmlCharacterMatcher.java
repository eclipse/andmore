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
package org.eclipse.andmore.internal.editors;

import static org.eclipse.wst.xml.core.internal.regions.DOMRegionContext.XML_EMPTY_TAG_CLOSE;
import static org.eclipse.wst.xml.core.internal.regions.DOMRegionContext.XML_END_TAG_OPEN;
import static org.eclipse.wst.xml.core.internal.regions.DOMRegionContext.XML_TAG_CLOSE;
import static org.eclipse.wst.xml.core.internal.regions.DOMRegionContext.XML_TAG_NAME;
import static org.eclipse.wst.xml.core.internal.regions.DOMRegionContext.XML_TAG_OPEN;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionList;
import org.eclipse.wst.xml.ui.internal.text.XMLDocumentRegionEdgeMatcher;

/**
 * Custom version of the character matcher for XML files which adds the ability to
 * jump between open and close tags in the XML file.
 */
@SuppressWarnings("restriction")
public class AndroidXmlCharacterMatcher extends XMLDocumentRegionEdgeMatcher {
    /**
     * Constructs a new character matcher for Android XML files
     */
    public AndroidXmlCharacterMatcher() {
    }

    @Override
    public IRegion match(IDocument doc, int offset) {
        if (offset < 0 || offset >= doc.getLength()) {
            return null;
        }

        IRegion match = findOppositeTag(doc, offset);
        if (match != null) {
            return match;
        }

        return super.match(doc, offset);
    }

    private IRegion findOppositeTag(IDocument document, int offset) {
        if (!(document instanceof IStructuredDocument)) {
            return null;
        }
        IStructuredDocument doc = (IStructuredDocument) document;

        IStructuredDocumentRegion region = doc.getRegionAtCharacterOffset(offset);
        if (region == null) {
            return null;
        }

        ITextRegion subRegion = region.getRegionAtCharacterOffset(offset);
        if (subRegion == null) {
            return null;
        }
        ITextRegionList subRegions = region.getRegions();
        int index = subRegions.indexOf(subRegion);

        String type = subRegion.getType();
        boolean isOpenTag = false;
        boolean isCloseTag = false;

        if (type.equals(XML_TAG_OPEN)) {
            isOpenTag = true;
        } else if (type.equals(XML_END_TAG_OPEN)) {
            isCloseTag = true;
        } else if (!(type.equals(XML_TAG_CLOSE) || type.equals(XML_TAG_NAME)) &&
                (subRegion.getStart() + region.getStartOffset() == offset)) {
            // Look to the left one character; we may have the case where you're
            // pointing to the right of a tag, e.g.
            //     <foo>^text
            offset--;
            region = doc.getRegionAtCharacterOffset(offset);
            if (region == null) {
                return null;
            }
            subRegion = region.getRegionAtCharacterOffset(offset);
            if (subRegion == null) {
                return null;
            }
            type = subRegion.getType();

            subRegions = region.getRegions();
            index = subRegions.indexOf(subRegion);
        }

        if (type.equals(XML_TAG_CLOSE) || type.equals(XML_TAG_NAME)) {
            for (int i = index; i >= 0; i--) {
                subRegion = subRegions.get(i);
                type = subRegion.getType();
                if (type.equals(XML_TAG_OPEN)) {
                    isOpenTag = true;
                    break;
                } else if (type.equals(XML_END_TAG_OPEN)) {
                    isCloseTag = true;
                    break;
                }
            }
        }

        if (isOpenTag) {
            // Find closing tag
            int target = findTagForwards(doc, subRegion.getStart() + region.getStartOffset(), 0);
            // Note - there is no point in looking up the whole region for the matching
            // tag, because even if you pass a length greater than 1 here, the paint highlighter
            // will only highlight a single character -- the *last* character of the region,
            // not the whole region itself.
            return new Region(target, 1);
        } else if (isCloseTag) {
            // Find open tag
            int target = findTagBackwards(doc, subRegion.getStart() + region.getStartOffset(), -1);
            return new Region(target, 1);
        }

        return null;
    }

    /**
     * Finds the corresponding open tag by searching backwards until the tag balance
     * reaches a given target.
     *
     * @param doc the document
     * @param offset the ending offset (where the search begins searching backwards from)
     * @param targetTagBalance the balance to end the search at
     * @return the offset of the beginning of the open tag
     */
    public static int findTagBackwards(IStructuredDocument doc, int offset, int targetTagBalance) {
        // Balance of open and closing tags
        int tagBalance = 0;
        // Balance of open and closing brackets
        IStructuredDocumentRegion region = doc.getRegionAtCharacterOffset(offset);
        if (region != null) {
            boolean inEmptyTag = true;

            while (region != null) {
                int regionStart = region.getStartOffset();
                ITextRegionList subRegions = region.getRegions();
                for (int i = subRegions.size() - 1; i >= 0; i--) {
                    ITextRegion subRegion = subRegions.get(i);
                    int subRegionStart = regionStart + subRegion.getStart();
                    if (subRegionStart >= offset) {
                        continue;
                    }
                    String type = subRegion.getType();

                    // Iterate backwards and keep track of the tag balance such that
                    // we can find the corresponding opening tag

                    if (XML_TAG_OPEN.equals(type)) {
                        if (!inEmptyTag) {
                            tagBalance--;
                        }
                        if (tagBalance == targetTagBalance) {
                            return subRegionStart;
                        }
                    } else if (XML_END_TAG_OPEN.equals(type)) {
                        tagBalance++;
                    } else if (XML_EMPTY_TAG_CLOSE.equals(type)) {
                        inEmptyTag = true;
                    } else if (XML_TAG_CLOSE.equals(type)) {
                        inEmptyTag = false;
                    }
                }

                region = region.getPrevious();
            }
        }

        return -1;
    }

    /**
     * Finds the corresponding closing tag by searching forwards until the tag balance
     * reaches a given target.
     *
     * @param doc the document
     * @param start the starting offset (where the search begins searching forwards from)
     * @param targetTagBalance the balance to end the search at
     * @return the offset of the beginning of the closing tag
     */
    public static int findTagForwards(IStructuredDocument doc, int start, int targetTagBalance) {
        int tagBalance = 0;
        IStructuredDocumentRegion region = doc.getRegionAtCharacterOffset(start);

        if (region != null) {
            while (region != null) {
                int regionStart = region.getStartOffset();
                ITextRegionList subRegions = region.getRegions();
                for (int i = 0, n = subRegions.size(); i < n; i++) {
                    ITextRegion subRegion = subRegions.get(i);
                    int subRegionStart = regionStart + subRegion.getStart();
                    int subRegionEnd = regionStart + subRegion.getEnd();
                    if (subRegionEnd < start) {
                        continue;
                    }
                    String type = subRegion.getType();

                    if (XML_TAG_OPEN.equals(type)) {
                        tagBalance++;
                    } else if (XML_END_TAG_OPEN.equals(type)) {
                        tagBalance--;
                        if (tagBalance == targetTagBalance) {
                            return subRegionStart;
                        }
                    } else if (XML_EMPTY_TAG_CLOSE.equals(type)) {
                        tagBalance--;
                        if (tagBalance == targetTagBalance) {
                            // We don't jump to matching tags within a self-closed tag
                            return -1;
                        }
                    }
                }

                region = region.getNext();
            }
        }

        return -1;
    }
}
