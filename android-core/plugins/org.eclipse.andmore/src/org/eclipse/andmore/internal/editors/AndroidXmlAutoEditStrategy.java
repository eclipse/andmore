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

import static org.eclipse.wst.xml.core.internal.regions.DOMRegionContext.XML_CONTENT;
import static org.eclipse.wst.xml.core.internal.regions.DOMRegionContext.XML_EMPTY_TAG_CLOSE;
import static org.eclipse.wst.xml.core.internal.regions.DOMRegionContext.XML_END_TAG_OPEN;
import static org.eclipse.wst.xml.core.internal.regions.DOMRegionContext.XML_TAG_CLOSE;
import static org.eclipse.wst.xml.core.internal.regions.DOMRegionContext.XML_TAG_NAME;
import static org.eclipse.wst.xml.core.internal.regions.DOMRegionContext.XML_TAG_OPEN;

import com.android.utils.Pair;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.AdtUtils;
import org.eclipse.andmore.internal.editors.formatting.EclipseXmlFormatPreferences;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorExtension3;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionList;

/**
 * Edit strategy for Android XML files. It attempts a number of edit
 * enhancements:
 * <ul>
 *   <li> Auto indentation. The default XML indentation scheme is to just copy the
 *        indentation of the previous line. This edit strategy improves on that situation
 *        by considering the tag and bracket balance on the current line and using it
 *        to determine whether the next line should be indented or use the same
 *        indentation as the parent, or even the indentation of an earlier line
 *        (when for example the current line closes an element which was started on an
 *        earlier line.)
 *   <li> Newline handling. In addition to indenting, it can also adjust the following text
 *        appropriately when a newline is inserted. For example, it will reformat
 *        the following (where | represents the caret position):
 *    <pre>
 *       {@code <item name="a">|</item>}
 *    </pre>
 *    into
 *    <pre>
 *       {@code <item name="a">}
 *           |
 *       {@code </item>}
 *    </pre>
 * </ul>
 * In the future we might consider other editing enhancements here as well, such as
 * refining the comment handling, or reindenting when you type the / of a closing tag,
 * or even making the bracket matcher more resilient.
 */
@SuppressWarnings("restriction") // XML model
public class AndroidXmlAutoEditStrategy implements IAutoEditStrategy {

