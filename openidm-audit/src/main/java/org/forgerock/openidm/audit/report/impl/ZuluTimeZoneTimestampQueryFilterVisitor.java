package org.forgerock.openidm.audit.report.impl;

import java.util.Set;

import org.forgerock.audit.util.DateUtil;
import org.forgerock.json.JsonPointer;
import org.forgerock.openidm.query.ValueTransformerQueryFilterVisitor;
import org.joda.time.DateTimeZone;

/**
 * A query filter visitor that normalizes the 'timestamp' values for the provided timestamp pointers into the full
 * Audit supported zulu time format ie "yyyy-MM-dd'T'HH:mm:s.SSS'Z'"  ex: "2017-02-27T01:01:01.000Z").
 */
public class ZuluTimeZoneTimestampQueryFilterVisitor
        extends ValueTransformerQueryFilterVisitor<Set<JsonPointer>, JsonPointer> {

    private static final DateUtil DATE_UTIL = DateUtil.getDateUtil(DateTimeZone.UTC);

    @Override
    protected Object transformValue(Set<JsonPointer> pointersToTransform, JsonPointer field, Object valueAssertion) {
        if (null != valueAssertion && pointersToTransform.contains(field)) {
            return DATE_UTIL.formatDateTime(DATE_UTIL.parseTimestamp(valueAssertion.toString()));
        } else {
            return valueAssertion;
        }
    }
}