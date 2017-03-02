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
package org.forgerock.openidm.audit.report.impl;

import org.forgerock.api.enums.ParameterSource;
import org.forgerock.api.enums.QueryType;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.Parameter;
import org.forgerock.api.models.Paths;
import org.forgerock.api.models.Query;
import org.forgerock.api.models.Resource;
import org.forgerock.api.models.Schema;
import org.forgerock.api.models.VersionedPath;
import org.forgerock.audit.events.EventTopicsMetaData;

/**
 * {@link ApiDescription} builder for {@link org.forgerock.openidm.audit.report.AuditReportService}.
 */
public class AuditReportServiceApiDescription {
    private static final String TITLE = "Audit Report";
    private static final String QUERY_DESCRIPTION = "<p>The provided queryFilter is forwarded to the auditService to "
            + "determine the set of audit records that need to be aggregated.  For each audit event, the timestamp of"
            + " the audit record is shifted by the provided UTC offset to determine which aggregated group the audit "
            + "event falls into.  The response will be an array of aggregated counts paired with the group timestamp, "
            + "in seconds since unix epoch, as the ID and the iso8601 format of the timestamp.</p>";

    private AuditReportServiceApiDescription() {
        // empty
    }

    public static ApiDescription build(EventTopicsMetaData eventTopicsMetaData) {
        Paths.Builder pathsBuilder = Paths.paths();

        for (final String topic : eventTopicsMetaData.getTopics()) {
            pathsBuilder.put('/' + topic, buildHandlerTopicPath(topic));
        }

        return ApiDescription.apiDescription()
                .id("temp")
                .version("0")
                .paths(pathsBuilder.build())
                .build();
    }

    private static VersionedPath buildHandlerTopicPath(String topic) {
        final String description = "Audit Reporting endpoints for the " + topic + " topic.";
        final Resource.Builder resourceBuilder = Resource.resource()
                .mvccSupported(false)
                .title(TITLE)
                .description(description)
                .resourceSchema(Schema.schema()
                        .type(ReportRecord.class).build())
                .query(Query.query()
                        .description(
                                "Query to return aggregated audit '" + topic + "' topic data. " + QUERY_DESCRIPTION)
                        .type(QueryType.FILTER)
                        .parameter(Parameter.parameter()
                                .name(AuditReportServiceImpl.PARAM_UTC_OFFSET)
                                .type("string")
                                .description("RFC-822 UTC offset to shift the audit record timestamp. "
                                        + "In the format of '+|-HHmm'; ex: '-0800', or '+0530'.")
                                .source(ParameterSource.ADDITIONAL)
                                .required(false)
                                .build())
                        .parameter(Parameter.parameter()
                                .name(AuditReportServiceImpl.PARAM_AGGREGATE)
                                .type("string")
                                .description("Indicates aggregated group scale for the audit records.")
                                .source(ParameterSource.ADDITIONAL)
                                .required(true)
                                .enumValues("min", "hour", "day", "week", "month")
                                .build())
                        .build());

        return VersionedPath.versionedPath()
                .put(VersionedPath.UNVERSIONED, resourceBuilder.build())
                .build();
    }
}