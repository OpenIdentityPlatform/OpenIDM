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

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.core.ServerConstants;
import org.apache.commons.lang3.time.FastDateFormat;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class DateUtil {
    private FastDateFormat formatter;
    private static final Locale LOCALE = new Locale("EN");

    /**
     * Create a new DateUtil with the default timezone that generates ISO8601 format timestamps with milliseconds
     * <p>2011-09-09T14:58:17.6+02:00
     * <p>2011-09-09T14:58:17.65+02:00
     * <p>2011-09-09T14:58:17.654+02:00
     *
     *
     * @return new DateUtil with default formatting
     */
    public static DateUtil getDateUtil() {
        return new DateUtil(ServerConstants.DATE_FORMAT_ISO8601_TIME_MILLISECOND, TimeZone.getDefault());
    }

    /**
     * Create a new DateUtil using a specified TimeZone that generates ISO8601 format timestamps with milliseconds
     * <p>2011-09-09T14:58:17.6+02:00
     * <p>2011-09-09T14:58:17.65+02:00
     * <p>2011-09-09T14:58:17.654+02:00
     *
     *
     * @param zone the given time zone
     * @return new DateUtil with specified time zone
     */
    public static DateUtil getDateUtil(TimeZone zone) {
        return new DateUtil(ServerConstants.DATE_FORMAT_ISO8601_TIME_MILLISECOND, zone);
    }

    /**
     * Create a new DateUtil using a TimeZone for the specified ID that generates
     * ISO8601 format timestamps with milliseconds
     * <p>2011-09-09T14:58:17.6+02:00
     * <p>2011-09-09T14:58:17.65+02:00
     * <p>2011-09-09T14:58:17.654+02:00
     *
     * @param timeZoneID the ID for a TimeZone, either an abbreviation such as "PST",
     *                   a full name such as "America/Los_Angeles", or a custom ID such as "GMT-8:00".
     *                   Note that the support of abbreviations is for JDK 1.1.x compatibility only and
     *                   full names should be used.
     * @return DateUtil with specified time zone
     */
    public static DateUtil getDateUtil(String timeZoneID) {
        return new DateUtil(ServerConstants.DATE_FORMAT_ISO8601_TIME_MILLISECOND, TimeZone.getTimeZone(timeZoneID));
    }

    /**
     * Create a new DateUtil using a Json object for configuration
     *
     * @param config Json object defining the configuration of the object. Expects "timezone" field to be defined
     *               otherwise uses the system default timezone
     * @return dateUtil object using the provided configuration object
     */
    public static DateUtil getDateUtil(JsonValue config) {
        TimeZone zone;
        String zoneID = config.get("timezone").asString(); // Returns null if object is undefined
        if (zoneID == null) {
            zone = TimeZone.getDefault();
        } else {
            zone = TimeZone.getTimeZone(zoneID);
        }
        return getDateUtil(zone);
    }

    /**
     * Private constructor
     * @param format string that defines the timestamp format
     * @param zone time zone to configure the formatter
     */
    private DateUtil(String format, TimeZone zone) {
        formatter = FastDateFormat.getInstance(format, zone, LOCALE);
    }

    /**
     * Generate a formatted timestamp for the current time
     *
     * @return String containing a timestamp
     */
    public String now() {
        return formatDateTime(new Date());
    }

    /**
     * Formats a given date into a timestamp
     * @param date date object to convert
     * @return String containing the formatted timestamp
     */
    public String formatDateTime(Date date) {
        String dateStr = formatter.format(date);

        int colonPos = dateStr.length() - 2;
        return dateStr.substring(0, colonPos)
                + ":" + dateStr.substring(colonPos);
    }

    /**
     * return the number of days between the two dates.
     *
     * @param start      Start date
     * @param end        End date
     * @param includeDay include both Days (increase the result with one)
     * @return number of days
     */
    public static int getDateDifferenceInDays(Date start, Date end, Boolean includeDay) {
        Integer result = null;
        if (start != null && end != null) {
            Long l = 86400000l;
            Long r = (end.getTime() - start.getTime()) / l;
            result = r.intValue();
            if (includeDay) {
                result++;
            }
        }
        return result;
    }
}
