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
package org.forgerock.openidm.sync.impl;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.forgerock.json.fluent.JsonValue;

/**
 * Represents the information and functionality for a 
 * reconciliation run
 * 
 * @author aegloff
 */
public class ReconciliationContext {

    private boolean complete = false;
    private boolean canceled = false;
    private String reconId; 
    
    // If set, the list of all queried source Ids
    private Set<String> sourceIds; 
    // If set, the list of all queried target Ids
    private Set<String> targetIds;

    /**
     * Creates the instance with info from the current call context
     * @param callingContext The resource call context
     */
    public ReconciliationContext(JsonValue callingContext) {
         reconId = callingContext.get("uuid").required().asString();
    }
    
    /**
     * @return A unique identifier for the reconciliation run
     */
    public String getReconId() {
        return reconId;
    }
    
    /**
     * Cancel the reconciliation run.
     * May not take immediate effect in stopping the reconciliation logic.
     */
    public void cancel() {
        canceled = true;
    }

    /**
     * @return Whether the reconciliation run has been canceled.
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * @return Statistics about this reconciliation run
     */
    /* TODO
    public ReconStats getStatistics() {
        return null; 
    }
    */
    
    /**
     * @param sourceIds the list of all ids in the source object set
     */
    void setSourceIds(List<String> sourceIds) {
        // Choose a hash based collection as we need fast "contains" handling
        this.sourceIds = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
        this.sourceIds.addAll(sourceIds);
    }
    
    /**
     * @param targetIds the list of all ids in the target object set
     */
    void setTargetIds(List<String> targetIds) {
        // Choose a hash based collection as we need fast "contains" handling
        this.targetIds = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
        this.targetIds.addAll(targetIds);
    }

    /**
     * @return the list of all ids in the source object set, 
     * queried at the outset of reconciliation. 
     * Null if no bulk source id query was done.
     */
    public Set<String> getSourceIds() {
        return sourceIds;
    }

    /**
     * @return the list of all ids in the target object set, 
     * queried at the outset of reconciliation. 
     * Null if no bulk target id query was done.
     */
    public Set<String> getTargetIds() {
        return targetIds;
    }
    
    /**
     * Flags the reconciliation run a complete.
     * May clean up state information that is not needed past the run.
     */
    public void setComplete() {
        complete = true;
        cleanupState();
    }
    
    /**
     * Remove any state from memory that should not be kept 
     * past the completion of the reconciliaiton run
     */
    private void cleanupState() {
        sourceIds = null;
        targetIds = null;
    }

}
