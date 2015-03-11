/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS. All Rights Reserved
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
 */

package org.forgerock.openidm.test.module;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.forgerock.json.resource.JsonResource;
import org.forgerock.json.resource.JsonResourceAccessor;
import org.forgerock.json.resource.JsonResourceContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;
import org.testng.Assert;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;

/**
 * A OpenIDMTestModule does ...
 *
 */
public class OpenIDMTestModule extends AbstractModule {

    private ConcurrentMap<String, ServiceTracker> serviceMap = new ConcurrentHashMap<String, ServiceTracker>();

    public static final String ROUTER = "router";
    private BundleContext context;

    OpenIDMTestModule(BundleContext context) {
        this.context = context;
    }

    protected void configure() {
        bind(BundleContext.class).toInstance(context);
    }


    @Provides
    @Named(ROUTER)
    public JsonResourceAccessor provideRouter() {
        ServiceTracker<JsonResource, JsonResource> tracker = null;
        try {
            Filter filter = context.createFilter(
                    "(&(objectclass=" + JsonResource.class.getName() + ")(service.pid=org.forgerock.openidm.router))");
            ServiceTracker newTracker = new ServiceTracker<JsonResource, JsonResource>(context, filter, null);

            tracker = serviceMap.putIfAbsent(ROUTER, newTracker);
            if (null == tracker) {
                tracker = newTracker;
                tracker.open();
            }

        } catch (InvalidSyntaxException e) {
            /* ignore */
        }

        Object resource = tracker.getService();
        if (resource instanceof JsonResource) {
            return new JsonResourceAccessor((JsonResource)resource,
                    JsonResourceContext.newContext("resource", JsonResourceContext.newRootContext()));
        }
        return null;
    }
}
