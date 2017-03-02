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

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.openidm.audit.report.impl.AuditReportServiceImpl.PARAM_AGGREGATE;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicInteger;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.QueryFilters;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Responses;
import org.forgerock.openidm.audit.impl.AuditServiceImpl;
import org.forgerock.openidm.audit.report.AuditReportService;
import org.forgerock.openidm.filter.JsonValueFilterVisitor;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.Test;

public class AuditReportServiceImplTest {

    private static final JsonValueFilterVisitor JSON_VALUE_FILTER_VISITOR = new JsonValueFilterVisitor();

    private JsonValue testAuditData = json(array(
            object(field("timestamp", "2016-12-05T01:01:01.000Z"), field("_id", "1")),
            object(field("timestamp", "2016-12-06T01:01:01.000Z"), field("_id", "2")),
            object(field("timestamp", "2017-01-01T01:01:01.000Z"), field("_id", "3")),
            object(field("timestamp", "2017-01-05T01:01:01.000Z"), field("_id", "4")),
            object(field("timestamp", "2017-01-06T01:01:01.000Z"), field("_id", "5")),
            object(field("timestamp", "2017-01-25T01:01:01.000Z"), field("_id", "6")),
            object(field("timestamp", "2017-02-27T01:01:01.000Z"), field("_id", "7")),
            object(field("timestamp", "2017-02-27T01:11:01.000Z"), field("_id", "8")),
            object(field("timestamp", "2017-02-27T02:34:32.115Z"), field("_id", "9")),
            object(field("timestamp", "2017-02-27T02:34:32.115Z"), field("_id", "10")),
            object(field("timestamp", "2017-02-27T07:59:59.999Z"), field("_id", "11")),
            object(field("timestamp", "2017-02-27T08:00:00.000Z"), field("_id", "12")), // midnight PST/UTC-0800
            object(field("timestamp", "2017-02-27T08:34:32.115Z"), field("_id", "13")),
            object(field("timestamp", "2017-02-27T09:34:32.115Z"), field("_id", "14")),
            object(field("timestamp", "2017-02-27T16:30:31.115Z"), field("_id", "15")),
            object(field("timestamp", "2017-02-27T16:31:32.115Z"), field("_id", "16")),
            object(field("timestamp", "2017-02-27T16:32:32.115Z"), field("_id", "17")),
            object(field("timestamp", "2017-02-27T16:33:32.115Z"), field("_id", "18")),
            object(field("timestamp", "2017-02-27T16:34:32.115Z"), field("_id", "19")),
            object(field("timestamp", "2017-02-27T17:00:32.115Z"), field("_id", "20")),
            object(field("timestamp", "2017-02-27T18:00:32.115Z"), field("_id", "21")),
            object(field("timestamp", "2017-02-27T19:34:32.115Z"), field("_id", "22")),
            object(field("timestamp", "2017-02-28T19:34:32.115Z"), field("_id", "23"))
    ));

