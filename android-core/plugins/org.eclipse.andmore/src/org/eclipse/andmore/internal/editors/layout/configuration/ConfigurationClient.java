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
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.resources.ResourceRepository;
import com.android.resources.ResourceType;
import com.android.ide.common.resources.ResourceValueMap;
import com.android.sdklib.IAndroidTarget;

import java.util.Map;

import org.eclipse.andmore.internal.editors.layout.gle2.LayoutCanvas;
import org.eclipse.andmore.internal.editors.layout.gle2.IncludeFinder.Reference;

/**
 * Interface implemented by clients who embed a {@link ConfigurationChooser}.
 */
public interface ConfigurationClient {
    /**
     * The configuration is about to be changed.
     *
     * @param flags details about what changed; consult the {@code CFG_} flags
     *            in {@link Configuration} such as
     *            {@link Configuration#CFG_DEVICE},
     *            {@link Configuration#CFG_LOCALE}, etc.
     */
    void aboutToChange(int flags);

    /**
     * The configuration has changed. If the client returns false, it means that
     * the change was rejected. This typically means that changing the
     * configuration in this particular way makes a configuration which has a
     * better file match than the current client's file, so it will open that
     * file to edit the new configuration -- and the current configuration
     * should go back to editing the state prior to this change.
     *
     * @param flags details about what changed; consult the {@code CFG_} flags
     *            such as {@link Configuration#CFG_DEVICE},
     *            {@link Configuration#CFG_LOCALE}, etc.
     * @return true if the change was accepted, false if it was rejected.
     */
    boolean changed(int flags);

    /**
     * Compute the project resources
     *
     * @return the project resources as a {@link ResourceRepository}
     */
    @Nullable
    ResourceRepository getProjectResources();

    /**
     * Compute the framework resources
     *
     * @return the project resources as a {@link ResourceRepository}
     */
    @Nullable
    ResourceRepository getFrameworkResources();

    /**
     * Compute the framework resources for the given Android API target
     *
     * @param target the target to look up framework resources for
     * @return the project resources as a {@link ResourceRepository}
     */
    @Nullable
    ResourceRepository getFrameworkResources(@Nullable IAndroidTarget target);

    /**
     * Returns the configured project resources for the current file and
     * configuration
     *
     * @return resource type maps to names to resource values
     */
    @NonNull
    Map<ResourceType, ResourceValueMap> getConfiguredProjectResources();

    /**
     * Returns the configured framework resources for the current file and
     * configuration
     *
     * @return resource type maps to names to resource values
     */
    @NonNull
    Map<ResourceType, ResourceValueMap> getConfiguredFrameworkResources();

    /**
     * If the current layout is an included layout rendered within an outer layout,
     * returns the outer layout.
     *
     * @return the outer including layout, or null
     */
    @Nullable
    Reference getIncludedWithin();

    /**
     * Called when the "Create" button is clicked.
     */
    void createConfigFile();

    /**
     * Called when an associated activity is picked
     *
     * @param fqcn the fully qualified class name for the associated activity context
     */
    void setActivity(@NonNull String fqcn);

    /**
     * Returns the associated layout canvas, if any
     *
     * @return the canvas, if any
     */
    @Nullable
    LayoutCanvas getCanvas();
}
