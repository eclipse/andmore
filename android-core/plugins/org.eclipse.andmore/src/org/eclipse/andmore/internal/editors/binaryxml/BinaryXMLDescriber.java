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

package org.eclipse.andmore.internal.editors.binaryxml;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescriber;
import org.eclipse.core.runtime.content.IContentDescription;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A content describer for Android binary xml files
 *
 * <p>
 * This class referenced by the "describer" configuration element in
 * extensions to the <code>org.eclipse.core.runtime.contentTypes</code>
 * extension point.
 * </p>
 *
 * References :
 * <a>http://android.git.kernel.org/?p=platform/frameworks/base.git;a=blob;
 * f=include/utils/ResourceTypes.h</a>
 *
 */
public class BinaryXMLDescriber implements IContentDescriber {

    private static final int RES_XML_HEADER_SIZE = 8;
    private final static short RES_XML_TYPE = 0x0003;

    /*
     * (non-Javadoc)
     * @see org.eclipse.core.runtime.content.IContentDescriber#describe(java.io.
     * InputStream, org.eclipse.core.runtime.content.IContentDescription)
     */
    @Override
    public int describe(InputStream contents, IContentDescription description) throws IOException {
        int status = INVALID;
        int length = 8;
        byte[] bytes = new byte[length];
        if (contents.read(bytes, 0, length) == length) {
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            short type = buf.getShort();
            short headerSize = buf.getShort();
            int size = buf.getInt(); // chunk size
            if (type == RES_XML_TYPE && headerSize == RES_XML_HEADER_SIZE) {
                status = VALID;
            }
        }
        return status;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.eclipse.core.runtime.content.IContentDescriber#getSupportedOptions()
     */
    @Override
    public QualifiedName[] getSupportedOptions() {
        return new QualifiedName[0];
    }

}
