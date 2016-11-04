/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2011-2016 ForgeRock AS.
 */
package org.forgerock.openidm.repo.jdbc.impl;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.forgerock.json.resource.InternalServerErrorException;

/**
 * Handles the conversion of ResultSets into Object set results
 *
 */
public interface ResultSetMapper {
    
    List<Map<String, Object>> mapToObject(ResultSet rs, String queryId, String type, Map<String, Object> params)
            throws SQLException, IOException, InternalServerErrorException;

    List<Map<String, Object>> mapToRawObject(ResultSet rs) throws SQLException,
            IOException, InternalServerErrorException;
}
