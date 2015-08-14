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
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.forgerock.json.resource.Responses.newResourceResponse;

import org.forgerock.http.Context;
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
public class OsInfoResourceProvider extends AbstractInfoResourceProvider {
    /**
     * {@inheritDoc}
     */
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

        return newResultPromise(newResourceResponse("", "", result));
    }
}
