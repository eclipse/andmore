
package org.eclipse.andmore.internal.editors.layout.gle2;

import com.android.ide.common.rendering.api.Capability;

import org.eclipse.andmore.internal.editors.layout.LayoutEditorDelegate;
import org.eclipse.andmore.internal.editors.layout.gle2.IncludeFinder.Reference;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Menu;

import java.util.List;

/**
 * Action which creates a submenu for the "Show Included In" action
 */
class ShowWithinMenu extends SubmenuAction {
    private LayoutEditorDelegate mEditorDelegate;

    ShowWithinMenu(LayoutEditorDelegate editorDelegate) {
        super("Show Included In");
        mEditorDelegate = editorDelegate;
    }

    @Override
    protected void addMenuItems(Menu menu) {
        GraphicalEditorPart graphicalEditor = mEditorDelegate.getGraphicalEditor();
        IFile file = graphicalEditor.getEditedFile();
        if (graphicalEditor.renderingSupports(Capability.EMBEDDED_LAYOUT)) {
            IProject project = file.getProject();
            IncludeFinder finder = IncludeFinder.get(project);
            final List<Reference> includedBy = finder.getIncludedBy(file);

            if (includedBy != null && includedBy.size() > 0) {
                for (final Reference reference : includedBy) {
                    String title = reference.getDisplayName();
                    IAction action = new ShowWithinAction(title, reference);
                    new ActionContributionItem(action).fill(menu, -1);
                }
                new Separator().fill(menu, -1);
            }
            IAction action = new ShowWithinAction("Nothing", null);
            if (includedBy == null || includedBy.size() == 0) {
                action.setEnabled(false);
            }
            new ActionContributionItem(action).fill(menu, -1);
        } else {
            addDisabledMessageItem("Not supported on platform");
        }
    }

    /** Action to select one particular include-context */
    private class ShowWithinAction extends Action {
        private Reference mReference;

        public ShowWithinAction(String title, Reference reference) {
            super(title, IAction.AS_RADIO_BUTTON);
            mReference = reference;
        }

        @Override
        public boolean isChecked() {
            Reference within = mEditorDelegate.getGraphicalEditor().getIncludedWithin();
            if (within == null) {
                return mReference == null;
            } else {
                return within.equals(mReference);
            }
        }

        @Override
        public void run() {
            if (!isChecked()) {
                mEditorDelegate.getGraphicalEditor().showIn(mReference);
            }
        }
    }
}
