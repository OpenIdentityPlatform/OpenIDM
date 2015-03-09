/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2013-2015 ForgeRock AS. All Rights Reserved
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

import org.forgerock.json.fluent.JsonValue;

/**
 * An interface for handling different recon types
 */
public interface ReconTypeHandler {
    
    /**
     * Performs a source query returning a {@link ReconQueryResult} object containing the query results.
     * 
     * @param pageSize a page size for the query. The value should be 0 if not paging.
     * @param pagingCookie an optional pagingCookie. The value should be null if not used.
     * @return a {@link ReconQueryResult} object containing the query results.
     * @throws SynchronizationException
     */
    ReconQueryResult querySource(int pageSize, String pagingCookie) throws SynchronizationException;

    /**
     * Performs a source query returning an {@link ResultIterable} object containing the query results.
     * 
     * @return the target identifiers and optional values to reconcile. Can be a sub-set of target identifiers.
     * @throws SynchronizationException
     */
    ResultIterable queryTarget() throws SynchronizationException;
    
    /**
     * Returns a boolean indicating if the target phase should be executed for this recon.
     * 
     * @return true if the target phase should be executed for this recon, false if not
     */
    boolean isRunTargetPhase();
    
    /**
     * Returns a boolean indicating if the recon should allow an empty source set.
     * 
     * @return true if the the recon should allow an empty source set, false otherwise.
     */
    boolean allowEmptySourceSet();

    /**
     * Returns a {@link JsonValue} object containing parameters concerning source and target selection.
     * 
     * @return the recon parameters concerning source and target selection
     */
    public abstract JsonValue getReconParameters();
}
