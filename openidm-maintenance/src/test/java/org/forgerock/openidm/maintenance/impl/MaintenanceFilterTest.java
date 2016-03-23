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
package org.forgerock.openidm.maintenance.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Requests.newCreateRequest;
import static org.forgerock.json.resource.Requests.newReadRequest;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.openidm.mocks.MockRequestHandler;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test the maintenance filter.
 */
public class MaintenanceFilterTest {

    private final Context root = new RootContext();
    private final Context update = new UpdateContext(new RootContext());
    private final JsonValue object = json(object());

    @DataProvider
    public Object[][] createCases() {
        return new Object[][]{
                // @formatter:off
                // I. Maintenance mode enabled tests
                //  A. non-update context
                { true,  "config",              root, false }, // disallow config writes
                { true,  "repo",                root, false }, // disallow repo writes
                { true,  "audit",               root,  true }, // allow auditing of update processes
                { true,  "system",              root, false },
                { true,  "maintenance",         root,  true }, // allow requests on maintenance endpoint(s)
                { true,  "maintenance/updates", root,  true }, // allow requests on maintenance/update

                //  B. allow all requests with update context
                { true,  "config",      update, true },
                { true,  "repo",        update, true },
                { true,  "audit",       update, true },
                { true,  "system",      update, true },
                { true,  "updates",     update, true },
                { true,  "maintenance", update, true },

                // II. Maintenance mode disabled tests - pass all requests
                { false, "config",      root,   true },
                { false, "repo",        root,   true },
                { false, "audit",       root,   true },
                { false, "updates",     root,   true },
                { false, "maintenance", root,   true }
                // @formatter:on
        };
    }

    /**
     * Test creates on endpoints are allowed/disallowed depending on maintenance mode.
     *
     * @param maintenanceEnabled whether maintenance is enabled
     * @param resourcePath the resource path requested
     * @param context the context, specifically to test presence/absence of UpdateContext
     * @param passRequest whether the request should be "passed", i.e. created
     */
    @Test(dataProvider = "createCases")
    public void testFitlerCreate(boolean maintenanceEnabled, String resourcePath, Context context, boolean passRequest) {
        final MockRequestHandler handler = new MockRequestHandler();
        final MaintenanceFilter filter = new MaintenanceFilter();
        if (maintenanceEnabled) {
            filter.enableMaintenanceMode();
        }

        final CreateRequest request = newCreateRequest(resourcePath, object);
        final Promise<ResourceResponse, ResourceException> promise = filter.filterCreate(context, request, handler);
        if (passRequest) {
            assertThat(promise).succeeded();
            assertThat(handler.getRequests()).containsExactly(request);
        } else {
            assertThat(promise).failedWithException().isInstanceOf(ServiceUnavailableException.class);
            assertThat(handler.getRequests()).isEmpty();
        }
    }

    // allow reads on these endpoints during maintenance
    @DataProvider
    public Object[][] readCases() {
        return new Object[][]{
                // @formatter:off
                { "config" },
                { "repo"   },
                { "system" },
                // @formatter:on
        };
    }

    /**
     * Test that endpoints can be read from during maintenance.
     *
     * @param resourcePath the resourcePath from which to read
     * @throws Exception if create/read throws an exception
     */
    @Test(dataProvider = "readCases")
    public void testFilterRead(String resourcePath) throws Exception {
        final MockRequestHandler handler = new MockRequestHandler();
        final MaintenanceFilter filter = new MaintenanceFilter();

        // first create an object
        final CreateRequest create = newCreateRequest(resourcePath, "1", object);
        Promise<ResourceResponse, ResourceException> promise = filter.filterCreate(root, create, handler);
        assertThat(promise).succeeded();
        assertThat(handler.getRequests()).containsExactly(create);

        handler.addResource(promise.get());
        filter.enableMaintenanceMode();

        // perform the read
        final ReadRequest request = newReadRequest(resourcePath, "1");
        promise = filter.filterRead(root, request, handler);
        assertThat(promise).succeeded();
        assertThat(handler.getRequests()).containsExactly(create, request);
    }
}