    @Override
    public void customizeDocumentCommand(IDocument document, DocumentCommand c) {
        if (!isSmartInsertMode()) {
            return;
        }

        if (!(document instanceof IStructuredDocument)) {
            // This shouldn't happen unless this strategy is used on an invalid document
            return;
        }
        IStructuredDocument doc = (IStructuredDocument) document;

        // Handle newlines/indentation
        if (c.length == 0 && c.text != null
                && TextUtilities.endsWith(doc.getLegalLineDelimiters(), c.text) != -1) {

            IModelManager modelManager = StructuredModelManager.getModelManager();
            IStructuredModel model = modelManager.getModelForRead(doc);
            if (model != null) {
                try {
                    final int offset = c.offset;
                    int lineStart = findLineStart(doc, offset);
                    int textStart = findTextStart(doc, lineStart, offset);

                    IStructuredDocumentRegion region = doc.getRegionAtCharacterOffset(textStart);
                    if (region != null && region.getType().equals(XML_TAG_NAME)) {
                        Pair<Integer,Integer> balance = getBalance(doc, textStart, offset);
                        int tagBalance = balance.getFirst();
                        int bracketBalance = balance.getSecond();

                        String lineIndent = ""; //$NON-NLS-1$
                        if (textStart > lineStart) {
                            lineIndent = doc.get(lineStart, textStart - lineStart);
                        }

                        // We only care if tag or bracket balance is greater than 0;
                        // we never *dedent* on negative balances
                        boolean addIndent = false;
                        if (bracketBalance < 0) {
                            // Handle
                            //    <foo
                            //        ></foo>^
                            // and
                            //    <foo
                            //        />^
                            ITextRegion left = getRegionAt(doc, offset, true /*biasLeft*/);
                            if (left != null
                                    && (left.getType().equals(XML_TAG_CLOSE)
                                        || left.getType().equals(XML_EMPTY_TAG_CLOSE))) {

                                // Find the corresponding open tag...
                                // The org.eclipse.wst.xml.ui.gotoMatchingTag frequently
                                // doesn't work, it just says "No matching brace found"
                                // (or I would use that here).

                                int targetBalance = 0;
                                ITextRegion right = getRegionAt(doc, offset, false /*biasLeft*/);
                                if (right != null && right.getType().equals(XML_END_TAG_OPEN)) {
                                    targetBalance = -1;
                                }
                                int openTag = AndroidXmlCharacterMatcher.findTagBackwards(doc,
                                        offset, targetBalance);
                                if (openTag != -1) {
                                    // Look up the indentation of the given line
                                    lineIndent = AndroidXmlEditor.getIndentAtOffset(doc, openTag);
                                }
                            }
                        } else if (tagBalance > 0 || bracketBalance > 0) {
                            // Add indentation
                            addIndent = true;
                        }

                        StringBuilder sb = new StringBuilder(c.text);
                        sb.append(lineIndent);
                        String oneIndentUnit = EclipseXmlFormatPreferences.create().getOneIndentUnit();
                        if (addIndent) {
                            sb.append(oneIndentUnit);
                        }

                        // Handle
                        //     <foo>^</foo>
                        // turning into
                        //     <foo>
                        //         ^
                        //     </foo>
                        ITextRegion left = getRegionAt(doc, offset, true /*biasLeft*/);
                        ITextRegion right = getRegionAt(doc, offset, false /*biasLeft*/);
                        if (left != null && right != null
                                && left.getType().equals(XML_TAG_CLOSE)
                                && right.getType().equals(XML_END_TAG_OPEN)) {
                            // Move end tag
                            if (tagBalance > 0 && bracketBalance < 0) {
                                sb.append(oneIndentUnit);
                            }
                            c.caretOffset = offset + sb.length();
                            c.shiftsCaret = false;
                            sb.append(TextUtilities.getDefaultLineDelimiter(doc));
                            sb.append(lineIndent);
                        }
                        c.text = sb.toString();
                    } else if (region != null && region.getType().equals(XML_CONTENT)) {
                        // Indenting in text content. If you're in the middle of editing
                        // text, just copy the current line indentation.
                        // However, if you're editing in leading whitespace (e.g. you press
                        // newline on a blank line following say an element) then figure
                        // out the indentation as if the newline had been pressed at the
                        // end of the element, and insert that amount of indentation.
                        // In this case we need to also make sure to subtract any existing
                        // whitespace on the current line such that if we have
                        //
                        // <foo>
                        // ^   <bar/>
                        // </foo>
                        //
                        // you end up with
                        //
                        // <foo>
                        //
                        //    ^<bar/>
                        // </foo>
                        //
                        String text = region.getText();
                        int regionStart = region.getStartOffset();
                        int delta = offset - regionStart;
                        boolean inWhitespacePrefix = true;
                        for (int i = 0, n = Math.min(delta, text.length()); i < n; i++) {
                            char ch = text.charAt(i);
                            if (!Character.isWhitespace(ch)) {
                                inWhitespacePrefix = false;
                                break;
                            }
                        }
                        if (inWhitespacePrefix) {
                            IStructuredDocumentRegion previous = region.getPrevious();
                            if (previous != null && previous.getType() == XML_TAG_NAME) {
                                ITextRegionList subRegions = previous.getRegions();
                                ITextRegion last = subRegions.get(subRegions.size() - 1);
                                if (last.getType() == XML_TAG_CLOSE ||
                                        last.getType() == XML_EMPTY_TAG_CLOSE) {
                                    // See if the last tag was a closing tag
                                    boolean wasClose = last.getType() == XML_EMPTY_TAG_CLOSE;
                                    if (!wasClose) {
                                        // Search backwards to see if the XML_TAG_CLOSE
                                        // is the end of an </endtag>
                                        for (int i = subRegions.size() - 2; i >= 0; i--) {
                                            ITextRegion current = subRegions.get(i);
                                            String type = current.getType();
                                            if (type != XML_TAG_NAME) {
                                                wasClose = type == XML_END_TAG_OPEN;
                                                break;
                                            }
                                        }
                                    }

                                    int begin = AndroidXmlCharacterMatcher.findTagBackwards(doc,
                                            previous.getStartOffset() + last.getStart(), 0);
                                    int prevLineStart = findLineStart(doc, begin);
                                    int prevTextStart = findTextStart(doc, prevLineStart, begin);

                                    String lineIndent = ""; //$NON-NLS-1$
                                    if (prevTextStart > prevLineStart) {
                                        lineIndent = doc.get(prevLineStart,
                                                prevTextStart - prevLineStart);
                                    }
                                    StringBuilder sb = new StringBuilder(c.text);
                                    sb.append(lineIndent);

                                    // See if there is whitespace on the insert line that
                                    // we should also remove
                                    for (int i = delta, n = text.length(); i < n; i++) {
                                        char ch = text.charAt(i);
                                        if (ch == ' ') {
                                            c.length++;
                                        } else {
                                            break;
                                        }
                                    }

                                    boolean addIndent = (last.getType() == XML_TAG_CLOSE)
                                            && !wasClose;

                                    // Is there just whitespace left of this text tag
                                    // until we reach an end tag?
                                    boolean whitespaceToEndTag = true;
                                    for (int i = delta; i < text.length(); i++) {
                                        char ch = text.charAt(i);
                                        if (ch == '\n' || !Character.isWhitespace(ch)) {
                                            whitespaceToEndTag = false;
                                            break;
                                        }
                                    }
                                    if (whitespaceToEndTag) {
                                        IStructuredDocumentRegion next = region.getNext();
                                        if (next != null && next.getType() == XML_TAG_NAME) {
                                            String nextType = next.getRegions().get(0).getType();
                                            if (nextType == XML_END_TAG_OPEN) {
                                                addIndent = false;
                                            }
                                        }
                                    }

                                    if (addIndent) {
                                        sb.append(EclipseXmlFormatPreferences.create()
                                                .getOneIndentUnit());
                                    }
                                    c.text = sb.toString();

                                    return;
                                }
                            }
                        }
                        copyPreviousLineIndentation(doc, c);
                    } else {
                        copyPreviousLineIndentation(doc, c);
                    }
                } catch (BadLocationException e) {
                    AdtPlugin.log(e, null);
                } finally {
                    model.releaseFromRead();
                }
            }
        }
    }

