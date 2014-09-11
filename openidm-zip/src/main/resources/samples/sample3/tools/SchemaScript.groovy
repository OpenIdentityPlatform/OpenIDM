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

import org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log

import java.sql.Connection

import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.REQUIRED

def operation = operation as OperationType
def configuration = configuration as ScriptedSQLConfiguration
def connection = connection as Connection
def log = log as Log

builder.schema({
    objectClass {
        type ObjectClass.ACCOUNT_NAME
        attributes {
            uid String.class, REQUIRED
            password String.class, REQUIRED
            firstname String.class, REQUIRED
            lastname String.class, REQUIRED
            fullname String.class, REQUIRED
            email String.class, REQUIRED
            organization String.class, REQUIRED
        }

    }
    objectClass {
        type ObjectClass.GROUP_NAME
        attributes {
            name String.class, REQUIRED
            gid String.class, REQUIRED
            description String.class, REQUIRED
        }
    }
    objectClass {
        type 'organization'
        attributes {
            name String.class, REQUIRED
            description String.class, REQUIRED
        }
    }
}
)
