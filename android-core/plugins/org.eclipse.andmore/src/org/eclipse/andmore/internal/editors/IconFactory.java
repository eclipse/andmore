/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.andmore.internal.editors;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.google.common.collect.Maps;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.andmore.internal.editors.ui.ErrorImageComposite;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import java.net.URL;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Factory to generate icons for Android Editors.
 * <p/>
 * Icons are kept here and reused.
 */
public class IconFactory {
    public static final int COLOR_RED     = SWT.COLOR_DARK_RED;
    public static final int COLOR_GREEN   = SWT.COLOR_DARK_GREEN;
    public static final int COLOR_BLUE    = SWT.COLOR_DARK_BLUE;
    public static final int COLOR_DEFAULT = SWT.COLOR_BLACK;

    public static final int SHAPE_CIRCLE  = 'C';
    public static final int SHAPE_RECT    = 'R';
    public static final int SHAPE_DEFAULT = SHAPE_CIRCLE;

    private static IconFactory sInstance;

    private Map<String, Image> mIconMap = Maps.newHashMap();
    private Map<URL, Image> mUrlMap = Maps.newHashMap();
    private Map<String, ImageDescriptor> mImageDescMap = Maps.newHashMap();
    private Map<Image, Image> mErrorIcons;
    private Map<Image, Image> mWarningIcons;

    private IconFactory() {
    }

    public static synchronized IconFactory getInstance() {
        if (sInstance == null) {
            sInstance = new IconFactory();
        }
        return sInstance;
    }

    public void dispose() {
        // Dispose icons
        for (Image icon : mIconMap.values()) {
            // The map can contain null values
            if (icon != null) {
                icon.dispose();
            }
        }
        mIconMap.clear();
        for (Image icon : mUrlMap.values()) {
            // The map can contain null values
            if (icon != null) {
                icon.dispose();
            }
        }
        mUrlMap.clear();
        if (mErrorIcons != null) {
            for (Image icon : mErrorIcons.values()) {
                // The map can contain null values
                if (icon != null) {
                    icon.dispose();
                }
            }
            mErrorIcons = null;
        }
        if (mWarningIcons != null) {
            for (Image icon : mWarningIcons.values()) {
                // The map can contain null values
                if (icon != null) {
                    icon.dispose();
                }
            }
            mWarningIcons = null;
        }
    }

    /**
     * Returns an Image for a given icon name.
     * <p/>
     * Callers should not dispose it.
     *
     * @param osName The leaf name, without the extension, of an existing icon in the
     *        editor's "icons" directory. If it doesn't exists, a default icon will be
     *        generated automatically based on the name.
     */
    public Image getIcon(String osName) {
        return getIcon(osName, COLOR_DEFAULT, SHAPE_DEFAULT);
    }

    /**
     * Returns an Image for a given icon name.
     * <p/>
     * Callers should not dispose it.
     *
     * @param osName The leaf name, without the extension, of an existing icon in the
     *        editor's "icons" directory. If it doesn't exist, a default icon will be
     *        generated automatically based on the name.
     * @param color The color of the text in the automatically generated icons,
     *        one of COLOR_DEFAULT, COLOR_RED, COLOR_BLUE or COLOR_RED.
     * @param shape The shape of the icon in the automatically generated icons,
     *        one of SHAPE_DEFAULT, SHAPE_CIRCLE or SHAPE_RECT.
     */
    public Image getIcon(String osName, int color, int shape) {
        String key = Character.toString((char) shape) + Integer.toString(color) + osName;
        Image icon = mIconMap.get(key);
        if (icon == null && !mIconMap.containsKey(key)) {
            ImageDescriptor id = getImageDescriptor(osName, color, shape);
            if (id != null) {
                icon = id.createImage();
            }
            // Note that we store null references in the icon map, to avoid looking them
            // up every time. If it didn't exist once, it will not exist later.
            mIconMap.put(key, icon);
        }
        return icon;
    }

    /**
     * Returns an ImageDescriptor for a given icon name.
     * <p/>
     * Callers should not dispose it.
     *
     * @param osName The leaf name, without the extension, of an existing icon in the
     *        editor's "icons" directory. If it doesn't exists, a default icon will be
     *        generated automatically based on the name.
     */
    public ImageDescriptor getImageDescriptor(String osName) {
        return getImageDescriptor(osName, COLOR_DEFAULT, SHAPE_DEFAULT);
    }

