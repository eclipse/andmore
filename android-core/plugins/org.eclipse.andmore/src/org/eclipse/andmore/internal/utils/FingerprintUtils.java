/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.andmore.internal.utils;

import java.util.Locale;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;

public class FingerprintUtils {

    /**
     * Returns the {@link Certificate} fingerprint as returned by <code>keytool</code>.
     *
     * @param certificate
     * @param hashAlgorithm
     */
    public static String getFingerprint(Certificate cert, String hashAlgorithm) {
        if (cert == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(hashAlgorithm);
            return toHexadecimalString(digest.digest(cert.getEncoded()));
        } catch(NoSuchAlgorithmException e) {
            // ignore
        } catch(CertificateEncodingException e) {
            // ignore
        }
        return null;
    }

    private static String toHexadecimalString(byte[] value) {
        StringBuffer sb = new StringBuffer();
        int len = value.length;
        for (int i = 0; i < len; i++) {
            int num = (value[i]) & 0xff;
            if (num < 0x10) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(num));
            if (i < len - 1) {
                sb.append(':');
            }
        }
        return sb.toString().toUpperCase(Locale.US);
    }
}