/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright 2012-2015 ForgeRock AS. All Rights Reserved
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

import org.forgerock.json.JsonValue;
import org.quartz.Calendar;
import org.quartz.JobPersistenceException;

/**
 * A wrapper that contains the name and serialize form of a calendar.
 */
public class CalendarWrapper {
    private String serialized;
    private String name;
    private String revision;
    
    /**
     * Creates a new CalendarWrapper from a specified Calendar object and name.
     * 
     * @param cal   the Calendar object
     * @param name  the name of the calendar
     * @throws JobPersistenceException
     */
    public CalendarWrapper(Calendar cal, String name) throws JobPersistenceException {
        this.name = name;
        this.serialized = RepoJobStoreUtils.serialize(cal);
    }
    
    /**
     * Creates a new CalendarWrapper from an object map.
     * 
     * @param map   a object map
     */
    public CalendarWrapper(Map<String, Object> map) {
        serialized = (String)map.get("serialized");
        name = (String)map.get("name");
        revision = (String)map.get("_rev");
    }
    
    /**
     * Deserializes and returns a Calendar object.
     * 
     * @return  the deserialized Calendar object
     * @throws Exception
     */
    public Calendar getCalendar() throws Exception {
        return (Calendar) RepoJobStoreUtils.deserialize(serialized);
    }
    
    /**
     * Returns the name of the calendar
     * 
     * @return  the name of the calendar
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns a JsonValue object wrapper around the object map for the calendar.
     * 
     * @return a JsonValue object
     */
    public JsonValue getValue() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("serialized", serialized);
        map.put("name", name);
        return new JsonValue(map);
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
