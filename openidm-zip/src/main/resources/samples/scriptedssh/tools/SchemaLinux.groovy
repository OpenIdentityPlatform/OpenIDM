/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for
 * the specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file
 * and include the License file at legal/CDDLv1.0.txt. If applicable, add the following
 * below the CDDL Header, with the fields enclosed by brackets [] replaced by your
 * own identifying information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */

package samples.scriptedssh.tools

import org.forgerock.openicf.connectors.ssh.SSHConfiguration
import org.forgerock.openicf.connectors.ssh.SSHConnection
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.common.security.GuardedString
import org.identityconnectors.framework.spi.operations.AuthenticateOp
import org.identityconnectors.framework.spi.operations.ResolveUsernameOp
import org.identityconnectors.framework.spi.operations.SyncOp

import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.MULTIVALUED

def operation = operation as OperationType
def configuration = configuration as SSHConfiguration
def connection = connection as SSHConnection
def log = log as Log

log.info("Entering {0} script", operation);
// We assume the operation is SCHEMA
assert operation == OperationType.SCHEMA, 'Operation must be a SCHEMA'

builder.schema({
    objectClass {
        type ObjectClass.ACCOUNT_NAME
        attribute OperationalAttributes.PASSWORD_NAME, GuardedString.class
        attributes {
            "description" String.class
            "home" String.class
            "group" String.class
            "shell" String.class
            "uid" String.class
            "expiryDate" String.class
        }
        disable SyncOp.class
    }
    objectClass {
        type ObjectClass.GROUP_NAME
        attributes {
            "gid" String.class
            "members" String.class, MULTIVALUED
        }
        disable SyncOp.class, AuthenticateOp.class, ResolveUsernameOp.class
    }
})