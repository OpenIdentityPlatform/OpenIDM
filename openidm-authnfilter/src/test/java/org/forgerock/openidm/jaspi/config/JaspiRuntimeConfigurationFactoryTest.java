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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.openidm.jaspi.config;

import org.forgerock.auth.common.AuditLogger;
import org.forgerock.auth.common.AuditLoggingConfigurator;
import org.forgerock.auth.common.AuditRecord;
import org.forgerock.auth.common.DebugLogger;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.openidm.jaspi.modules.IDMAuthModule;
import org.forgerock.openidm.jaspi.modules.IDMJaspiModuleWrapper;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.*;
import static org.forgerock.openidm.jaspi.auth.AuthenticationService.*;

public class JaspiRuntimeConfigurationFactoryTest {

    private JaspiRuntimeConfigurationFactory configurationFactory;

    @BeforeClass
    public void setUp() {
        configurationFactory = JaspiRuntimeConfigurationFactory.INSTANCE;
    }

    @AfterMethod
    public void tearDown() {
        configurationFactory.clear();
    }

    @Test (expectedExceptions = JsonValueException.class)
    public void setModuleConfigurationShouldThrowJsonValueExceptionWhenServerAuthContextPropertyNotSet()
            throws Exception {

        //Given
        JsonValue moduleConfiguration = JsonValue.json(
            JsonValue.object()
        );

        //When
        configurationFactory.setModuleConfiguration(moduleConfiguration);

        //Then
        fail();
    }

    @Test (expectedExceptions = JsonValueException.class)
    public void setModuleConfigurationShouldThrowJsonValueExceptionWhenAuthModulesPropertyNotSet() throws Exception {

        //Given
        JsonValue moduleConfiguration = JsonValue.json(
            JsonValue.object(
                JsonValue.field(SERVER_AUTH_CONTEXT_KEY, JsonValue.object())
            )
        );

        //When
        configurationFactory.setModuleConfiguration(moduleConfiguration);

        //Then
        fail();
    }

    @Test
    public void setModuleConfigurationShouldNotSetDisabledSessionAuthModule() throws Exception {

        //Given
        JsonValue moduleConfiguration = JsonValue.json(
            JsonValue.object(
                JsonValue.field(SERVER_AUTH_CONTEXT_KEY, JsonValue.object(
                    JsonValue.field(SESSION_MODULE_KEY, JsonValue.object(
                            JsonValue.field("enabled", false)
                    )),
                    JsonValue.field(AUTH_MODULES_KEY, JsonValue.array())
                ))
            )
        );

        //When
        configurationFactory.setModuleConfiguration(moduleConfiguration);

        //Then
        assertFalse(configurationFactory.getConfiguration()
                .get(SERVER_AUTH_CONTEXT_KEY)
                .isDefined(SESSION_MODULE_KEY));
    }

    @Test
    public void setModuleConfigurationShouldSetExplicitlyEnabledSessionAuthModule() throws Exception {

        //Given
        JsonValue moduleConfiguration = JsonValue.json(
                JsonValue.object(
                JsonValue.field(SERVER_AUTH_CONTEXT_KEY, JsonValue.object(
                    JsonValue.field(SESSION_MODULE_KEY, JsonValue.object(
                        JsonValue.field("enabled", true),
                        JsonValue.field(AUTH_MODULE_CLASS_NAME_KEY, "AUTH_MODULE_CLASS_NAME")
                    )),
                    JsonValue.field(AUTH_MODULES_KEY, JsonValue.array())
                ))
            )
        );

        //When
        configurationFactory.setModuleConfiguration(moduleConfiguration);

        //Then
        JsonValue sessionModuleConfig = configurationFactory.getConfiguration()
                .get(SERVER_AUTH_CONTEXT_KEY)
                .get(SESSION_MODULE_KEY);
        assertEquals(sessionModuleConfig.get(AUTH_MODULE_CLASS_NAME_KEY).asString(),
                IDMJaspiModuleWrapper.class.getName());
        assertEquals(sessionModuleConfig.get(AUTH_MODULE_PROPERTIES_KEY)
                .get("authModuleClassName").asString(), "AUTH_MODULE_CLASS_NAME");
        assertFalse(sessionModuleConfig.isDefined("enabled"));
    }

