/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2015 ForgeRock AS.
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

import java.io.BufferedWriter;
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

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openidm.config.enhanced.InvalidException;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Name;
import org.forgerock.openidm.smartevent.Publisher;
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
 */
public class CSVAuditLogger extends AbstractAuditLogger implements AuditLogger {
    final static Logger logger = LoggerFactory.getLogger(CSVAuditLogger.class);

    public static final String CONFIG_LOG_LOCATION = "location";
    public static final String CONFIG_LOG_RECORD_DELIM = "recordDelimiter";
    public static final String CONFIG_LOG_BUFFER_SIZE = "bufferSize";
    public static final String CONFIG_LOG_MAX_FLUSH_DELAY = "maxFlushDelay";

    private static final Object createLock = new Object();

    /**
     * Event names for monitoring audit behavior
     */
    public static final Name EVENT_AUDIT_CREATE = Name.get("openidm/internal/audit/csv/create");

    File auditLogDir;
    String recordDelim;
    int bufferSize;
    long maxFlushDelay;

    // The fileWriters are not static so neither is the flushTimer or its lock
    private WriteBufferFlushTimer flushTimer;
    private final Object flushTimerLock = new Object();

    final Map<String, BufferedWriter> fileWriters = new HashMap<String, BufferedWriter>();

    public void setConfig(JsonValue config) throws InvalidException {
        String location = null;
        try {
            super.setConfig(config);
            location = config.get(CONFIG_LOG_LOCATION).asString();
            auditLogDir = IdentityServer.getFileForWorkingPath(location);
            logger.info("Audit logging to: {}", auditLogDir.getAbsolutePath());
            auditLogDir.mkdirs();
            recordDelim = config.get(CONFIG_LOG_RECORD_DELIM).asString();
            if (StringUtils.isBlank(recordDelim)) {
                recordDelim = ServerConstants.EOL;
            }

            // Maintain FileWriter's 1024 byte write buffer default unless config overrides it.
            bufferSize = config.get(CONFIG_LOG_BUFFER_SIZE).defaultTo(1024).asInteger();

            // The file buffer may take a long time during slow periods to reach <bufferSize> and
            // trigger a flush.  maxFlushDelay will ensure that the buffer is flushed at least
            // once every <maxFlushDelay> seconds.
            maxFlushDelay = config.get(CONFIG_LOG_MAX_FLUSH_DELAY).defaultTo(0).asInteger();
        } catch (Exception ex) {
            logger.error("ERROR - Configured CSV file location must be a directory and {} is invalid.", auditLogDir.getAbsolutePath(), ex);
            throw new InvalidException("Configured CSV file location must be a directory and '" + location
                    + "' is invalid " + ex.getMessage(), ex);
        }
    }

