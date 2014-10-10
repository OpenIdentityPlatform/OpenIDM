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

import java.util.HashMap;
import java.util.Map;

public class InstanceState {
    
    public final static int STATE_RUNNING = 1;
    public final static int STATE_PROCESSING_DOWN = 2;
    public final static int STATE_DOWN = 3;

    public final static String PROP_INSTANCE_ID                 = "instanceId";
    public final static String PROP_RECOVERING_INSTANCE_ID      = "recoveringInstanceId";
    public final static String PROP_STATE                       = "state";
    public final static String PROP_TIMESTAMP_LEASE             = "timestamp";
    public final static String PROP_TIMESTAMP_STARTUP           = "startup";
    public final static String PROP_TIMESTAMP_SHUTDOWN          = "shutdown";
    public final static String PROP_TIMESTAMP_DETECTED_DOWN     = "detectedDown";
    public final static String PROP_TIMESTAMP_RECOVERY          = "recoveringTimestamp";
    public final static String PROP_TIMESTAMP_RECOVERY_STARTED  = "recoveryStarted";
    public final static String PROP_TIMESTAMP_RECOVERY_FINISHED = "recoveryFinished";
    public final static String PROP_RECOVERY_ATTEMPTS           = "recoveryAttempts";
    public final static String PROP_TYPE                        = "type";
    public final static String PROP_REV                         = "_rev";
    public final static String PROP_ID                          = "_id";
    
    private String instanceId;
    private int state;
    private int recoveryAttempts;
    private long startup;
    private long shutdown;
    private long timestamp;
    private long detectedDown;
    private long recoveryStarted;
    private long recoveryFinished;
    private long recoveringTimestamp;
    private String recoveringInstanceId;
    private String rev;
    private String id;
    
    public InstanceState(String instanceId, Map<String, Object> map) {
        this.instanceId = instanceId;
        this.recoveringInstanceId = (String)map.get(PROP_RECOVERING_INSTANCE_ID);
        this.state = ((map.get(PROP_STATE) == null) ? STATE_RUNNING : (Integer)map.get(PROP_STATE));
        this.timestamp = ((map.get(PROP_TIMESTAMP_LEASE) == null) ? System.currentTimeMillis() : 
            Long.parseLong((String)map.get(PROP_TIMESTAMP_LEASE)));
        this.startup = ((map.get(PROP_TIMESTAMP_STARTUP) == null) ? System.currentTimeMillis() : 
            Long.parseLong((String)map.get(PROP_TIMESTAMP_STARTUP)));
        this.shutdown = ((map.get(PROP_TIMESTAMP_SHUTDOWN) == null) ? 0L : 
            Long.parseLong((String)map.get(PROP_TIMESTAMP_SHUTDOWN)));
        this.detectedDown = ((map.get(PROP_TIMESTAMP_DETECTED_DOWN) == null) ? 0L : 
            Long.parseLong((String)map.get(PROP_TIMESTAMP_DETECTED_DOWN)));
        this.recoveringTimestamp = ((map.get(PROP_TIMESTAMP_RECOVERY) == null) ? 0L : 
            Long.parseLong((String)map.get(PROP_TIMESTAMP_RECOVERY)));
        this.recoveryStarted = ((map.get(PROP_TIMESTAMP_RECOVERY_STARTED) == null) ? 0L : 
            Long.parseLong((String)map.get(PROP_TIMESTAMP_RECOVERY_STARTED)));
        this.recoveryFinished = ((map.get(PROP_TIMESTAMP_RECOVERY_FINISHED) == null) ? 0L : 
            Long.parseLong((String)map.get(PROP_TIMESTAMP_RECOVERY_FINISHED)));
        this.recoveryAttempts = ((map.get(PROP_RECOVERY_ATTEMPTS) == null) ? 0 : 
            (Integer)map.get(PROP_RECOVERY_ATTEMPTS));
        this.rev = (String)map.get(PROP_REV);
        this.id = (String)map.get(PROP_ID);
    }
    
