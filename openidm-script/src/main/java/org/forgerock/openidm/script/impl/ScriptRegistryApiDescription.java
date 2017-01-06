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
 * Copyright 2017 ForgeRock AS.
 */

package org.forgerock.openidm.script.impl;

import static org.forgerock.api.commons.CommonsApi.Errors.*;
import static org.forgerock.openidm.util.JsonUtil.parseURL;

import java.util.Arrays;
import java.util.List;

import org.forgerock.api.models.Action;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.ApiError;
import org.forgerock.api.models.Paths;
import org.forgerock.api.models.Reference;
import org.forgerock.api.models.Resource;
import org.forgerock.api.models.Schema;
import org.forgerock.api.models.VersionedPath;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.script.impl.api.ScriptActionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ApiDescription} builder for {@link ScriptRegistryService}.
 */
class ScriptRegistryApiDescription {

    private final static Logger logger = LoggerFactory.getLogger(ScriptRegistryApiDescription.class);

    private ScriptRegistryApiDescription() {
        // empty
    }

    /**
     * Builds {@link ApiDescription}.
     *
     * @return {@link ApiDescription} instance or {@code null} if a fatal error occurred and was logged
     */
    public static ApiDescription build() {
        try {
            final JsonValue compileActionResponseSchema =
                    parseURL(ScriptRegistryApiDescription.class.getResource("api/compileActionResponseSchema.json"));
            final JsonValue evalActionResponseSchema =
                    parseURL(ScriptRegistryApiDescription.class.getResource("api/evalActionResponseSchema.json"));
            final List<ApiError> errors = Arrays.asList(
                    ApiError.apiError()
                            .reference(Reference.reference().value(BAD_REQUEST.getReference()).build())
                            .build(),
                    ApiError.apiError()
                            .reference(Reference.reference().value(INTERNAL_SERVER_ERROR.getReference()).build())
                            .build(),
                    ApiError.apiError()
                            .reference(Reference.reference().value(UNAVAILABLE.getReference()).build())
                            .build(),
                    ApiError.apiError()
                            .reference(Reference.reference().value(NOT_SUPPORTED.getReference()).build())
                            .build());

            return ApiDescription.apiDescription()
                    .id("temp")
                    .version("0")
                    .paths(Paths.paths()
                            .put("/", VersionedPath.versionedPath()
                                    .put(VersionedPath.UNVERSIONED, Resource.resource()
                                            .title("Script")
                                            .description("Service that can compile and/or execute scripts located in "
                                                    + "the `/script` directory.")
                                            .mvccSupported(false)
                                            .action(Action.action()
                                                    .name("compile")
                                                    .description("Compiles a script, to validate that it can be"
                                                            + " executed.")
                                                    .errors(errors)
                                                    .request(Schema.schema()
                                                            .type(ScriptActionRequest.class)
                                                            .build())
                                                    .response(Schema.schema()
                                                            .schema(compileActionResponseSchema)
                                                            .build())
                                                    .build())
                                            .action(Action.action()
                                                    .name("eval")
                                                    .description("Executes a script and returns the result, if any.")
                                                    .errors(errors)
                                                    .request(Schema.schema()
                                                            .type(ScriptActionRequest.class)
                                                            .build())
                                                    .response(Schema.schema()
                                                            .schema(evalActionResponseSchema)
                                                            .build())
                                                    .build())
                                            .build())
                                    .build())
                            .build())
                    .build();
        } catch (Exception e) {
            logger.info("Failed to generate API Description");
            return null;
        }
    }

}
