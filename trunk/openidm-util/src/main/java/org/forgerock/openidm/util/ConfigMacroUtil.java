/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package org.forgerock.openidm.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.core.ServerConstants;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Minutes;
import org.joda.time.Months;
import org.joda.time.ReadablePeriod;
import org.joda.time.Seconds;
import org.joda.time.Years;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConfigMacroUtil {

    /**
     * Setup logging for the {@link ConfigMacroUtil}.
     */
    private final static Logger logger = LoggerFactory.getLogger(ConfigMacroUtil.class);

    private final static DateUtil DATE_UTIL = DateUtil.getDateUtil(ServerConstants.TIME_ZONE_UTC);

    private ConfigMacroUtil() {
    }

    /**
     * Expands any interpolation contained within the JsonValue object in-place.
     *
     * @param json
     *            JsonValue to parse for macros
     */
    public static void expand(JsonValue json) {
        Iterator<String> iter = json.keys().iterator();
        while (iter.hasNext()) {
            String key = iter.next();

            String expanded = parse(json.get(key));
            if (expanded != null) {
                json.put(key, expanded);
            }
        }
    }

    /**
     * Start the string parsing. If base is not a string, will return null.
     *
     * @param base
     *            base JsonValue object to begin parsing from
     * @return a string with any interpolation expanded or null if base is not a
     *         string
     */
    private static String parse(JsonValue base) {
        if (!base.isString()) {
            return null;
        }

        return buildString(base.asString());
    }

    /**
     * Begins building the string from interpolation and normal string contents.
     *
     * @param str
     *            string to interpolate from
     * @return a string after interpolation
     */
    public static String buildString(String str) {
        StringBuilder builder = new StringBuilder();

        List<Integer> possibleLocations = possibleLocations(str);
        if (possibleLocations.isEmpty()) {
            return null;
        }

        List<Integer[]> confirmedLocations = confirmedLocations(str, possibleLocations);
        if (confirmedLocations.isEmpty()) {
            return null;
        }

        int lastEnd = 0;
        for (Integer[] pair : confirmedLocations) {
            int start = pair[0];
            int length = pair[1];
            int end = start + length;
            builder.append(str.substring(lastEnd, start));
            builder.append(interpolate(str.substring(start, end)));
            lastEnd = pair[0] + pair[1];
        }

        return builder.toString();
    }

    /**
     * Identifies any possible interpolation locations (begins by looking for
     * "${").
     *
     * @param str
     *            string to look through for interpolation sites
     * @return list of indices where the interpolation sites begin
     */
    private static List<Integer> possibleLocations(String str) {
        List<Integer> possibleLocations = new ArrayList<Integer>();

        int lastIndex = -1;
        int index;
        while ((index = str.indexOf("${", lastIndex + 1)) >= 0) {
            if (lastIndex == index) {
                break;
            }
            possibleLocations.add(index);
            lastIndex = index;
        }

        return possibleLocations;
    }

    /**
     * Confirm interpolation sites by looking for a closing brace.
     *
     * @param str
     *            string to confirm interpolation sites from
     * @param possibleLocations
     *            list of string indicies indicating possible interpolation
     *            beginnings
     * @return list paired integers containing (starting location, length of
     *         interpolation string)
     */
    private static List<Integer[]> confirmedLocations(String str, List<Integer> possibleLocations) {
        List<Integer[]> confirmedLocations = new ArrayList<Integer[]>();

        Integer[] lastPair = { -1, -1 };
        for (Integer start : possibleLocations) {
            int length = 0;

            // Ignore any escaped \${}
            if (start != 0 && str.charAt(start - 1) == '\\') {
                continue;
            }

            // Determine the length and existence of a ${} block
            boolean found = false;
            for (int i = start; i < str.length(); i++) {
                length += 1;
                if (str.charAt(i) == '}') {
                    found = true;
                    break;
                }
            }

            // Don't add overlapping pairs -- this will keep "${ ${hi} }" from
            // being interpolated
            // technically it will wind up "${ ${hi}"
            if ((lastPair[0] + lastPair[1] < start) && found) {
                Integer[] pair = { start, length };
                confirmedLocations.add(pair);
            }
        }

        return confirmedLocations;
    }

    /**
     * Interpolates the macros contained in the interpolation braces.
     *
     * <b>NOTE:</b> for ease of tokenization, this expects each token to have a
     * space between each component <b><i>e.g.</i></b> "Time.now + 1d" rather
     * than "Time.now+1d" <br>
     * <br>
     * <b>TODO:</b> Proper tokenizing
     *
     * @param str
     *            interpolation string
     * @return interpolated string
     */
    private static String interpolate(String str) {
        String toInterpolate = str.substring(2, str.length() - 1); // Strip ${
                                                                   // and }
        List<String> tokens = Arrays.asList(toInterpolate.split(" "));

        StringBuilder builder = new StringBuilder();
        Iterator<String> iter = tokens.iterator();
        while (iter.hasNext()) {
            String token = iter.next();

            if (token.equals("Time.now")) {
                builder.append(handleTime(tokens, iter));
            } else {
                logger.warn("Unrecognized token: {}", token);
                builder.append(token);
            }
        }

        return builder.toString();
    }

    /**
     * Handles the Time.now macro
     *
     * @param tokens
     *            list of tokens
     * @param iter
     *            iterator used to iterate over the list of tokens
     * @return string containing the interpolated time token
     */
    private static String handleTime(List<String> tokens, Iterator<String> iter) {
        DateTime dt = new DateTime();

        // Add some amount
        if (iter.hasNext()) {
            String operationToken = iter.next();
            if (operationToken.equals("+") || operationToken.equals("-")) {
                if (iter.hasNext()) {
                    String quantityToken = iter.next(); // Get the magnitude to
                                                        // add or subtract

                    ReadablePeriod period = getTimePeriod(quantityToken);

                    if (operationToken.equals("-")) {
                        dt = dt.minus(period);
                    } else {
                        dt = dt.plus(period);
                    }
                } else {
                    logger.warn("Token '{}' not followed by a quantity", operationToken);
                }
            } else {
                logger.warn("Invalid token '{}', must be operator '+' or '-'", operationToken);
            }
        }

        return DATE_UTIL.formatDateTime(dt);
    }

    /**
     * Defines the magnitudes that can be added to the timestamp
     *
     * @param token
     *            token of form "[number][magnitude]" (ex. "1d")
     * @return integer indicating the magnitude of the date for the calendar
     *         system
     */
    public static ReadablePeriod getTimePeriod(String token) {
        String valString = token.substring(0, token.length() - 1);
        int value = Integer.parseInt(valString);
        char mag = token.charAt(token.length() - 1);

        ReadablePeriod period;

        switch (mag) {
        case 's':
            period = Seconds.seconds(value);
            break;
        case 'm':
            period = Minutes.minutes(value);
            break;
        case 'h':
            period = Hours.hours(value);
            break;
        case 'd':
            period = Days.days(value);
            break;
        case 'M':
            period = Months.months(value);
            break;
        case 'y':
            period = Years.years(value);
            break;
        default:
            logger.warn("Invalid date magnitude: {}. Defaulting to seconds.", mag);
            period = Seconds.seconds(value);
            break;
        }

        return period;
    }
}
