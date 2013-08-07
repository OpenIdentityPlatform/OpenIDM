/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2012 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.quartz.impl;

import java.util.HashMap;
import java.util.Map;

import org.forgerock.json.fluent.JsonValue;
import org.quartz.JobDetail;
import org.quartz.JobPersistenceException;

/**
 * A wrapper that contains all necessary information about a Job.
 */
public class JobWrapper {
    
    private String serialized;
    private String key;
    private String revision;
    private boolean paused = false;
    
    /**
     * Creates a new JobWrapper from a JobDetail object
     * 
     * @param jobDetail a JobDetail object
     * @param paused    if the job is paused
     * @throws JobPersistenceException
     */
    public JobWrapper(JobDetail jobDetail, boolean paused) throws JobPersistenceException {
        this.key = jobDetail.getKey().toString();
        this.serialized = RepoJobStoreUtils.serialize(jobDetail);
        this.paused = paused;
    }
    
    /**
     * Creates a new JobWrapper from an object map.
     * 
     * @param map an object map
     */
    public JobWrapper(Map<String, Object> map) {
        serialized = (String)map.get("serialized");
        key = (String)map.get("key");
        paused = (Boolean)map.get("paused");
        revision = (String)map.get("_rev");
    }
    
    /**
     * Returns a JsonValue object representing the JobWrapper
     * 
     * @return  a JsonValue object
     */
    public JsonValue getValue() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("serialized", serialized);
        map.put("key", key);
        map.put("paused", paused);
        return new JsonValue(map);
    }
    
    /**
     * Returns the serialized JobDetail object
     * 
     * @return  the serialized JobDetail object
     */
    public String getSerialized() {
        return serialized;
    }
    
    /**
     * Retuns the Job key
     * 
     * @return the Job key
     */
    public String getKey() {
        return key;
    }
    
    /**
     * Returns the deserialized JobDetail object.
     * 
     * @return the JobDetail object
     * @throws Exception
     */
    public JobDetail getJobDetail() throws Exception {
        return (JobDetail)RepoJobStoreUtils.deserialize(serialized);
    }
    
    /**
     * Returns true if the JobWrapper is in the "paused" state, false otherwise.
     * 
     * @return  true if the JobWrapper is in the "paused" state, false otherwise
     */
    public boolean isPaused() {
        return paused;
    }
    
    /**
     * Sets the JobWrapper in the "paused" state.
     * 
     * @return the paused JobWrapper
     */
    public void pause() {
        setPaused(true);
    }
    
    /**
     * Resumes the JobWrapper from the paused state
     * 
     * @return the resumed JobWrapper
     */
    public void resume() {
        setPaused(false);
    }

    /**
     * Sets the "paused" state of the JobWrapper
     * 
     * @param paused true if "paused", false otherwise
     */
    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    /**
     * Returns the revision, as set by the repo
     * 
     * @return the repo revision
     */
    public String getRevision() {
        return revision;
    }
}