    @Test
    public void setModuleConfigurationShouldSetImplicitlyEnabledSessionAuthModule() throws Exception {

        //Given
        JsonValue moduleConfiguration = JsonValue.json(
            JsonValue.object(
                JsonValue.field(SERVER_AUTH_CONTEXT_KEY, JsonValue.object(
                    JsonValue.field(SESSION_MODULE_KEY, JsonValue.object(
                        JsonValue.field(AUTH_MODULE_CLASS_NAME_KEY, "AUTH_MODULE_CLASS_NAME")
                    )),
                    JsonValue.field(AUTH_MODULES_KEY, JsonValue.array())
                ))
            )
        );

        //When
        configurationFactory.setModuleConfiguration(moduleConfiguration);

        //Then
        JsonValue sessionModuleConfig = configurationFactory.getConfiguration()
                .get(SERVER_AUTH_CONTEXT_KEY)
                .get(SESSION_MODULE_KEY);
        assertEquals(sessionModuleConfig.get(AUTH_MODULE_CLASS_NAME_KEY).asString(),
                IDMJaspiModuleWrapper.class.getName());
        assertEquals(sessionModuleConfig.get(AUTH_MODULE_PROPERTIES_KEY)
                .get("authModuleClassName").asString(), "AUTH_MODULE_CLASS_NAME");
    }

    @Test
    public void setModuleConfigurationShouldResolveSessionAuthModuleClassAlias() throws Exception {

        //Given
        JsonValue moduleConfiguration = JsonValue.json(
            JsonValue.object(
                JsonValue.field(SERVER_AUTH_CONTEXT_KEY, JsonValue.object(
                    JsonValue.field(SESSION_MODULE_KEY, JsonValue.object(
                        JsonValue.field("name", IDMAuthModule.IWA.toString())
                    )),
                    JsonValue.field(AUTH_MODULES_KEY, JsonValue.array())
                ))
            )
        );

        //When
        configurationFactory.setModuleConfiguration(moduleConfiguration);

        //Then
        JsonValue sessionModuleConfig = configurationFactory.getConfiguration()
                .get(SERVER_AUTH_CONTEXT_KEY)
                .get(SESSION_MODULE_KEY);
        assertEquals(sessionModuleConfig.get(AUTH_MODULE_CLASS_NAME_KEY).asString(),
                IDMJaspiModuleWrapper.class.getName());
        assertEquals(sessionModuleConfig.get(AUTH_MODULE_PROPERTIES_KEY)
                .get("authModuleClassName").asString(), IDMAuthModule.IWA.getAuthModuleClass().getName());
    }

    @Test
    public void setModuleConfigurationShouldRemoveDisabledAuthModuleFromList() throws Exception {

        //Given
        JsonValue moduleConfiguration = JsonValue.json(
            JsonValue.object(
                JsonValue.field(SERVER_AUTH_CONTEXT_KEY, JsonValue.object(
                    JsonValue.field(AUTH_MODULES_KEY, JsonValue.array(
                        JsonValue.object(
                            JsonValue.field("name", IDMAuthModule.JWT_SESSION.toString())
                        ),
                        JsonValue.object(
                            JsonValue.field("name", IDMAuthModule.INTERNAL_USER.toString()),
                            JsonValue.field("enabled", false)
                        )
                    ))
                ))
            )
        );

        //When
        configurationFactory.setModuleConfiguration(moduleConfiguration);

        //Then
        JsonValue authModuleConfig = configurationFactory.getConfiguration()
                .get(SERVER_AUTH_CONTEXT_KEY)
                .get(AUTH_MODULES_KEY);
        assertEquals(authModuleConfig.size(), 1);
        assertEquals(authModuleConfig.get(0).get(AUTH_MODULE_CLASS_NAME_KEY).asString(),
                IDMJaspiModuleWrapper.class.getName());
        assertEquals(authModuleConfig.get(0).get(AUTH_MODULE_PROPERTIES_KEY)
                .get("authModuleClassName").asString(), IDMAuthModule.JWT_SESSION.getAuthModuleClass().getName());
    }

    @Test
    public void shouldGetLoggingConfigurator() {

        //Given

        //When
        AuditLoggingConfigurator loggingConfigurator = JaspiRuntimeConfigurationFactory.getLoggingConfigurator();

        //Then
        assertEquals(loggingConfigurator, configurationFactory);
    }

    @Test
    public void shouldGetDebugLogger() {

        //Given

        //When
        DebugLogger debugLogger = configurationFactory.getDebugLogger();

        //Then
        assertTrue(JaspiDebugLogger.class.isAssignableFrom(debugLogger.getClass()));
    }

    public static final class TestJaspiAuditLogger implements AuditLogger {

        @Override
        public void audit(AuditRecord auditRecord) {

        }
    }
}
