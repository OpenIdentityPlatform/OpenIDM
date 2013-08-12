/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
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

// TODO: Expose as a set of resource actions.
package org.forgerock.openidm.crypto.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.forgerock.json.crypto.JsonCrypto;
import org.forgerock.json.crypto.JsonCryptoException;
import org.forgerock.json.crypto.JsonCryptoTransformer;
import org.forgerock.json.crypto.JsonEncryptor;
import org.forgerock.json.crypto.simple.SimpleDecryptor;
import org.forgerock.json.crypto.simple.SimpleEncryptor;
import org.forgerock.json.crypto.simple.SimpleKeyStoreSelector;
import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonTransformer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.util.JsonUtil;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cryptography Service
 *
 * @author Paul C. Bryan
 * @author aegloff
 */
public class CryptoServiceImpl implements CryptoService {

    /**
     * Setup logging for the {@link CryptoServiceImpl}.
     */
    private final static Logger logger = LoggerFactory.getLogger(CryptoServiceImpl.class);

    /** TODO: Description. */
    private SimpleKeyStoreSelector keySelector;

    /** TODO: Description. */
    private final ArrayList<JsonTransformer> decryptionTransformers =
            new ArrayList<JsonTransformer>();

    /**
     * Opens a connection to the specified URI location and returns an input
     * stream with which to read its content. If the URI is not absolute, it is
     * resolved against the root of the local file system. If the specified
     * location is or contains {@code null}, this method returns {@code null}.
     *
     * @param location
     *            the location to open the stream for.
     * @return an input stream for reading the content of the location, or
     *         {@code null} if no location.
     * @throws IOException
     *             if there was exception opening the stream.
     */
    private InputStream openStream(String location) throws IOException {
        InputStream result = null;
        if (location != null) {
            File configFile =
                    IdentityServer.getFileForPath(location, IdentityServer.getInstance()
                            .getInstallLocation());
            if (configFile.exists()) {
                result = new FileInputStream(configFile);
            } else {
                logger.error("ERROR - KeyStore not found under CryptoService#location {}",
                        configFile.getAbsolutePath());
            }
        }
        return result;
    }

    public void activate(BundleContext context) {
        logger.debug("Activating cryptography service");
        try {
            int keyCount = 0;
            String password = IdentityServer.getInstance().getProperty("openidm.keystore.password");
            if (password != null) { // optional
                String type =
                        IdentityServer.getInstance().getProperty("openidm.keystore.type",
                                KeyStore.getDefaultType());
                String provider =
                        IdentityServer.getInstance().getProperty("openidm.keystore.provider");
                String location =
                        IdentityServer.getInstance().getProperty("openidm.keystore.location");

                try {
                    logger.info(
                            "Activating cryptography service of type: {} provider: {} location: {}",
                            new Object[] { type, provider, location });
                    KeyStore ks =
                            (provider == null || provider.trim().length() == 0 ? KeyStore
                                    .getInstance(type) : KeyStore.getInstance(type, provider));
                    InputStream in = openStream(location);
                    if (null != in) {
                        char[] clearPassword = Main.unfold(password);
                        ks.load(in, password == null ? null : clearPassword);
                        keySelector = new SimpleKeyStoreSelector(ks, new String(clearPassword));
                        Enumeration<String> aliases = ks.aliases();
                        while (aliases.hasMoreElements()) {
                            logger.info("Available cryptography key: {}", aliases.nextElement());
                            keyCount++;
                        }
                    }
                } catch (IOException ioe) {
                    logger.error("IOException when loading KeyStore file of type: " + type
                            + " provider: " + provider + " location:" + location, ioe);
                    throw new RuntimeException("IOException when loading KeyStore file of type: "
                            + type + " provider: " + provider + " location:" + location
                            + " message: " + ioe.getMessage(), ioe);
                } catch (GeneralSecurityException gse) {
                    logger.error("GeneralSecurityException when loading KeyStore file", gse);
                    throw new RuntimeException(
                            "GeneralSecurityException when loading KeyStore file of type: " + type
                                    + " provider: " + provider + " location:" + location
                                    + " message: " + gse.getMessage(), gse);
                }
                decryptionTransformers.add(new JsonCryptoTransformer(new SimpleDecryptor(
                        keySelector)));
            }
            logger.info("CryptoService is initialized with {} keys.", keyCount);
        } catch (final JsonValueException jve) {
            logger.error("Exception when loading CryptoService configuration", jve);
            throw jve;
        }
    }

    public void deactivate(BundleContext context) {
        decryptionTransformers.clear();
        keySelector = null;
        logger.info("CryptoService stopped.");
    }

    @Override
    public JsonEncryptor getEncryptor(String cipher, String alias) throws JsonCryptoException {
        Key key = keySelector.select(alias);
        if (key == null) {
            String msg = "Encryption key " + alias + " not found";
            logger.error(msg);
            throw new JsonCryptoException(msg);
        }
        return new SimpleEncryptor(cipher, key, alias);
    }

    @Override
    public List<JsonTransformer> getDecryptionTransformers() {
        return decryptionTransformers;
    }

    @Override
    public JsonValue encrypt(JsonValue value, String cipher, String alias)
            throws JsonCryptoException, JsonException {
        JsonValue result = null;
        if (value != null) {
            JsonEncryptor encryptor = getEncryptor(cipher, alias);
            result = new JsonCrypto(encryptor.getType(), encryptor.encrypt(value)).toJsonValue();
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
    public JsonValue decrypt(String value) throws JsonException {
        JsonValue jsonValue = JsonUtil.parseStringified(value);
        return decrypt(jsonValue);
    }

    @Override
    public JsonValue decryptIfNecessary(JsonValue value) throws JsonException {
        if (value == null) {
            return new JsonValue(null);
        }
        if (value.isNull() || !isEncrypted(value)) {
            return value;
        }
        return decrypt(value);
    }

    @Override
    public JsonValue decryptIfNecessary(String value) throws JsonException {
        JsonValue jsonValue = null;
        if (value != null) {
            jsonValue = JsonUtil.parseStringified(value);
        }
        return decryptIfNecessary(jsonValue);
    }

    @Override
    public boolean isEncrypted(JsonValue value) {
        return JsonCrypto.isJsonCrypto(value);
    }

    @Override
    public boolean isEncrypted(String value) {
        return JsonUtil.isEncrypted(value);
    }

}
