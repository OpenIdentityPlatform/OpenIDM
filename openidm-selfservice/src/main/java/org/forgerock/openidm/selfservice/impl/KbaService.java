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
 * Copyright 2015 ForgeRock AS.
 * Portions Copyrighted 2024 3A Systems LLC.
 */
package org.forgerock.openidm.selfservice.impl;

import static org.forgerock.json.resource.Responses.newResourceResponse;

import org.forgerock.json.resource.AbstractRequestHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.component.propertytypes.ServiceVendor;

/**
 * This service provides a path on the router for reading the KBA configuration.
 */
@Component(
        name = KbaService.PID,
        immediate = true,
        configurationPolicy = ConfigurationPolicy.IGNORE,
        property = {
                Constants.SERVICE_PID + "=" + KbaService.PID,
                ServerConstants.ROUTER_PREFIX + "=" + KbaService.ROUTER_PATH
        },
        service = RequestHandler.class)
@ServiceVendor(ServerConstants.SERVER_VENDOR_NAME)
@ServiceDescription("OpenIDM SelfService KBA Service")
public class KbaService extends AbstractRequestHandler {

    static final String PID = "org.forgerock.openidm.selfservice.kbaservice";

    static final String ROUTER_PATH = SelfService.ROUTER_PREFIX + "/kba";

    /**
     * The KBA Configuration.
     */
    @Reference(policy = ReferencePolicy.STATIC)
    private KbaConfiguration kbaConfiguration;

    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest readRequest) {
        return newResourceResponse("kba", null, kbaConfiguration.getConfig()).asPromise();
    }
}
