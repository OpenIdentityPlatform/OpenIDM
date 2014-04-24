/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2012-2014 ForgeRock AS. All Rights Reserved
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

import java.util.Collection;
import java.util.Map;
import org.forgerock.json.resource.Context;

/**
 * Reconciliation action interface.
 * Implementation is passed to ReconPhase and executed by the ReconTask
 * 
 * @author cgdrake
 */
public interface ReconAction {
    /**
     * Reconcile a given object ID
     * @param id the object id to reconcile
     * @param reconContext reconciliation context
     * @param rootContext json resource root ctx
     * @param allLinks all links if pre-queried, or null for on-demand link querying
     * @param remainingTargetIds The set to update/remove any targets that were matched
     * @throws SynchronizationException if there is a failure reported in reconciling this id
     */
    public void recon(String id, ReconciliationContext reconContext, Context rootContext, Map<String, Link> allLinks, Collection<String> remainingIds)  throws SynchronizationException;
}