    @Test(expectedExceptions = BadRequestException.class)
    public void verifyQueryFailEmptyParameters() throws ResourceException, InterruptedException {
        AuditReportServiceImpl reportService = setupTestAuditReportService();
        QueryResourceHandler queryResourceHandler = new QueryResourceHandler() {
            @Override
            public boolean handleResource(ResourceResponse resourceResponse) {
                return true;
            }
        };
        QueryRequest request = Requests.newQueryRequest(AuditReportService.ROUTER_PREFIX + "/access");
        reportService.handleQuery(new RootContext(), request, queryResourceHandler).getOrThrow();
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void verifyQueryFailInvalidParameters() throws ResourceException, InterruptedException {
        AuditReportServiceImpl reportService = setupTestAuditReportService();
        QueryResourceHandler queryResourceHandler = new QueryResourceHandler() {
            @Override
            public boolean handleResource(ResourceResponse resourceResponse) {
                return true;
            }
        };
        QueryRequest request = Requests.newQueryRequest(AuditReportService.ROUTER_PREFIX + "/access");
        request.setAdditionalParameter(PARAM_AGGREGATE, "bogus");
        reportService.handleQuery(new RootContext(), request, queryResourceHandler).getOrThrow();
    }

    @Test
    public void verifyAuditReport() throws Exception {
        QueryFilter<JsonPointer> queryFilter;

        // Verify the filter can pull from a few minute range, and that they collapse within an hour.
        queryFilter = QueryFilters.parse(
                "timestamp gt \"2017-02-27T16:31:00.000Z\" and timestamp lt \"2017-02-27T16:34:00.000Z\"");
        verifyAuditReport(AggregateBy.min, "+0000", queryFilter, 3);
        verifyAuditReport(AggregateBy.hour, "-0000", queryFilter, 1);

        // Verify a days range and that it will collapse into a single day
        queryFilter = QueryFilters.parse(
                "timestamp gt \"2017-02-27T00:00:00.000Z\" and timestamp lt \"2017-02-28T00:00:00.000Z\"");
        verifyAuditReport(AggregateBy.hour, "-0000", queryFilter, 9);
        verifyAuditReport(AggregateBy.day, "-0000", queryFilter, 1);
        // Shift the TZ to verify the UTC day split into 2.
        verifyAuditReport(AggregateBy.day, "-0800", queryFilter, 2);

        // Verify a days range and that it will collapse into a single day, but shift the query filter to utc-8.
        // This should be the same results as the day verification above.
        queryFilter = QueryFilters.parse(
                "timestamp gt \"2017-02-26T16:00:00.000-0800\" and timestamp lt \"2017-02-27T16:00:00.000-0800\"");
        verifyAuditReport(AggregateBy.hour, "-0000", queryFilter, 9);
        verifyAuditReport(AggregateBy.day, "-0000", queryFilter, 1);
        verifyAuditReport(AggregateBy.day, "-0800", queryFilter, 2);

        // Verify a week and months range.
        queryFilter = QueryFilters.parse(
                "timestamp gt \"2016-01-01T00:00:00.000Z\" and timestamp lt \"2018-01-01T00:00:00.000Z\"");
        verifyAuditReport(AggregateBy.week, "-0000", queryFilter, 5);
        verifyAuditReport(AggregateBy.month, "-0000", queryFilter, 3);
    }

    private JsonValue verifyAuditReport(final AggregateBy aggregateBy, final String utcOffset,
            final QueryFilter<JsonPointer> filter, final int expectedCount) throws Exception {
        AuditReportServiceImpl reportService = setupTestAuditReportService();

        QueryRequest request = Requests.newQueryRequest("auditReport/access");
        request.setQueryFilter(filter);
        request.setAdditionalParameter(PARAM_AGGREGATE, aggregateBy.name());
        if (null != utcOffset) {
            request.setAdditionalParameter(AuditReportServiceImpl.PARAM_UTC_OFFSET, utcOffset);
        }
        final AtomicInteger recordCount = new AtomicInteger(0);
        final JsonValue resultContents = json(array());
        Promise<QueryResponse, ResourceException> responsePromise =
                reportService.handleQuery(new RootContext(), request, new QueryResourceHandler() {
                    @Override
                    public boolean handleResource(ResourceResponse resourceResponse) {
                        recordCount.getAndIncrement();
                        resultContents.add(resourceResponse.getContent());
                        return true;
                    }
                });
        responsePromise.getOrThrow();
        System.out.println("resultContents = " + resultContents);
        assertThat(recordCount.get()).isEqualTo(expectedCount);
        return resultContents;
    }

    private AuditReportServiceImpl setupTestAuditReportService() {
        AuditReportServiceImpl reportService = new AuditReportServiceImpl();
        AuditServiceImpl auditService = mock(AuditServiceImpl.class);
        reportService.bindAuditService(auditService);
        when(auditService.handleQuery(any(Context.class), any(QueryRequest.class), any(QueryResourceHandler.class)))
                .thenAnswer(
                        new Answer<Promise<QueryResponse, ResourceException>>() {
                            @Override
                            public Promise<QueryResponse, ResourceException> answer(InvocationOnMock invocation)
                                    throws Throwable {
                                QueryRequest queryRequest = invocation.getArgumentAt(1, QueryRequest.class);
                                QueryFilter<JsonPointer> queryFilter = queryRequest.getQueryFilter();

                                QueryResourceHandler resourceHandler
                                        = invocation.getArgumentAt(2, QueryResourceHandler.class);

                                System.out.println("queryFilter = " + queryFilter);
                                for (JsonValue jsonValue : testAuditData) {
                                    if (queryFilter.accept(JSON_VALUE_FILTER_VISITOR, jsonValue)) {
                                        resourceHandler.handleResource(
                                                newResourceResponse(jsonValue.get("_id").asString(), null, jsonValue));
                                    }
                                }

                                return Responses.newQueryResponse().asPromise();
                            }
                        });
        return reportService;
    }
}
