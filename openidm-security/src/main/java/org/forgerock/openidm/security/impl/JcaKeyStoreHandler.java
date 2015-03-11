/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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

import org.forgerock.openidm.security.KeyStoreHandler;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * A NAME does ...
 */
public class JcaKeyStoreHandler implements KeyStoreHandler {

    private String location;
    private String password;
    private String type;
    private KeyStore store;

    public JcaKeyStoreHandler(String type, String location, String password) throws Exception {
        this.location = location;
        this.password = password;
        this.type = type;
        init();
    }

    void init() throws IOException, KeyStoreException, CertificateException,
            NoSuchAlgorithmException {
        InputStream in = new FileInputStream(location);
        try {
            store = KeyStore.getInstance(type);
            store.load(in, password.toCharArray());
        } finally {
            in.close();
        }
    }

    @Override
    public KeyStore getStore() {
        return store;
    }

    @Override
    public void setStore(KeyStore keystore) throws Exception {
        store = keystore;
        store();
    }

    @Override
    public void store() throws Exception {
        OutputStream out = new FileOutputStream(location);
        try {
            store.store(out, password.toCharArray());
        } finally {
            out.close();
        }
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getLocation() {
        return location;
    }

    public String getType() {
        return type;
    }
}
