/*
 * Copyright (C) 2010 The Android Open Source Project
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

package org.eclipse.andmore.hierarchyviewer.views;

import com.android.ddmuilib.ImageLoader;
import com.android.hierarchyviewerlib.HierarchyViewerDirector;
import com.android.hierarchyviewerlib.actions.PixelPerfectAutoRefreshAction;
import com.android.hierarchyviewerlib.models.PixelPerfectModel;
import com.android.hierarchyviewerlib.models.PixelPerfectModel.IImageChangeListener;
import com.android.hierarchyviewerlib.ui.PixelPerfectControls;
import com.android.hierarchyviewerlib.ui.PixelPerfectLoupe;
import com.android.hierarchyviewerlib.ui.PixelPerfectPixelPanel;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;

public class PixelPerfectLoupeView extends ViewPart implements IImageChangeListener {

	public static final String ID = "org.eclipse.andmore.hierarchyviewer.views.PixelPerfectLoupeView"; //$NON-NLS-1$

	private PixelPerfectLoupe mPixelPerfectLoupe;

	private Action mShowInLoupeAction = new Action("&Show Overlay", Action.AS_CHECK_BOX) {
		@Override
		public void run() {
			mPixelPerfectLoupe.setShowOverlay(isChecked());
		}
	};

	@Override
	public void createPartControl(Composite parent) {
		mShowInLoupeAction.setAccelerator(SWT.MOD1 + 'S');
		ImageLoader imageLoader = ImageLoader.getLoader(HierarchyViewerDirector.class);
		Image image = imageLoader.loadImage("show-overlay.png", Display.getDefault()); //$NON-NLS-1$
		mShowInLoupeAction.setImageDescriptor(ImageDescriptor.createFromImage(image));
		mShowInLoupeAction.setToolTipText("Show the overlay in the loupe view");
		mShowInLoupeAction.setEnabled(PixelPerfectModel.getModel().getOverlayImage() != null);
		PixelPerfectModel.getModel().addImageChangeListener(this);

		GridLayout loupeLayout = new GridLayout();
		loupeLayout.marginWidth = loupeLayout.marginHeight = 0;
		loupeLayout.horizontalSpacing = loupeLayout.verticalSpacing = 0;
		parent.setLayout(loupeLayout);

		Composite pixelPerfectLoupeBorder = new Composite(parent, SWT.BORDER);
		pixelPerfectLoupeBorder.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout pixelPerfectLoupeBorderGridLayout = new GridLayout();
		pixelPerfectLoupeBorderGridLayout.marginWidth = pixelPerfectLoupeBorderGridLayout.marginHeight = 0;
		pixelPerfectLoupeBorderGridLayout.horizontalSpacing = pixelPerfectLoupeBorderGridLayout.verticalSpacing = 0;
		pixelPerfectLoupeBorder.setLayout(pixelPerfectLoupeBorderGridLayout);

		mPixelPerfectLoupe = new PixelPerfectLoupe(pixelPerfectLoupeBorder);
		mPixelPerfectLoupe.setLayoutData(new GridData(GridData.FILL_BOTH));

		PixelPerfectPixelPanel pixelPerfectPixelPanel = new PixelPerfectPixelPanel(pixelPerfectLoupeBorder);
		pixelPerfectPixelPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		PixelPerfectControls pixelPerfectControls = new PixelPerfectControls(parent);
		pixelPerfectControls.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		placeActions();
	}

	private void placeActions() {
		IActionBars actionBars = getViewSite().getActionBars();

		IMenuManager mm = actionBars.getMenuManager();
		mm.removeAll();
		mm.add(PixelPerfectAutoRefreshAction.getAction());
		mm.add(mShowInLoupeAction);

		IToolBarManager tm = actionBars.getToolBarManager();
		tm.removeAll();
		tm.add(PixelPerfectAutoRefreshAction.getAction());
		tm.add(mShowInLoupeAction);
	}

	@Override
	public void dispose() {
		super.dispose();
		PixelPerfectModel.getModel().removeImageChangeListener(this);
	}

	@Override
	public void setFocus() {
		mPixelPerfectLoupe.setFocus();
	}

	@Override
	public void crosshairMoved() {
		// pass
	}

	@Override
	public void treeChanged() {
		// pass
	}

	@Override
	public void imageChanged() {
		// pass
	}

	@Override
	public void imageLoaded() {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				Image overlayImage = PixelPerfectModel.getModel().getOverlayImage();
				mShowInLoupeAction.setEnabled(overlayImage != null);
			}
		});
	}

	@Override
	public void overlayChanged() {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				mShowInLoupeAction.setEnabled(PixelPerfectModel.getModel().getOverlayImage() != null);
			}
		});
	}

	@Override
	public void overlayTransparencyChanged() {
		// pass
	}

	@Override
	public void selectionChanged() {
		// pass
	}

	@Override
	public void zoomChanged() {
		// pass
	}

}
