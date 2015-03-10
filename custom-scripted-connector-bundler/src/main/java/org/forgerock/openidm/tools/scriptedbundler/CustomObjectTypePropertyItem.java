/*
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
 */

package org.forgerock.openidm.tools.scriptedbundler;

import java.util.ArrayList;
import java.util.List;

/**
 * A single connector configuration objectType, contained in {@link CustomObjectTypeProperty}.
 *
 * Represented in a JSON configuration as:
 * <pre><blockquote>
 *      {
 *          "type" : "object",
 *          "properties" : [{
 *              "name" : "uid",
 *              "type" : "string"
 *          }]
 *      }
 * </blockquote></pre>
 */
public class CustomObjectTypePropertyItem extends CustomBaseObject {

    private String type;

    private List<CustomObjectTypePropertyItemProperty> properties = new ArrayList<CustomObjectTypePropertyItemProperty>();


    /**
     * Return the type of this item.
     *
     * @return
     */
    public String getType() {
        return type;
    }

    /**
     * Set the type of this item.
     *
     * @param type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Return the list of {@link CustomObjectTypePropertyItemProperty} for this item.
     *
     * @return
     */
    public List<CustomObjectTypePropertyItemProperty> getProperties() {
        return flagLast(properties);
    }

    /**
     * Set the {@link CustomObjectTypePropertyItemProperty} list for this item.
     *
     * @param properties
     */
    public void setProperties(List<CustomObjectTypePropertyItemProperty> properties) {
        this.properties.clear();
        this.properties.addAll(flagLast(properties));
    }
}
