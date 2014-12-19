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

package org.eclipse.andmore.gltrace.views.detail;

import org.eclipse.andmore.gltrace.GLProtoBuf.GLMessage.Function;
import org.eclipse.andmore.gltrace.model.GLCall;
import org.eclipse.andmore.gltrace.model.GLTrace;
import org.eclipse.andmore.gltrace.views.FitToCanvasAction;
import org.eclipse.andmore.gltrace.views.SaveImageAction;
import org.eclipse.andmore.gltrace.widgets.ImageCanvas;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import java.util.Arrays;
import java.util.List;

public class GlDrawCallDetailProvider implements ICallDetailProvider {
	private ImageCanvas mImageCanvas;
	private FitToCanvasAction mFitToCanvasAction;
	private SaveImageAction mSaveImageAction;
	private List<IContributionItem> mToolBarItems;

	@Override
	public boolean isApplicable(GLCall call) {
		return (call.getFunction() == Function.glDrawArrays || call.getFunction() == Function.glDrawElements)
				&& call.hasFb();
	}

	@Override
	public void createControl(Composite parent) {
		mImageCanvas = new ImageCanvas(parent);
		mImageCanvas.setFitToCanvas(false);

		mFitToCanvasAction = new FitToCanvasAction(false, mImageCanvas);
		mSaveImageAction = new SaveImageAction(mImageCanvas);

		mToolBarItems = Arrays.asList((IContributionItem) new ActionContributionItem(mFitToCanvasAction),
				(IContributionItem) new ActionContributionItem(mSaveImageAction));
	}

	@Override
	public void disposeControl() {
		if (mImageCanvas != null) {
			mImageCanvas.dispose();
			mImageCanvas = null;
		}
	}

	@Override
	public Control getControl() {
		return mImageCanvas;
	}

	@Override
	public void updateControl(GLTrace trace, GLCall call) {
		mImageCanvas.setImage(trace.getImage(call));
		mImageCanvas.setFitToCanvas(true);
	}

	@Override
	public List<IContributionItem> getToolBarItems() {
		return mToolBarItems;
	}
}
