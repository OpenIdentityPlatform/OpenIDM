/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.repo.orientdb.impl;

import com.orientechnologies.orient.core.record.impl.ODocument;

import java.util.Map;
import java.util.LinkedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class for handling OrientDB ODocuments
 * 
 * @author aegloff
 */
public class DocumentUtil  {
    final static Logger logger = LoggerFactory.getLogger(DocumentUtil.class);
    /**
     * Convert to simple binding model akin to JSON simple binding
     * @param doc the OrientDB document to convert
     * @return the doc converted into maps, lists, java types; or null if the doc was null
     */
    public static Map<String, Object> toMap(ODocument doc) {    	
        Map<String, Object> result = null;
        if (doc != null) {
            result = new LinkedHashMap<String, Object>(); // Do we really need to maintain order?   
            result.put("_id", doc.getIdentity().toString()); 
            result.put("_rev", doc.getVersion());
            for (java.util.Map.Entry<String, Object> entry : doc) {
                Object value = entry.getValue();
                // TODO: Handle Embeddedset/Embeddedlist/Embeddedmap
                // TODO: Handle reference relationship
                result.put(entry.getKey(), value);
            }
        }
        logger.trace("Converted document {} to {}", doc, result);
        return result;
    }
}