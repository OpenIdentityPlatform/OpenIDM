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
import static org.forgerock.json.resource.ResourceException.newInternalServerErrorException;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;

import org.forgerock.http.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.util.promise.Promise;
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
public class DatabaseInfoResourceProvider extends AbstractInfoResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInfoResourceProvider.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, ReadRequest request) {

        Boolean enabled = Boolean.parseBoolean(
                IdentityServer.getInstance().getProperty("openidm.bonecp.statistics.enabled", "false"));
        if (!enabled) {
            return newExceptionPromise(newInternalServerErrorException("BoneCP statistics mbean not enabled"));
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
            return newResultPromise(newResourceResponse("", "", results));
        } catch (Exception e) {
            logger.error("Unable to get BoneCP statistics mbean");
            return newExceptionPromise(newInternalServerErrorException("Unable to get BoneCP statistics mbean", e));
        }
    }
}
