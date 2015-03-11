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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */
package org.forgerock.openidm.util;

import java.util.Date;

import org.forgerock.openidm.core.ServerConstants;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.chrono.ISOChronology;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Manages timestamp strings in ISO8601 format
 * <p>
 * <b>Example:</b> <br>
 * 2011-09-09T14:58:17.6+02:00 <br>
 * 2011-09-09T14:58:17.65+02:00 <br>
 * 2011-09-09T14:58:17.654+02:00 <br>
 * 2011-09-09T14:58:17.654Z
 *
 * @version $Revision$ $Date$
 */
public final class DateUtil {
    private Chronology chrono;

    /**
     * Fetches a DateUtil that is set in the default timezone.
     *
     * @return DateUtil set in the default timezone
     */
    public static DateUtil getDateUtil() {
        return new DateUtil();
    }

    /**
     * Returns a DateUtil using a specified timezone.
     *
     * @param zone
     *            string representation of a timezone i.e. "UTC" or "Asia/Tokyo"
     * @return DateUtil set with the supplied timezone
     */
    public static DateUtil getDateUtil(String zone) {
        return new DateUtil(zone);
    }

    /**
     * Creates a DateUtil using a specified timezone.
     *
     * @param zone
     *            DateTimeZone object
     * @return DateUtil set with the supplied timezone
     */
    public static DateUtil getDateUtil(DateTimeZone zone) {
        return new DateUtil(zone);
    }

    /**
     * Creates a DateUtil using the default timezone and generates ISO8601
     * timestamps.
     */
    private DateUtil() {
        this(DateTimeZone.getDefault());
    }

    /**
     * Creates a DateUtil using a specified timezone and generates ISO8601
     * timestamps.
     *
     * @param zone
     *            string representation of a timezone. i.e. "UTC" or
     *            "Asia/Tokyo"
     */
    private DateUtil(String zone) {
        this(DateTimeZone.forID(zone));
    }

    /**
     * Creates a DateUtil using a specified timezone and generates ISO8601
     * timestamps.
     *
     * @param zone
     *            timezone object
     */
    private DateUtil(DateTimeZone zone) {
        chrono = ISOChronology.getInstance(zone);
    }

    /**
     * Generate a formatted timestamp for the current time.
     *
     * @return String containing a timestamp
     */
    public String now() {
        return new DateTime(chrono).toString();
    }

    public DateTime currentDateTime() {
        return new DateTime(chrono);
    }

    /**
     * Formats a given DateTime into a timestamp.
     *
     * @param date
     *            DateTime object to convert
     * @return String containing the formatted timestamp
     */
    public String formatDateTime(DateTime date) {
        return date.withChronology(chrono).toString();
    }

    /**
     * Formats a given date into a timestamp.
     *
     * @param date
     *            date object to convert
     * @return String containing the formatted timestamp
     */
    public String formatDateTime(Date date) {
        DateTime dt = new DateTime(date, chrono);
        return dt.toString();
    }

    /**
     * Parses an ISO8601 compliant timestamp into a DateTime object.
     *
     * @param timestamp
     *            timestamp to parse
     * @return DateTime using the zone and chronology indicated by the timestamp
     */
    public DateTime parseTimestamp(String timestamp) {
        DateTimeFormatter parser = ISODateTimeFormat.dateTime();
        return parser.withOffsetParsed().parseDateTime(timestamp);
    }

    /**
     * Parses an ISO8601 compliant timestamp into a DateTime object.
     *
     * Checks the length of the timestamp and returns null if the string is not
     * ISO8601 compliant Accepted formats:
     *
     * <pre>
     * yyyy-MM-ddTHH:mm:ss.SSSZ,
     * yyyy-MM-ddTHH:mm:ss.SSS+00,
     * yyyy-MM-ddTHH:mm:ss.SSS+00:00
     * </pre>
     *
     * @param timestamp
     *            timestamp to parse
     * @return
     */
    public DateTime parseIfDate(String timestamp) {
        DateTime d = null;
        if (timestamp.length() > 23 && timestamp.length() < 30) {
            try {
                d = parseTimestamp(timestamp);
            } catch (IllegalArgumentException e) {
                /* ignore */
            }
        }
        return d;
    }

    /**
     * return the number of days between the two dates.
     *
     * @param start
     *            Start date
     * @param end
     *            End date
     * @param includeDay
     *            include both Days (increase the result with one)
     * @return number of days
     */
    public static int getDateDifferenceInDays(Date start, Date end, Boolean includeDay) {
        Integer result = null;
        if (start != null && end != null) {
            Long l = 86400000L;
            Long r = (end.getTime() - start.getTime()) / l;
            result = r.intValue();
            if (includeDay) {
                result++;
            }
        }
        return result;
    }

    public static void main(String[] args) {
        System.out.println(new DateUtil(ServerConstants.TIME_ZONE_UTC).now());
    }
}
