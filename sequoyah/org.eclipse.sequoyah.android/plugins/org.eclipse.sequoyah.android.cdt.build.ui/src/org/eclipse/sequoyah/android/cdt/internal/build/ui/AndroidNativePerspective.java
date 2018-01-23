package org.eclipse.sequoyah.android.cdt.internal.build.ui;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class AndroidNativePerspective implements IPerspectiveFactory
{

    private final String VIEW_CONSOLE = "org.eclipse.ui.console.ConsoleView";

    private final String VIEW_MAKE_TARGET = "org.eclipse.cdt.make.ui.views.MakeView";

    private final String VIEW_PROJECT_EXPLORER = "org.eclipse.ui.navigator.ProjectExplorer";

    // DDMS Views

    private final String DDMSVIEW_EMULATOR_CONTROL =
            "com.android.ide.eclipse.ddms.views.EmulatorControlView";

    private final String DDMSVIEW_LOGCAT = "com.android.ide.eclipse.ddms.views.LogCatView";

    private final String DDMSVIEW_FILE_EXPLORER =
            "com.android.ide.eclipse.ddms.views.FileExplorerView";

    private final String PERSPECTIVE_DDMS = "com.android.ide.eclipse.ddms.Perspective";

    private final String PERSPECTIVE_DEBUG = "org.eclipse.debug.ui.DebugPerspective";

    private final String LAUNCH_COOLBAR_SHORTCUT = "org.eclipse.debug.ui.launchActionSet";

    private final String VIEW_SEQUOYAH_DEV_MGT =
            "org.eclipse.sequoyah.device.framework.ui.InstanceMgtView";

    private final String WIZARD_ANDROID_XML =
            "com.android.ide.eclipse.editors.wizards.NewXmlFileWizard";

    private final String WIZARD_JAVA_PACKAGE =
            "org.eclipse.jdt.ui.wizards.NewPackageCreationWizard";

    private final String WIZARD_JAVA_CLASS = "org.eclipse.jdt.ui.wizards.NewClassCreationWizard";

    private final String WIZARD_JAVA_INTERFACE =
            "org.eclipse.jdt.ui.wizards.NewInterfaceCreationWizard";

    private final String WIZARD_NEW_FOLDER = "org.eclipse.ui.wizards.new.folder";

    private static final String C_ELEMENT_CREATION_ACTION_SET =
            "org.eclipse.cdt.ui.CElementCreationActionSet";

    public void createInitialLayout(IPageLayout layout)
    {
        String editorArea = layout.getEditorArea();

        IFolderLayout folder1 =
                layout.createFolder("topLeft", IPageLayout.LEFT, (float) 0.25, editorArea); //$NON-NLS-1$
        folder1.addView(VIEW_PROJECT_EXPLORER);
        folder1.addView(DDMSVIEW_FILE_EXPLORER);

        IFolderLayout folder2 =
                layout.createFolder("bottom", IPageLayout.BOTTOM, (float) 0.75, editorArea); //$NON-NLS-1$
        folder2.addView(VIEW_SEQUOYAH_DEV_MGT);
        folder2.addView(DDMSVIEW_EMULATOR_CONTROL);
        folder2.addView(DDMSVIEW_LOGCAT);
        folder2.addView(VIEW_CONSOLE);
        folder2.addView(IPageLayout.ID_PROBLEM_VIEW);
        folder2.addView(IPageLayout.ID_TASK_LIST);

        IFolderLayout folder3 =
                layout.createFolder("leftBottom", IPageLayout.BOTTOM, 0.59f, "topLeft");
        folder3.addView(IPageLayout.ID_OUTLINE);
        folder3.addView(VIEW_MAKE_TARGET);

        layout.addShowViewShortcut(VIEW_SEQUOYAH_DEV_MGT);

        layout.addPerspectiveShortcut(PERSPECTIVE_DDMS);
        layout.addPerspectiveShortcut(PERSPECTIVE_DEBUG);

        layout.addActionSet(LAUNCH_COOLBAR_SHORTCUT);
        layout.addActionSet(C_ELEMENT_CREATION_ACTION_SET);
        layout.addActionSet(IPageLayout.ID_NAVIGATE_ACTION_SET);

        // views - build console
        layout.addShowViewShortcut(VIEW_CONSOLE);

        layout.addNewWizardShortcut(WIZARD_ANDROID_XML);
        layout.addNewWizardShortcut(WIZARD_JAVA_PACKAGE);
        layout.addNewWizardShortcut(WIZARD_NEW_FOLDER);
        layout.addNewWizardShortcut(WIZARD_JAVA_CLASS);
        layout.addNewWizardShortcut(WIZARD_JAVA_INTERFACE);

        // views - standard workbench
        layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
        layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
        layout.addShowViewShortcut(VIEW_MAKE_TARGET);
        layout.addShowViewShortcut(IPageLayout.ID_PROP_SHEET);
        layout.addShowViewShortcut(IPageLayout.ID_TASK_LIST);
        layout.addShowViewShortcut(VIEW_SEQUOYAH_DEV_MGT);
    }

}
