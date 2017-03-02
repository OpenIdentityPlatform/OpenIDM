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

import static org.forgerock.json.resource.Responses.newResourceResponse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.audit.events.AuditEventBuilder;
import org.forgerock.http.ApiProducer;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.AbstractRequestHandler;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Responses;
import org.forgerock.openidm.audit.AuditService;
import org.forgerock.openidm.audit.report.AuditReportService;
import org.forgerock.services.context.Context;
import org.forgerock.services.descriptor.Describable;
import org.forgerock.util.Function;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;
import org.joda.time.DateTime;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service provides query access to aggregated audit data.
 */
@Component(name = "org.forgerock.openidm.audit.report", immediate = true, policy = ConfigurationPolicy.IGNORE)
@Service
@Properties({
        @Property(name = "service.description", value = "Audit Report Service"),
        @Property(name = "service.vendor", value = "ForgeRock AS"),
        @Property(name = "openidm.router.prefix", value = AuditReportService.ROUTER_PREFIX + "/*")
})
public class AuditReportServiceImpl extends AbstractRequestHandler
        implements AuditReportService, Describable<ApiDescription, Request> {

    private static final Logger logger = LoggerFactory.getLogger(AuditReportServiceImpl.class);
    private static final JsonPointer TIMESTAMP_PTR = JsonPointer.ptr("timestamp");
    // The format of the UTC_OFFSET is expected to be '+|-HHmm', ex: -0800
    private static final int UTC_OFFSET_LENGTH = 5;

    static final String PARAM_UTC_OFFSET = "utcOffset";
    static final String PARAM_AGGREGATE = "aggregate";
    static final ZuluTimeZoneTimestampQueryFilterVisitor TIMESTAMP_QUERY_FILTER_VISITOR =
            new ZuluTimeZoneTimestampQueryFilterVisitor();

    private ApiDescription apiDescription;

    @Reference
    private AuditService auditService;

    @Activate
    void activate(ComponentContext componentContext) throws Exception {
        logger.debug("AuditReportServiceImpl activated with config " + componentContext.getProperties());
        apiDescription = AuditReportServiceApiDescription.build(auditService.getEventTopicsMetaData());
    }

    @Deactivate
    void deactivate(ComponentContext componentContext) throws Exception {
        logger.debug("AuditReportServiceImpl deactivated");
        apiDescription = null;
    }

    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(Context context, QueryRequest request,
            final QueryResourceHandler queryResourceHandler) {

        // Determine offset in milliseconds.
        final String offsetParam = request.getAdditionalParameter(PARAM_UTC_OFFSET);
        if (null != offsetParam && offsetParam.length() != UTC_OFFSET_LENGTH) {
            return new BadRequestException(
                    PARAM_UTC_OFFSET + " must be in the format of +/-HHMM, ex -0800.").asPromise();
        }
        final int utcOffsetMillis = (null == offsetParam)
                ? TimeZone.getDefault().getRawOffset()
                : parseUtcOffsetMillis(offsetParam);

        // Determine the aggregate parameter that was passed to us.
        final AggregateBy aggregateBy = getAggregateBy(request);
        if (null == aggregateBy) {
            return new BadRequestException(
                    PARAM_AGGREGATE + " parameter expected to be one of " + Arrays.asList(AggregateBy.values()))
                    .asPromise();
        }

        // Only QueryFilter is supported.
        final QueryFilter<JsonPointer> queryFilter = request.getQueryFilter();
        if (null != request.getQueryExpression() || null != request.getQueryId()) {
            return new BadRequestException("Only queryFilter is supported.").asPromise();
        } else if (null == queryFilter) {
            return new BadRequestException("QueryFilter is required.").asPromise();
        }

        // Build the request to use against the audit server.
        final QueryRequest auditQueryRequest = Requests.newQueryRequest(request.getResourcePathObject());
        auditQueryRequest.addField(TIMESTAMP_PTR); // only need the timestamp field to return.

        // Normalize the filter's timestamp references into Zulu timezones.
        final Set<JsonPointer> timestampPointers = new HashSet<>();
        timestampPointers.add(TIMESTAMP_PTR);
        try {
            auditQueryRequest.setQueryFilter(queryFilter.accept(TIMESTAMP_QUERY_FILTER_VISITOR, timestampPointers));
        } catch (IllegalArgumentException e) {
            return new BadRequestException("Unable to read the query filter.",e).asPromise();
        }

        // Make the query to the Audit Service, and then output the count of the grouped results.
        final TreeMap<Long, ReportRecord> reportRecords = new TreeMap<>();
        return auditService.handleQuery(context, auditQueryRequest, new QueryResourceHandler() {
            @Override
            public boolean handleResource(ResourceResponse resourceResponse) {
                JsonValue auditRecord = resourceResponse.getContent();
                // Audit timestamp data is stored and returned as a zulu timezone ISO8601 timestamp.
                DateTime auditTimestamp = DateTime.parse(auditRecord.get(AuditEventBuilder.TIMESTAMP).asString());
                DateTime groupTimestamp = aggregateBy.getAggregateTime(auditTimestamp, utcOffsetMillis);
                ReportRecord groupRecord = reportRecords.get(groupTimestamp.getMillis());
                if (null == groupRecord) {
                    groupRecord = new ReportRecord(groupTimestamp);
                    reportRecords.put(groupTimestamp.getMillis(), groupRecord);
                }
                groupRecord.increment();
                return true;
            }
        }).then(new Function<QueryResponse, QueryResponse, ResourceException>() {
            @Override
            public QueryResponse apply(QueryResponse auditResponse) throws ResourceException {
                for (ReportRecord reportRecord : reportRecords.values()) {
                    queryResourceHandler.handleResource(
                            newResourceResponse(reportRecord.getId(), null, reportRecord.toJson()));
                }
                return Responses.newQueryResponse();
            }
        });
    }

    /**
     * Parses a UTC offset string based on RFC-822 into the offset in milliseconds.
     * For example  +0530 would be parsed as 19800000, and -0800 would be parsed as -28800000
     *
     * @param offsetParam A + or - followed by 4 digits in the time pattern of 'HHmm'.
     * @return millisecond offset from utc.
     */
    private int parseUtcOffsetMillis(String offsetParam) {
        int intValue = Integer.parseInt(offsetParam);
        int minutes = intValue % 100;
        int hours = (intValue - minutes) / 100;
        return (hours * 60 + minutes) * 60000;
    }

    /**
     * Returns the value of the 'aggregate' additional parameter passed in the query request.
     *
     * @param request the query request to pull the parameter from.
     * @return the value of the aggregate parameter or null if the required parameter is not a value from the
     * {@link AggregateBy} enum.
     */
    private AggregateBy getAggregateBy(QueryRequest request) {
        final String additionalParameter = request.getAdditionalParameter(PARAM_AGGREGATE);
        if (null == additionalParameter) {
            return null;
        }
        try {
            return AggregateBy.valueOf(additionalParameter);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public ApiDescription api(ApiProducer<ApiDescription> apiProducer) {
        return apiDescription;
    }

    @Override
    public ApiDescription handleApiRequest(Context context, Request request) {
        return apiDescription;
    }

    @Override
    public void addDescriptorListener(Listener listener) {
        // nothing to do here.
    }

    @Override
    public void removeDescriptorListener(Listener listener) {
        // nothing to do here.
    }
}