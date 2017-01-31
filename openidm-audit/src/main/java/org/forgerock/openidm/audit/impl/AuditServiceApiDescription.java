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

package org.forgerock.openidm.audit.impl;

import static org.forgerock.api.commons.CommonsApi.COMMONS_API_DESCRIPTION;
import static org.forgerock.api.enums.CreateMode.ID_FROM_CLIENT;
import static org.forgerock.api.enums.CreateMode.ID_FROM_SERVER;
import static org.forgerock.api.models.VersionedPath.UNVERSIONED;
import static org.forgerock.guava.common.base.Strings.isNullOrEmpty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.forgerock.api.enums.CountPolicy;
import org.forgerock.api.enums.PagingMode;
import org.forgerock.api.enums.ParameterSource;
import org.forgerock.api.enums.QueryType;
import org.forgerock.api.models.Action;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.Create;
import org.forgerock.api.models.Items;
import org.forgerock.api.models.Parameter;
import org.forgerock.api.models.Paths;
import org.forgerock.api.models.Query;
import org.forgerock.api.models.Read;
import org.forgerock.api.models.Resource;
import org.forgerock.api.models.Schema;
import org.forgerock.api.models.VersionedPath;
import org.forgerock.api.transform.OpenApiTransformer;
import org.forgerock.audit.events.EventTopicsMetaData;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.openidm.audit.AuditService.AuditAction;
import org.forgerock.openidm.audit.impl.api.AvailableHandler;
import org.forgerock.openidm.audit.impl.api.ChangedActionRequest;
import org.forgerock.openidm.audit.impl.api.FlushActionResponse;
import org.forgerock.openidm.audit.impl.api.RotateActionResponse;
import org.forgerock.openidm.repo.QueryConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ApiDescription} builder for {@link org.forgerock.openidm.audit.AuditService}.
 */
public class AuditServiceApiDescription {

    private static final Logger logger = LoggerFactory.getLogger(AuditServiceApiDescription.class);

    private static final String TITLE = "Audit";
    private static final String CSV_HANDLER_NAME = "csv";
    private static final String JSON_HANDLER_NAME = "json";
    private static final JsonPointer HANDLER_NAME_POINTER = new JsonPointer("/config/name");

    private AuditServiceApiDescription() {
        // empty
    }

    /**
     * Builds {@link ApiDescription}.
     *
     * @param eventTopicsMetaData Topic metadata
     * @param eventHandlers Configured event handlers
     * @return {@link ApiDescription} instance or {@code null} if a fatal error occurred and was logged
     */
    public static ApiDescription build(final EventTopicsMetaData eventTopicsMetaData, final JsonValue eventHandlers) {
        try {
            final Paths paths = buildPaths(eventTopicsMetaData, eventHandlerNames(eventHandlers));
            final ApiDescription apiDescription = ApiDescription.apiDescription()
                    .id("temp")
                    .version("0")
                    .paths(paths)
                    .build();

            // do a dry-run of generating the Swagger model
            OpenApiTransformer.execute(apiDescription, COMMONS_API_DESCRIPTION);
            return apiDescription;
        } catch (final Exception e) {
            logger.info("Failed to generate API Description for Audit Service", e);
            return null;
        }
    }

    private static Paths buildPaths(final EventTopicsMetaData eventTopicsMetaData,
            final Set<String> eventHandlerNames) {
        final Paths.Builder pathsBuilder = Paths.paths();

        // these actions are OpenIDM specific
        pathsBuilder.put("/", VersionedPath.versionedPath()
                .put(UNVERSIONED, Resource.resource()
                        .mvccSupported(false)
                        .title(TITLE)
                        .description("Audit event handler actions.")
                        .action(Action.action()
                                .name(AuditAction.availableHandlers.name())
                                .description("List available audit event handlers.")
                                .response(Schema.schema()
                                        .type(AvailableHandler[].class)
                                        .build())
                                .build())
                        .action(Action.action()
                                .name(AuditAction.getChangedWatchedFields.name())
                                .description("Identify watched-fields that have changed. "
                                        + "The response contains an array of JSON Pointers.")
                                .request(Schema.schema()
                                        .type(ChangedActionRequest.class)
                                        .build())
                                .response(Schema.schema()
                                        .type(String[].class)
                                        .build())
                                .build())
                        .action(Action.action()
                                .name(AuditAction.getChangedPasswordFields.name())
                                .description("Identify password-fields that have changed. "
                                        + "The response contains an array of JSON Pointers.")
                                .request(Schema.schema()
                                        .type(ChangedActionRequest.class)
                                        .build())
                                .response(Schema.schema()
                                        .type(String[].class)
                                        .build())
                                .build())
                        .build())
                .build());

        for (final String topic : eventTopicsMetaData.getTopics()) {
            final JsonValue topicSchema = eventTopicsMetaData.getSchema(topic).get("schema").clone();
            if (topicSchema.get("id").isNull() || "/".equals(topicSchema.get("id").asString())) {
                topicSchema.put("id", "org.forgerock.audit.events." + topic);
            }
            if (topicSchema.get("title").isNull()) {
                topicSchema.put("title", topic);
            }
            pathsBuilder.put('/' + topic, buildHandlerTopicPath(topic, topicSchema, eventHandlerNames));
        }
        return pathsBuilder.build();
    }

