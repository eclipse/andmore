/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.sdkuilib.internal.repository.icons;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.sdklib.internal.repository.archives.Archive;
import com.android.sdklib.internal.repository.packages.Package;
import com.android.sdklib.internal.repository.sources.SdkSource;
import com.android.sdkuilib.internal.repository.core.PkgContentProvider;

import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;


/**
 * An utility class to serve {@link Image} correspond to the various icons
 * present in this package and dispose of them correctly at the end.
 */
public class ImageFactory {

    private final Display mDisplay;
    private final Map<String, Image> mImages = new HashMap<String, Image>();

    /**
     * Filter an image when it's loaded by
     * {@link ImageFactory#getImageByName(String, String, Filter)}.
     */
    public interface Filter {
        /**
         * Invoked by {@link ImageFactory#getImageByName(String, String, Filter)} when
         * a non-null {@link Image} object has been loaded. The filter should create a
         * new image, modifying it as needed. <br/>
         * If no modification is necessary, the filter can simply return the source image.  <br/>
         * The result will be cached and returned by {@link ImageFactory}.
         * <p/>
         *
         * @param source A non-null source image.
         * @return Either the source or a new, potentially modified, image.
         */
        @NonNull public Image filter(@NonNull Image source);
    }

    public ImageFactory(@NonNull Display display) {
        mDisplay = display;
    }

    /**
     * Loads an image given its filename (with its extension).
     * Might return null if the image cannot be loaded.  <br/>
     * The image is cached. Successive calls will return the <em>same</em> object. <br/>
     * The image is automatically disposed when {@link ImageFactory} is disposed.
     *
     * @param imageName The filename (with extension) of the image to load.
     * @return A new or existing {@link Image}. The caller must NOT dispose the image (the
     *  image will disposed by {@link #dispose()}). The returned image can be null if the
     *  expected file is missing.
     */
    @Nullable
    public Image getImageByName(@NonNull String imageName) {
        return getImageByName(imageName, imageName, null);
    }


    /**
     * Loads an image given its filename (with its extension), caches it using the given
     * {@code KeyName} name and optionally applies a filter to it.
     * Might return null if the image cannot be loaded.
     * The image is cached. Successive calls using {@code KeyName} will return the <em>same</em>
     * object directly (the filter is not re-applied in this case.) <br/>
     * The image is automatically disposed when {@link ImageFactory} is disposed.
     * <p/>
     *
     * @param imageName The filename (with extension) of the image to load.
     * @return A new or existing {@link Image}. The caller must NOT dispose the image (the
     *  image will disposed by {@link #dispose()}). The returned image is null if the
     *  expected file is missing.
     */
    @Nullable
    public Image getImageByName(@NonNull String imageName,
                                @NonNull String keyName,
                                @Nullable Filter filter) {

        Image image = mImages.get(keyName);
        if (image != null) {
            return image;
        }

        InputStream stream = getClass().getResourceAsStream(imageName);
        if (stream != null) {
            try {
                image = new Image(mDisplay, stream);
                if (image != null && filter != null) {
                    Image image2 = filter.filter(image);
                    if (image2 != image && !image.isDisposed()) {
                        image.dispose();
                    }
                    image = image2;
                }
            } catch (SWTException e) {
                // ignore
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }

        // Store the image in the hash, even if this failed. If it fails now, it will fail later.
        mImages.put(keyName, image);

        return image;
    }

    /**
     * Loads and returns the appropriate image for a given package, archive or source object.
     * The image is cached. Successive calls will return the <em>same</em> object.
     *
     * @param object A {@link SdkSource} or {@link Package} or {@link Archive}.
     * @return A new or existing {@link Image}. The caller must NOT dispose the image (the
     *  image will disposed by {@link #dispose()}). The returned image can be null if the
     *  object is of an unknown type.
     */
    @Nullable
    public Image getImageForObject(@Nullable Object object) {

        if (object == null) {
            return null;
        }

        if (object instanceof Image) {
            return (Image) object;
        }

        String clz = object.getClass().getSimpleName();
        if (clz.endsWith(Package.class.getSimpleName())) {
            String name = clz.replaceFirst(Package.class.getSimpleName(), "")   //$NON-NLS-1$
                             .replace("SystemImage", "sysimg")    //$NON-NLS-1$ //$NON-NLS-2$
                             .toLowerCase(Locale.US);
            name += "_pkg_16.png";                                              //$NON-NLS-1$
            return getImageByName(name);
        }

        if (object instanceof PkgContentProvider.RepoSourceError) {
            return getImageByName("error_icon_16.png");                       //$NON-NLS-1$

        } else if (object instanceof PkgContentProvider.RepoSourceNotification) {
            return getImageByName("nopkg_icon_16.png");                       //$NON-NLS-1$
        }

        if (object instanceof Archive) {
            if (((Archive) object).isCompatible()) {
                return getImageByName("archive_icon16.png");                    //$NON-NLS-1$
            } else {
                return getImageByName("incompat_icon16.png");                   //$NON-NLS-1$
            }
        }

        if (object instanceof String) {
            return getImageByName((String) object);
        }


        if (object != null) {
            // For debugging
            // System.out.println("No image for object " + object.getClass().getSimpleName());
        }

        return null;
    }

    /**
     * Dispose all the images created by this factory so far.
     */
    public void dispose() {
        Iterator<Image> it = mImages.values().iterator();
        while(it.hasNext()) {
            Image img = it.next();
            if (img != null && img.isDisposed() == false) {
                img.dispose();
            }
            it.remove();
        }
    }

}
