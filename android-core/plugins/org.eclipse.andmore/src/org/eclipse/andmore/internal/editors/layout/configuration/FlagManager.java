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
package org.eclipse.andmore.internal.editors.layout.configuration;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.resources.LocaleManager;
import com.android.ide.common.resources.configuration.LanguageQualifier;
import com.android.ide.common.resources.configuration.RegionQualifier;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;

import org.eclipse.andmore.internal.editors.IconFactory;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wb.internal.core.DesignerPlugin;

import java.util.Locale;
import java.util.Map;

/**
 * The {@linkplain FlagManager} provides access to flags for regions known
 * to {@link LocaleManager}. It also contains some locale related display
 * functions.
 * <p>
 * All the flag images came from the WindowBuilder subversion repository
 * http://dev.eclipse.org/svnroot/tools/org.eclipse.windowbuilder/trunk (and in
 * particular, a snapshot of revision 424). However, it appears that the icons
 * are from http://www.famfamfam.com/lab/icons/flags/ which states that "these
 * flag icons are available for free use for any purpose with no requirement for
 * attribution." Adding the URL here such that we can check back occasionally
 * and see if there are corrections or updates. Also note that the flag names
 * are in ISO 3166-1 alpha-2 country codes.
 */
public class FlagManager {
    private static final FlagManager sInstance = new FlagManager();

    /**
     * Returns the {@linkplain FlagManager} singleton
     *
     * @return the {@linkplain FlagManager} singleton, never null
     */
    @NonNull
    public static FlagManager get() {
        return sInstance;
    }

    /** Use the {@link #get()} factory method */
    private FlagManager() {
    }

    /** Map from region to flag icon */
    private Map<String, Image> mImageMap = Maps.newHashMap();

    /**
     * Returns the empty flag icon used to indicate an unknown country
     *
     * @return the globe icon used to indicate an unknown country
     */
    public static Image getEmptyIcon() {
      return DesignerPlugin.getImage("nls/flags/flag_empty.png"); //$NON-NLS-1$
    }

    /**
     * Returns the globe icon used to indicate "any" language
     *
     * @return the globe icon used to indicate "any" language
     */
    public static Image getGlobeIcon() {
        return IconFactory.getInstance().getIcon("globe"); //$NON-NLS-1$
    }

    /**
     * Returns the flag for the given language and region.
     *
     * @param language the language, or null (if null, region must not be null),
     *            the 2 letter language code (ISO 639-1), in lower case
     * @param region the region, or null (if null, language must not be null),
     *            the 2 letter region code (ISO 3166-1 alpha-2), in upper case
     * @return a suitable flag icon, or null
     */
    @Nullable
    public Image getFlag(@Nullable String language, @Nullable String region) {
        assert region != null || language != null;
        if (region == null || region.isEmpty()) {
            // Look up the region for a given language
            assert language != null;

            // Prefer the local registration of the current locale; even if
            // for example the default locale for English is the US, if the current
            // default locale is English, then use its associated country, which could
            // for example be Australia.
            Locale locale = Locale.getDefault();
            if (language.equals(locale.getLanguage())) {
                Image flag = getFlag(locale.getCountry());
                if (flag != null) {
                    return flag;
                }
            }

            // Special cases where we have a dedicated flag available:
            if (language.equals("ca")) {        //$NON-NLS-1$
                region = "catalonia";           //$NON-NLS-1$
            } else if (language.equals("gd")) { //$NON-NLS-1$
                region = "scotland";            //$NON-NLS-1$
            } else if (language.equals("cy")) { //$NON-NLS-1$
                region = "wales";               //$NON-NLS-1$
            } else {
                // Attempt to look up the country from the language
                region = LocaleManager.getLanguageRegion(language);
            }
        }

        if (region == null || region.isEmpty()) {
            // No country specified, and the language is for a country we
            // don't have a flag for
            return null;
        }

        return getIcon(region);
    }

    /**
     * Returns the flag for the given language and region.
     *
     * @param language the language qualifier, or null (if null, region must not be null),
     * @param region the region, or null (if null, language must not be null),
     * @return a suitable flag icon, or null
     */
    public Image getFlag(LanguageQualifier language, RegionQualifier region) {
        String languageCode = language != null ? language.getValue() : null;
        String regionCode = region != null ? region.getValue() : null;
        if (LanguageQualifier.FAKE_LANG_VALUE.equals(languageCode)) {
            languageCode = null;
        }
        if (RegionQualifier.FAKE_REGION_VALUE.equals(regionCode)) {
            regionCode = null;
        }
        return getFlag(languageCode, regionCode);
    }

    /**
     * Returns a flag for a given resource folder name (such as
     * {@code values-en-rUS}), or null
     *
     * @param folder the folder name
     * @return a corresponding flag icon, or null if none was found
     */
    @Nullable
    public Image getFlagForFolderName(@NonNull String folder) {
        RegionQualifier region = null;
        LanguageQualifier language = null;
        for (String qualifier : Splitter.on('-').split(folder)) {
            if (qualifier.length() == 3) {
                region = RegionQualifier.getQualifier(qualifier);
                if (region != null) {
                    break;
                }
            } else if (qualifier.length() == 2 && language == null) {
                language = LanguageQualifier.getQualifier(qualifier);
            }
        }
        if (region != null || language != null) {
            return FlagManager.get().getFlag(language, region);
        }

        return null;
    }

    /**
     * Returns the flag for the given region.
     *
     * @param region the 2 letter region code (ISO 3166-1 alpha-2), in upper case
     * @return a suitable flag icon, or null
     */
    @Nullable
    public Image getFlag(@NonNull String region) {
        assert region.length() == 2
                && Character.isUpperCase(region.charAt(0))
                && Character.isUpperCase(region.charAt(1)) : region;

        return getIcon(region);
    }

    private Image getIcon(@NonNull String base) {
        Image flagImage = mImageMap.get(base);
          if (flagImage == null) {
              // TODO: Special case locale currently running on system such
              // that the current country matches the current locale
              if (mImageMap.containsKey(base)) {
                  // Already checked: there's just no image there
                  return null;
              }
              String flagFileName = base.toLowerCase(Locale.US) + ".png"; //$NON-NLS-1$
              flagImage = DesignerPlugin.getImage("nls/flags/" + flagFileName); //$NON-NLS-1$
              mImageMap.put(base, flagImage);
          }

        return flagImage;
    }
}
