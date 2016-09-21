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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openidm.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openidm.core.ServerConstants.TIME_ZONE_UTC;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test the DateUtil class.
 */
public class DateUtilTest {

    private DateUtil dateUtil;
    private Date unixEpoc;
    
    @BeforeClass
    public void beforeClass() throws Exception {
        dateUtil = DateUtil.getDateUtil(TIME_ZONE_UTC);
        final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(TIME_ZONE_UTC));
        calendar.setTimeInMillis(0);
        unixEpoc = calendar.getTime();
    }
    
    @DataProvider
    public Object[][] timestampIntervalData() {
        return new Object[][] {
            // test interval format start/period
            { "2016-01-01T09:01:00.000Z", "2016-01-01T09:01:00.000Z/P1M", true },
            { "2016-03-01T09:01:00.000Z", "2016-01-01T09:01:00.000Z/P1M", false },
            // test interval format period/stop
            { "2016-01-01T09:01:00.000Z", "P1M1DT01H01M/2016-02-01T09:01:00.000Z", true },
            { "2016-03-01T09:01:00.000Z", "P1M1DT01H01M/2016-02-01T09:01:00.000Z", false },
            // test interval format start/stop
            { "2016-02-01T09:01:00.000Z", "2016-01-01T09:01:00.000Z/2016-03-01T09:01:00.000Z", true },
            { "2016-04-01T09:01:00.000Z", "2016-01-01T09:01:00.000Z/2016-03-01T09:01:00.000Z", false }
        };
    }

    @DataProvider
    public Object[][] dateDifferenceInDaysData() {
        return new Object[][] {
            { "dd/MM/yyyy", "01/12/2016", "11/12/2016", true, 10 },
            { "dd/MM/yyyy", "01/12/2016", "11/12/2016", false, 10 },
            { "dd-MM-yyyy hh:mm:ss", "01-12-2016 12:00:00", "11-12-2016 12:00:00", true, 10 },
            { "dd-MM-yyyy hh:mm:ss", "01-12-2016 12:00:00", "11-12-2016 12:00:00", false, 10 }
        };
    }
    
    @DataProvider
    public Object[][] schedulerData() {
        return new Object[][] {
            { "2016-01-01T09:01:00.000Z", "0 1 9 1 1 ? 2016"},
            { "2016-03-01T09:01:00.000+00", "0 1 9 1 3 ? 2016"},
            { "2016-06-20T09:01:55.000+01:00", "55 1 9 20 6 ? 2016"},
            { "2016-07-30T09:01:55.444+02:34", "55 1 9 30 7 ? 2016"}
        };
    }
    
    @DataProvider
    public Object[][] intervalData() {
        return new Object[][] {
            { "2016-01-01T09:01:00.000Z/P1M", "2016-01-01T09:01:00.000Z", "2016-02-01T09:01:00.000Z"},
            { "P1M1DT01H01M/2016-02-01T09:01:00.000Z", "2015-12-31T08:00:00.000Z", 
                "2016-02-01T09:01:00.000Z"},
            { "2016-01-01T09:01:00.000Z/2016-03-01T09:01:00.000Z", "2016-01-01T09:01:00.000Z", 
                "2016-03-01T09:01:00.000Z"},
        };
    }

    @DataProvider
    public Object[][] dateFormats() {
        return new Object[][] {
                /** Test custom format */
                { unixEpoc, "yyyy-MM-dd'T'HH:mm:ss", "1970-01-01T00:00:00"},
                /** Test null format */
                { unixEpoc, null, "1970-01-01T00:00:00.000Z"},
        };
    }

    @DataProvider
    public Object[][] parseDateFormats() {
        return new Object[][] {
                /** Test custom format */
                { "1970-01-01T00:00:00", "yyyy-MM-dd'T'HH:mm:ss"}
        };
    }
    
    @Test(dataProvider = "timestampIntervalData")
    public void testIsTimestampWithinInterval(String timestamp, String interval, boolean result) {
        DateTime instant = dateUtil.parseIfDate(timestamp);
        assertThat(dateUtil.isTimestampWithinInterval(instant, interval)).isEqualTo(result);
    }
    
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalidFormat() {
        String interval = "2016-01-01T09:01:00....invalid";
        dateUtil.isNowWithinInterval(interval);
    }
    
    @Test
    public void testIsNowWithinInterval() {
        String passInterval = dateUtil.formatDateTime(DateTime.now(DateTimeZone.UTC).minusDays(1)) 
                + "/" 
                + dateUtil.formatDateTime(DateTime.now(DateTimeZone.UTC).plusDays(1));
        String failInterval = dateUtil.formatDateTime(DateTime.now(DateTimeZone.UTC).plusDays(1)) 
                + "/" 
                + dateUtil.formatDateTime(DateTime.now(DateTimeZone.UTC).plusDays(2));

        assertThat(dateUtil.isNowWithinInterval(passInterval)).isEqualTo(true);
        assertThat(dateUtil.isNowWithinInterval(failInterval)).isEqualTo(false);
    }
    
    @Test(dataProvider = "dateDifferenceInDaysData")
    public void testGetDateDifferenceInDays(String format, String start, String end, Boolean includeDay, int diff) 
            throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        assertThat(dateUtil.getDateDifferenceInDays(sdf.parse(start), sdf.parse(end), includeDay))
                .isEqualTo(includeDay ? diff + 1 : diff);
    }
    
    @Test(dataProvider = "schedulerData")
    public void testGetSchedulerExpression(String date, String cronExpression) {
        assertThat(dateUtil.getSchedulerExpression(dateUtil.parseIfDate(date))).isEqualTo(cronExpression);
    }
    
    @Test(dataProvider = "intervalData")
    public void testGetStartAndEndOfInterval(String interval, String start, String end) {
        assertThat(dateUtil.getStartOfInterval(interval).withZone(DateTimeZone.UTC).toString()).isEqualTo(start);
        assertThat(dateUtil.getEndOfInterval(interval).withZone(DateTimeZone.UTC).toString()).isEqualTo(end);
    }

    @Test(dataProvider = "dateFormats")
    public void testFormatDateTime(final Date date, final String format, final String expectedString) {
        // given

        // when
        final String formattedTime = dateUtil.formatDateTime(date, format);

        // then
        assertThat(formattedTime).isNotNull().isEqualTo(expectedString);
    }

    @Test(dataProvider = "parseDateFormats")
    public void testParseTime(final String date, final String pattern) throws Exception {
        // given

        // when
        final Date time = dateUtil.parseTime(date, pattern);

        // then
        final SimpleDateFormat formatter = new SimpleDateFormat(pattern);
        formatter.setTimeZone(TimeZone.getTimeZone(TIME_ZONE_UTC));
        final Date expectedDate = formatter.parse(date);
        assertThat(time).isNotNull().isEqualTo(expectedDate);
    }
}
