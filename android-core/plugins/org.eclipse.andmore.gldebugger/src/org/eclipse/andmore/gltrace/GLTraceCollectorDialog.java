/*
 * Copyright (C) 2011 The Android Open Source Project
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

package org.eclipse.andmore.gltrace;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

import java.io.IOException;
import java.text.DecimalFormat;

/** Dialog displayed while the trace is being streamed from device to host. */
public class GLTraceCollectorDialog extends TitleAreaDialog {
	private static final String TITLE = "OpenGL ES Trace";
	private static final String DEFAULT_MESSAGE = "Trace collection in progress.";
	private static final DecimalFormat SIZE_FORMATTER = new DecimalFormat("#.##"); //$NON-NLS-1$

	private TraceOptions mTraceOptions;
	private final TraceFileWriter mTraceFileWriter;
	private final TraceCommandWriter mTraceCommandWriter;

	private Label mFramesCollectedLabel;
	private Label mTraceFileSizeLabel;
	private StatusRefreshTask mRefreshTask;

	protected GLTraceCollectorDialog(Shell parentShell, TraceFileWriter traceFileWriter,
			TraceCommandWriter traceCommandWriter, TraceOptions traceOptions) {
		super(parentShell);
		mTraceFileWriter = traceFileWriter;
		mTraceCommandWriter = traceCommandWriter;
		mTraceOptions = traceOptions;
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout(0, false));
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment = GridData.CENTER;
		c.setLayoutData(gd);

		createButton(c, IDialogConstants.OK_ID, "Stop Tracing", true);
		return c;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		parent.setLayout(new GridLayout());

		setTitle(TITLE);
		setMessage(DEFAULT_MESSAGE);

		Group controlGroup = new Group(parent, SWT.BORDER);
		controlGroup.setLayout(new GridLayout(2, false));
		controlGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		controlGroup.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
		controlGroup.setText("Trace Options");

		createLabel(controlGroup, "Collect Framebuffer contents on eglSwapBuffers()");
		final Button eglSwapCheckBox = createButton(controlGroup, mTraceOptions.collectFbOnEglSwap);

		createLabel(controlGroup, "Collect Framebuffer contents on glDraw*()");
		final Button glDrawCheckBox = createButton(controlGroup, mTraceOptions.collectFbOnGlDraw);

		createLabel(controlGroup, "Collect texture data for glTexImage*()");
		final Button glTexImageCheckBox = createButton(controlGroup, mTraceOptions.collectTextureData);

		SelectionListener l = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				boolean eglSwap = eglSwapCheckBox.getSelection();
				boolean glDraw = glDrawCheckBox.getSelection();
				boolean glTexImage = glTexImageCheckBox.getSelection();

				try {
					mTraceCommandWriter.setTraceOptions(eglSwap, glDraw, glTexImage);
				} catch (IOException e) {
					eglSwapCheckBox.setEnabled(false);
					glDrawCheckBox.setEnabled(false);
					glTexImageCheckBox.setEnabled(false);

					MessageDialog.openError(Display.getDefault().getActiveShell(), "OpenGL ES Trace",
							"Error while setting trace options: " + e.getMessage());
				}

				// update the text on the button
				if (!(event.getSource() instanceof Button)) {
					return;
				}
				Button sourceButton = (Button) event.getSource();
				sourceButton.setText(getToggleActionText(sourceButton.getSelection()));
				sourceButton.pack();
			}
		};

		eglSwapCheckBox.addSelectionListener(l);
		glDrawCheckBox.addSelectionListener(l);
		glTexImageCheckBox.addSelectionListener(l);

		Group statusGroup = new Group(parent, SWT.NONE);
		statusGroup.setLayout(new GridLayout(2, false));
		statusGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		statusGroup.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
		statusGroup.setText("Trace Status");

		createLabel(statusGroup, "Frames Collected:");
		mFramesCollectedLabel = createLabel(statusGroup, "");

		createLabel(statusGroup, "Trace File Size:");
		mTraceFileSizeLabel = createLabel(statusGroup, "");

		ProgressBar pb = new ProgressBar(statusGroup, SWT.INDETERMINATE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		pb.setLayoutData(gd);

		mRefreshTask = new StatusRefreshTask();
		new Thread(mRefreshTask, "Trace Status Refresh Thread").start();

		return super.createDialogArea(parent);
	}

	private Button createButton(Composite controlComposite, boolean selection) {
		Button b = new Button(controlComposite, SWT.TOGGLE);
		b.setText(getToggleActionText(selection));
		b.setSelection(selection);
		return b;
	}

	/** Get text to show on toggle box given its current selection. */
	private String getToggleActionText(boolean en) {
		return en ? "Disable" : "Enable";
	}

	private Label createLabel(Composite parent, String text) {
		Label l = new Label(parent, SWT.NONE);
		l.setText(text);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment = SWT.LEFT;
		gd.verticalAlignment = SWT.CENTER;
		l.setLayoutData(gd);

		return l;
	}

	@Override
	protected void okPressed() {
		mRefreshTask.cancel();
		super.okPressed();
	}

	/** Periodically refresh the trace status. */
	private class StatusRefreshTask implements Runnable {
		private static final int REFRESH_INTERVAL = 1000;
		private volatile boolean mIsCancelled = false;

		@Override
		public void run() {
			if (mTraceFileWriter == null) {
				return;
			}

			while (!mIsCancelled) {
				final String frameCount = Integer.toString(mTraceFileWriter.getCurrentFrameCount());

				double fileSize = mTraceFileWriter.getCurrentFileSize();
				fileSize /= (1024 * 1024); // convert to size in MB
				final String frameSize = SIZE_FORMATTER.format(fileSize) + " MB";

				Display.getDefault().syncExec(new Runnable() {
					@Override
					public void run() {
						if (mFramesCollectedLabel.isDisposed()) {
							return;
						}

						mFramesCollectedLabel.setText(frameCount);
						mTraceFileSizeLabel.setText(frameSize);

						mFramesCollectedLabel.pack();
						mTraceFileSizeLabel.pack();
					}
				});

				try {
					Thread.sleep(REFRESH_INTERVAL);
				} catch (InterruptedException e) {
					return;
				}
			}
		}

		public void cancel() {
			mIsCancelled = true;
		}
	}
}