    /**
     * Returns the offset of the start of the line (which might be whitespace)
     *
     * @param document the document
     * @param offset an offset for a character anywhere on the line
     * @return the offset of the first character on the line
     * @throws BadLocationException if the offset is invalid
     */
    public static int findLineStart(IDocument document, int offset) throws BadLocationException {
        offset = Math.max(0, Math.min(offset, document.getLength() - 1));
        IRegion info = document.getLineInformationOfOffset(offset);
        return info.getOffset();
    }

    /**
     * Finds the first non-whitespace character on the given line
     *
     * @param document the document to search
     * @param lineStart the offset of the beginning of the line
     * @param lineEnd the offset of the end of the line, or the maximum position on the
     *            line to search
     * @return the offset of the first non whitespace character, or the maximum position,
     *         whichever is smallest
     * @throws BadLocationException if the offsets are invalid
     */
    public static int findTextStart(IDocument document, int lineStart, int lineEnd)
            throws BadLocationException {
        for (int offset = lineStart; offset < lineEnd; offset++) {
            char c = document.getChar(offset);
            if (c != ' ' && c != '\t') {
                return offset;
            }
        }

        return lineEnd;
    }

    /**
     * Indent the new line the same way as the current line.
     *
     * @param doc the document to indent in
     * @param command the document command to customize
     * @throws BadLocationException if the offsets are invalid
     */
    private void copyPreviousLineIndentation(IDocument doc, DocumentCommand command)
            throws BadLocationException {

        if (command.offset == -1 || doc.getLength() == 0) {
            return;
        }

        int lineStart = findLineStart(doc, command.offset);
        int textStart = findTextStart(doc, lineStart, command.offset);

        StringBuilder sb = new StringBuilder(command.text);
        if (textStart > lineStart) {
            sb.append(doc.get(lineStart, textStart - lineStart));
        }

        command.text = sb.toString();
    }


