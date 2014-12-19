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
package org.eclipse.andmore.common.layout;

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_LAYOUT_GRAVITY;
import static com.android.SdkConstants.GRAVITY_VALUE_BOTTOM;
import static com.android.SdkConstants.GRAVITY_VALUE_CENTER;
import static com.android.SdkConstants.GRAVITY_VALUE_CENTER_HORIZONTAL;
import static com.android.SdkConstants.GRAVITY_VALUE_CENTER_VERTICAL;
import static com.android.SdkConstants.GRAVITY_VALUE_FILL;
import static com.android.SdkConstants.GRAVITY_VALUE_FILL_HORIZONTAL;
import static com.android.SdkConstants.GRAVITY_VALUE_FILL_VERTICAL;
import static com.android.SdkConstants.GRAVITY_VALUE_LEFT;
import static com.android.SdkConstants.GRAVITY_VALUE_RIGHT;
import static com.android.SdkConstants.GRAVITY_VALUE_TOP;

import org.w3c.dom.Element;

/** Helper class for looking up the gravity masks of gravity attributes */
public class GravityHelper {
    // From SDK constants; temporary
    public static final String GRAVITY_VALUE_START = "start";                         //$NON-NLS-1$
    public static final String GRAVITY_VALUE_END = "end";                             //$NON-NLS-1$

    /** Bitmask for a gravity which includes left */
    @SuppressWarnings("PointlessBitwiseExpression") // for symmetry with other fields
    public static final int GRAVITY_LEFT         = 1 << 0;

    /** Bitmask for a gravity which includes right */
    public static final int GRAVITY_RIGHT        = 1 << 1;

    /** Bitmask for a gravity which includes center horizontal */
    public static final int GRAVITY_CENTER_HORIZ = 1 << 2;

    /** Bitmask for a gravity which includes fill horizontal */
    public static final int GRAVITY_FILL_HORIZ   = 1 << 3;

    /** Bitmask for a gravity which includes center vertical */
    public static final int GRAVITY_CENTER_VERT  = 1 << 4;

    /** Bitmask for a gravity which includes fill vertical */
    public static final int GRAVITY_FILL_VERT    = 1 << 5;

    /** Bitmask for a gravity which includes top */
    public static final int GRAVITY_TOP          = 1 << 6;

    /** Bitmask for a gravity which includes bottom */
    public static final int GRAVITY_BOTTOM       = 1 << 7;

    /** Bitmask for a gravity which includes start */
    public static final int GRAVITY_START        = 1 << 8;

    /** Bitmask for a gravity which includes end */
    public static final int GRAVITY_END          = 1 << 9;

    /** Bitmask for a gravity which includes any horizontal constraint */
    public static final int GRAVITY_HORIZ_MASK = GRAVITY_CENTER_HORIZ | GRAVITY_FILL_HORIZ
            | GRAVITY_LEFT | GRAVITY_RIGHT | GRAVITY_START | GRAVITY_END;

    /** Bitmask for a gravity which any vertical constraint */
    public static final int GRAVITY_VERT_MASK = GRAVITY_CENTER_VERT | GRAVITY_FILL_VERT
            | GRAVITY_TOP | GRAVITY_BOTTOM;

    /**
     * Returns the gravity of the given element
     *
     * @param element the element to look up the gravity for
     * @return a bit mask corresponding to the selected gravities
     */
    public static int getGravity(Element element) {
        String gravityString = element.getAttributeNS(ANDROID_URI, ATTR_LAYOUT_GRAVITY);
        return getGravity(gravityString, GRAVITY_LEFT | GRAVITY_TOP);
    }

    /**
     * Returns the gravity bitmask for the given gravity string description
     *
     * @param gravityString the gravity string description
     * @param defaultMask the default/initial bitmask to start with
     * @return a bitmask corresponding to the gravity description
     */
    public static int getGravity(String gravityString, int defaultMask) {
        int gravity = defaultMask;
        if (gravityString != null && !gravityString.isEmpty()) {
            String[] anchors = gravityString.split("\\|"); //$NON-NLS-1$
            for (String anchor : anchors) {
                if (GRAVITY_VALUE_CENTER.equals(anchor)) {
                    gravity = GRAVITY_CENTER_HORIZ | GRAVITY_CENTER_VERT;
                } else if (GRAVITY_VALUE_FILL.equals(anchor)) {
                    gravity = GRAVITY_FILL_HORIZ | GRAVITY_FILL_VERT;
                } else if (GRAVITY_VALUE_CENTER_VERTICAL.equals(anchor)) {
                    gravity = (gravity & GRAVITY_HORIZ_MASK) | GRAVITY_CENTER_VERT;
                } else if (GRAVITY_VALUE_CENTER_HORIZONTAL.equals(anchor)) {
                    gravity = (gravity & GRAVITY_VERT_MASK) | GRAVITY_CENTER_HORIZ;
                } else if (GRAVITY_VALUE_FILL_VERTICAL.equals(anchor)) {
                    gravity = (gravity & GRAVITY_HORIZ_MASK) | GRAVITY_FILL_VERT;
                } else if (GRAVITY_VALUE_FILL_HORIZONTAL.equals(anchor)) {
                    gravity = (gravity & GRAVITY_VERT_MASK) | GRAVITY_FILL_HORIZ;
                } else if (GRAVITY_VALUE_TOP.equals(anchor)) {
                    gravity = (gravity & GRAVITY_HORIZ_MASK) | GRAVITY_TOP;
                } else if (GRAVITY_VALUE_BOTTOM.equals(anchor)) {
                    gravity = (gravity & GRAVITY_HORIZ_MASK) | GRAVITY_BOTTOM;
                } else if (GRAVITY_VALUE_LEFT.equals(anchor)) {
                    gravity = (gravity & (GRAVITY_VERT_MASK|GRAVITY_START)) | GRAVITY_LEFT;
                } else if (GRAVITY_VALUE_RIGHT.equals(anchor)) {
                    gravity = (gravity & (GRAVITY_VERT_MASK|GRAVITY_END)) | GRAVITY_RIGHT;
                } else if (GRAVITY_VALUE_START.equals(anchor)) {
                    gravity = (gravity & (GRAVITY_VERT_MASK|GRAVITY_LEFT)) | GRAVITY_START;
                } else if (GRAVITY_VALUE_END.equals(anchor)) {
                    gravity = (gravity & (GRAVITY_VERT_MASK|GRAVITY_RIGHT)) | GRAVITY_END;
                } // else: "clip" not supported
            }
        }

        return gravity;
    }

