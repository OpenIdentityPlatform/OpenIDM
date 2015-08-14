/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013-2015 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.openidm.security.impl;

import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import org.apache.commons.lang3.tuple.Pair;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.repo.RepositoryService;
import org.forgerock.openidm.security.KeyStoreHandler;
import org.forgerock.openidm.security.KeyStoreManager;

/**
 * A collection resource provider servicing requests on certificate entries in a keystore
 */
public class CertificateResourceProvider extends EntryResourceProvider {

    public CertificateResourceProvider(String resourceName, KeyStoreHandler store, KeyStoreManager manager,
            RepositoryService repoService) {
        super(resourceName, store, manager, repoService);
    }

    @Override
    protected void storeEntry(JsonValue value, String alias) throws Exception {
        String type = value.get("type").defaultTo(DEFAULT_CERTIFICATE_TYPE).asString();
        String certString = value.get("cert").required().asString();
        Certificate cert = readCertificate(certString, type);
        store.getStore().setCertificateEntry(alias, cert);
        store.store();
    }

    @Override
    protected JsonValue readEntry(String alias) throws Exception {
        Certificate cert = store.getStore().getCertificate(alias);
        return returnCertificate(alias, cert);
    }

    @Override
    public void createDefaultEntry(String alias) throws Exception {
        Pair<X509Certificate, PrivateKey> pair = generateCertificate("local.openidm.forgerock.org", 
                "OpenIDM Self-Signed Certificate", "None", "None", "None", "None",
                DEFAULT_ALGORITHM, DEFAULT_KEY_SIZE, DEFAULT_SIGNATURE_ALGORITHM, null, null);
        Certificate cert = pair.getKey();
        store.getStore().setCertificateEntry(alias, cert);
        store.store();
    }
}
