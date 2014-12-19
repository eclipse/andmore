package org.eclipse.andmore.ddms.views;

import com.android.ddmuilib.SysinfoPanel;

import org.eclipse.swt.widgets.Composite;

public class SysInfoView extends SelectionDependentViewPart {
	public static final String ID = "org.eclipse.andmore.ddms.views.SysInfoView"; //$NON-NLS-1$

	private SysinfoPanel mSysInfoPanel;

	@Override
	public void createPartControl(Composite parent) {
		mSysInfoPanel = new SysinfoPanel();
		mSysInfoPanel.createPanel(parent);
		setSelectionDependentPanel(mSysInfoPanel);
	}

	@Override
	public void setFocus() {
		mSysInfoPanel.setFocus();
	}

	@Override
	public void dispose() {
		mSysInfoPanel.dispose();
		super.dispose();
	}
}