    public void cleanup() {
        for (Map.Entry<String, BufferedWriter> entry : fileWriters.entrySet()) {
            try {
                BufferedWriter fileWriter = entry.getValue();
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
                    new Optional(), // linkQualifier
                    new Optional(), // mapping
                    new Optional(), // message
                    new Optional(new ParseJsonValue()), // messageDetail
                    new Optional(), // reconAction
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
        } else if (AuditServiceImpl.TYPE_SYNC.equals(type)) {
            processors = new CellProcessor[]{
                    new NotNull(), // _id
                    new Optional(), // action
                    new Optional(), // actionId
                    new Optional(), // exception
                    new Optional(), // linkQualifier
                    new Optional(), // mapping
                    new Optional(), // message
                    new Optional(new ParseJsonValue()), // messageDetail
                    new Optional(), // rootActionId
                    new Optional(), // situation
                    new Optional(), // sourceObjectId
                    new Optional(), // status
                    new Optional(), // targetObjectId
                    new Optional(), // timestamp
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
    public void query(ServerContext context, QueryRequest request, final QueryResultHandler handler, 
            final String type, final boolean formatted) throws ResourceException {
        Map<String, String> params = request.getAdditionalParameters();
        String queryId = request.getQueryId();
        try {
            List<Map<String, Object>> reconEntryList = getEntryList(type);
            if (reconEntryList == null) {
                throw new NotFoundException(type + " audit log not found");
            }

            JsonValue result = null;
            String reconId = params.get("reconId");
            if (AuditServiceImpl.QUERY_BY_RECON_ID.equals(queryId) && type.equals(AuditServiceImpl.TYPE_RECON)) {
                result = AuditServiceImpl.getReconResults(reconEntryList, formatted);
            } else if (AuditServiceImpl.QUERY_BY_MAPPING.equals(queryId) && type.equals(AuditServiceImpl.TYPE_RECON)) {
                result = getReconQueryResults(reconEntryList, reconId, "mapping", params.get("mappingName"), formatted);
            } else if (AuditServiceImpl.QUERY_BY_RECON_ID_AND_SITUATION.equals(queryId) && type.equals(AuditServiceImpl.TYPE_RECON)) {
                result = getReconQueryResults(reconEntryList, reconId, "situation", params.get("situation"), formatted);
            } else if (AuditServiceImpl.QUERY_BY_RECON_ID_AND_TYPE.equals(queryId) && type.equals(AuditServiceImpl.TYPE_RECON)) {
                result = getReconQueryResults(reconEntryList, reconId, "entryType", params.get("entryType"), formatted);
            } else if (AuditServiceImpl.QUERY_BY_ACTIVITY_PARENT_ACTION.equals(queryId) && type.equals(AuditServiceImpl.TYPE_ACTIVITY)) {
                String actionId = params.get("parentActionId");
                List<Map<String, Object>> rawEntryList = new ArrayList<Map<String, Object>>();
                for (Map<String, Object> entry : reconEntryList) {
                    if (entry.get("parentActionId").equals(actionId)) {
                        rawEntryList.add(entry);
                    }
                }
                result = AuditServiceImpl.getActivityResults(rawEntryList, formatted);
            } else {
                throw new BadRequestException("Unsupported queryId " +  queryId + " on type " + type);
            }
            for (JsonValue queryResult : result.get("result")) {
                String id = queryResult.get(Resource.FIELD_CONTENT_ID).asString();
                handler.handleResource(new Resource(id, null, queryResult));
            }
            handler.handleResult(new QueryResult());
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
    }

    private JsonValue getReconQueryResults(List<Map<String, Object>> list, String reconId, String param, String paramValue, boolean formatted) {
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
        try {
            AuditServiceImpl.preformatLogEntry(type, obj);
            // Synchronize writes so that simultaneous writes don't corrupt the file
            synchronized (createLock) {
                createImpl(type, obj);
            }
        } finally {
            measure.end();
        }
    }


    private void createImpl(String type, Map<String, Object> obj) throws ResourceException {

        // Re-try once in case the writer stream became closed for some reason
        boolean retry = false;
        int retryCount = 0;
        do {
            retry = false;
            BufferedWriter fileWriter = null;
            try {
                // TODO: Optimize ordering etc.
                Collection<String> fieldOrder = new TreeSet<String>(Collator.getInstance());
                fieldOrder.addAll(obj.keySet());

                File auditFile = getAuditLogFile(type);
                // Create header if creating a new file
                if (!auditFile.exists()) {
                    synchronized (fileWriters) {
                        BufferedWriter existingFileWriter = getWriter(type, auditFile, false);
                        File auditTmpFile = new File(auditLogDir, type + ".tmp");
                        // This is atomic, so only one caller will succeed with created
                        boolean created = auditTmpFile.createNewFile();
                        if (created) {
                            BufferedWriter tmpFileWriter = new BufferedWriter(
                                    new FileWriter(auditTmpFile, true), bufferSize);
                            writeHeaders(fieldOrder, tmpFileWriter);
                            tmpFileWriter.close();
                            auditTmpFile.renameTo(auditFile);
                            resetWriter(type, existingFileWriter);
                        }
                    }
                }
                fileWriter = getWriter(type, auditFile, true);
                writeEntry(fileWriter, type, auditFile, obj, fieldOrder);

                if (maxFlushDelay > 0) {
                    synchronized(flushTimerLock) {
                        if (flushTimer != null && flushTimer.isAlive()) {
                            flushTimer.interrupt();
                        }
                        flushTimer = new WriteBufferFlushTimer(maxFlushDelay);
                    }
                } else {
                    fileWriter.flush();
                }
            } catch (IOException ex) {
                if (retryCount == 0) {
                    retry = true;
                    logger.debug("IOException during entry write, reset writer and re-try {}", ex.getMessage());
                    resetWriter(type, fileWriter);
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

    private void writeEntry(BufferedWriter fileWriter, String type, File auditFile, Map<String, Object> obj,
                            Collection<String> fieldOrder)
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
    }

    private void writeHeaders(Collection<String> fieldOrder, BufferedWriter fileWriter)
            throws IOException {
        Iterator<String> iter = fieldOrder.iterator();
        while (iter.hasNext()) {
            String key = iter.next();
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

    private BufferedWriter getWriter(String type, File auditFile, boolean createIfMissing) throws IOException {
        // TODO: optimize synchronization strategy
        synchronized (fileWriters) {
            BufferedWriter existingWriter = fileWriters.get(type);
            if (existingWriter == null && createIfMissing) {
                existingWriter = new BufferedWriter(new FileWriter(auditFile, true), bufferSize);
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
    private void resetWriter(String type, BufferedWriter writerToReset) {
        BufferedWriter existingWriter = null;
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
                    jv = AuditServiceImpl.parseJsonString(((String)value), Map.class);
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

    private class WriteBufferFlushTimer extends Thread {
        long sleepSeconds;

        public WriteBufferFlushTimer(long sleepSeconds) {
            this.sleepSeconds = sleepSeconds;
            start();
        }

        @Override
        public void run() {
            try {
                sleep(sleepSeconds * 1000);
                synchronized (flushTimerLock) {
                    synchronized (fileWriters) {
                        for (BufferedWriter writer : fileWriters.values()) {
                            try {
                                writer.flush();
                            } catch (IOException e) {
                                logger.debug("Failed to flush CSV audit write buffer", e);
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                // This is expected if the timer is reset or IDM is shut down
            }
        }
    }
}
