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

package org.forgerock.openidm.cluster;

import static org.forgerock.api.commons.CommonsApi.Errors.BAD_REQUEST;

import org.forgerock.api.enums.ParameterSource;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.ApiError;
import org.forgerock.api.models.Parameter;
import org.forgerock.api.models.Paths;
import org.forgerock.api.models.Read;
import org.forgerock.api.models.Reference;
import org.forgerock.api.models.Resource;
import org.forgerock.api.models.Schema;
import org.forgerock.api.models.SubResources;
import org.forgerock.api.models.VersionedPath;
import org.forgerock.json.schema.validator.Constants;
import org.forgerock.openidm.cluster.api.ClusterListAllResponse;
import org.forgerock.openidm.cluster.api.ClusterNode;

/**
 * {@link ApiDescription} builder for {@link ClusterManager}.
 */
public class ClusterManagerApiDescription {

    private static final String TITLE = "Cluster";

    private ClusterManagerApiDescription() {
        // empty
    }

    /**
     * Builds {@link ApiDescription}.
     *
     * @return {@link ApiDescription} instance or {@code null} if a fatal error occurred and was logged
     */
    public static ApiDescription build() {
        final ApiError badRequestError = ApiError.apiError()
                .reference(Reference.reference()
                        .value(BAD_REQUEST.getReference())
                        .build())
                .build();

        final SubResources subResources = SubResources.subresources()
                .put("/{instanceId}", Resource.resource()
                        .title(TITLE)
                        .description("Manages a single cluster node.")
                        .mvccSupported(true)
                        .resourceSchema(Schema.schema()
                                .type(ClusterNode.class)
                                .build())
                        .parameter(Parameter.parameter()
                                .name("instanceId")
                                .type(Constants.TYPE_STRING)
                                .required(true)
                                .source(ParameterSource.PATH)
                                .description("Instance ID")
                                .build())
                        .read(Read.read()
                                .description("Reads cluster node status.")
                                .error(badRequestError)
                                .build())
                        .build())
                .build();

        return ApiDescription.apiDescription()
                .id("temp")
                .version("0")
                .paths(Paths.paths()
                        .put("/", VersionedPath.versionedPath()
                                .put(VersionedPath.UNVERSIONED, Resource.resource()
                                        .title(TITLE)
                                        .description("Manages the OpenIDM cluster.")
                                        .mvccSupported(false)
                                        .resourceSchema(Schema.schema()
                                                .type(ClusterListAllResponse.class)
                                                .build())
                                        .read(Read.read()
                                                .description("Lists the status of all cluster nodes.")
                                                .build())
                                        .subresources(subResources)
                                        .build())
                                .build())
                        .build())
                .build();
    }
}