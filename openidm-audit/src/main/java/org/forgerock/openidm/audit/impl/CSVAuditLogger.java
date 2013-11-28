/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2013 ForgeRock AS. All rights reserved.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.config.InvalidException;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.objset.BadRequestException;
import org.forgerock.openidm.objset.ForbiddenException;
import org.forgerock.openidm.objset.InternalServerErrorException;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.Patch;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Name;
import org.forgerock.openidm.smartevent.Publisher;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Comma delimited audit logger
 *
 * @author aegloff
 */
public class CSVAuditLogger implements AuditLogger {
    final static Logger logger = LoggerFactory.getLogger(CSVAuditLogger.class);

    public final static String CONFIG_LOG_LOCATION = "location";
    public final static String CONFIG_LOG_RECORD_DELIM = "recordDelimiter";
    
    private static Object lock = new Object();
    
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
        Map<String, Object> result = new HashMap<String, Object>();
        String[] split = AuditServiceImpl.splitFirstLevel(fullId);
        String type = split[0];
        String id = split[1];
        
        try {
            List<Map<String, Object>> entriesList = new ArrayList<Map<String, Object>>();
            List<Map<String, Object>> entryList = getEntryList(type); 
            if (entryList == null) {
                throw new NotFoundException(type + " audit log not found");
            }
            for (Map<String, Object> entry : entryList) {
                if (id == null) {
                    entriesList.add(AuditServiceImpl.formatLogEntry(entry, type));
                } else if (id.equals(entry.get("_id"))) {
                    return AuditServiceImpl.formatLogEntry(entry, type);
                }
            }
            if (id != null) {
                throw new NotFoundException("Audit log entry with id " + id + " not found");
            }
            result.put("entries", entriesList);
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
        return result;
    }
    
    /**
     * Parses a string of header fields and returns them as a list.
     * 
     * @param fieldsString a string containing header fields
     * @return a list of header fields.
     */
    private List<String> parseFields(String fieldsString) {
        List<String> list = new ArrayList();
        String [] fields = fieldsString.substring(0, fieldsString.length() - 1).split(",");
        for (String field : fields) {
            list.add(field);
        }
        return list;
    }
    
