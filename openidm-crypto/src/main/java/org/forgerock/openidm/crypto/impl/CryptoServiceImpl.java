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

package org.forgerock.openidm.crypto.impl;

// Java Standard Edition
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

// OSGi Framework
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

// JSON Fluent library
import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;
import org.forgerock.json.fluent.JsonTransformer;

// JSON Cryptography library
import org.forgerock.json.crypto.JsonCrypto;
import org.forgerock.json.crypto.JsonCryptoException;
import org.forgerock.json.crypto.JsonCryptoTransformer;
import org.forgerock.json.crypto.simple.SimpleDecryptor;
import org.forgerock.json.crypto.simple.SimpleEncryptor;
import org.forgerock.json.crypto.simple.SimpleKeyStoreSelector;

// OpenIDM
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.crypto.CryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * TODO: Description.
 *
 * @author Paul C. Bryan
 */
@Component(
    name = "org.forgerock.openidm.crypto",
    immediate = true,
    policy = ConfigurationPolicy.OPTIONAL
)
@Properties({
    @Property(name = "service.description", value = "OpenIDM cryptography service"),
    @Property(name = "service.vendor", value = "ForgeRock AS")
})
@Service
public class CryptoServiceImpl implements CryptoService {
    private final static Logger logger = LoggerFactory.getLogger(CryptoServiceImpl.class);
    /** TODO: Description. */
    private ComponentContext context;

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
     * @throws JsonNodeException if there was exception opening the stream.
     */
    private InputStream openStream(JsonNode location) throws JsonNodeException {
        InputStream result = null;
        if (location != null && !location.isNull()) {
            try {
                URI uri = location.asURI();
                URL loc = null;
                if (uri.isAbsolute()) {
                    loc = uri.toURL();
                } else {
                    //TODO Do we really need this if we don't have a unified way to detect the instance root?
                    //Default to user.dir system parameter. This is not reliable in embedded mode
                    loc = (new File(".")).getAbsoluteFile().toURI().resolve(uri).toURL();
                }
                result = loc.openStream();
                if (null == result) {
                    logger.info("KeyStore not found under crypto.json#location {}", loc);
                }
            } catch (IOException ioe) {
                throw new JsonNodeException(location, ioe);
            }
        }
        return result;
    }

    @Activate
    protected void activate(ComponentContext context) {
        this.context = context;
        JsonNode config = new JSONEnhancedConfig().getConfigurationAsJson(context);
        try {
            JsonNode keystore = config.get("keystore");
            if (!keystore.isNull()) { // optional
                String type = keystore.get("type").defaultTo(KeyStore.getDefaultType()).asString();
                String provider = keystore.get("provider").asString();
                String password = keystore.get("password").asString();
                try {
                    KeyStore ks = (provider == null ? KeyStore.getInstance(type) : KeyStore.getInstance(type, provider));
                    InputStream in = openStream(keystore.get("location"));
                    ks.load(in, password == null ? null : password.toCharArray());
                    keySelector = new SimpleKeyStoreSelector(ks, password);
                } catch (IOException ioe) {
                    throw new JsonNodeException(keystore, ioe);
                } catch (GeneralSecurityException gse) {
                    throw new JsonNodeException(keystore, gse);
                }
                decryptionTransformers.add(new JsonCryptoTransformer(new SimpleDecryptor(keySelector)));
            }
        } catch (JsonNodeException jne) {
            throw new ComponentException("Configuration error", jne);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        decryptionTransformers.clear();
        keySelector = null;
        this.context = null;
    }

    @Override
    public JsonTransformer getEncryptionTransformer(String cipher, String alias) throws JsonCryptoException {
        Key key = keySelector.select(alias);
        if (key == null) {
            throw new JsonCryptoException("key not found: " + alias);
        }
        return new JsonCryptoTransformer(new SimpleEncryptor(cipher, key, alias));
    }

    @Override
    public List<JsonTransformer> getDecryptionTransformers() {
        return decryptionTransformers;
    }

    @Override
    public JsonNode encrypt(JsonNode node, String cipher, String alias) throws JsonCryptoException, JsonException {
        JsonTransformer encryptionTransformer = getEncryptionTransformer(cipher, alias);
        JsonNode copy = node.copy(); // make deep copy to encrypt; apply all existing transformations
        encryptionTransformer.transform(copy); // apply encryption transformation to copy
        return copy;
    }

    @Override
    public JsonNode decrypt(JsonNode node) throws JsonException {
        node = new JsonNode(node); // make a shallow copy that we can modify
        node.getTransformers().addAll(getDecryptionTransformers()); // add decryption transformers
        return node.copy(); // make a deep copy, applying all transformations (incl. decryption)
    }
}
