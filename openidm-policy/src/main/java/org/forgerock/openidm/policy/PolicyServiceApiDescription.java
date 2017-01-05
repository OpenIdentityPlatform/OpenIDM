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

package org.forgerock.openidm.policy;

import static org.forgerock.guava.common.base.Strings.isNullOrEmpty;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.List;

import org.forgerock.api.enums.ParameterSource;
import org.forgerock.api.models.Action;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.Items;
import org.forgerock.api.models.Parameter;
import org.forgerock.api.models.Paths;
import org.forgerock.api.models.Read;
import org.forgerock.api.models.Resource;
import org.forgerock.api.models.Schema;
import org.forgerock.api.models.VersionedPath;
import org.forgerock.api.transform.OpenApiTransformer;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.schema.validator.Constants;
import org.forgerock.openidm.managed.ManagedObjectSet;
import org.forgerock.openidm.managed.SchemaField;
import org.forgerock.openidm.policy.api.ResourcePolicies;
import org.forgerock.openidm.policy.api.ResourcePolicy;
import org.forgerock.openidm.policy.api.ValidatePolicyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ApiDescription} builder for {@link PolicyService}.
 */
public class PolicyServiceApiDescription {

    private static final Logger logger = LoggerFactory.getLogger(PolicyServiceApiDescription.class);

    private static final String TITLE = "Policies";
    private static final JsonPointer ITEMS_TYPE_POINTER = new JsonPointer(Constants.ITEMS, Constants.TYPE);
    private static final JsonValue FLEXIBLE_OBJECT_SCHEMA = json(object(
            field(Constants.TYPE, Constants.TYPE_OBJECT),
            field(Constants.TITLE, "Key/Value Pairs"),
            field(Constants.ADDITIONALPROPERTIES, object(field(Constants.TYPE, Constants.TYPE_ANY)))));

    private PolicyServiceApiDescription() {
        // empty
    }

    /**
     * Builds {@link ApiDescription}.
     *
     * @param managedObjectSets {@link ManagedObjectSet} instances
     * @return {@link ApiDescription} instance or {@code null} if a fatal error occurred and was logged
     */
    public static ApiDescription build(final List<ManagedObjectSet> managedObjectSets) {
        try {
            final Paths.Builder paths = Paths.paths();
            for (final ManagedObjectSet objectSet : managedObjectSets) {
                final JsonValue schema = normalizeSchema(objectSet);
                final boolean hasTitle = !isNullOrEmpty(schema.get("title").asString());
                final String resourceTitle = hasTitle ? schema.get("title").asString() : objectSet.getName();
                final String resourceIdDescription = hasTitle ? schema.get("title").asString() + " ID" : "Resource ID";

                paths.put("/managed/" + objectSet.getName(), VersionedPath.versionedPath()
                        .put(VersionedPath.UNVERSIONED, Resource.resource()
                                .title(TITLE)
                                .description("Policies for " + resourceTitle + " objects.")
                                .mvccSupported(false)
                                .resourceSchema(Schema.schema()
                                        .type(ResourcePolicy.class)
                                        .build())
                                .read(Read.read()
                                        .description("List policies for " + resourceTitle + " objects.")
                                        .build())
                                .items(Items.items()
                                        .pathParameter(Parameter.parameter()
                                                .source(ParameterSource.PATH)
                                                .type(Constants.TYPE_STRING)
                                                .required(true)
                                                .name(objectSet.getName() + "Id")
                                                .description(resourceIdDescription)
                                                .build())
                                        .action(Action.action()
                                                .name("validateObject")
                                                .description("Validates a given " + resourceTitle + " object against "
                                                        + "its policy requirements.")
                                                .request(Schema.schema()
                                                        .schema(schema)
                                                        .build())
                                                .response(Schema.schema()
                                                        .type(ValidatePolicyResponse.class)
                                                        .build())
                                                .build())
                                        .action(Action.action()
                                                .name("validateProperty")
                                                .description(
                                                        "Verifies that a given property adheres to policy "
                                                                + "requirements.")
                                                .request(Schema.schema()
                                                        .schema(FLEXIBLE_OBJECT_SCHEMA)
                                                        .build())
                                                .response(Schema.schema()
                                                        .type(ValidatePolicyResponse.class)
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build());
            }

            final ApiDescription apiDescription = ApiDescription.apiDescription()
                    .id("temp")
                    .version("0")
                    .paths(paths
                            .put("/", VersionedPath.versionedPath()
                                    .put(VersionedPath.UNVERSIONED, Resource.resource()
                                            .title(TITLE)
                                            .description("Inspect and validate policies defined for managed objects.")
                                            .mvccSupported(false)
                                            .resourceSchema(Schema.schema()
                                                    .type(ResourcePolicies.class)
                                                    .build())
                                            .read(Read.read()
                                                    .description("List policies defined in the policy.json file. "
                                                            + "We recommend using `/policy/managed/{type}` endpoints "
                                                            + "for reading a complete list of policies for a given "
                                                            + "type.")
                                                    .build())
                                            .build())
                                    .build())
                            .build())
                    .build();

            // do a dry-run of generating the Swagger model, because the managed.json content is unpredictable
            OpenApiTransformer.execute(apiDescription);
            return apiDescription;
        } catch (Exception e) {
            logger.info("Failed to generate API Description for policy service");
            return null;
        }
    }

    private static JsonValue normalizeSchema(final ManagedObjectSet objectSet) {
        final JsonValue config = objectSet.getConfig();
        if (config.get("schema").isNull() || config.get("schema").get(Constants.TYPE).isNull()) {
            return FLEXIBLE_OBJECT_SCHEMA;
        }

        // copy schema before normalization
        final JsonValue schema = config.get("schema").copy();
        final JsonValue properties = schema.get(Constants.PROPERTIES);

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
            }
        }
        return schema;
    }

}