    private static VersionedPath buildHandlerTopicPath(final String topic, final JsonValue topicSchema,
            final Set<String> eventHandlerNames) {
        final String description = "Audit event handler endpoints for the " + topic + " topic.";
        final Resource.Builder resourceBuilder = Resource.resource()
                .mvccSupported(false)
                .title(TITLE)
                .description(description)
                .resourceSchema(Schema.schema()
                        .schema(topicSchema)
                        .build())
                .create(Create.create()
                        .description("Publish an audit event with the " + topic + " topic.")
                        .mode(ID_FROM_SERVER)
                        .build())
                .query(Query.query()
                        .description("Query for audit events with the " + topic + " topic by query-filter.")
                        .pagingModes(PagingMode.OFFSET, PagingMode.COOKIE)
                        .type(QueryType.FILTER)
                        .countPolicies(CountPolicy.EXACT)
                        .build())
                .query(Query.query()
                        .description("Query for all audit event identifiers with the " + topic + " topic.")
                        .pagingModes(PagingMode.OFFSET, PagingMode.COOKIE)
                        .type(QueryType.ID)
                        .queryId(QueryConstants.QUERY_ALL_IDS)
                        .countPolicies(CountPolicy.EXACT)
                        .build())
                .query(Query.query()
                        .description("Query for all audit events with the " + topic + " topic.")
                        .pagingModes(PagingMode.OFFSET, PagingMode.COOKIE)
                        .type(QueryType.ID)
                        .queryId(QueryConstants.QUERY_ALL)
                        .countPolicies(CountPolicy.EXACT)
                        .build())
                .items(Items.items()
                        .pathParameter(Parameter.parameter()
                                .name("eventId")
                                .type("string")
                                .source(ParameterSource.PATH)
                                .description("Event ID")
                                .required(true)
                                .build())
                        .create(Create.create()
                                .description("Publish an audit event with the " + topic + " topic and "
                                        + "client provided identifier.")
                                .mode(ID_FROM_CLIENT)
                                .build())
                        .read(Read.read()
                                .description("Read an audit event with the " + topic + " topic.")
                                .build())
                        .build());

        // only a couple handlers implement actions
        applyActions(resourceBuilder, eventHandlerNames);

        return VersionedPath.versionedPath()
                .put(UNVERSIONED, resourceBuilder.build())
                .build();
    }

    private static void applyActions(final Resource.Builder resourceBuilder, final Set<String> eventHandlerNames) {
        final List<String> rotateHandlerNames = new ArrayList<>();
        final List<String> flushHandlerNames = new ArrayList<>();
        if (eventHandlerNames.contains(CSV_HANDLER_NAME)) {
            rotateHandlerNames.add(CSV_HANDLER_NAME);
        }
        if (eventHandlerNames.contains(JSON_HANDLER_NAME)) {
            rotateHandlerNames.add(JSON_HANDLER_NAME);
            flushHandlerNames.add(JSON_HANDLER_NAME);
        }
        if (!rotateHandlerNames.isEmpty()) {
            resourceBuilder.action(Action.action()
                    .name("rotate")
                    .description("Requests that a log file be rotated.")
                    .parameter(Parameter.parameter()
                            .name("handler")
                            .type("string")
                            .source(ParameterSource.ADDITIONAL)
                            .description("Handler Name")
                            .enumValues(rotateHandlerNames.toArray(new String[rotateHandlerNames.size()]))
                            .required(true)
                            .build())
                    .response(Schema.schema()
                            .type(RotateActionResponse.class)
                            .build())
                    .build());
        }
        if (!flushHandlerNames.isEmpty()) {
            resourceBuilder.action(Action.action()
                    .name("flush")
                    .description("Requests that a log file have its buffer flushed.")
                    .parameter(Parameter.parameter()
                            .name("handler")
                            .type("string")
                            .source(ParameterSource.ADDITIONAL)
                            .description("Handler Name")
                            .enumValues(flushHandlerNames.toArray(new String[flushHandlerNames.size()]))
                            .required(true)
                            .build())
                    .response(Schema.schema()
                            .type(FlushActionResponse.class)
                            .build())
                    .build());
        }
    }

    private static Set<String> eventHandlerNames(final JsonValue eventHandlers) {
        final Set<String> names = new HashSet<>();
        for (final JsonValue handlerConfig : eventHandlers) {
            final String name = handlerConfig.get(HANDLER_NAME_POINTER).asString();
            if (!isNullOrEmpty(name)) {
                names.add(name);
            }
        }
        return names;
    }

}
