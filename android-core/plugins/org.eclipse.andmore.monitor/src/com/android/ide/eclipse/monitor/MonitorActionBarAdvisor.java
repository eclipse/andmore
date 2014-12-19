/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.ide.eclipse.monitor;

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.internal.IWorkbenchGraphicConstants;
import org.eclipse.ui.internal.WorkbenchImages;

public class MonitorActionBarAdvisor extends ActionBarAdvisor {
    private IWorkbenchAction mQuitAction;
    private IWorkbenchAction mCopyAction;
    private IWorkbenchAction mSelectAllAction;
    private IWorkbenchAction mFindAction;
    private IWorkbenchAction mOpenPerspectiveAction;
    private IWorkbenchAction mResetPerspectiveAction;
    private IWorkbenchAction mPreferencesAction;
    private IWorkbenchAction mAboutAction;

    public MonitorActionBarAdvisor(IActionBarConfigurer configurer) {
        super(configurer);
    }

    @Override
    protected void makeActions(IWorkbenchWindow window) {
        mQuitAction = ActionFactory.QUIT.create(window);
        register(mQuitAction);

        mCopyAction = ActionFactory.COPY.create(window);
        register(mCopyAction);

        mSelectAllAction = ActionFactory.SELECT_ALL.create(window);
        register(mSelectAllAction);

        mFindAction = ActionFactory.FIND.create(window);
        mFindAction.setText("Find...");     // replace the default "Find and Replace..."
        register(mFindAction);

        mOpenPerspectiveAction = ActionFactory.OPEN_PERSPECTIVE_DIALOG.create(window);
        register(mOpenPerspectiveAction);

        mResetPerspectiveAction = ActionFactory.RESET_PERSPECTIVE.create(window);
        register(mResetPerspectiveAction);

        mPreferencesAction = ActionFactory.PREFERENCES.create(window);
        register(mPreferencesAction);

        mAboutAction = ActionFactory.ABOUT.create(window);
        register(mAboutAction);
    }

    @Override
    protected void fillMenuBar(IMenuManager menuBar) {
        MenuManager fileMenu = new MenuManager("&File", IWorkbenchActionConstants.M_FILE);
        MenuManager editMenu = new MenuManager("&Edit", IWorkbenchActionConstants.M_EDIT);
        MenuManager helpMenu = new MenuManager("&Help", IWorkbenchActionConstants.M_HELP);
        MenuManager windowMenu = new MenuManager("&Window", IWorkbenchActionConstants.M_WINDOW);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        menuBar.add(windowMenu);
        menuBar.add(helpMenu);

        // contents of File menu
        fileMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        fileMenu.add(mQuitAction);

        // contents of Edit menu
        editMenu.add(mCopyAction);
        editMenu.add(mSelectAllAction);
        editMenu.add(mFindAction);

        // contents of Window menu
        windowMenu.add(mOpenPerspectiveAction);
        windowMenu.add(mResetPerspectiveAction);
        windowMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        windowMenu.add(mPreferencesAction);

        // contents of Help menu
        helpMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        helpMenu.add(mAboutAction);
    };
}
