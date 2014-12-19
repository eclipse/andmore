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

import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.editors.layout.gre.RulesEngine;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;


/**
 * JFace {@link Viewer} wrapper around {@link LayoutCanvas}.
 * <p/>
 * The viewer is owned by {@link GraphicalEditorPart}.
 * <p/>
 * The viewer is an {@link ISelectionProvider} instance and is set as the
 * site's main {@link ISelectionProvider} by the editor part. Consequently
 * canvas' selection changes are broadcasted to anyone listening, which includes
 * the part itself as well as the associated outline and property sheet pages.
 */
class LayoutCanvasViewer extends Viewer implements IPostSelectionProvider {

    private LayoutCanvas mCanvas;
    private final LayoutEditorDelegate mEditorDelegate;

    public LayoutCanvasViewer(LayoutEditorDelegate editorDelegate,
            RulesEngine rulesEngine,
            Composite parent,
            int style) {
        mEditorDelegate = editorDelegate;
        mCanvas = new LayoutCanvas(editorDelegate, rulesEngine, parent, style);

        mCanvas.getSelectionManager().addSelectionChangedListener(mSelectionListener);
    }

    private ISelectionChangedListener mSelectionListener = new ISelectionChangedListener() {
        @Override
        public void selectionChanged(SelectionChangedEvent event) {
            fireSelectionChanged(event);
            firePostSelectionChanged(event);
        }
    };

    @Override
    public Control getControl() {
        return mCanvas;
    }

    /**
     * Returns the underlying {@link LayoutCanvas}.
     * This is the same control as returned by {@link #getControl()} but clients
     * have it already casted in the right type.
     * <p/>
     * This can never be null.
     * @return The underlying {@link LayoutCanvas}.
     */
    public LayoutCanvas getCanvas() {
        return mCanvas;
    }

    /**
     * Returns the current layout editor's input.
     */
    @Override
    public Object getInput() {
        return mEditorDelegate.getEditor().getEditorInput();
    }

    /**
     * Unused. We don't support switching the input.
     */
    @Override
    public void setInput(Object input) {
    }

    /**
     * Returns a new {@link TreeSelection} where each {@link TreePath} item
     * is a {@link CanvasViewInfo}.
     */
    @Override
    public ISelection getSelection() {
        return mCanvas.getSelectionManager().getSelection();
    }

    /**
     * Sets a new selection. <code>reveal</code> is ignored right now.
     * <p/>
     * The selection can be null, which is interpreted as an empty selection.
     */
    @Override
    public void setSelection(ISelection selection, boolean reveal) {
        if (mEditorDelegate.getEditor().getIgnoreXmlUpdate()) {
            return;
        }
        mCanvas.getSelectionManager().setSelection(selection);
    }

    /** Unused. Refreshing is done solely by the owning {@link LayoutEditorDelegate}. */
    @Override
    public void refresh() {
        // ignore
    }

    public void dispose() {
        if (mSelectionListener != null) {
            mCanvas.getSelectionManager().removeSelectionChangedListener(mSelectionListener);
        }
        if (mCanvas != null) {
            mCanvas.dispose();
            mCanvas = null;
        }
    }

    // ---- Implements IPostSelectionProvider ----

    private ListenerList mPostChangedListeners = new ListenerList();

    @Override
    public void addPostSelectionChangedListener(ISelectionChangedListener listener) {
        mPostChangedListeners.add(listener);
    }

    @Override
    public void removePostSelectionChangedListener(ISelectionChangedListener listener) {
        mPostChangedListeners.remove(listener);
    }

    protected void firePostSelectionChanged(final SelectionChangedEvent event) {
        Object[] listeners = mPostChangedListeners.getListeners();
        for (int i = 0; i < listeners.length; i++) {
            final ISelectionChangedListener l = (ISelectionChangedListener) listeners[i];
            SafeRunnable.run(new SafeRunnable() {
                @Override
                public void run() {
                    l.selectionChanged(event);
                }
            });
        }
    }
}
