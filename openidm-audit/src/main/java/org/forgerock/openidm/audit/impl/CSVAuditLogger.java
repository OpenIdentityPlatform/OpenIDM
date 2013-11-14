/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All rights reserved.
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
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openidm.config.enhanced.InvalidException;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Name;
import org.forgerock.openidm.smartevent.Publisher;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.prefs.CsvPreference;
import org.supercsv.util.CsvContext;

/**
 * Comma delimited audit logger
 *
 * @author aegloff
 */
public class CSVAuditLogger extends AbstractAuditLogger implements AuditLogger {
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
            super.setConfig(config, ctx);
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
    public Map<String, Object> read(ServerContext context, String type, String id) throws ResourceException {
        try {
            Map<String, Object> result = new HashMap<String, Object>();
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
            return result;
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
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
        List<Map<String, Object>> entryList = new ArrayList<Map<String, Object>>();
        CellProcessor [] processors = null;
        if (AuditServiceImpl.TYPE_RECON.equals(type)) {
            processors = new CellProcessor[] {
                    new NotNull(), // _id
                    new Optional(), // action
                    new Optional(), // actionId
                    new Optional(), // ambiguousTargetObjectIds
                    new Optional(), // entryType
                    new Optional(), // exception
                    new Optional(), // mapping
                    new Optional(), // message
                    new Optional(new ParseJsonValue()), // messageDetail
                    new Optional(), // reconciling
                    new NotNull(), // reconId
                    new Optional(), // rootActionId
                    new Optional(), // situation
                    new Optional(), // sourceObjectId
                    new Optional(), // status
                    new Optional(), // targetObjectId
                    new NotNull() // timestamp
            };
        } else if (AuditServiceImpl.TYPE_ACTIVITY.equals(type)) {
            processors = new CellProcessor[] {
                    new NotNull(), // _id
                    new Optional(), // action
                    new Optional(), // activityId
                    new Optional(new ParseJsonValue()), // after
                    new Optional(new ParseJsonValue()), // before
                    new Optional(), // changedFields
                    new Optional(), // message
                    new Optional(), // objectId
                    new Optional(), // parentActionId
                    new Optional(), // passwordChanged
                    new Optional(), // requester
                    new Optional(), // rev
                    new Optional(), // rootActionId
                    new Optional(), // status
                    new NotNull() // timestamp
            };
        } else if (AuditServiceImpl.TYPE_ACCESS.equals(type)) {
            processors = new CellProcessor[] {
                    new NotNull(), // _id
                    new Optional(), // action
                    new Optional(), // ip
                    new Optional(), // principal
                    new Optional(new ParseJsonValue()), // roles
                    new Optional(), // status
                    new NotNull(), // timestamp
                    new Optional() // userid
            };
        } else {
            throw new InternalServerErrorException("Error parsing entries: unknown type " + type);
        }

        File auditFile = getAuditLogFile(type);
        if (auditFile.exists()) {
            ICsvMapReader reader = null;
            try {
                reader = new CsvMapReader(new FileReader(auditFile), new CsvPreference.Builder('"', ',', recordDelim).build());

                // the header elements are used to map the values to the bean (names must match)
                final String[] header = reader.getHeader(true);

                Map<String, Object> entryMap;
                while( (entryMap = reader.read(header, processors)) != null ) {
                        entryList.add(entryMap);
                }

            }
            finally {
                if( reader != null ) {
                    reader.close();
                }
            }
        }
        return entryList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> query(ServerContext context, String type, Map<String, String> params) throws ResourceException {
        String queryId = params.get("_queryId");
        boolean formatted = true;
        try {
            if (params.get("formatted") != null && !AuditServiceImpl.getBoolValue(params.get("formatted"))) {
                formatted = false;
            }

            List<Map<String, Object>> reconEntryList = getEntryList(type);
            if (reconEntryList == null) {
                throw new NotFoundException(type + " audit log not found");
            }

            String reconId = params.get("reconId");
            if (AuditServiceImpl.QUERY_BY_RECON_ID.equals(queryId) && type.equals(AuditServiceImpl.TYPE_RECON)) {
                return AuditServiceImpl.getReconResults(reconEntryList, formatted);
            } else if (AuditServiceImpl.QUERY_BY_MAPPING.equals(queryId) && type.equals(AuditServiceImpl.TYPE_RECON)) {
                return getReconQueryResults(reconEntryList, reconId, "mapping", params.get("mappingName"), formatted);
            } else if (AuditServiceImpl.QUERY_BY_RECON_ID_AND_SITUATION.equals(queryId) && type.equals(AuditServiceImpl.TYPE_RECON)) {
                return getReconQueryResults(reconEntryList, reconId, "situation", params.get("situation"), formatted);
            } else if (AuditServiceImpl.QUERY_BY_RECON_ID_AND_TYPE.equals(queryId) && type.equals(AuditServiceImpl.TYPE_RECON)) {
                return getReconQueryResults(reconEntryList, reconId, "entryType", params.get("entryType"), formatted);
            } else if (AuditServiceImpl.QUERY_BY_ACTIVITY_PARENT_ACTION.equals(queryId) && type.equals(AuditServiceImpl.TYPE_ACTIVITY)) {
                String actionId = params.get("parentActionId");
                List<Map<String, Object>> rawEntryList = new ArrayList<Map<String, Object>>();
                for (Map<String, Object> entry : reconEntryList) {
                    if (entry.get("parentActionId").equals(actionId)) {
                        rawEntryList.add(entry);
                    }
                }
                return AuditServiceImpl.getActivityResults(rawEntryList, formatted);
            } else {
                throw new BadRequestException("Unsupported queryId " +  queryId + " on type " + type);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new BadRequestException(e);
        }
    }

    private Map<String, Object> getReconQueryResults(List<Map<String, Object>> list, String reconId, String param, String paramValue, boolean formatted) {
        List<Map<String, Object>> rawEntryList = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> entry : list) {
            if ((reconId == null || (entry.get("reconId").equals(reconId))) && (param == null || paramValue.equals(entry.get(param)))) {
                rawEntryList.add(entry);
            }
        }
        return AuditServiceImpl.getReconResults(rawEntryList, formatted);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void create(ServerContext context, String type, Map<String, Object> obj) throws ResourceException {
        EventEntry measure = Publisher.start(EVENT_AUDIT_CREATE, obj, null);
        // Synchronize writes so that simultaneous writes don't corrupt the file
        synchronized (lock) {
            try {
                AuditServiceImpl.preformatLogEntry(type, obj);
                createImpl(type, obj);
            } finally {
                measure.end();
            }
        }
    }


    private void createImpl(String type, Map<String, Object> obj) throws ResourceException {

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
                if (value instanceof Map) {
                    value = new JsonValue((Map)value).toString();
                }
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
     * CellProcessor for parsing JsonValue objects from CSV file.
     */
    public class ParseJsonValue implements CellProcessor {

        @Override
        public Object execute(Object value, CsvContext context) {
            JsonValue jv = null;
            // Check if value is JSON object
            if (((String)value).startsWith("{") && ((String)value).endsWith("}")) {
                try {
                    jv = AuditServiceImpl.parseJsonString(((String)value));
                } catch (Exception e) {
                    logger.debug("Error parsing JSON string: " + e.getMessage());
                }
            }
            if (jv == null) {
                return value;
            }
            return jv.asMap();
        }

    }
}