    /**
     * Returns an ImageDescriptor for a given icon name.
     * <p/>
     * Callers should not dispose it.
     *
     * @param osName The leaf name, without the extension, of an existing icon in the
     *        editor's "icons" directory. If it doesn't exists, a default icon will be
     *        generated automatically based on the name.
     * @param color The color of the text in the automatically generated icons.
     *        one of COLOR_DEFAULT, COLOR_RED, COLOR_BLUE or COLOR_RED.
     * @param shape The shape of the icon in the automatically generated icons,
     *        one of SHAPE_DEFAULT, SHAPE_CIRCLE or SHAPE_RECT.
     */
    public ImageDescriptor getImageDescriptor(String osName, int color, int shape) {
        String key = Character.toString((char) shape) + Integer.toString(color) + osName;
        ImageDescriptor id = mImageDescMap.get(key);
        if (id == null && !mImageDescMap.containsKey(key)) {
            id = AbstractUIPlugin.imageDescriptorFromPlugin(
                    AdtPlugin.PLUGIN_ID,
                    String.format("/icons/%1$s.png", osName)); //$NON-NLS-1$

            if (id == null) {
                id = new LetterImageDescriptor(osName.charAt(0), color, shape);
            }

            // Note that we store null references in the icon map, to avoid looking them
            // up every time. If it didn't exist once, it will not exist later.
            mImageDescMap.put(key, id);
        }
        return id;
    }

    /**
     * Returns an Image for a given icon name.
     * <p/>
     * Callers should not dispose it.
     *
     * @param osName The leaf name, without the extension, of an existing icon
     *            in the editor's "icons" directory. If it doesn't exist, the
     *            fallback will be used instead.
     * @param fallback the fallback icon name to use if the primary icon does
     *            not exist, or null if the method should return null if the
     *            image does not exist
     * @return the icon, which should not be disposed by the caller, or null
     * if the image does not exist *and*
     */
    @Nullable
    public Image getIcon(@NonNull String osName, @Nullable String fallback) {
        String key = osName;
        Image icon = mIconMap.get(key);
        if (icon == null && !mIconMap.containsKey(key)) {
            ImageDescriptor id = getImageDescriptor(osName, fallback);
            if (id != null) {
                icon = id.createImage();
            }
            // Note that we store null references in the icon map, to avoid looking them
            // up every time. If it didn't exist once, it will not exist later.
            mIconMap.put(key, icon);
        }
        return icon;
    }

    /**
     * Returns an icon of the given name, or if that image does not exist and
     * icon of the given fallback name.
     *
     * @param key the icon name
     * @param fallbackKey the fallback image to use if the primary key does not
     *            exist
     * @return the image descriptor, or null if the image does not exist and the
     *         fallbackKey is null
     */
    @Nullable
    public ImageDescriptor getImageDescriptor(@NonNull String key, @Nullable String fallbackKey) {
        ImageDescriptor id = mImageDescMap.get(key);
        if (id == null && !mImageDescMap.containsKey(key)) {
            id = AbstractUIPlugin.imageDescriptorFromPlugin(
                    AdtPlugin.PLUGIN_ID,
                    String.format("/icons/%1$s.png", key)); //$NON-NLS-1$
            if (id == null) {
                if (fallbackKey == null) {
                    return null;
                }
                id = getImageDescriptor(fallbackKey);
            }

            // Place the fallback image for this key as well such that we don't keep trying
            // to load the failed image
            mImageDescMap.put(key, id);
        }

        return id;
    }

    /**
     * Returns the image indicated by the given URL
     *
     * @param url the url to the image resources
     * @return the image for the url, or null if it cannot be initialized
     */
    public Image getIcon(URL url) {
        Image image = mUrlMap.get(url);
        if (image == null) {
            ImageDescriptor descriptor = ImageDescriptor.createFromURL(url);
            image = descriptor.createImage();
            mUrlMap.put(url, image);
        }

        return image;
    }

    /**
     * Returns an image with an error icon overlaid on it. The icons are cached,
     * so the base image should be cached as well, or this method will keep
     * storing new overlays into its cache.
     *
     * @param image the base image
     * @return the combined image
     */
    @NonNull
    public Image addErrorIcon(@NonNull Image image) {
        if (mErrorIcons != null) {
            Image combined = mErrorIcons.get(image);
            if (combined != null) {
                return combined;
            }
        } else {
            mErrorIcons = new IdentityHashMap<Image, Image>();
        }

        Image combined = new ErrorImageComposite(image, false).createImage();
        mErrorIcons.put(image, combined);

        return combined;
    }

    /**
     * Returns an image with a warning icon overlaid on it. The icons are
     * cached, so the base image should be cached as well, or this method will
     * keep storing new overlays into its cache.
     *
     * @param image the base image
     * @return the combined image
     */
    @NonNull
    public Image addWarningIcon(@NonNull Image image) {
        if (mWarningIcons != null) {
            Image combined = mWarningIcons.get(image);
            if (combined != null) {
                return combined;
            }
        } else {
            mWarningIcons = new IdentityHashMap<Image, Image>();
        }

        Image combined = new ErrorImageComposite(image, true).createImage();
        mWarningIcons.put(image, combined);

        return combined;
    }

