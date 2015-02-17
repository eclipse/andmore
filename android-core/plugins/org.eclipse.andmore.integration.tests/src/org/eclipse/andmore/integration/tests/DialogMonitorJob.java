package org.eclipse.andmore.integration.tests;

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

public class DialogMonitorJob extends Thread {

	boolean m_allDone;

	Display m_display;

	IDialogProcessor m_dialogProcessor;

	boolean isSyncMode = true;

	public DialogMonitorJob(Display display, IDialogProcessor processor,
			boolean isSyncMode) {
		super("Monitoring Dialogs");
		m_display = display;
		m_dialogProcessor = processor;
		this.isSyncMode = isSyncMode;
	}

	/**
	 * Recursive method that crawls up the parents and looks for a dialog
	 * 
	 * @param compositeControl
	 * @return
	 */
	private Object getDialog(Control compositeControl) {

		if (compositeControl == null) {
			return null;
		}

		Object data = compositeControl.getData();
		if (data != null
				&& (data instanceof org.eclipse.jface.dialogs.Dialog
						|| data instanceof org.eclipse.swt.widgets.Dialog || data instanceof DialogPage)) {
			return data;
		}

		return getDialog(compositeControl.getParent());
	}

	private void processDialog() {
		Control control = m_display.getFocusControl();

		if (control != null) {

			Object dialogObject = getDialog(control);

			if (dialogObject != null) {
				m_dialogProcessor.processDialog(dialogObject);

				UnitTestHelper.runEventQueue();
			}
		}

	}

	@Override
	public void run() {
		while (true) {

			if (isSyncMode) {
				m_display.syncExec(new Runnable() {

					@Override
					public void run() {
						processDialog();
					}

				});

			} else {
				m_display.asyncExec(new Runnable() {

					@Override
					public void run() {
						processDialog();
					}

				});
			}

			if (m_allDone) {
				return;
			}

			/**
			 * Give a little time for the task to run. We do not want to take up
			 * all the CPU by just searching.
			 */
			try {
				Thread.sleep(150);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}

	}

	public void setAllDone(boolean allDone) {
		m_allDone = allDone;
	}

}