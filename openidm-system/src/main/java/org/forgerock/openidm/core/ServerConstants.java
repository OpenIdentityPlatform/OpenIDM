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
package org.forgerock.openidm.core;

/**
 * This class defines a set of constants that may be referenced throughout the
 * Identity Server source.
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
public class ServerConstants {

    /**
     * The end-of-line character for this platform.
     */
    public static final String EOL = System.getProperty("line.separator");


    /**
     * The date format string that will be used to construct and parse dates
     * represented in a form like UTC time, but using the local time zone.
     */
    public static final String DATE_FORMAT_COMPACT_LOCAL_TIME =
            "yyyyMMddHHmmss";


    /**
     * The date format string that will be used to construct and parse dates
     * represented using generalized time.  It is assumed that the provided date
     * formatter will be set to UTC.
     */
    public static final String DATE_FORMAT_GENERALIZED_TIME =
            "yyyyMMddHHmmss.SSS'Z'";


    /**
     * The date format string that will be used to construct and parse dates
     * represented using generalized time.  It is assumed that the provided date
     * formatter will be set to UTC.
     */
    public static final String DATE_FORMAT_LOCAL_TIME =
            "dd/MMM/yyyy:HH:mm:ss Z";


    /**
     * The date format string that will be used to construct and parse dates
     * represented using iso8601 time.
     */
    public static final String DATE_FORMAT_ISO8601_TIME =
            "yyyy-MM-dd'T'HH:mm:ssZ";

    /**
     * The date format string that will be used to construct and parse dates
     * represented using generalized time with a four-digit year.  It is assumed
     * that the provided date formatter will be set to UTC.
     */
    public static final String DATE_FORMAT_GMT_TIME =
            "yyyyMMddHHmmss'Z'";


    /**
     * The date format string that will be used to construct and parse dates
     * represented using generalized time with a two-digit year.  It is assumed
     * that the provided date formatter will be set to UTC.
     */
    public static final String DATE_FORMAT_UTC_TIME =
            "yyMMddHHmmss'Z'";


    /**
     * The name of the time zone for universal coordinated time (UTC).
     */
    public static final String TIME_ZONE_UTC = "UTC";

    /**
     * The value that will be used for the vendorName attribute in the root DSE.
     */
    public static final String SERVER_VENDOR_NAME = "ForgeRock AS.";

    /**
     * The name of the default cryptography cipher that will be used.
     */
    public static final String SECURITY_CRYPTOGRAPHY_DEFAULT_CIPHER = "AES/CBC/PKCS5Padding";


    /**
     * The default location relative to the openidm root of the boot properties file.
     * Override by setting system property {@code CONFIG_BOOT_FILE_LOCATION}
     */
    public static final String DEFAULT_BOOT_FILE_LOCATION = "conf/boot/boot.properties";
    
    /**
     * The name of the system property that can be used to specify the path to the
     * server root.
     */
    public static final String PROPERTY_SERVER_ROOT =
            "openidm.system.server.root";

    /**
     * The name of the system property that can be used to specify the location
     * of the boot properties file
     */
    public static final String PROPERTY_BOOT_FILE_LOCATION = "openidm.boot.file";
    

    /**
     * The name of the system property that can be used to specify the environment where
     * the server is deployed to.
     */
    public static final String PROPERTY_SERVER_ENVIRONMENT =
            "openidm.system.server.environment";

    /**
     * The name of the system property that can be used to enable
     * for the debug on startup.
     */
    public static final String PROPERTY_DEBUG_ENABLE =
            "openidm.system.debug.enable";

    /**
     * The column at which to wrap long lines of output in the command-line tools.
     */
    public static final int MAX_LINE_WIDTH;

    static {
        int columns = 80;
        try {
            String s = System.getenv("COLUMNS");
            if (s != null) {
                columns = Integer.parseInt(s);
            }
        } catch (Exception e) {
            // Do nothing.
        }
        MAX_LINE_WIDTH = columns - 1;
    }
}
