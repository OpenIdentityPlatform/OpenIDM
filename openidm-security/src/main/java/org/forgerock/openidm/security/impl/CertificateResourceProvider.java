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
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.openidm.security.impl;

import java.security.cert.Certificate;

import org.forgerock.json.JsonValue;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.keystore.KeyStoreManagementService;
import org.forgerock.openidm.keystore.KeyStoreService;
import org.forgerock.openidm.repo.RepositoryService;
import org.forgerock.openidm.util.CertUtil;

/**
 * A collection resource provider servicing requests on certificate entries in a keystore
 */
public class CertificateResourceProvider extends EntryResourceProvider {

    public CertificateResourceProvider(String resourceName, KeyStoreService keyStoreService,
            RepositoryService repoService, CryptoService cryptoService, KeyStoreManagementService keyStoreManager) {
        super(resourceName, keyStoreService.getKeyStore(), keyStoreService, repoService, cryptoService, keyStoreManager);
    }

    @Override
    protected void storeEntry(JsonValue value, String alias) throws Exception {
        String type = value.get("type").defaultTo(DEFAULT_CERTIFICATE_TYPE).asString();
        String certString = value.get("cert").required().asString();
        Certificate cert = CertUtil.readCertificate(certString);
        keyStore.setCertificateEntry(alias, cert);
        keyStoreService.store();
    }

    @Override
    protected JsonValue readEntry(String alias) throws Exception {
        Certificate cert = keyStore.getCertificate(alias);
        return returnCertificate(alias, cert);
    }
}
