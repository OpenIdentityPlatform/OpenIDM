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

import org.forgerock.openidm.core.ServerConstants;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class DateUtil {

    private static final SimpleDateFormat ISO8601_FORMAT = new SimpleDateFormat(ServerConstants.DATE_FORMAT_ISO8601_TIME_MILLISECOND);

    /**
     * Generate the current time string in ISO8601 format with milliseconds
     * 2011-09-09T14:58:17.6+02:00
     * 2011-09-09T14:58:17.65+02:00
     * 2011-09-09T14:58:17.654+02:00
     *
     * @return
     */
    public static String now() {
        // format in (almost) ISO8601 format
        String dateStr = ISO8601_FORMAT.format(new Date());

        // remap the timezone from 0000 to 00:00
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
