package org.forgerock.openidm.audit.report.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.resource.QueryFilters;
import org.forgerock.util.query.QueryFilter;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests {@link ZuluTimeZoneTimestampQueryFilterVisitor}.
 */
public class ZuluTimeZoneTimestampQueryFilterVisitorTest {

    static final ZuluTimeZoneTimestampQueryFilterVisitor ZULU_TIME_ZONE_TIMESTAMP_QUERY_FILTER_VISITOR =
            new ZuluTimeZoneTimestampQueryFilterVisitor();

    @DataProvider
    public static Object[][] transformationTests() {
        return new Object[][]{
                {"/timestamp lt \"2017-02-27T01:01:01.000-0000\"", "/timestamp lt \"2017-02-27T01:01:01.000Z\""},
                {"/timestamp lt \"2017-02-27T01:01:01.000+0000\"", "/timestamp lt \"2017-02-27T01:01:01.000Z\""},
                {"/timestamp lt \"2017-02-27T16:00:00.000-0800\"", "/timestamp lt \"2017-02-28T00:00:00.000Z\""},
                {"/timestamp lt \"2017-02-27T05:30:00.000+0530\"", "/timestamp lt \"2017-02-27T00:00:00.000Z\""},
                {"/timestamp gt \"2017-02-27T00:00:00.0Z\"", "/timestamp gt \"2017-02-27T00:00:00.000Z\""},
        };
    }

    @Test(dataProvider = "transformationTests")
    public void testVisitor(String from, String to) {
        Set<JsonPointer> timestampPointers = new HashSet<>();
        timestampPointers.add(new JsonPointer("timestamp"));
        QueryFilter<JsonPointer> transformedFilter = QueryFilters.parse(from).accept(
                ZULU_TIME_ZONE_TIMESTAMP_QUERY_FILTER_VISITOR, timestampPointers);
        assertThat(to).isEqualTo(transformedFilter.toString());
    }
}
