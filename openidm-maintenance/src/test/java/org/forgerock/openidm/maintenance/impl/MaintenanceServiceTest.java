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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openidm.maintenance.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.resource.Router.uriTemplate;

import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.MemoryBackend;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.services.context.RootContext;
import org.forgerock.json.resource.Router;
import org.forgerock.http.routing.RoutingMode;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test the Maintenance Service
 */
public class MaintenanceServiceTest {

    private final Router router = new Router();
    private final MaintenanceFilter maintenanceFilter = new MaintenanceFilter();
    private final MaintenanceService maintenanceService = new MaintenanceService();

    @BeforeClass
    public void BeforeClass() throws Exception {
        maintenanceService.bindMaintenanceFilter(maintenanceFilter);

        router.addRoute(uriTemplate("managed/user"), new MemoryBackend());
        router.addRoute(RoutingMode.EQUALS, uriTemplate("maintenance"), maintenanceService);
    }

    @Test(expectedExceptions = ServiceUnavailableException.class)
    public void testMaintenanceModeEnable() throws Exception {
        ActionRequest enableAction = Requests.newActionRequest("maintenance", "enable");
        assertThat(maintenanceService
                .handleAction(new RootContext(), enableAction)
                .get()
                .getJsonContent()
                .get("maintenanceEnabled")
                .asBoolean())
            .isEqualTo(true);
        maintenanceFilter
                .filterDelete(new RootContext(), Requests.newDeleteRequest("managed/user/0"), router)
                .getOrThrow();
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void testMaintenanceModeDisable() throws Exception {
        ActionRequest enableAction = Requests.newActionRequest("maintenance", "enable");
        assertThat(maintenanceService
                .handleAction(new RootContext(), enableAction)
                .get()
                .getJsonContent()
                .get("maintenanceEnabled")
                .asBoolean())
            .isEqualTo(true);

        ActionRequest disableAction = Requests.newActionRequest("maintenance", "disable");
        assertThat(maintenanceService
                .handleAction(new RootContext(), disableAction)
                .get()
                .getJsonContent()
                .get("maintenanceEnabled")
                .asBoolean())
            .isEqualTo(false);
        maintenanceFilter
                .filterDelete(new RootContext(), Requests.newDeleteRequest("managed/user/0"), router)
                .getOrThrow();
    }
}
