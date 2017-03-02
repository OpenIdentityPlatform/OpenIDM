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

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.MutableDateTime;

/**
 * Determines the aggregated time for which a timestamp falls into.
 */
enum AggregateBy {
    min {
        @Override
        protected DateTime getAggregateTime(DateTime timestamp, int offsetMillis) {
            MutableDateTime dateTime = timestamp.toDateTime(DateTimeZone.forOffsetMillis(offsetMillis))
                    .toMutableDateTime();
            return dateTime
                    .secondOfMinute().set(0)
                    .millisOfSecond().set(0)
                    .toDateTime();
        }
    },
    hour {
        @Override
        protected DateTime getAggregateTime(DateTime timestamp, int offsetMillis) {
            MutableDateTime dateTime = timestamp.toDateTime(DateTimeZone.forOffsetMillis(offsetMillis))
                    .toMutableDateTime();
            return dateTime
                    .minuteOfHour().set(0)
                    .secondOfMinute().set(0)
                    .millisOfSecond().set(0)
                    .toDateTime();
        }
    },
    day {
        @Override
        protected DateTime getAggregateTime(DateTime timestamp, int offsetMillis) {
            MutableDateTime dateTime = timestamp.toDateTime(DateTimeZone.forOffsetMillis(offsetMillis))
                    .toMutableDateTime();
            return dateTime
                    .hourOfDay().set(0)
                    .minuteOfHour().set(0)
                    .secondOfMinute().set(0)
                    .millisOfSecond().set(0)
                    .toDateTime();
        }
    },
    week {
        @Override
        protected DateTime getAggregateTime(DateTime timestamp, int offsetMillis) {
            MutableDateTime dateTime = timestamp.toDateTime(DateTimeZone.forOffsetMillis(offsetMillis))
                    .toMutableDateTime();
            return dateTime
                    .dayOfWeek().set(1)
                    .hourOfDay().set(0)
                    .minuteOfHour().set(0)
                    .secondOfMinute().set(0)
                    .millisOfSecond().set(0)
                    .toDateTime();
        }
    },
    month {
        @Override
        protected DateTime getAggregateTime(DateTime timestamp, int offsetMillis) {
            MutableDateTime dateTime = timestamp.toDateTime(DateTimeZone.forOffsetMillis(offsetMillis))
                    .toMutableDateTime();
            return dateTime
                    .dayOfMonth().set(1)
                    .hourOfDay().set(0)
                    .minuteOfHour().set(0)
                    .secondOfMinute().set(0)
                    .millisOfSecond().set(0)
                    .toDateTime();
        }
    };

    /**
     * Implement to zero out the non-significant values of the aggregation.
     *
     * @param timestamp audit record timestamp in zulu tz.
     * @param offsetMillis amount to shift the timestamp before determining the significant values.
     * @return The timestamp with zeroed-out non-significant values.
     */
    protected abstract DateTime getAggregateTime(DateTime timestamp, int offsetMillis);
}
