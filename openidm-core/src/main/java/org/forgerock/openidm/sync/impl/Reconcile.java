/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2012-2013 ForgeRock AS. All Rights Reserved
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
import org.forgerock.json.resource.ResourceException;


/**
 * Reconciliation service interface.
 * Used for implementation purposed, business logic should
 * instead invoke it via the router.
 */
public interface Reconcile {
    /**
     * Reconcile the given mapping according to the requested options
     * @param reconAction the recon action
     * @param mapping the mapping configuration
     * @param synchronous whether to synchronously (TRUE) wait for the reconciliation run, or
     *  to return immediately (FALSE) with the recon id, which can then be used for subsequent
     *  queries / actions on that reconciliation run.
     * @param reconParams all parameters passed to the recon invocation
     * @param config the overriding config
     */
    public String reconcile(
            ReconciliationService.ReconAction reconAction,
            JsonValue mapping,
            Boolean synchronous,
            JsonValue reconParams,
            JsonValue config)
            throws ResourceException;
}