    /**
     * A simple image description that generates a 16x16 image which consists
     * of a colored letter inside a black & white circle.
     */
    private static class LetterImageDescriptor extends ImageDescriptor {

        private final char mLetter;
        private final int mColor;
        private final int mShape;

        public LetterImageDescriptor(char letter, int color, int shape) {
            mLetter = Character.toUpperCase(letter);
            mColor = color;
            mShape = shape;
        }

        @Override
        public ImageData getImageData() {

            final int SX = 15;
            final int SY = 15;
            final int RX = 4;
            final int RY = 4;

            Display display = Display.getCurrent();
            if (display == null) {
                return null;
            }

            Image image = new Image(display, SX, SY);

            GC gc = new GC(image);
            gc.setAdvanced(true);
            gc.setAntialias(SWT.ON);
            gc.setTextAntialias(SWT.ON);

            // image.setBackground() does not appear to have any effect; we must explicitly
            // paint into the image the background color we want masked out later.
            // HOWEVER, alpha transparency does not work; we only get to mark a single color
            // as transparent. You might think we could pick a system color (to avoid having
            // to allocate and dispose the color), or a wildly unique color (to make sure we
            // don't accidentally pick up any extra pixels in the image as transparent), but
            // this has the very unfortunate side effect of making neighbor pixels in the
            // antialiased rendering of the circle pick up shades of that alternate color,
            // which looks bad. Therefore we pick a color which is similar to one of our
            // existing colors but hopefully different from most pixels. A visual check
            // confirms that this seems to work pretty well:
            RGB backgroundRgb = new RGB(254, 254, 254);
            Color backgroundColor = new Color(display, backgroundRgb);
            gc.setBackground(backgroundColor);
            gc.fillRectangle(0, 0, SX, SY);

            gc.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
            if (mShape == SHAPE_CIRCLE) {
                gc.fillOval(0, 0, SX - 1, SY - 1);
            } else if (mShape == SHAPE_RECT) {
                gc.fillRoundRectangle(0, 0, SX - 1, SY - 1, RX, RY);
            }

            gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
            gc.setLineWidth(1);
            if (mShape == SHAPE_CIRCLE) {
                gc.drawOval(0, 0, SX - 1, SY - 1);
            } else if (mShape == SHAPE_RECT) {
                gc.drawRoundRectangle(0, 0, SX - 1, SY - 1, RX, RY);
            }

            // Get a bold version of the default system font, if possible.
            Font font = display.getSystemFont();
            FontData[] fds = font.getFontData();
            fds[0].setStyle(SWT.BOLD);
            // use 3/4th of the circle diameter for the font size (in pixels)
            // and convert it to "font points" (font points in SWT are hardcoded in an
            // arbitrary 72 dpi and then converted in real pixels using whatever is
            // indicated by getDPI -- at least that's how it works under Win32).
            fds[0].setHeight((int) ((SY + 1) * 3./4. * 72./display.getDPI().y));
            // Note: win32 implementation always uses fds[0] so we change just that one.
            // getFontData indicates that the array of fd is really an unusual thing for X11.
            font = new Font(display, fds);
            gc.setFont(font);
            gc.setForeground(display.getSystemColor(mColor));

            // Text measurement varies so slightly depending on the platform
            int ofx = 0;
            int ofy = 0;
            if (SdkConstants.CURRENT_PLATFORM == SdkConstants.PLATFORM_WINDOWS) {
                ofx = +1;
                ofy = -1;
            } else if (SdkConstants.CURRENT_PLATFORM == SdkConstants.PLATFORM_DARWIN) {
                // Tweak pixel positioning of some letters that don't look good on the Mac
                if (mLetter != 'T' && mLetter != 'V') {
                    ofy = -1;
                }
                if (mLetter == 'I') {
                    ofx = -2;
                }
            }

            String s = Character.toString(mLetter);
            Point p = gc.textExtent(s);
            int tx = (SX + ofx - p.x) / 2;
            int ty = (SY + ofy - p.y) / 2;
            gc.drawText(s, tx, ty, true /* isTransparent */);

            font.dispose();
            gc.dispose();

            ImageData data = image.getImageData();
            image.dispose();
            backgroundColor.dispose();

            // Set transparent pixel in the palette such that on paint (over palette,
            // which has a background of SWT.COLOR_WIDGET_BACKGROUND, and over the tree
            // which has a white background) we will substitute the background in for
            // the backgroundPixel.
            int backgroundPixel = data.palette.getPixel(backgroundRgb);
            data.transparentPixel = backgroundPixel;

            return data;
        }
    }
}
