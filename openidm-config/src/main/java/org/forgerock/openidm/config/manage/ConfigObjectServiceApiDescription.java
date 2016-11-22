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

package org.forgerock.openidm.config.manage;

import static org.forgerock.api.commons.CommonsApi.Errors.BAD_REQUEST;
import static org.forgerock.api.commons.CommonsApi.Errors.NOT_FOUND;
import static org.forgerock.api.enums.PatchOperation.*;

import org.forgerock.api.enums.CreateMode;
import org.forgerock.api.enums.ParameterSource;
import org.forgerock.api.enums.QueryType;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.ApiError;
import org.forgerock.api.models.Create;
import org.forgerock.api.models.Delete;
import org.forgerock.api.models.Parameter;
import org.forgerock.api.models.Patch;
import org.forgerock.api.models.Paths;
import org.forgerock.api.models.Query;
import org.forgerock.api.models.Read;
import org.forgerock.api.models.Reference;
import org.forgerock.api.models.Resource;
import org.forgerock.api.models.Schema;
import org.forgerock.api.models.SubResources;
import org.forgerock.api.models.Update;
import org.forgerock.api.models.VersionedPath;
import org.forgerock.json.schema.validator.Constants;
import org.forgerock.openidm.config.manage.api.ConfigListAllResponse;
import org.forgerock.openidm.config.manage.api.ConfigResource;

/**
 * {@link ApiDescription} builder for {@link ConfigObjectService}.
 */
public class ConfigObjectServiceApiDescription {

    private ConfigObjectServiceApiDescription() {
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
        final ApiError notFoundError = ApiError.apiError()
                .reference(Reference.reference()
                        .value(NOT_FOUND.getReference())
                        .build())
                .build();

        final SubResources subResources = SubResources.subresources()
                .put("/", Resource.resource()
                        .title("Configuration - Query")
                        .description("Queries configuration endpoints, which map to JSON files.")
                        .mvccSupported(false)
                        .resourceSchema(Schema.schema()
                                .type(ConfigResource.class)
                                .build())
                        .query(Query.query()
                                .type(QueryType.FILTER)
                                .description("Queries the configurations.")
                                .error(badRequestError)
                                .build())
                        .build())
                .put("/{instanceId}", Resource.resource()
                        .title("Configuration - Single-Instance")
                        .description("Manages single-instance configuration endpoints, which map to JSON files.")
                        .mvccSupported(true)
                        .resourceSchema(Schema.schema()
                                .type(ConfigResource.class)
                                .build())
                        .parameter(Parameter.parameter()
                                .name("instanceId")
                                .type(Constants.TYPE_STRING)
                                .required(true)
                                .source(ParameterSource.PATH)
                                .description("Instance ID")
                                .build())
                        .create(Create.create()
                                .mode(CreateMode.ID_FROM_CLIENT)
                                .description("Create a configuration.")
                                .error(notFoundError)
                                .build())
                        .read(Read.read()
                                .description("Read a configuration.")
                                .error(notFoundError)
                                .build())
                        .update(Update.update()
                                .description("Update a configuration.")
                                .error(notFoundError)
                                .build())
                        .delete(Delete.delete()
                                .description("Delete a configuration.")
                                .error(notFoundError)
                                .build())
                        .patch(Patch.patch()
                                .operations(ADD, REMOVE, REPLACE, INCREMENT, COPY, MOVE, TRANSFORM)
                                .description("Patch a configuration.")
                                .error(notFoundError)
                                .error(badRequestError)
                                .build())
                        .build())
                .put("/{factoryPid}/{instanceId}", Resource.resource()
                        .title("Configuration - Multi-Instance")
                        .description("Manages multi-instance configuration endpoints, which map to JSON files.")
                        .mvccSupported(true)
                        .resourceSchema(Schema.schema()
                                .type(ConfigResource.class)
                                .build())
                        .parameter(Parameter.parameter()
                                .name("factoryPid")
                                .type(Constants.TYPE_STRING)
                                .required(true)
                                .source(ParameterSource.PATH)
                                .description("Persistent identity for group (factory)")
                                .build())
                        .parameter(Parameter.parameter()
                                .name("instanceId")
                                .type(Constants.TYPE_STRING)
                                .required(true)
                                .source(ParameterSource.PATH)
                                .description("Instance ID under parent group")
                                .build())
                        .create(Create.create()
                                .mode(CreateMode.ID_FROM_CLIENT)
                                .description("Create a configuration.")
                                .error(notFoundError)
                                .build())
                        .read(Read.read()
                                .description("Read a configuration.")
                                .error(notFoundError)
                                .build())
                        .update(Update.update()
                                .description("Update a configuration.")
                                .error(notFoundError)
                                .build())
                        .delete(Delete.delete()
                                .description("Delete a configuration.")
                                .error(notFoundError)
                                .build())
                        .patch(Patch.patch()
                                .operations(ADD, REMOVE, REPLACE, INCREMENT, COPY, MOVE, TRANSFORM)
                                .description("Patch a configuration.")
                                .error(notFoundError)
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
                                        .title("Configuration")
                                        .description("Lists configuration endpoints, which map to JSON files.")
                                        .mvccSupported(false)
                                        .resourceSchema(Schema.schema()
                                                .type(ConfigListAllResponse.class)
                                                .build())
                                        .read(Read.read()
                                                .description("Lists all configuration endpoints."
                                                        + " Some configurations are single-instance (e.g., audit)"
                                                        + " while others are multi-instance (grouped), which can be"
                                                        + " observed when the `_id` field contains a forward-slash"
                                                        + " (e.g., factoryId/instanceId).")
                                                .build())
                                        .subresources(subResources)
                                        .build())
                                .build())
                        .build())
                .build();
    }

}
