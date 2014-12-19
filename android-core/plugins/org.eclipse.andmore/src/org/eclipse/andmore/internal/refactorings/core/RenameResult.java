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
package org.eclipse.andmore.internal.refactorings.core;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;

/**
 * A result from a renaming operation
 */
public class RenameResult {
    private boolean mCanceled;
    private boolean mUnavailable;
    private @Nullable String mName;
    private boolean mClear;

    /**
     * Constructs a new rename result
     */
    private RenameResult() {
    }

    /**
     * Creates a new blank {@linkplain RenameResult}
     * @return a new result
     */
    @NonNull
    public static RenameResult create() {
        return new RenameResult();
    }

    /**
     * Creates a new {@linkplain RenameResult} for a user canceled renaming operation
     * @return a canceled operation
     */
    @NonNull
    public static RenameResult canceled() {
        return new RenameResult().setCanceled(true);
    }

    /**
     * Creates a {@linkplain RenameResult} for a renaming operation that was
     * not available (for example because the field attempted to be renamed
     * does not yet exist (or does not exist any more)
     *
     * @return a new result
     */
    @NonNull
    public static RenameResult unavailable() {
        return new RenameResult().setUnavailable(true);
    }

    /**
     * Creates a new {@linkplain RenameResult} for a successful renaming
     * operation to the given name
     *
     * @param name the new name
     * @return a new result
     */
    @NonNull
    public static RenameResult name(@Nullable String name) {
        return new RenameResult().setName(name);
    }

    /**
     * Marks this result as canceled
     *
     * @param canceled whether the result was canceled
     * @return this, for constructor chaining
     */
    @NonNull
    public RenameResult setCanceled(boolean canceled) {
        mCanceled = canceled;
        return this;
    }

    /**
     * Marks this result as unavailable
     *
     * @param unavailable whether this result was unavailable
     * @return this, for constructor chaining
     */
    @NonNull
    public RenameResult setUnavailable(boolean unavailable) {
        mUnavailable = unavailable;
        return this;
    }

    /**
     * Sets the new name of the renaming operation
     *
     * @param name the new name
     * @return this, for constructor chaining
     */
    @NonNull
    public RenameResult setName(@Nullable String name) {
        mName = name;
        return this;
    }

    /**
     * Marks this result as clearing the name (reverting it back to the default)
     *
     * @param clear whether the name was cleared
     * @return this, for constructor chaining
     */
    @NonNull
    public RenameResult setCleared(boolean clear) {
        mClear = clear;
        return this;
    }

    /**
     * Returns whether this result represents a canceled renaming operation
     *
     * @return true if the operation was canceled
     */
    public boolean isCanceled() {
        return mCanceled;
    }

    /**
     * Returns whether this result represents an unavailable renaming operation
     *
     * @return true if the operation was not available
     */
    public boolean isUnavailable() {
        return mUnavailable;
    }

    /**
     * Returns whether this result represents a renaming back to the default (possibly
     * clear) name. In this case, {@link #getName()} will return {@code null}.
     *
     * @return true if the name should be reset
     */
    public boolean isCleared() {
        return mClear;
    }

    /**
     * Returns the new name.
     *
     * @return the new name
     */
    @Nullable
    public String getName() {
        return mName;
    }
}