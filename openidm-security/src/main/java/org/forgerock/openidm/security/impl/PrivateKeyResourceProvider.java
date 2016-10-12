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

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.List;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.crypto.KeyRepresentation;
import org.forgerock.openidm.keystore.KeyStoreManagementService;
import org.forgerock.openidm.keystore.KeyStoreService;
import org.forgerock.openidm.repo.RepositoryService;
import org.forgerock.openidm.util.CertUtil;

/**
 * A collection resource provider servicing requests on private key entries in a keystore
 */
public class PrivateKeyResourceProvider extends EntryResourceProvider {

    private final char[] keyStorePassword;
    
    public PrivateKeyResourceProvider(String resourceName, KeyStoreService keyStoreService,
            RepositoryService repoService, CryptoService cryptoService, KeyStoreManagementService keyStoreManager) {
        super(resourceName, keyStoreService.getKeyStore(), keyStoreService, repoService, cryptoService, keyStoreManager);
        this.keyStorePassword = keyStoreService.getKeyStoreDetails().getPassword().toCharArray();
    }

    @Override
    protected void storeEntry(JsonValue value, String alias) throws Exception {
        String type = value.get("type").defaultTo(DEFAULT_CERTIFICATE_TYPE).asString();
        PrivateKey privateKey;
        String privateKeyPem = value.get("privateKey").asString();
        if (privateKeyPem == null) {
            privateKey = getKeyPair(alias).getPrivate();
        } else {
            privateKey = ((KeyPair) CertUtil.fromPem(privateKeyPem)).getPrivate();
        }
        if (privateKey == null) {
            throw new NotFoundException("No private key exists for the supplied signed certificate");
        }
        List<String> certStringChain = value.get("certs").required().asList(String.class);
        Certificate [] certChain = CertUtil.readCertificateChain(certStringChain);
        verify(privateKey, certChain[0]);
        keyStore.setEntry(alias, new PrivateKeyEntry(privateKey, certChain),
                new KeyStore.PasswordProtection(keyStorePassword));
        keyStoreService.store();
    }

    @Override
    protected JsonValue readEntry(String alias) throws Exception {
        Key key = keyStore.getKey(alias, keyStorePassword);
        if (key == null) {
            throw new NotFoundException("Alias does not correspond to a key entry in " + resourceName);
        } else {
            return KeyRepresentation.toJsonValue(alias, key);
        }
    }
}
