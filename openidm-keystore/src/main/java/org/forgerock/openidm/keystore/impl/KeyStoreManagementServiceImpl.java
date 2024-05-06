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
 * Portions Copyrighted 2024 3A Systems LLC.
 */
package org.forgerock.openidm.keystore.impl;

import static org.forgerock.openidm.core.IdentityServer.SSL_HOST_ALIASES;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;

import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.keystore.KeyStoreManagementService;
import org.forgerock.openidm.keystore.KeyStoreService;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.component.propertytypes.ServiceVendor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service to manage security mechanisms that require the keystore and truststore.
 */
@Component(
        name = KeyStoreManagementServiceImpl.PID,
        immediate = true,
        configurationPolicy = ConfigurationPolicy.IGNORE,
        property = Constants.SERVICE_PID + "=" + KeyStoreManagementServiceImpl.PID
)
@ServiceVendor(ServerConstants.SERVER_VENDOR_NAME)
@ServiceDescription("OpenIDM KeyStore Management Service")
public class KeyStoreManagementServiceImpl implements KeyStoreManagementService {

    private static final Logger logger = LoggerFactory.getLogger(KeyStoreManagementServiceImpl.class);

    /**
     * Regex to match a string with comma seperated keyvalue pairs. For example,
     * <pre>
     *     "localhost=my-key-alias, service.forgerock.com=fr-client"
     * </pre>
     */
    private static final String HOST_ALIAS_MAPPING_REGEX = ", *(?![^\\[\\]]*\\])";
    static final String PID = "org.forgerock.openidm.keystore.impl.manager";

    @Reference(target="(service.pid=" + KeyStoreServiceImpl.PID + ")")
    private KeyStoreService keyStore;

    @Reference(target="(service.pid=" + TrustStoreServiceImpl.PID + ")")
    private KeyStoreService trustStore;

    public void activate(@SuppressWarnings("unused") ComponentContext context) {
        reloadSslContext();
    }

    @Override
    public void reloadSslContext() {
        try {
            final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore.getKeyStore());
            final TrustManager[] trustManagers = tmf.getTrustManagers();

            final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore.getKeyStore(), keyStore.getKeyStoreDetails().getPassword().toCharArray());
            final KeyManager[] keyManagers = kmf.getKeyManagers();

            // Override the default X509KeyManager with our own MappedAliasKeyManager.
            // This allows for mapping hosts to specific key aliases within the keystore.
            for (int i = 0; i < keyManagers.length; i++) {
                if (keyManagers[i] instanceof X509KeyManager) {
                    keyManagers[i] = new MappedAliasKeyManager((X509KeyManager) keyManagers[i],
                            getHostAliasMappings(IdentityServer.getInstance().getProperty(SSL_HOST_ALIASES))
                    );
                }
            }

            final SSLContext context = SSLContext.getInstance("SSL");
            context.init(keyManagers, trustManagers, null);
            SSLContext.setDefault(context);
        } catch (final NoSuchAlgorithmException|KeyStoreException|KeyManagementException|UnrecoverableKeyException e) {
            logger.warn("Unable to reload the ssl context", e);
        }
    }

    /**
     * Parses a string of comma separated key-value pairs into a map.
     * For example: "localhost=my-key-alias, service.forgerock.com=fr-client"
     *
     * @param mappings The comma-separated string of key-value pairs to parse
     * @return A map containing the key value pairs. The map may contain null values.
     */
    private Map<String, String> getHostAliasMappings(final String mappings) {
        Map<String, String> map = new HashMap<>();
        if (mappings != null) {
            for (String pair : mappings.split(HOST_ALIAS_MAPPING_REGEX)) {
                String[] parts = pair.split("=");
                if (parts.length == 2) {
                    map.put(parts[0].toUpperCase(), parts[1]);
                } else if (parts.length == 1) {
                    map.put(parts[0].toUpperCase(), null);
                }
            }
        }
        return map;
    }

    public void bindKeyStore(KeyStoreService keyStoreService) {
        this.keyStore=keyStoreService;
    }

    public void bindTrustStore(KeyStoreService trustStoreService) {
        this.trustStore=trustStoreService;
    }
}
