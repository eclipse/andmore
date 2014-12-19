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

package org.eclipse.andmore.gltrace.views;

import org.eclipse.andmore.gltrace.GLProtoBuf.GLMessage.Function;
import org.eclipse.andmore.gltrace.model.GLCall;
import org.eclipse.andmore.gltrace.model.GLTrace;
import org.eclipse.andmore.gltrace.widgets.ImageCanvas;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.part.Page;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link FrameSummaryViewPage} displays summary information regarding a
 * frame. This includes the contents of the frame buffer at the end of the
 * frame, and statistics regarding the OpenGL Calls present in the frame.
 */
public class FrameSummaryViewPage extends Page {
	private GLTrace mTrace;

	private final Object mLock = new Object();
	private Job mRefresherJob;
	private int mCurrentFrame;

	private SashForm mSash;
	private ImageCanvas mImageCanvas;

	private Label mWallClockTimeLabel;
	private Label mThreadTimeLabel;

	private TableViewer mStatsTableViewer;
	private StatsLabelProvider mStatsLabelProvider;
	private StatsTableComparator mStatsTableComparator;

	private FitToCanvasAction mFitToCanvasAction;
	private SaveImageAction mSaveImageAction;

	private static final String[] STATS_TABLE_PROPERTIES = { "Function", "Count", "Wall Time (ns)", "Thread Time (ns)", };
	private static final float[] STATS_TABLE_COLWIDTH_RATIOS = { 0.4f, 0.1f, 0.25f, 0.25f, };
	private static final int[] STATS_TABLE_COL_ALIGNMENT = { SWT.LEFT, SWT.LEFT, SWT.RIGHT, SWT.RIGHT, };

	public FrameSummaryViewPage(GLTrace trace) {
		mTrace = trace;
	}

	public void setInput(GLTrace trace) {
		mTrace = trace;
	}

	@Override
	public void createControl(Composite parent) {
		mSash = new SashForm(parent, SWT.VERTICAL);

		// create image canvas where the framebuffer is displayed
		mImageCanvas = new ImageCanvas(mSash);

		// create a composite where the frame statistics are displayed
		createFrameStatisticsPart(mSash);

		mSash.setWeights(new int[] { 70, 30 });

		mFitToCanvasAction = new FitToCanvasAction(true, mImageCanvas);
		mSaveImageAction = new SaveImageAction(mImageCanvas);

		IToolBarManager toolbarManager = getSite().getActionBars().getToolBarManager();
		toolbarManager.add(mFitToCanvasAction);
		toolbarManager.add(mSaveImageAction);
	}

