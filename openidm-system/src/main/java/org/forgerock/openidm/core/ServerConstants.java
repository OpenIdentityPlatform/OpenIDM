/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE
 * or https://OpenDS.dev.java.net/OpenDS.LICENSE.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE.  If applicable,
 * add the following below this CDDL HEADER, with the fields enclosed
 * by brackets "[]" replaced with your own identifying information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2006-2010 Sun Microsystems, Inc.
 *      Portions Copyright 2010-2015 ForgeRock AS
 */
package org.forgerock.openidm.core;

/**
 * This class defines a set of constants that may be referenced throughout the
 * Identity Server source.
 * <p/>
 * Based on OpenDJ ServerConstants
 */
public final class ServerConstants {

    static {
        System.out.println(getDisplayVersion());
    }

    private ServerConstants() {
    }

    public static String getVersion() {
        return VERSION;
    }

    static String getInjectedRevision() {
        return SCM_REVISION;
    }

    public static String getRevision() {
        final String injectedRevision = getInjectedRevision();
        return null == injectedRevision || "0".equals(injectedRevision)
                ? "unknown"
                : injectedRevision;
    }

    public static String getBuild() {
        return "";
    }

    public static String getBranch() {
        return "";
    }

    public static String getDisplayVersion() {
        StringBuilder sb = new StringBuilder("OpenIDM version \"")
                .append(getVersion())
                .append("\" (revision: ")
                .append(getRevision())
                .append(")");

        if (null != getBuild() && !getBuild().isEmpty() && !"null".equals(getBuild())) {
            sb.append(' ').append(getBuild());
        }
        if (null != getBranch() && !getBranch().isEmpty() && !"null".equals(getBranch())) {
            sb.append(' ').append(getBranch());
        }
        return sb.toString();
    }

    /**
     * OpenIDM version number.
     */
    public static final String VERSION = "unknown";

    /**
     * OpenIDM SCM revision number.
     */
    public static final String SCM_REVISION = "unknown";

    /**
     * The end-of-line character for this platform.
     */
    public static final String EOL = System.getProperty("line.separator");

    /**
     * The date format string that will be used to construct and parse dates
     * represented in a form like UTC time, but using the local time zone.
     */
    public static final String DATE_FORMAT_COMPACT_LOCAL_TIME = "yyyyMMddHHmmss";

    /**
     * The date format string that will be used to construct and parse dates
     * represented using generalized time. It is assumed that the provided date
     * formatter will be set to UTC.
     */
    public static final String DATE_FORMAT_GENERALIZED_TIME = "yyyyMMddHHmmss.SSS'Z'";

    /**
     * The date format string that will be used to construct and parse dates
     * represented using generalized time. It is assumed that the provided date
     * formatter will be set to UTC.
     */
    public static final String DATE_FORMAT_LOCAL_TIME = "dd/MMM/yyyy:HH:mm:ss Z";

    /**
     * The date format string that will be used to construct and parse dates
     * represented using iso8601 time.
     */
    public static final String DATE_FORMAT_ISO8601_TIME = "yyyy-MM-dd'T'HH:mm:ssZ";

    /**
     * The date format string that will be used to construct and parse dates
     * represented using iso8601 time.
     */
    public static final String DATE_FORMAT_ISO8601_TIME_MILLISECOND = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    /**
     * The date format string that will be used to construct and parse dates
     * represented using generalized time with a four-digit year. It is assumed
     * that the provided date formatter will be set to UTC.
     */
    public static final String DATE_FORMAT_GMT_TIME = "yyyyMMddHHmmss'Z'";

    /**
     * The date format string that will be used to construct and parse dates
     * represented using generalized time with a two-digit year. It is assumed
     * that the provided date formatter will be set to UTC.
     */
    public static final String DATE_FORMAT_UTC_TIME = "yyMMddHHmmss'Z'";

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
     * The name of the default cryptography hashing algorithm that will be used.
     */
    public static final String SECURITY_CRYPTOGRAPHY_DEFAULT_HASHING_ALGORITHM = "SHA-256";

    /**
     * The default location relative to the openidm root of the boot properties
     * file. Override by setting system property
     * {@code CONFIG_BOOT_FILE_LOCATION}
     */
    public static final String DEFAULT_BOOT_FILE_LOCATION = "conf/boot/boot.properties";

    /**
     * Configuration property naming the Factory PID in the configuration
     * dictionary. The property's value is of type <code>String</code>.
     *
     * @since 1.1
     */
    public static final String CONFIG_FACTORY_PID = "config.factory-pid";

    /**
     * The name of the system property that can be used to specify the path to
     * the server root.
     */
    public static final String PROPERTY_SERVER_ROOT = "openidm.system.server.root";

    /**
     * The name of the system property that can be used to specify the location
     * of the boot properties file.
     */
    public static final String PROPERTY_BOOT_FILE_LOCATION = "openidm.boot.file";

    /**
     * The name of the system property that can be used to specify the
     * environment where the server is deployed to.
     */
    public static final String PROPERTY_SERVER_ENVIRONMENT = "openidm.system.server.environment";

    /**
     * The name of the system property that can be used to enable for the debug
     * on startup.
     */
    public static final String PROPERTY_DEBUG_ENABLE = "openidm.system.debug.enable";

    /**
     * The column at which to wrap long lines of output in the command-line
     * tools.
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

    public static final String ROUTER_PREFIX = "openidm.router.prefix";

    /**
     * Query input value for the QUERY_ID input key.
     *
     * Querying with this query id results in querying all object IDs for a
     * given ObjectSet.
     *
     * All ObjectSets supporting query must support the query corresponding to
     * this ID
     */
    public final static String QUERY_ALL_IDS = "query-all-ids";

    /**
     * System populated query input key.
     *
     * Key available for token substitution inside the query expression,
     * identifying the resource queried by name. Example use: select * from
     * ${_resource} where ...
     */
    public final static String RESOURCE_NAME = "_resource";

    public static final String LAUNCHER_INSTALL_LOCATION = "launcher.install.location";
    public static final String LAUNCHER_INSTALL_URL = "launcher.install.url";
    public static final String LAUNCHER_WORKING_LOCATION = "launcher.working.location";
    public static final String LAUNCHER_WORKING_URL = "launcher.working.url";
    public static final String LAUNCHER_PROJECT_LOCATION = "launcher.project.location";
    public static final String LAUNCHER_PROJECT_URL = "launcher.project.url";
}
