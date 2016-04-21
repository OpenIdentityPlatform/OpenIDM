/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 ForgeRock AS. All Rights Reserved
 */

package org.forgerock.openidm.provisioner.salesforce.internal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.Router;
import org.forgerock.openidm.config.enhanced.JSONEnhancedConfig;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.crypto.factory.CryptoServiceFactory;
import org.forgerock.openidm.crypto.factory.CryptoUpdateService;
import org.forgerock.openidm.crypto.impl.UpdatableKeyStoreSelector;
import org.forgerock.openidm.router.RouteBuilder;
import org.forgerock.openidm.router.RouteEntry;
import org.forgerock.openidm.router.RouterRegistry;
import org.forgerock.services.routing.RouteMatcher;
import org.osgi.service.component.ComponentContext;

/**
 * A TestUtil helps to setup new tests.
 *
 * @author Laszlo Hordos
 */
public final class TestUtil {

    private static final String CHANGEIT = "changeit";

    // Prevent instantiation.
    private TestUtil() {
        // Nothing to do.
    }

    public static class TestRouterRegistry implements RouterRegistry {

        private final Router router = new Router();

        private final Connection connection = Resources.newInternalConnection(router);

        public Connection getConnection() {
            return connection;
        }

        @Override
        public RouteEntry addRoute(RouteBuilder routeBuilder) {
            if (null != routeBuilder) {
                return new RouteEntryImpl(routeBuilder);
            }
            return null;
        }

        private class RouteEntryImpl implements RouteEntry {

            protected RouteMatcher[] registeredRoutes;

            RouteEntryImpl(final RouteBuilder builder) {
                registeredRoutes = builder.register(router);

            }

            public boolean removeRoute() {
                boolean isModified = false;
                final Router r = router;
                if (r != null) {
                    isModified = r.removeRoute(registeredRoutes);
                }
                return isModified;
            }
        }
    }

    public static <T> T setField(T service, String fieldName, Object value) {
        assertNotNull(service, "Target service is null");

        try {
            Field field = service.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(service, value);
        } catch (NoSuchFieldException e) {
            fail("Field name is not defined", e);
        } catch (IllegalAccessException e) {
            fail("Can not set the field value", e);
        }
        return service;
    }

    public static <T> Pair<T, ComponentContext> activate(T service, String factoryName,
            JsonValue configuration) {
        assertNotNull(service, "Target service is null");
        assertNotNull(configuration, "Configuration is null");

        Hashtable<String, Object> properties = new Hashtable<String, Object>();
        if (StringUtils.isNotBlank(factoryName)) {
            properties.put(ServerConstants.CONFIG_FACTORY_PID, factoryName);
        }
        properties.put(JSONEnhancedConfig.JSON_CONFIG_PROPERTY, configuration.toString());

        ComponentContext context = mock(ComponentContext.class);
        when(context.getProperties()).thenReturn(properties);
        when(context.getBundleContext()).thenReturn(null);

        try {
            // Todo implement better way to find the method
            Method method =
                    service.getClass().getDeclaredMethod("activate", ComponentContext.class);
            method.setAccessible(true);
            method.invoke(service, context);
        } catch (NoSuchMethodException e) {
            fail("Failed to find Activation method", e);
        } catch (IllegalAccessException e) {
            fail("Can not set the field value", e);
        } catch (InvocationTargetException e) {
            fail("Can not invoke the activation method", e);
        }
        return ImmutablePair.of(service, context);
    }

    public static void initCryptoService() {
        CryptoUpdateService cryptoUpdateService =
                (CryptoUpdateService) CryptoServiceFactory.getInstance();
        InputStream is = null;
        try {
            KeyStore ks = KeyStore.getInstance("JCEKS");
            is = new ByteArrayInputStream(Base64.decodeBase64(KEYSTORE.getBytes()));
            ks.load(is, CHANGEIT.toCharArray());

            Enumeration<String> e = ks.aliases();
            while (e.hasMoreElements()) {
                System.out.append(e.nextElement()).println();
            }

            setField(cryptoUpdateService, "keySelector",
                    new UpdatableKeyStoreSelector(ks, CHANGEIT)).updateKeySelector(ks, CHANGEIT);
        } catch (KeyStoreException e) {
            fail("JCEKS Keystore is not supported", e);
        } catch (Exception e) {
            fail("Load initialise Test keystore");
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    /* ignore */
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        InputStream is = TestUtil.class.getResourceAsStream("/keystore.jceks");
        assertNotNull(is);
        byte[] encoded = Base64.encodeBase64(IOUtils.toByteArray(is));

        System.out.append("------- KEYSTORE START -------").append(
                System.getProperty("line.separator")).append(new String(encoded)).append(
                System.getProperty("line.separator")).println("-------- KEYSTORE END --------");
    }

    public static final String KEYSTORE =
            "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";

}
