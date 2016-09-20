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
package org.forgerock.openidm.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URLDecoder;
import java.util.Properties;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests the {@link IdentityServer} class.
 */
public class IdentityServerTest {

    private static final String A_SYSTEM_PROPERTY = "A SYSTEM PROPERTY";
    private static final String CONFIG = "CONFIG";
    private static final String TEST_PROPERTY_KEY = IdentityServerTest.class.getName();
    private static final String TEST_PROPERTY_KEY_NOT_IN_BOOT = TEST_PROPERTY_KEY + "_NOT_BOOT";
    private static final Properties testProperties = new Properties();

    @BeforeClass
    public void setup() throws Exception {
        testProperties.setProperty(TEST_PROPERTY_KEY, CONFIG);
        testProperties.setProperty(TEST_PROPERTY_KEY_NOT_IN_BOOT, CONFIG);
        String bootFile =
                URLDecoder.decode(IdentityServerTest.class.getResource(
                        "/" + ServerConstants.DEFAULT_BOOT_FILE_LOCATION).getFile(), "UTF-8");
        assertThat(new File(bootFile).canRead()).isTrue();
        System.setProperty(ServerConstants.PROPERTY_BOOT_FILE_LOCATION, bootFile);
        IdentityServer.initInstance(new TestPropertyAccessor());
    }


    /**
     * Validates that the System properties will override properties from the boot file.
     *
     * @throws Exception on failure.
     */
    @Test
    public void testSystemOverridesBootProperty() throws Exception {
        // Given
        IdentityServer identityServer = IdentityServer.getInstance();

        // Then
        // Test that the property is pulled from the system properties
        System.setProperty(TEST_PROPERTY_KEY, A_SYSTEM_PROPERTY);
        assertThat(identityServer.getProperty(TEST_PROPERTY_KEY)).isEqualTo(A_SYSTEM_PROPERTY);

        // Test that the property is now pulled from the BOOT file.
        System.clearProperty(TEST_PROPERTY_KEY);
        assertThat(identityServer.getProperty(TEST_PROPERTY_KEY, "NOT_BOOT")).isEqualTo("BOOT");
    }

    /**
     * Since we can't unset the property in the boot file, we need to test that System can override Config on a property
     * not in the boot file.
     */
    @Test
    public void testSystemOverridesConfig() {
        // Given
        IdentityServer identityServer = IdentityServer.getInstance();

        // Then
        System.setProperty(TEST_PROPERTY_KEY_NOT_IN_BOOT, A_SYSTEM_PROPERTY);
        assertThat(identityServer.getProperty(TEST_PROPERTY_KEY_NOT_IN_BOOT)).isEqualTo(A_SYSTEM_PROPERTY);

        // Test that the property is pulled from the passed Accessor.
        System.clearProperty(TEST_PROPERTY_KEY_NOT_IN_BOOT);
        assertThat(identityServer.getProperty(TEST_PROPERTY_KEY_NOT_IN_BOOT, "NOT_CONFIG")).isEqualTo(CONFIG);

        // Test that the default value is returned.
        testProperties.clear();
        assertThat(identityServer.getProperty(TEST_PROPERTY_KEY_NOT_IN_BOOT, "DEFAULT")).isEqualTo("DEFAULT");
    }

    private static class TestPropertyAccessor implements PropertyAccessor {
        @Override
        @SuppressWarnings("unchecked")
        public <T> T getProperty(String key, T defaultValue, Class<T> expected) {
            String property = testProperties.getProperty(key);
            return (null != property)
                    ? (T) property
                    : defaultValue;
        }
    }
}
