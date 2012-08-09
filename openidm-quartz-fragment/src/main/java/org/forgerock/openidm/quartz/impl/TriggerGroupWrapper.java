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
 * A wrapper the contains all necessary information for a Trigger's group
 */
public class TriggerGroupWrapper {
    
    private String name;
    private String revision;
    private List<String> triggers;
    private boolean paused;
    
    /**
     * Creates a new TriggerGroupWrapper from a group name
     * 
     * @param triggerName the group name
     */
    public TriggerGroupWrapper(String triggerName) {
        triggers = new ArrayList<String>();
        name = triggerName;
        paused = false;
        
    }
    
    /**
     * Creates a new TriggerGroupWrapper from a JsonValue object
     * 
     * @param value the JsonValue object
     */
    public TriggerGroupWrapper(JsonValue value) {
        this(value.asMap());
    }
    
    /**
     * Creates a new TriggerGroupWrapper from a object map.
     * 
     * @param map   the object map
     */
    public TriggerGroupWrapper(Map<String, Object> map) {
        name = (String)map.get("name");
        if (map.get("paused") != null) {
            paused = (Boolean)map.get("paused");
        }
        if (map.get("triggers") != null) {
            triggers = (List<String>)map.get("triggers");
        } else {
            triggers = new ArrayList<String>();
        }
        revision = (String)map.get("_rev");
    }
    
    /**
     * Returns the name of the group
     * 
     * @return the name of the group
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns true if the group is in the "paused" state, false otherwise.
     * 
     * @return  true if the group is in the "paused" state, false otherwise
     */
    public boolean isPaused() {
        return paused;
    }
    
    /**
     * Sets the TriggerGroupWrapper in the "paused" state.
     */
    public void pause() {
        setPaused(true);
    }
    
    /**
     * Resumes the TriggerGroupWrapper form the "paused" state.
     */
    public void resume() {
        setPaused(false);
    }
    
    /**
     * Sets the "paused" state of the TriggerGroupWrapper
     * 
     * @param paused true if "paused", false otherwise
     */
    public void setPaused(boolean paused) {
        this.paused = paused;
    }
    
    /**
     * Adds a Trigger's ID to the list of Triggers in this group.
     * 
     * @param triggerId a Trigger's ID
     */
    public void addTrigger(String triggerId) {
        if (!triggers.contains(triggerId)) {
            triggers.add(triggerId);
        }
    }
    
    /**
     * Removes a Trigger's ID from the list of Triggers in this group.
     * 
     * @param triggerId a Trigger's ID
     */
    public void removeTrigger(String triggerId) {
        if (triggers.contains(triggerId)) {
            triggers.remove(triggerId);
        }
    }
    
    /**
     * Returns a list of all Trigger's names (IDs) in this group.
     * 
     * @return a list of Trigger's names
     */
    public List<String> getTriggerNames() {
        return triggers;
    }
    
    /**
     * Returns a JsonValue object wrapper around the object map for the TriggerGroupWrapper.
     * 
     * @return a JsonValue object
     */
    public JsonValue getValue() {
        Map<String, Object> valueMap = new HashMap<String, Object>();
        valueMap.put("triggers", triggers);
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