    public InstanceState(String instanceId) {
        this.instanceId = instanceId;
        this.state = STATE_RUNNING;
        this.timestamp = System.currentTimeMillis();
        this.startup = System.currentTimeMillis();
        this.shutdown = 0L;
        this.recoveringTimestamp = 0L;
        this.recoveringInstanceId = null;
        this.recoveryStarted = 0L;
        this.recoveryFinished = 0L;
        this.detectedDown = 0L;
        this.recoveryAttempts = 0;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(PROP_INSTANCE_ID, getInstanceId());
        map.put(PROP_STATE, getState());
        map.put(PROP_TIMESTAMP_LEASE, pad(new Long(getTimestamp())));
        map.put(PROP_TIMESTAMP_STARTUP, pad(new Long(getStartup())));
        map.put(PROP_TIMESTAMP_SHUTDOWN, pad(new Long(getShutdown())));
        map.put(PROP_TIMESTAMP_DETECTED_DOWN, pad(new Long(getDetectedDown())));
        map.put(PROP_RECOVERING_INSTANCE_ID, getRecoveringInstanceId());
        map.put(PROP_TIMESTAMP_RECOVERY, pad(new Long(getRecoveringTimestamp())));
        map.put(PROP_TIMESTAMP_RECOVERY_STARTED, pad(getRecoveryStarted()));
        map.put(PROP_TIMESTAMP_RECOVERY_FINISHED, pad(getRecoveryFinished()));
        map.put(PROP_RECOVERY_ATTEMPTS, getRecoveryAttempts());
        map.put(PROP_REV, getRevision());
        map.put(PROP_ID, id);
        map.put(PROP_TYPE, "state");
        return map;
    }
    
    public boolean hasFailed(long timeout) {
        if (System.currentTimeMillis() - timestamp > timeout) {
            return true;
        }
        return false;
    }
    
    public boolean hasRecoveringFailed(long timout) {
        if (state == STATE_PROCESSING_DOWN && 
                (System.currentTimeMillis() - recoveringTimestamp > timout)) {
            return true;
        }
        return false;
    }
    
    public boolean hasShutdown() {
        if (shutdown == 0L) {
            return false;
        }
        return true;
    }

    public void updateStartup() {
        this.startup = System.currentTimeMillis();
    }

    public void updateShutdown() {
        this.shutdown = System.currentTimeMillis();
    }

    public void updateTimestamp() {
        timestamp = System.currentTimeMillis();
    }
    
    public void updateRecoveringTimestamp() {
        recoveringTimestamp = System.currentTimeMillis();
    }
    
    public void updateDetectedDown() {
        detectedDown = System.currentTimeMillis();
    }

    public String getInstanceId() {
        return instanceId;
    }
    
    public int getState() {
        return state;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getStartup() {
        return startup;
    }

    public long getShutdown() {
        return shutdown;
    }
    
    public long getDetectedDown() {
        return detectedDown;
    }

    public long getRecoveringTimestamp() {
        return recoveringTimestamp;
    }

    public String getRecoveringInstanceId() {
        return recoveringInstanceId;
    }

    public int getRecoveryAttempts() {
        return recoveryAttempts;
    }
    
    public long getUptime() {
        return System.currentTimeMillis() - startup;
    }
    
    public String getRevision() {
        return rev;
    }
    
    public void clearShutdown() {
        shutdown = 0L;
    }
    
    public void clearDetectedDown() {
        detectedDown = 0L;
    }
    
    public void clearRecoveryAttempts() {
        recoveryAttempts = 0;
    }

    public void setState(int state) {
        this.state = state;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public void setRecoveringInstanceId(String recoveringInstanceId) {
        this.recoveringInstanceId = recoveringInstanceId;
    }

    public long getRecoveryStarted() {
        return recoveryStarted;
    }

    public void startRecovery() {
        recoveryStarted = System.currentTimeMillis();
        recoveryAttempts++;
    }

    public long getRecoveryFinished() {
        return recoveryFinished;
    }

    public void finishRecovery() {
        this.recoveryFinished = System.currentTimeMillis();
    }
    
    public static String pad(long l) {
        return String.format("%019d", l);
    }
}
