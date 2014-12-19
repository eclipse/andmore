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

import static com.android.ide.common.resources.configuration.LanguageQualifier.FAKE_LANG_VALUE;
import static com.android.ide.common.resources.configuration.RegionQualifier.FAKE_REGION_VALUE;

import com.android.annotations.NonNull;
import com.android.ide.common.resources.configuration.LanguageQualifier;
import com.android.ide.common.resources.configuration.RegionQualifier;
import com.google.common.base.Objects;

import org.eclipse.swt.graphics.Image;

/** A language,region pair */
public class Locale {
    /** A special marker region qualifier representing any region */
    public static final RegionQualifier ANY_REGION = new RegionQualifier(FAKE_REGION_VALUE);

    /** A special marker language qualifier representing any language */
    public static final LanguageQualifier ANY_LANGUAGE = new LanguageQualifier(FAKE_LANG_VALUE);

    /** A locale which matches any language and region */
    public static final Locale ANY = new Locale(ANY_LANGUAGE, ANY_REGION);

    /** The language qualifier, or {@link #ANY_LANGUAGE} if this locale matches any language  */
    @NonNull
    public final LanguageQualifier language;

    /** The language qualifier, or {@link #ANY_REGION} if this locale matches any region  */
    @NonNull
    public final RegionQualifier region;

    /**
     * Constructs a new {@linkplain Locale} matching a given language in a given locale.
     *
     * @param language the language
     * @param region the region
     */
    private Locale(@NonNull LanguageQualifier language, @NonNull RegionQualifier region) {
        if (language.getValue().equals(FAKE_LANG_VALUE)) {
            language = ANY_LANGUAGE;
        }
        if (region.getValue().equals(FAKE_REGION_VALUE)) {
            region = ANY_REGION;
        }
        this.language = language;
        this.region = region;
    }

    /**
     * Constructs a new {@linkplain Locale} matching a given language in a given specific locale.
     *
     * @param language the language
     * @param region the region
     * @return a locale with the given language and region
     */
    @NonNull
    public static Locale create(
            @NonNull LanguageQualifier language,
            @NonNull RegionQualifier region) {
        return new Locale(language, region);
    }

    /**
     * Constructs a new {@linkplain Locale} for the given language, matching any regions.
     *
     * @param language the language
     * @return a locale with the given language and region
     */
    public static Locale create(@NonNull LanguageQualifier language) {
        return new Locale(language, ANY_REGION);
    }

    /**
     * Returns a flag image to use for this locale
     *
     * @return a flag image, or a default globe icon
     */
    @NonNull
    public Image getFlagImage() {
        Image image = null;
        String languageCode = hasLanguage() ? language.getValue() : null;
        String regionCode = hasRegion() ? region.getValue() : null;
        if (languageCode == null && regionCode == null) {
            return FlagManager.getGlobeIcon();
        } else {
            FlagManager icons = FlagManager.get();
            image = icons.getFlag(languageCode, regionCode);
            if (image == null) {
                image = FlagManager.getEmptyIcon();
            }

            return image;
        }
    }

    /**
     * Returns true if this locale specifies a specific language. This is true
     * for all locales except {@link #ANY}.
     *
     * @return true if this locale specifies a specific language
     */
    public boolean hasLanguage() {
        return language != ANY_LANGUAGE;
    }

    /**
     * Returns true if this locale specifies a specific region
     *
     * @return true if this locale specifies a region
     */
    public boolean hasRegion() {
        return region != ANY_REGION;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((language == null) ? 0 : language.hashCode());
        result = prime * result + ((region == null) ? 0 : region.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Locale other = (Locale) obj;
        if (language == null) {
            if (other.language != null)
                return false;
        } else if (!language.equals(other.language))
            return false;
        if (region == null) {
            if (other.region != null)
                return false;
        } else if (!region.equals(other.region))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).omitNullValues()
            .addValue(language.getValue())
            .addValue(region.getValue())
            .toString();
    }

    /**
     * Returns the locale formatted as language-region. If region is not set,
     * language is returned. If language is not set, empty string is returned.
     */
    public String toLocaleId() {
        // Return lang-reg only if both lang and reg are present. Else return
        // lang.
        return hasLanguage() && hasRegion() ?
                language.getValue() + "-" + region.getValue()
                : hasLanguage() ? language.getValue() : "";
    }
}
