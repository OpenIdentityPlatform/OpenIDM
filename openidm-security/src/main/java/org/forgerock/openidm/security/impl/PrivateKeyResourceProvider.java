/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 ForgeRock AS. All Rights Reserved
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

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.openidm.repo.RepositoryService;
import org.forgerock.openidm.security.KeyStoreHandler;
import org.forgerock.openidm.security.KeyStoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A collection resource provider servicing requests on private key entries in a keystore
 */
public class PrivateKeyResourceProvider extends EntryResourceProvider {

    private final static Logger logger = LoggerFactory.getLogger(PrivateKeyResourceProvider.class);
    
    public PrivateKeyResourceProvider(String resourceName, KeyStoreHandler store, KeyStoreManager manager, RepositoryService repoService) {
        super(resourceName, store, manager, repoService);
    }

    @Override
    protected void storeEntry(JsonValue value, String alias) throws Exception {
        String type = value.get("type").defaultTo(DEFAULT_CERTIFICATE_TYPE).asString();
        PrivateKey privateKey = null;
        String privateKeyPem = value.get("privateKey").asString();
        if (privateKeyPem == null) {
            privateKey = getKeyPair(alias).getPrivate();
        } else {
            privateKey = ((KeyPair)fromPem(privateKeyPem)).getPrivate();
        }
        if (privateKey == null) {
            throw new NotFoundException("No private key exists for the supplied signed certificate");
        }
        List<String> certStringChain = value.get("certs").required().asList(String.class);
        Certificate [] certChain = readCertificateChain(certStringChain, type);
        verify(privateKey, certChain[0]);
        store.getStore().setEntry(alias, new PrivateKeyEntry(privateKey, certChain), 
                new KeyStore.PasswordProtection(store.getPassword().toCharArray()));
        store.store();
    }

    @Override
    protected JsonValue readEntry(String alias) throws Exception {
        Key key = store.getStore().getKey(alias, store.getPassword().toCharArray());
        if (key == null) {
            throw new NotFoundException("Alias does not correspond to a key entry in " + resourceName);
        } else {
            return returnKey(alias, key);
        }
    }

    @Override
    public void createDefaultEntry(String alias) throws Exception {
        Pair<X509Certificate, PrivateKey> pair = generateCertificate("localhost", 
                "OpenIDM Self-Signed Certificate", "None", "None", "None", "None",
                DEFAULT_ALGORITHM, DEFAULT_KEY_SIZE, DEFAULT_SIGNATURE_ALGORITHM, null, null);
        Certificate cert = pair.getKey();
        PrivateKey key = pair.getValue();
        store.getStore().setEntry(alias, new PrivateKeyEntry(key, new Certificate[]{cert}), 
                new KeyStore.PasswordProtection(store.getPassword().toCharArray()));
        store.store();
    }
}
