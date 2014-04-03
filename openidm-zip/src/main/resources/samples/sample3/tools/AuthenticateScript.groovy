/*
 *
 * Copyright (c) 2014 ForgeRock Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1.php or
 * OpenIDM/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at OpenIDM/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted 2010 [name of copyright owner]"
 *
 * $Id$
 */
import groovy.sql.Sql;
import org.identityconnectors.framework.common.exceptions.InvalidPasswordException;

// Parameters:
// The connector sends the following:
// connection
// configuration
// action ("AUTHENTICATE")
// log
// objectClass
// options
// username
// password

// It is expected that an authentication failure will throw an error from the package org.identityconnectors.framework.common.exceptions

log.info("Entering "+action+" Script");
def sql = new Sql(connection);
def authId = null;

sql.eachRow("SELECT uid FROM Users WHERE uid = ? AND password = sha2(?, 512)", [username, password]) { authId = it.uid }

if (authId == null) {
    throw new InvalidPasswordException("Authentication Failed")
}

return authId