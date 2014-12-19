/*
 * Copyright (C) 2009 The Android Open Source Project
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

package org.eclipse.andmore.internal.editors.layout.gle2;

import org.eclipse.andmore.internal.editors.descriptors.ElementDescriptor;
import org.eclipse.andmore.internal.editors.layout.descriptors.ViewElementDescriptor;
import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;

import java.io.UnsupportedEncodingException;

/**
 * A d'n'd {@link Transfer} class that can transfer a <em>simplified</em> XML fragment
 * to transfer elements and their attributes between {@link LayoutCanvas}.
 * <p/>
 * The implementation is based on the {@link ByteArrayTransfer} and what we transfer
 * is text with the following fixed format:
 * <p/>
 * <pre>
 * {element-name element-property ...
 *  attrib_name="attrib_value"
 *  attrib2="..."
 *  {...inner elements...
 *  }
 * }
 * {...next element...
 * }
 *
 * </pre>
 * The format has nothing to do with XML per se, except for the fact that the
 * transfered content represents XML elements and XML attributes.
 *
 * <p/>
 * The detailed syntax is:
 * <pre>
 * - ELEMENT     := {NAME PROPERTY*\nATTRIB_LINE*ELEMENT*}\n
 * - PROPERTY    := $[A-Z]=[^ ]*
 * - NAME        := [^\n=]+
 * - ATTRIB_LINE := @URI:NAME=[^\n]*\n
 * </pre>
 *
 * Elements are represented by {@link SimpleElement}s and their attributes by
 * {@link SimpleAttribute}s, all of which have very specific properties that are
 * specifically limited to our needs for drag'n'drop.
 */
final class SimpleXmlTransfer extends ByteArrayTransfer {

    // Reference: http://www.eclipse.org/articles/Article-SWT-DND/DND-in-SWT.html

    private static final String TYPE_NAME = "android.ADT.simple.xml.transfer.1";    //$NON-NLS-1$
    private static final int TYPE_ID = registerType(TYPE_NAME);
    private static final SimpleXmlTransfer sInstance = new SimpleXmlTransfer();

    /** Private constructor. Use {@link #getInstance()} to retrieve the singleton instance. */
    private SimpleXmlTransfer() {
        // pass
    }

    /** Returns the singleton instance. */
    public static SimpleXmlTransfer getInstance() {
        return sInstance;
    }

    /**
     * Helper method that returns the FQCN transfered for the given {@link ElementDescriptor}.
     * <p/>
     * If the descriptor is a {@link ViewElementDescriptor}, the transfered data is the FQCN
     * of the Android View class represented (e.g. "android.widget.Button").
     * For any other non-null descriptor, the XML name is used.
     * Otherwise it is null.
     *
     * @param desc The {@link ElementDescriptor} to transfer.
     * @return The FQCN, XML name or null.
     */
    public static String getFqcn(ElementDescriptor desc) {
        if (desc instanceof ViewElementDescriptor) {
            return ((ViewElementDescriptor) desc).getFullClassName();
        } else if (desc != null) {
            return desc.getXmlName();
        }

        return null;
    }

    @Override
    protected int[] getTypeIds() {
        return new int[] { TYPE_ID };
    }

    @Override
    protected String[] getTypeNames() {
        return new String[] { TYPE_NAME };
    }

    /** Transforms a array of {@link SimpleElement} into a native data transfer. */
    @Override
    protected void javaToNative(Object object, TransferData transferData) {
        if (object == null || !(object instanceof SimpleElement[])) {
            return;
        }

        if (isSupportedType(transferData)) {
            StringBuilder sb = new StringBuilder();
            for (SimpleElement e : (SimpleElement[]) object) {
                sb.append(e.toString());
            }
            String data = sb.toString();

            try {
                byte[] buf = data.getBytes("UTF-8");  //$NON-NLS-1$
                super.javaToNative(buf, transferData);
            } catch (UnsupportedEncodingException e) {
                // unlikely; ignore
            }
        }
    }

    /**
     * Recreates an array of {@link SimpleElement} from a native data transfer.
     *
     * @return An array of {@link SimpleElement} or null. The array may be empty.
     */
    @Override
    protected Object nativeToJava(TransferData transferData) {
        if (isSupportedType(transferData)) {
            byte[] buf = (byte[]) super.nativeToJava(transferData);
            if (buf != null && buf.length > 0) {
                try {
                    String s = new String(buf, "UTF-8"); //$NON-NLS-1$
                    return SimpleElement.parseString(s);
                } catch (UnsupportedEncodingException e) {
                    // unlikely to happen, but still possible
                }
            }
        }

        return null;
    }
}