    /**
     * Returns true if the given gravity bitmask is constrained horizontally
     *
     * @param gravity the gravity bitmask
     * @return true if the given gravity bitmask is constrained horizontally
     */
    public static boolean isConstrainedHorizontally(int gravity) {
        return (gravity & GRAVITY_HORIZ_MASK) != 0;
    }

    /**
     * Returns true if the given gravity bitmask is constrained vertically
     *
     * @param gravity the gravity bitmask
     * @return true if the given gravity bitmask is constrained vertically
     */
    public static boolean isConstrainedVertically(int gravity) {
        return (gravity & GRAVITY_VERT_MASK) != 0;
    }

    /**
     * Returns true if the given gravity bitmask is left aligned
     *
     * @param gravity the gravity bitmask
     * @return true if the given gravity bitmask is left aligned
     */
    public static boolean isLeftAligned(int gravity) {
        return (gravity & (GRAVITY_LEFT|GRAVITY_START)) != 0;
    }

    /**
     * Returns true if the given gravity bitmask is top aligned
     *
     * @param gravity the gravity bitmask
     * @return true if the given gravity bitmask is aligned
     */
    public static boolean isTopAligned(int gravity) {
        return (gravity & GRAVITY_TOP) != 0;
    }

    /** Returns a gravity value string from the given gravity bitmask
     *
     * @param gravity the gravity bitmask
     * @return the corresponding gravity string suitable as an XML attribute value
     */
    public static String getGravity(int gravity) {
        if (gravity == 0) {
            return "";
        }

        if ((gravity & (GRAVITY_CENTER_HORIZ | GRAVITY_CENTER_VERT)) ==
                (GRAVITY_CENTER_HORIZ | GRAVITY_CENTER_VERT)) {
            return GRAVITY_VALUE_CENTER;
        }

        StringBuilder sb = new StringBuilder(30);
        int horizontal = gravity & GRAVITY_HORIZ_MASK;
        int vertical = gravity & GRAVITY_VERT_MASK;

        if ((horizontal & (GRAVITY_LEFT|GRAVITY_START)) != 0) {
            if ((horizontal & GRAVITY_LEFT) != 0) {
                sb.append(GRAVITY_VALUE_LEFT);
            }
            if ((horizontal & GRAVITY_START) != 0) {
                if (sb.length() > 0) {
                    sb.append('|');
                }
                sb.append(GRAVITY_VALUE_START);
            }
        } else if ((horizontal & (GRAVITY_RIGHT|GRAVITY_END)) != 0) {
            if ((horizontal & GRAVITY_RIGHT) != 0) {
                sb.append(GRAVITY_VALUE_RIGHT);
            }
            if ((horizontal & GRAVITY_END) != 0) {
                if (sb.length() > 0) {
                    sb.append('|');
                }
                sb.append(GRAVITY_VALUE_END);
            }
        } else if ((horizontal & GRAVITY_CENTER_HORIZ) != 0) {
            sb.append(GRAVITY_VALUE_CENTER_HORIZONTAL);
        } else if ((horizontal & GRAVITY_FILL_HORIZ) != 0) {
            sb.append(GRAVITY_VALUE_FILL_HORIZONTAL);
        }

        if (sb.length() > 0 && vertical != 0) {
            sb.append('|');
        }

        if ((vertical & GRAVITY_TOP) != 0) {
            sb.append(GRAVITY_VALUE_TOP);
        } else if ((vertical & GRAVITY_BOTTOM) != 0) {
            sb.append(GRAVITY_VALUE_BOTTOM);
        } else if ((vertical & GRAVITY_CENTER_VERT) != 0) {
            sb.append(GRAVITY_VALUE_CENTER_VERTICAL);
        } else if ((vertical & GRAVITY_FILL_VERT) != 0) {
            sb.append(GRAVITY_VALUE_FILL_VERTICAL);
        }

        return sb.toString();
    }
}