    /**
     * Parser the csv file corresponding the the specified type (recon, activity, etc) and returns a list
     * of all entries in it.
     * 
     * @param type the audit log type
     * @return  A list of audit log entries
     * @throws Exception
     */
    private List<Map<String, Object>> getEntryList(String type) throws Exception {
        try {
            List<Map<String, Object>> entriesList = new ArrayList<Map<String, Object>>();
            File auditFile = getAuditLogFile(type);
            if (auditFile.exists()) {

                BufferedReader reader = new BufferedReader(new FileReader(auditFile));
                List<String> fields = parseFields(reader.readLine());
                String entryString = reader.readLine();
                while (entryString != null) {
                    Map<String, Object> entryMap = new HashMap<String, Object>();
                    String [] rawValues = entryString.substring(0, entryString.length() - 1).split(",");
                    List<String> values = new ArrayList<String>();
                    StringBuilder sb = null;
                    // Loop through values looking for complete values, partial values, empty values
                    // trim starting " and ending " before adding to list
                    for (String value : rawValues) {
                        if (sb != null) {
                            sb.append(",");
                        } else {
                            sb = new StringBuilder();
                        }
                        if (value.equals("\"\"")) {
                            // empty value
                            values.add("");
                            sb = null;
                        } else if (value.endsWith("\"") && !value.endsWith("\"\"")) {
                            // complete value (may be the end of a partial value);
                            sb.append(value);
                            values.add(sb.toString().substring(1, sb.toString().lastIndexOf("\"")));
                            sb = null;
                        } else {
                            // partial value
                            sb.append(value);
                        }
                    }
                    if (values.size() != fields.size()) {
                        throw new InternalServerErrorException("Error parsing entries from " + type + " log");
                    }
                    for (int i = 0; i < fields.size(); i++) {
                        // replace all "" with "
                        String value = values.get(i).replaceAll("\"\"", "\"");
                        JsonValue jv = null;
                        // Check if value is JSON object
                        if (value.startsWith("{") && value.endsWith("}")) {
                            try {
                                jv = AuditServiceImpl.parseJsonString(value);
                            } catch (Exception e) {
                                logger.debug("Error parsing JSON string: " + e.getMessage());
                            }
                        }
                        if (jv == null) {
                            entryMap.put(fields.get(i).replace("\"", ""), value);
                        } else {
                            entryMap.put(fields.get(i).replace("\"", ""), jv.asMap());
                        }
                    }
                    entriesList.add(entryMap);
                    entryString = reader.readLine();
                }
            } else {
                return null;
            }
            return entriesList;
        } catch (IOException e) {
            e.printStackTrace();
            throw new BadRequestException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> query(String fullId, Map<String, Object> params) throws ObjectSetException {
        String queryId = (String)params.get("_queryId");
        String[] split = AuditServiceImpl.splitFirstLevel(fullId);
        String type = split[0];
        try {
            List<Map<String, Object>> reconEntryList = getEntryList(type); 
            if (reconEntryList == null) {
                throw new NotFoundException(type + " audit log not found");
            }

            if (AuditServiceImpl.QUERY_BY_RECON_ID.equals(queryId) && type.equals(AuditServiceImpl.TYPE_RECON)) {
                String reconId = (String)params.get("reconId");
                return AuditServiceImpl.getReconResults(reconEntryList, reconId);
            } else if (AuditServiceImpl.QUERY_BY_RECON_SITUATION.equals(queryId) && type.equals(AuditServiceImpl.TYPE_RECON)) {
                String reconId = (String)params.get("reconId");
                String situation = (String)params.get("situation");
                List<Map<String, Object>> rawEntryList = new ArrayList<Map<String, Object>>();
                for (Map<String, Object> entry : reconEntryList) {
                    if (entry.get("reconId").equals(reconId) && entry.get("situation").equals(situation)) {
                        rawEntryList.add(entry);
                    }
                } 
                return AuditServiceImpl.getReconResults(rawEntryList, reconId);
            } else if (AuditServiceImpl.QUERY_BY_ACTIVITY_PARENT_ACTION.equals(queryId) && type.equals(AuditServiceImpl.TYPE_ACTIVITY)) {
                String actionId = (String)params.get("parentActionId");
                List<Map<String, Object>> rawEntryList = new ArrayList<Map<String, Object>>();
                for (Map<String, Object> entry : reconEntryList) {
                    if (entry.get("parentActionid").equals(actionId)) {
                        rawEntryList.add(entry);
                    }
                }
                return AuditServiceImpl.getActivityResults(rawEntryList);
            } else {
                throw new BadRequestException("Unsupported queryId " +  queryId + " on type " + type);
            }
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void create(String fullId, Map<String, Object> obj) throws ObjectSetException {
        EventEntry measure = Publisher.start(EVENT_AUDIT_CREATE, obj, null);
        // Synchronize writes so that simultaneous writes don't corrupt the file
        synchronized (lock) {
            try {
                createImpl(fullId, obj);
            } finally {
                measure.end();
            }
        }
    }
    
    
    private void createImpl(String fullId, Map<String, Object> obj) throws ObjectSetException {
        // TODO: replace ID handling utility
        String[] split = AuditServiceImpl.splitFirstLevel(fullId);
        String type = split[0];
        
        // Re-try once in case the writer stream became closed for some reason
        boolean retry = false;
        int retryCount = 0;
        do {
            retry = false;
            FileWriter fileWriter = null;
            // TODO: optimize buffered, cached writing
            try {
                // TODO: Optimize ordering etc.
                Collection<String> fieldOrder =
                        new TreeSet<String>(Collator.getInstance());
                fieldOrder.addAll(obj.keySet());
    
                File auditFile = getAuditLogFile(type);
                // Create header if creating a new file
                if (!auditFile.exists()) {
                    synchronized (this) {
                        FileWriter existingFileWriter = getWriter(type, auditFile, false);
                        File auditTmpFile = new File(auditLogDir, type + ".tmp");
                        // This is atomic, so only one caller will succeed with created
                        boolean created = auditTmpFile.createNewFile();
                        if (created) {
                            FileWriter tmpFileWriter = new FileWriter(auditTmpFile, true);
                            writeHeaders(fieldOrder, tmpFileWriter);
                            tmpFileWriter.close();
                            auditTmpFile.renameTo(auditFile);
                            resetWriter(type, existingFileWriter);
                        }
                    }
                }
                fileWriter = getWriter(type, auditFile, true);
                writeEntry(fileWriter, type, auditFile, obj, fieldOrder);
            } catch (IOException ex) {
                if (retryCount == 0) {
                    retry = true;
                    logger.debug("IOException during entry write, reset writer and re-try {}", ex.getMessage());
                    synchronized (this) {
                        resetWriter(type, fileWriter);
                    }
                } else {
                    throw new BadRequestException(ex);
                }
            }
            ++retryCount;
        } while (retry);
    }
    
    private File getAuditLogFile(String type) {
        return new File(auditLogDir, type + ".csv");
    }
    
    private void writeEntry(FileWriter fileWriter, String type, File auditFile, Map<String, Object> obj, Collection<String> fieldOrder) 
            throws IOException{
        
        String key = null;
        Iterator<String> iter = fieldOrder.iterator();
        while (iter.hasNext()) {
            key = iter.next();
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

    private FileWriter getWriter(String type, File auditFile, boolean createIfMissing) throws IOException {
        // TODO: optimize synchronization strategy
        synchronized (fileWriters) {
            FileWriter existingWriter = fileWriters.get(type);
            if (existingWriter == null && createIfMissing) {
                existingWriter = new FileWriter(auditFile, true);
                fileWriters.put(type, existingWriter);
            }
            return existingWriter;
        }
    }
    
    // This should only be called if it is known that 
    // the writer is invalid for use or no thread has obtained it / is using it
    // In other words, it does not synchronize on the use of the writer
    // If the writerToReset doesn't exist in the fileWriters (anymore) then 
    // another thread already reset it, and no action is taken
    private void resetWriter(String type, FileWriter writerToReset) {
        FileWriter existingWriter = null;
        // TODO: optimize synchronization strategy
        synchronized (fileWriters) {
            existingWriter = fileWriters.get(type);
            if (existingWriter != null && writerToReset != null && existingWriter == writerToReset) {
                fileWriters.remove(type);
                // attempt clean-up close
                try {
                    existingWriter.close();
                } catch (Exception ex) {
                    // Debug level as the writer is expected to potentially be invalid
                    logger.debug("File writer close in resetWriter reported failure ", ex);
                }
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
