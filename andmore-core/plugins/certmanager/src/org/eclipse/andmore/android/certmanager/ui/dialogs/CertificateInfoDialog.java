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
package org.eclipse.andmore.android.certmanager.ui.dialogs;

import java.security.cert.X509Certificate;

import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.eclipse.andmore.android.certmanager.CertificateManagerActivator;
import org.eclipse.andmore.android.certmanager.i18n.CertificateManagerNLS;
import org.eclipse.andmore.android.certmanager.ui.composite.KeyPropertiesBlock;
import org.eclipse.andmore.android.certmanager.ui.model.EntryNode;
import org.eclipse.andmore.android.common.log.StudioLogger;
import org.eclipse.andmore.android.common.utilities.EclipseUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Dialog to show certificate info.
 */
public class CertificateInfoDialog extends Dialog {
	private static final String HELP_ID = CertificateManagerActivator.PLUGIN_ID + ".certificate_info_dialog"; //$NON-NLS-1$

	final private KeyPropertiesBlock block;

	final private EntryNode entry;

	public CertificateInfoDialog(Shell parentShell, KeyPropertiesBlock blk, EntryNode entry) {
		super(parentShell);
		this.block = blk;
		this.entry = entry;
	}

	@Override
	protected Control createContents(Composite parent) {
		if (parent instanceof Shell) {
			((Shell) parent).setText(CertificateManagerNLS.CertificateInfoDialog_ShellTitle);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, HELP_ID);
		}
		return super.createContents(parent);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite newComposite = (Composite) super.createDialogArea(parent);
		X509Certificate cert = null;

		try {
			cert = entry.getX509Certificate();

			if (cert != null) {
				X500Name x500name = new JcaX509CertificateHolder(cert).getSubject();
				RDN commonName = x500name.getRDNs(BCStyle.CN).length >= 1 ? x500name.getRDNs(BCStyle.CN)[0] : null;
				RDN organization = x500name.getRDNs(BCStyle.O).length >= 1 ? x500name.getRDNs(BCStyle.O)[0] : null;
				RDN organizationUnit = x500name.getRDNs(BCStyle.OU).length >= 1 ? x500name.getRDNs(BCStyle.OU)[0]
						: null;
				RDN country = x500name.getRDNs(BCStyle.C).length >= 1 ? x500name.getRDNs(BCStyle.C)[0] : null;
				RDN state = x500name.getRDNs(BCStyle.ST).length >= 1 ? x500name.getRDNs(BCStyle.ST)[0] : null;
				RDN locality = x500name.getRDNs(BCStyle.L).length >= 1 ? x500name.getRDNs(BCStyle.L)[0] : null;

				block.createInfoBlock(newComposite, entry.getAlias(), printCertInfo(commonName),
						printCertInfo(organization), printCertInfo(organizationUnit), printCertInfo(country),
						printCertInfo(state), printCertInfo(locality), cert.getNotAfter(), cert.getNotBefore());
			} else {
				// not found Android certificate expected (X509Certificate)
				EclipseUtils.showErrorDialog(CertificateManagerNLS.CertificateInfoDialog_UnknownCertificateKeypairType,
						CertificateManagerNLS.CertificatePropertiesHandler_ErrorGettingCertificateOrKeypairProperties);
			}
		} catch (Exception e) {
			EclipseUtils.showErrorDialog(
					CertificateManagerNLS.CertificatePropertiesHandler_ErrorGettingCertificateOrKeypairProperties,
					e.getMessage());
			StudioLogger.error(CertificateInfoDialog.class,
					CertificateManagerNLS.CertificatePropertiesHandler_ErrorGettingCertificateOrKeypairProperties, e);
		}
		return newComposite;
	}

	private String printCertInfo(RDN certItem) {
		return certItem != null ? certItem.getFirst().getValue().toString()
				: CertificateManagerNLS.CertificateInfoDialog_NotAvailableProperty;
	}
}
