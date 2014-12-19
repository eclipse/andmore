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

package org.eclipse.andmore.internal.editors;

import org.eclipse.andmore.AdtPlugin;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import java.util.ArrayList;
import java.util.List;

/**
 * Outline used for XML editors that have multiple pages with separate outlines:
 * switches between them
 * <p>
 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=1917
 * <p>
 * Modeled after .org.eclipse.pde.internal.ui.editor.PDEMultiPageContentOutline
 */
public class XmlEditorMultiOutline extends Page implements IContentOutlinePage,
        ISelectionChangedListener {
    private boolean mDisposed;
    private PageBook mPageBook;
    private IContentOutlinePage mCurrentPage;
    private IActionBars mActionBars;
    private IContentOutlinePage mEmptyPage;
    private List<ISelectionChangedListener> mListeners;
    private ISelection mSelection;

    public XmlEditorMultiOutline() {
    }

    @Override
    public Control getControl() {
        return mPageBook;
    }

    @Override
    public void createControl(Composite parent) {
        mPageBook = new PageBook(parent, SWT.NONE);
    }

    @Override
    public void dispose() {
        mDisposed = true;
        mListeners = null;
        if (mPageBook != null && !mPageBook.isDisposed()) {
            mPageBook.dispose();
            mPageBook = null;
        }
        if (mEmptyPage != null) {
            mEmptyPage.dispose();
            mEmptyPage = null;
        }
    }

    public boolean isDisposed() {
        return mDisposed;
    }

    @Override
    public void makeContributions(IMenuManager menuManager, IToolBarManager toolBarManager,
            IStatusLineManager statusLineManager) {
    }

    @Override
    public void setActionBars(IActionBars actionBars) {
        mActionBars = actionBars;
        if (mCurrentPage != null) {
            setPageActive(mCurrentPage);
        }
    }

    @Override
    public void setFocus() {
        if (mCurrentPage != null) {
            mCurrentPage.setFocus();
        }
    }

    @Override
    public void addSelectionChangedListener(ISelectionChangedListener listener) {
        if (mListeners == null) {
            mListeners = new ArrayList<ISelectionChangedListener>(2);
        }
        mListeners.add(listener);
    }

    @Override
    public void removeSelectionChangedListener(ISelectionChangedListener listener) {
        mListeners.remove(listener);
    }

    @Override
    public ISelection getSelection() {
        return mSelection;
    }

    @Override
    public void selectionChanged(SelectionChangedEvent event) {
        setSelection(event.getSelection());
    }

    public void setPageActive(IContentOutlinePage page) {
        if (page == null) {
            if (mEmptyPage == null) {
                mEmptyPage = new EmptyPage();
            }
            page = mEmptyPage;
        }
        if (mCurrentPage != null) {
            mCurrentPage.removeSelectionChangedListener(this);
        }
        page.addSelectionChangedListener(this);
        mCurrentPage = page;
        // Still initializing?
        if (mPageBook == null) {
            return;
        }
        Control control = page.getControl();
        if (control == null || control.isDisposed()) {
            if (page instanceof IPageBookViewPage) {
                try {
                    ((IPageBookViewPage) page).init(getSite());
                } catch (PartInitException e) {
                    AdtPlugin.log(e, null);
                }
            }
            page.createControl(mPageBook);
            page.setActionBars(mActionBars);
            control = page.getControl();
        }
        mPageBook.showPage(control);
    }

    @Override
    public void setSelection(ISelection selection) {
        mSelection = selection;
        if (mListeners != null) {
            SelectionChangedEvent e = new SelectionChangedEvent(this, selection);
            for (int i = 0; i < mListeners.size(); i++) {
                mListeners.get(i).selectionChanged(e);
            }
        }
    }

    private static class EmptyPage implements IContentOutlinePage {
        private Composite mControl;

        private EmptyPage() {
        }

        @Override
        public void createControl(Composite parent) {
            mControl = new Composite(parent, SWT.NULL);
        }

        @Override
        public void dispose() {
        }

        @Override
        public Control getControl() {
            return mControl;
        }

        @Override
        public void setActionBars(IActionBars actionBars) {
        }

        @Override
        public void setFocus() {
        }

        @Override
        public void addSelectionChangedListener(ISelectionChangedListener listener) {
        }

        @Override
        public ISelection getSelection() {
            return StructuredSelection.EMPTY;
        }

        @Override
        public void removeSelectionChangedListener(ISelectionChangedListener listener) {
        }

        @Override
        public void setSelection(ISelection selection) {
        }
    }
}
