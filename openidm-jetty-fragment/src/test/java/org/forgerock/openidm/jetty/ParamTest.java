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
package org.forgerock.openidm.jetty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openidm.core.IdentityServer.*;

import java.util.Properties;

import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.PropertyAccessor;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ParamTest {

    private static final String KEYSTORE_FILE_LOCATION = "keystore/location";
    private static final String TRUSTSTORE_FILE_LOCATION = "truststore/location";
    private static final String CONF_JETTY_XML = "conf/jetty.xml";
    private static final String NONE = "none";
    private final TestPropertyAccessor propertyAccessor = new TestPropertyAccessor(new Properties());

    @BeforeClass
    public void setup() throws IllegalStateException {
        // attempt to initialize the identity server
        IdentityServer.initInstance(propertyAccessor);
    }

    @Test
    public void testGetKeyStoreLocation() {
        // given
        final Properties properties = createDefaultProperties();
        propertyAccessor.setProperties(properties);

        // when
        final String keyStoreLocation = Param.getKeystoreLocation();

        // then
        assertThat(keyStoreLocation)
                .isNotNull()
                .isEqualTo(IdentityServer.getFileForInstallPath(KEYSTORE_FILE_LOCATION).getAbsolutePath());
    }

    @Test
    public void testGetTrustStoreLocation() {
        // given
        final Properties properties = createDefaultProperties();
        propertyAccessor.setProperties(properties);

        // when
        final String trustStoreLocation = Param.getTruststoreLocation();

        // then
        assertThat(trustStoreLocation)
                .isNotNull()
                .isEqualTo(IdentityServer.getFileForInstallPath(TRUSTSTORE_FILE_LOCATION).getAbsolutePath());
    }

    @Test
    public void testGetKeyStoreLocationForPKCS11() {
        // given
        final Properties properties = createPKCS11Properties();
        propertyAccessor.setProperties(properties);

        // when
        final String keyStoreLocation = Param.getKeystoreLocation();

        // then
        assertThat(keyStoreLocation)
                .isNotNull()
                .isEqualTo(IdentityServer.getFileForInstallPath(CONF_JETTY_XML).getAbsolutePath());
    }

    @Test
    public void testGetTrustStoreLocationPKCS11() {
        // given
        final Properties properties = createPKCS11Properties();
        propertyAccessor.setProperties(properties);

        // when
        final String trustStoreLocation = Param.getTruststoreLocation();

        // then
        assertThat(trustStoreLocation)
                .isNotNull()
                .isEqualTo(IdentityServer.getFileForInstallPath(CONF_JETTY_XML).getAbsolutePath());
    }

    @Test
    public void testGetTrustStoreLocationWhenNotSet() {
        // given
        final Properties properties = createDefaultProperties();
        properties.remove(TRUSTSTORE_LOCATION);
        propertyAccessor.setProperties(properties);

        // when
        final String trustStoreLocation = Param.getTruststoreLocation();

        // then
        assertThat(trustStoreLocation)
                .isNotNull()
                .isEqualTo(IdentityServer.getFileForInstallPath(KEYSTORE_FILE_LOCATION).getAbsolutePath());
    }

    private Properties createPKCS11Properties() {
        final Properties properties = new Properties();
        properties.put(KEYSTORE_LOCATION, NONE);
        properties.put(TRUSTSTORE_LOCATION, NONE);
        return properties;
    }

    private Properties createDefaultProperties() {
        final Properties properties = new Properties();
        properties.put(KEYSTORE_LOCATION, KEYSTORE_FILE_LOCATION);
        properties.put(TRUSTSTORE_LOCATION, TRUSTSTORE_FILE_LOCATION);
        return properties;
    }

    private static class TestPropertyAccessor implements PropertyAccessor {
        private Properties properties;

        TestPropertyAccessor(final Properties properties) {
            setProperties(properties);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getProperty(String key, T defaultValue, Class<T> expected) {
            return properties.get(key) != null && properties.get(key).getClass().isAssignableFrom(expected)
                    ? (T) properties.get(key)
                    : defaultValue;
        }

        void setProperties(final Properties properties) {
            this.properties = properties;
        }
    }
}
