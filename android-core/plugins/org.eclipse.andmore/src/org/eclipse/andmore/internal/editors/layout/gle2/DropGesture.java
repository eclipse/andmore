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

package org.eclipse.andmore.internal.editors.layout.gle2;

import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;

/**
 * A {@link DropGesture} is a {@link Gesture} which deals with drag and drop, so
 * it has additional hooks for indicating whether the current position is
 * "valid", and in general gets access to the system drag and drop data
 * structures. See the {@link Gesture} documentation for more details on whether
 * you should choose a plain {@link Gesture} or a {@link DropGesture}.
 */
public abstract class DropGesture extends Gesture {
    /**
     * The cursor has entered the drop target boundaries.
     *
     * @param event The {@link DropTargetEvent} for this drag and drop event
     * @see DropTargetListener#dragEnter(DropTargetEvent)
     */
    public void dragEnter(DropTargetEvent event) {
    }

    /**
     * The cursor is moving over the drop target.
     *
     * @param event The {@link DropTargetEvent} for this drag and drop event
     * @see DropTargetListener#dragOver(DropTargetEvent)
     */
    public void dragOver(DropTargetEvent event) {
    }

    /**
     * The operation being performed has changed (usually due to the user
     * changing the selected modifier key(s) while dragging).
     *
     * @param event The {@link DropTargetEvent} for this drag and drop event
     * @see DropTargetListener#dragOperationChanged(DropTargetEvent)
     */
    public void dragOperationChanged(DropTargetEvent event) {
    }

    /**
     * The cursor has left the drop target boundaries OR the drop has been
     * canceled OR the data is about to be dropped.
     *
     * @param event The {@link DropTargetEvent} for this drag and drop event
     * @see DropTargetListener#dragLeave(DropTargetEvent)
     */
    public void dragLeave(DropTargetEvent event) {
    }

    /**
     * The drop is about to be performed. The drop target is given a last chance
     * to change the nature of the drop.
     *
     * @param event The {@link DropTargetEvent} for this drag and drop event
     * @see DropTargetListener#dropAccept(DropTargetEvent)
     */
    public void dropAccept(DropTargetEvent event) {
    }

    /**
     * The data is being dropped. The data field contains java format of the
     * data being dropped.
     *
     * @param event The {@link DropTargetEvent} for this drag and drop event
     * @see DropTargetListener#drop(DropTargetEvent)
     */
    public void drop(final DropTargetEvent event) {
    }
}
