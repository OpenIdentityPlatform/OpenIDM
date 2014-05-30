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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.jetty.Param;
import org.forgerock.openidm.security.KeyStoreHandler;
import org.forgerock.openidm.security.KeyStoreManager;
import org.forgerock.openidm.util.ResourceUtil;
import org.forgerock.util.encode.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Services requests on a specific keystore
 *
 * @author Laszlo Hordos
 * @author ckienle
 */
public class KeystoreResourceProvider extends SecurityResourceProvider implements SingletonResourceProvider {

    /**
     * Setup logging for the {@link SecurityManager}.
     */
    private final static Logger logger = LoggerFactory.getLogger(KeystoreResourceProvider.class);

    public KeystoreResourceProvider(String resourceName, KeyStoreHandler store, KeyStoreManager manager, ServerContext accessor, ConnectionFactory connectionFactory) {
        super(resourceName, store, manager, accessor, connectionFactory);
    }

    @Override
    public void actionInstance(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        try {
            String alias = request.getContent() != null
                    ? request.getContent().get("alias").asString()
                    : null;
            if (ACTION_GENERATE_CERT.equalsIgnoreCase(request.getAction()) || 
                    ACTION_GENERATE_CSR.equalsIgnoreCase(request.getAction())) {
                if (alias == null) {
                    throw ResourceException.getException(ResourceException.BAD_REQUEST, 
                            "A valid resource ID must be specified in the request");
                }
                String algorithm = request.getContent().get("algorithm").defaultTo(DEFAULT_ALGORITHM).asString();
                String signatureAlgorithm = request.getContent().get("signatureAlgorithm")
                        .defaultTo(DEFAULT_SIGNATURE_ALGORITHM).asString();
                int keySize = request.getContent().get("keySize").defaultTo(DEFAULT_KEY_SIZE).asInteger();
                JsonValue result = null;
                if (ACTION_GENERATE_CERT.equalsIgnoreCase(request.getAction())) {
                    // Generate self-signed certificate
                    if (store.getStore().containsAlias(alias)) {
                        handler.handleError(new ConflictException("The resource with ID '" + alias 
                                + "' could not be created because there is already another resource with the same ID"));
                    } else {
                        String domainName = request.getContent().get("domainName").required().asString();
                        String validFrom = request.getContent().get("validFrom").asString();
                        String validTo = request.getContent().get("validTo").asString();

                        // Generate the cert
                        Pair<X509Certificate, PrivateKey> pair = generateCertificate(domainName, algorithm, 
                                keySize, signatureAlgorithm, validFrom, validTo);
                        Certificate cert = pair.getKey();
                        PrivateKey key = pair.getValue();

                        String password = request.getContent().get("password").defaultTo(
                                Param.getKeystoreKeyPassword()).asString();

                        // Add it to the store and reload
                        store.getStore().setCertificateEntry(alias, cert);
                        //store.getStore().setEntry(alias, new KeyStore.PrivateKeyEntry(key, new Certificate[]{cert}), 
                                //new KeyStore.PasswordProtection(password.toCharArray()));
                        store.store();

                        manager.reload();

                        result = returnCertificate(alias, cert);
                    }
                } else {
                    // Generate CSR
                    Pair<PKCS10CertificationRequest, PrivateKey> csr = generateCSR(alias, algorithm, 
                            signatureAlgorithm, keySize, request.getContent());
                    result = returnCertificateRequest(alias, csr.getKey());
                    if (request.getContent().get("returnPrivateKey").defaultTo(false).asBoolean()) {
                        result.put("privateKey", getKeyMap(csr.getRight()));
                    }
                }
                handler.handleResult(result);
            } else {
                handler.handleError(new BadRequestException("Unsupported action " + request.getAction()));
            }
            
        } catch (Throwable t) {
            handler.handleError(ResourceUtil.adapt(t));
        }
    }

    @Override
    public void patchInstance(ServerContext context, PatchRequest request,
            ResultHandler<Resource> handler) {
        final ResourceException e = new NotSupportedException("Patch operations are not supported");
        handler.handleError(e);
    }

    @Override
    public void readInstance(final ServerContext context, final ReadRequest request,
            final ResultHandler<Resource> handler) {
        try {
            JsonValue content = new JsonValue(new LinkedHashMap<String, Object>(5));
            content.put("type", store.getStore().getType());
            content.put("provider", store.getStore().getProvider());
            Enumeration<String> aliases = store.getStore().aliases();
            List<String> aliasList = new ArrayList<String>();
            while (aliases.hasMoreElements()) {
                aliasList.add(aliases.nextElement());
            }
            content.put("aliases", aliasList);
            handler.handleResult(new Resource(resourceName, null, content));
        } catch (Throwable t) {
            handler.handleError(ResourceUtil.adapt(t));
        }
    }

    @Override
    public void updateInstance(ServerContext context, UpdateRequest request,
            ResultHandler<Resource> handler) {
        final ResourceException e = new NotSupportedException("Update operations are not supported");
        handler.handleError(e);
    }
    
    /**
     * Loads the keystore from the repository and stores it locally
     */
    public void loadKeystoreFromRepo() throws ResourceException {
        JsonValue keystoreValue = readFromRepo("/repo/security/keystore");
        String keystoreString = keystoreValue.get("keystoreString").asString();
        byte [] keystoreBytes = Base64.decode(keystoreString.getBytes());
        ByteArrayInputStream bais = new ByteArrayInputStream(keystoreBytes);
        try {
            KeyStore keystore = null;
            try {
                keystore = KeyStore.getInstance(store.getType());     
                keystore.load(bais, store.getPassword().toCharArray());
            } finally {
                bais.close();
            }
            store.setStore(keystore);
        } catch (Exception e) {
            throw ResourceException.getException(ResourceException.INTERNAL_ERROR, "Error creating keystore from store bytes", e);
        }
    }
    
    /**
     * Saves the local keystore to the respository
     */
    public void saveKeystoreToRepo() throws ResourceException {
        byte [] keystoreBytes = null;
        FileInputStream fin = null;
        File file = new File(store.getLocation());

        try {
            try {
                fin = new FileInputStream(file);
                keystoreBytes = new byte[(int) file.length()];
                fin.read(keystoreBytes);
            } finally {
                fin.close();
            }
        } catch (Exception e) {
            throw ResourceException.getException(ResourceException.INTERNAL_ERROR, e.getMessage(), e);
        }
        
        String keystoreString = new String(Base64.encode(keystoreBytes));
        JsonValue value = new JsonValue(new HashMap<String, Object>());
        value.add("keystoreString", keystoreString);
        storeInRepo("/repo/security", "keystore", value);
    }
}
