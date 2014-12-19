package org.eclipse.andmore.gltrace;

import com.android.ddmlib.Client;
import com.android.ddmlib.ClientData;

import org.eclipse.andmore.ddms.IClientAction;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import java.lang.reflect.InvocationTargetException;

public class DeviceViewAction implements IClientAction {
	private static final class StartTraceAction extends Action {
		private static final int LOCAL_FORWARDED_PORT = 6049;

		private Client mClient;

		public StartTraceAction() {
			super("Start OpenGL Trace");
			setImageDescriptor(GlTracePlugin.getImageDescriptor("/icons/connect.png")); //$NON-NLS-1$
			setClient(null);
		}

		public void setClient(Client c) {
			mClient = c;
			clientChanged();
		}

		private void clientChanged() {
			if (mClient == null) {
				setEnabled(false);
				return;
			}

			ClientData cd = mClient.getClientData();
			if (cd.hasFeature(ClientData.FEATURE_OPENGL_TRACING)) {
				setEnabled(true);
				setToolTipText("Trace OpenGL calls");
			} else {
				setEnabled(false);
				setToolTipText("Selected VM does not support tracing OpenGL calls");
			}
		}

		@Override
		public void run() {
			if (mClient == null) {
				return;
			}

			Shell shell = Display.getDefault().getActiveShell();
			GLTraceOptionsDialog dlg = new GLTraceOptionsDialog(shell, false, mClient.getClientData()
					.getClientDescription());
			if (dlg.open() != Window.OK) {
				return;
			}

			// start tracing on the client
			mClient.startOpenGlTracing();

			try {
				CollectTraceAction.setupForwarding(mClient.getDevice(), LOCAL_FORWARDED_PORT);
			} catch (Exception e) {
				MessageDialog.openError(shell, "Setup GL Trace",
						"Error while setting up port forwarding: " + e.getMessage());
				return;
			}

			// wait for a few seconds for the client to start the trace server
			try {
				new ProgressMonitorDialog(shell).run(true, true, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						Thread.sleep(3000);
					}
				});
			} catch (Exception e) {
			}

			// retrieve the trace from the device
			TraceOptions traceOptions = dlg.getTraceOptions();
			CollectTraceAction.startTracing(shell, traceOptions, LOCAL_FORWARDED_PORT);

			// inform the client that it doesn't need to be traced anymore
			mClient.stopOpenGlTracing();

			// remove port forwarding
			CollectTraceAction.disablePortForwarding(mClient.getDevice(), LOCAL_FORWARDED_PORT);

			// and finally open the editor to view the file
			CollectTraceAction.openInEditor(shell, traceOptions.traceDestination);
		}
	}

	private static final StartTraceAction sAction = new StartTraceAction();

	@Override
	public Action getAction() {
		return sAction;
	}

	@Override
	public void selectedClientChanged(Client c) {
		sAction.setClient(c);
	}
}
