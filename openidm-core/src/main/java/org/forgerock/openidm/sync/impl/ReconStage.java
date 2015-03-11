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

/**
 * Represents the different state and stages of the reconciliation process
 *
 */
enum ReconStage {

    /**
     * The initial state when a reconciliation run is first created..
     */
    ACTIVE_INITIALIZED("initialized"),

    /**
     * Querying the source, target and possibly link sets (ids) to reconcile
     */
    ACTIVE_QUERY_ENTRIES("querying sets of entries to reconcile"),

    /**
     * Reconciling the set of ids retrieved from the mapping source
     */
    ACTIVE_RECONCILING_SOURCE("reconciling source entries"),

    /**
     * Reconciling any remaining entries from the set of ids 
     * retrieved from the mapping target which have not yet been 
     * matched / processed in the source phase.
     */
    ACTIVE_RECONCILING_TARGET("reconciling target entries"),
    
    /**
     * Check if any links are now unused and need clean-up
     */
    ACTIVE_LINK_CLEANUP("link clean-up"),

    /**
     * Post-processing of reconciliation results
     */
    ACTIVE_PROCESSING_RESULTS("processing reconciling results"),
    
    /**
     * Try and abort reconciliation run in progress
     */
    ACTIVE_CANCELING("aborting reconciling"),

    /**
     * Completed processing reconciliation run successfully
     */
    COMPLETED_SUCCESS("reconciliation completed."),
    
    /**
     * Completed processing because reconciliation run was aborted
     */
    COMPLETED_CANCELED("reconciliation aborted."),

    /**
     * Finished processing reconciliation because of a failure
     */
    COMPLETED_FAILED("reconciliation failed");

    /** A human readable short description of the reconciliation state */
    private String description;
    
    /**
     * Initialize the enum
     * @param a short description of the reconciliation state 
     */
    ReconStage(String description) {
        this.description = description;
    }
    
    /**
     * @return whether it is in one of the completed states
     */
    public boolean isComplete() {
        return (this == COMPLETED_CANCELED || this == COMPLETED_FAILED || this == COMPLETED_SUCCESS) ;
    }
    
    /**
     * @return state implied by the stage
     * States can be 
     * ACTIVE   - reconciliation run in progress
     * CANCELED - reconciliation run successfully canceled
     * FAILED   - reconciliation run finished because of a failure
     * SUCCESS  - reconciliation run completed successfully
     */
    public String getState() {
        if (!isComplete()) {
            return "ACTIVE";
        } else if (this == COMPLETED_CANCELED){
            return "CANCELED";
        } else if (this == COMPLETED_FAILED){
            return "FAILED";
        } else if (this == COMPLETED_SUCCESS){
            return "SUCCESS";
        } else {
            throw new RuntimeException("Unexpected state " + this.getDescription());
        }
    }
    
    /**
     * @return a short description of the reconciliation state 
     */
    public String getDescription() {
        return description;
    }
}
