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
 * Copyright 2026 3A Systems, LLC.
 */
package org.forgerock.openidm.info.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.PropertyAccessor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.ComponentContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for the start-up readiness check of {@link HealthService}.
 */
public class HealthServiceTest {

    private static final String SERVICE_START_MAX_PROPERTY = "openidm.healthservice.servicestartmax";

    private Bundle systemBundle;
    private BundleContext bundleContext;
    private ComponentContext componentContext;
    private HealthService healthService;

    @BeforeClass
    public void initIdentityServer() {
        try {
            IdentityServer.initInstance((PropertyAccessor) null);
        } catch (IllegalStateException e) {
            // already initialised by another test in this JVM
        }
    }

    @BeforeMethod
    public void setUp() throws Exception {
        systemBundle = mock(Bundle.class);
        bundleContext = mock(BundleContext.class);
        when(bundleContext.getBundle(0)).thenReturn(systemBundle);
        when(bundleContext.getBundles()).thenReturn(new Bundle[0]);
        when(bundleContext.createFilter(anyString())).thenAnswer(
                invocation -> FrameworkUtil.createFilter((String) invocation.getArguments()[0]));
        componentContext = mock(ComponentContext.class);
        when(componentContext.getBundleContext()).thenReturn(bundleContext);
        healthService = new HealthService();
    }

    @AfterMethod
    public void tearDown() {
        System.clearProperty(SERVICE_START_MAX_PROPERTY);
        healthService.deactivate(componentContext);
    }

    /**
     * On samples/workflow the Activiti FileInstall refreshes the freshly installed .bar bundles while
     * the framework is still raising its start level. The resulting PACKAGES_REFRESHED event must not
     * be mistaken for a started framework, otherwise a premature start-up check reports a failure.
     */
    @Test
    public void packagesRefreshedDuringActivationDoesNotTriggerStartupCheck() throws Exception {
        when(systemBundle.getState()).thenReturn(Bundle.STARTING);
        // deliver the event right after the listener is registered, i.e. inside activate()
        doAnswer(invocation -> {
            FrameworkListener listener = (FrameworkListener) invocation.getArguments()[0];
            listener.frameworkEvent(new FrameworkEvent(FrameworkEvent.PACKAGES_REFRESHED, systemBundle, null));
            return null;
        }).when(bundleContext).addFrameworkListener(any(FrameworkListener.class));

        healthService.activate(componentContext);
        Thread.sleep(2500);

        assertEquals(state(), "STARTING");
    }

    /**
     * When the framework is already active the STARTED event will never arrive, so the start-up check
     * has to be scheduled from activate() — after the configured grace period, not a hard-coded one.
     */
    @Test
    public void frameworkAlreadyActiveSchedulesStartupCheckAfterServiceStartMax() throws Exception {
        System.setProperty(SERVICE_START_MAX_PROPERTY, "200");
        when(systemBundle.getState()).thenReturn(Bundle.ACTIVE);

        healthService.activate(componentContext);
        Thread.sleep(1000);

        assertEquals(state(), "ACTIVE_NOT_READY");
    }

    private String state() {
        return healthService.getHealthInfo().get("state").asString();
    }
}