    /**
     * Returns the subregion at the given offset, with a bias to the left or a bias to the
     * right. In other words, if | represents the caret position, in the XML
     * {@code <foo>|</bar>} then the subregion with bias left is the closing {@code >} and
     * the subregion with bias right is the opening {@code </}.
     *
     * @param doc the document
     * @param offset the offset in the document
     * @param biasLeft whether we should look at the token on the left or on the right
     * @return the subregion at the given offset, or null if not found
     */
    private static ITextRegion getRegionAt(IStructuredDocument doc, int offset,
            boolean biasLeft) {
        if (biasLeft) {
            offset--;
        }
        IStructuredDocumentRegion region =
                doc.getRegionAtCharacterOffset(offset);
        if (region != null) {
            return region.getRegionAtCharacterOffset(offset);
        }

        return null;
    }

    /**
     * Returns a pair of (tag-balance,bracket-balance) for the range textStart to offset.
     *
     * @param doc the document
     * @param start the offset of the starting character (inclusive)
     * @param end the offset of the ending character (exclusive)
     * @return the balance of tags and brackets
     */
    private static Pair<Integer, Integer> getBalance(IStructuredDocument doc,
            int start, int end) {
        // Balance of open and closing tags
        // <foo></foo> has tagBalance = 0, <foo> has tagBalance = 1
        int tagBalance = 0;
        // Balance of open and closing brackets
        // <foo attr1="value1"> has bracketBalance = 1, <foo has bracketBalance = 1
        int bracketBalance = 0;
        IStructuredDocumentRegion region = doc.getRegionAtCharacterOffset(start);

        if (region != null) {
            boolean inOpenTag = true;
            while (region != null && region.getStartOffset() < end) {
                int regionStart = region.getStartOffset();
                ITextRegionList subRegions = region.getRegions();
                for (int i = 0, n = subRegions.size(); i < n; i++) {
                    ITextRegion subRegion = subRegions.get(i);
                    int subRegionStart = regionStart + subRegion.getStart();
                    int subRegionEnd = regionStart + subRegion.getEnd();
                    if (subRegionEnd < start || subRegionStart >= end) {
                        continue;
                    }
                    String type = subRegion.getType();

                    if (XML_TAG_OPEN.equals(type)) {
                        bracketBalance++;
                        inOpenTag = true;
                    } else if (XML_TAG_CLOSE.equals(type)) {
                        bracketBalance--;
                        if (inOpenTag) {
                            tagBalance++;
                        } else {
                            tagBalance--;
                        }
                    } else if (XML_END_TAG_OPEN.equals(type)) {
                        bracketBalance++;
                        inOpenTag = false;
                    } else if (XML_EMPTY_TAG_CLOSE.equals(type)) {
                        bracketBalance--;
                    }
                }

                region = region.getNext();
            }
        }

        return Pair.of(tagBalance, bracketBalance);
    }

    /**
     * Determine if we're in smart insert mode (if so, don't do any edit magic)
     *
     * @return true if the editor is in smart mode (or if it's an unknown editor type)
     */
    private static boolean isSmartInsertMode() {
        ITextEditor textEditor = AdtUtils.getActiveTextEditor();
        if (textEditor instanceof ITextEditorExtension3) {
            ITextEditorExtension3 editor = (ITextEditorExtension3) textEditor;
            return editor.getInsertMode() == ITextEditorExtension3.SMART_INSERT;
        }

        return true;
    }
}
