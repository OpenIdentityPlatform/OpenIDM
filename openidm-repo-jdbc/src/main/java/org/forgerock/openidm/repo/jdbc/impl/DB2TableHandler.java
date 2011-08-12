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
package org.forgerock.openidm.repo.jdbc.impl;

import org.forgerock.json.fluent.JsonNode;

import java.util.Map;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class DB2TableHandler extends GenericTableHandler {

    public DB2TableHandler(String mainTableName, String propTableName, String dbSchemaName, JsonNode queriesConfig, int maxBatchSize) {
        super(mainTableName, propTableName, dbSchemaName, queriesConfig, maxBatchSize);
    }

    protected Map<QueryDefinition, String> initializeQueryMap() {
        Map<QueryDefinition, String> result = super.initializeQueryMap();

        // Main object table DB2 Script
        result.put(QueryDefinition.DELETEQUERYSTR, "DELETE FROM " + dbSchemaName + "." + mainTableName + " obj WHERE EXISTS (SELECT 1 FROM " + dbSchemaName + ".objecttypes objtype WHERE obj.objecttypes_id = objtype.id AND objtype.objecttype = ?) AND obj.objectid = ? AND obj.rev = ?");
        return result;
    }
}
