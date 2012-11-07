/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.audit.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.osgi.framework.BundleContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.forgerock.openidm.audit.AuditService;
import org.forgerock.openidm.config.InvalidException;
import org.forgerock.openidm.objset.BadRequestException;
import org.forgerock.openidm.objset.ConflictException;
import org.forgerock.openidm.objset.ForbiddenException;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.PreconditionFailedException;
import org.forgerock.openidm.objset.Patch;
import org.forgerock.openidm.objset.PreconditionFailedException;
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Name;
import org.forgerock.openidm.smartevent.Publisher;

/**
 * Comma delimited audit logger
 *
 * @author aegloff
 */
public class CSVAuditLogger implements AuditLogger {
    final static Logger logger = LoggerFactory.getLogger(CSVAuditLogger.class);

    public final static String CONFIG_LOG_LOCATION = "location";
    public final static String CONFIG_LOG_RECORD_DELIM = "recordDelimiter";
    
    /**
     * Event names for monitoring audit behavior
     */
    public static final Name EVENT_AUDIT_CREATE = Name.get("openidm/internal/audit/csv/create");

    File auditLogDir;
    String recordDelim;
    final Map<String, FileWriter> fileWriters = new HashMap<String, FileWriter>();

    public void setConfig(Map config, BundleContext ctx) throws InvalidException {
        String location = null;
        try {
            location = (String) config.get(CONFIG_LOG_LOCATION);
            auditLogDir = IdentityServer.getFileForWorkingPath(location);
            logger.info("Audit logging to: {}", auditLogDir.getAbsolutePath());
            auditLogDir.mkdirs();
            recordDelim = (String) config.get(CONFIG_LOG_RECORD_DELIM);
            if (recordDelim == null) {
                recordDelim = "";
            }
            recordDelim += ServerConstants.EOL;
        } catch (Exception ex) {
            logger.error("ERROR - Configured CSV file location must be a directory and {} is invalid.", auditLogDir.getAbsolutePath(), ex);
            throw new InvalidException("Configured CSV file location must be a directory and '" + location
                    + "' is invalid " + ex.getMessage(), ex);
        }
    }
    
    public void cleanup() {
        for (Map.Entry<String, FileWriter> entry : fileWriters.entrySet()) {
            try {
                FileWriter fileWriter = entry.getValue();
                if (fileWriter != null) {
                    fileWriter.close();
                }
            } catch (Exception ex) {
                logger.info("File writer close reported failure ", ex);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> read(String fullId) throws ObjectSetException {
        // TODO
        return new HashMap<String,Object>();
    }

    /**
     * Currently not supported.
     * <p/>
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> query(String fullId, Map<String, Object> params) throws ObjectSetException {
        // TODO
        return new HashMap<String,Object>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void create(String fullId, Map<String, Object> obj) throws ObjectSetException {
        EventEntry measure = Publisher.start(EVENT_AUDIT_CREATE, obj, null);
        try {
            createImpl(fullId, obj);
        } finally {
            measure.end();
        }
    }
    
    
    private void createImpl(String fullId, Map<String, Object> obj) throws ObjectSetException {
        // TODO: replace ID handling utility
        String[] split = AuditServiceImpl.splitFirstLevel(fullId);
        String type = split[0];

        // TODO: optimize buffered, cached writing
        FileWriter fileWriter = null;
        try {
            // TODO: Optimize ordering etc.
            Collection<String> fieldOrder =
                    new TreeSet<String>(Collator.getInstance());
            fieldOrder.addAll(obj.keySet());

            File auditFile = new File(auditLogDir, type + ".csv");
            // Create header if creating a new file
            if (!auditFile.exists()) {
                synchronized (this) {
                    File auditTmpFile = new File(auditLogDir, type + ".tmp");
                    boolean created = auditTmpFile.createNewFile();
                    if (created) {
                        FileWriter tmpFileWriter = new FileWriter(auditTmpFile, true);
                        writeHeaders(fieldOrder, tmpFileWriter);
                        tmpFileWriter.close();
                        auditTmpFile.renameTo(auditFile);
                    }
                    resetWriter(type);
                }
            }
            fileWriter = getWriter(type, auditFile);

            String key = null;
            Iterator iter = fieldOrder.iterator();
            while (iter.hasNext()) {
                key = (String) iter.next();
                Object value = obj.get(key);
                fileWriter.append("\"");
                if (value != null) {
                    String rawStr = value.toString();
                    // Escape quotes with double quotes
                    String escapedStr = rawStr.replaceAll("\"", "\"\"");
                    fileWriter.append(escapedStr);
                }
                fileWriter.append("\"");
                if (iter.hasNext()) {
                    fileWriter.append(",");
                }
            }
            fileWriter.append(recordDelim);
            fileWriter.flush();
        } catch (Exception ex) {
            throw new BadRequestException(ex);
        }
    }

    private void writeHeaders(Collection<String> fieldOrder, FileWriter fileWriter)
            throws IOException {
        Iterator iter = fieldOrder.iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            fileWriter.append("\"");
            String escapedStr = key.replaceAll("\"", "\"\"");
            fileWriter.append(escapedStr);
            fileWriter.append("\"");
            if (iter.hasNext()) {
                fileWriter.append(",");
            }
        }
        fileWriter.append(recordDelim);
    }

    private FileWriter getWriter(String type, File auditFile) throws IOException {
        // TODO: optimize synchronization strategy
        synchronized (fileWriters) {
            FileWriter existingWriter = fileWriters.get(type);
            if (existingWriter == null) {
                existingWriter = new FileWriter(auditFile, true);
                fileWriters.put(type, existingWriter);
            }
            return existingWriter;
        }
    }
    
    // This should only be called if it is known that 
    // the writer is invalid for use or no thread has obtained it / is using it
    // In other words, it does not synchronize on the use of the writer
    private void resetWriter(String type) {
        FileWriter existingWriter = null;
        // TODO: optimize synchronization strategy
        synchronized (fileWriters) {
            existingWriter = fileWriters.remove(type);
        }
        if (existingWriter != null) {
            // attempt clean-up close
            try {
                existingWriter.close();
            } catch (Exception ex) {
                // Debug level as the writer is expected to potentially be invalid
                logger.debug("File writer close in resetWriter reported failure ", ex);
            }
        }
    }

    /**
     * Audit service does not support changing audit entries.
     */
    @Override
    public void update(String fullId, String rev, Map<String, Object> obj) throws ObjectSetException {
        throw new ForbiddenException("Not allowed on audit service");
    }

    /**
     * Audit service currently does not support deleting audit entries.
     */
    @Override
    public void delete(String fullId, String rev) throws ObjectSetException {
        throw new ForbiddenException("Not allowed on audit service");
    }

    /**
     * Audit service does not support changing audit entries.
     */
    @Override
    public void patch(String id, String rev, Patch patch) throws ObjectSetException {
        throw new ForbiddenException("Not allowed on audit service");
    }

    /**
     * Audit service does not support actions on audit entries.
     */
    @Override
    public Map<String, Object> action(String fullId, Map<String, Object> params) throws ObjectSetException {
        throw new ForbiddenException("Not allowed on audit service");
    }
}
