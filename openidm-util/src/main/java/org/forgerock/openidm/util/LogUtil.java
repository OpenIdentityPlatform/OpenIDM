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
 * Copyright © 2012 ForgeRock AS. All rights reserved.
 */
package org.forgerock.openidm.util;

import org.slf4j.Logger;

public class LogUtil {

    public static enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR
    }

    public static LogLevel asLogLevel(String level) {
        return LogLevel.valueOf(level.toUpperCase());
    }

    /**
     * Logs a message at a specified level
     * @param logger logger to log with
     * @param level level to log at
     * @param message message to log
     */
    // NOTE: All separate implementations for efficiency reasons.
    // The methods in SLF4J are optimized for varying numbers of objects -- though it would be more elegant to have these
    // use the final Object[]-using method
    public static void logAtLevel(Logger logger, LogLevel level, String message) {
        switch (level) {
        case TRACE:
            logger.trace(message);
            break;
        case DEBUG:
            logger.debug(message);
            break;
        case INFO:
            logger.info(message);
            break;
        case WARN:
            logger.warn(message);
            break;
        case ERROR:
            logger.error(message);
            break;
        default:
            break;
        }
    }

    public static void logAtLevel(Logger logger, LogLevel level, String message, Object o1) {
        switch (level) {
        case TRACE:
            logger.trace(message, o1);
            break;
        case DEBUG:
            logger.debug(message, o1);
            break;
        case INFO:
            logger.info(message, o1);
            break;
        case WARN:
            logger.warn(message, o1);
            break;
        case ERROR:
            logger.error(message, o1);
            break;
        default:
            break;
        }
    }

    public static void logAtLevel(Logger logger, LogLevel level, String message, Object o1, Object o2) {
        switch (level) {
        case TRACE:
            logger.trace(message, o1, o2);
            break;
        case DEBUG:
            logger.debug(message, o1, o2);
            break;
        case INFO:
            logger.info(message, o1, o2);
            break;
        case WARN:
            logger.warn(message, o1, o2);
            break;
        case ERROR:
            logger.error(message, o1, o2);
            break;
        default:
            break;
        }
    }

    public static void logAtLevel(Logger logger, LogLevel level, String message, Object[] objects) {
        switch (level) {
        case TRACE:
            logger.trace(message, objects);
            break;
        case DEBUG:
            logger.debug(message, objects);
            break;
        case INFO:
            logger.info(message, objects);
            break;
        case WARN:
            logger.warn(message, objects);
            break;
        case ERROR:
            logger.error(message, objects);
            break;
        default:
            break;
        }
    }

}
