/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.audit.internal;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.openidm.audit.AuditEvent;
import org.forgerock.openidm.config.InvalidException;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.smartevent.EventEntry;
import org.forgerock.openidm.smartevent.Name;
import org.forgerock.openidm.smartevent.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.LifecycleAware;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 * @see <a
 *      href="https://github.com/FasterXML/jackson-dataformat-csv">jackson-dataformat-csv</a>
 */
public class CSVRecordPublisher implements EventHandler<EventHolder>, LifecycleAware {

    final static String CONFIG_LOG_LOCATION = "location";
    final static String CONFIG_LOG_RECORD_DELIM = "recordDelimiter";

    /**
     * Event names for monitoring audit behavior
     */
    public static final Name EVENT_AUDIT_CREATE = Name.get("openidm/internal/audit/csv/create");

    /**
     * Setup logging for the {@link CSVRecordPublisher}.
     */
    private static final Logger logger = LoggerFactory.getLogger(CSVRecordPublisher.class);

    private final CsvMapper mapper;
    {
        mapper = new CsvMapper();
        mapper.registerModule(new AfterburnerModule());
    }

    File auditLogDir;
    String recordDelim;
    final Map<String, FileWriter> fileWriters = new HashMap<String, FileWriter>();

    private final ObjectWriter writer;

    public CSVRecordPublisher(JsonValue configuration) {
        CsvSchema schema = mapper.schemaFor(AuditEvent.class); // schema from
                                                               // 'Pojo'
                                                               // definition

        writer = mapper.writer(schema);
        // AuditEvent result =
        // mapper.reader(AuditEvent.class).withSchema(schema).read(csv);


        mapper.

        String location = null;
        try {
            auditLogDir =
                    IdentityServer.getFileForWorkingPath(configuration.get(CONFIG_LOG_LOCATION)
                            .required().asString());
            logger.info("Audit logging to: {}", auditLogDir.getAbsolutePath());
            auditLogDir.mkdirs();
            recordDelim = configuration.get(CONFIG_LOG_RECORD_DELIM).asString();
            if (recordDelim == null) {
                recordDelim = "";
            }
            recordDelim += ServerConstants.EOL;
        } catch (Exception ex) {
            logger.error(
                    "ERROR - Configured CSV file location must be a directory and {} is invalid.",
                    auditLogDir.getAbsolutePath(), ex);
            throw new InvalidException("Configured CSV file location must be a directory and '"
                    + location + "' is invalid " + ex.getMessage(), ex);
        }

    }

    @Override
    public void onEvent(EventHolder event, long sequence, boolean endOfBatch) throws Exception {
        writer.writeValueAsString(event.getEvent());

        EventEntry measure = Publisher.start(EVENT_AUDIT_CREATE, null, null);
        try {

            String type = "access";
            // Re-try once in case the writer stream became closed for some
            // reason
            boolean retry = false;
            int retryCount = 0;
            do {
                retry = false;
                FileWriter fileWriter = null;
                // TODO: optimize buffered, cached writing
                try {
                    File auditFile = new File(auditLogDir, type + ".csv");
                    // Create header if creating a new file
                    if (!auditFile.exists()) {
                        synchronized (this) {
                            FileWriter existingFileWriter = getWriter(type, auditFile, false);
                            File auditTmpFile = new File(auditLogDir, type + ".tmp");
                            // This is atomic, so only one caller will succeed
                            // with created
                            boolean created = auditTmpFile.createNewFile();
                            if (created) {
                                FileWriter tmpFileWriter = new FileWriter(auditTmpFile, true);

                                // writeHeaders(fieldOrder, tmpFileWriter);
                                tmpFileWriter.close();
                                auditTmpFile.renameTo(auditFile);
                                resetWriter(type, existingFileWriter);
                            }
                        }
                    }
                    fileWriter = getWriter(type, auditFile, true);
                    writer.writeValue(fileWriter, event.getEvent());
                } catch (IOException ex) {
                    if (retryCount == 0) {
                        retry = true;
                        logger.debug("IOException during entry write, reset writer and re-try {}",
                                ex.getMessage());
                        synchronized (this) {
                            resetWriter(type, fileWriter);
                        }
                    } else {
                        throw new BadRequestException(ex);
                    }
                }
                ++retryCount;
            } while (retry);

        } finally {
            measure.end();
        }
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onShutdown() {
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

    private FileWriter getWriter(String type, File auditFile, boolean createIfMissing)
            throws IOException {
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
                    // Debug level as the writer is expected to potentially be
                    // invalid
                    logger.debug("File writer close in resetWriter reported failure ", ex);
                }
            }
        }
    }

}
