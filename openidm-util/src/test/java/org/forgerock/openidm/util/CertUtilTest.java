/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2026 3A Systems, LLC.
 */
package org.forgerock.openidm.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;

import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class CertUtilTest {

    private static final String ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;
    private static final String SIGNATURE_ALGORITHM = "SHA512WithRSAEncryption";

    @BeforeClass
    public void addBouncyCastleProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void testGenerateCertificateWithCommonNameOnlyHasNoCountry() throws Exception {
        final Pair<X509Certificate, PrivateKey> pair = CertUtil.generateCertificate("localhost",
                ALGORITHM, KEY_SIZE, SIGNATURE_ALGORITHM, null, null);

        final X500Name subject = subjectOf(pair.getKey());
        assertThat(subject.getRDNs(BCStyle.C)).isEmpty();
        assertThat(valueOf(subject, BCStyle.CN)).isEqualTo("localhost");
    }

    @Test
    public void testGenerateCertificateOmitsNoneCountry() throws Exception {
        // the placeholders DefaultKeyStoreInitializer passes for the default certificates
        final Pair<X509Certificate, PrivateKey> pair = CertUtil.generateCertificate("localhost",
                "OpenIDM Self-Signed Certificate", "None", "None", "None", "None",
                ALGORITHM, KEY_SIZE, SIGNATURE_ALGORITHM, null, null);

        final X500Name subject = subjectOf(pair.getKey());
        assertThat(subject.getRDNs(BCStyle.C)).isEmpty();
        assertThat(valueOf(subject, BCStyle.O)).isEqualTo("OpenIDM Self-Signed Certificate");
        assertThat(valueOf(subject, BCStyle.OU)).isEqualTo("None");
    }

    @Test
    public void testGenerateCertificateKeepsCountryCode() throws Exception {
        final Pair<X509Certificate, PrivateKey> pair = CertUtil.generateCertificate("localhost",
                "Example", "IT", "California", "US", "San Francisco",
                ALGORITHM, KEY_SIZE, SIGNATURE_ALGORITHM, null, null);

        assertThat(valueOf(subjectOf(pair.getKey()), BCStyle.C)).isEqualTo("US");
    }

    private static X500Name subjectOf(X509Certificate cert) throws Exception {
        return new JcaX509CertificateHolder(cert).getSubject();
    }

    private static String valueOf(X500Name name, ASN1ObjectIdentifier attribute) {
        return IETFUtils.valueToString(name.getRDNs(attribute)[0].getFirst().getValue());
    }
}
