/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 ForgeRock AS. All rights reserved.
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
import groovy.sql.Sql;
import org.identityconnectors.common.logging.Log;

import java.sql.Connection;
// Parameters:
// The connector sends the following:
// connection: handler to the SQL connection
// action: a string describing the action ("TEST" here)
// log: a handler to the Log facility

def sql = new Sql(connection as Connection);
def log = log as Log;

log.info("Entering Test Script");

// a relatively-cheap query to run on start up to ensure database connectivity
sql.eachRow("select * from auditrecon limit 1", { } );
sql.eachRow("select * from auditactivity limit 1", { } );
sql.eachRow("select * from auditaccess limit 1", { } );
sql.eachRow("select * from auditsync limit 1", { } );

