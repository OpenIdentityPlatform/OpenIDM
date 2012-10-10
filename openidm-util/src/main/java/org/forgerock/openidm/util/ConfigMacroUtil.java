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
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.forgerock.json.fluent.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigMacroUtil {
    private final static Logger logger = LoggerFactory.getLogger(ConfigMacroUtil.class);
    private final static DateUtil DATE_UTIL = DateUtil.getDateUtil("UTC");

    /**
     * Expands any interpolation contained within the JsonValue object in-place.
     * @param json JsonValue to parse for macros
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
     * Start the string parsing. If base is not a string, will return null
     * @param base base JsonValue object to begin parsing from
     * @return a string with any interpolation expanded or null if base is not a string
     */
    private static String parse(JsonValue base) {
        if (!base.isString()) {
            return null;
        }

        return buildString(base.asString());
    }

    /**
     * Begins building the string from interpolation and normal string contents
     * @param str string to interpolate from
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

        int last_end = 0;
        for (Integer[] pair : confirmedLocations) {
            int start = pair[0];
            int length = pair[1];
            int end = start + length;
            builder.append(str.substring(last_end, start));
            builder.append(interpolate(str.substring(start, end)));
            last_end = pair[0] + pair[1];
        }

        return builder.toString();
    }

    /**
     * Identifies any possible interpolation locations (begins by looking for "${")
     * @param str string to look through for interpolation sites
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
     * Confirm interpolation sites by looking for a closing brace
     * @param str string to confirm interpolation sites from
     * @param possibleLocations list of string indicies indicating possible interpolation beginnings
     * @return list paired integers containing (starting location, length of interpolation string)
     */
    private static List<Integer[]> confirmedLocations(String str, List<Integer> possibleLocations) {
        List<Integer[]> confirmedLocations = new ArrayList<Integer[]>();

        Integer[] last_pair = { -1 , -1 };
        for (Integer start : possibleLocations) {
            int length = 0;

            // Ignore any escaped \${}
            if (start != 0 && str.charAt(start-1) == '\\') {
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

            // Don't add overlapping pairs -- this will keep "${ ${hi} }" from being interpolated
            // technically it will wind up "${ ${hi}"
            if ((last_pair[0] + last_pair[1] < start) && found) {
                Integer[] pair = { start, length };
                confirmedLocations.add(pair);
            }
        }

        return confirmedLocations;
    }

    /**
     * Interpolates the macros contained in the interpolation braces
     * <br><br>
     * <b>NOTE:</b> for ease of tokenization, this expects each token to have a space between each component
     * <b><i>e.g.</i></b> "Time.now + 1d" rather than "Time.now+1d"
     * <br><br>
     * <b>TODO:</b> Proper tokenizing
     * @param str interpolation string
     * @return interpolated string
     */
    private static String interpolate(String str) {
        String toInterpolate = str.substring(2, str.length() - 1); // Strip ${ and }
        List<String> tokens = Arrays.asList(toInterpolate.split(" "));

        StringBuilder builder = new StringBuilder();
        Iterator<String> iter = tokens.iterator();
        while(iter.hasNext()) {
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
     * @param tokens list of tokens
     * @param iter iterator used to iterate over the list of tokens
     * @return string containing the interpolated time token
     */
    private static String handleTime(List<String> tokens, Iterator<String> iter) {
        Calendar cal = Calendar.getInstance();

        // Add some amount
        if (iter.hasNext()) {
            String operationToken = iter.next();
            if (operationToken.equals("+") || operationToken.equals("-")) {
                if (iter.hasNext()) {
                    String quantityToken = iter.next(); // Get the magnitude to add or subtract
                    int timeMagnitude = getTimeMagnitude(quantityToken);
                    String valueString = quantityToken.substring(0, quantityToken.length()-1);
                    int value = Integer.parseInt(valueString);

                    if (operationToken.equals("-")) {
                        value *= -1;
                    }

                    cal.add(timeMagnitude, value);
                } else {
                    logger.warn("Token '{}' not followed by a quantity", operationToken);
                }
            } else {
                logger.warn("Invalid token '{}', must be operator '+' or '-'", operationToken);
            }
        }

        return DATE_UTIL.formatDateTime(cal.getTime());
    }

    /**
     * Defines the magnitudes that can be added to the timestamp
     * @param token token of form "[number][magnitude]" (ex. "1d")
     * @return integer indicating the magnitude of the date for the calendar system
     */
    private static int getTimeMagnitude(String token) {
        char c = token.charAt(token.length()-1);

        int mag;
        switch (c) {
        case 's':
            mag = Calendar.SECOND;
            break;
        case 'm':
            mag = Calendar.MINUTE;
            break;
        case 'h':
            mag = Calendar.HOUR;
            break;
        case 'd':
            mag = Calendar.DATE;
            break;
        case 'M':
            mag = Calendar.MONTH;
            break;
        case 'y':
            mag = Calendar.YEAR;
            break;
        default:
            logger.warn("Invalid date magnitude: {}. Defaulting to seconds.", c);
            mag =  Calendar.SECOND;
            break;
        }

        return mag;
    }
}
