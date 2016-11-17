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

import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.Read;
import org.forgerock.api.annotations.Schema;
import org.forgerock.api.annotations.SingletonProvider;
import org.forgerock.openidm.info.health.api.OsInfoResource;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.util.promise.Promise;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;


/**
 * Gets Operating System data from the {@link java.lang.management.OperatingSystemMXBean OperatingSystemMXBean}.
 */
@SingletonProvider(@Handler(
        id = "osInfoResourceProvider:0",
        title = "Health - CPU and Operating System information",
        description = "Returns read-only data from the OperatingSystemMXBean",
        mvccSupported = false,
        resourceSchema = @Schema(fromType = OsInfoResource.class)))
public class OsInfoResourceProvider extends AbstractInfoResourceProvider {

    @Read(operationDescription = @Operation(description = "Read Operating System Information"))
    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, ReadRequest request) {
        final OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();

        final JsonValue result = json(object(
                field("availableProcessors", operatingSystemMXBean.getAvailableProcessors()),
                field("systemLoadAverage", operatingSystemMXBean.getSystemLoadAverage()),
                field("operatingSystemArchitecture", operatingSystemMXBean.getArch()),
                field("operatingSystemName", operatingSystemMXBean.getName()),
                field("operatingSystemVersion", operatingSystemMXBean.getVersion())
        ));

        return newResourceResponse("", "", result).asPromise();
    }
}
