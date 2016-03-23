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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openidm.maintenance.upgrade;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import org.forgerock.json.JsonValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Bean class for an update entry in the repo
 */
public class UpdateLogEntry {
    private String id;
    private String archive;
    private UpdateManagerImpl.UpdateStatus status;
    private String statusMessage;
    private int completedTasks;
    private int totalTasks;
    private String startDate;
    private String endDate;
    private String userName;
    private String nodeId;
    private List<JsonValue> files = new ArrayList<>();

    /**
     * Return this entry's ID
     * @return the ID
     */
    public String getId() {
        return id;
    }

    /**
     * Set this entry's ID
     * @param id the ID for this entry
     * @return this entry
     */
    public UpdateLogEntry setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * Return this entry's update archive
     * @return the update archive
     */
    public String getArchive() {
        return archive;
    }

    /**
     * Set this entry's update archive
     * @param archive the update archive
     * @return this entry
     */
    public UpdateLogEntry setArchive(String archive) {
        this.archive = archive;
        return this;
    }

    /**
     * Return this entry's status
     * @return status
     */
    public UpdateManagerImpl.UpdateStatus getStatus() {
        return status;
    }

    /**
     * Set this entry's status
     * @param status the status
     * @return this entry
     */
    public UpdateLogEntry setStatus(UpdateManagerImpl.UpdateStatus status) {
        this.status = status;
        return this;
    }

    /**
     * Return this entry's status message
     * @return the status message
     */
    public String getStatusMessage() {
        return statusMessage;
    }

    /**
     * Set this entry's status message
     * @param statusMessage the status message
     * @return this entry
     */
    public UpdateLogEntry setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
        return this;
    }

    /**
     * Return this entry's completed task count
     * @return the completed task count
     */
    public int getCompletedTasks() {
        return completedTasks;
    }

    /**
     * Set this entry's completed task count
     * @param completedTasks the completed task count
     * @return this entry
     */
    public UpdateLogEntry setCompletedTasks(int completedTasks) {
        this.completedTasks = completedTasks;
        return this;
    }

    /**
     * Return this entry's total task count
     * @return the total task count
     */
    public int getTotalTasks() {
        return totalTasks;
    }

    /**
     * Set this entry's total task count
     * @param totalTasks the total task count
     * @return this entry
     */
    public UpdateLogEntry setTotalTasks(int totalTasks) {
        this.totalTasks = totalTasks;
        return this;
    }

    /**
     * Return this entry's start date
     * @return the start date
     */
    public String getStartDate() {
        return startDate;
    }

    /**
     * Set this entry's start date
     * @param startDate the start date
     * @return this entry
     */
    public UpdateLogEntry setStartDate(String startDate) {
        this.startDate = startDate;
        return this;
    }

    /**
     * Return this entry's end date
     * @return the end date
     */
    public String getEndDate() {
        return endDate;
    }

    /**
     * Set this entry's end date
     * @param endDate the end date
     * @return this entry
     */
    public UpdateLogEntry setEndDate(String endDate) {
        this.endDate = endDate;
        return this;
    }

    /**
     * Return this entry's initiating username
     * @return the initiating username
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Set this entry's initiating username
     * @param userName the initiating username
     * @return this entry
     */
    public UpdateLogEntry setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    /**
     * Return this entry's cluster node ID
     * @return the cluster node ID
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Set this entry's cluster node ID
     * @param nodeId the cluster node ID
     * @return this entry
     */
    public UpdateLogEntry setNodeId(String nodeId) {
        this.nodeId = nodeId;
        return this;
    }

    /**
     * Return this entry's list of update files
     * @return the list of update files
     */
    public List<JsonValue> getFiles() {
        return files;
    }

    /**
     * Set this entry's list of update files
     * @param files the list of update files
     */
    public void setFiles(List<JsonValue> files) {
        this.files = files;
    }

    /**
     * Add a file to this entry's list of update files
     * @param file the file to add
     * @return this entry
     */
    public UpdateLogEntry addFile(JsonValue file) {
        files.add(file);
        return this;
    }

    /**
     * Return a json representation of this object
     *
     * @return this object in json form
     */
    public JsonValue toJson() {
        JsonValue ret = json(object(
                field("_id", getId()),
                field("archive", getArchive()),
                field("status", getStatus() != null ? getStatus().toString() : null),
                field("completedTasks", getCompletedTasks()),
                field("totalTasks", getTotalTasks()),
                field("startDate", getStartDate()),
                field("userName", getUserName())
        ));
        if (getStatusMessage() != null) {
            ret.put("statusMessage", getStatusMessage());
        }
        if (getEndDate() != null) {
            ret.put("endDate", getEndDate());
        }
        if (getNodeId() != null) {
            ret.put("nodeId", getNodeId());
        }
        ret.put("files", files);
        return ret;
    }
}
