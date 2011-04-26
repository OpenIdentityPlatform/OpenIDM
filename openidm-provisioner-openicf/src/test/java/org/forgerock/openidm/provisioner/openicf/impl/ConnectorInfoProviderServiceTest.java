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

import org.osgi.framework.*;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

import static org.mockito.Mockito.*;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class ConnectorInfoProviderServiceTest {

    private Dictionary properties = null;

    @BeforeTest
    public void beforeTest() throws Exception {
        InputStream inputStream = ConnectorInfoProviderServiceTest.class.getResourceAsStream("/config/ConnectorInfoProviderServiceConfiguration.json");
        Assert.assertNotNull(inputStream);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] temp = new byte[1024];
        int read;
        while ((read = inputStream.read(temp)) > 0) {
            buffer.write(temp, 0, read);
        }
        String config = new String(buffer.toByteArray());

        properties = new Hashtable<String, Object>();
        properties.put("jsonconfig", config);

    }

    @AfterTest
    public void afterTest() {
    }


    @Test
    public void testActivateProperly() throws Exception {
        URL root = ConnectorInfoProviderServiceTest.class.getResource("/connectorServer/");
        Assert.assertNotNull(root);
        Map<String, String> systemProperties = new HashMap<String, String>(1);
        systemProperties.put("bundles.configuration.location", root.getPath());

        ComponentContext context = mock(ComponentContext.class);
        //stubbing
        when(context.getProperties()).thenReturn(properties);
        when(context.getBundleContext()).thenReturn(new InnerBundleContext(systemProperties));
        InnerConnectorInfoProviderService instance = new InnerConnectorInfoProviderService();
        instance.activate(context);
    }

    @Test(expectedExceptions = ComponentException.class)
    public void testActivateNoConfiguration() throws Exception {
        ComponentContext context = mock(ComponentContext.class);
        //stubbing
        when(context.getProperties()).thenReturn(new Hashtable<String, String>());
        when(context.getBundleContext()).thenReturn(new InnerBundleContext());
        InnerConnectorInfoProviderService instance = new InnerConnectorInfoProviderService();
        instance.activate(context);
    }

//    @Test
//    public void testFindConnectorInfo() throws Exception {
//        ComponentContext context = mock(ComponentContext.class);
//        //stubbing
//        when(context.getProperties()).thenReturn(properties);
//        when(context.getBundleContext()).thenReturn(new InnerBundleContext());
//        InnerConnectorInfoProviderService instance = new InnerConnectorInfoProviderService();
//        instance.activate(context);
//    }

    @Test
    public void testGetConnectorInfos() throws Exception {

    }

    public class InnerConnectorInfoProviderService extends ConnectorInfoProviderService {
        @Override
        public void activate(ComponentContext context) {
            super.activate(context);    //To change body of overridden methods use File | Settings | File Templates.
        }

        @Override
        public void deactivate(ComponentContext context) {
            super.deactivate(context);    //To change body of overridden methods use File | Settings | File Templates.
        }
    }

    private class InnerBundleContext implements BundleContext {

        private Map<String, String> properties;

        private InnerBundleContext() {
            properties = Collections.emptyMap();
        }

        private InnerBundleContext(Map<String, String> properties) {
            assert null != properties;
            this.properties = properties;
        }

        @Override
        public String getProperty(String s) {
            String value = properties.get(s);
            if (null == value) {
                value = System.getProperty(s);
            }
            return value;
        }

        @Override
        public Bundle getBundle() {
            return null;
        }

        @Override
        public Bundle installBundle(String s) throws BundleException {
            return null;
        }

        @Override
        public Bundle installBundle(String s, InputStream inputStream) throws BundleException {
            return null;
        }

        @Override
        public Bundle getBundle(long l) {
            return null;
        }

        @Override
        public Bundle[] getBundles() {
            return new Bundle[0];
        }

        @Override
        public void addServiceListener(ServiceListener serviceListener, String s) throws InvalidSyntaxException {

        }

        @Override
        public void addServiceListener(ServiceListener serviceListener) {

        }

        @Override
        public void removeServiceListener(ServiceListener serviceListener) {

        }

        @Override
        public void addBundleListener(BundleListener bundleListener) {

        }

        @Override
        public void removeBundleListener(BundleListener bundleListener) {

        }

        @Override
        public void addFrameworkListener(FrameworkListener frameworkListener) {

        }

        @Override
        public void removeFrameworkListener(FrameworkListener frameworkListener) {

        }

        @Override
        public ServiceRegistration registerService(String[] strings, Object o, Dictionary dictionary) {
            return null;
        }

        @Override
        public ServiceRegistration registerService(String s, Object o, Dictionary dictionary) {
            return null;
        }

        @Override
        public ServiceReference[] getServiceReferences(String s, String s1) throws InvalidSyntaxException {
            return new ServiceReference[0];
        }

        @Override
        public ServiceReference[] getAllServiceReferences(String s, String s1) throws InvalidSyntaxException {
            return new ServiceReference[0];
        }

        @Override
        public ServiceReference getServiceReference(String s) {
            return null;
        }

        @Override
        public Object getService(ServiceReference reference) {
            return null;
        }

        @Override
        public boolean ungetService(ServiceReference reference) {
            return false;
        }

        @Override
        public File getDataFile(String s) {
            return null;
        }

        @Override
        public Filter createFilter(String s) throws InvalidSyntaxException {
            return null;
        }
    }
}
