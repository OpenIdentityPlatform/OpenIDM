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
 * Copyright 2016-2017 ForgeRock AS.
 */

package org.forgerock.openidm.provisioner.openicf.impl;

import static java.util.Arrays.asList;
import static org.forgerock.api.commons.CommonsApi.COMMONS_API_DESCRIPTION;
import static org.forgerock.api.commons.CommonsApi.Errors.*;
import static org.forgerock.api.enums.CreateMode.ID_FROM_SERVER;
import static org.forgerock.api.enums.PagingMode.COOKIE;
import static org.forgerock.api.enums.ParameterSource.ADDITIONAL;
import static org.forgerock.api.enums.ParameterSource.PATH;
import static org.forgerock.api.enums.PatchOperation.*;
import static org.forgerock.api.models.VersionedPath.UNVERSIONED;
import static org.forgerock.json.schema.validator.Constants.TYPE_BOOLEAN;
import static org.forgerock.json.schema.validator.Constants.TYPE_STRING;
import static org.forgerock.openidm.core.ServerConstants.QUERY_ALL_IDS;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.forgerock.api.enums.QueryType;
import org.forgerock.api.models.Action;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.ApiError;
import org.forgerock.api.models.Create;
import org.forgerock.api.models.Delete;
import org.forgerock.api.models.Items;
import org.forgerock.api.models.Parameter;
import org.forgerock.api.models.Patch;
import org.forgerock.api.models.Paths;
import org.forgerock.api.models.Query;
import org.forgerock.api.models.Read;
import org.forgerock.api.models.Reference;
import org.forgerock.api.models.Resource;
import org.forgerock.api.models.Schema;
import org.forgerock.api.models.Update;
import org.forgerock.api.models.VersionedPath;
import org.forgerock.api.transform.OpenApiTransformer;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.openidm.provisioner.openicf.impl.ObjectClassResourceProvider.ObjectClassAction;
import org.forgerock.openidm.provisioner.openicf.impl.OpenICFProvisionerService.ConnectorAction;
import org.forgerock.openidm.provisioner.openicf.impl.api.AuthenticateActionResponse;
import org.forgerock.openidm.provisioner.impl.api.LiveSyncActionResponse;
import org.forgerock.openidm.provisioner.openicf.impl.api.ScriptActionResponse;
import org.forgerock.openidm.provisioner.impl.api.TestActionResponse;
import org.forgerock.openidm.provisioner.openicf.internal.SystemAction;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ApiDescription} builder for {@link ObjectClassRequestHandler}.
 */
class ObjectClassRequestHandlerApiDescription {

    private final static Logger logger = LoggerFactory.getLogger(ObjectClassRequestHandlerApiDescription.class);

    private static final Parameter detailedFailureParameter = Parameter.parameter()
            .name("detailedFailure")
            .description("Return detailed failure information")
            .source(ADDITIONAL)
            .type(TYPE_BOOLEAN)
            .required(false)
            .build();

    private static final ApiError badRequestError =
            ApiError.apiError().reference(Reference.reference().value(BAD_REQUEST.getReference()).build()).build();
    private static final ApiError unauthorizedError =
            ApiError.apiError().reference(Reference.reference().value(UNAUTHORIZED.getReference()).build()).build();
    private static final ApiError notFoundError =
            ApiError.apiError().reference(Reference.reference().value(NOT_FOUND.getReference()).build()).build();
    private static final ApiError notSupportedError =
            ApiError.apiError().reference(Reference.reference().value(NOT_SUPPORTED.getReference()).build()).build();

    private ObjectClassRequestHandlerApiDescription() {
        // empty
    }

    /**
     * Builds {@link ApiDescription}.
     *
     * @param objectClassHandlers Registered object-class (key) to {@link ObjectClassResourceProvider} (value) pairs
     * @return {@link ApiDescription} instance or {@code null} if a fatal error occurred and was logged
     */
    public static ApiDescription build(final Map<String, RequestHandler> objectClassHandlers) {
        if (objectClassHandlers.isEmpty()) {
            logger.info("Failed to generate API Description for ICF provider, because there are no object-classes");
            return null;
        }
        try {
            final Paths paths = buildPaths(objectClassHandlers);
            final ApiDescription apiDescription = ApiDescription.apiDescription()
                    .id("temp")
                    .version("0")
                    .paths(paths)
                    .build();

            // do a dry-run of generating the Swagger model, because the managed.json content is unpredictable
            OpenApiTransformer.execute(apiDescription, COMMONS_API_DESCRIPTION);
            return apiDescription;
        } catch (Exception e) {
            String message = "Failed to generate API Description for ICF provider";
            final ObjectClassResourceProvider provider =
                    (ObjectClassResourceProvider) objectClassHandlers.get(ObjectClass.ALL_NAME);
            if (provider != null) {
                message += ": " + provider.getConfig().get("name").asString();
            }
            logger.info(message, e);
            return null;
        }
    }

