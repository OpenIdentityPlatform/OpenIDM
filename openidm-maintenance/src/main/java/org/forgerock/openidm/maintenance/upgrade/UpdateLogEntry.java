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

import java.util.ArrayList;
import java.util.List;

/**
 * Bean class for an update entry in the repo
 */
public class UpdateLogEntry {
    private String id;
    private String status;
    private String statusMessage;
    private int completedTasks;
    private int totalTasks;
    private String startDate;
    private String endDate;
    private String userName;
    private String nodeId;
    private List<JsonValue> files = new ArrayList<>();

    public String getId() {
        return id;
    }

    public UpdateLogEntry setId(String id) {
        this.id = id;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public UpdateLogEntry setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public UpdateLogEntry setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
        return this;
    }

    public int getCompletedTasks() {
        return completedTasks;
    }

    public UpdateLogEntry setCompletedTasks(int completedTasks) {
        this.completedTasks = completedTasks;
        return this;
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public UpdateLogEntry setTotalTasks(int totalTasks) {
        this.totalTasks = totalTasks;
        return this;
    }

    public String getStartDate() {
        return startDate;
    }

    public UpdateLogEntry setStartDate(String startDate) {
        this.startDate = startDate;
        return this;
    }

    public String getEndDate() {
        return endDate;
    }

    public UpdateLogEntry setEndDate(String endDate) {
        this.endDate = endDate;
        return this;
    }

    public String getUserName() {
        return userName;
    }

    public UpdateLogEntry setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public String getNodeId() {
        return nodeId;
    }

    public UpdateLogEntry setNodeId(String nodeId) {
        this.nodeId = nodeId;
        return this;
    }

    public List<JsonValue> getFiles() {
        return files;
    }

    public void setFiles(List<JsonValue> files) {
        this.files = files;
    }

    public UpdateLogEntry addFile(JsonValue file) {
        files.add(file);
        return this;
    }

    public JsonValue toJson() {
        JsonValue ret = json(object(
                field("_id", getId()),
                field("status", getStatus()),
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
