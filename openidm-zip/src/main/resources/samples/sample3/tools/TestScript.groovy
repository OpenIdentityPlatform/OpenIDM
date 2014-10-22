/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
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
 * @author Gael Allioux <gael.allioux@forgerock.com>
 */

import groovy.sql.Sql
import org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConfiguration
import org.identityconnectors.common.logging.Log
import org.forgerock.openicf.misc.scriptedcommon.OperationType

import java.sql.Connection


def operation = operation as OperationType
def configuration = configuration as ScriptedSQLConfiguration
def connection = new Sql(connection as Connection)
def log = log as Log


log.info("This is TestScript")

// if the database connection isn't properly established this query will result in an error.
// Errors thrown here will prevent the connector from being enabled.
connection.execute("show databases");