    private static Paths buildPaths(final Map<String, RequestHandler> objectClassHandlers) {
        final Paths.Builder pathsBuilder = Paths.paths();

        // the JSON configuration will be the same for all handlers in the map
        final JsonValue configuration =
                ((ObjectClassResourceProvider) objectClassHandlers.values().iterator().next()).getConfig();
        final String connectorName = configuration.get("name").asString();
        final String title = "Connector - " + connectorName;
        final String description = "ICF " + connectorName + " connector.";

        pathsBuilder.put("/", buildRootPath(title, description, getScriptIds(configuration)));

        // create sub-resource per key in objectClassHandlers
        for (final String name : objectClassHandlers.keySet()) {
            if (ObjectClassUtil.isSpecialName(name)) {
                continue;
            }
            final ObjectClassResourceProvider provider = (ObjectClassResourceProvider) objectClassHandlers.get(name);
            final JsonValue resourceSchema = provider.getConfig().get(new JsonPointer("/objectTypes/" + name)).clone();
            if (resourceSchema.get("title").isNull()) {
                resourceSchema.put("title", "Connector Object");
            }
            pathsBuilder.put("/" + name, buildObjectClassPath(name, title, description, resourceSchema));
        }
        return pathsBuilder.build();
    }

    private static VersionedPath buildRootPath(final String title, final String description, final String[] scriptIds) {
        final Resource.Builder resourceBuilder = Resource.resource()
                .mvccSupported(false)
                .title(title)
                .description(description)
                .action(Action.action()
                        .name(ConnectorAction.livesync.name())
                        .description("Trigger Live-Sync on all object-classes.")
                        .parameter(detailedFailureParameter)
                        .response(Schema.schema()
                                .type(LiveSyncActionResponse.class)
                                .build())
                        .build())
                .action(Action.action()
                        .name(ConnectorAction.test.name())
                        .description("Check the connector's status.")
                        .response(Schema.schema()
                                .type(TestActionResponse.class)
                                .build())
                        .build());

        if (scriptIds.length != 0) {
            resourceBuilder.action(Action.action()
                    .name(ConnectorAction.script.name())
                    .description("Execute a script defined within connector configuration.")
                    .parameter(Parameter.parameter()
                            .name(SystemAction.SCRIPT_ID)
                            .description("Script ID")
                            .source(ADDITIONAL)
                            .type(TYPE_STRING)
                            .required(true)
                            .enumValues(scriptIds)
                            .build())
                    .parameter(Parameter.parameter()
                            .name(SystemAction.SCRIPT_EXECUTE_MODE)
                            .description("Run script against connector or a resource")
                            .source(ADDITIONAL)
                            .type(TYPE_STRING)
                            .required(true)
                            .enumValues("connector", "resource")
                            .enumTitles("Run script on connector", "Run script on resource")
                            .defaultValue("connector")
                            .build())
                    .parameter(Parameter.parameter()
                            .name(SystemAction.SCRIPT_VARIABLE_PREFIX)
                            .description("Variable-prefix")
                            .source(ADDITIONAL)
                            .type(TYPE_STRING)
                            .required(false)
                            .build())
                    .parameter(Parameter.parameter()
                            .name("username")
                            .description("Username")
                            .source(ADDITIONAL)
                            .type(TYPE_STRING)
                            .required(false)
                            .build())
                    .parameter(Parameter.parameter()
                            .name("password")
                            .description("Password")
                            .source(ADDITIONAL)
                            .type(TYPE_STRING)
                            .required(false)
                            .build())
                    .parameter(Parameter.parameter()
                            .name("workingdir")
                            .description("Working directory")
                            .source(ADDITIONAL)
                            .type(TYPE_STRING)
                            .required(false)
                            .build())
                    .parameter(Parameter.parameter()
                            .name("timeout")
                            .description("Timeout")
                            .source(ADDITIONAL)
                            .type(TYPE_STRING)
                            .required(false)
                            .build())
                    .response(Schema.schema()
                            .type(ScriptActionResponse.class)
                            .build())
                    .errors(asList(badRequestError, notSupportedError))
                    .build());
        }
        return VersionedPath.versionedPath()
                .put(UNVERSIONED, resourceBuilder.build())
                .build();
    }

