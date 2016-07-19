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

package org.forgerock.openidm.managed;

import static org.forgerock.api.enums.PatchOperation.*;
import static org.forgerock.api.enums.PatchOperation.TRANSFORM;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.JsonValue.field;

import org.forgerock.api.enums.CreateMode;
import org.forgerock.api.enums.ParameterSource;
import org.forgerock.api.enums.QueryType;
import org.forgerock.api.models.Action;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.Create;
import org.forgerock.api.models.Delete;
import org.forgerock.api.models.Items;
import org.forgerock.api.models.Parameter;
import org.forgerock.api.models.Patch;
import org.forgerock.api.models.Paths;
import org.forgerock.api.models.Query;
import org.forgerock.api.models.Read;
import org.forgerock.api.models.Resource;
import org.forgerock.api.models.Schema;
import org.forgerock.api.models.SubResources;
import org.forgerock.api.models.Update;
import org.forgerock.api.models.VersionedPath;
import org.forgerock.api.transform.OpenApiTransformer;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.schema.validator.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ApiDescription} builder for {@link ManagedObjectService}.
 */
class ManagedObjectApiDescription {

    private final static Logger logger = LoggerFactory.getLogger(ManagedObjectApiDescription.class);

    private static final JsonPointer ITEMS_TYPE_POINTER =
            new JsonPointer(new String[]{Constants.ITEMS, Constants.TYPE});

    private static final JsonValue STATUS_RESPONSE_JSON =
            json(object(
                    field(Constants.TYPE,
                            Constants.TYPE_OBJECT),
                    field(Constants.PROPERTIES, object(
                            field("status", object(
                                    field(Constants.TYPE, Constants.TYPE_STRING)
                            ))
                    ))));

    private ManagedObjectApiDescription() {
        // empty
    }

    /**
     * Builds {@link ApiDescription} using JSON schema from {@link ManagedObjectSet#getConfig()}.
     *
     * @param objectSet {@link ManagedObjectSet} instance
     * @return {@link ApiDescription} instance or {@code null} if a fatal error occurred and was logged
     */
    public static ApiDescription build(final ManagedObjectSet objectSet) {
        try {
            // create a copy of the JSON Schema, and parse it to create an API Descriptor
            final JsonValue config = objectSet.getConfig();
            final JsonValue schema = config.get("schema").isNull() || config.get("schema").get(Constants.TYPE).isNull()
                    ? json(object(field(Constants.TYPE, Constants.TYPE_OBJECT))) : config.get("schema").copy();
            SubResources itemsSubResources = null;
            if (!objectSet.getRelationshipProviders().isEmpty()) {
                final JsonValue properties = schema.get(Constants.PROPERTIES);

                boolean hasItemsSubresources = false;
                final SubResources.Builder itemsSubresourcesBuilder = SubResources.subresources();

                for (final JsonPointer pointer : objectSet.getRelationshipProviders().keySet()) {
                    final JsonValue relationshipSchema = properties.get(pointer);

                    // relationship is NOT a valid JSON Schema type, so replace it
                    if (SchemaField.TYPE_RELATIONSHIP.equals(relationshipSchema.get(Constants.TYPE).asString())) {
                        // this is a singleton, so update schema
                        relationshipSchema.put(Constants.TYPE, Constants.TYPE_OBJECT);
                    } else {
                        // this is a collection, so update schema AND define sub-resource
                        final JsonValue itemsType = relationshipSchema.get(ITEMS_TYPE_POINTER);
                        if (itemsType != null && SchemaField.TYPE_RELATIONSHIP.equals(itemsType.asString())) {
                            relationshipSchema.put(ITEMS_TYPE_POINTER, Constants.TYPE_OBJECT);
                        }
                        itemsSubresourcesBuilder.put(pointer.leaf(), Resource.resource()
                                .mvccSupported(true)
                                .title(relationshipSchema.get("title").asString())
                                .description(relationshipSchema.get("description").asString())
                                .resourceSchema(Schema.schema().schema(relationshipSchema).build())
                                .create(Create.create()
                                        .mode(CreateMode.ID_FROM_SERVER)
                                        .build())
                                .query(Query.query()
                                        .type(QueryType.ID)
                                        .queryId("query-all")
                                        .build())
                                .query(Query.query()
                                        .type(QueryType.ID)
                                        .queryId("query-all-ids")
                                        .build())
                                .query(Query.query()
                                        .type(QueryType.FILTER)
                                        .queryableFields("*")
                                        .build())
                                .items(Items.items()
                                        .create(Create.create()
                                                .mode(CreateMode.ID_FROM_CLIENT)
                                                .build())
                                        .read(Read.read().build())
                                        .update(Update.update().build())
                                        .delete(Delete.delete().build())
                                        .patch(Patch.patch()
                                                .operations(ADD, REMOVE, REPLACE, INCREMENT, COPY, MOVE, TRANSFORM)
                                                .build())
                                        .build())
                                .build());
                        hasItemsSubresources = true;
                    }
                }
                if (hasItemsSubresources) {
                    itemsSubResources = itemsSubresourcesBuilder.build();
                }
            }

            final String title = schema.get("title").isNotNull() ? schema.get("title").asString() : objectSet.getName();
            final ApiDescription apiDescription = ApiDescription.apiDescription()
                    .id("temp")
                    .version("0")
                    .paths(Paths.paths()
                            .put("/", VersionedPath.versionedPath()
                                    .put(VersionedPath.UNVERSIONED, Resource.resource()
                                            .title("Managed " + title)
                                            .description("Endpoints for managing " + objectSet.getName() + " objects.")
                                            .mvccSupported(true)
                                            .resourceSchema(Schema.schema()
                                                    .schema(schema)
                                                    .build())
                                            .create(Create.create()
                                                    .mode(CreateMode.ID_FROM_SERVER)
                                                    .build())
                                            .query(Query.query()
                                                    .type(QueryType.ID)
                                                    .queryId("query-all")
                                                    .build())
                                            .query(Query.query()
                                                    .type(QueryType.ID)
                                                    .queryId("query-all-ids")
                                                    .build())
                                            .query(Query.query()
                                                    .type(QueryType.FILTER)
                                                    .queryableFields("*")
                                                    .build())
                                            .items(Items.items()
                                                    .pathParameter(Parameter.parameter()
                                                            .source(ParameterSource.PATH)
                                                            .type(Constants.TYPE_STRING)
                                                            .required(true)
                                                            .name(objectSet.getName() + "Id")
                                                            .build())
                                                    .create(Create.create()
                                                            .mode(CreateMode.ID_FROM_CLIENT)
                                                            .build())
                                                    .read(Read.read().build())
                                                    .update(Update.update().build())
                                                    .delete(Delete.delete().build())
                                                    .patch(Patch.patch()
                                                            .operations(ADD, REMOVE, REPLACE, INCREMENT, COPY, MOVE,
                                                                    TRANSFORM)
                                                            .build())
                                                    .action(Action.action()
                                                            .name("triggerSyncCheck")
                                                            .response(Schema.schema()
                                                                    .schema(STATUS_RESPONSE_JSON)
                                                                    .build())
                                                            .build())
                                                    .subresources(itemsSubResources)
                                                    .build())
                                            .build())
                                    .build())
                            .build())
                    .build();

            // do a dry-run of generating the Swagger model, because the managed.json content is unpredictable
            OpenApiTransformer.execute(apiDescription);
            return apiDescription;
        } catch (Exception e) {
            logger.info("Failed to generate API Description for managed.json section: " + objectSet.getName(), e);
            return null;
        }
    }

}
