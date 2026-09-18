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
package org.forgerock.openidm.script;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Dictionary;

import org.forgerock.json.resource.RequestHandler;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptEvent;
import org.forgerock.script.ScriptRegistry;
import org.mockito.Matchers;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for the script-listener side of {@link AbstractScriptedService}: registering the request
 * handler when the script appears and unregistering it when the script goes away.
 */
public class AbstractScriptedServiceTest {

    private static class TestService extends AbstractScriptedService {
        private final BundleContext bundleContext = mock(BundleContext.class);

        @Override
        protected BundleContext getBundleContext() {
            return bundleContext;
        }

        @Override
        protected ScriptRegistry getScriptRegistry() {
            return mock(ScriptRegistry.class);
        }
    }

    private TestService service;
    private ServiceRegistration<RequestHandler> registration;

    @BeforeMethod
    @SuppressWarnings("unchecked")
    public void setUp() {
        service = new TestService();
        registration = mock(ServiceRegistration.class);
        when(service.bundleContext.registerService(eq(RequestHandler.class), any(RequestHandler.class),
                Matchers.<Dictionary<String, ?>>any())).thenReturn(registration);
    }

    private static ScriptEvent event(int type) throws Exception {
        ScriptEvent event = mock(ScriptEvent.class);
        when(event.getType()).thenReturn(type);
        when(event.getScriptLibraryEntry()).thenReturn(mock(ScriptEntry.class));
        return event;
    }

    @Test
    public void registeredEventRegistersTheHandlerExactlyOnce() throws Exception {
        service.scriptChanged(event(ScriptEvent.REGISTERED));
        service.scriptChanged(event(ScriptEvent.REGISTERED));

        verify(service.bundleContext, times(1)).registerService(eq(RequestHandler.class),
                any(RequestHandler.class), Matchers.<Dictionary<String, ?>>any());
    }

    @Test
    public void unregisteringEventUnregistersTheHandler() throws Exception {
        service.scriptChanged(event(ScriptEvent.REGISTERED));

        service.scriptChanged(event(ScriptEvent.UNREGISTERING));

        verify(registration).unregister();
    }

    @Test
    public void unregisteringEventBeforeRegistrationIsIgnored() throws Exception {
        service.scriptChanged(event(ScriptEvent.UNREGISTERING));

        verify(service.bundleContext, times(0)).registerService(eq(RequestHandler.class),
                any(RequestHandler.class), Matchers.<Dictionary<String, ?>>any());
    }
}
