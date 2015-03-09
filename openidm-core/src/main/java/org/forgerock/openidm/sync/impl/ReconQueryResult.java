/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2015 ForgeRock AS. All Rights Reserved
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
import java.util.Iterator;

/**
 *  Contains an iterator over the recon query results as well as a
 *  paging cookie relating to the query.
 */
public class ReconQueryResult {

    /**
     * A {@link ResultIterable} containing the query results.
     */
    private ResultIterable resultIterable;
    
    /**
     * A {@link String} representing a paging cookie.
     */
    private String pagingCookie;

    /**
     * A no-argument constructor.
     */
    protected ReconQueryResult() {
        this.resultIterable = null;
        this.pagingCookie = null;
    }
    
    /**
     * A constructor.
     * 
     * @param resultIterable the query results.
     */
    protected ReconQueryResult(ResultIterable resultIterable) {
        this.resultIterable = resultIterable;
        this.pagingCookie = null;
    }
    
    /**
     * A constructor.
     * 
     * @param resultIterable the query results.
     * @param pagingCookie a paging cookie.
     */
    protected ReconQueryResult(ResultIterable resultIterable, String pagingCookie) {
        this.resultIterable = resultIterable;
        this.pagingCookie = pagingCookie;
    }

    /**
     * Returns a {@link Collection} of all IDs.
     * 
     * @return a {@link Collection} of all IDs.
     */
    public Collection<String> getAllIds() {
        return resultIterable.getAllIds();
    }
    
    /**
     * Returns a {@link ResultIterable} containing the query results.
     * 
     * @return a {@link ResultIterable} instance containing the query results.
     */
    public ResultIterable getResultIterable() {
        return resultIterable;
    }
    
    /**
     * Returns an {@link Iterator} over the query results.
     * 
     * @return an {@link Iterator} over the query results.
     */
    public Iterator<ResultEntry> getIterator() {
        return resultIterable.iterator();
    }

    public String getPagingCookie() {
        return pagingCookie;
    }

    /**
     * Sets the {@link ResultIterable} containing the query results.
     * 
     * @param resultIterable the {@link ResultIterable} to set.
     */
    public void setResultIterable(ResultIterable resultIterable) {
        this.resultIterable = resultIterable;
    }

    /**
     * Sets the paging cookie.
     * 
     * @param pagingCookie the paging cookie to set.
     */
    public void setPagingCookie(String pagingCookie) {
        this.pagingCookie = pagingCookie;
    }
    
}
