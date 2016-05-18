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
import org.identityconnectors.framework.common.objects.OperationOptions

def operation = operation as OperationType
def configuration = configuration as SSHConfiguration
def connection = connection as SSHConnection
def log = log as Log
def options = options as OperationOptions
def scriptArguments = scriptArguments as Map
def scriptLanguage = scriptLanguage as String
def scriptText = scriptText as String

if ("Shell".equalsIgnoreCase(scriptLanguage)) {

}