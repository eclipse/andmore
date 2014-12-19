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
package org.eclipse.andmore.android.certmanager.ui.tree;

import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.Locale;

import org.eclipse.andmore.android.certmanager.ui.model.EntryNode;
import org.eclipse.andmore.android.certmanager.ui.model.IKeyStoreEntry;
import org.eclipse.jface.viewers.ColumnLabelProvider;

public class ExpiresInColumnLabelProvider extends ColumnLabelProvider {
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ColumnLabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(Object element) {
		if (element instanceof IKeyStoreEntry) {
			EntryNode keyStoreEntry = (EntryNode) element;
			X509Certificate x509Certificate = keyStoreEntry.getX509Certificate();
			// Android certificate
			return getExpiresInDate(x509Certificate);
		}
		return "";
	}

	/**
	 * Returns the date where the certificate expires
	 * 
	 * @param cert
	 * @return
	 */
	private String getExpiresInDate(X509Certificate x509Certificate) {
		return (x509Certificate != null) ? DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(
				x509Certificate.getNotAfter()) : "";
	}
}
