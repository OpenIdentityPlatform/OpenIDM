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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.json.fluent.JsonValue;


/**
 * A wrapper the contains all necessary information for a Jobs's group
 */
public class JobGroupWrapper {
   
   private String name;
   private String revision;
   private List<String> jobs;
   private boolean paused = false;
   
   /**
    * Creates a JobGroupWrapper from a job name
    * 
    * @param jobName a job name
    */
   public JobGroupWrapper(String jobName) {
       jobs = new ArrayList<String>();
       name = jobName;
       paused = false;
       
   }
   
   /**
    * Creates a JobGroupWrapper from a JsonValue object
    * 
    * @param value a JsonValue object
    */
   public JobGroupWrapper(JsonValue value) {
       this(value.asMap());
   }
   
   /**
    * Creates a JobGroupWrapper from an object map
    * 
    * @param map    an object map
    */
   public JobGroupWrapper(Map<String, Object> map) {
       name = (String)map.get("name");
       if (map.get("paused") != null) {
           paused = (Boolean)map.get("paused");
       }
       if (map.get("jobs") != null) {
           jobs = (List<String>)map.get("jobs");
       } else {
           jobs = new ArrayList<String>();
       }
       revision = (String)map.get("_rev");
   }
   
   /**
    * Return the name of the Job
    * 
    * @return the name of the Job
    */
   public String getName() {
       return name;
   }
   
   /**
    * Returns true if the Job is in the "paused" state, false otherwise
    * 
    * @return   true if the Job is in the "paused" state, false otherwise
    */
   public boolean isPaused() {
       return paused;
   }

   /**
    * Sets the JobGroupWrapper in the "paused" state.
    */
   public void pause() {
       setPaused(true);
   }

   /**
    * Resumes the JobGroupWrapper form the "paused" state.
    */
   public void resume() {
       setPaused(false);
   }

   /**
    * Sets the "paused" state of the JobGroupWrapper
    * 
    * @param paused true if "paused", false otherwise
    */
   public void setPaused(boolean paused) {
       this.paused = paused;
   }
   
   /**
    * Adds a Job's ID to the list of Jobs in this group.
    * 
    * @param jobId a Job's ID
    */
   public void addJob(String jobId) {
       if (!jobs.contains(jobId)) {
           jobs.add(jobId);
       }
   }

   /**
    * Removes a Job's ID from the list of Jobs in this group.
    * 
    * @param jobId a Job's ID
    */
   public void removeJob(String jobId) {
       if (jobs.contains(jobId)) {
           jobs.remove(jobId);
       }
   }
   
   /**
    * Returns a list of all Job's names (IDs) in this group.
    * 
    * @return a list of Job's names
    */
   public List<String> getJobNames() {
       return jobs;
   }
   
   /**
    * Returns a JsonValue object wrapper around the object map for the JobGroupWrapper.
    * 
    * @return a JsonValue object
    */
   public JsonValue getValue() {
       Map<String, Object> valueMap = new HashMap<String, Object>();
       valueMap.put("jobs", jobs);
       valueMap.put("name", name);
       valueMap.put("paused", paused);
       return new JsonValue(valueMap);
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


