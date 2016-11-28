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

package org.forgerock.openidm.sync.impl;

import static org.forgerock.api.commons.CommonsApi.Errors.NOT_FOUND;

import org.forgerock.api.enums.ParameterSource;
import org.forgerock.api.models.Action;
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
import org.forgerock.openidm.sync.impl.ReconciliationService.ReconAction;
import org.forgerock.openidm.sync.impl.api.ListReconsResponse;
import org.forgerock.openidm.sync.impl.api.ReconCancelResponse;
import org.forgerock.openidm.sync.impl.api.ReconStateResponse;
import org.forgerock.openidm.sync.impl.api.ReconciliationServiceResource;

/**
 * {@link ApiDescription} builder for {@link ReconciliationService}.
 */
public class ReconciliationServiceApiDescription {

    private ReconciliationServiceApiDescription() {
        // empty
    }

    /**
     * Builds {@link ApiDescription}.
     *
     * @return {@link ApiDescription} instance or {@code null} if a fatal error occurred and was logged
     */
    public static ApiDescription build() {
        final ApiError notFoundError = ApiError.apiError()
                .reference(Reference.reference()
                        .value(NOT_FOUND.getReference())
                        .build())
                .build();

        final Parameter mappingParameter = Parameter.parameter()
                .name("mapping")
                .description("Mapping name (e.g., systemXmlfileAccounts_managedUser)")
                .type(Constants.TYPE_STRING)
                .source(ParameterSource.ADDITIONAL)
                .required(true)
                .build();
        final Parameter waitForCompletionParameter = Parameter.parameter()
                .name("waitForCompletion")
                .description("When true, request will block until reconcilliation completes, and will respond"
                        + " immediately when false.")
                .type(Constants.TYPE_BOOLEAN)
                .source(ParameterSource.ADDITIONAL)
                .defaultValue("false")
                .build();

        final SubResources subResources = SubResources.subresources()
                .put("/{reconId}", Resource.resource()
                        .title("Reconcilliation - Instance")
                        .description("Manages individual reconcilliation instances.")
                        .mvccSupported(true)
                        .resourceSchema(Schema.schema()
                                .type(ReconciliationServiceResource.class)
                                .build())
                        .parameter(Parameter.parameter()
                                .name("reconId")
                                .type(Constants.TYPE_STRING)
                                .required(true)
                                .source(ParameterSource.PATH)
                                .description("Recon ID")
                                .build())
                        .read(Read.read()
                                .description("Read an individual reconcilliation summary.")
                                .error(notFoundError)
                                .build())
                        .action(Action.action()
                                .name("cancel")
                                .description("Cancels a running reconcilliation")
                                .response(Schema.schema()
                                        .type(ReconCancelResponse.class)
                                        .build())
                                .error(notFoundError)
                                .build())
                        .build())
                .build();

        return ApiDescription.apiDescription()
                .id("temp")
                .version("0")
                .paths(Paths.paths()
                        .put("/", VersionedPath.versionedPath()
                                .put(VersionedPath.UNVERSIONED, Resource.resource()
                                        .title("Reconcilliation")
                                        .description("Utilities for managing reconcilliation.")
                                        .mvccSupported(false)
                                        .resourceSchema(Schema.schema()
                                                .type(ListReconsResponse.class)
                                                .build())
                                        .read(Read.read()
                                                .description("Lists all reconcilliation summaries.")
                                                .build())
                                        .action(Action.action()
                                                .name(ReconAction.recon.name())
                                                .description("Recon all available source identifiers.")
                                                .parameter(mappingParameter)
                                                .parameter(waitForCompletionParameter)
                                                .response(Schema.schema()
                                                        .type(ReconStateResponse.class)
                                                        .build())
                                                .build())
                                        .action(Action.action()
                                                .name(ReconAction.reconById.name())
                                                .description("Recon for a single source identifier.")
                                                .parameter(mappingParameter)
                                                .parameter(waitForCompletionParameter)
                                                .parameter(Parameter.parameter()
                                                        .name("ids")
                                                        .required(true)
                                                        .type(Constants.TYPE_STRING)
                                                        .source(ParameterSource.ADDITIONAL)
                                                        .description("Source identifier")
                                                        .build())
                                                .response(Schema.schema()
                                                        .type(ReconStateResponse.class)
                                                        .build())
                                                .build())
                                        .subresources(subResources)
                                        .build())
                                .build())
                        .build())
                .build();
    }

}
