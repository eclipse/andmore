/*
 * Copyright (C) 2010 The Android Open Source Project
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

import com.android.annotations.NonNull;
import com.android.ide.common.api.IAttributeInfo;

import java.util.EnumSet;

/** Test/mock implementation of {@link IAttributeInfo} */
public class TestAttributeInfo implements IAttributeInfo {
    private final String mName;
    private final EnumSet<Format> mFormats;
    private final String mDefinedBy;
    private final String[] mEnumValues;
    private final String[] mFlagValues;
    private final String mJavadoc;

    public TestAttributeInfo(String name) {
        this(name, null, null, null, null, null);
    }

    public TestAttributeInfo(String name, EnumSet<Format> formats, String definedBy,
            String[] enumValues, String[] flagValues, String javadoc) {
        super();
        this.mName = name;
        this.mFormats = formats;
        this.mDefinedBy = definedBy;
        this.mEnumValues = enumValues;
        this.mFlagValues = flagValues;
        this.mJavadoc = javadoc;
    }

    @Override
    public String getDeprecatedDoc() {
        return null;
    }

    @Override
    public String[] getEnumValues() {
        return mEnumValues;
    }

    @Override
    public String[] getFlagValues() {
        return mFlagValues;
    }

    @Override
    public @NonNull EnumSet<Format> getFormats() {
        return mFormats;
    }

    @Override
    public @NonNull String getJavaDoc() {
        return mJavadoc;
    }

    @Override
    public @NonNull String getName() {
        return mName;
    }

    @Override
    public @NonNull String getDefinedBy() {
        return mDefinedBy;
    }
}