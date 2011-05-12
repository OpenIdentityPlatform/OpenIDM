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

import org.osgi.framework.BundleContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;

import org.forgerock.openidm.audit.AuditService;
import org.forgerock.openidm.config.InvalidException;
import org.forgerock.openidm.objset.BadRequestException;
import org.forgerock.openidm.objset.ConflictException;
import org.forgerock.openidm.objset.ForbiddenException;
import org.forgerock.openidm.objset.MethodNotAllowedException;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.PreconditionFailedException;
import org.forgerock.openidm.objset.Patch;
import org.forgerock.openidm.objset.PreconditionFailedException;

/**
 * Comma delimited audit logger
 * @author aegloff
 */
public class CSVAuditLogger implements AuditLogger {
    final static Logger logger = LoggerFactory.getLogger(CSVAuditLogger.class);

    public final static String CONFIG_LOG_LOCATION = "location";
    public final static String CONFIG_LOG_RECORD_DELIM = "recordDelimiter";
    
    File auditLogDir;
    String recordDelim;
    Map<String, FileWriter> fileWriters = new HashMap<String, FileWriter>();

    public void setConfig(Map config, BundleContext ctx) throws InvalidException {
        String location = null;
        try {
            location = (String) config.get(CONFIG_LOG_LOCATION);
            auditLogDir = new File(location);
            auditLogDir.mkdirs();
            recordDelim = (String) config.get(CONFIG_LOG_RECORD_DELIM);
            if (recordDelim == null) {
                recordDelim = "";
            }
            recordDelim += System.getProperty("line.separator");
        } catch (Exception ex) {
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
     * {@inheritdoc}
     */
    public Map<String, Object> read(String fullId) throws ObjectSetException {
        // TODO
        return new HashMap();
    }

    /**
     * Currently not supported.
     * 
     * {@inheritdoc}
     */
    public Map<String, Object> query(String fullId, Map<String, Object> params) throws ObjectSetException {
        // TODO
        return new HashMap();
    }
    
    /**
     * {@inheritdoc}
     */
    public void create(String fullId, Map<String, Object> obj) throws ObjectSetException {
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
                }
            }
            fileWriter = getWriter(type, auditFile);

            String key = null;
            Iterator iter = fieldOrder.iterator();
            while(iter.hasNext()) {
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
        } catch (Exception ex) {
            throw new BadRequestException(ex);
        } 
    }
    
    private void writeHeaders(Collection<String> fieldOrder, FileWriter fileWriter) 
            throws IOException {
        Iterator iter = fieldOrder.iterator();
        while(iter.hasNext()) {
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
        synchronized(fileWriters) {
            FileWriter existingWriter = fileWriters.get(type);
            if (existingWriter == null) {
                existingWriter = new FileWriter(auditFile, true);
                fileWriters.put(type, existingWriter);
            }
            return existingWriter;
        }
    }
    
    /**
     * Audit service does not support changing audit entries.
     */
    public void update(String fullId, String rev, Map<String, Object> obj) throws ObjectSetException {
        throw new MethodNotAllowedException("Not allowed on audit service");
    }

    /**
     * Audit service currently does not support deleting audit entries.
     */ 
    public void delete(String fullId, String rev) throws ObjectSetException {
        throw new MethodNotAllowedException("Not allowed on audit service");
    }

    /**
     * Audit service does not support changing audit entries.
     */
    public void patch(String id, String rev, Patch patch) throws ObjectSetException {
        throw new MethodNotAllowedException("Not allowed on audit service");
    }
}
