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
    private String actionTaken;
    private String backupFile;
    private String stockFile;

    /**
     * Return the file path for the updated file.
     *
     * @return file path
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Set the file path for the updated file.
     *
     * @param filePath path of the updated file.
     * @return this
     */
    public UpdateFileLogEntry setFilePath(String filePath) {
        this.filePath = filePath;
        return this;
    }

    /**
     * Return the state of the updated file prior to the update.
     *
     * @return state of the file prior to the update.
     */
    public String getFileState() {
        return fileState;
    }

    /**
     * Set the state of the file prior to the update.
     *
     * @param fileState state of the file prior to the update.
     * @return this
     */
    public UpdateFileLogEntry setFileState(String fileState) {
        this.fileState = fileState;
        return this;
    }

    /**
     * Return the action taken in updating the file.
     *
     * @return action taken updating the file
     */
    public String getActionTaken() {
        return actionTaken;
    }

    /**
     * Set the action taken in updating the file.
     *
     * @param actionTaken action taken in updating the file.
     * @return this
     */
    public UpdateFileLogEntry setActionTaken(String actionTaken) {
        this.actionTaken = actionTaken;
        return this;
    }

    /**
     * Return the location of the backup file, if any.
     *
     * @return backup file location.
     */
    public String getBackupFile() {
        return backupFile;
    }

    /**
     * Set the location of the backup file.
     *
     * @param backupFile location of the backup file.
     * @return this
     */
    public UpdateFileLogEntry setBackupFile(String backupFile) {
        this.backupFile = backupFile;
        return this;
    }

    /**
     * Return the location of the stock file, if any.
     *
     * @return stock file location.
     */
    public String getStockFile() {
        return stockFile;
    }

    /**
     * Set the location of the stock file.
     *
     * @param stockFile stock file location.
     * @return this
     */
    public UpdateFileLogEntry setStockFile(String stockFile) {
        this.stockFile = stockFile;
        return this;
    }

    /**
     * Return a json representation of this object
     *
     * @return this object in json form
     */
    public JsonValue toJson() {
        JsonValue ret = json(object(
                field("filePath", getFilePath()),
                field("fileState", getFileState()),
                field("actionTaken", getActionTaken())
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
