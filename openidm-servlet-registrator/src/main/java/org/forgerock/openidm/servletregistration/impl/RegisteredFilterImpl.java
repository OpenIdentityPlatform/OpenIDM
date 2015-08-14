/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright 2013-2015 ForgeRock AS. All Rights Reserved
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
package org.forgerock.openidm.servletregistration.impl;

import java.util.UUID;

import javax.servlet.Filter;

import org.forgerock.json.JsonValue;
import org.forgerock.openidm.servletregistration.RegisteredFilter;

/**
 * A RegisteredFilter implementation
 * 
 */
public class RegisteredFilterImpl implements RegisteredFilter {
 
    private UUID id = null;
    private JsonValue config = null;
    private Integer order = null;
    private Filter filter = null;
    
    protected RegisteredFilterImpl(JsonValue config) {
        this.config = config;
        id = UUID.randomUUID();
        order = config.get(FILTER_ORDER).defaultTo(0).asInteger();
    }

    @Override
    public int compareTo(RegisteredFilter filter) {
        return order.compareTo(filter.getOrder());
    }

    @Override
    public boolean equals(Object filter) {
        if (filter instanceof RegisteredFilter) {
            return getId().equals(((RegisteredFilter) filter).getId());
        }
        return false;
    }

    /**
     * Setter for the Filter object
     * 
     * @param filter the Filter object
     */
    protected void setFilter(Filter filter) {
        this.filter = filter;
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    @Override
    public JsonValue getConfig() {
        return config;
    }

    @Override
    public Integer getOrder() {
        return order;
    }

    @Override
    public UUID getId() {
        return id;
    }
}
