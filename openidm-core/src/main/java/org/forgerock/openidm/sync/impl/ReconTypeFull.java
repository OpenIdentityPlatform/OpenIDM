/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.openidm.sync.SynchronizationException;


/**
 * Represents a full reconciliation
 * @author aegloff
 */
public class ReconTypeFull implements ReconTypeHandler {
    ReconciliationContext reconContext;
    public ReconTypeFull(ReconciliationContext reconContext) {
        this.reconContext = reconContext;
    }

    public List<String> querySourceIds() throws SynchronizationException {
        List<String> sourceIds = queryAllIds(reconContext.getObjectMapping().getSourceObjectSet(), reconContext);
        return sourceIds;
    }

    /**
     * Get all IDs for a given object set as List and in case sensitive fashion
     * @see queryAllIds(String, ReconciliationContext, Collection)
     */
    private List<String> queryAllIds(final String objectSet, ReconciliationContext reconContext) 
            throws SynchronizationException {
        return (List<String>) queryAllIds(objectSet, reconContext, Collections.synchronizedList(new ArrayList<String>()), true);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRunTargetPhase() {
        return true;
    }

    /**
     * Get all IDs for a given object set
     *
     * @param objectSet the object set to query
     * @param collectionToPopulate the collection to populate with results
     * @param caseSensitive whether the collection should be populated in case
     * sensitive fashion, or if false it populates as lower case only  
     * @return the collection of (unqualified) ids
     * @throws SynchronizationException if retrieving or processing the ids failed
     */
    private Collection<String> queryAllIds(final String objectSet, ReconciliationContext reconContext, 
            Collection<String> collectionToPopulate, boolean caseSensitive) throws SynchronizationException {
        Collection<String> ids = collectionToPopulate;

        HashMap<String, Object> query = new HashMap<String, Object>();
        query.put(QueryConstants.QUERY_ID, QueryConstants.QUERY_ALL_IDS);
        try {
            JsonValue objList = new JsonValue(reconContext.getService().getRouter().query(objectSet, query))
                    .get(QueryConstants.QUERY_RESULT).required().expect(List.class);
            for (JsonValue obj : objList) {
                String value = obj.get("_id").asString();
                if (!caseSensitive) {
                    value = (value == null ? null : reconContext.getObjectMapping().getLinkType().normalizeId(value));
                }
                ids.add(value);
            }
        } catch (JsonValueException jve) {
            throw new SynchronizationException(jve);
        } catch (ObjectSetException ose) {
            throw new SynchronizationException(ose);
        }

        reconContext.checkCanceled(); // Throws an exception if reconciliation was canceled
        return ids;
    }
}
