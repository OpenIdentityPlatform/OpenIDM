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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openidm.maintenance.upgrade;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import org.forgerock.json.JsonValue;

/**
 * Bean for an updateFile log entry
 */
public class UpdateFileLogEntry {
    private String filePath;
    private String fileState;
    private String backupFile;
    private String stockFile;

    public String getFilePath() {
        return filePath;
    }

    public UpdateFileLogEntry setFilePath(String filePath) {
        this.filePath = filePath;
        return this;
    }

    public String getFileState() {
        return fileState;
    }

    public UpdateFileLogEntry setFileState(String fileState) {
        this.fileState = fileState;
        return this;
    }

    public String getBackupFile() {
        return backupFile;
    }

    public UpdateFileLogEntry setBackupFile(String backupFile) {
        this.backupFile = backupFile;
        return this;
    }

    public String getStockFile() {
        return stockFile;
    }

    public UpdateFileLogEntry setStockFile(String stockFile) {
        this.stockFile = stockFile;
        return this;
    }

    public JsonValue toJson() {
        JsonValue ret = json(object(
                field("filePath", getFilePath()),
                field("fileState", getFileState())
        ));
        if (getBackupFile() != null) {
            ret.put("backupFile", getBackupFile());
        }
        if (getStockFile() != null) {
            ret.put("stockFile", getStockFile());
        }
        return ret;
    }
}
