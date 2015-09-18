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
 */

package org.forgerock.openidm.info.health;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newResourceResponse;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.services.context.Context;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * Gets Recon Health Info.
 */
public class ReconInfoResourceProvider extends AbstractInfoResourceProvider {

    final static Logger logger = LoggerFactory.getLogger(ReconInfoResourceProvider.class);

    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, ReadRequest request) {
        try {
            final ObjectName objectName = new ObjectName("org.forgerock.openidm.recon:type=Reconciliation");
            final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

            final JsonValue result = json(object(
                    field("activeThreads", mBeanServer.getAttribute(objectName, "ActiveThreads")),
                    field("corePoolSize", mBeanServer.getAttribute(objectName, "CorePoolSize")),
                    field("largestPoolSize", mBeanServer.getAttribute(objectName, "LargestPoolSize")),
                    field("maximumPoolSize", mBeanServer.getAttribute(objectName, "MaximumPoolSize")),
                    field("currentPoolSize", mBeanServer.getAttribute(objectName, "PoolSize"))
            ));
            return newResourceResponse("", "", result).asPromise();
        } catch (Exception e) {
            logger.error("Unable to get reconciliation mbean");
            return new InternalServerErrorException("Unable to get reconciliation mbean", e).asPromise();
        }
    }
}
