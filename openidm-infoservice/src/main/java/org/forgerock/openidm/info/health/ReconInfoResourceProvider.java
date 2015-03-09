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

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.util.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * Gets Recon Health Info.
 */
public class ReconInfoResourceProvider implements SingletonResourceProvider {

    final static Logger logger = LoggerFactory.getLogger(ReconInfoResourceProvider.class);

    @Override
    public void actionInstance(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        handler.handleError(ResourceUtil.notSupported(request));
    }

    @Override
    public void patchInstance(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        handler.handleError(ResourceUtil.notSupported(request));
    }

    @Override
    public void readInstance(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
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
            handler.handleResult(new Resource("", "", result));
        } catch (Exception e) {
            logger.error("Unable to get reconciliation mbean");
            handler.handleError(new InternalServerErrorException("Unable to get reconciliation mbean", e));
        }
    }

    @Override
    public void updateInstance(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(ResourceUtil.notSupported(request));
    }
}