	private void createFrameStatisticsPart(Composite parent) {
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(c);

		Label l = new Label(c, SWT.NONE);
		l.setText("Cumulative call duration of all OpenGL Calls in this frame:");
		l.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY));
		GridDataFactory.fillDefaults().span(2, 1).applyTo(l);

		l = new Label(c, SWT.NONE);
		l.setText("Wall Clock Time: ");
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(l);

		mWallClockTimeLabel = new Label(c, SWT.NONE);
		GridDataFactory.defaultsFor(mWallClockTimeLabel).grab(true, false).applyTo(mWallClockTimeLabel);

		l = new Label(c, SWT.NONE);
		l.setText("Thread Time: ");
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(l);

		mThreadTimeLabel = new Label(c, SWT.NONE);
		GridDataFactory.defaultsFor(mThreadTimeLabel).grab(true, false).applyTo(mThreadTimeLabel);

		l = new Label(c, SWT.HORIZONTAL | SWT.SEPARATOR);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(l);

		l = new Label(c, SWT.NONE);
		l.setText("Per OpenGL Function Statistics:");
		l.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY));
		GridDataFactory.fillDefaults().span(2, 1).applyTo(l);

		final Table table = new Table(c, SWT.BORDER | SWT.FULL_SELECTION);
		GridDataFactory.fillDefaults().grab(true, true).span(2, 1).applyTo(table);

		table.setLinesVisible(true);
		table.setHeaderVisible(true);

		mStatsTableViewer = new TableViewer(table);
		mStatsLabelProvider = new StatsLabelProvider();
		mStatsTableComparator = new StatsTableComparator(1);

		// when a column is selected, sort the table based on that column
		SelectionListener columnSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TableColumn tc = (TableColumn) e.widget;
				String colText = tc.getText();
				for (int i = 0; i < STATS_TABLE_PROPERTIES.length; i++) {
					if (STATS_TABLE_PROPERTIES[i].equals(colText)) {
						mStatsTableComparator.setSortColumn(i);
						table.setSortColumn(tc);
						table.setSortDirection(mStatsTableComparator.getDirection());
						mStatsTableViewer.refresh();
						break;
					}
				}
			}
		};

		for (int i = 0; i < STATS_TABLE_PROPERTIES.length; i++) {
			TableViewerColumn tvc = new TableViewerColumn(mStatsTableViewer, SWT.NONE);
			tvc.getColumn().setText(STATS_TABLE_PROPERTIES[i]);
			tvc.setLabelProvider(mStatsLabelProvider);
			tvc.getColumn().setAlignment(STATS_TABLE_COL_ALIGNMENT[i]);
			tvc.getColumn().addSelectionListener(columnSelectionListener);
		}
		mStatsTableViewer.setContentProvider(new StatsContentProvider());
		mStatsTableViewer.setInput(null);
		mStatsTableViewer.setComparator(mStatsTableComparator);

		// resize columns appropriately when the size of the widget changes
		table.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				int w = table.getClientArea().width;

				for (int i = 0; i < STATS_TABLE_COLWIDTH_RATIOS.length; i++) {
					table.getColumn(i).setWidth((int) (w * STATS_TABLE_COLWIDTH_RATIOS[i]));
				}
			}
		});
	}

	@Override
	public Control getControl() {
		return mSash;
	}

	@Override
	public void setFocus() {
	}

	public void setSelectedFrame(int frame) {
		if (mTrace == null) {
			return;
		}

		synchronized (mLock) {
			mCurrentFrame = frame;

			if (mRefresherJob != null) {
				return;
			}

			mRefresherJob = new Job("Update Frame Summary Task") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					final int currentFrame;
					synchronized (mLock) {
						currentFrame = mCurrentFrame;
						mRefresherJob = null;
					}
					;

					updateImageCanvas(currentFrame);
					updateFrameStats(currentFrame);

					return Status.OK_STATUS;
				}
			};
			mRefresherJob.setPriority(Job.SHORT);
			mRefresherJob.schedule(500);
		}
		;
	}

	private void updateFrameStats(int frame) {
		final List<GLCall> calls = mTrace.getGLCallsForFrame(frame);

		Job job = new Job("Update Frame Statistics") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				long wallClockDuration = 0;
				long threadDuration = 0;

				final Map<Function, PerCallStats> cumulativeStats = new EnumMap<Function, PerCallStats>(Function.class);

				for (GLCall c : calls) {
					wallClockDuration += c.getWallDuration();
					threadDuration += c.getThreadDuration();

					PerCallStats stats = cumulativeStats.get(c.getFunction());
					if (stats == null) {
						stats = new PerCallStats();
					}

					stats.count++;
					stats.threadDuration += c.getThreadDuration();
					stats.wallDuration += c.getWallDuration();

					cumulativeStats.put(c.getFunction(), stats);
				}

				final String wallTime = formatMilliSeconds(wallClockDuration);
				final String threadTime = formatMilliSeconds(threadDuration);

				Display.getDefault().syncExec(new Runnable() {
					@Override
					public void run() {
						mWallClockTimeLabel.setText(wallTime);
						mThreadTimeLabel.setText(threadTime);
						mStatsTableViewer.setInput(cumulativeStats);
					}
				});

				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
	}

	private String formatMilliSeconds(long nanoSeconds) {
		double milliSeconds = (double) nanoSeconds / 1000000;
		return String.format("%.2f ms", milliSeconds); //$NON-NLS-1$
	}

	private void updateImageCanvas(int frame) {
		int lastCallIndex = mTrace.getFrame(frame).getEndIndex() - 1;
		if (lastCallIndex >= 0 && lastCallIndex < mTrace.getGLCalls().size()) {
			GLCall call = mTrace.getGLCalls().get(lastCallIndex);
			final Image image = mTrace.getImage(call);
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					mImageCanvas.setImage(image);

					mFitToCanvasAction.setEnabled(image != null);
					mSaveImageAction.setEnabled(image != null);
				}
			});
		}
	}

	/**
	 * Cumulative stats maintained for each type of OpenGL Function in a
	 * particular frame.
	 */
	private static class PerCallStats {
		public int count;
		public long wallDuration;
		public long threadDuration;
	}

	private static class StatsContentProvider implements IStructuredContentProvider {
		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof Map<?, ?>) {
				return ((Map<?, ?>) inputElement).entrySet().toArray();
			}

			return null;
		}
	}

	private static class StatsLabelProvider extends ColumnLabelProvider {
		@Override
		public void update(ViewerCell cell) {
			Object element = cell.getElement();
			if (!(element instanceof Map.Entry<?, ?>)) {
				return;
			}

			Function f = (Function) ((Map.Entry<?, ?>) element).getKey();
			PerCallStats stats = (PerCallStats) ((Map.Entry<?, ?>) element).getValue();

			switch (cell.getColumnIndex()) {
			case 0:
				cell.setText(f.toString());
				break;
			case 1:
				cell.setText(Integer.toString(stats.count));
				break;
			case 2:
				cell.setText(formatDuration(stats.wallDuration));
				break;
			case 3:
				cell.setText(formatDuration(stats.threadDuration));
				break;
			default:
				// should not happen
				cell.setText("??"); //$NON-NLS-1$
				break;
			}
		}

		private String formatDuration(long time) {
			// Max duration is in the 10s of milliseconds = xx,xxx,xxx ns
			// So we require a format specifier that is 10 characters wide
			return String.format("%,10d", time); //$NON-NLS-1$
		}
	}

	private static class StatsTableComparator extends ViewerComparator {
		private int mSortColumn;
		private boolean mDescending = true;

		private StatsTableComparator(int defaultSortColIndex) {
			mSortColumn = defaultSortColIndex;
		}

		public void setSortColumn(int index) {
			if (index == mSortColumn) {
				// if same column as what we are currently sorting on,
				// then toggle the direction
				mDescending = !mDescending;
			} else {
				mSortColumn = index;
				mDescending = true;
			}
		}

		public int getDirection() {
			return mDescending ? SWT.UP : SWT.DOWN;
		}

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			Map.Entry<?, ?> entry1;
			Map.Entry<?, ?> entry2;

			if (mDescending) {
				entry1 = (Map.Entry<?, ?>) e1;
				entry2 = (Map.Entry<?, ?>) e2;
			} else {
				entry1 = (Map.Entry<?, ?>) e2;
				entry2 = (Map.Entry<?, ?>) e1;
			}

			String k1 = entry1.getKey().toString();
			String k2 = entry2.getKey().toString();

			PerCallStats stats1 = (PerCallStats) entry1.getValue();
			PerCallStats stats2 = (PerCallStats) entry2.getValue();

			switch (mSortColumn) {
			case 0: // function name
				return String.CASE_INSENSITIVE_ORDER.compare(k1, k2);
			case 1:
				return stats1.count - stats2.count;
			case 2:
				return (int) (stats1.wallDuration - stats2.wallDuration);
			case 3:
				return (int) (stats1.threadDuration - stats2.threadDuration);
			default:
				return super.compare(viewer, e1, e2);
			}
		}
	}
}
