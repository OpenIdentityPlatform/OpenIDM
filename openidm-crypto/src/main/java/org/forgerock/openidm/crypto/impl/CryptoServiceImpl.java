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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

// TODO: Expose as a set of ROA actions.

package org.forgerock.openidm.crypto.impl;

// Java SE
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

// SLF4J
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// OSGi Framework
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;

// Apache Felix Maven SCR Plugin
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

// JSON Fluent
import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.fluent.JsonTransformer;

// JSON Crypto
import org.forgerock.json.crypto.JsonCrypto;
import org.forgerock.json.crypto.JsonCryptoException;
import org.forgerock.json.crypto.JsonCryptoTransformer;
import org.forgerock.json.crypto.JsonEncryptor;
import org.forgerock.json.crypto.simple.SimpleDecryptor;
import org.forgerock.json.crypto.simple.SimpleEncryptor;
import org.forgerock.json.crypto.simple.SimpleKeyStoreSelector;

// OpenIDM
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.crypto.CryptoService;

/**
 * Cryptography Service
 *
 * @author Paul C. Bryan
 * @author aegloff
 */
public class CryptoServiceImpl implements CryptoService {

    private final static Logger LOGGER = LoggerFactory.getLogger(CryptoServiceImpl.class);
    
    /** TODO: Description. */
    private BundleContext context;

    /** TODO: Description. */
    private SimpleKeyStoreSelector keySelector;

    /** TODO: Description. */
    private final ArrayList<JsonTransformer> decryptionTransformers = new ArrayList<JsonTransformer>();

    /**
     * Opens a connection to the specified URI location and returns an input stream with which
     * to read its content. If the URI is not absolute, it is resolved against the root of
     * the local file system. If the specified location is or contains {@code null}, this
     * method returns {@code null}.
     *
     * @param location the location to open the stream for.
     * @return an input stream for reading the content of the location, or {@code null} if no location.
     * @throws IOException if there was exception opening the stream.
     */
    private InputStream openStream(String location) throws IOException {
        InputStream result = null;
        if (location != null) {
            File configFile = IdentityServer.getFileForPath(location);
            if (configFile.exists()) {
                result = new FileInputStream(configFile);
            } else {
                LOGGER.error("ERROR - KeyStore not found under CryptoService#location {}", configFile.getAbsolutePath());
            }
        }
        return result;
    }

    public void activate(BundleContext context) {
        LOGGER.debug("Activating cryptography service");
        this.context = context;
        try {
            int keyCount = 0;
            String password = IdentityServer.getInstance().getProperty("openidm.keystore.password");
            if (password != null) { // optional
                String type = IdentityServer.getInstance().getProperty("openidm.keystore.type", KeyStore.getDefaultType());
                String provider = IdentityServer.getInstance().getProperty("openidm.keystore.provider");
                String location = IdentityServer.getInstance().getProperty("openidm.keystore.location");
                
                try {
                    LOGGER.info("Activating cryptography service of type: {} provider: {} location: {}", new Object[] {type, provider, location});
                    KeyStore ks = (provider == null || provider.trim().length() == 0 ? KeyStore.getInstance(type) : KeyStore.getInstance(type, provider));
                    InputStream in = openStream(location);
                    if (null != in) {
                        char[] clearPassword = Main.unfold(password);
                        ks.load(in, password == null ? null : clearPassword);
                        keySelector = new SimpleKeyStoreSelector(ks, new String(clearPassword));
                        Enumeration<String> aliases = ks.aliases();
                        while (aliases.hasMoreElements()) {
                            LOGGER.info("Available cryptography key: {}", aliases.nextElement());
                            keyCount++;
                        }
                    }
                } catch (IOException ioe) {
                    LOGGER.error("IOException when loading KeyStore file of type: " 
                            + type + " provider: " + provider + " location:" + location, ioe);
                    throw new RuntimeException("IOException when loading KeyStore file of type: " 
                            + type + " provider: " + provider + " location:" + location + " message: " + ioe.getMessage(), ioe);
                } catch (GeneralSecurityException gse) {
                    LOGGER.error("GeneralSecurityException when loading KeyStore file", gse);
                    throw new RuntimeException("GeneralSecurityException when loading KeyStore file of type: " 
                        + type + " provider: " + provider + " location:" + location + " message: " + gse.getMessage(), gse);
                }
                decryptionTransformers.add(new JsonCryptoTransformer(new SimpleDecryptor(keySelector)));
            }
            LOGGER.info("CryptoService is initialized with {} keys.", keyCount);
        } catch (JsonValueException jve) {
            LOGGER.error("Exception when loading CryptoService configuration", jve);
            throw new ComponentException("Configuration error", jve);
        }
    }

    public void deactivate(BundleContext context) {
        decryptionTransformers.clear();
        keySelector = null;
        this.context = null;
        LOGGER.info("CryptoService stopped.");
    }

    @Override
    public JsonEncryptor getEncryptor(String cipher, String alias) throws JsonCryptoException {
        Key key = keySelector.select(alias);
        if (key == null) {
            String msg = "Encryption key " + alias + " not found";
            LOGGER.error(msg);
            throw new JsonCryptoException(msg);
        }
        return new SimpleEncryptor(cipher, key, alias);
    }

    @Override
    public List<JsonTransformer> getDecryptionTransformers() {
        return decryptionTransformers;
    }

    @Override
    public JsonValue encrypt(JsonValue value, String cipher, String alias) throws JsonCryptoException, JsonException {
        JsonValue result = null;
        if (value != null) {
            result = getEncryptor(cipher, alias).encrypt(value);
        }
        return result;
    }

    @Override
    public JsonValue decrypt(JsonValue value) throws JsonException {
        JsonValue result = null;
        if (value != null) {
            result = new JsonValue(value);
            result.getTransformers().addAll(0, getDecryptionTransformers());
            result.applyTransformers();
            result = result.copy();
        }
        return result;
    }
    
    @Override
    public boolean isEncrypted(JsonValue value) {
        return JsonCrypto.isJsonCrypto(value);
    }
}
