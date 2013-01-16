/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.logging;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Attach log listeners to log reader services to re-direct to slf4j
 */
public class LogServiceTracker extends ServiceTracker<LogReaderService, InternalLogListener> {

    /**
     * Setup logging for the {@link LogServiceTracker}.
     */
    final static Logger logger = LoggerFactory.getLogger(LogServiceTracker.class);

    public LogServiceTracker(BundleContext context) {
        super(context, LogReaderService.class, null);
    }

    @Override
    public InternalLogListener addingService(ServiceReference<LogReaderService> reference) {
        logger.trace("Add LogListener to LogReaderService");
        return new InternalLogListener(context.getService(reference));
    }

    @Override
    public void removedService(ServiceReference<LogReaderService> reference,
            InternalLogListener service) {
        logger.trace("Remove LogListener from LogReaderService");
        service.release();
        super.removedService(reference, service);
    }
}

class InternalLogListener implements LogListener {

    /**
     * Setup logging for OSGi log entries.
     */
    final static Logger defaultEntryLogger = LoggerFactory
            .getLogger("org.forgerock.openidm.Framework");

    private final LogReaderService service;

    InternalLogListener(LogReaderService reader) {
        service = reader;
        service.addLogListener(this);

        // This dumps the existing log entries.
        // These entries may appear out of chronological order
        // if services already started logging to slf4j directly
        java.util.Enumeration entries = reader.getLog();
        while (entries.hasMoreElements()) {
            logged((LogEntry) entries.nextElement());
        }
    }

    public void release() {
        service.removeLogListener(this);
    }

    /**
     * Log the OSGi entry using the slf4j logger
     * 
     * @param entry
     */
    public void logged(LogEntry entry) {

        StringBuilder logMessage = new StringBuilder("Bundle: ");
        logMessage.append(entry.getBundle());
        if (entry.getServiceReference() != null) {
            logMessage.append(" - ");
            logMessage.append(entry.getServiceReference());
        }
        logMessage.append(" ");
        logMessage.append(entry.getMessage());

        Throwable ex = entry.getException();

        switch (entry.getLevel()) {
        case LogService.LOG_ERROR: {
            defaultEntryLogger.error(logMessage.toString(), ex);
            break;
        }
        case LogService.LOG_WARNING: {
            defaultEntryLogger.warn(logMessage.toString(), ex);
            break;
        }
        case LogService.LOG_INFO: {
            defaultEntryLogger.info(logMessage.toString(), ex);
            break;
        }
        case LogService.LOG_DEBUG: {
            defaultEntryLogger.debug(logMessage.toString(), ex);
            break;
        }
        default: {
            defaultEntryLogger.warn("Unknown OSGi log level [" + entry.getLevel() + "] for"
                    + logMessage.toString(), ex);
        }
        }
    }

}
