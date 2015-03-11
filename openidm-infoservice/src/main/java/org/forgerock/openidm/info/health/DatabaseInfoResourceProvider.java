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
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.util.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Set;

/**
 * Gets BoneCP usage statistics from the {@link com.jolbox.bonecp.StatisticsMBean StatisticsMBean}.
 */
public class DatabaseInfoResourceProvider implements SingletonResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInfoResourceProvider.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionInstance(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        handler.handleError(ResourceUtil.notSupported(request));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void patchInstance(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        handler.handleError(ResourceUtil.notSupported(request));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readInstance(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {

        Boolean enabled = Boolean.parseBoolean(
                IdentityServer.getInstance().getProperty("openidm.bonecp.statistics.enabled", "false"));
        if (!enabled) {
            handler.handleError(new InternalServerErrorException("BoneCP statistics mbean not enabled."));
            return;
        }
        try {
            final ObjectName objectName = new ObjectName("com.jolbox.bonecp:type=BoneCP-*");
            final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

            Set<ObjectName> names = mBeanServer.queryNames(objectName, null);
            JsonValue results = new JsonValue(new HashMap<String, Object>());

            for (ObjectName name : names) {
                final JsonValue singleResult = json(object(
                        field("connectionWaitTimeAvg", mBeanServer.getAttribute(name, "ConnectionWaitTimeAvg")),
                        field("statementExecuteTimeAvg", mBeanServer.getAttribute(name, "StatementExecuteTimeAvg")),
                        field("statementPrepareTimeAvg", mBeanServer.getAttribute(name, "StatementPrepareTimeAvg")),
                        field("totalLeasedConnections", mBeanServer.getAttribute(name, "TotalLeased")),
                        field("totalFreeConnections", mBeanServer.getAttribute(name, "TotalFree")),
                        field("totalCreatedConnections", mBeanServer.getAttribute(name, "TotalCreatedConnections")),
                        field("cacheHits", mBeanServer.getAttribute(name, "CacheHits")),
                        field("cacheMiss", mBeanServer.getAttribute(name, "CacheMiss")),
                        field("statementsCached", mBeanServer.getAttribute(name, "StatementsCached")),
                        field("statementsPrepared", mBeanServer.getAttribute(name, "StatementsPrepared")),
                        field("connectionsRequested", mBeanServer.getAttribute(name, "ConnectionsRequested")),
                        field("cumulativeConnectionWaitTime",
                                mBeanServer.getAttribute(name, "CumulativeConnectionWaitTime")),
                        field("cumulativeStatementExecutionTime",
                                mBeanServer.getAttribute(name, "CumulativeStatementExecutionTime")),
                        field("cumulativeStatementPrepareTime",
                                mBeanServer.getAttribute(name, "CumulativeStatementPrepareTime")),
                        field("cacheHitRatio", mBeanServer.getAttribute(name, "CacheHitRatio")),
                        field("statementsExecuted", mBeanServer.getAttribute(name, "StatementsExecuted"))
                ));
                results.put(name.getCanonicalName(), singleResult.getObject());
            }
            handler.handleResult(new Resource("", "", results));
        } catch (Exception e) {
            logger.error("Unable to get BoneCP statistics mbean");
            handler.handleError(new InternalServerErrorException("Unable to get BoneCP statistics mbean", e));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateInstance(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(ResourceUtil.notSupported(request));
    }
}
