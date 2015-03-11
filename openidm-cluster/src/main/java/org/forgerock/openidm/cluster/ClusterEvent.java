/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2013-2014 ForgeRock AS. All Rights Reserved
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
*
*/
package org.forgerock.openidm.cluster;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

import org.forgerock.json.fluent.JsonValue;

/**
 * A cluster event container.
 * 
 */
public class ClusterEvent {
    
    public static final String EVENT_TYPE = "type";
    public static final String EVENT_INSTANCE_ID = "instanceId";
    public static final String EVENT_LISTENER_ID = "listenerId";
    public static final String EVENT_DETAILS = "details";
    
    private ClusterEventType type;
    private String instanceId;
    private String listenerId = null;
    private JsonValue details = null;

    public ClusterEvent(ClusterEventType type, String instanceId) {
        this.type = type;
        this.instanceId = instanceId;
    }
    
    public ClusterEvent(ClusterEventType type, String instanceId, String listenerId, JsonValue details) {
        this.type = type;
        this.instanceId = instanceId;
        this.listenerId = listenerId;
        this.details = details;
    }
    
    public ClusterEvent(String type, String instanceId, String listenerId, JsonValue details) {
        this.type = ClusterEventType.valueOf(type);
        this.instanceId = instanceId;
        this.listenerId = listenerId;
        this.details = details;
    }
    
    public ClusterEvent(JsonValue event) {
        this.type = ClusterEventType.valueOf(event.get(EVENT_TYPE).required().asString());
        this.instanceId = event.get(EVENT_INSTANCE_ID).required().asString();
        JsonValue listener = event.get(EVENT_LISTENER_ID);
        this.listenerId = listener.isNull() ? null : listener.asString();
        this.details = event.get(EVENT_DETAILS);
    }

    public ClusterEventType getType() {
        return type;
    }

    public void setType(ClusterEventType type) {
        this.type = type;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }
    
    public JsonValue getDetails() {
        return details;
    }

    public void setDetails(JsonValue details) {
        this.details = details;
    }

    public String getListenerId() {
        return listenerId;
    }

    public void setListenerId(String listenerId) {
        this.listenerId = listenerId;
    }
    
    protected JsonValue toJsonValue() {
        return json(object(
                field(EVENT_TYPE, getType().toString()),
                field(EVENT_INSTANCE_ID, getInstanceId()),
                field(EVENT_LISTENER_ID, getListenerId()),
                field(EVENT_DETAILS, getDetails().getObject())));
    }
}
