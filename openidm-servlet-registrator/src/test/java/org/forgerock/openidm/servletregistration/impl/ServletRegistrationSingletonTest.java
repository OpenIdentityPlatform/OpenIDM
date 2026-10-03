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

package org.forgerock.openidm.servletregistration.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertSame;

import java.lang.reflect.Field;
import java.util.Hashtable;

import jakarta.servlet.Servlet;

import org.ops4j.pax.web.service.MultiBundleWebContainerContext;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ServletRegistrationSingletonTest {

    private WebContainer webContainer;
    private MultiBundleWebContainerContext sharedContext;
    private ServletRegistrationSingleton registration;

    @BeforeMethod
    public void setUp() throws Exception {
        webContainer = mock(WebContainer.class);
        sharedContext = mock(MultiBundleWebContainerContext.class);
        when(webContainer.createDefaultSharedHttpContext()).thenReturn(sharedContext);

        registration = new ServletRegistrationSingleton();
        Field field = ServletRegistrationSingleton.class.getDeclaredField("webContainer");
        field.setAccessible(true);
        field.set(registration, webContainer);
    }

    /**
     * SCR holds the component's state lock while activate() runs. A WebContainer call made there is
     * executed on the single pax-web configuration thread, which may itself be waiting for that
     * state lock while it registers the WebContainer service (#228), so activate() must not call it.
     */
    @Test
    public void activateDoesNotCallWebContainer() {
        registration.activate(componentContext());

        verifyZeroInteractions(webContainer);
    }

    @Test
    public void sharedContextIsCreatedOnceOnFirstUse() throws Exception {
        registration.activate(componentContext());
        Servlet servlet = mock(Servlet.class);
        Hashtable<String, Object> params = new Hashtable<>();

        assertSame(registration.getContext(), sharedContext);
        registration.registerServlet("/openidm", servlet, params);

        verify(webContainer, times(1)).createDefaultSharedHttpContext();
        verify(webContainer).registerServlet("/openidm", servlet, params, sharedContext);
    }

    private static ComponentContext componentContext() {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getBundleContext()).thenReturn(mock(BundleContext.class));
        return context;
    }
}
