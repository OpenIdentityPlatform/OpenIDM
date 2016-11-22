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

package org.forgerock.openidm.info.health;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newResourceResponse;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Set;

import org.forgerock.api.annotations.ApiError;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Read;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.annotations.SingletonProvider;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.info.health.api.BoneCPDatabaseInfoResource;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gets BoneCP usage statistics from the {@link com.jolbox.bonecp.StatisticsMBean StatisticsMBean}.
 */
@SingletonProvider(@Handler(
        id = "databaseInfoResourceProvider:0",
        title = "Health - Database connection pool statistics",
        description = "Provides database connection pool statistics if enabled. Presently only supports statistics " +
                "gathering if using BoneCP.",
        mvccSupported = false,
        resourceSchema = @Schema(fromType = BoneCPDatabaseInfoResource.class)))
public class DatabaseInfoResourceProvider extends AbstractInfoResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInfoResourceProvider.class);

    @Read(operationDescription =
    @Operation(
            description = "Read BoneCP DB connection pool statistics.",
            errors = {
                    @ApiError(
                            code=ResourceException.UNAVAILABLE,
                            description = "If BoneCP is not configured as the data source connection pool."
                    )
            }))
    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, ReadRequest request) {

        Boolean enabled = Boolean.parseBoolean(
                IdentityServer.getInstance().getProperty("openidm.bonecp.statistics.enabled", "false"));
        if (!enabled) {
            return new ServiceUnavailableException("BoneCP statistics mbean not enabled").asPromise();
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
            return newResourceResponse("", "", results).asPromise();
        } catch (Exception e) {
            logger.error("Unable to get BoneCP statistics mbean");
            return new InternalServerErrorException("Unable to get BoneCP statistics mbean", e).asPromise();
        }
    }
}
