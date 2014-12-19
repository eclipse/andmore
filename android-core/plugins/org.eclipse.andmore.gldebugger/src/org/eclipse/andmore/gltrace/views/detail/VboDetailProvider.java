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

import org.eclipse.andmore.gltrace.GLEnum;
import org.eclipse.andmore.gltrace.GLUtils;
import org.eclipse.andmore.gltrace.state.GLCompositeProperty;
import org.eclipse.andmore.gltrace.state.GLStateType;
import org.eclipse.andmore.gltrace.state.IGLProperty;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VboDetailProvider implements IStateDetailProvider {
	private static enum DisplayFormat {
		GL_FLOAT, GL_BYTE, GL_UNSIGNED_BYTE, GL_SHORT, GL_UNSIGNED_SHORT, GL_FIXED,
	}

	private Composite mComposite;

	private Label mSizeLabel;
	private Label mUsageLabel;
	private Label mTypeLabel;
	private Combo mDisplayFormatCombo;
	private Text mTextControl;

	private byte[] mBufferData;

	@Override
	public boolean isApplicable(IGLProperty state) {
		return getVboProperty(state) != null;
	}

	@Override
	public void createControl(Composite parent) {
		mComposite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = layout.marginHeight = 0;
		mComposite.setLayout(layout);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(mComposite);

		Label l = new Label(mComposite, SWT.NONE);
		l.setText("Size: ");
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(l);

		mSizeLabel = new Label(mComposite, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(mSizeLabel);

		l = new Label(mComposite, SWT.NONE);
		l.setText("Usage: ");
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(l);

		mUsageLabel = new Label(mComposite, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(mUsageLabel);

		l = new Label(mComposite, SWT.NONE);
		l.setText("Type: ");
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(l);

		mTypeLabel = new Label(mComposite, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(mTypeLabel);

		l = new Label(mComposite, SWT.NONE);
		l.setText("Format Data As: ");
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(l);

		DisplayFormat[] values = DisplayFormat.values();
		List<String> formats = new ArrayList<String>(values.length);
		for (DisplayFormat format : values) {
			formats.add(format.name());
		}

		mDisplayFormatCombo = new Combo(mComposite, SWT.DROP_DOWN | SWT.READ_ONLY);
		mDisplayFormatCombo.setItems(formats.toArray(new String[formats.size()]));
		mDisplayFormatCombo.select(0);
		mDisplayFormatCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateContents();
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(mDisplayFormatCombo);

		mTextControl = new Text(mComposite, SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL
				| SWT.H_SCROLL);
		GridDataFactory.fillDefaults().span(2, 1).grab(true, true).applyTo(mTextControl);
		mTextControl.setEditable(false);
	}

	@Override
	public void disposeControl() {
	}

	@Override
	public Control getControl() {
		return mComposite;
	}

	@Override
	public void updateControl(IGLProperty state) {
		IGLProperty vbo = getVboProperty(state);
		if (vbo instanceof GLCompositeProperty) {
			GLCompositeProperty vboProperty = (GLCompositeProperty) vbo;

			IGLProperty sizeProperty = vboProperty.getProperty(GLStateType.BUFFER_SIZE);
			mSizeLabel.setText(sizeProperty.getStringValue() + " bytes"); //$NON-NLS-1$

			IGLProperty usageProperty = vboProperty.getProperty(GLStateType.BUFFER_USAGE);
			mUsageLabel.setText(usageProperty.getStringValue());

			IGLProperty typeProperty = vboProperty.getProperty(GLStateType.BUFFER_TYPE);
			mTypeLabel.setText(typeProperty.getStringValue());

			IGLProperty dataProperty = vboProperty.getProperty(GLStateType.BUFFER_DATA);
			mBufferData = (byte[]) dataProperty.getValue();
		} else {
			mBufferData = null;
		}

		updateContents();
	}

	private void updateContents() {
		if (mBufferData != null) {
			mTextControl.setText(GLUtils.formatData(mBufferData, GLEnum.valueOf(mDisplayFormatCombo.getText())));
			mTextControl.setEnabled(true);
			mDisplayFormatCombo.setEnabled(true);
		} else {
			mTextControl.setText("");
			mTextControl.setEnabled(false);
			mDisplayFormatCombo.setEnabled(false);
		}
	}

	@Override
	public List<IContributionItem> getToolBarItems() {
		return Collections.emptyList();
	}

	/**
	 * Get the {@link GLStateType#VBO_COMPOSITE} property given a node in the
	 * state hierarchy.
	 */
	private IGLProperty getVboProperty(IGLProperty state) {
		if (state.getType() == GLStateType.VBO_COMPOSITE) {
			return state;
		}

		state = state.getParent();
		if (state != null && state.getType() == GLStateType.VBO_COMPOSITE) {
			return state;
		}

		return null;
	}
}
