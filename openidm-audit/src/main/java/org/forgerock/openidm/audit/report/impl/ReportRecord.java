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

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceResponse;
import org.joda.time.DateTime;

/**
 * Api type for Audit Aggregated report record.
 */
@Title("Report Record")
public class ReportRecord {

    private String _id;
    private int count;
    private String iso8601;

    /**
     * Private default constructor for use by Api Descriptor.
     */
    private ReportRecord() {
    }

    /**
     * Uses the record time to build the ID (seconds since unix epoch) and to parse the iso8601 string.
     *
     * @param recordTime the timestamp to store a count for.
     */
    public ReportRecord(DateTime recordTime) {
        _id = Long.toString(recordTime.getMillis() / 1000);
        count = 0;
        iso8601 = recordTime.toString("yyyy-MM-dd'T'HH:mm:ss.SZ");
    }

    /**
     * Increments the count for this report record.
     */
    public void increment() {
        count++;
    }

    /**
     * Returns the JSON representation of this record.
     *
     * @return the JSON representation of this record.
     */
    public JsonValue toJson() {
        return json(object(
                field(ResourceResponse.FIELD_CONTENT_ID, _id),
                field("count", count),
                field("iso8601", iso8601)
        ));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ReportRecord that = (ReportRecord) o;

        return _id.equals(that._id);
    }

    @Override
    public int hashCode() {
        return _id.hashCode();
    }

    /**
     * Returns the id of this record which is the number of seconds since unix epoch.
     *
     * @return the id of this record
     */
    @Description("Aggregated group timestamp value in seconds since unix epoch")
    public String getId() {
        return _id;
    }

    /**
     * Count of audit events for this aggregation record.
     *
     * @return Count of audit events
     */
    @Description("Count of audit records in the group")
    public int getCount() {
        return count;
    }

    /**
     * Returns the ISO8601
     * @return
     */
    @Description("Aggregated group timestamp in ISO8601 format")
    public String getIso8601() {
        return iso8601;
    }
}
