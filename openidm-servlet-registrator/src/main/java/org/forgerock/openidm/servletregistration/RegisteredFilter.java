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
package org.forgerock.openidm.servletregistration;

import java.util.UUID;

import javax.servlet.Filter;

import org.forgerock.json.JsonValue;

/**
 * A registered servlet script and it's associated configuration, order number, and ID.
 * 
 *
 */
public interface RegisteredFilter extends Comparable<RegisteredFilter> {

    String FILTER_ORDER = "order";

    /**
     * Returns the registered filter's underlying javax.servlet.Filter object
     * 
     * @return the Filter object
     */
    Filter getFilter();
    
    /**
     * Returns the registered filter's configuration.
     * 
     * @return the configuration.
     */
    JsonValue getConfig();

    /**
     * Returns the registered filter's order number
     * 
     * @return the order number
     */
    Integer getOrder();

    /**
     * Returns the registered filter's unique identifier
     * 
     * @return the UUID
     */
    UUID getId();
    
}
