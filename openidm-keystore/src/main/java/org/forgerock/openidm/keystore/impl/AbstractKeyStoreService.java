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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openidm.keystore.impl;

import static org.forgerock.openidm.core.IdentityServer.PKCS11_CONFIG;
import static org.forgerock.security.keystore.KeyStoreType.PKCS11;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CertificateException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.keystore.KeyStoreDetails;
import org.forgerock.openidm.keystore.KeyStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract {@link KeyStoreService} which contains common code that all {@link KeyStoreService}'s use.
 */
abstract class AbstractKeyStoreService implements KeyStoreService {
    private static final Logger logger = LoggerFactory.getLogger(AbstractKeyStoreService.class);
    private static final String NONE = "none";

    /** The wrapped {@link KeyStore} object. */
    protected KeyStore store;

    /** The details used to initialize the {@link KeyStore}. */
    protected final KeyStoreDetails keyStoreDetails;
    /**
     * The {@link org.forgerock.openidm.keystore.impl.KeyStoreInitializer} used to load and initialize
     * the {@link KeyStore}
     */
    protected final KeyStoreInitializer keyStoreInitializer;

    /**
     * Constructs an {@link AbstractKeyStoreService} given the Identity Server properties to lookup the
     * {@link KeyStoreDetails}.
     * @throws GeneralSecurityException if unable to load and initialize the keystore.
     */
    public AbstractKeyStoreService() throws GeneralSecurityException {
        this.keyStoreDetails = createKeyStoreDetails();
        this.keyStoreInitializer = new DefaultKeyStoreInitializer();
        Security.addProvider(new BouncyCastleProvider());
        if (PKCS11.equals(keyStoreDetails.getType())) {
            final String config = IdentityServer.getInstance().getProperty(PKCS11_CONFIG);
            try {
                // Use reflection to try and load the class at runtime this is necessary because SunPKCS11
                // does not exist on windows when using a 64 bit JDK < 1.8b49.
                // see  for details.
                final Class<?> clazz = Class.forName("sun.security.pkcs11.SunPKCS11");
                if (config != null) {
                    try {
                        Security.addProvider((Provider) clazz.getConstructor(String.class).newInstance(config));
                    } catch (final NoSuchMethodException
                            |IllegalAccessException
                            |InstantiationException
                            |InvocationTargetException e) {
                        logger.error("Unable to find SunPKCS11 constructor", e);
                        throw new GeneralSecurityException("Unable to find SunPKCS11 constructor", e);
                    }
                } else {
                    logger.error("SunPKCS11 config not provided");
                    throw new GeneralSecurityException("SunPKCS11 config not provided");
                }
            } catch (final ClassNotFoundException e) {
                // This should only happen if the user is trying to use PKCS11 on windows with a 64 bit JDK older than
                // 8b49
                logger.error("SunPKCS11 class not available.", e);
                throw new GeneralSecurityException("SunPKCS11 class not available.", e);
            }
        }
    }

    @Override
    public KeyStore getKeyStore() {
        return store;
    }

    @Override
    public KeyStoreDetails getKeyStoreDetails() {
        return keyStoreDetails;
    }

    @Override
    public void store() throws GeneralSecurityException {
        if (store != null) {
            try (final OutputStream outputStream = keyStoreDetails.getOutputStream()) {
                store.store(outputStream, keyStoreDetails.getPassword().toCharArray());
            } catch (final IOException|KeyStoreException|CertificateException|NoSuchAlgorithmException e) {
                logger.warn("Unable to store keystore", e);
                throw new GeneralSecurityException("Unable to store keystore", e);
            }
        }
    }

    /**
     * Create the {@link KeyStoreDetails} for this KeyStoreService.
     * @return the {@link KeyStoreDetails}.
     * @throws GeneralSecurityException if unable to create the {@link KeyStoreDetails}.
     */
    abstract KeyStoreDetails createKeyStoreDetails() throws GeneralSecurityException;

    /**
     * Checks if the {@link KeyStoreService} is using a file based config.
     * @return true if the filename is not set to NONE; false otherwise.
     */
    boolean isUsingFile(final String filename) {
        return !NONE.equals(filename.toLowerCase());
    }
}
