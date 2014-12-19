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

import static com.android.SdkConstants.PREFIX_RESOURCE_REF;
import static com.android.SdkConstants.PREFIX_THEME_REF;

import org.eclipse.swt.graphics.Point;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
import org.eclipse.wst.xml.ui.internal.doubleclick.XMLDoubleClickStrategy;

/**
 * Custom version of {@link XMLDoubleClickStrategy} which is smarter about
 * selecting portions of resource references, etc.
 */
@SuppressWarnings("restriction") // XML API
public class AndroidDoubleClickStrategy extends XMLDoubleClickStrategy {
    /**
     * Creates a new {@linkplain AndroidDoubleClickStrategy}
     */
    public AndroidDoubleClickStrategy() {
    }

    @Override
    protected void processElementDoubleClicked() {
        // Special case: if you click on the local name portion of an attribute pair,
        // select only the local name. For example, if you click anywhere in the "text" region
        // of "android:text", select just the "text" portion.
        if (fTextRegion.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_NAME) {
            String regionText = fStructuredDocumentRegion.getText(fTextRegion);
            int cursor = fCaretPosition - fStructuredDocumentRegion.getStartOffset(fTextRegion);
            int ns = regionText.indexOf(':');
            if (cursor > ns) {
                int start = ns + 1;
                fTextViewer.setSelectedRange(fStructuredDocumentRegion.getStartOffset(fTextRegion)
                        + start,  fTextRegion.getTextLength() - start);
                return;
            }
        }

        super.processElementDoubleClicked();
    }

    @Override
    protected Point getWord(String string, int cursor) {
        if (string == null) {
            return null;
        }

        // Default implementation will strip off the surrounding quotes etc:
        Point position = super.getWord(string, cursor);

        assert cursor >= position.x && cursor <= position.y;

        // Special case: when you click on a resource identifier name, only select the
        // name portion
        if (string.startsWith(PREFIX_RESOURCE_REF, position.x) ||
                string.startsWith(PREFIX_THEME_REF, position.x)) {
            int nameStart = string.indexOf('/', position.x + 1);
            if (nameStart != -1 && nameStart < cursor) {
                position.x = nameStart + 1;
                return position;
            }
        }

        // Special case: when you have a dotted name, such as com.android.tools.MyClass,
        // and you click on the last part, select only that part
        int lastDot = string.lastIndexOf('.', cursor);
        if (lastDot >= position.x && lastDot < position.y - 1) {
            int next = string.indexOf('.', cursor);
            if (next == -1 || next > position.y) {
                position.x = lastDot + 1;
            }
        }

        return position;
    }
}
