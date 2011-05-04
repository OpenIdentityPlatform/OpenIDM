/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
 *
 * $Id$
 */

package org.forgerock.openidm.provisioner.openicf.impl;

import org.codehaus.jackson.map.ObjectMapper;
import org.fest.assertions.MapAssert;
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.openidm.config.installer.JSONConfigInstaller;
import org.forgerock.openidm.provisioner.ProvisionerService;
import org.forgerock.openidm.provisioner.openicf.ConnectorInfoProvider;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.fest.assertions.Assertions.assertThat;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public abstract class OpenICFProvisionerServiceTestBase {

    private TestLocalConnectorInfoProviderStub connectorInfoProvider = new TestLocalConnectorInfoProviderStub();
    private Dictionary properties = null;
    private ProvisionerService service = null;


    @BeforeTest
    public void BeforeTest() throws Exception {
        properties = new Hashtable<String, Object>(3);

        //Answer to the Ultimate Question of Life, the Universe, and Everything (42)
        properties.put(ComponentConstants.COMPONENT_ID, 42);
        properties.put(ComponentConstants.COMPONENT_NAME, OpenICFProvisionerServiceXMLConnectorTest.class.getCanonicalName());

        String configurationFile = getConfigurationFilePath();
        String config = getTestableSystemConfiguration(configurationFile);

        config = updateRuntimeConfiguration(config);

        properties.put(JSONConfigInstaller.JSON_CONFIG_PROPERTY, config);

        service = new OpenICFProvisionerService();

        Method bind = OpenICFProvisionerService.class.getDeclaredMethod("bind", ConnectorInfoProvider.class);
        Assert.assertNotNull(bind);
        bind.invoke(service, connectorInfoProvider);
        Method activate = OpenICFProvisionerService.class.getDeclaredMethod("activate", ComponentContext.class);
        Assert.assertNotNull(activate);

        ComponentContext context = mock(ComponentContext.class);
        //stubbing
        when(context.getProperties()).thenReturn(properties);
        activate.invoke(service, context);

    }

    protected abstract String updateRuntimeConfiguration(String config) throws Exception;

    protected String getConfigurationFilePath() {
        return "/config/" + getClass().getCanonicalName() + ".json";
    }

    protected String getTestableSystemConfiguration(String configurationFile) throws IOException {
        InputStream inputStream = TestLocalConnectorInfoProviderStub.class.getResourceAsStream(configurationFile);
        Assert.assertNotNull(inputStream, "Missing Configuration File at: " + configurationFile);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] temp = new byte[1024];
        int read;
        while ((read = inputStream.read(temp)) > 0) {
            buffer.write(temp, 0, read);
        }

        return new String(buffer.toByteArray());
    }

    protected ProvisionerService getService() {
        return service;
    }
}