    private static VersionedPath buildObjectClassPath(final String name, final String title, final String description,
            final JsonValue resourceSchema) {
        return VersionedPath.versionedPath()
                .put(UNVERSIONED, Resource.resource()
                        .mvccSupported(false)
                        .title(title)
                        .description(description)
                        .resourceSchema(Schema.schema().schema(resourceSchema).build())
                        .create(Create.create()
                                .description("Create an object on " + name + " object-class, with server-assigned ID.")
                                .mode(ID_FROM_SERVER)
                                .build())
                        .action(Action.action()
                                .name(ObjectClassAction.liveSync.name())
                                .description("Trigger Live-Sync on " + name + " object-class.")
                                .parameter(detailedFailureParameter)
                                .response(Schema.schema()
                                        .type(LiveSyncActionResponse.class)
                                        .build())
                                .build())
                        .action(Action.action()
                                .name(ObjectClassAction.authenticate.name())
                                .description("Authenticate a user on " + name + " object-class.")
                                .parameter(Parameter.parameter()
                                        .name("username")
                                        .description("Username")
                                        .source(ADDITIONAL)
                                        .type(TYPE_STRING)
                                        .required(true)
                                        .build())
                                .parameter(Parameter.parameter()
                                        .name("password")
                                        .description("Password")
                                        .source(ADDITIONAL)
                                        .type(TYPE_STRING)
                                        .required(true)
                                        .build())
                                .response(Schema.schema()
                                        .type(AuthenticateActionResponse.class)
                                        .build())
                                .error(unauthorizedError)
                                .build())
                        .query(Query.query()
                                .description("List all object IDs within " + name + " object-class.")
                                .type(QueryType.ID)
                                .queryId(QUERY_ALL_IDS)
                                .pagingModes(COOKIE)
                                .build())
                        .query(Query.query()
                                .description("Query by expression within " + name + " object-class. "
                                        + "This will only work for connectors that support expression-queries.")
                                .type(QueryType.EXPRESSION)
                                .pagingModes(COOKIE)
                                .build())
                        .query(Query.query()
                                .description("Query by filter within " + name + " object-class.")
                                .type(QueryType.FILTER)
                                .queryableFields("*")
                                .pagingModes(COOKIE)
                                .build())
                        .items(Items.items()
                                .pathParameter(Parameter.parameter()
                                        .source(PATH)
                                        .type(TYPE_STRING)
                                        .required(true)
                                        .name("id")
                                        .description("Object ID")
                                        .build())
                                .read(Read.read()
                                        .description("Read an object.")
                                        .error(notFoundError)
                                        .build())
                                .update(Update.update()
                                        .description("Update an object.")
                                        .error(notFoundError)
                                        .build())
                                .delete(Delete.delete()
                                        .description("Delete an object.")
                                        .error(notFoundError)
                                        .build())
                                .patch(Patch.patch()
                                        .description("Patch an object, within " + name + " object-class, "
                                                + "using the following operations:\n\n"
                                                + "- ADD\n"
                                                + "- REMOVE\n"
                                                + "- REPLACE\n")
                                        .operations(ADD, REMOVE, REPLACE)
                                        .error(notFoundError)
                                        .build())
                                .action(Action.action()
                                        .name(ObjectClassAction.authenticate.name())
                                        .description("Authenticate a user on an object within " + name
                                                + " object-class.")
                                        .parameter(Parameter.parameter()
                                                .name("username")
                                                .description("Username")
                                                .source(ADDITIONAL)
                                                .type(TYPE_STRING)
                                                .required(true)
                                                .build())
                                        .parameter(Parameter.parameter()
                                                .name("password")
                                                .description("Password")
                                                .source(ADDITIONAL)
                                                .type(TYPE_STRING)
                                                .required(true)
                                                .build())
                                        .response(Schema.schema()
                                                .type(AuthenticateActionResponse.class)
                                                .build())
                                        .error(unauthorizedError)
                                        .build())
                                .action(Action.action()
                                        .name(ObjectClassAction.liveSync.name())
                                        .description("Trigger Live-Sync on an object within " + name
                                                + " object-class.")
                                        .parameter(detailedFailureParameter)
                                        .response(Schema.schema()
                                                .type(LiveSyncActionResponse.class)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    /**
     * Checks if executable scripts are defined in the configuration, and lists their script ID values.
     *
     * @param config Connector configuration
     * @return Array of script IDs or empty-array
     */
    private static String[] getScriptIds(final JsonValue config) {
        final List<String> scriptIds = new ArrayList<>();
        if (config.isDefined("systemActions")) {
            for (final JsonValue value : config.get("systemActions")) {
                final String scriptId = value.get("scriptId").asString();
                if (scriptId != null && !scriptId.isEmpty()) {
                    scriptIds.add(scriptId);
                }
            }
        }
        return scriptIds.toArray(new String[scriptIds.size()]);
    }

}
